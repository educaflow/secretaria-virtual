package com.educaflow.subsystem.tramitador.tramitacion.util;

import com.axelor.inject.Beans;
import com.educaflow.base.util.Convert;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.subsystem.expedientes.db.TipoExpediente;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.Phase;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.State;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.TipoExpedienteStates;
import com.educaflow.subsystem.tramitador.tramitacion.internal.ExpedienteLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExpedienteUtilTest {

    private MockedStatic<Beans> beansMock;
    private TipoExpedienteStates tipoExpedienteStates;
    private TipoExpediente tipoExpediente;
    private Expediente expediente;

    @BeforeEach
    void setUp() {
        tipoExpediente = new TipoExpediente();
        tipoExpediente.setCode("MI_TIPO");
        expediente = new Expediente();
        expediente.setTipoExpediente(tipoExpediente);

        tipoExpedienteStates = mock(TipoExpedienteStates.class);
        ExpedienteLocator locator = mock(ExpedienteLocator.class);
        when(locator.getTipoExpedienteStates(tipoExpediente)).thenReturn(tipoExpedienteStates);

        beansMock = Mockito.mockStatic(Beans.class);
        beansMock.when(() -> Beans.get(ExpedienteLocator.class)).thenReturn(locator);
    }

    @AfterEach
    void tearDown() {
        beansMock.close();
    }

    private State state(String phaseCode, String phaseName, String code, String name, Profile profile, boolean isFinal) {
        Phase phase = mock(Phase.class);
        when(phase.getCode()).thenReturn(phaseCode);
        when(phase.getName()).thenReturn(phaseName);
        State state = mock(State.class);
        when(state.getPhase()).thenReturn(phase);
        when(state.getCode()).thenReturn(code);
        when(state.getName()).thenReturn(name);
        when(state.getProfile()).thenReturn(profile);
        when(state.isFinal()).thenReturn(isFinal);
        return state;
    }

    private State registrado(String phaseCode, String code, boolean isFinal) {
        State state = state(phaseCode, "Nombre " + phaseCode, code, "Nombre " + code, Profile.CREADOR, isFinal);
        when(tipoExpedienteStates.getState(phaseCode, code)).thenReturn(Optional.of(state));
        return state;
    }

    @Test
    void updateState_stateNulo_lanzaIllegalArgument() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ExpedienteUtil.updateState(expediente, null));
        assertEquals("El state no puede ser nulo.", ex.getMessage());
        assertNull(expediente.getCodeState());
    }

    @Test
    void updateState_estadoNoExisteEnElTipo_lanzaIllegalArgument() {
        State state = state("FASE", "Fase", "ESTADO", "Estado", Profile.CREADOR, false);
        when(tipoExpedienteStates.getState("FASE", "ESTADO")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ExpedienteUtil.updateState(expediente, state));
        assertTrue(ex.getMessage().startsWith("El estado FASE/ESTADO no es del tipo de expediente MI_TIPO"));
        assertNull(expediente.getCodeState());
        assertNull(expediente.getCodePhase());
    }

    @Test
    void updateState_stateDeOtroTipoConMismosCodigos_lanzaIllegalArgument() {
        registrado("FASE", "ESTADO", false);
        State otro = state("FASE", "Fase", "ESTADO", "Estado", Profile.CREADOR, false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ExpedienteUtil.updateState(expediente, otro));
        assertTrue(ex.getMessage().contains("FASE/ESTADO"));
        assertNull(expediente.getCodeState());
    }

    @Test
    void updateState_estadoNuevo_actualizaTodosLosCampos() {
        State state = registrado("FASE", "ESTADO", false);
        LocalDateTime antes = LocalDateTime.now(Convert.defaultZoneId).minusMinutes(1);

        ExpedienteUtil.updateState(expediente, state);

        assertEquals("FASE", expediente.getCodePhase());
        assertEquals("Nombre FASE", expediente.getNamePhase());
        assertEquals("ESTADO", expediente.getCodeState());
        assertEquals("Nombre ESTADO", expediente.getNameState());
        assertEquals(Profile.CREADOR, expediente.getPerfilEstado());
        assertNotNull(expediente.getFechaUltimoEstado());
        assertTrue(expediente.getFechaUltimoEstado().isAfter(antes));
        assertTrue(expediente.getAbierto());
    }

    @Test
    void updateState_estadoFinal_cierraElExpediente() {
        State state = registrado("FASE", "FIN", true);
        expediente.setAbierto(true);

        ExpedienteUtil.updateState(expediente, state);

        assertEquals("FIN", expediente.getCodeState());
        assertFalse(expediente.getAbierto());
    }

    @Test
    void updateState_mismoEstadoQueElActual_noCambiaNada() {
        State state = registrado("FASE", "ESTADO", false);
        LocalDateTime fecha = LocalDateTime.of(2020, 1, 1, 0, 0);
        expediente.setCodePhase("FASE");
        expediente.setCodeState("ESTADO");
        expediente.setNamePhase("previo");
        expediente.setFechaUltimoEstado(fecha);
        expediente.setAbierto(false);

        ExpedienteUtil.updateState(expediente, state);

        assertEquals("previo", expediente.getNamePhase());
        assertEquals(fecha, expediente.getFechaUltimoEstado());
        assertFalse(expediente.getAbierto());
        assertNull(expediente.getNameState());
    }

    @Test
    void updateState_mismoCodigoDeEstadoEnOtraFase_actualiza() {
        State state = registrado("FASE_NUEVA", "ESTADO", false);
        expediente.setCodePhase("FASE_VIEJA");
        expediente.setCodeState("ESTADO");

        ExpedienteUtil.updateState(expediente, state);

        assertEquals("FASE_NUEVA", expediente.getCodePhase());
        assertEquals("Nombre FASE_NUEVA", expediente.getNamePhase());
        assertEquals("ESTADO", expediente.getCodeState());
        assertTrue(expediente.getAbierto());
    }
}
