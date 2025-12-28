package com.educaflow.common.validation.rules

import com.educaflow.common.validation.engine.ValidationRule
import com.educaflow.common.validation.messages.BusinessMessages
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

class Past : ValidationRule {
    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        val date = toLocalDateOrNull(value) ?: return null
        val today = LocalDate.now()
        return if (!date.isBefore(today)) BusinessMessages.single("La fecha debe ser anterior a hoy") else null
    }
}

class PastOrToday : ValidationRule {
    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        val date = toLocalDateOrNull(value) ?: return null
        val today = LocalDate.now()
        return if (date.isAfter(today)) BusinessMessages.single("La fecha debe ser hoy o en el pasado") else null
    }
}

class Future : ValidationRule {
    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        val date = toLocalDateOrNull(value) ?: return null
        val today = LocalDate.now()
        return if (!date.isAfter(today)) BusinessMessages.single("La fecha debe ser posterior a hoy") else null
    }
}

class FutureOrToday : ValidationRule {
    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        val date = toLocalDateOrNull(value) ?: return null
        val today = LocalDate.now()
        return if (date.isBefore(today)) BusinessMessages.single("La fecha debe ser hoy o en el futuro") else null
    }
}


private fun toLocalDateOrNull(date: Any?): LocalDate? {
    return when (date) {
        is LocalDate ->  date
        is LocalDateTime ->  date.toLocalDate()
        is Date ->  date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        else ->  null
    }
}