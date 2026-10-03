package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1;

import com.axelor.i18n.I18n;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.InitialEventContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InitialEventManagerImplTest {

    private MockedStatic<I18n> i18nMock;

    @BeforeEach
    void setUp() {
        i18nMock = Mockito.mockStatic(I18n.class, Mockito.withSettings().strictness(Strictness.LENIENT));
        i18nMock.when(() -> I18n.get(any(String.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        i18nMock.close();
    }

    @SuppressWarnings("unchecked")
    private InitialEventContext<AnulacionMatriculaCicloFormativoV1> contextoConCurso(AnulacionMatriculaCicloFormativoV1 expediente, Integer curso) {
        Centro centro = new Centro();
        centro.setCurso(curso);
        expediente.setCentro(centro);
        expediente.setPresentadoEnPapel(false);
        InitialEventContext<AnulacionMatriculaCicloFormativoV1> contexto = mock(InitialEventContext.class);
        when(contexto.getExpediente()).thenReturn(expediente);
        return contexto;
    }

    @Test
    void triggerInitialEvent_centroConCursoCero_lanzaBusinessException() {
        InitialEventContext<AnulacionMatriculaCicloFormativoV1> contexto = contextoConCurso(new AnulacionMatriculaCicloFormativoV1(), 0);

        assertThrows(BusinessException.class, () -> new InitialEventManagerImpl().triggerInitialEvent(contexto));
    }

    @Test
    void triggerInitialEvent_centroConCurso_asignaElCursoAcademico() throws BusinessException {
        AnulacionMatriculaCicloFormativoV1 expediente = new AnulacionMatriculaCicloFormativoV1();

        new InitialEventManagerImpl().triggerInitialEvent(contextoConCurso(expediente, 2024));

        assertEquals("2024/2025", expediente.getCursoAcademico());
    }
}
