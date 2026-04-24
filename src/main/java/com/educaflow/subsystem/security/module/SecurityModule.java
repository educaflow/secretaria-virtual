package com.educaflow.subsystem.security.module;

import com.axelor.app.AxelorModule;
import com.axelor.auth.EduFlowAuthResolverRegistry;
import com.educaflow.subsystem.security.EducaFlowAuthResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecurityModule extends AxelorModule {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    protected void configure() {
        log.info("Registrando EducaFlowAuthResolver...");
        EduFlowAuthResolverRegistry.register(new EducaFlowAuthResolver());
        log.info("EducaFlowAuthResolver registrado.");
    }
}