package com.educaflow.subsystem.security.controller;

import com.axelor.db.modelservice.ModelServiceFactory;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.security.db.AceProfileCentro;
import com.educaflow.subsystem.security.service.AceProfileCentroService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AceProfileCentroControllerTest {

    private AceProfileCentroController controller;

    private ModelServiceFactory modelServiceFactory;
    private AceProfileCentroService aceProfileCentroService;

    @BeforeEach
    void setUp() throws Exception {
        controller = new AceProfileCentroController();

        modelServiceFactory = Mockito.mock(ModelServiceFactory.class);
        setField(controller, "modelServiceFactory", modelServiceFactory);

        aceProfileCentroService = Mockito.mock(AceProfileCentroService.class);
        when(modelServiceFactory.resolve(AceProfileCentro.class)).thenReturn(aceProfileCentroService);
    }

    @Test
    void idsCentrosSupervisados_supervisorDeDosCentros_devuelveSusIds() {
        when(aceProfileCentroService.getCentrosSupervisados()).thenReturn(List.of(centro(1L), centro(2L)));

        List<Long> ids = controller.idsCentrosSupervisados();

        assertEquals(List.of(1L, 2L), ids);
        verify(modelServiceFactory).resolve(AceProfileCentro.class);
        verify(aceProfileCentroService).getCentrosSupervisados();
    }

    @Test
    void idsCentrosSupervisados_supervisorDeUnCentro_devuelveSuUnicoId() {
        when(aceProfileCentroService.getCentrosSupervisados()).thenReturn(List.of(centro(1L)));

        List<Long> ids = controller.idsCentrosSupervisados();

        assertEquals(1, ids.size());
        assertEquals(List.of(1L), ids);
    }

    @Test
    void idsCentrosSupervisados_sinCentrosSupervisados_devuelveCentinelaMenosUno() {
        when(aceProfileCentroService.getCentrosSupervisados()).thenReturn(List.of());

        List<Long> ids = controller.idsCentrosSupervisados();

        assertEquals(List.of(-1L), ids);
    }

    private static Centro centro(Long id) {
        Centro centro = new Centro();
        centro.setId(id);
        return centro;
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = AceProfileCentroController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
