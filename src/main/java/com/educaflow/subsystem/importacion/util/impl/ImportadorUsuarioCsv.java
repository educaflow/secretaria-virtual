package com.educaflow.subsystem.importacion.util.impl;

import com.axelor.db.modelservice.ModelServiceFactory;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.subsystem.common.service.CentroUsuarioService;
import com.educaflow.subsystem.common.service.TipoUsuarioService;
import com.educaflow.subsystem.importacion.db.TipoFicheroImportacion;
import com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado;
import com.educaflow.subsystem.registrousuario.service.UsuarioAutorizadoService;
import com.educaflow.subsystem.importacion.util.ImportadorException;
import com.educaflow.subsystem.importacion.util.ImportadorFichero;
import com.educaflow.subsystem.importacion.util.ImportadorUsuarioUtil;
import com.educaflow.subsystem.importacion.util.MensajeImportacion;
import com.educaflow.subsystem.importacion.util.ResultadoImportacion;
import com.google.inject.Inject;

import java.io.BufferedReader;
import java.time.LocalDate;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ImportadorUsuarioCsv implements ImportadorFichero {

    private final byte[] contenido;
    private final String tipoUsuarioCode;
    private TipoUsuario tipoUsuario;
    private final Centro centro;
    private final Integer curso;

    @Inject
    private ModelServiceFactory modelServiceFactory;

    public ImportadorUsuarioCsv(TipoFicheroImportacion tipoFicheroImportacion, byte[] contenido) {
        this.contenido = contenido;
        this.tipoUsuarioCode = tipoFicheroImportacion.getValue();
        this.centro = SecurityUtil.getUser().getCentroActivo();
        this.curso = centro.getCurso();
    }

    @Override
    public ResultadoImportacion importar() {
        this.tipoUsuario = ((TipoUsuarioService) modelServiceFactory.resolve(TipoUsuario.class))
                .findByCodigo(tipoUsuarioCode)
                .orElseThrow(() -> new ImportadorException("Tipo de usuario no encontrado: " + tipoUsuarioCode));
        List<String> lineas = parsearLineas();
        List<MensajeImportacion> mensajes = new ArrayList<>();
        int[] contadores = procesarItems(lineas, mensajes);

        sincronizarCentroUsuarioTipoUsuario();

        String resumen = ImportadorUsuarioUtil.construirResumen(tipoUsuario, centro, curso, contadores[0], contadores[1], mensajes.size(), lineas.size());
        return new ResultadoImportacion(resumen, mensajes, centro, curso, LocalDate.now());
    }

    private void sincronizarCentroUsuarioTipoUsuario() {
        UsuarioAutorizadoService usuarioAutorizadoService =
                (UsuarioAutorizadoService) modelServiceFactory.resolve(UsuarioAutorizado.class);
        CentroUsuarioService centroUsuarioService =
                (CentroUsuarioService) modelServiceFactory.resolve(CentroUsuario.class);

        for (UsuarioAutorizado ua : usuarioAutorizadoService.findByCentroAndCodigoTipoUsuario(centro.getId(), tipoUsuarioCode)) {
            centroUsuarioService.findByCentroAndUsuarioDni(centro.getId(), ua.getDni()).ifPresent(cu -> {
                boolean tieneTipo = cu.getCentroUsuarioTipoUsuario().stream()
                        .anyMatch(cutu -> tipoUsuario.getId().equals(cutu.getTipoUsuario().getId()));
                if (!tieneTipo) {
                    centroUsuarioService.agregarTipo(cu, tipoUsuario);
                }
            });
        }
    }

    private List<String> parsearLineas() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(contenido), StandardCharsets.UTF_8))) {
            if (reader.readLine() == null) {
                throw new ImportadorException("El fichero CSV está vacío");
            }
            List<String> lineas = new ArrayList<>();
            String linea;
            while ((linea = reader.readLine()) != null) {
                linea = linea.trim();
                if (!linea.isEmpty()) lineas.add(linea);
            }
            return lineas;
        } catch (IOException e) {
            throw new ImportadorException("Error leyendo el fichero CSV: " + e.getMessage());
        }
    }

    private int[] procesarItems(List<String> lineas, List<MensajeImportacion> mensajes) {
        int creados = 0;
        int errores = 0;
        for (int i = 0; i < lineas.size(); i++) {
            String documentoRaw = lineas.get(i).split(",")[0].replace("\"", "").trim();
            try {
                ImportadorUsuarioUtil.procesarItem(documentoRaw, centro, tipoUsuario, true, modelServiceFactory);
                creados++;
            } catch (Exception e) {
                errores++;
                mensajes.add(new MensajeImportacion(i + 1, documentoRaw, e.getMessage()));
            }
        }
        return new int[]{creados, errores};
    }

}