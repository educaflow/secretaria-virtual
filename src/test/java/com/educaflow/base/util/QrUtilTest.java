package com.educaflow.base.util;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QrUtilTest {

    @Test
    void generarPng_conUnaUrl_devuelveUnPngCuadradoQueAlLeerloDaEsaUrl() throws Exception {
        String url = "https://secretaria.fpmislata.com/ws/public/regsalida/0123456789ABCDEFGHJKMNPQRS";

        BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(QrUtil.generarPng(url)));

        assertEquals(imagen.getWidth(), imagen.getHeight());
        assertEquals(url, leer(imagen));
    }

    @Test
    void generarPng_conTextoVacio_lanzaIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> QrUtil.generarPng(" "));
    }

    private static String leer(BufferedImage imagen) throws Exception {
        int[] pixeles = imagen.getRGB(0, 0, imagen.getWidth(), imagen.getHeight(), null, 0, imagen.getWidth());
        RGBLuminanceSource fuente = new RGBLuminanceSource(imagen.getWidth(), imagen.getHeight(), pixeles);

        return new QRCodeReader().decode(new BinaryBitmap(new HybridBinarizer(fuente))).getText();
    }
}
