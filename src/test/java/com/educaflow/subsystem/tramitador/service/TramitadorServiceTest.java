package com.educaflow.subsystem.tramitador.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.axelor.auth.db.User;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.i18n.I18n;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.subsystem.expedientes.db.Tramite;
import com.educaflow.subsystem.security.service.PerfilesUsuarioService;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.ContextoTramitacion;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
class TramitadorServiceTest {

    private static final String SIN_CENTRO = "Debe indicar el centro";
    private static final String SIN_PERFILES_DE_INICIO = "No puede crear expedientes de este trámite en el centro indicado";
    private static final String PERFIL_NO_VALIDO = "No puede presentar el expediente de esa forma en el centro indicado";

    @Mock
    private PerfilesUsuarioService perfilesUsuarioService;

    @InjectMocks
    private TramitadorService tramitadorService;

    private MockedStatic<I18n> i18nMock;
    private MockedStatic<SecurityUtil> securityUtilMock;

    private final Tramite tramite = mock(Tramite.class);
    private final Centro centro = mock(Centro.class);
    private final User user = mock(User.class);

    @BeforeEach
    void setUp() {
        i18nMock = Mockito.mockStatic(I18n.class, Mockito.withSettings().strictness(Strictness.LENIENT));
        i18nMock.when(() -> I18n.get(anyString())).thenAnswer(invocacion -> invocacion.getArgument(0));
        securityUtilMock = Mockito.mockStatic(SecurityUtil.class, Mockito.withSettings().strictness(Strictness.LENIENT));
        securityUtilMock.when(SecurityUtil::getUser).thenReturn(user);
    }

    @AfterEach
    void tearDown() {
        securityUtilMock.close();
        i18nMock.close();
    }

    private List<String> mensajes(Optional<BusinessMessages> resultado) {
        return resultado.orElseThrow().stream().map(BusinessMessage::getMessage).toList();
    }

    @Test
    void validateTriggerInitialEvent_sinCentro_avisaDelCentroSinConsultarPerfiles() {
        // El constructor del record impide centro nulo: se simula con un mock para caracterizar la guarda.
        ContextoTramitacion contexto = mock(ContextoTramitacion.class);
        when(contexto.centro()).thenReturn(null);

        Optional<BusinessMessages> resultado = tramitadorService.validateTriggerInitialEvent(contexto);

        assertEquals(List.of(SIN_CENTRO), mensajes(resultado));
        verifyNoInteractions(perfilesUsuarioService);
    }

    @Test
    void validateTriggerInitialEvent_sinPerfilesDeInicio_avisaQueNoPuedeCrear() {
        ContextoTramitacion contexto = new ContextoTramitacion(tramite, centro, Profile.CREADOR, false, false);
        when(perfilesUsuarioService.getPerfilesDeInicioSobreTramite(tramite, user, centro)).thenReturn(Set.of());

        Optional<BusinessMessages> resultado = tramitadorService.validateTriggerInitialEvent(contexto);

        assertEquals(List.of(SIN_PERFILES_DE_INICIO), mensajes(resultado));
    }

    @Test
    void validateTriggerInitialEvent_perfilNulo_avisaQueNoPuedePresentarDeEsaForma() {
        // El constructor del record impide perfil nulo: se simula con un mock para caracterizar la guarda.
        ContextoTramitacion contexto = mock(ContextoTramitacion.class);
        when(contexto.centro()).thenReturn(centro);
        when(contexto.tramite()).thenReturn(tramite);
        when(contexto.profile()).thenReturn(null);
        when(perfilesUsuarioService.getPerfilesDeInicioSobreTramite(tramite, user, centro)).thenReturn(Set.of(Profile.CREADOR));

        Optional<BusinessMessages> resultado = tramitadorService.validateTriggerInitialEvent(contexto);

        assertEquals(List.of(PERFIL_NO_VALIDO), mensajes(resultado));
    }

    @Test
    void validateTriggerInitialEvent_perfilQueNoPuedeCrear_avisaAunqueSeaDeInicio() {
        ContextoTramitacion contexto = new ContextoTramitacion(tramite, centro, Profile.AUDITOR, false, false);
        when(perfilesUsuarioService.getPerfilesDeInicioSobreTramite(tramite, user, centro)).thenReturn(Set.of(Profile.AUDITOR));

        Optional<BusinessMessages> resultado = tramitadorService.validateTriggerInitialEvent(contexto);

        assertEquals(List.of(PERFIL_NO_VALIDO), mensajes(resultado));
    }

    @Test
    void validateTriggerInitialEvent_perfilQueNoEsDeInicio_avisaQueNoPuedePresentarDeEsaForma() {
        ContextoTramitacion contexto = new ContextoTramitacion(tramite, centro, Profile.TRAMITADOR, false, false);
        when(perfilesUsuarioService.getPerfilesDeInicioSobreTramite(tramite, user, centro)).thenReturn(Set.of(Profile.CREADOR));

        Optional<BusinessMessages> resultado = tramitadorService.validateTriggerInitialEvent(contexto);

        assertEquals(List.of(PERFIL_NO_VALIDO), mensajes(resultado));
    }

    @Test
    void validateTriggerInitialEvent_perfilDeInicioQuePuedeCrear_esValido() {
        ContextoTramitacion contexto = new ContextoTramitacion(tramite, centro, Profile.CREADOR, false, false);
        when(perfilesUsuarioService.getPerfilesDeInicioSobreTramite(any(), any(), any())).thenReturn(Set.of(Profile.CREADOR, Profile.TRAMITADOR));

        Optional<BusinessMessages> resultado = tramitadorService.validateTriggerInitialEvent(contexto);

        assertTrue(resultado.isEmpty());
    }
}
