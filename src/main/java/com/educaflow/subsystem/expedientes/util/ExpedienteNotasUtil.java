package com.educaflow.subsystem.expedientes.util;

import com.axelor.auth.db.User;
import com.axelor.db.JpaRepository;
import com.educaflow.base.util.Convert;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.base.util.TextUtil;
import com.educaflow.subsystem.expedientes.db.Expediente;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Las notas de un expediente: lo que se dicen entre sí quienes lo tramitan, sin ser parte oficial de él.
 *
 * <p>Cada nota empieza en una línea nueva de {@code Expediente.notas} con la forma
 * {@code 02/10/2026 08:32 Nombre del usuario → mensaje}; las demás líneas de su mensaje van tabuladas. Se guardan directamente, sin pasar por el
 * tramitador: añadir una nota no es un evento ni cambia el estado.
 *
 * <p>Las notas no son para el creador del expediente, que ni las ve ni las añade. Lo que deba ver el
 * creador va en un campo propio del tipo de expediente.
 */
public class ExpedienteNotasUtil {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Añade una nota al expediente, a nombre del usuario autenticado y con la fecha actual, y lo guarda.
     * Necesita una transacción abierta. No comprueba quién la añade: eso es de quien llama.
     */
    public static void addNote(Expediente expediente, String mensaje) {
        String nota = formatNota(LocalDateTime.now(Convert.defaultZoneId), SecurityUtil.getUser().getName(), mensaje);
        String notas = expediente.getNotas();

        expediente.setNotas((notas == null || notas.isEmpty()) ? nota : notas + "\n" + nota);
        JpaRepository.of(Expediente.class).save(expediente);
    }

    /**
     * El texto de una nota. Cada línea del mensaje a partir de la segunda empieza por un tabulador: una
     * nota empieza siempre al principio de la línea, así que nadie puede colar dentro de la suya otra
     * que parezca de otro usuario.
     */
    static String formatNota(LocalDateTime fecha, String nombreUsuario, String mensaje) {
        TextUtil.requireNonBlank(mensaje, "mensaje no puede ser null ni blank");

        return fecha.format(FORMATO_FECHA) + " " + nombreUsuario + " → " + mensaje.strip().replaceAll("\\r\\n?|\\n", "\n\t");
    }

    /**
     * Si el usuario es el creador del expediente a efectos de las notas: quien lo registró, salvo que
     * se presentara en papel, porque entonces quien lo registró es personal del centro.
     */
    public static boolean esCreador(Expediente expediente, User user) {
        User usuarioRegistrador = expediente.getUsuarioRegistrador();
        if (usuarioRegistrador == null) {
            throw new IllegalStateException("El expediente " + expediente.getNumeroExpediente() + " no tiene usuarioRegistrador");
        }

        return usuarioRegistrador.getId().equals(user.getId()) && Boolean.TRUE.equals(expediente.getPresentadoEnPapel()) == false;
    }

}
