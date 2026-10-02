package com.educaflow.base.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Map;

public class QrUtil {

    /** Los píxeles de lado de cada módulo: de sobra para que el QR no se vea borroso al imprimirlo. */
    private static final int PIXELES_POR_MODULO = 10;

    /**
     * @return el PNG del código QR del texto, con el margen en blanco que necesita para poder leerse
     */
    public static byte[] generarPng(String texto) {
        TextUtil.requireNonBlank(texto, "El texto del código QR no puede estar vacío");

        try {
            // Con ancho y alto 0 la matriz sale con un punto por módulo, margen incluido
            BitMatrix modulos = new QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, 0, 0, Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M));
            int lado = modulos.getWidth() * PIXELES_POR_MODULO;
            BufferedImage imagen = new BufferedImage(lado, lado, BufferedImage.TYPE_BYTE_BINARY);
            for (int x = 0; x < lado; x++) {
                for (int y = 0; y < lado; y++) {
                    imagen.setRGB(x, y, modulos.get(x / PIXELES_POR_MODULO, y / PIXELES_POR_MODULO) ? 0x000000 : 0xFFFFFF);
                }
            }

            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(imagen, "png", png);

            return png.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
