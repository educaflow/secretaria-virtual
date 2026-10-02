package com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.verificacion

import com.educaflow.subsystem.tramitador.tramitacion.validation.StateEventValidator
import com.educaflow.subsystem.tramitador.tramitacion.validation.BeanValidationRulesForStateAndEvent
import com.educaflow.base.infrastructure.validation.dsl.rules
import com.educaflow.base.infrastructure.validation.engine.BeanValidationRules
import com.educaflow.tramites.util.verificacion.resultadoVerificacion
import com.educaflow.tramites.util.verificacion.textoSubsanacion
import com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesoradoV1 as model
import com.educaflow.subsystem.expedientes.db.ResultadoVerificacionJustificacionFaltaProfesoradoV1 as ResultadoVerificacion

class StateEventValidatorImpl: StateEventValidator {

    @BeanValidationRulesForStateAndEvent
    fun getForStatePendienteVerificacionInEventVerificar(): BeanValidationRules {
        return rules {
            +resultadoVerificacion(model::getResultadoVerificacion)
            +textoSubsanacion(model::getTextoSubsanacion, model::getResultadoVerificacion, ResultadoVerificacion.SUBSANAR)
        }
    }
}
