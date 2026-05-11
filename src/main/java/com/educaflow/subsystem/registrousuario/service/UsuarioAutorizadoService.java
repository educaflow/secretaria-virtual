package com.educaflow.subsystem.registrousuario.service;

import com.axelor.db.modelservice.ModelService;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado;

import java.util.List;
import java.util.Optional;

public interface UsuarioAutorizadoService extends ModelService<UsuarioAutorizado> {


    void cambiarTipoParaCentro(Centro centro, TipoUsuario tipoUsuarioAnterior, TipoUsuario tipoUsuarioNuevo);

    List<UsuarioAutorizado> getByCentro(Centro centro);

    void marcarTodosInactivos(Centro centro, TipoUsuario tipoUsuario);

    Optional<UsuarioAutorizado> findByCentroAndDniAndTipoUsuario(Centro centro, String dni, TipoUsuario tipoUsuario);

    List<UsuarioAutorizado> findByCentroAndCodigoTipoUsuario(Long centroId, String codigoTipoUsuario);

    List<UsuarioAutorizado> findActivosByCentroAndCodigo(Long centroId, String codigoTipoUsuario);
}