package com.educaflow.common.validation.rules

import com.educaflow.common.validation.engine.ValidationRule
import com.educaflow.common.validation.messages.BusinessMessages

data class MinValue(val min: Int) : ValidationRule {

    override fun validate(value: Any?,bean: Any): BusinessMessages? {
        if (value is Int) {
            return if (value < min) BusinessMessages.single("Debe tener como mínimo el valor de $min pero tiene el valor $value") else null
        }
        return null
    }
}

data class MaxValue(val max: Int) : ValidationRule {

    override fun validate(value: Any?,bean: Any): BusinessMessages? {
        if (value is Int) {
            return if (value > max) BusinessMessages.single("Debe tener como máximo el valor de $max pero tiene el valor $value") else null
        }
        return null
    }
}