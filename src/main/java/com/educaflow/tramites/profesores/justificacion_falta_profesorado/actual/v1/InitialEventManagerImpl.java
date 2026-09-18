package com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1;

import com.educaflow.subsystem.expedientes.tramitacion.eventmanager.InitialEventContext;
import com.educaflow.subsystem.expedientes.tramitacion.eventmanager.InitialEventManager;
import com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesoradoV1;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;

import java.time.LocalDate;
import com.educaflow.base.util.Convert;


/**
 * Rellena los datos iniciales de un expediente de JustificacionFaltaProfesoradoV1 recién creado.
 *
 * <p>El evento inicial es del <b>tipo de expediente</b>, no de una fase: se dispara cuando todavía
 * no hay estado del que partir. Por eso hay exactamente uno por tipo, aquí en la raíz de la
 * versión, y no uno por fase.
 *
 * <p>MUST fijar el estado en el que nace el expediente con {@code initialEventContext.updateState(...)}:
 * el estado inicial no se declara en el {@code TipoExpedienteInstance.xml}, lo decide esta clase.
 *
 * <p>Qué campos hay que rellenar depende del tipo de expediente: {@code Tramitador} no impone
 * ninguno.
 */
public class InitialEventManagerImpl implements InitialEventManager<JustificacionFaltaProfesoradoV1> {

    @Override
    public void triggerInitialEvent(InitialEventContext<JustificacionFaltaProfesoradoV1> initialEventContext) throws BusinessException {
        initialEventContext.getExpediente().setAnyo(LocalDate.now(Convert.defaultZoneId).getYear());

        initialEventContext.updateState(States.Recepcion.ENTRADA_DATOS);
    }

}
