package com.educaflow.tiposexpedientes.support;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

import java.lang.annotation.Annotation;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Lectura del bytecode de los tipos de expediente.
 *
 * <p>Se usa el {@code ClassFileImporter} de ArchUnit como <b>lector de bytecode</b>, no su DSL de
 * reglas: las reglas de este paquete están cuantificadas sobre los estados y eventos de un XML
 * externo, no sobre las clases importadas, y con el DSL una clase que faltara del todo haría que la
 * regla se cumpliese en vacío. El importador, en cambio, aporta justo lo que hace falta y ninguna de
 * las pegas de la reflexión: no <b>carga</b> las clases (nada de inicializadores estáticos de las
 * entidades de Axelor ni de {@code NoClassDefFoundError}) y da los tipos de parámetro como FQCN en
 * texto, que es exactamente lo que se compara.
 *
 * <p>Cubre a la vez el {@code EventManagerImpl} (Java) y el {@code StateEventValidatorImpl}
 * (Kotlin), porque el build compila ambos al mismo directorio de clases
 * ({@code compileKotlin.destinationDirectory} apunta a {@code build/classes/java/main}).
 */
public final class Bytecode {

    private static JavaClasses cache;

    private Bytecode() {}

    /** Las clases compiladas bajo com.educaflow.tramites. Cacheado. */
    public static synchronized JavaClasses all() {
        if (cache == null) {
            cache = new ClassFileImporter()
                    .withImportOption(new ImportOption.DoNotIncludeTests())
                    // Solo lo que compila este proyecto; nada que llegue en un JAR de dependencia.
                    .withImportOption(new ImportOption.DoNotIncludeJars())
                    .importPackages("com.educaflow.tramites");
        }
        return cache;
    }

    /** La clase con ese FQCN, o vacío si no está compilada. */
    public static Optional<JavaClass> clase(String fqcn) {
        return all().stream().filter(c -> c.getFullName().equals(fqcn)).findFirst();
    }

    /**
     * Los métodos declarados en la clase anotados con esa anotación, descartando los sintéticos y
     * puente que añade el compilador (sobre todo el de Kotlin).
     */
    public static List<JavaMethod> metodosAnotados(JavaClass clase, Class<? extends Annotation> anotacion) {
        return clase.getMethods().stream()
                .filter(Bytecode::esDelProgramador)
                .filter(m -> m.isAnnotatedWith(anotacion))
                .sorted(Comparator.comparing(JavaMethod::getName))
                .collect(Collectors.toList());
    }

    /** Los métodos declarados con ese nombre, con independencia de anotación y firma. */
    public static List<JavaMethod> metodosLlamados(JavaClass clase, String nombre) {
        return clase.getMethods().stream()
                .filter(Bytecode::esDelProgramador)
                .filter(m -> m.getName().equals(nombre))
                .collect(Collectors.toList());
    }

    /** Descarta los métodos que no escribió nadie: los sintéticos y puente que añade el compilador. */
    private static boolean esDelProgramador(JavaMethod metodo) {
        return !metodo.getModifiers().contains(JavaModifier.SYNTHETIC)
                && !metodo.getModifiers().contains(JavaModifier.BRIDGE);
    }

    /** Firma legible de un método: {@code nombre(TipoParam1, TipoParam2): TipoRetorno}. */
    public static String firma(JavaMethod metodo) {
        String params = metodo.getRawParameterTypes().stream()
                .map(JavaClass::getFullName)
                .collect(Collectors.joining(", "));
        return metodo.getName() + "(" + params + "): " + metodo.getRawReturnType().getFullName();
    }

    /** Firma legible esperada, construida a partir de los FQCN de los tipos. */
    public static String firmaEsperada(String nombre, String fqcnRetorno, String... fqcnParametros) {
        return nombre + "(" + String.join(", ", fqcnParametros) + "): " + fqcnRetorno;
    }

    /** true si el método tiene exactamente esos tipos de parámetro y ese tipo de retorno. */
    public static boolean tieneFirma(JavaMethod metodo, String fqcnRetorno, String... fqcnParametros) {
        if (!metodo.getRawReturnType().getFullName().equals(fqcnRetorno)) {
            return false;
        }
        List<JavaClass> parametros = metodo.getRawParameterTypes();
        if (parametros.size() != fqcnParametros.length) {
            return false;
        }
        for (int i = 0; i < fqcnParametros.length; i++) {
            if (!parametros.get(i).getFullName().equals(fqcnParametros[i])) {
                return false;
            }
        }
        return true;
    }
}
