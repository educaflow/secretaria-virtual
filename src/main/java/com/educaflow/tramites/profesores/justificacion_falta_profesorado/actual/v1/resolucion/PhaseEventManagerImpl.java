package com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.resolucion;

import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.i18n.I18n;
import com.educaflow.subsystem.common.db.CargoCodigo;
import com.educaflow.subsystem.criptografia.db.CertificadoDigital;
import com.educaflow.subsystem.criptografia.service.CertificadoDigitalService;
import com.educaflow.base.infrastructure.criptografia.AlmacenClave;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.pdf.CampoFirma;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.subsystem.expedientes.db.TipoResolucionJustificacionFaltaProfesoradoV1;
import com.educaflow.subsystem.expedientes.util.ExpedienteNotasUtil;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.EventContext;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.OnEnterState;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.PhaseEventManager;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.WhenEvent;
import com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesoradoV1;
import com.educaflow.subsystem.expedientes.db.repo.JustificacionFaltaProfesoradoV1Repository;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.States;

import com.educaflow.subsystem.registroentradasalida.db.RegistroSalida;
import com.educaflow.tramites.util.registro.AvisoRegistroHelper;
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
    ModelServiceFactory modelServiceFactory;

    @Inject
    AvisoRegistroHelper avisoRegistroHelper;

    @Inject
    public PhaseEventManagerImpl(JustificacionFaltaProfesoradoV1Repository repository) {
        super(JustificacionFaltaProfesoradoV1.class);
        this.repository = repository;
    }


    /**
     * La dirección resuelve la solicitud que la jefatura de estudios dio por correcta, o se la devuelve si no
     * está conforme con esa verificación.
     */
    @WhenEvent
    public void triggerResolver(JustificacionFaltaProfesoradoV1 justificacionFaltaProfesorado, JustificacionFaltaProfesoradoV1 original, EventContext eventContext) throws BusinessException {
        TipoResolucionJustificacionFaltaProfesoradoV1 tipoResolucion = justificacionFaltaProfesorado.getTipoResolucion();
        String motivoDevolucion= justificacionFaltaProfesorado.getMotivoDevolucion();
        String motivoRechazo= justificacionFaltaProfesorado.getMotivoRechazo();

        switch (tipoResolucion) {
            case ACEPTAR -> {
                emitirResolucion(justificacionFaltaProfesorado, eventContext);
                eventContext.updateState(States.Resolucion.ACEPTADO);

                justificacionFaltaProfesorado.setMotivoRechazo(null);
                justificacionFaltaProfesorado.setMotivoDevolucion(null);
            }
            case RECHAZAR -> {
                emitirResolucion(justificacionFaltaProfesorado, eventContext);
                eventContext.updateState(States.Resolucion.RECHAZADO);

                justificacionFaltaProfesorado.setMotivoDevolucion(null);
            }
            case DEVOLVER -> {
                ExpedienteNotasUtil.addNote(justificacionFaltaProfesorado, I18n.get("Devolución a jefatura por: %s").formatted(motivoDevolucion));
                eventContext.updateState(States.Verificacion.PENDIENTE_VERIFICACION);

                justificacionFaltaProfesorado.setTipoResolucion(null);
                justificacionFaltaProfesorado.setMotivoRechazo(null);
                justificacionFaltaProfesorado.setMotivoDevolucion(null);
            }
            case null -> throw new IllegalArgumentException("Tipo de resolución no reconocido: " + tipoResolucion);
        }
    }

    /** Genera la resolución, la firma con el certificado de la dirección del centro, la registra de salida y se la envía al solicitante. */
    private void emitirResolucion(JustificacionFaltaProfesoradoV1 justificacionFaltaProfesorado, EventContext eventContext) {
        justificacionFaltaProfesorado.setMotivoDevolucion(null);

        DocumentoPdf resolucion = justificacionFaltaProfesorado.getDocumentoPdf(JustificacionFaltaProfesoradoV1.TipoDocumentoPdf.RESOLUCION);
        CertificadoDigitalService certificadoDigitalService = (CertificadoDigitalService) modelServiceFactory.resolve(CertificadoDigital.class);
        AlmacenClave almacenClaveDirector = certificadoDigitalService.getByCentroCargo(justificacionFaltaProfesorado.getCentro(), CargoCodigo.DIRECTOR);
        DocumentoPdf resolucionFirmada = resolucion.firmar(almacenClaveDirector, new CampoFirma(CAMPO_FIRMA_RESOLUCION));
        MetaFile pdfResolucion = MetaFileHelper.createMetaFile(resolucionFirmada);

        RegistroSalida registroSalida = eventContext.createRegistroSalida(pdfResolucion, List.of(justificacionFaltaProfesorado.getJustificante()));
        justificacionFaltaProfesorado.setPdfResolucion(registroSalida.getDocumento());
        avisoRegistroHelper.avisarDeRegistroSalida(justificacionFaltaProfesorado, registroSalida);
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
