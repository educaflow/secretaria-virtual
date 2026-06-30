package com.educaflow.subsystem.smoketest.controller;

import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper;
import com.educaflow.subsystem.smoketest.db.SmokeTest;
import com.educaflow.subsystem.smoketest.service.SmokeTestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmokeTestControllerTest {

    private ModelServiceFactory modelServiceFactory;
    private SmokeTestService smokeTestService;
    private ActionRequest actionRequest;
    private ActionResponse actionResponse;
    private SmokeTestController controller;

    @BeforeEach
    void setUp() throws Exception {
        modelServiceFactory = Mockito.mock(ModelServiceFactory.class);
        smokeTestService = Mockito.mock(SmokeTestService.class);
        actionRequest = Mockito.mock(ActionRequest.class);
        actionResponse = Mockito.mock(ActionResponse.class);

        when(modelServiceFactory.resolve(SmokeTest.class)).thenReturn(smokeTestService);

        controller = new SmokeTestController();
        Field field = SmokeTestController.class.getDeclaredField("modelServiceFactory");
        field.setAccessible(true);
        field.set(controller, modelServiceFactory);
    }

    @Test
    void validateSave_altaTextoValido_noRespondeError() {
        SmokeTest smokeTest = new SmokeTest();
        smokeTest.setTexto("Prueba de humo 1");
        when(smokeTestService.validateInsert(smokeTest)).thenReturn(Optional.empty());

        try (MockedConstruction<ActionRequestHelper> requestHelper = mockConstruction(
                ActionRequestHelper.class,
                (mock, context) -> {
                    when(mock.getId()).thenReturn(null);
                    when(mock.getOriginalModel()).thenReturn(null);
                    when(mock.getModel(any())).thenReturn(smokeTest);
                });
             MockedConstruction<ActionResponseHelper> responseHelper = mockConstruction(
                     ActionResponseHelper.class)) {

            controller.validateSave(actionRequest, actionResponse);

            verify(smokeTestService).validateInsert(smokeTest);
            verify(smokeTestService, never()).validateUpdate(any(), any());
            verify(responseHelper.constructed().get(0), never())
                    .doResponseBusinessMessagesAsError(any());
        }
    }

    @Test
    void validateSave_altaTextoVacio_respondeBusinessMessagesError() {
        SmokeTest smokeTest = new SmokeTest();
        smokeTest.setTexto("");
        BusinessMessages businessMessages = Mockito.mock(BusinessMessages.class);
        when(smokeTestService.validateInsert(smokeTest)).thenReturn(Optional.of(businessMessages));

        try (MockedConstruction<ActionRequestHelper> requestHelper = mockConstruction(
                ActionRequestHelper.class,
                (mock, context) -> {
                    when(mock.getId()).thenReturn(null);
                    when(mock.getOriginalModel()).thenReturn(null);
                    when(mock.getModel(any())).thenReturn(smokeTest);
                });
             MockedConstruction<ActionResponseHelper> responseHelper = mockConstruction(
                     ActionResponseHelper.class)) {

            controller.validateSave(actionRequest, actionResponse);

            verify(responseHelper.constructed().get(0)).doResponseBusinessMessagesAsError(businessMessages);
        }
    }

    @Test
    void validateSave_modificacionTextoValido_noRespondeError() {
        SmokeTest original = new SmokeTest();
        original.setTexto("Prueba de humo 3");
        SmokeTest smokeTest = new SmokeTest();
        smokeTest.setTexto("Prueba de humo 3 editada");
        when(smokeTestService.validateUpdate(smokeTest, original)).thenReturn(Optional.empty());

        try (MockedConstruction<ActionRequestHelper> requestHelper = mockConstruction(
                ActionRequestHelper.class,
                (mock, context) -> {
                    when(mock.getId()).thenReturn(1L);
                    when(mock.getOriginalModel()).thenReturn(original);
                    when(mock.getModel(any())).thenReturn(smokeTest);
                });
             MockedConstruction<ActionResponseHelper> responseHelper = mockConstruction(
                     ActionResponseHelper.class)) {

            controller.validateSave(actionRequest, actionResponse);

            verify(smokeTestService).validateUpdate(smokeTest, original);
            verify(smokeTestService, never()).validateInsert(any());
            verify(responseHelper.constructed().get(0), never())
                    .doResponseBusinessMessagesAsError(any());
        }
    }

    @Test
    void validateSave_modificacionTextoVacio_respondeBusinessMessagesError() {
        SmokeTest original = new SmokeTest();
        original.setTexto("Prueba de humo 3");
        SmokeTest smokeTest = new SmokeTest();
        smokeTest.setTexto("");
        BusinessMessages businessMessages = Mockito.mock(BusinessMessages.class);
        when(smokeTestService.validateUpdate(smokeTest, original)).thenReturn(Optional.of(businessMessages));

        try (MockedConstruction<ActionRequestHelper> requestHelper = mockConstruction(
                ActionRequestHelper.class,
                (mock, context) -> {
                    when(mock.getId()).thenReturn(1L);
                    when(mock.getOriginalModel()).thenReturn(original);
                    when(mock.getModel(any())).thenReturn(smokeTest);
                });
             MockedConstruction<ActionResponseHelper> responseHelper = mockConstruction(
                     ActionResponseHelper.class)) {

            controller.validateSave(actionRequest, actionResponse);

            verify(responseHelper.constructed().get(0)).doResponseBusinessMessagesAsError(businessMessages);
            verify(smokeTestService, never()).validateInsert(any());
        }
    }
}
