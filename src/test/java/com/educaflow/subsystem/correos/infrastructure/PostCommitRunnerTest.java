package com.educaflow.subsystem.correos.infrastructure;

import com.axelor.db.JPA;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de {@link PostCommitRunner}: helper que registra una tarea para que se ejecute únicamente
 * si la transacción JPA actual termina en commit, nunca en rollback.
 *
 * <p>Colaboradores mockeados: el estático {@link JPA} y los mocks de {@link EntityManager},
 * {@link Session} y {@link Transaction} que forman la cadena {@code JPA.em().unwrap(Session.class)
 * .getTransaction()}.
 */
@ExtendWith(MockitoExtension.class)
class PostCommitRunnerTest {

    @Mock private EntityManager entityManagerMock;
    @Mock private Session sessionMock;
    @Mock private Transaction transactionMock;
    @Mock private Runnable tarea;

    @Test
    void runAfterCommit_transaccionHaceCommit_ejecutaLaTarea() {
        try (MockedStatic<JPA> jpaMock = mockStatic(JPA.class)) {
            jpaMock.when(JPA::em).thenReturn(entityManagerMock);
            when(entityManagerMock.unwrap(Session.class)).thenReturn(sessionMock);
            when(sessionMock.getTransaction()).thenReturn(transactionMock);

            PostCommitRunner.runAfterCommit(tarea);

            ArgumentCaptor<Synchronization> captor = ArgumentCaptor.forClass(Synchronization.class);
            verify(transactionMock).registerSynchronization(captor.capture());

            captor.getValue().afterCompletion(Status.STATUS_COMMITTED);
        }

        verify(tarea).run();
    }

    @Test
    void runAfterCommit_transaccionHaceRollback_noEjecutaLaTarea() {
        try (MockedStatic<JPA> jpaMock = mockStatic(JPA.class)) {
            jpaMock.when(JPA::em).thenReturn(entityManagerMock);
            when(entityManagerMock.unwrap(Session.class)).thenReturn(sessionMock);
            when(sessionMock.getTransaction()).thenReturn(transactionMock);

            PostCommitRunner.runAfterCommit(tarea);

            ArgumentCaptor<Synchronization> captor = ArgumentCaptor.forClass(Synchronization.class);
            verify(transactionMock).registerSynchronization(captor.capture());

            captor.getValue().afterCompletion(Status.STATUS_ROLLEDBACK);
        }

        verify(tarea, never()).run();
    }
}
