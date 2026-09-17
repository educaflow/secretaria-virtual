package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.solicitud

import com.educaflow.subsystem.expedientes.services.validation.StateEventValidator
import com.educaflow.subsystem.expedientes.services.validation.BeanValidationRulesForStateAndEvent
import com.educaflow.base.infrastructure.validation.dsl.rules
import com.educaflow.base.infrastructure.validation.engine.BeanValidationRules
import com.educaflow.base.infrastructure.validation.rules.FileMaxSize
import com.educaflow.base.infrastructure.validation.rules.FileType
import com.educaflow.base.infrastructure.validation.rules.FirmaPdf
import com.educaflow.base.infrastructure.validation.rules.SizeUnit
import com.educaflow.base.infrastructure.validation.dsl.ifLambda
import com.educaflow.base.infrastructure.validation.rules.Lambda
import com.educaflow.base.infrastructure.validation.rules.MaxLength
import com.educaflow.base.infrastructure.validation.rules.MinLength
import com.educaflow.base.infrastructure.validation.rules.Pattern
import com.educaflow.base.infrastructure.validation.rules.Required
import com.educaflow.subsystem.common.db.Persona
import com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.AnulacionMatriculaCicloFormativoV1Util as util
import com.educaflow.tramites.util.firma.ClaveCertificadoValida
import com.educaflow.tramites.util.firma.ifSituacionFirma
import com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1 as model

class StateEventValidatorImpl: StateEventValidator {

    @BeanValidationRulesForStateAndEvent
    fun getForStateDatosSolicitudInEventContinuar(): BeanValidationRules {
        return rules {
            // Solo se teclea en papel y en representación. En los demás modos el Tramitador restaura (o copia
            // del interesado) su identificación, así que estos field(...) solo le abren la whitelist.
            field(model::getPersonaSolicitante) {
                +ifLambda(util::esPresentadoEnPapelEnRepresentacion) {
                    +Lambda(util::tieneIdentificadoAlSolicitante, "Indique los apellidos, el nombre y un DNI o NIE válido de la persona que presenta la solicitud")
                }
                field(Persona::getApellidos) {
                    +MaxLength(150)
                }
                field(Persona::getNombre) {
                    +MaxLength(100)
                }
                field(Persona::getDni) {
                }
            }
            field(model::getPersonaInteresada) {
                field(Persona::getApellidos) {
                    +Required()
                    +MaxLength(150)
                }
                field(Persona::getNombre) {
                    +Required()
                    +MaxLength(100)
                }
                field(Persona::getDni) {
                    +Required()
                    +Lambda(util::tieneDniValido, "El DNI o NIE no es válido")
                }
                field(Persona::getNia) {
                    +Required()
                    +Pattern("^\\d{8}\$")
                }
                field(Persona::getDireccion) {
                    +Required()
                    +MinLength(5)
                    +MaxLength(150)
                }
                field(Persona::getTelefono) {
                    +Required()
                    +Pattern("^[6789]\\d{8}\$")
                }
                field(Persona::getMunicipio) {
                    +Required()
                }
                field(Persona::getCp) {
                    +Required()
                    +Pattern("^\\d{5}\$")
                }
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
    fun getForStateDatosSolicitudInEventVolver(): BeanValidationRules {
        return rules {
        }
    }

    @BeanValidationRulesForStateAndEvent
    fun getForStatePendienteDocumentoEscaneadoInEventContinuar(): BeanValidationRules {
        return rules {
            // El registro de entrada solo admite un PDF como documento de la solicitud.
            field(model::getPdfSolicitudFirmada) {
                +Required()
                +FileType(listOf("application/pdf"))
                +FileMaxSize(10, SizeUnit.MB)
            }
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
