package com.educaflow.tramites.util.verificacion

import com.educaflow.base.infrastructure.validation.rules.IfValueIn
import com.educaflow.base.infrastructure.validation.rules.MaxLength
import com.educaflow.base.infrastructure.validation.rules.MinLength
import com.educaflow.base.infrastructure.validation.rules.Required
import com.educaflow.subsystem.expedientes.db.Expediente
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VerificacionRulesTest {

    @Test
    fun resultadoVerificacion_esObligatorio() {
        val reglas = resultadoVerificacion(Expediente::getCodeState)

        assertEquals("codeState", reglas.getFieldName())
        // Required no es una data class: se comprueba por su tipo.
        assertEquals(1, reglas.validationRules.size)
        assertTrue(reglas.validationRules[0] is Required)
    }

    @Test
    fun textoSubsanacion_soloSeExigeCuandoElResultadoEsSubsanar() {
        val reglas = textoSubsanacion(Expediente::getNumeroExpediente, Expediente::getCodeState, "SUBSANAR")

        assertEquals("numeroExpediente", reglas.getFieldName())
        assertEquals(1, reglas.validationRules.size)
        val condicional = reglas.validationRules[0] as IfValueIn
        assertEquals(Expediente::getCodeState, condicional.dependentField)
        assertEquals(listOf<Any>("SUBSANAR"), condicional.dependentValues)
        assertEquals(3, condicional.validationRules.size)
        assertTrue(condicional.validationRules[0] is Required)
        assertEquals(listOf(MinLength(10), MaxLength(1000)), condicional.validationRules.drop(1))
    }
}
