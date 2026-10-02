package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.resolucion;

import com.axelor.auth.db.User;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.i18n.I18n;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.util.Convert;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.subsystem.common.service.DirectorCentroService;
import com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.subsystem.expedientes.db.TipoResolucionAnulacionMatriculaCicloFormativoV1;
import com.educaflow.subsystem.expedientes.db.repo.AnulacionMatriculaCicloFormativoV1Repository;
import com.educaflow.subsystem.expedientes.util.ExpedienteNotasUtil;
import com.educaflow.subsystem.firmas.db.EstadoTareaFirma;
import com.educaflow.subsystem.firmas.db.TareaFirma;
import com.educaflow.subsystem.firmas.service.TareaFirmaInsertDTO;
import com.educaflow.subsystem.firmas.service.TareaFirmaNotifier;
import com.educaflow.subsystem.firmas.service.TareaFirmaService;
import com.educaflow.subsystem.registroentradasalida.db.RegistroSalida;
import com.educaflow.subsystem.tramitador.service.TramitadorService;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.EventContext;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.OnEnterState;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.PhaseEventManager;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.WhenEvent;
import com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.AnulacionMatriculaCicloFormativoV1Util;
import com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.States;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;


/**
 * La secretaría propone la resolución y el director la firma en su bandeja de firmas. Por eso esta clase es
 * además el {@link TareaFirmaNotifier} de esa tarea de firma: cuando el director la firma o la rechaza, el
 * subsistema de firmas llama a {@link #notify} y este dispara el evento de sistema que mueve el expediente.
 */
public class PhaseEventManagerImpl extends PhaseEventManager<AnulacionMatriculaCicloFormativoV1> implements TareaFirmaNotifier {

    // El campoFirma del hueco que documentospdf/resolucion.xml deja bajo «El director / la directora».
    private static final String CAMPO_FIRMA_RESOLUCION = "firmaDirector";

    // Los dos eventos de sistema (systemEvents) del estado PENDIENTE_FIRMA_DIRECTOR.
    private static final String EVENTO_FIRMAR = "FIRMAR";
    private static final String EVENTO_RECHAZAR_FIRMA = "RECHAZAR_FIRMA";

    private final AnulacionMatriculaCicloFormativoV1Repository repository;
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    ModelServiceFactory modelServiceFactory;

    @Inject
    DirectorCentroService directorCentroService;

    @Inject
    TramitadorService tramitadorService;

    @Inject
    public PhaseEventManagerImpl(AnulacionMatriculaCicloFormativoV1Repository repository) {
        super(AnulacionMatriculaCicloFormativoV1.class);
        this.repository = repository;
    }


    /** La secretaría vuelve a la verificación: todavía no ha enviado nada a la firma. */
    @WhenEvent
    public void triggerBack(AnulacionMatriculaCicloFormativoV1 expediente, AnulacionMatriculaCicloFormativoV1 original, EventContext eventContext) throws BusinessException {
        AnulacionMatriculaCicloFormativoV1Util.exigePertenecerAlCentroDelExpediente(expediente, "Solo puede resolver solicitudes de su propio centro");
        AnulacionMatriculaCicloFormativoV1Util.exigeOstentarElPerfilDelEstado(expediente, "Solo la secretaría del centro puede resolver esta solicitud");

        eventContext.updateState(States.Verificacion.PENDIENTE_VERIFICACION);
    }

    /** La secretaría acepta o rechaza la anulación: se genera la resolución y se le pone a firmar al director. */
    @WhenEvent
    public void triggerResolver(AnulacionMatriculaCicloFormativoV1 expediente, AnulacionMatriculaCicloFormativoV1 original, EventContext eventContext) throws BusinessException {
        AnulacionMatriculaCicloFormativoV1Util.exigePertenecerAlCentroDelExpediente(expediente, "Solo puede resolver solicitudes de su propio centro");
        AnulacionMatriculaCicloFormativoV1Util.exigeOstentarElPerfilDelEstado(expediente, "Solo la secretaría del centro puede resolver esta solicitud");
        User director = directorCentroService.getDirector(expediente.getCentro());

        if (expediente.getTipoResolucion() != TipoResolucionAnulacionMatriculaCicloFormativoV1.RECHAZAR) {
            expediente.setMotivoRechazo(null);
        }
        expediente.setPdfResolucionFirmada(null);

        expediente.setFechaResolucion(LocalDate.now(Convert.defaultZoneId));

        DocumentoPdf resolucionPdf = expediente.getDocumentoPdf(AnulacionMatriculaCicloFormativoV1.TipoDocumentoPdf.RESOLUCION);
        expediente.setPdfResolucion(MetaFileHelper.createMetaFile(resolucionPdf));

        TareaFirmaService tareaFirmaService = (TareaFirmaService) modelServiceFactory.resolve(TareaFirma.class);
        tareaFirmaService.insert(new TareaFirmaInsertDTO(
                director,
                expediente.getCentro(),
                List.of(expediente.getPdfResolucion()),
                I18n.get("Resolución de la solicitud de anulación de matrícula del expediente %s").formatted(expediente.getNumeroExpediente()),
                CAMPO_FIRMA_RESOLUCION,
                PhaseEventManagerImpl.class,
                expediente.getId()));

        eventContext.updateState(States.Resolucion.PENDIENTE_FIRMA_DIRECTOR);
    }

