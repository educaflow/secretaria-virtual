package com.educaflow.base.infrastructure.validation.rules

import com.educaflow.base.infrastructure.validation.engine.ValidationRule
import com.axelor.db.modelservice.BusinessMessages
import com.axelor.i18n.I18n
import com.educaflow.base.util.DniUtil

class Dni : ValidationRule {

    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        if (value !is String || value.isBlank()) {
            return null
        }
        return if (!DniUtil.isValid(value.trim().uppercase())) BusinessMessages.single(I18n.get("El DNI/NIE no es válido")) else null
    }
}

class Nia : ValidationRule {

    private val pattern = Regex("^[0-9]{8}\$")

    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        if (value !is String || value.isBlank()) {
            return null
        }
        return if (!pattern.matches(value.trim())) BusinessMessages.single(I18n.get("El NIA no es válido, debe tener 8 dígitos")) else null
    }
}

class Nuss : ValidationRule {

    // 12 dígitos; los 2 primeros son el código de provincia (01 a 52, incluidos Ceuta y Melilla) donde
    // se tramitó la primera alta, los 8 siguientes el número secuencial y los 2 últimos el control
    private val pattern = Regex("^(0[1-9]|[1-4][0-9]|5[0-2])[0-9]{10}\$")

    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        if (value !is String || value.isBlank()) {
            return null
        }
        return if (!pattern.matches(value.trim())) BusinessMessages.single(I18n.get("El número de la Seguridad Social no es válido, debe tener 12 dígitos y empezar por un código de provincia válido (01 a 52)")) else null
    }
}
