package com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo;

import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Familia;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Fuente;
import com.itextpdf.io.font.FontProgram;
import com.itextpdf.io.font.FontProgramFactory;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Los recursos del dibujo que se leen una sola vez: los programas de las fuentes de cada familia y
 * el logo. Un {@link FontProgram} no cambia al usarse, así que se comparte entre documentos e
 * hilos; lo que no se comparte es el {@code PdfFont} de cada documento ({@link Fuentes}).
 *
 * <p>Las familias se cargan según se piden: un documento que solo use una no lee de disco la otra.
 */
final class Assets {

    private static final String RUTA_ASSETS = "/com/educaflow/base/infrastructure/pdfgenerator/impl/comun/assets/";

    private static final Map<Familia, Map<Fuente, FontProgram>> PROGRAMAS = new ConcurrentHashMap<>();

    private Assets() {
    }

    private static final class Holder {
        static final ImageData LOGO = ImageDataFactory.create(leer(RUTA_ASSETS + "logo-gva.png"));
    }

    static FontProgram programa(Familia familia, Fuente fuente) {
        return PROGRAMAS.computeIfAbsent(familia, Assets::cargarProgramas).get(fuente);
    }

    static ImageData logo() {
        return Holder.LOGO;
    }

    private static Map<Fuente, FontProgram> cargarProgramas(Familia familia) {
        try {
            Map<Fuente, FontProgram> programas = new EnumMap<>(Fuente.class);
            for (Fuente fuente : Fuente.values()) {
                programas.put(fuente,
                        FontProgramFactory.createFont(leer(RUTA_ASSETS + "fonts/" + familia.getFichero(fuente)), false));
            }
            return programas;
        } catch (IOException ex) {
            throw new IllegalStateException("No se pueden cargar las fuentes " + familia + " del generador de PDF", ex);
        }
    }

    private static byte[] leer(String recurso) {
        try (InputStream in = Assets.class.getResourceAsStream(recurso)) {
            if (in == null) {
                throw new IllegalStateException("Falta el recurso " + recurso + " del generador de PDF");
            }
            return in.readAllBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("No se puede leer el recurso " + recurso + " del generador de PDF", ex);
        }
    }
}
