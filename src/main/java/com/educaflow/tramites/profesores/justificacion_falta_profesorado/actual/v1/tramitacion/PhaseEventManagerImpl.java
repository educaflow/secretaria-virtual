package com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.tramitacion;

import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.pdf.CampoFirma;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.subsystem.criptografia.service.AlmacenClaveResolver;
import com.educaflow.subsystem.expedientes.db.TipoResolucionJustificacionFaltaProfesoradoV1;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.EventContext;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.OnEnterState;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.PhaseEventManager;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.WhenEvent;
import com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesoradoV1;
import com.educaflow.subsystem.expedientes.db.repo.JustificacionFaltaProfesoradoV1Repository;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.States;

import com.educaflow.subsystem.registroentradasalida.db.RegistroSalida;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class PhaseEventManagerImpl extends PhaseEventManager<JustificacionFaltaProfesoradoV1> {

    /** El {@code campoFirma} del hueco de la firma del director en {@code documentospdf/resolucion.xml}. */
    private static final String CAMPO_FIRMA_RESOLUCION = "firmaDirector";

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

        DocumentoPdf resolucionFirmada =resolucion.firmar(almacenClaveResolver.getDirector(justificacionFaltaProfesorado.getCentro()),new CampoFirma(CAMPO_FIRMA_RESOLUCION));

        MetaFile pdfResolucion = MetaFileHelper.createMetaFile(resolucionFirmada);

        RegistroSalida registroSalida=eventContext.createRegistroSalida(pdfResolucion, List.of(justificacionFaltaProfesorado.getJustificante()));
        justificacionFaltaProfesorado.setPdfResolucion(registroSalida.getDocumento());
        switch (tipoResolucion) {
            case ACEPTAR -> eventContext.updateState(States.Tramitacion.ACEPTADO);
            case RECHAZAR -> eventContext.updateState(States.Tramitacion.RECHAZADO);
            case SUBSANAR_DATOS -> eventContext.updateState(States.Recepcion.ENTRADA_DATOS);
            default -> throw new IllegalArgumentException("Tipo de resolución no reconocido: " + tipoResolucion);
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