    /**
     * Evento de sistema: el director ha firmado la resolución en su bandeja de firmas. La resolución firmada
     * llega ya copiada en el expediente, del {@code requestData} de {@link #notify}. Se registra de salida y
     * el expediente se cierra.
     */
    @WhenEvent
    public void triggerFirmar(AnulacionMatriculaCicloFormativoV1 expediente, AnulacionMatriculaCicloFormativoV1 original, EventContext eventContext) throws BusinessException {
        // El documento que llega es el de la tarea de firma, no del expediente: se registra de salida una copia.
        MetaFile resolucionFirmada = MetaFileUtil.cloneMetaFile(expediente.getPdfResolucionFirmada());
        RegistroSalida registroSalida = eventContext.createRegistroSalida(resolucionFirmada, List.of());
        expediente.setPdfResolucionFirmada(registroSalida.getDocumento());

        expediente.setPdfResolucion(null);

        TipoResolucionAnulacionMatriculaCicloFormativoV1 tipoResolucion = expediente.getTipoResolucion();
        switch (tipoResolucion) {
            case ACEPTAR -> eventContext.updateState(States.Resolucion.ACEPTADA);
            case RECHAZAR -> eventContext.updateState(States.Resolucion.RECHAZADA);
            case null -> throw new IllegalStateException("El expediente " + expediente.getNumeroExpediente() + " no tiene tipo de resolución");
        }
    }

    /**
     * Evento de sistema: el director ha rechazado firmar la resolución. El expediente vuelve a la secretaría
     * para que decida otra vez; el motivo ya está en las notas del expediente, donde lo deja {@link #notify}.
     */
    @WhenEvent
    public void triggerRechazarFirma(AnulacionMatriculaCicloFormativoV1 expediente, AnulacionMatriculaCicloFormativoV1 original, EventContext eventContext) throws BusinessException {
        expediente.setPdfResolucion(null);

        eventContext.updateState(States.Resolucion.PENDIENTE_RESOLUCION);
    }

    /**
     * El director ha firmado o rechazado la resolución en su bandeja de firmas. Hace de controlador del
     * tramitador: de la tarea de firma saca el expediente ({@code callBackData} es su id, que es lo que
     * {@link #triggerResolver} guardó en la tarea al crearla), el evento de sistema y el {@code requestData}, y
     * dispara el evento.
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
            tramitadorService.triggerEvent(expediente, evento, requestData, new EventContext(expediente, Profile.DIRECTOR, modelServiceFactory));
        } catch (BusinessException ex) {
            throw new IllegalStateException("No se ha podido disparar el evento " + evento + " del expediente "
                    + expediente.getNumeroExpediente() + ": " + ex.getBusinessMessages(), ex);
        }
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
            motivoRechazoFirma = I18n.get("El director ha rechazado firmar la resolución sin indicar el motivo");
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
    public void onEnterPendienteResolucion(AnulacionMatriculaCicloFormativoV1 expediente, EventContext eventContext) {

    }

    @OnEnterState
    public void onEnterPendienteFirmaDirector(AnulacionMatriculaCicloFormativoV1 expediente, EventContext eventContext) {

    }

    @OnEnterState
    public void onEnterAceptada(AnulacionMatriculaCicloFormativoV1 expediente, EventContext eventContext) {

    }

    @OnEnterState
    public void onEnterRechazada(AnulacionMatriculaCicloFormativoV1 expediente, EventContext eventContext) {

    }

}
