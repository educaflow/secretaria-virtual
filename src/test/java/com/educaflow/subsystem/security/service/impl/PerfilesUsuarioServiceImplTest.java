package com.educaflow.subsystem.security.service.impl;

import com.axelor.auth.db.User;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.subsystem.expedientes.db.Tramite;
import com.educaflow.subsystem.security.db.repo.AceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerfilesUsuarioServiceImplTest {

    @Mock
    AceRepository aceRepository;

    @InjectMocks
    PerfilesUsuarioServiceImpl service;

    private static Centro centro(long id) {
        Centro centro = new Centro();
        centro.setId(id);
        return centro;
    }

    private static User usuario(long id) {
        User usuario = new User();
        usuario.setId(id);
        usuario.setCentroUsuarios(new ArrayList<>());
        return usuario;
    }

    private static CentroUsuario enCentro(User usuario, Centro centro) {
        CentroUsuario centroUsuario = new CentroUsuario();
        centroUsuario.setCentro(centro);
        centroUsuario.setUsuario(usuario);
        usuario.getCentroUsuarios().add(centroUsuario);
        return centroUsuario;
    }

    @Test
    void getPerfilesSobreExpediente_preguntaPorElCentroDelExpedienteYElCentroUsuarioDeEseCentro() {
        Centro centroExpediente = centro(2L);
        User usuario = usuario(5L);
        enCentro(usuario, centro(1L));
        CentroUsuario centroUsuarioDelExpediente = enCentro(usuario, centroExpediente);
        Expediente expediente = new Expediente();
        expediente.setCentro(centroExpediente);
        when(aceRepository.findPerfilesByExpediente(expediente, centroExpediente, centroUsuarioDelExpediente))
                .thenReturn(Set.of(Profile.SECRETARIO));

        assertEquals(Set.of(Profile.SECRETARIO), service.getPerfilesSobreExpediente(expediente, usuario));
    }

    @Test
    void getPerfilesSobreExpediente_usuarioQueNoEsDelCentro_seConsultaSinCentroUsuario() {
        Centro centroExpediente = centro(2L);
        User usuario = usuario(5L);
        enCentro(usuario, centro(1L));
        Expediente expediente = new Expediente();
        expediente.setCentro(centroExpediente);
        when(aceRepository.findPerfilesByExpediente(expediente, centroExpediente, null)).thenReturn(Set.of());

        assertEquals(Set.of(), service.getPerfilesSobreExpediente(expediente, usuario));
    }

    @Test
    void getPerfilesSobreExpediente_elRegistradorEsCreadorAunqueNoTengaAce() {
        Centro centroExpediente = centro(2L);
        User usuario = usuario(5L);
        CentroUsuario centroUsuario = enCentro(usuario, centroExpediente);
        Expediente expediente = new Expediente();
        expediente.setCentro(centroExpediente);
        expediente.setUsuarioRegistrador(usuario(5L));
        when(aceRepository.findPerfilesByExpediente(expediente, centroExpediente, centroUsuario)).thenReturn(Set.of());

        assertEquals(Set.of(Profile.CREADOR), service.getPerfilesSobreExpediente(expediente, usuario));
    }

    @Test
    void getPerfilesSobreTramite_preguntaPorElCentroIndicado() {
        Centro centroElegido = centro(2L);
        User usuario = usuario(5L);
        enCentro(usuario, centro(1L));
        CentroUsuario centroUsuario = enCentro(usuario, centroElegido);
        Tramite tramite = new Tramite();
        when(aceRepository.findPerfilesByTramite(tramite, centroElegido, centroUsuario)).thenReturn(Set.of(Profile.CREADOR));

        assertEquals(Set.of(Profile.CREADOR), service.getPerfilesSobreTramite(tramite, usuario, centroElegido));
    }

    @Test
    void getPerfilesSobreTramite_sinCentro_ningunoYNoConsulta() {
        assertEquals(Set.of(), service.getPerfilesSobreTramite(new Tramite(), usuario(5L), null));
        verify(aceRepository, never()).findPerfilesByTramite(any(), any(), any());
    }

}
