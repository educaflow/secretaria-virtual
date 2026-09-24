package com.educaflow.base.infrastructure.pdfgenerator.impl.dibujo;

import com.educaflow.base.infrastructure.pdfgenerator.impl.texto.Fuente;
import com.itextpdf.io.font.FontProgram;
import com.itextpdf.io.font.FontProgramFactory;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

/**
 * Los recursos del dibujo que se leen una sola vez: los programas de las cuatro fuentes Roboto y el
 * logo. Un {@link FontProgram} no cambia al usarse, así que se comparte entre documentos e hilos; lo
 * que no se comparte es el {@code PdfFont} de cada documento ({@link Fuentes}).
 */
final class FuentesRoboto {

    private static final String RUTA_ASSETS = "/com/educaflow/base/infrastructure/pdfgenerator/impl/assets/";

    private FuentesRoboto() {
    }

    private static final class Holder {
        static final Map<Fuente, FontProgram> PROGRAMAS = cargarProgramas();
        static final ImageData LOGO = ImageDataFactory.create(leer(RUTA_ASSETS + "logo-gva.png"));
    }

    static FontProgram programa(Fuente fuente) {
        return Holder.PROGRAMAS.get(fuente);
    }

    static ImageData logo() {
        return Holder.LOGO;
    }

    private static Map<Fuente, FontProgram> cargarProgramas() {
        try {
            Map<Fuente, FontProgram> programas = new EnumMap<>(Fuente.class);
            for (Fuente fuente : Fuente.values()) {
                programas.put(fuente, FontProgramFactory.createFont(leer(RUTA_ASSETS + "fonts/" + fuente.getFichero()), false));
            }
            return programas;
        } catch (IOException ex) {
            throw new IllegalStateException("No se pueden cargar las fuentes Roboto del generador de PDF", ex);
        }
    }

    private static byte[] leer(String recurso) {
        try (InputStream in = FuentesRoboto.class.getResourceAsStream(recurso)) {
            if (in == null) {
                throw new IllegalStateException("Falta el recurso " + recurso + " del generador de PDF");
            }
            return in.readAllBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("No se puede leer el recurso " + recurso + " del generador de PDF", ex);
        }
    }
}
