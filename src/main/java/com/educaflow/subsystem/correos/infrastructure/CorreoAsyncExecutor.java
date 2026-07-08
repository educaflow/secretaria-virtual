package com.educaflow.subsystem.correos.infrastructure;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Envoltorio de un {@link ExecutorService} de tamaño fijo dedicado al envío asíncrono de correos.
 *
 * <p>Esta clase es un POJO deliberadamente ajeno a Guice: será gestionada (en una tarea futura)
 * como singleton por un {@code Provider} cuyo ciclo de vida esté atado a los eventos de
 * arranque/parada de la aplicación, invocando {@link #detener()} para evitar fugas de hilos al
 * redesplegar o parar Tomcat.
 */
public class CorreoAsyncExecutor {

    private static final Logger log = LoggerFactory.getLogger(CorreoAsyncExecutor.class);

    private final ExecutorService executorService;

    public CorreoAsyncExecutor(int tamanoPool) {
        this.executorService = Executors.newFixedThreadPool(tamanoPool, new CorreoThreadFactory());
    }

    public void submit(Runnable tarea) {
        executorService.submit(
                () -> {
                    try {
                        tarea.run();
                    } catch (RuntimeException ex) {
                        log.error("Fallo no controlado en el envío asíncrono de un correo", ex);
                    }
                });
    }

    public void detener() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
        }
    }

    /**
     * Fábrica de hilos daemon con nombre incremental {@code correo-envio-N}. Al ser daemon, si el
     * hook de parada no llegara a invocar {@link #detener()}, estos hilos no impiden que la
     * JVM/Tomcat termine.
     */
    private static final class CorreoThreadFactory implements ThreadFactory {

        private final AtomicInteger contador = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "correo-envio-" + contador.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
