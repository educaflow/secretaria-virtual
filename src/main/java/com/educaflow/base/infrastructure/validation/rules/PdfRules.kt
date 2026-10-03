package com.educaflow.base.infrastructure.validation.rules

import com.axelor.meta.db.MetaFile
import com.educaflow.base.infrastructure.metafile.MetaFileHelper
import com.educaflow.base.infrastructure.pdf.DocumentoPdfUtil
import com.educaflow.base.infrastructure.validation.engine.ValidationRule
import com.axelor.db.modelservice.BusinessMessages
import com.axelor.i18n.I18n
import com.educaflow.base.util.DniUtil
import com.educaflow.base.util.SecurityUtil
import kotlin.reflect.KCallable

/**
 * Valida que el PDF firmado que llega en `value` es el original firmado por el **usuario autenticado**,
 * que es siempre quien firma: el DNI se lee de él, nunca del bean ni del formulario.
 *
 * Si el usuario no tiene un DNI válido la regla **falla**, tenga o no valor el campo: sin DNI no se puede
 * comprobar quién firmó, así que nunca puede darse por buena la firma.
 *
 * También **falla** si el campo del documento original no tiene `MetaFile`: sin original no se puede
 * comprobar la firma.
 *
 * Y **falla** si el documento firmado no es un PDF (se decide por su contenido), en vez de dejar escapar el error técnico.
 */
data class FirmaPdf(val documentoOriginalField: KCallable<*>) : ValidationRule {

    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        val dni = SecurityUtil.getUser()?.dni
        if (dni.isNullOrBlank() || DniUtil.isValid(dni) == false) {
            return BusinessMessages.single(I18n.get("No es posible comprobar la firma porque su usuario no tiene un documento de identidad válido. Póngase en contacto con el administrador."))
        }

        if (value == null) {
            return null
        }

        if (value !is MetaFile) {
            return null
        }

        if (MetaFileHelper.isPdf(value) == false) {
            return BusinessMessages.single(I18n.get("El documento firmado debe ser un fichero PDF."))
        }

        val metaFileOriginal = documentoOriginalField.call(bean)
        if (metaFileOriginal !is MetaFile) {
            return BusinessMessages.single(I18n.get("No es posible comprobar la firma porque falta el documento original."))
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
