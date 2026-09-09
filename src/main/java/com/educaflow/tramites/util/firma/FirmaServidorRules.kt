package com.educaflow.tramites.util.firma

import com.axelor.db.modelservice.BusinessMessages
import com.axelor.i18n.I18n
import com.educaflow.base.infrastructure.validation.dsl.BeanValidationDSL
import com.educaflow.base.infrastructure.validation.engine.ValidationRule
import com.educaflow.base.util.SecurityUtil
import com.educaflow.subsystem.criptografia.service.SituacionFirma
import com.educaflow.subsystem.criptografia.util.CertificadoDigitalHelper

/*
 * Reglas del DSL de validación que gobiernan la firma en el servidor de los documentos de un expediente.
 *
 * La situación de firma se recalcula siempre del DNI del usuario autenticado: nunca se lee del formulario.
 * Qué rama aplica lo decide el validador con `ifSituacionFirma`; las reglas no vuelven a preguntar la
 * situación por dentro para decidir si actúan, así que cada una solo es válida dentro de la rama que le
 * corresponde y no devuelve «válido» por si acaso confiando en que otra regla tape el caso.
 *
 * Uso en un `StateEventValidatorImpl`:
 *
 *     field(model::getClaveCertificado) {
 *         +ifSituacionFirma({ it.isFirmaEnServidor() }) {
 *             +ClaveCertificadoValida()
 *         }
 *     }
 *     field(model::getPdfSolicitudFirmado) {
 *         +ifSituacionFirma({ !it.isFirmaEnServidor() }) {
 *             +Required()
 *             +FirmaPdf(model::getPdfSolicitud)
 *         }
 *     }
 */

private fun dniUsuarioAutenticado(): String? {
    return SecurityUtil.getUser()?.dni
}

private fun situacionFirmaUsuarioAutenticado(): SituacionFirma {
    return CertificadoDigitalHelper.getSituacionFirmaByDni(dniUsuarioAutenticado())
}

data class ClaveCertificadoValida(val mensaje: String = "No es posible firmar la solicitud: %s") : ValidationRule {

    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        val situacionFirma = situacionFirmaUsuarioAutenticado()
        if (situacionFirma.isFirmaEnServidor == false) {
            throw IllegalStateException("ClaveCertificadoValida solo se puede usar cuando se firma en el servidor, y la situación de firma es " + situacionFirma)
        }

        val clave = value as? String
        if (situacionFirma.isNecesitaClaveOPin && clave.isNullOrBlank()) {
            return BusinessMessages.single(mensajeClaveRequerida(situacionFirma))
        }

        if (CertificadoDigitalHelper.isClaveCertificadoCorrecta(dniUsuarioAutenticado(), clave) == false) {
            return BusinessMessages.single(I18n.get(mensaje).format(CertificadoDigitalHelper.motivoClaveErronea(situacionFirma)))
        }

        return null
    }

    private fun mensajeClaveRequerida(situacionFirma: SituacionFirma): String {
        return when (situacionFirma) {
            SituacionFirma.DISPOSITIVO_SIN_PIN -> I18n.get("El PIN es obligatorio")
            SituacionFirma.FICHERO_SIN_CLAVE -> I18n.get("La contraseña es obligatoria")
            else -> throw IllegalStateException("La situación de firma " + situacionFirma + " no necesita clave ni PIN")
        }
    }
}


data class IfSituacionFirma(
    val condicion: (SituacionFirma) -> Boolean,
    val validationRules: List<ValidationRule>
) : ValidationRule {

    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        if (condicion(situacionFirmaUsuarioAutenticado()) == false) {
            return null
        }

        val messages = BusinessMessages()
        for (validationRule in validationRules) {
            val innerMessages = validationRule.validate(value, bean)
            if ((innerMessages != null) && (innerMessages.isNotEmpty())) {
                messages.addAll(innerMessages)
            }
        }
        return messages
    }
}

@BeanValidationDSL
class IfSituacionFirmaBuilder(private val condicion: (SituacionFirma) -> Boolean) {
    private val rules = mutableListOf<ValidationRule>()

    fun build(): IfSituacionFirma {
        return IfSituacionFirma(condicion, rules)
    }

    @BeanValidationDSL
    operator fun ValidationRule.unaryPlus() {
        rules += this
    }
}

@BeanValidationDSL
fun ifSituacionFirma(condicion: (SituacionFirma) -> Boolean, setup: IfSituacionFirmaBuilder.() -> Unit): IfSituacionFirma {
    val builder = IfSituacionFirmaBuilder(condicion)
    builder.setup()
    return builder.build()
}
