package com.educaflow.subsystem.criptografia.util;

import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.inject.Beans;
import com.educaflow.subsystem.criptografia.db.CertificadoDigital;
import com.educaflow.subsystem.criptografia.service.CertificadoDigitalService;
import com.educaflow.subsystem.criptografia.service.SituacionFirma;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificadoDigitalHelperTest {

    // DNI válido de referencia de la spec (el mismo que usa CertificadoDigitalServiceImplTest).
    private static final String DNI_VALIDO = "85432016B";
    // Mismo cuerpo numérico con la letra de control equivocada.
    private static final String DNI_LETRA_INCORRECTA = "12345678A";

    private ModelServiceFactory modelServiceFactory;
    private CertificadoDigitalService certificadoDigitalService;
    private MockedStatic<Beans> beansMock;

    @BeforeEach
    void setUp() {
        modelServiceFactory = Mockito.mock(ModelServiceFactory.class);
        certificadoDigitalService = Mockito.mock(CertificadoDigitalService.class);
        beansMock = Mockito.mockStatic(Beans.class);
    }

    @AfterEach
    void tearDown() {
        beansMock.close();
    }

    /* ------------------------------------------------------------------ */
    /* Helpers                                                            */
    /* ------------------------------------------------------------------ */

    private void stubResolucionDelServicio() {
        beansMock.when(() -> Beans.get(ModelServiceFactory.class)).thenReturn(modelServiceFactory);
        when(modelServiceFactory.resolve(CertificadoDigital.class)).thenReturn(certificadoDigitalService);
    }

    /**
     * Situación de firma que devuelve el servicio de criptografía para el certificado del firmante. El
     * servicio dice cada caso con su valor del enum —«no tiene certificado» es {@code SIN_CERTIFICADO}— y
     * nunca con {@code null}.
     */
    private void stubSituacionFirmaDelCertificado(SituacionFirma situacionFirma) {
        stubResolucionDelServicio();
        when(certificadoDigitalService.getSituacionFirmaByDni(DNI_VALIDO)).thenReturn(situacionFirma);
    }

    /* ------------------------------------------------------------------ */
    /* getSituacionFirmaByDni(String)                                     */
    /* ------------------------------------------------------------------ */

    @Test
    void getSituacionFirmaByDni_dniNulo_devuelveSinDni() {
        SituacionFirma situacionFirma = CertificadoDigitalHelper.getSituacionFirmaByDni(null);

        assertEquals(SituacionFirma.SIN_DNI, situacionFirma);
        verifyNoInteractions(modelServiceFactory);
        verifyNoInteractions(certificadoDigitalService);
    }

    @Test
    void getSituacionFirmaByDni_dniEnBlanco_devuelveSinDni() {
        SituacionFirma situacionFirma = CertificadoDigitalHelper.getSituacionFirmaByDni("   ");

        assertEquals(SituacionFirma.SIN_DNI, situacionFirma);
        verifyNoInteractions(modelServiceFactory);
        verifyNoInteractions(certificadoDigitalService);
    }

    @Test
    void getSituacionFirmaByDni_dniInvalido_devuelveSinDniYNoConsultaElCertificado() {
        SituacionFirma situacionFirma = CertificadoDigitalHelper.getSituacionFirmaByDni(DNI_LETRA_INCORRECTA);

        // Un DNI inválido es una situación, no un error de negocio: no se llega a consultar el
        // certificado (getSituacionFirmaByDni del servicio abortaría con un error de negocio).
        assertEquals(SituacionFirma.SIN_DNI, situacionFirma);
        verify(certificadoDigitalService, never()).getSituacionFirmaByDni(any());
    }

    @Test
    void getSituacionFirmaByDni_sinCertificadoHabilitado_devuelveSinCertificado() {
        stubSituacionFirmaDelCertificado(SituacionFirma.SIN_CERTIFICADO);

        SituacionFirma situacionFirma = CertificadoDigitalHelper.getSituacionFirmaByDni(DNI_VALIDO);

        assertEquals(SituacionFirma.SIN_CERTIFICADO, situacionFirma);
    }

    @Test
    void getSituacionFirmaByDni_elServicioDevuelveNull_degradaASinCertificado() {
        // El servicio no devuelve null hoy, pero este método promete no devolverlo nunca a quien lo llama:
        // si algún día lo hiciera, sale el valor seguro y no un null que reventaría aguas abajo.
        stubSituacionFirmaDelCertificado(null);

        SituacionFirma situacionFirma = CertificadoDigitalHelper.getSituacionFirmaByDni(DNI_VALIDO);

        assertEquals(SituacionFirma.SIN_CERTIFICADO, situacionFirma);
    }

    @Test
    void getSituacionFirmaByDni_certificadoEnDispositivoConPin_devuelveDispositivoConPin() {
        stubSituacionFirmaDelCertificado(SituacionFirma.DISPOSITIVO_CON_PIN);

        SituacionFirma situacionFirma = CertificadoDigitalHelper.getSituacionFirmaByDni(DNI_VALIDO);

        assertEquals(SituacionFirma.DISPOSITIVO_CON_PIN, situacionFirma);
    }

    @Test
    void getSituacionFirmaByDni_certificadoEnDispositivoSinPin_devuelveDispositivoSinPin() {
        stubSituacionFirmaDelCertificado(SituacionFirma.DISPOSITIVO_SIN_PIN);

        SituacionFirma situacionFirma = CertificadoDigitalHelper.getSituacionFirmaByDni(DNI_VALIDO);

        assertEquals(SituacionFirma.DISPOSITIVO_SIN_PIN, situacionFirma);
    }

    @Test
    void getSituacionFirmaByDni_certificadoEnFicheroConClave_devuelveFicheroConClave() {
        stubSituacionFirmaDelCertificado(SituacionFirma.FICHERO_CON_CLAVE);

        SituacionFirma situacionFirma = CertificadoDigitalHelper.getSituacionFirmaByDni(DNI_VALIDO);

        assertEquals(SituacionFirma.FICHERO_CON_CLAVE, situacionFirma);
    }

    @Test
    void getSituacionFirmaByDni_certificadoEnFicheroSinClave_devuelveFicheroSinClave() {
        stubSituacionFirmaDelCertificado(SituacionFirma.FICHERO_SIN_CLAVE);

        SituacionFirma situacionFirma = CertificadoDigitalHelper.getSituacionFirmaByDni(DNI_VALIDO);

        assertEquals(SituacionFirma.FICHERO_SIN_CLAVE, situacionFirma);
    }

    // Un fallo del subsistema de criptografía es un error técnico, no una situación de firma: el helper NO
    // lo degrada a SIN_CERTIFICADO ni lo registra, lo deja subir. Tragárselo daría por buena una respuesta
    // inventada («no tiene certificado») ante una avería, y el firmante vería el panel equivocado sin que
    // nadie se entere de que el subsistema está caído.
    @Test
    void getSituacionFirmaByDni_elServicioDeCriptografiaFalla_propagaLaExcepcion() {
        stubResolucionDelServicio();
        RuntimeException fallo = new RuntimeException("fallo de BD");
        when(certificadoDigitalService.getSituacionFirmaByDni(DNI_VALIDO)).thenThrow(fallo);

        RuntimeException excepcion = assertThrows(RuntimeException.class,
                () -> CertificadoDigitalHelper.getSituacionFirmaByDni(DNI_VALIDO));

        assertSame(fallo, excepcion);
    }

    @Test
    void getSituacionFirmaByDni_laResolucionDelServicioFalla_propagaLaExcepcion() {
        RuntimeException fallo = new RuntimeException("inyector no disponible");
        beansMock.when(() -> Beans.get(ModelServiceFactory.class)).thenThrow(fallo);

        RuntimeException excepcion = assertThrows(RuntimeException.class,
                () -> CertificadoDigitalHelper.getSituacionFirmaByDni(DNI_VALIDO));

        assertSame(fallo, excepcion);
    }
}
