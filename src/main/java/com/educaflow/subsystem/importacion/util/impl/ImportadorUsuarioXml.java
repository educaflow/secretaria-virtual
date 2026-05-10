package com.educaflow.subsystem.importacion.util.impl;

import com.axelor.db.JpaRepository;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.base.util.XMLUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.subsystem.common.service.CentroService;
import com.educaflow.subsystem.common.service.CentroUsuarioService;
import com.educaflow.subsystem.common.service.TipoUsuarioService;
import com.educaflow.subsystem.importacion.db.TareaImportacion;
import com.educaflow.subsystem.importacion.db.TipoFicheroImportacion;
import com.educaflow.subsystem.importacion.util.ImportadorException;
import com.educaflow.subsystem.importacion.util.ImportadorFichero;
import com.educaflow.subsystem.importacion.util.ImportadorUsuarioUtil;
import com.educaflow.subsystem.importacion.util.MensajeImportacion;
import com.educaflow.subsystem.importacion.util.ResultadoImportacion;
import com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado;
import com.educaflow.subsystem.registrousuario.service.UsuarioAutorizadoService;
import com.google.inject.Inject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ImportadorUsuarioXml implements ImportadorFichero {

    private static final Map<TipoFicheroImportacion, String> NODO_ITEMS = Map.of(
            TipoFicheroImportacion.PROFESOR, "docente",
            TipoFicheroImportacion.ALUMNO, "alumno",
            TipoFicheroImportacion.FAMILIAR, "familiar"
    );

    private static final Map<TipoFicheroImportacion, String> SCHEMAS = Map.of(
            TipoFicheroImportacion.PROFESOR, "/data-import/schemas/profesores.xsd",
            TipoFicheroImportacion.ALUMNO,   "/data-import/schemas/alumnos.xsd",
            TipoFicheroImportacion.FAMILIAR, "/data-import/schemas/familiares.xsd"
    );

    private static final DateTimeFormatter FORMATO_FECHA_EXPORTACION =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final byte[] contenido;
    private final TipoFicheroImportacion tipoFicheroImportacion;
    private final String nodoItem;
    private final String tipoUsuarioCode;
    private final String schemaPath;
    private final Document document;
    private TipoUsuario tipoUsuario;
    private Centro centro;
    private Integer curso;

    @Inject
    private ModelServiceFactory modelServiceFactory;

    public ImportadorUsuarioXml(TipoFicheroImportacion tipoFicheroImportacion, byte[] contenido) {
        this.tipoFicheroImportacion = tipoFicheroImportacion;
        this.contenido = contenido;
        this.nodoItem = NODO_ITEMS.get(tipoFicheroImportacion);
        this.tipoUsuarioCode = tipoFicheroImportacion.getValue();
        this.schemaPath = SCHEMAS.get(tipoFicheroImportacion);
        this.document = XMLUtil.getDocument(contenido);
    }

    @Override
    public ResultadoImportacion importar() {
        this.tipoUsuario = ((TipoUsuarioService) modelServiceFactory.resolve(TipoUsuario.class))
                .findByCodigo(tipoUsuarioCode)
                .orElseThrow(() -> new ImportadorException("Tipo de usuario no encontrado: " + tipoUsuarioCode));
        validarEsquema(contenido);
        centro = getCentroFromXml().orElseThrow(() -> new ImportadorException("Centro no encontrado en el fichero XML"));
        curso = getCursoFromXml().orElseThrow(() -> new ImportadorException("Curso no encontrado en el fichero XML"));
        comprobarCentroActivo();
        comprobarCursoNoFuturo();

        LocalDate fechaExportacion = getFechaExportacionFromXml()
                .orElseThrow(() -> new ImportadorException("Fecha de exportación no encontrada en el fichero XML"));

        boolean esActual = esImportacionActual(fechaExportacion);
        UsuarioAutorizadoService usuarioAutorizadoService =
                (UsuarioAutorizadoService) modelServiceFactory.resolve(UsuarioAutorizado.class);

        if (esActual) {
            usuarioAutorizadoService.marcarTodosInactivos(centro, tipoUsuario);
        }

        NodeList items = document.getDocumentElement().getElementsByTagName(nodoItem);
        List<MensajeImportacion> mensajes = new ArrayList<>();
        int[] contadores = procesarItems(items, mensajes, esActual);

        int avisos = 0;
        if (esActual) {
            CentroUsuarioService centroUsuarioService =
                    (CentroUsuarioService) modelServiceFactory.resolve(CentroUsuario.class);
            List<String> cambios = centroUsuarioService.calcularTiposUsuarioRegistrados(centro.getId(), tipoUsuario);
            cambios.stream()
                    .map(cambio -> new MensajeImportacion(null, null, cambio))
                    .forEach(mensajes::add);
            avisos = cambios.size();
        }

        String resumen = ImportadorUsuarioUtil.construirResumen(tipoUsuario, centro, curso,
                contadores[0], contadores[1], avisos, items.getLength());
        return new ResultadoImportacion(resumen, mensajes, centro, curso, fechaExportacion);
    }

    private boolean esImportacionActual(LocalDate fechaExportacion) {
        TareaImportacion ultima = JpaRepository.of(TareaImportacion.class)
                .all()
                .filter("self.centro.id = :centroId" +
                        " AND self.tipoFichero = :tipoFichero" +
                        " AND self.fechaExportacion IS NOT NULL")
                .bind("centroId", centro.getId())
                .bind("tipoFichero", tipoFicheroImportacion)
                .order("-fechaExportacion")
                .fetchOne();
        return ultima == null || !fechaExportacion.isBefore(ultima.getFechaExportacion());
    }

    private void validarEsquema(byte[] contenido) {
        try (InputStream schema = getClass().getResourceAsStream(schemaPath);
             InputStream data = new ByteArrayInputStream(contenido)) {
            if (schema == null) {
                throw new ImportadorException("Esquema de validación no encontrado: " + schemaPath);
            }
            Optional<String> error = XMLUtil.validarConSchema(data, schema);
            if (error.isPresent()) {
                throw new ImportadorException("El fichero XML no es válido: " + error.get());
            }
        } catch (IOException e) {
            throw new ImportadorException("Error leyendo el esquema de validación: " + schemaPath);
        }
    }

    private void comprobarCursoNoFuturo() {
        Integer cursoCentro = centro.getCurso();
        if (cursoCentro != null && curso > cursoCentro) {
            throw new ImportadorException(
                    "El fichero pertenece al curso " + curso +
                    ", pero el curso activo del centro es " + cursoCentro +
                    ". No se pueden importar datos de un curso posterior al activo.");
        }
    }

    private void comprobarCentroActivo() {
        Centro centroActivo = SecurityUtil.getUser().getCentroActivo();
        if (centroActivo == null) {
            throw new ImportadorException("No tienes un centro activo asignado en tu perfil");
        }
        if (!centroActivo.getCode().equals(centro.getCode())) {
            throw new ImportadorException(
                    "El fichero pertenece al centro '" + centro.getName() +
                    "', pero tu centro activo es '" + centroActivo.getName() + "'");
        }
    }

    private int[] procesarItems(NodeList items, List<MensajeImportacion> mensajes, boolean esActual) {
        int creados = 0;
        int errores = 0;
        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            String documentoRaw = item.getAttribute("documento");
            try {
                ImportadorUsuarioUtil.procesarItem(documentoRaw, centro, tipoUsuario, esActual, modelServiceFactory);
                creados++;
            } catch (Exception e) {
                errores++;
                mensajes.add(new MensajeImportacion(i + 1, documentoRaw, e.getMessage()));
            }
        }
        return new int[]{creados, errores};
    }

    private Optional<Centro> getCentroFromXml() {
        String codigoCentro = document.getDocumentElement().getAttribute("codigo");
        CentroService centroService = (CentroService) modelServiceFactory.resolve(Centro.class);
        return centroService.findByCodigo(codigoCentro);
    }

    private Optional<Integer> getCursoFromXml() {
        String valor = document.getDocumentElement().getAttribute("curso");
        if (valor.isBlank()) return Optional.empty();
        return Optional.of(Integer.parseInt(valor));
    }

    private Optional<LocalDate> getFechaExportacionFromXml() {
        String valor = document.getDocumentElement().getAttribute("fechaExportacion");
        if (valor.isBlank()) return Optional.empty();
        try {
            return Optional.of(LocalDateTime.parse(valor, FORMATO_FECHA_EXPORTACION).toLocalDate());
        } catch (DateTimeParseException e) {
            throw new ImportadorException(
                    "Formato de fecha de exportación inválido: " + valor + " (se espera dd/MM/yyyy HH:mm:ss)");
        }
    }
}