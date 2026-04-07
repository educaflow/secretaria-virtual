package com.educaflow.subsystem.expedientes.controllers;

import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Response;
import com.educaflow.base.infrastructure.autofirma.AutoFirma;
import com.educaflow.base.infrastructure.pdf.Rectangulo;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.services.internal.ExpedienteUtil;

public class FirmaController {

    @CallMethod
    public Response firmarDocumentoEntrada(long id, String sourceField, String targetField, float x, float y, float width, float height, int pageNumber) {
        try {
            Expediente expediente = ExpedienteUtil.getExpedienteFromIdExpediente(id);
            Rectangulo rectanguloPosicionFirmaPDF = new Rectangulo(x, y, width, height);
            Class clazz = expediente.getClass();

            String dni=expediente.getDniFirmaDocumentoEntrada();
            if ((dni==null) || (dni.isBlank())) {
                throw new RuntimeException("El DNI no puede estar vacio");
            }


            AutoFirma autofirma = (new AutoFirma(clazz))
                    .setRectangulo(rectanguloPosicionFirmaPDF)
                    .setPageNumber(pageNumber)
                    .addSourceTargetField(sourceField, targetField)
                    .setNif(dni);


            ActionResponse actionResponse = new ActionResponse();
            AutoFirma.sendToActionResponse(autofirma, actionResponse);

            return actionResponse;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
