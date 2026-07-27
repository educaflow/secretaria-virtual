---
name: developer-create-arch-tests
description: >-
  Dado el catálogo de reglas de arquitectura `agent_docs/architecture-rules.md`
  (decisiones estilo ADR: cada regla `C-N` con Decisión, Verificación —sujeto,
  condición, exenciones, mensaje— y Cumplimiento, SIN código ArchUnit), genera las
  clases de test JUnit 5 + ArchUnit que verifican esas reglas escribiendo el código
  ArchUnit a partir del bloque Verificación de cada regla, organizadas en
  sub-paquetes por categoría bajo un único paquete raíz
  `com.educaflow.architecture` en `src/test/java`. El paquete generado es una
  PROYECCIÓN del markdown: para cambiar un test se edita `architecture-rules.md` y
  se vuelve a ejecutar este skill, nunca se editan los `.java` a mano.
  La regeneración es INCREMENTAL: cada test lleva citada literal la Verificación de su regla
  y el skill empareja regla↔test por ese contenido (no por el ID, que se renumera),
  tocando solo lo que cambió. Carga `/k-archunit` como referencia de ArchUnit.
allowed-tools: Read, Write, Edit, Bash, Skill
---

# developer-create-arch-tests

Asumes el rol de **generador de tests de arquitectura**: transformas el catálogo
declarativo `agent_docs/architecture-rules.md` en clases de test JUnit 5 + ArchUnit
dentro de `src/test/java`, organizadas en sub-paquetes por categoría bajo un único
paquete raíz. El catálogo **no trae código**: tú escribes cada `@ArchTest` traduciendo
**fielmente** el bloque *Verificación* de cada regla a la API de ArchUnit
(`/k-archunit`). El catálogo es la **única fuente de verdad** sobre *qué* se verifica;
este skill solo decide *cómo* expresarlo en la API. No inventas reglas.

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
2. **Parsear** el catálogo: convenciones globales, categorías y reglas. (Fase 1)
3. **Resolver** la estructura de paquetes y clases (un sub-paquete por categoría). (Fase 2)
4. **Reconciliar y generar**: emparejar reglas↔tests existentes por contenido y tocar solo lo que cambió, traduciendo a ArchUnit lo nuevo o modificado. (Fase 3)
5. **Verificar** que compila y reportar al usuario. (Fase 4)

**STOP conditions**:

- No existe el fichero de entrada → **ERROR** y detente.
- El catálogo no contiene **ninguna** regla `### C-N — …` con bloque `**Verificación.**`
  bajo un encabezado `# Categoría` → **ERROR** (entrada no válida).
- El paquete raíz ya contiene ficheros **sin** la cabecera `GENERADO` (editados a mano) → **STOP** y pregunta antes de sobrescribir.
- La dependencia `archunit-junit5` no está en `build.gradle` y el usuario no autoriza añadirla → **STOP**.

---

## 1. Entrada y salida

### 1.1 Entrada

- `agent_docs/architecture-rules.md` — catálogo declarativo estilo ADR, sin código.
  Aporta:
  - Una sección global `## Convenciones de verificación` con el **ámbito de análisis**,
    los **paquetes exentos** y el significado de las marcas de cumplimiento.
  - Reglas `### C-N — <título>` agrupadas bajo encabezados `# Categoría N — <título>`.
    Cada regla trae `**Decisión.**`, `**Verificación.**` (sujeto, condición,
    exenciones, mensaje y notas como la vacuidad) y `**Cumplimiento.**`
    (`✅` / `⚠️` / `❌ INCUMPLE`).
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
- Cada clase es **autocontenida**: declara sus propios imports y su propia constante
  `PAQUETES_EXENTOS` (derivada de las convenciones del catálogo), sin clases base
  compartidas.

---

## 2. Principios

### 2.1 Proyección del catálogo

- El paquete raíz generado **MUST** ser una proyección exacta de `architecture-rules.md`:
  una regla del catálogo ⇒ sus `@ArchTest` generados; una regla borrada del catálogo ⇒
  su `@ArchTest` se borra al reconciliar.
- **MUST NOT** editar a mano los `.java` generados. Para cambiar un test: edita el
  catálogo y vuelve a ejecutar `/developer-create-arch-tests`.
