package com.educaflow.tramites.util.entrada

import com.educaflow.base.infrastructure.validation.rules.FileMaxSize
import com.educaflow.base.infrastructure.validation.rules.FileType
import com.educaflow.base.infrastructure.validation.rules.Required
import com.educaflow.base.infrastructure.validation.rules.SizeUnit
import com.educaflow.subsystem.expedientes.db.PruebaV1
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EntradaRulesTest {

    @Test
    fun solicitudEscaneada_exigeUnPdfDeComoMucho10MbEnElCampoIndicado() {
        val reglas = solicitudEscaneada(PruebaV1::getJustificante)

        assertEquals("justificante", reglas.getFieldName())
        // Required no es una data class: se comprueba por su tipo, y el resto de reglas por su valor.
        assertEquals(3, reglas.validationRules.size)
        assertTrue(reglas.validationRules[0] is Required)
        assertEquals(listOf(FileType(listOf("application/pdf")), FileMaxSize(10, SizeUnit.MB)), reglas.validationRules.drop(1))
    }
}
