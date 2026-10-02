// =====================================================================
// GENERADO por /developer-create-arch-tests desde agent_docs/architecture-rules.md
// NO EDITAR A MANO. Para cambiar un test, edita architecture-rules.md y
// vuelve a ejecutar /developer-create-arch-tests.
// =====================================================================
package com.educaflow.architecture.estructurainterna;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.ReferencedClassObject;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.library.freeze.FreezingArchRule;

@AnalyzeClasses(
    packages = "com.educaflow",
    importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeJars.class})
class EstructuraInternaTest {

    private static final String[] PAQUETES_EXENTOS = {
        "..expedientes..", "..tramites.."
    };

    // [C9] Verificación:
    //   - Sujeto: clases de `..controller..`, excluidos los paquetes exentos.
    //   - Condición: ninguna depende de clases de `..db.repo..`.
    //   - Mensaje: «el controlador delega el acceso a datos en el servicio, nunca usa el repositorio directamente».
    // frozen: incumplimiento conocido (ver "Cumplimiento" en architecture-rules.md)
    @ArchTest
    static final ArchRule c9_controladorNoAccedeARepositorio =
        FreezingArchRule.freeze(
            noClasses()
                .that().resideInAPackage("..controller..")
                    .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
                .should().dependOnClassesThat().resideInAPackage("..db.repo..")
                .because("el controlador delega el acceso a datos en el servicio, nunca usa el repositorio directamente"));

    // [C10] Verificación:
    //   - Sujeto: clases de `..controller..`, excluidos los paquetes exentos.
    //   - Condición: ninguna depende de la clase `com.axelor.db.JpaRepository`.
    //   - Mensaje: «cargar entidades es del servicio; el controlador no usa JpaRepository».
    // frozen: incumplimiento conocido (ver "Cumplimiento" en architecture-rules.md)
    @ArchTest
    static final ArchRule c10_controladorNoUsaJpaRepository =
        FreezingArchRule.freeze(
            noClasses()
                .that().resideInAPackage("..controller..")
                    .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
                .should().dependOnClassesThat()
                    .haveFullyQualifiedName("com.axelor.db.JpaRepository")
                .because("cargar entidades es del servicio; el controlador no usa JpaRepository"));

    // [C11] Verificación:
    //   - Sujeto: clases de `..db.repo..`, excluidos los paquetes exentos.
    //   - Condición: ninguna depende de clases de `..service..` ni `..controller..`.
    //   - Mensaje: «el repositorio es capa de datos: no conoce servicios ni controladores».
    @ArchTest
    static final ArchRule c11_repositorioNoDependeDeServicioNiControlador =
        noClasses()
            .that().resideInAPackage("..db.repo..")
                .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
            .should().dependOnClassesThat()
                .resideInAnyPackage("..service..", "..controller..")
            .because("el repositorio es capa de datos: no conoce servicios ni controladores");

    // [C12] Verificación:
    //   - Sujeto: clases de `..service..`, excluidos los paquetes exentos.
    //   - Condición: ninguna depende de clases de `..controller..`.
    //   - Mensaje: «la dependencia es Controller→Service, nunca Service→Controller».
    @ArchTest
    static final ArchRule c12_servicioNoDependeDeControlador =
        noClasses()
            .that().resideInAPackage("..service..")
                .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
            .should().dependOnClassesThat().resideInAPackage("..controller..")
            .because("la dependencia es Controller→Service, nunca Service→Controller");

    // [C13] Verificación:
    //   - Sujeto: clases de `..db..`, excluidas las de `..db.repo..` y los paquetes exentos.
    //   - Condición: ninguna depende de clases de `..service..` ni `..controller..`.
    //   - Mensaje: «las entidades de dominio son POJOs; la lógica de negocio vive en el servicio».
    @ArchTest
    static final ArchRule c13_entidadesDominioSonPojos =
        noClasses()
            .that().resideInAPackage("..db..")
                .and().resideOutsideOfPackages("..db.repo..")
                .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
            .should().dependOnClassesThat()
                .resideInAnyPackage("..service..", "..controller..")
            .because("las entidades de dominio son POJOs; la lógica de negocio vive en el servicio");

