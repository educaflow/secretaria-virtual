package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.firma

import com.educaflow.subsystem.tramitador.tramitacion.validation.StateEventValidator
import com.educaflow.subsystem.tramitador.tramitacion.validation.BeanValidationRulesForStateAndEvent
import com.educaflow.base.infrastructure.validation.dsl.rules
import com.educaflow.base.infrastructure.validation.engine.BeanValidationRules
import com.educaflow.base.infrastructure.validation.rules.Required
import com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1 as model

// FIRMAR y RECHAZAR_FIRMA son eventos de sistema: los dispara el servidor cuando el secretario y, después, el
// director firman o rechazan la resolución en su bandeja de firmas. FIRMAR trae de la tarea de firma la
// resolución firmada; RECHAZAR_FIRMA no trae nada.
class StateEventValidatorImpl: StateEventValidator {

    @BeanValidationRulesForStateAndEvent
    fun getForStatePendienteFirmaSecretarioInEventFirmar(): BeanValidationRules {
        return rules {
            field(model::getPdfResolucionFirmada) {
                +Required()
            }
        }
    }

    @BeanValidationRulesForStateAndEvent
    fun getForStatePendienteFirmaSecretarioInEventRechazarFirma(): BeanValidationRules {
        return rules {
        }
    }

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
