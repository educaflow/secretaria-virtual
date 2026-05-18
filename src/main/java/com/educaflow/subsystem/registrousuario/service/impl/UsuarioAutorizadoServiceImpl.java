package com.educaflow.subsystem.registrousuario.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado;
import com.educaflow.subsystem.registrousuario.db.repo.AbstractUsuarioAutorizadoRepository;
import com.educaflow.subsystem.registrousuario.service.UsuarioAutorizadoService;

import java.util.Optional;

public class UsuarioAutorizadoServiceImpl
        extends DefaultModelService<UsuarioAutorizado>
        implements UsuarioAutorizadoService {

    public UsuarioAutorizadoServiceImpl(Class<UsuarioAutorizado> model,
                                        Repository<UsuarioAutorizado> repository) {
        super(model, repository);
    }

    @Override
    public Optional<BusinessMessages> validateInsert(UsuarioAutorizado entidad) {
        return findByCentroDniTipoUsuarioCurso(
                entidad.getCentro(), entidad.getDni(),
                entidad.getTipoUsuario(), entidad.getCurso())
            .map(existente -> {
                var msgs = new BusinessMessages();
                msgs.add(new BusinessMessage(String.format(
                    "Ya existe un usuario autorizado con DNI '%s' para el centro '%s', tipo '%s' y curso %d.",
                    entidad.getDni(),
                    entidad.getCentro().getName(),
                    entidad.getTipoUsuario().getNombre(),
                    entidad.getCurso()
                )));
                return msgs;
            });
    }

    @Override
    public Optional<UsuarioAutorizado> findByCentroDniTipoUsuarioCurso(
            Centro centro, String dni, TipoUsuario tipoUsuario, Integer curso) {
        return Optional.ofNullable(
            ((AbstractUsuarioAutorizadoRepository) repository)
                .findByCentroAndDniAndTipoUsuarioAndCurso(centro, dni, tipoUsuario, curso)
        );
    }
}
