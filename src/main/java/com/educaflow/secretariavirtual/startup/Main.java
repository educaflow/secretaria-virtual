package com.educaflow.secretariavirtual.startup;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.app.AxelorModule;
import com.educaflow.base.infrastructure.criptografia.EntornoCriptografico;
import com.educaflow.base.infrastructure.criptografia.config.AlmacenCertificadosConfiablesConfig;
import com.educaflow.base.infrastructure.criptografia.config.EntornoCriptograficoConfig;
import com.educaflow.base.infrastructure.criptografia.config.DispositivoCriptograficoConfig;
import com.educaflow.base.infrastructure.db.BulkTables;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Main extends AxelorModule {

    protected void configure() {

        configureEntornoCriptografico();
        configureDatabase();

    }



    private void configureEntornoCriptografico() {
        EntornoCriptograficoConfigProvider entornoCriptograficoConfigProvider=new EntornoCriptograficoConfigProvider();
        EntornoCriptograficoConfig entornoCriptograficoConfig = entornoCriptograficoConfigProvider.getEntornoCriptograficoConfigFromAppSettings();
        EntornoCriptografico.configure(entornoCriptograficoConfig);
    }

    private void configureDatabase() {
        String dataBaseDriver = AppSettings.get().get(AvailableAppSettings.DB_DEFAULT_DRIVER);
        String dataBaseURL = AppSettings.get().get(AvailableAppSettings.DB_DEFAULT_URL);
        String dataBaseUser = AppSettings.get().get(AvailableAppSettings.DB_DEFAULT_USER);
        String dataBasePassword = AppSettings.get().get(AvailableAppSettings.DB_DEFAULT_PASSWORD);
        String schemaName = "public";



        TruncateTables truncateTables = new TruncateTables();
        truncateTables.truncateTables(dataBaseDriver,dataBaseURL,dataBaseUser,dataBasePassword,schemaName);

        DataBaseMigrate dataBaseMigrate = new DataBaseMigrate();
        dataBaseMigrate.migrate(dataBaseDriver,dataBaseURL,dataBaseUser,dataBasePassword,schemaName);
    }


}
