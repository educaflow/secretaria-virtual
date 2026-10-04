package com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.entrada;

import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.*;
import com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesoradoV1;
import com.educaflow.subsystem.expedientes.db.repo.JustificacionFaltaProfesoradoV1Repository;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.JustificacionFaltaProfesoradoV1Util;
import com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.States;
import com.educaflow.tramites.util.entrada.EntradaHelper;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class PhaseEventManagerImpl extends PhaseEventManager<JustificacionFaltaProfesoradoV1> {

    /**
     * El {@code campoFirma} del hueco de la firma en {@code documentospdf/solicitud.xml}. Es exactamente el que
     * la {@code <action-method>} de AutoFirma pasa a {@code firmarDocumentoEnCampo}, para que la firma caiga en
     * el mismo sitio se firme en el equipo del profesor o en el servidor.
     */
    private static final String CAMPO_FIRMA_SOLICITUD = "firmaSolicitante";

    private final JustificacionFaltaProfesoradoV1Repository repository;
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    EntradaHelper entradaHelper;

    @Inject
    public PhaseEventManagerImpl(JustificacionFaltaProfesoradoV1Repository repository) {
        super(JustificacionFaltaProfesoradoV1.class);
        this.repository = repository;
    }



    @WhenEvent
    public void triggerDelete(JustificacionFaltaProfesoradoV1 justificacionFaltaProfesorado, JustificacionFaltaProfesoradoV1 original, EventContext eventContext) throws BusinessException {
        JustificacionFaltaProfesoradoV1Util.exigeSerElCreador(justificacionFaltaProfesorado, "Solo puede borrar sus propias solicitudes");
    }

    /** Solo en papel: adjuntada la solicitud escaneada, se pasa a copiar sus datos. */
    @WhenEvent
    public void triggerContinuar(JustificacionFaltaProfesoradoV1 justificacionFaltaProfesorado, JustificacionFaltaProfesoradoV1 original, EventContext eventContext) throws BusinessException {
        EntradaHelper.exigePresentadoEnPapel(original, true);

        eventContext.updateState(States.Entrada.ENTRADA_DATOS);
    }

    /**
     * Genera la solicitud con los datos tecleados. Telemáticamente el profesor tiene que firmarla; en papel ya
     * la firmó a mano quien la entregó, así que se presenta con el escaneado.
     */
    @WhenEvent
    public void triggerGuardarDatos(JustificacionFaltaProfesoradoV1 justificacionFaltaProfesorado, JustificacionFaltaProfesoradoV1 original, EventContext eventContext) throws BusinessException {
        JustificacionFaltaProfesoradoV1Util.exigeSerElCreador(justificacionFaltaProfesorado, "Solo puede modificar sus propias solicitudes");

        borrarPeriodoQueNoPideElTipoDeJornada(justificacionFaltaProfesorado);
        generarSolicitud(justificacionFaltaProfesorado);

        if (Boolean.TRUE.equals(original.getPresentadoEnPapel())) {
            entradaHelper.presentar(justificacionFaltaProfesorado, JustificacionFaltaProfesoradoV1Util.CAMPOS_ENTRADA, List.of(justificacionFaltaProfesorado.getJustificante()), eventContext);
            eventContext.updateState(States.Verificacion.PENDIENTE_VERIFICACION);
        } else {
            justificacionFaltaProfesorado.setPdfSolicitudFirmada(null);
            eventContext.updateState(States.Entrada.PENDIENTE_PRESENTACION);
        }
    }

    private static void borrarPeriodoQueNoPideElTipoDeJornada(JustificacionFaltaProfesoradoV1 justificacionFaltaProfesorado) {
        if (JustificacionFaltaProfesoradoV1Util.necesitaFechaFin(justificacionFaltaProfesorado) == false) {
            justificacionFaltaProfesorado.setFechaFin(null);
        }
        if (JustificacionFaltaProfesoradoV1Util.necesitaHoraInicio(justificacionFaltaProfesorado) == false) {
            justificacionFaltaProfesorado.setHoraInicio(null);
        }
        if (JustificacionFaltaProfesoradoV1Util.necesitaHoraFin(justificacionFaltaProfesorado) == false) {
            justificacionFaltaProfesorado.setHoraFin(null);
        }
    }

    /** La solicitud lleva detrás el justificante, para que lo que se firma y se registra sea un único documento. */
    private static void generarSolicitud(JustificacionFaltaProfesoradoV1 justificacionFaltaProfesorado) {
        DocumentoPdf solicitudPdf = justificacionFaltaProfesorado.getDocumentoPdf(JustificacionFaltaProfesoradoV1.TipoDocumentoPdf.SOLICITUD);
        MetaFile justificante = justificacionFaltaProfesorado.getJustificante();
        if (justificante != null) {
            solicitudPdf = solicitudPdf.anyadirDocumentoPdf(MetaFileHelper.getDocumentoPdfFromImagenOrPdf(justificante));
        }
        MetaFile pdfSolicitud = MetaFileHelper.createMetaFile(solicitudPdf);
        justificacionFaltaProfesorado.setPdfSolicitud(pdfSolicitud);
    }

    /**
     * Lo disparan dos estados: PENDIENTE_PRESENTACION vuelve a los datos, y ENTRADA_DATOS (solo en papel) vuelve
     * al escaneado para cambiarlo.
     */
    @WhenEvent
    public void triggerBack(JustificacionFaltaProfesoradoV1 justificacionFaltaProfesorado, JustificacionFaltaProfesoradoV1 original, EventContext eventContext) throws BusinessException {
        justificacionFaltaProfesorado.setClaveCertificado(null);

        JustificacionFaltaProfesoradoV1Util.exigeSerElCreador(justificacionFaltaProfesorado, "Solo puede volver atrás en sus propias solicitudes");

        if (EntradaHelper.estaEn(original, States.Entrada.ENTRADA_DATOS)) {
            EntradaHelper.exigePresentadoEnPapel(original, true);
            eventContext.updateState(States.Entrada.PENDIENTE_DOCUMENTO_ESCANEADO);
        } else {
            eventContext.updateState(States.Entrada.ENTRADA_DATOS);
        }
    }

    /** Solo telemáticamente: el profesor firma la solicitud generada. En papel se presenta en GUARDAR_DATOS. */
    @WhenEvent
    public void triggerPresentar(JustificacionFaltaProfesoradoV1 exp, JustificacionFaltaProfesoradoV1 original, EventContext eventContext) throws BusinessException {
        try {
            JustificacionFaltaProfesoradoV1Util.exigeSerElCreador(exp, "Solo puede presentar sus propias solicitudes");
            EntradaHelper.exigePresentadoEnPapel(original, false);
            entradaHelper.firmarSolicitudSiEsEnServidor(exp, JustificacionFaltaProfesoradoV1Util.CAMPOS_ENTRADA, CAMPO_FIRMA_SOLICITUD);

            entradaHelper.presentar(exp, JustificacionFaltaProfesoradoV1Util.CAMPOS_ENTRADA, List.of(exp.getJustificante()), eventContext);
            eventContext.updateState(States.Verificacion.PENDIENTE_VERIFICACION);
        } finally {
            exp.setClaveCertificado(null);
        }
    }

/***************************************************************************************/
/*************************************** Estados ***************************************/
/***************************************************************************************/

    @OnEnterState
    public void onEnterPendienteDocumentoEscaneado(JustificacionFaltaProfesoradoV1 justificacionFaltaProfesorado, EventContext eventContext) {

    }
    @OnEnterState
    public void onEnterEntradaDatos(JustificacionFaltaProfesoradoV1 justificacionFaltaProfesorado, EventContext eventContext) {

    }
    @OnEnterState
    public void onEnterPendientePresentacion(JustificacionFaltaProfesoradoV1 justificacionFaltaProfesorado, EventContext eventContext) {

    }

}
