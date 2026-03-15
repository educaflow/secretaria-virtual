package com.educaflow.system.tiposexpedientes.justificacion_falta_profesorado;

import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.pdf.CampoFirma;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.pdf.Rectangulo;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.certificados.AlmacenClaveLoader;
import com.educaflow.subsystem.common.db.Persona;
import com.educaflow.subsystem.expedientes.services.EventContext;
import com.educaflow.subsystem.expedientes.services.EventManager;
import com.educaflow.subsystem.expedientes.services.annotations.OnEnterState;
import com.educaflow.subsystem.expedientes.services.annotations.WhenEvent;
import com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesorado;
import com.educaflow.subsystem.expedientes.db.TipoResolucionJustificacionFaltaProfesorado;
import com.educaflow.subsystem.expedientes.db.repo.JustificacionFaltaProfesoradoRepository;

import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.subsystem.firma.db.Firma;
import com.educaflow.subsystem.firmas.service.DatosFirma;
import com.educaflow.subsystem.firmas.service.FirmaNotifier;
import com.educaflow.subsystem.firmas.service.FirmaService;
import com.educaflow.subsystem.registroentradasalida.db.RegistroEntrada;
import com.educaflow.subsystem.registroentradasalida.db.RegistroSalida;
import com.educaflow.subsystem.registroentradasalida.db.repo.RegistroEntradaRepository;
import com.google.inject.Inject;

import java.time.LocalDate;
import java.util.List;


public class EventManagerFaltaProfesor extends EventManager<JustificacionFaltaProfesorado, JustificacionFaltaProfesorado.State, JustificacionFaltaProfesorado.Event,JustificacionFaltaProfesorado.Profile> implements FirmaNotifier{

    private final JustificacionFaltaProfesoradoRepository repository;

    @Inject
    RegistroEntradaRepository registroEntradaRepository;
    @Inject
    AlmacenClaveLoader almacenClaveLoader;

    @Inject
    FirmaService firmaService;

    @Inject
    public EventManagerFaltaProfesor(JustificacionFaltaProfesoradoRepository repository) {
        super(JustificacionFaltaProfesorado.class, JustificacionFaltaProfesorado.State.class, JustificacionFaltaProfesorado.Event.class,JustificacionFaltaProfesorado.Profile.class);
        this.repository = repository;
    }

    @Override
    public void triggerInitialEvent(JustificacionFaltaProfesorado justificacionFaltaProfesorado, EventContext eventContext) throws BusinessException {


        justificacionFaltaProfesorado.setAnyo(LocalDate.now().getYear());
        Persona persona=new Persona();
        persona.setNombre(justificacionFaltaProfesorado.getCreador().getNombre());
        persona.setApellidos(justificacionFaltaProfesorado.getCreador().getApellidos());
        persona.setDni(justificacionFaltaProfesorado.getCreador().getDni());
        justificacionFaltaProfesorado.setPersonaInteresada(persona);
        justificacionFaltaProfesorado.setPersonaSolicitante(persona);

    }

    @WhenEvent
    public void triggerGuardarDatos(JustificacionFaltaProfesorado justificacionFaltaProfesorado, JustificacionFaltaProfesorado original, EventContext eventContext) throws BusinessException {
        DocumentoPdf solicitudPdf = justificacionFaltaProfesorado.getDocumentoPdf(JustificacionFaltaProfesorado.TipoDocumentoPdf.SOLICITUD);
        MetaFile pdfSolicitud = MetaFileHelper.createMetaFile(solicitudPdf);
        justificacionFaltaProfesorado.setPdfSolicitud(pdfSolicitud);
        justificacionFaltaProfesorado.setPdfSolicitudFirmado(null);

        justificacionFaltaProfesorado.updateState(JustificacionFaltaProfesorado.State.PENDIENTE_PRESENTACION);


        ///Quitar esto es solo una prueba
        /*******************/
        DatosFirma datosFirma=new DatosFirma(SecurityUtil.getUser(),pdfSolicitud,"Firma Expediente:"+justificacionFaltaProfesorado.getNumeroExpediente(),new Rectangulo(100,100,400,50),this.getClass(),"Datos de callback");
        firmaService.insert(datosFirma);
    }
    @WhenEvent
    public void triggerPresentar(JustificacionFaltaProfesorado justificacionFaltaProfesorado, JustificacionFaltaProfesorado original, EventContext eventContext) throws BusinessException {
        RegistroEntrada registroEntrada=justificacionFaltaProfesorado.addRegistroEntrada(justificacionFaltaProfesorado.getPdfSolicitudFirmado(), List.of(justificacionFaltaProfesorado.getJustificante()));
        justificacionFaltaProfesorado.setPdfJustificanteRegistroEntrada(registroEntrada.getDocumentoResguardoPresentacion());
        justificacionFaltaProfesorado.updateState(JustificacionFaltaProfesorado.State.PENDIENTE_RESOLUCION);
        justificacionFaltaProfesorado.setDisconformidad(null);
        justificacionFaltaProfesorado.setResolucion(null);

    }

    @WhenEvent
    public void triggerResolver(JustificacionFaltaProfesorado justificacionFaltaProfesorado, JustificacionFaltaProfesorado original, EventContext eventContext)  throws BusinessException {
        TipoResolucionJustificacionFaltaProfesorado tipoResolucion = justificacionFaltaProfesorado.getTipoResolucion();
        DocumentoPdf resolucion = justificacionFaltaProfesorado.getDocumentoPdf(JustificacionFaltaProfesorado.TipoDocumentoPdf.RESOLUCION);

        DocumentoPdf resolucionFirmada =resolucion.firmar(almacenClaveLoader.getDirector(justificacionFaltaProfesorado.getCentro()),new CampoFirma(new Rectangulo(75,280,400,20)));

        MetaFile pdfResolucion = MetaFileHelper.createMetaFile(resolucionFirmada);

        RegistroSalida registroSalida=justificacionFaltaProfesorado.addRegistroSalida(pdfResolucion, List.of(justificacionFaltaProfesorado.getJustificante()));
        justificacionFaltaProfesorado.setPdfResolucion(registroSalida.getDocumento());
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


    @Override
    public void notify(Firma firma, Object callBackData) {
        System.out.println("Notificado!!!!!!:"+callBackData+ " en firma.id="+firma.getId());
    }
}