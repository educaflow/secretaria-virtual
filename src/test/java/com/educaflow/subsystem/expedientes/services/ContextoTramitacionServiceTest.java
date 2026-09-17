package com.educaflow.subsystem.expedientes.services;

import com.axelor.auth.db.User;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.Cargo;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.common.db.CentroUsuarioCargo;
import com.educaflow.subsystem.common.db.CentroUsuarioTipoUsuario;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.subsystem.expedientes.db.TipoTramite;
import com.educaflow.subsystem.expedientes.db.Tramite;
import com.educaflow.subsystem.expedientes.services.internal.ExpedienteSecurity;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Cubre a la vez el servicio de la pantalla y la regla de {@link ExpedienteSecurity} en la que
 * delega: son la misma decisión vista desde los dos lados.
 */
class ContextoTramitacionServiceTest {

    private final ContextoTramitacionService servicio = new ContextoTramitacionService();

    private final Centro mislata = centro(1L, "CIPFP Mislata");
    private final Centro batoi = centro(2L, "CIPFP Batoi");

    /* ------------------------------------------------------------------ */
    /* Helpers                                                            */
    /* ------------------------------------------------------------------ */

    private static Centro centro(long id, String nombre) {
        Centro centro = new Centro();
        centro.setId(id);
        centro.setCode("C" + id);
        centro.setName(nombre);
        return centro;
    }

    /** Un trámite de alumnos: los interesados son los alumnos y el director (cargo). */
    private static Tramite tramite(boolean admiteRepresentacion) {
        TipoTramite tipoTramite = new TipoTramite();
        tipoTramite.setAdmiteRepresentacion(admiteRepresentacion);
        tipoTramite.setTiposUsuarioInteresados(tiposUsuario("ALUMNO"));
        tipoTramite.setCargosInteresados(cargos("DIRECTOR"));
        Tramite tramite = new Tramite();
        tramite.setCode("MiTramite");
        tramite.setTipoTramite(tipoTramite);
        return tramite;
    }

    /** Un trámite cuyo tipo admite la presentación en papel, registrada por administrativos y secretarios. */
    private static Tramite tramiteEnPapel(boolean admiteRepresentacion) {
        Tramite tramite = tramite(admiteRepresentacion);
        TipoTramite tipoTramite = tramite.getTipoTramite();
        tipoTramite.setAdmitePresentacionEnPapel(true);
        tipoTramite.setTiposUsuarioRegistradores(tiposUsuario("ADMINISTRATIVO"));
        tipoTramite.setCargosRegistradores(cargos("SECRETARIO"));
        return tramite;
    }

    private static Set<TipoUsuario> tiposUsuario(String... codigos) {
        return Set.of(codigos).stream().map(ContextoTramitacionServiceTest::tipoUsuario).collect(Collectors.toSet());
    }

    private static Set<Cargo> cargos(String... codigos) {
        return Set.of(codigos).stream().map(ContextoTramitacionServiceTest::cargo).collect(Collectors.toSet());
    }

    private static TipoUsuario tipoUsuario(String codigo) {
        TipoUsuario tipoUsuario = new TipoUsuario();
        tipoUsuario.setCodigo(codigo);
        return tipoUsuario;
    }

    private static Cargo cargo(String codigo) {
        Cargo cargo = new Cargo();
        cargo.setCode(codigo);
        return cargo;
    }

    private static User usuario() {
        User usuario = new User();
        usuario.setId(10L);
        usuario.setCentroUsuarios(new ArrayList<>());
        return usuario;
    }

    private static void enCentro(User usuario, Centro centro, String... tiposUsuario) {
        CentroUsuario centroUsuario = new CentroUsuario();
        centroUsuario.setCentro(centro);
        centroUsuario.setUsuario(usuario);
        List<CentroUsuarioTipoUsuario> tipos = new ArrayList<>();
        for (String codigo : tiposUsuario) {
            CentroUsuarioTipoUsuario centroUsuarioTipoUsuario = new CentroUsuarioTipoUsuario();
            centroUsuarioTipoUsuario.setCentroUsuario(centroUsuario);
            centroUsuarioTipoUsuario.setTipoUsuario(tipoUsuario(codigo));
            tipos.add(centroUsuarioTipoUsuario);
        }
        centroUsuario.setCentroUsuarioTipoUsuario(tipos);
        usuario.getCentroUsuarios().add(centroUsuario);
    }

