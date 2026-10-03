package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1;

import com.axelor.i18n.I18n;
import com.educaflow.subsystem.common.db.Centro;
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
        if (curso == null || curso == 0) {
            throw new BusinessException(I18n.get("El centro no tiene configurado el curso académico"));
        }
        expediente.setCursoAcademico(curso + "/" + (curso + 1));

        // En papel se empieza adjuntando la solicitud escaneada y después se copian sus datos.
        if (expediente.getPresentadoEnPapel()==true) {
            initialEventContext.updateState(States.Entrada.PENDIENTE_DOCUMENTO_ESCANEADO);
        } else {
            initialEventContext.updateState(States.Entrada.ENTRADA_DATOS);
        }
    }

}
