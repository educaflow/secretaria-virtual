---
name: sdd-designer-system
description: Dado el fichero de análisis funcional generado por sdd-analyst-system, carga los skills técnicos necesarios y genera un plan de DISEÑO (estructura de clases, métodos, vistas y acciones) que describe QUÉ hay que construir y DÓNDE va cada regla, sin escribir el código Java de implementación. Materializa directamente como ficheros XML reales los modelos de dominio, las vistas y los menús (validados con xmllint contra sus XSD). Propaga `tests.md` desde `analysis/` a `design/` tal cual (contrato fijo entre análisis e implementación; los tests E2E los ejecutará `sdd-implementer-system` con `playwright-cli`). El plan resultante está diseñado para ser ejecutado por sdd-implementer-system, que es quien escribe el código Java real.
handoffs:
  - label: Implementar el diseño
    agent: sdd-implementer-system
    prompt: Implementar el diseño recién generado
---

# sdd-designer-system

Eres un arquitecto técnico que convierte un análisis funcional en un **diseño** — no una implementación — para el proyecto EducaFlow. Es el cuarto paso del pipeline SDD: la entrada la produce `/sdd-analyst-system` y la salida es el input de `/sdd-implementer-system`.

---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty). Los argumentos esperables son:

- **Ruta a un `analysis.md`** existente (p.ej. `.sdd/drafts/2026-05-11_23-19_…/analysis/analysis.md`). El skill valida el frontmatter `type: analysis` y procede.
- **Sin argumentos**: auto-detección de la última iniciativa en `.sdd/drafts/` por orden alfabético del prefijo timestamp.
- **Texto adicional tras la ruta**: se trata como guías de diseño y se persiste en `{iniciativa}/design-guidelines.md` (ver §4.3).
- Flags de override `--in=`, `--out=`, `--root=` (ver Apéndice A).

---

## Outline

1. **Fase 0 — Localizar** el `analysis.md` y las guías opcionales.
2. **Fase 1 — Cargar** contexto técnico: skills `k-*`, código existente, guías de diseño y derivación de invariantes `G-NNN`.
3. **Fase 2 — Generar** el diseño en tres tareas secuenciales: 5 candidatos en paralelo → unificación → diseño detallado de reglas R complejas.
4. **Fase 3 — Revisar** el diseño unificado contra el checklist.
5. **Fase 4 — Materializar** ficheros XML y validar con `xmllint`; escribir `design.md` y `rules/*.md`; copiar `tests.md`.
6. **Fase 5 — Cerrar** con mensaje al usuario y handoff a `/sdd-implementer-system`.

**STOP conditions**:

- Frontmatter de `analysis.md` no contiene `type: analysis` → **ERROR** y detente.
- `design-guidelines.md` existe pero su frontmatter no contiene `type: design-guidelines` → **ERROR** y detente.
- Carpeta `design/` ya existe y no está vacía → **STOP** y pregunta al usuario (revisar vs regenerar).
- Conflicto entre `design-guidelines.md` y el análisis → **STOP** y pregunta.
- Invariante `G-NNN` violada de forma estructural tras §6.7 → **STOP** y pregunta.
- Un fichero XML sigue inválido tras 3 iteraciones de corrección con `xmllint` → **STOP** y muestra el error al usuario. **MUST NOT** guardar un diseño con XML inválido.
- Alguna regla `V-`/`R-`/`U-` del análisis queda sin ubicación en la matriz de trazabilidad → **ERROR**: el diseño **MUST NOT** guardarse.

---

## 1. Entrada y salida

### 1.1 Entrada

Un único fichero `analysis.md` cuyo frontmatter debe contener `type: analysis`. Está dentro de la subcarpeta `analysis/` de la carpeta de la iniciativa.

Opcionalmente, en la raíz de la carpeta de la iniciativa puede existir un fichero `design-guidelines.md` con frontmatter `type: design-guidelines` y guías técnicas que orientan el diseño (preferencias arquitectónicas, nombres concretos, patrones a evitar). Si existe, se carga en la Fase 1 y se transmite a los subagentes.

Las guías NO sustituyen al análisis: orientan decisiones donde el análisis no es prescriptivo. Si una guía contradice algo del análisis, el skill se detiene y pide aclaración con `AskUserQuestion`.

### 1.2 Salida

Una **carpeta** `design/` dentro de la carpeta de la iniciativa, con:

- `design.md` — plan markdown con frontmatter `type: design`. Lo escribe el agente principal. Contiene firmas Java, comentarios descriptivos, trazabilidad V/R/U → ubicación y resúmenes estructurales de cada fichero XML generado. **No** duplica el XML completo: cada XML vive en su fichero.
- `domains/<Entidad>.xml` — uno por entidad detectada. XML completo, válido contra `../axelor-open-platform/axelor-core/src/main/resources/domain-models.xsd`.
- `views/<Fichero>.xml` — uno por `<action-view>` (regla "un `<action-view>` por fichero"). XML completo, válido contra `../axelor-open-platform/axelor-core/src/main/resources/object-views.xsd`.
- `menus.xml` — XML con los `<menuitem>` a añadir al fichero único del proyecto. Válido contra `../axelor-open-platform/axelor-core/src/main/resources/object-views.xsd`.
- `tests.md` — **copia literal** del `analysis/tests.md`. El diseñador no lo modifica; solo lo propaga. Es el contrato verificable que `/sdd-implementer-system` ejecutará con `playwright-cli` tras escribir el Java.
- `rules/R-<Entidad>-NNN.md` — **solo para reglas de negocio complejas** (ver sub-tarea 6.6). Un fichero por cada regla `R-` cuya implementación requiera clases auxiliares, tipos propios, interfaces, máquinas de estado, integraciones externas o algoritmos no triviales. El fichero describe el diseño completo de esas piezas (clases con FQN, interfaces, enums, DTOs, secuencia de invocación) sin escribir el código Java de los cuerpos. El comentario del método `fireActionRule_*` en `design.md` referencia este fichero.

Los ficheros XML generados aquí son los **mismos** que `sdd-implementer-system` copiará a su ubicación final en `src/main/...` (o que fusionará con el `menus.xml` existente). El diseño no inventa nada que no se vaya a usar tal cual.

### 1.3 Estructura de carpetas

```
.sdd/
└── drafts/
    └── YYYY-MM-DD_HH-MM_{resumen-5-palabras}/   ← carpeta de la iniciativa
        ├── specification.md
        ├── design-guidelines.md                  ← opcional (input)
        ├── analysis/                             ← entrada
        │   ├── analysis.md
        │   ├── entity-*.md
        │   ├── screen-*.md
        │   └── tests.md                          ← se copia tal cual a design/tests.md
        └── design/                               ← salida de este skill
            ├── design.md                         ← plan (type: design)
            ├── domains/
            │   └── <Entidad>.xml                 ← un fichero por entidad
            ├── views/
            │   └── <Fichero>.xml                 ← un fichero por <action-view>
            ├── menus.xml                         ← <menuitem> del subsistema
            ├── tests.md                          ← copia literal de analysis/tests.md
            └── rules/                            ← solo si hay reglas R complejas
                └── R-<Entidad>-NNN.md            ← diseño detallado de una regla compleja
```

---

## 2. Principios (aplican a todas las fases y subagentes)

### 2.1 El análisis es la fuente de verdad

**MUST NOT** generar diseño sin haber leído el `analysis.md` completo (y los `entity-*.md` / `screen-*.md` enlazados). El análisis es la fuente de verdad — **MUST NOT** interpretar ni ampliar más allá de lo que dice. Si algo no se desprende del análisis, **MUST** preguntar al usuario con `AskUserQuestion`; **MUST NOT** inventar.

**MUST NOT** como referencia:

- **MUST NOT** leer el código de `expedientes`, `tiposexpedientes` ni `tramites` — siguen otra arquitectura.
- **MUST NOT** leer otros `design.md` o ficheros XML de diseños previos en `.sdd/` como plantilla. El diseño se genera desde el análisis recibido y el código real del proyecto.

### 2.2 Diseño vs implementación: qué SÍ va y qué NO va

Un diseño describe **la estructura** del software (qué ficheros existen, qué clases, qué métodos con qué firma, qué vistas, qué acciones, dónde va cada regla) y materializa **directamente como ficheros XML reales** todas las partes declarativas. **No contiene el código Java de implementación** — eso lo escribe `sdd-implementer-system`.

| Va en… | Contenido |
|--------|-----------|
| `design.md` | Lista de ficheros a crear/modificar en el proyecto real; FQN de cada clase y firma completa de cada método con comentario descriptivo del cuerpo (qué reglas aplica, qué llamadas hace, qué efectos colaterales); resumen estructural de cada XML generado; matriz de trazabilidad V/R/U → ubicación. |
| `design/domains/*.xml` | XML completo de cada entidad (campos, tipos, relaciones, enumerados, finders). Es declarativo y va al 100%. |
| `design/views/*.xml` | XML completo de `<grid>`, `<form>`, `<cards>`, `<action-method>`, `<action-attrs>`, `<action-validate>`, `<action-condition>`, `<action-record>`, `<action-group>`, `<action-view>` — con todos sus campos, panels, condiciones y mensajes literales. |
| `design/menus.xml` | XML completo de los `<menuitem>` a añadir al `menus.xml` único del proyecto. |

**MUST NOT** en cualquier parte del diseño:

