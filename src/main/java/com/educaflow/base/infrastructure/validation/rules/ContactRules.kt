package com.educaflow.base.infrastructure.validation.rules

import com.educaflow.base.infrastructure.validation.engine.ValidationRule
import com.educaflow.base.util.NumeroTelefono
import com.axelor.db.modelservice.BusinessMessages
import com.axelor.i18n.I18n

class Phone : ValidationRule {

    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        if (value !is String || value.isBlank()) {
            return null
        }
        return if (!NumeroTelefono(value).esValidoDeEspana()) BusinessMessages.single(I18n.get("El teléfono no es válido, debe ser un teléfono de España")) else null
    }
}
