package com.educaflow.tramites.prueba.v1.tramitacion

import com.educaflow.subsystem.expedientes.tramitacion.validation.StateEventValidator
import com.educaflow.subsystem.expedientes.tramitacion.validation.BeanValidationRulesForStateAndEvent
import com.educaflow.base.infrastructure.validation.dsl.rules
import com.educaflow.base.infrastructure.validation.engine.BeanValidationRules

class StateEventValidatorImpl : StateEventValidator {

    @BeanValidationRulesForStateAndEvent
    fun getForStateRevisionInEventRechazar(): BeanValidationRules {
        return rules {
        }
    }

    @BeanValidationRulesForStateAndEvent
    fun getForStateRevisionInEventSubsanar(): BeanValidationRules {
        return rules {
        }
    }

    @BeanValidationRulesForStateAndEvent
    fun getForStateRevisionInEventAceptar(): BeanValidationRules {
        return rules {
        }
    }





}
