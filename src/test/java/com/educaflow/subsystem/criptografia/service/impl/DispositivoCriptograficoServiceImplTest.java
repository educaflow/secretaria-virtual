package com.educaflow.subsystem.criptografia.service.impl;

import com.axelor.db.Query;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.educaflow.base.infrastructure.criptografia.EntornoCriptografico;
import com.educaflow.base.infrastructure.criptografia.slot.SlotFlag;
import com.educaflow.base.infrastructure.criptografia.slot.SlotInfo;
import com.educaflow.base.infrastructure.criptografia.slot.SlotInfoFactory;
import com.educaflow.subsystem.criptografia.db.DispositivoCriptografico;
import com.educaflow.subsystem.criptografia.db.repo.DispositivoCriptograficoRepository;
import com.educaflow.subsystem.criptografia.util.DispositivoCriptograficoInfoBuilder;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class DispositivoCriptograficoServiceImplTest {

    private static final String PIN = "1234";
    private static final String NUEVO_PIN = "5678";

    @TempDir
    Path tempDir;

    private DispositivoCriptograficoRepository repository;
    private DispositivoCriptograficoServiceImpl service;
    private MockedStatic<SlotInfoFactory> slotInfoFactory;
    private Path libreria;

    @BeforeEach
    void setUp() throws IOException {
        repository = Mockito.mock(DispositivoCriptograficoRepository.class);
        service = new DispositivoCriptograficoServiceImpl(DispositivoCriptografico.class, repository);
        slotInfoFactory = Mockito.mockStatic(SlotInfoFactory.class);
        libreria = Files.createFile(tempDir.resolve("libpkcs11.so"));
    }

    @AfterEach
    void tearDown() {
        slotInfoFactory.close();
    }

    @Test
    void validateInsert_rutaNula_devuelveErrorDeRutaVacia() {
        DispositivoCriptografico dispositivo = dispositivoNuevo(null, 0);

        BusinessMessage mensaje = unicoMensaje(service.validateInsert(dispositivo));

        assertEquals("pkcs11LibraryPath", mensaje.getFieldName());
        assertEquals("La ruta de la librería PKCS#11 no puede estar vacía", mensaje.getMessage());
        slotInfoFactory.verifyNoInteractions();
    }

    @Test
    void validateInsert_rutaEnBlanco_devuelveErrorDeRutaVacia() {
        DispositivoCriptografico dispositivo = dispositivoNuevo("   ", 0);

        BusinessMessage mensaje = unicoMensaje(service.validateInsert(dispositivo));

        assertEquals("pkcs11LibraryPath", mensaje.getFieldName());
        assertEquals("La ruta de la librería PKCS#11 no puede estar vacía", mensaje.getMessage());
    }

    @Test
    void validateInsert_libreriaInexistente_devuelveErrorDeRutaInexistente() {
        String ruta = tempDir.resolve("no-existe.so").toString();
        DispositivoCriptografico dispositivo = dispositivoNuevo(ruta, 0);

        BusinessMessage mensaje = unicoMensaje(service.validateInsert(dispositivo));

        assertEquals("pkcs11LibraryPath", mensaje.getFieldName());
        assertEquals("La librería PKCS#11 no existe en la ruta indicada: " + ruta, mensaje.getMessage());
        slotInfoFactory.verifyNoInteractions();
    }

    @Test
    void validateInsert_libreriaInaccesible_devuelveErrorDeAcceso() {
        slotInfoFactory.when(() -> SlotInfoFactory.getSlotsInfo(libreria)).thenThrow(new RuntimeException("boom"));
        DispositivoCriptografico dispositivo = dispositivoNuevo(libreria.toString(), 0);

        BusinessMessage mensaje = unicoMensaje(service.validateInsert(dispositivo));

        assertEquals("pkcs11LibraryPath", mensaje.getFieldName());
        assertEquals("No se puede acceder a la librería PKCS#11: boom", mensaje.getMessage());
        Mockito.verifyNoInteractions(repository);
    }

    @Test
    void validateInsert_slotNuloLibreYPinCorrecto_esValidoYUsaElSlotCero() {
        slotInfoFactory.when(() -> SlotInfoFactory.getSlotsInfo(libreria)).thenReturn(slots(0));
        when(repository.findBySlot(0)).thenReturn(List.of());
        DispositivoCriptografico dispositivo = dispositivoNuevo(libreria.toString(), null);

        Optional<BusinessMessages> resultado = service.validateInsert(dispositivo);

        assertTrue(resultado.isEmpty());
        slotInfoFactory.verify(() -> SlotInfoFactory.validatePin(libreria, 0, PIN));
    }

    @Test
    void validateInsert_slotInexistente_devuelveErrorDeSlotYNoValidaElPin() {
        slotInfoFactory.when(() -> SlotInfoFactory.getSlotsInfo(libreria)).thenReturn(slots(0, 1));
        when(repository.findBySlot(5)).thenReturn(List.of());
        DispositivoCriptografico dispositivo = dispositivoNuevo(libreria.toString(), 5);

        BusinessMessage mensaje = unicoMensaje(service.validateInsert(dispositivo));

        assertEquals("slot", mensaje.getFieldName());
        assertEquals("El slot 5 no existe en la librería PKCS#11. Slots disponibles: 2", mensaje.getMessage());
        slotInfoFactory.verify(() -> SlotInfoFactory.validatePin(any(), anyInt(), ArgumentMatchers.<String>any()), never());
    }

    @Test
    void validateInsert_slotOcupadoPorOtroDispositivo_devuelveErrorDeSlotOcupado() {
        slotInfoFactory.when(() -> SlotInfoFactory.getSlotsInfo(libreria)).thenReturn(slots(0, 1));
        when(repository.findBySlot(1)).thenReturn(List.of(conId(7L)));
        DispositivoCriptografico dispositivo = dispositivoNuevo(libreria.toString(), 1);

        BusinessMessage mensaje = unicoMensaje(service.validateInsert(dispositivo));

        assertEquals("slot", mensaje.getFieldName());
        assertEquals("Ya existe un dispositivo criptográfico configurado en el slot 1. Cada slot solo puede tener un dispositivo.",
                mensaje.getMessage());
        slotInfoFactory.verify(() -> SlotInfoFactory.validatePin(any(), anyInt(), ArgumentMatchers.<String>any()), never());
    }

    @Test
    void validateInsert_slotInexistenteYOcupado_devuelveLosDosErrores() {
        slotInfoFactory.when(() -> SlotInfoFactory.getSlotsInfo(libreria)).thenReturn(slots(0));
        when(repository.findBySlot(3)).thenReturn(List.of(conId(9L)));
        DispositivoCriptografico dispositivo = dispositivoNuevo(libreria.toString(), 3);

        BusinessMessages mensajes = service.validateInsert(dispositivo).orElseThrow();

        assertEquals(2, mensajes.size());
        assertEquals("El slot 3 no existe en la librería PKCS#11. Slots disponibles: 1", mensajes.get(0).getMessage());
        assertEquals("Ya existe un dispositivo criptográfico configurado en el slot 3. Cada slot solo puede tener un dispositivo.",
                mensajes.get(1).getMessage());
    }

    @Test
    void validateUpdate_slotOcupadoPorElMismoDispositivo_esValido() {
        slotInfoFactory.when(() -> SlotInfoFactory.getSlotsInfo(libreria)).thenReturn(slots(0, 1));
        when(repository.findBySlot(1)).thenReturn(List.of(conId(4L)));
        DispositivoCriptografico dispositivo = dispositivo(libreria.toString(), 1, 4L);

        Optional<BusinessMessages> resultado = service.validateUpdate(dispositivo, dispositivo(libreria.toString(), 1, 4L));

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateUpdate_soloCambiaElNombre_noValidaElPin() {
        slotInfoFactory.when(() -> SlotInfoFactory.getSlotsInfo(libreria)).thenReturn(slots(0, 1));
        when(repository.findBySlot(1)).thenReturn(List.of(conId(4L)));
        DispositivoCriptografico original = dispositivo(libreria.toString(), 1, 4L);
        original.setName("antes");
        DispositivoCriptografico dispositivo = dispositivo(libreria.toString(), 1, 4L);
        dispositivo.setName("despues");

        Optional<BusinessMessages> resultado = service.validateUpdate(dispositivo, original);

        assertTrue(resultado.isEmpty());
        slotInfoFactory.verify(() -> SlotInfoFactory.validatePin(any(), anyInt(), ArgumentMatchers.<String>any()), never());
    }

    @Test
    void validateUpdate_conNuevoPin_validaElPinNuevo() {
        slotInfoFactory.when(() -> SlotInfoFactory.getSlotsInfo(libreria)).thenReturn(slots(0, 1));
        when(repository.findBySlot(1)).thenReturn(List.of(conId(4L)));
        DispositivoCriptografico original = dispositivo(libreria.toString(), 1, 4L);
        DispositivoCriptografico dispositivo = dispositivo(libreria.toString(), 1, 4L);
        dispositivo.setNuevoPin(NUEVO_PIN);

        Optional<BusinessMessages> resultado = service.validateUpdate(dispositivo, original);

        assertTrue(resultado.isEmpty());
        slotInfoFactory.verify(() -> SlotInfoFactory.validatePin(libreria, 1, NUEVO_PIN));
    }

    @Test
    void validateUpdate_nuevoPinEnBlanco_noValidaElPin() {
        slotInfoFactory.when(() -> SlotInfoFactory.getSlotsInfo(libreria)).thenReturn(slots(0, 1));
        when(repository.findBySlot(1)).thenReturn(List.of(conId(4L)));
        DispositivoCriptografico original = dispositivo(libreria.toString(), 1, 4L);
        DispositivoCriptografico dispositivo = dispositivo(libreria.toString(), 1, 4L);
        dispositivo.setNuevoPin("   ");

        Optional<BusinessMessages> resultado = service.validateUpdate(dispositivo, original);

        assertTrue(resultado.isEmpty());
        slotInfoFactory.verify(() -> SlotInfoFactory.validatePin(any(), anyInt(), ArgumentMatchers.<String>any()), never());
    }

    @Test
    void validateUpdate_nuevoPinIncorrecto_devuelveErrorEnElNuevoPin() {
        slotInfoFactory.when(() -> SlotInfoFactory.getSlotsInfo(libreria)).thenReturn(slots(0, 1));
        slotInfoFactory.when(() -> SlotInfoFactory.validatePin(eq(libreria), eq(1), eq(NUEVO_PIN)))
                .thenThrow(new RuntimeException("C_Login error: 160"));
        when(repository.findBySlot(1)).thenReturn(List.of(conId(4L)));
        DispositivoCriptografico original = dispositivo(libreria.toString(), 1, 4L);
        DispositivoCriptografico dispositivo = dispositivo(libreria.toString(), 1, 4L);
        dispositivo.setNuevoPin(NUEVO_PIN);

        BusinessMessage mensaje = unicoMensaje(service.validateUpdate(dispositivo, original));

        assertEquals("nuevoPin", mensaje.getFieldName());
    }

    @Test
    void validateUpdate_sinOriginal_validaElPin() {
        slotInfoFactory.when(() -> SlotInfoFactory.getSlotsInfo(libreria)).thenReturn(slots(0, 1));
        when(repository.findBySlot(1)).thenReturn(List.of(conId(4L)));
        DispositivoCriptografico dispositivo = dispositivo(libreria.toString(), 1, 4L);

        Optional<BusinessMessages> resultado = service.validateUpdate(dispositivo, null);

        assertTrue(resultado.isEmpty());
        slotInfoFactory.verify(() -> SlotInfoFactory.validatePin(libreria, 1, PIN));
    }

    @Test
    void validateUpdate_cambiaElSlot_devuelveErrorYNoConsultaElToken() {
        DispositivoCriptografico original = dispositivo(libreria.toString(), 1, 4L);
        DispositivoCriptografico dispositivo = dispositivo(libreria.toString(), 2, 4L);

        BusinessMessage mensaje = unicoMensaje(service.validateUpdate(dispositivo, original));

        assertEquals("slot", mensaje.getFieldName());
        slotInfoFactory.verify(() -> SlotInfoFactory.getSlotsInfo(any()), never());
    }

    @Test
    void validateUpdate_cambiaLaLibreria_devuelveErrorYNoConsultaElToken() {
        DispositivoCriptografico original = dispositivo(libreria.toString(), 1, 4L);
        DispositivoCriptografico dispositivo = dispositivo(libreria.toString() + "-otra", 1, 4L);

        BusinessMessage mensaje = unicoMensaje(service.validateUpdate(dispositivo, original));

        assertEquals("pkcs11LibraryPath", mensaje.getFieldName());
        slotInfoFactory.verify(() -> SlotInfoFactory.getSlotsInfo(any()), never());
    }

    @Test
    void validateInsert_sinNuevoPin_devuelveErrorDePinObligatorioYNoConsultaElToken() {
        DispositivoCriptografico dispositivo = dispositivoNuevo(libreria.toString(), 0);
        dispositivo.setNuevoPin(null);

        BusinessMessage mensaje = unicoMensaje(service.validateInsert(dispositivo));

        assertEquals("nuevoPin", mensaje.getFieldName());
        assertEquals("El PIN es obligatorio", mensaje.getMessage());
        slotInfoFactory.verifyNoInteractions();
    }

    @Test
    void validateInsert_nuevoPinEnBlanco_devuelveErrorDePinObligatorio() {
        DispositivoCriptografico dispositivo = dispositivoNuevo(libreria.toString(), 0);
        dispositivo.setNuevoPin("   ");

        BusinessMessage mensaje = unicoMensaje(service.validateInsert(dispositivo));

        assertEquals("nuevoPin", mensaje.getFieldName());
        slotInfoFactory.verifyNoInteractions();
    }

    @Test
    void validateInsert_pinIncorrecto_devuelveErrorDePin() {
        slotInfoFactory.when(() -> SlotInfoFactory.getSlotsInfo(libreria)).thenReturn(slots(0));
        slotInfoFactory.when(() -> SlotInfoFactory.validatePin(eq(libreria), eq(0), eq(PIN)))
                .thenThrow(new RuntimeException("C_Login error: 160"));
        when(repository.findBySlot(0)).thenReturn(List.of());
        DispositivoCriptografico dispositivo = dispositivoNuevo(libreria.toString(), 0);

        BusinessMessage mensaje = unicoMensaje(service.validateInsert(dispositivo));

        assertEquals("nuevoPin", mensaje.getFieldName());
        assertEquals("El PIN no es correcto: C_Login error: 160", mensaje.getMessage());
        Mockito.verify(repository, never()).all();
    }

    @Test
    void validateInsert_alTokenSoloLeQuedaUnIntento_noPruebaElPinYAvisa() {
        slotInfoFactory.when(() -> SlotInfoFactory.getSlotsInfo(libreria)).thenReturn(slotConFlag(0, SlotFlag.USER_PIN_FINAL_TRY));
        when(repository.findBySlot(0)).thenReturn(List.of());
        DispositivoCriptografico dispositivo = dispositivoNuevo(libreria.toString(), 0);

        BusinessMessage mensaje = unicoMensaje(service.validateInsert(dispositivo));

        assertEquals("nuevoPin", mensaje.getFieldName());
        assertTrue(mensaje.getMessage().contains("solo le queda un intento"));
        slotInfoFactory.verify(() -> SlotInfoFactory.validatePin(any(), anyInt(), ArgumentMatchers.<String>any()), never());
    }

    @Test
    void validateInsert_elPinDelTokenEstaBloqueado_noPruebaElPinYAvisa() {
        slotInfoFactory.when(() -> SlotInfoFactory.getSlotsInfo(libreria)).thenReturn(slotConFlag(0, SlotFlag.USER_PIN_LOCKED));
        when(repository.findBySlot(0)).thenReturn(List.of());
        DispositivoCriptografico dispositivo = dispositivoNuevo(libreria.toString(), 0);

        BusinessMessage mensaje = unicoMensaje(service.validateInsert(dispositivo));

        assertEquals("nuevoPin", mensaje.getFieldName());
        assertTrue(mensaje.getMessage().contains("bloqueado"));
        slotInfoFactory.verify(() -> SlotInfoFactory.validatePin(any(), anyInt(), ArgumentMatchers.<String>any()), never());
    }

    @Test
    void validateInsert_alTokenLeQuedanPocosIntentosPeroMasDeUno_siPruebaElPin() {
        slotInfoFactory.when(() -> SlotInfoFactory.getSlotsInfo(libreria)).thenReturn(slotConFlag(0, SlotFlag.USER_PIN_COUNT_LOW));
        when(repository.findBySlot(0)).thenReturn(List.of());
        DispositivoCriptografico dispositivo = dispositivoNuevo(libreria.toString(), 0);

        Optional<BusinessMessages> resultado = service.validateInsert(dispositivo);

        assertTrue(resultado.isEmpty());
        slotInfoFactory.verify(() -> SlotInfoFactory.validatePin(libreria, 0, PIN));
    }

    @Test
    void validateUpdate_conNuevoPinYAlTokenSoloLeQuedaUnIntento_noPruebaElPinYAvisa() {
        slotInfoFactory.when(() -> SlotInfoFactory.getSlotsInfo(libreria)).thenReturn(slotConFlag(1, SlotFlag.USER_PIN_FINAL_TRY));
        when(repository.findBySlot(1)).thenReturn(List.of(conId(4L)));
        DispositivoCriptografico original = dispositivo(libreria.toString(), 1, 4L);
        DispositivoCriptografico dispositivo = dispositivo(libreria.toString(), 1, 4L);
        dispositivo.setNuevoPin(NUEVO_PIN);

        BusinessMessage mensaje = unicoMensaje(service.validateUpdate(dispositivo, original));

        assertEquals("nuevoPin", mensaje.getFieldName());
        slotInfoFactory.verify(() -> SlotInfoFactory.validatePin(any(), anyInt(), ArgumentMatchers.<String>any()), never());
    }

    @Test
    void validateUpdate_sinNuevoPinYAlTokenSoloLeQuedaUnIntento_esValido() {
        slotInfoFactory.when(() -> SlotInfoFactory.getSlotsInfo(libreria)).thenReturn(slotConFlag(1, SlotFlag.USER_PIN_FINAL_TRY));
        when(repository.findBySlot(1)).thenReturn(List.of(conId(4L)));
        DispositivoCriptografico original = dispositivo(libreria.toString(), 1, 4L);
        DispositivoCriptografico dispositivo = dispositivo(libreria.toString(), 1, 4L);

        Optional<BusinessMessages> resultado = service.validateUpdate(dispositivo, original);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void insert_copiaElNuevoPinAlPinYVaciaElTransitorio() {
        try (MockedStatic<DispositivoCriptograficoInfoBuilder> infoBuilder = Mockito.mockStatic(DispositivoCriptograficoInfoBuilder.class);
             MockedStatic<EntornoCriptografico> entorno = Mockito.mockStatic(EntornoCriptografico.class)) {
            prepararSlotLibre(0);
            prepararGuardado();
            infoBuilder.when(() -> DispositivoCriptograficoInfoBuilder.listarAlias(any(), any(), any())).thenReturn(List.of());
            DispositivoCriptografico dispositivo = dispositivoNuevo(libreria.toString(), 0);

            service.insert(dispositivo);

            assertEquals(PIN, dispositivo.getPin());
            assertNull(dispositivo.getNuevoPin());
            infoBuilder.verify(() -> DispositivoCriptograficoInfoBuilder.listarAlias(libreria.toString(), 0, PIN));
        }
    }

    @Test
    void update_sinNuevoPin_conservaElPinGuardado() {
        try (MockedStatic<EntornoCriptografico> entorno = Mockito.mockStatic(EntornoCriptografico.class)) {
            prepararSlotDelPropioDispositivo(1, 4L);
            prepararGuardado();
            DispositivoCriptografico original = dispositivo(libreria.toString(), 1, 4L);
            DispositivoCriptografico dispositivo = dispositivo(libreria.toString(), 1, 4L);

            service.update(dispositivo, original);

            assertEquals(PIN, dispositivo.getPin());
        }
    }

    @Test
    void update_conNuevoPin_sustituyeElPinGuardadoYVaciaElTransitorio() {
        try (MockedStatic<EntornoCriptografico> entorno = Mockito.mockStatic(EntornoCriptografico.class)) {
            prepararSlotDelPropioDispositivo(1, 4L);
            prepararGuardado();
            DispositivoCriptografico original = dispositivo(libreria.toString(), 1, 4L);
            DispositivoCriptografico dispositivo = dispositivo(libreria.toString(), 1, 4L);
            dispositivo.setNuevoPin(NUEVO_PIN);

            service.update(dispositivo, original);

            assertEquals(NUEVO_PIN, dispositivo.getPin());
            assertNull(dispositivo.getNuevoPin());
        }
    }

    @Test
    void validate_alta_conNuevoPin_loAdelantaAlPinParaQueLaEntidadNazcaConPin() {
        prepararValidateDelRepositorio();
        Map<String, Object> json = jsonDeAlta();
        json.put("nuevoPin", NUEVO_PIN);

        Map<String, Object> resultado = service.validate(json, Map.of());

        assertEquals(NUEVO_PIN, resultado.get("pin"));
        assertEquals(NUEVO_PIN, resultado.get("nuevoPin"));
    }

    @Test
    void validate_conNuevoPin_tambienEnLaModificacion() {
        prepararValidateDelRepositorio();
        Map<String, Object> json = jsonDeAlta();
        json.put("id", 4L);
        json.put("nuevoPin", NUEVO_PIN);

        Map<String, Object> resultado = service.validate(json, Map.of());

        assertEquals(NUEVO_PIN, resultado.get("pin"));
    }

    @Test
    void validate_sinNuevoPin_noPoneElPin() {
        prepararValidateDelRepositorio();

        Map<String, Object> resultado = service.validate(jsonDeAlta(), Map.of());

        assertFalse(resultado.containsKey("pin"));
    }

    @Test
    void validate_nuevoPinEnBlanco_noPoneElPin() {
        prepararValidateDelRepositorio();
        Map<String, Object> json = jsonDeAlta();
        json.put("nuevoPin", "   ");

        Map<String, Object> resultado = service.validate(json, Map.of());

        assertFalse(resultado.containsKey("pin"));
    }

    @Test
    void validate_unPinDictadoPorElCliente_seDescartaAunqueVengaConNuevoPin() {
        prepararValidateDelRepositorio();
        Map<String, Object> json = jsonDeAlta();
        json.put("pin", "9999");
        json.put("nuevoPin", NUEVO_PIN);

        Map<String, Object> resultado = service.validate(json, Map.of());

        assertEquals(NUEVO_PIN, resultado.get("pin"));
    }

    @Test
    void validate_unPinDictadoPorElClienteSinNuevoPin_seDescarta() {
        prepararValidateDelRepositorio();
        Map<String, Object> json = jsonDeAlta();
        json.put("pin", "9999");

        Map<String, Object> resultado = service.validate(json, Map.of());

        assertFalse(resultado.containsKey("pin"));
    }

    @Test
    void validateGetSlotsDisponibles_rutaNula_pideLaRuta() {
        BusinessMessage mensaje = unicoMensaje(service.validateGetSlotsDisponibles(null));

        assertEquals("pkcs11LibraryPath", mensaje.getFieldName());
        assertEquals("Indica la ruta de la librería PKCS#11 para ver los slots disponibles", mensaje.getMessage());
        slotInfoFactory.verifyNoInteractions();
    }

    @Test
    void validateGetSlotsDisponibles_rutaEnBlanco_pideLaRuta() {
        BusinessMessage mensaje = unicoMensaje(service.validateGetSlotsDisponibles("   "));

        assertEquals("pkcs11LibraryPath", mensaje.getFieldName());
        assertEquals("Indica la ruta de la librería PKCS#11 para ver los slots disponibles", mensaje.getMessage());
        slotInfoFactory.verifyNoInteractions();
    }

    @Test
    void validateGetSlotsDisponibles_libreriaInexistente_avisaYNoCargaLaLibreria() {
        String ruta = tempDir.resolve("no-existe.so").toString();

        BusinessMessage mensaje = unicoMensaje(service.validateGetSlotsDisponibles(ruta));

        assertEquals("pkcs11LibraryPath", mensaje.getFieldName());
        assertEquals("La librería PKCS#11 no existe en la ruta indicada: " + ruta, mensaje.getMessage());
        slotInfoFactory.verifyNoInteractions();
    }

    @Test
    void validateGetSlotsDisponibles_laRutaEsUnDirectorio_avisaYNoCargaLaLibreria() {
        BusinessMessage mensaje = unicoMensaje(service.validateGetSlotsDisponibles(tempDir.toString()));

        assertEquals("La librería PKCS#11 no existe en la ruta indicada: " + tempDir, mensaje.getMessage());
        slotInfoFactory.verifyNoInteractions();
    }

    @Test
    void validateGetSlotsDisponibles_rutaConCaracteresInvalidos_avisaSinLanzarExcepcion() {
        BusinessMessage mensaje = unicoMensaje(service.validateGetSlotsDisponibles("/ruta/con\0nulo.so"));

        assertEquals("pkcs11LibraryPath", mensaje.getFieldName());
        assertTrue(mensaje.getMessage().startsWith("No se puede acceder a la librería PKCS#11: "));
        slotInfoFactory.verifyNoInteractions();
    }

    @Test
    void validateGetSlotsDisponibles_libreriaInaccesible_avisaConElMotivo() {
        slotInfoFactory.when(() -> SlotInfoFactory.getSlotsInfo(libreria)).thenThrow(new RuntimeException("boom"));

        BusinessMessage mensaje = unicoMensaje(service.validateGetSlotsDisponibles(libreria.toString()));

        assertEquals("pkcs11LibraryPath", mensaje.getFieldName());
        assertEquals("No se puede acceder a la librería PKCS#11: boom", mensaje.getMessage());
    }

    @Test
    void validateGetSlotsDisponibles_sinDispositivos_avisaQueNoHayNinguno() {
        slotInfoFactory.when(() -> SlotInfoFactory.getSlotsInfo(libreria)).thenReturn(List.of());

        BusinessMessage mensaje = unicoMensaje(service.validateGetSlotsDisponibles(libreria.toString()));

        assertEquals("pkcs11LibraryPath", mensaje.getFieldName());
        assertEquals("No hay ningún dispositivo conectado en la librería PKCS#11 indicada", mensaje.getMessage());
    }

    @Test
    void validateGetSlotsDisponibles_conDispositivos_esValido() {
        slotInfoFactory.when(() -> SlotInfoFactory.getSlotsInfo(libreria)).thenReturn(slots(0));

        assertTrue(service.validateGetSlotsDisponibles(libreria.toString()).isEmpty());
    }

    @Test
    void validateGetDescripcionSlotsDisponibles_rutaNula_pideLaRuta() {
        BusinessMessage mensaje = unicoMensaje(service.validateGetDescripcionSlotsDisponibles(null));

        assertEquals("pkcs11LibraryPath", mensaje.getFieldName());
        assertEquals("Indica la ruta de la librería PKCS#11 para ver los slots disponibles", mensaje.getMessage());
    }

    @Test
    void validateGetDescripcionSlotsDisponibles_conDispositivos_esValido() {
        slotInfoFactory.when(() -> SlotInfoFactory.getSlotsInfo(libreria)).thenReturn(slots(0));

        assertTrue(service.validateGetDescripcionSlotsDisponibles(libreria.toString()).isEmpty());
    }

    @Test
    void getSlotsDisponibles_variosDispositivos_devuelveTodosSusIndices() {
        slotInfoFactory.when(() -> SlotInfoFactory.getSlotsInfo(libreria)).thenReturn(slots(0, 1));

        assertEquals(List.of(0, 1), service.getSlotsDisponibles(libreria.toString()));
    }

    @Test
    void getSlotsDisponibles_rutaQueNoPasaLaValidacion_lanzaExcepcion() {
        assertThrows(ValidationException.class, () -> service.getSlotsDisponibles(null));
    }

    @Test
    void getDescripcionSlotsDisponibles_variosDispositivos_describeCadaUnoEnUnaLinea() {
        slotInfoFactory.when(() -> SlotInfoFactory.getSlotsInfo(libreria)).thenReturn(List.of(
                slotConToken(0, "DNIe", "FNMT", "DNIe v3", "A1"),
                slotConToken(1, "HSM prod", "Yubico", "YubiKey 5", "B2")));

        assertEquals("Slot 0: DNIe — FNMT DNIe v3 (nº serie A1)\nSlot 1: HSM prod — Yubico YubiKey 5 (nº serie B2)", service.getDescripcionSlotsDisponibles(libreria.toString()));
    }

    @Test
    void getDescripcionSlotsDisponibles_rutaQueNoPasaLaValidacion_lanzaExcepcion() {
        assertThrows(ValidationException.class, () -> service.getDescripcionSlotsDisponibles(null));
    }

    private void prepararValidateDelRepositorio() {
        when(repository.validate(any(), any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private static Map<String, Object> jsonDeAlta() {
        Map<String, Object> json = new HashMap<>();
        json.put("name", "Token");
        json.put("pkcs11LibraryPath", "/usr/lib/libpkcs11.so");
        json.put("slot", 0);
        return json;
    }

    private static SlotInfo slotConToken(int indice, String etiqueta, String fabricante, String modelo, String serie) {
        SlotInfo slot = slots(indice).get(0);
        slot.tokenLabel = etiqueta;
        slot.tokenManufacturer = fabricante;
        slot.tokenModel = modelo;
        slot.tokenSerial = serie;
        return slot;
    }

    private void prepararSlotLibre(int slot) {
        slotInfoFactory.when(() -> SlotInfoFactory.getSlotsInfo(libreria)).thenReturn(slots(slot));
        when(repository.findBySlot(slot)).thenReturn(List.of());
    }

    private void prepararSlotDelPropioDispositivo(int slot, Long id) {
        slotInfoFactory.when(() -> SlotInfoFactory.getSlotsInfo(libreria)).thenReturn(slots(slot));
        when(repository.findBySlot(slot)).thenReturn(List.of(conId(id)));
    }

    @SuppressWarnings("unchecked")
    private void prepararGuardado() {
        when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        Query<DispositivoCriptografico> todos = Mockito.mock(Query.class);
        when(repository.all()).thenReturn(todos);
        when(todos.fetch()).thenReturn(List.of());
    }

    private static BusinessMessage unicoMensaje(Optional<BusinessMessages> resultado) {
        assertTrue(resultado.isPresent());
        assertEquals(1, resultado.get().size());
        return resultado.get().get(0);
    }

    private static DispositivoCriptografico dispositivo(String ruta, Integer slot, Long id) {
        DispositivoCriptografico dispositivo = new DispositivoCriptografico();
        dispositivo.setPkcs11LibraryPath(ruta);
        dispositivo.setSlot(slot);
        dispositivo.setPin(PIN);
        dispositivo.setId(id);
        return dispositivo;
    }

    /**
     * Dispositivo de un alta: sin id ni PIN guardado, con el PIN tecleado en el transitorio {@code nuevoPin}.
     */
    private static DispositivoCriptografico dispositivoNuevo(String ruta, Integer slot) {
        DispositivoCriptografico dispositivo = dispositivo(ruta, slot, null);
        dispositivo.setPin(null);
        dispositivo.setNuevoPin(PIN);
        return dispositivo;
    }

    private static DispositivoCriptografico conId(Long id) {
        DispositivoCriptografico dispositivo = new DispositivoCriptografico();
        dispositivo.setId(id);
        return dispositivo;
    }

    private static List<SlotInfo> slots(int... indices) {
        return Arrays.stream(indices).mapToObj(indice -> {
            SlotInfo slot = new SlotInfo();
            slot.index = indice;
            slot.tokenFlags = EnumSet.noneOf(SlotFlag.class);
            return slot;
        }).toList();
    }

    private static List<SlotInfo> slotConFlag(int indice, SlotFlag flag) {
        List<SlotInfo> slots = slots(indice);
        slots.get(0).tokenFlags.add(flag);
        return slots;
    }

    @Test
    void allowPropertiesInsert_soloPermiteLosCamposQueDictaElCliente() {
        AllowProperties allowProperties = service.allowPropertiesInsert();

        assertTrue(allowProperties.allowProperty("name"));
        assertTrue(allowProperties.allowProperty("pkcs11LibraryPath"));
        assertTrue(allowProperties.allowProperty("slot"));
        assertTrue(allowProperties.allowProperty("nuevoPin"));
        assertFalse(allowProperties.allowProperty("pin"));
        assertFalse(allowProperties.allowProperty("alias"));
        assertFalse(allowProperties.allowProperty("info"));
        assertFalse(allowProperties.allowProperty("slotsDisponibles"));
    }

    @Test
    void allowPropertiesUpdate_soloPermiteLosCamposQueDictaElCliente() {
        AllowProperties allowProperties = service.allowPropertiesUpdate();

        assertTrue(allowProperties.allowProperty("name"));
        assertTrue(allowProperties.allowProperty("pkcs11LibraryPath"));
        assertTrue(allowProperties.allowProperty("slot"));
        assertTrue(allowProperties.allowProperty("nuevoPin"));
        assertFalse(allowProperties.allowProperty("pin"));
        assertFalse(allowProperties.allowProperty("alias"));
        assertFalse(allowProperties.allowProperty("info"));
        assertFalse(allowProperties.allowProperty("slotsDisponibles"));
    }
}
