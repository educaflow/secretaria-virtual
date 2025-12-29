package com.educaflow.subsystems.tiposexpedientes.comision_servicio;

import com.educaflow.shared.expedientes.StateEventValidator
import com.educaflow.shared.expedientes.annotations.BeanValidationRulesForStateAndEvent


import com.educaflow.base.infrastructure.validation.dsl.rules
import com.educaflow.base.infrastructure.validation.engine.BeanValidationRules

class StateEventValidatorImpl: StateEventValidator {

    @BeanValidationRulesForStateAndEvent
    public fun getForStateEntradaDatosInEventDelete(): BeanValidationRules {
        return rules {

        }
    }

    @BeanValidationRulesForStateAndEvent
    public fun getForStateEntradaDatosInEventPresentar(): BeanValidationRules {
        return rules {

        }
    }

    @BeanValidationRulesForStateAndEvent
    public fun getForStateFirmaPorUsuarioInEventBack(): BeanValidationRules {
        return rules {

        }
    }

    @BeanValidationRulesForStateAndEvent
    public fun getForStateFirmaPorUsuarioInEventPresentarDocumentosFirmados(): BeanValidationRules {
        return rules {

        }
    }

    @BeanValidationRulesForStateAndEvent
    public fun getForStateResolverPermitirComisionInEventResolver(): BeanValidationRules {
        return rules {

        }
    }

    @BeanValidationRulesForStateAndEvent
    public fun getForStateEntregaTicketsInEventResolver(): BeanValidationRules {
        return rules {

        }
    }
;

}