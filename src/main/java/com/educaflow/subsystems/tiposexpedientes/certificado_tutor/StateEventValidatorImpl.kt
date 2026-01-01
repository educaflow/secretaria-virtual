package com.educaflow.subsystems.tiposexpedientes.certificado_tutor;

import com.educaflow.shared.expedientes.services.StateEventValidator
import com.educaflow.shared.expedientes.services.annotations.BeanValidationRulesForStateAndEvent


import com.educaflow.base.infrastructure.validation.dsl.rules
import com.educaflow.base.infrastructure.validation.engine.BeanValidationRules
import com.educaflow.shared.expedientes.db.CertificadoTutor as model

class StateEventValidatorImpl: StateEventValidator {

    @BeanValidationRulesForStateAndEvent
    public fun getForStateEntradaDatosInEventDelete(): BeanValidationRules {
        return rules {
            field(model::getValoresAmbitoCreador) {

            }

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