package com.educaflow.common.util;

import com.axelor.db.ValueEnum;
import com.axelor.db.annotations.EnumWidget;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
class ConvertTest {

    @Test
    void objectToLong() {
        assertNull(Convert.objectToLong(null));
        assertEquals(123L, Convert.objectToLong(123L));
        assertEquals(123L, Convert.objectToLong(123));

        assertThrows(Exception.class, () -> Convert.objectToLong("abc"));
    }

    @Test
    void objectToUserString() {
        // Strings y null
        assertEquals("", Convert.objectToUserString(null));
        assertEquals("", Convert.objectToUserString(""));
        assertEquals(" Hola mundo ", Convert.objectToUserString(" Hola mundo "));

        // Booleanos
        assertEquals("Sí", Convert.objectToUserString(true));
        assertEquals("No", Convert.objectToUserString(false));

        // Enteros
        assertEquals("3", Convert.objectToUserString(3));
        assertEquals("3", Convert.objectToUserString(3L));
        assertEquals("-3", Convert.objectToUserString(-3));
        assertEquals("-3", Convert.objectToUserString(-3L));
        assertEquals("1.234", Convert.objectToUserString(1234));
        assertEquals("-1.234", Convert.objectToUserString(-1234));

        // Números decimales
        assertEquals("3,14", Convert.objectToUserString(3.14159));
        assertEquals("-3,14", Convert.objectToUserString(-3.14159));
        assertEquals("1.234,56", Convert.objectToUserString(1234.56));

        // Fechas y horas
        LocalDate date = LocalDate.of(2025, 8, 28);
        LocalTime time = LocalTime.of(14, 30, 15);
        LocalDateTime dateTime = LocalDateTime.of(date, time);
        Instant instant = dateTime.atZone(Convert.defaultZoneId).toInstant();
        Date utilDate = Date.from(instant);

        assertEquals("28/08/2025", Convert.objectToUserString(date));
        assertEquals("14:30", Convert.objectToUserString(time));
        assertEquals("28/08/2025 14:30", Convert.objectToUserString(dateTime));
        assertEquals("28/08/2025 14:30", Convert.objectToUserString(instant));
        assertEquals("28/08/2025 14:30", Convert.objectToUserString(utilDate));

        //Enumerados
        assertEquals("Enfermedad comun", Convert.objectToUserString(MotivoFaltaJustificacionFaltaProfesorado.ENFERMEDAD_COMUN));
        assertEquals("Permiso médico, educativo o asistencial", Convert.objectToUserString(MotivoFaltaJustificacionFaltaProfesorado.PERMISO_MEDICO_EDUCATIVO_ASISTENCIAL));
        assertEquals("Traslado domicilio", Convert.objectToUserString(MotivoFaltaJustificacionFaltaProfesorado.TRASLADO_DOMICILIO));
        assertEquals("Valor primero", Convert.objectToUserString(Prueba2.VALOR_PRIMERO));


        // Otros objetos
        Object obj = new Object();
        assertEquals(obj.toString(), Convert.objectToUserString(obj));

    }

    enum MotivoFaltaJustificacionFaltaProfesorado implements ValueEnum<String> {
        @EnumWidget()
        ENFERMEDAD_COMUN,

        @EnumWidget(
                title = "Permiso médico, educativo o asistencial"
        )
        PERMISO_MEDICO_EDUCATIVO_ASISTENCIAL,

        TRASLADO_DOMICILIO,
        ;

        @Override
        public String getValue() {
            return name();
        }


    }
    enum Prueba2 {
        VALOR_PRIMERO
    }
}