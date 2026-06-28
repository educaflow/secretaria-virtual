# Reglas de arquitectura ArchUnit — secretaría virtual

Catálogo de reglas de arquitectura **específicas de este proyecto** (no genéricas de
ArchUnit; para eso ver `SKILL.md`, `reference.md` y `examples.md`). Cada regla trae un
ejemplo ArchUnit listo para usar (ArchUnit 1.4.2, JUnit 5) anclado en los paquetes reales
de `com.educaflow`. Este fichero es la base para, en el futuro, generar tests ArchUnit del
proyecto.

> **Mantener coherencia con [`architecture.md`](architecture.md).** Este fichero **codifica
> como reglas verificables** las invariantes de la arquitectura; `architecture.md` la
> **describe** en prosa (paquetes, responsabilidades, sistemas/subsistemas, expedientes). Si
> cambias una regla aquí, **MUST** comprobar si hay que actualizar la descripción de
> `architecture.md`, y viceversa. Los dos ficheros describen la misma arquitectura desde
> ángulos distintos y no deben divergir.

## Fuente de verdad

Las reglas se derivan de la **arquitectura documentada**, NO de lo que el código hace hoy
(el código puede tener bugs; codificar "lo que el código hace" produciría reglas erróneas).
El origen de cada regla es:

- `CLAUDE.md` raíz (capas `com.educaflow`, multicentro, sistemas/subsistemas).
- Skill `k-sistemas` (`SKILL.md`, `servicios.md`, `controladores.md`): capas, nomenclatura,
  Controller→Service→Repository, `ModelServiceFactory`, repositorios.
- Skill `k-secure-coding`: antipatrones (`Beans.get`, mass-assignment…).
- Skill `k-guice`: módulos `AxelorModule`.

> **CRITICAL — el código puede incumplir una regla correcta.** Si una clase real viola una
> regla, eso es un **bug del código**, NO una excepción a la regla. Cada regla lleva un
> apartado **Estado actual** que indica si el código la cumple hoy; las que no se cumplen se
> marcan como candidatas a `FreezingArchRule.freeze(...)` (ver `reference.md` §11) para
> introducir el test sin romper el build y arreglar las violaciones de forma incremental.

## Cómo se usan estas reglas

Una clase de test JUnit 5 con `@AnalyzeClasses` y cada regla como `@ArchTest static final
ArchRule`:

```java
@AnalyzeClasses(
    packages = "com.educaflow",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaSecretariaVirtualTest {

    // Paquetes con arquitectura propia (EventManager, view_models, carpetas en plural):
    // EXENTOS de todas las reglas. Cada regla excluye su sujeto con
    // .resideOutsideOfPackages(PAQUETES_EXENTOS).
    private static final String[] PAQUETES_EXENTOS = {
        "..expedientes..", "..tiposexpedientes..", "..tramites.."
    };

    // ... aquí cada @ArchTest static final ArchRule del catálogo ...
}
```

### Imports usados por el catálogo

```java
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import com.tngtech.archunit.library.GeneralCodingRules;

// Tipos del framework Axelor referenciados por las reglas:
import com.axelor.app.AxelorModule;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.db.modelservice.ModelService;
```

## Mapa de capas y paquetes reales

```
base/util  ←  base/infrastructure  ←  subsystem  ←  system  ←  secretariavirtual
(más baja)                                                      (más alta, ensamblaje)
```

| Capa                | Paquete                              |
|---------------------|--------------------------------------|
| base/util           | `com.educaflow.base.util`            |
| base/infrastructure | `com.educaflow.base.infrastructure`  |
| subsystem           | `com.educaflow.subsystem.*`          |
| system              | `com.educaflow.system.*`             |
| secretariavirtual   | `com.educaflow.secretariavirtual.*`  |

Estructura interna de cada sistema/subsistema:

| Elemento          | Paquete           | Convención de nombre                 | Tipo base                 |
|-------------------|-------------------|--------------------------------------|---------------------------|
| Controlador       | `..controller..`  | `<Entidad>Controller`                | —                         |
| Interfaz servicio | `..service..`     | `<Entidad>Service`                   | `ModelService<T>`         |
| Impl. servicio    | `..service.impl..`| `<Entidad>ServiceImpl`               | `DefaultModelService<T>`  |
| Repositorio       | `..db.repo..`     | `<Entidad>Repository` / `<E>Listener`| —                         |
| Entidad dominio   | `..db..`          | `<Entidad>` (generada)               | POJO                      |
| DTO               | `..service..`     | `<algo>DTO`                          | `record`                  |
| Módulo Guice      | `..module..`      | `<Subsistema>Module`                 | `AxelorModule`            |

---

# Categoría 1 — Dependencias entre capas

Modelo en capas con dependencia **solo ascendente**: una capa solo puede ser usada por las
capas superiores. Las reglas C1–C5 son las restricciones "negativas" individuales (mensajes
de fallo claros); C6 es la versión consolidada con `layeredArchitecture()`.

### C1 — `base.util` no depende de ningún otro paquete `com.educaflow`

`base/util` es la capa más baja: solo puede usar el JDK y librerías externas.

*Origen: `k-sistemas/SKILL.md` §"Reglas de dependencia".*

```java
@ArchTest
static final ArchRule c1_baseUtilNoDependeDeOtrosPaquetesEducaflow =
    noClasses()
        .that().resideInAPackage("com.educaflow.base.util..")
        .should().dependOnClassesThat(
            resideInAPackage("com.educaflow..")
                .and(DescribedPredicate.not(resideInAPackage("com.educaflow.base.util.."))))
        .because("base/util es la capa más baja: no puede depender de ningún otro paquete com.educaflow");
```

> **Estado actual:** ✅ CUMPLE. Ninguna clase de `base/util` importa de `com.educaflow` (15
> clases: `Convert`, `TextUtil`, `JsonUtil`, `MetaFileUtil`, `CryptoUtil`, `AllowProperties`…).

### C2 — `base.infrastructure` solo depende (dentro de educaflow) de `base.util`

`base/infrastructure` puede usar `base/util` y a sí misma, nunca `subsystem`, `system` ni
`secretariavirtual`.

*Origen: `k-sistemas/SKILL.md` §"Reglas de dependencia".*

```java
@ArchTest
static final ArchRule c2_baseInfrastructureSoloDependeDeBaseUtil =
    noClasses()
        .that().resideInAPackage("com.educaflow.base.infrastructure..")
        .should().dependOnClassesThat()
            .resideInAnyPackage(
                "com.educaflow.subsystem..",
                "com.educaflow.system..",
                "com.educaflow.secretariavirtual..")
        .because("base/infrastructure solo puede depender, dentro de com.educaflow, de base/util");
```

> **Estado actual:** ✅ CUMPLE. Ninguna clase de `base/infrastructure` importa de
> `subsystem`, `system` ni `secretariavirtual`.

### C3 — `subsystem` no depende de `system` ni de `secretariavirtual`

Un subsistema puede depender de `base/**` y de otros subsistemas (sin ciclos, ver C7), nunca
de un sistema ni del ensamblaje.

*Origen: `k-sistemas/SKILL.md` §"Reglas de dependencia".*

```java
@ArchTest
static final ArchRule c3_subsystemNoDependeDeSystemNiSecretariaVirtual =
    noClasses()
        .that().resideInAPackage("com.educaflow.subsystem..")
            .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
        .should().dependOnClassesThat()
            .resideInAnyPackage(
                "com.educaflow.system..",
                "com.educaflow.secretariavirtual..")
        .because("un subsystem nunca depende de un system ni del ensamblaje secretariavirtual");
```

> **Estado actual:** ✅ CUMPLE. No se detectan imports de `subsystem` hacia `system` ni
> `secretariavirtual`.

