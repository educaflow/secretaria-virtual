package com.educaflow.common.validation.rules

import com.axelor.meta.db.MetaFile
import com.educaflow.common.validation.engine.ValidationRule
import com.educaflow.common.validation.messages.BusinessMessages
import java.util.regex.Pattern

data class FileMaxSize(val max: Int,val unit: SizeUnit) : ValidationRule {

    override fun validate(value: Any?,bean: Any): BusinessMessages? {
        if (value is MetaFile) {
            if (value.fileSize<=max*unit.multiplier) {
                return null
            } else {
                return BusinessMessages.single("El tamaño del archivo debe ser como máximo de ${max / 1024} KB pero tiene un tamaño de ${value.fileSize / 1024} KB")
            }
        }
        return null
    }
}

enum class SizeUnit(val multiplier: Long) {
    B(1),
    KB(1024),
    MB(1024 * 1024),
    GB(1024 * 1024 * 1024)
}

data class FileType(val fileTypes: List<String>) : ValidationRule {

    override fun validate(value: Any?,bean: Any): BusinessMessages? {
        if (value is MetaFile) {
            if (value.fileType in fileTypes) {
                return null
            } else {
                return BusinessMessages.single("El tipo de archivo debe ser uno de los siguientes: ${fileTypes.joinToString(", ")} pero es ${value.fileType}")
            }
        }
        return null
    }
}


data class FileName(val regex: String) : ValidationRule {

    private val pattern = Pattern.compile(regex)

    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        if (value is MetaFile) {
            val fileName = value.fileName
            return if (pattern.matcher(fileName).matches()) {
                null
            } else {
                BusinessMessages.single("El nombre de archivo '$fileName' no cumple con el patrón '$regex'.")
            }
        }
        return null
    }
}