package com.axelor.auth.service.impl;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.axelor.auth.db.User;
import com.axelor.db.Repository;
import com.educaflow.subsystem.common.db.CargoCodigo;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.common.db.CentroUsuarioCargo;
import com.educaflow.subsystem.common.db.repo.CentroUsuarioCargoRepository;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private Repository<User> userRepository;

    @Mock
    private CentroUsuarioCargoRepository centroUsuarioCargoRepository;

    private UserServiceImpl userService;

    private final Centro centro = new Centro();

    @BeforeEach
    void crearServicio() throws Exception {
        userService = new UserServiceImpl(User.class, userRepository);
        setField(userService, "centroUsuarioCargoRepository", centroUsuarioCargoRepository);
    }

    @Test
    void getByCentroAndCargo_centroConUnUnicoUsuarioConElCargo_devuelveSuUsuario() {
        User secretario = new User();
        when(centroUsuarioCargoRepository.findByCentroAndCargo(centro, CargoCodigo.SECRETARIO)).thenReturn(List.of(cargoDe(secretario)));

        assertSame(secretario, userService.getByCentroAndCargo(centro, CargoCodigo.SECRETARIO));
    }

    @Test
    void getByCentroAndCargo_centroSinNadieConElCargo_lanzaRuntimeExceptionQueNombraElCargo() {
        when(centroUsuarioCargoRepository.findByCentroAndCargo(centro, CargoCodigo.SECRETARIO)).thenReturn(List.of());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.getByCentroAndCargo(centro, CargoCodigo.SECRETARIO));

        assertTrue(mensajeDe(ex).contains(CargoCodigo.SECRETARIO.name()));
    }

    @Test
    void getByCentroAndCargo_centroConVariosUsuariosConElCargo_lanzaRuntimeExceptionQueNombraElCargo() {
        when(centroUsuarioCargoRepository.findByCentroAndCargo(centro, CargoCodigo.DIRECTOR))
                .thenReturn(List.of(cargoDe(new User()), cargoDe(new User())));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.getByCentroAndCargo(centro, CargoCodigo.DIRECTOR));

        assertTrue(mensajeDe(ex).contains(CargoCodigo.DIRECTOR.name()));
    }

    @Test
    void getByCentroAndCargo_ningunoYVarios_dicenCosasDistintas() {
        when(centroUsuarioCargoRepository.findByCentroAndCargo(centro, CargoCodigo.DIRECTOR)).thenReturn(List.of());
        RuntimeException ninguno = assertThrows(RuntimeException.class, () -> userService.getByCentroAndCargo(centro, CargoCodigo.DIRECTOR));

        when(centroUsuarioCargoRepository.findByCentroAndCargo(centro, CargoCodigo.DIRECTOR))
                .thenReturn(List.of(cargoDe(new User()), cargoDe(new User())));
        RuntimeException varios = assertThrows(RuntimeException.class, () -> userService.getByCentroAndCargo(centro, CargoCodigo.DIRECTOR));

        assertNotEquals(mensajeDe(ninguno), mensajeDe(varios));
    }

    @Test
    void getByCentroAndCargo_sinCentro_lanzaNullPointerSinConsultar() {
        assertThrows(NullPointerException.class, () -> userService.getByCentroAndCargo(null, CargoCodigo.DIRECTOR));

        verifyNoInteractions(centroUsuarioCargoRepository);
    }

    @Test
    void getByCentroAndCargo_sinCargo_lanzaNullPointerSinConsultar() {
        assertThrows(NullPointerException.class, () -> userService.getByCentroAndCargo(centro, null));

        verifyNoInteractions(centroUsuarioCargoRepository);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = UserServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static String mensajeDe(RuntimeException ex) {
        return ex.getMessage();
    }

    private static CentroUsuarioCargo cargoDe(User usuario) {
        CentroUsuario centroUsuario = new CentroUsuario();
        centroUsuario.setUsuario(usuario);

        CentroUsuarioCargo centroUsuarioCargo = new CentroUsuarioCargo();
        centroUsuarioCargo.setCentroUsuario(centroUsuario);

        return centroUsuarioCargo;
    }
}
