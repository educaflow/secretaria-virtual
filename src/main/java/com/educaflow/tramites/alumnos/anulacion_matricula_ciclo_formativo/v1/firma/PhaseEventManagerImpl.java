package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.firma;

import com.axelor.auth.db.User;
import com.axelor.auth.service.UserService;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.i18n.I18n;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.subsystem.common.db.CargoCodigo;
import com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1;
import com.educaflow.subsystem.expedientes.db.TipoResolucionAnulacionMatriculaCicloFormativoV1;
import com.educaflow.subsystem.expedientes.db.repo.AnulacionMatriculaCicloFormativoV1Repository;
import com.educaflow.subsystem.expedientes.util.ExpedienteNotasUtil;
import com.educaflow.subsystem.firmas.db.EstadoTareaFirma;
import com.educaflow.subsystem.firmas.db.TareaFirma;
import com.educaflow.subsystem.firmas.service.TareaFirmaNotifier;
import com.educaflow.subsystem.registroentradasalida.db.RegistroSalida;
import com.educaflow.subsystem.tramitador.service.TramitadorService;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.EventContext;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.OnEnterState;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.PhaseEventManager;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.State;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.WhenEvent;
import com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.AnulacionMatriculaCicloFormativoV1Util;
import com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.States;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;


/**
 * La resolución la firman, en este orden, el secretario y el director, cada uno en su bandeja de firmas. Por eso
 * esta clase es además el {@link TareaFirmaNotifier} de las dos tareas de firma: cuando el firmante firma o
 * rechaza, el subsistema de firmas llama a {@link #notify} y este dispara el evento de sistema que mueve el
 * expediente. Los dos estados de espera comparten los eventos {@code FIRMAR} y {@code RECHAZAR_FIRMA}.
 */
public class PhaseEventManagerImpl extends PhaseEventManager<AnulacionMatriculaCicloFormativoV1> implements TareaFirmaNotifier {

    // Los dos eventos de sistema (systemEvents) de PENDIENTE_FIRMA_SECRETARIO y PENDIENTE_FIRMA_DIRECTOR.
    private static final String EVENTO_FIRMAR = "FIRMAR";
    private static final String EVENTO_RECHAZAR_FIRMA = "RECHAZAR_FIRMA";

    private final AnulacionMatriculaCicloFormativoV1Repository repository;
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    ModelServiceFactory modelServiceFactory;

    @Inject
    TramitadorService tramitadorService;

    @Inject
    public PhaseEventManagerImpl(AnulacionMatriculaCicloFormativoV1Repository repository) {
        super(AnulacionMatriculaCicloFormativoV1.class);
        this.repository = repository;
    }


    /**
     * Evento de sistema: el firmante del estado actual ha firmado la resolución en su bandeja de firmas. La
     * resolución firmada llega ya copiada en el expediente, del {@code requestData} de {@link #notify}. Si ha
     * firmado el secretario, se le pone a firmar al director; si ha firmado el director, se registra de salida y
     * el expediente se cierra.
     */
    @WhenEvent
    public void triggerFirmar(AnulacionMatriculaCicloFormativoV1 expediente, AnulacionMatriculaCicloFormativoV1 original, EventContext eventContext) throws BusinessException {
        State estado = getEstado(original);

        switch (estado) {
            case States.Firma.PENDIENTE_FIRMA_SECRETARIO -> ponerAFirmarAlDirector(expediente, eventContext);
            case States.Firma.PENDIENTE_FIRMA_DIRECTOR -> registrarDeSalidaYCerrar(expediente, eventContext);
            default -> throw new IllegalStateException("El evento " + EVENTO_FIRMAR + " no se espera en el estado '"
                    + estado.getPhase().getCode() + "/" + estado.getCode() + "' del expediente " + expediente.getNumeroExpediente());
        }
    }

    /**
     * Evento de sistema: el secretario o el director ha rechazado firmar la resolución. El expediente vuelve a la
     * secretaría para que decida otra vez; el motivo ya está en las notas del expediente, donde lo deja
     * {@link #notify}.
     */
    @WhenEvent
    public void triggerRechazarFirma(AnulacionMatriculaCicloFormativoV1 expediente, AnulacionMatriculaCicloFormativoV1 original, EventContext eventContext) throws BusinessException {
        expediente.setPdfResolucion(null);

        eventContext.updateState(States.Resolucion.PENDIENTE_RESOLUCION);
    }

    private void ponerAFirmarAlDirector(AnulacionMatriculaCicloFormativoV1 expediente, EventContext eventContext) throws BusinessException {
        UserService userService = (UserService) modelServiceFactory.resolve(User.class);
        User director = userService.getByCentroAndCargo(expediente.getCentro(), CargoCodigo.DIRECTOR);

        // El documento que llega es el de la tarea de firma del secretario, no del expediente: el director firma una
        // copia, que conserva vacío su hueco de firma.
        expediente.setPdfResolucion(MetaFileUtil.cloneMetaFile(expediente.getPdfResolucionFirmada()));
        expediente.setPdfResolucionFirmada(null);

        AnulacionMatriculaCicloFormativoV1Util.ponerResolucionAFirmar(expediente, director, AnulacionMatriculaCicloFormativoV1Util.CAMPO_FIRMA_DIRECTOR);

        eventContext.updateState(States.Firma.PENDIENTE_FIRMA_DIRECTOR);
    }

