package com.educaflow.base.infrastructure.validation.rules

import com.educaflow.base.infrastructure.validation.engine.ValidationRule
import com.axelor.db.modelservice.BusinessMessages
import com.axelor.i18n.I18n

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

        return if (listSize < min) BusinessMessages.single(I18n.get("Debe tener como mínimo %s elementos pero tiene %s").format(min, listSize)) else null

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

        return if (listSize > max) BusinessMessages.single(I18n.get("Debe tener como máximo %s elementos pero tiene %s").format(max, listSize)) else null

    }
}