### C4 — `system` no depende de `secretariavirtual`

Un sistema puede depender de `base/**` y de subsistemas, nunca del ensamblaje (capa más alta).
La independencia entre sistemas la cubre C8.

*Origen: `k-sistemas/SKILL.md` §"Reglas de dependencia" + decisión: `secretariavirtual` es la capa más alta.*

```java
@ArchTest
static final ArchRule c4_systemNoDependeDeSecretariaVirtual =
    noClasses()
        .that().resideInAPackage("com.educaflow.system..")
            .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
        .should().dependOnClassesThat()
            .resideInAPackage("com.educaflow.secretariavirtual..")
        .because("un system nunca depende del ensamblaje secretariavirtual (capa más alta)");
```

> **Estado actual:** ✅ CUMPLE (no se detectan imports de `system` → `secretariavirtual`).

### C5 — `secretariavirtual` es la capa más alta: nadie depende de ella

`secretariavirtual` (menús, arranque, vistas globales) ensambla la aplicación. Ninguna otra
capa (`base`, `subsystem`, `system`) puede depender de ella.

*Origen: decisión de proyecto — `secretariavirtual` = capa de ensamblaje superior.*

```java
@ArchTest
static final ArchRule c5_secretariaVirtualNoEsAccedidaPorNadie =
    noClasses()
        .that().resideInAnyPackage(
                "com.educaflow.base..",
                "com.educaflow.subsystem..",
                "com.educaflow.system..")
            .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
        .should().dependOnClassesThat()
            .resideInAPackage("com.educaflow.secretariavirtual..")
        .because("secretariavirtual es la capa más alta: ninguna otra capa puede depender de ella");
```

> **Estado actual:** ✅ CUMPLE. Ninguna clase fuera de `secretariavirtual` importa de
> `secretariavirtual`.

### C6 — Arquitectura en capas consolidada (alternativa a C1–C5)

Regla única que define las 5 capas y la dirección permitida. Sustituye conceptualmente a
C1–C5 (úsala en lugar de ellas, o C1–C5 para mensajes de fallo más específicos). Los 3
paquetes exentos viven dentro de `subsystem`/`system`; se ignoran con `ignoreDependency`.

*Origen: `k-sistemas/SKILL.md` §"Reglas de dependencia".*

```java
@ArchTest
static final ArchRule c6_arquitecturaEnCapas =
    layeredArchitecture().consideringOnlyDependenciesInLayers()
        .layer("Util").definedBy("com.educaflow.base.util..")
        .layer("Infrastructure").definedBy("com.educaflow.base.infrastructure..")
        .layer("Subsystem").definedBy("com.educaflow.subsystem..")
        .layer("System").definedBy("com.educaflow.system..")
        .layer("SecretariaVirtual").definedBy("com.educaflow.secretariavirtual..")

        .whereLayer("Util").mayOnlyBeAccessedByLayers("Infrastructure", "Subsystem", "System", "SecretariaVirtual")
        .whereLayer("Infrastructure").mayOnlyBeAccessedByLayers("Subsystem", "System", "SecretariaVirtual")
        .whereLayer("Subsystem").mayOnlyBeAccessedByLayers("System", "SecretariaVirtual")
        .whereLayer("System").mayOnlyBeAccessedByLayers("SecretariaVirtual")
        .whereLayer("SecretariaVirtual").mayNotBeAccessedByAnyLayer()

        // Paquetes con arquitectura propia: se ignoran como origen y como destino.
        .ignoreDependency(resideInAPackage("..expedientes.."), DescribedPredicate.alwaysTrue())
        .ignoreDependency(DescribedPredicate.alwaysTrue(), resideInAPackage("..expedientes.."))
        .ignoreDependency(resideInAPackage("..tiposexpedientes.."), DescribedPredicate.alwaysTrue())
        .ignoreDependency(DescribedPredicate.alwaysTrue(), resideInAPackage("..tiposexpedientes.."))
        .ignoreDependency(resideInAPackage("..tramites.."), DescribedPredicate.alwaysTrue())
        .ignoreDependency(DescribedPredicate.alwaysTrue(), resideInAPackage("..tramites.."));
```

