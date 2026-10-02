package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.resolucion

import com.educaflow.subsystem.tramitador.tramitacion.validation.StateEventValidator
import com.educaflow.subsystem.tramitador.tramitacion.validation.BeanValidationRulesForStateAndEvent
import com.educaflow.base.infrastructure.validation.dsl.ifValueIn
import com.educaflow.base.infrastructure.validation.dsl.rules
import com.educaflow.base.infrastructure.validation.engine.BeanValidationRules
import com.educaflow.base.infrastructure.validation.rules.MaxLength
import com.educaflow.base.infrastructure.validation.rules.MinLength
import com.educaflow.base.infrastructure.validation.rules.Required
import com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1 as model
import com.educaflow.subsystem.expedientes.db.TipoResolucionAnulacionMatriculaCicloFormativoV1 as TipoResolucion

class StateEventValidatorImpl: StateEventValidator {

    @BeanValidationRulesForStateAndEvent
    fun getForStatePendienteResolucionInEventBack(): BeanValidationRules {
        return rules {
        }
    }

    @BeanValidationRulesForStateAndEvent
    fun getForStatePendienteResolucionInEventResolver(): BeanValidationRules {
        return rules {
            field(model::getTipoResolucion) {
                +Required()
            }
            field(model::getMotivoRechazo) {
                +ifValueIn(model::getTipoResolucion, listOf(TipoResolucion.RECHAZAR)) {
                    +Required()
                    +MinLength(10)
                    +MaxLength(1000)
                }
            }
        }
    }

    // FIRMAR y RECHAZAR_FIRMA son eventos de sistema: los dispara el servidor cuando el director firma o
    // rechaza la resolución en su bandeja de firmas. FIRMAR trae de la tarea de firma la resolución
    // firmada; RECHAZAR_FIRMA no trae nada.
    @BeanValidationRulesForStateAndEvent
    fun getForStatePendienteFirmaDirectorInEventFirmar(): BeanValidationRules {
        return rules {
            field(model::getPdfResolucionFirmada) {
                +Required()
            }
        }
    }

    @BeanValidationRulesForStateAndEvent
    fun getForStatePendienteFirmaDirectorInEventRechazarFirma(): BeanValidationRules {
        return rules {
        }
    }
}