    // [C14] Verificación:
    //   - Sujeto: clases de `..controller..` y `..service.impl..`, excluidos los paquetes exentos. `base/util` queda deliberadamente **fuera** del sujeto: allí algún `Beans.get` de infraestructura puede ser legítimo.
    //   - Condición: ninguna depende de la clase `com.axelor.inject.Beans`.
    //   - Mensaje: «Beans.get es service-locator; se usa inyección / ModelServiceFactory».
    // frozen: incumplimiento conocido (ver "Cumplimiento" en architecture-rules.md)
    @ArchTest
    static final ArchRule c14_noBeansGetEnControladorNiServiceImpl =
        FreezingArchRule.freeze(
            noClasses()
                .that().resideInAnyPackage("..controller..", "..service.impl..")
                    .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
                .should().dependOnClassesThat()
                    .haveFullyQualifiedName("com.axelor.inject.Beans")
                .because("Beans.get es service-locator; se usa inyección / ModelServiceFactory"));

    // [C23] Verificación:
    //   - Sujeto: métodos **declarados** (no heredados) en interfaces asignables a `com.axelor.db.modelservice.ModelService`, excluidos los paquetes exentos y excluidos los propios métodos de infraestructura del contrato: los que empiezan por `validate` o por `allowProperties`.
    //   - Condición: para cada método `m` del sujeto existe en la misma interfaz un método llamado `validate` + el nombre de `m` con la inicial en mayúscula, con **la misma lista de tipos de parámetros en el mismo orden** y con tipo de retorno `java.util.Optional`.
    //   - Vacuidad: una interfaz sin acciones propias cumple la regla (no debe fallar por sujeto vacío).
    //   - Nota: el argumento genérico de `Optional<BusinessMessages>` se borra en bytecode, así que la condición de retorno solo puede comprobar `Optional`. Es suficiente: ningún otro método del contrato devuelve `Optional`.
    //   - Mensaje: «cada acción propia de un *Service declara su validador validate<Accion> con la misma firma de parámetros».
    // frozen: incumplimiento conocido (ver "Cumplimiento" en architecture-rules.md)
    @ArchTest
    static final ArchRule c23_accionDeServicioDeclaraSuValidador =
        FreezingArchRule.freeze(
            methods()
                .that().areDeclaredInClassesThat().areInterfaces()
                    .and().areDeclaredInClassesThat()
                        .areAssignableTo("com.axelor.db.modelservice.ModelService")
                    .and().areDeclaredInClassesThat().resideOutsideOfPackages(PAQUETES_EXENTOS)
                    .and().haveNameNotStartingWith("validate")
                    .and().haveNameNotStartingWith("allowProperties")
                .should(declararSuValidador())
                .because("cada acción propia de un *Service declara su validador validate<Accion> con la misma firma de parámetros")
                .allowEmptyShould(true));

    // [C28] Verificación:
    //   - Sujeto: clases de `..controller..`, excluidos los paquetes exentos, que llaman a algún método **acción** de servicio. Un método acción es el sujeto de C23: un método **declarado** (no heredado) en una interfaz de `com.educaflow` asignable a `com.axelor.db.modelservice.ModelService`, cuyo nombre no empieza por `validate` ni por `allowProperties`.
    //   - Condición: por cada acción `m` de una interfaz `S` a la que llama la clase, la misma clase declara un método llamado `validate` + el nombre de `m` con la inicial en mayúscula, y ese método llama a un método de `S` con ese mismo nombre.
    //   - Vacuidad: un controlador que no llama a ninguna acción de servicio cumple la regla (no debe fallar por sujeto vacío).
    //   - Mensaje: «el controlador que expone una acción de un *Service expone también su validate<Accion>, que llama al validador de esa acción».
    // frozen: incumplimiento conocido (ver "Cumplimiento" en architecture-rules.md)
    @ArchTest
    static final ArchRule c28_controladorExponeLaValidacionDeLaAccionQueExpone =
        FreezingArchRule.freeze(
            classes()
                .that().resideInAPackage("..controller..")
                    .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
                .should(exponerLaValidacionDeCadaAccionDeServicioALaQueLlama())
                .because("el controlador que expone una acción de un *Service expone también su validate<Accion>, que llama al validador de esa acción")
                .allowEmptyShould(true));

