package com.educaflow.subsystem.importacion.service.tipoimportador.impl;

import com.axelor.db.JpaRepository;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.inject.Beans;
import com.educaflow.base.util.DniUtil;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.base.util.XMLUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.subsystem.common.service.CentroUsuarioService;
import com.educaflow.subsystem.importacion.service.tipoimportador.ImportadorException;
import com.educaflow.subsystem.importacion.service.tipoimportador.ImportadorFichero;
import com.educaflow.subsystem.importacion.service.tipoimportador.ResultadoImportacion;
import com.educaflow.subsystem.importacion.service.tipoimportador.ResultadoImportacion.MensajeImportacion;
import com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado;
import com.educaflow.subsystem.registrousuario.db.repo.UsuarioAutorizadoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ImportadorUsuarioXml implements ImportadorFichero {

    private final String nodoItem;
    private final String tipoUsuarioCode;
    private final String schemaPath;
    private final UsuarioAutorizadoRepository usuarioAutorizadoRepository;

    private final Logger logger = LoggerFactory.getLogger(ImportadorUsuarioXml.class);

    public ImportadorUsuarioXml(String nodoItem, String tipoUsuarioCode, String schemaPath) {
        this.nodoItem = nodoItem;
        this.tipoUsuarioCode = tipoUsuarioCode;
        this.schemaPath = schemaPath;
        this.usuarioAutorizadoRepository = (UsuarioAutorizadoRepository) JpaRepository.of(UsuarioAutorizado.class);

    }

    @Override
    public ResultadoImportacion importar(byte[] contenido, Centro centro, Integer curso) {
        validarEsquema(contenido);
        comprobarCentroActivo(centro);
        TipoUsuario tipoUsuario = obtenerTipoUsuario().orElseThrow(
                () -> new ImportadorException("Tipo de usuario no encontrado: " + tipoUsuarioCode)
        );

        logger.info("Importando usuarios del tipo '{}' para el centro '{}' y curso '{}'",
                tipoUsuario.getName(), centro.getName(), curso);

        NodeList items = parsearItems(contenido);
        List<MensajeImportacion> mensajes = new ArrayList<>();
        int[] contadores = procesarItems(items, centro, curso, tipoUsuario, mensajes);

        CentroUsuarioService centroUsuarioService = (CentroUsuarioService) Beans.get(ModelServiceFactory.class).resolve(CentroUsuario.class);
        if (curso <= centro.getCurso()) {
            centroUsuarioService.calcularTiposUsuarioRegistrados(centro.getId())
                    .stream()
                    .map(cambio -> new MensajeImportacion(null, null, cambio))
                    .forEach(mensajes::add);
        }

        String resumen = construirResumen(centro, tipoUsuario, curso, contadores[0], contadores[1], mensajes.size(), items.getLength());
        return new ResultadoImportacion(resumen, mensajes);
    }

    private NodeList parsearItems(byte[] contenido) {
        Document doc = XMLUtil.getDocument(contenido);
        return doc.getDocumentElement().getElementsByTagName(nodoItem);
    }

    private int[] procesarItems(NodeList items, Centro centro, Integer curso, TipoUsuario tipoUsuario,
                                 List<MensajeImportacion> mensajes) {
        usuarioAutorizadoRepository.deleteByCentroAndCursoAndTipoUsuario(centro.getId(), curso, tipoUsuario.getId());

        int creados = 0;
        int errores = 0;
        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            String documentoRaw = item.getAttribute("documento");
            try {
                procesarItem(documentoRaw, centro, curso, tipoUsuario);
                creados++;
            } catch (Exception e) {
                errores++;
                mensajes.add(new MensajeImportacion(i + 1, documentoRaw, e.getMessage()));
            }
        }
        return new int[]{creados, errores};
    }

    private void procesarItem(String documentoRaw, Centro centro, Integer curso, TipoUsuario tipoUsuario) {
        String dni = DniUtil.clean(documentoRaw);
        if (!DniUtil.isValid(dni)) {
            throw new ImportadorException("DNI inválido: " + documentoRaw);
        }
        UsuarioAutorizado ua = new UsuarioAutorizado();
        ua.setCentro(centro);
        ua.setCurso(curso);
        ua.setDni(dni);
        ua.setTipoUsuario(tipoUsuario);
        usuarioAutorizadoRepository.save(ua);
    }

    private String construirResumen(Centro centro, TipoUsuario tipoUsuario, Integer curso,
                                    int creados, int errores, int avisos, int total) {
        return "Importando usuarios del tipo '" + tipoUsuario.getName() +
                "' para el centro '" + centro.getName() +
                "' y curso '" + curso + "'\n" +
                "Importados: " + creados +
                " | Errores: " + errores +
                " | Avisos: " + avisos +
                " | Total: " + total;
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

    private void comprobarCentroActivo(Centro centro) {
        Centro centroActivo = SecurityUtil.getUser().getCentroActivo();
        if (centroActivo == null) {
            throw new ImportadorException("No tienes un centro activo asignado en tu perfil");
        }
        if (!centroActivo.getCode().equals(centro.getCode())) {
            throw new ImportadorException(
                    "El fichero pertenece al centro '" + centro.getName() + "', pero tu centro activo es '" + centroActivo.getName() + "'");
        }
    }

    private Optional<TipoUsuario> obtenerTipoUsuario() {
        TipoUsuario tipoUsuario = JpaRepository.of(TipoUsuario.class).all()
                .filter("self.code = :code")
                .bind("code", tipoUsuarioCode)
                .fetchOne();
        return Optional.ofNullable(tipoUsuario);
    }
}