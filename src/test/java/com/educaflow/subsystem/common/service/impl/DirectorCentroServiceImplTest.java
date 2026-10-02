package com.educaflow.subsystem.common.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.axelor.auth.db.User;
import com.axelor.i18n.I18n;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.subsystem.common.db.CargoCodigo;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.common.db.CentroUsuarioCargo;
import com.educaflow.subsystem.common.db.repo.CentroUsuarioCargoRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
class DirectorCentroServiceImplTest {

    @Mock
    private CentroUsuarioCargoRepository centroUsuarioCargoRepository;

    @InjectMocks
    private DirectorCentroServiceImpl directorCentroService;

    private MockedStatic<I18n> i18nMock;

    private final Centro centro = new Centro();

    @BeforeEach
    void mockearEstaticos() {
        i18nMock = Mockito.mockStatic(I18n.class, Mockito.withSettings().strictness(Strictness.LENIENT));
        i18nMock.when(() -> I18n.get(anyString())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @AfterEach
    void cerrarEstaticos() {
        i18nMock.close();
    }

    @Test
    void getDirector_centroConUnUnicoDirector_devuelveSuUsuario() throws BusinessException {
        User director = new User();
        when(centroUsuarioCargoRepository.findByCentroAndCargo(centro, CargoCodigo.DIRECTOR)).thenReturn(List.of(cargoDe(director)));

        assertSame(director, directorCentroService.getDirector(centro));
    }

    @Test
    void getDirector_centroSinDirector_lanzaBusinessException() {
        when(centroUsuarioCargoRepository.findByCentroAndCargo(centro, CargoCodigo.DIRECTOR)).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class, () -> directorCentroService.getDirector(centro));

        assertEquals("El centro no tiene ningún usuario con el cargo de director; avise a quien gestiona los cargos del centro",
                ex.getBusinessMessages().get(0).getMessage());
    }

    @Test
    void getDirector_centroConVariosDirectores_lanzaBusinessException() {
        when(centroUsuarioCargoRepository.findByCentroAndCargo(centro, CargoCodigo.DIRECTOR))
                .thenReturn(List.of(cargoDe(new User()), cargoDe(new User())));

        BusinessException ex = assertThrows(BusinessException.class, () -> directorCentroService.getDirector(centro));

        assertEquals("El centro tiene más de un usuario con el cargo de director; avise a quien gestiona los cargos del centro",
                ex.getBusinessMessages().get(0).getMessage());
    }

    @Test
    void getDirector_sinCentro_lanzaNullPointerSinConsultar() {
        assertThrows(NullPointerException.class, () -> directorCentroService.getDirector(null));

        verifyNoInteractions(centroUsuarioCargoRepository);
    }

    private static CentroUsuarioCargo cargoDe(User usuario) {
        CentroUsuario centroUsuario = new CentroUsuario();
        centroUsuario.setUsuario(usuario);

        CentroUsuarioCargo centroUsuarioCargo = new CentroUsuarioCargo();
        centroUsuarioCargo.setCentroUsuario(centroUsuario);

        return centroUsuarioCargo;
    }
}
