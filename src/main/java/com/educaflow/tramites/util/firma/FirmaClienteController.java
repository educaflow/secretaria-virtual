package com.educaflow.tramites.util.firma;

import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Response;
import com.educaflow.base.infrastructure.autofirma.AutoFirma;
import com.educaflow.base.infrastructure.pdf.Rectangulo;
import com.educaflow.base.util.DniUtil;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.base.util.TextUtil;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.tramitador.tramitacion.util.ExpedienteUtil;

import java.util.function.Consumer;

public class FirmaClienteController {

    /** Firma con AutoFirma en un rectángulo de una página del documento. */
    @CallMethod
    public Response firmarDocumento(long idExpediente, String sourceField, String targetField, float x, float y, float width, float height, int pageNumber) {
        Rectangulo rectanguloPosicionFirmaPDF = new Rectangulo(x, y, width, height);

        return firmar(idExpediente, sourceField, targetField, autofirma -> autofirma.setRectangulo(rectanguloPosicionFirmaPDF).setPageNumber(pageNumber));
    }

    /** Firma con AutoFirma en el campo de firma vacío del documento que se llama {@code nombreCampoFirma}: la posición y la página son las del campo. */
    @CallMethod
    public Response firmarDocumentoEnCampo(long idExpediente, String sourceField, String targetField, String nombreCampoFirma) {
        return firmar(idExpediente, sourceField, targetField, autofirma -> autofirma.setNombreCampoFirma(nombreCampoFirma));
    }

    private Response firmar(long idExpediente, String sourceField, String targetField, Consumer<AutoFirma> lugarFirma) {
        try {
            Expediente expediente = ExpedienteUtil.getExpedienteFromIdExpediente(idExpediente);
            Class clazz = expediente.getClass();

            String dniFirmante=SecurityUtil.getUser().getDni();
            TextUtil.requireNonBlank(dniFirmante, "dniFirmante no puede ser null ni blank");
            if (DniUtil.isValid(dniFirmante)==false) {
                throw new IllegalArgumentException("dniFirmante no tiene un formato válido: " + DniUtil.enmascarar(dniFirmante));
            }


            AutoFirma autofirma = new AutoFirma(clazz)
                    .addSourceTargetField(sourceField, targetField)
                    .setDni(dniFirmante);
            lugarFirma.accept(autofirma);


            ActionResponse actionResponse = new ActionResponse();
            AutoFirma.sendToActionResponse(autofirma, actionResponse);

            return actionResponse;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
