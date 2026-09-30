package com.educaflow.subsystem.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.educaflow.subsystem.common.db.CargoCodigo;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * El enum {@link CargoCodigo} y las filas de {@code cargos.xml} son dos listas de lo mismo:
 * cada fila de {@code Cargo} guarda en {@code code} un valor del enum.
 */
class CargoCodigoTest {

    private static final Path CARGOS_XML =
            Path.of("src/main/java/com/educaflow/subsystem/common/data-init/input/cargos.xml");

    @Test
    void losCodigosDeCargosXmlSonLosValoresDelEnum() throws Exception {
        Set<String> valoresEnum = Arrays.stream(CargoCodigo.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertEquals(valoresEnum, codigosDeCargosXml());
    }

    private static Set<String> codigosDeCargosXml() throws Exception {
        NodeList filas = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(CARGOS_XML.toFile())
                .getElementsByTagName("cargo");
        Set<String> codigos = new HashSet<>();
        for (int i = 0; i < filas.getLength(); i++) {
            codigos.add(((Element) filas.item(i)).getAttribute("code"));
        }
        return codigos;
    }
}
