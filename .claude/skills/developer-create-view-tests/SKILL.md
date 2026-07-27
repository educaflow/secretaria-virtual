---
name: developer-create-view-tests
description: >-
  Dado el catálogo de reglas de vistas `agent_docs/view-rules.md` (decisiones
  estilo ADR: cada regla `VAR-<categoría>.<n>` con Decisión y Verificación
  —sujeto, condición, exenciones—, SIN código), genera las clases de test JUnit 5
  planas que verifican esas reglas leyendo los XML de vistas Axelor con JAXP
  (DOM + XPath), organizadas en un sub-paquete por categoría bajo el paquete raíz
  `com.educaflow.views`, sobre una librería compartida `support/`. NO usa ArchUnit
  (ArchUnit analiza bytecode, no XML). El paquete generado es una PROYECCIÓN del
  markdown: para cambiar un test se edita `view-rules.md` y se re-ejecuta este skill,
  nunca se editan los `.java` a mano. La regeneración es INCREMENTAL: cada test lleva
  citada literal la Verificación de su regla y el skill empareja regla↔test por ese
  contenido (no por el ID, que se renumera), tocando solo lo que cambió.
allowed-tools: Read, Write, Edit, Bash, Skill
---

# developer-create-view-tests

Asumes el rol de **generador de tests de vistas**: transformas el catálogo declarativo `agent_docs/view-rules.md` en clases de test **JUnit 5 planas** dentro de `src/test/java/com/educaflow/views`, una por categoría, sobre la librería compartida `support/`. El catálogo **no trae código**: tú escribes cada `@Test` traduciendo **fielmente** el bloque *Verificación* de cada regla a JAXP (DOM + XPath). El catálogo es la **única fuente de verdad** sobre *qué* se verifica; este skill solo decide *cómo* expresarlo en código. No inventas reglas ni tolerancias.

**CRITICAL — por qué NO es `/developer-create-arch-tests`.** ArchUnit analiza **bytecode**, así que **no sirve para XML**. Las vistas son ficheros XML: se verifican con tests JUnit normales que parsean el DOM con JAXP. Por eso este skill es independiente y no genera `@ArchTest` ni usa freezing ni violation-store.

---

## User Input

```text

```

You **MUST** consider the user input before proceeding (if not empty). Argumentos posibles (todos opcionales; en uso normal no se pasa ninguno):

- `--in=<ruta>` — catálogo de entrada (por defecto `agent_docs/view-rules.md`).
- `--out=<ruta>` — carpeta raíz de tests (por defecto `src/test/java`).
- `--root-package=<fqn>` — paquete raíz (por defecto `com.educaflow.views`).

---

## Outline

1. **Cargar** el contrato (`/k-vistas`) y leer el catálogo de entrada. (Fase 0)
2. **Parsear** el catálogo: convenciones globales, glosario, categorías y reglas `VAR-N.M`. (Fase 1)
3. **Resolver** la estructura de paquetes: `support/` compartido + un sub-paquete por categoría. (Fase 2)
4. **Reconciliar y generar**: emparejar reglas↔tests existentes por contenido y tocar solo lo que cambió, traduciendo a JAXP (DOM + XPath) lo nuevo o modificado. (Fase 3)
5. **Verificar** que compila y ejecuta, clasificar los fallos y reportar. (Fase 4)

**STOP conditions**:

- No existe el fichero de entrada → **ERROR** y detente.
- El catálogo no contiene **ninguna** regla `## VAR-N.M — …` con bloque `**Verificación.**` bajo un encabezado `# Categoría` → **ERROR** (entrada no válida).
- El paquete de una categoría ya contiene ficheros **sin** la cabecera `GENERADO` (editados a mano) → **STOP** y pregunta antes de sobrescribir. La librería `support/` **NO** cuenta como generada (ver §2.3).
- Una categoría está marcada en el catálogo como **no testeada** (p.ej. «catalogadas, no testeadas») → **MUST NOT** generarle clase.

---

## 1. Entrada y salida

### 1.1 Entrada