    // [C24] Verificación:
    //   - Sujeto: clases de `com.educaflow.subsystem.tramitador.tramitacion..`.
    //   - Condición: ninguna depende de clases de `com.educaflow.subsystem.tramitador.service..` ni de `com.educaflow.subsystem.tramitador.controller..`.
    //   - Exenciones: no aplican. `tramitador` **no** está entre los paquetes exentos de las Convenciones de verificación, así que esta regla —como el resto— se le aplica sin más.
    //   - Mensaje: «el motor de tramitación no depende de los servicios ni de los controladores del tramitador: la dependencia va de los servicios/controladores al motor, nunca al revés».
    @ArchTest
    static final ArchRule c24_motorDeTramitacionNoDependeDeServiciosNiControladores =
        noClasses()
            .that().resideInAPackage("com.educaflow.subsystem.tramitador.tramitacion..")
            .should().dependOnClassesThat()
                .resideInAnyPackage(
                    "com.educaflow.subsystem.tramitador.service..",
                    "com.educaflow.subsystem.tramitador.controller..")
            .because("el motor de tramitación no depende de los servicios ni de los controladores del tramitador: la dependencia va de los servicios/controladores al motor, nunca al revés");

    // [C25] Verificación:
    //   - Sujeto: clases de `com.educaflow.subsystem.expedientes..`, **incluidas** las entidades generadas de `..db..` — que son justamente el punto por el que el ciclo entra.
    //     **CRITICAL**: esta regla declara expresamente que **NO** se le aplica la exención global de `..expedientes..` de las Convenciones de verificación; su sujeto es precisamente ese paquete exento, y excluirlo dejaría la regla vacía.
    //   - Condición: ninguna depende de clases de `com.educaflow.subsystem.tramitador..`.
    //   - Exenciones: no aplican.
    //   - Mensaje: «el dominio de expedientes no depende del tramitador: la dependencia va del tramitador al dominio, nunca al revés».
    @ArchTest
    static final ArchRule c25_dominioDeExpedientesNoDependeDelTramitador =
        noClasses()
            .that().resideInAPackage("com.educaflow.subsystem.expedientes..")
            .should().dependOnClassesThat()
                .resideInAPackage("com.educaflow.subsystem.tramitador..")
            .because("el dominio de expedientes no depende del tramitador: la dependencia va del tramitador al dominio, nunca al revés");

    // [C26] Verificación:
    //   - Sujeto: clases de `com.educaflow..` que residen en `..db.repo..` y cuyo nombre simple termina en `Repository`.
    //     Los `*Listener` de `db.repo` (C18) quedan fuera: los referencia la propia entidad por diseño.
    //   - Dueño de cada repositorio del sujeto: el definido en «Unidades y dueños».
    //   - Condición: toda clase que dependa de un repositorio del sujeto cumple una de estas:
    //     - es un repositorio del sujeto con el **mismo** dueño;
    //     - el dueño es un sistema/subsistema `com.educaflow.<subsystem|system>.<X>` y la clase reside en `com.educaflow.<subsystem|system>.<X>.service..`;
    //     - el dueño es un trámite y la clase reside en el paquete de ese trámite (o en sus subpaquetes).
    //   - Exenciones: no aplican. **CRITICAL**: esta regla declara expresamente que **NO** se le aplican las exenciones globales de `..expedientes..` ni de `..tramites..`, ni como origen ni como destino: los repositorios de `subsystem/expedientes` y los de las entidades de los trámites (que se generan en `com.educaflow.subsystem.expedientes.db.repo`) también son privados.
    //   - Mensaje: «un repositorio es privado de la unidad dueña de su entidad: solo lo usan sus servicios (o, en un trámite, sus clases); las demás unidades piden los datos a uno de sus servicios».
    @ArchTest
    static final ArchRule c26_repositorioSoloLoUsaSuDueno =
        classes()
            .that().resideInAPackage("com.educaflow..")
                .and().resideInAPackage("..db.repo..")
                .and().haveSimpleNameEndingWith("Repository")
            .should(serUsadoSoloPorSuDueno())
            .because("un repositorio es privado de la unidad dueña de su entidad: solo lo usan sus servicios (o, en un trámite, sus clases); las demás unidades piden los datos a uno de sus servicios");

