package com.educaflow.base.infrastructure.validation.rules

import com.educaflow.base.infrastructure.validation.engine.ValidationRule
import com.axelor.db.modelservice.BusinessMessages
import com.axelor.i18n.I18n
import com.educaflow.base.util.IbanUtil

class Iban : ValidationRule {

    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        if (value !is String || value.isBlank()) {
            return null
        }
        val iban = value.trim().uppercase().replace(" ", "")
        return if (!IbanUtil.isValid(iban)) BusinessMessages.single(I18n.get("El IBAN no es válido")) else null
    }
}
