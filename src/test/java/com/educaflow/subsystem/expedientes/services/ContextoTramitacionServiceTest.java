package com.educaflow.subsystem.expedientes.services;

import com.axelor.auth.db.User;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.subsystem.expedientes.db.Tramite;
import com.educaflow.subsystem.security.service.PerfilesUsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * El servicio no decide por sí mismo qué perfiles tiene el usuario: eso lo contesta
 * {@link PerfilesUsuarioService} contra las filas de {@code Ace}. Lo que aquí se comprueba es lo que
 * el servicio sí decide: qué centros se ofrecen y qué combinaciones de «en papel» / «en
 * representación» caben con los perfiles que tiene en cada centro.
 */
@ExtendWith(MockitoExtension.class)
class ContextoTramitacionServiceTest {

    private final PerfilesUsuarioService perfilesUsuarioService = mock(PerfilesUsuarioService.class);

    private final ContextoTramitacionService servicio = new ContextoTramitacionService();

    private final Centro mislata = centro(1L, "CIPFP Mislata");
    private final Centro batoi = centro(2L, "CIPFP Batoi");

    @BeforeEach
    void inyectarDependencias() throws Exception {
        setField(servicio, "perfilesUsuarioService", perfilesUsuarioService);
    }

    /* ------------------------------------------------------------------ */
    /* Helpers                                                            */
    /* ------------------------------------------------------------------ */

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = ContextoTramitacionService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Centro centro(long id, String nombre) {
        Centro centro = new Centro();
        centro.setId(id);
        centro.setCode("C" + id);
        centro.setName(nombre);
        return centro;
    }

    private static Tramite tramite(boolean permitidoPresentarEnRepresentacion) {
        Tramite tramite = new Tramite();
        tramite.setCode("MiTramite");
        tramite.setPermitidoPresentarEnRepresentacion(permitidoPresentarEnRepresentacion);
        return tramite;
    }

    private static User usuario() {
        User usuario = new User();
        usuario.setId(10L);
        usuario.setCentroUsuarios(new ArrayList<>());
        return usuario;
    }

    /** Da de alta al usuario en el centro, sin decir nada todavía de sus perfiles. */
    private static void enCentro(User usuario, Centro centro) {
        CentroUsuario centroUsuario = new CentroUsuario();
        centroUsuario.setCentro(centro);
        centroUsuario.setUsuario(usuario);
        usuario.getCentroUsuarios().add(centroUsuario);
    }

    /** Los perfiles que el usuario tiene sobre el trámite en ese centro. */
    private void conPerfiles(Tramite tramite, User usuario, Centro centro, Profile... perfiles) {
        when(perfilesUsuarioService.getPerfilesSobreTramite(tramite, usuario, centro)).thenReturn(Set.of(perfiles));
    }

    /** Las dos opciones de «¿para quién?» que quedan en ese centro presentando de esa forma. */
    private void assertOpciones(boolean paraMi, boolean paraOtraPersona, Tramite tramite, User usuario, Centro centro, boolean presentadoEnPapel) {
        assertEquals(paraMi, servicio.puedeCrear(tramite, usuario, centro, presentadoEnPapel, false), "para mí");
        assertEquals(paraOtraPersona, servicio.puedeCrear(tramite, usuario, centro, presentadoEnPapel, true), "para otra persona");
    }

    /* ------------------------------------------------------------------ */
    /* getCentros                                                         */
    /* ------------------------------------------------------------------ */

    @Test
    void getCentros_soloLosCentrosDondeTienePerfilParaCrear() {
        Tramite tramite = tramite(false);
        User usuario = usuario();
        enCentro(usuario, mislata);
        enCentro(usuario, batoi);
        conPerfiles(tramite, usuario, mislata, Profile.AFECTADO);
        conPerfiles(tramite, usuario, batoi, Profile.CREADOR);

        assertEquals(List.of(batoi), servicio.getCentros(tramite, usuario));
    }

    @Test
    void getCentros_elTramitadorTambienCrea() {
        Tramite tramite = tramite(false);
        User usuario = usuario();
        enCentro(usuario, mislata);
        conPerfiles(tramite, usuario, mislata, Profile.TRAMITADOR);

        assertEquals(List.of(mislata), servicio.getCentros(tramite, usuario));
    }

    @Test
    void getCentros_variosCentros_ordenadosPorNombre() {
        Tramite tramite = tramite(false);
        User usuario = usuario();
        enCentro(usuario, mislata);
        enCentro(usuario, batoi);
        conPerfiles(tramite, usuario, mislata, Profile.CREADOR);
        conPerfiles(tramite, usuario, batoi, Profile.CREADOR);

        assertEquals(List.of(batoi, mislata), servicio.getCentros(tramite, usuario));
    }

    @Test
    void getCentros_usuarioSinCentros_ninguno() {
        assertEquals(List.of(), servicio.getCentros(tramite(false), usuario()));
    }

    @Test
    void getCentros_sinTramite_ninguno() {
        User usuario = usuario();
        enCentro(usuario, mislata);

        assertEquals(List.of(), servicio.getCentros(null, usuario));
    }

    @Test
    void getCentros_sinUsuario_ninguno() {
        assertEquals(List.of(), servicio.getCentros(tramite(false), null));
    }

    @Test
    void getCentros_sinPerfilEnNingunCentro_ninguno() {
        Tramite tramite = tramite(false);
        User usuario = usuario();
        enCentro(usuario, mislata);
        conPerfiles(tramite, usuario, mislata);

        assertEquals(List.of(), servicio.getCentros(tramite, usuario));
    }

    /* ------------------------------------------------------------------ */
    /* Presentar en representación                                        */
    /* ------------------------------------------------------------------ */

