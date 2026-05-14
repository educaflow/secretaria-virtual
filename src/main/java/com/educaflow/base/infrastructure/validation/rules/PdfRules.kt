package com.educaflow.base.infrastructure.validation.rules

import com.axelor.meta.db.MetaFile
import com.educaflow.base.infrastructure.metafile.MetaFileHelper
import com.educaflow.base.infrastructure.pdf.DocumentoPdfUtil
import com.educaflow.base.infrastructure.validation.engine.ValidationRule
import com.axelor.db.modelservice.BusinessMessages
import kotlin.reflect.KCallable

data class FirmaPdf(val documentoOriginalField: KCallable<*>, val dniField: KCallable<*>) : ValidationRule {

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

        val dni = dniField.call(bean)
        if (dni !is String) {
            return null
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
