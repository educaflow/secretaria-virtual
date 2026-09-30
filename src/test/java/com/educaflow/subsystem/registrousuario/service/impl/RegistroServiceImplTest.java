package com.educaflow.subsystem.registrousuario.service.impl;

import com.axelor.auth.db.User;
import com.axelor.db.JpaRepository;
import com.axelor.db.Query;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.BusinessMessages;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistroServiceImplTest {

    private static final String DNI = "12345678Z";
    private static final String EMAIL = "usuario@example.com";

    private RegistroServiceImpl service;
    private MockedStatic<JpaRepository> jpaRepositoryMock;
    private JpaRepository<User> userRepository;
    private Query<User> query;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new RegistroServiceImpl(User.class, Mockito.mock(Repository.class));
        userRepository = Mockito.mock(JpaRepository.class);
        query = Mockito.mock(Query.class);
        jpaRepositoryMock = Mockito.mockStatic(JpaRepository.class);
        jpaRepositoryMock.when(() -> JpaRepository.of(User.class)).thenReturn(userRepository);
        when(userRepository.all()).thenReturn(query);
        when(query.filter("self.dni = :dni")).thenReturn(query);
        when(query.bind("dni", DNI)).thenReturn(query);
    }

    @AfterEach
    void tearDown() {
        jpaRepositoryMock.close();
    }

    private void usuarioEnBaseDeDatos(User user) {
        when(query.fetchOne()).thenReturn(user);
    }

    private static User usuarioConEmail(String email) {
        User user = new User();
        user.setEmail(email);
        return user;
    }

    @Test
    void findEmailByDni_validacionConErrores_lanzaIllegalArgumentExceptionSinConsultar() {
        RegistroServiceImpl spy = Mockito.spy(service);
        BusinessMessages mensajes = new BusinessMessages();
        doReturn(Optional.of(mensajes)).when(spy).validateFindEmailByDni(DNI);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> spy.findEmailByDni(DNI));

        assertEquals(mensajes.toString(), ex.getMessage());
        jpaRepositoryMock.verifyNoInteractions();
    }

    @Test
    void findEmailByDni_dniNull_devuelveVacioSinConsultar() {
        assertEquals(Optional.empty(), service.findEmailByDni(null));
        jpaRepositoryMock.verifyNoInteractions();
    }

    @Test
    void findEmailByDni_dniEnBlanco_devuelveVacioSinConsultar() {
        assertEquals(Optional.empty(), service.findEmailByDni("   "));
        jpaRepositoryMock.verifyNoInteractions();
    }

    @Test
    void findEmailByDni_sinUsuario_devuelveVacio() {
        usuarioEnBaseDeDatos(null);

        assertEquals(Optional.empty(), service.findEmailByDni(DNI));
        verify(query).bind("dni", DNI);
    }

    @Test
    void findEmailByDni_usuarioSinEmail_devuelveVacio() {
        usuarioEnBaseDeDatos(usuarioConEmail(null));

        assertEquals(Optional.empty(), service.findEmailByDni(DNI));
    }

    @Test
    void findEmailByDni_usuarioConEmailEnBlanco_devuelveVacio() {
        usuarioEnBaseDeDatos(usuarioConEmail("  "));

        assertEquals(Optional.empty(), service.findEmailByDni(DNI));
    }

    @Test
    void findEmailByDni_usuarioConEmail_devuelveEmail() {
        usuarioEnBaseDeDatos(usuarioConEmail(EMAIL));

        assertEquals(Optional.of(EMAIL), service.findEmailByDni(DNI));
    }
}
