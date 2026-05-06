package com.educaflow.secretariavirtual.startup;

import com.axelor.app.AppSettings;
import com.educaflow.base.infrastructure.criptografia.config.AlmacenCertificadosConfiablesConfig;
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

public class AlmacenCertificadosConfiablesProvider {

    public AlmacenCertificadosConfiablesConfig getAlmacenCertificadosConfiablesConfig() {
        String pathAlmacen = AppSettings.get().get("entornoCriptografico.almacenCertificadosConfiables.path");
        String passwordAlmacen = AppSettings.get().get("entornoCriptografico.almacenCertificadosConfiables.password");
        InputStream inputStreamAlmacen = Main.class.getClassLoader().getResourceAsStream(pathAlmacen);

        String pathListaCrls = AppSettings.get().get("entornoCriptografico.almacenCertificadosConfiables.pathListaCRLs");
        List<InputStream> certificateRevocationListsInputStream = getCrlsInputStream(Path.of(pathListaCrls));

        return new AlmacenCertificadosConfiablesConfig(inputStreamAlmacen, passwordAlmacen, certificateRevocationListsInputStream);
    }

    private List<InputStream> getCrlsInputStream(Path pathListaCrls) {
        try {
            InputStream inputStreamListaCrls = AlmacenCertificadosConfiablesProvider.class.getClassLoader().getResourceAsStream(pathListaCrls.toString());
            if (inputStreamListaCrls == null) {
                throw new RuntimeException("No se encuentra el fichero con la lista de CRLs: " + pathListaCrls);
            }
            Path parent = pathListaCrls.getParent();

            List<InputStream> crlsInputStream = new ArrayList<>();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(inputStreamListaCrls);

            XPath xPath = XPathFactory.newInstance().newXPath();
            NodeList nodes = (NodeList) xPath.evaluate("/crls/crl", doc, XPathConstants.NODESET);

            for (int i = 0; i < nodes.getLength(); i++) {
                String crlFileName = nodes.item(i).getTextContent().trim();
                String completeCrlFileName = parent.resolve(crlFileName).toString();
                InputStream crlInputStream = AlmacenCertificadosConfiablesProvider.class.getClassLoader().getResourceAsStream(completeCrlFileName);
                if (crlInputStream == null) {
                    throw new RuntimeException("No se encuentra el fichero CRL: " + completeCrlFileName);
                }
                crlsInputStream.add(crlInputStream);
            }

            return crlsInputStream;
        } catch (Exception ex) {
            throw new RuntimeException("Error leyendo la lista de ficheros CRL", ex);
        }
    }
}