package com.educaflow.base.infrastructure.evaluator.impl;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvaluatorImplGroovyTest {

    private final EvaluatorImplGroovy evaluator = new EvaluatorImplGroovy();
    private final Map<String, Object> contexto = Map.of("self", Map.of("nombre", "Ana", "edad", 30));

    @Test
    void evaluateDevuelveLosValoresYSeCallaLoQueFalla() {
        Map<String, Object> resultados = evaluator.evaluate(List.of("self.nombre", "self.noExiste.x", "self.edad + 1"), contexto);

        assertEquals("Ana", resultados.get("self.nombre"));
        assertEquals(31, resultados.get("self.edad + 1"));
        assertFalse(resultados.containsKey("self.noExiste.x"));
    }

    @Test
    void evaluateStrictDevuelveLosValoresCuandoTodoEvalua() {
        Map<String, Object> resultados = evaluator.evaluateStrict(List.of("self.nombre", "self.edad > 18", "null"), contexto);

        assertEquals("Ana", resultados.get("self.nombre"));
        assertEquals(true, resultados.get("self.edad > 18"));
        assertTrue(resultados.containsKey("null"));
        assertEquals(null, resultados.get("null"));
    }

    @Test
    void evaluateStrictAbortaEnLaPrimeraQueFallaDiciendoCual() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> evaluator.evaluateStrict(List.of("self.nombre", "self.noExiste.x"), contexto));

        assertTrue(ex.getMessage().contains("self.noExiste.x"), ex.getMessage());
    }

    @Test
    void lasExpresionesEnBlancoSeSaltanEnLosDosModos() {
        assertTrue(evaluator.evaluate(List.of("", "  "), contexto).isEmpty());
        assertTrue(evaluator.evaluateStrict(List.of("", "  "), contexto).isEmpty());
    }
}
