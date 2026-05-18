package com.educaflow.subsystem.registrousuario.service;

import com.axelor.db.modelservice.ModelService;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado;

import java.util.Optional;

public interface UsuarioAutorizadoService extends ModelService<UsuarioAutorizado> {

    Optional<UsuarioAutorizado> findByCentroDniTipoUsuarioCurso(
            Centro centro, String dni, TipoUsuario tipoUsuario, Integer curso);
}
