package com.educaflow.subsystem.criptografia.util;

import com.educaflow.base.infrastructure.criptografia.DatosCertificado;
import com.educaflow.base.infrastructure.criptografia.EntornoCriptografico;
import com.educaflow.base.infrastructure.criptografia.TipoCertificado;
import com.educaflow.base.infrastructure.criptografia.TipoEmisorCertificado;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class AliasInfoBuilderTest {

    private static final String LIB = "/usr/lib/libpkcs11.so";
    private static final String PIN = "1234";
    private static final String ALIAS = "mi-alias";

    private MockedStatic<DispositivoCriptograficoInfoBuilder> dispositivoMock;
    private MockedStatic<EntornoCriptografico> entornoMock;

    @BeforeEach
    void setUp() {
        dispositivoMock = Mockito.mockStatic(DispositivoCriptograficoInfoBuilder.class);
        entornoMock = Mockito.mockStatic(EntornoCriptografico.class);
    }

    @AfterEach
    void tearDown() {
        dispositivoMock.close();
        entornoMock.close();
    }

    @Test
    void buildAliasInfo_libreriaNula_devuelveVacioSinLeerDispositivo() {
        assertEquals("", AliasInfoBuilder.buildAliasInfo(null, 1, PIN, ALIAS));
        dispositivoMock.verifyNoInteractions();
    }

    @Test
    void buildAliasInfo_libreriaEnBlanco_devuelveVacio() {
        assertEquals("", AliasInfoBuilder.buildAliasInfo("   ", 1, PIN, ALIAS));
        dispositivoMock.verifyNoInteractions();
    }

    @Test
    void buildAliasInfo_aliasNulo_devuelveVacio() {
        assertEquals("", AliasInfoBuilder.buildAliasInfo(LIB, 1, PIN, null));
        dispositivoMock.verifyNoInteractions();
    }

    @Test
    void buildAliasInfo_aliasEnBlanco_devuelveVacio() {
        assertEquals("", AliasInfoBuilder.buildAliasInfo(LIB, 1, PIN, ""));
        dispositivoMock.verifyNoInteractions();
    }

    @Test
    void buildAliasInfo_slotNulo_usaSlotCero() {
        dispositivoMock.when(() -> DispositivoCriptograficoInfoBuilder.readCertificadosPorAlias(LIB, 0, PIN))
                .thenReturn(Map.of());

        String info = AliasInfoBuilder.buildAliasInfo(LIB, null, PIN, ALIAS);

        assertEquals("(el alias no tiene un certificado X.509 asociado)\n", info);
        dispositivoMock.verify(() -> DispositivoCriptograficoInfoBuilder.readCertificadosPorAlias(LIB, 0, PIN));
    }

    @Test
    void buildAliasInfo_slotInformado_usaEseSlot() {
        dispositivoMock.when(() -> DispositivoCriptograficoInfoBuilder.readCertificadosPorAlias(LIB, 3, PIN))
                .thenReturn(Map.of());

        AliasInfoBuilder.buildAliasInfo(LIB, 3, PIN, ALIAS);

        dispositivoMock.verify(() -> DispositivoCriptograficoInfoBuilder.readCertificadosPorAlias(LIB, 3, PIN));
    }

    @Test
    void buildAliasInfo_lecturaFalla_devuelveMensajeDeError() {
        dispositivoMock.when(() -> DispositivoCriptograficoInfoBuilder.readCertificadosPorAlias(anyString(), anyInt(), anyString()))
                .thenThrow(new RuntimeException("token ausente"));

        assertEquals("No se pudo leer la información del alias: token ausente",
                AliasInfoBuilder.buildAliasInfo(LIB, 1, PIN, ALIAS));
    }

    @Test
    void buildAliasInfo_datosNoInterpretables_devuelveMensaje() {
        X509Certificate cert = Mockito.mock(X509Certificate.class);
        dispositivoMock.when(() -> DispositivoCriptograficoInfoBuilder.readCertificadosPorAlias(LIB, 1, PIN))
                .thenReturn(Map.of(ALIAS, cert));
        entornoMock.when(() -> EntornoCriptografico.getDatosCertificado(cert))
                .thenThrow(new RuntimeException("almacén no configurado"));

        assertEquals("No se pudieron interpretar los datos del certificado: almacén no configurado\n",
                AliasInfoBuilder.buildAliasInfo(LIB, 1, PIN, ALIAS));
    }

    @Test
    void buildAliasInfo_certificadoCompleto_formateaTodosLosCampos() {
        X509Certificate cert = Mockito.mock(X509Certificate.class);
        Date inicio = Date.from(Instant.EPOCH);
        Date fin = Date.from(Instant.EPOCH.plus(Duration.ofDays(1)));
        DatosCertificado datos = Mockito.mock(DatosCertificado.class);
        when(datos.isValidoEnListaCertificadosConfiables()).thenReturn(true);
        when(datos.isSelloTiempo()).thenReturn(false);
        when(datos.getCnSubject()).thenReturn("CN=Pepe");
        when(datos.getNombre()).thenReturn("Pepe");
        when(datos.getApellidos()).thenReturn("Pérez");
        when(datos.getDNI()).thenReturn("98803877V");
        when(datos.getCif()).thenReturn(null);
        when(datos.getCnIssuer()).thenReturn("CN=FNMT");
        when(datos.getTipoEmisorCertificado()).thenReturn(TipoEmisorCertificado.FNMT);
        when(datos.getTipoCertificado()).thenReturn(TipoCertificado.USUARIO_FINAL);
        when(datos.getValidoNoAntesDe()).thenReturn(inicio);
        when(datos.getValidoNoDespuesDe()).thenReturn(fin);
        dispositivoMock.when(() -> DispositivoCriptograficoInfoBuilder.readCertificadosPorAlias(LIB, 1, PIN))
                .thenReturn(Map.of(ALIAS, cert));
        entornoMock.when(() -> EntornoCriptografico.getDatosCertificado(any())).thenReturn(datos);

        String info = AliasInfoBuilder.buildAliasInfo(LIB, 1, PIN, ALIAS);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String esperado = linea("Valido según TSL", "sí")
                + linea("Es sello tiempo", "no")
                + linea("CN Sujeto", "CN=Pepe")
                + linea("Nombre", "Pepe")
                + linea("Apellidos", "Pérez")
                + linea("DNI", "98803877V")
                + linea("CIF", "")
                + linea("CN Emisor", "CN=FNMT")
                + linea("Tipo emisor certificado", "FNMT")
                + linea("Tipo Certificado", "USUARIO_FINAL")
                + linea("Fecha inicio", sdf.format(inicio))
                + linea("Fecha fin", sdf.format(fin));
        assertEquals(esperado, info);
    }

    @Test
    void buildAliasInfo_tiposDesconocidosYNoValido_marcaDesconocidoYNo() {
        X509Certificate cert = Mockito.mock(X509Certificate.class);
        DatosCertificado datos = Mockito.mock(DatosCertificado.class);
        when(datos.isValidoEnListaCertificadosConfiables()).thenReturn(false);
        when(datos.isSelloTiempo()).thenReturn(true);
        when(datos.getTipoEmisorCertificado()).thenReturn(null);
        when(datos.getTipoCertificado()).thenReturn(null);
        when(datos.getValidoNoAntesDe()).thenReturn(Date.from(Instant.EPOCH));
        when(datos.getValidoNoDespuesDe()).thenReturn(Date.from(Instant.EPOCH));
        dispositivoMock.when(() -> DispositivoCriptograficoInfoBuilder.readCertificadosPorAlias(LIB, 1, PIN))
                .thenReturn(Map.of(ALIAS, cert));
        entornoMock.when(() -> EntornoCriptografico.getDatosCertificado(cert)).thenReturn(datos);

        String info = AliasInfoBuilder.buildAliasInfo(LIB, 1, PIN, ALIAS);

        assertEquals(true, info.startsWith(linea("Valido según TSL", "no") + linea("Es sello tiempo", "sí")));
        assertEquals(true, info.contains(linea("Tipo emisor certificado", "(desconocido)")));
        assertEquals(true, info.contains(linea("Tipo Certificado", "(desconocido)")));
    }

    private static String linea(String etiqueta, String valor) {
        return String.format("%-24s %s%n", etiqueta + ":", valor);
    }
}