    private static void conCargos(User usuario, Centro centro, String... cargos) {
        CentroUsuario centroUsuario = usuario.getCentroUsuarios().stream()
                .filter(cu -> Objects.equals(cu.getCentro(), centro))
                .findFirst()
                .orElseThrow();
        List<CentroUsuarioCargo> centroUsuarioCargos = new ArrayList<>();
        for (String codigo : cargos) {
            CentroUsuarioCargo centroUsuarioCargo = new CentroUsuarioCargo();
            centroUsuarioCargo.setCentroUsuario(centroUsuario);
            centroUsuarioCargo.setCargo(cargo(codigo));
            centroUsuarioCargos.add(centroUsuarioCargo);
        }
        centroUsuario.setCentroUsuarioCargo(centroUsuarioCargos);
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
    void getCentros_soloLosCentrosDondeEsInteresado() {
        Tramite tramite = tramite(false);
        User usuario = usuario();
        enCentro(usuario, mislata, "PROFESOR");
        enCentro(usuario, batoi, "ALUMNO");

        assertEquals(List.of(batoi), servicio.getCentros(tramite, usuario));
    }

    @Test
    void getCentros_variosCentros_ordenadosPorNombre() {
        Tramite tramite = tramite(false);
        User usuario = usuario();
        enCentro(usuario, mislata, "ALUMNO");
        enCentro(usuario, batoi, "ALUMNO");

        assertEquals(List.of(batoi, mislata), servicio.getCentros(tramite, usuario));
    }

    @Test
    void getCentros_usuarioSinCentros_ninguno() {
        assertEquals(List.of(), servicio.getCentros(tramite(false), usuario()));
    }

    @Test
    void getCentros_familiarEnTramiteSinRepresentacion_noAparece() {
        Tramite tramite = tramite(false);
        User usuario = usuario();
        enCentro(usuario, mislata, "FAMILIAR");
        enCentro(usuario, batoi, "ALUMNO");

        assertEquals(List.of(batoi), servicio.getCentros(tramite, usuario));
    }

    /* ------------------------------------------------------------------ */
    /* Para quién, presentando el propio usuario                          */
    /* ------------------------------------------------------------------ */

    @Test
    void paraQuien_alumno_soloParaMi() {
        Tramite tramite = tramite(true);
        User usuario = usuario();
        enCentro(usuario, mislata, "ALUMNO");

        assertOpciones(true, false, tramite, usuario, mislata, false);
    }

    @Test
    void paraQuien_familiarEnTramiteQueAdmiteRepresentacion_soloParaOtraPersona() {
        Tramite tramite = tramite(true);
        User usuario = usuario();
        enCentro(usuario, mislata, "FAMILIAR");

        assertOpciones(false, true, tramite, usuario, mislata, false);
    }

    @Test
    void paraQuien_alumnoYFamiliarEnElMismoCentro_lasDosOpciones() {
        Tramite tramite = tramite(true);
        User usuario = usuario();
        enCentro(usuario, mislata, "ALUMNO", "FAMILIAR");

        assertOpciones(true, true, tramite, usuario, mislata, false);
    }

    @Test
    void paraQuien_familiarEnTramiteSinRepresentacion_ninguna() {
        Tramite tramite = tramite(false);
        User usuario = usuario();
        enCentro(usuario, mislata, "FAMILIAR");

        assertOpciones(false, false, tramite, usuario, mislata, false);
    }

    @Test
    void paraQuien_losTiposDeOtroCentroNoCuentan() {
        Tramite tramite = tramite(true);
        User usuario = usuario();
        enCentro(usuario, mislata, "ALUMNO");
        enCentro(usuario, batoi, "FAMILIAR");

        assertOpciones(false, true, tramite, usuario, batoi, false);
    }

    @Test
    void paraQuien_cargoInteresado_paraMi() {
        Tramite tramite = tramite(true);
        User usuario = usuario();
        enCentro(usuario, mislata, "PROFESOR");
        conCargos(usuario, mislata, "DIRECTOR");

        assertOpciones(true, false, tramite, usuario, mislata, false);
    }

    @Test
    void paraQuien_tipoDeUsuarioQueNoEsInteresado_ninguna() {
        Tramite tramite = tramite(true);
        User usuario = usuario();
        enCentro(usuario, mislata, "EXALUMNO", "EXFAMILIAR");

        assertOpciones(false, false, tramite, usuario, mislata, false);
    }

    @Test
    void paraQuien_sinCentro_ninguna() {
        Tramite tramite = tramite(true);
        User usuario = usuario();
        enCentro(usuario, mislata, "ALUMNO");

        assertOpciones(false, false, tramite, usuario, null, false);
        assertOpciones(false, false, tramite, usuario, null, true);
    }

    @Test
    void paraQuien_tramiteSinTipoDeTramite_ninguna() {
        Tramite tramite = tramite(true);
        tramite.setTipoTramite(null);
        User usuario = usuario();
        enCentro(usuario, mislata, "ALUMNO", "FAMILIAR");

        assertOpciones(false, false, tramite, usuario, mislata, false);
        assertOpciones(false, false, tramite, usuario, mislata, true);
        assertEquals(List.of(), servicio.getCentros(tramite, usuario));
    }

    /* ------------------------------------------------------------------ */
    /* checkPuedeCrear                                                    */
    /* ------------------------------------------------------------------ */

    @Test
    void checkPuedeCrear_centroYOpcionPermitidos_noLanza() {
        Tramite tramite = tramite(true);
        User usuario = usuario();
        enCentro(usuario, mislata, "ALUMNO", "FAMILIAR");

        assertDoesNotThrow(() -> ExpedienteSecurity.checkPuedeCrear(tramite, usuario, mislata, false, true));
    }

    @Test
    void checkPuedeCrear_centroDondeNoEsInteresado_lanzaUnauthorized() {
        Tramite tramite = tramite(true);
        User usuario = usuario();
        enCentro(usuario, mislata, "ALUMNO");
        enCentro(usuario, batoi, "PROFESOR");

        assertThrows(UnauthorizedException.class,
                () -> ExpedienteSecurity.checkPuedeCrear(tramite, usuario, batoi, false, false));
    }

    @Test
    void checkPuedeCrear_centroAlQueNoPertenece_lanzaUnauthorized() {
        Tramite tramite = tramite(true);
        User usuario = usuario();
        enCentro(usuario, mislata, "ALUMNO");

        assertThrows(UnauthorizedException.class,
                () -> ExpedienteSecurity.checkPuedeCrear(tramite, usuario, batoi, false, false));
    }

    @Test
    void checkPuedeCrear_opcionQueNoLeCorresponde_lanzaUnauthorized() {
        Tramite tramite = tramite(true);
        User usuario = usuario();
        enCentro(usuario, mislata, "ALUMNO");

        assertThrows(UnauthorizedException.class,
                () -> ExpedienteSecurity.checkPuedeCrear(tramite, usuario, mislata, false, true));
    }

    /* ------------------------------------------------------------------ */
    /* Presentación en papel                                              */
    /* ------------------------------------------------------------------ */

    @Test
    void enPapel_registradorQueNoEsInteresadoNiFamiliar_soloRegistraEnPapel() {
        Tramite tramite = tramiteEnPapel(true);
        User usuario = usuario();
        enCentro(usuario, mislata, "ADMINISTRATIVO");

        assertEquals(List.of(mislata), servicio.getCentros(tramite, usuario));
        assertFalse(servicio.puedePresentarElUsuario(tramite, usuario, mislata));
        assertTrue(servicio.puedeRegistrarEnPapel(tramite, usuario, mislata));
        assertOpciones(true, true, tramite, usuario, mislata, true);
        assertOpciones(false, false, tramite, usuario, mislata, false);
    }

    @Test
    void enPapel_cargoRegistrador_registraEnPapel() {
        Tramite tramite = tramiteEnPapel(true);
        User usuario = usuario();
        enCentro(usuario, mislata, "PROFESOR");
        conCargos(usuario, mislata, "SECRETARIO");

        assertTrue(servicio.puedeRegistrarEnPapel(tramite, usuario, mislata));
    }

    @Test
    void enPapel_ni_tipoDeUsuario_ni_cargoRegistrador_noRegistra() {
        Tramite tramite = tramiteEnPapel(true);
        User usuario = usuario();
        enCentro(usuario, mislata, "PROFESOR");
        conCargos(usuario, mislata, "JEFE_ESTUDIOS");

        assertFalse(servicio.puedeRegistrarEnPapel(tramite, usuario, mislata));
        assertEquals(List.of(), servicio.getCentros(tramite, usuario));
    }

    @Test
    void enPapel_tipoDeTramiteQueNoLaAdmite_noRegistraAunqueSeaRegistrador() {
        Tramite tramite = tramiteEnPapel(true);
        tramite.getTipoTramite().setAdmitePresentacionEnPapel(false);
        User usuario = usuario();
        enCentro(usuario, mislata, "ADMINISTRATIVO");

        assertFalse(servicio.puedeRegistrarEnPapel(tramite, usuario, mislata));
    }

    @Test
    void enPapel_losTiposYCargosDeOtroCentroNoCuentan() {
        Tramite tramite = tramiteEnPapel(true);
        User usuario = usuario();
        enCentro(usuario, mislata, "ADMINISTRATIVO");
        enCentro(usuario, batoi, "PROFESOR");
        conCargos(usuario, mislata, "SECRETARIO");

        assertTrue(servicio.puedeRegistrarEnPapel(tramite, usuario, mislata));
        assertFalse(servicio.puedeRegistrarEnPapel(tramite, usuario, batoi));
    }

    @Test
    void enPapel_tipoDeTramiteSinRepresentacion_soloParaMi() {
        Tramite tramite = tramiteEnPapel(false);
        User usuario = usuario();
        enCentro(usuario, mislata, "ADMINISTRATIVO");

        assertOpciones(true, false, tramite, usuario, mislata, true);
    }

    @Test
    void enPapel_administrativaQueEsAlumnaYMadre_puedeLasCuatroOpciones() {
        Tramite tramite = tramiteEnPapel(true);
        User usuario = usuario();
        enCentro(usuario, mislata, "ADMINISTRATIVO", "ALUMNO", "FAMILIAR");

        assertTrue(servicio.puedePresentarElUsuario(tramite, usuario, mislata));
        assertTrue(servicio.puedeRegistrarEnPapel(tramite, usuario, mislata));
        assertOpciones(true, true, tramite, usuario, mislata, false);
        assertOpciones(true, true, tramite, usuario, mislata, true);
    }

    @Test
    void enPapel_administrativaQueEsMadrePeroNoAlumna_sinPapelSoloEnRepresentacion() {
        Tramite tramite = tramiteEnPapel(true);
        User usuario = usuario();
        enCentro(usuario, mislata, "ADMINISTRATIVO", "FAMILIAR");

        assertTrue(servicio.puedeRegistrarEnPapel(tramite, usuario, mislata));
        assertOpciones(false, true, tramite, usuario, mislata, false);
    }

    @Test
    void checkPuedeCrear_enPapelSiendoRegistrador_noLanza() {
        Tramite tramite = tramiteEnPapel(true);
        User usuario = usuario();
        enCentro(usuario, mislata, "ADMINISTRATIVO");

        assertDoesNotThrow(() -> ExpedienteSecurity.checkPuedeCrear(tramite, usuario, mislata, true, true));
    }

    @Test
    void checkPuedeCrear_enPapelSinSerRegistrador_lanzaUnauthorized() {
        Tramite tramite = tramiteEnPapel(true);
        User usuario = usuario();
        enCentro(usuario, mislata, "ALUMNO");

        assertThrows(UnauthorizedException.class,
                () -> ExpedienteSecurity.checkPuedeCrear(tramite, usuario, mislata, true, false));
    }

    @Test
    void checkPuedeCrear_registradorQuePresentaPorSiMismo_lanzaUnauthorized() {
        Tramite tramite = tramiteEnPapel(true);
        User usuario = usuario();
        enCentro(usuario, mislata, "ADMINISTRATIVO");

        assertThrows(UnauthorizedException.class,
                () -> ExpedienteSecurity.checkPuedeCrear(tramite, usuario, mislata, false, false));
    }

}
