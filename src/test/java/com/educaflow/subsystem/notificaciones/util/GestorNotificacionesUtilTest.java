package com.educaflow.subsystem.notificaciones.util;

import com.axelor.auth.db.User;
import com.educaflow.subsystem.common.db.Cargo;
import com.educaflow.subsystem.common.db.CargoCodigo;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.common.db.CentroUsuarioCargo;
import com.educaflow.subsystem.common.db.CentroUsuarioTipoUsuario;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.subsystem.common.db.TipoUsuarioCodigo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GestorNotificacionesUtilTest {

    private Centro centroA;
    private Centro centroB;

    @BeforeEach
    void setUp() {
        centroA = new Centro();
        centroA.setId(1L);
        centroB = new Centro();
        centroB.setId(2L);
    }

    private static User usuario(CentroUsuario... centroUsuarios) {
        User usuario = new User();
        usuario.setCentroUsuarios(List.of(centroUsuarios));
        return usuario;
    }

    private static CentroUsuario centroUsuario(Centro centro) {
        CentroUsuario centroUsuario = new CentroUsuario();
        centroUsuario.setCentro(centro);
        return centroUsuario;
    }

    private static CentroUsuario conTipo(CentroUsuario centroUsuario, TipoUsuarioCodigo codigo) {
        TipoUsuario tipoUsuario = new TipoUsuario();
        tipoUsuario.setCodigo(codigo);
        CentroUsuarioTipoUsuario centroUsuarioTipoUsuario = new CentroUsuarioTipoUsuario();
        centroUsuarioTipoUsuario.setTipoUsuario(tipoUsuario);
        centroUsuario.addCentroUsuarioTipoUsuario(centroUsuarioTipoUsuario);
        return centroUsuario;
    }

    private static CentroUsuario conCargo(CentroUsuario centroUsuario, CargoCodigo codigo) {
        Cargo cargo = new Cargo();
        cargo.setCode(codigo);
        CentroUsuarioCargo centroUsuarioCargo = new CentroUsuarioCargo();
        centroUsuarioCargo.setCargo(cargo);
        centroUsuario.addCentroUsuarioCargo(centroUsuarioCargo);
        return centroUsuario;
    }

    /*************************************** esGestorEnAlgunCentro ***************************************/

    @Test
    void esGestorEnAlgunCentro_supervisorDeUnCentro_devuelveTrue() {
        User usuario = usuario(conTipo(centroUsuario(centroA), TipoUsuarioCodigo.SUPERVISOR));

        assertTrue(GestorNotificacionesUtil.esGestorEnAlgunCentro(usuario));
    }

    @Test
    void esGestorEnAlgunCentro_administrativoDeUnCentro_devuelveTrue() {
        User usuario = usuario(conTipo(centroUsuario(centroA), TipoUsuarioCodigo.ADMINISTRATIVO));

        assertTrue(GestorNotificacionesUtil.esGestorEnAlgunCentro(usuario));
    }

    @Test
    void esGestorEnAlgunCentro_directorDeUnCentro_devuelveTrue() {
        User usuario = usuario(conCargo(centroUsuario(centroA), CargoCodigo.DIRECTOR));

        assertTrue(GestorNotificacionesUtil.esGestorEnAlgunCentro(usuario));
    }

    @Test
    void esGestorEnAlgunCentro_jefeDeEstudios_devuelveTrue() {
        User usuario = usuario(conCargo(centroUsuario(centroA), CargoCodigo.JEFE_ESTUDIOS));

        assertTrue(GestorNotificacionesUtil.esGestorEnAlgunCentro(usuario));
    }

    @Test
    void esGestorEnAlgunCentro_secretario_devuelveTrue() {
        User usuario = usuario(conCargo(centroUsuario(centroA), CargoCodigo.SECRETARIO));

        assertTrue(GestorNotificacionesUtil.esGestorEnAlgunCentro(usuario));
    }

    @Test
    void esGestorEnAlgunCentro_vicesecretario_devuelveFalse() {
        User usuario = usuario(conCargo(centroUsuario(centroA), CargoCodigo.VICESECRETARIO));

        assertFalse(GestorNotificacionesUtil.esGestorEnAlgunCentro(usuario));
    }

    @Test
    void esGestorEnAlgunCentro_profesorSinCargo_devuelveFalse() {
        User usuario = usuario(conTipo(centroUsuario(centroA), TipoUsuarioCodigo.PROFESOR));

        assertFalse(GestorNotificacionesUtil.esGestorEnAlgunCentro(usuario));
    }

    @Test
    void esGestorEnAlgunCentro_gestorSoloEnElSegundoCentro_devuelveTrue() {
        User usuario = usuario(
                conTipo(centroUsuario(centroA), TipoUsuarioCodigo.PROFESOR),
                conTipo(centroUsuario(centroB), TipoUsuarioCodigo.SUPERVISOR));

        assertTrue(GestorNotificacionesUtil.esGestorEnAlgunCentro(usuario));
    }

    @Test
    void esGestorEnAlgunCentro_centroUsuariosNull_devuelveFalse() {
        User usuario = new User();

        assertFalse(GestorNotificacionesUtil.esGestorEnAlgunCentro(usuario));
    }

    @Test
    void esGestorEnAlgunCentro_centroUsuariosVacio_devuelveFalse() {
        User usuario = usuario();

        assertFalse(GestorNotificacionesUtil.esGestorEnAlgunCentro(usuario));
    }

    @Test
    void esGestorEnAlgunCentro_coleccionesDeTiposYCargosNullOConElementosNull_seIgnoranSinExcepcion() {
        CentroUsuario sinColecciones = centroUsuario(centroA);
        sinColecciones.setCentroUsuarioTipoUsuario(null);
        sinColecciones.setCentroUsuarioCargo(null);
        CentroUsuario conElementosNull = centroUsuario(centroB);
        conElementosNull.addCentroUsuarioTipoUsuario(new CentroUsuarioTipoUsuario());
        conElementosNull.addCentroUsuarioCargo(new CentroUsuarioCargo());
        User usuario = usuario(sinColecciones, conElementosNull);

        boolean esGestor = assertDoesNotThrow(() -> GestorNotificacionesUtil.esGestorEnAlgunCentro(usuario));

        assertFalse(esGestor);
    }

    /*************************************** idsCentrosGestionados ***************************************/

    @Test
    void idsCentrosGestionados_gestorDeDosCentros_devuelveAmbosIds() {
        User usuario = usuario(
                conTipo(centroUsuario(centroA), TipoUsuarioCodigo.SUPERVISOR),
                conCargo(centroUsuario(centroB), CargoCodigo.DIRECTOR));

        List<Long> ids = GestorNotificacionesUtil.idsCentrosGestionados(usuario);

        assertEquals(List.of(1L, 2L), ids.stream().sorted().toList());
    }

    @Test
    void idsCentrosGestionados_gestorEnUnoYProfesorEnOtro_devuelveSoloElGestionado() {
        User usuario = usuario(
                conTipo(centroUsuario(centroA), TipoUsuarioCodigo.ADMINISTRATIVO),
                conTipo(centroUsuario(centroB), TipoUsuarioCodigo.PROFESOR));

        List<Long> ids = GestorNotificacionesUtil.idsCentrosGestionados(usuario);

        assertEquals(List.of(1L), ids);
    }

    @Test
    void idsCentrosGestionados_mismoCentroEnDosCentroUsuario_noRepiteElId() {
        User usuario = usuario(
                conTipo(centroUsuario(centroA), TipoUsuarioCodigo.SUPERVISOR),
                conCargo(centroUsuario(centroA), CargoCodigo.SECRETARIO));

        List<Long> ids = GestorNotificacionesUtil.idsCentrosGestionados(usuario);

        assertEquals(List.of(1L), ids);
    }

    @Test
    void idsCentrosGestionados_noGestionaNinguno_devuelveCentinelaMenosUno() {
        User usuario = usuario(conCargo(conTipo(centroUsuario(centroA), TipoUsuarioCodigo.PROFESOR), CargoCodigo.VICESECRETARIO));

        List<Long> ids = GestorNotificacionesUtil.idsCentrosGestionados(usuario);

        assertEquals(List.of(-1L), ids);
    }

    @Test
    void idsCentrosGestionados_usuarioNull_devuelveCentinelaMenosUno() {
        List<Long> ids = assertDoesNotThrow(() -> GestorNotificacionesUtil.idsCentrosGestionados(null));

        assertEquals(List.of(-1L), ids);
    }

    @Test
    void idsCentrosGestionados_centroUsuariosNull_devuelveCentinelaMenosUno() {
        User usuario = new User();

        List<Long> ids = GestorNotificacionesUtil.idsCentrosGestionados(usuario);

        assertEquals(List.of(-1L), ids);
    }
}
