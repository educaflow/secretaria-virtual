package com.educaflow.subsystem.registroentradasalida.service.impl;

import com.axelor.app.AppSettings;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.i18n.I18n;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.criptografia.AlmacenClaveFichero;
import com.educaflow.base.infrastructure.criptografia.EntornoCriptograficoDePruebas;
import com.educaflow.base.infrastructure.fichero.Fichero;
import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.numeradores.db.repo.NumeradorRepository;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.pdf.DocumentoPdfFactory;
import com.educaflow.base.infrastructure.pdf.impl.helper.DocumentoPdfHelper;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.base.util.TokenUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.educaflow.subsystem.common.db.CargoCodigo;
import com.educaflow.subsystem.criptografia.db.CertificadoDigital;
import com.educaflow.subsystem.criptografia.service.CertificadoDigitalService;
import com.educaflow.subsystem.registroentradasalida.controller.RegistroSalidaController;
import com.educaflow.subsystem.registroentradasalida.db.RegistroSalida;
import com.educaflow.subsystem.registroentradasalida.db.repo.RegistroSalidaRepository;
import com.educaflow.subsystem.registroentradasalida.service.RegistroSalidaInsertDTO;
import com.educaflow.subsystem.registroentradasalida.service.RegistroSalidaService;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfStream;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject;
import com.itextpdf.signatures.SignatureUtil;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.quality.Strictness;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistroSalidaServiceImplTest {

    private static final String BASE_URL = "https://secretaria.example.org";
    private static final String FILE_CERTIFICADO = "/com/educaflow/base/infrastructure/pdf/mi_certificado_password_nadanada.p12";
    private static final String PASSWORD_CERTIFICADO = "nadanada";
    /** La ruta bajo la que Axelor publica los servicios JAX-RS. */
    private static final String RUTA_SERVICIOS_WEB = "/ws";
    private static final String MSG_NO_EXISTE = "No existe ningún documento con ese código de verificación.";

    private RegistroSalidaRepository repository;
    private RegistroSalidaServiceImpl service;
    private Centro centro;

    /** Lo que hay guardado en cada MetaFile: hace de almacén de ficheros de Axelor. */
    private Map<MetaFile, byte[]> contenidos;

    private MockedStatic<I18n> i18nMock;
    private MockedStatic<AppSettings> appSettingsMock;
    private MockedStatic<MetaFileUtil> metaFileUtilMock;

    @BeforeAll
    static void initAll() {
        EntornoCriptograficoDePruebas.configurar();
    }

    @BeforeEach
    void setUp() throws Exception {
        repository = Mockito.mock(RegistroSalidaRepository.class);
        service = new RegistroSalidaServiceImpl(RegistroSalida.class, repository);
        CertificadoDigitalService certificadoDigitalService = Mockito.mock(CertificadoDigitalService.class);
        ModelServiceFactory modelServiceFactory = Mockito.mock(ModelServiceFactory.class);
        when(modelServiceFactory.resolve(CertificadoDigital.class)).thenReturn(certificadoDigitalService);
        setField(service, "numeradorRepository", Mockito.mock(NumeradorRepository.class));
        setField(service, "modelServiceFactory", modelServiceFactory);
        when(certificadoDigitalService.getByCentroCargo(any(), eq(CargoCodigo.SECRETARIO))).thenAnswer(invocation -> new AlmacenClaveFichero(getClass().getResourceAsStream(FILE_CERTIFICADO), PASSWORD_CERTIFICADO));

        centro = new Centro();
        centro.setCode("46019660");

        i18nMock = Mockito.mockStatic(I18n.class, Mockito.withSettings().strictness(Strictness.LENIENT));
        i18nMock.when(() -> I18n.get(any(String.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Un espía y no un mock: del resto de la configuración tiran otras clases, que deben seguir leyendo la de verdad
        AppSettings appSettings = Mockito.spy(AppSettings.get());
        Mockito.doReturn(BASE_URL).when(appSettings).getBaseURL();
        appSettingsMock = Mockito.mockStatic(AppSettings.class);
        appSettingsMock.when(AppSettings::get).thenReturn(appSettings);

        contenidos = new HashMap<>();
        metaFileUtilMock = Mockito.mockStatic(MetaFileUtil.class);
        metaFileUtilMock.when(MetaFileUtil::createMetaFileInstance).thenAnswer(invocation -> new MetaFile());
        metaFileUtilMock.when(() -> MetaFileUtil.downloadContent(any())).thenAnswer(invocation -> contenidos.get(invocation.<MetaFile>getArgument(0)));
        metaFileUtilMock.when(() -> MetaFileUtil.downloadHeader(any(), anyInt())).thenAnswer(invocation -> {
            byte[] contenido = contenidos.get(invocation.<MetaFile>getArgument(0));
            return Arrays.copyOf(contenido, Math.min(contenido.length, invocation.<Integer>getArgument(1)));
        });
        metaFileUtilMock.when(() -> MetaFileUtil.uploadContent(any(), any())).thenAnswer(invocation -> {
            contenidos.put(invocation.getArgument(0), invocation.getArgument(1));
            return invocation.getArgument(0);
        });
    }

    @AfterEach
    void tearDown() {
        i18nMock.close();
        appSettingsMock.close();
        metaFileUtilMock.close();
    }


    /****************************************************************************************/
    /******************************** createRegistroSalida **********************************/
    /****************************************************************************************/

    @Test
    void createRegistroSalida_elDocumentoLlevaEnSuMetadatoElCsvDelRegistro() {
        RegistroSalida registroSalida = crearRegistroSalida(List.of());

        assertTrue(TokenUtil.isCodigoSeguroVerificacion(registroSalida.getCsv()));
        assertEquals(registroSalida.getCsv(), documentoDe(registroSalida).getMetadato(RegistroSalidaService.METADATO_CSV));
    }

    @Test
    void createRegistroSalida_elDocumentoOriginalSeQuedaSinElMetadato() {
        RegistroSalida registroSalida = crearRegistroSalida(List.of());

        assertEquals(null, MetaFileHelper.getDocumentoPdf(registroSalida.getDocumentoOriginal()).getMetadato(RegistroSalidaService.METADATO_CSV));
    }

    @Test
    void createRegistroSalida_elDocumentoSaleConLaFirmaDelSecretarioCorrecta() {
        RegistroSalida registroSalida = crearRegistroSalida(List.of());

        assertEquals(1, documentoDe(registroSalida).getFirmasPdf().size());
        assertEquals(true, documentoDe(registroSalida).getFirmasPdf().get(0).isCorrecta());
    }

    /**
     * La URL del código QR se compara con la que de verdad atiende el controlador (la que dicen sus
     * anotaciones), no con un texto: si se cambia la ruta en un sitio y no en el otro, este test falla.
     */
    @Test
    void createRegistroSalida_elCodigoQrLlevaALaDescargaQueAtiendeElControlador() throws Exception {
        RegistroSalida registroSalida = crearRegistroSalida(List.of());

        assertEquals(BASE_URL + RUTA_SERVICIOS_WEB + rutaDescargaDelControlador(registroSalida.getCsv()), leerCodigoQr(documentoDe(registroSalida)));
    }

    @Test
    void createRegistroSalida_conElCsvDelMetadatoDelDocumentoSeDescargaEseMismoDocumento() {
        RegistroSalida registroSalida = crearRegistroSalida(List.of());
        String csv = documentoDe(registroSalida).getMetadato(RegistroSalidaService.METADATO_CSV);
        when(repository.findByCsv(csv)).thenReturn(registroSalida);

        Fichero descarga = service.getDescargaByCsv(csv);

        assertArrayEquals(contenidos.get(registroSalida.getDocumento()), descarga.data());
    }


    /****************************************************************************************/
    /********************************** getDescargaByCsv ************************************/
    /****************************************************************************************/

    @Test
    void getDescargaByCsv_sinAnexos_devuelveElPdfDelDocumento() {
        String csv = TokenUtil.generateCodigoSeguroVerificacion();
        RegistroSalida registroSalida = registroSalidaGuardado(csv, metaFile("resolucion.pdf", "el documento"), List.of());

        Fichero descarga = service.getDescargaByCsv(csv);

        assertEquals("resolucion.pdf", descarga.fileName());
        assertEquals(MetaFileHelper.PDF_MIME_TYPE, descarga.mimeType());
        assertArrayEquals(contenidos.get(registroSalida.getDocumento()), descarga.data());
    }

    @Test
    void getDescargaByCsv_conAnexos_devuelveUnZipConElDocumentoYLosAnexos() throws Exception {
        String csv = TokenUtil.generateCodigoSeguroVerificacion();
        registroSalidaGuardado(csv, metaFile("resolucion.pdf", "el documento"), List.of(metaFile("justificante.pdf", "un anexo"), metaFile("otro.pdf", "otro anexo")));

        Fichero descarga = service.getDescargaByCsv(csv);

        assertEquals("application/zip", descarga.mimeType());
        assertTrue(descarga.fileName().endsWith(".zip"));
        Map<String, String> ficheros = descomprimir(descarga.data());
        assertEquals(List.of("resolucion.pdf", "justificante.pdf", "otro.pdf"), new ArrayList<>(ficheros.keySet()));
        assertEquals(List.of("el documento", "un anexo", "otro anexo"), new ArrayList<>(ficheros.values()));
    }

    @Test
    void getDescargaByCsv_conAnexosQueSeLlamanIgual_losMeteTodosEnElZip() throws Exception {
        String csv = TokenUtil.generateCodigoSeguroVerificacion();
        registroSalidaGuardado(csv, metaFile("documento.pdf", "el documento"), List.of(metaFile("documento.pdf", "un anexo"), metaFile("documento.pdf", "otro anexo")));

        Fichero descarga = service.getDescargaByCsv(csv);

        Map<String, String> ficheros = descomprimir(descarga.data());
        assertEquals(3, ficheros.size());
        assertEquals(new HashSet<>(List.of("el documento", "un anexo", "otro anexo")), new HashSet<>(ficheros.values()));
        assertTrue(ficheros.keySet().stream().allMatch(nombre -> nombre.endsWith("documento.pdf")), "los repetidos conservan la extensión: " + ficheros.keySet());
    }

    @Test
    void getDescargaByCsv_conUnCsvQueNoEsDeNingunRegistro_aborta() {
        assertThrows(ValidationException.class, () -> service.getDescargaByCsv(TokenUtil.generateCodigoSeguroVerificacion()));
    }


    /****************************************************************************************/
    /************************************ getUrlDescarga ************************************/
    /****************************************************************************************/

    /** Se compara con la ruta que de verdad atiende el controlador, igual que la del código QR. */
    @Test
    void getUrlDescarga_llevaALaDescargaQueAtiendeElControlador() throws Exception {
        String csv = TokenUtil.generateCodigoSeguroVerificacion();

        assertEquals(BASE_URL + RUTA_SERVICIOS_WEB + rutaDescargaDelControlador(csv), service.getUrlDescarga(csv));
    }

    @Test
    void getUrlDescarga_conUnCsvMalFormado_aborta() {
        assertThrows(ValidationException.class, () -> service.getUrlDescarga("no es un csv"));
    }

    @Test
    void validateGetUrlDescarga_conUnCsvBienFormado_esValido() {
        assertTrue(service.validateGetUrlDescarga(TokenUtil.generateCodigoSeguroVerificacion()).isEmpty());
    }

    @Test
    void validateGetUrlDescarga_sinCsv_rechaza() {
        assertTrue(service.validateGetUrlDescarga(null).isPresent());
    }

    @Test
    void getUrlDescarga_esLaMismaQueLlevaElCodigoQrDelDocumento() throws Exception {
        RegistroSalida registroSalida = crearRegistroSalida(List.of());

        assertEquals(leerCodigoQr(documentoDe(registroSalida)), service.getUrlDescarga(registroSalida.getCsv()));
    }


    /****************************************************************************************/
    /****************************** validateGetDescargaByCsv ********************************/
    /****************************************************************************************/

    @Test
    void validateInsert_siempreRechaza() {
        assertTrue(service.validateInsert(new RegistroSalida()).isPresent());
    }

    @Test
    void insert_lanzaValidationExceptionSinGuardarNada() {
        RegistroSalida registroSalida = new RegistroSalida();

        assertThrows(ValidationException.class, () -> service.insert(registroSalida));
        verify(repository, never()).save(any());
    }

    @Test
    void validateUpdate_siempreRechaza() {
        assertTrue(service.validateUpdate(new RegistroSalida(), new RegistroSalida()).isPresent());
    }

    @Test
    void update_lanzaValidationExceptionSinGuardarNada() {
        RegistroSalida registroSalida = new RegistroSalida();

        assertThrows(ValidationException.class, () -> service.update(registroSalida, new RegistroSalida()));
        verify(repository, never()).save(any());
    }

    @Test
    void validateRemove_siempreRechaza() {
        assertTrue(service.validateRemove(new RegistroSalida()).isPresent());
    }

    @Test
    void remove_lanzaValidationExceptionSinBorrarNada() {
        RegistroSalida registroSalida = new RegistroSalida();

        assertThrows(ValidationException.class, () -> service.remove(registroSalida));
        verify(repository, never()).remove(any());
    }

    @Test
    void validateGetDescargaByCsv_conElCsvDeUnRegistro_esValido() {
        String csv = TokenUtil.generateCodigoSeguroVerificacion();
        registroSalidaGuardado(csv, metaFile("resolucion.pdf", "el documento"), List.of());

        assertEquals(Optional.empty(), service.validateGetDescargaByCsv(csv));
    }

    @Test
    void validateGetDescargaByCsv_conUnCsvQueNoEsDeNingunRegistro_diceQueNoExiste() {
        Optional<BusinessMessages> resultado = service.validateGetDescargaByCsv(TokenUtil.generateCodigoSeguroVerificacion());

        assertEquals(MSG_NO_EXISTE, resultado.orElseThrow().get(0).getMessage());
    }

    @Test
    void validateGetDescargaByCsv_conAlgoQueNoTieneFormaDeCsv_diceQueNoExisteSinBuscarloEnLaBaseDeDatos() {
        Optional<BusinessMessages> resultado = service.validateGetDescargaByCsv("' or '1'='1");

        assertEquals(MSG_NO_EXISTE, resultado.orElseThrow().get(0).getMessage());
        verify(repository, never()).findByCsv(any());
    }

    @Test
    void validateGetDescargaByCsv_sinCsv_diceQueNoExisteSinBuscarloEnLaBaseDeDatos() {
        Optional<BusinessMessages> resultado = service.validateGetDescargaByCsv(null);

        assertEquals(MSG_NO_EXISTE, resultado.orElseThrow().get(0).getMessage());
        verify(repository, never()).findByCsv(any());
    }


    /****************************************************************************************/
    /************************************* Utilidades ***************************************/
    /****************************************************************************************/

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = RegistroSalidaServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private RegistroSalida crearRegistroSalida(List<MetaFile> anexos) {
        MetaFile documentoOriginal = new MetaFile();
        documentoOriginal.setFileName("resolucion.pdf");
        documentoOriginal.setFileType(MetaFileHelper.PDF_MIME_TYPE);
        contenidos.put(documentoOriginal, pdfDeUnaPagina());

        return service.createRegistroSalida(new RegistroSalidaInsertDTO(centro, "Resolución"), documentoOriginal, anexos);
    }

    private DocumentoPdf documentoDe(RegistroSalida registroSalida) {
        return DocumentoPdfFactory.getDocumentoPdf(contenidos.get(registroSalida.getDocumento()), registroSalida.getDocumento().getFileName());
    }

    private RegistroSalida registroSalidaGuardado(String csv, MetaFile documento, List<MetaFile> anexos) {
        RegistroSalida registroSalida = new RegistroSalida();
        registroSalida.setCsv(csv);
        registroSalida.setNumeroRegistro("46019660/2026/00001");
        registroSalida.setDocumento(documento);
        registroSalida.setAnexos(anexos);
        when(repository.findByCsv(csv)).thenReturn(registroSalida);

        return registroSalida;
    }

    private MetaFile metaFile(String fileName, String contenido) {
        MetaFile metaFile = new MetaFile();
        metaFile.setFileName(fileName);
        metaFile.setFileType(MetaFileHelper.PDF_MIME_TYPE);
        contenidos.put(metaFile, contenido.getBytes(StandardCharsets.UTF_8));

        return metaFile;
    }

    private static byte[] pdfDeUnaPagina() {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (PdfDocument pdfDocument = new PdfDocument(new PdfWriter(salida))) {
            pdfDocument.addNewPage();
        }

        return salida.toByteArray();
    }

    /** La ruta, con su CSV, de la descarga tal y como la declaran las anotaciones JAX-RS del controlador. */
    private static String rutaDescargaDelControlador(String csv) throws Exception {
        Method descarga = RegistroSalidaController.class.getMethod("getDescargaByCsv", String.class);
        String parametroCsv = descarga.getParameters()[0].getAnnotation(QueryParam.class).value();

        return RegistroSalidaController.class.getAnnotation(Path.class).value() + descarga.getAnnotation(Path.class).value() + "?" + parametroCsv + "=" + csv;
    }

    /** Lee el código QR que pinta la última firma del documento. */
    private static String leerCodigoQr(DocumentoPdf documentoPdf) throws Exception {
        PdfDocument pdfDocument = DocumentoPdfHelper.getPdfDocument(documentoPdf);
        List<String> firmas = new SignatureUtil(pdfDocument).getSignatureNames();
        PdfDictionary apariencia = PdfAcroForm.getAcroForm(pdfDocument, false).getField(firmas.get(firmas.size() - 1)).getWidgets().get(0).getNormalAppearanceObject();
        BufferedImage imagen = new PdfImageXObject(buscarImagen(apariencia.getAsDictionary(PdfName.Resources))).getBufferedImage();

        int[] pixeles = imagen.getRGB(0, 0, imagen.getWidth(), imagen.getHeight(), null, 0, imagen.getWidth());
        RGBLuminanceSource fuente = new RGBLuminanceSource(imagen.getWidth(), imagen.getHeight(), pixeles);

        return new QRCodeReader().decode(new BinaryBitmap(new HybridBinarizer(fuente))).getText();
    }

    /** La imagen de unos recursos o de los formularios que anidan: iText pinta la apariencia en capas. */
    private static PdfStream buscarImagen(PdfDictionary recursos) {
        PdfDictionary xObjects = (recursos == null) ? null : recursos.getAsDictionary(PdfName.XObject);
        if (xObjects == null) {
            return null;
        }

        for (PdfName nombre : xObjects.keySet()) {
            PdfStream xObject = xObjects.getAsStream(nombre);
            PdfStream imagen = PdfName.Image.equals(xObject.getAsName(PdfName.Subtype)) ? xObject : buscarImagen(xObject.getAsDictionary(PdfName.Resources));
            if (imagen != null) {
                return imagen;
            }
        }

        return null;
    }

    private static Map<String, String> descomprimir(byte[] zip) throws Exception {
        Map<String, String> ficheros = new LinkedHashMap<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zip))) {
            for (ZipEntry entrada = zipInputStream.getNextEntry(); entrada != null; entrada = zipInputStream.getNextEntry()) {
                ficheros.put(entrada.getName(), new String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8));
            }
        }

        return ficheros;
    }

}
