package com.educaflow.subsystem.importacion.service.tipoimportador.impl;

import com.axelor.db.JpaRepository;
import com.educaflow.base.util.DniUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.subsystem.importacion.service.tipoimportador.ImportadorException;
import com.educaflow.subsystem.importacion.service.tipoimportador.ImportadorFichero;
import com.educaflow.subsystem.importacion.service.tipoimportador.ResultadoImportacion;
import com.educaflow.subsystem.importacion.service.tipoimportador.ResultadoImportacion.MensajeImportacion;
import com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado;
import com.educaflow.subsystem.registrousuario.db.repo.UsuarioAutorizadoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ImportadorProfesoresExternos implements ImportadorFichero {

    private static final String TIPO_CODE = "PROFESOR_EXTERNO";

    private final UsuarioAutorizadoRepository usuarioAutorizadoRepository;

    private final Logger logger = LoggerFactory.getLogger(ImportadorProfesoresExternos.class);

    public ImportadorProfesoresExternos() {
        this.usuarioAutorizadoRepository = (UsuarioAutorizadoRepository) JpaRepository.of(UsuarioAutorizado.class);
    }

    @Override
    public ResultadoImportacion importar(byte[] contenido, Centro centro, Integer curso) {
        TipoUsuario tipoUsuario = obtenerTipoUsuario();
        int cursoActual = centro.getCurso();

        logger.info("Importando profesores externos para el centro '{}' y curso '{}'", centro.getName(), cursoActual);

        List<String> lineas = parsearLineas(contenido);
        List<MensajeImportacion> mensajes = new ArrayList<>();
        int[] contadores = procesarItems(lineas, centro, cursoActual, tipoUsuario, mensajes);

        String resumen = construirResumen(centro, cursoActual, contadores[0], contadores[1], mensajes.size(), lineas.size());
        return new ResultadoImportacion(resumen, mensajes);
    }

    private List<String> parsearLineas(byte[] contenido) {
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

    private int[] procesarItems(List<String> lineas, Centro centro, Integer curso,
                                 TipoUsuario tipoUsuario, List<MensajeImportacion> mensajes) {
        int creados = 0;
        int existentes = 0;
        for (int i = 0; i < lineas.size(); i++) {
            String documentoRaw = extraerDocumento(lineas.get(i));
            try {
                if (procesarItem(documentoRaw, centro, curso, tipoUsuario)) {
                    creados++;
                } else {
                    existentes++;
                }
            } catch (Exception e) {
                mensajes.add(new MensajeImportacion(i + 1, documentoRaw, e.getMessage()));
            }
        }
        return new int[]{creados, existentes};
    }

    private String extraerDocumento(String linea) {
        return linea.split(",")[0].replace("\"", "").trim();
    }

    private boolean procesarItem(String documentoRaw, Centro centro, Integer curso, TipoUsuario tipoUsuario) {
        String dni = DniUtil.clean(documentoRaw);
        if (!DniUtil.isValid(dni)) {
            throw new ImportadorException("DNI inválido: " + documentoRaw);
        }
        boolean existe = usuarioAutorizadoRepository.findByCentroAndCursoAndDocumento(centro, curso, dni)
                .stream().anyMatch(ua -> tipoUsuario.equals(ua.getTipoUsuario()));
        if (existe) {
            return false;
        }
        UsuarioAutorizado ua = new UsuarioAutorizado();
        ua.setCentro(centro);
        ua.setCurso(curso);
        ua.setDni(dni);
        ua.setTipoUsuario(tipoUsuario);
        usuarioAutorizadoRepository.save(ua);
        return true;
    }

    private String construirResumen(Centro centro, Integer curso, int creados, int existentes, int avisos, int total) {
        return "Importando profesores externos para el centro '" + centro.getName() +
                "' y curso '" + curso + "'\n" +
                "Nuevos: " + creados +
                " | Ya existían: " + existentes +
                " | Avisos: " + avisos +
                " | Total: " + total;
    }

    private TipoUsuario obtenerTipoUsuario() {
        TipoUsuario tipoUsuario = JpaRepository.of(TipoUsuario.class).all()
                .filter("self.code = :code")
                .bind("code", TIPO_CODE)
                .fetchOne();
        if (tipoUsuario == null) {
            throw new ImportadorException("TipoUsuario no encontrado: " + TIPO_CODE);
        }
        return tipoUsuario;
    }
}