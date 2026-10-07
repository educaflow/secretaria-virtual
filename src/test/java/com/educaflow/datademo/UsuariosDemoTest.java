package com.educaflow.datademo;

import com.educaflow.base.util.DniUtil;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Los DNI de los usuarios de demo son DNI de demo: generados con data-demo/generar_dni_demo.sh.
 */
class UsuariosDemoTest {

    @Test
    void documentos_empiezanPor9_sonDistintos_yPasanDniUtilIsValid() {
        List<String> documentos = UsuariosDemo.getUsuariosDemo().stream().map(UsuariosDemo.UsuarioDemo::documento).toList();

        assertFalse(documentos.isEmpty());
        for (String documento : documentos) {
            assertTrue(documento.startsWith("9"), "El DNI de demo no empieza por 9: " + documento);
            assertTrue(DniUtil.isValid(documento), "El DNI de demo no es válido: " + documento);
        }
        assertEquals(documentos.size(), new HashSet<>(documentos).size(), "Hay DNI de demo repetidos: " + documentos);
    }
}
