package com.educaflow.subsystems.tiposexpedientes.abstractsimplesolicitudresolucion;

import com.educaflow.shared.expedientes.services.StateEventValidator
import com.educaflow.shared.expedientes.services.annotations.BeanValidationRulesForStateAndEvent


import com.educaflow.base.infrastructure.validation.dsl.ifValueIn
import com.educaflow.base.infrastructure.validation.dsl.rules
import com.educaflow.base.infrastructure.validation.engine.BeanValidationRules
import com.educaflow.base.infrastructure.validation.rules.*
import java.time.LocalDate
import com.educaflow.shared.expedientes.db.AbstractSimpleSolicitudResolucion as model

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