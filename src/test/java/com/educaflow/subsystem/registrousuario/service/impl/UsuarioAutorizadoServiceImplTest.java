package com.educaflow.subsystem.registrousuario.service.impl;

import com.axelor.auth.db.User;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.TipoUsuarioCodigo;
import com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UsuarioAutorizadoServiceImplTest {

    private UsuarioAutorizadoServiceImpl service;
    private MockedStatic<SecurityUtil> securityUtilMock;
    private User usuario;
    private Centro centro;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new UsuarioAutorizadoServiceImpl(UsuarioAutorizado.class, Mockito.mock(Repository.class));
        usuario = mock(User.class);
        centro = new Centro();
        centro.setId(1L);
        securityUtilMock = Mockito.mockStatic(SecurityUtil.class);
        securityUtilMock.when(SecurityUtil::getUser).thenReturn(usuario);
        securityUtilMock.when(() -> SecurityUtil.isAdmin(usuario)).thenReturn(false);
    }

    @AfterEach
    void tearDown() {
        securityUtilMock.close();
    }

    private UsuarioAutorizado usuarioAutorizado(Centro centroDelUsuarioAutorizado) {
        UsuarioAutorizado usuarioAutorizado = new UsuarioAutorizado();
        usuarioAutorizado.setCentro(centroDelUsuarioAutorizado);
        usuarioAutorizado.setDni("12345678Z");
        return usuarioAutorizado;
    }

    private void stubSupervisorDelCentro(boolean esSupervisor) {
        when(usuario.tieneTipoUsuario(centro, TipoUsuarioCodigo.SUPERVISOR)).thenReturn(esSupervisor);
    }

    @Test
    void validateInsert_supervisorDelCentro_esValido() {
        stubSupervisorDelCentro(true);

        assertTrue(service.validateInsert(usuarioAutorizado(centro)).isEmpty());
    }

    @Test
    void validateInsert_adminSinSerSupervisor_esValido() {
        stubSupervisorDelCentro(false);
        securityUtilMock.when(() -> SecurityUtil.isAdmin(usuario)).thenReturn(true);

        assertTrue(service.validateInsert(usuarioAutorizado(centro)).isEmpty());
    }

    @Test
    void validateInsert_centroQueNoSupervisa_rechaza() {
        stubSupervisorDelCentro(false);

        assertTrue(service.validateInsert(usuarioAutorizado(centro)).isPresent());
    }

    @Test
    void validateInsert_sinCentro_rechaza() {
        assertTrue(service.validateInsert(usuarioAutorizado(null)).isPresent());
    }

    @Test
    void validateInsert_sinUsuarioAutenticado_rechaza() {
        securityUtilMock.when(SecurityUtil::getUser).thenReturn(null);

        assertTrue(service.validateInsert(usuarioAutorizado(centro)).isPresent());
    }

    @Test
    void validateUpdate_compruebaElCentroDelOriginal() {
        stubSupervisorDelCentro(false);
        Centro otroCentro = new Centro();
        otroCentro.setId(2L);
        when(usuario.tieneTipoUsuario(otroCentro, TipoUsuarioCodigo.SUPERVISOR)).thenReturn(true);

        assertTrue(service.validateUpdate(usuarioAutorizado(otroCentro), usuarioAutorizado(centro)).isPresent());
    }

    @Test
    void validateUpdate_supervisorDelCentroDelOriginal_esValido() {
        stubSupervisorDelCentro(true);

        assertTrue(service.validateUpdate(usuarioAutorizado(centro), usuarioAutorizado(centro)).isEmpty());
    }

    @Test
    void validateRemove_centroQueNoSupervisa_rechaza() {
        stubSupervisorDelCentro(false);

        assertTrue(service.validateRemove(usuarioAutorizado(centro)).isPresent());
    }

    @Test
    void validateRemove_supervisorDelCentro_esValido() {
        stubSupervisorDelCentro(true);

        assertTrue(service.validateRemove(usuarioAutorizado(centro)).isEmpty());
    }

    @Test
    void allowPropertiesInsert_admiteLosCamposDelFormularioYNoLaFechaDeExportacion() {
        AllowProperties allowProperties = service.allowPropertiesInsert();

        assertTrue(allowProperties.allowProperty("centro"));
        assertTrue(allowProperties.allowProperty("curso"));
        assertTrue(allowProperties.allowProperty("dni"));
        assertTrue(allowProperties.allowProperty("tipoUsuario"));
        assertFalse(allowProperties.allowProperty("fechaExportacion"));
    }

    @Test
    void allowPropertiesUpdate_noAdmiteCentroNiCursoNiFechaDeExportacion() {
        AllowProperties allowProperties = service.allowPropertiesUpdate();

        assertTrue(allowProperties.allowProperty("dni"));
        assertTrue(allowProperties.allowProperty("tipoUsuario"));
        assertFalse(allowProperties.allowProperty("centro"));
        assertFalse(allowProperties.allowProperty("curso"));
        assertFalse(allowProperties.allowProperty("fechaExportacion"));
    }
}
