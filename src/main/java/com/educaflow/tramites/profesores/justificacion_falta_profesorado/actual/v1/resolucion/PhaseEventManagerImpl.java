package com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.resolucion;

import com.axelor.db.modelservice.ModelServiceFactory;
import com.educaflow.subsystem.common.db.CargoCodigo;
import com.educaflow.subsystem.criptografia.db.CertificadoDigital;
import com.educaflow.subsystem.criptografia.service.CertificadoDigitalService;
import com.educaflow.base.infrastructure.criptografia.AlmacenClave;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.pdf.CampoFirma;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
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
    ModelServiceFactory modelServiceFactory;

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
        switch (tipoResolucion) {
            case ACEPTAR -> {
                justificacionFaltaProfesorado.setMotivoRechazo(null);
                emitirResolucion(justificacionFaltaProfesorado, eventContext);
                eventContext.updateState(States.Resolucion.ACEPTADO);
            }
            case RECHAZAR -> {
                emitirResolucion(justificacionFaltaProfesorado, eventContext);
                eventContext.updateState(States.Resolucion.RECHAZADO);
            }
            case DEVOLVER -> {
                // No se emite ningún documento: la jefatura de estudios vuelve a verificar, con el motivo a la vista.
                justificacionFaltaProfesorado.setMotivoRechazo(null);
                justificacionFaltaProfesorado.setTipoResolucion(null);
                justificacionFaltaProfesorado.setResultadoVerificacion(null);
                eventContext.updateState(States.Verificacion.PENDIENTE_VERIFICACION);
            }
            case null -> throw new IllegalArgumentException("Tipo de resolución no reconocido: " + tipoResolucion);
        }
    }

    /** Genera la resolución, la firma con el certificado de la dirección del centro y la registra de salida. */
    private void emitirResolucion(JustificacionFaltaProfesoradoV1 justificacionFaltaProfesorado, EventContext eventContext) {
        justificacionFaltaProfesorado.setMotivoDevolucion(null);

        DocumentoPdf resolucion = justificacionFaltaProfesorado.getDocumentoPdf(JustificacionFaltaProfesoradoV1.TipoDocumentoPdf.RESOLUCION);
        CertificadoDigitalService certificadoDigitalService = (CertificadoDigitalService) modelServiceFactory.resolve(CertificadoDigital.class);
        AlmacenClave almacenClaveDirector = certificadoDigitalService.getByCentroCargo(justificacionFaltaProfesorado.getCentro(), CargoCodigo.DIRECTOR);
        DocumentoPdf resolucionFirmada = resolucion.firmar(almacenClaveDirector, new CampoFirma(CAMPO_FIRMA_RESOLUCION));
        MetaFile pdfResolucion = MetaFileHelper.createMetaFile(resolucionFirmada);

        RegistroSalida registroSalida = eventContext.createRegistroSalida(pdfResolucion, List.of(justificacionFaltaProfesorado.getJustificante()));
        justificacionFaltaProfesorado.setPdfResolucion(registroSalida.getDocumento());
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
