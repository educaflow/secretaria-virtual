package com.educaflow.subsystem.importacion.service.tipoimportador;

import java.util.List;

public record ResultadoImportacion(String resumen, List<MensajeImportacion> mensajes) {

    public record MensajeImportacion(Integer fila, String dato, String mensaje) {}
}