package com.educaflow.system.ventanilla.controller;

import com.axelor.db.JpaRepository;
import com.axelor.db.Model;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.i18n.I18n;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.subsystem.expedientes.db.TipoExpediente;
import com.educaflow.subsystem.expedientes.db.TipoTramite;
import com.educaflow.subsystem.expedientes.db.Tramite;
import com.educaflow.system.ventanilla.db.AsistenteNuevoExpediente;
import com.educaflow.system.ventanilla.service.AsistenteNuevoExpedienteService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsistenteNuevoExpedienteControllerTest {

    private static final String NOMBRE_TRAMITE = "Anulación de matrícula";
    private static final String NOMBRE_TRAMITE_DERIVADO = "value:" + NOMBRE_TRAMITE;
    private static final String AYUDA_TRAMITE = "Texto de ayuda";
    private static final String NOMBRE_TIPO_TRAMITE = "Matrícula";

    private static final String TITULO_ERROR_CREAR = "No es posible crear el expediente";
    private static final String MENSAJE_PRECONDICION = "mensaje de precondición";
    private static final String MENSAJE_CENTRO = "Debe indicar el centro";
    private static final String MENSAJE_TRAMITE = "Debe indicar el trámite";
    private static final String MENSAJE_SIN_PERFIL_DE_INICIO = "No puede crear expedientes de este trámite en el centro indicado";

    private AsistenteNuevoExpedienteController controller;

    private ModelServiceFactory modelServiceFactory;
    private AsistenteNuevoExpedienteService asistenteNuevoExpedienteService;
    private ActionRequest actionRequest;
    private ActionResponse actionResponse;

    private Map<String, Object> context;

    private Centro centroA;
    private Centro centroB;
    private TipoTramite tipoTramiteAlumno;
    private Tramite tramite;

    private MockedStatic<JpaRepository> jpaRepositoryMock;
    private MockedStatic<I18n> i18nMock;

    @BeforeEach
    void setUp() throws Exception {
        controller = new AsistenteNuevoExpedienteController();

        modelServiceFactory = Mockito.mock(ModelServiceFactory.class);
        setField(controller, "modelServiceFactory", modelServiceFactory);

        asistenteNuevoExpedienteService = Mockito.mock(AsistenteNuevoExpedienteService.class);
        actionRequest = Mockito.mock(ActionRequest.class);
        actionResponse = Mockito.mock(ActionResponse.class);

        centroA = centro(1L, "Centro A");
        centroB = centro(2L, "Centro B");

        tipoTramiteAlumno = new TipoTramite();
        tipoTramiteAlumno.setId(20L);
        tipoTramiteAlumno.setName(NOMBRE_TIPO_TRAMITE);
        tipoTramiteAlumno.setTipoUsuario(tipoUsuario("ALUMNO"));

        tramite = new Tramite();
        tramite.setId(10L);
        tramite.setVersion(1);
        tramite.setName(NOMBRE_TRAMITE);
        tramite.setHelp(AYUDA_TRAMITE);
        tramite.setPermitidoPresentarEnRepresentacion(true);
        tramite.setTipoTramite(tipoTramiteAlumno);
        tramite.setDefaultTipoExpediente(new TipoExpediente());

        context = new HashMap<>();
        context.put("_model", AsistenteNuevoExpediente.class.getName());
        Map<String, Object> data = new HashMap<>();
        data.put("context", context);
        when(actionRequest.getData()).thenReturn(data);

        JpaRepository<AsistenteNuevoExpediente> repositorioAsistente = repositorioQueCrea();
        JpaRepository<Centro> repositorioCentro = repositorioQueEncuentra(centroA);
        JpaRepository<Tramite> repositorioTramite = repositorioQueEncuentra(tramite);

        jpaRepositoryMock = Mockito.mockStatic(JpaRepository.class);
        jpaRepositoryMock.when(() -> JpaRepository.of(AsistenteNuevoExpediente.class)).thenReturn(repositorioAsistente);
        jpaRepositoryMock.when(() -> JpaRepository.of(Centro.class)).thenReturn(repositorioCentro);
        jpaRepositoryMock.when(() -> JpaRepository.of(Tramite.class)).thenReturn(repositorioTramite);

        i18nMock = Mockito.mockStatic(I18n.class);
        i18nMock.when(() -> I18n.get(any(String.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(modelServiceFactory.resolve(AsistenteNuevoExpediente.class))
                .thenReturn(asistenteNuevoExpedienteService);

        // Cada allowProperties*/validate* solo lo usan los tests de su propia acción (prepararCentros/prepararTramites/recalcular/validateTriggerInitialEvent).
        Mockito.lenient().when(asistenteNuevoExpedienteService.allowPropertiesPrepararCentros())
                .thenReturn(AllowProperties.createDenyAllProperties());
        Mockito.lenient().when(asistenteNuevoExpedienteService.allowPropertiesPrepararTramites())
                .thenReturn(AllowProperties.createAllowProperties(Map.of("centro", Map.of())));
        Mockito.lenient().when(asistenteNuevoExpedienteService.allowPropertiesRecalcular())
                .thenReturn(AllowProperties.createAllowProperties(Map.of(
                        "tramite", Map.of(),
                        "centro", Map.of(),
                        "presentadoEnPapel", Map.of())));
        Mockito.lenient().when(asistenteNuevoExpedienteService.allowPropertiesTriggerInitialEvent())
                .thenReturn(AllowProperties.createAllowProperties(Map.of(
                        "tramite", Map.of(),
                        "centro", Map.of(),
                        "presentadoEnPapel", Map.of(),
                        "presentadoEnRepresentacion", Map.of())));

        Mockito.lenient().when(asistenteNuevoExpedienteService.validatePrepararCentros(any())).thenReturn(Optional.empty());
        Mockito.lenient().when(asistenteNuevoExpedienteService.validatePrepararTramites(any())).thenReturn(Optional.empty());
        Mockito.lenient().when(asistenteNuevoExpedienteService.validateRecalcular(any())).thenReturn(Optional.empty());
        Mockito.lenient().when(asistenteNuevoExpedienteService.validateTriggerInitialEvent(any())).thenReturn(Optional.empty());
    }

    @AfterEach
    void tearDown() {
        cerrarSiNoEsNulo(i18nMock);
        cerrarSiNoEsNulo(jpaRepositoryMock);
    }

    /* ------------------------------------------------------------------ */
    /* Helpers                                                            */
    /* ------------------------------------------------------------------ */

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = AsistenteNuevoExpedienteController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void cerrarSiNoEsNulo(MockedStatic<?> mockedStatic) {
        if (mockedStatic != null) {
            mockedStatic.close();
        }
    }

    @SuppressWarnings("unchecked")
    private static JpaRepository<AsistenteNuevoExpediente> repositorioQueCrea() {
        JpaRepository<AsistenteNuevoExpediente> repositorio = Mockito.mock(JpaRepository.class);
        Mockito.lenient().when(repositorio.create(null)).thenAnswer(invocation -> new AsistenteNuevoExpediente());
        return repositorio;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Model> JpaRepository<T> repositorioQueEncuentra(T entidad) {
        JpaRepository<T> repositorio = Mockito.mock(JpaRepository.class);
        Mockito.lenient().when(repositorio.find(entidad.getId())).thenReturn(entidad);
        return repositorio;
    }

    private static Centro centro(Long id, String name) {
        Centro centro = new Centro();
        centro.setId(id);
        centro.setVersion(1);
        centro.setName(name);
        return centro;
    }

    private static TipoUsuario tipoUsuario(String codigo) {
        TipoUsuario tipoUsuario = new TipoUsuario();
        tipoUsuario.setCodigo(codigo);
        return tipoUsuario;
    }

    private static Optional<BusinessMessages> mensajes(String texto) {
        return Optional.of(BusinessMessages.single(texto));
    }

    private static Map<String, Object> registroCentro(Centro centro) {
        return Map.of("id", centro.getId(), "version", centro.getVersion(), "name", centro.getName());
    }

    private static Map<String, Object> registroTramite(Tramite tramite) {
        Map<String, Object> registro = new LinkedHashMap<>();
        registro.put("id", tramite.getId());
        registro.put("version", tramite.getVersion());
        registro.put("name", tramite.getName());
        if (tramite.getTipoTramite() != null) {
            registro.put("tipoTramite.name", tramite.getTipoTramite().getName());
        }
        return registro;
    }

    private static AsistenteNuevoExpediente asistenteConCentros(Set<Centro> centrosDisponibles, boolean hayQueElegirCentro, Centro centro) {
        AsistenteNuevoExpediente asistente = new AsistenteNuevoExpediente();
        asistente.setCentrosDisponibles(centrosDisponibles);
        asistente.setHayQueElegirCentro(hayQueElegirCentro);
        asistente.setCentro(centro);
        return asistente;
    }

    private static AsistenteNuevoExpediente asistenteConTramites(Set<Tramite> tramitesDisponibles) {
        AsistenteNuevoExpediente asistente = new AsistenteNuevoExpediente();
        asistente.setTramitesDisponibles(tramitesDisponibles);
        return asistente;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> valorDevuelto(String campo) {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(actionResponse).setValue(eq(campo), captor.capture());
        return (List<Map<String, Object>>) captor.getValue();
    }

    private String errorSinTitulo() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(actionResponse).setError(captor.capture());
        return captor.getValue();
    }

    /* ------------------------------------------------------------------ */
    /* prepararCentros                                                    */
    /* ------------------------------------------------------------------ */

    @Test
    void prepararCentros_sinMensajes_invocaLaAccionYDevuelveLosTresValoresDelArranque() {
        when(asistenteNuevoExpedienteService.prepararCentros(any()))
                .thenReturn(asistenteConCentros(new LinkedHashSet<>(List.of(centroA, centroB)), true, null));

        controller.prepararCentros(actionRequest, actionResponse);

        List<Map<String, Object>> centrosDisponibles = valorDevuelto("centrosDisponibles");
        assertEquals(2, centrosDisponibles.size());
        assertTrue(centrosDisponibles.contains(registroCentro(centroA)));
        assertTrue(centrosDisponibles.contains(registroCentro(centroB)));

        verify(actionResponse).setValue("hayQueElegirCentro", true);
        verify(actionResponse).setValue("centro", null);
        verify(actionResponse, never()).setError(anyString());
    }

    @Test
    void prepararCentros_conUnSoloCentroCandidato_devuelveLaReferenciaDelCentro() {
        when(asistenteNuevoExpedienteService.prepararCentros(any()))
                .thenReturn(asistenteConCentros(new LinkedHashSet<>(List.of(centroA)), false, centroA));

        controller.prepararCentros(actionRequest, actionResponse);

        verify(actionResponse).setValue("centro", registroCentro(centroA));
        verify(actionResponse).setValue("hayQueElegirCentro", false);
    }

    @Test
    void prepararCentros_sinCentrosCandidatos_devuelveLaListaVacia() {
        when(asistenteNuevoExpedienteService.prepararCentros(any()))
                .thenReturn(asistenteConCentros(new LinkedHashSet<>(), false, null));

        controller.prepararCentros(actionRequest, actionResponse);

        assertTrue(valorDevuelto("centrosDisponibles").isEmpty());
        verify(actionResponse).setValue("centro", null);
        verify(actionResponse).setValue("hayQueElegirCentro", false);
        verify(actionResponse, never()).setError(anyString());
        verify(actionResponse, never()).setError(anyString(), anyString());
    }

    @Test
    void prepararCentros_conMensajes_contestaErrorYNoInvocaLaAccion() {
        when(asistenteNuevoExpedienteService.validatePrepararCentros(any())).thenReturn(mensajes(MENSAJE_PRECONDICION));

        controller.prepararCentros(actionRequest, actionResponse);

        assertTrue(errorSinTitulo().contains(MENSAJE_PRECONDICION));
        verify(actionResponse, never()).setError(anyString(), anyString());
        verify(asistenteNuevoExpedienteService, never()).prepararCentros(any());
    }

    @Test
    void prepararCentros_conCamposEnviadosPorElCliente_construyeElBeanConLaWhitelistDeLaAccion() {
        context.put("centro", Map.of("id", 1L));
        context.put("centrosDisponibles", List.of(Map.of("id", 1L)));
        context.put("hayQueElegirCentro", true);
        when(asistenteNuevoExpedienteService.prepararCentros(any()))
                .thenReturn(asistenteConCentros(new LinkedHashSet<>(List.of(centroA)), false, centroA));

        controller.prepararCentros(actionRequest, actionResponse);

        ArgumentCaptor<AsistenteNuevoExpediente> captor = ArgumentCaptor.forClass(AsistenteNuevoExpediente.class);
        verify(asistenteNuevoExpedienteService).prepararCentros(captor.capture());

        AsistenteNuevoExpediente bean = captor.getValue();
        assertNull(bean.getCentro(), "La whitelist de la acción es deny-all: el centro enviado no puede entrar");
        assertNull(bean.getCentrosDisponibles(), "El listado de centros lo calcula el servidor, no lo dicta el cliente");
        assertFalse(bean.getHayQueElegirCentro(), "El oráculo del arranque lo calcula el servidor, no lo dicta el cliente");
    }

    /* ------------------------------------------------------------------ */
    /* prepararTramites                                                   */
    /* ------------------------------------------------------------------ */

    @Test
    void prepararTramites_sinMensajes_devuelveElRegistroCompletoDeCadaTramite() {
        when(asistenteNuevoExpedienteService.prepararTramites(any()))
                .thenReturn(asistenteConTramites(new LinkedHashSet<>(List.of(tramite))));

        controller.prepararTramites(actionRequest, actionResponse);

        assertEquals(List.of(registroTramite(tramite)), valorDevuelto("tramitesDisponibles"));
    }

    @Test
    void prepararTramites_noTraduceElNombreDelTramite() {
        when(asistenteNuevoExpedienteService.prepararTramites(any()))
                .thenReturn(asistenteConTramites(new LinkedHashSet<>(List.of(tramite))));

        controller.prepararTramites(actionRequest, actionResponse);

        Map<String, Object> referencia = valorDevuelto("tramitesDisponibles").get(0);
        assertEquals(Set.of("id", "version", "name", "tipoTramite.name"), referencia.keySet());
        assertEquals(NOMBRE_TRAMITE, referencia.get("name"));
        i18nMock.verifyNoInteractions();
    }

    @Test
    void prepararTramites_noDevuelveHayQueElegirCentro() {
        AsistenteNuevoExpediente resultado = asistenteConTramites(new LinkedHashSet<>(List.of(tramite)));
        resultado.setHayQueElegirCentro(true);
        when(asistenteNuevoExpedienteService.prepararTramites(any())).thenReturn(resultado);

        controller.prepararTramites(actionRequest, actionResponse);

        verify(actionResponse, never()).setValue(eq("hayQueElegirCentro"), any());
    }

    @Test
    void prepararTramites_conMensajes_contestaErrorYNoInvocaLaAccion() {
        when(asistenteNuevoExpedienteService.validatePrepararTramites(any())).thenReturn(mensajes(MENSAJE_CENTRO));

        controller.prepararTramites(actionRequest, actionResponse);

        assertTrue(errorSinTitulo().contains(MENSAJE_CENTRO));
        verify(actionResponse, never()).setError(anyString(), anyString());
        verify(asistenteNuevoExpedienteService, never()).prepararTramites(any());
    }

    /* ------------------------------------------------------------------ */
    /* recalcular                                                         */
    /* ------------------------------------------------------------------ */

    @Test
    void recalcular_sinMensajes_devuelveLosSeisValoresDelPaso3() {
        AsistenteNuevoExpediente resultado = new AsistenteNuevoExpediente();
        resultado.setTramite(tramite);
        resultado.setPresentadoEnPapel(false);
        resultado.setPresentadoEnRepresentacion(null);
        resultado.setHayQuePreguntarPresentacion(true);
        resultado.setHayQuePreguntarParaQuien(true);
        when(asistenteNuevoExpedienteService.recalcular(any())).thenReturn(resultado);

        controller.recalcular(actionRequest, actionResponse);

        verify(actionResponse).setValue("nombreTramite", NOMBRE_TRAMITE_DERIVADO);
        verify(actionResponse).setValue("ayudaTramite", AYUDA_TRAMITE);
        verify(actionResponse).setAttr("presentadoEnPapel", "value:set", false);
        verify(actionResponse).setAttr("presentadoEnRepresentacion", "value:set", null);
        verify(actionResponse).setValue("hayQuePreguntarPresentacion", true);
        verify(actionResponse).setValue("hayQuePreguntarParaQuien", true);
    }

    @Test
    void recalcular_leeNombreYAyudaDelGetterDelBean() {
        AsistenteNuevoExpediente resultado = new AsistenteNuevoExpediente();
        resultado.setTramite(tramite);
        when(asistenteNuevoExpedienteService.recalcular(any())).thenReturn(resultado);

        controller.recalcular(actionRequest, actionResponse);

        verify(actionResponse).setValue("nombreTramite", NOMBRE_TRAMITE_DERIVADO);
        verify(actionResponse).setValue("ayudaTramite", AYUDA_TRAMITE);
    }

    @Test
    void recalcular_sinTramiteEnElBean_devuelveNombreYAyudaNulos() {
        when(asistenteNuevoExpedienteService.recalcular(any())).thenReturn(new AsistenteNuevoExpediente());

        controller.recalcular(actionRequest, actionResponse);

        verify(actionResponse).setValue("nombreTramite", null);
        verify(actionResponse).setValue("ayudaTramite", null);
    }

    @Test
    void recalcular_conMensajes_contestaErrorYNoInvocaLaAccion() {
        when(asistenteNuevoExpedienteService.validateRecalcular(any())).thenReturn(mensajes(MENSAJE_TRAMITE));

        controller.recalcular(actionRequest, actionResponse);

        assertTrue(errorSinTitulo().contains(MENSAJE_TRAMITE));
        verify(actionResponse, never()).setError(anyString(), anyString());
        verify(asistenteNuevoExpedienteService, never()).recalcular(any());
    }

    /* ------------------------------------------------------------------ */
    /* validateTriggerInitialEvent                                        */
    /* ------------------------------------------------------------------ */

    @Test
    void validateTriggerInitialEvent_sinMensajes_noContestaNingunError() {
        controller.validateTriggerInitialEvent(actionRequest, actionResponse);

        verify(actionResponse, never()).setError(anyString());
        verify(actionResponse, never()).setError(anyString(), anyString());
    }

    @Test
    void validateTriggerInitialEvent_conMensajes_contestaErrorConElTituloNoEsPosibleCrearElExpediente() {
        when(asistenteNuevoExpedienteService.validateTriggerInitialEvent(any()))
                .thenReturn(mensajes(MENSAJE_SIN_PERFIL_DE_INICIO));

        controller.validateTriggerInitialEvent(actionRequest, actionResponse);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(actionResponse).setError(captor.capture(), eq(TITULO_ERROR_CREAR));
        assertTrue(captor.getValue().contains(MENSAJE_SIN_PERFIL_DE_INICIO));
        verify(actionResponse, never()).setError(anyString());
    }

    @Test
    void validateTriggerInitialEvent_conCamposDelServidorEnviadosPorElCliente_construyeElBeanConSuWhitelist() {
        context.put("centro", Map.of("id", 1L));
        context.put("tramite", Map.of("id", 10L));
        context.put("presentadoEnPapel", true);
        context.put("presentadoEnRepresentacion", false);
        context.put("hayQuePreguntarPresentacion", true);
        context.put("hayQuePreguntarParaQuien", true);
        context.put("hayQueElegirCentro", true);
        context.put("tramitesDisponibles", List.of(Map.of("id", 10L)));

        controller.validateTriggerInitialEvent(actionRequest, actionResponse);

        ArgumentCaptor<AsistenteNuevoExpediente> captor = ArgumentCaptor.forClass(AsistenteNuevoExpediente.class);
        verify(asistenteNuevoExpedienteService).validateTriggerInitialEvent(captor.capture());

        AsistenteNuevoExpediente bean = captor.getValue();
        assertSame(centroA, bean.getCentro());
        assertSame(tramite, bean.getTramite());
        assertTrue(bean.getPresentadoEnPapel());
        assertFalse(bean.getPresentadoEnRepresentacion());
        assertFalse(bean.getHayQuePreguntarPresentacion());
        assertFalse(bean.getHayQuePreguntarParaQuien());
        assertFalse(bean.getHayQueElegirCentro());
        assertNull(bean.getTramitesDisponibles());
    }

    @Test
    void validateTriggerInitialEvent_noInvocaNingunaAccionDelServicio() {
        controller.validateTriggerInitialEvent(actionRequest, actionResponse);

        verify(asistenteNuevoExpedienteService, never()).prepararCentros(any());
        verify(asistenteNuevoExpedienteService, never()).prepararTramites(any());
        verify(asistenteNuevoExpedienteService, never()).recalcular(any());
    }
}