> **Notas:** `consideringOnlyDependenciesInLayers()` ignora automáticamente las dependencias a
> clases fuera de las capas (JDK, Axelor, Guice…). `ignoreDependency(origen, destino)` ignora
> una dependencia cuando AMBOS predicados se cumplen; usando `DescribedPredicate.alwaysTrue()`
> en uno de los lados se ignora el paquete exento tanto si es origen como si es destino.
>
> **Estado actual:** ✅ CUMPLE en las capas principales (mismo resultado que C1–C5).

### C7 — Sin ciclos entre subsistemas

No puede haber dependencias cíclicas entre subsistemas (`subsystem.A → subsystem.B → A`).

*Origen: `k-sistemas/SKILL.md` ("Lo que no puede haber son relaciones cíclicas") + `CLAUDE.md`.*

```java
@ArchTest
static final ArchRule c7_subsistemasSinCiclos =
    slices().matching("com.educaflow.subsystem.(*)..")
        .should().beFreeOfCycles()
        // 'expedientes' tiene arquitectura propia: se excluye del análisis de ciclos.
        .ignoreDependency(resideInAPackage("..expedientes.."), DescribedPredicate.alwaysTrue())
        .ignoreDependency(DescribedPredicate.alwaysTrue(), resideInAPackage("..expedientes.."));
```

> **Estado actual:** ❌ INCUMPLE (usar `freeze()`). Existe un ciclo
> `subsystem.common ↔ subsystem.importacion` por la relación JPA bidireccional
> `Centro.tareasImportacion` (en `common`) ↔ `TareaImportacion.centro` /
> `ResultadoImportacion.centro` (en `importacion`). El ciclo se congela como deuda conocida para
> no romper el build; arreglarlo más adelante (p.ej. eliminar la back-reference
> `Centro.tareasImportacion`) hará que la store se encoja. Es un bug del código respecto a la
> regla, no una excepción a ella.

### C8 — Los sistemas son independientes entre sí

Ningún `system` puede depender de otro `system` (cada uno se podría eliminar sin afectar a los
demás). Sí pueden depender de subsistemas.

*Origen: `k-sistemas/SKILL.md` §"Reglas de dependencia" ("Un sistema… Nunca de otro sistema").*

```java
@ArchTest
static final ArchRule c8_sistemasIndependientesEntreSi =
    slices().matching("com.educaflow.system.(*)..")
        .should().notDependOnEachOther()
        // 'tiposexpedientes' y 'tramites' tienen arquitectura propia: se excluyen.
        .ignoreDependency(resideInAPackage("..tiposexpedientes.."), DescribedPredicate.alwaysTrue())
        .ignoreDependency(DescribedPredicate.alwaysTrue(), resideInAPackage("..tiposexpedientes.."))
        .ignoreDependency(resideInAPackage("..tramites.."), DescribedPredicate.alwaysTrue())
        .ignoreDependency(DescribedPredicate.alwaysTrue(), resideInAPackage("..tramites.."));
```

> **Estado actual:** ✅ CUMPLE. No se detectan dependencias entre `actas`/`gestioncentro`
> (los demás sistemas están exentos).

---

# Categoría 2 — Estructura interna (Controller → Service → Repository)

Dentro de un sistema/subsistema la dependencia fluye Controller → Service → Repository →
Entidad. Estas reglas excluyen los `PAQUETES_EXENTOS` (estructura distinta).

### C9 — Un controlador no accede a un repositorio (pasa por el servicio)

La regla canónica: el controlador es el punto de entrada y delega TODA la lógica (incluido el
acceso a datos) en el servicio.