- `agent_docs/view-rules.md` — catálogo declarativo estilo ADR, sin código. Aporta:
  - Secciones globales `## Convenciones de verificación`, `## Glosario de términos`, `## Fuente de verdad`, `## Paquetes exentos` con el **ámbito de análisis**, los **paquetes exentos** y el vocabulario (contexto, bloque, clase de bloque, rol de una acción, PI).
  - Reglas `## VAR-N.M — <título>` agrupadas bajo encabezados `# Categoría N — <título>`. Cada regla trae `**Decisión.**` (solo el motivo) y `**Verificación.**` (Sujeto + Condición [+ Exenciones]) con ejemplos ✅/❌.
- `/k-vistas` — referencia de vistas Axelor (namespace, tipos de vista, nombres, PI). Se carga con `Skill`.

### 1.2 Salida

Clases de test JUnit 5 planas bajo el paquete raíz, **una clase por categoría**, más la librería compartida `support/`:

```
src/test/java/com/educaflow/views/
    support/            ← librería compartida (NO generada; ver §2.3)
    <subpaquete-cat-1>/Categoria1<Titulo>Test.java
    <subpaquete-cat-2>/Categoria2<Titulo>Test.java
    …
```

### 1.3 Estructura de carpetas

- **Raíz única**: `com.educaflow.views` (configurable con `--root-package`).
- **Un sub-paquete + una clase por categoría** testeada del catálogo.
- **`support/`**: paquete de infraestructura compartida (descubrimiento/parseo de XML, helpers DOM, parser de nombres, acumulador de violaciones). Es una **librería estable**, NO una proyección de ninguna regla concreta (§2.3).
- Cada clase de categoría es **autocontenida** salvo por `support/`: declara sus imports y su lógica; no hay clases base entre categorías.

---

## 2. Principios

### 2.1 Proyección del catálogo

- Las clases de categoría generadas **MUST** ser una proyección exacta de `view-rules.md`: una regla del catálogo ⇒ sus `@Test` generados; una regla borrada ⇒ su `@Test` se borra al reconciliar.
- **MUST NOT** editar a mano las clases generadas. Para cambiar un test: edita el catálogo (o, si el fallo es de traducción, corrige este skill) y vuelve a ejecutar `/developer-create-view-tests`.
- Cada clase de categoría generada **MUST** llevar la cabecera `GENERADO` (§6.1).
- La regeneración es **INCREMENTAL** (§6.3):
  **MUST NOT** vaciar los paquetes ni re-traducir reglas que no cambiaron;
  un test cuya regla sigue igual queda **byte a byte idéntico**.
  Las clases existentes se modifican con `Edit` quirúrgico (solo los métodos afectados), nunca reescribiéndolas enteras.
  **MUST NOT** tocar `support/`.
- **CRITICAL — la identidad de una regla es su contenido, NO su ID**:
  el catálogo renumera sin huecos al borrar o mover reglas,
  así que el emparejamiento regla↔test se hace por el bloque *Verificación* (citado literal encima de cada test, §6.1)
  y el ID es solo una etiqueta a sincronizar.

### 2.2 Traducción fiel y ESTRICTA de la Verificación

- **CRITICAL**: implementa **exactamente** el Sujeto, la Condición y las Exenciones del bloque *Verificación*. Aplica **solo** las exenciones que la propia regla declara.
- **MUST NOT** añadir tolerancias «por la realidad del proyecto». Rige `## Fuente de verdad` del catálogo: si un XML real viola una regla correcta, es un **bug del XML**, NO una excepción a añadir al test. El test **DEBE** fallar.
  - ✅ CORRECTO: `if (!"false".equals(attr(g,"canDelete"))) v.add(...)` — marca todo grid con `canDelete≠false`.
  - ❌ INCORRECTO: `if (!esGestionCentro(vf) && !"false".equals(...))` — exención inventada que el catálogo no declara.
