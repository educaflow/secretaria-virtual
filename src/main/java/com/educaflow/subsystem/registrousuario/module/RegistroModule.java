package com.educaflow.subsystem.registrousuario.module;

import com.axelor.app.AxelorModule;
import com.educaflow.subsystem.registrousuario.service.RegistroPendienteService;
import com.educaflow.subsystem.registrousuario.service.UsuarioAutorizadoService;
import com.educaflow.subsystem.registrousuario.service.impl.RegistroPendienteServiceImpl;
import com.educaflow.subsystem.registrousuario.service.impl.UsuarioAutorizadoServiceImpl;

public class RegistroModule extends AxelorModule {

    @Override
    protected void configure() {
        /*bind(RegistroPendienteService.class).to(RegistroPendienteServiceImpl.class);
        bind(UsuarioAutorizadoService.class).to(UsuarioAutorizadoServiceImpl.class);*/
        /*bind(RegistroPendienteRepository.class);
        bind(UsuarioAutorizadoRepository.class);*/
    }
}