    // [C27] Verificación:
    //   - Sujeto: las llamadas a `com.axelor.db.JpaRepository.of(Class)` y al constructor de `com.axelor.db.JpaRepository` (el `super(Entidad.class)` de un repositorio que hereda de él) hechas desde clases de `com.educaflow..`.
    //   - Entidad de cada llamada: el literal de clase (`Entidad.class`) que aparece en la **misma línea** que la llamada y que es una entidad con dueño según «Unidades y dueños».
    //     Una llamada sin literal (la clase llega en una variable, como en los helpers genéricos de `base/infrastructure`) o cuyo literal no es una entidad con dueño queda fuera: no se puede atribuir.
    //   - Condición: la clase que hace la llamada reside en el paquete de la unidad dueña de la entidad (o en sus subpaquetes), **o** es un repositorio de `..db.repo..` con ese mismo dueño según C26 (los `Abstract<Entidad>Repository` generados).
    //   - Exenciones: no aplican. **CRITICAL**: igual que C26, **NO** se le aplican las exenciones globales de `..expedientes..` ni de `..tramites..`.
    //   - Mensaje: «JpaRepository solo lee entidades de su propia unidad: las de otra unidad se piden a uno de sus servicios».
    // frozen: incumplimiento conocido (ver "Cumplimiento" en architecture-rules.md)
    @ArchTest
    static final ArchRule c27_jpaRepositorySoloLeeEntidadesDeSuUnidad =
        FreezingArchRule.freeze(
            classes()
                .that().resideInAPackage("com.educaflow..")
                .should(crearJpaRepositorySoloSobreEntidadesDeSuUnidad())
                .because("JpaRepository solo lee entidades de su propia unidad: las de otra unidad se piden a uno de sus servicios"));

    private static ArchCondition<JavaClass> serUsadoSoloPorSuDueno() {
        return new ArchCondition<JavaClass>("ser usado solo por su unidad dueña") {
            @Override
            public void check(JavaClass repositorio, ConditionEvents events) {
                Unidad dueno = Duenos.INSTANCE.duenoDeRepositorio(repositorio);
                for (Dependency dependencia : repositorio.getDirectDependenciesToSelf()) {
                    JavaClass origen = dependencia.getOriginClass();
                    if (!Duenos.INSTANCE.puedeUsarRepositorio(origen, dueno)) {
                        events.add(SimpleConditionEvent.violated(dependencia, dependencia.getDescription()));
                    }
                }
            }
        };
    }

    private static ArchCondition<JavaClass> crearJpaRepositorySoloSobreEntidadesDeSuUnidad() {
        return new ArchCondition<JavaClass>("crear JpaRepository solo sobre entidades de su propia unidad") {
            @Override
            public void check(JavaClass clase, ConditionEvents events) {
                for (JavaCodeUnit codeUnit : clase.getCodeUnits()) {
                    for (JavaCall<?> llamada : codeUnit.getCallsFromSelf()) {
                        if (!esCreacionDeJpaRepository(llamada)) {
                            continue;
                        }
                        for (ReferencedClassObject literal : codeUnit.getReferencedClassObjects()) {
                            if (literal.getLineNumber() != llamada.getLineNumber()) {
                                continue;
                            }
                            Unidad dueno = Duenos.INSTANCE.duenoDeEntidad(literal.getRawType().getName());
                            if (dueno != null && !Duenos.INSTANCE.puedeLeerEntidad(clase, dueno)) {
                                events.add(SimpleConditionEvent.violated(llamada,
                                    llamada.getDescription() + " crea un JpaRepository sobre "
                                        + literal.getRawType().getName() + ", que es de " + dueno.paquete()));
                            }
                        }
                    }
                }
            }
        };
    }

    private static boolean esCreacionDeJpaRepository(JavaCall<?> llamada) {
        boolean deJpaRepository = llamada.getTarget().getOwner().getName().equals("com.axelor.db.JpaRepository");
        boolean esOf = llamada.getName().equals("of");
        boolean esConstructor = llamada.getName().equals(JavaConstructor.CONSTRUCTOR_NAME);
        return deJpaRepository && (esOf || esConstructor);
    }

