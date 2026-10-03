package com.educaflow.base.infrastructure.validation.rules

import com.axelor.meta.MetaFiles
import com.axelor.meta.db.MetaFile
import com.educaflow.base.infrastructure.metafile.MetaFileHelper
import com.educaflow.base.util.MetaFileUtil
import com.itextpdf.io.image.ImageType
import com.itextpdf.io.image.ImageTypeDetector
import com.educaflow.base.infrastructure.validation.engine.ValidationRule
import com.axelor.db.modelservice.BusinessMessages
import com.axelor.i18n.I18n
import java.nio.file.Files
import java.util.regex.Pattern

data class FileMaxSize(val max: Int,val unit: SizeUnit) : ValidationRule {

    override fun validate(value: Any?,bean: Any): BusinessMessages? {
        if (value is MetaFile) {
            // Se mide el fichero en disco: fileSize es un dato del MetaFile que el cliente puede dictar.
            val tamano = Files.size(MetaFiles.getPath(value))
            if (tamano<=max*unit.multiplier) {
                return null
            } else {
                return BusinessMessages.single(I18n.get("El tamaño del archivo debe ser como máximo de %s %s pero tiene un tamaño de %s KB").format(max, unit, tamano / 1024))
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

/**
 * El tipo se decide por la cabecera del contenido: el `fileType` del MetaFile es el Content-Type que declaró el cliente.
 * Solo admite los tipos que sabe detectar por contenido; cualquier otro rechazaría en silencio todos los ficheros.
 */
data class FileType(val fileTypes: List<String>) : ValidationRule {

    init {
        require(TIPOS_DETECTABLES.containsAll(fileTypes)) {
            "FileType solo admite los tipos que detecta por contenido $TIPOS_DETECTABLES, pero se ha indicado $fileTypes"
        }
    }

    override fun validate(value: Any?,bean: Any): BusinessMessages? {
        if (value is MetaFile) {
            if (tipoReal(value) in fileTypes) {
                return null
            } else {
                return BusinessMessages.single(I18n.get("El tipo de archivo debe ser uno de los siguientes: %s").format(fileTypes.joinToString(", ")))
            }
        }
        return null
    }

    private fun tipoReal(metaFile: MetaFile): String? {
        if (MetaFileHelper.isPdf(metaFile)) {
            return MetaFileHelper.PDF_MIME_TYPE
        }
        return TIPOS_IMAGEN[ImageTypeDetector.detectImageType(MetaFileUtil.downloadHeader(metaFile, BYTES_CABECERA_IMAGEN))]
    }

    private companion object {
        // ImageTypeDetector solo mira los 8 primeros bytes
        const val BYTES_CABECERA_IMAGEN = 8
        val TIPOS_IMAGEN = mapOf(
            ImageType.PNG to "image/png",
            ImageType.JPEG to "image/jpeg",
            ImageType.GIF to "image/gif"
        )
        val TIPOS_DETECTABLES = TIPOS_IMAGEN.values + MetaFileHelper.PDF_MIME_TYPE
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
                BusinessMessages.single(I18n.get("El nombre de archivo '%s' no cumple con el patrón '%s'.").format(fileName, regex))
            }
        }
        return null
    }
}