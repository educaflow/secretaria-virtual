package com.educaflow.base.infrastructure.validation.rules

import com.educaflow.base.infrastructure.validation.engine.ValidationRule
import com.axelor.db.modelservice.BusinessMessages
import com.axelor.i18n.I18n

class PostalCode : ValidationRule {

    // 5 dígitos; los 2 primeros son el código de provincia del INE (01 a 52, incluidos Ceuta y Melilla)
    private val pattern = Regex("^(0[1-9]|[1-4][0-9]|5[0-2])[0-9]{3}\$")

    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        if (value !is String || value.isBlank()) {
            return null
        }
        return if (!pattern.matches(value.trim())) BusinessMessages.single(I18n.get("El código postal no es válido, debe tener 5 dígitos y empezar por un código de provincia válido (01 a 52)")) else null
    }
}
