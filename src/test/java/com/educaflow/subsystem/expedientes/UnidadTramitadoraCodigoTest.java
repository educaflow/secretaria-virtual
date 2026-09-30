package com.educaflow.subsystem.expedientes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.educaflow.subsystem.expedientes.db.UnidadTramitadoraCodigo;
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
 * El enum {@link UnidadTramitadoraCodigo} y las filas de {@code UnidadesTramitadoras.xml} son dos listas de lo mismo:
 * cada fila de {@code UnidadTramitadora} guarda en {@code code} un valor del enum.
 */
class UnidadTramitadoraCodigoTest {

    private static final Path UNIDADES_TRAMITADORAS_XML =
            Path.of("src/main/java/com/educaflow/subsystem/expedientes/data-init/input/UnidadesTramitadoras.xml");

    @Test
    void losCodigosDeUnidadesTramitadorasXmlSonLosValoresDelEnum() throws Exception {
        Set<String> valoresEnum = Arrays.stream(UnidadTramitadoraCodigo.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertEquals(valoresEnum, codigosDeUnidadesTramitadorasXml());
    }

    private static Set<String> codigosDeUnidadesTramitadorasXml() throws Exception {
        NodeList filas = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(UNIDADES_TRAMITADORAS_XML.toFile())
                .getElementsByTagName("unidadtramitadora");
        Set<String> codigos = new HashSet<>();
        for (int i = 0; i < filas.getLength(); i++) {
            codigos.add(((Element) filas.item(i)).getAttribute("code"));
        }
        return codigos;
    }
}