- Cada regla produce uno o varios métodos `@Test void varN_M_<resumenCamelCase>()`. Usa **varios** si la regla tiene sub-condiciones o sujetos heterogéneos (p.ej. una `VAR-5.1` con tabla por tipo de elemento ⇒ un test por fila).
- **MUST NOT** debilitar, reforzar ni «mejorar» una regla. Si parece mal, se corrige en el catálogo, no aquí.
- Un test **MUST** acumular **todas** las violaciones en una `List<Violacion>` y terminar con `Violacion.assertNone("VAR-N.M — <resumen de la norma>", v)` (estilo ArchUnit: reporta todos los incumplimientos de golpe, no aborta en el primero).

### 2.3 La librería `support/` NO es una proyección

- `support/` es **infraestructura compartida** (parseo DOM, helpers, parser de nombres), no deriva de ninguna regla concreta. Por eso:
  - **MUST NOT** llevar cabecera `GENERADO` ni borrarse al regenerar.
  - Si `support/` **no existe**, créala implementando el contrato de §7.
  - Si **existe**, reutilízala; **extiéndela** (añade un helper) **solo** si una regla necesita algo que aún no ofrece. **MUST NOT** reescribir helpers existentes de los que dependan otras categorías.

### 2.4 Sin freezing ni violation-store

- **MUST NOT** usar `FreezingArchRule`, `archunit_store` ni ningún mecanismo de baseline: son de ArchUnit. Un test que falla señala una desviación real del XML que se reporta al usuario (§8), no se «congela».

---

## 3. Fase 0 — Cargar contrato y entrada

1. Carga `/k-vistas` con `Skill` (referencia de vistas Axelor).
2. Resuelve el fichero de entrada (`--in=` o `agent_docs/view-rules.md`) y léelo **entero** (incluidas las secciones globales y el glosario).
   - Si no existe → **ERROR** y detente.
3. Comprueba que existe la librería `support/` bajo el paquete raíz. Si falta, anota que hay que crearla en la Fase 3 (§7).

---

## 4. Fase 1 — Parsear el catálogo

**Contrato de parseo** — solo se generan tests de lo que cumple TODAS estas condiciones:

1. **MUST** procesar únicamente las reglas `## VAR-N.M — <título>` que estén bajo un encabezado `# Categoría N — <título>` y tengan bloque `**Verificación.**`.
2. **MUST** ignorar las categorías marcadas como **no testeadas** en el catálogo (texto tipo «Sin sujetos … → catalogadas, no testeadas»): no generan clase.
3. **MUST** ignorar la sección final `# Fuera del alcance de estos tests` (requieren leer el código Java, no solo el XML).
4. Extrae una sola vez, de las secciones globales:
   - El **ámbito de análisis** (carpetas `views/`, `menus.xml`) → lo consume `support/ViewFiles`.
   - Los **paquetes exentos** → constante en `support/ViewFiles` (**MUST** coincidir con la lista del catálogo).
   - El **glosario** (contexto, bloque, clase de bloque, rol de una acción, PI) → guía cómo implementar sujetos y clasificaciones.

Por cada regla incluida, registra: `id` (`VAR-N.M`), el título, el bloque *Verificación* completo (Sujeto, Condición, Exenciones) y la categoría a la que pertenece.

---

## 5. Fase 2 — Resolver estructura de paquetes

1. **Paquete raíz**: `--root-package=` o `com.educaflow.views`. Carpeta destino: `<out>/<root-package con / >/`.
2. **Un sub-paquete + una clase por categoría** testeada. Deriva ambos del título de la categoría (texto tras `— `, cortando en el primer `(` si lo hay):
   - **Sub-paquete** = una sola palabra en minúsculas, sin acentos, solo `[a-z0-9]` (elige el sustantivo principal del título; elimina espacios y signos).
   - **Clase** = `Categoria<N><TituloPascalCase>Test` (sin acentos ni signos).
