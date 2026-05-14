package com.educaflow.base.infrastructure.validation.engine

import com.axelor.db.modelservice.BusinessMessages

interface ValidationRule {

    /**
     * Validates a bean against the rule.
     * Si retorna null es que la validación ha sido correcta. En caso contrario, retorna el mensaje de error.
     */
    fun validate(value: Any?,bean: Any): BusinessMessages?

}