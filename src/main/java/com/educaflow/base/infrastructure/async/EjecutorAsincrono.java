package com.educaflow.base.infrastructure.async;

import com.axelor.db.JPA;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EjecutorAsincrono {

    private static final Logger log = LoggerFactory.getLogger(EjecutorAsincrono.class);

    private final ExecutorService executorService;

    public EjecutorAsincrono(int tamanoPool) {
        this.executorService = Executors.newFixedThreadPool(tamanoPool, new HiloDaemonFactory());
    }

    public void ejecutarTrasCommit(Runnable tarea) {
        Objects.requireNonNull(tarea, "tarea no puede ser null");
        Transaction transaction = JPA.em().unwrap(Session.class).getTransaction();
        if (transaction == null || !transaction.isActive()) {
            throw new IllegalStateException(
                    "ejecutarTrasCommit requiere una transacción activa en el hilo actual");
        }
        transaction.registerSynchronization(new EnvioTrasCommit(tarea));
    }

    public void detener() {
        executorService.shutdown();
        try {
            esperarTerminacion();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
        }
    }

    private void esperarTerminacion() throws InterruptedException {
        if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
            executorService.shutdownNow();
        }
    }

    private void enviar(Runnable tarea) {
        executorService.execute(() -> ejecutarRegistrandoFallos(tarea));
    }

    private static void ejecutarRegistrandoFallos(Runnable tarea) {
        try {
            tarea.run();
        } catch (RuntimeException ex) {
            log.error("Fallo no controlado en una tarea asíncrona", ex);
        }
    }

    private final class EnvioTrasCommit implements Synchronization {

        private final Runnable tarea;

        private EnvioTrasCommit(Runnable tarea) {
            this.tarea = tarea;
        }

        @Override
        public void beforeCompletion() {}

        @Override
        public void afterCompletion(int status) {
            if (status == Status.STATUS_COMMITTED) {
                enviar(tarea);
            }
        }
    }

    private static final class HiloDaemonFactory implements ThreadFactory {

        private final AtomicInteger contador = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "async-" + contador.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
