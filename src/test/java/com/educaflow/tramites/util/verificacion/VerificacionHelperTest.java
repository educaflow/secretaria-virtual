package com.educaflow.tramites.util.verificacion;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.i18n.I18n;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.Persona;
import com.educaflow.subsystem.correos.db.Correo;
import com.educaflow.subsystem.correos.service.CorreoService;
import com.educaflow.subsystem.expedientes.db.PruebaV1;
import com.educaflow.subsystem.notificaciones.db.Correo;
import com.educaflow.subsystem.notificaciones.db.TipoNotificacion;
import com.educaflow.subsystem.notificaciones.service.CorreoService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
class VerificacionHelperTest {

    private static final String TEXTO_SUBSANACION = "Falta el justificante del segundo día";
    private static final LocalDateTime T = LocalDateTime.of(2026, 10, 6, 12, 0);

    @Mock
    private ModelServiceFactory modelServiceFactory;

    @Mock
    private CorreoService correoService;

    @InjectMocks
    private VerificacionHelper verificacionHelper;

    private MockedStatic<I18n> i18nMock;

    private final Centro centro = new Centro();
    private final PruebaV1 expediente = new PruebaV1();
    private final Correo correoCreado = new Correo();
    private final HistorialEstado historialMasReciente = historialConFecha(T.minusHours(1));

    @BeforeEach
    void preparar() {
        i18nMock = Mockito.mockStatic(I18n.class, Mockito.withSettings().strictness(Strictness.LENIENT));
        i18nMock.when(() -> I18n.get(anyString())).thenAnswer(invocacion -> invocacion.getArgument(0));

        Persona solicitante = new Persona();
        solicitante.setDni("93882914L");
        solicitante.setNombre("Ana");
        solicitante.setApellidos("García López");
        solicitante.setEmail("ana@example.com");

        expediente.setPersonaSolicitante(solicitante);
        expediente.setCentro(centro);
        expediente.setNumeroExpediente("00007/2026");
        expediente.setName("Prueba V1");
        expediente.setHistorialEstados(new ArrayList<>(List.of(
                historialConFecha(T.minusHours(2)), historialMasReciente, historialConFecha(T.minusHours(3)))));

        correoCreado.setTipoNotificacion(TipoNotificacion.CORREO);
        when(modelServiceFactory.resolve(Correo.class)).thenReturn(correoService);
        when(correoService.create()).thenReturn(correoCreado);
    }

    @AfterEach
    void cerrarEstaticos() {
        i18nMock.close();
    }

    @Test
    void avisarDeSubsanacion_elSolicitanteTieneCorreo_creaElCorreoConElTextoDeLaSubsanacion() {
        when(correoService.validateInsert(any(Correo.class))).thenReturn(Optional.empty());

        boolean avisado = verificacionHelper.avisarDeSubsanacion(expediente, TEXTO_SUBSANACION);

        ArgumentCaptor<Correo> correo = ArgumentCaptor.forClass(Correo.class);
        verify(correoService).insert(correo.capture());
        assertAll(
                () -> assertTrue(avisado),
                () -> assertSame(correoCreado, correo),
                () -> assertEquals(TipoNotificacion.CORREO, correo.getTipoNotificacion()),
                () -> assertEquals("ana@example.com", correo.getPara()),
                () -> assertEquals("93882914L", correo.getDniDestinatario()),
                () -> assertEquals("Ana", correo.getNombre()),
                () -> assertEquals("García López", correo.getApellidos()),
                () -> assertSame(centro, correo.getCentro()),
                () -> assertEquals("Subsanación del expediente 00007/2026", correo.getName()),
                () -> assertSame(historialMasReciente, correo.getHistorialEstado()),
                () -> assertTrue(correo.getAsunto().contains("00007/2026")),
                () -> assertTrue(correo.getCuerpo().contains(TEXTO_SUBSANACION)),
                () -> assertTrue(correo.getCuerpo().contains("Prueba V1")));
    }

    @Test
    void avisarDeSubsanacion_noSePuedeEscribirAlSolicitante_noCreaNingunCorreoYNoFalla() {
        expediente.getPersonaSolicitante().setEmail(null);
        when(correoService.validateInsert(any(Correo.class)))
                .thenReturn(Optional.of(BusinessMessages.single("Debe indicar al menos un destinatario en el «para»")));

        boolean avisado = verificacionHelper.avisarDeSubsanacion(expediente, TEXTO_SUBSANACION);

        assertFalse(avisado);
        verify(correoService, never()).insert(any(Correo.class));
    }

    @Test
    void avisarDeSubsanacion_unSoloHistorial_loLiga() {
        HistorialEstado unicoHistorial = historialConFecha(T);
        expediente.setHistorialEstados(new ArrayList<>(List.of(unicoHistorial)));
        when(correoService.validateInsert(any(Correo.class))).thenReturn(Optional.empty());

        verificacionHelper.avisarDeSubsanacion(expediente, TEXTO_SUBSANACION);

        assertSame(unicoHistorial, correoInsertado().getHistorialEstado());
    }

    private Correo correoInsertado() {
        ArgumentCaptor<Correo> correo = ArgumentCaptor.forClass(Correo.class);
        verify(correoService).insert(correo.capture());
        return correo.getValue();
    }

    private static HistorialEstado historialConFecha(LocalDateTime fecha) {
        HistorialEstado historialEstado = new HistorialEstado();
        historialEstado.setFecha(fecha);
        return historialEstado;
    }
}
