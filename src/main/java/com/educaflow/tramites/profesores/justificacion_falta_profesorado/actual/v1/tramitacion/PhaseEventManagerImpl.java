package com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.tramitacion;

import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.pdf.CampoFirma;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.pdf.Rectangulo;
import com.educaflow.subsystem.criptografia.service.AlmacenClaveResolver;
import com.educaflow.subsystem.expedientes.db.TipoResolucionJustificacionFaltaProfesoradoV1;
import com.educaflow.subsystem.expedientes.services.eventmanager.EventContext;
import com.educaflow.subsystem.expedientes.services.eventmanager.OnEnterState;
import com.educaflow.subsystem.expedientes.services.eventmanager.WhenEvent;
import com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesoradoV1;
import com.educaflow.subsystem.expedientes.db.repo.JustificacionFaltaProfesoradoV1Repository;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.States;

import com.educaflow.subsystem.registroentradasalida.db.RegistroSalida;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class PhaseEventManagerImpl extends com.educaflow.subsystem.expedientes.services.eventmanager.PhaseEventManager<JustificacionFaltaProfesoradoV1> {

    private static final Rectangulo rectanguloPosicionFirmaPDFResolucion =new Rectangulo(75,280,400,20);

    private final JustificacionFaltaProfesoradoV1Repository repository;
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    AlmacenClaveResolver almacenClaveResolver;

    @Inject
    public PhaseEventManagerImpl(JustificacionFaltaProfesoradoV1Repository repository) {
        super(JustificacionFaltaProfesoradoV1.class);
        this.repository = repository;
    }


    @WhenEvent
    public void triggerResolver(JustificacionFaltaProfesoradoV1 justificacionFaltaProfesorado, JustificacionFaltaProfesoradoV1 original, EventContext eventContext) throws BusinessException {
        TipoResolucionJustificacionFaltaProfesoradoV1 tipoResolucion = justificacionFaltaProfesorado.getTipoResolucion();
        DocumentoPdf resolucion = justificacionFaltaProfesorado.getDocumentoPdf(JustificacionFaltaProfesoradoV1.TipoDocumentoPdf.RESOLUCION);

        DocumentoPdf resolucionFirmada =resolucion.firmar(almacenClaveResolver.getDirector(justificacionFaltaProfesorado.getCentro()),new CampoFirma(rectanguloPosicionFirmaPDFResolucion));

        MetaFile pdfResolucion = MetaFileHelper.createMetaFile(resolucionFirmada);

        RegistroSalida registroSalida=eventContext.createRegistroSalida(pdfResolucion, List.of(justificacionFaltaProfesorado.getJustificante()));
        justificacionFaltaProfesorado.setPdfResolucion(registroSalida.getDocumento());
        switch (tipoResolucion) {
            case ACEPTAR:
                eventContext.updateState(States.Tramitacion.ACEPTADO);
                break;
            case RECHAZAR:
                eventContext.updateState(States.Tramitacion.RECHAZADO);
                break;
            case SUBSANAR_DATOS:
                eventContext.updateState(States.Recepcion.ENTRADA_DATOS);
                break;
            default:
                throw new IllegalArgumentException("Tipo de resolución no reconocido: " + tipoResolucion);
        }
    }


/***************************************************************************************/
/*************************************** Estados ***************************************/
/***************************************************************************************/

    @OnEnterState
    public void onEnterPendienteResolucion(JustificacionFaltaProfesoradoV1 justificacionFaltaProfesorado, EventContext eventContext) {

    }
    @OnEnterState
    public void onEnterAceptado(JustificacionFaltaProfesoradoV1 justificacionFaltaProfesorado, EventContext eventContext) {

    }
    @OnEnterState
    public void onEnterRechazado(JustificacionFaltaProfesoradoV1 justificacionFaltaProfesorado, EventContext eventContext) {

    }

}
