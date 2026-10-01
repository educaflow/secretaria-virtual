package com.educaflow.base.infrastructure.async;

import com.axelor.db.JPA;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EjecutorAsincronoTest {

    private static final long TIMEOUT_SEGUNDOS = 5;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Session session;

    @Mock
    private Transaction transaction;

    @Mock
    private Runnable tarea;

    private MockedStatic<JPA> jpa;

    private final List<EjecutorAsincrono> ejecutores = new ArrayList<>();

    @BeforeEach
    void setUp() {
        jpa = Mockito.mockStatic(JPA.class);
    }

    @AfterEach
    void tearDown() {
        Thread.interrupted();
        ejecutores.forEach(EjecutorAsincrono::detener);
        jpa.close();
    }

    /*************************************** Constructor ***************************************/

    @Test
    void constructor_tareaEjecutada_correEnUnHiloDaemonLlamadoAsyncN() throws InterruptedException {
        EjecutorAsincrono ejecutor = nuevoEjecutor();
        conTransaccion(true);
        AtomicReference<Thread> hilo = new AtomicReference<>();
        CountDownLatch ejecutada = new CountDownLatch(1);

        programarTrasCommit(ejecutor, () -> {
            hilo.set(Thread.currentThread());
            ejecutada.countDown();
        }).afterCompletion(Status.STATUS_COMMITTED);

        assertTrue(ejecutada.await(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS));
        assertTrue(hilo.get().isDaemon());
        assertTrue(hilo.get().getName().startsWith("async-"));
    }

    /*************************************** ejecutarTrasCommit ***************************************/

    @Test
    void ejecutarTrasCommit_transaccionHaceCommit_ejecutaLaTarea() throws InterruptedException {
        EjecutorAsincrono ejecutor = nuevoEjecutor();
        conTransaccion(true);
        CountDownLatch ejecutada = new CountDownLatch(1);

        programarTrasCommit(ejecutor, ejecutada::countDown).afterCompletion(Status.STATUS_COMMITTED);

        assertTrue(ejecutada.await(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS));
    }

    @Test
    void ejecutarTrasCommit_transaccionHaceRollback_noEjecutaLaTarea() throws InterruptedException {
        EjecutorAsincrono ejecutor = nuevoEjecutor();
        conTransaccion(true);

        CountDownLatch centinelaEjecutada = new CountDownLatch(1);

        programarTrasCommit(ejecutor, tarea).afterCompletion(Status.STATUS_ROLLEDBACK);
        programarTrasCommit(ejecutor, centinelaEjecutada::countDown).afterCompletion(Status.STATUS_COMMITTED);

        assertTrue(centinelaEjecutada.await(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS));
        verify(tarea, never()).run();
    }

    @Test
    void ejecutarTrasCommit_sinTransaccionActiva_lanzaIllegalStateExceptionSinRegistrarNada() {
        EjecutorAsincrono ejecutor = nuevoEjecutor();
        conTransaccion(false);

        assertThrows(IllegalStateException.class, () -> ejecutor.ejecutarTrasCommit(tarea));

        verify(transaction, never()).registerSynchronization(any());
        verify(tarea, never()).run();
    }

    @Test
    void ejecutarTrasCommit_sinTransaccionEnLaSesion_lanzaIllegalStateException() {
        EjecutorAsincrono ejecutor = nuevoEjecutor();
        conSesion(null);

        assertThrows(IllegalStateException.class, () -> ejecutor.ejecutarTrasCommit(tarea));

        verify(tarea, never()).run();
    }

    @Test
    void ejecutarTrasCommit_tareaLanzaRuntimeException_noPropagaYElPoolSigueUtilizable()
            throws InterruptedException {
        EjecutorAsincrono ejecutor = nuevoEjecutor();
        conTransaccion(true);
        AtomicReference<Thread> hiloPrimera = new AtomicReference<>();
        AtomicReference<Thread> hiloSegunda = new AtomicReference<>();
        CountDownLatch segundaEjecutada = new CountDownLatch(1);
        Synchronization primera = programarTrasCommit(ejecutor, () -> {
            hiloPrimera.set(Thread.currentThread());
            throw new RuntimeException("boom");
        });
        Synchronization segunda = programarTrasCommit(ejecutor, () -> {
            hiloSegunda.set(Thread.currentThread());
            segundaEjecutada.countDown();
        });

        assertDoesNotThrow(() -> {
            primera.afterCompletion(Status.STATUS_COMMITTED);
            segunda.afterCompletion(Status.STATUS_COMMITTED);
        });

        assertTrue(segundaEjecutada.await(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS));
        assertSame(hiloPrimera.get(), hiloSegunda.get());
    }

    /*************************************** detener ***************************************/

    @Test
    void detener_conTareaEnCurso_esperaSuFinalizacionAntesDeCerrar() {
        EjecutorAsincrono ejecutor = nuevoEjecutor();
        conTransaccion(true);
        AtomicBoolean terminada = new AtomicBoolean(false);

        programarTrasCommit(ejecutor, () -> {
            dormir(200);
            terminada.set(true);
        }).afterCompletion(Status.STATUS_COMMITTED);
        ejecutor.detener();

        assertTrue(terminada.get());
    }

    @Test
    void detener_trasHaberDetenido_elPoolRechazaNuevasTareas() {
        EjecutorAsincrono ejecutor = nuevoEjecutor();
        ejecutor.detener();
        conTransaccion(true);

        Synchronization synchronization = programarTrasCommit(ejecutor, tarea);

        assertThrows(RejectedExecutionException.class,
                () -> synchronization.afterCompletion(Status.STATUS_COMMITTED));
        verify(tarea, never()).run();
    }

    @Test
    void detener_hiloInterrumpido_propagaLaInterrupcionYFuerzaElCierre() throws InterruptedException {
        EjecutorAsincrono ejecutor = nuevoEjecutor();
        conTransaccion(true);
        CountDownLatch enCurso = new CountDownLatch(1);
        CountDownLatch interrumpida = new CountDownLatch(1);
        programarTrasCommit(ejecutor, () -> {
            enCurso.countDown();
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(30));
            } catch (InterruptedException ex) {
                interrumpida.countDown();
            }
        }).afterCompletion(Status.STATUS_COMMITTED);
        assertTrue(enCurso.await(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS));

        Thread.currentThread().interrupt();
        ejecutor.detener();

        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted();
        assertTrue(interrumpida.await(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS));
        Synchronization posterior = programarTrasCommit(ejecutor, tarea);
        assertThrows(RejectedExecutionException.class,
                () -> posterior.afterCompletion(Status.STATUS_COMMITTED));
        verify(tarea, never()).run();
    }

    /*************************************** Utilidades ***************************************/

    private EjecutorAsincrono nuevoEjecutor() {
        EjecutorAsincrono ejecutor = new EjecutorAsincrono(1);
        ejecutores.add(ejecutor);
        return ejecutor;
    }

    private void conSesion(Transaction transaccionDeLaSesion) {
        jpa.when(JPA::em).thenReturn(entityManager);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.getTransaction()).thenReturn(transaccionDeLaSesion);
    }

    private void conTransaccion(boolean activa) {
        conSesion(transaction);
        when(transaction.isActive()).thenReturn(activa);
    }

    private Synchronization programarTrasCommit(EjecutorAsincrono ejecutor, Runnable tareaProgramada) {
        ejecutor.ejecutarTrasCommit(tareaProgramada);
        ArgumentCaptor<Synchronization> captor = ArgumentCaptor.forClass(Synchronization.class);
        verify(transaction, atLeastOnce()).registerSynchronization(captor.capture());
        return captor.getValue();
    }

    private static void dormir(long milisegundos) {
        try {
            Thread.sleep(milisegundos);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