*Origen: `k-sistemas/controladores.md` §"NO lógica de negocio, I/O o acceso a BD en el controlador".*

```java
@ArchTest
static final ArchRule c9_controladorNoAccedeARepositorio =
    noClasses()
        .that().resideInAPackage("..controller..")
            .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
        .should().dependOnClassesThat().resideInAPackage("..db.repo..")
        .because("el controlador delega el acceso a datos en el servicio, nunca usa el repositorio directamente");
```

> **Estado actual:** ❌ INCUMPLE (aspiracional — usar `freeze()`).
> `com.educaflow.system.gestioncentro.controller.GestionCentroController` usa
> `JpaRepository.of(...)` / referencias a repositorio directamente. (Las violaciones de
> `expedientes` quedan fuera por estar exento.) Es un bug del código respecto a
> `controladores.md`, no una excepción a la regla.

### C10 — Un controlador no usa `com.axelor.db.JpaRepository`

Cargar entidades (`JpaRepository.of(X.class).find(id)`) es responsabilidad del servicio.

*Origen: `k-sistemas/controladores.md` §"NO lee entidades con `JpaRepository.of(...)`".*

```java
@ArchTest
static final ArchRule c10_controladorNoUsaJpaRepository =
    noClasses()
        .that().resideInAPackage("..controller..")
            .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
        .should().dependOnClassesThat()
            .haveFullyQualifiedName("com.axelor.db.JpaRepository")
        .because("cargar entidades es del servicio; el controlador no usa JpaRepository");
```

> **Estado actual:** ❌ INCUMPLE (aspiracional — usar `freeze()`).
> `com.educaflow.subsystem.firmas.controller.TareaFirmaController` hace
> `JpaRepository.of(TareaFirma.class).find(...)`. Bug del código respecto a `controladores.md`.

### C11 — Un repositorio no depende de servicios ni controladores

La dependencia es Controller → Service → Repository; nunca al revés.

*Origen: `k-sistemas/SKILL.md` §`db/` + `controladores.md`/`servicios.md` (capas).*

```java
@ArchTest
static final ArchRule c11_repositorioNoDependeDeServicioNiControlador =
    noClasses()
        .that().resideInAPackage("..db.repo..")
            .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
        .should().dependOnClassesThat()
            .resideInAnyPackage("..service..", "..controller..")
        .because("el repositorio es capa de datos: no conoce servicios ni controladores");
```

> **Estado actual:** ✅ CUMPLE. Ningún repositorio (`RegistroEntradaRepository`,
> `GestionCentroRepository`, `CentroRepository`, `RegistroPendienteRepository`…) importa de
> `service` ni `controller`.

### C12 — Un servicio no depende de un controlador

La dependencia es Controller → Service, nunca Service → Controller.

*Origen: `k-sistemas/controladores.md` ("el controlador llama al servicio").*

```java
@ArchTest
static final ArchRule c12_servicioNoDependeDeControlador =
    noClasses()
        .that().resideInAPackage("..service..")
            .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
        .should().dependOnClassesThat().resideInAPackage("..controller..")
        .because("la dependencia es Controller→Service, nunca Service→Controller");
```

> **Estado actual:** ✅ CUMPLE. Ningún servicio importa de `controller`.

### C13 — Las entidades de dominio son POJOs (no dependen de service/controller)

Las entidades de `..db..` (excluyendo `..db.repo..`) no llevan lógica de negocio: esa vive en
el servicio.

*Origen: `k-sistemas/SKILL.md` §`db/` ("lógica de negocio en el servicio, NO en la entidad").*

```java
@ArchTest
static final ArchRule c13_entidadesDominioSonPojos =
    noClasses()
        .that().resideInAPackage("..db..")
            .and().resideOutsideOfPackages("..db.repo..")
            .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
        .should().dependOnClassesThat()
            .resideInAnyPackage("..service..", "..controller..")
        .because("las entidades de dominio son POJOs; la lógica de negocio vive en el servicio");
```

