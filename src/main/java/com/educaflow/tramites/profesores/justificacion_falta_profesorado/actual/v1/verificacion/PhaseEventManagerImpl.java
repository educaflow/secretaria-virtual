package com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.verificacion;

import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesoradoV1;
import com.educaflow.subsystem.expedientes.db.ResultadoVerificacionJustificacionFaltaProfesoradoV1;
import com.educaflow.subsystem.expedientes.db.repo.JustificacionFaltaProfesoradoV1Repository;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.EventContext;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.OnEnterState;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.PhaseEventManager;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.WhenEvent;
import com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.States;
import com.educaflow.tramites.util.verificacion.VerificacionHelper;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PhaseEventManagerImpl extends PhaseEventManager<JustificacionFaltaProfesoradoV1> {

    private final JustificacionFaltaProfesoradoV1Repository repository;
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    VerificacionHelper verificacionHelper;

    @Inject
    public PhaseEventManagerImpl(JustificacionFaltaProfesoradoV1Repository repository) {
        super(JustificacionFaltaProfesoradoV1.class);
        this.repository = repository;
    }


    /**
     * La jefatura de estudios comprueba la solicitud: si está correcta pasa a la dirección, que es quien la
     * resuelve; si no, vuelve al profesor para que la subsane y la presente otra vez.
     */
    @WhenEvent
    public void triggerVerificar(JustificacionFaltaProfesoradoV1 justificacionFaltaProfesorado, JustificacionFaltaProfesoradoV1 original, EventContext eventContext) throws BusinessException {
        // Lo que la dirección devolvió queda atendido con esta verificación, sea cual sea su resultado.
        justificacionFaltaProfesorado.setMotivoDevolucion(null);

        ResultadoVerificacionJustificacionFaltaProfesoradoV1 resultadoVerificacion = justificacionFaltaProfesorado.getResultadoVerificacion();
        switch (resultadoVerificacion) {
            case CORRECTO -> {
                justificacionFaltaProfesorado.setTextoSubsanacion(null);
                eventContext.updateState(States.Resolucion.PENDIENTE_RESOLUCION);
            }
            case SUBSANAR -> {
                verificacionHelper.avisarDeSubsanacion(justificacionFaltaProfesorado, justificacionFaltaProfesorado.getTextoSubsanacion());

                // En papel la persona entregará una solicitud nueva: se vuelve a empezar por el escaneado.
                if (Boolean.TRUE.equals(original.getPresentadoEnPapel())) {
                    eventContext.updateState(States.Entrada.PENDIENTE_DOCUMENTO_ESCANEADO);
                } else {
                    eventContext.updateState(States.Entrada.ENTRADA_DATOS);
                }
            }
            case null -> throw new IllegalArgumentException("Resultado de la verificación no reconocido: " + resultadoVerificacion);
        }
    }


/***************************************************************************************/
/*************************************** Estados ***************************************/
/***************************************************************************************/

    @OnEnterState
    public void onEnterPendienteVerificacion(JustificacionFaltaProfesoradoV1 justificacionFaltaProfesorado, EventContext eventContext) {

    }

}
