package com.educaflow.system.gruposnotas.controller;

import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.system.gruposnotas.db.Grupo;
import com.educaflow.system.gruposnotas.service.GrupoService;
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
 * Tests unitarios de {@link GrupoController}.
 *
 * <p>Verifican que el controlador se limita a delegar en {@link GrupoService}: resuelve el
 * servicio vía {@code ModelServiceFactory}, extrae el modelo original y el modelo entrante con
 * el {@link AllowProperties} de la acción mediante {@link ActionRequestHelper}, y llama al método
 * de negocio correspondiente con {@code (grupo, grupoOriginal)}. La mecánica interna del
 * {@code ActionRequestHelper} se aísla con {@code mockConstruction}.</p>
 */
@ExtendWith(MockitoExtension.class)
class GrupoControllerTest {

    private ModelServiceFactory modelServiceFactory;
    private GrupoService grupoService;
    private ActionRequest actionRequest;
    private ActionResponse actionResponse;
    private GrupoController controller;

    private final Grupo grupo = new Grupo();
    private final Grupo grupoOriginal = new Grupo();

    @BeforeEach
    void setUp() throws Exception {
        modelServiceFactory = mock(ModelServiceFactory.class);
        grupoService = mock(GrupoService.class);
        actionRequest = mock(ActionRequest.class);
        actionResponse = mock(ActionResponse.class);

        controller = new GrupoController();
        setField(controller, "modelServiceFactory", modelServiceFactory);

        doReturn(grupoService).when(modelServiceFactory).resolve(Grupo.class);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void cerrar_delegaEnServicioConBeanYOriginal() {
        AllowProperties allowProperties = mock(AllowProperties.class);
        when(grupoService.allowPropertiesCerrar()).thenReturn(allowProperties);
        when(grupoService.cerrar(grupo, grupoOriginal)).thenReturn(grupo);

        try (MockedConstruction<ActionRequestHelper> construction = mockConstruction(
                ActionRequestHelper.class,
                (mock, context) -> {
                    when(mock.getOriginalModel()).thenReturn(grupoOriginal);
                    when(mock.getModel(any())).thenReturn(grupo);
                })) {

            controller.cerrar(actionRequest, actionResponse);
        }

        verify(grupoService).cerrar(grupo, grupoOriginal);
        verify(grupoService).allowPropertiesCerrar();
    }

    @Test
    void reabrir_delegaEnServicioConBeanYOriginal() {
        AllowProperties allowProperties = mock(AllowProperties.class);
        when(grupoService.allowPropertiesReabrir()).thenReturn(allowProperties);
        when(grupoService.reabrir(grupo, grupoOriginal)).thenReturn(grupo);

        try (MockedConstruction<ActionRequestHelper> construction = mockConstruction(
                ActionRequestHelper.class,
                (mock, context) -> {
                    when(mock.getOriginalModel()).thenReturn(grupoOriginal);
                    when(mock.getModel(any())).thenReturn(grupo);
                })) {

            controller.reabrir(actionRequest, actionResponse);
        }

        verify(grupoService).reabrir(grupo, grupoOriginal);
        verify(grupoService).allowPropertiesReabrir();
    }
}
