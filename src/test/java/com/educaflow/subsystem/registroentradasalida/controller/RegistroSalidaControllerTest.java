package com.educaflow.subsystem.registroentradasalida.controller;

import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.educaflow.base.infrastructure.fichero.Fichero;
import com.educaflow.subsystem.registroentradasalida.db.RegistroSalida;
import com.educaflow.subsystem.registroentradasalida.service.RegistroSalidaService;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistroSalidaControllerTest {

    private static final String CSV = "0123456789ABCDEFGHJKMNPQRS";
    /** Axelor solo deja entrar sin haber iniciado sesión a lo que cuelga de esta ruta. */
    private static final String RUTA_SIN_LOGIN = "/public/";

    private RegistroSalidaController controller;
    private RegistroSalidaService registroSalidaService;

    @BeforeEach
    void setUp() throws Exception {
        registroSalidaService = Mockito.mock(RegistroSalidaService.class);
        ModelServiceFactory modelServiceFactory = Mockito.mock(ModelServiceFactory.class);
        when(modelServiceFactory.resolve(RegistroSalida.class)).thenReturn(registroSalidaService);

        controller = new RegistroSalidaController();
        Field field = RegistroSalidaController.class.getDeclaredField("modelServiceFactory");
        field.setAccessible(true);
        field.set(controller, modelServiceFactory);
    }

    @Test
    void laRutaDeLaDescargaNoPideIniciarSesion() {
        assertTrue(RegistroSalidaService.RUTA_DESCARGA_PUBLICA.startsWith(RUTA_SIN_LOGIN));
    }

    @Test
    void getDescargaByCsv_conElCsvDeUnRegistro_devuelveSuFichero() {
        byte[] contenido = "el documento".getBytes(StandardCharsets.UTF_8);
        when(registroSalidaService.validateGetDescargaByCsv(CSV)).thenReturn(Optional.empty());
        when(registroSalidaService.getDescargaByCsv(CSV)).thenReturn(new Fichero("resolucion.pdf", contenido, "application/pdf"));

        Response response = controller.getDescargaByCsv(CSV);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertArrayEquals(contenido, (byte[]) response.getEntity());
        assertEquals(MediaType.valueOf("application/pdf"), response.getMediaType());
        assertTrue(response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION).contains("resolucion.pdf"));
    }

    @Test
    void getDescargaByCsv_conUnCsvQueNoEsDeNingunRegistro_respondeQueNoExisteSinIntentarLaDescarga() {
        when(registroSalidaService.validateGetDescargaByCsv(CSV)).thenReturn(Optional.of(BusinessMessages.single("No existe")));

        Response response = controller.getDescargaByCsv(CSV);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        verify(registroSalidaService, never()).getDescargaByCsv(any());
    }

    @Test
    void validateGetDescargaByCsv_conElCsvDeUnRegistro_respondeQueEsValido() {
        when(registroSalidaService.validateGetDescargaByCsv(CSV)).thenReturn(Optional.empty());

        Response response = controller.validateGetDescargaByCsv(CSV);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    void validateGetDescargaByCsv_conUnCsvQueNoEsDeNingunRegistro_respondeConLosMensajesDeLaValidacion() {
        BusinessMessages businessMessages = BusinessMessages.single("No existe");
        when(registroSalidaService.validateGetDescargaByCsv(CSV)).thenReturn(Optional.of(businessMessages));

        Response response = controller.validateGetDescargaByCsv(CSV);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(Map.of("errors", businessMessages), response.getEntity());
    }

}
