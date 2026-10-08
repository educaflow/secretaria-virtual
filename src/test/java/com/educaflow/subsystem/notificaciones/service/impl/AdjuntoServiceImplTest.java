package com.educaflow.subsystem.notificaciones.service.impl;

import com.axelor.auth.db.User;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.i18n.I18n;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.notificaciones.db.Adjunto;
import com.educaflow.subsystem.notificaciones.db.Correo;
import com.educaflow.subsystem.notificaciones.db.repo.AdjuntoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdjuntoServiceImplTest {

    private static final long MB = 1024L * 1024;

    private static final String MENSAJE_INMUTABLE = "El adjunto es inmutable tras su creación.";
    private static final String MENSAJE_NO_SE_BORRAN = "Los adjuntos no se pueden borrar.";
    private static final String MENSAJE_DEBE_PERTENECER_A_CORREO = "El adjunto debe pertenecer a un correo";
    private static final String MENSAJE_CENTRO_AJENO = "No puede añadir adjuntos a correos de un centro que no es suyo";
    private static final String MENSAJE_CORREO_EXISTENTE = "No se pueden añadir adjuntos a un correo ya existente";
    private static final String MENSAJE_NOMBRE_OBLIGATORIO = "El nombre del fichero es obligatorio";
    private static final String MENSAJE_CARACTERES_PROHIBIDOS =
            "El nombre del fichero no puede contener los caracteres / \\ ni caracteres de control";
    private static final String MENSAJE_LONGITUD_NOMBRE = "El nombre del fichero no puede superar 255 caracteres";
    private static final String MENSAJE_NOMBRE_NO_PERMITIDO = "El nombre del fichero no puede ser «.» ni «..»";
    private static final String MENSAJE_CONTENIDO_OBLIGATORIO = "El contenido del adjunto es obligatorio";
    private static final String MENSAJE_TAMANO_MAXIMO = "El adjunto no puede superar los 10 MB";
    private static final String MENSAJE_VACIO = "El fichero adjunto está vacío";
    private static final String MENSAJE_YA_EXISTE = "Ya existe un adjunto con ese nombre en el correo";

    private AdjuntoRepository adjuntoRepository;
    private AdjuntoServiceImpl service;

    private Centro centroA;
    private Centro centroB;

    private MockedStatic<I18n> i18nMock;
    private MockedStatic<SecurityUtil> securityUtilMock;
    private MockedStatic<MetaFileUtil> metaFileUtilMock;

    @BeforeEach
    void setUp() {
        adjuntoRepository = mock(AdjuntoRepository.class);
        service = new AdjuntoServiceImpl(Adjunto.class, adjuntoRepository);

        centroA = new Centro();
        centroA.setId(1L);
        centroB = new Centro();
        centroB.setId(2L);

        i18nMock = Mockito.mockStatic(I18n.class, Mockito.withSettings().strictness(Strictness.LENIENT));
        i18nMock.when(() -> I18n.get(any(String.class))).thenAnswer(invocation -> invocation.getArgument(0));

        securityUtilMock = Mockito.mockStatic(SecurityUtil.class, Mockito.withSettings().strictness(Strictness.LENIENT));
        securityUtilMock.when(SecurityUtil::getUser).thenReturn(usuarioDeCentro(centroA));
        stubIsAdmin(false);

        metaFileUtilMock = Mockito.mockStatic(MetaFileUtil.class, Mockito.withSettings().strictness(Strictness.LENIENT));
        stubTamanoEnDisco(1024L);
    }

    @AfterEach
    void tearDown() {
        i18nMock.close();
        securityUtilMock.close();
        metaFileUtilMock.close();
    }

    /*************************************** Helpers ***************************************/

    private void stubIsAdmin(boolean esAdmin) {
        securityUtilMock.when(() -> SecurityUtil.isAdmin(any())).thenReturn(esAdmin);
    }

    private void stubTamanoEnDisco(long bytes) {
        metaFileUtilMock.when(() -> MetaFileUtil.getSize(any())).thenReturn(bytes);
    }

    private User usuarioDeCentro(Centro centro) {
        CentroUsuario centroUsuario = new CentroUsuario();
        centroUsuario.setCentro(centro);
        User usuario = new User();
        usuario.setCentroUsuarios(new ArrayList<>(List.of(centroUsuario)));
        return usuario;
    }

    private Adjunto adjuntoValido() {
        Correo correo = new Correo();
        correo.setCentro(centroA);
        Adjunto adjunto = new Adjunto();
        adjunto.setNombreFichero("informe.pdf");
        adjunto.setContenido(new MetaFile());
        adjunto.setCorreo(correo);
        correo.setAdjuntos(new ArrayList<>(List.of(adjunto)));
        return adjunto;
    }

    private Adjunto hermano(String nombreFichero) {
        Adjunto hermano = new Adjunto();
        hermano.setNombreFichero(nombreFichero);
        hermano.setContenido(new MetaFile());
        return hermano;
    }

    private List<String> textos(Optional<BusinessMessages> resultado) {
        assertTrue(resultado.isPresent());
        return resultado.get().stream().map(BusinessMessage::getMessage).toList();
    }

    /*************************************** update ***************************************/

    @Test
    void update_siempre_lanzaUnsupportedOperationException() {
        Adjunto nuevo = adjuntoValido();
        Adjunto original = adjuntoValido();

        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                () -> service.update(nuevo, original));

        assertEquals(MENSAJE_INMUTABLE, ex.getMessage());
        verify(adjuntoRepository, never()).save(any());
    }

    /*************************************** remove ***************************************/

    @Test
    void remove_siempre_lanzaUnsupportedOperationException() {
        Adjunto adjunto = adjuntoValido();

        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                () -> service.remove(adjunto));

        assertEquals(MENSAJE_NO_SE_BORRAN, ex.getMessage());
        verify(adjuntoRepository, never()).remove(any());
    }

    /*************************************** validateInsert ***************************************/

    @Test
    void validateInsert_todoValido_devuelveOptionalVacio() {
        assertTrue(service.validateInsert(adjuntoValido()).isEmpty());
    }

    @Test
    void validateInsert_correoNulo_devuelveMensajeDebePertenecerAUnCorreo() {
        Adjunto adjunto = adjuntoValido();
        adjunto.setCorreo(null);

        List<String> mensajes = textos(service.validateInsert(adjunto));

        assertTrue(mensajes.contains(MENSAJE_DEBE_PERTENECER_A_CORREO));
        assertFalse(mensajes.contains(MENSAJE_CENTRO_AJENO));
        assertFalse(mensajes.contains(MENSAJE_CORREO_EXISTENTE));
        assertFalse(mensajes.contains(MENSAJE_YA_EXISTE));
    }

    @Test
    void validateInsert_noAdminCorreoDeOtroCentro_devuelveMensajeCentroAjeno() {
        Adjunto adjunto = adjuntoValido();
        adjunto.getCorreo().setCentro(centroB);

        assertTrue(textos(service.validateInsert(adjunto)).contains(MENSAJE_CENTRO_AJENO));
    }

    @Test
    void validateInsert_administradorCorreoDeOtroCentro_esValido() {
        Adjunto adjunto = adjuntoValido();
        adjunto.getCorreo().setCentro(centroB);
        stubIsAdmin(true);

        assertTrue(service.validateInsert(adjunto).isEmpty());
    }

    @Test
    void validateInsert_correoYaPersistido_devuelveMensajeCorreoExistente() {
        Adjunto adjunto = adjuntoValido();
        adjunto.getCorreo().setId(100L);

        assertTrue(textos(service.validateInsert(adjunto)).contains(MENSAJE_CORREO_EXISTENTE));
    }

    @Test
    void validateInsert_nombreFicheroSoloEspacios_devuelveMensajeObligatorio() {
        Adjunto adjunto = adjuntoValido();
        adjunto.setNombreFichero("  ");

        List<String> mensajes = textos(service.validateInsert(adjunto));

        assertTrue(mensajes.contains(MENSAJE_NOMBRE_OBLIGATORIO));
        assertFalse(mensajes.contains(MENSAJE_CARACTERES_PROHIBIDOS));
        assertFalse(mensajes.contains(MENSAJE_LONGITUD_NOMBRE));
        assertFalse(mensajes.contains(MENSAJE_NOMBRE_NO_PERMITIDO));
    }

    @Test
    void validateInsert_nombreFicheroConBarra_devuelveMensajeCaracteresProhibidos() {
        List.of("a/b.pdf", "a\\b.pdf", "a\nb.pdf").forEach(nombreFichero -> {
            Adjunto adjunto = adjuntoValido();
            adjunto.setNombreFichero(nombreFichero);

            assertTrue(textos(service.validateInsert(adjunto)).contains(MENSAJE_CARACTERES_PROHIBIDOS), nombreFichero);
        });
    }

    @Test
    void validateInsert_nombreFicheroConEspaciosYTildes_esValido() {
        Adjunto adjunto = adjuntoValido();
        adjunto.setNombreFichero("Solicitud de matrícula.pdf");

        assertTrue(service.validateInsert(adjunto).isEmpty());
    }

    @Test
    void validateInsert_nombreFicheroDe256Caracteres_devuelveMensajeLongitud() {
        Adjunto adjunto = adjuntoValido();
        adjunto.setNombreFichero("a".repeat(252) + ".pdf");

        assertTrue(textos(service.validateInsert(adjunto)).contains(MENSAJE_LONGITUD_NOMBRE));
    }

    @Test
    void validateInsert_nombreFicheroDe255Caracteres_esValido() {
        Adjunto adjunto = adjuntoValido();
        adjunto.setNombreFichero("a".repeat(251) + ".pdf");

        assertTrue(service.validateInsert(adjunto).isEmpty());
    }

    @Test
    void validateInsert_nombreFicheroPunto_devuelveMensajeNombreNoPermitido() {
        Adjunto adjunto = adjuntoValido();
        adjunto.setNombreFichero(".");

        assertTrue(textos(service.validateInsert(adjunto)).contains(MENSAJE_NOMBRE_NO_PERMITIDO));
    }

    @Test
    void validateInsert_nombreFicheroDosPuntosConEspacios_devuelveMensajeNombreNoPermitido() {
        Adjunto adjunto = adjuntoValido();
        adjunto.setNombreFichero(" .. ");

        assertTrue(textos(service.validateInsert(adjunto)).contains(MENSAJE_NOMBRE_NO_PERMITIDO));
    }

    @Test
    void validateInsert_contenidoNulo_devuelveMensajeObligatorioYNoMide() {
        Adjunto adjunto = adjuntoValido();
        adjunto.setContenido(null);

        assertTrue(textos(service.validateInsert(adjunto)).contains(MENSAJE_CONTENIDO_OBLIGATORIO));
        metaFileUtilMock.verify(() -> MetaFileUtil.getSize(any()), never());
    }

    @Test
    void validateInsert_contenidoDeMasDe10MB_devuelveMensajeTamanoMaximo() {
        stubTamanoEnDisco(AdjuntoServiceImpl.TAMANO_MAXIMO_BYTES + 1);

        assertTrue(textos(service.validateInsert(adjuntoValido())).contains(MENSAJE_TAMANO_MAXIMO));
    }

    @Test
    void validateInsert_contenidoDeExactamente10MB_esValido() {
        stubTamanoEnDisco(AdjuntoServiceImpl.TAMANO_MAXIMO_BYTES);

        assertTrue(service.validateInsert(adjuntoValido()).isEmpty());
    }

    @Test
    void validateInsert_fileSizeDelClienteMenorQueElDisco_seUsaElDisco() {
        Adjunto adjunto = adjuntoValido();
        adjunto.getContenido().setFileSize(1L);
        stubTamanoEnDisco(AdjuntoServiceImpl.TAMANO_MAXIMO_BYTES + MB);

        assertTrue(textos(service.validateInsert(adjunto)).contains(MENSAJE_TAMANO_MAXIMO));
    }

    @Test
    void validateInsert_ficheroVacio_devuelveMensajeVacio() {
        stubTamanoEnDisco(0L);

        assertTrue(textos(service.validateInsert(adjuntoValido())).contains(MENSAJE_VACIO));
    }

    @Test
    void validateInsert_ficheroDeUnByte_esValido() {
        stubTamanoEnDisco(1L);

        assertTrue(service.validateInsert(adjuntoValido()).isEmpty());
    }

    @Test
    void validateInsert_nombreRepetidoEntreHermanosTrasTrim_devuelveMensajeYaExiste() {
        Adjunto adjunto = adjuntoValido();
        adjunto.getCorreo().getAdjuntos().add(hermano(" informe.pdf "));

        assertTrue(textos(service.validateInsert(adjunto)).contains(MENSAJE_YA_EXISTE));
    }

    @Test
    void validateInsert_nombreUnicoEntreHermanos_esValido() {
        Adjunto adjunto = adjuntoValido();
        adjunto.getCorreo().getAdjuntos().add(hermano("foto.png"));
        adjunto.getCorreo().getAdjuntos().add(hermano(null));

        assertTrue(service.validateInsert(adjunto).isEmpty());
    }

    /*************************************** validateUpdate ***************************************/

    @Test
    void validateUpdate_siempre_devuelveMensajeInmutable() {
        assertEquals(List.of(MENSAJE_INMUTABLE), textos(service.validateUpdate(adjuntoValido(), adjuntoValido())));
    }

    /*************************************** validateRemove ***************************************/

    @Test
    void validateRemove_siempre_devuelveMensajeNoSeBorran() {
        assertEquals(List.of(MENSAJE_NO_SE_BORRAN), textos(service.validateRemove(adjuntoValido())));
    }

    /*************************************** AllowProperties ***************************************/

    @Test
    void allowPropertiesInsert_permiteNombreFicheroContenidoYCorreo() {
        AllowProperties allowProperties = service.allowPropertiesInsert();

        List.of("nombreFichero", "contenido", "correo")
                .forEach(propiedad -> assertTrue(allowProperties.allowProperty(propiedad), propiedad));
        assertFalse(allowProperties.allowProperty("nombreOriginal"));
    }

    @Test
    void allowPropertiesUpdate_denegaTodo() {
        AllowProperties allowProperties = service.allowPropertiesUpdate();

        List.of("nombreFichero", "contenido", "correo")
                .forEach(propiedad -> assertFalse(allowProperties.allowProperty(propiedad), propiedad));
    }

    @Test
    void allowPropertiesRemove_denegaTodo() {
        AllowProperties allowProperties = service.allowPropertiesRemove();

        List.of("nombreFichero", "contenido", "correo")
                .forEach(propiedad -> assertFalse(allowProperties.allowProperty(propiedad), propiedad));
    }
}
