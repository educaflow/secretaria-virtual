package com.educaflow.base.infrastructure.validation.rules

import com.educaflow.base.infrastructure.validation.engine.ValidationRule
import com.axelor.db.modelservice.BusinessMessages
import kotlin.reflect.KFunction

data class IfValueIn(val dependentField:KFunction<*>, val dependentValues: List<Any>, val validationRules: List<ValidationRule>) : ValidationRule {

    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        val currentDependentValue=dependentField.call(bean);

        if (currentDependentValue !in dependentValues) {
            return null
        }

        val messages= BusinessMessages()
        for (validationRule in validationRules) {
            val innerMessages = validationRule.validate(value, bean)
            if ((innerMessages!= null) && (innerMessages.isNotEmpty())) {
                messages.addAll(innerMessages)
            }
        }
        return messages
    }
}

data class IfValueNotIn(val dependentField:KFunction<*>, val dependentValues: List<Any>, val validationRules: List<ValidationRule>) : ValidationRule {

    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        val currentDependentValue=dependentField.call(bean);

        if (currentDependentValue in dependentValues) {
            return null
        }

        val messages=BusinessMessages()
        for (validationRule in validationRules) {
            val innerMessages = validationRule.validate(value, bean)
            if ((innerMessages!= null) && (innerMessages.isNotEmpty())) {
                messages.addAll(innerMessages)
            }
        }
        return messages
    }
}
/**
 * Aplica las reglas anidadas solo si [condicion] devuelve `true` para el bean entero; si devuelve
 * `false`, el campo se da por válido sin evaluarlas. Es la versión de [IfValueIn] para condiciones que
 * no se reducen a «este otro campo vale X»: la condición se le pasa como una función estática del tipo.
 *
 * Se construye con `ifLambda(...) { }` del DSL:
 *
 *     field(model::getMotivoRechazo) {
 *         +ifLambda(MiTramiteV1Util::esRechazo) {
 *             +Required()
 *             +MaxLength(500)
 *         }
 *     }
 */
data class IfLambda<T : Any>(val condicion: (T) -> Boolean, val validationRules: List<ValidationRule>) : ValidationRule {

    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        @Suppress("UNCHECKED_CAST")
        if (condicion(bean as T) == false) {
            return null
        }

        val messages = BusinessMessages()
        for (validationRule in validationRules) {
            val innerMessages = validationRule.validate(value, bean)
            if ((innerMessages != null) && (innerMessages.isNotEmpty())) {
                messages.addAll(innerMessages)
            }
        }
        return messages
    }
}