    private static void registrarDeSalidaYCerrar(AnulacionMatriculaCicloFormativoV1 expediente, EventContext eventContext) throws BusinessException {
        // El documento que llega es el de la tarea de firma, no del expediente: se registra de salida una copia.
        MetaFile resolucionFirmada = MetaFileUtil.cloneMetaFile(expediente.getPdfResolucionFirmada());
        RegistroSalida registroSalida = eventContext.createRegistroSalida(resolucionFirmada, List.of());
        expediente.setPdfResolucionFirmada(registroSalida.getDocumento());

        expediente.setPdfResolucion(null);

        TipoResolucionAnulacionMatriculaCicloFormativoV1 tipoResolucion = expediente.getTipoResolucion();
        switch (tipoResolucion) {
            case ACEPTAR -> eventContext.updateState(States.Cierre.ACEPTADA);
            case RECHAZAR -> eventContext.updateState(States.Cierre.RECHAZADA);
            case null -> throw new IllegalStateException("El expediente " + expediente.getNumeroExpediente() + " no tiene tipo de resolución");
        }
    }

    /**
     * El secretario o el director ha firmado o rechazado la resolución en su bandeja de firmas. Hace de controlador
     * del tramitador: de la tarea de firma saca el expediente ({@code callBackData} es su id, que es lo que
     * {@link AnulacionMatriculaCicloFormativoV1Util#ponerResolucionAFirmar} guardó en la tarea al crearla), el
     * evento de sistema y el {@code requestData}, y dispara el evento con el perfil del estado en que espera.
     */
    @Override
    public void notify(TareaFirma tareaFirma, Object callBackData) {
        AnulacionMatriculaCicloFormativoV1 expediente = getExpedienteDeLaTareaFirma(tareaFirma, callBackData);
        String evento = getEventoDeSistema(tareaFirma);
        Map<String, Object> requestData = getRequestData(tareaFirma);

        if (tareaFirma.getEstadoTareaFirma() == EstadoTareaFirma.RECHAZADO) {
            ExpedienteNotasUtil.addNote(expediente, I18n.get("Rechazo firmar la resolución: %s").formatted(getMotivoRechazoFirma(tareaFirma)));
        }

        try {
            tramitadorService.triggerEvent(expediente, evento, requestData, new EventContext(expediente, getEstado(expediente).getProfile(), modelServiceFactory));
        } catch (BusinessException ex) {
            throw new IllegalStateException("No se ha podido disparar el evento " + evento + " del expediente "
                    + expediente.getNumeroExpediente() + ": " + ex.getBusinessMessages(), ex);
        }
    }

    private static State getEstado(AnulacionMatriculaCicloFormativoV1 expediente) {
        return States.INSTANCE.getState(expediente.getCodePhase(), expediente.getCodeState())
                .orElseThrow(() -> new IllegalStateException("El expediente " + expediente.getNumeroExpediente() + " está en el estado '"
                        + expediente.getCodePhase() + "/" + expediente.getCodeState() + "', que no existe en este tipo de expediente."));
    }

    private AnulacionMatriculaCicloFormativoV1 getExpedienteDeLaTareaFirma(TareaFirma tareaFirma, Object callBackData) {
        AnulacionMatriculaCicloFormativoV1 expediente = repository.find(((Number) callBackData).longValue());
        if (expediente == null) {
            throw new IllegalStateException("No existe el expediente id=" + callBackData + " de la tarea de firma id=" + tareaFirma.getId());
        }

        return expediente;
    }

    /**
     * Lo que el evento de sistema necesita de la tarea de firma, con la forma del {@code requestData} de una
     * petición: cada referencia es un mapa con su id. Solo entra en el expediente lo que declara el validador
     * de ese evento.
     */
    private static Map<String, Object> getRequestData(TareaFirma tareaFirma) {
        if (tareaFirma.getEstadoTareaFirma() != EstadoTareaFirma.FIRMADO) {
            return Map.of();
        }

        MetaFile resolucionFirmada = tareaFirma.getDocumentosFirma().get(0).getDocumentoFirmado();

        return Map.of("pdfResolucionFirmada", Map.of("id", resolucionFirmada.getId()));
    }

    private static String getMotivoRechazoFirma(TareaFirma tareaFirma) {
        String motivoRechazoFirma = tareaFirma.getMotivoRechazo();
        if (motivoRechazoFirma == null || motivoRechazoFirma.isBlank()) {
            motivoRechazoFirma = I18n.get("Se ha rechazado firmar la resolución sin indicar el motivo");
        }

        return motivoRechazoFirma;
    }

    private static String getEventoDeSistema(TareaFirma tareaFirma) {
        return switch (tareaFirma.getEstadoTareaFirma()) {
            case FIRMADO -> EVENTO_FIRMAR;
            case RECHAZADO -> EVENTO_RECHAZAR_FIRMA;
            case PENDIENTE -> throw new IllegalStateException("La tarea de firma id=" + tareaFirma.getId() + " sigue pendiente");
        };
    }


/***************************************************************************************/
/*************************************** Estados ***************************************/
/***************************************************************************************/

    @OnEnterState
    public void onEnterPendienteFirmaSecretario(AnulacionMatriculaCicloFormativoV1 expediente, EventContext eventContext) {

    }

    @OnEnterState
    public void onEnterPendienteFirmaDirector(AnulacionMatriculaCicloFormativoV1 expediente, EventContext eventContext) {

    }

}
