package com.educaflow.common.validation.rules

import com.educaflow.common.validation.engine.ValidationRule
import com.educaflow.common.validation.messages.BusinessMessages

data class MinListSize(val min: Int) : ValidationRule {

    override fun validate(value: Any?,bean: Any): BusinessMessages? {
        var listSize:Int;

        if (value == null) {
            listSize = 0
        } else if (value is List<*>) {
            listSize = value.size
        } else {
            return null
        }

        return if (listSize < min) BusinessMessages.single("Debe tener como mínimo $min elementos pero tiene $listSize") else null

    }
}

data class MaxListSize(val max: Int) : ValidationRule {

    override fun validate(value: Any?,bean: Any): BusinessMessages? {
        var listSize:Int;

        if (value == null) {
            listSize = 0
        } else if (value is List<*>) {
            listSize = value.size
        } else {
            return null
        }

        return if (listSize > max) BusinessMessages.single("Debe tener como máximo $max elementos pero tiene $listSize") else null

    }
}