3. Ejemplos de derivación:
   - ✅ `Categoría 1 — Fichero y ubicación` → paquete `estructura`, clase `Categoria1FicheroTest`
   - ✅ `Categoría 2 — Nomenclatura` → paquete `nombres`, clase `Categoria2NomenclaturaTest`
   - ✅ `Categoría 3 — Bloques y secciones (Processing Instructions)` → paquete `bloques`, clase `Categoria3BloquesTest` (se corta en el `(`)
   - ❌ `bloques-y-secciones` (los guiones no son válidos en un nombre de paquete Java)
   - ❌ `Bloques` como paquete (un paquete va en minúsculas)
4. El nombre concreto del sub-paquete es de estilo; lo **MUST** es: minúsculas `[a-z0-9]`, una clase por categoría, y estabilidad entre ejecuciones (no renombrar paquetes ya existentes salvo que cambie el título de la categoría).

---

## 6. Fase 3 — Reconciliar y generar las clases de test

1. Si `support/` no existe, créala (§7).
2. Si hay `.java` de categoría **sin** cabecera `GENERADO` → **STOP** (§Outline).
3. **Reconcilia** el catálogo con los tests existentes (§6.3):
   clasifica cada regla/test en INTACTO, RENOMBRAR, REGENERAR, NUEVO o BORRAR.
4. Aplica el plan:
   crea las clases de categorías nuevas con la plantilla §6.1;
   modifica las existentes con `Edit` quirúrgico (solo los métodos afectados);
   borra las clases cuya categoría desapareció del catálogo.
5. Todo `@Test` nuevo o regenerado traduce su *Verificación* (§2.2),
   lleva su cita literal (§6.1) y respeta el orden del catálogo.

### 6.1 Plantilla de clase generada

````
// =====================================================================
// GENERADO por /developer-create-view-tests desde agent_docs/view-rules.md
// NO EDITAR A MANO. Para cambiar un test, edita view-rules.md (o corrige
// la traducción en el skill /developer-create-view-tests) y vuelve a ejecutarlo.
// =====================================================================
package com.educaflow.views.<subpaquete>;

import com.educaflow.views.support.*;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import java.util.ArrayList;
import java.util.List;
<otros imports que necesite el código de esta clase>

/**
 * Categoría N — <título> (agent_docs/view-rules.md).
 * Reglas verificadas: VAR-N.1, VAR-N.2, …
 */
class Categoria<N><Titulo>Test {

    // [VAR-N.1] Verificación:
    //   <líneas del bloque Verificación de VAR-N.1, copiadas literales del catálogo>
    @Test
    void varN_1_<resumen>() {
        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            // Sujeto + Condición de la Verificación de VAR-N.1, ESTRICTOS.
            // Solo las Exenciones que la regla declara.
        }
        Violacion.assertNone("VAR-N.1 — <resumen de la norma>", v);
    }

    // … un @Test por regla (o varios si la regla tiene sub-condiciones) …
}
````

**MUST**: clase y métodos en visibilidad de paquete (sin `public`) — JUnit 5 no requiere `public`.

**REQUIRED — cita literal de la Verificación**: encima de cada `@Test` va el bloque *Verificación.* de su regla citado **literal** del catálogo, línea a línea, con el marcador exacto `// [VAR-N.M] Verificación:` y prefijo `//   `. Esta cita es la clave del emparejamiento incremental (§6.3). Si una regla produce varios `@Test`, la cita completa va sobre el primero y los demás llevan solo el marcador `// [VAR-N.M] (continuación)`.

### 6.2 Reglas de traducción a JAXP (DOM + XPath)

