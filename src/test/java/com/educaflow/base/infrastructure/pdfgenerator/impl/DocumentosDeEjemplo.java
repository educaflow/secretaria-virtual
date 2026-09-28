package com.educaflow.base.infrastructure.pdfgenerator.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Los documentos de ejemplo con los que trabajan los golden masters del generador, y el contexto
 * con el que se genera cada uno. Es el único sitio donde se declaran: dos golden masters con
 * contextos distintos dejarían de comparar lo mismo sin que nada lo delatara.
 */
final class DocumentosDeEjemplo {

    // Los recursos se leen del árbol de fuentes, y no del classpath, porque el fallo tiene que
    // nombrar la ruta exacta en la que hay que crear a mano el .golden que falta.
    static final Path CARPETA = Path.of("src/test/resources/com/educaflow/base/infrastructure/pdfgenerator");

    private static final Set<String> ABORTAN_A_PROPOSITO = Set.of(
            "documento_visible_error.xml",
            "documento_visible_no_booleano.xml",
            "documento_check_no_booleano.xml");

    private static final String VALOR_LARGO = String.join(" ", Collections.nCopies(20, "observación"));

    private static final Map<String, Map<String, Object>> SELF_POR_DOCUMENTO = Map.ofEntries(
            Map.entry("documento_basico.xml", self("nombre", "Núria Peñalver l·lengua", "acepta", true, "ciudad", "Mislata")),
            Map.entry("documento_colapsar.xml", self("mostrar", true, "nombre", "Ana")),
            Map.entry("documento_reservar.xml", self("mostrar", true, "nombre", "Ana")),
            Map.entry("documento_valor_largo.xml", self("texto", VALOR_LARGO)),
            Map.entry("documento_salto_pagina.xml", self("texto", "Valor")),
            Map.entry("documento_titulo_inline.xml", self("titular", "Ana Gil", "mostrar", true, "ciudad", "Mislata")),
            Map.entry("documento_inline_hueco.xml", self("dato", "X")),
            Map.entry("documento_inline_hueco_sin_inline.xml", self()));

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 24, 10, 30);

    private DocumentosDeEjemplo() {
    }

    static List<String> nombresQueSeGeneran() throws IOException {
        try (Stream<Path> ficheros = Files.list(CARPETA)) {
            return ficheros.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".xml"))
                    .filter(n -> !ABORTAN_A_PROPOSITO.contains(n))
                    .sorted()
                    .toList();
        }
    }

    static byte[] xml(String documento) throws IOException {
        return Files.readAllBytes(CARPETA.resolve(documento));
    }

    static Map<String, Object> contexto(String documento) {
        Map<String, Object> self = SELF_POR_DOCUMENTO.get(documento);
        if (self == null) {
            throw new AssertionError("El documento " + documento + " no tiene contexto declarado."
                    + " Añádele una entrada a SELF_POR_DOCUMENTO en " + DocumentosDeEjemplo.class.getSimpleName()
                    + " con las variables que usan sus expresiones,"
                    + " o inclúyelo en ABORTAN_A_PROPOSITO si su generación debe fallar.");
        }
        return Map.of("self", self, "now", NOW);
    }

    static String sinExtension(String documento) {
        return documento.substring(0, documento.length() - ".xml".length());
    }

    private static Map<String, Object> self(Object... claveValor) {
        Map<String, Object> self = new HashMap<>();
        for (int i = 0; i < claveValor.length; i += 2) {
            self.put((String) claveValor[i], claveValor[i + 1]);
        }
        return self;
    }
}
