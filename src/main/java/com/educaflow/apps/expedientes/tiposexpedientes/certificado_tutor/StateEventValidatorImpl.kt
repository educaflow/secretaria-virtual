package com.educaflow.apps.expedientes.tiposexpedientes.certificado_tutor;

import com.educaflow.apps.expedientes.common.StateEventValidator
import com.educaflow.apps.expedientes.common.annotations.BeanValidationRulesForStateAndEvent


import com.educaflow.common.validation.dsl.ifValueIn
import com.educaflow.common.validation.dsl.rules
import com.educaflow.common.validation.engine.BeanValidationRules
import com.educaflow.common.validation.rules.*
import java.time.LocalDate
import com.educaflow.apps.expedientes.db.CertificadoTutor as model

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