package com.educaflow.tiposexpedientes.support;

import com.educaflow.common.buildtools.files.tipoexpediente.Fase;

import java.nio.file.Path;
import java.util.List;

/**
 * Un {@code <form state="…">} del {@code views.xml} de una fase, ya parseado: el estado al que
 * atiende, el perfil para el que se pinta (vacío = el form genérico de reserva) y los botones de su
 * {@code <footer>}, que son los que disparan eventos.
 *
 * <p>La fase <b>no</b> está en el fichero: la deduce el viewprocessor del nombre de la carpeta, así
 * que aquí viene de la {@link Fase} cuyo {@code views.xml} se estaba leyendo.
 */
public record FormDeEstado(Fase fase, Path fichero, String state, String profile, List<Boton> botones) {

    /**
     * Un botón del {@code <footer>}. Su {@code name} <b>es</b> el evento que dispara y su
     * {@code onClick} la acción que lo lleva al servidor.
     */
    public record Boton(String name, String onClick) {}

    /** El form de reserva, sin perfil: al que cae el runtime cuando no existe el del perfil actuante. */
    public boolean esGenerico() {
        return profile.isBlank();
    }

    /**
     * El nombre global de la vista que compondrá el viewprocessor, y que buscará en runtime
     * {@code PhaseEventManager.getViewName}. Solo se usa en los mensajes de error: el formato está
     * duplicado aquí a propósito —el del generador es privado—, y como no se compara con nada, si
     * alguna vez divergiera el único efecto sería un mensaje con un nombre desfasado.
     */
    public String nombreVista() {
        String base = "exp-" + fase.getTipoExpediente().getCode() + "-" + fase.getName() + "-" + state;

        return esGenerico() ? (base + "-form") : (base + "-" + profile + "-form");
    }

    /** Cómo se identifica el form en los mensajes de error. */
    @Override
    public String toString() {
        return esGenerico() ? ("<form state=\"" + state + "\">")
                : ("<form state=\"" + state + "\" profile=\"" + profile + "\">");
    }
}
