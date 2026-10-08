package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1;

import com.axelor.auth.db.User;
import com.axelor.meta.db.MetaFile;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.i18n.I18n;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.common.db.Persona;
import com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1;
import com.educaflow.subsystem.expedientes.db.ResultadoVerificacionAnulacionMatriculaCicloFormativoV1;
import com.educaflow.tramites.util.entrada.CamposEntrada;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class AnulacionMatriculaCicloFormativoV1UtilTest {

    private static final String MENSAJE_MODIFICAR = "Solo puede modificar sus propias solicitudes";
    private static final String MENSAJE_BORRAR = "Solo puede borrar sus propias solicitudes";
    private static final String MENSAJE_REVISAR = "Solo puede revisar solicitudes de su propio centro";
    private static final String MENSAJE_FIRMAR = "Solo puede firmar resoluciones de su propio centro";
    private static final String MENSAJE_DEVOLVER = "Solo puede devolver resoluciones de su propio centro";

    private static final long ID_USUARIO_IRRELEVANTE = 1L;

    private static final String MOTIVO_RECHAZO = "Fuera de plazo";

    private MockedStatic<I18n> i18nMock;
    private MockedStatic<SecurityUtil> securityUtilMock;

    @BeforeEach
    void setUp() {
        // lenient: I18n solo se consume en las ramas que lanzan, así que los happy paths no lo llaman.
        i18nMock = Mockito.mockStatic(I18n.class,
                Mockito.withSettings().strictness(Strictness.LENIENT));
        i18nMock.when(() -> I18n.get(any(String.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        securityUtilMock = Mockito.mockStatic(SecurityUtil.class);
    }

    @AfterEach
    void tearDown() {
        // Defensivo: si setUp falla a mitad, @AfterEach se ejecuta igual y un close() sobre un nulo taparía
        // la causa real del fallo con una NullPointerException.
        // El try/finally garantiza que el segundo mock se cierra aunque el close() del primero lance:
        // un MockedStatic que quedara registrado contaminaría las clases de test posteriores.
        try {
            cerrarSiNoEsNulo(securityUtilMock);
        } finally {
            cerrarSiNoEsNulo(i18nMock);
        }
    }

    /* ------------------------------------------------------------------ */
    /* Helpers                                                            */
    /* ------------------------------------------------------------------ */

    private static void cerrarSiNoEsNulo(MockedStatic<?> mockedStatic) {
        if (mockedStatic != null) {
            mockedStatic.close();
        }
    }

    private static User usuario(long id) {
        User usuario = new User();
        usuario.setId(id);
        return usuario;
    }

    private static Centro centro(long id) {
        Centro centro = new Centro();
        centro.setId(id);
        return centro;
    }

    private static User usuarioDeLosCentros(long id, Centro... centros) {
        User usuario = usuario(id);
        List<CentroUsuario> centroUsuarios = new ArrayList<>();
        for (Centro centro : centros) {
            CentroUsuario centroUsuario = new CentroUsuario();
            centroUsuario.setCentro(centro);
            centroUsuario.setUsuario(usuario);
            centroUsuarios.add(centroUsuario);
        }
        usuario.setCentroUsuarios(centroUsuarios);
        return usuario;
    }

    private static AnulacionMatriculaCicloFormativoV1 expedienteDe(User usuarioRegistrador) {
        AnulacionMatriculaCicloFormativoV1 expediente = new AnulacionMatriculaCicloFormativoV1();
        expediente.setUsuarioRegistrador(usuarioRegistrador);
        return expediente;
    }

    private static AnulacionMatriculaCicloFormativoV1 expedienteEn(Centro centro) {
        AnulacionMatriculaCicloFormativoV1 expediente = new AnulacionMatriculaCicloFormativoV1();
        expediente.setCentro(centro);
        return expediente;
    }

    private static Persona personaConDni(String dni) {
        Persona persona = new Persona();
        persona.setDni(dni);
        return persona;
    }

    private static AnulacionMatriculaCicloFormativoV1 expedienteEnRepresentacion(Persona solicitante, Persona interesada) {
        AnulacionMatriculaCicloFormativoV1 expediente = new AnulacionMatriculaCicloFormativoV1();
        expediente.setPresentadoEnRepresentacion(true);
        expediente.setPersonaSolicitante(solicitante);
        expediente.setPersonaInteresada(interesada);
        return expediente;
    }

    private void usuarioAutenticado(User usuario) {
        securityUtilMock.when(SecurityUtil::getUser).thenReturn(usuario);
    }

    private static String mensajeDe(BusinessException excepcion) {
        // Una sola lectura: getBusinessMessages() devuelve una lista nueva en cada llamada,
        // así que el aserto de cardinalidad debe caer sobre la misma instancia que se lee.
        BusinessMessages mensajes = excepcion.getBusinessMessages();
        assertEquals(1, mensajes.size());
        return mensajes.get(0).getMessage();
    }

    /* ------------------------------------------------------------------ */
    /* exigeSerElCreador                                                  */
    /* ------------------------------------------------------------------ */

    @Test
    void exigeSerElCreador_usuarioAutenticadoEsElRegistrador_noLanza() {
        AnulacionMatriculaCicloFormativoV1 expediente = expedienteDe(usuario(7L));
        // Dos instancias de User distintas a propósito: la guarda mira el id, no la referencia al User.
        // No prueba que el id se compare por valor: con ids en el rango de caché de Long (contrato del
        // diseño) el autoboxing reutiliza el mismo Long, así que un == sobre los ids pasaría igual.
        usuarioAutenticado(usuario(7L));

        assertDoesNotThrow(() -> AnulacionMatriculaCicloFormativoV1Util.exigeSerElCreador(expediente, MENSAJE_MODIFICAR));
    }

    @Test
    void exigeSerElCreador_usuarioAutenticadoDistintoDelRegistrador_lanzaConElMensajeRecibido() {
        AnulacionMatriculaCicloFormativoV1 expediente = expedienteDe(usuario(7L));
        usuarioAutenticado(usuario(8L));

        BusinessException excepcion = assertThrows(BusinessException.class,
                () -> AnulacionMatriculaCicloFormativoV1Util.exigeSerElCreador(expediente, MENSAJE_MODIFICAR));

        assertEquals(MENSAJE_MODIFICAR, mensajeDe(excepcion));
    }

    @Test
    void exigeSerElCreador_cadaLlamanteRecibeSuPropioMensaje() {
        AnulacionMatriculaCicloFormativoV1 expediente = expedienteDe(usuario(7L));
        usuarioAutenticado(usuario(8L));

        // Dos llamantes distintos de la fase SOLICITUD (CONTINUAR y DELETE): el texto sale del
        // parámetro, no de un literal interno de la clase de utilidad.
        assertAll(
                () -> assertEquals(MENSAJE_MODIFICAR, mensajeDe(assertThrows(BusinessException.class,
                        () -> AnulacionMatriculaCicloFormativoV1Util.exigeSerElCreador(expediente, MENSAJE_MODIFICAR)))),
                () -> assertEquals(MENSAJE_BORRAR, mensajeDe(assertThrows(BusinessException.class,
                        () -> AnulacionMatriculaCicloFormativoV1Util.exigeSerElCreador(expediente, MENSAJE_BORRAR)))));
    }

    @Test
    void exigeSerElCreador_expedienteSinUsuarioRegistrador_lanzaIllegalState() {
        // El registrador lo fija el Tramitador al dar de alta: que falte no es un error del usuario.
        AnulacionMatriculaCicloFormativoV1 expediente = expedienteDe(null);

        assertThrows(IllegalStateException.class,
                () -> AnulacionMatriculaCicloFormativoV1Util.exigeSerElCreador(expediente, MENSAJE_MODIFICAR));
    }

    @Test
    void exigeSerElCreador_sinUsuarioAutenticado_lanzaNullPointer() {
        AnulacionMatriculaCicloFormativoV1 expediente = expedienteDe(usuario(7L));
        usuarioAutenticado(null);

        assertThrows(NullPointerException.class,
                () -> AnulacionMatriculaCicloFormativoV1Util.exigeSerElCreador(expediente, MENSAJE_MODIFICAR));
    }

    /* ------------------------------------------------------------------ */
    /* exigePertenecerAlCentroDelExpediente                               */
    /* ------------------------------------------------------------------ */

    @Test
    void exigePertenecerAlCentro_usuarioDelCentroDelExpediente_noLanza() {
        AnulacionMatriculaCicloFormativoV1 expediente = expedienteEn(centro(3L));
        // Dos instancias de Centro distintas a propósito: la guarda mira el id, no la referencia al Centro.
        usuarioAutenticado(usuarioDeLosCentros(ID_USUARIO_IRRELEVANTE, centro(3L)));

        assertDoesNotThrow(
                () -> AnulacionMatriculaCicloFormativoV1Util.exigePertenecerAlCentroDelExpediente(expediente, MENSAJE_REVISAR));
    }

    @Test
    void exigePertenecerAlCentro_usuarioDeVariosCentrosEntreEllosElDelExpediente_noLanza() {
        AnulacionMatriculaCicloFormativoV1 expediente = expedienteEn(centro(3L));
        usuarioAutenticado(usuarioDeLosCentros(ID_USUARIO_IRRELEVANTE, centro(4L), centro(3L)));

        assertDoesNotThrow(
                () -> AnulacionMatriculaCicloFormativoV1Util.exigePertenecerAlCentroDelExpediente(expediente, MENSAJE_FIRMAR));
    }

    @Test
    void exigePertenecerAlCentro_usuarioDeOtroCentro_lanzaConElMensajeRecibido() {
        AnulacionMatriculaCicloFormativoV1 expediente = expedienteEn(centro(3L));
        usuarioAutenticado(usuarioDeLosCentros(ID_USUARIO_IRRELEVANTE, centro(4L)));

        BusinessException excepcion = assertThrows(BusinessException.class,
                () -> AnulacionMatriculaCicloFormativoV1Util.exigePertenecerAlCentroDelExpediente(expediente, MENSAJE_REVISAR));

        assertEquals(MENSAJE_REVISAR, mensajeDe(excepcion));
    }

    @Test
    void exigePertenecerAlCentro_expedienteSinCentro_lanzaIllegalState() {
        // El centro lo fija el Tramitador al dar de alta: que falte no es un error del usuario.
        AnulacionMatriculaCicloFormativoV1 expediente = expedienteEn(null);

        assertThrows(IllegalStateException.class,
                () -> AnulacionMatriculaCicloFormativoV1Util.exigePertenecerAlCentroDelExpediente(expediente, MENSAJE_FIRMAR));
    }

    @Test
    void exigePertenecerAlCentro_usuarioSinCentros_lanza() {
        AnulacionMatriculaCicloFormativoV1 expediente = expedienteEn(centro(3L));
        usuarioAutenticado(usuarioDeLosCentros(ID_USUARIO_IRRELEVANTE));

        BusinessException excepcion = assertThrows(BusinessException.class,
                () -> AnulacionMatriculaCicloFormativoV1Util.exigePertenecerAlCentroDelExpediente(expediente, MENSAJE_DEVOLVER));

        assertEquals(MENSAJE_DEVOLVER, mensajeDe(excepcion));
    }

    @Test
    void exigePertenecerAlCentro_sinUsuarioAutenticado_lanzaNullPointer() {
        AnulacionMatriculaCicloFormativoV1 expediente = expedienteEn(centro(3L));
        usuarioAutenticado(null);

        assertThrows(NullPointerException.class,
                () -> AnulacionMatriculaCicloFormativoV1Util.exigePertenecerAlCentroDelExpediente(expediente, MENSAJE_FIRMAR));
    }

    /* ------------------------------------------------------------------ */
    /* esPresentadoEnRepresentacion                                       */
    /* ------------------------------------------------------------------ */

    @Test
    void esPresentadoEnRepresentacion_expedienteEnRepresentacion_true() {
        AnulacionMatriculaCicloFormativoV1 expediente = new AnulacionMatriculaCicloFormativoV1();
        expediente.setPresentadoEnRepresentacion(true);

        assertTrue(AnulacionMatriculaCicloFormativoV1Util.esPresentadoEnRepresentacion(expediente));
    }

    @Test
    void esPresentadoEnRepresentacion_paraElPropioSolicitanteOSinInformar_false() {
        AnulacionMatriculaCicloFormativoV1 paraSiMismo = new AnulacionMatriculaCicloFormativoV1();
        paraSiMismo.setPresentadoEnRepresentacion(false);
        AnulacionMatriculaCicloFormativoV1 sinInformar = new AnulacionMatriculaCicloFormativoV1();
        sinInformar.setPresentadoEnRepresentacion(null);

        assertAll(
                () -> assertFalse(AnulacionMatriculaCicloFormativoV1Util.esPresentadoEnRepresentacion(paraSiMismo)),
                () -> assertFalse(AnulacionMatriculaCicloFormativoV1Util.esPresentadoEnRepresentacion(sinInformar)));
    }

    /* ------------------------------------------------------------------ */
    /* interesadoDistintoDelSolicitante                                   */
    /* ------------------------------------------------------------------ */

    @Test
    void interesadoDistintoDelSolicitante_dnisDistintos_true() {
        AnulacionMatriculaCicloFormativoV1 expediente =
                expedienteEnRepresentacion(personaConDni("12345678Z"), personaConDni("87654321X"));

        assertTrue(AnulacionMatriculaCicloFormativoV1Util.interesadoDistintoDelSolicitante(expediente));
    }

    @Test
    void interesadoDistintoDelSolicitante_mismoDni_false() {
        AnulacionMatriculaCicloFormativoV1 expediente =
                expedienteEnRepresentacion(personaConDni("12345678Z"), personaConDni("12345678Z"));

        assertFalse(AnulacionMatriculaCicloFormativoV1Util.interesadoDistintoDelSolicitante(expediente));
    }

    @Test
    void interesadoDistintoDelSolicitante_mismoDniEscritoDistinto_false() {
        // El DNI es un campo tecleado: minúsculas, espacios y el cero inicial no lo convierten en otra persona.
        AnulacionMatriculaCicloFormativoV1 expediente =
                expedienteEnRepresentacion(personaConDni("12345678Z"), personaConDni(" 012345678z "));

        assertFalse(AnulacionMatriculaCicloFormativoV1Util.interesadoDistintoDelSolicitante(expediente));
    }

    @Test
    void interesadoDistintoDelSolicitante_algunDniEnBlanco_true() {
        // La obligatoriedad del DNI la exigen otras reglas: esta solo compara cuando hay dos DNI.
        AnulacionMatriculaCicloFormativoV1 sinDniInteresada =
                expedienteEnRepresentacion(personaConDni("12345678Z"), personaConDni(null));
        AnulacionMatriculaCicloFormativoV1 sinDniSolicitante =
                expedienteEnRepresentacion(personaConDni("  "), personaConDni("12345678Z"));

        assertAll(
                () -> assertTrue(AnulacionMatriculaCicloFormativoV1Util.interesadoDistintoDelSolicitante(sinDniInteresada)),
                () -> assertTrue(AnulacionMatriculaCicloFormativoV1Util.interesadoDistintoDelSolicitante(sinDniSolicitante)));
    }

    @Test
    void interesadoDistintoDelSolicitante_sinAlgunaPersona_lanzaIllegalState() {
        // Las dos personas las crea el Tramitador al dar de alta: que falte una no es un error del usuario.
        AnulacionMatriculaCicloFormativoV1 sinSolicitante = expedienteEnRepresentacion(null, personaConDni("12345678Z"));
        AnulacionMatriculaCicloFormativoV1 sinInteresada = expedienteEnRepresentacion(personaConDni("12345678Z"), null);

        assertAll(
                () -> assertThrows(IllegalStateException.class,
                        () -> AnulacionMatriculaCicloFormativoV1Util.interesadoDistintoDelSolicitante(sinSolicitante)),
                () -> assertThrows(IllegalStateException.class,
                        () -> AnulacionMatriculaCicloFormativoV1Util.interesadoDistintoDelSolicitante(sinInteresada)));
    }

    /* ------------------------------------------------------------------ */
    /* borrarSubsanacion                                                  */
    /* ------------------------------------------------------------------ */

    @Test
    void borrarSubsanacion_expedienteConSubsanacionPedida_vaciaElResultadoYElTexto() {
        AnulacionMatriculaCicloFormativoV1 expediente = new AnulacionMatriculaCicloFormativoV1();
        expediente.setResultadoVerificacion(ResultadoVerificacionAnulacionMatriculaCicloFormativoV1.SUBSANAR);
        expediente.setTextoSubsanacion("Falta indicar el ciclo en el que está matriculado");
        expediente.setMotivoRechazo(MOTIVO_RECHAZO);

        AnulacionMatriculaCicloFormativoV1Util.borrarSubsanacion(expediente);

        assertAll(
                () -> assertNull(expediente.getResultadoVerificacion()),
                () -> assertNull(expediente.getTextoSubsanacion()),
                () -> assertEquals(MOTIVO_RECHAZO, expediente.getMotivoRechazo()));
    }

    @Test
    void camposEntrada_usaLosCamposDeLaSolicitudDeEsteTipo() {
        AnulacionMatriculaCicloFormativoV1 expediente = new AnulacionMatriculaCicloFormativoV1();
        MetaFile pdfSolicitud = new MetaFile();
        MetaFile pdfSolicitudFirmada = new MetaFile();
        MetaFile resguardo = new MetaFile();
        expediente.setPdfSolicitud(pdfSolicitud);
        expediente.setTextoSubsanacion("Falta indicar el ciclo en el que está matriculado");

        CamposEntrada<AnulacionMatriculaCicloFormativoV1> campos = AnulacionMatriculaCicloFormativoV1Util.CAMPOS_ENTRADA;
        campos.setPdfSolicitudFirmada().accept(expediente, pdfSolicitudFirmada);
        campos.setPdfJustificanteRegistroEntrada().accept(expediente, resguardo);
        campos.borrarSubsanacion().accept(expediente);

        assertAll(
                () -> assertSame(pdfSolicitud, campos.getPdfSolicitud().apply(expediente)),
                () -> assertSame(pdfSolicitudFirmada, campos.getPdfSolicitudFirmada().apply(expediente)),
                () -> assertSame(pdfSolicitudFirmada, expediente.getPdfSolicitudFirmada()),
                () -> assertSame(resguardo, expediente.getPdfJustificanteRegistroEntrada()),
                () -> assertNull(expediente.getTextoSubsanacion()));
    }

}
