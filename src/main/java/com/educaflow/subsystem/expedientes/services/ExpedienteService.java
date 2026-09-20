package com.educaflow.subsystem.expedientes.services;

import com.axelor.db.Model;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.i18n.I18n;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.subsystem.expedientes.db.Tramite;
import com.educaflow.subsystem.expedientes.tramitacion.eventmanager.ContextoTramitacion;
import com.educaflow.subsystem.expedientes.tramitacion.eventmanager.EventContext;
import com.educaflow.subsystem.expedientes.tramitacion.eventmanager.PhaseEventManager;
import com.educaflow.subsystem.expedientes.tramitacion.eventmanager.State;
import com.educaflow.subsystem.expedientes.tramitacion.internal.ExpedienteLocator;
import com.educaflow.subsystem.expedientes.tramitacion.core.Tramitador;
import com.educaflow.subsystem.expedientes.tramitacion.util.ExpedienteUtil;
import com.educaflow.subsystem.security.service.PerfilesUsuarioService;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ExpedienteService {

    @Inject
    Tramitador tramitador;

    @Inject
    PerfilesUsuarioService perfilesUsuarioService;

    @Inject
    ExpedienteLocator expedienteLocator;

    @Inject
    ModelServiceFactory modelServiceFactory;


    @Transactional
    public Expediente triggerInitialEvent(ContextoTramitacion contextoTramitacion) throws BusinessException {
        validateTriggerInitialEvent(contextoTramitacion).ifPresent(BusinessMessages::throwIfInvalid);

        return tramitador.triggerInitialEvent(contextoTramitacion);
    }

    /**
     * Dispara un evento sobre el expediente.
     *
     * <p>Quién puede dispararlo es el actor del estado actual. Sin esto, cualquiera con acceso de
     * lectura al expediente podría disparar los eventos de cualquier perfil — por ejemplo, el creador
     * autoaprobándose el expediente con el evento del TRAMITADOR.
     */
    @Transactional
    public void triggerEvent(Expediente expediente, String eventName, Map<String, Object> requestData, EventContext eventContext) throws BusinessException {
        validateTriggerEvent(expediente, eventName, requestData, eventContext).ifPresent(BusinessMessages::throwIfInvalid);

        tramitador.triggerEvent(expediente, eventName, requestData, eventContext);
    }

    public BusinessMessages validateChild(Expediente expediente, Model bean, Class<? extends Model> beanClass, String validateProperty, Map<String, Object> requestData) {
        validateValidateChild(expediente, bean, beanClass, validateProperty, requestData).ifPresent(BusinessMessages::throwIfInvalid);

        return tramitador.validateChild(expediente, bean, beanClass, validateProperty, requestData);
    }


    public VistaExpediente getVistaExpediente(Expediente expediente, Profile profile) {
        validateGetVistaExpediente(expediente, profile).ifPresent(BusinessMessages::throwIfInvalid);

        EventContext eventContext = new EventContext(expediente, profile, modelServiceFactory);
        PhaseEventManager phaseEventManager = expedienteLocator.getPhaseEventManager(expediente.getTipoExpediente(), expediente.getCodePhase());
        String viewName = phaseEventManager.getViewName(expediente, eventContext);

        return new VistaExpediente(viewName, phaseEventManager.getModelClass(), expediente, profile);
    }








    /*************************************************************************************/
    /****************************** Funciones de Validación ******************************/
    /*************************************************************************************/

    public Optional<BusinessMessages> validateTriggerInitialEvent(ContextoTramitacion contextoTramitacion) {
        BusinessMessages businessMessages = new BusinessMessages();

        validateCentroYPerfil(contextoTramitacion, businessMessages);
        validateRepresentacion(contextoTramitacion, businessMessages);

        return businessMessages.isValid() ? Optional.empty() : Optional.of(businessMessages);
    }

    public Optional<BusinessMessages> validateTriggerEvent(Expediente expediente, String eventName, Map<String, Object> requestData, EventContext eventContext) {
        BusinessMessages businessMessages = new BusinessMessages();

        validatePerfilDelEstado(expediente, businessMessages);

        return businessMessages.isValid() ? Optional.empty() : Optional.of(businessMessages);
    }

    /**
     * Validar un detalle es parte de tramitar el expediente: las reglas que se le aplican son las del
     * estado actual, así que quien no es su actor tampoco lo valida.
     */
    public Optional<BusinessMessages> validateValidateChild(Expediente expediente, Model bean, Class<? extends Model> beanClass, String validateProperty, Map<String, Object> requestData) {
        BusinessMessages businessMessages = new BusinessMessages();

        validatePerfilDelEstado(expediente, businessMessages);

        return businessMessages.isValid() ? Optional.empty() : Optional.of(businessMessages);
    }

    public Optional<BusinessMessages> validateGetVistaExpediente(Expediente expediente, Profile profile) {
        BusinessMessages businessMessages = new BusinessMessages();

        validatePerfilDelUsuario(expediente, profile, businessMessages);

        return businessMessages.isValid() ? Optional.empty() : Optional.of(businessMessages);
    }




    /********************************************************************************/
    /****************************** Funciones privadas ******************************/
    /********************************************************************************/

    private void validateCentroYPerfil(ContextoTramitacion contextoTramitacion, BusinessMessages businessMessages) {
        Centro centro = contextoTramitacion.centro();
        if (centro == null) {
            businessMessages.add(new BusinessMessage(I18n.get("Debe indicar el centro")));
            return;
        }

        Set<Profile> perfilesDeInicio = perfilesUsuarioService.getPerfilesDeInicioSobreTramite(contextoTramitacion.tramite(), SecurityUtil.getUser(), centro);
        if (perfilesDeInicio.isEmpty()) {
            businessMessages.add(new BusinessMessage(I18n.get("No puede crear expedientes de este trámite en el centro indicado")));
            return;
        }

        Profile profile = contextoTramitacion.profile();
        if (profile == null || !profile.puedeCrearExpediente() ||!perfilesDeInicio.contains(profile)) {
            businessMessages.add(new BusinessMessage(I18n.get("No puede presentar el expediente de esa forma en el centro indicado")));
        }
    }

    private void validateRepresentacion(ContextoTramitacion contextoTramitacion, BusinessMessages businessMessages) {
        if (contextoTramitacion.presentadoEnRepresentacion() && !contextoTramitacion.tramite().getPermitidoPresentarEnRepresentacion()) {
            businessMessages.add(new BusinessMessage(I18n.get("Este trámite no permite presentar la solicitud en representación de otra persona")));
        }
    }

    private void validatePerfilDelEstado(Expediente expediente, BusinessMessages businessMessages) {
        State state = ExpedienteUtil.getState(expediente);
        Profile profileDelEstado = state.getProfile();
        if (profileDelEstado == null) {
            return;
        }

        if (getPerfilesSobreExpediente(expediente).contains(profileDelEstado)) {
            return;
        }

        businessMessages.add(new BusinessMessage(I18n.get("No puede actuar sobre el expediente en su estado actual")));
    }

    private void validatePerfilDelUsuario(Expediente expediente, Profile profile, BusinessMessages businessMessages) {
        if (profile != null && getPerfilesSobreExpediente(expediente).contains(profile)) {
            return;
        }

        businessMessages.add(new BusinessMessage(I18n.get("No puede ver el expediente con ese perfil")));
    }

    private Set<Profile> getPerfilesSobreExpediente(Expediente expediente) {
        if (SecurityUtil.isAdmin(SecurityUtil.getUser())) {
            return Set.of(Profile.values());
        }

        return perfilesUsuarioService.getPerfilesSobreExpediente(expediente, SecurityUtil.getUser());
    }


}
