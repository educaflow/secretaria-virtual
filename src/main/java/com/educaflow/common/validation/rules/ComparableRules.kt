package com.educaflow.common.validation.rules

import com.educaflow.common.validation.engine.ValidationRule
import com.educaflow.common.validation.messages.BusinessMessages
import kotlin.reflect.KFunction

data class GreaterThan<T : Comparable<T>>(val comparableAnotherField:KFunction<T?>) : ValidationRule {
    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        val comparableAnotherValue=comparableAnotherField.call(bean);
        if (value == null) {
            return null
        }
        if (comparableAnotherValue == null) {
            return null
        }

        if (value is Comparable<*>) {
            @Suppress("UNCHECKED_CAST")
            val comparableValue = value as T
            return if (comparableValue.compareTo(comparableAnotherValue) > 0) null
            else BusinessMessages.single("El valor debe ser mayor que $comparableAnotherValue")
        }

        return null
    }
}

data class GreaterThanOrEqual<T : Comparable<T>>(val comparableAnotherField: KFunction<T?>) : ValidationRule {
    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        val anotherValue = comparableAnotherField.call(bean) ?: return null
        if (value == null) return null
        if (value is Comparable<*>) {
            @Suppress("UNCHECKED_CAST")
            val comparableValue = value as T
            return if (comparableValue.compareTo(anotherValue) >= 0) null
            else BusinessMessages.single("El valor debe ser mayor o igual que $anotherValue")
        }
        return null
    }
}

data class LessThan<T : Comparable<T>>(val comparableAnotherField: KFunction<T?>) : ValidationRule {
    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        val anotherValue = comparableAnotherField.call(bean) ?: return null
        if (value == null) return null
        if (value is Comparable<*>) {
            @Suppress("UNCHECKED_CAST")
            val comparableValue = value as T
            return if (comparableValue.compareTo(anotherValue) < 0) null
            else BusinessMessages.single("El valor debe ser menor que $anotherValue")
        }
        return null
    }
}

data class LessThanOrEqual<T : Comparable<T>>(val comparableAnotherField: KFunction<T?>) : ValidationRule {
    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        val anotherValue = comparableAnotherField.call(bean) ?: return null
        if (value == null) return null
        if (value is Comparable<*>) {
            @Suppress("UNCHECKED_CAST")
            val comparableValue = value as T
            return if (comparableValue.compareTo(anotherValue) <= 0) null
            else BusinessMessages.single("El valor debe ser menor o igual que $anotherValue")
        }
        return null
    }
}

data class EqualTo<T>(val anotherField: KFunction<T?>) : ValidationRule {
    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        val anotherValue = anotherField.call(bean)
        return if (value == anotherValue) null
        else BusinessMessages.single("El valor debe ser igual a $anotherValue")
    }
}

data class NotEqualTo<T>(val anotherField: KFunction<T?>) : ValidationRule {
    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        val anotherValue = anotherField.call(bean)
        return if (value != anotherValue) null
        else BusinessMessages.single("El valor no debe ser igual a $anotherValue")
    }
}
