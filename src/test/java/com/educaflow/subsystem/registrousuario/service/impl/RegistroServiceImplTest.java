package com.educaflow.subsystem.registrousuario.service.impl;

import com.axelor.auth.db.User;
import com.axelor.auth.db.repo.UserRepository;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.BusinessMessages;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RegistroServiceImplTest {

    private static final String DNI = "93882914L";
    private static final String EMAIL = "usuario@example.com";

    private RegistroServiceImpl service;
    private UserRepository userRepository;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        service = new RegistroServiceImpl(User.class, Mockito.mock(Repository.class));
        userRepository = Mockito.mock(UserRepository.class);
        Field field = RegistroServiceImpl.class.getDeclaredField("userRepository");
        field.setAccessible(true);
        field.set(service, userRepository);
    }

    private void usuarioEnBaseDeDatos(User user) {
        when(userRepository.findByDni(DNI)).thenReturn(user);
    }

    private static User usuarioConEmail(String email) {
        User user = new User();
        user.setEmail(email);
        return user;
    }

    @Test
    void findEmailByDni_validacionConErrores_lanzaValidationExceptionSinConsultar() {
        RegistroServiceImpl spy = Mockito.spy(service);
        BusinessMessages mensajes = BusinessMessages.single("El DNI no es válido");
        doReturn(Optional.of(mensajes)).when(spy).validateFindEmailByDni(DNI);

        ValidationException ex = assertThrows(ValidationException.class, () -> spy.findEmailByDni(DNI));

        assertEquals(mensajes.toString(), ex.getMessage());
        verifyNoInteractions(userRepository);
    }

    @Test
    void findEmailByDni_dniNull_devuelveVacioSinConsultar() {
        assertEquals(Optional.empty(), service.findEmailByDni(null));
        verifyNoInteractions(userRepository);
    }

    @Test
    void findEmailByDni_dniEnBlanco_devuelveVacioSinConsultar() {
        assertEquals(Optional.empty(), service.findEmailByDni("   "));
        verifyNoInteractions(userRepository);
    }

    @Test
    void findEmailByDni_sinUsuario_devuelveVacio() {
        usuarioEnBaseDeDatos(null);

        assertEquals(Optional.empty(), service.findEmailByDni(DNI));
        verify(userRepository).findByDni(DNI);
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
