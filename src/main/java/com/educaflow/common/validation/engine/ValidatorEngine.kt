package com.educaflow.common.validation.engine


import com.educaflow.common.validation.messages.BusinessMessages


class ValidatorEngine {

    fun validate(bean: Any,validationRules: BeanValidationRules) : BusinessMessages {
        val businessMessages = BusinessMessages();

        for (fieldValidationRule in validationRules.fieldValidationRules) {
            val methodField= fieldValidationRule.methodField
            val value=methodField.call(bean)
            val fieldBusinessMessages = fieldValidationRule.validate( value ,bean)
            if ((fieldBusinessMessages!=null) && (fieldBusinessMessages.isNotEmpty())) {
                businessMessages.addAll(fieldBusinessMessages);
            }

        }

        return businessMessages
    }


    fun validate(bean: Any, fieldValidationRules: List<FieldValidationRules>) : BusinessMessages {
        val businessMessages = BusinessMessages();

        for (fieldValidationRule in fieldValidationRules) {
            val methodField= fieldValidationRule.methodField
            val value=methodField.call(bean)
            val fieldBusinessMessages = fieldValidationRule.validate( value ,bean)
            if ((fieldBusinessMessages!=null) && (fieldBusinessMessages.isNotEmpty())) {
                businessMessages.addAll(fieldBusinessMessages);
            }

        }

        return businessMessages
    }



}