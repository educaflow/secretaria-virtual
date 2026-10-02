package com.educaflow.tramites.util.verificacion

import com.educaflow.base.infrastructure.validation.dsl.FieldValidationRulesBuilder
import com.educaflow.base.infrastructure.validation.dsl.ifValueIn
import com.educaflow.base.infrastructure.validation.engine.FieldValidationRules
import com.educaflow.base.infrastructure.validation.rules.MaxLength
import com.educaflow.base.infrastructure.validation.rules.MinLength
import com.educaflow.base.infrastructure.validation.rules.Required
import kotlin.reflect.KFunction

/*
 * Reglas del DSL de validación de la fase VERIFICACION que son iguales en todos los tipos de expediente.
 *
 * Uso en un `StateEventValidatorImpl`:
 *
 *     return rules {
 *         +resultadoVerificacion(model::getResultadoVerificacion)
 *         +textoSubsanacion(model::getTextoSubsanacion, model::getResultadoVerificacion, ResultadoVerificacion.SUBSANAR)
 *     }
 */

/** El verificador tiene que decir si la solicitud está correcta o hay que subsanarla. */
fun resultadoVerificacion(resultadoVerificacion: KFunction<*>): FieldValidationRules {
    val builder = FieldValidationRulesBuilder(resultadoVerificacion)
    with(builder) {
        +Required()
    }
    return builder.build()
}

/**
 * Qué hay que subsanar: obligatorio solo cuando el resultado de la verificación es [subsanar], que es el valor
 * del enum del tipo de expediente que pide la subsanación.
 */
fun textoSubsanacion(textoSubsanacion: KFunction<*>, resultadoVerificacion: KFunction<*>, subsanar: Any): FieldValidationRules {
    val builder = FieldValidationRulesBuilder(textoSubsanacion)
    with(builder) {
        +ifValueIn(resultadoVerificacion, listOf(subsanar)) {
            +Required()
            +MinLength(10)
            +MaxLength(1000)
        }
    }
    return builder.build()
}
