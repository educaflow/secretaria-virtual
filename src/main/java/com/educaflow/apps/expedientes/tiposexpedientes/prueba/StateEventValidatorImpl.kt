package com.educaflow.apps.expedientes.tiposexpedientes.prueba

import com.educaflow.apps.expedientes.common.StateEventValidator
import com.educaflow.apps.expedientes.common.annotations.BeanValidationRulesForStateAndEvent
import com.educaflow.apps.expedientes.db.FormacionCentroTrabajo
import com.educaflow.base.infrastructure.validation.dsl.rules
import com.educaflow.base.infrastructure.validation.engine.BeanValidationRules
import com.educaflow.base.infrastructure.validation.rules.FileMaxSize
import com.educaflow.base.infrastructure.validation.rules.FileType
import com.educaflow.base.infrastructure.validation.rules.MaxLength
import com.educaflow.base.infrastructure.validation.rules.MaxListSize
import com.educaflow.base.infrastructure.validation.rules.MinLength
import com.educaflow.base.infrastructure.validation.rules.MinListSize
import com.educaflow.base.infrastructure.validation.rules.NoAllUpperCase
import com.educaflow.base.infrastructure.validation.rules.Required
import com.educaflow.base.infrastructure.validation.rules.SizeUnit
import com.educaflow.apps.expedientes.db.Prueba as model

class StateEventValidatorImpl : StateEventValidator {

    @BeanValidationRulesForStateAndEvent
    fun getForStateEntradaDatosInEventPresentar(): BeanValidationRules {
        return rules {
            field(model::getSolicita) {
                +Required()
                +NoAllUpperCase()
                +MinLength(5)
                +MaxLength(100)
            }
            field(model::getCiclo) {
                +Required()
            }
            field(model::getJustificante) {
                +Required()
                +FileType(listOf("image/png", "image/jpeg", "image/gif", "application/pdf"))
                +FileMaxSize(5, SizeUnit.MB)
            }
            field(model::getFormacionesCentrosTrabajo) {
                +Required()
                +MinListSize(1)
                +MaxListSize(3)
                field(FormacionCentroTrabajo::getAlumno) {
                    +Required()
                    +MinLength(5)
                    +MaxLength(50)
                }
                field(FormacionCentroTrabajo::getA2) {
                    +Required()
                    +FileType(listOf("application/pdf"))
                    +FileMaxSize(5, SizeUnit.MB)
                }
                field(FormacionCentroTrabajo::getA3) {
                    +Required()
                    +FileType(listOf("application/pdf"))
                    +FileMaxSize(5, SizeUnit.MB)
                }
            }
        }
    }

    @BeanValidationRulesForStateAndEvent
    fun getForStateRevisionInEventRechazar(): BeanValidationRules {
        return rules {
        }
    }

    @BeanValidationRulesForStateAndEvent
    fun getForStateRevisionInEventSubsanar(): BeanValidationRules {
        return rules {
        }
    }

    @BeanValidationRulesForStateAndEvent
    fun getForStateRevisionInEventAceptar(): BeanValidationRules {
        return rules {
        }
    }

    @BeanValidationRulesForStateAndEvent
    fun getForStateAceptadoInEventSubsanar(): BeanValidationRules {
        return rules {
        }
    }
    @BeanValidationRulesForStateAndEvent
    fun getForStateRechazadoInEventSubsanar(): BeanValidationRules {
        return rules {
        }
    }


}