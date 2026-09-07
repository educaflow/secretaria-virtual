package com.educaflow.subsystem.firmas.service.impl;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.criptografia.AlmacenClave;
import com.educaflow.base.infrastructure.criptografia.AlmacenClaveDispositivo;
import com.educaflow.base.infrastructure.criptografia.AlmacenClaveFichero;
import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.pdf.CampoFirma;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.subsystem.criptografia.db.CertificadoDigital;
import com.educaflow.subsystem.criptografia.service.AlmacenClaveResolver;
import com.educaflow.subsystem.criptografia.service.CertificadoDigitalService;
import com.educaflow.subsystem.criptografia.service.FirmaEnServidorService;
import com.educaflow.subsystem.firmas.db.DocumentoFirma;
import com.educaflow.subsystem.firmas.db.EstadoTareaFirma;
import com.educaflow.subsystem.criptografia.service.SituacionFirma;
import com.educaflow.subsystem.firmas.db.TareaFirma;
import com.educaflow.subsystem.firmas.db.repo.TareaFirmaRepository;
import com.educaflow.subsystem.firmas.service.TareaFirmaNotifier;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.security.UnrecoverableKeyException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TareaFirmaServiceImplTest {

    // DNI válido de referencia de la spec (el mismo que usa CertificadoDigitalServiceImplTest).
    private static final String DNI = "85432016B";
    private static final String CLAVE = "nadanada";
    private static final String CLAVE_EN_BLANCO = "   ";
    private static final String CLAVE_SECRETA = "claveSecretaDePrueba";
    private static final String PIN = "1234";

    private static final String MENSAJE_SOLO_PENDIENTES = "Solo se pueden firmar las tareas pendientes de firmar";
    private static final String MENSAJE_SOLO_EL_ENCARGADO = "Solo puede firmar los documentos la persona a la que se le han encargado";
    private static final String MENSAJE_SIN_DNI = "No es posible firmar los documentos porque su usuario no tiene un DNI. Póngase en contacto con el administrador.";
    private static final String MENSAJE_SIN_CERTIFICADO = "No es posible firmar en el servidor porque no tiene un certificado digital dado de alta";
    private static final String MENSAJE_PIN_OBLIGATORIO = "El PIN es obligatorio";
    private static final String MENSAJE_CONTRASENA_OBLIGATORIA = "La contraseña es obligatoria";
    private static final String MENSAJE_SIN_DOCUMENTOS = "La tarea de firma no tiene ningún documento que firmar";
    // El mensaje lo compone el servicio: el vocabulario de la bandeja de firmas más el motivo que redacta
    // CertificadoDigitalHelper.motivoClaveErronea, que es quien distingue quién puede corregir la clave.
    private static final String MENSAJE_NO_ES_POSIBLE_FIRMAR = "No es posible firmar los documentos: ";
    private static final String MENSAJE_CONTRASENA_INCORRECTA = MENSAJE_NO_ES_POSIBLE_FIRMAR + "la contraseña indicada no es correcta";
    private static final String MENSAJE_CLAVE_GUARDADA_INCORRECTA = MENSAJE_NO_ES_POSIBLE_FIRMAR + "la clave guardada de su certificado digital no es correcta. Póngase en contacto con el administrador";
    private static final String MENSAJE_NO_SE_HAN_PODIDO_FIRMAR = "No se han podido firmar los documentos: ";
    private static final String MOTIVO_CLAVE_INCORRECTA = "clave incorrecta";
    private static final String MOTIVO_TECNICO_DEL_JDK = "keystore password was incorrect";

    private TareaFirmaRepository repository;
    private TareaFirmaServiceImpl service;
    private ModelServiceFactory modelServiceFactory;
    private CertificadoDigitalService certificadoDigitalService;
    private TareaFirmaNotifier tareaFirmaNotifier;

    private User firmante;

    /**
     * La clave que «teclea» el firmante en el caso. No es un campo de la tarea: el servicio la recibe como
     * argumento escalar, así que el arrange la deja aquí y las llamadas al servicio la pasan tal cual.
     */
    private String claveFirmaTecleada;

    /** Mock del PDF original de cada documento de la tarea, en el mismo orden que sus {@code DocumentoFirma}. */
    private List<DocumentoPdf> documentosPdfOriginales;

    private MockedStatic<I18n> i18nMock;
    private MockedStatic<AuthUtils> authUtilsMock;
    private MockedStatic<MetaFileHelper> metaFileHelperMock;
    private MockedStatic<Beans> beansMock;

    @BeforeEach
    void setUp() throws Exception {
        repository = Mockito.mock(TareaFirmaRepository.class);
        service = new TareaFirmaServiceImpl(TareaFirma.class, repository);

        modelServiceFactory = Mockito.mock(ModelServiceFactory.class);
        certificadoDigitalService = Mockito.mock(CertificadoDigitalService.class);
        tareaFirmaNotifier = Mockito.mock(TareaFirmaNotifier.class);

        // La firma en el servidor la hace el subsistema de criptografía. Aquí se cablea su cadena REAL
        // (FirmaEnServidorService -> AlmacenClaveResolver) contra el mismo ModelServiceFactory mockeado que
        // usa el resto de la clase, en vez de mockear el servicio: así estos tests siguen ejerciendo el
        // camino completo hasta getAlmacenClaveByDni y DocumentoPdf.firmar, que es lo que comprueban.
        AlmacenClaveResolver almacenClaveResolver = new AlmacenClaveResolver();
        setField(almacenClaveResolver, AlmacenClaveResolver.class, "modelServiceFactory", modelServiceFactory);
        FirmaEnServidorService firmaEnServidorService = new FirmaEnServidorService();
        setField(firmaEnServidorService, FirmaEnServidorService.class, "almacenClaveResolver", almacenClaveResolver);
        setField(service, "firmaEnServidorService", firmaEnServidorService);

        firmante = new User();
        firmante.setId(1L);
        firmante.setDni(DNI);

        // Los cuatro estáticos se crean con la estrictez por defecto: ninguno necesita marcarse LENIENT.
        // Los stubs de AuthUtils los programa el arrange de cada caso y los consume siempre la ruta que ese
        // caso ejerce. Los únicos que se quedan a veces sin consumir son I18n (los tests de AllowProperties
        // no traducen ningún mensaje), Beans.get (solo se usa cuando la tarea llega a notificarse o a
        // consultar el certificado del firmante) y MetaFileHelper.createMetaFile (las rutas de error no
        // publican ningún fichero), y ninguno de ellos hace fallar la clase.
        i18nMock = Mockito.mockStatic(I18n.class);
        i18nMock.when(() -> I18n.get(any(String.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authUtilsMock = Mockito.mockStatic(AuthUtils.class);
        metaFileHelperMock = Mockito.mockStatic(MetaFileHelper.class);
        beansMock = Mockito.mockStatic(Beans.class);
    }

    @AfterEach
    void tearDown() {
        // Defensivo: si setUp falla a mitad, @AfterEach se ejecuta igual y un close() sobre un nulo taparía
        // la causa real del fallo con una NullPointerException.
        cerrarSiNoEsNulo(beansMock);
        cerrarSiNoEsNulo(metaFileHelperMock);
        cerrarSiNoEsNulo(authUtilsMock);
        cerrarSiNoEsNulo(i18nMock);
    }

    /* ------------------------------------------------------------------ */
    /* Helpers                                                            */
    /* ------------------------------------------------------------------ */

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        setField(target, TareaFirmaServiceImpl.class, fieldName, value);
    }

    /**
     * Variante para cablear a mano un colaborador que no es el servicio bajo prueba: la clase donde está
     * declarado el campo llega como parámetro en vez de darse por supuesta.
     */
    private static void setField(Object target, Class<?> claseDeclarante, String fieldName, Object value) throws Exception {
        Field field = claseDeclarante.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void cerrarSiNoEsNulo(MockedStatic<?> mockedStatic) {
        if (mockedStatic != null) {
            mockedStatic.close();
        }
    }

    private static List<String> mensajes(Optional<BusinessMessages> resultado) {
        assertTrue(resultado.isPresent());
        return resultado.get().stream().map(BusinessMessage::getMessage).toList();
    }

    private void stubUsuarioAutenticado(User usuario) {
        authUtilsMock.when(AuthUtils::getUser).thenReturn(usuario);
    }

    /**
     * Deja al firmante en la situación de firma del caso. No se mockea {@code CertificadoDigitalHelper}: se
     * monta la misma respuesta que le daría el subsistema de criptografía, para que estos tests sigan
     * ejerciendo el camino real por el que el servicio averigua la situación —el certificado dado de alta
     * para el DNI del firmante— y no una versión de mentira de ese camino.
     *
     * <p>{@code SIN_DNI} se monta quitándole el DNI de la ficha al firmante, que es lo que lo provoca: es un
     * estado del propio firmante y no del certificado. La respuesta del servicio se monta igual que en los
     * demás casos, porque {@code getSituacionFirmaByDni} es quien lo dice también sin DNI.
     */
    private void stubSituacionFirma(SituacionFirma situacionFirma) {
        String dniDelFirmante = DNI;
        if (situacionFirma == SituacionFirma.SIN_DNI) {
            firmante.setDni(null);
            dniDelFirmante = null;
        }

        beansMock.when(() -> Beans.get(ModelServiceFactory.class)).thenReturn(modelServiceFactory);
        Mockito.lenient().when(modelServiceFactory.resolve(CertificadoDigital.class)).thenReturn(certificadoDigitalService);
        // El servicio de criptografía nunca devuelve null: dice cada caso con su valor del enum, incluidos
        // SIN_DNI y SIN_CERTIFICADO.
        Mockito.lenient().when(certificadoDigitalService.getSituacionFirmaByDni(dniDelFirmante))
                .thenReturn(situacionFirma);
    }

    /**
     * Stub del servicio de criptografía: la resolución del {@code ModelService} y el almacén de claves que
     * devuelve para el firmante. Es {@code lenient} porque las validaciones que fallan antes de
     * V-TareaFirma-008 no llegan a abrir el certificado.
     */
    private void stubAlmacenClave(AlmacenClave almacenClave) {
        Mockito.lenient().when(modelServiceFactory.resolve(CertificadoDigital.class)).thenReturn(certificadoDigitalService);
        Mockito.lenient().when(certificadoDigitalService.getAlmacenClaveByDni(anyString(), any())).thenReturn(almacenClave);
    }

    /**
     * Certificado en fichero cuya clave abre (o no) el almacén. Se mockea {@code AlmacenClaveFichero} en vez de
     * construir uno real porque aquí se prueba qué hace el servicio con la respuesta, no la criptografía: eso
     * es de {@code AlmacenClaveFicheroTest}.
     */
    private AlmacenClaveFichero stubAlmacenClaveFichero(boolean claveValida) {
        AlmacenClaveFichero almacenClaveFichero = Mockito.mock(AlmacenClaveFichero.class);
        Mockito.lenient().when(almacenClaveFichero.isPasswordValid()).thenReturn(claveValida);
        stubAlmacenClave(almacenClaveFichero);
        return almacenClaveFichero;
    }

    /**
     * Certificado en un dispositivo criptográfico. Es lo que devuelve el subsistema de criptografía para un
     * {@code DISPOSITIVO_PKCS11}: un {@code AlmacenClaveDispositivo} que solo lleva el slot y el alias y que
     * <strong>no</strong> abre el dispositivo al construirse. Se devuelve para poder comprobar que nadie lo
     * toca, que es la garantía que de verdad protege la tarjeta de quedarse bloqueada por intentos fallidos.
     */
    private AlmacenClaveDispositivo stubAlmacenClaveDispositivo() {
        AlmacenClaveDispositivo almacenClaveDispositivo = Mockito.mock(AlmacenClaveDispositivo.class);
        stubAlmacenClave(almacenClaveDispositivo);
        return almacenClaveDispositivo;
    }

    /**
     * Stubs comunes a las dos acciones que resuelven una tarea como firmada: el repositorio devuelve la
     * tarea que se le pasa y la clase notificadora de la tarea resuelve al mock del notificador.
     */
    private void stubGuardadoYNotificacion() {
        Mockito.lenient().when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        beansMock.when(() -> Beans.get(TareaFirmaNotifierDePrueba.class)).thenReturn(tareaFirmaNotifier);
    }

    private List<DocumentoFirma> documentosFirma(int numeroDocumentos, TareaFirma tareaFirma) {
        List<DocumentoFirma> documentosFirma = new ArrayList<>();
        for (int i = 0; i < numeroDocumentos; i++) {
            MetaFile documentoOriginal = new MetaFile();
            documentoOriginal.setFileName("documento-" + i + ".pdf");

            DocumentoFirma documentoFirma = new DocumentoFirma();
            documentoFirma.setDocumentoOriginal(documentoOriginal);
            documentoFirma.setTareaFirma(tareaFirma);
            documentosFirma.add(documentoFirma);
        }
        return documentosFirma;
    }

    /** Usuario autenticado que NO es el firmante de la tarea. */
    private static User otroUsuario() {
        User otroUsuario = new User();
        otroUsuario.setId(99L);
        return otroUsuario;
    }

    private TareaFirma tareaFirmaPendiente(int numeroDocumentos) {
        TareaFirma tareaFirma = new TareaFirma();
        tareaFirma.setId(7L);
        tareaFirma.setFirmante(firmante);
        tareaFirma.setEstadoTareaFirma(EstadoTareaFirma.PENDIENTE);
        tareaFirma.setFechaSolicitud(LocalDateTime.now());
        tareaFirma.setMotivoFirma("Firma de los documentos");
        tareaFirma.setDocumentosFirma(documentosFirma(numeroDocumentos, tareaFirma));
        tareaFirma.setX(new BigDecimal("75"));
        tareaFirma.setY(new BigDecimal("200"));
        tareaFirma.setWidth(new BigDecimal("400"));
        tareaFirma.setHeight(new BigDecimal("60"));
        tareaFirma.setPage(1);
        tareaFirma.setFqcnFirmaNotifier(TareaFirmaNotifierDePrueba.class.getName());
        tareaFirma.setFqcnCallBackData(null);
        return tareaFirma;
    }

    /**
     * Segundo parámetro de las acciones del servicio. Ninguna de las acciones bajo test compara contra la
     * tarea original, así que se pasa una {@code TareaFirma} cualquiera.
     */
    private static TareaFirma tareaFirmaOriginalIrrelevante() {
        return new TareaFirma();
    }

    /**
     * Arrange base de las acciones sobre una tarea: tarea PENDIENTE con {@code numeroDocumentos}
     * documentos, la clave tecleada y la situación de firma del caso, y el usuario que el servidor da
     * por autenticado.
     */
    private TareaFirma arrangeTarea(int numeroDocumentos, SituacionFirma situacionFirma, String claveFirma, User usuarioAutenticado) {
        TareaFirma tareaFirma = tareaFirmaPendiente(numeroDocumentos);
        claveFirmaTecleada = claveFirma;
        stubUsuarioAutenticado(usuarioAutenticado);
        stubSituacionFirma(situacionFirma);
        return tareaFirma;
    }

    /**
     * Arrange común de los tests de {@code validateFirmarEnServidor}: tarea PENDIENTE del propio
     * firmante, con un documento y la situación de firma que fija el caso.
     */
    private TareaFirma arrangeValidacion(SituacionFirma situacionFirma, String claveFirma) {
        // Almacén genérico (no es de fichero): V-TareaFirma-008 no comprueba nada salvo que el caso lo
        // sustituya por un AlmacenClaveFichero con stubAlmacenClaveFichero.
        stubAlmacenClave(Mockito.mock(AlmacenClave.class));
        return arrangeTarea(1, situacionFirma, claveFirma, firmante);
    }

    /**
     * Variante del arrange de {@code validateFirmarEnServidor} para los casos en que el usuario
     * autenticado no es el firmante de la tarea (o no hay ninguno).
     */
    private TareaFirma arrangeValidacion(SituacionFirma situacionFirma, String claveFirma, User usuarioAutenticado) {
        stubAlmacenClave(Mockito.mock(AlmacenClave.class));
        return arrangeTarea(1, situacionFirma, claveFirma, usuarioAutenticado);
    }

    /**
     * Arrange común de los tests de {@code firmarEnServidor}: tarea PENDIENTE del usuario autenticado,
     * clave tecleada, la situación de firma del caso y {@code numeroDocumentos} documentos con su PDF
     * original mockeado.
     */
    private TareaFirma arrangeFirmaEnServidor(int numeroDocumentos, SituacionFirma situacionFirma) {
        TareaFirma tareaFirma = arrangeTarea(numeroDocumentos, situacionFirma, CLAVE, firmante);

        Mockito.lenient().when(modelServiceFactory.resolve(CertificadoDigital.class)).thenReturn(certificadoDigitalService);
        Mockito.lenient().when(certificadoDigitalService.getAlmacenClaveByDni(anyString(), any()))
                .thenAnswer(invocation -> Mockito.mock(AlmacenClave.class));

        // El PDF original se responde POR ARGUMENTO, nunca por orden de llamada: así la correspondencia
        // documento <-> PDF es explícita y no depende de cómo recorra producción la lista de documentos.
        // El mapa es de identidad porque dos MetaFile sin id nunca son iguales entre sí.
        Map<MetaFile, DocumentoPdf> pdfsPorDocumentoOriginal = new IdentityHashMap<>();
        documentosPdfOriginales = new ArrayList<>();
        for (DocumentoFirma documentoFirma : tareaFirma.getDocumentosFirma()) {
            DocumentoPdf documentoPdfOriginal = Mockito.mock(DocumentoPdf.class);
            Mockito.lenient().when(documentoPdfOriginal.firmar(any(), any())).thenReturn(Mockito.mock(DocumentoPdf.class));

            documentosPdfOriginales.add(documentoPdfOriginal);
            pdfsPorDocumentoOriginal.put(documentoFirma.getDocumentoOriginal(), documentoPdfOriginal);
        }

        metaFileHelperMock.when(() -> MetaFileHelper.getDocumentoPdf(any()))
                .thenAnswer(invocation -> pdfsPorDocumentoOriginal.get(invocation.getArgument(0)));
        metaFileHelperMock.when(() -> MetaFileHelper.createMetaFile(any()))
                .thenAnswer(invocation -> new MetaFile());

        stubGuardadoYNotificacion();

        return tareaFirma;
    }

    /**
     * Arrange común de los tests de {@code marcarComoFirmada}: tarea PENDIENTE con un documento. La
     * acción no valida ni consulta la situación de firma, así que no necesita más estáticos.
     */
    private TareaFirma arrangeMarcarComoFirmada() {
        TareaFirma tareaFirma = tareaFirmaPendiente(1);
        stubGuardadoYNotificacion();
        return tareaFirma;
    }

    /** Notificadora de prueba: da un FQCN real que resolver sin depender de ninguna notificadora del proyecto. */
    static class TareaFirmaNotifierDePrueba implements TareaFirmaNotifier {

        @Override
        public void notify(TareaFirma tareaFirma, Object callBackData) {
            // Nunca se ejecuta: Beans.get está mockeado y devuelve un mock de TareaFirmaNotifier.
        }
    }

    /* ------------------------------------------------------------------ */
    /* validateFirmarEnServidor                                           */
    /* ------------------------------------------------------------------ */

    @Test
    void validateFirmarEnServidor_tareaPendienteDelUsuarioConCertificadoConClaveGuardada_devuelveOptionalVacio() {
        TareaFirma tareaFirma = arrangeValidacion(SituacionFirma.FICHERO_CON_CLAVE, null);

        Optional<BusinessMessages> resultado = service.validateFirmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateFirmarEnServidor_tareaYaFirmada_devuelveMensajeSoloSePuedenFirmarLasPendientes() {
        TareaFirma tareaFirma = arrangeValidacion(SituacionFirma.FICHERO_CON_CLAVE, null);
        tareaFirma.setEstadoTareaFirma(EstadoTareaFirma.FIRMADO);

        Optional<BusinessMessages> resultado = service.validateFirmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada);

        assertTrue(mensajes(resultado).contains(MENSAJE_SOLO_PENDIENTES));
    }

    @Test
    void validateFirmarEnServidor_tareaRechazada_devuelveMensajeSoloSePuedenFirmarLasPendientes() {
        TareaFirma tareaFirma = arrangeValidacion(SituacionFirma.FICHERO_CON_CLAVE, null);
        tareaFirma.setEstadoTareaFirma(EstadoTareaFirma.RECHAZADO);

        Optional<BusinessMessages> resultado = service.validateFirmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada);

        assertTrue(mensajes(resultado).contains(MENSAJE_SOLO_PENDIENTES));
    }

    @Test
    void validateFirmarEnServidor_estadoNulo_devuelveMensajeSoloSePuedenFirmarLasPendientes() {
        TareaFirma tareaFirma = arrangeValidacion(SituacionFirma.FICHERO_CON_CLAVE, null);
        tareaFirma.setEstadoTareaFirma(null);

        Optional<BusinessMessages> resultado =
                assertDoesNotThrow(() -> service.validateFirmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada));

        assertTrue(mensajes(resultado).contains(MENSAJE_SOLO_PENDIENTES));
    }

    @Test
    void validateFirmarEnServidor_firmanteDistintoDelUsuarioAutenticado_devuelveMensajeSoloPuedeFirmarLaPersonaEncargada() {
        TareaFirma tareaFirma = arrangeValidacion(SituacionFirma.FICHERO_CON_CLAVE, null, otroUsuario());

        Optional<BusinessMessages> resultado = service.validateFirmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada);

        assertTrue(mensajes(resultado).contains(MENSAJE_SOLO_EL_ENCARGADO));
    }

    @Test
    void validateFirmarEnServidor_sinUsuarioAutenticado_devuelveMensajeSoloPuedeFirmarLaPersonaEncargada() {
        TareaFirma tareaFirma = arrangeValidacion(SituacionFirma.FICHERO_CON_CLAVE, null, null);

        Optional<BusinessMessages> resultado =
                assertDoesNotThrow(() -> service.validateFirmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada));

        assertTrue(mensajes(resultado).contains(MENSAJE_SOLO_EL_ENCARGADO));
    }

    @Test
    void validateFirmarEnServidor_situacionSinDni_devuelveMensajeSuUsuarioNoTieneDni() {
        TareaFirma tareaFirma = arrangeValidacion(SituacionFirma.SIN_DNI, null);

        Optional<BusinessMessages> resultado = service.validateFirmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada);

        assertTrue(mensajes(resultado).contains(MENSAJE_SIN_DNI));
    }

    @Test
    void validateFirmarEnServidor_situacionSinCertificado_devuelveMensajeNoTieneCertificadoDadoDeAlta() {
        TareaFirma tareaFirma = arrangeValidacion(SituacionFirma.SIN_CERTIFICADO, null);

        Optional<BusinessMessages> resultado = service.validateFirmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada);

        assertTrue(mensajes(resultado).contains(MENSAJE_SIN_CERTIFICADO));
    }

    @Test
    void validateFirmarEnServidor_dispositivoSinPinYSinClaveTecleada_devuelveMensajeElPinEsObligatorio() {
        TareaFirma tareaFirma = arrangeValidacion(SituacionFirma.DISPOSITIVO_SIN_PIN, null);

        Optional<BusinessMessages> resultado = service.validateFirmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada);

        assertTrue(mensajes(resultado).contains(MENSAJE_PIN_OBLIGATORIO));
    }

    @Test
    void validateFirmarEnServidor_dispositivoSinPinYClaveEnBlanco_devuelveMensajeElPinEsObligatorio() {
        TareaFirma tareaFirma = arrangeValidacion(SituacionFirma.DISPOSITIVO_SIN_PIN, CLAVE_EN_BLANCO);

        Optional<BusinessMessages> resultado = service.validateFirmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada);

        assertTrue(mensajes(resultado).contains(MENSAJE_PIN_OBLIGATORIO));
    }

    @Test
    void validateFirmarEnServidor_dispositivoSinPinConPinTecleado_devuelveOptionalVacio() {
        TareaFirma tareaFirma = arrangeValidacion(SituacionFirma.DISPOSITIVO_SIN_PIN, PIN);

        Optional<BusinessMessages> resultado = service.validateFirmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateFirmarEnServidor_ficheroSinClaveYSinClaveTecleada_devuelveMensajeLaContrasenaEsObligatoria() {
        TareaFirma tareaFirma = arrangeValidacion(SituacionFirma.FICHERO_SIN_CLAVE, null);

        Optional<BusinessMessages> resultado = service.validateFirmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada);

        assertTrue(mensajes(resultado).contains(MENSAJE_CONTRASENA_OBLIGATORIA));
    }

    @Test
    void validateFirmarEnServidor_ficheroSinClaveYClaveEnBlanco_devuelveMensajeLaContrasenaEsObligatoria() {
        TareaFirma tareaFirma = arrangeValidacion(SituacionFirma.FICHERO_SIN_CLAVE, "");

        Optional<BusinessMessages> resultado = service.validateFirmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada);

        assertTrue(mensajes(resultado).contains(MENSAJE_CONTRASENA_OBLIGATORIA));
    }

    @Test
    void validateFirmarEnServidor_ficheroSinClaveConClaveTecleada_devuelveOptionalVacio() {
        TareaFirma tareaFirma = arrangeValidacion(SituacionFirma.FICHERO_SIN_CLAVE, CLAVE);

        Optional<BusinessMessages> resultado = service.validateFirmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateFirmarEnServidor_ficheroConClaveGuardadaYSinClaveTecleada_devuelveOptionalVacio() {
        TareaFirma tareaFirma = arrangeValidacion(SituacionFirma.FICHERO_CON_CLAVE, null);

        Optional<BusinessMessages> resultado = service.validateFirmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateFirmarEnServidor_dispositivoConPinYSinClaveTecleada_devuelveOptionalVacio() {
        TareaFirma tareaFirma = arrangeValidacion(SituacionFirma.DISPOSITIVO_CON_PIN, null);

        Optional<BusinessMessages> resultado = service.validateFirmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateFirmarEnServidor_ficheroSinClaveYContrasenaTecleadaIncorrecta_devuelveMensajeLaContrasenaNoEsCorrecta() {
        TareaFirma tareaFirma = arrangeValidacion(SituacionFirma.FICHERO_SIN_CLAVE, CLAVE);
        stubAlmacenClaveFichero(false);

        Optional<BusinessMessages> resultado = service.validateFirmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada);

        assertTrue(mensajes(resultado).contains(MENSAJE_CONTRASENA_INCORRECTA));
    }

    @Test
    void validateFirmarEnServidor_ficheroSinClaveYContrasenaTecleadaCorrecta_devuelveOptionalVacio() {
        TareaFirma tareaFirma = arrangeValidacion(SituacionFirma.FICHERO_SIN_CLAVE, CLAVE);
        stubAlmacenClaveFichero(true);

        Optional<BusinessMessages> resultado = service.validateFirmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateFirmarEnServidor_ficheroConClaveGuardadaIncorrecta_devuelveMensajeQueRemiteAlAdministrador() {
        TareaFirma tareaFirma = arrangeValidacion(SituacionFirma.FICHERO_CON_CLAVE, null);
        stubAlmacenClaveFichero(false);

        Optional<BusinessMessages> resultado = service.validateFirmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada);

        assertTrue(mensajes(resultado).contains(MENSAJE_CLAVE_GUARDADA_INCORRECTA));
    }

    @Test
    void validateFirmarEnServidor_claveDelCertificadoIncorrecta_comprueLaClaveConElDniDelFirmanteYLaClaveTecleada() {
        TareaFirma tareaFirma = arrangeValidacion(SituacionFirma.FICHERO_SIN_CLAVE, CLAVE);
        stubAlmacenClaveFichero(false);

        service.validateFirmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada);

        verify(certificadoDigitalService).getAlmacenClaveByDni(DNI, CLAVE);
    }

    @Test
    void validateFirmarEnServidor_dispositivoConPin_noAbreElCertificadoParaComprobarElPin() {
        AlmacenClaveDispositivo dispositivo = stubAlmacenClaveDispositivo();
        TareaFirma tareaFirma = arrangeValidacion(SituacionFirma.DISPOSITIVO_CON_PIN, null);

        Optional<BusinessMessages> resultado = service.validateFirmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada);

        // Los intentos fallidos bloquean la tarjeta: el PIN no se comprueba nunca por adelantado.
        assertTrue(resultado.isEmpty());
        verifyNoInteractions(dispositivo);
    }

    @Test
    void validateFirmarEnServidor_dispositivoSinPinConPinTecleado_noAbreElCertificadoParaComprobarElPin() {
        AlmacenClaveDispositivo dispositivo = stubAlmacenClaveDispositivo();
        TareaFirma tareaFirma = arrangeValidacion(SituacionFirma.DISPOSITIVO_SIN_PIN, PIN);

        service.validateFirmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada);

        verifyNoInteractions(dispositivo);
    }

    @Test
    void validateFirmarEnServidor_certificadoIlegible_propagaElErrorDeLaAplicacion() {
        TareaFirma tareaFirma = arrangeValidacion(SituacionFirma.FICHERO_SIN_CLAVE, CLAVE);
        when(modelServiceFactory.resolve(CertificadoDigital.class)).thenReturn(certificadoDigitalService);
        RuntimeException errorDeLaAplicacion = new RuntimeException("El fichero del certificado no se puede leer");
        when(certificadoDigitalService.getAlmacenClaveByDni(anyString(), any())).thenThrow(errorDeLaAplicacion);

        // No poder leer el certificado es un error de la aplicación, no una validación que el firmante pueda
        // corregir: no se convierte en mensaje de negocio ni se traga, se propaga tal cual y falla todo.
        RuntimeException excepcion = assertThrows(RuntimeException.class,
                () -> service.validateFirmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada));

        assertSame(errorDeLaAplicacion, excepcion);
        assertFalse(excepcion instanceof ValidationException);
    }

    @Test
    void validateFirmarEnServidor_otraValidacionYaHaFallado_noAbreElCertificado() {
        TareaFirma tareaFirma = arrangeValidacion(SituacionFirma.FICHERO_CON_CLAVE, null);
        tareaFirma.setEstadoTareaFirma(EstadoTareaFirma.FIRMADO);

        service.validateFirmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada);

        // Abrir el certificado es getAlmacenClaveByDni: consultar de qué tipo es para saber la situación
        // de firma no lo abre, y por eso se acota la comprobación a esa llamada y no al mock entero.
        verify(certificadoDigitalService, never()).getAlmacenClaveByDni(anyString(), any());
    }

    @Test
    void validateFirmarEnServidor_tareaSinDocumentos_devuelveMensajeNoTieneNingunDocumento() {
        TareaFirma tareaFirma = arrangeValidacion(SituacionFirma.FICHERO_CON_CLAVE, null);
        tareaFirma.setDocumentosFirma(new ArrayList<>());

        Optional<BusinessMessages> resultado = service.validateFirmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada);

        assertTrue(mensajes(resultado).contains(MENSAJE_SIN_DOCUMENTOS));
    }

    @Test
    void validateFirmarEnServidor_documentosNulos_devuelveMensajeNoTieneNingunDocumento() {
        TareaFirma tareaFirma = arrangeValidacion(SituacionFirma.FICHERO_CON_CLAVE, null);
        tareaFirma.setDocumentosFirma(null);

        Optional<BusinessMessages> resultado =
                assertDoesNotThrow(() -> service.validateFirmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada));

        assertTrue(mensajes(resultado).contains(MENSAJE_SIN_DOCUMENTOS));
    }

    @Test
    void validateFirmarEnServidor_variasReglasIncumplidas_acumulaTodosLosMensajes() {
        TareaFirma tareaFirma = arrangeValidacion(SituacionFirma.SIN_CERTIFICADO, null, otroUsuario());
        tareaFirma.setEstadoTareaFirma(EstadoTareaFirma.FIRMADO);
        tareaFirma.setDocumentosFirma(new ArrayList<>());

        Optional<BusinessMessages> resultado = service.validateFirmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada);

        List<String> mensajes = mensajes(resultado);
        assertTrue(mensajes.contains(MENSAJE_SOLO_PENDIENTES));
        assertTrue(mensajes.contains(MENSAJE_SOLO_EL_ENCARGADO));
        assertTrue(mensajes.contains(MENSAJE_SIN_CERTIFICADO));
        assertTrue(mensajes.contains(MENSAJE_SIN_DOCUMENTOS));
    }

    @Test
    void validateFirmarEnServidor_claveTecleada_nuncaApareceEnLosMensajes() {
        TareaFirma tareaFirma = arrangeValidacion(SituacionFirma.SIN_CERTIFICADO, CLAVE_SECRETA);

        Optional<BusinessMessages> resultado = service.validateFirmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada);

        assertTrue(resultado.isPresent());
        // Se comprueba la clave completa y no sus prefijos: "clave" es también el principio del literal de
        // negocio "clave incorrecta", así que una comprobación por fragmentos daría un falso positivo.
        assertFalse(resultado.get().toString().contains(CLAVE_SECRETA));
    }

    @Test
    void validateFirmarEnServidor_siempre_recalculaLaSituacionDeFirmaEnElServidor() {
        TareaFirma tareaFirma = arrangeValidacion(SituacionFirma.SIN_CERTIFICADO, null);

        Optional<BusinessMessages> resultado = service.validateFirmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada);

        assertTrue(mensajes(resultado).contains(MENSAJE_SIN_CERTIFICADO));
        // La situación no se toma de lo que trajera la pantalla: se consulta el certificado dado de alta
        // para el DNI del firmante que trae la tarea cargada de base de datos.
        verify(certificadoDigitalService, atLeastOnce()).getSituacionFirmaByDni(DNI);
    }

    /* ------------------------------------------------------------------ */
    /* firmarEnServidor                                                   */
    /* ------------------------------------------------------------------ */

    @Test
    void firmarEnServidor_tareaValidaConUnDocumento_firmaGuardaYDejaLaTareaFirmada() {
        TareaFirma tareaFirma = arrangeFirmaEnServidor(1, SituacionFirma.FICHERO_CON_CLAVE);

        service.firmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada);

        assertNotNull(tareaFirma.getDocumentosFirma().get(0).getDocumentoFirmado());
        assertEquals(EstadoTareaFirma.FIRMADO, tareaFirma.getEstadoTareaFirma());
        assertNotNull(tareaFirma.getFechaResolucion());
        verify(repository).save(tareaFirma);
    }

    @Test
    void firmarEnServidor_tareaConDosDocumentos_pideUnAlmacenClaveNuevoPorCadaDocumento() {
        TareaFirma tareaFirma = arrangeFirmaEnServidor(2, SituacionFirma.FICHERO_CON_CLAVE);
        ArgumentCaptor<AlmacenClave> almacenDelPrimero = ArgumentCaptor.forClass(AlmacenClave.class);
        ArgumentCaptor<AlmacenClave> almacenDelSegundo = ArgumentCaptor.forClass(AlmacenClave.class);

        service.firmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada);

        // Se comprueba sobre el almacén que recibe cada documento, no contando las llamadas al servicio de
        // criptografía: la validación previa también le pide uno para comprobar la clave (V-TareaFirma-008).
        verify(documentosPdfOriginales.get(0)).firmar(almacenDelPrimero.capture(), any());
        verify(documentosPdfOriginales.get(1)).firmar(almacenDelSegundo.capture(), any());
        assertNotSame(almacenDelPrimero.getValue(), almacenDelSegundo.getValue());
    }

    @Test
    void firmarEnServidor_tareaConDosDocumentos_asignaUnMetaFileFirmadoDistintoACadaDocumento() {
        TareaFirma tareaFirma = arrangeFirmaEnServidor(2, SituacionFirma.FICHERO_CON_CLAVE);

        service.firmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada);

        MetaFile primero = tareaFirma.getDocumentosFirma().get(0).getDocumentoFirmado();
        MetaFile segundo = tareaFirma.getDocumentosFirma().get(1).getDocumentoFirmado();
        assertNotNull(primero);
        assertNotNull(segundo);
        assertNotSame(primero, segundo);
        metaFileHelperMock.verify(() -> MetaFileHelper.createMetaFile(any()), times(2));
    }

    @Test
    void firmarEnServidor_tareaValida_construyeElCampoFirmaConElRecuadroYLaPaginaDeLaTarea() {
        TareaFirma tareaFirma = arrangeFirmaEnServidor(1, SituacionFirma.FICHERO_CON_CLAVE);
        ArgumentCaptor<CampoFirma> captor = ArgumentCaptor.forClass(CampoFirma.class);

        service.firmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada);

        verify(documentosPdfOriginales.get(0)).firmar(any(), captor.capture());
        CampoFirma campoFirma = captor.getValue();
        assertEquals(75f, campoFirma.getRectanguloMensaje().x());
        assertEquals(200f, campoFirma.getRectanguloMensaje().y());
        assertEquals(400f, campoFirma.getRectanguloMensaje().width());
        assertEquals(60f, campoFirma.getRectanguloMensaje().height());
        assertEquals(1, campoFirma.getNumeroPagina());
    }

    @Test
    void firmarEnServidor_tareaValida_pasaAlServicioDeCriptografiaElDniDelFirmanteYLaClaveTecleada() {
        TareaFirma tareaFirma = arrangeFirmaEnServidor(1, SituacionFirma.FICHERO_CON_CLAVE);

        service.firmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada);

        // atLeastOnce: la validación previa pide otro almacén para comprobar la clave (V-TareaFirma-008).
        verify(certificadoDigitalService, atLeastOnce()).getAlmacenClaveByDni(DNI, CLAVE);
    }

    @Test
    void firmarEnServidor_firmaDelSegundoDocumentoFalla_noDejaFirmadoNingunDocumento() {
        TareaFirma tareaFirma = arrangeFirmaEnServidor(2, SituacionFirma.FICHERO_CON_CLAVE);
        when(documentosPdfOriginales.get(1).firmar(any(), any()))
                .thenThrow(new RuntimeException(MOTIVO_CLAVE_INCORRECTA));

        assertThrows(ValidationException.class, () -> service.firmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada));

        assertNull(tareaFirma.getDocumentosFirma().get(0).getDocumentoFirmado());
        assertNull(tareaFirma.getDocumentosFirma().get(1).getDocumentoFirmado());
        metaFileHelperMock.verify(() -> MetaFileHelper.createMetaFile(any()), never());
        verify(repository, never()).save(any());
    }

    @Test
    void firmarEnServidor_laFirmaFalla_lanzaValidationExceptionConMensajeQueEmpiezaPorNoSeHanPodidoFirmar() {
        TareaFirma tareaFirma = arrangeFirmaEnServidor(1, SituacionFirma.FICHERO_CON_CLAVE);
        when(documentosPdfOriginales.get(0).firmar(any(), any()))
                .thenThrow(new RuntimeException(MOTIVO_CLAVE_INCORRECTA));

        ValidationException excepcion = assertThrows(ValidationException.class,
                () -> service.firmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada));

        assertTrue(excepcion.getMessage().contains(MENSAJE_NO_SE_HAN_PODIDO_FIRMAR));
    }

    @Test
    void firmarEnServidor_laFirmaFalla_noFiltraElTextoTecnicoDeLaExcepcionAlUsuario() {
        TareaFirma tareaFirma = arrangeFirmaEnServidor(1, SituacionFirma.FICHERO_CON_CLAVE);
        when(documentosPdfOriginales.get(0).firmar(any(), any()))
                .thenThrow(new RuntimeException(new IOException(MOTIVO_TECNICO_DEL_JDK)));

        ValidationException excepcion = assertThrows(ValidationException.class,
                () -> service.firmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada));

        assertTrue(excepcion.getMessage().startsWith(MENSAJE_NO_SE_HAN_PODIDO_FIRMAR));
        assertFalse(excepcion.getMessage().contains(MOTIVO_TECNICO_DEL_JDK));
        assertFalse(excepcion.getMessage().contains("java."));
        assertFalse(excepcion.getMessage().contains("Exception"));
    }

    @Test
    void firmarEnServidor_ficheroSinClaveYContrasenaIncorrecta_avisaDeQueLaContrasenaNoEsCorrecta() {
        TareaFirma tareaFirma = arrangeFirmaEnServidor(1, SituacionFirma.FICHERO_SIN_CLAVE);
        claveFirmaTecleada = CLAVE_SECRETA;
        when(documentosPdfOriginales.get(0).firmar(any(), any()))
                .thenThrow(claveIncorrectaComoLaLanzaElJdk());

        ValidationException excepcion = assertThrows(ValidationException.class,
                () -> service.firmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada));

        assertEquals(MENSAJE_NO_SE_HAN_PODIDO_FIRMAR + "la contraseña indicada no es correcta",
                excepcion.getMessage());
        assertFalse(excepcion.getMessage().contains(CLAVE_SECRETA));
    }

    @Test
    void firmarEnServidor_dispositivoSinPinYPinIncorrecto_avisaDeQueElPinNoEsCorrecto() {
        TareaFirma tareaFirma = arrangeFirmaEnServidor(1, SituacionFirma.DISPOSITIVO_SIN_PIN);
        claveFirmaTecleada = CLAVE_SECRETA;
        when(documentosPdfOriginales.get(0).firmar(any(), any()))
                .thenThrow(new RuntimeException(new IOException(new LoginException("failed login"))));

        ValidationException excepcion = assertThrows(ValidationException.class,
                () -> service.firmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada));

        assertEquals(MENSAJE_NO_SE_HAN_PODIDO_FIRMAR + "el PIN indicado no es correcto",
                excepcion.getMessage());
        assertFalse(excepcion.getMessage().contains(CLAVE_SECRETA));
    }

    @Test
    void firmarEnServidor_claveGuardadaIncorrecta_remiteAlAdministradorEnVezDeAlFirmante() {
        TareaFirma tareaFirma = arrangeFirmaEnServidor(1, SituacionFirma.FICHERO_CON_CLAVE);
        when(documentosPdfOriginales.get(0).firmar(any(), any()))
                .thenThrow(claveIncorrectaComoLaLanzaElJdk());

        ValidationException excepcion = assertThrows(ValidationException.class,
                () -> service.firmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada));

        assertEquals(MENSAJE_NO_SE_HAN_PODIDO_FIRMAR
                        + "la clave guardada de su certificado digital no es correcta. Póngase en contacto con el administrador",
                excepcion.getMessage());
    }

    /**
     * Reproduce la cadena de causas real de una contraseña incorrecta: {@code KeyStore.load} lanza una
     * {@code IOException} con {@code UnrecoverableKeyException} como causa, y tanto
     * {@code CriptografiaUtil.getKeyStore} como {@code DocumentoPdf.firmar} la envuelven en una
     * {@code RuntimeException}. Por eso el marcador queda a tres niveles de profundidad.
     */
    private static RuntimeException claveIncorrectaComoLaLanzaElJdk() {
        return new RuntimeException(new RuntimeException(
                new IOException(MOTIVO_TECNICO_DEL_JDK, new UnrecoverableKeyException(MOTIVO_TECNICO_DEL_JDK))));
    }

    @Test
    void firmarEnServidor_laObtencionDelAlmacenClaveFalla_propagaElErrorDeLaAplicacionSinFirmarNada() {
        TareaFirma tareaFirma = arrangeFirmaEnServidor(1, SituacionFirma.FICHERO_CON_CLAVE);
        RuntimeException errorDeLaAplicacion =
                new RuntimeException("No se puede leer el certificado desde el sistema de archivos: /certificados/firmante.p12");
        when(certificadoDigitalService.getAlmacenClaveByDni(any(), any())).thenThrow(errorDeLaAplicacion);

        // Un certificado que no se puede leer es un error de la aplicación, no un fallo de firma que se
        // traduzca al motivo de negocio de RN-TareaFirma-007: la excepción sale tal cual y la acción falla entera.
        RuntimeException excepcion = assertThrows(RuntimeException.class,
                () -> service.firmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada));

        assertSame(errorDeLaAplicacion, excepcion);
        assertNull(tareaFirma.getDocumentosFirma().get(0).getDocumentoFirmado());
        assertEquals(EstadoTareaFirma.PENDIENTE, tareaFirma.getEstadoTareaFirma());
        verify(repository, never()).save(any());
        verifyNoInteractions(tareaFirmaNotifier);
    }

    @Test
    void firmarEnServidor_elPdfOriginalNoEsValido_lanzaElMismoErrorDeNegocio() {
        TareaFirma tareaFirma = arrangeFirmaEnServidor(1, SituacionFirma.FICHERO_CON_CLAVE);
        metaFileHelperMock.when(() -> MetaFileHelper.getDocumentoPdf(any()))
                .thenThrow(new RuntimeException("El MetaFile no es de tipo PDF"));

        ValidationException excepcion = assertThrows(ValidationException.class,
                () -> service.firmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada));

        assertTrue(excepcion.getMessage().startsWith(MENSAJE_NO_SE_HAN_PODIDO_FIRMAR));
    }

    @Test
    void firmarEnServidor_laFirmaFalla_dejaLaTareaPendienteYSinFechaDeResolucion() {
        TareaFirma tareaFirma = arrangeFirmaEnServidor(1, SituacionFirma.FICHERO_CON_CLAVE);
        when(documentosPdfOriginales.get(0).firmar(any(), any()))
                .thenThrow(new RuntimeException(MOTIVO_CLAVE_INCORRECTA));

        assertThrows(ValidationException.class, () -> service.firmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada));

        assertEquals(EstadoTareaFirma.PENDIENTE, tareaFirma.getEstadoTareaFirma());
        assertNull(tareaFirma.getFechaResolucion());
    }

    @Test
    void firmarEnServidor_tareaValida_asignaEstadoYFechaDeResolucionIncondicionalmente() {
        TareaFirma tareaFirma = arrangeFirmaEnServidor(1, SituacionFirma.FICHERO_CON_CLAVE);
        LocalDateTime fechaAntigua = LocalDateTime.of(2000, 1, 1, 0, 0);
        tareaFirma.setFechaResolucion(fechaAntigua);

        service.firmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada);

        assertNotEquals(fechaAntigua, tareaFirma.getFechaResolucion());
        assertEquals(EstadoTareaFirma.FIRMADO, tareaFirma.getEstadoTareaFirma());
    }

    @Test
    void firmarEnServidor_validacionRechazada_lanzaValidationExceptionYNoFirmaNiGuarda() {
        TareaFirma tareaFirma = arrangeFirmaEnServidor(1, SituacionFirma.SIN_CERTIFICADO);
        tareaFirma.setEstadoTareaFirma(EstadoTareaFirma.FIRMADO);

        ValidationException excepcion = assertThrows(ValidationException.class,
                () -> service.firmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada));

        assertTrue(excepcion.getMessage().contains(MENSAJE_SOLO_PENDIENTES));
        verify(repository, never()).save(any());
        metaFileHelperMock.verify(() -> MetaFileHelper.createMetaFile(any()), never());
        verify(certificadoDigitalService, never()).getAlmacenClaveByDni(any(), any());
    }

    @Test
    void firmarEnServidor_firmaCompletada_notificaAlProcesoQueEncargoLaFirma() {
        TareaFirma tareaFirma = arrangeFirmaEnServidor(1, SituacionFirma.FICHERO_CON_CLAVE);

        service.firmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada);

        InOrder inOrder = Mockito.inOrder(repository, tareaFirmaNotifier);
        inOrder.verify(repository).save(tareaFirma);
        inOrder.verify(tareaFirmaNotifier).notify(tareaFirma, null);
        verify(tareaFirmaNotifier, times(1)).notify(tareaFirma, null);
    }

    @Test
    void firmarEnServidor_laFirmaFalla_noNotificaAlProcesoQueEncargoLaFirma() {
        TareaFirma tareaFirma = arrangeFirmaEnServidor(1, SituacionFirma.FICHERO_CON_CLAVE);
        when(documentosPdfOriginales.get(0).firmar(any(), any()))
                .thenThrow(new RuntimeException(MOTIVO_CLAVE_INCORRECTA));

        assertThrows(ValidationException.class, () -> service.firmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada));

        verifyNoInteractions(tareaFirmaNotifier);
    }

    @Test
    void firmarEnServidor_firmaCompletada_devuelveLaTareaDevueltaPorElRepositorio() {
        TareaFirma tareaFirma = arrangeFirmaEnServidor(1, SituacionFirma.FICHERO_CON_CLAVE);
        TareaFirma tareaFirmaGuardada = tareaFirmaPendiente(1);
        when(repository.save(any())).thenReturn(tareaFirmaGuardada);

        TareaFirma resultado = service.firmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada);

        assertSame(tareaFirmaGuardada, resultado);
    }

    @Test
    void firmarEnServidor_laFirmaFalla_noIncluyeLaClaveDeFirmaEnElMensajeDeError() {
        TareaFirma tareaFirma = arrangeFirmaEnServidor(1, SituacionFirma.FICHERO_CON_CLAVE);
        claveFirmaTecleada = CLAVE_SECRETA;
        when(documentosPdfOriginales.get(0).firmar(any(), any()))
                .thenThrow(new RuntimeException(MOTIVO_CLAVE_INCORRECTA));

        ValidationException excepcion = assertThrows(ValidationException.class,
                () -> service.firmarEnServidor(tareaFirma, tareaFirmaOriginalIrrelevante(), claveFirmaTecleada));

        // Se comprueba la clave completa y no sus prefijos: "clave" es también el principio del literal de
        // negocio "clave incorrecta", así que una comprobación por fragmentos daría un falso positivo.
        assertFalse(excepcion.getMessage().contains(CLAVE_SECRETA));
    }

    /* ------------------------------------------------------------------ */
    /* marcarComoFirmada                                                  */
    /* ------------------------------------------------------------------ */

    @Test
    void marcarComoFirmada_tareaValida_dejaLaTareaFirmadaConFechaDeResolucion() {
        TareaFirma tareaFirma = arrangeMarcarComoFirmada();

        service.marcarComoFirmada(tareaFirma, tareaFirmaOriginalIrrelevante());

        assertEquals(EstadoTareaFirma.FIRMADO, tareaFirma.getEstadoTareaFirma());
        assertNotNull(tareaFirma.getFechaResolucion());
        verify(repository).save(tareaFirma);
    }

    @Test
    void marcarComoFirmada_fechaResolucionPreexistente_laSobrescribe() {
        TareaFirma tareaFirma = arrangeMarcarComoFirmada();
        LocalDateTime fechaAntigua = LocalDateTime.of(2000, 1, 1, 0, 0);
        tareaFirma.setFechaResolucion(fechaAntigua);

        service.marcarComoFirmada(tareaFirma, tareaFirmaOriginalIrrelevante());

        assertNotEquals(fechaAntigua, tareaFirma.getFechaResolucion());
    }

    @Test
    void marcarComoFirmada_tareaValida_notificaAlProcesoQueEncargoLaFirma() {
        TareaFirma tareaFirma = arrangeMarcarComoFirmada();

        service.marcarComoFirmada(tareaFirma, tareaFirmaOriginalIrrelevante());

        verify(tareaFirmaNotifier, times(1)).notify(tareaFirma, null);
    }

    /* ------------------------------------------------------------------ */
    /* AllowProperties                                                    */
    /* ------------------------------------------------------------------ */

    @Test
    void allowPropertiesFirmarEnServidor_noPermiteNingunCampo() {
        AllowProperties allowProperties = service.allowPropertiesFirmarEnServidor();

        // La clave de firma no es un campo de la entidad: llega al servicio como argumento, no por la whitelist.
        assertFalse(allowProperties.allowProperty("claveFirma"));
        assertFalse(allowProperties.allowProperty("situacionFirma"));
        assertFalse(allowProperties.allowProperty("estadoTareaFirma"));
        assertFalse(allowProperties.allowProperty("fechaResolucion"));
        assertFalse(allowProperties.allowProperty("firmante"));
        assertFalse(allowProperties.allowProperty("documentosFirma"));
        assertFalse(allowProperties.allowProperty("motivoFirma"));
        assertFalse(allowProperties.allowProperty("motivoRechazo"));
        assertFalse(allowProperties.allowProperty("fqcnFirmaNotifier"));
        assertFalse(allowProperties.allowProperty("fqcnCallBackData"));
        assertFalse(allowProperties.allowProperty("callBackData"));
        assertFalse(allowProperties.allowProperty("x"));
        assertFalse(allowProperties.allowProperty("y"));
        assertFalse(allowProperties.allowProperty("width"));
        assertFalse(allowProperties.allowProperty("height"));
        assertFalse(allowProperties.allowProperty("page"));
    }

    @Test
    void allowPropertiesInsert_noPermiteNingunCampo() {
        AllowProperties allowProperties = service.allowPropertiesInsert();

        assertFalse(allowProperties.allowProperty("firmante"));
        assertFalse(allowProperties.allowProperty("estadoTareaFirma"));
        assertFalse(allowProperties.allowProperty("documentosFirma"));
        assertFalse(allowProperties.allowProperty("fqcnFirmaNotifier"));
    }

    @Test
    void allowPropertiesUpdate_noPermiteNingunCampo() {
        AllowProperties allowProperties = service.allowPropertiesUpdate();

        assertFalse(allowProperties.allowProperty("estadoTareaFirma"));
        assertFalse(allowProperties.allowProperty("fechaResolucion"));
        assertFalse(allowProperties.allowProperty("firmante"));
        assertFalse(allowProperties.allowProperty("documentosFirma"));
    }
}
