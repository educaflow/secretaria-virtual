package com.educaflow.tramites.util.registro;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.axelor.meta.db.MetaFile;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.Persona;
import com.educaflow.subsystem.expedientes.db.HistorialEstado;
import com.educaflow.subsystem.expedientes.db.PruebaV1;
import com.educaflow.subsystem.notificaciones.db.Adjunto;
import com.educaflow.subsystem.notificaciones.db.Correo;
import com.educaflow.subsystem.notificaciones.db.Sms;
import com.educaflow.subsystem.notificaciones.service.CorreoService;
import com.educaflow.subsystem.notificaciones.service.SmsService;
import com.educaflow.subsystem.registroentradasalida.db.RegistroEntrada;
import com.educaflow.subsystem.registroentradasalida.db.RegistroSalida;
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
class AvisoRegistroHelperTest {

    private static final String NUMERO_EXPEDIENTE = "00007/2026-46012345";
    private static final String NUMERO_REGISTRO_ENTRADA = "00031/2026";
    private static final String NUMERO_REGISTRO_SALIDA = "00012/2026-46012345";
    private static final LocalDateTime T = LocalDateTime.of(2026, 10, 6, 12, 0);

    @Mock
    private ModelServiceFactory modelServiceFactory;

    @Mock
    private CorreoService correoService;

    @Mock
    private SmsService smsService;

    @InjectMocks
    private AvisoRegistroHelper avisoRegistroHelper;

    private MockedStatic<I18n> i18nMock;

    private final Centro centro = new Centro();
    private final PruebaV1 expediente = new PruebaV1();
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
        solicitante.setTelefono("600111222");

        expediente.setPersonaSolicitante(solicitante);
        expediente.setCentro(centro);
        expediente.setNumeroExpediente(NUMERO_EXPEDIENTE);
        expediente.setName("Prueba V1");
        expediente.setHistorialEstados(new ArrayList<>(List.of(
                historialConFecha(T.minusHours(2)), historialMasReciente, historialConFecha(T.minusHours(3)))));