- **MUST** usar los helpers de `support/` (§7) para descubrir ficheros, navegar el DOM y leer atributos; **MUST NOT** reparsear XML ni releer ficheros a mano si el helper existe.
- El **sujeto** de una regla se implementa recorriendo `ViewFiles.all()` (y `ViewFiles.menusDoc()` si el sujeto incluye `menus.xml`) y filtrando por tag/atributo/`name`.
- La **clase de bloque** (maestro/detalle/referencia) y el desglose de un `name` (contexto, variante, ruta de entidad, descripción, tipo) se obtienen de `support/NombreVista`, NO se reparsean con `substring` ad-hoc en cada test.
- Las **PI** (`<?sv-*?>`) son nodos `PROCESSING_INSTRUCTION_NODE` del DOM: se recorren con JAXP igual que los elementos (el `DocumentBuilder` las conserva).
- **MUST NOT** usar `awk`/`grep`/`sed` sobre el XML: todo con JAXP. La única regla que mira **texto crudo** es la de formato de `menus.xml` (líneas, sangría), que usa `ViewFiles.menusLineas()`.
- Cada `Violacion` lleva `(fichero, ubicación, detalle)`: `fichero` = `vf.rel()`, `ubicación` = el `name` de la vista/acción o una descripción del nodo, `detalle` = qué condición se incumplió.

### 6.3 Reconciliación incremental

La unidad de reconciliación es la **regla** (su cita + todos sus métodos `@Test`, incluidas las continuaciones). Empareja las reglas del catálogo con los tests existentes en **tres pasadas ordenadas** (cada elemento emparejado sale del juego):

1. **Por contenido** — la cita `// [VAR-N.M] Verificación:` de un test coincide con el bloque *Verificación* de una regla
   (comparación literal línea a línea, ignorando espacios finales y las apariciones del propio ID, que cambian al renumerar).
   Si varias reglas/tests coinciden entre sí, prioriza el emparejamiento de mismo ID.
   - Mismo ID → **INTACTO**: **MUST NOT** tocar los métodos.
   - Distinto ID o distinta categoría (regla renumerada o movida) → **RENOMBRAR**:
     cambio mecánico de los nombres `varN_M_…`, del ID en la cita y en los `assertNone("VAR-N.M — …")`;
     mover de clase si cambió de categoría. **MUST NOT** re-traducir el cuerpo.
2. **Por ID** — regla y test aún sin pareja con el mismo `VAR-N.M`: la *Verificación* cambió →
   **REGENERAR** solo los métodos de esa regla (nueva traducción §2.2 + nueva cita).
3. **Resto** — regla sin pareja → **NUEVO**; test sin pareja → **BORRAR**.

Casos especiales:

- **Test sin cita (legacy)**: generado por una versión anterior del skill.
  Compara su código con la regla de su ID:
  si implementa fielmente la *Verificación* → añade la cita y **MUST NOT** tocar el cuerpo; si no → REGENERAR.
- **Primera generación** (no existen clases de categoría) → todo es NUEVO.
- **Cambio en las secciones globales** (`## Convenciones de verificación`, `## Glosario de términos`, `## Fuente de verdad`, `## Paquetes exentos`):
  invalida la traducción de **todos** los tests →
  sincroniza `support/ViewFiles` (`PAQUETES_EXENTOS`, ámbito de análisis) y
  re-evalúa cada test contra su regla (el que siga siendo traducción fiel queda INTACTO).

---

## 7. Contrato de la librería `support/`

`support/` es la infraestructura mínima que las clases de categoría consumen. Si no existe, créala con **exactamente** estas clases y responsabilidades (visibilidad de paquete, comentarios en español). Si existe, reutilízala y solo **añade** helpers que falten.

1. **`ViewFiles`** — descubrimiento y parseo (cacheado) de los XML de vistas.
   - `static List<ViewFile> all()` — todos los `views/*.xml` **no exentos**, parseados. El discriminador es la carpeta `views/` bajo `src/main/java/com/educaflow`, no el elemento raíz.
   - `PAQUETES_EXENTOS` — lista de segmentos de ruta exentos, **idéntica** a la del catálogo.
   - `static Document menusDoc()` / `Path menusPath()` / `List<String> menusLineas()` — acceso a `secretariavirtual/menus/menus.xml` (DOM y texto crudo).
   - `static Set<String> entidadesDelModulo(ViewFile)` — nombres `<entity name>` de los XML de `../domains` del módulo dueño.
   - Helpers DOM estáticos (sin namespace): `byTag(nodo, tag)`, `childrenByTag(nodo, tag)`, `attr(elem, name)`, `hasAttr(elem, name)`.
   - El `DocumentBuilderFactory` va con `setNamespaceAware(false)` (los XML usan namespace por defecto sin prefijo → tags simples) y las features de seguridad (sin DTD ni entidades externas).

