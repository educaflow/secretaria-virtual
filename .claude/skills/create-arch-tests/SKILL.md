---
name: create-arch-tests
description: >-
  Dado el catálogo de reglas de arquitectura `agent_docs/architecture-rules.md`
  (reglas `C-NNN` con su snippet `@ArchTest` ya escrito para este proyecto),
  genera las clases de test JUnit 5 + ArchUnit que verifican esas reglas,
  organizadas en sub-paquetes por categoría bajo un único paquete raíz
  `com.educaflow.architecture` en `src/test/java`. El paquete generado es una
  PROYECCIÓN PURA del markdown: para cambiar un test se edita
  `architecture-rules.md` y se vuelve a ejecutar este skill, nunca se editan los
  `.java` a mano. Carga `/k-archunit` como referencia de ArchUnit.
allowed-tools: Read, Write, Edit, Bash, Skill
---

# create-arch-tests

Asumes el rol de **generador de tests de arquitectura**: transformas el catálogo
de reglas `agent_docs/architecture-rules.md` en clases de test JUnit 5 + ArchUnit
dentro de `src/test/java`, organizadas en sub-paquetes por categoría bajo un único
paquete raíz. No inventas reglas: copias literalmente los snippets `@ArchTest` del
catálogo y los repartes en clases. El catálogo es la **única fuente de verdad**.

---

## User Input

```text

```

You **MUST** consider the user input before proceeding (if not empty). Argumentos
posibles (todos opcionales; en uso normal no se pasa ninguno):

- `--in=<ruta>` — catálogo de entrada (por defecto `agent_docs/architecture-rules.md`).
- `--out=<ruta>` — carpeta raíz de tests (por defecto `src/test/java`).
- `--root-package=<fqn>` — paquete raíz (por defecto `com.educaflow.architecture`).

---

## Outline

1. **Cargar** el contrato (`/k-archunit`) y leer el catálogo de entrada. (Fase 0)
2. **Parsear** el catálogo: imports, `PAQUETES_EXENTOS`, categorías y reglas. (Fase 1)
3. **Resolver** la estructura de paquetes y clases (un sub-paquete por categoría). (Fase 2)
4. **Generar** las clases de test (proyección pura del catálogo). (Fase 3)
5. **Verificar** que compila y reportar al usuario. (Fase 4)

**STOP conditions**:

- No existe el fichero de entrada → **ERROR** y detente.
- El catálogo no contiene **ningún** bloque ```java con `@ArchTest` bajo un encabezado `# Categoría` → **ERROR** (entrada no válida).
- El paquete raíz ya contiene ficheros **sin** la cabecera `GENERADO` (editados a mano) → **STOP** y pregunta antes de sobrescribir.
- La dependencia `archunit-junit5` no está en `build.gradle` y el usuario no autoriza añadirla → **STOP**.

---

## 1. Entrada y salida

### 1.1 Entrada

- `agent_docs/architecture-rules.md` — catálogo de reglas. Cada regla `C-NNN` trae,
  bajo un encabezado `# Categoría N — <título>`, un bloque ```java con uno o más
  campos `@ArchTest static final ArchRule …` listos, su `Origen` y un bloque
  `> **Estado actual:**` con `✅` / `⚠️` / `❌ INCUMPLE`.
- `/k-archunit` — referencia de ArchUnit (API, freezing, JUnit 5). Se carga con `Skill`.

### 1.2 Salida

Clases de test JUnit 5 + ArchUnit bajo un único paquete raíz, una clase por categoría:

```
src/test/java/com/educaflow/architecture/
    <subpaquete-categoria-1>/<Titulo1>Test.java
    <subpaquete-categoria-2>/<Titulo2>Test.java
    …
```

### 1.3 Estructura de carpetas

- **Raíz única**: `com.educaflow.architecture` (configurable con `--root-package`).
- **Un sub-paquete + una clase por categoría** del catálogo.
- Cada clase es **autocontenida**: declara sus propios imports y su propia
  constante `PAQUETES_EXENTOS` (copiados del catálogo), sin clases base compartidas.

---

## 2. Principios

### 2.1 Proyección pura del catálogo

- El paquete raíz generado **MUST** ser una proyección exacta de `architecture-rules.md`:
  una regla del catálogo ⇒ un `@ArchTest` generado; una regla borrada del catálogo ⇒
  el `@ArchTest` desaparece al regenerar.
- **MUST NOT** editar a mano los `.java` generados. Para cambiar un test: edita el
  catálogo y vuelve a ejecutar `/create-arch-tests`.
- Cada fichero generado **MUST** llevar la cabecera `GENERADO` (§6.1).
- Antes de generar, **MUST** vaciar el paquete raíz (borrar los `.java` con cabecera
  `GENERADO`) para que el resultado sea solo lo que dice el catálogo hoy.

### 2.2 Copia literal de los snippets

- Los campos `@ArchTest static final ArchRule …` se copian **literalmente** del catálogo.
- **MUST NOT** reescribir, "mejorar" ni renombrar la lógica de una regla: si está mal,
  se arregla en el catálogo, no aquí.
- La única transformación permitida sobre el cuerpo de una regla es **envolverla en
  `FreezingArchRule.freeze(...)`** cuando su estado es `❌ INCUMPLE` (§6.3).

---

## 3. Fase 0 — Cargar contrato y entrada

1. Carga `/k-archunit` con `Skill` (referencia de ArchUnit, freezing, JUnit 5).
2. Resuelve el fichero de entrada (`--in=` o `agent_docs/architecture-rules.md`) y léelo.
   - Si no existe → **ERROR** y detente.
3. Verifica que `build.gradle` tiene `com.tngtech.archunit:archunit-junit5`.
   - Si falta: pregunta al usuario si añadirla; si autoriza, `Edit` en `build.gradle`
     (`testImplementation 'com.tngtech.archunit:archunit-junit5:1.4.2'`); si no → **STOP**.

---

## 4. Fase 1 — Parsear el catálogo

**Contrato de parseo** — solo se generan tests de lo que cumple TODAS estas condiciones:

1. **MUST** procesar únicamente bloques ```java que contengan `@ArchTest` y estén bajo
   un encabezado `# Categoría N — <título>`.
