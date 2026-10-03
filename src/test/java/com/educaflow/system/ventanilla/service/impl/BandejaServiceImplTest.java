package com.educaflow.system.ventanilla.service.impl;

import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.subsystem.security.service.PerfilesUsuarioService;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BandejaServiceImplTest {

    private PerfilesUsuarioService perfilesUsuarioService;
    private BandejaServiceImpl service;
    private MockedStatic<SecurityUtil> securityUtilMock;
    private MockedStatic<JPA> jpaMock;
    private Query<Expediente> query;
    private User user;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        perfilesUsuarioService = mock(PerfilesUsuarioService.class);
        service = new BandejaServiceImpl(perfilesUsuarioService);
        user = new User();

        securityUtilMock = Mockito.mockStatic(SecurityUtil.class);
        securityUtilMock.when(() -> SecurityUtil.isAdmin(any())).thenReturn(false);

        query = mock(Query.class);
        when(query.filter(anyString())).thenReturn(query);
        when(query.bind(anyString(), any())).thenReturn(query);
        jpaMock = Mockito.mockStatic(JPA.class);
        jpaMock.when(() -> JPA.all(Expediente.class)).thenReturn(query);
    }

    @AfterEach
    void tearDown() {
        jpaMock.close();
        securityUtilMock.close();
    }

    private static Expediente expediente(long id, Profile perfilEstado) {
        Expediente expediente = new Expediente();
        expediente.setId(id);
        expediente.setPerfilEstado(perfilEstado);
        return expediente;
    }

    private void candidatos(Expediente... expedientes) {
        when(query.fetch()).thenReturn(Arrays.asList(expedientes));
    }

    @Test
    void idsPendientesDeMi_devuelveSoloLosExpedientesCuyoPerfilDelEstadoOstentaElUsuario() {
        Expediente mio = expediente(1L, Profile.TRAMITADOR);
        Expediente ajeno = expediente(2L, Profile.TRAMITADOR);
        candidatos(mio, ajeno);
        when(perfilesUsuarioService.getPerfilesSobreExpediente(mio, user)).thenReturn(Set.of(mio.getPerfilEstado()));
        when(perfilesUsuarioService.getPerfilesSobreExpediente(ajeno, user)).thenReturn(Set.of());

        List<Long> ids = service.idsPendientesDeMi(user);

        assertEquals(List.of(mio.getId()), ids);
    }

    @Test
    void idsPendientesDeMi_sinCandidatos_devuelveListaVacia() {
        candidatos();

        assertTrue(service.idsPendientesDeMi(user).isEmpty());
    }

    @Test
    void idsPendientesDeMi_acotaPorCreadorYPorUsuario() {
        candidatos();

        service.idsPendientesDeMi(user);

        verify(query).bind("creador", Profile.CREADOR);
        verify(query).bind("usuario", user);
    }

    @Test
    void perfilConElQueAbrir_administrador_abreConElPerfilDelEstadoSinPreguntarAlServicio() {
        securityUtilMock.when(() -> SecurityUtil.isAdmin(user)).thenReturn(true);
        Expediente expediente = expediente(1L, Profile.TRAMITADOR);

        Profile perfil = service.perfilConElQueAbrir(expediente, user);

        assertEquals(expediente.getPerfilEstado(), perfil);
        verifyNoInteractions(perfilesUsuarioService);
    }

    @Test
    void perfilConElQueAbrir_noAdministradorSinElPerfilDelEstado_abreConElDeMayorPrioridadDelServicioDePerfiles() {
        Expediente expediente = expediente(1L, Profile.TRAMITADOR);
        Set<Profile> delServicio = Set.of(Profile.CREADOR);
        when(perfilesUsuarioService.getPerfilesSobreExpediente(expediente, user)).thenReturn(delServicio);

        Profile perfil = service.perfilConElQueAbrir(expediente, user);

        assertEquals(delServicio.iterator().next(), perfil);
    }

    @Test
    void perfilConElQueAbrir_sinNingunPerfil_lanzaUnauthorizedException() {
        Expediente expediente = expediente(1L, Profile.TRAMITADOR);
        when(perfilesUsuarioService.getPerfilesSobreExpediente(expediente, user)).thenReturn(Set.of());

        assertThrows(UnauthorizedException.class, () -> service.perfilConElQueAbrir(expediente, user));
    }
}
