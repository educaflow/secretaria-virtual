package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.cierre;

import com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1;
import com.educaflow.subsystem.expedientes.db.repo.AnulacionMatriculaCicloFormativoV1Repository;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.EventContext;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.OnEnterState;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.PhaseEventManager;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** Los estados finales: la anulación aceptada o rechazada, con la resolución ya firmada y registrada de salida. */
public class PhaseEventManagerImpl extends PhaseEventManager<AnulacionMatriculaCicloFormativoV1> {

    private final AnulacionMatriculaCicloFormativoV1Repository repository;
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    public PhaseEventManagerImpl(AnulacionMatriculaCicloFormativoV1Repository repository) {
        super(AnulacionMatriculaCicloFormativoV1.class);
        this.repository = repository;
    }


/***************************************************************************************/
/*************************************** Estados ***************************************/
/***************************************************************************************/

    @OnEnterState
    public void onEnterAceptada(AnulacionMatriculaCicloFormativoV1 expediente, EventContext eventContext) {

    }

    @OnEnterState
    public void onEnterRechazada(AnulacionMatriculaCicloFormativoV1 expediente, EventContext eventContext) {

    }

}