- Cada fichero generado **MUST** llevar la cabecera `GENERADO` (§6.1).
- La regeneración es **INCREMENTAL** (§6.4):
  **MUST NOT** vaciar el paquete ni re-traducir reglas que no cambiaron;
  un test cuya regla sigue igual queda **byte a byte idéntico**.
  Las clases existentes se modifican con `Edit` quirúrgico (solo los campos afectados), nunca reescribiéndolas enteras.
- **CRITICAL — la identidad de una regla es su contenido, NO su ID**:
  el catálogo renumera sin huecos al borrar o mover reglas,
  así que el emparejamiento regla↔test se hace por el bloque *Verificación* (citado literal encima de cada test, §6.1)
  y el ID es solo una etiqueta a sincronizar.

### 2.2 Traducción fiel de la Verificación

- Cada regla produce un campo `@ArchTest static final ArchRule c<N>_<resumenCamelCase>`
  — o varios, si la propia regla define sub-comprobaciones (p.ej. `C15a`/`C15b` ⇒ dos
  campos).
- **MUST** implementar exactamente el sujeto, la condición y las exenciones del bloque
  *Verificación*. **MUST NOT** debilitar, reforzar ni "mejorar" una regla: si parece
  mal, se corrige en el catálogo, no aquí.
- **CRITICAL — estabilidad del freezing**: el *Mensaje* de la regla se usa **literal**
  (como `.because(...)`, o como descripción con `.as(...)` cuando la regla lo indica).
  La descripción resultante es la clave de la violation store
  (`src/test/resources/archunit_store`); cambiar la lógica o el texto re-baseliza las
  violaciones de esa regla.
- Si la *Verificación* indica que la regla se cumple en vacío (sujeto sin elementos),
  **MUST** generar la regla de forma que no falle por vacuidad (`allowEmptyShould`).
- Las exenciones "como origen y destino" de las reglas de slices/ciclos **MUST**
  ignorar la dependencia cuando el paquete exento aparece en **cualquiera** de los dos
  lados.

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

1. **MUST** procesar únicamente las reglas `### C-N — <título>` que estén bajo un
   encabezado `# Categoría N — <título>` y tengan bloque `**Verificación.**`.
2. **MUST** ignorar por completo las secciones `# Reglas genéricas deliberadamente NO
   incluidas` y `# Fuera del alcance de ArchUnit`.
3. **MUST** ignorar los identificadores marcados como **retirados** en el catálogo
   (p.ej. una nota "el identificador `C6` está retirado"): no generan test ni cuentan
   como hueco de numeración.
4. Extrae una sola vez, de `## Convenciones de verificación`:
   - El **ámbito de análisis** (paquete base, exclusión de tests) → `@AnalyzeClasses`.
   - Los **paquetes exentos** → constante `PAQUETES_EXENTOS`.

Por cada regla incluida, registra: `id` (del encabezado `### C-N — …`), el título, el
bloque *Verificación* completo (sujeto, condición, exenciones, mensaje, notas), la
categoría a la que pertenece y su **Cumplimiento** (`✅` / `⚠️` / `❌ INCUMPLE`).

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

## 6. Fase 3 — Reconciliar y generar las clases de test

1. Si hay `.java` **sin** cabecera `GENERADO` bajo el paquete raíz → **STOP** (§Outline).
2. **Reconcilia** el catálogo con los tests existentes (§6.4):
   clasifica cada regla/test en INTACTO, RENOMBRAR, REGENERAR, NUEVO o BORRAR.
3. Aplica el plan:
   crea las clases de categorías nuevas con la plantilla §6.1;
   modifica las existentes con `Edit` quirúrgico (solo los campos afectados);
   borra las clases cuya categoría desapareció del catálogo.
4. Todo campo `@ArchTest` nuevo o regenerado traduce su *Verificación* (§2.2),
   lleva su cita literal (§6.1), respeta el orden del catálogo
   y se envuelve en `freeze(...)` si su regla es `❌ INCUMPLE` (§6.3).

### 6.1 Plantilla de clase generada

````
// =====================================================================
// GENERADO por /developer-create-arch-tests desde agent_docs/architecture-rules.md
// NO EDITAR A MANO. Para cambiar un test, edita architecture-rules.md y
// vuelve a ejecutar /developer-create-arch-tests.
// =====================================================================
package com.educaflow.architecture.<subpaquete>;

