package com.educaflow.system.tiposexpedientes.justificacion_falta_profesorado;

import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesorado;
import com.educaflow.base.infrastructure.autofirma.AutoFirma;
import com.educaflow.base.infrastructure.pdf.Rectangulo;
import com.educaflow.subsystem.expedientes.services.internal.ExpedienteUtil;

public class ActionController {

    private static final Rectangulo rectanguloPosicionFirmaPDFJustificacionFaltaProfesorado=new Rectangulo(100,20,600,100);

    @CallMethod
    public void firmarDocumentacionParaPresentar(ActionRequest actionRequest, ActionResponse actionResponse) {
        ActionRequestHelper actionRequestHelper = new ActionRequestHelper(actionRequest,JustificacionFaltaProfesorado.class);
        JustificacionFaltaProfesorado justificacionFaltaProfesorado = (JustificacionFaltaProfesorado)ExpedienteUtil.getExpedienteFromIdExpediente(actionRequestHelper.getId());

        AutoFirma autofirma = (new AutoFirma(JustificacionFaltaProfesorado.class))
            .setRectangulo(rectanguloPosicionFirmaPDFJustificacionFaltaProfesorado)
            .setPageNumber(1)
            .addSourceTargetField("pdfSolicitud","pdfSolicitudFirmado")
            .setNif(justificacionFaltaProfesorado.getPersonaSolicitante().getDni());

        AutoFirma.sendToActionResponse(autofirma,actionResponse);
    }
}