        when(modelServiceFactory.resolve(Correo.class)).thenReturn(correoService);
        when(modelServiceFactory.resolve(Sms.class)).thenReturn(smsService);
        when(correoService.create()).thenAnswer(invocacion -> new Correo());
        when(smsService.create()).thenAnswer(invocacion -> new Sms());
    }

    @AfterEach
    void cerrarEstaticos() {
        i18nMock.close();
    }

    @Test
    void avisarDeRegistroEntrada_elSolicitanteTieneCorreoYMovil_leEnviaElResguardoPorCorreoYElNumeroPorSms() {
        MetaFile resguardo = pdf("resguardo.pdf");
        RegistroEntrada registroEntrada = new RegistroEntrada();
        registroEntrada.setNumeroRegistro(NUMERO_REGISTRO_ENTRADA);
        registroEntrada.setDocumentoResguardoPresentacion(resguardo);
        aceptarTodo();

        avisoRegistroHelper.avisarDeRegistroEntrada(expediente, registroEntrada);

        Correo correo = correoInsertado();
        Sms sms = smsInsertado();
        Adjunto adjunto = correo.getAdjuntos().get(0);
        assertAll(
                () -> assertEquals("ana@example.com", correo.getPara()),
                () -> assertEquals("93882914L", correo.getDniDestinatario()),
                () -> assertEquals("Ana", correo.getNombre()),
                () -> assertEquals("García López", correo.getApellidos()),
                () -> assertSame(centro, correo.getCentro()),
                () -> assertSame(historialMasReciente, correo.getHistorialEstado()),
                () -> assertTrue(correo.getName().contains(NUMERO_REGISTRO_ENTRADA)),
                () -> assertTrue(correo.getAsunto().contains(NUMERO_REGISTRO_ENTRADA)),
                () -> assertTrue(correo.getAsunto().contains(NUMERO_EXPEDIENTE)),
                () -> assertTrue(correo.getCuerpo().contains("Prueba V1")),
                () -> assertEquals(1, correo.getAdjuntos().size()),
                () -> assertSame(resguardo, adjunto.getContenido()),
                () -> assertEquals("resguardo.pdf", adjunto.getNombreFichero()),
                () -> assertSame(correo, adjunto.getCorreo()),
                () -> assertEquals("600111222", sms.getTelefono()),
                () -> assertEquals("93882914L", sms.getDniDestinatario()),
                () -> assertSame(centro, sms.getCentro()),
                () -> assertSame(historialMasReciente, sms.getHistorialEstado()),
                () -> assertTrue(sms.getMensaje().contains(NUMERO_REGISTRO_ENTRADA)),
                () -> assertTrue(sms.getMensaje().contains(NUMERO_EXPEDIENTE)));
    }

    @Test
    void avisarDeRegistroSalida_elSolicitanteTieneCorreoYMovil_leEnviaElDocumentoPorCorreoYElNumeroPorSms() {
        MetaFile documento = pdf("resolucion.pdf");
        RegistroSalida registroSalida = new RegistroSalida();
        registroSalida.setNumeroRegistro(NUMERO_REGISTRO_SALIDA);
        registroSalida.setDocumento(documento);
        aceptarTodo();

        avisoRegistroHelper.avisarDeRegistroSalida(expediente, registroSalida);

        Correo correo = correoInsertado();
        Sms sms = smsInsertado();
        assertAll(
                () -> assertTrue(correo.getAsunto().contains(NUMERO_REGISTRO_SALIDA)),
                () -> assertSame(documento, correo.getAdjuntos().get(0).getContenido()),
                () -> assertEquals("resolucion.pdf", correo.getAdjuntos().get(0).getNombreFichero()),
                () -> assertTrue(sms.getMensaje().contains(NUMERO_REGISTRO_SALIDA)));
    }

    @Test
    void avisarDeRegistroEntrada_elCorreoNoSuperaLaValidacion_soloEnviaElSms() {
        when(correoService.validateInsert(any(Correo.class)))
                .thenReturn(Optional.of(BusinessMessages.single("Debe indicar al menos un destinatario en el «para»")));
        when(smsService.validateInsert(any(Sms.class))).thenReturn(Optional.empty());

        avisoRegistroHelper.avisarDeRegistroEntrada(expediente, registroEntrada());

        verify(correoService, never()).insert(any(Correo.class));
        verify(smsService).insert(any(Sms.class));
    }

    @Test
    void avisarDeRegistroEntrada_elSmsNoSuperaLaValidacion_soloEnviaElCorreo() {
        when(correoService.validateInsert(any(Correo.class))).thenReturn(Optional.empty());
        when(smsService.validateInsert(any(Sms.class)))
                .thenReturn(Optional.of(BusinessMessages.single("El teléfono es obligatorio")));

        avisoRegistroHelper.avisarDeRegistroEntrada(expediente, registroEntrada());

        verify(correoService).insert(any(Correo.class));
        verify(smsService, never()).insert(any(Sms.class));
    }

    private void aceptarTodo() {
        when(correoService.validateInsert(any(Correo.class))).thenReturn(Optional.empty());
        when(smsService.validateInsert(any(Sms.class))).thenReturn(Optional.empty());
    }

    private RegistroEntrada registroEntrada() {
        RegistroEntrada registroEntrada = new RegistroEntrada();
        registroEntrada.setNumeroRegistro(NUMERO_REGISTRO_ENTRADA);
        registroEntrada.setDocumentoResguardoPresentacion(pdf("resguardo.pdf"));
        return registroEntrada;
    }

    private Correo correoInsertado() {
        ArgumentCaptor<Correo> correo = ArgumentCaptor.forClass(Correo.class);
        verify(correoService).insert(correo.capture());
        return correo.getValue();
    }

    private Sms smsInsertado() {
        ArgumentCaptor<Sms> sms = ArgumentCaptor.forClass(Sms.class);
        verify(smsService).insert(sms.capture());
        return sms.getValue();
    }

    private static MetaFile pdf(String nombre) {
        MetaFile metaFile = new MetaFile();
        metaFile.setFileName(nombre);
        return metaFile;
    }

    private static HistorialEstado historialConFecha(LocalDateTime fecha) {
        HistorialEstado historialEstado = new HistorialEstado();
        historialEstado.setFecha(fecha);
        return historialEstado;
    }
}
