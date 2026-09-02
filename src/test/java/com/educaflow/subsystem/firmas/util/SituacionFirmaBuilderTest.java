package com.educaflow.subsystem.firmas.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.axelor.auth.db.User;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.inject.Beans;
import com.educaflow.subsystem.criptografia.db.CertificadoDigital;
import com.educaflow.subsystem.criptografia.service.CertificadoDigitalService;
import com.educaflow.subsystem.criptografia.service.TipoAlmacenClave;
import com.educaflow.subsystem.firmas.db.SituacionFirma;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SituacionFirmaBuilderTest {

    // DNI válido de referencia de la spec (el mismo que usa CertificadoDigitalServiceImplTest).
    private static final String DNI_VALIDO = "85432016B";
    // Mismo cuerpo numérico con la letra de control equivocada.
    private static final String DNI_LETRA_INCORRECTA = "12345678A";

    private ModelServiceFactory modelServiceFactory;
    private CertificadoDigitalService certificadoDigitalService;
    private MockedStatic<Beans> beansMock;

    private ch.qos.logback.classic.Logger loggerDeLaClase;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        modelServiceFactory = Mockito.mock(ModelServiceFactory.class);
        certificadoDigitalService = Mockito.mock(CertificadoDigitalService.class);
        beansMock = Mockito.mockStatic(Beans.class);
    }

    @AfterEach
    void tearDown() {
        beansMock.close();
        if (logAppender != null) {
            loggerDeLaClase.detachAppender(logAppender);
            logAppender.stop();
            logAppender = null;
        }
    }

    /* ------------------------------------------------------------------ */
    /* Helpers                                                            */
    /* ------------------------------------------------------------------ */

    private static User usuarioConDni(String dni) {
        User user = new User();
        user.setDni(dni);
        return user;
    }

    private void stubResolucionDelServicio() {
        beansMock.when(() -> Beans.get(ModelServiceFactory.class)).thenReturn(modelServiceFactory);
        when(modelServiceFactory.resolve(CertificadoDigital.class)).thenReturn(certificadoDigitalService);
    }

    private void stubTipoAlmacenClave(TipoAlmacenClave tipoAlmacenClave) {
        stubResolucionDelServicio();
        when(certificadoDigitalService.getTipoAlmacenClaveByDni(DNI_VALIDO)).thenReturn(tipoAlmacenClave);
    }

    private void engancharAppenderAlLogger() {
        loggerDeLaClase = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(SituacionFirmaBuilder.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        loggerDeLaClase.addAppender(logAppender);
    }

    /* ------------------------------------------------------------------ */
    /* build(User)                                                        */
    /* ------------------------------------------------------------------ */

    @Test
    void build_firmanteNulo_devuelveSinDni() {
        SituacionFirma situacionFirma = SituacionFirmaBuilder.build(null);

        assertEquals(SituacionFirma.SIN_DNI, situacionFirma);
        verifyNoInteractions(modelServiceFactory);
        verifyNoInteractions(certificadoDigitalService);
    }

    @Test
    void build_firmanteSinDni_devuelveSinDni() {
        User firmante = usuarioConDni(null);

        SituacionFirma situacionFirma = SituacionFirmaBuilder.build(firmante);

        assertEquals(SituacionFirma.SIN_DNI, situacionFirma);
        verifyNoInteractions(modelServiceFactory);
        verifyNoInteractions(certificadoDigitalService);
    }

    @Test
    void build_firmanteConDniEnBlanco_devuelveSinDni() {
        User firmante = usuarioConDni("   ");

        SituacionFirma situacionFirma = SituacionFirmaBuilder.build(firmante);

        assertEquals(SituacionFirma.SIN_DNI, situacionFirma);
        verifyNoInteractions(modelServiceFactory);
        verifyNoInteractions(certificadoDigitalService);
    }

    @Test
    void build_firmanteConDniInvalido_devuelveSinDniYNoConsultaElCertificado() {
        User firmante = usuarioConDni(DNI_LETRA_INCORRECTA);

        SituacionFirma situacionFirma = SituacionFirmaBuilder.build(firmante);

        // Un DNI inválido es una situación, no un error de negocio: no se llega a consultar el
        // certificado (getTipoAlmacenClaveByDni abortaría con un error de negocio).
        assertEquals(SituacionFirma.SIN_DNI, situacionFirma);
        verify(certificadoDigitalService, never()).getTipoAlmacenClaveByDni(any());
    }

    @Test
    void build_sinCertificadoHabilitado_devuelveSinCertificado() {
        stubTipoAlmacenClave(null);

        SituacionFirma situacionFirma = SituacionFirmaBuilder.build(usuarioConDni(DNI_VALIDO));

        assertEquals(SituacionFirma.SIN_CERTIFICADO, situacionFirma);
    }

    @Test
    void build_tipoDispositivoConPin_devuelveDispositivoConPin() {
        stubTipoAlmacenClave(TipoAlmacenClave.DISPOSITIVO_CON_PIN);

        SituacionFirma situacionFirma = SituacionFirmaBuilder.build(usuarioConDni(DNI_VALIDO));

        assertEquals(SituacionFirma.DISPOSITIVO_CON_PIN, situacionFirma);
    }

    @Test
    void build_tipoDispositivoSinPin_devuelveDispositivoSinPin() {
        stubTipoAlmacenClave(TipoAlmacenClave.DISPOSITIVO_SIN_PIN);

        SituacionFirma situacionFirma = SituacionFirmaBuilder.build(usuarioConDni(DNI_VALIDO));

        assertEquals(SituacionFirma.DISPOSITIVO_SIN_PIN, situacionFirma);
    }

    @Test
    void build_tipoFicheroConClave_devuelveFicheroConClave() {
        stubTipoAlmacenClave(TipoAlmacenClave.FICHERO_CON_CLAVE);

        SituacionFirma situacionFirma = SituacionFirmaBuilder.build(usuarioConDni(DNI_VALIDO));

        assertEquals(SituacionFirma.FICHERO_CON_CLAVE, situacionFirma);
    }

    @Test
    void build_tipoFicheroSinClave_devuelveFicheroSinClave() {
        stubTipoAlmacenClave(TipoAlmacenClave.FICHERO_SIN_CLAVE);

        SituacionFirma situacionFirma = SituacionFirmaBuilder.build(usuarioConDni(DNI_VALIDO));

        assertEquals(SituacionFirma.FICHERO_SIN_CLAVE, situacionFirma);
    }

    @Test
    void build_elServicioDeCriptografiaFalla_degradaASinCertificadoSinPropagarLaExcepcion() {
        stubResolucionDelServicio();
        when(certificadoDigitalService.getTipoAlmacenClaveByDni(DNI_VALIDO))
                .thenThrow(new RuntimeException("fallo de BD"));

        SituacionFirma situacionFirma =
                assertDoesNotThrow(() -> SituacionFirmaBuilder.build(usuarioConDni(DNI_VALIDO)));

        // Valor seguro: deja al firmante el panel de AutoFirma y su «Atrás».
        assertEquals(SituacionFirma.SIN_CERTIFICADO, situacionFirma);
    }

    @Test
    void build_laResolucionDelServicioFalla_degradaASinCertificado() {
        beansMock.when(() -> Beans.get(ModelServiceFactory.class))
                .thenThrow(new RuntimeException("inyector no disponible"));

        SituacionFirma situacionFirma =
                assertDoesNotThrow(() -> SituacionFirmaBuilder.build(usuarioConDni(DNI_VALIDO)));

        // El try/catch cubre también la resolución del servicio, no solo la consulta.
        assertEquals(SituacionFirma.SIN_CERTIFICADO, situacionFirma);
    }

    @Test
    void build_elServicioDeCriptografiaFalla_noRegistraElDniCompletoEnElLog() {
        engancharAppenderAlLogger();
        stubResolucionDelServicio();
        when(certificadoDigitalService.getTipoAlmacenClaveByDni(DNI_VALIDO))
                .thenThrow(new RuntimeException("fallo de BD"));

        SituacionFirmaBuilder.build(usuarioConDni(DNI_VALIDO));

        List<ILoggingEvent> eventos = List.copyOf(logAppender.list);
        assertTrue(eventos.stream().anyMatch(evento -> evento.getLevel() == Level.ERROR),
                "Se esperaba al menos un evento de error en el log");
        assertFalse(eventos.stream().anyMatch(this::contieneElDniCompleto),
                "Ninguna línea del log debe contener el DNI completo: debe ir enmascarado");
    }

    private boolean contieneElDniCompleto(ILoggingEvent evento) {
        if (evento.getFormattedMessage() != null && evento.getFormattedMessage().contains(DNI_VALIDO)) {
            return true;
        }
        Object[] argumentos = evento.getArgumentArray();
        if (argumentos == null) {
            return false;
        }
        return Arrays.stream(argumentos)
                .filter(argumento -> argumento != null)
                .anyMatch(argumento -> argumento.toString().contains(DNI_VALIDO));
    }
}