2. **MUST** ignorar por completo las secciones `# Reglas genéricas deliberadamente NO
   incluidas` y `# Fuera del alcance de ArchUnit`.
3. **MUST** excluir las reglas marcadas como redundantes en su propio texto —las que
   dicen `(alternativa a …)` o `Sustituye conceptualmente a …`— para no duplicar
   verificación. Anótalas en el reporte como "excluida por redundancia".
   - ✅ Incluir `C1`–`C5` (mensajes de fallo específicos).
   - ❌ Excluir `C6` (consolidada, marcada "alternativa a C1–C5").
4. Extrae una sola vez, del preámbulo del catálogo:
   - El **bloque de imports** (`### Imports usados por el catálogo`).
   - La constante **`PAQUETES_EXENTOS`** (`## Cómo se usan estas reglas`).

Por cada regla incluida, registra: `id` (del encabezado `### C-NNN — …`), el/los
campos `@ArchTest` del bloque (puede haber 2, p.ej. `C15a`/`C15b` en una misma regla),
la categoría a la que pertenece, y su **Estado actual** (`✅` / `⚠️` / `❌ INCUMPLE`).

---

## 5. Fase 2 — Resolver estructura de paquetes

1. **Paquete raíz**: `--root-package=` o `com.educaflow.architecture`. Carpeta destino:
   `<out>/<root-package con / >/`.
2. **Un sub-paquete + una clase por categoría**. Deriva ambos del título de la categoría
   (texto tras `— ` y antes del primer `(` si lo hay):
   - **Sub-paquete** = título en minúsculas, sin acentos, solo `[a-z0-9]` (se eliminan
     espacios y signos).
   - **Clase** = título en PascalCase (sin acentos ni signos) + `Test`.
3. Ejemplos de derivación:
   - ✅ `Categoría 1 — Dependencias entre capas` → paquete `dependenciasentrecapas`, clase `DependenciasEntreCapasTest`
   - ✅ `Categoría 2 — Estructura interna (Controller → Service → Repository)` → paquete `estructurainterna`, clase `EstructuraInternaTest` (se corta en el `(`)
   - ✅ `Categoría 3 — Nomenclatura y ubicación` → paquete `nomenclaturayubicacion`, clase `NomenclaturaYUbicacionTest`
   - ❌ `dependencias-entre-capas` (los guiones no son válidos en un nombre de paquete Java)
   - ❌ `DependenciasEntreCapas` como paquete (un paquete va en minúsculas)

---

## 6. Fase 3 — Generar las clases de test

1. **Vacía** el paquete raíz: borra los `.java` que lleven la cabecera `GENERADO`
   (§2.1). Si hay `.java` **sin** esa cabecera → **STOP** (§Outline).
2. Por cada categoría, **escribe una clase** con la plantilla §6.1.
3. Copia en la clase, **literalmente**, los campos `@ArchTest` de sus reglas (§2.2),
   en el orden del catálogo, envolviendo en `freeze(...)` las `❌ INCUMPLE` (§6.3).

### 6.1 Plantilla de clase generada

````
// =====================================================================
// GENERADO por /create-arch-tests desde agent_docs/architecture-rules.md
// NO EDITAR A MANO. Para cambiar un test, edita architecture-rules.md y
// vuelve a ejecutar /create-arch-tests.
// =====================================================================
package com.educaflow.architecture.<subpaquete>;

<bloque de imports del catálogo, literal>
<+ si la clase tiene alguna regla frozen:>
import com.tngtech.archunit.library.freeze.FreezingArchRule;

@AnalyzeClasses(
    packages = "com.educaflow",
    importOptions = ImportOption.DoNotIncludeTests.class)
class <Titulo>Test {

    private static final String[] PAQUETES_EXENTOS = {
        "..expedientes..", "..tiposexpedientes..", "..tramites.."
    };

    <campos @ArchTest de las reglas de esta categoría, literales del catálogo>
}
````

