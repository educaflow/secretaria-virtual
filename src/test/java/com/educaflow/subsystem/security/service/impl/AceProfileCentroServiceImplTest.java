package com.educaflow.subsystem.security.service.impl;

import com.axelor.auth.db.User;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.i18n.I18n;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.common.db.Cargo;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.subsystem.expedientes.db.Tramite;
import com.educaflow.subsystem.security.db.AceProfileCentro;
import com.educaflow.subsystem.security.db.repo.AceProfileCentroRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AceProfileCentroServiceImplTest {

    private static final String MENSAJE_CENTRO_OBLIGATORIO = "El centro es obligatorio";
    private static final String MENSAJE_TRAMITE_OBLIGATORIO = "El trámite es obligatorio";
    private static final String MENSAJE_PERFIL_OBLIGATORIO = "El perfil es obligatorio";
    private static final String MENSAJE_INDICA_A_QUIEN = "Indica a quién se da el perfil: un tipo de usuario, un cargo o un usuario";
    private static final String MENSAJE_SOLO_UNO = "Indica solo uno: un tipo de usuario, un cargo o un usuario";
    private static final String MENSAJE_USUARIO_NO_PERTENECE = "El usuario no pertenece al centro";
    private static final String MENSAJE_YA_EXISTE = "Ya existe esa asignación de perfil";
    private static final String MENSAJE_SOLO_CENTROS_SUPERVISADOS = "Solo puedes gestionar perfiles de los centros de los que eres supervisor";

    private static final List<String> CAMPOS_DE_LA_FILA = List.of(
            "centro", "tramite", "perfil", "tipoUsuario", "cargo", "usuario");

    private AceProfileCentroServiceImpl service;
    private AceProfileCentroRepository repository;

    private Centro centroMislata;
    private Centro centroBatoi;
    private User usuarioAutenticado;
    private Tramite tramite;

    private MockedStatic<I18n> i18nMock;
    private MockedStatic<SecurityUtil> securityUtilMock;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(AceProfileCentroRepository.class);
        service = new AceProfileCentroServiceImpl(AceProfileCentro.class, repository);

        centroMislata = centro(1L, "Mislata");
        centroBatoi = centro(2L, "Batoi");

        usuarioAutenticado = new User();
        usuarioAutenticado.setId(7L);

        tramite = new Tramite();
        tramite.setId(10L);

        i18nMock = Mockito.mockStatic(I18n.class);
        i18nMock.when(() -> I18n.get(any(String.class))).thenAnswer(invocation -> invocation.getArgument(0));

        securityUtilMock = Mockito.mockStatic(SecurityUtil.class);
        securityUtilMock.when(SecurityUtil::getUser).thenReturn(usuarioAutenticado);
        securityUtilMock.when(() -> SecurityUtil.isAdmin(usuarioAutenticado)).thenReturn(false);

        lenient().when(repository.findCentrosSupervisados(usuarioAutenticado)).thenReturn(List.of(centroMislata));
        lenient().when(repository.existeOtraIgual(any())).thenReturn(false);
    }

    @AfterEach
    void tearDown() {
        cerrarSiNoEsNulo(securityUtilMock);
        cerrarSiNoEsNulo(i18nMock);
    }

    /* ------------------------------------------------------------------ */
    /* Helpers                                                            */
    /* ------------------------------------------------------------------ */

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

    private static Cargo cargo(String name) {
        var cargo = new Cargo();
        cargo.setName(name);
        return cargo;
    }

    private static User usuarioDestino(Centro... centros) {
        var usuario = new User();
        usuario.setId(20L);
        usuario.setCentroUsuarios(new ArrayList<>());
        for (Centro centro : centros) {
            var centroUsuario = new CentroUsuario();
            centroUsuario.setCentro(centro);
            centroUsuario.setUsuario(usuario);
            usuario.getCentroUsuarios().add(centroUsuario);
        }
        return usuario;
    }

    private AceProfileCentro filaValidaConCargo() {
        var fila = new AceProfileCentro();
        fila.setCentro(centroMislata);
        fila.setTramite(tramite);
        fila.setPerfil(Profile.TRAMITADOR);
        fila.setCargo(cargo("Jefe de estudios"));
        return fila;
    }

    private AceProfileCentro filaValidaConCargoConId() {
        var fila = filaValidaConCargo();
        fila.setId(50L);
        return fila;
    }

    private AceProfileCentro filaConUnicoUsuario(Centro centro, User usuario) {
        var fila = new AceProfileCentro();
        fila.setCentro(centro);
        fila.setTramite(tramite);
        fila.setPerfil(Profile.COLABORADOR);
        fila.setUsuario(usuario);
        return fila;
    }

    private AceProfileCentro filaSinDestinatario() {
        var fila = new AceProfileCentro();
        fila.setCentro(centroMislata);
        fila.setTramite(tramite);
        fila.setPerfil(Profile.TRAMITADOR);
        return fila;
    }

    private void usuarioAutenticadoEsAdministrador() {
        securityUtilMock.when(() -> SecurityUtil.isAdmin(usuarioAutenticado)).thenReturn(true);
        lenient().when(repository.findCentrosSupervisados(usuarioAutenticado)).thenReturn(List.of());
    }

    private static List<String> textos(Optional<BusinessMessages> resultado) {
        assertTrue(resultado.isPresent());
        return resultado.get().stream().map(BusinessMessage::getMessage).toList();
    }

    private static void assertUnicoMensaje(String mensajeEsperado, Optional<BusinessMessages> resultado) {
        assertEquals(List.of(mensajeEsperado), textos(resultado));
    }

    private static void assertContieneMensaje(String mensajeEsperado, Optional<BusinessMessages> resultado) {
        assertTrue(textos(resultado).contains(mensajeEsperado));
    }

    /* ------------------------------------------------------------------ */
    /* validateInsert                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    void validateInsert_filaConCargoEnCentroSupervisado_devuelveVacio() {
        var fila = filaValidaConCargo();

        Optional<BusinessMessages> resultado = service.validateInsert(fila);

        assertEquals(Optional.empty(), resultado);
        verify(repository).existeOtraIgual(fila);
    }

    @Test
    void validateInsert_filaConTipoUsuarioEnCentroSupervisado_devuelveVacio() {
        var fila = new AceProfileCentro();
        fila.setCentro(centroMislata);
        fila.setTramite(tramite);
        fila.setPerfil(Profile.CREADOR);
        fila.setTipoUsuario(tipoUsuario("ALUMNO"));

        Optional<BusinessMessages> resultado = service.validateInsert(fila);

        assertEquals(Optional.empty(), resultado);
    }

    @Test
    void validateInsert_filaConUsuarioDelCentro_devuelveVacio() {
        var fila = filaConUnicoUsuario(centroMislata, usuarioDestino(centroMislata));

        Optional<BusinessMessages> resultado = service.validateInsert(fila);

        assertEquals(Optional.empty(), resultado);
    }

    @Test
    void validateInsert_administradorEnCentroNoSupervisado_devuelveVacio() {
        usuarioAutenticadoEsAdministrador();
        var fila = filaValidaConCargo();
        fila.setCentro(centroBatoi);

        Optional<BusinessMessages> resultado = service.validateInsert(fila);

        assertEquals(Optional.empty(), resultado);
    }

    @Test
    void validateInsert_centroNulo_devuelveMensajeCentroObligatorio() {
        var fila = filaValidaConCargo();
        fila.setCentro(null);

        Optional<BusinessMessages> resultado = service.validateInsert(fila);

        assertUnicoMensaje(MENSAJE_CENTRO_OBLIGATORIO, resultado);
        verify(repository, never()).existeOtraIgual(any());
        verify(repository, never()).findCentrosSupervisados(any());
    }

    @Test
    void validateInsert_tramiteNulo_devuelveMensajeTramiteObligatorio() {
        var fila = filaValidaConCargo();
        fila.setTramite(null);

        Optional<BusinessMessages> resultado = service.validateInsert(fila);

        assertUnicoMensaje(MENSAJE_TRAMITE_OBLIGATORIO, resultado);
        verify(repository, never()).existeOtraIgual(any());
    }

    @Test
    void validateInsert_perfilNulo_devuelveMensajePerfilObligatorio() {
        var fila = filaValidaConCargo();
        fila.setPerfil(null);

        Optional<BusinessMessages> resultado = service.validateInsert(fila);

        assertUnicoMensaje(MENSAJE_PERFIL_OBLIGATORIO, resultado);
        verify(repository, never()).existeOtraIgual(any());
    }

    @Test
    void validateInsert_sinDestinatario_devuelveMensajeIndicaAQuien() {
        var fila = filaSinDestinatario();

        Optional<BusinessMessages> resultado = service.validateInsert(fila);

        assertUnicoMensaje(MENSAJE_INDICA_A_QUIEN, resultado);
        verify(repository, never()).existeOtraIgual(any());
    }

    @Test
    void validateInsert_tipoUsuarioYCargo_devuelveMensajeSoloUno() {
        var fila = filaValidaConCargo();
        fila.setTipoUsuario(tipoUsuario("PROFESOR"));

        Optional<BusinessMessages> resultado = service.validateInsert(fila);

        assertUnicoMensaje(MENSAJE_SOLO_UNO, resultado);
        verify(repository, never()).existeOtraIgual(any());
    }

    @Test
    void validateInsert_tresDestinatarios_devuelveUnSoloMensajeSoloUno() {
        var fila = filaValidaConCargo();
        fila.setTipoUsuario(tipoUsuario("PROFESOR"));
        fila.setUsuario(usuarioDestino(centroMislata));

        Optional<BusinessMessages> resultado = service.validateInsert(fila);

        assertUnicoMensaje(MENSAJE_SOLO_UNO, resultado);
    }

    @Test
    void validateInsert_obligatoriosYDestinatarioFallan_acumulaMensajesDeFase1() {
        var fila = new AceProfileCentro();

        Optional<BusinessMessages> resultado = service.validateInsert(fila);

        assertEquals(List.of(MENSAJE_CENTRO_OBLIGATORIO, MENSAJE_TRAMITE_OBLIGATORIO,
                MENSAJE_PERFIL_OBLIGATORIO, MENSAJE_INDICA_A_QUIEN), textos(resultado));
        verify(repository, never()).existeOtraIgual(any());
    }

    @Test
    void validateInsert_centroNoSupervisado_devuelveMensajeSoloCentrosSupervisados() {
        var fila = filaValidaConCargo();
        fila.setCentro(centroBatoi);

        Optional<BusinessMessages> resultado = service.validateInsert(fila);

        assertContieneMensaje(MENSAJE_SOLO_CENTROS_SUPERVISADOS, resultado);
    }

    @Test
    void validateInsert_usuarioSinCentrosSupervisados_devuelveMensajeSoloCentrosSupervisados() {
        when(repository.findCentrosSupervisados(usuarioAutenticado)).thenReturn(List.of());
        var fila = filaValidaConCargo();

        Optional<BusinessMessages> resultado = service.validateInsert(fila);

        assertContieneMensaje(MENSAJE_SOLO_CENTROS_SUPERVISADOS, resultado);
    }

    @Test
    void validateInsert_centroSupervisadoComoOtraInstanciaConMismoId_devuelveVacio() {
        var fila = filaValidaConCargo();
        fila.setCentro(centro(1L, "Mislata"));

        Optional<BusinessMessages> resultado = service.validateInsert(fila);

        assertEquals(Optional.empty(), resultado);
    }

    @Test
    void validateInsert_usuarioDeOtroCentro_devuelveMensajeUsuarioNoPertenece() {
        var fila = filaConUnicoUsuario(centroMislata, usuarioDestino(centroBatoi));

        Optional<BusinessMessages> resultado = service.validateInsert(fila);

        assertContieneMensaje(MENSAJE_USUARIO_NO_PERTENECE, resultado);
    }

    @Test
    void validateInsert_usuarioSinCentros_devuelveMensajeUsuarioNoPertenece() {
        var fila = filaConUnicoUsuario(centroMislata, usuarioDestino());

        Optional<BusinessMessages> resultado = service.validateInsert(fila);

        assertContieneMensaje(MENSAJE_USUARIO_NO_PERTENECE, resultado);
    }

    @Test
    void validateInsert_asignacionRepetida_devuelveMensajeYaExiste() {
        var fila = filaValidaConCargo();
        when(repository.existeOtraIgual(fila)).thenReturn(true);

        Optional<BusinessMessages> resultado = service.validateInsert(fila);

        assertUnicoMensaje(MENSAJE_YA_EXISTE, resultado);
        verify(repository).existeOtraIgual(fila);
    }

    @Test
    void validateInsert_variasReglasDeFase2Fallan_acumulaMensajes() {
        var fila = filaConUnicoUsuario(centroBatoi, usuarioDestino(centroMislata));
        when(repository.existeOtraIgual(fila)).thenReturn(true);

        Optional<BusinessMessages> resultado = service.validateInsert(fila);

        assertEquals(List.of(MENSAJE_SOLO_CENTROS_SUPERVISADOS, MENSAJE_USUARIO_NO_PERTENECE, MENSAJE_YA_EXISTE),
                textos(resultado));
    }

    /* ------------------------------------------------------------------ */
    /* validateUpdate                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    void validateUpdate_filaValidaEnCentroSupervisado_devuelveVacio() {
        var fila = filaValidaConCargoConId();
        var original = filaValidaConCargoConId();

        Optional<BusinessMessages> resultado = service.validateUpdate(fila, original);

        assertEquals(Optional.empty(), resultado);
        verify(repository).existeOtraIgual(fila);
    }

    @Test
    void validateUpdate_centroNulo_devuelveMensajeCentroObligatorio() {
        var fila = filaValidaConCargoConId();
        fila.setCentro(null);
        var original = filaValidaConCargoConId();

        Optional<BusinessMessages> resultado = service.validateUpdate(fila, original);

        assertUnicoMensaje(MENSAJE_CENTRO_OBLIGATORIO, resultado);
    }

    @Test
    void validateUpdate_tramiteNulo_devuelveMensajeTramiteObligatorio() {
        var fila = filaValidaConCargoConId();
        fila.setTramite(null);
        var original = filaValidaConCargoConId();

        Optional<BusinessMessages> resultado = service.validateUpdate(fila, original);

        assertUnicoMensaje(MENSAJE_TRAMITE_OBLIGATORIO, resultado);
    }

    @Test
    void validateUpdate_perfilNulo_devuelveMensajePerfilObligatorio() {
        var fila = filaValidaConCargoConId();
        fila.setPerfil(null);
        var original = filaValidaConCargoConId();

        Optional<BusinessMessages> resultado = service.validateUpdate(fila, original);

        assertUnicoMensaje(MENSAJE_PERFIL_OBLIGATORIO, resultado);
    }

    @Test
    void validateUpdate_sinDestinatario_devuelveMensajeIndicaAQuien() {
        var fila = filaSinDestinatario();
        fila.setId(50L);
        var original = filaValidaConCargoConId();

        Optional<BusinessMessages> resultado = service.validateUpdate(fila, original);

        assertUnicoMensaje(MENSAJE_INDICA_A_QUIEN, resultado);
    }

    @Test
    void validateUpdate_dosDestinatarios_devuelveMensajeSoloUno() {
        var fila = filaValidaConCargoConId();
        fila.setTipoUsuario(tipoUsuario("PROFESOR"));
        var original = filaValidaConCargoConId();

        Optional<BusinessMessages> resultado = service.validateUpdate(fila, original);

        assertUnicoMensaje(MENSAJE_SOLO_UNO, resultado);
    }

    @Test
    void validateUpdate_usuarioDeOtroCentro_devuelveMensajeUsuarioNoPertenece() {
        var fila = filaConUnicoUsuario(centroMislata, usuarioDestino(centroBatoi));
        fila.setId(50L);
        var original = filaValidaConCargoConId();

        Optional<BusinessMessages> resultado = service.validateUpdate(fila, original);

        assertContieneMensaje(MENSAJE_USUARIO_NO_PERTENECE, resultado);
    }

    @Test
    void validateUpdate_asignacionRepetida_devuelveMensajeYaExiste() {
        var fila = filaValidaConCargoConId();
        var original = filaValidaConCargoConId();
        when(repository.existeOtraIgual(fila)).thenReturn(true);

        Optional<BusinessMessages> resultado = service.validateUpdate(fila, original);

        assertUnicoMensaje(MENSAJE_YA_EXISTE, resultado);
        verify(repository).existeOtraIgual(fila);
    }

    @Test
    void validateUpdate_centroDeLaFilaNoSupervisado_devuelveMensajeSoloCentrosSupervisados() {
        var fila = filaValidaConCargoConId();
        fila.setCentro(centroBatoi);
        var original = filaValidaConCargoConId();

        Optional<BusinessMessages> resultado = service.validateUpdate(fila, original);

        assertContieneMensaje(MENSAJE_SOLO_CENTROS_SUPERVISADOS, resultado);
    }

    @Test
    void validateUpdate_administradorEnCentroNoSupervisado_devuelveVacio() {
        usuarioAutenticadoEsAdministrador();
        var fila = filaValidaConCargoConId();
        fila.setCentro(centroBatoi);
        var original = filaValidaConCargoConId();

        Optional<BusinessMessages> resultado = service.validateUpdate(fila, original);

        assertEquals(Optional.empty(), resultado);
    }

    @Test
    void validateUpdate_originalNulo_noSeUsaYDevuelveVacio() {
        var fila = filaValidaConCargoConId();

        Optional<BusinessMessages> resultado = assertDoesNotThrow(() -> service.validateUpdate(fila, null));

        assertEquals(Optional.empty(), resultado);
    }

    /* ------------------------------------------------------------------ */
    /* validateRemove                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    void validateRemove_filaDeCentroSupervisado_devuelveVacio() {
        var fila = filaValidaConCargoConId();

        Optional<BusinessMessages> resultado = service.validateRemove(fila);

        assertEquals(Optional.empty(), resultado);
    }

    @Test
    void validateRemove_filaDeCentroNoSupervisado_devuelveMensajeSoloCentrosSupervisados() {
        var fila = filaValidaConCargoConId();
        fila.setCentro(centroBatoi);

        Optional<BusinessMessages> resultado = service.validateRemove(fila);

        assertUnicoMensaje(MENSAJE_SOLO_CENTROS_SUPERVISADOS, resultado);
    }

    @Test
    void validateRemove_administradorEnCentroNoSupervisado_devuelveVacio() {
        usuarioAutenticadoEsAdministrador();
        var fila = filaValidaConCargoConId();
        fila.setCentro(centroBatoi);

        Optional<BusinessMessages> resultado = service.validateRemove(fila);

        assertEquals(Optional.empty(), resultado);
    }

    @Test
    void validateRemove_usuarioSinCentrosSupervisados_devuelveMensajeSoloCentrosSupervisados() {
        when(repository.findCentrosSupervisados(usuarioAutenticado)).thenReturn(List.of());
        var fila = filaValidaConCargoConId();

        Optional<BusinessMessages> resultado = service.validateRemove(fila);

        assertUnicoMensaje(MENSAJE_SOLO_CENTROS_SUPERVISADOS, resultado);
    }

    @Test
    void validateRemove_filaSinDestinatarioNiTramite_soloAplicaCentroGestionable() {
        var fila = new AceProfileCentro();
        fila.setId(50L);
        fila.setCentro(centroMislata);

        Optional<BusinessMessages> resultado = service.validateRemove(fila);

        assertEquals(Optional.empty(), resultado);
        verify(repository, never()).existeOtraIgual(any());
    }

    /* ------------------------------------------------------------------ */
    /* validateGetCentrosSupervisados                                     */
    /* ------------------------------------------------------------------ */

    @Test
    void validateGetCentrosSupervisados_siempre_devuelveVacio() {
        lenient().when(repository.findCentrosSupervisados(usuarioAutenticado)).thenReturn(List.of());

        Optional<BusinessMessages> resultado = service.validateGetCentrosSupervisados();

        assertEquals(Optional.empty(), resultado);
    }

    /* ------------------------------------------------------------------ */
    /* getCentrosSupervisados                                             */
    /* ------------------------------------------------------------------ */

    @Test
    void getCentrosSupervisados_supervisorDeDosCentros_devuelveLaListaDelRepositorio() {
        when(repository.findCentrosSupervisados(usuarioAutenticado)).thenReturn(List.of(centroMislata, centroBatoi));

        List<Centro> centros = service.getCentrosSupervisados();

        assertEquals(List.of(centroMislata, centroBatoi), centros);
        verify(repository).findCentrosSupervisados(usuarioAutenticado);
    }

    @Test
    void getCentrosSupervisados_sinCentrosSupervisados_devuelveListaVacia() {
        when(repository.findCentrosSupervisados(usuarioAutenticado)).thenReturn(List.of());

        List<Centro> centros = assertDoesNotThrow(() -> service.getCentrosSupervisados());

        assertTrue(centros.isEmpty());
    }

    /* ------------------------------------------------------------------ */
    /* AllowProperties                                                    */
    /* ------------------------------------------------------------------ */

    @Test
    void allowPropertiesInsert_aceptaLosSeisCamposDeLaFila() {
        AllowProperties allowProperties = service.allowPropertiesInsert();

        CAMPOS_DE_LA_FILA.forEach(campo -> assertTrue(allowProperties.allowProperty(campo), campo));
    }

    @Test
    void allowPropertiesUpdate_excluyeCentroYAceptaElResto() {
        AllowProperties allowProperties = service.allowPropertiesUpdate();

        assertFalse(allowProperties.allowProperty("centro"));
        List.of("tramite", "perfil", "tipoUsuario", "cargo", "usuario")
                .forEach(campo -> assertTrue(allowProperties.allowProperty(campo), campo));
    }

    @Test
    void allowPropertiesRemove_noAceptaNingunCampo() {
        AllowProperties allowProperties = service.allowPropertiesRemove();

        CAMPOS_DE_LA_FILA.forEach(campo -> assertFalse(allowProperties.allowProperty(campo), campo));
    }
}
