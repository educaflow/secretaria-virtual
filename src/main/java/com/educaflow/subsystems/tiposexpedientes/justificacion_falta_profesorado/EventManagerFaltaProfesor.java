package com.educaflow.subsystems.tiposexpedientes.justificacion_falta_profesorado;

import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.criptografia.AlmacenClaveDispositivo;
import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.pdf.CampoFirma;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.pdf.Rectangulo;
import com.educaflow.shared.expedientes.services.EventContext;
import com.educaflow.shared.expedientes.services.EventManager;
import com.educaflow.shared.expedientes.services.annotations.OnEnterState;
import com.educaflow.shared.expedientes.services.annotations.WhenEvent;
import com.educaflow.shared.expedientes.db.JustificacionFaltaProfesorado;
import com.educaflow.shared.expedientes.db.TipoResolucionJustificacionFaltaProfesorado;
import com.educaflow.shared.expedientes.db.repo.JustificacionFaltaProfesoradoRepository;

import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.shared.registroentradasalida.db.DatosRegistroEntrada;
import com.educaflow.shared.registroentradasalida.db.PersonaRegistro;
import com.educaflow.shared.registroentradasalida.db.RegistroEntrada;
import com.educaflow.shared.registroentradasalida.db.repo.RegistroEntradaRepository;
import com.google.inject.Inject;

import java.time.LocalDate;


public class EventManagerFaltaProfesor extends EventManager<JustificacionFaltaProfesorado, JustificacionFaltaProfesorado.State, JustificacionFaltaProfesorado.Event,JustificacionFaltaProfesorado.Profile> {

    private final JustificacionFaltaProfesoradoRepository repository;

    @Inject
    RegistroEntradaRepository registroEntradaRepository;

    @Inject
    public EventManagerFaltaProfesor(JustificacionFaltaProfesoradoRepository repository) {
        super(JustificacionFaltaProfesorado.class, JustificacionFaltaProfesorado.State.class, JustificacionFaltaProfesorado.Event.class,JustificacionFaltaProfesorado.Profile.class);
        this.repository = repository;
    }

    @Override
    public void triggerInitialEvent(JustificacionFaltaProfesorado justificacionFaltaProfesorado, EventContext eventContext) throws BusinessException {


        justificacionFaltaProfesorado.setAnyo(LocalDate.now().getYear());
        justificacionFaltaProfesorado.setNombre("Lorenzo");
        justificacionFaltaProfesorado.setApellidos("Acción García");
        justificacionFaltaProfesorado.setDni("12345678Z");

    }

    @WhenEvent
    public void triggerGuardarDatos(JustificacionFaltaProfesorado justificacionFaltaProfesorado, JustificacionFaltaProfesorado original, EventContext eventContext) throws BusinessException {
        DocumentoPdf solicitudPdf = justificacionFaltaProfesorado.getDocumentoPdf(JustificacionFaltaProfesorado.TipoDocumentoPdf.SOLICITUD);
        MetaFile pdfSolicitud = MetaFileHelper.createMetaFile(solicitudPdf);
        justificacionFaltaProfesorado.setPdfSolicitud(pdfSolicitud);
        justificacionFaltaProfesorado.setPdfSolicitudFirmado(null);

        justificacionFaltaProfesorado.updateState(JustificacionFaltaProfesorado.State.PENDIENTE_PRESENTACION);


    }
    @WhenEvent
    public void triggerPresentar(JustificacionFaltaProfesorado justificacionFaltaProfesorado, JustificacionFaltaProfesorado original, EventContext eventContext) throws BusinessException {
        RegistroEntrada registroEntrada=justificacionFaltaProfesorado.addRegistroEntrada(justificacionFaltaProfesorado.getPdfSolicitudFirmado());
        justificacionFaltaProfesorado.setPdfJustificanteRegistroEntrada(registroEntrada.getDocumentoResguardoPresentacion());

        justificacionFaltaProfesorado.updateState(JustificacionFaltaProfesorado.State.PENDIENTE_RESOLUCION);
        justificacionFaltaProfesorado.setDisconformidad(null);
        justificacionFaltaProfesorado.setResolucion(null);

    }

    @WhenEvent
    public void triggerResolver(JustificacionFaltaProfesorado justificacionFaltaProfesorado, JustificacionFaltaProfesorado original, EventContext eventContext)  throws BusinessException {
        TipoResolucionJustificacionFaltaProfesorado tipoResolucion = justificacionFaltaProfesorado.getTipoResolucion();

        switch (tipoResolucion) {
            case ACEPTAR:
                justificacionFaltaProfesorado.updateState(JustificacionFaltaProfesorado.State.ACEPTADO);
                break;
            case RECHAZAR:
                justificacionFaltaProfesorado.updateState(JustificacionFaltaProfesorado.State.RECHAZADO);
                break;
            case SUBSANAR_DATOS:
                justificacionFaltaProfesorado.updateState(JustificacionFaltaProfesorado.State.ENTRADA_DATOS);
                break;
            default:
                throw new IllegalArgumentException("Tipo de resolución no reconocido: " + tipoResolucion);
        }
    }


    @WhenEvent
    public void triggerBack(JustificacionFaltaProfesorado justificacionFaltaProfesorado, JustificacionFaltaProfesorado original, EventContext eventContext)  throws BusinessException {
            JustificacionFaltaProfesorado.State state=JustificacionFaltaProfesorado.State.valueOf(justificacionFaltaProfesorado.getCodeState());

            switch (state) {
                case ENTRADA_DATOS:
                    justificacionFaltaProfesorado.updateState( JustificacionFaltaProfesorado.State.ENTRADA_DATOS);
                    break;
                case PENDIENTE_PRESENTACION:
                    justificacionFaltaProfesorado.updateState( JustificacionFaltaProfesorado.State.ENTRADA_DATOS);
                    break;
                case PENDIENTE_RESOLUCION:
                    justificacionFaltaProfesorado.updateState( JustificacionFaltaProfesorado.State.ENTRADA_DATOS);
                    break;
                case ACEPTADO:
                    justificacionFaltaProfesorado.updateState( JustificacionFaltaProfesorado.State.PENDIENTE_PRESENTACION);
                    break;
                case RECHAZADO:
                    justificacionFaltaProfesorado.updateState( JustificacionFaltaProfesorado.State.PENDIENTE_PRESENTACION);
                    break;
                default:
                    throw new IllegalArgumentException("State no reconocido: " + state);
            }

    }


    @WhenEvent
    public void triggerDelete(JustificacionFaltaProfesorado justificacionFaltaProfesorado, JustificacionFaltaProfesorado original, EventContext eventContext) throws BusinessException {
    }


    @OnEnterState
    public void onEnterEntradaDatos(JustificacionFaltaProfesorado justificacionFaltaProfesorado, EventContext eventContext) {
    }


    @OnEnterState
    public void onEnterPendientePresentacion(JustificacionFaltaProfesorado justificacionFaltaProfesorado, EventContext eventContext) {
    }

    @OnEnterState
    public void onEnterPendienteResolucion(JustificacionFaltaProfesorado justificacionFaltaProfesorado, EventContext eventContext) {
    }


    @OnEnterState
    public void onEnterAceptado(JustificacionFaltaProfesorado justificacionFaltaProfesorado, EventContext eventContext) {
    }

    @OnEnterState
    public void onEnterRechazado(JustificacionFaltaProfesorado justificacionFaltaProfesorado, EventContext eventContext) {
    }





}