package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.resolucion

import com.educaflow.subsystem.tramitador.tramitacion.validation.StateEventValidator
import com.educaflow.subsystem.tramitador.tramitacion.validation.BeanValidationRulesForStateAndEvent
import com.educaflow.base.infrastructure.validation.dsl.rules
import com.educaflow.base.infrastructure.validation.engine.BeanValidationRules
import com.educaflow.base.infrastructure.validation.rules.MaxLength
import com.educaflow.base.infrastructure.validation.rules.MinLength
import com.educaflow.base.infrastructure.validation.rules.Required
import com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1 as model

class StateEventValidatorImpl: StateEventValidator {

    @BeanValidationRulesForStateAndEvent
    fun getForStatePendienteFirmaDirectorInEventFirmar(): BeanValidationRules {
        return rules {
        }
    }

    @BeanValidationRulesForStateAndEvent
    fun getForStatePendienteFirmaDirectorInEventDevolver(): BeanValidationRules {
        return rules {
            field(model::getMotivoDevolucion) {
                +Required()
                +MinLength(10)
                +MaxLength(1000)
            }
        }
    }
}
