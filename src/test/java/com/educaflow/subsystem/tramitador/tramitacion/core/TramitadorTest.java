package com.educaflow.subsystem.tramitador.tramitacion.core;

import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.educaflow.base.infrastructure.numeradores.db.repo.NumeradorRepository;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.subsystem.expedientes.db.PruebaV1;
import com.educaflow.subsystem.expedientes.db.TipoExpediente;
import com.educaflow.subsystem.expedientes.db.Tramite;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.ContextoTramitacion;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.InitialEventManager;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.PhaseEventManager;
import com.educaflow.subsystem.tramitador.tramitacion.internal.ExpedienteLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;
import com.educaflow.base.infrastructure.validation.engine.BeanValidationRules;
import com.educaflow.base.infrastructure.validation.engine.FieldValidationRules;
import com.educaflow.base.infrastructure.validation.engine.ValidationRule;
import com.educaflow.subsystem.common.db.Persona;
import com.educaflow.subsystem.tramitador.tramitacion.validation.BeanValidationRulesForStateAndEvent;
import com.educaflow.subsystem.tramitador.tramitacion.validation.StateEventValidator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import kotlin.reflect.KFunction;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TramitadorTest {

    @Nested
    @ExtendWith(MockitoExtension.class)
    class TriggerInitialEvent {

        @Mock
        private NumeradorRepository numeradorRepository;
        @Mock
        private ExpedienteLocator expedienteLocator;
        @Mock
        private ModelServiceFactory modelServiceFactory;
        @Mock
        private InitialEventManager<?> initialEventManager;
        @Mock
        private PhaseEventManager<?> phaseEventManager;
        @InjectMocks
        private Tramitador tramitador;

        private final TipoExpediente tipoExpediente = new TipoExpediente();
        private ContextoTramitacion contextoTramitacion;
        private MockedStatic<SecurityUtil> securityUtil;

        @BeforeEach
        void preparar() {
            Tramite tramite = new Tramite();
            tramite.setName("Justificación de faltas");
            tramite.setDefaultTipoExpediente(tipoExpediente);
            tipoExpediente.setName("Justificación de faltas V1");
            tipoExpediente.setTramite(tramite);
            Centro centro = new Centro();
            centro.setCode("46000001");
            contextoTramitacion = new ContextoTramitacion(tramite, centro, Profile.CREADOR, false, false, null);

            doReturn(initialEventManager).when(expedienteLocator).getInitialEventManager(tipoExpediente);
            doReturn(PruebaV1.class).when(expedienteLocator).getModelClass(tipoExpediente);
            lenient().doReturn(phaseEventManager).when(expedienteLocator).getPhaseEventManager(any(), any());

            securityUtil = mockStatic(SecurityUtil.class);
            securityUtil.when(SecurityUtil::getUser).thenReturn(new User());
        }

        @AfterEach
        void cerrarEstaticos() {
            securityUtil.close();
        }

        @Test
        void eventoInicialConBusinessException_laRelanzaSinEnvolverYNoPideNumero() throws Exception {
            BusinessException rechazo = new BusinessException("rechazado");
            doThrow(rechazo).when(initialEventManager).triggerInitialEvent(any());

            BusinessException ex = assertThrows(BusinessException.class, () -> tramitador.triggerInitialEvent(contextoTramitacion));

            assertSame(rechazo, ex);
            verifyNoInteractions(numeradorRepository);
        }

        @Test
        void eventoInicialConOtraExcepcion_laEnvuelveEnRuntimeException() throws Exception {
            IllegalStateException fallo = new IllegalStateException("fallo");
            doThrow(fallo).when(initialEventManager).triggerInitialEvent(any());

            RuntimeException ex = assertThrows(RuntimeException.class, () -> tramitador.triggerInitialEvent(contextoTramitacion));

            assertSame(fallo, ex.getCause());
            verifyNoInteractions(numeradorRepository);
        }

        @Test
        void eventoInicialSinErrores_elNombreDelExpedienteEsElDelTramiteNoElDelTipo() throws Exception {
            try (MockedStatic<JPA> jpa = mockStatic(JPA.class)) {
                Expediente expediente = tramitador.triggerInitialEvent(contextoTramitacion);

                assertEquals("Justificación de faltas", expediente.getName());
            }
        }

        @Test
        void eventoInicialSinErrores_pideElNumeroDespuesDelEventoYGuarda() throws Exception {
            try (MockedStatic<JPA> jpa = mockStatic(JPA.class)) {
                Expediente expediente = tramitador.triggerInitialEvent(contextoTramitacion);

                InOrder orden = inOrder(initialEventManager, numeradorRepository);
                orden.verify(initialEventManager).triggerInitialEvent(any());
                orden.verify(numeradorRepository).getSiguienteNumeroExpediente(eq("46000001"), anyString());
                jpa.verify(() -> JPA.save(expediente));
            }
        }

        @Test
        void presentadoPorElUsuario_tomaElIdiomaDelUsuarioYNoElDelContexto() throws Exception {
            User usuario = new User();
            usuario.setLanguage("ca");
            securityUtil.when(SecurityUtil::getUser).thenReturn(usuario);
            ContextoTramitacion contexto = new ContextoTramitacion(contextoTramitacion.tramite(), contextoTramitacion.centro(),
                    Profile.CREADOR, false, false, "es");

            try (MockedStatic<JPA> jpa = mockStatic(JPA.class)) {
                Expediente expediente = tramitador.triggerInitialEvent(contexto);

                assertEquals("ca", expediente.getIdioma());
            }
        }

        @Test
        void presentadoEnPapel_tomaElIdiomaDelContextoYNoElDelUsuario() throws Exception {
            User usuario = new User();
            usuario.setLanguage("es");
            securityUtil.when(SecurityUtil::getUser).thenReturn(usuario);
            ContextoTramitacion contexto = new ContextoTramitacion(contextoTramitacion.tramite(), contextoTramitacion.centro(),
                    Profile.TRAMITADOR, true, false, "ca");

            try (MockedStatic<JPA> jpa = mockStatic(JPA.class)) {
                Expediente expediente = tramitador.triggerInitialEvent(contexto);

                assertEquals("ca", expediente.getIdioma());
            }
        }
    }

    @Nested
    class ContactoDelSolicitante {

        @Test
        void crearPersona_copiaElCorreoYElTelefonoDelUsuario() throws Throwable {
            User usuario = new User();
            usuario.setEmail("ana@example.org");
            usuario.setTelefono("600111222");

            Persona persona = crearPersona(usuario);

            assertEquals("ana@example.org", persona.getEmail());
            assertEquals("600111222", persona.getTelefono());
        }

        @Test
        void presentadoPorElUsuario_restauraElContactoDelSolicitanteYNoElDelInteresado() throws Throwable {
            Expediente original = expediente(false, false,
                    contacto(1L, "ana@example.org", "600111222"), contacto(2L, "ana@example.org", "600111222"));
            Expediente peticion = expediente(false, false,
                    contacto(1L, "otro@example.org", "699999999"), contacto(2L, "interesado@example.org", "711222333"));

            restaurarPersonas(peticion, original);

            assertEquals("ana@example.org", peticion.getPersonaSolicitante().getEmail());
            assertEquals("600111222", peticion.getPersonaSolicitante().getTelefono());
            assertEquals("interesado@example.org", peticion.getPersonaInteresada().getEmail());
            assertEquals("711222333", peticion.getPersonaInteresada().getTelefono());
        }

        @Test
        void enPapelParaMi_copiaAlSolicitanteElContactoTecleadoDelInteresado() throws Throwable {
            Expediente original = expediente(true, false, contacto(1L, null, null), contacto(2L, null, null));
            Expediente peticion = expediente(true, false,
                    contacto(1L, null, null), contacto(2L, "interesado@example.org", "711222333"));

            restaurarPersonas(peticion, original);

            assertEquals("interesado@example.org", peticion.getPersonaSolicitante().getEmail());
            assertEquals("711222333", peticion.getPersonaSolicitante().getTelefono());
        }

        @Test
        void enPapelEnRepresentacion_conservaElContactoTecleadoDelSolicitante() throws Throwable {
            Expediente original = expediente(true, true, contacto(1L, null, null), contacto(2L, null, null));
            Expediente peticion = expediente(true, true,
                    contacto(1L, "padre@example.org", "600111222"), contacto(2L, "hijo@example.org", "711222333"));

            restaurarPersonas(peticion, original);

            assertEquals("padre@example.org", peticion.getPersonaSolicitante().getEmail());
            assertEquals("600111222", peticion.getPersonaSolicitante().getTelefono());
        }

        private static Persona contacto(Long id, String email, String telefono) {
            Persona persona = persona(id);
            persona.setEmail(email);
            persona.setTelefono(telefono);
            return persona;
        }

        private static Expediente expediente(boolean presentadoEnPapel, boolean presentadoEnRepresentacion,
                                             Persona solicitante, Persona interesada) {
            Expediente expediente = new PruebaV1();
            expediente.setPresentadoEnPapel(presentadoEnPapel);
            expediente.setPresentadoEnRepresentacion(presentadoEnRepresentacion);
            expediente.setPersonaSolicitante(solicitante);
            expediente.setPersonaInteresada(interesada);
            return expediente;
        }
    }

    @Nested
    class ExigeMismaPersona {

        @Test
        void ambasNulas_noLanza() {
            assertDoesNotThrow(() -> exigeMismaPersona(null, null, "personaSolicitante"));
        }

        @Test
        void mismaId_noLanza() {
            assertDoesNotThrow(() -> exigeMismaPersona(persona(5L), persona(5L), "personaSolicitante"));
        }

        @Test
        void ambasSinId_noLanza() {
            assertDoesNotThrow(() -> exigeMismaPersona(persona(null), persona(null), "personaInteresada"));
        }

        @Test
        void idDistinta_lanzaConIdsYCampo() {
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> exigeMismaPersona(persona(2L), persona(1L), "personaInteresada"));

            assertEquals("La petición intenta cambiar la persona del campo 'personaInteresada' del expediente: de 1 a 2.", ex.getMessage());
        }

        @Test
        void personaNulaYOriginalSinId_lanzaAunqueLasIdsCoincidanEnNull() {
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> exigeMismaPersona(null, persona(null), "personaSolicitante"));

            assertEquals("La petición intenta cambiar la persona del campo 'personaSolicitante' del expediente: de null a null.", ex.getMessage());
        }

        @Test
        void personaConIdYOriginalNula_lanza() {
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> exigeMismaPersona(persona(7L), null, "personaSolicitante"));

            assertEquals("La petición intenta cambiar la persona del campo 'personaSolicitante' del expediente: de null a 7.", ex.getMessage());
        }

        @Test
        void personaNulaYOriginalConId_lanza() {
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> exigeMismaPersona(null, persona(3L), "personaInteresada"));

            assertEquals("La petición intenta cambiar la persona del campo 'personaInteresada' del expediente: de 3 a null.", ex.getMessage());
        }
    }

    @Nested
    class DescartarCambios {

        @Test
        void transaccionActiva_laMarcaParaRollbackYDesvinculaElExpediente() throws Throwable {
            EntityManager em = mock(EntityManager.class);
            EntityTransaction transaccion = mock(EntityTransaction.class);
            when(em.getTransaction()).thenReturn(transaccion);
            when(transaccion.isActive()).thenReturn(true);
            Expediente expediente = new PruebaV1();

            try (MockedStatic<JPA> jpa = mockStatic(JPA.class)) {
                jpa.when(JPA::em).thenReturn(em);

                descartarCambios(expediente);
            }

            verify(transaccion).setRollbackOnly();
            verify(em).detach(expediente);
        }

        @Test
        void sinTransaccionActiva_soloDesvinculaElExpediente() throws Throwable {
            EntityManager em = mock(EntityManager.class);
            EntityTransaction transaccion = mock(EntityTransaction.class);
            when(em.getTransaction()).thenReturn(transaccion);
            Expediente expediente = new PruebaV1();

            try (MockedStatic<JPA> jpa = mockStatic(JPA.class)) {
                jpa.when(JPA::em).thenReturn(em);

                descartarCambios(expediente);
            }

            verify(transaccion, never()).setRollbackOnly();
            verify(em).detach(expediente);
        }
    }

    @Nested
    class GetBeansValidationRulesPorEstado {

        @Test
        void devuelveSoloLasReglasDeLosMetodosAnotadosDelEstado() throws Throwable {
            ValidatorConReglas validator = new ValidatorConReglas();

            List<BeanValidationRules> reglas = getBeansValidationRules(validator, "ENTRADA_DATOS");

            assertEquals(2, reglas.size());
            Set<BeanValidationRules> mismasInstancias = Collections.newSetFromMap(new IdentityHashMap<>());
            mismasInstancias.addAll(reglas);
            assertTrue(mismasInstancias.contains(validator.reglasPresentar));
            assertTrue(mismasInstancias.contains(validator.reglasGuardar));
        }

        @Test
        void metodoAnotadoQueDevuelveNull_lanzaEnvuelto() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> getBeansValidationRules(new ValidatorConNull(), "ENTRADA_DATOS"));

            assertEquals("Error al obtener las reglas de validación para el estado: ENTRADA_DATOS en " + ValidatorConNull.class.getName(), ex.getMessage());
            assertInstanceOf(RuntimeException.class, ex.getCause());
            assertEquals("El método retorno null:getForStateEntradaDatosInEventPresentar", ex.getCause().getMessage());
        }

        @Test
        void sinMetodosDelEstado_lanzaEnvuelto() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> getBeansValidationRules(new ValidatorConReglas(), "REVISION"));

            assertEquals("Error al obtener las reglas de validación para el estado: REVISION en " + ValidatorConReglas.class.getName(), ex.getMessage());
            assertEquals("No se han encontrado las reglas de validación para el estado: REVISION", ex.getCause().getMessage());
        }
    }

    @Nested
    class GetFieldsValidationRules {

        @Test
        void sinBeans_devuelveListaVacia() throws Throwable {
            assertTrue(getFieldsValidationRules(List.of(), "getNombre").isEmpty());
        }

        @Test
        void devuelveSoloLasSubreglasDeCampoDeLosCamposConEseMetodo() throws Throwable {
            FieldValidationRules subDireccion = campo("getCalle", List.of());
            FieldValidationRules subOtroBean = campo("getNumero", List.of());
            ValidationRule reglaSimple = mock(ValidationRule.class);

            FieldValidationRules direccion = campo("getDireccion", List.of(subDireccion, reglaSimple));
            FieldValidationRules otroCampo = campo("getNombre", List.of(campo("getApellido", List.of())));
            FieldValidationRules direccionOtroBean = campo("getDireccion", List.of(subOtroBean));

            List<BeanValidationRules> beans = List.of(
                    new BeanValidationRules(List.of(direccion, otroCampo)),
                    new BeanValidationRules(List.of()),
                    new BeanValidationRules(List.of(direccionOtroBean)));

            List<FieldValidationRules> resultado = getFieldsValidationRules(beans, "getDireccion");

            assertEquals(2, resultado.size());
            assertSame(subDireccion, resultado.get(0));
            assertSame(subOtroBean, resultado.get(1));
        }

        @Test
        void campoConEseMetodoSinSubreglasDeCampo_devuelveListaVacia() throws Throwable {
            FieldValidationRules nombre = campo("getNombre", List.of(mock(ValidationRule.class)));

            List<FieldValidationRules> resultado = getFieldsValidationRules(List.of(new BeanValidationRules(List.of(nombre))), "getNombre");

            assertTrue(resultado.isEmpty());
        }

        @Test
        void ningunCampoConEseMetodo_devuelveListaVacia() throws Throwable {
            FieldValidationRules nombre = campo("getNombre", List.of(campo("getApellido", List.of())));

            List<FieldValidationRules> resultado = getFieldsValidationRules(List.of(new BeanValidationRules(List.of(nombre))), "getDireccion");

            assertTrue(resultado.isEmpty());
        }
    }

    private static FieldValidationRules campo(String nombreMetodo, List<ValidationRule> reglas) {
        KFunction<?> metodo = mock(KFunction.class);
        when(metodo.getName()).thenReturn(nombreMetodo);
        return new FieldValidationRules(metodo, reglas);
    }

    @SuppressWarnings("unchecked")
    private static List<FieldValidationRules> getFieldsValidationRules(List<BeanValidationRules> beans, String methodName) throws Throwable {
        Method method = Tramitador.class.getDeclaredMethod("getFieldsValidationRules", List.class, String.class);
        method.setAccessible(true);
        try {
            return (List<FieldValidationRules>) method.invoke(new Tramitador(), beans, methodName);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }

    public static class ValidatorConReglas implements StateEventValidator {
        final BeanValidationRules reglasPresentar = new BeanValidationRules(List.of());
        final BeanValidationRules reglasGuardar = new BeanValidationRules(List.of());
        final BeanValidationRules reglasSinAnotar = new BeanValidationRules(List.of());
        final BeanValidationRules reglasOtroEstado = new BeanValidationRules(List.of());

        @BeanValidationRulesForStateAndEvent
        public BeanValidationRules getForStateEntradaDatosInEventPresentar() {
            return reglasPresentar;
        }

        @BeanValidationRulesForStateAndEvent
        public BeanValidationRules getForStateEntradaDatosInEventGuardar() {
            return reglasGuardar;
        }

        public BeanValidationRules getForStateEntradaDatosInEventSinAnotar() {
            return reglasSinAnotar;
        }

        @BeanValidationRulesForStateAndEvent
        public BeanValidationRules getForStateFirmaInEventFirmar() {
            return reglasOtroEstado;
        }
    }

    public static class ValidatorConNull implements StateEventValidator {
        @BeanValidationRulesForStateAndEvent
        public BeanValidationRules getForStateEntradaDatosInEventPresentar() {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<BeanValidationRules> getBeansValidationRules(StateEventValidator validator, String state) throws Throwable {
        Method method = Tramitador.class.getDeclaredMethod("getBeansValidationRules", StateEventValidator.class, String.class);
        method.setAccessible(true);
        try {
            return (List<BeanValidationRules>) method.invoke(new Tramitador(), validator, state);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }

    private static Persona persona(Long id) {
        Persona persona = new Persona();
        persona.setId(id);
        return persona;
    }

    private static void descartarCambios(Expediente expediente) throws Throwable {
        Method method = Tramitador.class.getDeclaredMethod("descartarCambios", Expediente.class);
        method.setAccessible(true);
        try {
            method.invoke(null, expediente);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }

    private static Persona crearPersona(User user) throws Throwable {
        Method method = Tramitador.class.getDeclaredMethod("crearPersona", User.class);
        method.setAccessible(true);
        try {
            return (Persona) method.invoke(null, user);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }

    private static void restaurarPersonas(Expediente expediente, Expediente expedienteOriginal) throws Throwable {
        Method method = Tramitador.class.getDeclaredMethod("restaurarPersonas", Expediente.class, Expediente.class);
        method.setAccessible(true);
        try {
            method.invoke(null, expediente, expedienteOriginal);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }

    private static void exigeMismaPersona(Persona persona, Persona personaOriginal, String nombreCampo) throws Throwable {
        Method method = Tramitador.class.getDeclaredMethod("exigeMismaPersona", Persona.class, Persona.class, String.class);
        method.setAccessible(true);
        try {
            method.invoke(null, persona, personaOriginal, nombreCampo);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }
}
