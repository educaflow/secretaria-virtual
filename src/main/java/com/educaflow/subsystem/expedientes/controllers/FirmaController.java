package com.educaflow.subsystem.expedientes.controllers;

import com.axelor.auth.db.User;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Response;
import com.educaflow.base.infrastructure.autofirma.AutoFirma;
import com.educaflow.base.infrastructure.pdf.Rectangulo;
import com.educaflow.base.util.DniUtil;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.services.internal.ExpedienteUtil;

public class FirmaController {

    @CallMethod
    public Response firmarDocumento(long idExpediente, String sourceField, String targetField, float x, float y, float width, float height, int pageNumber) {
        try {
            Expediente expediente = ExpedienteUtil.getExpedienteFromIdExpediente(idExpediente);
            Rectangulo rectanguloPosicionFirmaPDF = new Rectangulo(x, y, width, height);
            Class clazz = expediente.getClass();

            String dniFirmante=SecurityUtil.getUser().getDni();
            if (dniFirmante==null) {
                throw new RuntimeException("dniFirmante no puede ser null");
            }
            if (dniFirmante.isBlank()) {
                throw new RuntimeException("dniFirmante no puede estar vacio");
            }
            if (DniUtil.isValid(dniFirmante)==false) {
                throw new RuntimeException("dniFirmante no tiene un formato válido: " + DniUtil.enmascarar(dniFirmante));
            }


            AutoFirma autofirma = new AutoFirma(clazz)
                    .setRectangulo(rectanguloPosicionFirmaPDF)
                    .setPageNumber(pageNumber)
                    .addSourceTargetField(sourceField, targetField)
                    .setDni(dniFirmante);


            ActionResponse actionResponse = new ActionResponse();
            AutoFirma.sendToActionResponse(autofirma, actionResponse);

            return actionResponse;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
