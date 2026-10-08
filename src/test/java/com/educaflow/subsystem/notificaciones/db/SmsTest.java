package com.educaflow.subsystem.notificaciones.db;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SmsTest {

    @Test
    void getDestino_sms_devuelveElTelefono() {
        Sms sms = new Sms();
        sms.setTelefono("+34600111222");

        assertEquals("+34600111222", sms.getDestino());
    }

    @Test
    void getDestino_valorAsignadoPorElCliente_seIgnora() {
        Sms sms = new Sms();
        sms.setTelefono("+34600111222");
        sms.setDestino("+34699999999");

        assertEquals("+34600111222", sms.getDestino());
    }
}
