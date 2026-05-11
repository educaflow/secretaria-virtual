package com.educaflow.subsystem.importacion.util;

import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.inject.Beans;
import com.educaflow.base.util.DniUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.subsystem.importacion.util.impl.ImportadorUsuarioXml;
import com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado;
import com.educaflow.subsystem.registrousuario.service.UsuarioAutorizadoService;

import java.util.List;
import java.util.Map;

public class ImportadorUsuarioUtil {

    private ImportadorUsuarioUtil() {}

    public static void procesarDocumentos(List<String> documentos, Centro centro, TipoUsuario tipoUsuario, TipoUsuario tipoUsuarioEx) {
        ModelServiceFactory modelServiceFactory = Beans.get(ModelServiceFactory.class);
        UsuarioAutorizadoService usuarioAutorizadoService = (UsuarioAutorizadoService) modelServiceFactory.resolve(UsuarioAutorizado.class);

        for (String documento: documentos) {
            if (!DniUtil.isValid(documento)) {
                throw new ImportadorException("DNI inválido: " + documento);
            }
            usuarioAutorizadoService.findByCentroAndDniAndTipoUsuario(centro, documento, tipoUsuarioEx).ifPresentOrElse(
                    ua -> {
                        ua.setTipoUsuario(tipoUsuario); usuarioAutorizadoService.update(ua, null);
                    },
                    () -> {
                        UsuarioAutorizado ua = new UsuarioAutorizado();
                        ua.setCentro(centro);
                        ua.setDni(documento);
                        ua.setTipoUsuario(tipoUsuario);
                        usuarioAutorizadoService.insert(ua);
                    }
            );
        }
    }

    public static void procesarItem(String documentoRaw, Centro centro, TipoUsuario tipoUsuario,
                                    boolean esActivo, ModelServiceFactory modelServiceFactory) {
        UsuarioAutorizadoService service =
                (UsuarioAutorizadoService) modelServiceFactory.resolve(UsuarioAutorizado.class);
        String dni = DniUtil.clean(documentoRaw);
        if (!DniUtil.isValid(dni)) {
            throw new ImportadorException("DNI inválido: " + documentoRaw);
        }
        service.findByCentroAndDniAndTipoUsuario(centro, dni, tipoUsuario).ifPresentOrElse(
                ua -> {
                    if (esActivo) { ua.setActivo(true); service.update(ua, null); }
                },
                () -> {
                    UsuarioAutorizado ua = new UsuarioAutorizado();
                    ua.setCentro(centro);
                    ua.setDni(dni);
                    ua.setTipoUsuario(tipoUsuario);
                    ua.setActivo(esActivo);
                    service.insert(ua);
                }
        );
    }

    public static String construirResumen(TipoUsuario tipoUsuario, Centro centro, Integer curso,
                                          int creados, int errores, int avisos, int total) {
        return "Importando usuarios del tipo '" + tipoUsuario.getNombre() +
                "' para el centro '" + centro.getName() +
                "' y curso '" + curso + "'\n" +
                "Importados: " + creados +
                " | Errores: " + errores +
                " | Avisos: " + avisos +
                " | Total: " + total;
    }
}