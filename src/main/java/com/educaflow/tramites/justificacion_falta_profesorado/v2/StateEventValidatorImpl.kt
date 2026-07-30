package com.educaflow.tramites.justificacion_falta_profesorado.v2

import com.educaflow.subsystem.expedientes.services.validation.StateEventValidator
import com.educaflow.subsystem.expedientes.services.validation.BeanValidationRulesForStateAndEvent
import com.educaflow.subsystem.expedientes.db.MotivoFaltaJustificacionFaltaProfesoradoV2
import com.educaflow.subsystem.expedientes.db.TipoJornadaFaltaJustificacionFaltaProfesoradoV2
import com.educaflow.subsystem.expedientes.db.TipoResolucionJustificacionFaltaProfesoradoV2
import com.educaflow.base.infrastructure.validation.dsl.ifValueIn
import com.educaflow.base.infrastructure.validation.dsl.rules
import com.educaflow.base.infrastructure.validation.engine.BeanValidationRules
import com.educaflow.base.infrastructure.validation.rules.FileMaxSize
import com.educaflow.base.infrastructure.validation.rules.FileType
import com.educaflow.base.infrastructure.validation.rules.FirmaPdf
import com.educaflow.base.infrastructure.validation.rules.GreaterThan
import com.educaflow.base.infrastructure.validation.rules.MaxLength
import com.educaflow.base.infrastructure.validation.rules.MaxValue
import com.educaflow.base.infrastructure.validation.rules.MinLength
import com.educaflow.base.infrastructure.validation.rules.MinValue
import com.educaflow.base.infrastructure.validation.rules.NoAllUpperCase
import com.educaflow.base.infrastructure.validation.rules.Required
import com.educaflow.base.infrastructure.validation.rules.Pattern
import com.educaflow.base.infrastructure.validation.rules.SizeUnit
import java.time.LocalDate
import com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesoradoV2 as model

class StateEventValidatorImpl: StateEventValidator {

    @BeanValidationRulesForStateAndEvent
    public fun getForStateEntradaDatosInEventGuardarDatos(): BeanValidationRules {
        return rules {
            field(model::getDias) {
                +Required()
                +Pattern("^(\\s*\\b(?:[1-9]|[12][0-9]|3[01])\\b\\s*)(?:,\\s*\\b(?:[1-9]|[12][0-9]|3[01])\\b\\s*)*\$")
            }
            field(model::getMes) {
                +Required()
            }
            field(model::getAnyo) {
                +MaxValue(LocalDate.now().year)
                +MinValue(LocalDate.now().year - 1)
                +Required()
            }
            field(model::getTipoJornadaFalta) {
                +Required()
            }
            field(model::getHoraInicio) {
                +ifValueIn(model::getTipoJornadaFalta, listOf(TipoJornadaFaltaJustificacionFaltaProfesoradoV2.JORNADA_PARCIAL)) {
                    +Required()
                }
            }
            field(model::getHoraFin) {
                +ifValueIn(model::getTipoJornadaFalta, listOf(TipoJornadaFaltaJustificacionFaltaProfesoradoV2.JORNADA_PARCIAL)) {
                    +Required()
                    +GreaterThan(model::getHoraInicio)
                }
            }
            field(model::getMotivoFalta) {
                +Required()
            }
            field(model::getOtroMotivo) {
                +ifValueIn(model::getMotivoFalta, listOf(MotivoFaltaJustificacionFaltaProfesoradoV2.OTROS)) {
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
            field(model::getPdfSolicitudFirmado) {
                +Required()
                +FirmaPdf(model::getPdfSolicitud, model::getDniFirmaDocumentoEntrada)
            }
        }
    }

    @BeanValidationRulesForStateAndEvent
    fun getForStatePendienteResolucionInEventResolver():BeanValidationRules {
        return rules {
            field(model::getTipoResolucion) {
                +Required()
            }
            field(model::getDisconformidad) {
                +ifValueIn(model::getTipoResolucion, listOf(TipoResolucionJustificacionFaltaProfesoradoV2.SUBSANAR_DATOS)) {
                    +Required()
                }
            }
            field(model::getResolucion) {
                +ifValueIn(model::getTipoResolucion, listOf(TipoResolucionJustificacionFaltaProfesoradoV2.RECHAZAR)) {
                    +Required()
                }
            }

        }
    }
}