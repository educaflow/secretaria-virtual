package com.educaflow.apps.expedientes.tiposexpedientes.justificacion_falta_profesorado

import com.educaflow.apps.expedientes.common.StateEventValidator
import com.educaflow.apps.expedientes.common.annotations.BeanValidationRulesForStateAndEvent
import com.educaflow.apps.expedientes.db.MotivoFaltaJustificacionFaltaProfesorado
import com.educaflow.apps.expedientes.db.TipoJornadaFaltaJustificacionFaltaProfesorado
import com.educaflow.apps.expedientes.db.TipoResolucionJustificacionFaltaProfesorado
import com.educaflow.common.validation.dsl.ifValueIn
import com.educaflow.common.validation.dsl.rules
import com.educaflow.common.validation.engine.BeanValidationRules
import com.educaflow.common.validation.rules.DocumentoPdfFirmaValida
import com.educaflow.common.validation.rules.FileMaxSize
import com.educaflow.common.validation.rules.FileType
import com.educaflow.common.validation.rules.GreaterThan
import com.educaflow.common.validation.rules.MaxLength
import com.educaflow.common.validation.rules.MaxValue
import com.educaflow.common.validation.rules.MinLength
import com.educaflow.common.validation.rules.MinValue
import com.educaflow.common.validation.rules.NoAllUpperCase
import com.educaflow.common.validation.rules.Required
import com.educaflow.common.validation.rules.Pattern
import com.educaflow.common.validation.rules.SizeUnit
import java.time.LocalDate
import com.educaflow.apps.expedientes.db.JustificacionFaltaProfesorado as model

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
                +ifValueIn(model::getTipoJornadaFalta, listOf(TipoJornadaFaltaJustificacionFaltaProfesorado.JORNADA_PARCIAL)) {
                    +Required()
                }
            }
            field(model::getHoraFin) {
                +ifValueIn(model::getTipoJornadaFalta, listOf(TipoJornadaFaltaJustificacionFaltaProfesorado.JORNADA_PARCIAL)) {
                    +Required()
                    +GreaterThan(model::getHoraInicio)
                }
            }
            field(model::getMotivoFalta) {
                +Required()
            }
            field(model::getOtroMotivo) {
                +ifValueIn(model::getMotivoFalta, listOf(MotivoFaltaJustificacionFaltaProfesorado.OTROS)) {
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
            field(model::getDocumentacionPresentadaFirmadaUsuario) {
                +Required()
                +DocumentoPdfFirmaValida()
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
                +ifValueIn(model::getTipoResolucion, listOf(TipoResolucionJustificacionFaltaProfesorado.SUBSANAR_DATOS)) {
                    +Required()
                }
            }
            field(model::getResolucion) {
                +ifValueIn(model::getTipoResolucion, listOf(TipoResolucionJustificacionFaltaProfesorado.RECHAZAR)) {
                    +Required()
                }
            }

        }
    }
}