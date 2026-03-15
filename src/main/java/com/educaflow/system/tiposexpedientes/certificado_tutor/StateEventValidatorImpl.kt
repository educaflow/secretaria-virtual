package com.educaflow.system.tiposexpedientes.certificado_tutor;

import com.educaflow.subsystem.expedientes.services.StateEventValidator
import com.educaflow.subsystem.expedientes.services.annotations.BeanValidationRulesForStateAndEvent


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
    public fun getForStateRevisionInEventSubsanar(): BeanValidationRules {
        return rules {

        }
    }

    @BeanValidationRulesForStateAndEvent
    public fun getForStateRevisionInEventAceptar(): BeanValidationRules {
        return rules {

        }
    }

    @BeanValidationRulesForStateAndEvent
    public fun getForStateRevisionInEventRechazar(): BeanValidationRules {
        return rules {

        }
    }

    @BeanValidationRulesForStateAndEvent
    public fun getForStateAceptadoInEventSubsanar(): BeanValidationRules {
        return rules {

        }
    }

    @BeanValidationRulesForStateAndEvent
    public fun getForStateRechazadoInEventSubsanar(): BeanValidationRules {
        return rules {

        }
    }
;

}