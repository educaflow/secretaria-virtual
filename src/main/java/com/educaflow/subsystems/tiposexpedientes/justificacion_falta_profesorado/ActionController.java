package com.educaflow.subsystems.tiposexpedientes.justificacion_falta_profesorado;

import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.shared.expedientes.db.JustificacionFaltaProfesorado;
import com.educaflow.base.infrastructure.autofirma.AutoFirma;
import com.educaflow.base.infrastructure.pdf.Rectangulo;

public class ActionController {

    @CallMethod
    public void firmarDocumentacionParaPresentar(ActionRequest actionRequest, ActionResponse actionResponse) {

        AutoFirma autofirma = (new AutoFirma(JustificacionFaltaProfesorado.class))
            .setRectangulo(new Rectangulo(100,20,600,100))
            .setPageNumber(1)
            .setSourceField("pdfSolicitud")
            .setTargetField("pdfSolicitudFirmado");

        AutoFirma.sendToActionResponse(autofirma,actionResponse);
    }
}
