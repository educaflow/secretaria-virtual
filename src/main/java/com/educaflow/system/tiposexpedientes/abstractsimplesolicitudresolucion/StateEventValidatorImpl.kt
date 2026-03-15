package com.educaflow.system.tiposexpedientes.abstractsimplesolicitudresolucion;

import com.educaflow.shared.expedientes.services.StateEventValidator
import com.educaflow.shared.expedientes.services.annotations.BeanValidationRulesForStateAndEvent


import com.educaflow.base.infrastructure.validation.dsl.rules
import com.educaflow.base.infrastructure.validation.engine.BeanValidationRules

class StateEventValidatorImpl: StateEventValidator {

    @BeanValidationRulesForStateAndEvent
    public fun getForStateEntradaDatosInEventDelete(): BeanValidationRules {
        return rules {

        }
    }

    @BeanValidationRulesForStateAndEvent
    public fun getForStateEntradaDatosInEventGuardarDatos(): BeanValidationRules {
        return rules {

        }
    }

    @BeanValidationRulesForStateAndEvent
    public fun getForStatePendientePresentacionInEventBack(): BeanValidationRules {
        return rules {

        }
    }

    @BeanValidationRulesForStateAndEvent
    public fun getForStatePendientePresentacionInEventPresentar(): BeanValidationRules {
        return rules {

        }
    }

    @BeanValidationRulesForStateAndEvent
    public fun getForStatePendienteResolucionInEventResolver(): BeanValidationRules {
        return rules {

        }
    }
;

}