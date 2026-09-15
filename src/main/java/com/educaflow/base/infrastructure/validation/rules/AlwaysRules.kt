package com.educaflow.base.infrastructure.validation.rules

import com.educaflow.base.infrastructure.validation.engine.ValidationRule
import com.axelor.db.modelservice.BusinessMessages
import com.axelor.i18n.I18n

/**
 * Rechaza **siempre** el campo con el mensaje indicado, sea cual sea su valor (incluido nulo).
 *
 * Se usa dentro de una rama `ifValueIn`/`ifValueNotIn`: la rama dice **cuándo** aplica y esta regla dice
 * **con qué mensaje** se rechaza. Suelta, fuera de una rama, rechazaría cualquier valor.
 */
data class AlwaysFail(val mensaje: String) : ValidationRule {

    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        return BusinessMessages.single(I18n.get(mensaje))
    }
}

/**
 * Acepta **siempre** el campo, sea cual sea su valor (incluido nulo). Es la contraria de [AlwaysFail].
 *
 * Se usa dentro de una rama `ifValueIn`/`ifValueNotIn` para declarar de forma explícita que en ese caso
 * el campo no se valida.
 */
class AlwaysPass : ValidationRule {

    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        return null
    }
}
