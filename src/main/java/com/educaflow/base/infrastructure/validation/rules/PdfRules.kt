package com.educaflow.base.infrastructure.validation.rules

import com.axelor.meta.db.MetaFile
import com.educaflow.base.infrastructure.metafile.MetaFileHelper
import com.educaflow.base.infrastructure.pdf.DocumentoPdfUtil
import com.educaflow.base.infrastructure.validation.engine.ValidationRule
import com.axelor.db.modelservice.BusinessMessages
import com.educaflow.base.util.DniUtil
import com.educaflow.base.util.SecurityUtil
import kotlin.reflect.KCallable

/**
 * Valida que el PDF firmado que llega en `value` es el original firmado por el **usuario autenticado**,
 * que es siempre quien firma: el DNI se lee de él, nunca del bean ni del formulario.
 */
data class FirmaPdf(val documentoOriginalField: KCallable<*>) : ValidationRule {

    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        if (value == null) {
            return null
        }

        if (value !is MetaFile) {
            return null
        }

        val metaFileOriginal = documentoOriginalField.call(bean)
        if (metaFileOriginal !is MetaFile) {
            return null
        }

        val dni = SecurityUtil.getUser()?.dni
        if (dni.isNullOrBlank()) {
            return null
        }

        if (DniUtil.isValid(dni)==false) {
            return BusinessMessages.single("El DNI no es válido")
        }

        val documentoOriginal = MetaFileHelper.getDocumentoPdf(metaFileOriginal)
        val documentoFirmado = MetaFileHelper.getDocumentoPdf(value)

        val errorMessage = DocumentoPdfUtil.validateFirmaPdf(documentoOriginal, documentoFirmado, dni)

        if (errorMessage.isPresent) {
            return BusinessMessages.single(errorMessage.get())
        } else {
            return null
        }
    }
}
