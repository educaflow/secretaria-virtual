package com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.resolucion

import com.educaflow.subsystem.tramitador.tramitacion.validation.StateEventValidator
import com.educaflow.subsystem.tramitador.tramitacion.validation.BeanValidationRulesForStateAndEvent
import com.educaflow.subsystem.expedientes.db.TipoResolucionJustificacionFaltaProfesoradoV1
import com.educaflow.base.infrastructure.validation.dsl.ifValueIn
import com.educaflow.base.infrastructure.validation.dsl.rules
import com.educaflow.base.infrastructure.validation.engine.BeanValidationRules
import com.educaflow.base.infrastructure.validation.rules.MaxLength
import com.educaflow.base.infrastructure.validation.rules.Required
import com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesoradoV1 as model

class StateEventValidatorImpl: StateEventValidator {

    @BeanValidationRulesForStateAndEvent
    fun getForStatePendienteResolucionInEventResolver():BeanValidationRules {
        return rules {
            field(model::getTipoResolucion) {
                +Required()
            }
            field(model::getMotivoRechazo) {
                +ifValueIn(model::getTipoResolucion, listOf(TipoResolucionJustificacionFaltaProfesoradoV1.RECHAZAR)) {
                    +Required()
                    +MaxLength(1000)
                }
            }
            field(model::getMotivoDevolucion) {
                +ifValueIn(model::getTipoResolucion, listOf(TipoResolucionJustificacionFaltaProfesoradoV1.DEVOLVER)) {
                    +Required()
                    +MaxLength(1000)
                }
            }

        }
    }
}
