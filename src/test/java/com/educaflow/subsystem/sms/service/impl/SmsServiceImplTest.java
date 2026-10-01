package com.educaflow.subsystem.sms.service.impl;

import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.db.JpaRepository;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.i18n.I18n;
import com.educaflow.base.infrastructure.async.EjecutorAsincrono;
import com.educaflow.base.infrastructure.sms.SmsSender;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.common.db.CentroUsuarioTipoUsuario;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.subsystem.common.db.TipoUsuarioCodigo;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.db.HistorialEstado;
import com.educaflow.subsystem.sms.db.EstadoSms;
import com.educaflow.subsystem.sms.db.Sms;
import jakarta.inject.Provider;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsServiceImplTest {

    private static final String DNI_VALIDO = "12345678Z";
    private static final String DNI_LETRA_INCORRECTA = "12345678A";
    private static final Long SMS_ID = 100L;

    private static final String MSG_DNI_OBLIGATORIO = "El DNI del destinatario es obligatorio";
    private static final String MSG_DNI_NO_VALIDO = "El DNI del destinatario no es válido; compruebe la letra";
    private static final String MSG_NOMBRE_OBLIGATORIO = "El nombre es obligatorio";
    private static final String MSG_APELLIDOS_OBLIGATORIOS = "Los apellidos son obligatorios";
    private static final String MSG_TELEFONO_OBLIGATORIO = "El teléfono es obligatorio";
    private static final String MSG_TELEFONO_NO_MOVIL_ESPANA =
            "El teléfono debe ser un número de móvil de España válido (por ejemplo, 600111222)";
    private static final String MSG_MENSAJE_OBLIGATORIO = "El mensaje es obligatorio";
    private static final String MSG_MENSAJE_NO_CABE =
            "El mensaje no cabe en un solo SMS: como máximo 160 caracteres, o 70 si contiene acentos u otros caracteres especiales";
    private static final String MSG_CENTRO_OBLIGATORIO = "El centro es obligatorio";
    private static final String MSG_CENTRO_NO_SUYO = "No puede crear SMS para un centro que no es suyo";
    private static final String MSG_HISTORIAL_NO_EXISTE = "El estado del expediente indicado no existe";
    private static final String MSG_INMUTABLE = "El SMS es inmutable tras su creación.";
    private static final String MSG_NO_BORRABLE = "Los SMS no se pueden borrar.";
    private static final String MSG_SOLO_FALLIDOS = "Solo se pueden reenviar SMS que han fallado";
    private static final String MSG_REENVIO_CENTRO_NO_SUYO = "No puede reenviar SMS de un centro que no es suyo";

    private static final List<String> CAMPOS_CLIENTE = List.of(
            "dniDestinatario", "nombre", "apellidos", "telefono", "mensaje", "centro", "historialEstado");
    private static final List<String> CAMPOS_SERVIDOR = List.of(
            "estado", "fechaCreacion", "fechaPrimerIntentoEnvio", "fechaUltimoIntentoEnvio",
            "fechaEnvio", "numeroReintentos", "descripcionUltimoFallo");

    private Repository<Sms> repository;
    private SmsServiceImpl service;
    private Provider<SmsSender> smsSenderProvider;
    private SmsSender smsSender;
    private EjecutorAsincrono ejecutorAsincrono;

    private Centro centroA;
    private Centro centroB;

    private MockedStatic<I18n> i18nMock;
    private MockedStatic<SecurityUtil> securityUtilMock;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        repository = Mockito.mock(Repository.class);
        service = new SmsServiceImpl(Sms.class, repository);

        smsSenderProvider = Mockito.mock(Provider.class);
        smsSender = Mockito.mock(SmsSender.class);
        ejecutorAsincrono = Mockito.mock(EjecutorAsincrono.class);
        setField(service, "smsSenderProvider", smsSenderProvider);
        setField(service, "ejecutorAsincrono", ejecutorAsincrono);

        centroA = new Centro();
        centroA.setId(1L);
        centroB = new Centro();
        centroB.setId(2L);

        i18nMock = Mockito.mockStatic(I18n.class, Mockito.withSettings().strictness(Strictness.LENIENT));
        i18nMock.when(() -> I18n.get(any(String.class))).thenAnswer(invocation -> invocation.getArgument(0));

        securityUtilMock = Mockito.mockStatic(SecurityUtil.class, Mockito.withSettings().strictness(Strictness.LENIENT));
    }

    @AfterEach
    void tearDown() {
        i18nMock.close();
        securityUtilMock.close();
    }

    /* ------------------------------------------------------------------ */
    /* Helpers                                                            */
    /* ------------------------------------------------------------------ */

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = SmsServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Sms smsValido() {
        Sms sms = new Sms();
        sms.setDniDestinatario(DNI_VALIDO);
        sms.setNombre("Juan");
        sms.setApellidos("Pérez");
        sms.setTelefono("600111222");
        sms.setMensaje("Mañana no hay clase");
        sms.setCentro(centroA);
        sms.setHistorialEstado(null);
        return sms;
    }

    private Sms smsFallido(Centro centro) {
        Sms sms = smsValido();
        sms.setId(SMS_ID);
        sms.setEstado(EstadoSms.FALLIDO);
        sms.setCentro(centro);
        return sms;
    }

    private Sms entidadConSoloId() {
        Sms sms = new Sms();
        sms.setId(SMS_ID);
        return sms;
    }

    private Sms smsEnBd(EstadoSms estado) {
        Sms sms = new Sms();
        sms.setId(SMS_ID);
        sms.setEstado(estado);
        sms.setTelefono("+34600111222");
        sms.setMensaje("Mañana no hay clase");
        sms.setCentro(centroA);
        sms.setNumeroReintentos(0);
        sms.setFechaPrimerIntentoEnvio(null);
        sms.setFechaUltimoIntentoEnvio(null);
        return sms;
    }

    private void stubIsAdmin(boolean esAdmin) {
        securityUtilMock.when(() -> SecurityUtil.isAdmin(any())).thenReturn(esAdmin);
    }

    private void stubUsuario(User user) {
        securityUtilMock.when(SecurityUtil::getUser).thenReturn(user);
    }

    private User usuarioDeCentro(Centro centro) {
        CentroUsuario centroUsuario = new CentroUsuario();
        centroUsuario.setCentro(centro);
        User user = new User();
        user.setCentroUsuarios(List.of(centroUsuario));
        return user;
    }

    private User usuarioConTipoEnCentro(Centro centro, TipoUsuarioCodigo codigo) {
        TipoUsuario tipoUsuario = new TipoUsuario();
        tipoUsuario.setCodigo(codigo);
        CentroUsuarioTipoUsuario centroUsuarioTipoUsuario = new CentroUsuarioTipoUsuario();
        centroUsuarioTipoUsuario.setTipoUsuario(tipoUsuario);
        CentroUsuario centroUsuario = new CentroUsuario();
        centroUsuario.setCentro(centro);
        centroUsuario.setCentroUsuarioTipoUsuario(List.of(centroUsuarioTipoUsuario));
        centroUsuarioTipoUsuario.setCentroUsuario(centroUsuario);
        User user = new User();
        user.setCentroUsuarios(List.of(centroUsuario));
        return user;
    }

    private HistorialEstado historialEstadoDeCentro(Long id, Centro centroExpediente) {
        Expediente expediente = new Expediente();
        expediente.setCentro(centroExpediente);
        HistorialEstado historialEstado = new HistorialEstado();
        historialEstado.setId(id);
        historialEstado.setExpediente(expediente);
        return historialEstado;
    }

    private String mensaje(Optional<BusinessMessages> optional) {
        assertTrue(optional.isPresent());
        return optional.get().get(0).getMessage();
    }

    private List<String> mensajes(Optional<BusinessMessages> optional) {
        assertTrue(optional.isPresent());
        return optional.get().stream().map(BusinessMessage::getMessage).toList();
    }

    private MockedStatic<JPA> mockJpaRunInTransaction() {
        MockedStatic<JPA> jpaMock = Mockito.mockStatic(JPA.class);
        jpaMock.when(() -> JPA.runInTransaction(any(Runnable.class)))
                .thenAnswer(invocation -> {
                    ((Runnable) invocation.getArgument(0)).run();
                    return null;
                });
        return jpaMock;
    }

    private Runnable tareaProgramada() {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(ejecutorAsincrono).ejecutarTrasCommit(captor.capture());
        return captor.getValue();
    }

    private void ejecutarTareaProgramada() {
        Runnable tarea = tareaProgramada();
        try (MockedStatic<JPA> ignored = mockJpaRunInTransaction()) {
            tarea.run();
        }
    }

    private void programarEnvioDe(Sms enBd) {
        stubIsAdmin(true);
        when(repository.find(SMS_ID)).thenReturn(enBd);
        service.reenviar(entidadConSoloId(), smsFallido(centroA));
    }

    /* ------------------------------------------------------------------ */
    /* validateInsert                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    void validateInsert_smsValido_devuelveOptionalVacio() {
        stubIsAdmin(true);

        assertTrue(service.validateInsert(smsValido()).isEmpty());
    }

    @Test
    void validateInsert_dniDestinatarioNulo_devuelveMensajeObligatorio() {
        Sms sms = smsValido();
        sms.setDniDestinatario(null);
        stubIsAdmin(true);

        assertEquals(List.of(MSG_DNI_OBLIGATORIO), mensajes(service.validateInsert(sms)));
    }

    @Test
    void validateInsert_dniDestinatarioEnBlanco_devuelveMensajeObligatorio() {
        Sms sms = smsValido();
        sms.setDniDestinatario("   ");
        stubIsAdmin(true);

        assertEquals(MSG_DNI_OBLIGATORIO, mensaje(service.validateInsert(sms)));
    }

    @Test
    void validateInsert_dniDestinatarioConLetraIncorrecta_devuelveMensajeNoValido() {
        Sms sms = smsValido();
        sms.setDniDestinatario(DNI_LETRA_INCORRECTA);
        stubIsAdmin(true);

        List<String> resultado = mensajes(service.validateInsert(sms));

        assertEquals(List.of(MSG_DNI_NO_VALIDO), resultado);
        assertFalse(resultado.get(0).contains(DNI_LETRA_INCORRECTA));
    }

    @Test
    void validateInsert_dniDestinatarioNulo_noAnadeElMensajeDeDniNoValido() {
        Sms sms = smsValido();
        sms.setDniDestinatario(null);
        stubIsAdmin(true);

        List<String> resultado = mensajes(service.validateInsert(sms));

        assertEquals(1, resultado.size());
        assertEquals(MSG_DNI_OBLIGATORIO, resultado.get(0));
        assertFalse(resultado.contains(MSG_DNI_NO_VALIDO));
    }

    @Test
    void validateInsert_nombreNuloOEnBlanco_devuelveMensajeObligatorio() {
        Sms nulo = smsValido();
        nulo.setNombre(null);
        Sms blanco = smsValido();
        blanco.setNombre("   ");
        stubIsAdmin(true);

        assertEquals(MSG_NOMBRE_OBLIGATORIO, mensaje(service.validateInsert(nulo)));
        assertEquals(MSG_NOMBRE_OBLIGATORIO, mensaje(service.validateInsert(blanco)));
    }

    @Test
    void validateInsert_apellidosNulosOEnBlanco_devuelveMensajeObligatorio() {
        Sms nulo = smsValido();
        nulo.setApellidos(null);
        Sms blanco = smsValido();
        blanco.setApellidos("   ");
        stubIsAdmin(true);

        assertEquals(MSG_APELLIDOS_OBLIGATORIOS, mensaje(service.validateInsert(nulo)));
        assertEquals(MSG_APELLIDOS_OBLIGATORIOS, mensaje(service.validateInsert(blanco)));
    }

    @Test
    void validateInsert_nombreYApellidosAusentes_devuelveLosDosMensajes() {
        Sms sms = smsValido();
        sms.setNombre(null);
        sms.setApellidos(null);
        stubIsAdmin(true);

        assertEquals(List.of(MSG_NOMBRE_OBLIGATORIO, MSG_APELLIDOS_OBLIGATORIOS), mensajes(service.validateInsert(sms)));
    }

    @Test
    void validateInsert_variosCamposAusentes_acumulaTodosLosMensajes() {
        stubIsAdmin(true);

        List<String> resultado = mensajes(service.validateInsert(new Sms()));

        assertEquals(List.of(
                MSG_DNI_OBLIGATORIO,
                MSG_NOMBRE_OBLIGATORIO,
                MSG_APELLIDOS_OBLIGATORIOS,
                MSG_TELEFONO_OBLIGATORIO,
                MSG_MENSAJE_OBLIGATORIO,
                MSG_CENTRO_OBLIGATORIO), resultado);
        assertFalse(resultado.contains(MSG_DNI_NO_VALIDO));
        assertFalse(resultado.contains(MSG_TELEFONO_NO_MOVIL_ESPANA));
        assertFalse(resultado.contains(MSG_MENSAJE_NO_CABE));
        assertFalse(resultado.contains(MSG_CENTRO_NO_SUYO));
        assertFalse(resultado.contains(MSG_HISTORIAL_NO_EXISTE));
    }

    @Test
    void validateInsert_telefonoNuloOEnBlanco_devuelveMensajeObligatorio() {
        Sms nulo = smsValido();
        nulo.setTelefono(null);
        Sms blanco = smsValido();
        blanco.setTelefono("   ");
        stubIsAdmin(true);

        assertEquals(MSG_TELEFONO_OBLIGATORIO, mensaje(service.validateInsert(nulo)));
        assertEquals(MSG_TELEFONO_OBLIGATORIO, mensaje(service.validateInsert(blanco)));
    }

    @Test
    void validateInsert_telefonoFijoEspanol_devuelveMensajeDeMovilDeEspana() {
        Sms sms = smsValido();
        sms.setTelefono("963000000");
        stubIsAdmin(true);

        assertEquals(List.of(MSG_TELEFONO_NO_MOVIL_ESPANA), mensajes(service.validateInsert(sms)));
    }

    @Test
    void validateInsert_telefonoIncompleto_devuelveMensajeDeMovilDeEspana() {
        Sms sms = smsValido();
        sms.setTelefono("60011");
        stubIsAdmin(true);

        assertEquals(MSG_TELEFONO_NO_MOVIL_ESPANA, mensaje(service.validateInsert(sms)));
    }

    @Test
    void validateInsert_telefonoMovilExtranjeroValido_devuelveMensajeDeMovilDeEspana() {
        Sms sms = smsValido();
        sms.setTelefono("+33612345678");
        stubIsAdmin(true);

        assertEquals(MSG_TELEFONO_NO_MOVIL_ESPANA, mensaje(service.validateInsert(sms)));
    }

    @Test
    void validateInsert_telefonoMovilSinPrefijo_esValido() {
        Sms sms = smsValido();
        sms.setTelefono("600111222");
        stubIsAdmin(true);

        assertTrue(service.validateInsert(sms).isEmpty());
    }

    @Test
    void validateInsert_telefonoNulo_noAnadeElMensajeDeMovilDeEspana() {
        Sms sms = smsValido();
        sms.setTelefono(null);
        stubIsAdmin(true);

        assertEquals(List.of(MSG_TELEFONO_OBLIGATORIO), mensajes(service.validateInsert(sms)));
    }

    @Test
    void validateInsert_mensajeNuloOEnBlanco_devuelveMensajeObligatorioSinLanzar() {
        stubIsAdmin(true);

        for (String mensaje : new String[]{null, "", "   "}) {
            Sms sms = smsValido();
            sms.setMensaje(mensaje);

            Optional<BusinessMessages> resultado = assertDoesNotThrow(() -> service.validateInsert(sms));

            assertEquals(List.of(MSG_MENSAJE_OBLIGATORIO), mensajes(resultado));
        }
    }

    @Test
    void validateInsert_mensajeQueNoCabeEnUnSms_devuelveMensajeDeLongitud() {
        Sms sms = smsValido();
        sms.setMensaje("a".repeat(161));
        stubIsAdmin(true);

        assertEquals(List.of(MSG_MENSAJE_NO_CABE), mensajes(service.validateInsert(sms)));
    }

    @Test
    void validateInsert_mensajeConAcentosQueSuperaSetentaCaracteres_devuelveMensajeDeLongitud() {
        Sms sms = smsValido();
        sms.setMensaje("ú" + "a".repeat(70));
        stubIsAdmin(true);

        assertEquals(MSG_MENSAJE_NO_CABE, mensaje(service.validateInsert(sms)));
    }

    @Test
    void validateInsert_mensajeDeCientoSesentaCaracteresBasicos_esValido() {
        Sms sms = smsValido();
        sms.setMensaje("a".repeat(160));
        stubIsAdmin(true);

        assertTrue(service.validateInsert(sms).isEmpty());
    }

    @Test
    void validateInsert_mensajeConAcentoDeSetentaCaracteres_esValido() {
        Sms sms = smsValido();
        sms.setMensaje("ú" + "a".repeat(69));
        stubIsAdmin(true);

        assertTrue(service.validateInsert(sms).isEmpty());
    }

    @Test
    void validateInsert_centroNulo_devuelveMensajeObligatorio() {
        Sms sms = smsValido();
        sms.setCentro(null);
        stubIsAdmin(true);

        assertEquals(List.of(MSG_CENTRO_OBLIGATORIO), mensajes(service.validateInsert(sms)));
    }

    @Test
    void validateInsert_usuarioNoAdminConCentroAjeno_devuelveMensajeCentroNoSuyo() {
        Sms sms = smsValido();
        sms.setCentro(centroB);
        stubIsAdmin(false);
        stubUsuario(usuarioDeCentro(centroA));

        assertEquals(List.of(MSG_CENTRO_NO_SUYO), mensajes(service.validateInsert(sms)));
    }

    @Test
    void validateInsert_usuarioNoAdminConCentroPropio_esValido() {
        Sms sms = smsValido();
        sms.setCentro(centroA);
        stubIsAdmin(false);
        stubUsuario(usuarioDeCentro(centroA));

        assertTrue(service.validateInsert(sms).isEmpty());
    }

    @Test
    void validateInsert_administradorConCualquierCentro_esValido() {
        Sms sms = smsValido();
        sms.setCentro(centroB);
        stubIsAdmin(true);

        assertTrue(service.validateInsert(sms).isEmpty());
    }

    @Test
    void validateInsert_centroNuloYUsuarioNoAdmin_devuelveSoloElMensajeDeObligatorio() {
        Sms sms = smsValido();
        sms.setCentro(null);
        stubIsAdmin(false);
        stubUsuario(usuarioDeCentro(centroA));

        List<String> resultado = mensajes(service.validateInsert(sms));

        assertEquals(List.of(MSG_CENTRO_OBLIGATORIO), resultado);
        assertFalse(resultado.contains(MSG_CENTRO_NO_SUYO));
    }

    @Test
    void validateInsert_historialEstadoConExpedienteDelMismoCentro_esValidoSinReleerPorRepositorio() {
        Sms sms = smsValido();
        sms.setCentro(centroA);
        sms.setHistorialEstado(historialEstadoDeCentro(5L, centroA));
        stubIsAdmin(true);

        try (MockedStatic<JpaRepository> jpaRepositoryMock = Mockito.mockStatic(JpaRepository.class)) {
            Optional<BusinessMessages> resultado = service.validateInsert(sms);

            assertTrue(resultado.isEmpty());
            jpaRepositoryMock.verifyNoInteractions();
        }
    }

    @Test
    void validateInsert_historialEstadoSinId_devuelveMensajeNoExiste() {
        Sms sms = smsValido();
        sms.setHistorialEstado(new HistorialEstado());
        stubIsAdmin(true);

        assertEquals(List.of(MSG_HISTORIAL_NO_EXISTE), mensajes(service.validateInsert(sms)));
    }

    @Test
    void validateInsert_historialEstadoSinExpediente_devuelveMensajeNoExiste() {
        Sms sms = smsValido();
        HistorialEstado historialEstado = new HistorialEstado();
        historialEstado.setId(5L);
        historialEstado.setExpediente(null);
        sms.setHistorialEstado(historialEstado);
        stubIsAdmin(true);

        assertEquals(List.of(MSG_HISTORIAL_NO_EXISTE), mensajes(service.validateInsert(sms)));
    }

    @Test
    void validateInsert_historialEstadoDeExpedienteDeOtroCentro_devuelveMensajeNoExiste() {
        Sms sms = smsValido();
        sms.setCentro(centroA);
        sms.setHistorialEstado(historialEstadoDeCentro(5L, centroB));
        stubIsAdmin(true);

        assertEquals(List.of(MSG_HISTORIAL_NO_EXISTE), mensajes(service.validateInsert(sms)));
    }

    @Test
    void validateInsert_historialEstadoNoIndicado_noSeEvaluaYEsValido() {
        Sms sms = smsValido();
        sms.setHistorialEstado(null);
        stubIsAdmin(true);

        Optional<BusinessMessages> resultado = assertDoesNotThrow(() -> service.validateInsert(sms));

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateInsert_historialEstadoIndicadoYCentroNulo_devuelveSoloElMensajeDelCentro() {
        Sms sms = smsValido();
        sms.setCentro(null);
        sms.setHistorialEstado(historialEstadoDeCentro(5L, centroA));
        stubIsAdmin(true);

        Optional<BusinessMessages> resultado = assertDoesNotThrow(() -> service.validateInsert(sms));

        List<String> textos = mensajes(resultado);
        assertEquals(List.of(MSG_CENTRO_OBLIGATORIO), textos);
        assertFalse(textos.contains(MSG_HISTORIAL_NO_EXISTE));
    }

    /* ------------------------------------------------------------------ */
    /* insert                                                             */
    /* ------------------------------------------------------------------ */

    @Test
    void insert_smsValido_asignaLosValoresInicialesCanonicalizaElTelefonoYPersiste() {
        Sms sms = smsValido();
        stubIsAdmin(true);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Sms resultado = service.insert(sms);

        assertEquals(EstadoSms.PENDIENTE, resultado.getEstado());
        assertNotNull(resultado.getFechaCreacion());
        assertEquals(0, resultado.getNumeroReintentos());
        assertNull(resultado.getFechaPrimerIntentoEnvio());
        assertNull(resultado.getFechaUltimoIntentoEnvio());
        assertNull(resultado.getFechaEnvio());
        assertNull(resultado.getDescripcionUltimoFallo());
        assertEquals("+34600111222", resultado.getTelefono());
        verify(repository).save(sms);
        verify(ejecutorAsincrono).ejecutarTrasCommit(any());
    }

    @Test
    void insert_telefonoYaEnE164_loDejaIgual() {
        Sms sms = smsValido();
        sms.setTelefono("+34600111222");
        stubIsAdmin(true);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals("+34600111222", service.insert(sms).getTelefono());
    }

    @Test
    void insert_telefonoConEspacios_loGuardaEnE164() {
        Sms sms = smsValido();
        sms.setTelefono("600 11 12 22");
        stubIsAdmin(true);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals("+34600111222", service.insert(sms).getTelefono());
    }

    @Test
    void insert_clienteEnviaCamposDeServidor_seSobrescribenIncondicionalmente() {
        Sms sms = smsValido();
        sms.setEstado(EstadoSms.ENVIADO);
        sms.setFechaEnvio(LocalDateTime.of(2000, 1, 1, 0, 0));
        sms.setFechaPrimerIntentoEnvio(LocalDateTime.of(2000, 1, 1, 0, 0));
        sms.setFechaUltimoIntentoEnvio(LocalDateTime.of(2000, 1, 2, 0, 0));
        sms.setNumeroReintentos(99);
        sms.setDescripcionUltimoFallo("inventado");
        stubIsAdmin(true);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Sms resultado = service.insert(sms);

        assertEquals(EstadoSms.PENDIENTE, resultado.getEstado());
        assertEquals(0, resultado.getNumeroReintentos());
        assertNull(resultado.getFechaEnvio());
        assertNull(resultado.getFechaPrimerIntentoEnvio());
        assertNull(resultado.getFechaUltimoIntentoEnvio());
        assertNull(resultado.getDescripcionUltimoFallo());
    }

    @Test
    void insert_smsInvalido_lanzaValidationExceptionYNoPersisteNiProgramaEnvio() {
        Sms sms = smsValido();
        sms.setDniDestinatario(null);
        stubIsAdmin(true);

        ValidationException ex = assertThrows(ValidationException.class, () -> service.insert(sms));

        assertEquals(MSG_DNI_OBLIGATORIO, ex.getMessage());
        verify(repository, never()).save(any());
        verify(ejecutorAsincrono, never()).ejecutarTrasCommit(any());
    }

    @Test
    void insert_smsValido_programaLaTareaConElIdGuardadoYNoConLaEntidad() {
        Sms sms = smsValido();
        stubIsAdmin(true);
        Sms guardado = smsValido();
        guardado.setId(SMS_ID);
        when(repository.save(any())).thenReturn(guardado);
        when(repository.find(SMS_ID)).thenReturn(guardado);
        when(smsSenderProvider.get()).thenReturn(smsSender);

        service.insert(sms);
        ejecutarTareaProgramada();

        verify(repository).find(SMS_ID);
    }

    /* ------------------------------------------------------------------ */
    /* update / remove                                                    */
    /* ------------------------------------------------------------------ */

    @Test
    void update_siempre_lanzaUnsupportedOperationException() {
        Sms nuevo = smsValido();
        Sms original = smsValido();

        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                () -> service.update(nuevo, original));

        assertEquals(MSG_INMUTABLE, ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void remove_siempre_lanzaUnsupportedOperationException() {
        Sms sms = smsValido();

        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                () -> service.remove(sms));

        assertEquals(MSG_NO_BORRABLE, ex.getMessage());
        verify(repository, never()).remove(any());
    }

    /* ------------------------------------------------------------------ */
    /* validateUpdate / validateRemove                                    */
    /* ------------------------------------------------------------------ */

    @Test
    void validateUpdate_siempre_devuelveElMensajeDeInmutabilidad() {
        assertEquals(List.of(MSG_INMUTABLE), mensajes(service.validateUpdate(smsValido(), smsValido())));
    }

    @Test
    void validateRemove_siempre_devuelveElMensajeDeNoBorrado() {
        assertEquals(List.of(MSG_NO_BORRABLE), mensajes(service.validateRemove(smsValido())));
    }

    /* ------------------------------------------------------------------ */
    /* validateReenviar                                                   */
    /* ------------------------------------------------------------------ */

    @Test
    void validateReenviar_smsFallidoYAdministrador_devuelveOptionalVacio() {
        stubIsAdmin(true);

        assertTrue(service.validateReenviar(entidadConSoloId(), smsFallido(centroA)).isEmpty());
    }

    @Test
    void validateReenviar_smsPendiente_devuelveMensajeSoloFallidos() {
        Sms original = smsFallido(centroA);
        original.setEstado(EstadoSms.PENDIENTE);
        stubIsAdmin(true);

        assertEquals(List.of(MSG_SOLO_FALLIDOS), mensajes(service.validateReenviar(entidadConSoloId(), original)));
    }

    @Test
    void validateReenviar_smsYaEnviado_devuelveMensajeSoloFallidos() {
        Sms original = smsFallido(centroA);
        original.setEstado(EstadoSms.ENVIADO);
        stubIsAdmin(true);

        assertEquals(MSG_SOLO_FALLIDOS, mensaje(service.validateReenviar(entidadConSoloId(), original)));
    }

    @Test
    void validateReenviar_entidadOriginalNula_devuelveMensajeSoloFallidosSinLanzar() {
        Optional<BusinessMessages> resultado = assertDoesNotThrow(() -> service.validateReenviar(new Sms(), null));

        assertEquals(List.of(MSG_SOLO_FALLIDOS), mensajes(resultado));
    }

    @Test
    void validateReenviar_usuarioSupervisorDelCentro_devuelveOptionalVacio() {
        stubIsAdmin(false);
        stubUsuario(usuarioConTipoEnCentro(centroA, TipoUsuarioCodigo.SUPERVISOR));

        assertTrue(service.validateReenviar(entidadConSoloId(), smsFallido(centroA)).isEmpty());
    }

    @Test
    void validateReenviar_usuarioAdministrativoDelCentro_devuelveOptionalVacio() {
        stubIsAdmin(false);
        stubUsuario(usuarioConTipoEnCentro(centroA, TipoUsuarioCodigo.ADMINISTRATIVO));

        assertTrue(service.validateReenviar(entidadConSoloId(), smsFallido(centroA)).isEmpty());
    }

    @Test
    void validateReenviar_usuarioDelCentroSinCargoDeGestion_devuelveMensajeCentroNoSuyo() {
        stubIsAdmin(false);
        stubUsuario(usuarioConTipoEnCentro(centroA, TipoUsuarioCodigo.ALUMNO));

        assertEquals(List.of(MSG_REENVIO_CENTRO_NO_SUYO),
                mensajes(service.validateReenviar(entidadConSoloId(), smsFallido(centroA))));
    }

    @Test
    void validateReenviar_usuarioQueNoPerteneceAlCentro_devuelveMensajeCentroNoSuyo() {
        stubIsAdmin(false);
        stubUsuario(usuarioConTipoEnCentro(centroA, TipoUsuarioCodigo.SUPERVISOR));

        assertEquals(MSG_REENVIO_CENTRO_NO_SUYO,
                mensaje(service.validateReenviar(entidadConSoloId(), smsFallido(centroB))));
    }

    @Test
    void validateReenviar_administradorDeCentroAjeno_devuelveOptionalVacio() {
        stubIsAdmin(true);

        assertTrue(service.validateReenviar(entidadConSoloId(), smsFallido(centroB)).isEmpty());
    }

    @Test
    void validateReenviar_smsPendienteYUsuarioSinGestion_devuelveLosDosMensajes() {
        Sms original = smsFallido(centroB);
        original.setEstado(EstadoSms.PENDIENTE);
        stubIsAdmin(false);
        stubUsuario(usuarioConTipoEnCentro(centroA, TipoUsuarioCodigo.SUPERVISOR));

        assertEquals(List.of(MSG_SOLO_FALLIDOS, MSG_REENVIO_CENTRO_NO_SUYO),
                mensajes(service.validateReenviar(entidadConSoloId(), original)));
    }

    /* ------------------------------------------------------------------ */
    /* reenviar                                                           */
    /* ------------------------------------------------------------------ */

    @Test
    void reenviar_smsFallido_programaElEnvioSinPersistirCambios() {
        Sms original = smsFallido(centroA);
        stubIsAdmin(true);

        Sms resultado = service.reenviar(entidadConSoloId(), original);

        assertSame(original, resultado);
        verify(repository, never()).save(any());
        verify(ejecutorAsincrono).ejecutarTrasCommit(any());
    }

    @Test
    void reenviar_smsFallido_programaLaTareaDelMismoIdQueElOriginal() {
        Sms original = smsFallido(centroA);
        stubIsAdmin(true);
        when(repository.find(SMS_ID)).thenReturn(original);
        when(smsSenderProvider.get()).thenReturn(smsSender);

        service.reenviar(entidadConSoloId(), original);
        ejecutarTareaProgramada();

        verify(repository).find(SMS_ID);
    }

    @Test
    void reenviar_smsNoFallido_lanzaValidationExceptionYNoProgramaEnvio() {
        Sms original = smsFallido(centroA);
        original.setEstado(EstadoSms.PENDIENTE);
        stubIsAdmin(true);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.reenviar(entidadConSoloId(), original));

        assertEquals(MSG_SOLO_FALLIDOS, ex.getMessage());
        verify(ejecutorAsincrono, never()).ejecutarTrasCommit(any());
    }

    @Test
    void reenviar_usuarioSinGestionDelCentro_lanzaValidationExceptionYNoProgramaEnvio() {
        Sms original = smsFallido(centroA);
        stubIsAdmin(false);
        stubUsuario(usuarioConTipoEnCentro(centroA, TipoUsuarioCodigo.ALUMNO));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.reenviar(entidadConSoloId(), original));

        assertEquals(MSG_REENVIO_CENTRO_NO_SUYO, ex.getMessage());
        verify(ejecutorAsincrono, never()).ejecutarTrasCommit(any());
    }

    @Test
    void reenviar_entidadOriginalNula_lanzaValidationExceptionYNoProgramaEnvio() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.reenviar(new Sms(), null));

        assertEquals(MSG_SOLO_FALLIDOS, ex.getMessage());
        verify(ejecutorAsincrono, never()).ejecutarTrasCommit(any());
    }

    /* ------------------------------------------------------------------ */
    /* enviarSms (a través de la tarea programada)                        */
    /* ------------------------------------------------------------------ */

    @Test
    void enviarSms_proveedorAceptaElEnvio_marcaEnviadoConFechaDeEnvioYSinDescripcionDeFallo() {
        Sms enBd = smsEnBd(EstadoSms.PENDIENTE);
        when(smsSenderProvider.get()).thenReturn(smsSender);
        programarEnvioDe(enBd);

        ejecutarTareaProgramada();

        assertEquals(EstadoSms.ENVIADO, enBd.getEstado());
        assertNotNull(enBd.getFechaEnvio());
        assertNull(enBd.getDescripcionUltimoFallo());
        assertNotNull(enBd.getFechaPrimerIntentoEnvio());
        assertNotNull(enBd.getFechaUltimoIntentoEnvio());
        assertEquals(1, enBd.getNumeroReintentos());
        verify(repository, times(1)).save(enBd);
    }

    @Test
    void enviarSms_envio_construyeElRecordDeTransporteConElTelefonoEnE164YElMensaje() {
        Sms enBd = smsEnBd(EstadoSms.PENDIENTE);
        when(smsSenderProvider.get()).thenReturn(smsSender);
        programarEnvioDe(enBd);

        ejecutarTareaProgramada();

        ArgumentCaptor<com.educaflow.base.infrastructure.sms.Sms> captor =
                ArgumentCaptor.forClass(com.educaflow.base.infrastructure.sms.Sms.class);
        verify(smsSender).send(captor.capture());
        assertEquals("+34600111222", captor.getValue().telefonoDestino());
        assertEquals("Mañana no hay clase", captor.getValue().mensaje());
    }

    @Test
    void enviarSms_proveedorLanzaExcepcion_marcaFallidoConLaTrazaYSinFechaDeEnvioSinPropagar() {
        Sms enBd = smsEnBd(EstadoSms.PENDIENTE);
        when(smsSenderProvider.get()).thenReturn(smsSender);
        doThrow(new RuntimeException("Twilio caído")).when(smsSender).send(any());
        programarEnvioDe(enBd);

        assertDoesNotThrow(this::ejecutarTareaProgramada);

        assertEquals(EstadoSms.FALLIDO, enBd.getEstado());
        assertNull(enBd.getFechaEnvio());
        assertTrue(enBd.getDescripcionUltimoFallo().contains("Twilio caído"));
        assertTrue(enBd.getDescripcionUltimoFallo().contains("\tat "));
        verify(repository).save(enBd);
    }

    @Test
    void enviarSms_credencialesAusentes_marcaFallidoConLaTraza() {
        Sms enBd = smsEnBd(EstadoSms.PENDIENTE);
        when(smsSenderProvider.get()).thenThrow(new IllegalArgumentException("accountSid no puede ser null ni blank"));
        programarEnvioDe(enBd);

        assertDoesNotThrow(this::ejecutarTareaProgramada);

        assertEquals(EstadoSms.FALLIDO, enBd.getEstado());
        assertTrue(enBd.getDescripcionUltimoFallo().contains("accountSid"));
        assertNull(enBd.getFechaEnvio());
        verify(repository).save(enBd);
    }

    @Test
    void enviarSms_envioCorrectoTrasUnFallo_borraLaDescripcionDeFalloPrevia() {
        Sms enBd = smsEnBd(EstadoSms.FALLIDO);
        enBd.setDescripcionUltimoFallo("fallo anterior");
        enBd.setNumeroReintentos(1);
        when(smsSenderProvider.get()).thenReturn(smsSender);
        programarEnvioDe(enBd);

        ejecutarTareaProgramada();

        assertEquals(EstadoSms.ENVIADO, enBd.getEstado());
        assertNull(enBd.getDescripcionUltimoFallo());
        assertNotNull(enBd.getFechaEnvio());
    }

    @Test
    void enviarSms_falloConFechaDeEnvioPrevia_ponLaFechaDeEnvioANull() {
        Sms enBd = smsEnBd(EstadoSms.FALLIDO);
        enBd.setFechaEnvio(LocalDateTime.now(ZoneId.systemDefault()).minusDays(1));
        when(smsSenderProvider.get()).thenReturn(smsSender);
        doThrow(new RuntimeException("Twilio caído")).when(smsSender).send(any());
        programarEnvioDe(enBd);

        ejecutarTareaProgramada();

        assertEquals(EstadoSms.FALLIDO, enBd.getEstado());
        assertNull(enBd.getFechaEnvio());
    }

    @Test
    void enviarSms_smsYaEnviado_noLlamaAlProveedorNiPersiste() {
        Sms enBd = smsEnBd(EstadoSms.ENVIADO);
        Mockito.lenient().when(smsSenderProvider.get()).thenReturn(smsSender);
        programarEnvioDe(enBd);

        ejecutarTareaProgramada();

        verify(smsSender, never()).send(any());
        verify(repository, never()).save(any());
    }

    @Test
    void enviarSms_smsInexistente_lanzaIllegalStateExceptionConElId() {
        Mockito.lenient().when(smsSenderProvider.get()).thenReturn(smsSender);
        programarEnvioDe(null);

        IllegalStateException ex = assertThrows(IllegalStateException.class, this::ejecutarTareaProgramada);

        assertEquals("No existe el SMS 100", ex.getMessage());
        verify(smsSender, never()).send(any());
        verify(repository, never()).save(any());
    }

    @Test
    void enviarSms_primerIntento_fijaLaFechaDelPrimerIntento() {
        Sms enBd = smsEnBd(EstadoSms.PENDIENTE);
        when(smsSenderProvider.get()).thenReturn(smsSender);
        programarEnvioDe(enBd);

        ejecutarTareaProgramada();

        assertNotNull(enBd.getFechaPrimerIntentoEnvio());
        assertNotNull(enBd.getFechaUltimoIntentoEnvio());
        assertEquals(1, enBd.getNumeroReintentos());
    }

    @Test
    void enviarSms_reintento_noSobrescribeLaFechaDelPrimerIntentoYSumaUnReintento() {
        LocalDateTime haceDosDias = LocalDateTime.now(ZoneId.systemDefault()).minusDays(2);
        LocalDateTime ayer = LocalDateTime.now(ZoneId.systemDefault()).minusDays(1);
        Sms enBd = smsEnBd(EstadoSms.FALLIDO);
        enBd.setFechaPrimerIntentoEnvio(haceDosDias);
        enBd.setFechaUltimoIntentoEnvio(ayer);
        enBd.setNumeroReintentos(1);
        when(smsSenderProvider.get()).thenReturn(smsSender);
        programarEnvioDe(enBd);

        ejecutarTareaProgramada();

        assertEquals(haceDosDias, enBd.getFechaPrimerIntentoEnvio());
        assertTrue(enBd.getFechaUltimoIntentoEnvio().isAfter(ayer));
        assertEquals(2, enBd.getNumeroReintentos());
    }

    @Test
    void enviarSms_intentoQueFalla_cuentaElIntentoAntesDeLlamarAlProveedor() {
        Sms enBd = smsEnBd(EstadoSms.PENDIENTE);
        when(smsSenderProvider.get()).thenReturn(smsSender);
        doThrow(new RuntimeException("Twilio caído")).when(smsSender).send(any());
        programarEnvioDe(enBd);

        ejecutarTareaProgramada();

        assertEquals(1, enBd.getNumeroReintentos());
        assertNotNull(enBd.getFechaUltimoIntentoEnvio());
    }

    @Test
    void enviarSms_tarea_seEjecutaEnSuPropiaTransaccion() {
        Sms enBd = smsEnBd(EstadoSms.PENDIENTE);
        when(smsSenderProvider.get()).thenReturn(smsSender);
        programarEnvioDe(enBd);
        Runnable tarea = tareaProgramada();

        try (MockedStatic<JPA> jpaMock = mockJpaRunInTransaction()) {
            tarea.run();

            jpaMock.verify(() -> JPA.runInTransaction(any(Runnable.class)));
        }
    }

    /* ------------------------------------------------------------------ */
    /* allowProperties*                                                   */
    /* ------------------------------------------------------------------ */

    @Test
    void allowPropertiesInsert_permiteLosSieteCamposClienteYDeniegaLosDeServidor() {
        AllowProperties resultado = service.allowPropertiesInsert();

        CAMPOS_CLIENTE.forEach(campo -> assertTrue(resultado.allowProperty(campo), campo));
        CAMPOS_SERVIDOR.forEach(campo -> assertFalse(resultado.allowProperty(campo), campo));
        assertFalse(resultado.allowProperty("nombreExpediente"));
    }

    @Test
    void allowPropertiesReenviar_devuelveWhitelistVacia() {
        AllowProperties resultado = service.allowPropertiesReenviar();

        assertFalse(resultado.allowProperty("estado"));
        assertFalse(resultado.allowProperty("centro"));
        assertFalse(resultado.allowProperty("telefono"));
        assertFalse(resultado.allowProperty("mensaje"));
    }

    @Test
    void allowPropertiesUpdate_devuelveWhitelistVacia() {
        AllowProperties resultado = service.allowPropertiesUpdate();

        CAMPOS_CLIENTE.forEach(campo -> assertFalse(resultado.allowProperty(campo), campo));
        CAMPOS_SERVIDOR.forEach(campo -> assertFalse(resultado.allowProperty(campo), campo));
    }

    @Test
    void allowPropertiesRemove_devuelveWhitelistVacia() {
        AllowProperties resultado = service.allowPropertiesRemove();

        CAMPOS_CLIENTE.forEach(campo -> assertFalse(resultado.allowProperty(campo), campo));
        CAMPOS_SERVIDOR.forEach(campo -> assertFalse(resultado.allowProperty(campo), campo));
    }
}