    @Test
    void permitidoPresentarEnRepresentacion_loDiceElTramite() {
        assertTrue(servicio.permitidoPresentarEnRepresentacion(tramite(true)));
        assertFalse(servicio.permitidoPresentarEnRepresentacion(tramite(false)));
    }

    /* ------------------------------------------------------------------ */
    /* El perfil con el que se actúa                                      */
    /* ------------------------------------------------------------------ */

    @Test
    void getProfile_enPapelTramitador_sinPapelCreador() {
        assertEquals(Profile.TRAMITADOR, servicio.getProfile(true));
        assertEquals(Profile.CREADOR, servicio.getProfile(false));
    }

    /* ------------------------------------------------------------------ */
    /* Cuándo hay que preguntar cómo presenta                             */
    /* ------------------------------------------------------------------ */

    @Test
    void presentadoEnPapel_soloCreador_seDeduceFalseSinPreguntar() {
        Tramite tramite = tramite(true);
        User usuario = usuario();
        conPerfiles(tramite, usuario, mislata, Profile.CREADOR);

        assertFalse(servicio.esNecesarioPresentadoEnPapel(tramite, usuario, mislata));
        assertFalse(servicio.deducirPresentadoEnPapel(tramite, usuario, mislata));
    }

    @Test
    void presentadoEnPapel_soloTramitador_seDeduceTrueSinPreguntar() {
        Tramite tramite = tramite(true);
        User usuario = usuario();
        conPerfiles(tramite, usuario, mislata, Profile.TRAMITADOR);

        assertFalse(servicio.esNecesarioPresentadoEnPapel(tramite, usuario, mislata));
        assertTrue(servicio.deducirPresentadoEnPapel(tramite, usuario, mislata));
    }

    @Test
    void presentadoEnPapel_creadorYTramitador_hayQuePreguntar() {
        Tramite tramite = tramite(true);
        User usuario = usuario();
        conPerfiles(tramite, usuario, mislata, Profile.CREADOR, Profile.TRAMITADOR);

        assertTrue(servicio.esNecesarioPresentadoEnPapel(tramite, usuario, mislata));
    }

    @Test
    void presentadoEnPapel_sinNingunoDeLosDosPerfiles_noSePregunta() {
        Tramite tramite = tramite(true);
        User usuario = usuario();
        conPerfiles(tramite, usuario, mislata, Profile.AFECTADO);

        assertFalse(servicio.esNecesarioPresentadoEnPapel(tramite, usuario, mislata));
        assertFalse(servicio.deducirPresentadoEnPapel(tramite, usuario, mislata));
    }

    @Test
    void presentadoEnPapel_losPerfilesDeOtroCentroNoCuentan() {
        Tramite tramite = tramite(true);
        User usuario = usuario();
        conPerfiles(tramite, usuario, mislata, Profile.CREADOR, Profile.TRAMITADOR);
        conPerfiles(tramite, usuario, batoi, Profile.TRAMITADOR);

        assertTrue(servicio.esNecesarioPresentadoEnPapel(tramite, usuario, mislata));
        assertFalse(servicio.esNecesarioPresentadoEnPapel(tramite, usuario, batoi));
    }

    /* ------------------------------------------------------------------ */
    /* puedeCrear                                                         */
    /* ------------------------------------------------------------------ */

    @Test
    void puedeCrear_soloCreador_presentaTelematicamentePeroNoEnPapel() {
        Tramite tramite = tramite(true);
        User usuario = usuario();
        conPerfiles(tramite, usuario, mislata, Profile.CREADOR);

        assertOpciones(true, true, tramite, usuario, mislata, false);
        assertOpciones(false, false, tramite, usuario, mislata, true);
    }

    @Test
    void puedeCrear_soloTramitador_registraEnPapelPeroNoPresentaTelematicamente() {
        Tramite tramite = tramite(true);
        User usuario = usuario();
        conPerfiles(tramite, usuario, mislata, Profile.TRAMITADOR);

        assertOpciones(false, false, tramite, usuario, mislata, false);
        assertOpciones(true, true, tramite, usuario, mislata, true);
    }

    @Test
    void puedeCrear_creadorYTramitador_lasDosFormas() {
        Tramite tramite = tramite(true);
        User usuario = usuario();
        conPerfiles(tramite, usuario, mislata, Profile.CREADOR, Profile.TRAMITADOR);

        assertOpciones(true, true, tramite, usuario, mislata, false);
        assertOpciones(true, true, tramite, usuario, mislata, true);
    }

    @Test
    void puedeCrear_tramiteSinRepresentacion_soloParaMi() {
        Tramite tramite = tramite(false);
        User usuario = usuario();
        conPerfiles(tramite, usuario, mislata, Profile.CREADOR, Profile.TRAMITADOR);

        assertOpciones(true, false, tramite, usuario, mislata, false);
        assertOpciones(true, false, tramite, usuario, mislata, true);
    }

    @Test
    void puedeCrear_sinNingunoDeLosDosPerfiles_ninguna() {
        Tramite tramite = tramite(true);
        User usuario = usuario();
        conPerfiles(tramite, usuario, mislata, Profile.AFECTADO);

        assertOpciones(false, false, tramite, usuario, mislata, false);
        assertOpciones(false, false, tramite, usuario, mislata, true);
    }

    @Test
    void puedeCrear_sinCentro_ninguna() {
        Tramite tramite = tramite(true);
        User usuario = usuario();
        conPerfiles(tramite, usuario, null);

        assertOpciones(false, false, tramite, usuario, null, false);
        assertOpciones(false, false, tramite, usuario, null, true);
    }

}
