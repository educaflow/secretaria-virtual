package com.educaflow.subsystem.registrousuario.service.impl;

import com.educaflow.subsystem.registrousuario.db.repo.UsuarioAutorizadoRepository;
import com.educaflow.subsystem.registrousuario.service.UsuarioAutorizadoService;
import jakarta.inject.Inject;

public class UsuarioAutorizadoServiceImpl implements UsuarioAutorizadoService {

    @Inject
    private UsuarioAutorizadoRepository repository;

    @Override
    public boolean isAuthorized(String dni) {
        return repository.isAuthorized(dni);
    }
}
