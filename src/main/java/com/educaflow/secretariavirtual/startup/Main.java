package com.educaflow.secretariavirtual.startup;

import com.axelor.app.AppSettings;
import com.axelor.app.AxelorModule;
import com.educaflow.base.infrastructure.criptografia.EntornoCriptografico;
import com.educaflow.base.infrastructure.criptografia.config.AlmacenCertificadosConfiablesConfig;
import com.educaflow.base.infrastructure.mail.MailSender;
import com.educaflow.base.infrastructure.mail.impl.MailSenderImpl;
import com.educaflow.base.infrastructure.mail.impl.SmtpCredentialSimplePassword;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends AxelorModule {

    private final Logger logger = LoggerFactory.getLogger(Main.class);

    @Override
    protected void configure() {
        logger.info("Iniciando Aplicación Secretaria Virtual...");

        configureEntornoCriptografico();
        configureDatabase();
    }



    private void configureEntornoCriptografico() {
        AlmacenCertificadosConfiablesConfig almacenConfig = new AlmacenCertificadosConfiablesProvider().getAlmacenCertificadosConfiablesConfig();
        EntornoCriptografico.configureAlmacenCertificadosConfiables(almacenConfig);
        EntornoCriptografico.configureDispositivosCriptograficos(null);
    }

    private void configureDatabase() {
        TruncateTables truncateTables = new TruncateTables();
        truncateTables.truncateTables();

        bind(DataBaseMigrate.class);
    }

    @Provides
    @Singleton
    public MailSender provideMailSender() {
        String host = AppSettings.get().get("mail.smtp.host");
        String user = AppSettings.get().get("mail.smtp.user");
        String pass = AppSettings.get().get("mail.smtp.password");

        SmtpCredentialSimplePassword smtpCredentialSimplePassword = new SmtpCredentialSimplePassword(host, user, pass);

        return new MailSenderImpl(smtpCredentialSimplePassword);
    }

}
