package com.educaflow.system.ventanilla.controller;

import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.i18n.I18n;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.expedientes.db.Tramite;
import com.educaflow.system.ventanilla.db.AsistenteNuevoExpediente;
import com.educaflow.system.ventanilla.service.AsistenteNuevoExpedienteService;
import com.google.inject.Inject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AsistenteNuevoExpedienteController {

    @Inject
    private ModelServiceFactory modelServiceFactory;

    @CallMethod
    public void prepararCentros(ActionRequest actionRequest, ActionResponse actionResponse) {
        final AsistenteNuevoExpedienteService asistenteNuevoExpedienteService = (AsistenteNuevoExpedienteService) modelServiceFactory.resolve(AsistenteNuevoExpediente.class);

        ActionRequestHelper<AsistenteNuevoExpediente> actionRequestHelper = new ActionRequestHelper<>(actionRequest, AsistenteNuevoExpediente.class);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        AsistenteNuevoExpediente asistente = actionRequestHelper.getModel(asistenteNuevoExpedienteService.allowPropertiesPrepararCentros());

        Optional<BusinessMessages> validationResult = asistenteNuevoExpedienteService.validatePrepararCentros(asistente);
        if (validationResult.isPresent()) {
            actionResponseHelper.doResponseBusinessMessagesAsError(validationResult.get());
            return;
        }

        AsistenteNuevoExpediente resultado = asistenteNuevoExpedienteService.prepararCentros(asistente);

        List<Map<String, Object>> centrosDisponibles = resultado.getCentrosDisponibles().stream()
                .map(this::toRegistroCentro)
                .toList();
        Centro centro = resultado.getCentro();

        actionResponse.setValue("centrosDisponibles", centrosDisponibles);
        actionResponse.setValue("hayQueElegirCentro", resultado.getHayQueElegirCentro());
        actionResponse.setValue("centro", centro == null ? null : toRegistroCentro(centro));
    }

    @CallMethod
    public void prepararTramites(ActionRequest actionRequest, ActionResponse actionResponse) {
        final AsistenteNuevoExpedienteService asistenteNuevoExpedienteService = (AsistenteNuevoExpedienteService) modelServiceFactory.resolve(AsistenteNuevoExpediente.class);

        ActionRequestHelper<AsistenteNuevoExpediente> actionRequestHelper = new ActionRequestHelper<>(actionRequest, AsistenteNuevoExpediente.class);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        AsistenteNuevoExpediente asistente = actionRequestHelper.getModel(asistenteNuevoExpedienteService.allowPropertiesPrepararTramites());

        Optional<BusinessMessages> validationResult = asistenteNuevoExpedienteService.validatePrepararTramites(asistente);
        if (validationResult.isPresent()) {
            actionResponseHelper.doResponseBusinessMessagesAsError(validationResult.get());
            return;
        }

        AsistenteNuevoExpediente resultado = asistenteNuevoExpedienteService.prepararTramites(asistente);

        List<Map<String, Object>> tramitesDisponibles = resultado.getTramitesDisponibles().stream()
                .map(this::toRegistroTramite)
                .toList();

        actionResponse.setValue("tramitesDisponibles", tramitesDisponibles);
    }

    @CallMethod
    public void recalcular(ActionRequest actionRequest, ActionResponse actionResponse) {
        final AsistenteNuevoExpedienteService asistenteNuevoExpedienteService = (AsistenteNuevoExpedienteService) modelServiceFactory.resolve(AsistenteNuevoExpediente.class);

        ActionRequestHelper<AsistenteNuevoExpediente> actionRequestHelper = new ActionRequestHelper<>(actionRequest, AsistenteNuevoExpediente.class);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        AsistenteNuevoExpediente asistente = actionRequestHelper.getModel(asistenteNuevoExpedienteService.allowPropertiesRecalcular());

        Optional<BusinessMessages> validationResult = asistenteNuevoExpedienteService.validateRecalcular(asistente);
        if (validationResult.isPresent()) {
            actionResponseHelper.doResponseBusinessMessagesAsError(validationResult.get());
            return;
        }

        AsistenteNuevoExpediente resultado = asistenteNuevoExpedienteService.recalcular(asistente);

        actionResponse.setValue("nombreTramite", resultado.getNombreTramite());
        actionResponse.setValue("ayudaTramite", resultado.getAyudaTramite());
        // Los dos campos de respuesta son booleanos de TRES estados (null = sin contestar). axelor-front
        // funde el bloque `values` con updateRecord, que descarta el cambio cuando el valor actual y el
        // nuevo coinciden al coercionarlos a número: null y false son ambos 0, así que por esa vía el paso
        // null↔false se pierde en silencio. `value:set` se aplica sin diff, así que el registro del
        // formulario refleja siempre lo que el servidor calculó.
        actionResponse.setAttr("presentadoEnPapel", "value:set", resultado.getPresentadoEnPapel());
        actionResponse.setAttr("presentadoEnRepresentacion", "value:set", resultado.getPresentadoEnRepresentacion());
        actionResponse.setValue("hayQuePreguntarPresentacion", resultado.getHayQuePreguntarPresentacion());
        actionResponse.setValue("hayQuePreguntarParaQuien", resultado.getHayQuePreguntarParaQuien());
    }

    @CallMethod
    public void validateTriggerInitialEvent(ActionRequest actionRequest, ActionResponse actionResponse) {
        final AsistenteNuevoExpedienteService asistenteNuevoExpedienteService = (AsistenteNuevoExpedienteService) modelServiceFactory.resolve(AsistenteNuevoExpediente.class);

        ActionRequestHelper<AsistenteNuevoExpediente> actionRequestHelper = new ActionRequestHelper<>(actionRequest, AsistenteNuevoExpediente.class);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        AsistenteNuevoExpediente asistente = actionRequestHelper.getModel(asistenteNuevoExpedienteService.allowPropertiesTriggerInitialEvent());

        Optional<BusinessMessages> validationResult = asistenteNuevoExpedienteService.validateTriggerInitialEvent(asistente);
        if (validationResult.isPresent()) {
            actionResponseHelper.doResponseBusinessMessagesAsError(I18n.get("No es posible crear el expediente"), validationResult.get());
        }
    }

    // AsistenteNuevoExpediente es persistable="false", así que la búsqueda diferida con la que el cliente
    // completa los campos que falten no se puede resolver (Axelor construye un JPQL con raíz en una entidad
    // que Hibernate no conoce y devuelve 0 registros): los dos métodos toRegistroXxx siguientes MUST incluir
    // "version" y TODAS las columnas del grid correspondiente.
    private Map<String, Object> toRegistroCentro(Centro centro) {
        return Map.of(
                "id", centro.getId(),
                "version", centro.getVersion(),
                "name", centro.getName());
    }

    private Map<String, Object> toRegistroTramite(Tramite tramite) {
        Map<String, Object> registro = new LinkedHashMap<>();
        registro.put("id", tramite.getId());
        registro.put("version", tramite.getVersion());
        registro.put("name", tramite.getName());
        if (tramite.getTipoTramite() != null) {
            registro.put("tipoTramite.name", tramite.getTipoTramite().getName());
        }
        return registro;
    }

}
