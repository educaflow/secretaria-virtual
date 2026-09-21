package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.revision

import com.educaflow.subsystem.tramitador.tramitacion.validation.StateEventValidator
import com.educaflow.subsystem.tramitador.tramitacion.validation.BeanValidationRulesForStateAndEvent
import com.educaflow.base.infrastructure.validation.dsl.ifValueIn
import com.educaflow.base.infrastructure.validation.dsl.rules
import com.educaflow.base.infrastructure.validation.engine.BeanValidationRules
import com.educaflow.base.infrastructure.validation.rules.MaxLength
import com.educaflow.base.infrastructure.validation.rules.MinLength
import com.educaflow.base.infrastructure.validation.rules.Required
import com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1 as model
import com.educaflow.subsystem.expedientes.db.SentidoRevisionAnulacionMatriculaCicloFormativoV1 as SentidoRevision

class StateEventValidatorImpl: StateEventValidator {

    @BeanValidationRulesForStateAndEvent
    fun getForStatePendienteRevisionInEventContinuar(): BeanValidationRules {
        return rules {
            field(model::getSentidoRevision) {
                +Required()
            }
            field(model::getMotivoRechazo) {
                +ifValueIn(model::getSentidoRevision, listOf(SentidoRevision.RECHAZAR)) {
                    +Required()
                    +MinLength(10)
                    +MaxLength(1000)
                }
            }
            field(model::getTextoSubsanacion) {
                +ifValueIn(model::getSentidoRevision, listOf(SentidoRevision.SUBSANAR)) {
                    +Required()
                    +MinLength(10)
                    +MaxLength(1000)
                }
            }
        }
    }
}