> **Estado actual:** ⚠️ Previsiblemente CUMPLE (las entidades las genera Axelor desde los XML
> de `domains/` y son POJOs). Verificar al ejecutar; si alguna entidad con `extra-code`
> referencia un servicio, marcar con `freeze()`.

### C14 — `Beans.get(...)` prohibido en controladores y `*ServiceImpl`

El service-locator `com.axelor.inject.Beans` oculta dependencias. En esas capas se usa
inyección / `ModelServiceFactory`.

*Origen: `k-sistemas/controladores.md` §"NO obtener servicios con `Beans.get(...)`".*

```java
@ArchTest
static final ArchRule c14_noBeansGetEnControladorNiServiceImpl =
    noClasses()
        .that().resideInAnyPackage("..controller..", "..service.impl..")
            .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
        .should().dependOnClassesThat()
            .haveFullyQualifiedName("com.axelor.inject.Beans")
        .because("Beans.get es service-locator; se usa inyección / ModelServiceFactory");
```

> **Estado actual:** ❌ INCUMPLE (aspiracional — usar `freeze()`).
> `com.educaflow.subsystem.firmas.service.impl.TareaFirmaServiceImpl` usa
> `Beans.get(firmaNotifierClass)` (instanciación dinámica de un `TareaFirmaNotifier`). Bug del
> código respecto a `controladores.md`. (Nota: `base/util` queda fuera del sujeto a propósito:
> allí algún `Beans.get` de infraestructura puede ser legítimo.)

---

# Categoría 3 — Nomenclatura y ubicación

`ModelServiceFactory` descubre servicios por nombre/paquete exactos, así que estas reglas
protegen un contrato real, no solo estilo.

### C15 — Controladores: `..controller..` ⇔ `*Controller`

*Origen: `k-sistemas/controladores.md` §"un controlador por entidad" + `SKILL.md` §`controller/`.*

```java
@ArchTest
static final ArchRule c15a_clasesEnControllerTerminanEnController =
    classes()
        .that().resideInAPackage("..controller..")
            .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
        .should().haveSimpleNameEndingWith("Controller");

@ArchTest
static final ArchRule c15b_controllersResidenEnPaqueteController =
    classes()
        .that().haveSimpleNameEndingWith("Controller")
            .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
        .should().resideInAPackage("..controller..");
```

> **Estado actual:** ✅ CUMPLE. Los controladores (`CertificadoDigitalController`,
> `TareaFirmaController`, `GestionCentroController`, `LeyEducativaController`…) cumplen ambas
> direcciones. Corregido: `RegistroController` residía en `registrousuario.controllers`
> (plural); se movió a `registrousuario.controller` para satisfacer C15b.

### C16 — Impl. de servicio: `DefaultModelService` ⇒ `*ServiceImpl` en `..service.impl..`

`ModelServiceFactory` instancia por reflexión `<Entidad>ServiceImpl`; un nombre distinto hace
que `resolve(...)` devuelva `null` en runtime sin error de compilación.

*Origen: `k-sistemas/servicios.md` §"Descubrimiento automático".*

```java
@ArchTest
static final ArchRule c16_implServicioNombreYUbicacion =
    classes()
        .that().areAssignableTo(DefaultModelService.class)
            .and().doNotHaveFullyQualifiedName("com.axelor.db.modelservice.DefaultModelService")
            .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
        .should().haveSimpleNameEndingWith("ServiceImpl")
        .andShould().resideInAPackage("..service.impl..")
        .because("ModelServiceFactory descubre la impl por el nombre <Entidad>ServiceImpl en service.impl");
```

> **Estado actual:** ✅ CUMPLE. Las 10 impl. (`TareaFirmaServiceImpl`,
> `RegistroEntradaServiceImpl`, `CertificadoDigitalServiceImpl`…) están en `service.impl` y
> terminan en `ServiceImpl`.

