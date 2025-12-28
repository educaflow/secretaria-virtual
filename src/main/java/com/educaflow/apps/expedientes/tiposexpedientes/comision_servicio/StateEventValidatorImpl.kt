package com.educaflow.apps.expedientes.tiposexpedientes.comision_servicio;

import com.educaflow.apps.expedientes.common.StateEventValidator
import com.educaflow.apps.expedientes.common.annotations.BeanValidationRulesForStateAndEvent


import com.educaflow.common.validation.dsl.ifValueIn
import com.educaflow.common.validation.dsl.rules
import com.educaflow.common.validation.engine.BeanValidationRules
import com.educaflow.common.validation.rules.*
import java.time.LocalDate
import com.educaflow.apps.expedientes.db.ComisionServicio as model

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