2. **`ViewFile`** — un fichero de vistas parseado.
   - `Document doc()`, `Path path()`, `String fileName()`, `String rel()` (ruta legible desde `com/educaflow`), `boolean isSubsystem()` / `isSystem()`, `String ownerModule()`.
   - Atajos: `byTag(tag)`, `forms()`, `grids()`, `actionViews()`, `Map<String,List<String>> actionGroups()` (name → lista de `<action name>` hijos).

3. **`NombreVista`** — descomposición del `name` según el glosario (`{contexto}-[{descripcion}-]{tipo}`, `contexto = {marcadorCapa}{Modulo}.{Variante}@{Entidad}[.{Detalle}…]`).
   - `static NombreVista parse(String name)` — `null` si no casa el patrón (acciones globales/predefinidas sin `@`, nombres Axelor adaptados).
   - Accesores: `marcadorCapa`, `modulo`, `variante`, `rutaEntidad` (lista), `descripcion`, `tipo`, `contexto()`, `entidad()` (último segmento de la ruta).
   - `enum Clase { MAESTRO, DETALLE, REFERENCIA }` + `Clase clase()` — deducida mecánicamente: variante `Ref` ⇒ REFERENCIA; ruta de ≥2 segmentos ⇒ DETALLE; si no ⇒ MAESTRO.

4. **`Index`** — índices globales cross-XML y utilidades de resolución de acciones.
   - `PREDEFINIDAS` — acciones globales/predefinidas del glosario (`save`, `back`, `force-back`, `delete`, `close`, `save-modal`, `delete-modal`, `new`, `validate`, `remote-validationSave-action`, `remote-validationDelete-action`).
   - `grupos()`, `accionesDeclaradas()`, `metodosOScripts()`, `modeloPorVista()`, `onClick(form, btn)`, `accionesDeGrupo(vf, grupo)`.

5. **`Violacion`** — `record (String fichero, String ubicacion, String detalle)` con `toString()` legible y `static void assertNone(String regla, List<Violacion> v)` que falla el test listando todas las violaciones (`regla` = id + mensaje).

**MUST**: la constante `PAQUETES_EXENTOS` de `ViewFiles` **MUST** coincidir carácter a carácter con la lista `## Paquetes exentos` del catálogo. Si cambian en el catálogo, actualízala.

---

## 8. Fase 4 — Verificar, clasificar y reportar

1. Compila los tests: `./gradlew compileTestJava`. Si falla, corrige y recompila. **LIMIT**: máximo 3 iteraciones; si tras la 3ª no compila, **STOP** y muestra el error.
2. Ejecuta la suite: `./gradlew test --tests "com.educaflow.views.*"`.
3. **Clasifica cada fallo** en una de tres categorías (**MUST NOT** «arreglar» un fallo sin clasificarlo antes):
   - **(A) Bug del test** — el test NO implementa fielmente la *Verificación* (mal sujeto, condición o exención). ⇒ **MUST** corregir la traducción (en el skill/plantilla) y regenerar.
   - **(B) Hueco del catálogo** — la regla no contempla un mecanismo legítimo (p.ej. una referencia Axelor no listada en una tabla). ⇒ **STOP** y propón al usuario editar `view-rules.md`; NO lo edites por tu cuenta sin aprobación.
   - **(C) Desviación real del XML** — el XML incumple una regla correcta (`## Fuente de verdad`). ⇒ **MUST NOT** tocar el test ni la regla; se reporta como bug del XML para que lo arreglen sus dueños.
4. Aplica el checklist §9. **MUST NOT** dar por terminado si queda algún punto sin cumplir.
5. Reporta al usuario, escueto:
   - Resumen de la reconciliación: nº de reglas INTACTAS / RENOMBRADAS / REGENERADAS / NUEVAS / BORRADAS (§6.3).
   - Clases generadas o modificadas (ruta) y nº de reglas por clase.
   - Categorías del catálogo omitidas (no testeadas / fuera de alcance).
   - Fallos agrupados por (A)/(B)/(C), con el fichero y la regla `VAR-N.M`.

