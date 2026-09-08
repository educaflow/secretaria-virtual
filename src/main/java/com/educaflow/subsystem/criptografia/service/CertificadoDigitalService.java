package com.educaflow.subsystem.criptografia.service;

import com.axelor.db.modelservice.ModelService;
import com.educaflow.base.infrastructure.criptografia.AlmacenClave;
import com.axelor.db.modelservice.BusinessMessages;
import com.educaflow.subsystem.criptografia.db.CertificadoDigital;

import java.util.Optional;

public interface CertificadoDigitalService extends ModelService<CertificadoDigital> {

    AlmacenClave getAlmacenClaveByDni(String dni);
    Optional<BusinessMessages> validateGetAlmacenClaveByDni(String dni);
    AlmacenClave getAlmacenClaveByDni(String dni, String claveAcceso);
    Optional<BusinessMessages> validateGetAlmacenClaveByDni(String dni, String claveAcceso);
    SituacionFirma getSituacionFirmaByDni(String dni);
    Optional<BusinessMessages> validateGetSituacionFirmaByDni(String dni);

    /**
     * Resuelve el titular de un DNI.
     *
     * <p>Acción escalar de solo lectura: no persiste nada y no recibe ni devuelve la entidad. Si existe un
     * usuario de la aplicación con ese DNI, devuelve el nombre y los apellidos de su ficha con
     * {@code tomadoDelUsuario = true}; si no existe, devuelve {@link DatosTitular#sinUsuario()}.
     *
     * <p>Es el <strong>mismo</strong> cálculo que se aplica al guardar el certificado digital, para que la
     * pantalla no pueda prometer un titular distinto del que el servidor va a persistir.
     *
     * @param dni DNI del que se quiere resolver el titular
     * @return los datos del titular; nunca {@code null}
     */
    DatosTitular getDatosTitularByDni(String dni);

    /**
     * Validador de {@link #getDatosTitularByDni(String)}.
     *
     * @param dni DNI del que se quiere resolver el titular
     * @return los mensajes de negocio que impiden la operación, o vacío si no hay ninguno
     */
    Optional<BusinessMessages> validateGetDatosTitularByDni(String dni);
}
