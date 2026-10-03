package com.educaflow.base.infrastructure.validation.rules

import com.axelor.meta.MetaFiles
import com.axelor.meta.db.MetaFile
import com.educaflow.base.infrastructure.validation.engine.ValidationRule
import com.axelor.db.modelservice.BusinessMessages
import com.axelor.i18n.I18n
import java.math.BigDecimal
import java.nio.file.Files

class Required : ValidationRule {

    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        if (value == null) {
            return BusinessMessages.single(I18n.get("Es requerido"))
        }

        if (value is String && value.isBlank()) {
            return BusinessMessages.single(I18n.get("Es requerido"))
        }

        if (value is MetaFile) {
            if (value.fileName==null || value.fileName.isBlank()) {
                return BusinessMessages.single(I18n.get("Es requerido"))
            }
            // Se mide el fichero en disco: fileSize es un dato del MetaFile que el cliente puede dictar.
            if (Files.size(MetaFiles.getPath(value)) <= 0) {
                return BusinessMessages.single(I18n.get("No puede estar vacío"))
            }
        }

        if (value is Number) {
            when (value) {
                is BigDecimal -> if (value.compareTo(BigDecimal.ZERO) == 0) return BusinessMessages.single(I18n.get("No puede ser cero"))
                is Int, is Long, is Short, is Byte -> if (value.toLong() == 0L) return BusinessMessages.single(I18n.get("No puede ser cero"))
                is Float, is Double -> if (value.toDouble() == 0.0) return BusinessMessages.single(I18n.get("No puede ser cero"))
            }
        }

        return null
    }
}
