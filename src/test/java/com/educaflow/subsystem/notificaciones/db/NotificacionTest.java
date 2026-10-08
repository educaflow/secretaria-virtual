package com.educaflow.subsystem.notificaciones.db;

import com.educaflow.base.infrastructure.junit.JUnitHelper;
import org.junit.jupiter.api.Test;

class NotificacionTest {

    @Test
    void getDestino_notificacionGenerica_lanzaIllegalStateException() {
        Notificacion notificacion = new Notificacion();

        JUnitHelper.assertThrowsCause(IllegalStateException.class, notificacion::getDestino);
    }
}
