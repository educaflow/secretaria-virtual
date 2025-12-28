package com.educaflow.common.validation.dsl

import com.educaflow.common.validation.engine.BeanValidationRules
import com.educaflow.common.validation.engine.FieldValidationRules
import com.educaflow.common.validation.engine.ValidationRule
import com.educaflow.common.validation.rules.IfValueNotIn
import com.educaflow.common.validation.rules.IfValueIn
import kotlin.reflect.KFunction

@DslMarker
annotation class BeanValidationDSL

@BeanValidationDSL
class IfValueInBuilder(private val dependField: KFunction<*>, private val dependValues:List<Any>) {
    private val rules = mutableListOf<ValidationRule>()


    fun build(): IfValueIn {
        return IfValueIn(dependField, dependValues, rules)
    }

    @BeanValidationDSL
    operator fun ValidationRule.unaryPlus() {
        rules += this
    }
}


@BeanValidationDSL
class IfValueNotInBuilder(private val dependField: KFunction<*>, private val dependValues:List<Any>) {
    private val rules = mutableListOf<ValidationRule>()


    fun build(): IfValueNotIn {
        return IfValueNotIn(dependField, dependValues, rules)
    }

    @BeanValidationDSL
    operator fun ValidationRule.unaryPlus() {
        rules += this
    }
}


@BeanValidationDSL
fun ifValueIn(dependField: KFunction<*>, dependValues: List<Any>, setup: IfValueInBuilder.() -> Unit): IfValueIn {
    val builder = IfValueInBuilder(dependField, dependValues)
    builder.setup()
    return builder.build()
}

@BeanValidationDSL
fun ifValueNotIn(dependField: KFunction<*>, dependValues: List<Any>, setup: IfValueNotInBuilder.() -> Unit): IfValueNotIn {
    val builder = IfValueNotInBuilder(dependField, dependValues)
    builder.setup()
    return builder.build()
}

@BeanValidationDSL
class FieldValidationRulesBuilder(private val property: KFunction<*>) {
    private val rules = mutableListOf<ValidationRule>() // La lista de reglas se mueve aquí


    fun build(): FieldValidationRules {
        return FieldValidationRules(property, rules)
    }

    @BeanValidationDSL
    operator fun ValidationRule.unaryPlus() {
        rules += this
    }

    @BeanValidationDSL
    public fun field(property: KFunction<*>, setup: FieldValidationRulesBuilder.() -> Unit) {
        val builder = FieldValidationRulesBuilder(property)
        builder.setup()
        rules += builder.build()
    }
}


@BeanValidationDSL
class BeanValidationRulesBuilder {
    private val fieldValidationRules = mutableListOf<FieldValidationRules>()


    @BeanValidationDSL
    public fun field(property: KFunction<*>, setup: FieldValidationRulesBuilder.() -> Unit) {
        val builder = FieldValidationRulesBuilder(property)
        builder.setup()
        fieldValidationRules += builder.build()
    }


    @BeanValidationDSL
    operator fun FieldValidationRules.unaryPlus() {
        fieldValidationRules += this
    }


    fun build(): BeanValidationRules {
        return BeanValidationRules(fieldValidationRules)
    }

}

/**
 * Función de nivel superior para iniciar la construcción de las reglas de validación de un bean.
 * @param setup Lambda con el receptor [BeanValidationRulesBuilder] para definir todas las reglas.
 * @return Un objeto [BeanValidationRules] que contiene todas las reglas definidas.
 */
@BeanValidationDSL
fun rules(setup: BeanValidationRulesBuilder.() -> Unit): BeanValidationRules {
    val builder = BeanValidationRulesBuilder()
    builder.setup()
    return builder.build()
}