### C17 — Interfaz de servicio: `ModelService` ⇒ `*Service` en `..service..`

*Origen: `k-sistemas/servicios.md` §"Estructura de la interfaz".*

```java
@ArchTest
static final ArchRule c17_interfazServicioNombreYUbicacion =
    classes()
        .that().areInterfaces()
            .and().areAssignableTo(ModelService.class)
            .and().doNotHaveFullyQualifiedName("com.axelor.db.modelservice.ModelService")
            .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
        .should().haveSimpleNameEndingWith("Service")
        .andShould().resideInAPackage("..service..")
        .because("la interfaz de servicio se llama <Entidad>Service y vive en service");
```

> **Estado actual:** ✅ CUMPLE. Las interfaces (`TareaFirmaService`, `RegistroEntradaService`,
> `LeyEducativaService`…) cumplen. Nota: `..service.impl..` es subpaquete de `..service..`, así
> que la ubicación es válida también para una interfaz auxiliar; la impl. la cubre C16.

### C18 — Repositorios: clases en `..db.repo..` terminan en `Repository` o `Listener`

*Origen: `k-sistemas/SKILL.md` §`db/`.*

```java
@ArchTest
static final ArchRule c18_repositoriosNombre =
    classes()
        .that().resideInAPackage("..db.repo..")
            .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
        .should().haveSimpleNameEndingWith("Repository")
        .orShould().haveSimpleNameEndingWith("Listener")
        .because("en db/repo solo hay repositorios (*Repository) y, excepcionalmente, listeners (*Listener)");
```

> **Estado actual:** ✅ CUMPLE. `RegistroEntradaRepository`, `RegistroSalidaRepository`,
> `GestionCentroRepository`, `CentroRepository`, `RegistroPendienteRepository`,
> `DispositivoCriptograficoRepository`, `NumeradorRepository`.

### C19 — Módulos Guice: `AxelorModule` ⇒ `*Module` en `..module..`

*Origen: `k-sistemas/SKILL.md` §`module/` + `k-guice`.*

```java
@ArchTest
static final ArchRule c19_modulosGuiceNombreYUbicacion =
    classes()
        .that().areAssignableTo(AxelorModule.class)
            .and().doNotHaveFullyQualifiedName("com.axelor.app.AxelorModule")
            .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
        .should().haveSimpleNameEndingWith("Module")
        .andShould().resideInAPackage("..module..")
        .because("los módulos Guice se llaman <Subsistema>Module y viven en module/");
```

> **Estado actual:** ✅ CUMPLE. Los módulos de subsistema/sistema cumplen
> (`SecurityModule`, `CriptografiaModule`, `RegistroModule`, `GestionModule` en `..module..`) y
> el módulo raíz del ensamblaje se movió de `startup` a `com.educaflow.secretariavirtual.module`
> (`SecretariaVirtualModule`), por lo que ya reside en `..module..`.

### C20 — DTOs: `*DTO` son `record` y residen en `..service..`

*Origen: `k-sistemas/servicios.md` §"DTO de inserción".*

```java
@ArchTest
static final ArchRule c20_dtosSonRecordsEnService =
    classes()
        .that().haveSimpleNameEndingWith("DTO")
            .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
        .should().beAssignableTo(Record.class)   // todo record extiende java.lang.Record
        .andShould().resideInAPackage("..service..")
        .because("los DTOs del proyecto son records de Java y viven junto a la interfaz del servicio");
```

> **Estado actual:** ✅ CUMPLE. `RegistroEntradaInsertDTO`, `RegistroSalidaInsertDTO`,
> `TareaFirmaInsertDTO` son records en `service`.
>
> *(Nota API: ArchUnit 1.4.2 ofrece el predicado `.areRecords()` para el lado `.that()`; para
> la condición `.should()` se usa `beAssignableTo(Record.class)`, que es equivalente y
> robusto.)*

