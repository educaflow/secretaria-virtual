package com.educaflow.subsystem.expedientes.services;

import com.axelor.auth.db.User;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.expedientes.db.Tramite;
import com.educaflow.subsystem.expedientes.services.internal.ExpedienteSecurity;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Lo que la pantalla «Nuevo expediente» necesita saber para preguntar solo lo que tiene sentido: en
 * qué centros puede crear el usuario y, en cada uno, si presenta él o registra una solicitud
 * entregada en papel, y para quién.
 *
 * <p>No decide nada por su cuenta: todo sale de {@link ExpedienteSecurity#puedeCrear}, la misma regla
 * con la que {@code ExpedienteService} autoriza el alta de verdad. Así la pantalla no puede ofrecer
 * algo que el servidor luego rechace, ni al revés.
 *
 * <p><b>Ojo con el paquete:</b> vive en {@code …expedientes.services} (plural). {@code
 * ModelServiceFactory} de Axelor resuelve el {@code ModelService} de la entidad {@code
 * …expedientes.db.ContextoTramitacion} buscando {@code …expedientes.service.ContextoTramitacionService}
 * (singular), así que hoy no colisionan. Mover esta clase a un paquete {@code service} haría que
 * Axelor la tomara por el {@code ModelService} de la entidad y reventara por no implementarlo.
 */
public class ContextoTramitacionService {

    /**
     * Los centros del usuario en los que puede crear algún expediente del trámite, ordenados por
     * nombre. Un centro aparece si puede presentar él o si puede registrar en papel.
     */
    public List<Centro> getCentros(Tramite tramite, User user) {
        if ((tramite == null) || (user == null) || (user.getCentroUsuarios() == null)) {
            return List.of();
        }

        return user.getCentroUsuarios().stream()
                .map(CentroUsuario::getCentro)
                .filter(Objects::nonNull)
                .filter(centro -> puedePresentarElUsuario(tramite, user, centro) || puedeRegistrarEnPapel(tramite, user, centro))
                .sorted(Comparator.comparing(Centro::getName, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /** Si el usuario puede presentar el trámite por sí mismo, para él o en representación de otra persona. */
    public boolean puedePresentarElUsuario(Tramite tramite, User user, Centro centro) {
        return puedeCrear(tramite, user, centro, false, false)
                || puedeCrear(tramite, user, centro, false, true);
    }

    /** Si el usuario puede registrar en el centro una solicitud que le han entregado en papel. */
    public boolean puedeRegistrarEnPapel(Tramite tramite, User user, Centro centro) {
        return puedeCrear(tramite, user, centro, true, false)
                || puedeCrear(tramite, user, centro, true, true);
    }

    public boolean puedeCrear(Tramite tramite, User user, Centro centro, boolean presentadoEnPapel, boolean presentadoEnRepresentacion) {
        return ExpedienteSecurity.puedeCrear(tramite, user, centro, presentadoEnPapel, presentadoEnRepresentacion);
    }

}
