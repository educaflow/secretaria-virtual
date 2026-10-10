package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.entrada

import com.educaflow.subsystem.tramitador.tramitacion.validation.StateEventValidator
import com.educaflow.subsystem.tramitador.tramitacion.validation.BeanValidationRulesForStateAndEvent
import com.educaflow.base.infrastructure.validation.dsl.rules
import com.educaflow.base.infrastructure.validation.engine.BeanValidationRules
import com.educaflow.base.infrastructure.validation.rules.Dni
import com.educaflow.base.infrastructure.validation.rules.FileMaxSize
import com.educaflow.base.infrastructure.validation.rules.FileType
import com.educaflow.base.infrastructure.validation.rules.FirmaPdf
import com.educaflow.base.infrastructure.validation.dsl.ifLambda
import com.educaflow.base.infrastructure.validation.rules.Lambda
import com.educaflow.base.infrastructure.validation.rules.MaxLength
import com.educaflow.base.infrastructure.validation.rules.MinLength
import com.educaflow.base.infrastructure.validation.rules.Nia
import com.educaflow.base.infrastructure.validation.rules.Phone
import com.educaflow.base.infrastructure.validation.rules.PostalCode
import com.educaflow.base.infrastructure.validation.rules.Required
import com.educaflow.base.infrastructure.validation.rules.SizeUnit
import com.educaflow.subsystem.common.db.Persona
import com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.AnulacionMatriculaCicloFormativoV1Util as util
import com.educaflow.tramites.util.firma.ClaveCertificadoValida
import com.educaflow.tramites.util.firma.ifSituacionFirma
import com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1 as model

class StateEventValidatorImpl: StateEventValidator {

    @BeanValidationRulesForStateAndEvent
    fun getForStateEntradaDatosInEventGuardarDatos(): BeanValidationRules {
        return rules {
            // Solo se teclea en papel y en representación. En los demás modos el Tramitador restaura (o copia
            // del interesado) su identificación y su contacto, así que estos field(...) solo le abren la whitelist.
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
                field(Persona::getEmail) {
                    +MaxLength(255)
                }
                field(Persona::getTelefono) {
                    +Phone()
                }
            }
            field(model::getPersonaInteresada) {
                +ifLambda(util::esPresentadoEnRepresentacion) {
                    +Lambda(util::interesadoDistintoDelSolicitante, "La persona interesada no puede tener el mismo DNI que quien presenta la solicitud: si la presenta para sí misma, cree el expediente eligiendo «Para la persona que lo presenta»")
                }
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
                    +Dni()
                }
                field(Persona::getNia) {
                    +Required()
                    +Nia()
                }
                field(Persona::getDireccion) {
                    +Required()
                    +MinLength(5)
                    +MaxLength(150)
                }
                field(Persona::getTelefono) {
                    +Required()
                    +Phone()
                }
                field(Persona::getMunicipio) {
                    +Required()
                }
                field(Persona::getCp) {
                    +Required()
                    +PostalCode()
                }
            }
            field(model::getCiclo) {
                +Required()
                +Lambda(util::sinOtraSolicitudEnCursoParaElMismoCiclo, "Ya tiene una solicitud de anulación en curso para este ciclo")
            }
        }
    }

    @BeanValidationRulesForStateAndEvent
    fun getForStatePendientePresentacionInEventBack(): BeanValidationRules {
        return rules {
        }
    }

    @BeanValidationRulesForStateAndEvent
    fun getForStateEntradaDatosInEventBack(): BeanValidationRules {
        return rules {
        }
    }

    @BeanValidationRulesForStateAndEvent
    fun getForStatePendienteDocumentoEscaneadoInEventContinuar(): BeanValidationRules {
        return rules {
            field(model::getPdfSolicitudFirmada) {
                +Required()
                +FileType(listOf("application/pdf"))
                +FileMaxSize(10, SizeUnit.MB)
            }
        }
    }

    @BeanValidationRulesForStateAndEvent
    fun getForStatePendientePresentacionInEventPresentar(): BeanValidationRules {
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
