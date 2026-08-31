package com.educaflow.tramites.prueba.v1.tramitacion

import com.educaflow.subsystem.expedientes.services.validation.StateEventValidator
import com.educaflow.subsystem.expedientes.services.validation.BeanValidationRulesForStateAndEvent
import com.educaflow.base.infrastructure.validation.dsl.rules
import com.educaflow.base.infrastructure.validation.engine.BeanValidationRules
import com.educaflow.subsystem.expedientes.db.PruebaV1 as model

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
