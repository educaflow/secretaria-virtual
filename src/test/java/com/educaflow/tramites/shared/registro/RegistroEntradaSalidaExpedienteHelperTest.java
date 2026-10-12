package com.educaflow.tramites.shared.registro;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.subsystem.expedientes.db.ExpedienteResoluble;
import com.educaflow.subsystem.expedientes.db.ResultadoVerificacion;
import com.educaflow.subsystem.registroentradasalida.db.RegistroEntrada;
import com.educaflow.subsystem.registroentradasalida.db.RegistroSalida;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.EventContext;
import com.educaflow.tramites.shared.notificaciones.NotificacionesExpedienteHelper;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistroEntradaSalidaExpedienteHelperTest {

    @Mock
    private NotificacionesExpedienteHelper notificacionesExpedienteHelper;

    @InjectMocks
    private RegistroEntradaSalidaExpedienteHelper registroEntradaSalidaExpedienteHelper;

    private final ExpedienteResoluble expediente = new ExpedienteResoluble();

    @Test
    void registroEntradaSolicitud_registraLaSolicitudFirmadaGuardaElResguardoConSuFechaAvisaAlSolicitanteYBorraLaSubsanacion() throws BusinessException {
        MetaFile anexo = new MetaFile();
        MetaFile resguardo = new MetaFile();
        RegistroEntrada registroEntrada = new RegistroEntrada();
        registroEntrada.setDocumentoResguardoPresentacion(resguardo);
        LocalDateTime fechaAsiento = LocalDateTime.of(2026, 9, 1, 10, 0);
        registroEntrada.setFecha(fechaAsiento);
        MetaFile pdfSolicitudFirmada = new MetaFile();
        LocalDateTime fechaFirma = LocalDateTime.of(2026, 8, 31, 18, 30);
        expediente.setPdfSolicitudFirmada(pdfSolicitudFirmada);
        expediente.setFechaHoraPdfSolicitudFirmada(fechaFirma);
        expediente.setResultadoVerificacion(ResultadoVerificacion.SUBSANAR);
        expediente.setTextoSubsanacion("Falta el justificante");
        EventContext eventContext = mock(EventContext.class);
        when(eventContext.createRegistroEntrada(pdfSolicitudFirmada, List.of(anexo))).thenReturn(registroEntrada);

        registroEntradaSalidaExpedienteHelper.registroEntradaSolicitud(expediente, List.of(anexo), eventContext);

        assertAll(
                () -> assertSame(resguardo, expediente.getPdfJustificanteRegistroEntrada()),
                () -> assertSame(fechaAsiento, expediente.getFechaHoraPdfJustificanteRegistroEntrada()),
                // La fecha de la firma es la del PDF, no la del asiento: el registro no la toca.
                () -> assertSame(fechaFirma, expediente.getFechaHoraPdfSolicitudFirmada()),
                () -> assertNull(expediente.getResultadoVerificacion()),
                () -> assertNull(expediente.getTextoSubsanacion()));
        verify(notificacionesExpedienteHelper).notificarRegistroEntrada(expediente, registroEntrada);
    }

    @Test
    void registroSalidaResolucion_registraLaResolucionFirmadaGuardaElDocumentoRegistradoConSuFechaYAvisaAlSolicitante() throws BusinessException {
        MetaFile anexo = new MetaFile();
        MetaFile documentoRegistrado = new MetaFile();
        RegistroSalida registroSalida = new RegistroSalida();
        registroSalida.setDocumento(documentoRegistrado);
        LocalDateTime fechaAsiento = LocalDateTime.of(2026, 9, 3, 9, 15);
        registroSalida.setFecha(fechaAsiento);
        MetaFile pdfResolucionFirmada = new MetaFile();
        LocalDateTime fechaFirma = LocalDateTime.of(2026, 9, 2, 12, 30);
        expediente.setPdfResolucionFirmada(pdfResolucionFirmada);
        expediente.setFechaHoraPdfResolucionFirmada(fechaFirma);
        EventContext eventContext = mock(EventContext.class);
        when(eventContext.createRegistroSalida(pdfResolucionFirmada, List.of(anexo))).thenReturn(registroSalida);

        registroEntradaSalidaExpedienteHelper.registroSalidaResolucion(expediente, List.of(anexo), eventContext);

        assertAll(
                () -> assertSame(documentoRegistrado, expediente.getPdfResolucionFirmadaRegistroSalida()),
                () -> assertSame(fechaAsiento, expediente.getFechaHoraPdfResolucionFirmadaRegistroSalida()),
                () -> assertSame(pdfResolucionFirmada, expediente.getPdfResolucionFirmada()),
                () -> assertSame(fechaFirma, expediente.getFechaHoraPdfResolucionFirmada()));
        verify(notificacionesExpedienteHelper).notificarRegistroSalida(expediente, registroSalida);
    }
}
