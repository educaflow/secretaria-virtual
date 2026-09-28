package com.educaflow.subsystem.security.module;

import com.axelor.app.AxelorModule;
import com.axelor.auth.EducaFlowAuthResolverRegistry;
import com.educaflow.subsystem.security.EducaFlowAuthResolverImpl;
import com.educaflow.subsystem.security.service.MenuSecurityService;
import com.educaflow.subsystem.security.service.PerfilesUsuarioService;
import com.educaflow.subsystem.security.service.impl.MenuSecurityServiceImpl;
import com.educaflow.subsystem.security.service.impl.PerfilesUsuarioServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecurityModule extends AxelorModule {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    protected void configure() {
        log.info("Registrando EducaFlowAuthResolverImpl...");
        EducaFlowAuthResolverRegistry.register(new EducaFlowAuthResolverImpl());
        log.info("EducaFlowAuthResolverImpl registrado.");

        bind(PerfilesUsuarioService.class).to(PerfilesUsuarioServiceImpl.class);
        bind(MenuSecurityService.class).to(MenuSecurityServiceImpl.class);
    }
}