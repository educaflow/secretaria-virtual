package com.educaflow.subsystem.criptografia.service.impl;

import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.educaflow.base.infrastructure.criptografia.slot.SlotInfo;
import com.educaflow.base.infrastructure.criptografia.slot.SlotInfoFactory;
import com.educaflow.subsystem.criptografia.db.DispositivoCriptografico;
import com.educaflow.subsystem.criptografia.db.repo.DispositivoCriptograficoRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class DispositivoCriptograficoServiceImplTest {

    private static final String PIN = "1234";

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
        DispositivoCriptografico dispositivo = dispositivo(null, 0, null);

        BusinessMessage mensaje = unicoMensaje(service.validateInsert(dispositivo));

        assertEquals("pkcs11LibraryPath", mensaje.getFieldName());
        assertEquals("La ruta de la librería PKCS#11 no puede estar vacía", mensaje.getMessage());
        slotInfoFactory.verifyNoInteractions();
    }

    @Test
    void validateInsert_rutaEnBlanco_devuelveErrorDeRutaVacia() {
        DispositivoCriptografico dispositivo = dispositivo("   ", 0, null);

        BusinessMessage mensaje = unicoMensaje(service.validateInsert(dispositivo));

        assertEquals("pkcs11LibraryPath", mensaje.getFieldName());
        assertEquals("La ruta de la librería PKCS#11 no puede estar vacía", mensaje.getMessage());
    }

    @Test
    void validateInsert_libreriaInexistente_devuelveErrorDeRutaInexistente() {
        String ruta = tempDir.resolve("no-existe.so").toString();
        DispositivoCriptografico dispositivo = dispositivo(ruta, 0, null);

        BusinessMessage mensaje = unicoMensaje(service.validateInsert(dispositivo));

        assertEquals("pkcs11LibraryPath", mensaje.getFieldName());
        assertEquals("La librería PKCS#11 no existe en la ruta indicada: " + ruta, mensaje.getMessage());
        slotInfoFactory.verifyNoInteractions();
    }

    @Test
    void validateInsert_libreriaInaccesible_devuelveErrorDeAcceso() {
        slotInfoFactory.when(() -> SlotInfoFactory.getSlotsInfo(libreria)).thenThrow(new RuntimeException("boom"));
        DispositivoCriptografico dispositivo = dispositivo(libreria.toString(), 0, null);

        BusinessMessage mensaje = unicoMensaje(service.validateInsert(dispositivo));

        assertEquals("pkcs11LibraryPath", mensaje.getFieldName());
        assertEquals("No se puede acceder a la librería PKCS#11: boom", mensaje.getMessage());
        Mockito.verifyNoInteractions(repository);
    }

    @Test
    void validateInsert_slotNuloLibreYPinCorrecto_esValidoYUsaElSlotCero() {
        slotInfoFactory.when(() -> SlotInfoFactory.getSlotsInfo(libreria)).thenReturn(slots(0));
        when(repository.findBySlot(0)).thenReturn(List.of());
        DispositivoCriptografico dispositivo = dispositivo(libreria.toString(), null, null);

        Optional<BusinessMessages> resultado = service.validateInsert(dispositivo);

        assertTrue(resultado.isEmpty());
        slotInfoFactory.verify(() -> SlotInfoFactory.validatePin(libreria, 0, PIN));
    }

    @Test
    void validateInsert_slotInexistente_devuelveErrorDeSlotYNoValidaElPin() {
        slotInfoFactory.when(() -> SlotInfoFactory.getSlotsInfo(libreria)).thenReturn(slots(0, 1));
        when(repository.findBySlot(5)).thenReturn(List.of());
        DispositivoCriptografico dispositivo = dispositivo(libreria.toString(), 5, null);

        BusinessMessage mensaje = unicoMensaje(service.validateInsert(dispositivo));

        assertEquals("slot", mensaje.getFieldName());
        assertEquals("El slot 5 no existe en la librería PKCS#11. Slots disponibles: 2", mensaje.getMessage());
        slotInfoFactory.verify(() -> SlotInfoFactory.validatePin(any(), anyInt(), ArgumentMatchers.<String>any()), never());
    }

    @Test
    void validateInsert_slotOcupadoPorOtroDispositivo_devuelveErrorDeSlotOcupado() {
        slotInfoFactory.when(() -> SlotInfoFactory.getSlotsInfo(libreria)).thenReturn(slots(0, 1));
        when(repository.findBySlot(1)).thenReturn(List.of(conId(7L)));
        DispositivoCriptografico dispositivo = dispositivo(libreria.toString(), 1, null);

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
        DispositivoCriptografico dispositivo = dispositivo(libreria.toString(), 3, null);

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
    void validateUpdate_cambiaElPin_validaElPinNuevo() {
        slotInfoFactory.when(() -> SlotInfoFactory.getSlotsInfo(libreria)).thenReturn(slots(0, 1));
        when(repository.findBySlot(1)).thenReturn(List.of(conId(4L)));
        DispositivoCriptografico original = dispositivo(libreria.toString(), 1, 4L);
        original.setPin("9999");
        DispositivoCriptografico dispositivo = dispositivo(libreria.toString(), 1, 4L);

        Optional<BusinessMessages> resultado = service.validateUpdate(dispositivo, original);

        assertTrue(resultado.isEmpty());
        slotInfoFactory.verify(() -> SlotInfoFactory.validatePin(libreria, 1, PIN));
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
    void validateInsert_pinIncorrecto_devuelveErrorDePin() {
        slotInfoFactory.when(() -> SlotInfoFactory.getSlotsInfo(libreria)).thenReturn(slots(0));
        slotInfoFactory.when(() -> SlotInfoFactory.validatePin(eq(libreria), eq(0), eq(PIN)))
                .thenThrow(new RuntimeException("C_Login error: 160"));
        when(repository.findBySlot(0)).thenReturn(List.of());
        DispositivoCriptografico dispositivo = dispositivo(libreria.toString(), 0, null);

        BusinessMessage mensaje = unicoMensaje(service.validateInsert(dispositivo));

        assertEquals("pin", mensaje.getFieldName());
        assertEquals("El PIN no es correcto: C_Login error: 160", mensaje.getMessage());
        Mockito.verify(repository, never()).all();
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

    private static DispositivoCriptografico conId(Long id) {
        DispositivoCriptografico dispositivo = new DispositivoCriptografico();
        dispositivo.setId(id);
        return dispositivo;
    }

    private static List<SlotInfo> slots(int... indices) {
        return Arrays.stream(indices).mapToObj(indice -> {
            SlotInfo slot = new SlotInfo();
            slot.index = indice;
            return slot;
        }).toList();
    }

    @Test
    void allowPropertiesInsert_soloPermiteLosCamposQueDictaElCliente() {
        AllowProperties allowProperties = service.allowPropertiesInsert();

        assertTrue(allowProperties.allowProperty("name"));
        assertTrue(allowProperties.allowProperty("pkcs11LibraryPath"));
        assertTrue(allowProperties.allowProperty("slot"));
        assertTrue(allowProperties.allowProperty("pin"));
        assertFalse(allowProperties.allowProperty("alias"));
        assertFalse(allowProperties.allowProperty("info"));
    }

    @Test
    void allowPropertiesUpdate_soloPermiteLosCamposQueDictaElCliente() {
        AllowProperties allowProperties = service.allowPropertiesUpdate();

        assertTrue(allowProperties.allowProperty("name"));
        assertTrue(allowProperties.allowProperty("pkcs11LibraryPath"));
        assertTrue(allowProperties.allowProperty("slot"));
        assertTrue(allowProperties.allowProperty("pin"));
        assertFalse(allowProperties.allowProperty("alias"));
        assertFalse(allowProperties.allowProperty("info"));
    }
}
