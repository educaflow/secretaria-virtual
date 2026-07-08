package com.educaflow.subsystem.correos.infrastructure;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests de {@link CorreoAsyncExecutor}: envoltorio de un {@code ExecutorService} de tamaño fijo con
 * hilos daemon nombrados, ejecución de tareas aislada de fallos y parada ordenada.
 *
 * <p>No hay colaboradores que mockear: se usa un {@code ExecutorService} real con un pool pequeño
 * para observar el comportamiento real de los hilos.
 */
class CorreoAsyncExecutorTest {

    private static final long TIMEOUT_SEGUNDOS = 2L;

    private final List<CorreoAsyncExecutor> executoresACerrar = new ArrayList<>();

    @AfterEach
    void cerrarExecutoresPendientes() {
        for (CorreoAsyncExecutor executor : executoresACerrar) {
            executor.detener();
        }
    }

    @Test
    void submit_tareaValida_seEjecutaEnUnHiloDaemonConNombreCorreoEnvio() throws InterruptedException {
        CorreoAsyncExecutor executor = new CorreoAsyncExecutor(1);
        executoresACerrar.add(executor);

        AtomicReference<Thread> hiloCapturado = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Runnable tarea =
                () -> {
                    hiloCapturado.set(Thread.currentThread());
                    latch.countDown();
                };

        executor.submit(tarea);
        boolean completada = latch.await(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);

        assertTrue(completada, "La tarea debería haberse ejecutado dentro del timeout");
        Thread hilo = hiloCapturado.get();
        assertNotNull(hilo, "La tarea debería haber capturado el hilo de ejecución");
        assertTrue(hilo.isDaemon(), "El hilo del pool debe ser daemon");
        assertTrue(
                hilo.getName().startsWith("correo-envio-"),
                "El nombre del hilo debe empezar por 'correo-envio-', pero fue: " + hilo.getName());
    }

    @Test
    void submit_tareaLanzaRuntimeException_noPropagaYElPoolSigueUtilizable() throws InterruptedException {
        CorreoAsyncExecutor executor = new CorreoAsyncExecutor(1);
        executoresACerrar.add(executor);

        CountDownLatch latch = new CountDownLatch(1);
        Runnable tareaQueFalla =
                () -> {
                    throw new RuntimeException("boom");
                };
        Runnable tareaQueMarcaLatch = latch::countDown;

        assertDoesNotThrow(
                () -> {
                    executor.submit(tareaQueFalla);
                    executor.submit(tareaQueMarcaLatch);
                });
        boolean completada = latch.await(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);

        assertTrue(
                completada,
                "El hilo del pool debe seguir vivo y procesar la segunda tarea pese al fallo de la primera");
    }

    @Test
    void detener_sinTareasPendientes_terminaYRechazaNuevosEnvios() {
        CorreoAsyncExecutor executor = new CorreoAsyncExecutor(1);
        executoresACerrar.add(executor);

        executor.detener();

        assertThrows(
                RejectedExecutionException.class,
                () -> executor.submit(() -> {}),
                "Tras detener(), el ExecutorService interno ya está cerrado y debe rechazar nuevos envíos");
    }

    @Test
    void detener_conTareaEnCurso_esperaSuFinalizacionAntesDeCerrar() {
        CorreoAsyncExecutor executor = new CorreoAsyncExecutor(1);
        executoresACerrar.add(executor);

        AtomicBoolean completada = new AtomicBoolean(false);
        Runnable tareaLenta =
                () -> {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    completada.set(true);
                };

        executor.submit(tareaLenta);
        executor.detener();

        assertTrue(
                completada.get(),
                "detener() debe esperar (awaitTermination) a que la tarea en curso termine antes de cerrar");
    }
}
