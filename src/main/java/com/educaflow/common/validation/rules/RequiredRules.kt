package com.educaflow.common.validation.rules

import com.axelor.meta.db.MetaFile
import com.educaflow.common.validation.engine.ValidationRule
import com.educaflow.common.validation.messages.BusinessMessages
import java.math.BigDecimal
import kotlin.reflect.KFunction

class Required : ValidationRule {

    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        if (value == null) {
            return BusinessMessages.single("Es requerido")
        }

        if (value is String && value.trim().isEmpty()) {
            return BusinessMessages.single("Es requerido")
        }

        if (value is MetaFile) {
            if (value.fileName.isEmpty()) {
                return BusinessMessages.single("Es requerido")
            }
            if (value.fileSize <= 0) {
                return BusinessMessages.single("No puede estar vacío")
            }
        }

        if (value is Number) {
            when (value) {
                is BigDecimal -> if (value.compareTo(BigDecimal.ZERO) == 0) return BusinessMessages.single("No puede ser cero")
                is Int, is Long, is Short, is Byte -> if (value.toLong() == 0L) return BusinessMessages.single("No puede ser cero")
                is Float, is Double -> if (value.toDouble() == 0.0) return BusinessMessages.single("No puede ser cero")
            }
        }

        return null
    }
}

