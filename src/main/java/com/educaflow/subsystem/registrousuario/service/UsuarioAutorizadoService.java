package com.educaflow.subsystem.registrousuario.service;

import com.axelor.db.modelservice.ModelService;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado;

import java.util.List;

public interface UsuarioAutorizadoService extends ModelService<UsuarioAutorizado> {

    boolean isAuthorized(String dni);

    List<UsuarioAutorizado> getByCentro(Centro centro);

    List<TipoUsuario> getByCentroAndDni(Centro centro, String dni);

    void marcarTodosInactivos(Centro centro, TipoUsuario tipoUsuario);

    void activarOInsertar(String dni, Centro centro, TipoUsuario tipoUsuario);

    void insertarInactivoSiNoExiste(String dni, Centro centro, TipoUsuario tipoUsuario);
}