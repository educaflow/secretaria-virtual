package com.educaflow.subsystem.criptografia.controller;

import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper;
import com.educaflow.subsystem.criptografia.db.CertificadoDigital;
import com.educaflow.subsystem.criptografia.service.CertificadoDigitalService;
import com.educaflow.subsystem.criptografia.service.DatosTitular;
import com.google.inject.Inject;

import java.util.Optional;

public class CertificadoDigitalController {

    private static final String CAMPO_VISTA_DNI = "dni";

    @Inject
    private ModelServiceFactory modelServiceFactory;

    /**
     * Resuelve el titular del DNI tecleado y lo devuelve al formulario.
     *
     * <p>Es una acción escalar de solo lectura: no construye la entidad, no escribe en base de datos (por eso
     * no lleva {@code @Transactional}) y los tres valores que devuelve son valores de formulario, que solo se
     * persisten si el administrador pulsa «Guardar», momento en el que el servidor vuelve a calcularlos.
     */
    @CallMethod
    public void getDatosTitularByDni(ActionRequest actionRequest, ActionResponse actionResponse) {
        final CertificadoDigitalService certificadoDigitalService = (CertificadoDigitalService) modelServiceFactory.resolve(CertificadoDigital.class);

        ActionRequestHelper<CertificadoDigital> actionRequestHelper = new ActionRequestHelper<>(actionRequest, CertificadoDigital.class);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        String dni = getDni(actionRequestHelper);

        Optional<BusinessMessages> validationResult = certificadoDigitalService.validateGetDatosTitularByDni(dni);
        if (validationResult.isPresent()) {
            actionResponseHelper.doResponseBusinessMessagesAsError(validationResult.get());
            return;
        }

        DatosTitular datosTitular = certificadoDigitalService.getDatosTitularByDni(dni);

        actionResponse.setValue("nombre", datosTitular.nombre());
        actionResponse.setValue("apellidos", datosTitular.apellidos());
        actionResponse.setValue("nombreTomadoDelUsuario", datosTitular.tomadoDelUsuario());
    }

    /**
     * El DNI tecleado en el formulario, o {@code null} si no se ha tecleado ninguno.
     *
     * <p>Se lee del contexto de la petición porque la acción es escalar: no hace falta ninguna entidad. En
     * particular <strong>MUST NOT</strong> usarse {@code getModel(...)}, que cuando el request trae {@code id}
     * devuelve la entidad gestionada por JPA y arriesgaría un flush no querido en la transacción de la petición.
     */
    private static String getDni(ActionRequestHelper<CertificadoDigital> actionRequestHelper) {
        Object dni = actionRequestHelper.getRequestData().get(CAMPO_VISTA_DNI);

        return dni == null ? null : dni.toString();
    }

}
