package com.educaflow.subsystem.firmas.util;

import com.axelor.auth.db.User;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.inject.Beans;
import com.educaflow.base.util.DniUtil;
import com.educaflow.subsystem.criptografia.db.CertificadoDigital;
import com.educaflow.subsystem.criptografia.service.CertificadoDigitalService;
import com.educaflow.subsystem.criptografia.service.TipoAlmacenClave;
import com.educaflow.subsystem.firmas.db.SituacionFirma;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calcula la {@link SituacionFirma} de un firmante: en qué situación está para poder firmar en el
 * servidor, deducida de su DNI y del certificado digital habilitado dado de alta para ese DNI.
 *
 * <p>Lo invoca el getter del campo derivado {@code situacionFirma} de {@code TareaFirma}, así que
 * <strong>nunca devuelve {@code null} ni propaga excepción</strong>: ante cualquier error degrada a
 * {@link SituacionFirma#SIN_CERTIFICADO}, que es el valor seguro (deja al firmante el panel de
 * AutoFirma y no habilita la firma en servidor con una situación que no se ha podido determinar).
 * El getter que genera Axelor para un campo virtual solo captura {@code NullPointerException}, de
 * modo que cualquier otra excepción rompería la carga del formulario de firma si no se capturase
 * aquí.
 *
 * <p>Vive en {@code ..util..} y no en el {@code *ServiceImpl} porque el cálculo lo dispara el getter
 * de la entidad, y las entidades de {@code ..db..} no pueden depender de {@code ..service..}.
 */
public final class SituacionFirmaBuilder {

    private static final Logger log = LoggerFactory.getLogger(SituacionFirmaBuilder.class);

    private SituacionFirmaBuilder() {
    }

    public static SituacionFirma build(User firmante) {
        String dni = (firmante == null) ? null : firmante.getDni();

        // Comprobación previa obligatoria: getTipoAlmacenClaveByDni valida el DNI y aborta con un
        // error de negocio si no es válido. Aquí un DNI inválido no es un error, es una situación.
        if (DniUtil.isValid(dni) == false) {
            return SituacionFirma.SIN_DNI;
        }

        try {
            CertificadoDigitalService certificadoDigitalService = (CertificadoDigitalService) Beans
                    .get(ModelServiceFactory.class)
                    .resolve(CertificadoDigital.class);

            TipoAlmacenClave tipoAlmacenClave = certificadoDigitalService.getTipoAlmacenClaveByDni(dni);
            if (tipoAlmacenClave == null) {
                return SituacionFirma.SIN_CERTIFICADO;
            }

            return toSituacionFirma(tipoAlmacenClave);
        } catch (Exception ex) {
            log.error("No se pudo determinar la situación de firma del firmante con dni={}", enmascararDni(dni), ex);
            return SituacionFirma.SIN_CERTIFICADO;
        }
    }

    /**
     * Traduce el tipo de almacén de clave del subsistema de criptografía a la situación de firma
     * del mismo nombre. El {@code switch} es exhaustivo y <strong>sin rama {@code default}</strong>
     * a propósito: si algún día {@link TipoAlmacenClave} gana un valor nuevo, esta traducción deja
     * de compilar y obliga a decidir su {@link SituacionFirma}, en lugar de degradar en silencio a
     * un valor equivocado.
     */
    private static SituacionFirma toSituacionFirma(TipoAlmacenClave tipoAlmacenClave) {
        return switch (tipoAlmacenClave) {
            case DISPOSITIVO_CON_PIN -> SituacionFirma.DISPOSITIVO_CON_PIN;
            case DISPOSITIVO_SIN_PIN -> SituacionFirma.DISPOSITIVO_SIN_PIN;
            case FICHERO_CON_CLAVE -> SituacionFirma.FICHERO_CON_CLAVE;
            case FICHERO_SIN_CLAVE -> SituacionFirma.FICHERO_SIN_CLAVE;
        };
    }

    /**
     * Devuelve el DNI enmascarado para poder trazarlo en el log sin escribirlo completo: solo se
     * conservan los tres últimos caracteres alfanuméricos y el resto se sustituye por asteriscos.
     */
    private static String enmascararDni(String dni) {
        int visibles = 3;
        StringBuilder enmascarado = new StringBuilder();
        for (int i = 0; i < dni.length(); i++) {
            char caracter = dni.charAt(i);
            if (i < dni.length() - visibles || Character.isLetterOrDigit(caracter) == false) {
                enmascarado.append('*');
            } else {
                enmascarado.append(caracter);
            }
        }
        return enmascarado.toString();
    }
}
