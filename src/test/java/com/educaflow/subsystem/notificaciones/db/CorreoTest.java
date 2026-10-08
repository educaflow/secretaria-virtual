package com.educaflow.subsystem.notificaciones.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class CorreoTest {

    @Test
    void getDestino_correo_devuelveElPara() {
        Correo correo = new Correo();
        correo.setPara("a@x.com");

        assertEquals("a@x.com", correo.getDestino());
    }

    @Test
    void getDestino_valorAsignadoPorElCliente_seIgnora() {
        Correo correo = new Correo();
        correo.setPara("a@x.com");
        correo.setDestino("falso@x.com");

        assertEquals("a@x.com", correo.getDestino());
    }

    @Test
    void getDestino_sinPara_devuelveNull() {
        Correo correo = new Correo();

        assertNull(correo.getDestino());
    }
}
