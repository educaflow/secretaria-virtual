package com.educaflow.subsystem.correos.service.impl;

import com.axelor.auth.db.User;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.i18n.I18n;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.correos.db.Adjunto;
import com.educaflow.subsystem.correos.db.Correo;
import com.educaflow.subsystem.correos.db.repo.AdjuntoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class AdjuntoServiceImplTest {

    private AdjuntoRepository repository;
    private AdjuntoServiceImpl service;

    private Centro centroA;
    private Centro centroB;

    private MockedStatic<I18n> i18nMock;
    private MockedStatic<SecurityUtil> securityUtilMock;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(AdjuntoRepository.class);
        service = new AdjuntoServiceImpl(Adjunto.class, repository);

        centroA = new Centro();
        centroA.setId(1L);
        centroB = new Centro();
        centroB.setId(2L);

        // lenient: I18n se programa en el setup pero no todos los tests recorren las ramas que lo
        // consumen (happy paths que no producen mensaje).
        i18nMock = Mockito.mockStatic(I18n.class,
                Mockito.withSettings().strictness(Strictness.LENIENT));
        i18nMock.when(() -> I18n.get(any(String.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // lenient: no todos los tests estiman SecurityUtil (p.ej. validateUpdate/validateRemove).
        securityUtilMock = Mockito.mockStatic(SecurityUtil.class,
                Mockito.withSettings().strictness(Strictness.LENIENT));
    }

    @AfterEach
    void tearDown() {
        i18nMock.close();
        securityUtilMock.close();
    }

    /* ------------------------------------------------------------------ */
    /* Helpers                                                            */
    /* ------------------------------------------------------------------ */

    private String mensaje(Optional<BusinessMessages> optional) {
        assertTrue(optional.isPresent());
        return optional.get().get(0).getMessage();
    }

    private void stubIsAdmin(boolean esAdmin) {
        securityUtilMock.when(() -> SecurityUtil.isAdmin(any())).thenReturn(esAdmin);
    }

    private CentroUsuario centroUsuario(Centro centro) {
        CentroUsuario centroUsuario = new CentroUsuario();
        centroUsuario.setCentro(centro);
        return centroUsuario;
    }

    private User usuarioDeCentro(Centro centro) {
        User user = new User();
        user.setCentroUsuarios(List.of(centroUsuario(centro)));
        return user;
    }

    private Correo correoEnCreacion(Centro centro) {
        Correo correo = new Correo();
        correo.setCentro(centro);
        correo.setFechaCreacion(null);
        return correo;
    }

    private Adjunto adjuntoValido() {
        Correo correo = correoEnCreacion(centroA);
        Adjunto adjunto = new Adjunto();
        adjunto.setCorreo(correo);
        adjunto.setNombreFichero("doc.pdf");
        adjunto.setContenido(Mockito.mock(MetaFile.class));
        correo.setAdjuntos(List.of(adjunto));
        return adjunto;
    }

    /* ------------------------------------------------------------------ */
    /* update                                                             */
    /* ------------------------------------------------------------------ */

    @Test
    void update_siempre_lanzaUnsupportedOperationException() {
        Adjunto nuevo = adjuntoValido();
        Adjunto original = adjuntoValido();

        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                () -> service.update(nuevo, original));

        assertEquals("El adjunto es inmutable tras su creación.", ex.getMessage());
    }

    /* ------------------------------------------------------------------ */
    /* remove                                                             */
    /* ------------------------------------------------------------------ */

    @Test
    void remove_siempre_lanzaUnsupportedOperationException() {
        Adjunto adjunto = adjuntoValido();

        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                () -> service.remove(adjunto));

        assertEquals("Los adjuntos no se pueden borrar.", ex.getMessage());
    }

    /* ------------------------------------------------------------------ */
    /* validateInsert                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    void validateInsert_todoValido_devuelveOptionalVacio() {
        Adjunto adjunto = adjuntoValido();
        stubIsAdmin(true);

        Optional<BusinessMessages> resultado = service.validateInsert(adjunto);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateInsert_correoNulo_devuelveMensajeDebePertenecerAUnCorreo() {
        Adjunto adjunto = adjuntoValido();
        adjunto.setCorreo(null);
        stubIsAdmin(true);

        Optional<BusinessMessages> resultado = service.validateInsert(adjunto);

        assertEquals("El adjunto debe pertenecer a un correo", mensaje(resultado));
    }

    @Test
    void validateInsert_usuarioNoAdminDeOtroCentro_devuelveMensajeCentroNoSuyo() {
        Adjunto adjunto = adjuntoValido();
        adjunto.getCorreo().setCentro(centroB);
        stubIsAdmin(false);
        securityUtilMock.when(SecurityUtil::getUser).thenReturn(usuarioDeCentro(centroA));

        Optional<BusinessMessages> resultado = service.validateInsert(adjunto);

        assertEquals("No puede añadir adjuntos a correos de un centro que no es suyo", mensaje(resultado));
    }

    @Test
    void validateInsert_correoYaExistente_devuelveMensajeNoSePuedenAnadirAUnoExistente() {
        Adjunto adjunto = adjuntoValido();
        adjunto.getCorreo().setFechaCreacion(LocalDateTime.of(2020, 1, 1, 0, 0));
        stubIsAdmin(true);

        Optional<BusinessMessages> resultado = service.validateInsert(adjunto);

        assertEquals("No se pueden añadir adjuntos a un correo ya existente", mensaje(resultado));
    }

    @Test
    void validateInsert_correoEnCreacion_esValido() {
        Adjunto adjunto = adjuntoValido();
        adjunto.getCorreo().setFechaCreacion(null);
        stubIsAdmin(true);

        Optional<BusinessMessages> resultado = service.validateInsert(adjunto);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void validateInsert_nombreFicheroNulo_devuelveMensajeObligatorio() {
        Adjunto adjunto = adjuntoValido();
        adjunto.setNombreFichero(null);
        stubIsAdmin(true);

        Optional<BusinessMessages> resultado = service.validateInsert(adjunto);

        assertEquals("El nombre del fichero es obligatorio", mensaje(resultado));
    }

    @Test
    void validateInsert_contenidoNulo_devuelveMensajeDebeAdjuntarFichero() {
        Adjunto adjunto = adjuntoValido();
        adjunto.setContenido(null);
        stubIsAdmin(true);

        Optional<BusinessMessages> resultado = service.validateInsert(adjunto);

        assertEquals("El contenido del adjunto es obligatorio", mensaje(resultado));
    }

    @Test
    void validateInsert_nombreFicheroDuplicadoEntreHermanos_devuelveMensajeYaExiste() {
        Adjunto adjunto = adjuntoValido();
        Adjunto hermano = new Adjunto();
        hermano.setNombreFichero("doc.pdf");
        adjunto.getCorreo().setAdjuntos(List.of(hermano, adjunto));
        stubIsAdmin(true);

        Optional<BusinessMessages> resultado = service.validateInsert(adjunto);

        assertEquals("Ya existe un adjunto con ese nombre en el correo", mensaje(resultado));
    }

    @Test
    void validateInsert_nombreFicheroUnicoEntreHermanos_esValido() {
        Adjunto adjunto = adjuntoValido();
        Adjunto hermano = new Adjunto();
        hermano.setNombreFichero("otro.pdf");
        adjunto.getCorreo().setAdjuntos(List.of(hermano, adjunto));
        stubIsAdmin(true);

        Optional<BusinessMessages> resultado = service.validateInsert(adjunto);

        assertTrue(resultado.isEmpty());
    }

    /* ------------------------------------------------------------------ */
    /* validateUpdate                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    void validateUpdate_siempre_devuelveMensajeInmutabilidad() {
        Adjunto nuevo = adjuntoValido();
        Adjunto original = adjuntoValido();

        Optional<BusinessMessages> resultado = service.validateUpdate(nuevo, original);

        assertEquals("El adjunto es inmutable tras su creación.", mensaje(resultado));
    }

    /* ------------------------------------------------------------------ */
    /* validateRemove                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    void validateRemove_siempre_devuelveMensajeNoSePuedenBorrar() {
        Adjunto adjunto = adjuntoValido();

        Optional<BusinessMessages> resultado = service.validateRemove(adjunto);

        assertEquals("Los adjuntos no se pueden borrar.", mensaje(resultado));
    }

    /* ------------------------------------------------------------------ */
    /* allowPropertiesInsert                                              */
    /* ------------------------------------------------------------------ */

    @Test
    void allowPropertiesInsert_permiteNombreFicheroContenidoYCorreo() {
        AllowProperties result = service.allowPropertiesInsert();

        assertTrue(result.allowProperty("nombreFichero"));
        assertTrue(result.allowProperty("contenido"));
        assertTrue(result.allowProperty("correo"));
    }
}