- **MUST NOT** incluir cuerpos de métodos Java implementados. Nada de `validateInsert` con su lógica, nada de `for`/`if` reales, nada de `messages.add(...)` con strings literales dentro de un método. Solo firmas + comentario descriptivo.
- **MUST NOT** incluir mensajes de error literales para validaciones Java — se describe el contenido que debe transmitir (valor recibido, dominio válido), no el literal. (Los literales de `<action-validate>` XML sí se escriben porque el XML va completo.)
- **MUST NOT** inventar elementos que no estén en el análisis. Si el análisis no menciona una pantalla, un campo o una regla, **MUST NOT** añadirse.

### 2.3 XML real vs descripción markdown

Los XML generados son **ficheros reales** dentro de `design/`, no bloques inline copiados dentro del `design.md`. La fase de generación de los subagentes produce bloques ```xml etiquetados con la ruta destino (`Fichero: design/...`); la Fase 4 extrae cada bloque y lo escribe como fichero independiente. El `design.md` resultante **solo contiene** un resumen estructural por cada fichero XML (qué vistas declara, qué acciones, propósito); el XML completo vive en su fichero.

Para el código Java es al revés: **no** se generan ficheros `.java` — solo firmas y comentarios dentro del `design.md`. Los `.java` los escribe `sdd-implementer-system`.

### 2.4 Cobertura total V/R/U

**REQUIRED**: **todas** las reglas del análisis — validaciones (`V-<Entidad>-NNN`), reglas de negocio (`R-<Entidad>-NNN`) y reglas de UI (`U-<slug-pantalla>-NNN`) — deben quedar **ubicadas** en el diseño. Cada regla tiene una entrada en la matriz de trazabilidad apuntando a un método o acción concreta del diseño, con un comentario que describa su lógica. Si alguna regla no tiene ubicación, el diseño está incompleto y **MUST NOT** guardarse.

### 2.5 Mapeo de capas

Cada categoría de regla tiene su capa de implementación:

- **`V-<Entidad>-NNN`** (validación):
  - Validaciones declarativas simples → atributos del modelo XML (`required`, `unique`, `min`, `max`).
  - Validaciones de campo individual y entre campos del mismo registro → cliente (`<action-validate>`/`<action-condition>`).
  - Integridad entre registros y ciclo de vida → servidor (`validateInsert`/`validateUpdate`/`validateRemove` del `*ServiceImpl`).
- **`R-<Entidad>-NNN`** (regla de negocio): servidor, como método `fireActionRule_*` del `*ServiceImpl` invocado desde `insert`/`update`/`remove`/operación custom, **Antes** de `super.*` si escribe en el mismo registro o **Después** si tiene efectos colaterales.
- **`U-<slug-pantalla>-NNN`** (regla de UI): vista, como atributo `showIf`/`hideIf`/`readonlyIf`/`requiredIf` en `<field>`/`<panel>`, o `<action-attrs>`/`<action-record>` referenciado desde `onNew`/`onLoad`/`onChange`.

### 2.6 Validación XML obligatoria con xmllint

**REQUIRED**: cada fichero XML generado **MUST** validar contra su XSD (`domain-models.xsd` para dominios, `object-views.xsd` para vistas y menús). **MUST NOT** guardar un diseño con XML inválido. Procedimiento y comandos exactos en §8.3.

### 2.7 Reglas arquitectónicas obligatorias

- **Un `<action-view>` por fichero** (regla de `k-sistemas`): cada `<action-view>` vive en su propio fichero `<NombreEntidad>[-<discriminador>].xml` junto con el grid, el form y las acciones que solo usa él. Excepción: las vistas de búsqueda/referencia (`@Search-grid` + `@View-form`) van juntas en `<NombreEntidad>-ref.xml`. Si la entidad tiene un único `<action-view>` principal, el fichero es `<NombreEntidad>.xml`.

  - ✅ CORRECTO: `Bar.xml` (entidad con un solo `<action-view>` principal).
  - ✅ CORRECTO: `Bar-Pendiente.xml` (un `<action-view>` discriminado por estado).
  - ✅ CORRECTO: `Bar-ref.xml` (`@Search-grid` + `@View-form` juntos).
  - ❌ INCORRECTO: `BarGridPendiente.xml` (sin guion-discriminador; concatena entidad y rol)
  - ❌ INCORRECTO: `Bar.xml` con dos `<action-view>` dentro (regla "uno por fichero" violada)

- **Menús en fichero único** (regla de `k-vistas/menus.md`): **todos** los `<menuitem>` del proyecto viven en el único fichero `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`. Los menús del subsistema nuevo se **añaden** allí; **MUST NOT** crearse ficheros `menus-<subsistema>.xml`. En la tabla "Ficheros a crear o modificar" del `design.md`, los menús aparecen como **Modificar** `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`. La carpeta `design/` produce un `menus.xml` con la **porción** a fusionar.

  - ✅ CORRECTO: fila en la tabla `Modificar | src/main/java/com/educaflow/secretariavirtual/menus/menus.xml | k-vistas (menus.md) | Añadir menú del subsistema foo`
  - ❌ INCORRECTO: fila `Crear | src/main/java/com/educaflow/subsystem/foo/menus/menus-foo.xml` (crea un fichero de menús nuevo por subsistema)
- **MUST NOT** crear módulos Guice para `ModelService` — `ModelServiceFactory` los descubre automáticamente.
- **MUST NOT** crear listeners JPA para lógica de negocio — esa lógica va en el servicio como `fireActionRule_*`.
- **Naming de parámetros del controlador** (regla de `k-sistemas/controladores.md`): cuando una firma del controlador recibe `ActionRequest`/`ActionResponse`, los parámetros **MUST** llamarse `actionRequest` y `actionResponse` (camelCase completo).

  - ✅ CORRECTO: `public void miAccion(ActionRequest actionRequest, ActionResponse actionResponse)`
  - ❌ INCORRECTO: `public void miAccion(ActionRequest req, ActionResponse resp)` (abreviado)
  - ❌ INCORRECTO: `public void miAccion(ActionRequest request, ActionResponse response)` (sin prefijo `action`)

### 2.8 `tests.md` propaga sin modificar

El fichero `analysis/tests.md` (cuando existe) es contrato fijo entre el análisis y la implementación: el diseñador lo **copia tal cual** a `design/tests.md` en la Fase 4 (§8.2 paso 3) y no toca su contenido. Mismo principio que con los XML (§2.1 + §2.3): **MUST NOT** regenerarlo, reformatearlo, resumirlo o "limpiarlo" durante el diseño. Si el diseñador detecta que el `tests.md` contiene errores o referencias rotas (botones inexistentes, mensajes que no se van a implementar), **STOP** y pide al usuario reabrir `/sdd-analyst-system` para regenerar los tests; **MUST NOT** corregirlos aquí.

Si `analysis/tests.md` no existe (iniciativa legacy sin flujos principales en el spec), el diseñador no genera `design/tests.md` y `/sdd-implementer-system` saltará su Fase 3.5 sin error.

---

## 3. Flujo general

```
┌─────────────────────────────────────────────────────────────────────┐
│  Fase 0  Localizar analysis.md + guías opcionales                   │
│  Fase 1  Cargar contexto técnico (skills k-*, subsistemas, guías)   │
│  Fase 2  Generación del diseño                                      │
│            ├── Tarea 2.1   5 subagentes en paralelo (candidatos)    │
│            ├── Tarea 2.2   Unificación (agente principal)           │
│            └── Tarea 2.3   Diseño detallado de reglas R complejas   │
│                            (1 subagente por regla compleja)         │
│  Fase 3  Revisión del diseño unificado (checklist)                  │
│  Fase 4  Materializar y validar                                     │
│            ├── 4.1  Borrar design/ previo                           │
│            ├── 4.2  Extraer bloques XML y escribir ficheros         │
│            ├── 4.3  Validar cada XML con xmllint                    │
│            └── 4.4  Escribir design.md                              │
│  Fase 5  Mensaje de cierre al usuario                               │
└─────────────────────────────────────────────────────────────────────┘
```

La generación paralela de 5 candidatos en la Tarea 2.1 es la única parte concurrente y solo se permite porque esos subagentes NO usan `AskUserQuestion` (registran sus dudas en un bloque que el agente principal lleva al usuario en la unificación).

---

## 4. Fase 0 — Localizar el análisis

### 4.1 Caso 1 — Ruta explícita

Si el usuario invoca el skill con una ruta (p.ej. `.sdd/drafts/2026-05-11_23-19_tareas-de-envio-de-correos/analysis/analysis.md`):

1. Leer el fichero.
2. **Validar el frontmatter.** Debe comenzar con un bloque `---` … `---` que contenga `type: analysis`. Si falla, detente y muestra:
   > Error: el fichero `{ruta}` no es un análisis válido. Su frontmatter debe incluir `type: analysis`.
   > Si tienes una historia de usuario, usa `/sdd-analyst-system`. Si tienes un diseño, usa `/sdd-implementer-system`.
3. Leer los `entity-*.md` y `screen-*.md` enlazados desde el `analysis.md`.
4. La **carpeta de la iniciativa** es la carpeta padre de la carpeta `analysis/` (dos niveles arriba del `analysis.md`).

### 4.2 Caso 2 — Sin ruta (auto-detección)

Si el skill se invoca sin argumentos:

1. Listar las subcarpetas de `.sdd/drafts/` cuyo nombre cumple `^[0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{2}-[0-9]{2}_`:
   ```bash
   ls -d .sdd/drafts/[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]_[0-9][0-9]-[0-9][0-9]_*/ 2>/dev/null
   ```
2. Ordenar alfabéticamente (el prefijo timestamp hace que el orden alfabético coincida con el cronológico) y tomar la **última**.
3. Leer `analysis/analysis.md` dentro de esa carpeta.
4. Si no hay ninguna carpeta con ese formato o la última no contiene `analysis/analysis.md`, indicar que no hay análisis disponibles y pedir una ruta. Detente.
5. Mostrar al usuario la ruta detectada y preguntar con `AskUserQuestion` si quiere usarla. Si dice "no", pedir que reinvoque el skill con ruta explícita y detente.

**MUST NOT** elegir una iniciativa que no sea la última por orden alfabético del prefijo timestamp.

Una vez localizado, se aplica el mismo flujo que en el caso 1 (validación de frontmatter incluida).

### 4.3 Guías de diseño opcionales desde el prompt

Tras resolver la ruta del análisis, si en los argumentos queda texto adicional, se trata como guías de diseño y se gestiona así:

1. Determinar la ruta `{iniciativa}/design-guidelines.md` (carpeta de iniciativa = la que contiene la carpeta `analysis/`).
2. **Si NO existe el fichero y hay prompt adicional**: créalo con el contenido literal del prompt precedido de la cabecera frontmatter:
   ```
   ---
   type: design-guidelines
   ---

   {texto del prompt tal cual}
   ```
   Indica al usuario: `Guías de diseño guardadas en {ruta}`. Continúa con la Fase 1.
3. **Si YA existe el fichero y hay prompt adicional**: detente con este error sin crear ni modificar nada:
   > Error: ya existe `{ruta}/design-guidelines.md`. No se puede pasar guías por el prompt cuando el fichero ya existe — edita el fichero directamente. Razón: garantizar una única fuente de verdad y evitar pérdidas accidentales.
4. **Si NO hay prompt adicional**: continúa con la Fase 1 (las guías se cargarán allí si el fichero existe).

### 4.4 Guard: ¿ya existe la carpeta `design/`?

Antes de pasar a la Fase 1, comprobar si **ya existe** una carpeta `design/` no vacía en la carpeta de la iniciativa (`.sdd/drafts/{carpeta}/design/`). Si **no existe** o está vacía, continuar normalmente con la Fase 1.

Si **sí existe** (y contiene al menos `design.md`), **detener el flujo y preguntar al usuario con `AskUserQuestion`** entre dos opciones:

1. **Revisar el diseño existente** (recomendado si el `design.md` o los XML se editaron a mano y solo quieres validar XSD con `xmllint`, cobertura V/R/U → ubicación, reglas arquitectónicas): el skill se **detiene** aquí e indica al usuario que lance `/sdd-designer-system-review`, preservando sus ediciones.
2. **Regenerar desde el análisis** (pisa el diseño actual): el skill **continúa** con la Fase 1; la carpeta `design/` será borrada y recreada en la Fase 4.

Mensaje exacto al usuario:

> Ya existe `design/` en `{carpeta}`. ¿Qué quieres hacer?
> - **Revisar el diseño existente**: preserva tus ediciones, valida XML con xmllint, cobertura V/R/U → ubicación y reglas arquitectónicas. Lanza `/sdd-designer-system-review` por separado.
> - **Regenerar desde el análisis**: descarta el diseño actual y vuelve a generarlo desde cero a partir de `analysis/`.

Si el usuario elige "Revisar", responder literalmente:

```
Para revisar el diseño existente sin perder tus ediciones, ejecuta:
  /sdd-designer-system-review .sdd/drafts/{carpeta}/design/
```

Y **detente**. No lances `/sdd-designer-system-review` tú mismo.

Si el usuario elige "Regenerar", continuar con la Fase 1.

---

## 5. Fase 1 — Cargar contexto técnico

### 5.1 Cargar skills técnicos

Según las áreas que cubre el análisis:

- **Siempre** `k-sistemas` — arquitectura de dominios, servicios, controladores; convenciones de FQN y nombres de clase.
- **Siempre** `k-validaciones` — categorías V/R/U, en qué capa va cada tipo, cómo se redactan los mensajes.
- **Siempre** `k-code-quality` — reglas de calidad de Java/Kotlin (descomposición de métodos, responsabilidad única, nombrado, idiomas modernos, convenciones Axelor/Guice/JPA). Aplica al diseñar firmas, descomponer servicios en colaboradores y nombrar clases/métodos.
- **Siempre** `k-secure-coding` — frontera de confianza Axelor, mass-assignment, asignación incondicional de campos `servidor` en `*ServiceImpl.insert/update`, `AllowProperties` por acción, multi-centro/IDOR. **Determina** qué pieza implementa cada R-Antes-de-Crear y cómo se compone la lista blanca de cada `@CallMethod`.
- Si hay vistas o menús: `k-vistas` — estructura de ficheros XML, nombres de vistas y acciones.
- Si hay permisos o roles: `k-seguridad` — qué permisos/roles crear y cómo se nombran.

Son la fuente de verdad sobre **qué piezas existen y cómo se llaman**, no sobre el código exacto que las implementa.

### 5.2 Explorar código existente

- Leer el `CLAUDE.md` del proyecto para entender capas, convenciones, tipos de usuario y el árbol real de subsistemas existentes.
- Explorar `src/main/java/com/educaflow/subsystem/` y `src/main/java/com/educaflow/system/` para identificar qué reutilizar (FQN, dependencias) y qué dependencias potenciales hay con subsistemas existentes.
- Revisar `base/infrastructure/` para identificar utilidades reutilizables (PDF, mail, evaluator, etc.).

**MUST NOT** usar como referencia el código de `expedientes`/`tiposexpedientes`/`tramites` ni leer `design.md`/XML de diseños previos como plantilla (ver principio 2.1).

### 5.3 Cargar guías de diseño si existen

Comprobar si en `{iniciativa}/design-guidelines.md` existe el fichero:

- Si existe, validar el frontmatter `type: design-guidelines`. Si no lo tiene, detente con error:
  > Error: el fichero `{ruta}` no es un fichero de guías de diseño válido. Debe comenzar con:
  > ```
  > ---
  > type: design-guidelines
  > ---
  > ```
- Si la cabecera es correcta, extraer las guías como texto literal y mostrar al usuario: `Cargando guías de diseño desde {ruta}`.
- Si no existe, continuar sin guías (es opcional).

### 5.4 Derivar invariantes verificables a partir de las guías

**REQUIRED**: traducir las guías de `design-guidelines.md` (prosa libre) a invariantes nombradas y verificables antes de generar el diseño. Sin este paso, las guías se incumplen silenciosamente.

Este paso lo ejecuta **el agente principal** (no se delega en subagentes), justo después de cargar las guías y antes del pre-flight de conflictos.

#### Cómo derivar las invariantes

Por cada bloque/párrafo de `design-guidelines.md`, identificar:

1. **Encapsulaciones prescritas** ("X se encapsula en la clase Y", "la lógica de Z vive en W"): producen una invariante negativa de fuga.
   - Forma: `G-NNN: Solo el fichero/clase Y contiene <patrón concreto>. Cualquier referencia a <patrón> desde otros ficheros del diseño es violación.`
   - Es **clave** convertir la frase positiva ("encapsular en Y") en su contrapartida negativa verificable ("nadie fuera de Y referencia X-internals"), porque la negación es lo que un `grep` puede comprobar.
2. **Nombres prescritos** (de clase, paquete, método, fichero, propiedad de configuración): producen invariantes de identidad.
   - Forma: `G-NNN: La clase se llama exactamente <Nombre> y vive en el paquete <FQN>.`
3. **Patrones a evitar** ("no usar X", "evitar Y"): producen invariantes negativas directas.
   - Forma: `G-NNN: Ningún fichero del diseño puede contener <patrón>.`
4. **Mecánicas obligatorias** ("se hace asíncrono con scheduler", "se firma con HSM", "se valida con regex X"): producen invariantes de mecanismo.
   - Forma: `G-NNN: La operación <op> se realiza vía <mecanismo>, no de forma <alternativa>.`

#### Formato de las invariantes

Cada invariante lleva:

- **ID** `G-NNN` (numeración local secuencial dentro de la iniciativa).
- **Texto** en una frase corta, formulada como una afirmación que puede ser cierta o falsa al inspeccionar el `design.md` resultante.
- **Verificación**: `grep` concreto (si la invariante es un patrón textual sobre el diseño) o `manual` (si requiere lectura semántica).

Ejemplo (derivado del caso `enviar-correos`, design-guidelines que pedía encapsular SMTP en `MailSenderProvider`):

```
G-001  Solo `module/MailSenderProvider.java` lee las propiedades `mail.smtp.*` de AppSettings.
       Verificación: grep -rn "AppSettings.*mail\.smtp\|mail\.smtp\.[a-z]+" design/ design.md
                     → todas las coincidencias deben estar bajo la sección/fichero MailSenderProvider.

G-002  La clase de provisión se llama exactamente `MailSenderProvider` y vive en el paquete
       `com.educaflow.subsystem.correos.module`.
       Verificación: grep -n "MailSenderProvider" design.md  → debe aparecer; grep de variantes
                     (MailProvider, MailSenderFactory) → 0 coincidencias.

G-003  El envío de correos es asíncrono vía scheduler con cron de cada minuto, no síncrono en
       la creación de TareaCorreo.
       Verificación: manual (revisar paso del job y MetaSchedule).
```

#### Qué hacer con las invariantes

- Mostrar la lista al usuario con `AskUserQuestion` (`¿son correctas estas invariantes derivadas de tu guía?`) **solo si** alguna no es obvia o si el agente principal tiene dudas sobre la traducción. Si la derivación es mecánica y unívoca, no preguntar (no se piden aprobaciones cosméticas).
- Pasarlas a los subagentes en Fase 2 (ver §6.2).
- Re-verificarlas mecánicamente al final de Fase 2 (ver §6.7).
- Incluirlas en el `design.md` final (ver §8.4) para que `sdd-implementer-system` y `sdd-designer-system-review` puedan re-comprobarlas.

Si **no hay** `design-guidelines.md`, este paso se omite (no se inventan invariantes).

---

### 5.5 Pre-flight de conflictos guías ↔ análisis

Solo si hay guías cargadas:

- Comparar cada guía con el análisis (entidades, operaciones, vistas, validaciones, seguridad).
- Si detectas un conflicto (una guía contradice una decisión explícita del análisis), **detente y pregunta al usuario con `AskUserQuestion`**. Opciones:
  - (a) actualizar la guía manualmente,
  - (b) actualizar el análisis re-ejecutando `sdd-analyst-system`,
  - (c) ignorar el conflicto explícitamente.

No continuar hasta que el conflicto esté resuelto.

---

## 6. Fase 2 — Generación del diseño

### 6.1 Arquitectura: tres tareas secuenciales

La generación se hace en tres tareas estrictamente secuenciales:

1. **Tarea 2.1 — Candidatos**: lanzar **exactamente 5 subagentes en paralelo** que producen 5 propuestas de diseño independientes.
2. **Tarea 2.2 — Unificación**: el agente principal compara las 5 propuestas y produce el diseño unificado final.
3. **Tarea 2.3 — Diseño detallado de reglas R complejas**: sobre el diseño unificado, el agente principal identifica las reglas de negocio `R-` cuya implementación es compleja (clases auxiliares, tipos propios, máquinas de estado, integraciones externas) y lanza **un subagente por cada regla compleja** que produce un fichero `rules/R-<Entidad>-NNN.md` con el diseño detallado de las piezas necesarias.

La generación paralela en la Tarea 2.1 aporta diversidad de decisiones (troceo de pasos, nombres de clase, ubicación de reglas). La unificación posterior elige la mejor opción por cada decisión y resuelve dudas con el usuario. La Tarea 2.3 es **opcional** — si ninguna regla del análisis es lo suficientemente compleja, se omite.

### 6.2 Tarea 2.1 — 5 subagentes en paralelo

**CRITICAL**: lanza **exactamente 5 subagentes** en una **única respuesta** con 5 invocaciones a `Agent` simultáneas. **MUST NOT** lanzarlos secuencialmente. **MUST NOT** usar `run_in_background` (necesitas los resultados para la Tarea 2.2). Los 5 reciben **el mismo prompt** y devuelven solo el contenido del diseño en su mensaje de respuesta, sin escribir ningún fichero.

Los 5 subagentes **MUST NOT** usar `AskUserQuestion` (corren en paralelo). Si encuentran ambigüedad, eligen la interpretación más razonable y la registran en un bloque `=== DUDAS ===` al final de su respuesta; el agente principal recogerá las dudas de la candidatura ganadora y las llevará al usuario en la Tarea 2.2.

**Contenido del prompt único (común a los 5 subagentes):**

- El `analysis.md` completo y los `entity-*.md` / `screen-*.md` enlazados (texto literal).
- La carpeta de trabajo determinada en la Fase 0.
- El contexto técnico de la Fase 1: subsistemas reutilizables con su FQN (`com.educaflow.subsystem.X.db.Y`), infraestructura en `base/infrastructure/`, patrones reales de servicios y controladores ya implementados — **descritos como contrato**, no como código copiado.
- El contenido relevante de los skills cargados (`k-sistemas`, `k-validaciones`, `k-code-quality`, `k-vistas`, `k-seguridad`) resumido inline. **El subagente NO carga skills** — solo lee el prompt.
- Las **invariantes `G-NNN` derivadas en §5.4** (si había guías) en formato tabla, seguidas del **texto literal** de la guía. El subagente debe redactar el diseño de forma que **cada invariante quede satisfecha**. Para cada `G-NNN`, al final de su respuesta el subagente declara una tabla `G-NNN | ubicación en el diseño que la cumple | método de verificación`. Si una invariante no puede satisfacerse por incompatibilidad local con el análisis, va a `=== DUDAS ===` (no en "Conflictos detectados con guías" — esa sección se reserva para conflictos genuinos detectados durante la redacción, ortogonales a las invariantes). Si no había `design-guidelines.md`, omitir el bloque de invariantes.
- Los principios 2.2, 2.4, 2.5, 2.6 y 2.7 (transmitir literalmente).
- El formato de salida esperado (ver §6.2.2) y el checklist (ver §6.4).
- Las **tres tareas internas** del subagente (ver §6.2.1).

#### 6.2.1 Tareas internas del subagente

El prompt debe encargar al subagente, en este orden:

1. **Construir el diseño**: cabecera (Objetivo, Capa, Análisis de origen, Skills necesarios), tabla de ficheros a crear o modificar, y lista de pasos respetando el orden obligatorio (ver 6.3).
2. **Detallar contenido del diseño, XML y trazabilidad**:
   - **Dominios** — escribir el XML completo de cada entidad en un bloque ```xml etiquetado con la ruta destino:

     ````
     Fichero: design/domains/Bar.xml
     ```xml
     <?xml version="1.0" encoding="UTF-8"?>
     <domain-models ...>
       ...
     </domain-models>
     ```
     ````

     Válido contra `domain-models.xsd`.
   - **Servicios y controladores** — clases con FQN y, para cada una, todas las firmas de método (modificadores, retorno, parámetros, excepciones) con comentario descriptivo del cuerpo (qué reglas aplica, qué llamadas hace, qué efectos colaterales). **Sin código Java real dentro.**
   - **Vistas** — XML completo de cada fichero (`<grid>`, `<form>`, `<cards>`, `<action-method>`, `<action-attrs>`, `<action-validate>`, `<action-condition>`, `<action-record>`, `<action-group>`, `<action-view>`) en bloques etiquetados con ruta `design/views/<Fichero>.xml`, válido contra `object-views.xsd`. Acompañado de un resumen estructural corto (vistas declaradas, acciones, propósito) para que la lectura del `design.md` no exija abrir todos los XML.
   - **Menús** — XML completo de los `<menuitem>` en un bloque etiquetado `Fichero: design/menus.xml`. Válido contra `object-views.xsd`.
   - **Seguridad** — permisos, roles, grupos por nombre y la regla de acceso en lenguaje natural.
   - **Trazabilidad** — matriz con tres bloques (`V-<Entidad>-NNN`, `R-<Entidad>-NNN`, `U-<slug>-NNN`) → ubicación (clase.método o fichero XML + nombre de acción) demostrando que **toda regla del análisis está ubicada** según el mapeo de capas del principio 2.5.
3. **Aplicar el checklist y corregir antes de devolver** (ver 6.4). El subagente NO debe devolver el diseño hasta que todos los puntos del checklist estén satisfechos.

#### 6.2.2 Estructura del diseño que devuelve el subagente

```markdown
# Diseño: <Nombre>

**Objetivo:** <Una frase>
**Capa:** system|subsystem/<nombre>
**Análisis de origen:** .sdd/drafts/{carpeta-iniciativa}/analysis/analysis.md
**Skills necesarios para la implementación:** k-sistemas, k-code-quality, k-secure-coding, k-vistas[, k-seguridad]

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/foo/domains/Bar.xml` | Crear | k-sistemas (modelos.md) | Entidad Bar |
| `subsystem/foo/views/Bar.xml`   | Crear | k-vistas (forms.md, grids.md) | Vistas de Bar |
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | Añadir menú del subsistema |
| ... | | | |

## Pasos

### Paso N — <Título>
...

## Frontera de confianza — AllowProperties por controller
...

## Trazabilidad V/R/U → ubicación
...

=== DUDAS ===
- ...
=== END DUDAS ===
```

### 6.3 Reglas para los pasos

Cada paso debe:

- Tener un título claro.
- Indicar qué se va a crear o modificar **a nivel de estructura**, no a nivel de implementación.
- Para **dominios**: el XML completo en un bloque ```xml etiquetado con `design/domains/<Entidad>.xml`. Válido contra `domain-models.xsd`.
- Para **servicios/controladores**: clase con FQN y, para cada método, firma completa + comentario descriptivo del cuerpo. **MUST NOT** incluir el cuerpo implementado.
- Para **vistas**: el XML completo en un bloque ```xml etiquetado con `design/views/<Fichero>.xml`, válido contra `object-views.xsd`, acompañado de un resumen estructural corto (vistas declaradas, acciones, propósito).
- Para **menús**: el XML completo de los `<menuitem>` en un bloque ```xml etiquetado con `design/menus.xml`.
- Para **seguridad**: permisos, roles, grupos y reglas descritas en lenguaje natural.
- Ser lo suficientemente pequeño para implementarse y verificarse de forma independiente (≤ 30 minutos).
- Indicar qué verificar al final (¿compila?, ¿qué grep confirma que está bien?).

**Orden obligatorio de los pasos:**

1. **Ficheros estáticos y recursos** (si los hay) — plantillas PDF, esquemas XSD, certificados.
2. **Dominios** — XML completo de cada entidad, un bloque por entidad con ruta `design/domains/<Entidad>.xml`.
3. **Servicios** — interfaz `<Entidad>Service` (extiende `ModelService<Entidad>`) + implementación `<Entidad>ServiceImpl` (extiende `DefaultModelService<Entidad>`). Firma completa + comentario del cuerpo para cada método (constructor, CRUD, `validateInsert`/`validateUpdate`/`validateRemove`, `fireActionRule_*`, métodos de negocio).
4. **Repositorios** (si hay queries propias) — `db/repo/` con la lista de finders adicionales (firma + comentario del cuerpo).
5. **Controladores** (si hay lógica de botones) — clase con FQN; para cada `@CallMethod`, firma y comentario que indique en qué método de servicio delega. Parámetros llamados **siempre** `actionRequest` y `actionResponse` (ver principio 2.7).
6. **Vistas** — un fichero XML por `<action-view>` (regla "un `<action-view>` por fichero"). XML completo + resumen estructural por fichero.
7. **Menús** — modificación del `menus.xml` único del proyecto; en `design/menus.xml` la porción a fusionar.
8. **Seguridad** — `data-init/input/` con la lista de permisos, roles, grupos y la descripción en lenguaje natural de cada regla de acceso.
9. **Datos iniciales** — catálogos precargados (descripción de qué registros se cargan, no el XML de import).
10. **Verificación final** — compilar y confirmar que arranca sin errores. Comando exacto.

#### 6.3.1 Detalle del paso de servicios (cómo documentar V y R)

Cada firma de `validateInsert`/`validateUpdate`/`validateRemove` (para V-) y de `fireActionRule_*` (para R-) lleva un comentario que describe, **para cada regla del análisis ubicada en ese método**:

1. **Identificador** (`V-<Entidad>-NNN` o `R-<Entidad>-NNN`).
2. **Lógica resumida** — qué se comprueba (V) o qué hace el sistema (R).
3. Para V: **contenido del mensaje de error** descrito por lo que debe transmitir (valor recibido + dominio válido). **No el literal.**
4. Para R: **momento** (Antes/Después de `super.*`) y **efectos colaterales** previstos.
5. Si los valores válidos o las dependencias vienen de BD, indicar la fuente (catálogo, repositorio, etc.).

Ejemplo:

```java
// Clase: com.educaflow.subsystem.foo.service.impl.BarServiceImpl
// Método:
public Optional<BusinessMessages> validateInsert(Bar entidad);
//   Aplica:
//     - V-Bar-001 (alias del HSM): comprueba que el alias exista en el slot indicado.
//       Mensaje debe transmitir: alias recibido + slot recibido + lista de aliases
//       disponibles (obtenidos del repositorio de aliases del slot, envuelto en try/catch
//       para que un fallo de conectividad no bloquee otras validaciones).
//     - V-Bar-002 (longitud del nombre): comprueba que el nombre tenga entre 3 y 50
//       caracteres. Mensaje debe transmitir: nombre recibido + longitud actual + rango.
```

#### 6.3.2 Detalle del paso de servicios (cómo documentar campos `servidor` — defensa anti mass-assignment)

Para cada R-<Entidad>-NNN con momento `Antes` que asigna un campo clasificado como `servidor` en el análisis (columna "Origen del valor" del `entity-*.md`), el comentario del `fireActionRule_*` correspondiente **MUST** documentar explícitamente:

1. Que la asignación es **incondicional** (sin `if (campo == null)`). Ver `[[k-secure-coding]]` §2.
2. El origen del valor (`LocalDateTime.now()`, `AuthUtils.getUser().getCentro()`, constante del enum, etc.).
3. Que el cliente NO puede dictar este campo aunque venga relleno en el JSON del endpoint REST genérico.

✅ CORRECTO (comentario en `design.md`):

```java
private void fireActionRule_AsignarFechaCreacion(Bar bar);
//   Aplica R-Bar-001 (campo `fechaCreacion`, clasificado `servidor` en entity-Bar.md):
//   asignación INCONDICIONAL `bar.setFechaCreacion(LocalDateTime.now())`.
//   MUST NOT añadir guarda `if (bar.getFechaCreacion() == null)`: permitiría que un
//   atacante por el endpoint REST genérico cuele una fecha falsificada (ver k-secure-coding §2).
```

❌ INCORRECTO:

```java
private void fireActionRule_AsignarFechaCreacion(Bar bar);
//   Si fechaCreacion es null, asignar LocalDateTime.now().
```

Para campos inmutables tras crear (típico `fechaCreacion`, `numeroSecuencial`), el `design.md` **MUST** excluirlos de la whitelist `allowPropertiesUpdate` para que el cliente no pueda enviarlos (ver `[[k-secure-coding]]` §3.2 regla 1).

#### 6.3.3 Sección "Frontera de confianza — AllowProperties por acción"

El `design.md` final **MUST** llevar una sección `## Frontera de confianza — AllowProperties por acción` con una tabla por cada acción del servicio invocada desde un `@CallMethod`. La tabla materializa la decisión de seguridad sobre qué campos del bean acepta esa acción.

> Las reglas de validez (qué forma elegir, qué campos pueden o no estar) viven en `[[k-secure-coding]]` §3. Este apartado solo fija **el formato del documento**; las reglas no se repiten aquí.

**Formato de cada tabla**:

```markdown
### `BarServiceImpl.<accion>` (invocado desde `BarController.<callMethod>`)

Entidad: `Bar`. **Forma elegida**: `createAllowProperties` | `createAllowAllProperties`.

| Campo            | Origen   | En whitelist | Justificación / Ubicación de la asignación              |
|------------------|----------|--------------|---------------------------------------------------------|
| `nombre`         | cliente  | sí           | Input directo del usuario.                              |
| `fechaCreacion`  | servidor | **NO**       | Asignada en `BarServiceImpl.insert` → `fireActionRule_…`; en `update` no se toca (excluida de la whitelist). |
| `estado`         | servidor | **NO**       | Recalculada en `BarServiceImpl.update` → `fireActionRule_…`. |
```

Si hay alta programática vía DTO (`record`), añadir sub-apartado `### DTO de alta programática` con los campos del record y justificación de cualquier `servidor` que aparezca.

Esta sección es el contrato de seguridad. `sdd-designer-system-review` la valida aplicando `[[k-secure-coding]]` §3, `sdd-implementer-system` la usa para generar el `allowPropertiesXxx` real, y los code-reviews humanos la consultan ante cualquier campo nuevo.

### 6.4 Checklist que el subagente aplica en su Tarea 3

El subagente revisa su propio diseño contra esta lista y corrige antes de devolverlo. Si algún punto no se cumple, **MUST NOT** devolver el diseño hasta arreglarlo. **LIMIT**: máximo 3 iteraciones de auto-corrección; si tras la 3ª sigue sin cumplirse algún punto, lo deja registrado en `=== DUDAS ===` y devuelve.

- [ ] ¿Cada paso tiene toda la información para que un implementador entienda qué hay que crear sin leer el resto del diseño?
- [ ] ¿Los nombres de clases, métodos, ficheros y acciones son coherentes entre todos los pasos?
- [ ] ¿Ningún paso contiene placeholders del tipo "TBD", "similar a", "según convenga"? (si los hay, sustituir por contenido concreto)
- [ ] ¿El paso de verificación final incluye el comando exacto de compilación?
- [ ] ¿El paso de dominios incluye el XML completo de cada entidad en un bloque ```xml etiquetado con `design/domains/<Entidad>.xml`? El XML debe ser sintácticamente válido contra `domain-models.xsd`.
- [ ] ¿El paso de servicios contiene SOLO firmas de método con comentarios descriptivos del cuerpo, y NO cuerpos implementados? Si hay código Java real (lógica, `if`, `for`, `messages.add(...)` con literales), eliminarlo y dejarlo como comentario.
- [ ] ¿El paso de vistas incluye el XML completo de cada fichero en un bloque ```xml etiquetado con `design/views/<Fichero>.xml`, acompañado de un resumen estructural? Válido contra `object-views.xsd`.
- [ ] ¿Hay un bloque ```xml etiquetado con `design/menus.xml`? Válido contra `object-views.xsd`.
- [ ] ¿Cada `<action-view>` está declarado en su propio fichero (regla 2.7)? Excepción: `@Search-grid`+`@View-form` van juntos en `<NombreEntidad>-ref.xml`.
- [ ] ¿La tabla "Ficheros a crear o modificar" lista los menús como "Modificar `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`", no como un fichero nuevo `menus-<subsistema>.xml`?
- [ ] ¿Los parámetros de los métodos del controlador se llaman `actionRequest` y `actionResponse`?
- [ ] ¿Cada método en el paso de servicios tiene un comentario que indica qué reglas `V-`/`R-` aplica, qué lógica ejecuta y qué transmiten los mensajes de error?
- [ ] ¿Cada acción de vista declarada tiene un comentario de su propósito y los campos/condiciones que intervienen?
- [ ] ¿Las reglas están mapeadas a la capa correcta según el principio 2.5?
- [ ] ¿El diseño tiene la sección `## Frontera de confianza — AllowProperties por acción` con una tabla por cada acción del servicio invocada desde un `@CallMethod`, en el formato fijado en §6.3.3, y pasando las reglas de `[[k-secure-coding]]` §3?
- [ ] ¿Cada R-<Entidad>-NNN con momento `Antes` que asigna un campo `servidor` tiene un comentario en su `fireActionRule_*` que documenta asignación **incondicional** (sin `if (campo == null)`) y referencia `[[k-secure-coding]]` §3.3?
- [ ] ¿Ningún cuerpo de método del diseño contiene el anti-patrón `if (campo == null) setCampo(...)` para campos `servidor`?
- [ ] ¿TODAS las reglas `V-` del análisis están ubicadas con un comentario que describe qué se comprueba y qué transmite el mensaje?
- [ ] ¿TODAS las reglas `R-` del análisis están ubicadas como `fireActionRule_*` con comentario que describe qué hace, sobre qué entidad, en qué operación y con qué momento (Antes/Después)?
- [ ] ¿TODAS las reglas `U-` del análisis están ubicadas en la vista correspondiente con su mecanismo concreto (atributo inline `*If` o nombre de `<action-attrs>`/`<action-record>` con su evento)?
- [ ] ¿La matriz V/R/U → ubicación tiene una entrada por cada regla del análisis y cada entrada apunta a una clase + método o fichero XML + nombre de acción/atributo?
- [ ] ¿Ningún paso crea un módulo Guice para un `ModelService`? (si lo crea, eliminarlo — regla 2.7)
- [ ] ¿Ningún paso crea un listener JPA para lógica de negocio? (si lo crea, moverlo al servicio como `fireActionRule_*`)
- [ ] ¿Cada paso es lo suficientemente pequeño para implementarse y verificarse en ≤ 30 minutos?
- [ ] ¿Los pasos respetan el orden obligatorio de 6.3?
- [ ] ¿El diseño referencia el `analysis.md` en la cabecera?
- [ ] ¿El diseño respeta todas las guías de diseño recibidas? Si alguna no se ha podido respetar por incompatibilidad con el análisis, ¿está documentada en una sección "Conflictos detectados con guías"?

### 6.5 Tarea 2.2 — Unificación (agente principal)

Una vez recibidas las 5 candidaturas, **tú mismo** (no un subagente) produces el diseño unificado:

1. **Comparar las 5 candidaturas** sección por sección y paso por paso.
2. **Para cada decisión donde haya divergencia** (troceo de pasos, nombres de clases o métodos, estructura de vistas, ubicación de cada regla), escoge la mejor opción según los principios de `k-sistemas`, `k-validaciones`, `k-vistas` y `k-seguridad`. En empate razonable, elige la opción que minimiza ambigüedad para el implementador.
3. **Tabla de ficheros a crear o modificar**: consolida la unión de todos los ficheros propuestos, eliminando duplicados y descartando los que no aporten valor real (helpers innecesarios introducidos por uno solo de los diseños y no requeridos por el análisis).
4. **Pasos**: escoge el troceo más limpio (cada paso ≤ 30 minutos, autocontenido, con verificación clara al final). Combina lo mejor de cada candidatura respetando el orden obligatorio.
5. **Dominios, vistas y menús (XML)**: para cada fichero escoge la versión más correcta según `k-sistemas` y `k-vistas` y la coherencia con subsistemas existentes.
6. **Firmas de servicios y controladores**: escoge las firmas y comentarios más claros. Si una candidatura tiene comentarios más detallados sobre las reglas que aplica un método, úsalos.
7. **Trazabilidad V/R/U**: construye una matriz con los tres bloques que cubra **todas** las reglas del análisis. Cada fila apunta al método o acción concreta del diseño. Si alguna regla queda sin ubicación, **MUST** completarla antes de cerrar — **MUST NOT** cerrar con huecos de cobertura.
8. **Renumera los pasos** de forma consecutiva sin huecos, respetando el orden obligatorio.
9. **Guías de diseño**: aplícalas como criterio adicional en cualquier empate. Si una opción respeta una guía y la otra no, escoge la que la respeta.
10. **Dudas y conflictos**:
    - Toma el bloque `=== DUDAS ===` de la candidatura ganadora (y de las demás si añaden dudas relevantes); plantea cada una al usuario con `AskUserQuestion` y aplica las respuestas al diseño.
    - Consolida los "Conflictos detectados con guías" de los 5 subagentes. Si tras la unificación queda algún conflicto sin resolver, **detente y pregunta al usuario con `AskUserQuestion`** antes de cerrar (opciones: actualizar guía, reabrir análisis, ignorar).

Si en la unificación detectas algo ambiguo o faltante que ninguna candidatura resolvió, decide la opción más conservadora (que mantenga trazabilidad con el análisis y respete los skills) y anota el motivo en una sección "Notas de unificación" al final del diseño, **fuera de los pasos** (no contamina la implementación).

**LIMIT**: máximo 3 rondas de `AskUserQuestion` durante la unificación (dudas + conflictos guías). Si tras la 3ª siguen abiertos puntos críticos, **STOP** y pide al usuario que reabra el análisis o las guías.

El resultado de la Tarea 2.2 es el **diseño unificado** que pasa a la Tarea 2.3.

### 6.6 Tarea 2.3 — Diseño detallado de reglas R complejas

Sobre el diseño unificado de la Tarea 2.2, el agente principal recorre la matriz de trazabilidad `R-<Entidad>-NNN` → ubicación y decide cuáles requieren un fichero de diseño detallado aparte.

#### 6.6.1 Criterios para considerar una regla "compleja"

Una regla `R-<Entidad>-NNN` se considera compleja — y por tanto necesita su propio fichero `rules/R-<Entidad>-NNN.md` — si su implementación cumple **al menos uno** de estos criterios:

- Necesita **clases auxiliares** propias (helpers, builders, calculadoras, parsers, generadores) que no encajan en el `*ServiceImpl` y que no son utilidades genéricas de `base/infrastructure/`.
- Necesita **tipos propios** del dominio de la regla (DTOs, value objects, records, sealed types) que no son entidades JPA y no existen ya.
- Necesita **interfaces nuevas** (contratos para estrategias, adaptadores de integración, ports de hexagonal).
- Implementa una **máquina de estados** con transiciones, guardas y acciones por transición.
- Coordina **varios subsistemas** o servicios (más de dos colaboradores externos al servicio donde vive `fireActionRule_*`).
- Integra con un **sistema externo** (correo SMTP, HSM, firma, OCR, registro telemático, pasarela de pagos, etc.) más allá de un wrapper trivial.
- Aplica un **algoritmo no trivial** (planificación, optimización, conciliación, paginación específica, retry/backoff con políticas) que merece quedar documentado.
- Tiene **efectos colaterales transaccionales** complejos (commit/rollback parcial, idempotencia, deduplicación, locks).
- Genera artefactos (PDF, CSV, XML firmado) con su propio diseño de plantilla, contenido y composición.

Una regla que se reduce a 2-3 llamadas directas a un servicio existente **no** es compleja: se documenta inline en el comentario del `fireActionRule_*` del `design.md` y no necesita fichero aparte.

#### 6.6.2 Cómo lanzar los subagentes

Para cada regla compleja identificada, **lanza un subagente** con `Agent`. Si hay varias reglas complejas independientes entre sí:

- **CRITICAL**: lánzalos **todos en una única respuesta** con N invocaciones a `Agent` simultáneas (una por regla compleja). **MUST NOT** lanzarlos secuencialmente.
- **MUST NOT** usar `run_in_background` — necesitas los resultados completos para continuar con la Fase 3.
- **MUST NOT** usar `AskUserQuestion` dentro de los subagentes: como en la Tarea 2.1, registran sus dudas en un bloque `=== DUDAS ===` al final y el agente principal las recoge tras la espera.

**Contenido del prompt de cada subagente (uno por regla compleja):**

- El identificador de la regla (`R-<Entidad>-NNN`) y su descripción literal extraída del `entity-<Entidad>.md` del análisis.
- La entidad afectada y la operación que la dispara (insert/update/remove/operación custom) según la tabla `Acciones` del análisis.
- El momento previsto (Antes/Después de `super.*`) decidido en la unificación.
- El FQN de la clase y el nombre del método `fireActionRule_*` donde vivirá (decidido en la unificación).
- El contexto técnico relevante de la Fase 1: subsistemas existentes que puede reutilizar, infraestructura de `base/infrastructure/` disponible, FQN de tipos y servicios ya implementados.
- El contenido relevante de `k-code-quality` resumido inline (reglas de descomposición de métodos, responsabilidad única, nombrado, idiomas Java modernos) — aplica al diseñar las clases nuevas, interfaces y tipos propios del diseño detallado.
- Los principios 2.1, 2.2 y 2.7 (transmitir literalmente).
- La instrucción de **NO usar `AskUserQuestion`** y de registrar dudas en `=== DUDAS ===`.
- Las dos tareas internas del subagente (ver 6.6.3) y el formato de salida esperado (ver 6.6.4).

#### 6.6.3 Tareas internas del subagente

El subagente ejecuta **estas dos tareas en orden**:

1. **Análisis de la regla**: antes de proponer ningún diseño, escribe (dentro de su respuesta, en la sección `## Análisis de la regla` del fichero markdown) qué hace la regla en términos funcionales — desglosada paso a paso:
   - Qué se dispara y cuándo (entidad, operación, momento).
   - Qué información necesita leer y de dónde (otras entidades, parámetros del request, configuración, integraciones externas).
   - Qué acciones realiza y en qué orden (cálculos, llamadas, escrituras, notificaciones).
   - Qué efectos colaterales produce y qué garantías de transaccionalidad/idempotencia debe cumplir.
   - Qué casos de error o excepciones puede encontrar y cómo deben tratarse (qué se reintenta, qué se ignora, qué se propaga).
   - Qué entradas/salidas tiene cada colaborador identificado.

   **Solo después de tener el análisis completo** pasa a la siguiente tarea.

2. **Diseño detallado**: a partir del análisis, define las piezas que hacen falta — sin escribir el cuerpo Java:
   - **Clases nuevas** con su FQN, su responsabilidad en una frase y sus métodos (firma completa + comentario descriptivo del cuerpo).
   - **Interfaces** con sus métodos y la justificación de por qué se necesita la abstracción (estrategia, adaptador, port).
   - **Tipos propios** (DTOs, value objects, records, sealed types, enums) con sus campos y su semántica.
   - **Diagrama de secuencia** en ASCII o como lista numerada (`fireActionRule_* → A.metodo1 → B.metodo2 → …`) que muestre el orden de llamadas entre los colaboradores.
   - **Tabla de errores**: para cada excepción/condición de error, qué pieza la genera, cómo se traduce a `BusinessMessages` o se propaga.
   - **Contenido del método `fireActionRule_*`** — únicamente la firma + un comentario que liste, en orden, las llamadas a los colaboradores del diseño detallado y referencie este fichero como fuente del diseño completo. **Sin código Java real dentro.** Este bloque es el que el agente principal copiará en `design.md` para sustituir el comentario inline original.

#### 6.6.4 Formato de salida del subagente

El subagente devuelve **dos cosas** en su respuesta:

1. El **contenido completo del fichero markdown** `rules/R-<Entidad>-NNN.md`, dentro de un bloque etiquetado `=== FILE: rules/R-<Entidad>-NNN.md ===` … `=== END FILE ===`. Estructura del fichero:

   ```markdown
   # R-<Entidad>-NNN — <título corto de la regla>

   **Entidad:** <Entidad>
   **Operación:** insert | update | remove | <operación custom>
   **Momento:** Antes | Después de super.*
   **Servicio host:** com.educaflow.subsystem.<x>.service.impl.<Entidad>ServiceImpl
   **Método host:** fireActionRule_<nombreLegible>(<firma>)

   ## Análisis de la regla
   <descripción funcional paso a paso del qué/cuándo/cómo/errores>

   ## Diseño detallado

   ### Clases nuevas
   - <FQN> — <responsabilidad en una frase>
     - <firma de método> — <comentario>
     - …

   ### Interfaces
   - <FQN> — <responsabilidad y justificación>
     - <firma de método> — <comentario>

   ### Tipos propios
   - <FQN> (record/value object/enum) — <campos> — <semántica>

   ### Diagrama de secuencia
   fireActionRule_<x>
     ├─ <Colaborador1>.metodo(...) → <qué devuelve>
     ├─ <Colaborador2>.metodo(...) → <qué devuelve>
     └─ …

   ### Errores
   | Condición | Origen | Tratamiento |
   |-----------|--------|-------------|
   | <cuándo>  | <clase.método> | <BusinessMessages | excepción | log + retry | …> |

   ### Contenido del método `fireActionRule_*`
   ```java
   // Firma:
   <firma completa>
   //   Implementa R-<Entidad>-NNN. Diseño detallado en design/rules/R-<Entidad>-NNN.md.
   //   Secuencia:
   //     1. <llamada 1>
   //     2. <llamada 2>
   //     …
   ```
   ```

2. El **bloque del método `fireActionRule_*`** que el agente principal debe injertar en el paso de servicios del `design.md`, dentro de un bloque etiquetado `=== FIRE-ACTION ===` … `=== END FIRE-ACTION ===`. Es el mismo contenido que la última sección del fichero markdown (firma + comentario que referencia el fichero), repetido aquí para que el agente principal lo localice sin parsear el markdown.

#### 6.6.5 Qué hace el agente principal con la respuesta del subagente

Por cada subagente terminado:

1. Extraer el bloque `=== FILE: rules/R-<Entidad>-NNN.md ===` y **guardarlo en memoria** — el fichero físico se escribe en la Fase 4 junto al resto. No lo escribas todavía.
2. Extraer el bloque `=== FIRE-ACTION ===` y **sustituir** en el diseño unificado el comentario inline previo del método `fireActionRule_*` correspondiente por este nuevo contenido (que ahora referencia el fichero `design/rules/R-<Entidad>-NNN.md`).
3. Asegurarse de que la **tabla de trazabilidad V/R/U** del diseño marca la regla compleja con un puntero al fichero detallado, p.ej.:

   ```
   | R-Bar-003 | BarServiceImpl.fireActionRule_publicar (Después de super.update) | Detalle: design/rules/R-Bar-003.md |
   ```

4. Recoger las **dudas** del bloque `=== DUDAS ===` (si las hubiera) y plantearlas al usuario con `AskUserQuestion` antes de pasar a la Fase 3. Aplicar las respuestas al fichero markdown en memoria.

#### 6.6.6 Si no hay reglas R complejas

Si tras revisar todas las `R-` del análisis ninguna cumple los criterios de 6.6.1, **se omite la Tarea 2.3** completa. La carpeta `design/rules/` no se crea y el `design.md` no contiene referencias a ficheros de detalle de reglas. Esto es esperable en subsistemas CRUD sencillos.

---

### 6.7 Verificación mecánica de las invariantes derivadas de las guías

Tras la unificación (y, si existió, la Tarea 2.3) y antes de entrar en la Fase 3 de revisión, **el agente principal vuelve a comprobar cada invariante `G-NNN` derivada en §5.4 contra el diseño unificado** — no contra la declaración que hicieron los subagentes, sino contra el texto real del diseño y los XML generados.

Este paso solo aplica si en §5.4 se derivaron invariantes. Si no había `design-guidelines.md`, se omite.

#### Procedimiento

Para cada invariante `G-NNN`:

1. **Si la verificación es `grep`** (la mayoría):
   - Ejecutar el `grep` exacto definido en la columna "Verificación" de la invariante, sobre el texto completo del diseño unificado **y** sobre los bloques de XML embebidos (en este punto los XML todavía no se han materializado a `design/`, así que el grep va sobre el contenido en memoria).
   - Filtrar las coincidencias permitidas (las que la invariante autoriza, p.ej. dentro de la sección del `MailSenderProvider`).
   - Si quedan coincidencias **no permitidas** → la invariante está violada.
2. **Si la verificación es `manual`**:
   - Releer las secciones del diseño relevantes a la invariante y juzgar si se cumple.
   - Si se viola → tratarla como las del grep.

#### Qué hacer si una invariante está violada

No marcar el diseño como bueno. Se elige una de estas dos vías, según la gravedad y unicidad de la fuga:

- **Fuga local y obvia** (una sola referencia mal puesta, p.ej. el helper `construirMail` leyendo `AppSettings.get("mail.smtp.user")` cuando la invariante exigía encapsular SMTP en el provider): el agente principal **edita el diseño unificado en memoria** para mover la responsabilidad al sitio que la invariante exige, dejando una nota corta en "Notas de unificación" que mencione la corrección. Repetir la verificación. **LIMIT**: máximo 3 ediciones-revalidaciones por invariante; si tras la 3ª sigue violada, tratarla como fuga estructural.
- **Fuga estructural** (varias referencias diseminadas, contradicción de fondo, invariante incompatible con el análisis): **detenerse y preguntar al usuario** con `AskUserQuestion`. Opciones:
  - (a) Reabrir Tarea 2.1 con un prompt reforzado que recalca la invariante violada,
  - (b) Reformular la invariante en `design-guidelines.md` (la guía resultó ser ambigua o sobre-restrictiva),
  - (c) Aceptar la violación como excepción explícita documentada en el diseño (último recurso — debe quedar en una sección "Excepciones a las invariantes" con justificación).

**MUST NOT** avanzar a Fase 3 con invariantes violadas y sin documentar la excepción.

---

## 7. Fase 3 — Revisión del diseño unificado

Aunque cada subagente ya aplicó el checklist 6.4 sobre su propia candidatura, debes volver a aplicarlo sobre el **diseño unificado** — la unificación puede haber introducido inconsistencias (numeración de pasos, nombres mezclados, descripciones combinadas) que ningún subagente individual podía detectar.

Antes de pasar a la Fase 4, comprueba sobre el diseño unificado:

- [ ] Todos los puntos del checklist 6.4 sobre el contenido unificado.
- [ ] ¿La tabla "Ficheros a crear o modificar" del proyecto real (no de `design/`) es coherente con los bloques XML generados? Por ejemplo, si hay un bloque `design/views/Bar-Pendiente.xml`, debe haber una fila para el fichero correspondiente en `src/main/...` del proyecto real.
- [ ] ¿La matriz V/R/U final tiene una entrada por cada regla del análisis sin huecos?
- [ ] **¿Cada regla `R-` que cumple los criterios de 6.6.1 tiene su fichero `rules/R-<Entidad>-NNN.md` en memoria (a escribir en 8.2) y su comentario inline del `fireActionRule_*` ha sido sustituido por el contenido del bloque `=== FIRE-ACTION ===` que referencia el fichero?** Si una regla compleja sigue documentada solo inline, lanzar el subagente de la Tarea 2.3 para esa regla antes de continuar.
- [ ] **¿La matriz de trazabilidad de cada regla compleja `R-` incluye el puntero `Detalle: design/rules/R-<Entidad>-NNN.md`?**
- [ ] ¿La verificación mecánica de §6.7 quedó limpia para todas las invariantes `G-NNN`? Si alguna requirió excepción, ¿está documentada en la sección "Excepciones a las invariantes" del `design.md` con razón y ubicación? El bullet genérico "respeta las guías" se considera cubierto por la §6.7 — este checklist solo verifica que esa sub-fase se ejecutó y dejó trazas en el `design.md`.

Si encuentras algún problema, corrígelo antes de pasar a la Fase 4. **LIMIT**: máximo 3 pasadas de revisión-corrección sobre el diseño unificado; si tras la 3ª siguen apareciendo problemas no triviales, **STOP** y pregunta al usuario.

---

## 8. Fase 4 — Materializar y validar

**MUST NOT** mostrar el diseño unificado al usuario ni preguntar si lo aprueba antes de escribir los ficheros. Tras la revisión interna de la Fase 3, el skill pasa directamente a escribir XML, `design.md`, `rules/*.md` y `tests.md`. El usuario revisará el `design/` ya materializado y, si quiere cambios, los edita a mano o lanza `/sdd-designer-system-review`.

> **REQUIRED** — ubicación del diseño: se guarda en la subcarpeta `design/` dentro de la carpeta de la iniciativa (la misma que contiene `analysis/`). Ejemplo: `.sdd/drafts/2026-05-11_23-19_tareas-de-envio-de-correos/design/`. **MUST NOT** guardarse en la raíz del proyecto ni en otra carpeta.

### 8.1 Borrar diseño previo

Borrar recursivamente la carpeta `design/` si ya existe y recrear el esqueleto:

```bash
rm -rf .sdd/drafts/{carpeta-iniciativa}/design
mkdir -p .sdd/drafts/{carpeta-iniciativa}/design/domains
mkdir -p .sdd/drafts/{carpeta-iniciativa}/design/views
# Solo si la Tarea 2.3 produjo ficheros de reglas complejas:
mkdir -p .sdd/drafts/{carpeta-iniciativa}/design/rules
```

Esto sustituye sin ambigüedad cualquier diseño previo. No se conservan iteraciones anteriores.

### 8.2 Extraer los XML del diseño unificado y los ficheros de reglas, y escribirlos como ficheros reales

1. **XML**: recorre el diseño unificado y, por cada bloque ```xml etiquetado con una línea `Fichero: design/...`, escribe ese contenido como fichero en la ruta indicada (`Write`).
2. **Ficheros de reglas complejas** (si la Tarea 2.3 los produjo): por cada bloque `=== FILE: rules/R-<Entidad>-NNN.md ===` que guardaste en memoria tras la Tarea 2.3, escríbelo en `design/rules/R-<Entidad>-NNN.md` (`Write`).
3. **Tests E2E (copia literal de `analysis/tests.md`)**: el diseñador **no toca** el contenido del `tests.md`; solo lo pasa al siguiente eslabón del pipeline. Copia el fichero tal cual:

   ```bash
   cp .sdd/drafts/{iniciativa}/analysis/tests.md .sdd/drafts/{iniciativa}/design/tests.md
   ```

   Si `analysis/tests.md` no existe (iniciativa antigua sin tests), saltar este paso con un aviso al usuario: `/sdd-implementer-system` saltará el bucle de tests. **MUST NOT** regenerar, reformatear o resumir el `tests.md` — su contenido es contrato fijo entre análisis e implementación (mismo principio que con los XML).

Estructura resultante esperada:

```
.sdd/drafts/{iniciativa}/design/
├── design.md                       ← se escribe en 8.4
├── domains/<Entidad>.xml           ← uno por entidad
├── views/<Fichero>.xml             ← uno por <action-view> + ficheros *-ref.xml
├── menus.xml                       ← <menuitem> a fusionar con el menus.xml del proyecto
├── tests.md                        ← copia literal de analysis/tests.md
└── rules/R-<Entidad>-NNN.md        ← solo si hay reglas R complejas (Tarea 2.3)
```

### 8.3 Validar cada XML con xmllint

Una vez escritos, validar **cada** fichero XML con `xmllint --noout --schema <xsd> <fichero>`:

- **Dominios** → `../axelor-open-platform/axelor-core/src/main/resources/domain-models.xsd`
- **Vistas y menús** → `../axelor-open-platform/axelor-core/src/main/resources/object-views.xsd`

Comandos concretos (ejecutar para cada fichero):

```bash
# Dominios
for f in .sdd/drafts/{iniciativa}/design/domains/*.xml; do
  xmllint --noout --schema ../axelor-open-platform/axelor-core/src/main/resources/domain-models.xsd "$f" || echo "FAIL: $f"
done

# Vistas
for f in .sdd/drafts/{iniciativa}/design/views/*.xml; do
  xmllint --noout --schema ../axelor-open-platform/axelor-core/src/main/resources/object-views.xsd "$f" || echo "FAIL: $f"
done

# Menús
xmllint --noout --schema ../axelor-open-platform/axelor-core/src/main/resources/object-views.xsd \
  .sdd/drafts/{iniciativa}/design/menus.xml
```

**Si algún fichero falla la validación:**

1. Lee el error de `xmllint` y corrige el XML con `Edit` sobre el fichero.
2. Vuelve a ejecutar `xmllint` sobre ese fichero. **LIMIT**: máximo 3 iteraciones de corrección por fichero.
3. Si tras la 3ª iteración el error persiste por una incompatibilidad real con el XSD (atributo inexistente, estructura no permitida), **STOP** y muestra el error al usuario. **MUST NOT** escribir un diseño con XML inválido. Pide al usuario que aclare o reabra el análisis.

La Fase 4 **MUST NOT** considerarse terminada hasta que **todos** los XML pasan `xmllint` sin errores.

### 8.4 Escribir el `design.md`

Escribir el `design.md` en la raíz de `design/`. **REQUIRED** frontmatter:

```
---
type: design
---

{contenido del diseño unificado, con resumen estructural por cada XML — no el XML inline}
```

El `design.md` **no contiene** los XML completos inline (esos viven en sus ficheros); en su lugar contiene, por cada fichero XML generado, una entrada con su ruta y el resumen estructural (vistas, acciones, propósito).

#### Sección obligatoria "Invariantes de las guías"

Si en §5.4 se derivaron invariantes `G-NNN`, el `design.md` debe incluir **al final** (antes de "Conflictos detectados con guías" si la hay) una sección con esta forma:

```markdown
## Invariantes de las guías

Estas invariantes se derivaron de `design-guidelines.md` y se verificaron mecánicamente
en §6.7 contra el diseño unificado. Sirven de contrato para `sdd-implementer-system`
(las re-verifica sobre el código Java generado) y `sdd-designer-system-review`
(las re-verifica sobre el diseño materializado).

| ID    | Invariante | Ubicación que la cumple | Verificación |
|-------|------------|-------------------------|--------------|
| G-001 | Solo `module/MailSenderProvider.java` lee `mail.smtp.*` de AppSettings. | Paso 7 del diseño (Provider) | `grep -rn "AppSettings.*mail\.smtp\|mail\.smtp\.[a-z]+" design/ design.md` → todas las coincidencias bajo el bloque del Provider. |
| G-002 | …          | …                       | …            |
```

**Si una invariante quedó como excepción explícita** (vía §6.7 opción c), añadir además una subsección:

```markdown
### Excepciones a las invariantes

- **G-NNN** — Excepción aceptada por el usuario el {fecha}. Razón: {motivo}. Ubicación de la fuga aceptada: {ruta/sección}.
```

Si **no había guías** y por tanto no hay invariantes, omitir toda la sección (no escribir un encabezado vacío).

---

## 9. Fase 5 — Mensaje de cierre al usuario

```
Diseño guardado en .sdd/drafts/{carpeta-iniciativa}/design/

Ficheros generados:
  - design.md
  - domains/ (N ficheros XML — validados contra domain-models.xsd)
  - views/   (M ficheros XML — validados contra object-views.xsd)
  - menus.xml (validado contra object-views.xsd)
  - tests.md  (copia literal de analysis/tests.md — los ejecutará /sdd-implementer-system con playwright-cli)
  - rules/   (K ficheros markdown — solo si hay reglas R complejas)

Si quieres iterar sobre este diseño, puedes:
  1. Editar (o crear) .sdd/drafts/{carpeta-iniciativa}/design-guidelines.md con guías
     adicionales. Debe empezar con:
       ---
       type: design-guidelines
       ---
     Las guías persisten a nivel de iniciativa.
  2. Re-ejecutar:
     /sdd-designer-system .sdd/drafts/{carpeta-iniciativa}/analysis/analysis.md
     (la carpeta design/ anterior se borrará y se generará una nueva).

Para implementar este diseño tal cual ejecuta:
  /sdd-implementer-system .sdd/drafts/{carpeta-iniciativa}/design/design.md
```

**MUST NOT** lanzar `sdd-implementer-system` tú mismo. El usuario decide cuándo ejecutarlo.

---

## Quick Guidelines

- El análisis es la fuente de verdad. **MUST NOT** inventar elementos no presentes en él; ante ambigüedad, `AskUserQuestion`.
- Diseño ≠ implementación: XML completo va a ficheros reales en `design/domains|views/` + `design/menus.xml`; para Java solo firmas + comentarios del cuerpo. **MUST NOT** incluir lógica Java real.
- **Cobertura total V/R/U** sin huecos; matriz de trazabilidad apuntando a ubicación concreta. Un `<action-view>` por fichero; `<menuitem>` al `menus.xml` único del proyecto (**MUST NOT** crear `menus-<subsistema>.xml`).
- Validación `xmllint` obligatoria contra `domain-models.xsd` y `object-views.xsd`. **LIMIT**: 3 iteraciones por fichero; tras la 3ª, **STOP**.
- `tests.md` se copia literal de `analysis/` a `design/`. **MUST NOT** modificarlo, reformatearlo ni resumirlo.
- Generación: **CRITICAL** lanzar exactamente 5 subagentes en una única respuesta (Tarea 2.1) → unificación (agente principal) → 1 subagente por regla R compleja (Tarea 2.3). Subagentes paralelos **MUST NOT** usar `AskUserQuestion`.
- Si hay `design-guidelines.md`: derivar invariantes `G-NNN` verificables (§5.4), re-verificarlas mecánicamente (§6.7) e incluirlas en el `design.md` final.

---

## Apéndice A — Override de rutas (para testing)

- `--in=<ruta>` — fichero `analysis.md` de entrada explícito. **Desactiva la auto-detección** de la Fase 0 caso 2. La "carpeta de la iniciativa" es la carpeta padre de la carpeta `analysis/` que contiene ese fichero.
- `--out=<ruta>` — **carpeta** donde se materializa la estructura `design/`. Sustituye literalmente a `{carpeta-iniciativa}/design/` en Fase 4 y en el mensaje de Fase 5. Si ya existe, se **borra recursivamente** antes de escribir.
- `--root=<ruta>` — raíz alternativa a `.sdd/drafts/`. Todas las rutas relativas se resuelven contra esta raíz.

En uso normal no se especifican.