package com.educaflow.subsystem.security.service.impl;

import com.axelor.auth.db.User;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.subsystem.expedientes.db.TipoExpediente;
import com.educaflow.subsystem.expedientes.db.Tramite;
import com.educaflow.subsystem.security.db.repo.AceProfileCentroRepository;
import com.educaflow.subsystem.security.db.repo.AceProfileExpedienteRepository;
import com.educaflow.subsystem.security.db.repo.AceProfileGlobalRepository;
import com.educaflow.subsystem.security.db.repo.AceProfileTipoExpedienteRepository;
import com.educaflow.subsystem.security.db.repo.AceProfileTipoUsuarioTramiteRepository;
import com.educaflow.subsystem.security.db.repo.AceProfileTramiteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerfilesUsuarioServiceImplTest {

    @Mock
    AceProfileGlobalRepository aceProfileGlobalRepository;

    @Mock
    AceProfileTipoUsuarioTramiteRepository aceProfileTipoUsuarioTramiteRepository;

    @Mock
    AceProfileTramiteRepository aceProfileTramiteRepository;

    @Mock
    AceProfileTipoExpedienteRepository aceProfileTipoExpedienteRepository;

    @Mock
    AceProfileCentroRepository aceProfileCentroRepository;

    @Mock
    AceProfileExpedienteRepository aceProfileExpedienteRepository;

    @InjectMocks
    PerfilesUsuarioServiceImpl service;

    private static Centro centro(Long id) {
        Centro centro = new Centro();
        centro.setId(id);
        return centro;
    }

    private static User usuario(Long id) {
        User usuario = new User();
        usuario.setId(id);
        usuario.setCentroUsuarios(new ArrayList<>());
        return usuario;
    }

    private static Tramite tramite() {
        Tramite tramite = new Tramite();
        tramite.setTipoUsuario(new TipoUsuario());
        tramite.setDefaultTipoExpediente(new TipoExpediente());
        return tramite;
    }

    /** El registrador es otro usuario: quien quiera que el usuario sea el creador lo pisa en el test. */
    private static Expediente expediente(Centro centro, Tramite tramite, TipoExpediente tipoExpediente) {
        tipoExpediente.setTramite(tramite);
        Expediente expediente = new Expediente();
        expediente.setCentro(centro);
        expediente.setTipoExpediente(tipoExpediente);
        expediente.setUsuarioRegistrador(usuario(99L));
        return expediente;
    }

    private static CentroUsuario enCentro(User usuario, Centro centro) {
        CentroUsuario centroUsuario = new CentroUsuario();
        centroUsuario.setCentro(centro);
        centroUsuario.setUsuario(usuario);
        usuario.getCentroUsuarios().add(centroUsuario);
        return centroUsuario;
    }

    private void verificarQueNoSeConsultaNingunAce() {
        verifyNoInteractions(aceProfileGlobalRepository, aceProfileTipoUsuarioTramiteRepository, aceProfileTramiteRepository,
                aceProfileTipoExpedienteRepository, aceProfileCentroRepository, aceProfileExpedienteRepository);
    }

    @Test
    void getPerfilesSobreExpediente_sumaTodosLosNivelesConElCentroUsuarioDelCentroDelExpediente() {
        Centro centroExpediente = centro(2L);
        User usuario = usuario(5L);
        enCentro(usuario, centro(1L));
        CentroUsuario centroUsuario = enCentro(usuario, centroExpediente);
        TipoUsuario tipoUsuarioTramite = new TipoUsuario();
        Tramite tramite = tramite();
        tramite.setTipoUsuario(tipoUsuarioTramite);
        TipoExpediente tipoExpediente = new TipoExpediente();
        Expediente expediente = expediente(centroExpediente, tramite, tipoExpediente);
        when(aceProfileGlobalRepository.findPerfiles(centroUsuario)).thenReturn(Set.of(Profile.DIRECTOR));
        when(aceProfileTipoUsuarioTramiteRepository.findPerfiles(tipoUsuarioTramite, centroUsuario)).thenReturn(Set.of(Profile.TRAMITADOR));
        when(aceProfileTramiteRepository.findPerfiles(tramite, centroUsuario)).thenReturn(Set.of(Profile.SECRETARIO));
        when(aceProfileCentroRepository.findPerfiles(tramite, centroUsuario)).thenReturn(Set.of(Profile.AUDITOR));
        when(aceProfileTipoExpedienteRepository.findPerfiles(tipoExpediente, centroUsuario)).thenReturn(Set.of(Profile.COLABORADOR));
        when(aceProfileExpedienteRepository.findPerfiles(expediente, centroUsuario)).thenReturn(Set.of(Profile.AFECTADO));

        assertEquals(Set.of(Profile.DIRECTOR, Profile.TRAMITADOR, Profile.SECRETARIO, Profile.AUDITOR, Profile.COLABORADOR, Profile.AFECTADO),
                service.getPerfilesSobreExpediente(expediente, usuario));
    }

    @Test
    void getPerfilesSobreExpediente_creadorDeLasTablasNoEsCreadorSobreElExpediente() {
        Centro centroExpediente = centro(2L);
        User usuario = usuario(5L);
        CentroUsuario centroUsuario = enCentro(usuario, centroExpediente);
        TipoUsuario tipoUsuarioTramite = new TipoUsuario();
        Tramite tramite = tramite();
        tramite.setTipoUsuario(tipoUsuarioTramite);
        TipoExpediente tipoExpediente = new TipoExpediente();
        Expediente expediente = expediente(centroExpediente, tramite, tipoExpediente);
        when(aceProfileGlobalRepository.findPerfiles(centroUsuario)).thenReturn(Set.of(Profile.CREADOR));
        when(aceProfileTipoUsuarioTramiteRepository.findPerfiles(tipoUsuarioTramite, centroUsuario)).thenReturn(Set.of(Profile.CREADOR));
        when(aceProfileTramiteRepository.findPerfiles(tramite, centroUsuario)).thenReturn(Set.of(Profile.CREADOR, Profile.TRAMITADOR));
        when(aceProfileCentroRepository.findPerfiles(tramite, centroUsuario)).thenReturn(Set.of(Profile.CREADOR));
        when(aceProfileTipoExpedienteRepository.findPerfiles(tipoExpediente, centroUsuario)).thenReturn(Set.of(Profile.CREADOR));

        assertEquals(Set.of(Profile.TRAMITADOR), service.getPerfilesSobreExpediente(expediente, usuario));
    }

    @Test
    void getPerfilesSobreExpediente_creadorPorElPropioExpedienteSiEsCreador() {
        Centro centroExpediente = centro(2L);
        User usuario = usuario(5L);
        CentroUsuario centroUsuario = enCentro(usuario, centroExpediente);
        Expediente expediente = expediente(centroExpediente, tramite(), new TipoExpediente());
        when(aceProfileExpedienteRepository.findPerfiles(expediente, centroUsuario)).thenReturn(Set.of(Profile.CREADOR));

        assertEquals(Set.of(Profile.CREADOR), service.getPerfilesSobreExpediente(expediente, usuario));
    }

    @Test
    void getPerfilesSobreExpediente_usuarioQueNoEsDelCentro_ningunoYNoConsulta() {
        Centro centroExpediente = centro(2L);
        User usuario = usuario(5L);
        enCentro(usuario, centro(1L));
        Expediente expediente = expediente(centroExpediente, tramite(), new TipoExpediente());

        assertEquals(Set.of(), service.getPerfilesSobreExpediente(expediente, usuario));
        verify(aceProfileGlobalRepository, never()).findPerfiles(any());
        verify(aceProfileExpedienteRepository, never()).findPerfiles(any(), any());
    }

    @Test
    void getPerfilesSobreExpediente_elRegistradorEsCreadorAunqueNoTengaAce() {
        Centro centroExpediente = centro(2L);
        User usuario = usuario(5L);
        enCentro(usuario, centroExpediente);
        Expediente expediente = expediente(centroExpediente, tramite(), new TipoExpediente());
        expediente.setUsuarioRegistrador(usuario(5L));

        assertEquals(Set.of(Profile.CREADOR), service.getPerfilesSobreExpediente(expediente, usuario));
    }

    @Test
    void getPerfilesSobreTramite_sumaGlobalTipoUsuarioTramiteYCentroDelCentroIndicado() {
        Centro centroElegido = centro(2L);
        User usuario = usuario(5L);
        enCentro(usuario, centro(1L));
        CentroUsuario centroUsuario = enCentro(usuario, centroElegido);
        TipoUsuario tipoUsuarioTramite = new TipoUsuario();
        Tramite tramite = tramite();
        tramite.setTipoUsuario(tipoUsuarioTramite);
        when(aceProfileGlobalRepository.findPerfiles(centroUsuario)).thenReturn(Set.of(Profile.DIRECTOR));
        when(aceProfileTipoUsuarioTramiteRepository.findPerfiles(tipoUsuarioTramite, centroUsuario)).thenReturn(Set.of(Profile.CREADOR));
        when(aceProfileTramiteRepository.findPerfiles(tramite, centroUsuario)).thenReturn(Set.of(Profile.TRAMITADOR));
        when(aceProfileCentroRepository.findPerfiles(tramite, centroUsuario)).thenReturn(Set.of(Profile.AUDITOR));

        assertEquals(Set.of(Profile.DIRECTOR, Profile.CREADOR, Profile.TRAMITADOR, Profile.AUDITOR),
                service.getPerfilesSobreTramite(tramite, usuario, centroElegido));
    }

    @Test
    void getPerfilesSobreTramite_incluyeLosDelTipoDeExpedienteActivo() {
        Centro centroElegido = centro(2L);
        User usuario = usuario(5L);
        CentroUsuario centroUsuario = enCentro(usuario, centroElegido);
        TipoExpediente tipoActivo = new TipoExpediente();
        Tramite tramite = tramite();
        tramite.setDefaultTipoExpediente(tipoActivo);
        when(aceProfileTipoExpedienteRepository.findPerfiles(tipoActivo, centroUsuario)).thenReturn(Set.of(Profile.CREADOR));

        assertEquals(Set.of(Profile.CREADOR), service.getPerfilesSobreTramite(tramite, usuario, centroElegido));
    }

    @Test
    void getPerfilesSobreExpediente_usaSuTipoDeExpedienteYNoElActivoDelTramite() {
        Centro centroExpediente = centro(2L);
        User usuario = usuario(5L);
        CentroUsuario centroUsuario = enCentro(usuario, centroExpediente);
        TipoExpediente tipoDelExpediente = new TipoExpediente();
        TipoExpediente tipoActivo = new TipoExpediente();
        Tramite tramite = tramite();
        tramite.setDefaultTipoExpediente(tipoActivo);
        Expediente expediente = expediente(centroExpediente, tramite, tipoDelExpediente);
        when(aceProfileTipoExpedienteRepository.findPerfiles(tipoDelExpediente, centroUsuario)).thenReturn(Set.of(Profile.TRAMITADOR));

        assertEquals(Set.of(Profile.TRAMITADOR), service.getPerfilesSobreExpediente(expediente, usuario));
        verify(aceProfileTipoExpedienteRepository, never()).findPerfiles(tipoActivo, centroUsuario);
    }

    @Test
    void getPerfilesSobreTramite_sinCentro_lanzaExcepcionSinConsultar() {
        assertThrows(NullPointerException.class, () -> service.getPerfilesSobreTramite(tramite(), usuario(5L), null));
        verificarQueNoSeConsultaNingunAce();
    }

    @Test
    void getPerfilesSobreExpediente_sinExpedienteOSinUsuario_lanzaExcepcionSinConsultar() {
        Centro centroExpediente = centro(2L);
        User usuario = usuario(5L);
        enCentro(usuario, centroExpediente);
        Expediente expediente = expediente(centroExpediente, tramite(), new TipoExpediente());

        assertThrows(NullPointerException.class, () -> service.getPerfilesSobreExpediente(null, usuario));
        assertThrows(NullPointerException.class, () -> service.getPerfilesSobreExpediente(expediente, null));
        verificarQueNoSeConsultaNingunAce();
    }

    @Test
    void getPerfilesSobreExpediente_registradorQueNoEsDelCentro_ningunoYNoConsulta() {
        Centro centroExpediente = centro(2L);
        User usuario = usuario(5L);
        enCentro(usuario, centro(1L));
        Expediente expediente = expediente(centroExpediente, tramite(), new TipoExpediente());
        expediente.setUsuarioRegistrador(usuario(5L));

        assertEquals(Set.of(), service.getPerfilesSobreExpediente(expediente, usuario));
        verificarQueNoSeConsultaNingunAce();
    }

    @Test
    void getPerfilesSobreExpediente_registradorDistinto_noEsCreador() {
        Centro centroExpediente = centro(2L);
        User usuario = usuario(5L);
        enCentro(usuario, centroExpediente);
        Expediente expediente = expediente(centroExpediente, tramite(), new TipoExpediente());
        expediente.setUsuarioRegistrador(usuario(6L));

        assertEquals(Set.of(), service.getPerfilesSobreExpediente(expediente, usuario));
    }

    @Test
    void getPerfilesSobreExpediente_sinTipoExpediente_lanzaExcepcionSinConsultar() {
        Centro centroExpediente = centro(2L);
        User usuario = usuario(5L);
        enCentro(usuario, centroExpediente);
        Expediente expediente = new Expediente();
        expediente.setCentro(centroExpediente);
        expediente.setUsuarioRegistrador(usuario(99L));

        assertThrows(NullPointerException.class, () -> service.getPerfilesSobreExpediente(expediente, usuario));
        verificarQueNoSeConsultaNingunAce();
    }

    @Test
    void getPerfilesSobreExpediente_tipoExpedienteSinTramite_lanzaExcepcionSinConsultar() {
        Centro centroExpediente = centro(2L);
        User usuario = usuario(5L);
        enCentro(usuario, centroExpediente);
        Expediente expediente = expediente(centroExpediente, null, new TipoExpediente());

        assertThrows(NullPointerException.class, () -> service.getPerfilesSobreExpediente(expediente, usuario));
        verificarQueNoSeConsultaNingunAce();
    }

    @Test
    void getPerfilesSobreExpediente_tramiteSinTipoUsuario_lanzaExcepcionSinConsultar() {
        Centro centroExpediente = centro(2L);
        User usuario = usuario(5L);
        enCentro(usuario, centroExpediente);
        Tramite tramite = tramite();
        tramite.setTipoUsuario(null);
        Expediente expediente = expediente(centroExpediente, tramite, new TipoExpediente());

        assertThrows(NullPointerException.class, () -> service.getPerfilesSobreExpediente(expediente, usuario));
        verificarQueNoSeConsultaNingunAce();
    }

    @Test
    void getPerfilesSobreExpediente_centroSinId_ningunoAunqueSeaElRegistrador() {
        Centro centroSinId = centro(null);
        User usuario = usuario(5L);
        enCentro(usuario, centroSinId);
        Expediente expediente = expediente(centroSinId, tramite(), new TipoExpediente());

        assertEquals(Set.of(), service.getPerfilesSobreExpediente(expediente, usuario));

        expediente.setUsuarioRegistrador(usuario(5L));
        assertEquals(Set.of(), service.getPerfilesSobreExpediente(expediente, usuario));
        verificarQueNoSeConsultaNingunAce();
    }

    @Test
    void getPerfilesSobreExpediente_elCentroSeEmparejaPorIdYNoPorInstancia() {
        User usuario = usuario(5L);
        CentroUsuario centroUsuario = enCentro(usuario, centro(2L));
        Expediente expediente = expediente(centro(2L), tramite(), new TipoExpediente());
        when(aceProfileExpedienteRepository.findPerfiles(expediente, centroUsuario)).thenReturn(Set.of(Profile.AFECTADO));

        assertEquals(Set.of(Profile.AFECTADO), service.getPerfilesSobreExpediente(expediente, usuario));
    }

    @Test
    void getPerfilesSobreTramite_sinTramiteOSinUsuario_lanzaExcepcionSinConsultar() {
        Centro centroElegido = centro(2L);
        User usuario = usuario(5L);
        enCentro(usuario, centroElegido);

        assertThrows(NullPointerException.class, () -> service.getPerfilesSobreTramite(null, usuario, centroElegido));
        assertThrows(NullPointerException.class, () -> service.getPerfilesSobreTramite(tramite(), null, centroElegido));
        verificarQueNoSeConsultaNingunAce();
    }

    @Test
    void getPerfilesSobreTramite_usuarioQueNoEsDelCentroIndicado_ningunoYNoConsulta() {
        User usuario = usuario(5L);
        enCentro(usuario, centro(1L));

        assertEquals(Set.of(), service.getPerfilesSobreTramite(tramite(), usuario, centro(2L)));
        verificarQueNoSeConsultaNingunAce();
    }

    @Test
    void getPerfilesSobreTramite_sinTipoUsuario_lanzaExcepcionSinConsultar() {
        Centro centroElegido = centro(2L);
        User usuario = usuario(5L);
        enCentro(usuario, centroElegido);
        Tramite tramite = tramite();
        tramite.setTipoUsuario(null);

        assertThrows(NullPointerException.class, () -> service.getPerfilesSobreTramite(tramite, usuario, centroElegido));
        verificarQueNoSeConsultaNingunAce();
    }

    @Test
    void getPerfilesSobreTramite_sinTipoExpedienteActivo_lanzaExcepcionSinConsultar() {
        Centro centroElegido = centro(2L);
        User usuario = usuario(5L);
        enCentro(usuario, centroElegido);
        Tramite tramite = tramite();
        tramite.setDefaultTipoExpediente(null);

        assertThrows(NullPointerException.class, () -> service.getPerfilesSobreTramite(tramite, usuario, centroElegido));
        verificarQueNoSeConsultaNingunAce();
    }

    @Test
    void getPerfilesSobreTramite_centroSinId_ningunoYNoConsulta() {
        Centro centroSinId = centro(null);
        User usuario = usuario(5L);
        enCentro(usuario, centroSinId);

        assertEquals(Set.of(), service.getPerfilesSobreTramite(tramite(), usuario, centroSinId));
        verificarQueNoSeConsultaNingunAce();
    }

    @Test
    void getPerfilesSobreExpediente_perteneceAVariosCentros_soloConsultaElCentroUsuarioDelExpediente() {
        Centro centroOtro = centro(1L);
        Centro centroExpediente = centro(2L);
        User usuario = usuario(5L);
        CentroUsuario centroUsuarioOtro = enCentro(usuario, centroOtro);
        CentroUsuario centroUsuarioExpediente = enCentro(usuario, centroExpediente);
        Expediente expediente = expediente(centroExpediente, tramite(), new TipoExpediente());
        when(aceProfileExpedienteRepository.findPerfiles(expediente, centroUsuarioExpediente)).thenReturn(Set.of(Profile.AFECTADO));

        assertEquals(Set.of(Profile.AFECTADO), service.getPerfilesSobreExpediente(expediente, usuario));
        verify(aceProfileGlobalRepository, never()).findPerfiles(centroUsuarioOtro);
        verify(aceProfileExpedienteRepository, never()).findPerfiles(expediente, centroUsuarioOtro);
    }

    @Test
    void getPerfilesSobreTramite_perteneceAVariosCentros_soloConsultaElCentroUsuarioDelCentroElegido() {
        Centro centroOtro = centro(1L);
        Centro centroElegido = centro(2L);
        User usuario = usuario(5L);
        CentroUsuario centroUsuarioOtro = enCentro(usuario, centroOtro);
        CentroUsuario centroUsuarioElegido = enCentro(usuario, centroElegido);
        Tramite tramite = tramite();
        when(aceProfileTramiteRepository.findPerfiles(tramite, centroUsuarioElegido)).thenReturn(Set.of(Profile.TRAMITADOR));

        assertEquals(Set.of(Profile.TRAMITADOR), service.getPerfilesSobreTramite(tramite, usuario, centroElegido));
        verify(aceProfileGlobalRepository, never()).findPerfiles(centroUsuarioOtro);
        verify(aceProfileTramiteRepository, never()).findPerfiles(tramite, centroUsuarioOtro);
    }

    @Test
    void getPerfilesSobreExpediente_losAceDeOtroCentroNoCuentan() {
        User usuario = usuario(5L);
        CentroUsuario centroUsuarioOtro = enCentro(usuario, centro(1L));
        Centro centroExpediente = centro(2L);
        enCentro(usuario, centroExpediente);
        Expediente expediente = expediente(centroExpediente, tramite(), new TipoExpediente());
        lenient().when(aceProfileGlobalRepository.findPerfiles(centroUsuarioOtro)).thenReturn(Set.of(Profile.DIRECTOR));

        assertEquals(Set.of(), service.getPerfilesSobreExpediente(expediente, usuario));
        verify(aceProfileGlobalRepository, never()).findPerfiles(centroUsuarioOtro);
    }

    @Test
    void getPerfilesSobreTramite_losAceDeOtroCentroNoCuentan() {
        User usuario = usuario(5L);
        CentroUsuario centroUsuarioOtro = enCentro(usuario, centro(1L));
        Centro centroElegido = centro(2L);
        enCentro(usuario, centroElegido);
        Tramite tramite = tramite();
        lenient().when(aceProfileGlobalRepository.findPerfiles(centroUsuarioOtro)).thenReturn(Set.of(Profile.DIRECTOR));

        assertEquals(Set.of(), service.getPerfilesSobreTramite(tramite, usuario, centroElegido));
        verify(aceProfileGlobalRepository, never()).findPerfiles(centroUsuarioOtro);
    }

    @Test
    void getPerfilesSobreExpediente_sinCentroUsuarios_ningunoAunqueSeaElRegistrador() {
        Centro centroExpediente = centro(2L);
        User usuario = usuario(5L);
        usuario.setCentroUsuarios(null);
        Expediente expediente = expediente(centroExpediente, tramite(), new TipoExpediente());

        assertEquals(Set.of(), service.getPerfilesSobreExpediente(expediente, usuario));

        expediente.setUsuarioRegistrador(usuario(5L));
        assertEquals(Set.of(), service.getPerfilesSobreExpediente(expediente, usuario));
        verificarQueNoSeConsultaNingunAce();
    }

    @Test
    void getPerfilesSobreTramite_sinCentroUsuarios_ningunoYNoConsulta() {
        User usuario = usuario(5L);
        usuario.setCentroUsuarios(null);

        assertEquals(Set.of(), service.getPerfilesSobreTramite(tramite(), usuario, centro(2L)));
        verificarQueNoSeConsultaNingunAce();
    }

    @Test
    void getPerfilesSobreExpediente_ignoraLosCentroUsuarioSinCentro() {
        Centro centroExpediente = centro(2L);
        User usuario = usuario(5L);
        enCentro(usuario, null);
        CentroUsuario centroUsuario = enCentro(usuario, centroExpediente);
        Expediente expediente = expediente(centroExpediente, tramite(), new TipoExpediente());
        when(aceProfileExpedienteRepository.findPerfiles(expediente, centroUsuario)).thenReturn(Set.of(Profile.AFECTADO));

        assertEquals(Set.of(Profile.AFECTADO), service.getPerfilesSobreExpediente(expediente, usuario));
    }

    @Test
    void getPerfilesSobreExpediente_expedienteSinCentro_ningunoAunqueSeaElRegistrador() {
        User usuario = usuario(5L);
        enCentro(usuario, centro(2L));
        Expediente expediente = expediente(null, tramite(), new TipoExpediente());

        assertEquals(Set.of(), service.getPerfilesSobreExpediente(expediente, usuario));

        expediente.setUsuarioRegistrador(usuario(5L));
        assertEquals(Set.of(), service.getPerfilesSobreExpediente(expediente, usuario));
        verificarQueNoSeConsultaNingunAce();
    }

    @Test
    void getPerfilesSobreExpediente_sinAces_ningunoPeroConsultaCadaNivelUnaVezConSusArgumentos() {
        Centro centroExpediente = centro(2L);
        User usuario = usuario(5L);
        CentroUsuario centroUsuario = enCentro(usuario, centroExpediente);
        TipoUsuario tipoUsuarioTramite = new TipoUsuario();
        Tramite tramite = tramite();
        tramite.setTipoUsuario(tipoUsuarioTramite);
        TipoExpediente tipoExpediente = new TipoExpediente();
        Expediente expediente = expediente(centroExpediente, tramite, tipoExpediente);

        assertEquals(Set.of(), service.getPerfilesSobreExpediente(expediente, usuario));
        verify(aceProfileGlobalRepository, times(1)).findPerfiles(centroUsuario);
        verify(aceProfileTipoUsuarioTramiteRepository, times(1)).findPerfiles(tipoUsuarioTramite, centroUsuario);
        verify(aceProfileTramiteRepository, times(1)).findPerfiles(tramite, centroUsuario);
        verify(aceProfileCentroRepository, times(1)).findPerfiles(tramite, centroUsuario);
        verify(aceProfileTipoExpedienteRepository, times(1)).findPerfiles(tipoExpediente, centroUsuario);
        verify(aceProfileExpedienteRepository, times(1)).findPerfiles(expediente, centroUsuario);
        verifyNoMoreInteractions(aceProfileGlobalRepository, aceProfileTipoUsuarioTramiteRepository, aceProfileTramiteRepository,
                aceProfileTipoExpedienteRepository, aceProfileCentroRepository, aceProfileExpedienteRepository);
    }

    @Test
    void getPerfilesSobreTramite_sinAces_ningunoPeroConsultaCadaNivelUnaVezConSusArgumentos() {
        Centro centroElegido = centro(2L);
        User usuario = usuario(5L);
        CentroUsuario centroUsuario = enCentro(usuario, centroElegido);
        TipoUsuario tipoUsuarioTramite = new TipoUsuario();
        TipoExpediente tipoActivo = new TipoExpediente();
        Tramite tramite = tramite();
        tramite.setTipoUsuario(tipoUsuarioTramite);
        tramite.setDefaultTipoExpediente(tipoActivo);

        assertEquals(Set.of(), service.getPerfilesSobreTramite(tramite, usuario, centroElegido));
        verify(aceProfileGlobalRepository, times(1)).findPerfiles(centroUsuario);
        verify(aceProfileTipoUsuarioTramiteRepository, times(1)).findPerfiles(tipoUsuarioTramite, centroUsuario);
        verify(aceProfileTramiteRepository, times(1)).findPerfiles(tramite, centroUsuario);
        verify(aceProfileCentroRepository, times(1)).findPerfiles(tramite, centroUsuario);
        verify(aceProfileTipoExpedienteRepository, times(1)).findPerfiles(tipoActivo, centroUsuario);
        verifyNoMoreInteractions(aceProfileGlobalRepository, aceProfileTipoUsuarioTramiteRepository, aceProfileTramiteRepository,
                aceProfileTipoExpedienteRepository, aceProfileCentroRepository);
        verifyNoInteractions(aceProfileExpedienteRepository);
    }

    @Test
    void getPerfilesSobreExpediente_registradorConAces_sumaLosAcesYCreador() {
        Centro centroExpediente = centro(2L);
        User usuario = usuario(5L);
        CentroUsuario centroUsuario = enCentro(usuario, centroExpediente);
        Tramite tramite = tramite();
        Expediente expediente = expediente(centroExpediente, tramite, new TipoExpediente());
        expediente.setUsuarioRegistrador(usuario(5L));
        when(aceProfileTramiteRepository.findPerfiles(tramite, centroUsuario)).thenReturn(Set.of(Profile.TRAMITADOR));
        when(aceProfileExpedienteRepository.findPerfiles(expediente, centroUsuario)).thenReturn(Set.of(Profile.AFECTADO));

        assertEquals(Set.of(Profile.TRAMITADOR, Profile.AFECTADO, Profile.CREADOR),
                service.getPerfilesSobreExpediente(expediente, usuario));
    }

    @Test
    void getPerfilesSobreExpediente_registradorQueYaEsCreadorPorAce_creadorUnaSolaVez() {
        Centro centroExpediente = centro(2L);
        User usuario = usuario(5L);
        CentroUsuario centroUsuario = enCentro(usuario, centroExpediente);
        Tramite tramite = tramite();
        Expediente expediente = expediente(centroExpediente, tramite, new TipoExpediente());
        expediente.setUsuarioRegistrador(usuario(5L));
        when(aceProfileTramiteRepository.findPerfiles(tramite, centroUsuario)).thenReturn(Set.of(Profile.CREADOR, Profile.TRAMITADOR));

        assertEquals(Set.of(Profile.CREADOR, Profile.TRAMITADOR), service.getPerfilesSobreExpediente(expediente, usuario));
    }

    @Test
    void getPerfilesSobreTramite_estaContenidoEnLosDeUnExpedienteDelTipoActivoEnElMismoCentro() {
        Centro centro = centro(2L);
        User usuario = usuario(5L);
        CentroUsuario centroUsuario = enCentro(usuario, centro);
        TipoUsuario tipoUsuarioTramite = new TipoUsuario();
        TipoExpediente tipoActivo = new TipoExpediente();
        Tramite tramite = tramite();
        tramite.setTipoUsuario(tipoUsuarioTramite);
        tramite.setDefaultTipoExpediente(tipoActivo);
        Expediente expediente = expediente(centro, tramite, tipoActivo);
        when(aceProfileGlobalRepository.findPerfiles(centroUsuario)).thenReturn(Set.of(Profile.DIRECTOR));
        when(aceProfileTipoUsuarioTramiteRepository.findPerfiles(tipoUsuarioTramite, centroUsuario)).thenReturn(Set.of(Profile.TRAMITADOR));
        when(aceProfileTramiteRepository.findPerfiles(tramite, centroUsuario)).thenReturn(Set.of(Profile.SECRETARIO));
        when(aceProfileCentroRepository.findPerfiles(tramite, centroUsuario)).thenReturn(Set.of(Profile.AUDITOR));
        when(aceProfileTipoExpedienteRepository.findPerfiles(tipoActivo, centroUsuario)).thenReturn(Set.of(Profile.COLABORADOR));
        when(aceProfileExpedienteRepository.findPerfiles(expediente, centroUsuario)).thenReturn(Set.of(Profile.AFECTADO));

        Set<Profile> perfilesSobreTramite = service.getPerfilesSobreTramite(tramite, usuario, centro);
        Set<Profile> perfilesSobreExpediente = service.getPerfilesSobreExpediente(expediente, usuario);

        assertTrue(perfilesSobreExpediente.containsAll(perfilesSobreTramite));
    }

    @Test
    void getPerfilesSobreExpediente_siempreDevuelveUnConjuntoInmutable() {
        Centro centroExpediente = centro(2L);
        User usuarioDelCentro = usuario(5L);
        enCentro(usuarioDelCentro, centroExpediente);
        User usuarioDeOtroCentro = usuario(6L);
        enCentro(usuarioDeOtroCentro, centro(1L));
        Expediente expediente = expediente(centroExpediente, tramite(), new TipoExpediente());

        assertAll(
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> service.getPerfilesSobreExpediente(expediente, usuarioDeOtroCentro).add(Profile.CREADOR)),
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> service.getPerfilesSobreExpediente(expediente, usuarioDelCentro).add(Profile.CREADOR)));
    }

    @Test
    void getPerfilesSobreTramite_siempreDevuelveUnConjuntoInmutable() {
        Centro centroElegido = centro(2L);
        User usuarioDelCentro = usuario(5L);
        enCentro(usuarioDelCentro, centroElegido);
        User usuarioDeOtroCentro = usuario(6L);
        enCentro(usuarioDeOtroCentro, centro(1L));
        Tramite tramite = tramite();

        assertAll(
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> service.getPerfilesSobreTramite(tramite, usuarioDeOtroCentro, centroElegido).add(Profile.CREADOR)),
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> service.getPerfilesSobreTramite(tramite, usuarioDelCentro, centroElegido).add(Profile.CREADOR)));
    }

}