> Compilar (`compileTestJava`) verifica que el código generado es válido. **Ejecutar** (`test`) revela las desviaciones reales de los XML — que son el objetivo del catálogo, no un error del skill.

---

## 9. Checklist final

- [ ] ¿Cada categoría **testeada** del catálogo tiene su clase bajo el paquete raíz único?
- [ ] ¿Cada `@Test` implementa **exactamente** el Sujeto, la Condición y las Exenciones de la *Verificación* de su regla, sin tolerancias inventadas?
- [ ] ¿Cada test acumula todas las violaciones y termina en `Violacion.assertNone("VAR-N.M — …", v)`?
- [ ] ¿Cada clase de categoría lleva la cabecera `GENERADO` y `support/` NO la lleva?
- [ ] ¿Cada regla lleva su cita `// [VAR-N.M] Verificación:` idéntica al catálogo actual?
- [ ] ¿Los tests INTACTOS quedaron byte a byte iguales (el diff solo toca lo reconciliado)?
- [ ] ¿Se omitieron las categorías «no testeadas» y la sección `# Fuera del alcance de estos tests`?
- [ ] ¿`PAQUETES_EXENTOS` de `support/ViewFiles` coincide con el catálogo?
- [ ] ¿Los nombres de sub-paquete son `[a-z0-9]` y las clases `Categoria<N><Titulo>Test`?
- [ ] ¿`./gradlew compileTestJava` pasa y los fallos de `test` están clasificados en (A)/(B)/(C)?

**LIMIT**: máximo 3 iteraciones de corrección sobre este checklist; si tras la 3ª siguen fallando ítems, documenta lo que queda y avisa al usuario.

---

## Quick Guidelines

- El catálogo `agent_docs/view-rules.md` es la **única fuente de verdad** y **no trae código**: tú traduces cada bloque *Verificación* a JAXP (DOM + XPath); para cambiar un test, edita el markdown y re-ejecuta; **MUST NOT** editar los `.java` generados.
- **NO ArchUnit**: ArchUnit es bytecode; las vistas son XML → tests JUnit planos con JAXP. Sin freezing ni violation-store.
- Traducción **ESTRICTA**: mismo sujeto, condición y **solo** las exenciones que la regla declara. Un XML que incumple una regla correcta es un bug del XML (`## Fuente de verdad`), no una exención a añadir: el test **DEBE** fallar.
- Un único paquete raíz (`com.educaflow.views`), un sub-paquete + una clase por categoría, sobre la librería compartida `support/` (§7), que **NO** es proyección y **NO** se regenera.
- Cada regla ⇒ uno o varios `@Test varN_M_…` que acumulan `Violacion` y cierran con `Violacion.assertNone(...)`.
- Regeneración **INCREMENTAL** (§6.3): empareja regla↔test por la cita literal de la *Verificación* (el ID solo es una etiqueta: el catálogo renumera sin huecos) y clasifica en INTACTO / RENOMBRAR / REGENERAR / NUEVO / BORRAR; **MUST NOT** tocar los tests cuya regla no cambió ni `support/`. **STOP** si encuentras `.java` de categoría sin cabecera `GENERADO`.
- Carga `/k-vistas` como referencia; verifica con `compileTestJava` (**LIMIT** 3) y clasifica los fallos de `test` en (A) bug del test / (B) hueco del catálogo / (C) desviación real del XML.

---

## Apéndice A — Override de rutas (para testing)

- `--in=<ruta>` — catálogo de entrada explícito (por defecto `agent_docs/view-rules.md`).
- `--out=<ruta>` — carpeta raíz de tests (por defecto `src/test/java`).
- `--root-package=<fqn>` — paquete raíz (por defecto `com.educaflow.views`).

En uso normal no se especifican.
