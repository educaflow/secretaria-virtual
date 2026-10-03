package com.educaflow.base.infrastructure.validation.rules

import com.educaflow.base.infrastructure.validation.engine.ValidationRule
import com.axelor.db.modelservice.BusinessMessages
import com.axelor.i18n.I18n
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
            else BusinessMessages.single(I18n.get("El valor debe ser mayor que %s").format(comparableAnotherValue))
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
            else BusinessMessages.single(I18n.get("El valor debe ser mayor o igual que %s").format(anotherValue))
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
            else BusinessMessages.single(I18n.get("El valor debe ser menor que %s").format(anotherValue))
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
            else BusinessMessages.single(I18n.get("El valor debe ser menor o igual que %s").format(anotherValue))
        }
        return null
    }
}

data class EqualTo<T>(val anotherField: KFunction<T?>) : ValidationRule {
    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        val anotherValue = anotherField.call(bean)
        return if (value == anotherValue) null
        else BusinessMessages.single(I18n.get("El valor debe ser igual a %s").format(anotherValue))
    }
}

data class NotEqualTo<T>(val anotherField: KFunction<T?>) : ValidationRule {
    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        val anotherValue = anotherField.call(bean)
        return if (value != anotherValue) null
        else BusinessMessages.single(I18n.get("El valor no debe ser igual a %s").format(anotherValue))
    }
}

/**
 * El valor no puede ser menor que [min]. Vale para cualquier `Comparable` (enteros, decimales, fechas, horas…):
 *
 *     +MinValue(0)
 *     +MinValue(LocalDate.now(Convert.defaultZoneId).minusYears(1))
 *
 * Un valor nulo se da por válido: la obligatoriedad la pone `Required`.
 */
data class MinValue<T : Comparable<T>>(val min: T) : ValidationRule {

    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        if (value == null) {
            return null
        }
        if (value is Comparable<*>) {
            @Suppress("UNCHECKED_CAST")
            val comparableValue = value as T
            return if (comparableValue.compareTo(min) < 0) BusinessMessages.single(I18n.get("Debe tener como mínimo el valor de %s pero tiene el valor %s").format(min, value)) else null
        }
        return null
    }
}

/**
 * El valor no puede ser mayor que [max]. Vale para cualquier `Comparable` (enteros, decimales, fechas, horas…):
 *
 *     +MaxValue(LocalDate.now(Convert.defaultZoneId).year)
 *
 * Un valor nulo se da por válido: la obligatoriedad la pone `Required`.
 */
data class MaxValue<T : Comparable<T>>(val max: T) : ValidationRule {

    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        if (value == null) {
            return null
        }
        if (value is Comparable<*>) {
            @Suppress("UNCHECKED_CAST")
            val comparableValue = value as T
            return if (comparableValue.compareTo(max) > 0) BusinessMessages.single(I18n.get("Debe tener como máximo el valor de %s pero tiene el valor %s").format(max, value)) else null
        }
        return null
    }
}
