package com.educaflow.common.validation.engine

import com.axelor.db.annotations.Widget
import com.axelor.i18n.I18n
import com.educaflow.common.util.TextUtil
import com.educaflow.common.validation.messages.BusinessMessage
import com.educaflow.common.validation.messages.BusinessMessages
import java.lang.reflect.Field
import kotlin.reflect.KFunction

data class FieldValidationRules(val methodField:KFunction<*>, val validationRules: List<ValidationRule>) : ValidationRule{


    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        val fieldName = getFieldName()
        val label= getLabel(bean.javaClass, fieldName);

        val messages = BusinessMessages()
        for (validationRule in validationRules) {
            if (validationRule is FieldValidationRules) {
                if (value == null) {
                    continue
                }
                if (value is List<*>) {
                    val listValue=value
                    for ((index, value) in listValue.withIndex()){

                        if (value == null) {
                            continue
                        }
                        val innerBean = value;
                        val innerValue = validationRule.methodField.call(innerBean)
                        val innerMessages = validationRule.validate(innerValue, innerBean)
                        messages.addAll(getBusinessMessagesUpdatingFieldNameAndLabel(innerMessages,fieldName,label,index))
                    }

                } else {
                    val innerBean = value;
                    val innerValue = validationRule.methodField.call(innerBean)
                    val innerMessages = validationRule.validate(innerValue, innerBean)
                    messages.addAll(getBusinessMessagesUpdatingFieldNameAndLabel(innerMessages,fieldName,label,null))
                }
            } else {
                val innerMessages = validationRule.validate(value, bean)
                messages.addAll(getBusinessMessagesUpdatingFieldNameAndLabel(innerMessages,fieldName,label,null))
            }


        }
        return if (messages.isEmpty()) null else messages
    }

    public fun getFieldName(): String {
        val methodName=methodField.name

        val fieldName = when {
            methodName.startsWith("get") && methodName.length > 3 -> methodName.substring(3)
            methodName.startsWith("is") && methodName.length > 2 -> methodName.substring(2)
            else -> throw IllegalArgumentException("El método $methodName no es un getter válido")
        }

        return if (fieldName.length >= 2 && fieldName[0].isUpperCase() && fieldName[1].isUpperCase()) {
            fieldName
        } else {
            fieldName.replaceFirstChar { it.lowercaseChar() }
        }

    }

}







private fun getLabel(clazz: Class<*>, nombreCampo: String): String {
    try {
        var label:String;
        val field: Field = clazz.getDeclaredField(nombreCampo)
        if (field.isAnnotationPresent(Widget::class.java)) {
            val widget: Widget = field.getAnnotation(Widget::class.java)
            label=widget.title ?: TextUtil.humanize(nombreCampo)
        } else {
            label=TextUtil.humanize(nombreCampo)
        }

        return I18n.get(label)

    } catch (e: NoSuchFieldException) {
        throw IllegalArgumentException("El campo '$nombreCampo' no existe en la clase ${clazz.simpleName}", e)
    }
}


private fun getBusinessMessagesUpdatingFieldNameAndLabel(businessMessages: BusinessMessages?, fieldName: String, label: String,index : Int?): BusinessMessages {
    if (businessMessages == null ) {
        return BusinessMessages()
    }

    val realBusinessMessages = BusinessMessages()

    for (businessMessage in businessMessages) {
        val realFieldName: String?
        val realMessage: String? = businessMessage.message
        val realLabel: String?
        if (businessMessage.fieldName == null || businessMessage.fieldName.isEmpty()) {
            realFieldName = fieldName
        } else {
            if (index==null) {
                realFieldName = fieldName+"."+businessMessage.fieldName
            } else {
                realFieldName = fieldName+"[$index]."+businessMessage.fieldName
            }

        }
        if (businessMessage.label == null || businessMessage.label.trim().isEmpty()) {
            realLabel = label
        } else {
            if (index==null) {
                realLabel = label + " " + businessMessage.label
            } else {
                realLabel = label+" [$index] "+businessMessage.label
            }

        }

        val realBusinessMessage = BusinessMessage(realFieldName, realMessage, realLabel)
        realBusinessMessages.add(realBusinessMessage)
    }

    return realBusinessMessages

}


