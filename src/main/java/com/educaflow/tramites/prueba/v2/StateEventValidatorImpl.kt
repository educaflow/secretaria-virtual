package com.educaflow.tramites.prueba.v2;

import com.educaflow.subsystem.expedientes.services.validation.StateEventValidator
import com.educaflow.subsystem.expedientes.services.validation.BeanValidationRulesForStateAndEvent


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