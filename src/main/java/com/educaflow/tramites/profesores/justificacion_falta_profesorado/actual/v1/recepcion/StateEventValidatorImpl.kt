package com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.recepcion

import com.educaflow.subsystem.tramitador.tramitacion.validation.StateEventValidator
import com.educaflow.subsystem.tramitador.tramitacion.validation.BeanValidationRulesForStateAndEvent
import com.educaflow.subsystem.expedientes.db.MotivoFaltaJustificacionFaltaProfesoradoV1
import com.educaflow.base.infrastructure.validation.dsl.ifLambda
import com.educaflow.base.infrastructure.validation.dsl.ifValueIn
import com.educaflow.base.infrastructure.validation.dsl.rules
import com.educaflow.base.infrastructure.validation.engine.BeanValidationRules
import com.educaflow.base.infrastructure.validation.rules.FileMaxSize
import com.educaflow.base.infrastructure.validation.rules.FileType
import com.educaflow.base.infrastructure.validation.rules.FirmaPdf
import com.educaflow.base.infrastructure.validation.rules.GreaterThan
import com.educaflow.base.infrastructure.validation.rules.MaxLength
import com.educaflow.base.infrastructure.validation.rules.MinLength
import com.educaflow.base.infrastructure.validation.rules.MinValue
import com.educaflow.base.infrastructure.validation.rules.NoAllUpperCase
import com.educaflow.base.infrastructure.validation.rules.PastOrToday
import com.educaflow.base.infrastructure.validation.rules.Required
import com.educaflow.base.infrastructure.validation.rules.SizeUnit
import com.educaflow.base.util.Convert
import java.time.LocalDate
import com.educaflow.tramites.util.firma.ClaveCertificadoValida
import com.educaflow.tramites.util.firma.ifSituacionFirma
import com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.JustificacionFaltaProfesoradoV1Util as util
import com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesoradoV1 as model

class StateEventValidatorImpl: StateEventValidator {

    @BeanValidationRulesForStateAndEvent
    public fun getForStateEntradaDatosInEventGuardarDatos(): BeanValidationRules {
        return rules {
            field(model::getTipoJornadaFalta) {
                +Required()
            }
            field(model::getFechaInicio) {
                +Required()
                +MinValue(LocalDate.now(Convert.defaultZoneId).minusDays(7))
                +PastOrToday()
            }
            field(model::getFechaFin) {
                +ifLambda(util::necesitaFechaFin) {
                    +Required()
                    +GreaterThan(model::getFechaInicio)
                    +PastOrToday()
                }
            }
            field(model::getHoraInicio) {
                +ifLambda(util::necesitaHoraInicio) {
                    +Required()
                }
            }
            field(model::getHoraFin) {
                +ifLambda(util::necesitaHoraFin) {
                    +Required()
                    +GreaterThan(model::getHoraInicio)
                }
            }
            field(model::getMotivoFalta) {
                +Required()
            }
            field(model::getOtroMotivo) {
                +ifValueIn(model::getMotivoFalta, listOf(MotivoFaltaJustificacionFaltaProfesoradoV1.OTROS)) {
                    +Required()
                    +NoAllUpperCase()
                    +MinLength(5)
                    +MaxLength(100)
                }
            }
            field(model::getJustificante) {
                +Required()
                +FileType(listOf("image/png","image/jpeg","image/gif","application/pdf"))
                +FileMaxSize(5, SizeUnit.MB)
            }
        }
    }

    @BeanValidationRulesForStateAndEvent
    fun getForStatePendientePresentacionInEventBack():BeanValidationRules {
        return rules {
        }
    }

    @BeanValidationRulesForStateAndEvent
    fun getForStatePendientePresentacionInEventPresentar():BeanValidationRules {
        return rules {
            field(model::getClaveCertificado) {
                +ifSituacionFirma({ it.isFirmaEnServidor() }) {
                    +ClaveCertificadoValida()
                }
            }
            field(model::getPdfSolicitudFirmado) {
                +ifSituacionFirma({ !it.isFirmaEnServidor() }) {
                    +Required()
                    +FirmaPdf(model::getPdfSolicitud)
                }
            }
        }
    }
}
