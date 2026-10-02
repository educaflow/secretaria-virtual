package com.educaflow.tramites.util.entrada

import com.educaflow.base.infrastructure.validation.dsl.FieldValidationRulesBuilder
import com.educaflow.base.infrastructure.validation.engine.FieldValidationRules
import com.educaflow.base.infrastructure.validation.rules.FileMaxSize
import com.educaflow.base.infrastructure.validation.rules.FileType
import com.educaflow.base.infrastructure.validation.rules.Required
import com.educaflow.base.infrastructure.validation.rules.SizeUnit
import kotlin.reflect.KFunction

/*
 * Reglas del DSL de validación de la fase ENTRADA que son iguales en todos los tipos de expediente.
 *
 * Uso en un `StateEventValidatorImpl`:
 *
 *     return rules {
 *         +solicitudEscaneada(model::getPdfSolicitudFirmada)
 *     }
 */

/**
 * La solicitud entregada en papel, escaneada: el registro de entrada solo admite un PDF como documento de la
 * solicitud. [pdfSolicitudFirmada] es el getter del campo en el que se adjunta.
 */
fun solicitudEscaneada(pdfSolicitudFirmada: KFunction<*>): FieldValidationRules {
    val builder = FieldValidationRulesBuilder(pdfSolicitudFirmada)
    with(builder) {
        +Required()
        +FileType(listOf("application/pdf"))
        +FileMaxSize(10, SizeUnit.MB)
    }
    return builder.build()
}