**MUST**: clase y campos en visibilidad de paquete (sin `public`), igual que el
catálogo — JUnit 5 + ArchUnit no requieren `public`.

### 6.2 Estado actual: qué hacer con cada marca

| Estado en el catálogo | Acción al generar |
|-----------------------|-------------------|
| `✅` CUMPLE           | Generar la regla tal cual. |
| `⚠️` (dudoso/parcial) | Generar tal cual **y** listarla en el reporte como "puede fallar al ejecutar". |
| `❌ INCUMPLE`         | Envolver en `FreezingArchRule.freeze(...)` (§6.3) para que el build siga verde. |

### 6.3 Envolver una regla en `freeze`

Para una regla `❌ INCUMPLE`, transforma la asignación envolviendo la expresión de la
regla en `FreezingArchRule.freeze(...)` y añade un comentario:

- ✅ CORRECTO:
  ```java
  // frozen: incumplimiento conocido (ver "Estado actual" en architecture-rules.md)
  @ArchTest
  static final ArchRule c9_controladorNoAccedeARepositorio =
      FreezingArchRule.freeze(
          noClasses()
              .that().resideInAPackage("..controller..")
                  .and().resideOutsideOfPackages(PAQUETES_EXENTOS)
              .should().dependOnClassesThat().resideInAPackage("..db.repo..")
              .because("…"));
  ```
- ❌ INCORRECTO: dejar la regla `❌ INCUMPLE` sin `freeze` (rompe el build en la 1ª ejecución).
- ❌ INCORRECTO: editar la lógica de la regla para que "pase" (falsea la arquitectura; el incumplimiento es un bug del código, no de la regla).

> El freezing guarda las violaciones conocidas en un store la primera vez (ver
> `/k-archunit` → `reference.md` §Freezing). El build queda verde y solo falla ante
> **nuevas** violaciones.

---

## 7. Fase 4 — Verificar y reportar

1. Compila los tests: `./gradlew compileTestJava`.
2. Si falla la compilación, corrige y recompila. **LIMIT**: máximo 3 iteraciones; si
   tras la 3ª sigue sin compilar, **STOP** y muestra el error al usuario.
3. Aplica el checklist §8. **MUST NOT** dar por terminado si queda algún punto sin cumplir.
4. Reporta al usuario, escueto:
   - Clases generadas (ruta) y nº de reglas por clase.
   - Reglas excluidas por redundancia.
   - Reglas `❌` envueltas en `freeze` y reglas `⚠️` que pueden fallar al ejecutar.

> Compilar (`compileTestJava`) verifica que el código generado es válido. **Ejecutar**
> las reglas es `./gradlew test` (lo hace el build normal del proyecto, ver CLAUDE.md).

---

## 8. Checklist final

- [ ] ¿Cada categoría del catálogo (salvo las excluidas por redundancia) tiene su clase bajo el paquete raíz único?
- [ ] ¿Todos los campos `@ArchTest` se copiaron **literalmente** del catálogo (salvo el `freeze` de las `❌`)?
- [ ] ¿Cada fichero generado lleva la cabecera `GENERADO`?
- [ ] ¿Las reglas `❌ INCUMPLE` están envueltas en `FreezingArchRule.freeze(...)` y solo esas?
- [ ] ¿Se excluyeron las secciones `NO incluidas` / `Fuera del alcance` y las reglas marcadas "alternativa a …"?
- [ ] ¿Los nombres de sub-paquete son `[a-z0-9]` y las clases PascalCase + `Test`?
- [ ] ¿`./gradlew compileTestJava` pasa?

**LIMIT**: máximo 3 iteraciones de corrección sobre este checklist; si tras la 3ª
siguen fallando ítems, documenta lo que queda y avisa al usuario.

---

## Quick Guidelines

- El catálogo `agent_docs/architecture-rules.md` es la **única fuente de verdad**: para cambiar un test, edita el markdown y re-ejecuta; **MUST NOT** editar los `.java`.
- Copia los `@ArchTest` **literalmente**; la única transformación es `freeze(...)` para las `❌ INCUMPLE`.
- Un único paquete raíz (`com.educaflow.architecture`), un sub-paquete + una clase por categoría; clases autocontenidas (imports + `PAQUETES_EXENTOS` propios).
- Vacía el paquete raíz antes de regenerar; **STOP** si encuentras `.java` editados a mano (sin cabecera `GENERADO`).
- Excluye las secciones `NO incluidas` / `Fuera del alcance` y las reglas marcadas "alternativa a …".
- Carga `/k-archunit` como referencia; verifica con `./gradlew compileTestJava` (**LIMIT** 3 iteraciones).

---

## Apéndice A — Override de rutas (para testing)

- `--in=<ruta>` — catálogo de entrada explícito (por defecto `agent_docs/architecture-rules.md`).
- `--out=<ruta>` — carpeta raíz de tests (por defecto `src/test/java`).
- `--root-package=<fqn>` — paquete raíz (por defecto `com.educaflow.architecture`).

En uso normal no se especifican.
