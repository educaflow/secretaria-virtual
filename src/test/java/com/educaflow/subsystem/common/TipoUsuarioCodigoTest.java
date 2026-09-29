package com.educaflow.subsystem.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.educaflow.subsystem.common.db.TipoUsuarioCodigo;
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
 * El enum {@link TipoUsuarioCodigo} y las filas de {@code tiposUsuario.xml} son dos listas de lo mismo:
 * cada fila de {@code TipoUsuario} guarda en {@code codigo} un valor del enum.
 */
class TipoUsuarioCodigoTest {

    private static final Path TIPOS_USUARIO_XML =
            Path.of("src/main/java/com/educaflow/subsystem/common/data-init/input/tiposUsuario.xml");

    @Test
    void losCodigosDeTiposUsuarioXmlSonLosValoresDelEnum() throws Exception {
        Set<String> valoresEnum = Arrays.stream(TipoUsuarioCodigo.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertEquals(valoresEnum, codigosDeTiposUsuarioXml());
    }

    private static Set<String> codigosDeTiposUsuarioXml() throws Exception {
        NodeList filas = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(TIPOS_USUARIO_XML.toFile())
                .getElementsByTagName("tipoUsuario");
        Set<String> codigos = new HashSet<>();
        for (int i = 0; i < filas.getLength(); i++) {
            codigos.add(((Element) filas.item(i)).getAttribute("codigo"));
        }
        return codigos;
    }
}
