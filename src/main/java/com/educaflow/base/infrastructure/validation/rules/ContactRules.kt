package com.educaflow.base.infrastructure.validation.rules

import com.educaflow.base.infrastructure.validation.engine.ValidationRule
import com.axelor.db.modelservice.BusinessMessages
import com.axelor.i18n.I18n

class Phone : ValidationRule {

    private val pattern = Regex("^[6789][0-9]{8}\$")

    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        if (value !is String || value.isBlank()) {
            return null
        }
        return if (!pattern.matches(value.trim())) BusinessMessages.single(I18n.get("El teléfono no es válido, debe tener 9 dígitos y empezar por 6, 7, 8 o 9")) else null
    }
}