<imports que necesite el código generado de esta clase>

@AnalyzeClasses(
    packages = "com.educaflow",
    importOptions = ImportOption.DoNotIncludeTests.class)
class <Titulo>Test {

    private static final String[] PAQUETES_EXENTOS = {
        <paquetes exentos de las Convenciones de verificación>
    };

    <campos @ArchTest de las reglas de esta categoría>
}
````

**MUST**: clase y campos en visibilidad de paquete (sin `public`) — JUnit 5 + ArchUnit
no requieren `public`.

**REQUIRED — cita literal de la Verificación**: encima de cada campo `@ArchTest` va el bloque *Verificación.* de su regla citado **literal** del catálogo, línea a línea, con el marcador exacto `// [C-N] Verificación:` y prefijo `//   `. Esta cita es la clave del emparejamiento incremental (§6.4).

```java
// [C-9] Verificación:
//   <líneas del bloque Verificación de C-9, copiadas literales del catálogo>
@ArchTest
static final ArchRule c9_controladorNoAccedeARepositorio = …;
```

### 6.2 Cumplimiento: qué hacer con cada marca

| Cumplimiento en el catálogo | Acción al generar |
|-----------------------------|-------------------|
| `✅` CUMPLE                 | Generar la regla tal cual. |
| `⚠️` (dudoso/parcial)       | Generar tal cual **y** listarla en el reporte como "puede fallar al ejecutar". |
| `❌ INCUMPLE`               | Envolver en `FreezingArchRule.freeze(...)` (§6.3) para que el build siga verde. |

### 6.3 Envolver una regla en `freeze`

Para una regla `❌ INCUMPLE`, envuelve la expresión de la regla en
`FreezingArchRule.freeze(...)` y añade un comentario:

- ✅ CORRECTO:
  ```java
  // frozen: incumplimiento conocido (ver "Cumplimiento" en architecture-rules.md)
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
- ❌ INCORRECTO: cambiar la lógica de la regla para que "pase" (falsea la arquitectura; el incumplimiento es un bug del código, no de la regla).

> El freezing guarda las violaciones conocidas en la store versionada
> `src/test/resources/archunit_store` (ver `/k-archunit` → freezing). El build queda
> verde y solo falla ante **nuevas** violaciones.

### 6.4 Reconciliación incremental

Empareja las reglas del catálogo con los tests existentes en **tres pasadas ordenadas** (cada elemento emparejado sale del juego):

1. **Por contenido** — la cita `// [C-N] Verificación:` de un test coincide con el bloque *Verificación* de una regla
   (comparación literal línea a línea, ignorando espacios finales y las apariciones del propio ID, que cambian al renumerar).
   Si varias reglas/tests coinciden entre sí, prioriza el emparejamiento de mismo ID.
   - Mismo ID → **INTACTO**: **MUST NOT** tocar el campo.
   - Distinto ID o distinta categoría (regla renumerada o movida) → **RENOMBRAR**:
     cambio mecánico del nombre del campo, del ID de la cita y del *Mensaje* si contiene el ID;
     mover de clase si cambió de categoría. **MUST NOT** re-traducir la expresión ArchUnit.
2. **Por ID** — regla y test aún sin pareja con el mismo `C-N`: la *Verificación* cambió →
   **REGENERAR** solo ese campo (nueva traducción §2.2 + nueva cita).
3. **Resto** — regla sin pareja → **NUEVO**; test sin pareja → **BORRAR**.

Casos especiales:

- **Test sin cita (legacy)**: generado por una versión anterior del skill.
  Compara su código con la regla de su ID:
  si implementa fielmente la *Verificación* → añade la cita y **MUST NOT** tocar el cuerpo; si no → REGENERAR.
- **Primera generación** (no existe el paquete raíz) → todo es NUEVO.
- **Cambio en `## Convenciones de verificación`** (ámbito de análisis o paquetes exentos):
  invalida la traducción de **todos** los tests →
  actualiza `PAQUETES_EXENTOS`/`@AnalyzeClasses` en todas las clases y
  re-evalúa cada test contra su regla (el que siga siendo traducción fiel queda INTACTO).
- **Freezing**: RENOMBRAR o REGENERAR una regla frozen cambia su descripción →
  su entrada en la violation store se re-baseliza y pueden quedar entradas huérfanas en `archunit_store`.
  **MUST** listarlas en el reporte (§7).

