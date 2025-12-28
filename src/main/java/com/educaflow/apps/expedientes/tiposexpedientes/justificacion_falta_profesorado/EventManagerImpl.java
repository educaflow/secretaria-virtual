package com.educaflow.apps.expedientes.tiposexpedientes.justificacion_falta_profesorado;

import com.educaflow.common.criptografia.AlmacenClaveDispositivo;
import com.educaflow.common.domains.db.MetaFilePdf;
import com.educaflow.apps.configuracioncentro.db.Centro;
import com.educaflow.apps.expedientes.common.EventContext;
import com.educaflow.apps.expedientes.common.annotations.OnEnterState;
import com.educaflow.apps.expedientes.common.annotations.WhenEvent;
import com.educaflow.apps.expedientes.db.JustificacionFaltaProfesorado;
import com.educaflow.apps.expedientes.db.TipoResolucionJustificacionFaltaProfesorado;
import com.educaflow.apps.expedientes.db.repo.JustificacionFaltaProfesoradoRepository;
import com.educaflow.common.pdf.*;

import com.educaflow.common.criptografia.AlmacenClaveFichero;

import com.educaflow.common.validation.messages.BusinessException;
import com.google.inject.Inject;

import java.io.InputStream;
import java.time.LocalDate;


public class EventManagerImpl extends com.educaflow.apps.expedientes.common.EventManager<JustificacionFaltaProfesorado, JustificacionFaltaProfesorado.State, JustificacionFaltaProfesorado.Event,JustificacionFaltaProfesorado.Profile> {

    private final JustificacionFaltaProfesoradoRepository repository;

    @Inject
    public EventManagerImpl(JustificacionFaltaProfesoradoRepository repository) {
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
        DocumentoPdf justificantePdf= justificacionFaltaProfesorado.getJustificante().getDocumentoPdf();
        solicitudPdf=solicitudPdf.anyadirDocumentoPdf(justificantePdf);




        MetaFilePdf metaFilePdf= new MetaFilePdf(solicitudPdf);
        justificacionFaltaProfesorado.setDocumentacionParaPresentarSinFirmar(metaFilePdf);


        justificacionFaltaProfesorado.updateState(JustificacionFaltaProfesorado.State.PENDIENTE_PRESENTACION);
    }
    @WhenEvent
    public void triggerPresentar(JustificacionFaltaProfesorado justificacionFaltaProfesorado, JustificacionFaltaProfesorado original, EventContext eventContext) throws BusinessException {
        DocumentoPdf documentacionPresentadaFirmadaUsuario= justificacionFaltaProfesorado.getDocumentacionPresentadaFirmadaUsuario().getDocumentoPdf();

        byte[] sello=getSelloCentro(justificacionFaltaProfesorado.getCentroReceptor());

        //AlmacenClaveFichero almacenClave=new AlmacenClaveFichero(EventManagerImpl.class.getResourceAsStream("/firma/mi_certificado.p12"),"nadanada");
        AlmacenClaveDispositivo almacenClave=new AlmacenClaveDispositivo( 0,"CertFirmaDigitalDirector");
        CampoFirma campoFirma=new CampoFirma(new Rectangulo(80,10,120,100))
                .setMensaje("Recibido en el centro CIPFP Mislata el día "+ LocalDate.now())
                .setMotivo("Registro por parte del director que ha recibido la  documentación presentada por el usuario")
                .setImage(sello)
                .setNumeroPagina(1);

        DocumentoPdf justificanteDocumentacionPresentadaFirmadaCentro=documentacionPresentadaFirmadaUsuario.firmar(almacenClave,campoFirma);
        MetaFilePdf metaFilePdf= new MetaFilePdf(justificanteDocumentacionPresentadaFirmadaCentro);
        justificacionFaltaProfesorado.setJustificanteDocumentacionPresentadaFirmadaCentro(metaFilePdf);

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


    private byte[] getSelloCentro(Centro centro) {
        System.out.print("Error:Cada centro debe tener su propio sello de entrada");

        try (InputStream inputStream = EventManagerImpl.class.getClassLoader().getResourceAsStream("firma/sello_centro_educativo.png")) {
            if (inputStream == null) {
                throw new RuntimeException("No se ha encontrado el recurso: sello_centro_educativo.png");
            }
            return inputStream.readAllBytes();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


}