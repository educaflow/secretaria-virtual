package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.resolucion;

import com.axelor.i18n.I18n;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.criptografia.AlmacenClave;
import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.pdf.CampoFirma;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.pdf.Rectangulo;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.util.Convert;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.criptografia.service.AlmacenClaveResolver;
import com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1;
import com.educaflow.subsystem.expedientes.db.SentidoRevisionAnulacionMatriculaCicloFormativoV1;
import com.educaflow.subsystem.expedientes.db.repo.AnulacionMatriculaCicloFormativoV1Repository;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.EventContext;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.OnEnterState;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.PhaseEventManager;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.WhenEvent;
import com.educaflow.subsystem.registroentradasalida.db.RegistroSalida;
import com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.AnulacionMatriculaCicloFormativoV1Util;
import com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.States;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;


public class PhaseEventManagerImpl extends PhaseEventManager<AnulacionMatriculaCicloFormativoV1> {

    // Hueco que la resolución deja bajo «El director / la directora del centro», medido sobre el PDF
    // que genera el documento. CampoFirma sin página resuelve a la última.
    private static final Rectangulo POSICION_FIRMA_RESOLUCION = new Rectangulo(150, 317, 300, 53);

    private final AnulacionMatriculaCicloFormativoV1Repository repository;
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    AlmacenClaveResolver almacenClaveResolver;

    @Inject
    public PhaseEventManagerImpl(AnulacionMatriculaCicloFormativoV1Repository repository) {
        super(AnulacionMatriculaCicloFormativoV1.class);
        this.repository = repository;
    }


    @WhenEvent
    public void triggerFirmar(AnulacionMatriculaCicloFormativoV1 expediente, AnulacionMatriculaCicloFormativoV1 original, EventContext eventContext) throws BusinessException {
        AnulacionMatriculaCicloFormativoV1Util.exigePertenecerAlCentroDelExpediente(expediente, "Solo puede firmar resoluciones de su propio centro");
        AnulacionMatriculaCicloFormativoV1Util.exigeOstentarElPerfilDelEstado(expediente, "Solo el director del centro puede firmar la resolución");

        String mensajeSinCertificado = I18n.get("El centro no tiene configurada la firma del Director; avise al administrador");
        AlmacenClave almacenDirector;
        try {
            almacenDirector = almacenClaveResolver.getDirector(expediente.getCentro());
        } catch (RuntimeException ex) {
            BusinessException businessException = new BusinessException(mensajeSinCertificado);
            businessException.initCause(ex);
            throw businessException;
        }
        if (almacenDirector == null) {
            throw new BusinessException(mensajeSinCertificado);
        }

        expediente.setFechaResolucion(LocalDate.now(Convert.defaultZoneId));
        expediente.setFirmadoPor(SecurityUtil.getUser());

        // Se regenera en vez de firmar el pdfResolucion guardado: aquel es una foto sin la fecha de
        // resolución ni el firmante, que se acaban de anotar.
        DocumentoPdf resolucionPdf = expediente.getDocumentoPdf(AnulacionMatriculaCicloFormativoV1.TipoDocumentoPdf.RESOLUCION);
        DocumentoPdf resolucionFirmada = resolucionPdf.firmar(almacenDirector, new CampoFirma(POSICION_FIRMA_RESOLUCION));

        MetaFile pdfTemporal = MetaFileHelper.createMetaFile(resolucionFirmada);
        RegistroSalida registroSalida = eventContext.createRegistroSalida(pdfTemporal, List.of());
        expediente.setPdfResolucionFirmada(registroSalida.getDocumento());

        expediente.setPdfResolucion(null);
        AnulacionMatriculaCicloFormativoV1Util.borrarDevolucionDelDirector(expediente);

        SentidoRevisionAnulacionMatriculaCicloFormativoV1 sentidoRevision = expediente.getSentidoRevision();
        switch (sentidoRevision) {
            case ACEPTAR -> eventContext.updateState(States.Resolucion.ACEPTADA);
            case RECHAZAR -> eventContext.updateState(States.Resolucion.RECHAZADA);
            case SUBSANAR -> throw new IllegalArgumentException("No se puede firmar una resolución cuyo sentido es: " + sentidoRevision);
            case null, default -> throw new IllegalArgumentException("Sentido de la revisión no reconocido: " + sentidoRevision);
        }
    }

    @WhenEvent
    public void triggerDevolver(AnulacionMatriculaCicloFormativoV1 expediente, AnulacionMatriculaCicloFormativoV1 original, EventContext eventContext) throws BusinessException {
        AnulacionMatriculaCicloFormativoV1Util.exigePertenecerAlCentroDelExpediente(expediente, "Solo puede devolver resoluciones de su propio centro");
        AnulacionMatriculaCicloFormativoV1Util.exigeOstentarElPerfilDelEstado(expediente, "Solo el director del centro puede devolver la resolución a la secretaría");

        expediente.setFechaDevolucion(LocalDate.now(Convert.defaultZoneId));
        expediente.setDevueltoPor(SecurityUtil.getUser());

        eventContext.updateState(States.Revision.PENDIENTE_REVISION);
    }


/***************************************************************************************/
/*************************************** Estados ***************************************/
/***************************************************************************************/

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
