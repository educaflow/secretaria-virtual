package com.educaflow.common.validation.rules

import com.educaflow.common.validation.engine.ValidationRule
import com.educaflow.common.validation.messages.BusinessMessages
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