package com.educaflow.secretariavirtual.startup;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El arranque vacía las tablas de metadatos con {@code TRUNCATE ... CASCADE}, que arrastra a toda tabla con una FK
 * <b>hacia</b> una tabla truncada aunque esté en la lista de excluidas. Las excluidas lo están porque guardan datos
 * reales (usuarios, grupos, ficheros, secuencias, filtros): si alguna de sus entidades ganara un {@code many-to-one}
 * o {@code one-to-one} hacia una entidad de tabla truncada (p. ej. {@code User → MetaAction}), el siguiente arranque
 * las vaciaría sin avisar.
 *
 * <p>Se leen las entidades compiladas (las de Axelor vienen en JAR, las del proyecto en {@code build/src-gen}) con el
 * importador de bytecode de ArchUnit, sin cargar las clases. Una relación {@code many-to-many} no crea FK desde la
 * tabla de la entidad sino desde su tabla de unión, así que no arrastra a la excluida y no se comprueba.
 */
class TablasExcluidasDelTruncadoTest {

    private static Map<String, JavaClass> entidadPorTabla;

    @BeforeAll
    static void importarEntidades() {
        JavaClasses clases = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.axelor", "com.educaflow");
        entidadPorTabla = clases.stream()
                .filter(c -> c.isAnnotatedWith(Entity.class) && c.isAnnotatedWith(Table.class))
                .collect(Collectors.toMap(TablasExcluidasDelTruncadoTest::tabla, c -> c, (a, b) -> a));
    }

    @Test
    void todasLasTablasExcluidasTienenEntidad() {
        List<String> sinEntidad = DataBaseStartup.TABLAS_EXCLUIDAS.stream()
                .filter(t -> !entidadPorTabla.containsKey(t))
                .sorted()
                .toList();
        assertEquals(List.of(), sinEntidad, "Tablas excluidas del truncado sin entidad JPA: nada que comprobar sobre ellas");
    }

    @TestFactory
    Stream<DynamicTest> ningunaTablaExcluidaApuntaAUnaTablaTruncada() {
        return DataBaseStartup.TABLAS_EXCLUIDAS.stream().sorted().map(tabla -> DynamicTest.dynamicTest(tabla, () -> {
            JavaClass entidad = entidadPorTabla.get(tabla);
            assertTrue(entidad != null, "La tabla " + tabla + " no tiene entidad JPA");

            List<String> fksPeligrosas = new ArrayList<>();
            for (JavaField campo : entidad.getAllFields()) {
                if (!esClaveAjena(campo)) {
                    continue;
                }
                String destino = tablaDe(campo.getRawType()).orElse(null);
                if (destino != null && DataBaseStartup.seTrunca(destino)) {
                    fksPeligrosas.add(campo.getOwner().getSimpleName() + "." + campo.getName() + " → " + destino);
                }
            }
            assertFalse(!fksPeligrosas.isEmpty(),
                    "La tabla " + tabla + " está excluida del truncado pero tiene FKs hacia tablas truncadas, "
                            + "y el TRUNCATE CASCADE la vaciaría en cada arranque: " + fksPeligrosas);
        }));
    }

    @Test
    void laPoliticaDeTruncadoDistingueLosTresCasos() {
        assertTrue(DataBaseStartup.seTrunca("meta_action"));
        assertTrue(DataBaseStartup.seTrunca("auth_role"));
        assertFalse(DataBaseStartup.seTrunca("auth_user"));
        assertFalse(DataBaseStartup.seTrunca("common_centro"));
        assertTrue(DataBaseStartup.seTrunca("security_ace_profile_global"));
    }

    private static boolean esClaveAjena(JavaField campo) {
        return campo.isAnnotatedWith(ManyToOne.class)
                || (campo.isAnnotatedWith(OneToOne.class) && campo.getAnnotationOfType(OneToOne.class).mappedBy().isEmpty());
    }

    private static Optional<String> tablaDe(JavaClass clase) {
        return clase.isAnnotatedWith(Table.class) ? Optional.of(tabla(clase)) : Optional.empty();
    }

    private static String tabla(JavaClass clase) {
        return clase.getAnnotationOfType(Table.class).name().toLowerCase(Locale.ROOT);
    }
}
