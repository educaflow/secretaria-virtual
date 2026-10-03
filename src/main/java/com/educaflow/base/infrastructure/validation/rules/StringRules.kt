package com.educaflow.base.infrastructure.validation.rules

import com.educaflow.base.infrastructure.validation.engine.ValidationRule
import com.axelor.db.modelservice.BusinessMessages
import com.axelor.i18n.I18n
import java.util.regex.Pattern
import java.util.Locale

data class MinLength(val min: Int) : ValidationRule {

    override fun validate(value: Any?,bean: Any): BusinessMessages? {
        if (value is String) {
            return if (value.length < min) BusinessMessages.single(I18n.get("Debe tener como mínimo una longitud de %s pero tiene %s").format(min, value.length)) else null
        }
        return null
    }
}

data class MaxLength(val max: Int) : ValidationRule {

    override fun validate(value: Any?,bean: Any): BusinessMessages? {
        if (value is String) {
            return if (value.length > max) BusinessMessages.single(I18n.get("Debe tener como máximo una longitud de %s pero tiene %s").format(max, value.length)) else null
        }
        return null
    }
}

class NoAllUpperCase : ValidationRule {

    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        if (value is String) {
            if (value.any { it.isLetter() } && value.uppercase(Locale.of("es", "ES")) == value) {
                return BusinessMessages.single(I18n.get("No puede estar todo en mayúsculas"))
            }
        }
        return null
    }
}

data class Pattern(val regex: String) : ValidationRule {

    private val compiledPattern = Pattern.compile(regex, Pattern.UNICODE_CHARACTER_CLASS)

    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        if (value is String) {
            val matcher = compiledPattern.matcher(value.trim())
            return if (!matcher.matches()) BusinessMessages.single(I18n.get("El valor no cumple con el patrón especificado")) else null
        }
        return null
    }
}

class ListIntNumbers : ValidationRule {

    // Solo números enteros separados por comas, espacios opcionales
    private val pattern = Regex("^(\\s*-?\\d+\\s*)(,\\s*-?\\d+\\s*)*\$")

    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        if (value is String) {
            val trimmed = value.trim()
            return if (!pattern.matches(trimmed)) BusinessMessages.single(I18n.get("Debe ser una lista de números enteros separados por comas")) else null
        }
        return null
    }
}