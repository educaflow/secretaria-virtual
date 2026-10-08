package com.educaflow.subsystem.notificaciones.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TipoNotificacionTest {

    @Test
    void getClaseNotificacion_cadaCanal_devuelveSuClase() {
        assertEquals(Correo.class, TipoNotificacion.CORREO.getClaseNotificacion());
        assertEquals(Sms.class, TipoNotificacion.SMS.getClaseNotificacion());
    }

    @Test
    void getClaseNotificacion_todosLosValores_idaYVueltaConDeClase() {
        for (TipoNotificacion tipo : TipoNotificacion.values()) {
            assertEquals(tipo, TipoNotificacion.deClase(tipo.getClaseNotificacion()));
        }
    }

    @Test
    void deClase_correo_devuelveCorreo() {
        assertEquals(TipoNotificacion.CORREO, TipoNotificacion.deClase(Correo.class));
    }

    @Test
    void deClase_sms_devuelveSms() {
        assertEquals(TipoNotificacion.SMS, TipoNotificacion.deClase(Sms.class));
    }

    @Test
    void deClase_notificacionGenerica_lanzaIllegalStateException() {
        assertThrows(IllegalStateException.class, () -> TipoNotificacion.deClase(Notificacion.class));
    }

    @Test
    void deClase_claseAjena_lanzaIllegalStateException() {
        assertThrows(IllegalStateException.class, () -> TipoNotificacion.deClase(String.class));
    }
}
