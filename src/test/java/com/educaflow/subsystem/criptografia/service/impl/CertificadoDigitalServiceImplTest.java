package com.educaflow.subsystem.criptografia.service.impl;

import com.axelor.auth.db.User;
import com.axelor.auth.db.repo.UserRepository;
import com.axelor.db.Query;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.criptografia.AlmacenClave;
import com.educaflow.base.infrastructure.criptografia.AlmacenClaveDispositivo;
import com.educaflow.base.infrastructure.criptografia.AlmacenClaveFichero;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.subsystem.criptografia.db.Alias;
import com.educaflow.subsystem.criptografia.db.CertificadoDigital;
import com.educaflow.subsystem.criptografia.db.DispositivoCriptografico;
import com.educaflow.subsystem.criptografia.db.TipoUbicacionCertificado;
import com.educaflow.subsystem.criptografia.db.repo.CertificadoDigitalRepository;
import com.educaflow.subsystem.criptografia.service.DatosTitular;
import com.educaflow.subsystem.criptografia.service.SituacionFirma;
import jakarta.persistence.NonUniqueResultException;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificadoDigitalServiceImplTest {

    private static final String DNI = "85432016B";
    private static final String DNI_CON_USUARIO = "29050788V";
    private static final String DNI_SIN_USUARIO = "12345678Z";
    private static final String DNI_INVALIDO = "12345678A";
    private static final String DNI_A_MEDIO_TECLEAR = "1234";
    private static final String RUTA_CLASSPATH_CERTIFICADO = "firma/mi_certificado.p12";
    private static final String RUTA_CLASSPATH_CERTIFICADO_SECRETARIO =
            "firma/instalar_certificado_criptografico/secretario.p12";
    private static final String CLAVE = "nadanada";
    private static final String CLAVE_TECLEADA_DISTINTA = "claveTecleadaDistinta";
    private static final String CLAVE_EN_BLANCO = "   ";
    private static final String CLAVE_SECRETA = "claveSecretaDePrueba";
    private static final String NOMBRE_TITULAR = "Secretario";
    private static final String APELLIDOS_TITULAR = "CIPFP Mislata";
    private static final String NOMBRE_ADMINISTRADOR = "Ana";
    private static final String APELLIDOS_ADMINISTRADOR = "García López";
    private static final String APELLIDOS_ADMINISTRADOR_CORREGIDOS = "García Pérez";
    private static final String NOMBRE_DEL_CLIENTE = "Impostor";
    private static final String APELLIDOS_DEL_CLIENTE = "Falso";
    private static final String MENSAJE_PASSWORD_NULL = "El password no puede ser null";
    private static final String MENSAJE_DNI_NO_VALIDO = "El DNI no es válido";
    private static final String MENSAJE_NOMBRE_OBLIGATORIO = "El nombre es obligatorio";
    private static final String MENSAJE_APELLIDOS_OBLIGATORIOS = "Los apellidos son obligatorios";
    private static final String MENSAJE_HABILITADO_DUPLICADO =
            "Ya existe un certificado digital habilitado para el DNI ";

    private CertificadoDigitalRepository repository;
    private UserRepository userRepository;
    private CertificadoDigitalServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        repository = Mockito.mock(CertificadoDigitalRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        service = new CertificadoDigitalServiceImpl(CertificadoDigital.class, repository);
        setField(service, "userRepository", userRepository);
    }

    /* ------------------------------------------------------------------ */
    /* Helpers                                                            */
    /* ------------------------------------------------------------------ */

    /**
     * Inyecta un colaborador en un campo {@code @Inject} privado del servicio: en un test unitario no hay
     * inyector Guice, así que se hace por reflexión (misma técnica que {@code TareaFirmaControllerTest}).
     */
    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = CertificadoDigitalServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private CertificadoDigital certificadoDispositivoPkcs11() {
        DispositivoCriptografico dispositivoCriptografico = new DispositivoCriptografico();
        dispositivoCriptografico.setSlot(1);

        Alias alias = new Alias();
        alias.setName("certificado-centro");

        CertificadoDigital certificado = new CertificadoDigital();
        certificado.setDni(DNI);
        certificado.setTipoCertificado(TipoUbicacionCertificado.DISPOSITIVO_PKCS11);
        certificado.setDispositivoCriptografico(dispositivoCriptografico);
        certificado.setAlias(alias);
        return certificado;
    }

    private CertificadoDigital certificadoClasspath(String password) {
        CertificadoDigital certificado = new CertificadoDigital();
        certificado.setDni(DNI);
        certificado.setTipoCertificado(TipoUbicacionCertificado.CLASSPATH);
        certificado.setRutaClasspath(RUTA_CLASSPATH_CERTIFICADO);
        certificado.setPassword(password);
        certificado.setEnabled(Boolean.TRUE);
        return certificado;
    }

    private CertificadoDigital certificadoFicheroBd(MetaFile fichero, String password) {
        CertificadoDigital certificado = new CertificadoDigital();
        certificado.setDni(DNI);
        certificado.setTipoCertificado(TipoUbicacionCertificado.FICHERO_BD);
        certificado.setFichero(fichero);
        certificado.setPassword(password);
        certificado.setEnabled(Boolean.TRUE);
        return certificado;
    }

    private CertificadoDigital certificadoSistemaArchivos(Path rutaCertificado, String password) {
        CertificadoDigital certificado = new CertificadoDigital();
        certificado.setDni(DNI);
        certificado.setTipoCertificado(TipoUbicacionCertificado.SISTEMA_ARCHIVOS);
        certificado.setRutaSistemaArchivos(rutaCertificado.toString());
        certificado.setPassword(password);
        certificado.setEnabled(Boolean.TRUE);
        return certificado;
    }

    /**
     * Certificado «por lo demás válido» de la descripción de tests: tipo {@code CLASSPATH} con una ruta que
     * existe en el classpath de test y un DNI con formato válido, para que {@code validateCertificado} no
     * añada mensajes propios y el escenario se centre en la regla que se está comprobando.
     */
    private CertificadoDigital certificadoValido(String dni) {
        CertificadoDigital certificado = new CertificadoDigital();
        certificado.setDni(dni);
        certificado.setTipoCertificado(TipoUbicacionCertificado.CLASSPATH);
        certificado.setRutaClasspath(RUTA_CLASSPATH_CERTIFICADO);
        return certificado;
    }

    private User usuario(String nombre, String apellidos) {
        User user = new User();
        user.setNombre(nombre);
        user.setApellidos(apellidos);
        return user;
    }

    private CertificadoDigital certificadoConId(Long id) {
        CertificadoDigital certificado = new CertificadoDigital();
        certificado.setId(id);
        return certificado;
    }

    @SuppressWarnings("unchecked")
    private Query<CertificadoDigital> mockQuery() {
        return Mockito.mock(Query.class);
    }

    /** Programa el finder de habilitados para la lectura del vigente ({@code fetchOne}). */
    private void stubCertificadoHabilitado(String dni, CertificadoDigital certificado) {
        Query<CertificadoDigital> query = mockQuery();
        when(repository.findByDniHabilitados(dni)).thenReturn(query);
        when(query.fetchOne()).thenReturn(certificado);
    }

    /** Programa el finder de habilitados para la lista que consulta V-CertificadoDigital-005 ({@code fetch}). */
    private void stubCertificadosHabilitados(String dni, List<CertificadoDigital> certificados) {
        Query<CertificadoDigital> query = mockQuery();
        when(repository.findByDniHabilitados(dni)).thenReturn(query);
        when(query.fetch()).thenReturn(certificados);
    }

    /**
     * Comprueba que el almacén es un {@link AlmacenClaveFichero} con la clave esperada y
     * cierra el {@code InputStream} del certificado, que el servicio abre sobre un recurso
     * real (classpath o sistema de archivos) y nadie más cerraría.
     */
    private void assertAlmacenFicheroConClave(AlmacenClave almacenClave, String claveEsperada) throws IOException {
        AlmacenClaveFichero almacenClaveFichero = assertInstanceOf(AlmacenClaveFichero.class, almacenClave);
        try (InputStream contenidoCertificado = almacenClaveFichero.getFileCertificate()) {
            assertNotNull(contenidoCertificado);
            assertEquals(claveEsperada, almacenClaveFichero.getPassword());
        }
    }

    private void assertContieneMensaje(Optional<BusinessMessages> messages, String campo, String texto) {
        assertTrue(messages.isPresent(), "Se esperaba al menos un mensaje de validación");
        assertTrue(messages.get().stream()
                        .anyMatch(message -> campo.equals(message.getFieldName()) && texto.equals(message.getMessage())),
                () -> "No hay ningún mensaje del campo «" + campo + "» con el texto «" + texto
                        + "». Mensajes obtenidos: " + messages.get());
    }

    private void assertMensajeUnico(Optional<BusinessMessages> messages, String campo, String texto) {
        assertContieneMensaje(messages, campo, texto);
        assertEquals(1, messages.get().size(),
                () -> "Se esperaba un único mensaje. Mensajes obtenidos: " + messages.get());
    }

    private byte[] contenidoDelRecursoDeClasspath(String rutaClasspath) throws IOException {
        try (InputStream recurso = getClass().getClassLoader().getResourceAsStream(rutaClasspath)) {
            assertNotNull(recurso, () -> "No se encuentra el recurso de test en el classpath: " + rutaClasspath);
            return recurso.readAllBytes();
        }
    }

    /* ------------------------------------------------------------------ */
    /* insert                                                             */
    /* ------------------------------------------------------------------ */

    @Test
    void insert_dniDeUsuarioExistente_asignaNombreYApellidosDeLaFichaYMarcaElFlag() {
        CertificadoDigital certificado = certificadoValido(DNI_CON_USUARIO);
        certificado.setEnabled(Boolean.TRUE);
        when(userRepository.findByDni(DNI_CON_USUARIO)).thenReturn(usuario(NOMBRE_TITULAR, APELLIDOS_TITULAR));
        stubCertificadosHabilitados(DNI_CON_USUARIO, List.of());
        when(repository.save(certificado)).thenReturn(certificado);

        service.insert(certificado);

        assertEquals(NOMBRE_TITULAR, certificado.getNombre());
        assertEquals(APELLIDOS_TITULAR, certificado.getApellidos());
        assertTrue(certificado.getNombreTomadoDelUsuario());
        verify(repository).save(certificado);
    }

    @Test
    void insert_dniSinUsuario_conservaNombreYApellidosDelAdministradorYDejaElFlagAFalse() {
        CertificadoDigital certificado = certificadoValido(DNI_SIN_USUARIO);
        certificado.setNombre(NOMBRE_ADMINISTRADOR);
        certificado.setApellidos(APELLIDOS_ADMINISTRADOR);
        when(userRepository.findByDni(DNI_SIN_USUARIO)).thenReturn(null);
        stubCertificadosHabilitados(DNI_SIN_USUARIO, List.of());
        when(repository.save(certificado)).thenReturn(certificado);

        service.insert(certificado);

        assertEquals(NOMBRE_ADMINISTRADOR, certificado.getNombre());
        assertEquals(APELLIDOS_ADMINISTRADOR, certificado.getApellidos());
        assertFalse(certificado.getNombreTomadoDelUsuario());
        verify(repository).save(certificado);
    }

    @Test
    void insert_dniDeUsuarioExistente_descartaElNombreYElFlagQueEnviaElCliente() {
        CertificadoDigital certificado = certificadoValido(DNI_CON_USUARIO);
        certificado.setEnabled(Boolean.TRUE);
        certificado.setNombre(NOMBRE_DEL_CLIENTE);
        certificado.setApellidos(APELLIDOS_DEL_CLIENTE);
        certificado.setNombreTomadoDelUsuario(Boolean.TRUE);
        when(userRepository.findByDni(DNI_CON_USUARIO)).thenReturn(usuario(NOMBRE_TITULAR, APELLIDOS_TITULAR));
        stubCertificadosHabilitados(DNI_CON_USUARIO, List.of());
        when(repository.save(certificado)).thenReturn(certificado);

        service.insert(certificado);

        assertEquals(NOMBRE_TITULAR, certificado.getNombre());
        assertEquals(APELLIDOS_TITULAR, certificado.getApellidos());
        assertTrue(certificado.getNombreTomadoDelUsuario());
    }

    @Test
    void insert_dniSinUsuarioYFlagTrueDelCliente_fuerzaElFlagAFalse() {
        CertificadoDigital certificado = certificadoValido(DNI_SIN_USUARIO);
        certificado.setNombre(NOMBRE_ADMINISTRADOR);
        certificado.setApellidos(APELLIDOS_ADMINISTRADOR);
        certificado.setNombreTomadoDelUsuario(Boolean.TRUE);
        when(userRepository.findByDni(DNI_SIN_USUARIO)).thenReturn(null);
        stubCertificadosHabilitados(DNI_SIN_USUARIO, List.of());
        when(repository.save(certificado)).thenReturn(certificado);

        service.insert(certificado);

        assertFalse(certificado.getNombreTomadoDelUsuario());
        assertEquals(NOMBRE_ADMINISTRADOR, certificado.getNombre());
    }

    @Test
    void insert_usuarioTitularConNombreVacioEnSuFicha_copiaLosValoresVaciosYMarcaElFlag() {
        CertificadoDigital certificado = certificadoValido(DNI_CON_USUARIO);
        certificado.setNombre(NOMBRE_ADMINISTRADOR);
        certificado.setApellidos(APELLIDOS_ADMINISTRADOR);
        when(userRepository.findByDni(DNI_CON_USUARIO)).thenReturn(usuario(null, null));
        stubCertificadosHabilitados(DNI_CON_USUARIO, List.of());
        when(repository.save(certificado)).thenReturn(certificado);

        service.insert(certificado);

        assertNull(certificado.getNombre());
        assertNull(certificado.getApellidos());
        assertTrue(certificado.getNombreTomadoDelUsuario());
    }

    @Test
    void insert_variosUsuariosConElMismoDni_propagaLaExcepcionDelFinder() {
        CertificadoDigital certificado = certificadoValido(DNI_CON_USUARIO);
        certificado.setEnabled(Boolean.FALSE);
        when(userRepository.findByDni(DNI_CON_USUARIO)).thenThrow(new NonUniqueResultException());

        assertThrows(NonUniqueResultException.class, () -> service.insert(certificado));

        verify(repository, never()).save(any());
    }

    @Test
    void insert_certificadoInvalido_lanzaValidationExceptionYNoPersiste() {
        CertificadoDigital certificado = certificadoValido(DNI_SIN_USUARIO);
        certificado.setEnabled(Boolean.FALSE);
        when(userRepository.findByDni(DNI_SIN_USUARIO)).thenReturn(null);

        ValidationException ex = assertThrows(ValidationException.class, () -> service.insert(certificado));

        assertTrue(ex.getMessage().contains(MENSAJE_NOMBRE_OBLIGATORIO),
                () -> "El mensaje de la excepción no contiene «" + MENSAJE_NOMBRE_OBLIGATORIO + "»: " + ex.getMessage());
        assertTrue(ex.getMessage().contains(MENSAJE_APELLIDOS_OBLIGATORIOS),
                () -> "El mensaje de la excepción no contiene «" + MENSAJE_APELLIDOS_OBLIGATORIOS + "»: " + ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void insert_certificadoValido_devuelveLoQueDevuelveRepositorySave() {
        CertificadoDigital certificado = certificadoValido(DNI_CON_USUARIO);
        certificado.setEnabled(Boolean.TRUE);
        CertificadoDigital certificadoPersistido = certificadoValido(DNI_CON_USUARIO);
        when(userRepository.findByDni(DNI_CON_USUARIO)).thenReturn(usuario(NOMBRE_TITULAR, APELLIDOS_TITULAR));
        stubCertificadosHabilitados(DNI_CON_USUARIO, List.of());
        when(repository.save(certificado)).thenReturn(certificadoPersistido);

        CertificadoDigital resultado = service.insert(certificado);

        assertSame(certificadoPersistido, resultado);
    }

    /* ------------------------------------------------------------------ */
    /* update                                                             */
    /* ------------------------------------------------------------------ */

    @Test
    void update_clienteCambiaElDni_restauraElDniDelOriginal() {
        CertificadoDigital certificadoOriginal = certificadoValido(DNI_CON_USUARIO);
        certificadoOriginal.setId(1L);
        certificadoOriginal.setNombreTomadoDelUsuario(Boolean.TRUE);
        certificadoOriginal.setNombre(NOMBRE_TITULAR);
        certificadoOriginal.setApellidos(APELLIDOS_TITULAR);

        CertificadoDigital certificado = certificadoValido(DNI_SIN_USUARIO);
        certificado.setId(1L);
        certificado.setEnabled(Boolean.FALSE);
        when(repository.save(certificado)).thenReturn(certificado);

        service.update(certificado, certificadoOriginal);

        assertEquals(DNI_CON_USUARIO, certificado.getDni());
        verify(repository).save(certificado);
    }

    @Test
    void update_originalConNombreTomadoDelUsuario_restauraNombreApellidosYFlagDelOriginal() {
        CertificadoDigital certificadoOriginal = certificadoValido(DNI_CON_USUARIO);
        certificadoOriginal.setId(1L);
        certificadoOriginal.setEnabled(Boolean.FALSE);
        certificadoOriginal.setNombreTomadoDelUsuario(Boolean.TRUE);
        certificadoOriginal.setNombre(NOMBRE_TITULAR);
        certificadoOriginal.setApellidos(APELLIDOS_TITULAR);

        CertificadoDigital certificado = certificadoValido(DNI_CON_USUARIO);
        certificado.setEnabled(Boolean.FALSE);
        certificado.setNombre("Otro");
        certificado.setApellidos("Distintos");
        certificado.setNombreTomadoDelUsuario(Boolean.FALSE);
        when(repository.save(certificado)).thenReturn(certificado);

        service.update(certificado, certificadoOriginal);

        assertEquals(NOMBRE_TITULAR, certificado.getNombre());
        assertEquals(APELLIDOS_TITULAR, certificado.getApellidos());
        assertTrue(certificado.getNombreTomadoDelUsuario());
    }

    @Test
    void update_originalConNombreEscritoPorElAdministrador_conservaElNombreDelFormularioYElFlagAFalse() {
        CertificadoDigital certificadoOriginal = certificadoValido(DNI_SIN_USUARIO);
        certificadoOriginal.setId(1L);
        certificadoOriginal.setEnabled(Boolean.FALSE);
        certificadoOriginal.setNombreTomadoDelUsuario(Boolean.FALSE);
        certificadoOriginal.setNombre(NOMBRE_ADMINISTRADOR);
        certificadoOriginal.setApellidos(APELLIDOS_ADMINISTRADOR);

        CertificadoDigital certificado = certificadoValido(DNI_SIN_USUARIO);
        certificado.setEnabled(Boolean.FALSE);
        certificado.setNombre(NOMBRE_ADMINISTRADOR);
        certificado.setApellidos(APELLIDOS_ADMINISTRADOR_CORREGIDOS);
        certificado.setNombreTomadoDelUsuario(Boolean.TRUE);
        when(repository.save(certificado)).thenReturn(certificado);

        service.update(certificado, certificadoOriginal);

        assertEquals(APELLIDOS_ADMINISTRADOR_CORREGIDOS, certificado.getApellidos());
        assertEquals(NOMBRE_ADMINISTRADOR, certificado.getNombre());
        assertFalse(certificado.getNombreTomadoDelUsuario());
    }

    @Test
    void update_originalSinFlagAsignado_seComportaComoEscritoPorElAdministrador() {
        CertificadoDigital certificadoOriginal = certificadoValido(DNI_SIN_USUARIO);
        certificadoOriginal.setId(1L);
        certificadoOriginal.setEnabled(Boolean.FALSE);
        certificadoOriginal.setNombre(NOMBRE_TITULAR);
        certificadoOriginal.setApellidos(APELLIDOS_TITULAR);

        CertificadoDigital certificado = certificadoValido(DNI_SIN_USUARIO);
        certificado.setEnabled(Boolean.FALSE);
        certificado.setNombre(NOMBRE_ADMINISTRADOR);
        certificado.setApellidos(APELLIDOS_ADMINISTRADOR_CORREGIDOS);
        when(repository.save(certificado)).thenReturn(certificado);

        service.update(certificado, certificadoOriginal);

        assertEquals(NOMBRE_ADMINISTRADOR, certificado.getNombre());
        assertEquals(APELLIDOS_ADMINISTRADOR_CORREGIDOS, certificado.getApellidos());
        assertFalse(certificado.getNombreTomadoDelUsuario());
    }

    @Test
    void update_certificadoInvalido_lanzaValidationExceptionYNoPersiste() {
        CertificadoDigital certificadoOriginal = certificadoValido(DNI_SIN_USUARIO);
        certificadoOriginal.setId(1L);
        certificadoOriginal.setEnabled(Boolean.FALSE);
        certificadoOriginal.setNombreTomadoDelUsuario(Boolean.FALSE);

        CertificadoDigital certificado = certificadoValido(DNI_SIN_USUARIO);
        certificado.setEnabled(Boolean.FALSE);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.update(certificado, certificadoOriginal));

        assertTrue(ex.getMessage().contains(MENSAJE_NOMBRE_OBLIGATORIO),
                () -> "El mensaje de la excepción no contiene «" + MENSAJE_NOMBRE_OBLIGATORIO + "»: " + ex.getMessage());
        assertTrue(ex.getMessage().contains(MENSAJE_APELLIDOS_OBLIGATORIOS),
                () -> "El mensaje de la excepción no contiene «" + MENSAJE_APELLIDOS_OBLIGATORIOS + "»: " + ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void update_certificadoValido_devuelveLoQueDevuelveRepositorySave() {
        CertificadoDigital certificadoOriginal = certificadoValido(DNI_CON_USUARIO);
        certificadoOriginal.setId(1L);
        certificadoOriginal.setEnabled(Boolean.FALSE);
        certificadoOriginal.setNombreTomadoDelUsuario(Boolean.FALSE);

        CertificadoDigital certificado = certificadoValido(DNI_CON_USUARIO);
        certificado.setEnabled(Boolean.FALSE);
        certificado.setNombre(NOMBRE_ADMINISTRADOR);
        certificado.setApellidos(APELLIDOS_ADMINISTRADOR);

        CertificadoDigital certificadoPersistido = certificadoValido(DNI_CON_USUARIO);
        when(repository.save(certificado)).thenReturn(certificadoPersistido);

        CertificadoDigital resultado = service.update(certificado, certificadoOriginal);

        assertSame(certificadoPersistido, resultado);
    }

    /* ------------------------------------------------------------------ */
    /* validateInsert                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    void validateInsert_sinUsuarioTitularYSinNombre_devuelveMensajeElNombreEsObligatorio() {
        CertificadoDigital certificado = certificadoValido(DNI_SIN_USUARIO);
        certificado.setEnabled(Boolean.FALSE);
        certificado.setApellidos(APELLIDOS_ADMINISTRADOR);
        when(userRepository.findByDni(DNI_SIN_USUARIO)).thenReturn(null);

        Optional<BusinessMessages> messages = service.validateInsert(certificado);

        assertMensajeUnico(messages, "nombre", MENSAJE_NOMBRE_OBLIGATORIO);
    }

    @Test
    void validateInsert_sinUsuarioTitularYSinApellidos_devuelveMensajeLosApellidosSonObligatorios() {
        CertificadoDigital certificado = certificadoValido(DNI_SIN_USUARIO);
        certificado.setEnabled(Boolean.FALSE);
        certificado.setNombre(NOMBRE_ADMINISTRADOR);
        when(userRepository.findByDni(DNI_SIN_USUARIO)).thenReturn(null);

        Optional<BusinessMessages> messages = service.validateInsert(certificado);

        assertMensajeUnico(messages, "apellidos", MENSAJE_APELLIDOS_OBLIGATORIOS);
    }

    @Test
    void validateInsert_sinUsuarioTitularYSinNombreNiApellidos_devuelveLosDosMensajes() {
        CertificadoDigital certificado = certificadoValido(DNI_SIN_USUARIO);
        certificado.setEnabled(Boolean.FALSE);
        when(userRepository.findByDni(DNI_SIN_USUARIO)).thenReturn(null);

        Optional<BusinessMessages> messages = service.validateInsert(certificado);

        assertContieneMensaje(messages, "nombre", MENSAJE_NOMBRE_OBLIGATORIO);
        assertContieneMensaje(messages, "apellidos", MENSAJE_APELLIDOS_OBLIGATORIOS);
        assertEquals(2, messages.get().size(),
                () -> "Se esperaban exactamente los dos mensajes de obligatoriedad: " + messages.get());
    }

    @Test
    void validateInsert_sinUsuarioTitularYNombreEnBlanco_devuelveMensajeElNombreEsObligatorio() {
        CertificadoDigital certificado = certificadoValido(DNI_SIN_USUARIO);
        certificado.setEnabled(Boolean.FALSE);
        certificado.setNombre("   ");
        certificado.setApellidos(APELLIDOS_ADMINISTRADOR);
        when(userRepository.findByDni(DNI_SIN_USUARIO)).thenReturn(null);

        Optional<BusinessMessages> messages = service.validateInsert(certificado);

        assertMensajeUnico(messages, "nombre", MENSAJE_NOMBRE_OBLIGATORIO);
    }

    @Test
    void validateInsert_conUsuarioTitular_noExigeNombreNiApellidos() {
        CertificadoDigital certificado = certificadoValido(DNI_CON_USUARIO);
        certificado.setEnabled(Boolean.FALSE);
        when(userRepository.findByDni(DNI_CON_USUARIO)).thenReturn(usuario(NOMBRE_TITULAR, APELLIDOS_TITULAR));

        Optional<BusinessMessages> messages = service.validateInsert(certificado);

        assertEquals(Optional.empty(), messages);
    }

    @Test
    void validateInsert_habilitadoYYaExisteOtroHabilitadoConEseDni_devuelveMensajeDeCertificadoHabilitadoDuplicado() {
        CertificadoDigital certificado = certificadoValido(DNI_CON_USUARIO);
        certificado.setEnabled(Boolean.TRUE);
        stubCertificadosHabilitados(DNI_CON_USUARIO, List.of(certificadoConId(1L)));
        when(userRepository.findByDni(DNI_CON_USUARIO)).thenReturn(usuario(NOMBRE_TITULAR, APELLIDOS_TITULAR));

        Optional<BusinessMessages> messages = service.validateInsert(certificado);

        assertMensajeUnico(messages, "enabled", MENSAJE_HABILITADO_DUPLICADO + DNI_CON_USUARIO);
    }

    @Test
    void validateInsert_habilitadoYSinOtrosHabilitadosDeEseDni_devuelveOptionalVacio() {
        CertificadoDigital certificado = certificadoValido(DNI_CON_USUARIO);
        certificado.setEnabled(Boolean.TRUE);
        stubCertificadosHabilitados(DNI_CON_USUARIO, List.of());
        when(userRepository.findByDni(DNI_CON_USUARIO)).thenReturn(usuario(NOMBRE_TITULAR, APELLIDOS_TITULAR));

        Optional<BusinessMessages> messages = service.validateInsert(certificado);

        assertEquals(Optional.empty(), messages);
    }

    @Test
    void validateInsert_deshabilitado_noConsultaLosHabilitadosDelDni() {
        CertificadoDigital certificado = certificadoValido(DNI_CON_USUARIO);
        certificado.setEnabled(Boolean.FALSE);
        when(userRepository.findByDni(DNI_CON_USUARIO)).thenReturn(usuario(NOMBRE_TITULAR, APELLIDOS_TITULAR));

        Optional<BusinessMessages> messages = service.validateInsert(certificado);

        assertEquals(Optional.empty(), messages);
        verify(repository, never()).findByDniHabilitados(any());
    }

    @Test
    void validateInsert_dniRepetidoEnCertificadosDeshabilitados_noRechazaPorDniDuplicado() {
        CertificadoDigital certificado = certificadoValido(DNI_CON_USUARIO);
        certificado.setEnabled(Boolean.TRUE);
        stubCertificadosHabilitados(DNI_CON_USUARIO, List.of());
        when(userRepository.findByDni(DNI_CON_USUARIO)).thenReturn(usuario(NOMBRE_TITULAR, APELLIDOS_TITULAR));

        Optional<BusinessMessages> messages = service.validateInsert(certificado);

        assertEquals(Optional.empty(), messages);
    }

    @Test
    void validateInsert_dniConFormatoInvalido_devuelveMensajeElDniNoEsValido() {
        CertificadoDigital certificado = certificadoValido(DNI_INVALIDO);
        certificado.setEnabled(Boolean.FALSE);
        certificado.setNombre(NOMBRE_ADMINISTRADOR);
        certificado.setApellidos(APELLIDOS_ADMINISTRADOR);
        when(userRepository.findByDni(DNI_INVALIDO)).thenReturn(null);

        Optional<BusinessMessages> messages = service.validateInsert(certificado);

        assertMensajeUnico(messages, "dni", MENSAJE_DNI_NO_VALIDO);
    }

    @Test
    void validateInsert_certificadoCompletoConUsuarioTitular_devuelveOptionalVacio() {
        CertificadoDigital certificado = certificadoValido(DNI_CON_USUARIO);
        certificado.setEnabled(Boolean.TRUE);
        stubCertificadosHabilitados(DNI_CON_USUARIO, List.of());
        when(userRepository.findByDni(DNI_CON_USUARIO)).thenReturn(usuario(NOMBRE_TITULAR, APELLIDOS_TITULAR));

        Optional<BusinessMessages> messages = service.validateInsert(certificado);

        assertEquals(Optional.empty(), messages);
    }

    /* ------------------------------------------------------------------ */
    /* validateUpdate                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    void validateUpdate_originalEscritoPorElAdministradorYSinNombre_devuelveMensajeElNombreEsObligatorio() {
        CertificadoDigital certificadoOriginal = certificadoValido(DNI_SIN_USUARIO);
        certificadoOriginal.setId(1L);
        certificadoOriginal.setNombreTomadoDelUsuario(Boolean.FALSE);

        CertificadoDigital certificado = certificadoValido(DNI_SIN_USUARIO);
        certificado.setEnabled(Boolean.FALSE);
        certificado.setApellidos(APELLIDOS_ADMINISTRADOR);

        Optional<BusinessMessages> messages = service.validateUpdate(certificado, certificadoOriginal);

        assertMensajeUnico(messages, "nombre", MENSAJE_NOMBRE_OBLIGATORIO);
    }

    @Test
    void validateUpdate_originalEscritoPorElAdministradorYSinApellidos_devuelveMensajeLosApellidosSonObligatorios() {
        CertificadoDigital certificadoOriginal = certificadoValido(DNI_SIN_USUARIO);
        certificadoOriginal.setId(1L);
        certificadoOriginal.setNombreTomadoDelUsuario(Boolean.FALSE);

        CertificadoDigital certificado = certificadoValido(DNI_SIN_USUARIO);
        certificado.setEnabled(Boolean.FALSE);
        certificado.setNombre(NOMBRE_ADMINISTRADOR);

        Optional<BusinessMessages> messages = service.validateUpdate(certificado, certificadoOriginal);

        assertMensajeUnico(messages, "apellidos", MENSAJE_APELLIDOS_OBLIGATORIOS);
    }

    @Test
    void validateUpdate_originalConNombreTomadoDelUsuario_noExigeNombreNiApellidos() {
        CertificadoDigital certificadoOriginal = certificadoValido(DNI_CON_USUARIO);
        certificadoOriginal.setId(1L);
        certificadoOriginal.setNombreTomadoDelUsuario(Boolean.TRUE);

        CertificadoDigital certificado = certificadoValido(DNI_CON_USUARIO);
        certificado.setEnabled(Boolean.FALSE);

        Optional<BusinessMessages> messages = service.validateUpdate(certificado, certificadoOriginal);

        assertEquals(Optional.empty(), messages);
    }

    @Test
    void validateUpdate_flagDelClienteATrueSobreOriginalConFlagFalse_sigueExigiendoNombreYApellidos() {
        CertificadoDigital certificadoOriginal = certificadoValido(DNI_SIN_USUARIO);
        certificadoOriginal.setId(1L);
        certificadoOriginal.setEnabled(Boolean.FALSE);
        certificadoOriginal.setNombreTomadoDelUsuario(Boolean.FALSE);

        CertificadoDigital certificado = certificadoValido(DNI_SIN_USUARIO);
        certificado.setEnabled(Boolean.FALSE);
        certificado.setNombreTomadoDelUsuario(Boolean.TRUE);

        Optional<BusinessMessages> messages = service.validateUpdate(certificado, certificadoOriginal);

        assertContieneMensaje(messages, "nombre", MENSAJE_NOMBRE_OBLIGATORIO);
        assertContieneMensaje(messages, "apellidos", MENSAJE_APELLIDOS_OBLIGATORIOS);
    }

    @Test
    void validateUpdate_originalSinFlagAsignado_exigeNombreYApellidos() {
        CertificadoDigital certificadoOriginal = certificadoValido(DNI_SIN_USUARIO);
        certificadoOriginal.setId(1L);
        certificadoOriginal.setEnabled(Boolean.FALSE);

        CertificadoDigital certificado = certificadoValido(DNI_SIN_USUARIO);
        certificado.setEnabled(Boolean.FALSE);

        Optional<BusinessMessages> messages = service.validateUpdate(certificado, certificadoOriginal);

        assertContieneMensaje(messages, "nombre", MENSAJE_NOMBRE_OBLIGATORIO);
        assertContieneMensaje(messages, "apellidos", MENSAJE_APELLIDOS_OBLIGATORIOS);
    }

    @Test
    void validateUpdate_consultaLosHabilitadosConElDniDelOriginalNoConElDelBeanEntrante() {
        CertificadoDigital certificadoOriginal = certificadoValido(DNI_CON_USUARIO);
        certificadoOriginal.setId(1L);
        certificadoOriginal.setNombreTomadoDelUsuario(Boolean.TRUE);

        CertificadoDigital certificado = certificadoValido(DNI_SIN_USUARIO);
        certificado.setId(1L);
        certificado.setEnabled(Boolean.TRUE);
        stubCertificadosHabilitados(DNI_CON_USUARIO, List.of());

        service.validateUpdate(certificado, certificadoOriginal);

        verify(repository).findByDniHabilitados(DNI_CON_USUARIO);
        verify(repository, never()).findByDniHabilitados(DNI_SIN_USUARIO);
    }

    @Test
    void validateUpdate_habilitandoCuandoOtroDelMismoDniYaEstaHabilitado_devuelveMensajeDeCertificadoHabilitadoDuplicado() {
        CertificadoDigital certificadoOriginal = certificadoValido(DNI_CON_USUARIO);
        certificadoOriginal.setId(2L);
        certificadoOriginal.setEnabled(Boolean.FALSE);
        certificadoOriginal.setNombreTomadoDelUsuario(Boolean.TRUE);

        CertificadoDigital certificado = certificadoValido(DNI_CON_USUARIO);
        certificado.setId(2L);
        certificado.setEnabled(Boolean.TRUE);
        stubCertificadosHabilitados(DNI_CON_USUARIO, List.of(certificadoConId(1L)));

        Optional<BusinessMessages> messages = service.validateUpdate(certificado, certificadoOriginal);

        assertMensajeUnico(messages, "enabled", MENSAJE_HABILITADO_DUPLICADO + DNI_CON_USUARIO);
    }

    @Test
    void validateUpdate_habilitadoYElUnicoHabilitadoEsElPropioRegistro_devuelveOptionalVacio() {
        CertificadoDigital certificadoOriginal = certificadoValido(DNI_CON_USUARIO);
        certificadoOriginal.setId(1L);
        certificadoOriginal.setEnabled(Boolean.TRUE);
        certificadoOriginal.setNombreTomadoDelUsuario(Boolean.TRUE);

        CertificadoDigital certificado = certificadoValido(DNI_CON_USUARIO);
        certificado.setId(1L);
        certificado.setEnabled(Boolean.TRUE);
        stubCertificadosHabilitados(DNI_CON_USUARIO, List.of(certificadoConId(1L)));

        Optional<BusinessMessages> messages = service.validateUpdate(certificado, certificadoOriginal);

        assertEquals(Optional.empty(), messages);
    }

    @Test
    void validateUpdate_deshabilitando_noConsultaLosHabilitadosDelDni() {
        CertificadoDigital certificadoOriginal = certificadoValido(DNI_CON_USUARIO);
        certificadoOriginal.setId(1L);
        certificadoOriginal.setEnabled(Boolean.TRUE);
        certificadoOriginal.setNombreTomadoDelUsuario(Boolean.TRUE);

        CertificadoDigital certificado = certificadoValido(DNI_CON_USUARIO);
        certificado.setId(1L);
        certificado.setEnabled(Boolean.FALSE);

        Optional<BusinessMessages> messages = service.validateUpdate(certificado, certificadoOriginal);

        assertEquals(Optional.empty(), messages);
        verify(repository, never()).findByDniHabilitados(any());
    }

    /* ------------------------------------------------------------------ */
    /* validateGetDatosTitularByDni                                       */
    /* ------------------------------------------------------------------ */

    @Test
    void validateGetDatosTitularByDni_dniValido_devuelveOptionalVacio() {
        Optional<BusinessMessages> messages = service.validateGetDatosTitularByDni(DNI_CON_USUARIO);

        assertEquals(Optional.empty(), messages);
    }

    @Test
    void validateGetDatosTitularByDni_dniNuloEnBlancoOConFormatoInvalido_devuelveOptionalVacio() {
        assertEquals(Optional.empty(), service.validateGetDatosTitularByDni(null));
        assertEquals(Optional.empty(), service.validateGetDatosTitularByDni("   "));
        assertEquals(Optional.empty(), service.validateGetDatosTitularByDni(DNI_A_MEDIO_TECLEAR));
    }

    @Test
    void validateGetDatosTitularByDni_noConsultaNingunRepositorio() {
        service.validateGetDatosTitularByDni(DNI_INVALIDO);

        verifyNoInteractions(repository, userRepository);
    }

    /* ------------------------------------------------------------------ */
    /* getDatosTitularByDni                                               */
    /* ------------------------------------------------------------------ */

    @Test
    void getDatosTitularByDni_dniDeUsuarioExistente_devuelveNombreApellidosYTomadoDelUsuarioTrue() {
        when(userRepository.findByDni(DNI_CON_USUARIO)).thenReturn(usuario(NOMBRE_TITULAR, APELLIDOS_TITULAR));

        DatosTitular datosTitular = service.getDatosTitularByDni(DNI_CON_USUARIO);

        assertEquals(NOMBRE_TITULAR, datosTitular.nombre());
        assertEquals(APELLIDOS_TITULAR, datosTitular.apellidos());
        assertTrue(datosTitular.tomadoDelUsuario());
    }

    @Test
    void getDatosTitularByDni_dniSinUsuario_devuelveDatosTitularSinUsuario() {
        when(userRepository.findByDni(DNI_SIN_USUARIO)).thenReturn(null);

        DatosTitular datosTitular = service.getDatosTitularByDni(DNI_SIN_USUARIO);

        assertNull(datosTitular.nombre());
        assertNull(datosTitular.apellidos());
        assertFalse(datosTitular.tomadoDelUsuario());
    }

    @Test
    void getDatosTitularByDni_dniNulo_devuelveDatosTitularSinUsuarioYNoConsultaElRepositorio() {
        DatosTitular datosTitular = service.getDatosTitularByDni(null);

        assertEquals(DatosTitular.sinUsuario(), datosTitular);
        verify(userRepository, never()).findByDni(any());
    }

    @Test
    void getDatosTitularByDni_dniEnBlanco_devuelveDatosTitularSinUsuarioYNoConsultaElRepositorio() {
        DatosTitular datosTitular = service.getDatosTitularByDni("   ");

        assertEquals(DatosTitular.sinUsuario(), datosTitular);
        verify(userRepository, never()).findByDni(any());
    }

    @Test
    void getDatosTitularByDni_dniIncompletoOConLetraIncorrecta_noLanzaYDevuelveSinUsuario() {
        when(userRepository.findByDni(DNI_A_MEDIO_TECLEAR)).thenReturn(null);

        DatosTitular datosTitular = service.getDatosTitularByDni(DNI_A_MEDIO_TECLEAR);

        assertEquals(DatosTitular.sinUsuario(), datosTitular);
    }

    @Test
    void getDatosTitularByDni_cualquierDni_noPersisteNada() {
        when(userRepository.findByDni(DNI_CON_USUARIO)).thenReturn(usuario(NOMBRE_TITULAR, APELLIDOS_TITULAR));

        service.getDatosTitularByDni(DNI_CON_USUARIO);

        verify(repository, never()).save(any());
        verify(repository, never()).remove(any());
    }

    @Test
    void getDatosTitularByDni_yInsert_conElMismoDni_resuelvenElMismoTitular() {
        CertificadoDigital certificado = certificadoValido(DNI_CON_USUARIO);
        certificado.setEnabled(Boolean.TRUE);
        when(userRepository.findByDni(DNI_CON_USUARIO)).thenReturn(usuario(NOMBRE_TITULAR, APELLIDOS_TITULAR));
        stubCertificadosHabilitados(DNI_CON_USUARIO, List.of());
        when(repository.save(certificado)).thenReturn(certificado);

        DatosTitular datosTitular = service.getDatosTitularByDni(DNI_CON_USUARIO);
        service.insert(certificado);

        assertEquals(datosTitular.nombre(), certificado.getNombre());
        assertEquals(datosTitular.apellidos(), certificado.getApellidos());
        assertEquals(datosTitular.tomadoDelUsuario(), certificado.getNombreTomadoDelUsuario());
    }

    /* ------------------------------------------------------------------ */
    /* getAlmacenClaveByDni                                               */
    /* ------------------------------------------------------------------ */

    @Test
    void getAlmacenClaveByDni_entradaDeshabilitada_devuelveNullIgualQueSiNoExistiera() {
        stubCertificadoHabilitado(DNI, null);

        assertNull(service.getAlmacenClaveByDni(DNI));
    }

    @Test
    void getAlmacenClaveByDni_entradaInexistente_devuelveNull() {
        stubCertificadoHabilitado(DNI, null);

        assertNull(service.getAlmacenClaveByDni(DNI));
    }

    @Test
    void getAlmacenClaveByDni_entradaHabilitada_devuelveAlmacenClave() {
        CertificadoDigital certificado = certificadoDispositivoPkcs11();
        certificado.setEnabled(Boolean.TRUE);
        stubCertificadoHabilitado(DNI, certificado);

        AlmacenClave almacenClave = service.getAlmacenClaveByDni(DNI);

        assertNotNull(almacenClave);
        assertInstanceOf(AlmacenClaveDispositivo.class, almacenClave);
    }

    @Test
    void getAlmacenClaveByDni_entradaConEnabledPorDefecto_devuelveAlmacenClave() {
        CertificadoDigital certificado = certificadoDispositivoPkcs11();
        stubCertificadoHabilitado(DNI, certificado);

        AlmacenClave almacenClave = service.getAlmacenClaveByDni(DNI);

        assertNotNull(almacenClave);
        assertInstanceOf(AlmacenClaveDispositivo.class, almacenClave);
    }

    /* ------------------------------------------------------------------ */
    /* getAlmacenClaveByDni(dni, claveAcceso)                             */
    /* ------------------------------------------------------------------ */

    @Test
    void getAlmacenClaveByDni_dniConCertificadoHabilitado_devuelveElAlmacenDeEseCertificado() throws IOException {
        stubCertificadoHabilitado(DNI, certificadoClasspath(CLAVE));

        AlmacenClave almacenClave = service.getAlmacenClaveByDni(DNI, null);

        assertAlmacenFicheroConClave(almacenClave, CLAVE);
    }

    @Test
    void getAlmacenClaveByDni_sinNingunCertificadoHabilitadoParaEseDni_devuelveNull() {
        stubCertificadoHabilitado(DNI, null);

        assertNull(service.getAlmacenClaveByDni(DNI, null));
    }

    @Test
    void getAlmacenClaveByDni_variosCertificadosDelMismoDni_usaSoloElHabilitado() throws IOException {
        CertificadoDigital certificadoHabilitado = new CertificadoDigital();
        certificadoHabilitado.setDni(DNI_CON_USUARIO);
        certificadoHabilitado.setTipoCertificado(TipoUbicacionCertificado.CLASSPATH);
        certificadoHabilitado.setRutaClasspath(RUTA_CLASSPATH_CERTIFICADO_SECRETARIO);
        certificadoHabilitado.setPassword(CLAVE);
        certificadoHabilitado.setEnabled(Boolean.TRUE);
        stubCertificadoHabilitado(DNI_CON_USUARIO, certificadoHabilitado);

        AlmacenClave almacenClave = service.getAlmacenClaveByDni(DNI_CON_USUARIO, null);

        AlmacenClaveFichero almacenClaveFichero = assertInstanceOf(AlmacenClaveFichero.class, almacenClave);
        try (InputStream contenidoCertificado = almacenClaveFichero.getFileCertificate()) {
            assertArrayEquals(contenidoDelRecursoDeClasspath(RUTA_CLASSPATH_CERTIFICADO_SECRETARIO),
                    contenidoCertificado.readAllBytes());
        }
        assertEquals(CLAVE, almacenClaveFichero.getPassword());
        verify(repository).findByDniHabilitados(DNI_CON_USUARIO);
    }

    @Test
    void getAlmacenClaveByDni_ficheroConClaveGuardada_usaLaGuardadaEIgnoraLaTecleada() throws IOException {
        stubCertificadoHabilitado(DNI, certificadoClasspath(CLAVE));

        AlmacenClave almacenClave = service.getAlmacenClaveByDni(DNI, CLAVE_TECLEADA_DISTINTA);

        assertAlmacenFicheroConClave(almacenClave, CLAVE);
    }

    @Test
    void getAlmacenClaveByDni_ficheroSinClaveGuardada_usaLaClaveTecleada() throws IOException {
        stubCertificadoHabilitado(DNI, certificadoClasspath(null));

        AlmacenClave almacenClave = service.getAlmacenClaveByDni(DNI, CLAVE);

        assertAlmacenFicheroConClave(almacenClave, CLAVE);
    }

    @Test
    void getAlmacenClaveByDni_ficheroConClaveGuardadaEnBlanco_usaLaClaveTecleada() throws IOException {
        stubCertificadoHabilitado(DNI, certificadoClasspath(CLAVE_EN_BLANCO));

        AlmacenClave almacenClave = service.getAlmacenClaveByDni(DNI, CLAVE);

        assertAlmacenFicheroConClave(almacenClave, CLAVE);
    }

    @Test
    void getAlmacenClaveByDni_ficheroSinClaveGuardadaNiTecleada_lanzaExcepcion() {
        stubCertificadoHabilitado(DNI, certificadoClasspath(null));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.getAlmacenClaveByDni(DNI, null));

        assertEquals(MENSAJE_PASSWORD_NULL, ex.getMessage());
    }

    @Test
    void getAlmacenClaveByDni_ficheroEnBaseDeDatos_descargaElContenidoYUsaLaClaveEfectiva() throws IOException {
        MetaFile fichero = new MetaFile();
        stubCertificadoHabilitado(DNI, certificadoFicheroBd(fichero, null));

        try (MockedStatic<MetaFileUtil> metaFileUtil = Mockito.mockStatic(MetaFileUtil.class)) {
            metaFileUtil.when(() -> MetaFileUtil.downloadContent(fichero)).thenReturn(new byte[]{1, 2, 3});

            AlmacenClave almacenClave = service.getAlmacenClaveByDni(DNI, CLAVE);

            assertAlmacenFicheroConClave(almacenClave, CLAVE);
            metaFileUtil.verify(() -> MetaFileUtil.downloadContent(fichero));
        }
    }

    @Test
    void getAlmacenClaveByDni_sistemaDeArchivos_usaLaClaveEfectiva(@TempDir Path carpetaTemporal) throws IOException {
        Path rutaCertificado = Files.write(carpetaTemporal.resolve("certificado.p12"), new byte[]{1, 2, 3});
        stubCertificadoHabilitado(DNI, certificadoSistemaArchivos(rutaCertificado, null));

        AlmacenClave almacenClave = service.getAlmacenClaveByDni(DNI, CLAVE);

        assertAlmacenFicheroConClave(almacenClave, CLAVE);
    }

    @Test
    void getAlmacenClaveByDni_dispositivoPkcs11_descartaLaClaveTecleada() {
        CertificadoDigital certificado = certificadoDispositivoPkcs11();
        certificado.setEnabled(Boolean.TRUE);
        stubCertificadoHabilitado(DNI, certificado);

        AlmacenClave almacenClave = service.getAlmacenClaveByDni(DNI, "pinTecleado");

        AlmacenClaveDispositivo almacenClaveDispositivo = assertInstanceOf(AlmacenClaveDispositivo.class, almacenClave);
        assertEquals(1, almacenClaveDispositivo.getSlot());
        assertEquals("certificado-centro", almacenClaveDispositivo.getAlias());
    }

    @Test
    void getAlmacenClaveByDni_sinCertificadoParaElDni_devuelveNull() {
        stubCertificadoHabilitado(DNI, null);

        assertNull(service.getAlmacenClaveByDni(DNI, CLAVE));
    }

    @Test
    void getAlmacenClaveByDni_certificadoDeshabilitado_devuelveNull() {
        stubCertificadoHabilitado(DNI, null);

        assertNull(service.getAlmacenClaveByDni(DNI, CLAVE));
    }

    @Test
    void getAlmacenClaveByDni_dniInvalido_lanzaValidationExceptionYNoConsultaElRepositorio() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.getAlmacenClaveByDni(DNI_INVALIDO, CLAVE));

        assertTrue(ex.getMessage().contains(MENSAJE_DNI_NO_VALIDO),
                () -> "El mensaje de la excepción no contiene «" + MENSAJE_DNI_NO_VALIDO + "»: " + ex.getMessage());
        verify(repository, never()).findByDniHabilitados(any());
    }

    /* ------------------------------------------------------------------ */
    /* getAlmacenClaveByDni(dni) — delegación                             */
    /* ------------------------------------------------------------------ */

    @Test
    void getAlmacenClaveByDniUnArgumento_certificadoConClaveGuardada_devuelveElMismoResultadoQueAntes() throws IOException {
        stubCertificadoHabilitado(DNI, certificadoClasspath(CLAVE));

        AlmacenClave almacenClave = service.getAlmacenClaveByDni(DNI);

        assertAlmacenFicheroConClave(almacenClave, CLAVE);
    }

    @Test
    void getAlmacenClaveByDniUnArgumento_dispositivoPkcs11Habilitado_devuelveAlmacenClaveDispositivo() {
        CertificadoDigital certificado = certificadoDispositivoPkcs11();
        certificado.setEnabled(Boolean.TRUE);
        stubCertificadoHabilitado(DNI, certificado);

        AlmacenClave almacenClave = service.getAlmacenClaveByDni(DNI);

        assertInstanceOf(AlmacenClaveDispositivo.class, almacenClave);
    }

    @Test
    void getAlmacenClaveByDniUnArgumento_certificadoSinClaveGuardada_lanzaExcepcion() {
        stubCertificadoHabilitado(DNI, certificadoClasspath(null));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.getAlmacenClaveByDni(DNI));

        assertEquals(MENSAJE_PASSWORD_NULL, ex.getMessage());
    }

    /* ------------------------------------------------------------------ */
    /* getSituacionFirmaByDni                                             */
    /* ------------------------------------------------------------------ */

    @Test
    void getSituacionFirmaByDni_sinCertificadoHabilitadoParaEseDni_devuelveSinCertificado() {
        stubCertificadoHabilitado(DNI, null);

        assertEquals(SituacionFirma.SIN_CERTIFICADO, service.getSituacionFirmaByDni(DNI));
    }

    @Test
    void getSituacionFirmaByDni_certificadoHabilitadoDeFicheroConClave_devuelveFicheroConClave() {
        stubCertificadoHabilitado(DNI, certificadoClasspath(CLAVE));

        assertEquals(SituacionFirma.FICHERO_CON_CLAVE, service.getSituacionFirmaByDni(DNI));
    }

    @Test
    void getSituacionFirmaByDni_certificadoHabilitadoEnDispositivoSinPin_devuelveDispositivoSinPin() {
        CertificadoDigital certificado = certificadoDispositivoPkcs11();
        certificado.setEnabled(Boolean.TRUE);
        stubCertificadoHabilitado(DNI, certificado);

        assertEquals(SituacionFirma.DISPOSITIVO_SIN_PIN, service.getSituacionFirmaByDni(DNI));
    }

    @Test
    void getSituacionFirmaByDni_dniNuloOEnBlanco_devuelveSinDni() {
        assertEquals(SituacionFirma.SIN_DNI, service.getSituacionFirmaByDni(null));
        assertEquals(SituacionFirma.SIN_DNI, service.getSituacionFirmaByDni("   "));
        verify(repository, never()).findByDniHabilitados(any());
    }

    @Test
    void getSituacionFirmaByDni_dniConFormatoInvalido_lanzaIllegalArgumentExceptionConElDniEnmascarado() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.getSituacionFirmaByDni(DNI_INVALIDO));

        assertTrue(ex.getMessage().startsWith(MENSAJE_DNI_NO_VALIDO + ": "),
                () -> "El mensaje no empieza por «" + MENSAJE_DNI_NO_VALIDO + ": »: " + ex.getMessage());
        assertFalse(ex.getMessage().contains(DNI_INVALIDO),
                () -> "El mensaje incluye el DNI completo sin enmascarar: " + ex.getMessage());
    }

    /* ------------------------------------------------------------------ */
    /* allowPropertiesInsert                                              */
    /* ------------------------------------------------------------------ */

    @Test
    void allowPropertiesInsert_permiteLosOnceCamposDeLaAccionCrear() {
        AllowProperties allowProperties = service.allowPropertiesInsert();

        assertTrue(allowProperties.allowProperty("dni"));
        assertTrue(allowProperties.allowProperty("nombre"));
        assertTrue(allowProperties.allowProperty("apellidos"));
        assertTrue(allowProperties.allowProperty("tipoCertificado"));
        assertTrue(allowProperties.allowProperty("fichero"));
        assertTrue(allowProperties.allowProperty("password"));
        assertTrue(allowProperties.allowProperty("dispositivoCriptografico"));
        assertTrue(allowProperties.allowProperty("alias"));
        assertTrue(allowProperties.allowProperty("rutaClasspath"));
        assertTrue(allowProperties.allowProperty("rutaSistemaArchivos"));
        assertTrue(allowProperties.allowProperty("enabled"));
    }

    @Test
    void allowPropertiesInsert_deniegaNombreTomadoDelUsuario() {
        AllowProperties allowProperties = service.allowPropertiesInsert();

        assertFalse(allowProperties.allowProperty("nombreTomadoDelUsuario"));
    }

    /* ------------------------------------------------------------------ */
    /* allowPropertiesUpdate                                              */
    /* ------------------------------------------------------------------ */

    @Test
    void allowPropertiesUpdate_permiteLosDiezCamposDeLaAccionModificar() {
        AllowProperties allowProperties = service.allowPropertiesUpdate();

        assertTrue(allowProperties.allowProperty("nombre"));
        assertTrue(allowProperties.allowProperty("apellidos"));
        assertTrue(allowProperties.allowProperty("tipoCertificado"));
        assertTrue(allowProperties.allowProperty("fichero"));
        assertTrue(allowProperties.allowProperty("password"));
        assertTrue(allowProperties.allowProperty("dispositivoCriptografico"));
        assertTrue(allowProperties.allowProperty("alias"));
        assertTrue(allowProperties.allowProperty("rutaClasspath"));
        assertTrue(allowProperties.allowProperty("rutaSistemaArchivos"));
        assertTrue(allowProperties.allowProperty("enabled"));
    }

    @Test
    void allowPropertiesUpdate_deniegaDniYNombreTomadoDelUsuario() {
        AllowProperties allowProperties = service.allowPropertiesUpdate();

        assertFalse(allowProperties.allowProperty("dni"));
        assertFalse(allowProperties.allowProperty("nombreTomadoDelUsuario"));
    }

    /* ------------------------------------------------------------------ */
    /* validateGetAlmacenClaveByDni(dni, claveAcceso)                     */
    /* ------------------------------------------------------------------ */

    @Test
    void validateGetAlmacenClaveByDni_dniValidoYClaveTecleada_devuelveOptionalVacio() {
        Optional<BusinessMessages> messages = service.validateGetAlmacenClaveByDni(DNI, CLAVE);

        assertEquals(Optional.empty(), messages);
    }

    @Test
    void validateGetAlmacenClaveByDni_claveAccesoNula_devuelveOptionalVacio() {
        Optional<BusinessMessages> messages = service.validateGetAlmacenClaveByDni(DNI, null);

        assertEquals(Optional.empty(), messages);
    }

    @Test
    void validateGetAlmacenClaveByDni_dniInvalido_devuelveMensajeElDniNoEsValido() {
        Optional<BusinessMessages> messages = service.validateGetAlmacenClaveByDni(DNI_INVALIDO, CLAVE);

        assertTrue(messages.isPresent(), "Se esperaba al menos un mensaje de validación para un DNI inválido");
        assertTrue(messages.get().stream()
                        .anyMatch(message -> "dni".equals(message.getFieldName())
                                && MENSAJE_DNI_NO_VALIDO.equals(message.getMessage())),
                () -> "No hay ningún mensaje del campo «dni» con el texto «" + MENSAJE_DNI_NO_VALIDO
                        + "». Mensajes obtenidos: " + messages.get());
    }

    @Test
    void validateGetAlmacenClaveByDni_dniInvalido_noIncluyeLaClaveEnNingunMensaje() {
        String primeraMitadDeLaClave = CLAVE_SECRETA.substring(0, CLAVE_SECRETA.length() / 2);
        String segundaMitadDeLaClave = CLAVE_SECRETA.substring(CLAVE_SECRETA.length() / 2);

        Optional<BusinessMessages> messages = service.validateGetAlmacenClaveByDni(DNI_INVALIDO, CLAVE_SECRETA);

        assertTrue(messages.isPresent(), "Se esperaba al menos un mensaje de validación para un DNI inválido");
        String textoMensajes = messages.get().toString();
        assertFalse(textoMensajes.contains(CLAVE_SECRETA),
                () -> "Los mensajes de validación filtran la clave completa: " + textoMensajes);
        assertFalse(textoMensajes.contains(primeraMitadDeLaClave),
                () -> "Los mensajes de validación filtran «" + primeraMitadDeLaClave + "»: " + textoMensajes);
        assertFalse(textoMensajes.contains(segundaMitadDeLaClave),
                () -> "Los mensajes de validación filtran «" + segundaMitadDeLaClave + "»: " + textoMensajes);
    }
}