    /** Un sistema/subsistema o un trámite (ver «Unidades y dueños» en architecture-rules.md). */
    private record Unidad(String paquete, boolean esTramite) {

        boolean contiene(String paqueteClase) {
            return paqueteClase.equals(paquete) || paqueteClase.startsWith(paquete + ".");
        }
    }

    /** Dueños de entidades y repositorios, leídos de los XML de dominio y de los TramiteInstance.xml de src/main/java. */
    private static final class Duenos {

        private static final Pattern SISTEMA = Pattern.compile("^(com\\.educaflow\\.(?:subsystem|system)\\.[^.]+)(\\..*)?$");

        static final Duenos INSTANCE = new Duenos();

        private final Path raiz = raizDelCodigo();
        private final List<Unidad> tramites = new ArrayList<>();
        private final Map<String, Unidad> duenoPorEntidad = new HashMap<>();

        private Duenos() {
            try (Stream<Path> ficheros = Files.walk(raiz.resolve("com/educaflow"))) {
                List<Path> xmls = ficheros.filter(f -> f.toString().endsWith(".xml")).toList();
                xmls.stream()
                    .filter(f -> f.getFileName().toString().equals("TramiteInstance.xml"))
                    .forEach(f -> tramites.add(new Unidad(paqueteDe(f.getParent()), true)));
                for (Path xml : xmls) {
                    registrarEntidades(xml);
                }
            } catch (Exception e) {
                throw new IllegalStateException("No se pudieron leer los XML de dominio de " + raiz, e);
            }
        }

        Unidad duenoDeEntidad(String fqn) {
            return duenoPorEntidad.get(fqn);
        }

        Unidad duenoDeRepositorio(JavaClass repositorio) {
            String nombre = repositorio.getSimpleName().replaceFirst("^Abstract", "").replaceFirst("Repository$", "");
            String paqueteEntidad = repositorio.getPackageName().replaceFirst("\\.repo$", "");
            Unidad dueno = duenoPorEntidad.get(paqueteEntidad + "." + nombre);
            return dueno != null ? dueno : unidadDe(repositorio.getPackageName());
        }

        boolean puedeUsarRepositorio(JavaClass origen, Unidad dueno) {
            if (dueno == null) {
                return true;
            }
            if (esRepositorio(origen) && dueno.equals(duenoDeRepositorio(origen))) {
                return true;
            }
            String paquete = origen.getPackageName();
            return dueno.esTramite()
                ? dueno.contiene(paquete)
                : new Unidad(dueno.paquete() + ".service", false).contiene(paquete);
        }

        boolean puedeLeerEntidad(JavaClass clase, Unidad dueno) {
            if (dueno.contiene(clase.getPackageName())) {
                return true;
            }
            return esRepositorio(clase) && dueno.equals(duenoDeRepositorio(clase));
        }

        private static boolean esRepositorio(JavaClass clase) {
            String paquete = clase.getPackageName();
            return (paquete.endsWith(".db.repo") || paquete.contains(".db.repo."))
                && clase.getSimpleName().endsWith("Repository");
        }

        private Unidad unidadDe(String paquete) {
            for (Unidad tramite : tramites) {
                if (tramite.contiene(paquete)) {
                    return tramite;
                }
            }
            Matcher sistema = SISTEMA.matcher(paquete);
            return sistema.matches() ? new Unidad(sistema.group(1), false) : null;
        }

        private void registrarEntidades(Path xml) throws Exception {
            if (!Files.readString(xml).contains("<domain-models")) {
                return;
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            Document documento = factory.newDocumentBuilder().parse(xml.toFile());
            NodeList modulos = documento.getElementsByTagName("module");
            if (modulos.getLength() == 0) {
                return;
            }
            String paqueteEntidades = ((Element) modulos.item(0)).getAttribute("package");
            Unidad dueno = unidadDe(paqueteDe(xml.getParent()));
            if (dueno == null) {
                return;
            }
            NodeList entidades = documento.getElementsByTagName("entity");
            for (int i = 0; i < entidades.getLength(); i++) {
                String nombre = ((Element) entidades.item(i)).getAttribute("name");
                duenoPorEntidad.put(paqueteEntidades + "." + nombre, dueno);
            }
        }

        private String paqueteDe(Path carpeta) {
            return raiz.relativize(carpeta).toString().replace(java.io.File.separatorChar, '.');
        }

        private static Path raizDelCodigo() {
            for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
                if (Files.isDirectory(dir.resolve("src/main/java/com/educaflow"))) {
                    return dir.resolve("src/main/java");
                }
            }
            throw new IllegalStateException("No se encuentra src/main/java/com/educaflow subiendo desde " + Path.of("").toAbsolutePath());
        }
    }

