package com.educaflow.subsystem.expedientes.util;

import com.axelor.auth.db.User;
import com.educaflow.subsystem.expedientes.db.Expediente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpedienteNotasUtilTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 10, 2, 8, 32);

    @Test
    @DisplayName("formatNota: la nota es fecha, hora, nombre del usuario y mensaje")
    void formatNota_formatoDeLaLinea() {
        String nota = ExpedienteNotasUtil.formatNota(FECHA, "Lorenzo González", "Falta el justificante");

        assertEquals("02/10/2026 08:32 Lorenzo González → Falta el justificante", nota);
    }

    @Test
    @DisplayName("formatNota: las líneas del mensaje a partir de la segunda empiezan por un tabulador")
    void formatNota_lineasSiguientesTabuladas() {
        String nota = ExpedienteNotasUtil.formatNota(FECHA, "Ana", "  uno\r\ndos\rtres\n\n03/10/2026 09:00 Otro → falsa  ");

        assertEquals("02/10/2026 08:32 Ana → uno\n\tdos\n\ttres\n\t\n\t03/10/2026 09:00 Otro → falsa", nota);
    }

    @Test
    @DisplayName("formatNota: un mensaje vacío no es una nota")
    void formatNota_mensajeVacio() {
        assertThrows(IllegalArgumentException.class, () -> ExpedienteNotasUtil.formatNota(FECHA, "Ana", "   "));
        assertThrows(NullPointerException.class, () -> ExpedienteNotasUtil.formatNota(FECHA, "Ana", null));
    }

    @Test
    @DisplayName("esCreador: quien registró el expediente es su creador")
    void esCreador_usuarioRegistrador() {
        assertTrue(ExpedienteNotasUtil.esCreador(expediente(1L, false), user(1L)));
    }

    @Test
    @DisplayName("esCreador: quien registró un expediente presentado en papel no es su creador")
    void esCreador_presentadoEnPapel() {
        assertFalse(ExpedienteNotasUtil.esCreador(expediente(1L, true), user(1L)));
    }

    @Test
    @DisplayName("esCreador: otro usuario no es el creador")
    void esCreador_otroUsuario() {
        assertFalse(ExpedienteNotasUtil.esCreador(expediente(1L, false), user(2L)));
    }

    @Test
    @DisplayName("esCreador: un expediente sin usuarioRegistrador no debería existir")
    void esCreador_sinUsuarioRegistrador() {
        Expediente expediente = new Expediente();

        assertThrows(IllegalStateException.class, () -> ExpedienteNotasUtil.esCreador(expediente, user(1L)));
    }

    private static Expediente expediente(Long idUsuarioRegistrador, boolean presentadoEnPapel) {
        Expediente expediente = new Expediente();
        expediente.setUsuarioRegistrador(user(idUsuarioRegistrador));
        expediente.setPresentadoEnPapel(presentadoEnPapel);

        return expediente;
    }

    private static User user(Long id) {
        User user = new User();
        user.setId(id);

        return user;
    }

}
