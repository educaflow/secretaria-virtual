package com.educaflow.base.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AsciiTableUtilTest {

    private static List<String> lines(String rendered) {
        return Arrays.asList(rendered.split("\\R"));
    }

    private static List<List<Object>> mutableRows(List<?>... rows) {
        List<List<Object>> result = new ArrayList<>();
        for (List<?> row : rows) {
            result.add(new ArrayList<>(row));
        }
        return result;
    }

    @Test
    void renderTable_conCabecerasYFilas_pintaTituloCabecerasYFilas() {
        List<List<Object>> rows = mutableRows(List.of("a1", "b1"), List.of("a2", "b2"));

        String rendered = AsciiTableUtil.renderTable("Tabla", List.of("ColA", "ColB"), rows);

        List<String> lines = lines(rendered);
        // regla, título, regla, cabeceras, regla, fila1, fila2, regla
        assertEquals(8, lines.size(), rendered);
        assertTrue(lines.get(1).contains("Tabla"));
        assertTrue(lines.get(3).contains("ColA"));
        assertTrue(lines.get(3).contains("ColB"));
        assertTrue(lines.get(5).contains("a1"));
        assertTrue(lines.get(5).contains("b1"));
        assertTrue(lines.get(6).contains("a2"));
        assertTrue(lines.get(6).contains("b2"));
        assertFalse(rendered.contains("__null__"));
    }

    @Test
    void renderTable_conValoresNulos_losSustituyePorMarcaYMutaLasFilas() {
        List<List<Object>> rows = mutableRows(Arrays.asList("a1", null));

        String rendered = AsciiTableUtil.renderTable("Tabla", List.of("ColA", "ColB"), rows);

        assertTrue(rendered.contains("__null__"));
        // comportamiento actual: modifica la lista de entrada
        assertEquals("__null__", rows.get(0).get(1));
        assertEquals("a1", rows.get(0).get(0));
    }

    @Test
    void renderTable_sinFilas_noPintaLaReglaFinalDeFilas() {
        String rendered = AsciiTableUtil.renderTable("Tabla", List.of("ColA", "ColB"), new ArrayList<>());

        List<String> lines = lines(rendered);
        // regla, título, regla, cabeceras, regla
        assertEquals(5, lines.size(), rendered);
        assertTrue(lines.get(1).contains("Tabla"));
        assertTrue(lines.get(3).contains("ColA"));
    }

    @Test
    void renderTable_conCabecerasNulas_pintaSoloTituloYFilas() {
        List<List<Object>> rows = mutableRows(List.of("unico"));

        String rendered = AsciiTableUtil.renderTable("Tabla", null, rows);

        List<String> lines = lines(rendered);
        // regla, título, regla, fila, regla
        assertEquals(5, lines.size(), rendered);
        assertTrue(lines.get(1).contains("Tabla"));
        assertTrue(lines.get(3).contains("unico"));
    }

    @Test
    void renderTable_conCabecerasVacias_pintaSoloTituloYFilas() {
        List<List<Object>> rows = mutableRows(List.of("unico"));

        String rendered = AsciiTableUtil.renderTable("Tabla", List.of(), rows);

        List<String> lines = lines(rendered);
        assertEquals(5, lines.size(), rendered);
        assertTrue(lines.get(1).contains("Tabla"));
        assertTrue(lines.get(3).contains("unico"));
    }

    @Test
    void renderTable_conFilasDeDistintoAnchoQueCabeceras_lanzaExcepcionDeAsciiTable() {
        List<List<Object>> rows = mutableRows(List.of("solo una"));

        assertThrows(RuntimeException.class,
                () -> AsciiTableUtil.renderTable("Tabla", List.of("ColA", "ColB"), rows));
    }

    @Test
    void renderTable_conExcepcion_pintaMensajeYTrazaConCausa() {
        Exception ex = new RuntimeException("fallo externo", new IllegalStateException("fallo interno"));

        String rendered = AsciiTableUtil.renderTable("Errores", ex);

        assertTrue(rendered.contains("Errores"));
        assertTrue(rendered.contains("Error"));
        assertTrue(rendered.contains("fallo externo"));
        assertTrue(rendered.contains("Caused by:fallo interno"));
        assertTrue(rendered.contains("AsciiTableUtilTest"));
    }
}
