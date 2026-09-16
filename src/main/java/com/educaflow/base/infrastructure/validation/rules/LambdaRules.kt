package com.educaflow.base.infrastructure.validation.rules

import com.educaflow.base.infrastructure.validation.engine.ValidationRule
import com.axelor.db.modelservice.BusinessMessages
import com.axelor.i18n.I18n

/**
 * Delega la comprobación en una función que recibe el bean entero y devuelve `true` si es válido.
 * Si devuelve `false`, el campo se rechaza con [mensaje].
 *
 * Pensada para pasarle una función estática del propio tipo de expediente, de modo que el validador
 * se quede declarativo y la lógica (consultas, cálculos…) viva en una clase de utilidad:
 *
 *     field(model::getCiclo) {
 *         +Lambda(MiTramiteV1Util::sinOtraSolicitudEnCurso, "Ya tiene una solicitud en curso")
 *     }
 */
data class Lambda<T : Any>(val funcion: (T) -> Boolean, val mensaje: String) : ValidationRule {

    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        @Suppress("UNCHECKED_CAST")
        val valido = funcion(bean as T)

        return if (valido) null else BusinessMessages.single(I18n.get(mensaje))
    }
}
