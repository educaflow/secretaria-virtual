package com.educaflow.common.validation.rules

import com.educaflow.common.validation.engine.ValidationRule
import com.educaflow.common.validation.messages.BusinessMessages
import java.util.regex.Pattern
import java.util.Locale

data class MinLength(val min: Int) : ValidationRule {

    override fun validate(value: Any?,bean: Any): BusinessMessages? {
        if (value is String) {
            return if (value.length < min) BusinessMessages.single("Debe tener como mínimo una longitud de $min pero tiene ${value.length}") else null
        }
        return null
    }
}

data class MaxLength(val max: Int) : ValidationRule {

    override fun validate(value: Any?,bean: Any): BusinessMessages? {
        if (value is String) {
            return if (value.length > max) BusinessMessages.single("Debe tener como máximo una longitud de $max pero tiene ${value.length}") else null
        }
        return null
    }
}

class NoAllUpperCase : ValidationRule {

    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        if (value is String) {
            if (value.uppercase(Locale("es")).equals(value, ignoreCase = false)) {
                return BusinessMessages.single("No puede estar todo en mayúsculas")
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
            return if (!matcher.matches()) BusinessMessages.single("El valor no cumple con el patrón especificado") else null
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
            return if (!pattern.matches(trimmed)) BusinessMessages.single("Debe ser una lista de números enteros separados por comas") else null
        }
        return null
    }
}