    private static ArchCondition<JavaClass> exponerLaValidacionDeCadaAccionDeServicioALaQueLlama() {
        return new ArchCondition<JavaClass>(
                "exponer el validate<Accion> de cada acción de servicio a la que llama") {
            @Override
            public void check(JavaClass controlador, ConditionEvents events) {
                Map<String, JavaMethod> acciones = new HashMap<>();
                for (JavaCall<?> llamada : controlador.getMethodCallsFromSelf()) {
                    accionDeServicio(llamada).ifPresent(accion -> acciones.putIfAbsent(accion.getFullName(), accion));
                }

                for (JavaMethod accion : acciones.values()) {
                    JavaClass servicio = accion.getOwner();
                    String nombreValidador = "validate"
                        + Character.toUpperCase(accion.getName().charAt(0))
                        + accion.getName().substring(1);

                    boolean expuesto = controlador.getMethods().stream()
                        .filter(metodo -> metodo.getName().equals(nombreValidador))
                        .anyMatch(metodo -> metodo.getMethodCallsFromSelf().stream()
                            .anyMatch(llamada -> llamada.getTargetOwner().equals(servicio)
                                && llamada.getName().equals(nombreValidador)));

                    events.add(new SimpleConditionEvent(controlador, expuesto,
                        expuesto
                            ? controlador.getName() + " expone " + nombreValidador
                            : controlador.getName() + " llama a la acción " + accion.getFullName()
                                + " y no tiene un método " + nombreValidador + " que llame a su validador"));
                }
            }

            /** El método acción de servicio al que va la llamada, si lo es (el sujeto de C23). */
            private java.util.Optional<JavaMethod> accionDeServicio(JavaCall<?> llamada) {
                JavaClass servicio = llamada.getTargetOwner();
                String nombre = llamada.getName();

                if (servicio.isInterface() == false
                        || servicio.getPackageName().startsWith("com.educaflow") == false
                        || servicio.isAssignableTo("com.axelor.db.modelservice.ModelService") == false
                        || nombre.startsWith("validate")
                        || nombre.startsWith("allowProperties")) {
                    return java.util.Optional.empty();
                }

                return llamada.getTarget().resolveMember()
                    .filter(miembro -> miembro instanceof JavaMethod)
                    .map(miembro -> (JavaMethod) miembro)
                    .filter(metodo -> metodo.getOwner().equals(servicio));
            }
        };
    }

    private static ArchCondition<JavaMethod> declararSuValidador() {
        return new ArchCondition<JavaMethod>(
                "declarar su validador validate<Accion> con la misma firma de parámetros") {
            @Override
            public void check(JavaMethod accion, ConditionEvents events) {
                String nombreValidador = "validate"
                    + Character.toUpperCase(accion.getName().charAt(0))
                    + accion.getName().substring(1);
                List<String> parametrosAccion = tiposDe(accion);

                boolean declarado = accion.getOwner().getMethods().stream()
                    .anyMatch(candidato ->
                        candidato.getName().equals(nombreValidador)
                            && tiposDe(candidato).equals(parametrosAccion)
                            && candidato.getRawReturnType().getName().equals("java.util.Optional"));

                events.add(new SimpleConditionEvent(accion, declarado,
                    declarado
                        ? accion.getFullName() + " declara " + nombreValidador
                        : "falta " + nombreValidador + " para la acción " + accion.getFullName()));
            }

            private List<String> tiposDe(JavaMethod metodo) {
                return metodo.getRawParameterTypes().stream().map(JavaClass::getName).toList();
            }
        };
    }
}
