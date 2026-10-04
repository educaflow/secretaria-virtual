package com.educaflow.tramites.util.entrada;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.axelor.auth.db.User;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.criptografia.service.SituacionFirma;
import com.educaflow.subsystem.criptografia.db.CertificadoDigital;
import com.educaflow.subsystem.criptografia.service.CertificadoDigitalService;
import com.educaflow.subsystem.expedientes.db.PruebaV1;
import com.educaflow.subsystem.registroentradasalida.db.RegistroEntrada;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.EventContext;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.Phase;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.State;
import com.educaflow.tramites.util.firma.FirmaServidorHelper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

/**
 * {@link EntradaHelper} no conoce los campos de ningún tipo de expediente: los recibe en un {@link CamposEntrada}.
 * Aquí se le da uno que apunta a los campos de este test, sobre un expediente cualquiera.
 */
@ExtendWith(MockitoExtension.class)
class EntradaHelperTest {

    private static final String DNI = "12345678Z";
    private static final String CLAVE = "clave";
    private static final String CAMPO_FIRMA = "firmaSolicitante";

    @Mock
    private FirmaServidorHelper firmaServidorHelper;

    @Mock
    private ModelServiceFactory modelServiceFactory;

    @Mock
    private CertificadoDigitalService certificadoDigitalService;

    @InjectMocks
    private EntradaHelper entradaHelper;

    private MockedStatic<SecurityUtil> securityUtilMock;

    private final PruebaV1 expediente = new PruebaV1();
    private final MetaFile pdfSolicitud = new MetaFile();
    private MetaFile pdfSolicitudFirmada;
    private MetaFile pdfJustificanteRegistroEntrada;
    private boolean subsanacionBorrada;

    private final CamposEntrada<PruebaV1> campos = new CamposEntrada<>(
            exp -> pdfSolicitud,
            exp -> pdfSolicitudFirmada,
            (exp, metaFile) -> pdfSolicitudFirmada = metaFile,
            (exp, metaFile) -> pdfJustificanteRegistroEntrada = metaFile,
            exp -> subsanacionBorrada = true);

    @BeforeEach
    void mockearEstaticos() {
        User usuario = new User();
        usuario.setDni(DNI);

        securityUtilMock = Mockito.mockStatic(SecurityUtil.class, Mockito.withSettings().strictness(Strictness.LENIENT));
        securityUtilMock.when(SecurityUtil::getUser).thenReturn(usuario);
    }

    @AfterEach
    void cerrarEstaticos() {
        securityUtilMock.close();
    }

    private void stubSituacionFirma(SituacionFirma situacionFirma) {
        when(modelServiceFactory.resolve(CertificadoDigital.class)).thenReturn(certificadoDigitalService);
        when(certificadoDigitalService.getSituacionFirmaByDni(DNI)).thenReturn(situacionFirma);
    }

    @Test
    void firmarSolicitudSiEsEnServidor_firmaEnServidor_dejaLaSolicitudFirmadaEnSuCampo() throws BusinessException {
        MetaFile firmada = new MetaFile();
        expediente.setClaveCertificado(CLAVE);
        stubSituacionFirma(SituacionFirma.FICHERO_SIN_CLAVE);
        when(firmaServidorHelper.firmarEnServidor(DNI, SituacionFirma.FICHERO_SIN_CLAVE, CLAVE, pdfSolicitud, CAMPO_FIRMA)).thenReturn(firmada);

        entradaHelper.firmarSolicitudSiEsEnServidor(expediente, campos, CAMPO_FIRMA);

        assertSame(firmada, pdfSolicitudFirmada);
    }

    @Test
    void firmarSolicitudSiEsEnServidor_firmaConAutoFirma_noTocaLaSolicitudQueYaLlegoFirmada() throws BusinessException {
        MetaFile firmadaEnElEquipoDelUsuario = new MetaFile();
        pdfSolicitudFirmada = firmadaEnElEquipoDelUsuario;
        stubSituacionFirma(SituacionFirma.SIN_CERTIFICADO);

        entradaHelper.firmarSolicitudSiEsEnServidor(expediente, campos, CAMPO_FIRMA);

        assertSame(firmadaEnElEquipoDelUsuario, pdfSolicitudFirmada);
        verifyNoInteractions(firmaServidorHelper);
    }

