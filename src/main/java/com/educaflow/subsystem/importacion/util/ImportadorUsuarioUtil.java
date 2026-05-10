package com.educaflow.subsystem.importacion.util;

import com.axelor.db.modelservice.ModelServiceFactory;
import com.educaflow.base.util.DniUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado;
import com.educaflow.subsystem.registrousuario.service.UsuarioAutorizadoService;

public class ImportadorUsuarioUtil {

    private ImportadorUsuarioUtil() {}

    public static void procesarItem(String documentoRaw, Centro centro, TipoUsuario tipoUsuario,
                                    boolean esActivo, ModelServiceFactory modelServiceFactory) {
        UsuarioAutorizadoService usuarioAutorizadoService =
                (UsuarioAutorizadoService) modelServiceFactory.resolve(UsuarioAutorizado.class);
        String dni = DniUtil.clean(documentoRaw);
        if (!DniUtil.isValid(dni)) {
            throw new ImportadorException("DNI inválido: " + documentoRaw);
        }
        if (esActivo) {
            usuarioAutorizadoService.activarOInsertar(dni, centro, tipoUsuario);
        } else {
            usuarioAutorizadoService.insertarInactivoSiNoExiste(dni, centro, tipoUsuario);
        }
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