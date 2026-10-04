package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.resolucion;

import com.axelor.auth.db.User;
import com.axelor.auth.service.UserService;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.subsystem.common.db.CargoCodigo;
import com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1;
import com.educaflow.subsystem.expedientes.db.TipoResolucionAnulacionMatriculaCicloFormativoV1;
import com.educaflow.subsystem.expedientes.db.repo.AnulacionMatriculaCicloFormativoV1Repository;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.EventContext;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.OnEnterState;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.PhaseEventManager;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.WhenEvent;
import com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.AnulacionMatriculaCicloFormativoV1Util;
import com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.States;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * La secretaría propone la resolución. Al resolver se genera y se pone a firmar, primero al secretario y después
 * al director, en su bandeja de firmas: eso ya es la fase FIRMA.
 */
public class PhaseEventManagerImpl extends PhaseEventManager<AnulacionMatriculaCicloFormativoV1> {

    private final AnulacionMatriculaCicloFormativoV1Repository repository;
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    ModelServiceFactory modelServiceFactory;

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

    /**
     * La secretaría acepta o rechaza la anulación: se genera la resolución y se le pone a firmar al secretario.
     * Cuando la firme, la fase FIRMA se la pondrá a firmar al director.
     */
    @WhenEvent
    public void triggerResolver(AnulacionMatriculaCicloFormativoV1 expediente, AnulacionMatriculaCicloFormativoV1 original, EventContext eventContext) throws BusinessException {
        AnulacionMatriculaCicloFormativoV1Util.exigePertenecerAlCentroDelExpediente(expediente, "Solo puede resolver solicitudes de su propio centro");
        AnulacionMatriculaCicloFormativoV1Util.exigeOstentarElPerfilDelEstado(expediente, "Solo la secretaría del centro puede resolver esta solicitud");
        UserService userService = (UserService) modelServiceFactory.resolve(User.class);
        User secretario = userService.getByCentroAndCargo(expediente.getCentro(), CargoCodigo.SECRETARIO);
        // El director no firma hasta que firme el secretario, pero se exige ya: si faltara, el evento fallaría
        // al firmar el secretario en su bandeja, con la resolución ya puesta a firmar.
        userService.getByCentroAndCargo(expediente.getCentro(), CargoCodigo.DIRECTOR);

        if (expediente.getTipoResolucion() != TipoResolucionAnulacionMatriculaCicloFormativoV1.RECHAZAR) {
            expediente.setMotivoRechazo(null);
        }
        expediente.setPdfResolucionFirmada(null);

        DocumentoPdf resolucionPdf = expediente.getDocumentoPdf(AnulacionMatriculaCicloFormativoV1.TipoDocumentoPdf.RESOLUCION);
        expediente.setPdfResolucion(MetaFileHelper.createMetaFile(resolucionPdf));

        AnulacionMatriculaCicloFormativoV1Util.ponerResolucionAFirmar(expediente, secretario, AnulacionMatriculaCicloFormativoV1Util.CAMPO_FIRMA_SECRETARIO);

        eventContext.updateState(States.Firma.PENDIENTE_FIRMA_SECRETARIO);
    }


/***************************************************************************************/
/*************************************** Estados ***************************************/
/***************************************************************************************/

    @OnEnterState
    public void onEnterPendienteResolucion(AnulacionMatriculaCicloFormativoV1 expediente, EventContext eventContext) {

    }

}
