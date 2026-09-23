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
import com.educaflow.base.infrastructure.validation.rules.Lambda
import com.educaflow.base.infrastructure.validation.rules.MaxLength
import com.educaflow.base.infrastructure.validation.rules.MinLength
import com.educaflow.base.infrastructure.validation.rules.NoAllUpperCase
import com.educaflow.base.infrastructure.validation.rules.Required
import com.educaflow.base.infrastructure.validation.rules.SizeUnit
import com.educaflow.tramites.util.firma.ClaveCertificadoValida
import com.educaflow.tramites.util.firma.ifSituacionFirma
import com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.JustificacionFaltaProfesoradoV1Util as util
import com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesoradoV1 as model

class StateEventValidatorImpl: StateEventValidator {

    @BeanValidationRulesForStateAndEvent
    public fun getForStateEntradaDatosInEventGuardarDatos(): BeanValidationRules {
        return rules {
            field(model::getTipoJornadaFalta) {
                +Lambda(util::tieneTipoJornadaFalta, "Debe indicar el tipo de jornada faltada")
            }
            field(model::getFechaInicio) {
                +Lambda(util::tieneFechaInicio, "Debe indicar la fecha")
                +Lambda(util::fechaInicioNoAnteriorAHaceUnAnyo, "La fecha debe ser de los últimos 12 meses")
                +Lambda(util::fechaInicioNoPosteriorAHoy, "La fecha no puede ser posterior a hoy")
            }
            field(model::getFechaFin) {
                +ifLambda(util::necesitaFechaFin) {
                    +Lambda(util::tieneFechaFin, "Debe indicar la fecha de fin")
                    +ifLambda(util::tieneFechaFin) {
                        +ifLambda(util::tieneFechaInicio) {
                            +Lambda(util::fechaFinPosteriorAFechaInicio, "La fecha de fin debe ser posterior a la fecha de inicio")
                        }
                    }
                    +Lambda(util::fechaFinNoPosteriorAHoy, "La fecha de fin no puede ser posterior a hoy")
                }
            }
            field(model::getHoraInicio) {
                +ifLambda(util::necesitaHoraInicio) {
                    +Lambda(util::tieneHoraInicio, "Debe indicar la hora de inicio")
                }
            }
            field(model::getHoraFin) {
                +ifLambda(util::necesitaHoraFin) {
                    +Lambda(util::tieneHoraFin, "Debe indicar la hora de fin")
                    +ifLambda(util::tieneHoraFin) {
                        +ifLambda(util::tieneHoraInicio) {
                            +Lambda(util::horaFinPosteriorAHoraInicio, "La hora de fin debe ser posterior a la hora de inicio")
                        }
                    }
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
