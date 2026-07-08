package com.educaflow.subsystem.correos.infrastructure;

import com.axelor.db.JPA;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import org.hibernate.Session;

/**
 * Utilidad estática, sin estado, para programar la ejecución de una tarea justo después de que la
 * transacción actual del hilo que invoca {@link #runAfterCommit(Runnable)} haga commit.
 *
 * <p>No tiene binding de Guice: opera directamente sobre el {@link jakarta.persistence.EntityManager}
 * de la transacción actual del hilo invocante, así que no necesita ser gestionada como bean.
 */
public final class PostCommitRunner {

    private PostCommitRunner() {}

    public static void runAfterCommit(Runnable tarea) {
        Session session = JPA.em().unwrap(Session.class);
        session
                .getTransaction()
                .registerSynchronization(
                        new Synchronization() {
                            @Override
                            public void beforeCompletion() {}

                            @Override
                            public void afterCompletion(int status) {
                                if (status == Status.STATUS_COMMITTED) {
                                    tarea.run();
                                }
                            }
                        });
    }
}
