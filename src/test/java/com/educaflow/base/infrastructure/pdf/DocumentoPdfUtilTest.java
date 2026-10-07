package com.educaflow.base.infrastructure.pdf;

import com.axelor.i18n.I18n;
import com.educaflow.base.infrastructure.criptografia.DatosCertificado;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentoPdfUtilTest {

    private static final String DNI = "93882914L";
    private static final String TEXTO = "texto del documento";

    private MockedStatic<I18n> i18nMock;

    @BeforeEach
    void setUp() {
        // lenient: los caminos sin mensaje no llaman a I18n.
        i18nMock = Mockito.mockStatic(I18n.class, Mockito.withSettings().strictness(Strictness.LENIENT));
        i18nMock.when(() -> I18n.get(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        i18nMock.close();
    }

    private static DocumentoPdf documento(String texto, ResultadoFirma... firmas) {
        DocumentoPdf documentoPdf = mock(DocumentoPdf.class);
        when(documentoPdf.getFirmasPdf()).thenReturn(List.of(firmas));
        when(documentoPdf.getPlainText()).thenReturn(texto);
        return documentoPdf;
    }

    private static ResultadoFirma firma(boolean correcta, boolean confiable, boolean selloTiempo, String dni) {
        DatosCertificado datosCertificado = mock(DatosCertificado.class);
        when(datosCertificado.isValidoEnListaCertificadosConfiables()).thenReturn(confiable);
        when(datosCertificado.isSelloTiempo()).thenReturn(selloTiempo);
        when(datosCertificado.getDNI()).thenReturn(dni);
        when(datosCertificado.getCnSubject()).thenReturn("CN " + dni);
        ResultadoFirma resultadoFirma = mock(ResultadoFirma.class);
        when(resultadoFirma.isCorrecta()).thenReturn(correcta);
        when(resultadoFirma.isCubreDocumentoCompleto()).thenReturn(true);
        when(resultadoFirma.getDatosCertificado()).thenReturn(datosCertificado);
        return resultadoFirma;
    }

    private static ResultadoFirma firmaValida() {
        return firma(true, true, false, DNI);
    }

    @Test
    void validateFirmaPdf_originalNulo_lanzaExcepcion() {
        DocumentoPdf firmado = documento(TEXTO);
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> DocumentoPdfUtil.validateFirmaPdf(null, firmado, DNI));
        assertEquals("El documento original no puede ser nulo", ex.getMessage());
    }

    @Test
    void validateFirmaPdf_firmadoNulo_lanzaExcepcion() {
        DocumentoPdf original = documento(TEXTO);
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> DocumentoPdfUtil.validateFirmaPdf(original, null, DNI));
        assertEquals("El documento firmado no puede ser nulo", ex.getMessage());
    }

    @Test
    void validateFirmaPdf_dniNulo_lanzaExcepcion() {
        DocumentoPdf original = documento(TEXTO);
        DocumentoPdf firmado = documento(TEXTO);
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> DocumentoPdfUtil.validateFirmaPdf(original, firmado, null));
        assertEquals("El DNI no puede ser nulo ni estar vacio", ex.getMessage());
    }

    @Test
    void validateFirmaPdf_dniEnBlanco_lanzaExcepcion() {
        DocumentoPdf original = documento(TEXTO);
        DocumentoPdf firmado = documento(TEXTO);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> DocumentoPdfUtil.validateFirmaPdf(original, firmado, "  "));
        assertEquals("El DNI no puede ser nulo ni estar vacio", ex.getMessage());
    }

    @Test
    void validateFirmaPdf_faltaFirmaOriginal_devuelveError() {
        ResultadoFirma firmaOriginal = firma(true, true, false, "11111111H");
        DocumentoPdf original = documento(TEXTO, firmaOriginal);
        DocumentoPdf firmado = documento(TEXTO, firmaValida());

        Optional<String> resultado = DocumentoPdfUtil.validateFirmaPdf(original, firmado, DNI);

        assertEquals(Optional.of("Falta la firma CN 11111111H en el documento"), resultado);
    }

    @Test
    void validateFirmaPdf_masDeUnaFirmaNueva_devuelveError() {
        DocumentoPdf original = documento(TEXTO);
        DocumentoPdf firmado = documento(TEXTO, firmaValida(), firmaValida());

        Optional<String> resultado = DocumentoPdfUtil.validateFirmaPdf(original, firmado, DNI);

        assertEquals(Optional.of("No es posible firmar el documento por más de una persona"), resultado);
    }

    @Test
    void validateFirmaPdf_textoDistinto_devuelveError() {
        DocumentoPdf original = documento(TEXTO);
        DocumentoPdf firmado = documento("otro texto", firmaValida());

        Optional<String> resultado = DocumentoPdfUtil.validateFirmaPdf(original, firmado, DNI);

        assertEquals(Optional.of("El documento firmado no es igual al documento original"), resultado);
    }

    @Test
    void validateFirmaPdf_sinFirmaNueva_devuelveError() {
        ResultadoFirma firmaOriginal = firmaValida();
        DocumentoPdf original = documento(TEXTO, firmaOriginal);
        DocumentoPdf firmado = documento(TEXTO, firmaOriginal);

        Optional<String> resultado = DocumentoPdfUtil.validateFirmaPdf(original, firmado, DNI);

        assertEquals(Optional.of("El documento no se ha firmado"), resultado);
    }

    @Test
    void validateFirmaPdf_firmaIncorrecta_devuelveError() {
        DocumentoPdf original = documento(TEXTO);
        DocumentoPdf firmado = documento(TEXTO, firma(false, true, false, DNI));

        Optional<String> resultado = DocumentoPdfUtil.validateFirmaPdf(original, firmado, DNI);

        assertEquals(Optional.of("La firma no es correcta. Hay un error en ella"), resultado);
    }

    @Test
    void validateFirmaPdf_firmaNuevaNoCubreDocumento_devuelveError() {
        ResultadoFirma firmaNueva = firmaValida();
        when(firmaNueva.isCubreDocumentoCompleto()).thenReturn(false);
        DocumentoPdf original = documento(TEXTO);
        DocumentoPdf firmado = documento(TEXTO, firmaNueva);

        Optional<String> resultado = DocumentoPdfUtil.validateFirmaPdf(original, firmado, DNI);

        assertEquals(Optional.of("El documento se ha modificado después de firmarlo"), resultado);
    }

    @Test
    void validateFirmaPdf_certificadoNoConfiable_devuelveError() {
        DocumentoPdf original = documento(TEXTO);
        DocumentoPdf firmado = documento(TEXTO, firma(true, false, false, DNI));

        Optional<String> resultado = DocumentoPdfUtil.validateFirmaPdf(original, firmado, DNI);

        assertEquals(Optional.of("La firma no es valida según la lista de certificados aceptados por la aplicación"), resultado);
    }

    @Test
    void validateFirmaPdf_selloTiempo_devuelveError() {
        DocumentoPdf original = documento(TEXTO);
        DocumentoPdf firmado = documento(TEXTO, firma(true, true, true, DNI));

        Optional<String> resultado = DocumentoPdfUtil.validateFirmaPdf(original, firmado, DNI);

        assertEquals(Optional.of("La firma no puede ser un sello de tiempo"), resultado);
    }

    @Test
    void validateFirmaPdf_dniDistinto_devuelveError() {
        DocumentoPdf original = documento(TEXTO);
        DocumentoPdf firmado = documento(TEXTO, firma(true, true, false, "99999999R"));

        Optional<String> resultado = DocumentoPdfUtil.validateFirmaPdf(original, firmado, DNI);

        assertEquals(Optional.of("Se debe firmar con el DNI/NIE '" + DNI + "' sin embargo se ha firmado con '99999999R'"), resultado);
    }

    @Test
    void validateFirmaPdf_firmaNuevaValidaConservandoLasOriginales_devuelveVacio() {
        ResultadoFirma firmaOriginal = firma(true, true, false, "11111111H");
        DocumentoPdf original = documento(TEXTO, firmaOriginal);
        DocumentoPdf firmado = documento(TEXTO, firmaOriginal, firmaValida());

        Optional<String> resultado = DocumentoPdfUtil.validateFirmaPdf(original, firmado, DNI);

        assertTrue(resultado.isEmpty());
    }
}