---

## 7. Fase 4 — Verificar y reportar

1. Compila los tests: `./gradlew compileTestJava`.
2. Si falla la compilación, corrige y recompila. **LIMIT**: máximo 3 iteraciones; si
   tras la 3ª sigue sin compilar, **STOP** y muestra el error al usuario.
3. Aplica el checklist §8. **MUST NOT** dar por terminado si queda algún punto sin cumplir.
4. Reporta al usuario, escueto:
   - Resumen de la reconciliación: nº de reglas INTACTAS / RENOMBRADAS / REGENERADAS / NUEVAS / BORRADAS (§6.4).
   - Clases generadas o modificadas (ruta) y nº de reglas por clase.
   - Reglas ignoradas (retiradas / sin Verificación).
   - Reglas `❌` envueltas en `freeze` y reglas `⚠️` que pueden fallar al ejecutar.
   - Reglas frozen RENOMBRADAS o REGENERADAS: avisa de que su entrada en la violation
     store se re-baseliza (pueden quedar entradas huérfanas en `archunit_store`).

> Compilar (`compileTestJava`) verifica que el código generado es válido. **Ejecutar**
> las reglas es `./gradlew test` (lo hace el build normal del proyecto, ver CLAUDE.md).

---

## 8. Checklist final

- [ ] ¿Cada categoría del catálogo tiene su clase bajo el paquete raíz único?
- [ ] ¿Cada `@ArchTest` implementa **exactamente** el sujeto, la condición y las exenciones de la *Verificación* de su regla, con el *Mensaje* literal?
- [ ] ¿Cada fichero generado lleva la cabecera `GENERADO`?
- [ ] ¿Cada `@ArchTest` lleva su cita `// [C-N] Verificación:` idéntica al catálogo actual?
- [ ] ¿Los tests INTACTOS quedaron byte a byte iguales (el diff solo toca lo reconciliado)?
- [ ] ¿Las reglas `❌ INCUMPLE` están envueltas en `FreezingArchRule.freeze(...)` y solo esas?
- [ ] ¿Se excluyeron las secciones `NO incluidas` / `Fuera del alcance` y los identificadores retirados?
- [ ] ¿Los nombres de sub-paquete son `[a-z0-9]` y las clases PascalCase + `Test`?
- [ ] ¿`./gradlew compileTestJava` pasa?

**LIMIT**: máximo 3 iteraciones de corrección sobre este checklist; si tras la 3ª
siguen fallando ítems, documenta lo que queda y avisa al usuario.

---

## Quick Guidelines

- El catálogo `agent_docs/architecture-rules.md` es la **única fuente de verdad** y **no trae código**: tú traduces cada bloque *Verificación* a la API de ArchUnit; para cambiar un test, edita el markdown y re-ejecuta; **MUST NOT** editar los `.java`.
- Traducción **fiel**: mismo sujeto, condición y exenciones; *Mensaje* literal (es la clave de la violation store del freezing); la única transformación extra es `freeze(...)` para las `❌ INCUMPLE`.
- Un único paquete raíz (`com.educaflow.architecture`), un sub-paquete + una clase por categoría; clases autocontenidas (imports + `PAQUETES_EXENTOS` propios).
- Regeneración **INCREMENTAL** (§6.4): empareja regla↔test por la cita literal de la *Verificación* (el ID solo es una etiqueta: el catálogo renumera sin huecos) y clasifica en INTACTO / RENOMBRAR / REGENERAR / NUEVO / BORRAR; **MUST NOT** tocar los tests cuya regla no cambió. **STOP** si encuentras `.java` editados a mano (sin cabecera `GENERADO`).
- Excluye las secciones `NO incluidas` / `Fuera del alcance` y los identificadores retirados.
- Carga `/k-archunit` como referencia; verifica con `./gradlew compileTestJava` (**LIMIT** 3 iteraciones).

---

## Apéndice A — Override de rutas (para testing)

- `--in=<ruta>` — catálogo de entrada explícito (por defecto `agent_docs/architecture-rules.md`).
- `--out=<ruta>` — carpeta raíz de tests (por defecto `src/test/java`).
- `--root-package=<fqn>` — paquete raíz (por defecto `com.educaflow.architecture`).

En uso normal no se especifican.