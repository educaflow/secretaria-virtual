package com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.tramitacion

import com.educaflow.subsystem.expedientes.services.validation.StateEventValidator
import com.educaflow.subsystem.expedientes.services.validation.BeanValidationRulesForStateAndEvent
import com.educaflow.subsystem.expedientes.db.TipoResolucionJustificacionFaltaProfesoradoV1
import com.educaflow.base.infrastructure.validation.dsl.ifValueIn
import com.educaflow.base.infrastructure.validation.dsl.rules
import com.educaflow.base.infrastructure.validation.engine.BeanValidationRules
import com.educaflow.base.infrastructure.validation.rules.Required
import com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesoradoV1 as model

class StateEventValidatorImpl: StateEventValidator {

    @BeanValidationRulesForStateAndEvent
    fun getForStatePendienteResolucionInEventResolver():BeanValidationRules {
        return rules {
            field(model::getTipoResolucion) {
                +Required()
            }
            field(model::getDisconformidad) {
                +ifValueIn(model::getTipoResolucion, listOf(TipoResolucionJustificacionFaltaProfesoradoV1.SUBSANAR_DATOS)) {
                    +Required()
                }
            }
            field(model::getResolucion) {
                +ifValueIn(model::getTipoResolucion, listOf(TipoResolucionJustificacionFaltaProfesoradoV1.RECHAZAR)) {
                    +Required()
                }
            }

        }
    }
}