---

# Categoría 4 — Inyección de dependencias (Guice)

### C21 — Un `ModelService` nunca se inyecta con `@Inject`

Los `ModelService` se resuelven con `ModelServiceFactory.resolve(Entidad.class)`, nunca con
`@Inject`. Inyectarlos los registraría dos veces y rompería la factoría.

*Origen: `k-sistemas/servicios.md` §"Obtener otro servicio" + `controladores.md` §`ModelServiceFactory`.*

```java
@ArchTest
static final ArchRule c21_modelServiceNoSeInyecta =
    noFields()
        .that().haveRawType(assignableTo(ModelService.class))
        .should().beAnnotatedWith("com.google.inject.Inject")
        .orShould().beAnnotatedWith("jakarta.inject.Inject")
        .as("Ningún campo de tipo ModelService debe llevar @Inject (se usa ModelServiceFactory)")
        .allowEmptyShould(true);
```

> **Estado actual:** ✅ CUMPLE. Controladores y servicios inyectan `ModelServiceFactory` y
> resuelven los servicios en el método; no hay campos `@Inject` de tipo `ModelService`.
> `allowEmptyShould(true)` evita que la regla falle si no existe ningún campo de ese tipo.

---

# Categoría 5 — Reglas genéricas de higiene

Solo se incluyen reglas genéricas **seguras** para este proyecto.

### C22 — No acceder a los streams estándar (`System.out` / `System.err`)

Se usa logging, no salida por consola.

*Origen: `GeneralCodingRules` (higiene); CLAUDE.md §logging.*

```java
@ArchTest
static final ArchRule c22_noStreamsEstandar =
    GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
```

> **Estado actual:** ❌ INCUMPLE (aspiracional — usar `freeze()`). Hay ~31 usos de
> `System.out`, 1 de `System.err` y 1 `printStackTrace()` en `base/infrastructure`
> (`CertificateViewer`, `BulkTables`, `XMLUtil`, `DocumentoPdfImplIText`…) y algún
> `EventManager`. Limpiar progresivamente o congelar.
>
> *(Nota API: la constante predefinida no admite `.and()` para aplicar `PAQUETES_EXENTOS`. Si
> se necesita excluir paquetes, reescribir como `noClasses().that(resideOutsideOfPackages(...))
> .should(...)` con una condición de acceso a streams personalizada, o envolver en `freeze()`.)*

---

# Reglas genéricas deliberadamente NO incluidas

Estas reglas de `GeneralCodingRules` **chocan con prácticas documentadas** del proyecto; usarlas
generaría falsos positivos masivos:

- `NO_CLASSES_SHOULD_USE_FIELD_INJECTION` — el proyecto **usa** inyección por campo (`@Inject`
  en campos de controladores y servicios, p.ej. `ModelServiceFactory`). Es el patrón correcto
  aquí, no un antipatrón.
- `NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS` — `controladores.md` indica relanzar errores
  técnicos no esperados como `RuntimeException`. Esta regla prohíbe `RuntimeException`, así que
  contradice el patrón.

# Fuera del alcance de ArchUnit

Las reglas de `k-secure-coding` son mayormente **de comportamiento**, no estructurales, y no se
pueden verificar con ArchUnit (haría falta análisis de flujo de datos). Se citan como
recordatorio, sin regla automatizable:

- Mass-assignment / `AllowProperties`: qué campos puede dictar el cliente por acción.
- Asignación incondicional de campos `servidor` en `*ServiceImpl.insert/update`.
- Autorización multicentro / IDOR (cada centro solo ve su información).
- Validación de adjuntos, inyección JPQL/SQL, log injection, manejo de secretos.

Para estas, la defensa vive en revisiones con `k-secure-coding` y en los tests E2E
(`/sdd-debug-with-test-e2e-desc`), no en ArchUnit.
