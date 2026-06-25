package com.educaflow.system.gruposnotas.controller;

import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper;
import com.educaflow.system.gruposnotas.db.Grupo;
import com.educaflow.system.gruposnotas.service.GrupoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrupoControllerTest {

    private ModelServiceFactory modelServiceFactory;
    private GrupoService grupoService;
    private ActionRequest actionRequest;
    private ActionResponse actionResponse;
    private GrupoController controller;

    @BeforeEach
    void setUp() throws Exception {
        modelServiceFactory = Mockito.mock(ModelServiceFactory.class);
        grupoService = Mockito.mock(GrupoService.class);
        actionRequest = Mockito.mock(ActionRequest.class);
        actionResponse = Mockito.mock(ActionResponse.class);

        when(modelServiceFactory.resolve(Grupo.class)).thenReturn(grupoService);

        controller = new GrupoController();
        Field field = GrupoController.class.getDeclaredField("modelServiceFactory");
        field.setAccessible(true);
        field.set(controller, modelServiceFactory);
    }

    @Test
    void cerrarGrupo_sinErroresDeValidacion_ejecutaCerrarYRefresca() {
        Grupo original = new Grupo();
        Grupo grupo = new Grupo();
        when(grupoService.validateCerrarGrupo(grupo, original)).thenReturn(Optional.empty());

        try (MockedConstruction<ActionRequestHelper> requestHelper = mockConstruction(
                ActionRequestHelper.class,
                (mock, context) -> {
                    when(mock.getOriginalModel()).thenReturn(original);
                    when(mock.getModel(any())).thenReturn(grupo);
                });
             MockedConstruction<ActionResponseHelper> responseHelper = mockConstruction(
                     ActionResponseHelper.class)) {

            controller.cerrarGrupo(actionRequest, actionResponse);

            verify(grupoService).cerrarGrupo(grupo, original);
            verify(actionResponse).setReload(true);
            verify(responseHelper.constructed().get(0), never())
                    .doResponseBusinessMessagesAsError(any());
        }
    }

    @Test
    void cerrarGrupo_conErroresDeValidacion_respondeBusinessMessagesYNoCierra() {
        Grupo original = new Grupo();
        Grupo grupo = new Grupo();
        BusinessMessages businessMessages = Mockito.mock(BusinessMessages.class);
        when(grupoService.validateCerrarGrupo(grupo, original)).thenReturn(Optional.of(businessMessages));

        try (MockedConstruction<ActionRequestHelper> requestHelper = mockConstruction(
                ActionRequestHelper.class,
                (mock, context) -> {
                    when(mock.getOriginalModel()).thenReturn(original);
                    when(mock.getModel(any())).thenReturn(grupo);
                });
             MockedConstruction<ActionResponseHelper> responseHelper = mockConstruction(
                     ActionResponseHelper.class)) {

            controller.cerrarGrupo(actionRequest, actionResponse);

            verify(responseHelper.constructed().get(0)).doResponseBusinessMessagesAsError(businessMessages);
            verify(grupoService, never()).cerrarGrupo(any(), any());
            verify(actionResponse, never()).setReload(anyBoolean());
        }
    }

    @Test
    void reabrirGrupo_sinErroresDeValidacion_ejecutaReabrirYRefresca() {
        Grupo original = new Grupo();
        Grupo grupo = new Grupo();
        when(grupoService.validateReabrirGrupo(grupo, original)).thenReturn(Optional.empty());

        try (MockedConstruction<ActionRequestHelper> requestHelper = mockConstruction(
                ActionRequestHelper.class,
                (mock, context) -> {
                    when(mock.getOriginalModel()).thenReturn(original);
                    when(mock.getModel(any())).thenReturn(grupo);
                });
             MockedConstruction<ActionResponseHelper> responseHelper = mockConstruction(
                     ActionResponseHelper.class)) {

            controller.reabrirGrupo(actionRequest, actionResponse);

            verify(grupoService).reabrirGrupo(grupo, original);
            verify(actionResponse).setReload(true);
            verify(responseHelper.constructed().get(0), never())
                    .doResponseBusinessMessagesAsError(any());
        }
    }

    @Test
    void reabrirGrupo_conErroresDeValidacion_respondeBusinessMessagesYNoReabre() {
        Grupo original = new Grupo();
        Grupo grupo = new Grupo();
        BusinessMessages businessMessages = Mockito.mock(BusinessMessages.class);
        when(grupoService.validateReabrirGrupo(grupo, original)).thenReturn(Optional.of(businessMessages));

        try (MockedConstruction<ActionRequestHelper> requestHelper = mockConstruction(
                ActionRequestHelper.class,
                (mock, context) -> {
                    when(mock.getOriginalModel()).thenReturn(original);
                    when(mock.getModel(any())).thenReturn(grupo);
                });
             MockedConstruction<ActionResponseHelper> responseHelper = mockConstruction(
                     ActionResponseHelper.class)) {

            controller.reabrirGrupo(actionRequest, actionResponse);

            verify(responseHelper.constructed().get(0)).doResponseBusinessMessagesAsError(businessMessages);
            verify(grupoService, never()).reabrirGrupo(any(), any());
            verify(actionResponse, never()).setReload(anyBoolean());
        }
    }
}
