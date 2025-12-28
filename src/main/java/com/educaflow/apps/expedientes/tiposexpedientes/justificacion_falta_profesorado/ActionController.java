package com.educaflow.apps.expedientes.tiposexpedientes.justificacion_falta_profesorado;

import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.apps.expedientes.db.JustificacionFaltaProfesorado;
import com.educaflow.apps.expedientes.tiposexpedientes.shared.AutoFirma;
import com.educaflow.common.pdf.Rectangulo;

public class ActionController {

    @CallMethod
    public void firmarDocumentacionParaPresentar(ActionRequest actionRequest, ActionResponse actionResponse) {

        AutoFirma autofirma = (new AutoFirma(JustificacionFaltaProfesorado.class))
            .setRectangulo(new Rectangulo(300,10,120,100))
            .setPageNumber(1)
            .setMotivo("Registrar documentación presentada por el usuario")
            .setSourceField("documentacionParaPresentarSinFirmar")
            .setTargetField("documentacionPresentadaFirmadaUsuario");

        AutoFirma.sendToActionResponse(autofirma,actionResponse);
    }
}
