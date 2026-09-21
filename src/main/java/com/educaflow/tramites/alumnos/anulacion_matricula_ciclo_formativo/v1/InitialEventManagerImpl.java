package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1;

import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.Municipio;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.InitialEventContext;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.InitialEventManager;
import com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;


public class InitialEventManagerImpl implements InitialEventManager<AnulacionMatriculaCicloFormativoV1> {

    @Override
    public void triggerInitialEvent(InitialEventContext<AnulacionMatriculaCicloFormativoV1> initialEventContext) throws BusinessException {
        AnulacionMatriculaCicloFormativoV1 expediente = initialEventContext.getExpediente();
        Centro centro = expediente.getCentro();
        Integer curso = centro.getCurso();
        expediente.setCursoAcademico(curso == null ? null : curso + "/" + (curso + 1));

        expediente.setNombreCentro(centro.getName());

        Municipio municipio = centro.getMunicipio();
        expediente.setLocalidadCentro(municipio == null ? null : municipio.getName());

        // En papel se empieza adjuntando la solicitud escaneada y después se copian sus datos.
        if (expediente.getPresentadoEnPapel()==true) {
            initialEventContext.updateState(States.Solicitud.PENDIENTE_DOCUMENTO_ESCANEADO);
        } else {
            initialEventContext.updateState(States.Solicitud.DATOS_SOLICITUD);
        }
    }

}
