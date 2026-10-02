package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.verificacion;

import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.util.Convert;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1;
import com.educaflow.subsystem.expedientes.db.ResultadoVerificacionAnulacionMatriculaCicloFormativoV1;
import com.educaflow.subsystem.expedientes.db.repo.AnulacionMatriculaCicloFormativoV1Repository;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.EventContext;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.OnEnterState;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.PhaseEventManager;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.WhenEvent;
import com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.AnulacionMatriculaCicloFormativoV1Util;
import com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.States;
import com.educaflow.tramites.util.verificacion.VerificacionHelper;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;


public class PhaseEventManagerImpl extends PhaseEventManager<AnulacionMatriculaCicloFormativoV1> {

    private final AnulacionMatriculaCicloFormativoV1Repository repository;
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    VerificacionHelper verificacionHelper;

    @Inject
    public PhaseEventManagerImpl(AnulacionMatriculaCicloFormativoV1Repository repository) {
        super(AnulacionMatriculaCicloFormativoV1.class);
        this.repository = repository;
    }


    /**
     * La secretaría comprueba la solicitud: si está correcta pasa a resolverla; si no, vuelve al alumno para
     * que la subsane y la presente otra vez. Aquí no se acepta ni se rechaza la anulación.
     */
    @WhenEvent
    public void triggerVerificar(AnulacionMatriculaCicloFormativoV1 expediente, AnulacionMatriculaCicloFormativoV1 original, EventContext eventContext) throws BusinessException {
        AnulacionMatriculaCicloFormativoV1Util.exigePertenecerAlCentroDelExpediente(expediente, "Solo puede verificar solicitudes de su propio centro");
        AnulacionMatriculaCicloFormativoV1Util.exigeOstentarElPerfilDelEstado(expediente, "Solo la secretaría del centro puede verificar esta solicitud");

        expediente.setFechaVerificacion(LocalDate.now(Convert.defaultZoneId));
        expediente.setVerificadoPor(SecurityUtil.getUser());

        ResultadoVerificacionAnulacionMatriculaCicloFormativoV1 resultadoVerificacion = expediente.getResultadoVerificacion();
        switch (resultadoVerificacion) {
            case CORRECTO -> {
                expediente.setTextoSubsanacion(null);
                eventContext.updateState(States.Resolucion.PENDIENTE_RESOLUCION);
            }
            case SUBSANAR -> {
                verificacionHelper.avisarDeSubsanacion(expediente, expediente.getTextoSubsanacion());

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
    public void onEnterPendienteVerificacion(AnulacionMatriculaCicloFormativoV1 expediente, EventContext eventContext) {

    }

}
