package com.educaflow.subsystem.security.service.impl;

import com.axelor.auth.db.User;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.common.db.TipoUsuarioCodigo;
import com.educaflow.subsystem.expedientes.db.UnidadTramitadoraCodigo;
import com.educaflow.subsystem.notificaciones.util.GestorNotificacionesUtil;
import com.educaflow.subsystem.security.service.PerfilesUsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuSecurityServiceImplTest {

    @Mock
    PerfilesUsuarioService perfilesUsuarioService;

    @Mock
    User user;

    @InjectMocks
    MenuSecurityServiceImpl service;

    MockedStatic<SecurityUtil> securityUtil;

    MockedStatic<GestorNotificacionesUtil> gestorNotificacionesUtil;

    @BeforeEach
    void setUp() {
        securityUtil = Mockito.mockStatic(SecurityUtil.class);
        securityUtil.when(SecurityUtil::getUser).thenReturn(user);
        gestorNotificacionesUtil = Mockito.mockStatic(GestorNotificacionesUtil.class);
    }

    @AfterEach
    void tearDown() {
        securityUtil.close();
        gestorNotificacionesUtil.close();
    }

    private void usuario(boolean admin, boolean supervisor) {
        securityUtil.when(() -> SecurityUtil.isAdmin(user)).thenReturn(admin);
        lenient().when(user.tieneTipoUsuario(TipoUsuarioCodigo.SUPERVISOR)).thenReturn(supervisor);
    }

    @Test
    void sinUsuarioNoEsVisible() {
        securityUtil.when(SecurityUtil::getUser).thenReturn(null);

        assertFalse(service.isVisible("miCentro-menuitem"));
        assertFalse(service.isVisible("otro-menuitem"));
        verifyNoInteractions(perfilesUsuarioService);
    }

    @Test
    void menuNoControladoSiempreEsVisible() {
        usuario(false, false);

        assertTrue(service.isVisible("otro-menuitem"));
        verifyNoInteractions(perfilesUsuarioService);
    }

    @Test
    void menusDelCentroDependenDeSerSupervisor() {
        usuario(false, true);
        assertTrue(service.isVisible("firmas-delCentro-menuitem"));
        assertTrue(service.isVisible("miCentro-menuitem"));

        usuario(true, false);
        assertFalse(service.isVisible("firmas-delCentro-menuitem"));
        assertFalse(service.isVisible("miCentro-menuitem"));
    }

    @Test
    void tramitacionEsVisibleParaAdminSinConsultarPerfiles() {
        usuario(true, false);

        assertTrue(service.isVisible("tramitacion-menuitem"));
        verifyNoInteractions(perfilesUsuarioService);
    }

    @Test
    void tramitacionDependeDeSerTramitadorSiNoEsAdmin() {
        usuario(false, false);
        when(perfilesUsuarioService.isTramitador(user)).thenReturn(true, false);

        assertTrue(service.isVisible("tramitacion-menuitem"));
        assertFalse(service.isVisible("tramitacion-menuitem"));
    }

    @Test
    void tramitacionJefaturaDependeDeSerTramitadorDeJefatura() {
        usuario(true, true);
        when(perfilesUsuarioService.isTramitador(user, UnidadTramitadoraCodigo.JEFATURA_ESTUDIOS)).thenReturn(true, false);

        assertTrue(service.isVisible("tramitacion-jefaturaDeEstudios-menuitem"));
        assertFalse(service.isVisible("tramitacion-jefaturaDeEstudios-menuitem"));
    }

    @Test
    void tramitacionSecretariaDependeDeSerTramitadorDeSecretaria() {
        usuario(true, true);
        when(perfilesUsuarioService.isTramitador(user, UnidadTramitadoraCodigo.SECRETARIA)).thenReturn(true, false);

        assertTrue(service.isVisible("tramitacion-secretaria-menuitem"));
        assertFalse(service.isVisible("tramitacion-secretaria-menuitem"));
    }

    @Test
    void registroEsVisibleParaAdminSinConsultarPerfiles() {
        usuario(true, false);

        assertTrue(service.isVisible("registro-menuitem"));
        verifyNoInteractions(perfilesUsuarioService);
    }

    @Test
    void registroEsVisibleParaTramitadorDeSecretaria() {
        usuario(false, false);
        when(perfilesUsuarioService.isTramitador(user, UnidadTramitadoraCodigo.SECRETARIA)).thenReturn(true);

        assertTrue(service.isVisible("registro-menuitem"));
    }

    @Test
    void registroEsVisibleParaSupervisor() {
        usuario(false, true);
        when(perfilesUsuarioService.isTramitador(user, UnidadTramitadoraCodigo.SECRETARIA)).thenReturn(false);

        assertTrue(service.isVisible("registro-menuitem"));
    }

    @Test
    void registroNoEsVisibleSinNingunPermiso() {
        usuario(false, false);
        when(perfilesUsuarioService.isTramitador(user, UnidadTramitadoraCodigo.SECRETARIA)).thenReturn(false);

        assertFalse(service.isVisible("registro-menuitem"));
    }

    @Test
    void isVisible_notificacionesDelCentroUsuarioGestor_esVisible() {
        usuario(false, false);
        gestorNotificacionesUtil.when(() -> GestorNotificacionesUtil.esGestorEnAlgunCentro(user)).thenReturn(true);

        assertTrue(service.isVisible("notificaciones-delCentro-menuitem"));
    }

    @Test
    void isVisible_notificacionesDelCentroUsuarioNoGestorAunqueSeaAdminYSupervisor_noEsVisible() {
        usuario(true, true);
        gestorNotificacionesUtil.when(() -> GestorNotificacionesUtil.esGestorEnAlgunCentro(user)).thenReturn(false);

        assertFalse(service.isVisible("notificaciones-delCentro-menuitem"));
    }

    @Test
    void isVisible_notificacionesDelCentroSinUsuario_noEsVisibleNiPreguntaAlGestor() {
        securityUtil.when(SecurityUtil::getUser).thenReturn(null);

        assertFalse(service.isVisible("notificaciones-delCentro-menuitem"));
        gestorNotificacionesUtil.verifyNoInteractions();
    }
}
