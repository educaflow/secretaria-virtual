package com.educaflow.subsystem.expedientes.controllers;

import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Response;
import com.educaflow.base.infrastructure.autofirma.AutoFirma;
import com.educaflow.base.infrastructure.pdf.Rectangulo;
import com.educaflow.base.util.DniUtil;
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
            if (dni==null) {
                throw new RuntimeException("dniFirmaDocumentoEntrada no puede ser null");
            }
            if (dni.isBlank()) {
                throw new RuntimeException("dniFirmaDocumentoEntrada no puede estar vacio");
            }
            if (DniUtil.isValid(dni)==false) {
                throw new RuntimeException("dniFirmaDocumentoEntrada no tiene un formato válido: " + getDniOfuscado(dni));
            }


            AutoFirma autofirma = (new AutoFirma(clazz))
                    .setRectangulo(rectanguloPosicionFirmaPDF)
                    .setPageNumber(pageNumber)
                    .addSourceTargetField(sourceField, targetField)
                    .setDni(dni);


            ActionResponse actionResponse = new ActionResponse();
            AutoFirma.sendToActionResponse(autofirma, actionResponse);

            return actionResponse;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deja visibles solo los 3 últimos caracteres del DNI, para que el mensaje de error permita
     * identificarlo sin volcar el dato personal completo en la respuesta ni en el log.
     */
    private static String getDniOfuscado(String dni) {
        if (dni.length() <= 3) {
            return "*".repeat(dni.length());
        }

        return "*".repeat(dni.length() - 3) + dni.substring(dni.length() - 3);
    }
}
