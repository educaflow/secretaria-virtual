package com.educaflow.base.util;

import com.axelor.db.ValueEnum;
import com.axelor.db.annotations.EnumWidget;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
class ConvertTest {

    @Test
    void objectToLong() {
        // null devuelve null (Long nullable)
        assertNull(Convert.objectToLong(null));
        assertEquals(123L, Convert.objectToLong(123L));
        assertEquals(123L, Convert.objectToLong(123));

        // No acepta String
        assertThrows(IllegalArgumentException.class, () -> Convert.objectToLong("123"));
        assertThrows(IllegalArgumentException.class, () -> Convert.objectToLong("abc"));
    }

    @Test
    void objectToBoolean() {
        // null devuelve null (Boolean nullable)
        assertNull(Convert.objectToBoolean(null));
        assertEquals(true, Convert.objectToBoolean(true));
        assertEquals(false, Convert.objectToBoolean(false));

        // No acepta otros tipos
        assertThrows(IllegalArgumentException.class, () -> Convert.objectToBoolean("true"));
        assertThrows(IllegalArgumentException.class, () -> Convert.objectToBoolean(1));
    }

    @Test
    void objectToInt() {
        // null devuelve null (Integer nullable)
        assertNull(Convert.objectToInt(null));
        assertEquals(123, Convert.objectToInt(123));
        assertEquals(123, Convert.objectToInt(123L));

        // No acepta String
        assertThrows(IllegalArgumentException.class, () -> Convert.objectToInt("123"));
        assertThrows(IllegalArgumentException.class, () -> Convert.objectToInt("abc"));
    }

    @Test
    void coerceToLong() {
        // null y String vacía devuelven 0 (primitivo, nunca null)
        assertEquals(0L, Convert.coerceToLong(null));
        assertEquals(0L, Convert.coerceToLong(""));

        // Acepta String numérica
        assertEquals(123L, Convert.coerceToLong("123"));
        assertEquals(-123L, Convert.coerceToLong("-123"));

        // Acepta Number
        assertEquals(123L, Convert.coerceToLong(123L));
        assertEquals(123L, Convert.coerceToLong(123));

        // String no numérica lanza excepción
        assertThrows(NumberFormatException.class, () -> Convert.coerceToLong("abc"));
    }

    @Test
    void coerceToInt() {
        // null y String vacía devuelven 0
        assertEquals(0, Convert.coerceToInt(null));
        assertEquals(0, Convert.coerceToInt(""));

        // Acepta String numérica
        assertEquals(123, Convert.coerceToInt("123"));
        assertEquals(-123, Convert.coerceToInt("-123"));

        // Acepta Number
        assertEquals(123, Convert.coerceToInt(123L));
        assertEquals(123, Convert.coerceToInt(123));

        // String no numérica lanza excepción
        assertThrows(NumberFormatException.class, () -> Convert.coerceToInt("abc"));
    }

    /**
     * Axelor usa la zona por defecto de la JVM sin que se pueda configurar: sus propios
     * {@code LocalDateTime.now()} (auditoría, tokens...) y el serializador JSON que interpreta
     * TODO {@code LocalDateTime} en {@code ZoneId.systemDefault()} para mandarlo al navegador.
     * El proyecto en cambio fecha todo con {@code Convert.defaultZoneId}. Si las dos zonas no
     * coinciden, las fechas del proyecto y las de Axelor quedan desfasadas entre sí y el
     * navegador las muestra mal, así que la JVM MUST arrancar en la zona del proyecto
     * (por ejemplo con {@code TZ=Europe/Madrid} o {@code -Duser.timezone=Europe/Madrid}).
     */
    @Test
    void defaultZoneId_coincideConLaZonaDeLaJVM() {
        assertEquals(Convert.defaultZoneId, ZoneId.systemDefault(),
                "La JVM corre en la zona '" + ZoneId.systemDefault() + "' y el proyecto usa '"
                        + Convert.defaultZoneId + "': Axelor fecha con la de la JVM y el proyecto con la suya, "
                        + "y quedarían desfasadas. Arranca la JVM en " + Convert.defaultZoneId
                        + " (TZ=" + Convert.defaultZoneId + " o -Duser.timezone=" + Convert.defaultZoneId + ")");
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

        ZonedDateTime zonedDateTime = dateTime.atZone(Convert.defaultZoneId);
        assertEquals("28/08/2025 14:30", Convert.objectToUserString(zonedDateTime));

        OffsetDateTime offsetDateTime = dateTime.atOffset(Convert.defaultZoneId.getRules().getOffset(dateTime));
        assertEquals("28/08/2025 14:30", Convert.objectToUserString(offsetDateTime));

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
        @EnumWidget
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