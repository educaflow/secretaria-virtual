package com.educaflow.system.ventanilla.service.impl;

import com.axelor.auth.db.User;
import com.axelor.db.Repository;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.i18n.I18n;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.common.db.CentroUsuarioTipoUsuario;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.subsystem.expedientes.db.TipoExpediente;
import com.educaflow.subsystem.expedientes.db.TipoTramite;
import com.educaflow.subsystem.expedientes.db.Tramite;
import com.educaflow.subsystem.security.service.PerfilesUsuarioService;
import com.educaflow.subsystem.tramitador.service.TramitadorService;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.ContextoTramitacion;
import com.educaflow.system.ventanilla.db.AsistenteNuevoExpediente;
import com.educaflow.system.ventanilla.db.repo.VentanillaRepository;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsistenteNuevoExpedienteServiceImplTest {

    private static final String MENSAJE_CENTRO = "Debe indicar el centro";
    private static final String MENSAJE_TRAMITE = "Debe indicar el trámite";
    private static final String MENSAJE_COMO_SE_PRESENTA = "Debe indicar cómo se presenta el expediente";
    private static final String MENSAJE_PARA_QUIEN = "Debe indicar para quién es el expediente";
    private static final String MENSAJE_SIN_PERFIL_DE_INICIO = "No puede crear expedientes de este trámite en el centro indicado";
    private static final String MENSAJE_FORMA_NO_PERMITIDA = "No puede presentar el expediente de esa forma en el centro indicado";
    private static final String MENSAJE_SIN_REPRESENTACION = "Este trámite no permite presentar la solicitud en representación de otra persona";
    private static final String MENSAJE_NO_PARA_USTED_MISMO = "No puede crear este expediente para usted mismo en el centro indicado";
    private static final String MENSAJE_NO_EN_REPRESENTACION = "No puede crear este expediente en representación de otra persona en el centro indicado";

    private static final String NOMBRE_TRAMITE = "Anulación de matrícula";
    private static final String NOMBRE_TRAMITE_DERIVADO = "value:" + NOMBRE_TRAMITE;
    private static final String AYUDA_TRAMITE = "Texto de ayuda";

    private static final List<String> CAMPOS_DEL_MODELO = List.of(
            "centro", "tramite", "presentadoEnPapel", "presentadoEnRepresentacion",
            "nombreTramite", "ayudaTramite", "centrosDisponibles", "tramitesDisponibles",
            "hayQueElegirCentro", "hayQuePreguntarPresentacion", "hayQuePreguntarParaQuien");

    private static final List<String> CAMPOS_DERIVADOS_Y_DEL_SERVIDOR = List.of(
            "nombreTramite", "ayudaTramite", "centrosDisponibles", "tramitesDisponibles",
            "hayQueElegirCentro", "hayQuePreguntarPresentacion", "hayQuePreguntarParaQuien");

    private AsistenteNuevoExpedienteServiceImpl service;

    private TramitadorService tramitadorService;
    private PerfilesUsuarioService perfilesUsuarioService;
    private VentanillaRepository ventanillaRepository;

    private User usuario;
    private Centro centroA;
    private Centro centroB;
    private Centro centroC;
    private TipoTramite tipoTramiteAlumno;
    private TipoTramite tipoTramiteSinTipoUsuario;
    private Tramite tramite;
    private Tramite otroTramite;
    private Tramite tramiteNoEvaluable;

    private MockedStatic<I18n> i18nMock;
    private MockedStatic<SecurityUtil> securityUtilMock;

    @BeforeEach
    void setUp() throws Exception {
        tramitadorService = Mockito.mock(TramitadorService.class);
        perfilesUsuarioService = Mockito.mock(PerfilesUsuarioService.class);
        ventanillaRepository = Mockito.mock(VentanillaRepository.class);

        service = new AsistenteNuevoExpedienteServiceImpl(AsistenteNuevoExpediente.class, repositorioMock());
        setField(service, "tramitadorService", tramitadorService);
        setField(service, "perfilesUsuarioService", perfilesUsuarioService);
        setField(service, "ventanillaRepository", ventanillaRepository);

        usuario = new User();
        usuario.setId(7L);
        usuario.setCentroUsuarios(new ArrayList<>());

        centroA = centro(1L, "Centro A");
        centroB = centro(2L, "Centro B");
        centroC = centro(3L, "Centro C");

        tipoTramiteAlumno = new TipoTramite();
        tipoTramiteAlumno.setName("Matrícula");
        tipoTramiteAlumno.setTipoUsuario(tipoUsuario("ALUMNO"));

        tipoTramiteSinTipoUsuario = new TipoTramite();

        tramite = new Tramite();
        tramite.setId(10L);
        tramite.setName(NOMBRE_TRAMITE);
        tramite.setHelp(AYUDA_TRAMITE);
        tramite.setPermitidoPresentarEnRepresentacion(true);
        tramite.setTipoTramite(tipoTramiteAlumno);
        tramite.setDefaultTipoExpediente(new TipoExpediente());

        otroTramite = new Tramite();
        otroTramite.setId(11L);
        otroTramite.setName("Otro trámite");
        otroTramite.setTipoTramite(tipoTramiteAlumno);
        otroTramite.setDefaultTipoExpediente(new TipoExpediente());

        tramiteNoEvaluable = new Tramite();
        tramiteNoEvaluable.setId(99L);

        i18nMock = Mockito.mockStatic(I18n.class);
        i18nMock.when(() -> I18n.get(any(String.class))).thenAnswer(invocation -> invocation.getArgument(0));

        securityUtilMock = Mockito.mockStatic(SecurityUtil.class);
        securityUtilMock.when(SecurityUtil::getUser).thenReturn(usuario);
    }

    @AfterEach
    void tearDown() {
        cerrarSiNoEsNulo(securityUtilMock);
        cerrarSiNoEsNulo(i18nMock);
    }

    /* ------------------------------------------------------------------ */
    /* Helpers                                                            */
    /* ------------------------------------------------------------------ */

    @SuppressWarnings("unchecked")
    private static Repository<AsistenteNuevoExpediente> repositorioMock() {
        return Mockito.mock(Repository.class);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        setField(target, AsistenteNuevoExpedienteServiceImpl.class, fieldName, value);
    }

    private static void setField(Object target, Class<?> claseDeclarante, String fieldName, Object value) throws Exception {
        var field = claseDeclarante.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void cerrarSiNoEsNulo(MockedStatic<?> mockedStatic) {
        if (mockedStatic != null) {
            mockedStatic.close();
        }
    }

    private static Centro centro(Long id, String name) {
        var centro = new Centro();
        centro.setId(id);
        centro.setName(name);
        return centro;
    }

    private static TipoUsuario tipoUsuario(String codigo) {
        var tipoUsuario = new TipoUsuario();
        tipoUsuario.setCodigo(codigo);
        return tipoUsuario;
    }

    private void centroUsuario(Centro centro, String... codigosTipoUsuario) {
        var centroUsuario = new CentroUsuario();
        centroUsuario.setCentro(centro);
        centroUsuario.setUsuario(usuario);
        centroUsuario.setCentroUsuarioTipoUsuario(Arrays.stream(codigosTipoUsuario)
                .map(codigo -> {
                    var centroUsuarioTipoUsuario = new CentroUsuarioTipoUsuario();
                    centroUsuarioTipoUsuario.setCentroUsuario(centroUsuario);
                    centroUsuarioTipoUsuario.setTipoUsuario(tipoUsuario(codigo));
                    return centroUsuarioTipoUsuario;
                })
                .collect(Collectors.toCollection(ArrayList::new)));

        usuario.getCentroUsuarios().add(centroUsuario);
    }

    private void stubCatalogo(Tramite... tramitesEvaluables) {
        when(ventanillaRepository.findTramitesEvaluables())
                .thenReturn(new ArrayList<>(List.of(tramitesEvaluables)));
    }

    private void stubPerfilesDeInicio(Tramite tramite, Centro centro, Profile... perfiles) {
        when(perfilesUsuarioService.getPerfilesDeInicioSobreTramite(tramite, usuario, centro))
                .thenReturn(Set.of(perfiles));
    }

    private void stubOraculo(Function<ContextoTramitacion, Optional<BusinessMessages>> respuesta) {
        when(tramitadorService.validateTriggerInitialEvent(any(ContextoTramitacion.class)))
                .thenAnswer(invocation -> respuesta.apply(invocation.getArgument(0)));
    }

    private void stubOraculoAcepta() {
        stubOraculo(contexto -> Optional.empty());
    }

    private void stubOraculoRechazaSiempre(String mensaje) {
        stubOraculo(contexto -> mensajes(mensaje));
    }

    private void stubOraculoSoloAdmitePapel() {
        stubOraculo(contexto -> contexto.profile() == Profile.CREADOR
                ? mensajes(MENSAJE_FORMA_NO_PERMITIDA)
                : Optional.empty());
    }

    private void stubOraculoSoloAdmiteQueLoPresenteElUsuario() {
        stubOraculo(contexto -> contexto.profile() == Profile.TRAMITADOR
                ? mensajes(MENSAJE_FORMA_NO_PERMITIDA)
                : Optional.empty());
    }

    private void stubOraculoSoloAdmiteParaMi() {
        stubOraculo(contexto -> contexto.presentadoEnRepresentacion()
                ? mensajes(MENSAJE_SIN_REPRESENTACION)
                : Optional.empty());
    }

    private void stubOraculoNoAdmiteRepresentacionEnPapel() {
        stubOraculo(contexto -> contexto.profile() == Profile.TRAMITADOR && contexto.presentadoEnRepresentacion()
                ? mensajes(MENSAJE_SIN_REPRESENTACION)
                : Optional.empty());
    }

    private static Optional<BusinessMessages> mensajes(String texto) {
        return Optional.of(BusinessMessages.single(texto));
    }

    private static BusinessMessage unicoMensaje(Optional<BusinessMessages> resultado) {
        assertTrue(resultado.isPresent());
        assertEquals(1, resultado.get().size());
        return resultado.get().get(0);
    }

    private static void assertMensajeDeLaVentanilla(String mensajeEsperado, String campoEsperado,
                                                    Optional<BusinessMessages> resultado) {
        BusinessMessage mensaje = unicoMensaje(resultado);

        assertEquals(mensajeEsperado, mensaje.getMessage());
        assertEquals(campoEsperado, mensaje.getFieldName());
        assertEquals(tituloDelCampo(campoEsperado), mensaje.getLabel());
    }

    private static void assertMensajeDelMotor(String mensajeEsperado, Optional<BusinessMessages> resultado) {
        BusinessMessage mensaje = unicoMensaje(resultado);

        assertEquals(mensajeEsperado, mensaje.getMessage());
        assertNull(mensaje.getFieldName());
        assertNull(mensaje.getLabel());
    }

    private static String tituloDelCampo(String campo) {
        return Mapper.of(AsistenteNuevoExpediente.class).getProperty(campo).getTitle();
    }

    private AsistenteNuevoExpediente asistenteConCentro() {
        var asistente = new AsistenteNuevoExpediente();
        asistente.setCentro(centroA);
        return asistente;
    }

    private AsistenteNuevoExpediente asistenteConCentroYTramite(Tramite tramite) {
        var asistente = asistenteConCentro();
        asistente.setTramite(tramite);
        return asistente;
    }

    private AsistenteNuevoExpediente asistenteCompleto() {
        var asistente = asistenteConCentroYTramite(tramite);
        asistente.setPresentadoEnPapel(false);
        asistente.setPresentadoEnRepresentacion(false);
        return asistente;
    }

    private static ArgumentCaptor<ContextoTramitacion> capturaDeContextos() {
        return ArgumentCaptor.forClass(ContextoTramitacion.class);
    }

    /* ------------------------------------------------------------------ */
    /* prepararCentros                                                    */
    /* ------------------------------------------------------------------ */

    @Test
    void prepararCentros_variosCentrosConTramitesIniciables_losOfreceTodosYExigeElegir() {
        centroUsuario(centroA);
        centroUsuario(centroB);
        stubCatalogo(tramite);
        stubPerfilesDeInicio(tramite, centroA, Profile.CREADOR);
        stubPerfilesDeInicio(tramite, centroB, Profile.CREADOR);
        var asistente = new AsistenteNuevoExpediente();

        var resultado = service.prepararCentros(asistente);

        assertEquals(2, asistente.getCentrosDisponibles().size());
        assertTrue(asistente.getCentrosDisponibles().contains(centroA));
        assertTrue(asistente.getCentrosDisponibles().contains(centroB));
        assertTrue(asistente.getHayQueElegirCentro());
        assertNull(asistente.getCentro());
        assertSame(asistente, resultado);
    }

    @Test
    void prepararCentros_unSoloCentroConTramitesIniciables_loFijaYNoExigeElegir() {
        centroUsuario(centroA);
        centroUsuario(centroB);
        stubCatalogo(tramite);
        stubPerfilesDeInicio(tramite, centroA, Profile.CREADOR);
        stubPerfilesDeInicio(tramite, centroB);
        var asistente = new AsistenteNuevoExpediente();

        service.prepararCentros(asistente);

        assertEquals(1, asistente.getCentrosDisponibles().size());
        assertTrue(asistente.getCentrosDisponibles().contains(centroA));
        assertFalse(asistente.getHayQueElegirCentro());
        assertSame(centroA, asistente.getCentro());
    }

    @Test
    void prepararCentros_ningunCentroConTramitesIniciables_dejaLaListaVaciaYElCentroNulo() {
        centroUsuario(centroA);
        centroUsuario(centroB);
        stubCatalogo(tramite);
        stubPerfilesDeInicio(tramite, centroA);
        stubPerfilesDeInicio(tramite, centroB);
        var asistente = new AsistenteNuevoExpediente();

        service.prepararCentros(asistente);

        assertTrue(asistente.getCentrosDisponibles().isEmpty());
        assertFalse(asistente.getHayQueElegirCentro());
        assertNull(asistente.getCentro());
    }

    @Test
    void prepararCentros_usuarioSinCentros_dejaLaListaVaciaYNoClasificaNada() {
        var asistente = new AsistenteNuevoExpediente();

        service.prepararCentros(asistente);

        assertTrue(asistente.getCentrosDisponibles().isEmpty());
        assertFalse(asistente.getHayQueElegirCentro());
        assertNull(asistente.getCentro());
        verifyNoInteractions(perfilesUsuarioService);
    }

    @Test
    void prepararCentros_catalogoSinTramitesEvaluables_noOfreceNingunCentro() {
        centroUsuario(centroA);
        centroUsuario(centroB);
        stubCatalogo();
        var asistente = new AsistenteNuevoExpediente();

        service.prepararCentros(asistente);

        assertTrue(asistente.getCentrosDisponibles().isEmpty());
        assertNull(asistente.getCentro());
        verifyNoInteractions(perfilesUsuarioService);
    }

    @Test
    void prepararCentros_conVariosCentros_leeElCatalogoUnaSolaVez() {
        centroUsuario(centroA);
        centroUsuario(centroB);
        centroUsuario(centroC);
        stubCatalogo(tramite, otroTramite);
        when(perfilesUsuarioService.getPerfilesDeInicioSobreTramite(any(), any(), any()))
                .thenReturn(Set.of(Profile.CREADOR));
        var asistente = new AsistenteNuevoExpediente();

        service.prepararCentros(asistente);

        verify(ventanillaRepository, times(1)).findTramitesEvaluables();
        verify(perfilesUsuarioService, times(6)).getPerfilesDeInicioSobreTramite(any(), any(), any());
    }

    @Test
    void prepararCentros_conCentrosYCentroEnviadosPorElCliente_losSobrescribeIncondicionalmente() {
        centroUsuario(centroA);
        stubCatalogo(tramite);
        stubPerfilesDeInicio(tramite, centroA, Profile.CREADOR);
        var asistente = new AsistenteNuevoExpediente();
        asistente.setCentrosDisponibles(new LinkedHashSet<>(Set.of(centroB)));
        asistente.setCentro(centroB);
        asistente.setHayQueElegirCentro(true);

        service.prepararCentros(asistente);

        assertEquals(1, asistente.getCentrosDisponibles().size());
        assertTrue(asistente.getCentrosDisponibles().contains(centroA));
        assertSame(centroA, asistente.getCentro());
        assertFalse(asistente.getHayQueElegirCentro());
    }

    /* ------------------------------------------------------------------ */
    /* prepararTramites                                                   */
    /* ------------------------------------------------------------------ */

    @Test
    void prepararTramites_centroConTramitesIniciables_soloOfreceEsos() {
        stubCatalogo(tramite, otroTramite);
        stubPerfilesDeInicio(tramite, centroA, Profile.CREADOR);
        stubPerfilesDeInicio(otroTramite, centroA);
        var asistente = asistenteConCentro();

        var resultado = service.prepararTramites(asistente);

        assertEquals(1, asistente.getTramitesDisponibles().size());
        assertTrue(asistente.getTramitesDisponibles().contains(tramite));
        assertSame(asistente, resultado);
        verify(ventanillaRepository, times(1)).findTramitesEvaluables();
    }

    @Test
    void prepararTramites_centroSinNingunTramiteIniciable_dejaLaListaVacia() {
        stubCatalogo(tramite, otroTramite);
        stubPerfilesDeInicio(tramite, centroA);
        stubPerfilesDeInicio(otroTramite, centroA);
        var asistente = asistenteConCentro();

        service.prepararTramites(asistente);

        assertTrue(asistente.getTramitesDisponibles().isEmpty());
    }

    @Test
    void prepararTramites_centroAjenoAlUsuario_dejaLaListaVacia() {
        centroUsuario(centroA);
        stubCatalogo(tramite);
        stubPerfilesDeInicio(tramite, centroB);
        var asistente = new AsistenteNuevoExpediente();
        asistente.setCentro(centroB);

        service.prepararTramites(asistente);

        assertTrue(asistente.getTramitesDisponibles().isEmpty());
    }

    @Test
    void prepararTramites_conTramitesEnviadosPorElCliente_losSobrescribeIncondicionalmente() {
        stubCatalogo(tramite, otroTramite);
        stubPerfilesDeInicio(tramite, centroA, Profile.CREADOR);
        stubPerfilesDeInicio(otroTramite, centroA);
        var asistente = asistenteConCentro();
        asistente.setTramitesDisponibles(new LinkedHashSet<>(Set.of(otroTramite)));

        service.prepararTramites(asistente);

        assertEquals(1, asistente.getTramitesDisponibles().size());
        assertTrue(asistente.getTramitesDisponibles().contains(tramite));
    }

    @Test
    void prepararTramites_noRecalculaHayQueElegirCentroNiTocaElCentro() {
        centroUsuario(centroA);
        centroUsuario(centroB);
        stubCatalogo(tramite);
        stubPerfilesDeInicio(tramite, centroA, Profile.CREADOR);
        var asistente = asistenteConCentro();
        asistente.setHayQueElegirCentro(true);

        service.prepararTramites(asistente);

        assertTrue(asistente.getHayQueElegirCentro());
        assertSame(centroA, asistente.getCentro());
        assertNull(asistente.getCentrosDisponibles());
        verify(perfilesUsuarioService, never()).getPerfilesDeInicioSobreTramite(any(), any(), eq(centroB));
    }

    @Test
    void prepararTramites_sinCentro_lanzaValidationExceptionConDebeIndicarElCentro() {
        var asistente = new AsistenteNuevoExpediente();

        var excepcion = assertThrows(ValidationException.class, () -> service.prepararTramites(asistente));

        assertTrue(excepcion.getMessage().contains(MENSAJE_CENTRO));
        verifyNoInteractions(ventanillaRepository);
    }

    /* ------------------------------------------------------------------ */
    /* recalcular                                                         */
    /* ------------------------------------------------------------------ */

    @Test
    void recalcular_lasDosFormasPosiblesYSinContestar_preguntaComoSePresentaYNoPreguntaParaQuienEs() {
        tramite.setTipoTramite(tipoTramiteSinTipoUsuario);
        stubCatalogo(tramite);
        stubOraculoAcepta();
        var asistente = asistenteConCentroYTramite(tramite);

        var resultado = service.recalcular(asistente);

        assertTrue(asistente.getHayQuePreguntarPresentacion());
        assertNull(asistente.getPresentadoEnPapel());
        assertFalse(asistente.getHayQuePreguntarParaQuien());
        assertNull(asistente.getPresentadoEnRepresentacion());
        assertSame(asistente, resultado);
    }

    @Test
    void recalcular_lasDosFormasPosiblesYFormaYaContestada_preguntaParaQuienEs() {
        tramite.setTipoTramite(tipoTramiteSinTipoUsuario);
        stubCatalogo(tramite);
        stubOraculoAcepta();
        var asistente = asistenteConCentroYTramite(tramite);
        asistente.setPresentadoEnPapel(false);

        service.recalcular(asistente);

        assertTrue(asistente.getHayQuePreguntarPresentacion());
        assertFalse(asistente.getPresentadoEnPapel());
        assertTrue(asistente.getHayQuePreguntarParaQuien());
        assertNull(asistente.getPresentadoEnRepresentacion());
    }

    @Test
    void recalcular_soloSePuedePresentarEnPapel_fijaLaFormaYNoPregunta() {
        centroUsuario(centroA, "ALUMNO");
        stubCatalogo(tramite);
        stubOraculoSoloAdmitePapel();
        var asistente = asistenteConCentroYTramite(tramite);

        service.recalcular(asistente);

        assertFalse(asistente.getHayQuePreguntarPresentacion());
        assertTrue(asistente.getPresentadoEnPapel());
    }

    @Test
    void recalcular_soloSePuedePresentarEnPapelYElClienteEnviaLaOtraForma_laDescarta() {
        centroUsuario(centroA, "ALUMNO");
        stubCatalogo(tramite);
        stubOraculoSoloAdmitePapel();
        var asistente = asistenteConCentroYTramite(tramite);
        asistente.setPresentadoEnPapel(false);

        service.recalcular(asistente);

        assertTrue(asistente.getPresentadoEnPapel());
    }

    @Test
    void recalcular_ningunaFormaPosible_dejaLosDosCamposInformadosYNoPreguntaNada() {
        centroUsuario(centroA, "ALUMNO");
        stubCatalogo(tramite);
        stubOraculoRechazaSiempre(MENSAJE_SIN_PERFIL_DE_INICIO);
        var asistente = asistenteConCentroYTramite(tramite);

        service.recalcular(asistente);

        assertFalse(asistente.getHayQuePreguntarPresentacion());
        assertFalse(asistente.getPresentadoEnPapel());
        assertFalse(asistente.getHayQuePreguntarParaQuien());
        assertFalse(asistente.getPresentadoEnRepresentacion());
    }

    @Test
    void recalcular_unSoloDestinatarioPosibleParaLaFormaFijada_loFijaYNoPregunta() {
        centroUsuario(centroA, "ALUMNO");
        stubCatalogo(tramite);
        stubOraculoSoloAdmiteParaMi();
        var asistente = asistenteConCentroYTramite(tramite);
        asistente.setPresentadoEnPapel(false);

        service.recalcular(asistente);

        assertFalse(asistente.getHayQuePreguntarParaQuien());
        assertFalse(asistente.getPresentadoEnRepresentacion());
    }

    @Test
    void recalcular_alCambiarLaFormaDePresentar_noConservaElDestinatarioAnterior() {
        centroUsuario(centroA, "ALUMNO");
        stubCatalogo(tramite);
        stubOraculoNoAdmiteRepresentacionEnPapel();
        var asistente = asistenteConCentroYTramite(tramite);
        asistente.setPresentadoEnPapel(true);
        asistente.setPresentadoEnRepresentacion(true);

        service.recalcular(asistente);

        assertFalse(asistente.getPresentadoEnRepresentacion());
    }

    @Test
    void recalcular_consultaElOraculoExactamenteUnaVezPorCelda() {
        centroUsuario(centroA, "ALUMNO");
        stubCatalogo(tramite);
        stubOraculoAcepta();
        ArgumentCaptor<ContextoTramitacion> contextos = capturaDeContextos();

        service.recalcular(asistenteCompleto());

        verify(tramitadorService, times(4)).validateTriggerInitialEvent(contextos.capture());
        var parejas = contextos.getAllValues().stream()
                .map(contexto -> contexto.profile() + "/" + contexto.presentadoEnRepresentacion())
                .collect(Collectors.toSet());
        assertEquals(Set.of("CREADOR/false", "CREADOR/true", "TRAMITADOR/false", "TRAMITADOR/true"), parejas);
    }

    @Test
    void recalcular_noLeeElCatalogoParaCalcularLaMatriz() {
        centroUsuario(centroA, "ALUMNO");
        stubCatalogo(tramite);
        stubOraculoAcepta();

        service.recalcular(asistenteCompleto());

        verify(ventanillaRepository, times(1)).findTramitesEvaluables();
    }

    @Test
    void recalcular_nuncaConsultaLosPerfilesDelUsuarioParaLaFormaEnviada() {
        centroUsuario(centroA, "ALUMNO");
        stubCatalogo(tramite);
        stubOraculoSoloAdmiteQueLoPresenteElUsuario();
        var asistente = asistenteConCentroYTramite(tramite);
        asistente.setPresentadoEnPapel(true);

        service.recalcular(asistente);

        verify(perfilesUsuarioService, never()).getPerfil(any(), any(), any(), anyBoolean());
    }

    @Test
    void recalcular_noAsignaNombreNiAyudaDelTramite() {
        centroUsuario(centroA, "ALUMNO");
        stubCatalogo(tramite);
        stubOraculoAcepta();
        var asistente = asistenteCompleto();
        asistente.setNombreTramite("nombre falso");
        asistente.setAyudaTramite("ayuda falsa");

        service.recalcular(asistente);

        assertEquals(NOMBRE_TRAMITE_DERIVADO, asistente.getNombreTramite());
        assertEquals(AYUDA_TRAMITE, asistente.getAyudaTramite());
    }

    @Test
    void recalcular_sinCentro_lanzaValidationExceptionConDebeIndicarElCentro() {
        var asistente = new AsistenteNuevoExpediente();
        asistente.setTramite(tramite);

        var excepcion = assertThrows(ValidationException.class, () -> service.recalcular(asistente));

        assertTrue(excepcion.getMessage().contains(MENSAJE_CENTRO));
        verifyNoInteractions(tramitadorService);
    }

    @Test
    void recalcular_tramiteNoEvaluable_lanzaValidationExceptionConDebeIndicarElTramite() {
        stubCatalogo(tramite);
        var asistente = asistenteConCentroYTramite(tramiteNoEvaluable);

        var excepcion = assertThrows(ValidationException.class, () -> service.recalcular(asistente));

        assertTrue(excepcion.getMessage().contains(MENSAJE_TRAMITE));
        verifyNoInteractions(tramitadorService);
        verify(perfilesUsuarioService, never()).getPerfilesDeInicioSobreTramite(any(), any(), any());
    }

    /* ------------------------------------------------------------------ */
    /* validatePrepararCentros                                            */
    /* ------------------------------------------------------------------ */

    @Test
    void validatePrepararCentros_cualquierBean_devuelveVacio() {
        var asistente = new AsistenteNuevoExpediente();

        assertTrue(service.validatePrepararCentros(asistente).isEmpty());

        verifyNoInteractions(tramitadorService);
        verifyNoInteractions(perfilesUsuarioService);
        verifyNoInteractions(ventanillaRepository);
    }

    /* ------------------------------------------------------------------ */
    /* validatePrepararTramites                                           */
    /* ------------------------------------------------------------------ */

    @Test
    void validatePrepararTramites_conCentro_devuelveVacio() {
        assertTrue(service.validatePrepararTramites(asistenteConCentro()).isEmpty());
    }

    @Test
    void validatePrepararTramites_sinCentro_devuelveDebeIndicarElCentro() {
        var asistente = new AsistenteNuevoExpediente();

        assertMensajeDeLaVentanilla(MENSAJE_CENTRO, "centro", service.validatePrepararTramites(asistente));
    }

    @Test
    void validatePrepararTramites_sinTramite_devuelveVacioYNoConsultaElCatalogo() {
        assertTrue(service.validatePrepararTramites(asistenteConCentro()).isEmpty());

        verifyNoInteractions(ventanillaRepository);
    }

    /* ------------------------------------------------------------------ */
    /* validateRecalcular                                                 */
    /* ------------------------------------------------------------------ */

    @Test
    void validateRecalcular_centroYTramiteEvaluable_devuelveVacio() {
        stubCatalogo(tramite);

        assertTrue(service.validateRecalcular(asistenteConCentroYTramite(tramite)).isEmpty());
    }

    @Test
    void validateRecalcular_sinCentro_devuelveDebeIndicarElCentroYNoConsultaElCatalogo() {
        var asistente = new AsistenteNuevoExpediente();
        asistente.setTramite(tramite);

        assertMensajeDeLaVentanilla(MENSAJE_CENTRO, "centro", service.validateRecalcular(asistente));

        verifyNoInteractions(ventanillaRepository);
    }

    @Test
    void validateRecalcular_sinTramite_devuelveDebeIndicarElTramite() {
        stubCatalogo(tramite);

        assertMensajeDeLaVentanilla(MENSAJE_TRAMITE, "tramite", service.validateRecalcular(asistenteConCentro()));
    }

    @Test
    void validateRecalcular_tramiteFueraDeLosEvaluables_devuelveDebeIndicarElTramite() {
        stubCatalogo(tramite);

        assertMensajeDeLaVentanilla(MENSAJE_TRAMITE, "tramite", service.validateRecalcular(asistenteConCentroYTramite(tramiteNoEvaluable)));
    }

    @Test
    void validateRecalcular_sinCentroNiTramite_devuelveUnSoloMensaje() {
        var asistente = new AsistenteNuevoExpediente();

        assertMensajeDeLaVentanilla(MENSAJE_CENTRO, "centro", service.validateRecalcular(asistente));
    }

    @Test
    void validateRecalcular_sinLasDosRespuestasDelUsuario_devuelveVacio() {
        stubCatalogo(tramite);
        var asistente = asistenteConCentroYTramite(tramite);

        assertTrue(service.validateRecalcular(asistente).isEmpty());

        verifyNoInteractions(tramitadorService);
    }

    /* ------------------------------------------------------------------ */
    /* validateTriggerInitialEvent                                        */
    /* ------------------------------------------------------------------ */

    @Test
    void validateTriggerInitialEvent_datosCompletosYMotorYAfinadoConformes_devuelveVacio() {
        centroUsuario(centroA, "ALUMNO");
        stubCatalogo(tramite);
        stubOraculoAcepta();

        assertTrue(service.validateTriggerInitialEvent(asistenteCompleto()).isEmpty());
    }

    @Test
    void validateTriggerInitialEvent_sinCentro_devuelveDebeIndicarElCentroYNoPreguntaAlMotor() {
        var asistente = asistenteCompleto();
        asistente.setCentro(null);

        assertMensajeDeLaVentanilla(MENSAJE_CENTRO, "centro", service.validateTriggerInitialEvent(asistente));

        verifyNoInteractions(tramitadorService);
    }

    @Test
    void validateTriggerInitialEvent_sinTramite_devuelveDebeIndicarElTramiteYNoConstruyeElContexto() {
        stubCatalogo(tramite);
        var asistente = asistenteCompleto();
        asistente.setTramite(null);

        assertMensajeDeLaVentanilla(MENSAJE_TRAMITE, "tramite", service.validateTriggerInitialEvent(asistente));

        verifyNoInteractions(tramitadorService);
    }

    @Test
    void validateTriggerInitialEvent_tramiteNoEvaluable_devuelveDebeIndicarElTramite() {
        stubCatalogo(tramite);
        var asistente = asistenteCompleto();
        asistente.setTramite(tramiteNoEvaluable);

        assertMensajeDeLaVentanilla(MENSAJE_TRAMITE, "tramite", service.validateTriggerInitialEvent(asistente));

        verifyNoInteractions(perfilesUsuarioService);
        verifyNoInteractions(tramitadorService);
    }

    @Test
    void validateTriggerInitialEvent_sinFormaDePresentar_devuelveDebeIndicarComoSePresenta() {
        stubCatalogo(tramite);
        var asistente = asistenteCompleto();
        asistente.setPresentadoEnPapel(null);

        assertMensajeDeLaVentanilla(MENSAJE_COMO_SE_PRESENTA, "presentadoEnPapel", service.validateTriggerInitialEvent(asistente));

        verifyNoInteractions(tramitadorService);
    }

    @Test
    void validateTriggerInitialEvent_sinDestinatario_devuelveDebeIndicarParaQuienEs() {
        stubCatalogo(tramite);
        var asistente = asistenteCompleto();
        asistente.setPresentadoEnRepresentacion(null);

        assertMensajeDeLaVentanilla(MENSAJE_PARA_QUIEN, "presentadoEnRepresentacion", service.validateTriggerInitialEvent(asistente));

        verifyNoInteractions(tramitadorService);
    }

    @Test
    void validateTriggerInitialEvent_usuarioSinPerfilDeInicioEnElCentro_devuelveElLiteralDelMotor() {
        centroUsuario(centroA, "ALUMNO");
        stubCatalogo(tramite);
        stubOraculoRechazaSiempre(MENSAJE_SIN_PERFIL_DE_INICIO);

        assertMensajeDelMotor(MENSAJE_SIN_PERFIL_DE_INICIO, service.validateTriggerInitialEvent(asistenteCompleto()));
    }

    @Test
    void validateTriggerInitialEvent_formaDePresentarNoPermitida_devuelveElLiteralDelMotor() {
        centroUsuario(centroA, "ALUMNO");
        stubCatalogo(tramite);
        stubOraculoRechazaSiempre(MENSAJE_FORMA_NO_PERMITIDA);

        assertMensajeDelMotor(MENSAJE_FORMA_NO_PERMITIDA, service.validateTriggerInitialEvent(asistenteCompleto()));
    }

    @Test
    void validateTriggerInitialEvent_tramiteQueNoAdmiteRepresentacion_devuelveElLiteralDelMotor() {
        centroUsuario(centroA, "ALUMNO");
        stubCatalogo(tramite);
        stubOraculoRechazaSiempre(MENSAJE_SIN_REPRESENTACION);
        var asistente = asistenteCompleto();
        asistente.setPresentadoEnRepresentacion(true);

        assertMensajeDelMotor(MENSAJE_SIN_REPRESENTACION, service.validateTriggerInitialEvent(asistente));
    }

    @Test
    void validateTriggerInitialEvent_motorRechaza_noEvaluaElAfinadoDelDestinatario() {
        stubCatalogo(tramite);
        stubOraculoRechazaSiempre(MENSAJE_SIN_PERFIL_DE_INICIO);

        assertMensajeDelMotor(MENSAJE_SIN_PERFIL_DE_INICIO, service.validateTriggerInitialEvent(asistenteCompleto()));
    }

    @Test
    void validateTriggerInitialEvent_paraUstedMismoSiendoFamiliarQueNoEsDelTipoDelTramite_devuelveNoPuedeCrearParaUstedMismo() {
        centroUsuario(centroA, "FAMILIAR");
        stubCatalogo(tramite);
        stubOraculoAcepta();

        assertMensajeDeLaVentanilla(MENSAJE_NO_PARA_USTED_MISMO, "presentadoEnRepresentacion", service.validateTriggerInitialEvent(asistenteCompleto()));
    }

    @Test
    void validateTriggerInitialEvent_paraUstedMismoSiendoDelTipoDelTramite_devuelveVacio() {
        centroUsuario(centroA, "ALUMNO", "FAMILIAR");
        stubCatalogo(tramite);
        stubOraculoAcepta();

        assertTrue(service.validateTriggerInitialEvent(asistenteCompleto()).isEmpty());
    }

    @Test
    void validateTriggerInitialEvent_paraUstedMismoSinSerFamiliar_devuelveVacio() {
        centroUsuario(centroA, "PROFESOR");
        stubCatalogo(tramite);
        stubOraculoAcepta();

        assertTrue(service.validateTriggerInitialEvent(asistenteCompleto()).isEmpty());
    }

    @Test
    void validateTriggerInitialEvent_paraUstedMismoEnTramiteQueNoAdmiteRepresentacion_devuelveVacio() {
        tramite.setPermitidoPresentarEnRepresentacion(false);
        centroUsuario(centroA, "FAMILIAR");
        stubCatalogo(tramite);
        stubOraculoAcepta();

        assertTrue(service.validateTriggerInitialEvent(asistenteCompleto()).isEmpty());
    }

    @Test
    void validateTriggerInitialEvent_enRepresentacionSinSerFamiliarYSiendoDelTipoDelTramite_devuelveNoPuedeCrearEnRepresentacion() {
        centroUsuario(centroA, "ALUMNO");
        stubCatalogo(tramite);
        stubOraculoAcepta();
        var asistente = asistenteCompleto();
        asistente.setPresentadoEnRepresentacion(true);

        assertMensajeDeLaVentanilla(MENSAJE_NO_EN_REPRESENTACION, "presentadoEnRepresentacion", service.validateTriggerInitialEvent(asistente));
    }

    @Test
    void validateTriggerInitialEvent_enRepresentacionSiendoFamiliar_devuelveVacio() {
        centroUsuario(centroA, "ALUMNO", "FAMILIAR");
        stubCatalogo(tramite);
        stubOraculoAcepta();
        var asistente = asistenteCompleto();
        asistente.setPresentadoEnRepresentacion(true);

        assertTrue(service.validateTriggerInitialEvent(asistente).isEmpty());
    }

    @Test
    void validateTriggerInitialEvent_enRepresentacionSinSerDelTipoDelTramite_devuelveVacio() {
        centroUsuario(centroA, "PROFESOR");
        stubCatalogo(tramite);
        stubOraculoAcepta();
        var asistente = asistenteCompleto();
        asistente.setPresentadoEnRepresentacion(true);

        assertTrue(service.validateTriggerInitialEvent(asistente).isEmpty());
    }

    @Test
    void validateTriggerInitialEvent_tipoTramiteSinTipoUsuario_noAplicaElAfinadoYDevuelveVacio() {
        tramite.setTipoTramite(tipoTramiteSinTipoUsuario);
        centroUsuario(centroA, "FAMILIAR");
        stubCatalogo(tramite);
        stubOraculoAcepta();

        assertTrue(service.validateTriggerInitialEvent(asistenteCompleto()).isEmpty());
    }

    @Test
    void validateTriggerInitialEvent_registradoEnPapel_noAplicaElAfinado() {
        centroUsuario(centroA, "FAMILIAR");
        stubCatalogo(tramite);
        stubOraculoAcepta();
        var asistente = asistenteCompleto();
        asistente.setPresentadoEnPapel(true);

        assertTrue(service.validateTriggerInitialEvent(asistente).isEmpty());
    }

    @Test
    void validateTriggerInitialEvent_construyeElContextoConElPerfilQueExigeLaFormaDePresentar() {
        centroUsuario(centroA, "ALUMNO");
        stubCatalogo(tramite);
        stubOraculoAcepta();
        ArgumentCaptor<ContextoTramitacion> contextos = capturaDeContextos();
        var presentadoPorElUsuario = asistenteCompleto();
        var presentadoEnPapel = asistenteCompleto();
        presentadoEnPapel.setPresentadoEnPapel(true);

        service.validateTriggerInitialEvent(presentadoPorElUsuario);
        service.validateTriggerInitialEvent(presentadoEnPapel);

        verify(tramitadorService, times(2)).validateTriggerInitialEvent(contextos.capture());
        var contextoDelUsuario = contextos.getAllValues().get(0);
        var contextoEnPapel = contextos.getAllValues().get(1);
        assertEquals(Profile.CREADOR, contextoDelUsuario.profile());
        assertFalse(contextoDelUsuario.presentadoEnPapel());
        assertEquals(Profile.TRAMITADOR, contextoEnPapel.profile());
        assertTrue(contextoEnPapel.presentadoEnPapel());
        contextos.getAllValues().forEach(contexto -> {
            assertSame(tramite, contexto.tramite());
            assertSame(centroA, contexto.centro());
            assertFalse(contexto.presentadoEnRepresentacion());
        });
        verify(perfilesUsuarioService, never()).getPerfil(any(), any(), any(), anyBoolean());
    }

    @Test
    void validateTriggerInitialEvent_conVariasPuertasEnFallo_devuelveUnSoloMensaje() {
        var asistente = new AsistenteNuevoExpediente();

        assertMensajeDeLaVentanilla(MENSAJE_CENTRO, "centro", service.validateTriggerInitialEvent(asistente));
    }

    @Test
    void validateTriggerInitialEvent_usuarioQueNoPerteneceAlCentroPeroElMotorAcepta_lanzaIllegalStateException() {
        stubCatalogo(tramite);
        stubOraculoAcepta();
        var asistente = asistenteCompleto();

        assertThrows(IllegalStateException.class, () -> service.validateTriggerInitialEvent(asistente));
    }

    /* ------------------------------------------------------------------ */
    /* AllowProperties                                                    */
    /* ------------------------------------------------------------------ */

    @Test
    void allowPropertiesPrepararCentros_noAceptaNingunCampo() {
        AllowProperties allowProperties = service.allowPropertiesPrepararCentros();

        assertFalse(allowProperties.allowProperty("centro"));
        assertFalse(allowProperties.allowProperty("tramite"));
        assertFalse(allowProperties.allowProperty("presentadoEnPapel"));
        assertFalse(allowProperties.allowProperty("presentadoEnRepresentacion"));
        assertFalse(allowProperties.allowProperty("centrosDisponibles"));
        assertFalse(allowProperties.allowProperty("hayQueElegirCentro"));
        assertFalse(allowProperties.allowProperty("hayQuePreguntarPresentacion"));
        assertFalse(allowProperties.allowProperty("hayQuePreguntarParaQuien"));
    }

    @Test
    void allowPropertiesPrepararTramites_soloAceptaElCentro() {
        AllowProperties allowProperties = service.allowPropertiesPrepararTramites();

        assertTrue(allowProperties.allowProperty("centro"));
        assertFalse(allowProperties.allowProperty("tramite"));
        assertFalse(allowProperties.allowProperty("presentadoEnPapel"));
        assertFalse(allowProperties.allowProperty("presentadoEnRepresentacion"));
        assertFalse(allowProperties.allowProperty("tramitesDisponibles"));
        assertFalse(allowProperties.allowProperty("hayQueElegirCentro"));
    }

    @Test
    void allowPropertiesRecalcular_aceptaTramiteCentroYFormaDePresentarPeroNoElDestinatario() {
        AllowProperties allowProperties = service.allowPropertiesRecalcular();

        assertTrue(allowProperties.allowProperty("tramite"));
        assertTrue(allowProperties.allowProperty("centro"));
        assertTrue(allowProperties.allowProperty("presentadoEnPapel"));
        assertFalse(allowProperties.allowProperty("presentadoEnRepresentacion"));
        assertFalse(allowProperties.allowProperty("centrosDisponibles"));
        assertFalse(allowProperties.allowProperty("tramitesDisponibles"));
        assertFalse(allowProperties.allowProperty("hayQueElegirCentro"));
        assertFalse(allowProperties.allowProperty("hayQuePreguntarPresentacion"));
        assertFalse(allowProperties.allowProperty("hayQuePreguntarParaQuien"));
    }

    @Test
    void allowPropertiesTriggerInitialEvent_aceptaExactamenteLosCuatroCamposDeLaAccionCrearExpediente() {
        AllowProperties allowProperties = service.allowPropertiesTriggerInitialEvent();

        assertTrue(allowProperties.allowProperty("tramite"));
        assertTrue(allowProperties.allowProperty("centro"));
        assertTrue(allowProperties.allowProperty("presentadoEnPapel"));
        assertTrue(allowProperties.allowProperty("presentadoEnRepresentacion"));
        assertFalse(allowProperties.allowProperty("centrosDisponibles"));
        assertFalse(allowProperties.allowProperty("tramitesDisponibles"));
        assertFalse(allowProperties.allowProperty("hayQueElegirCentro"));
        assertFalse(allowProperties.allowProperty("hayQuePreguntarPresentacion"));
        assertFalse(allowProperties.allowProperty("hayQuePreguntarParaQuien"));
    }

    @Test
    void allowPropertiesInsert_noAceptaNingunCampo() {
        AllowProperties allowProperties = service.allowPropertiesInsert();

        CAMPOS_DEL_MODELO.forEach(campo -> assertFalse(allowProperties.allowProperty(campo), campo));
    }

    @Test
    void allowPropertiesUpdate_noAceptaNingunCampo() {
        AllowProperties allowProperties = service.allowPropertiesUpdate();

        CAMPOS_DEL_MODELO.forEach(campo -> assertFalse(allowProperties.allowProperty(campo), campo));
    }

    @Test
    void allowProperties_ningunaAccionAceptaLosCamposDerivadosNiLosDelServidor() {
        List<AllowProperties> todasLasAcciones = List.of(
                service.allowPropertiesPrepararCentros(),
                service.allowPropertiesPrepararTramites(),
                service.allowPropertiesRecalcular(),
                service.allowPropertiesTriggerInitialEvent(),
                service.allowPropertiesInsert(),
                service.allowPropertiesUpdate());

        todasLasAcciones.forEach(allowProperties -> CAMPOS_DERIVADOS_Y_DEL_SERVIDOR
                .forEach(campo -> assertFalse(allowProperties.allowProperty(campo), campo)));
    }
}