    @Test
    void presentar_registraLaSolicitudFirmadaGuardaElResguardoYBorraLaSubsanacion() {
        MetaFile anexo = new MetaFile();
        MetaFile resguardo = new MetaFile();
        RegistroEntrada registroEntrada = new RegistroEntrada();
        registroEntrada.setDocumentoResguardoPresentacion(resguardo);
        pdfSolicitudFirmada = new MetaFile();
        EventContext eventContext = mock(EventContext.class);
        when(eventContext.createRegistroEntrada(pdfSolicitudFirmada, List.of(anexo))).thenReturn(registroEntrada);

        entradaHelper.presentar(expediente, campos, List.of(anexo), eventContext);

        assertAll(
                () -> assertSame(resguardo, pdfJustificanteRegistroEntrada),
                () -> assertTrue(subsanacionBorrada));
    }

    @Test
    void exigePresentadoEnPapel_elModoCoincide_noLanza() {
        expediente.setPresentadoEnPapel(true);
        assertDoesNotThrow(() -> EntradaHelper.exigePresentadoEnPapel(expediente, true));

        expediente.setPresentadoEnPapel(false);
        assertDoesNotThrow(() -> EntradaHelper.exigePresentadoEnPapel(expediente, false));
    }

    @Test
    void exigePresentadoEnPapel_elModoNoCoincide_lanzaIllegalState() {
        expediente.setPresentadoEnPapel(false);
        assertThrows(IllegalStateException.class, () -> EntradaHelper.exigePresentadoEnPapel(expediente, true));

        expediente.setPresentadoEnPapel(true);
        assertThrows(IllegalStateException.class, () -> EntradaHelper.exigePresentadoEnPapel(expediente, false));
    }

    @Test
    void estaEn_comparaLaFaseYElEstadoDelExpediente() {
        Phase fase = mock(Phase.class);
        when(fase.getCode()).thenReturn("ENTRADA");
        State estado = mock(State.class);
        when(estado.getPhase()).thenReturn(fase);
        when(estado.getCode()).thenReturn("ENTRADA_DATOS");

        expediente.setCodePhase("ENTRADA");
        expediente.setCodeState("ENTRADA_DATOS");
        assertTrue(EntradaHelper.estaEn(expediente, estado));

        expediente.setCodeState("PENDIENTE_PRESENTACION");
        assertFalse(EntradaHelper.estaEn(expediente, estado));

        // El nombre de un estado solo es único dentro de su fase.
        expediente.setCodePhase("RESOLUCION");
        expediente.setCodeState("ENTRADA_DATOS");
        assertFalse(EntradaHelper.estaEn(expediente, estado));
    }

    @Test
    void camposEntrada_sinAlgunoDeLosCampos_lanzaNullPointer() {
        assertThrows(NullPointerException.class, () -> new CamposEntrada<PruebaV1>(null, exp -> null, (exp, m) -> { }, (exp, m) -> { }, exp -> { }));
        assertThrows(NullPointerException.class, () -> new CamposEntrada<PruebaV1>(exp -> null, null, (exp, m) -> { }, (exp, m) -> { }, exp -> { }));
        assertThrows(NullPointerException.class, () -> new CamposEntrada<PruebaV1>(exp -> null, exp -> null, null, (exp, m) -> { }, exp -> { }));
        assertThrows(NullPointerException.class, () -> new CamposEntrada<PruebaV1>(exp -> null, exp -> null, (exp, m) -> { }, null, exp -> { }));
        assertThrows(NullPointerException.class, () -> new CamposEntrada<PruebaV1>(exp -> null, exp -> null, (exp, m) -> { }, (exp, m) -> { }, null));
        assertNull(pdfJustificanteRegistroEntrada);
    }
}
