package com.educaflow.system.gruposnotas.controller;

import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.system.gruposnotas.db.Nota;
import com.educaflow.system.gruposnotas.service.NotaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de {@link NotaController}.
 *
 * <p>Verifican que el controlador se limita a delegar en {@link NotaService}: resuelve el
 * servicio vía {@code ModelServiceFactory}, extrae el modelo original y el modelo entrante con
 * el {@link AllowProperties} de la acción (whitelist {@code valor}) mediante
 * {@link ActionRequestHelper}, y llama a {@code guardarNota(nota, notaOriginal)}. La mecánica
 * interna del {@code ActionRequestHelper} se aísla con {@code mockConstruction}.</p>
 */
@ExtendWith(MockitoExtension.class)
class NotaControllerTest {

    private ModelServiceFactory modelServiceFactory;
    private NotaService notaService;
    private ActionRequest actionRequest;
    private ActionResponse actionResponse;
    private NotaController controller;

    private final Nota nota = new Nota();
    private final Nota notaOriginal = new Nota();

    @BeforeEach
    void setUp() throws Exception {
        modelServiceFactory = mock(ModelServiceFactory.class);
        notaService = mock(NotaService.class);
        actionRequest = mock(ActionRequest.class);
        actionResponse = mock(ActionResponse.class);

        controller = new NotaController();
        setField(controller, "modelServiceFactory", modelServiceFactory);

        doReturn(notaService).when(modelServiceFactory).resolve(Nota.class);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void guardarNota_delegaEnServicioConValorYOriginal() {
        AllowProperties allowProperties = mock(AllowProperties.class);
        when(notaService.allowPropertiesGuardarNota()).thenReturn(allowProperties);
        when(notaService.guardarNota(nota, notaOriginal)).thenReturn(nota);

        try (MockedConstruction<ActionRequestHelper> construction = mockConstruction(
                ActionRequestHelper.class,
                (mock, context) -> {
                    when(mock.getOriginalModel()).thenReturn(notaOriginal);
                    when(mock.getModel(any())).thenReturn(nota);
                })) {

            controller.guardarNota(actionRequest, actionResponse);
        }

        verify(notaService).guardarNota(nota, notaOriginal);
        verify(notaService).allowPropertiesGuardarNota();
    }
}
