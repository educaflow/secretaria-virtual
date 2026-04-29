package com.educaflow.subsystem.importacion.service.tipoimportador.impl;

import com.axelor.db.JpaRepository;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessage;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessages;
import com.educaflow.base.util.DniUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.subsystem.importacion.service.tipoimportador.ImportadorException;
import com.educaflow.subsystem.importacion.service.tipoimportador.ImportadorTipoFichero;
import com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado;
import com.educaflow.subsystem.registrousuario.db.repo.UsuarioAutorizadoRepository;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ImportadorProfesoresExternos implements ImportadorTipoFichero {

    private static final String TIPO_CODE = "PROFESOR_EXTERNO";

    @Override
    public List<String> importar(byte[] contenido, Centro centro, Integer curso) {
        TipoUsuario tipoUsuario = obtenerTipoUsuario();
        UsuarioAutorizadoRepository repo = (UsuarioAutorizadoRepository) JpaRepository.of(UsuarioAutorizado.class);

        int creados = 0;
        int existentes = 0;
        List<String> errores = new ArrayList<>();
        List<String> log = new ArrayList<>();
        int fila = 0;
        int total = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(contenido), StandardCharsets.UTF_8))) {

            String line = reader.readLine(); // cabecera
            if (line == null) {
                throw new ImportadorException("El fichero CSV está vacío");
            }

            while ((line = reader.readLine()) != null) {
                fila++;
                line = line.trim();
                if (line.isEmpty()) continue;

                total++;
                String documentoRaw = line.split(",")[0].replace("\"", "").trim();
                String dni = DniUtil.clean(documentoRaw);
                if (!DniUtil.isValid(dni)) {
                    errores.add("DNI inválido: " + documentoRaw);
                }

                boolean existe = repo.all()
                        .filter("self.centro = :centro AND self.curso = :curso AND self.dni = :dni AND self.tipoUsuario = :tipoUsuario")
                        .bind("centro", centro)
                        .bind("curso", curso)
                        .bind("dni", dni)
                        .bind("tipoUsuario", tipoUsuario)
                        .count() > 0;

                if (existe) {
                    existentes++;
                } else {
                    UsuarioAutorizado ua = new UsuarioAutorizado();
                    ua.setCentro(centro);
                    ua.setCurso(curso);
                    ua.setDni(dni);
                    ua.setTipoUsuario(tipoUsuario);
                    repo.save(ua);
                    creados++;
                }
            }
        } catch (IOException e) {
            throw new ImportadorException("Error leyendo el fichero CSV: " + e.getMessage());
        }

        log.add((String.format(
                "Nuevos: %d | Ya existían: %d | Errores: %d | Total: %d",
                creados, existentes, errores.size(), total)));
        errores.forEach(msg -> log.add(msg));
        return log;
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