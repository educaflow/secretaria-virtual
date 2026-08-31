package com.educaflow.subsystem.security.module;

import com.axelor.app.AxelorModule;
import com.axelor.auth.EduFlowAuthResolverRegistry;
import com.educaflow.subsystem.security.EducaFlowAuthResolverImpl;
import com.educaflow.subsystem.security.service.PerfilesUsuarioService;
import com.educaflow.subsystem.security.service.impl.PerfilesUsuarioServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecurityModule extends AxelorModule {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    protected void configure() {
        log.info("Registrando EducaFlowAuthResolverImpl...");
        EduFlowAuthResolverRegistry.register(new EducaFlowAuthResolverImpl());
        log.info("EducaFlowAuthResolverImpl registrado.");

        //PerfilesUsuarioService no es un ModelService, así que ModelServiceFactory no lo descubre:
        //necesita binding explícito.
        bind(PerfilesUsuarioService.class).to(PerfilesUsuarioServiceImpl.class);
    }
}