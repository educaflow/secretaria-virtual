package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.solicitud

import com.educaflow.subsystem.expedientes.services.validation.StateEventValidator
import com.educaflow.subsystem.expedientes.services.validation.BeanValidationRulesForStateAndEvent
import com.educaflow.base.infrastructure.validation.dsl.rules
import com.educaflow.base.infrastructure.validation.engine.BeanValidationRules
import com.educaflow.base.infrastructure.validation.rules.FirmaPdf
import com.educaflow.base.infrastructure.validation.rules.Lambda
import com.educaflow.base.infrastructure.validation.rules.MaxLength
import com.educaflow.base.infrastructure.validation.rules.MinLength
import com.educaflow.base.infrastructure.validation.rules.Pattern
import com.educaflow.base.infrastructure.validation.rules.Required
import com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.AnulacionMatriculaCicloFormativoV1Util as util
import com.educaflow.tramites.util.firma.ClaveCertificadoValida
import com.educaflow.tramites.util.firma.ifSituacionFirma
import com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1 as model

class StateEventValidatorImpl: StateEventValidator {

    @BeanValidationRulesForStateAndEvent
    fun getForStateDatosSolicitudInEventContinuar(): BeanValidationRules {
        return rules {
            field(model::getNia) {
                +Required()
                +Pattern("^\\d{8}\$")
            }
            field(model::getDireccion) {
                +Required()
                +MinLength(5)
                +MaxLength(150)
            }
            field(model::getTelefono) {
                +Required()
                +Pattern("^[6789]\\d{8}\$")
            }
            field(model::getPoblacion) {
                +Required()
                +MinLength(2)
                +MaxLength(100)
            }
            field(model::getProvincia) {
                +Required()
                +MinLength(2)
                +MaxLength(50)
            }
            field(model::getCodigoPostal) {
                +Required()
                +Pattern("^\\d{5}\$")
            }
            field(model::getCiclo) {
                +Required()
                +Lambda(util::sinOtraSolicitudEnCursoParaElMismoCiclo, "Ya tiene una solicitud de anulación en curso para este ciclo")
            }
        }
    }

    @BeanValidationRulesForStateAndEvent
    fun getForStatePendienteFirmaInEventVolver(): BeanValidationRules {
        return rules {
        }
    }

    @BeanValidationRulesForStateAndEvent
    fun getForStatePendienteFirmaInEventPresentar(): BeanValidationRules {
        return rules {
            field(model::getClaveCertificado) {
                +ifSituacionFirma({ it.isFirmaEnServidor() }) {
                    +ClaveCertificadoValida()
                }
            }
            field(model::getPdfSolicitudFirmada) {
                +ifSituacionFirma({ !it.isFirmaEnServidor() }) {
                    +Required()
                    +FirmaPdf(model::getPdfSolicitud)
                }
            }
        }
    }
}
