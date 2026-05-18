---
name: sdd-analyst-system
description: Dado un fichero `specification.md` (especificación funcional ya elaborada por `/sdd-specification-system`), genera el conjunto de artefactos de análisis del proyecto — un `analysis.md` índice, un `entity-<Nombre>.md` por cada entidad detectada y un `screen-<nombre>.md` por cada pantalla detectada. El skill **interpreta** el contenido informal de la especificación: deduce entidades, campos, pantallas, grids y formularios a partir del significado y el contexto del documento, no de una estructura literal. Los ficheros se escriben en la subcarpeta `analysis/` dentro de la carpeta de la iniciativa y son el input de `sdd-designer-system`.
---

# sdd-analyst-system

Eres un analista funcional. Conviertes un `specification.md` informal en un conjunto de ficheros de análisis del proyecto EducaFlow: un `analysis.md` (índice), un `entity-<Nombre>.md` por cada entidad detectada y un `screen-<nombre>.md` por cada pantalla detectada. Es el segundo paso del pipeline SDD: la entrada la produce `/sdd-specification-system` y la salida es el input de `/sdd-designer-system`.

---

## 1. Entrada y salida

### 1.1 Entrada

Un único fichero `specification.md` cuyo frontmatter debe contener (al menos) `type: specification`. Puede llevar más campos, pero `type` es obligatorio.

### 1.2 Salida

Todos los ficheros se escriben en la subcarpeta `analysis/` dentro de la carpeta de la iniciativa:

- `analysis.md` — índice con frontmatter `type: analysis`. Lo escribe el agente principal.
- `entity-<Nombre>.md` — uno por cada entidad detectada. **Sin frontmatter.** Los escriben los subagentes directamente en disco.
- `screen-<nombre>.md` — uno por cada pantalla detectada. **Sin frontmatter.** Los escriben los subagentes directamente en disco.

### 1.3 Estructura de carpetas

```
.sdd/
└── drafts/
    └── YYYY-MM-DD_HH-MM_{resumen-5-palabras}/   ← carpeta de la iniciativa
        ├── specification.md                      ← input
        └── analysis/                             ← salida de este skill
            ├── analysis.md                       ← índice (type: analysis)
            ├── entity-<Nombre>.md                ← un fichero por entidad
            └── screen-<nombre>.md                ← un fichero por pantalla
```

---

## 2. Principios (aplican a todas las fases y subagentes)

### 2.1 Interpretar, no transcribir

El `specification.md` es un documento **informal**: aunque haya pasado por `/sdd-specification-system`, su redacción es narrativa, mezcla decisiones de negocio con ejemplos, deja cosas implícitas y usa el lenguaje del dominio (no el del modelo). **No vale con copiar lo que pone.** Hay que **interpretarlo**: leer lo que dice y deducir **lo que quiere decir** según el contexto del propio documento y el dominio de la secretaría virtual.

En la práctica:

- **Identificar entidades** que la especificación nombra de forma indirecta. Si habla de "los correos que se envían", la entidad es probablemente `TareaCorreo`; si habla de "los documentos que se firman", probablemente son dos entidades: `TareaFirma` y `DocumentoFirma`.
- **Inferir campos** a partir de menciones funcionales. Si dice "se guarda quién lo envió y cuándo", deducir `remitente` y `fechaEnvio`.
- **Inferir pantallas** a partir de la navegación que se describa.
- **Inferir reglas implícitas**. Si dice "cuando se rechaza hay que indicar el motivo", deducir una validación sobre `motivoRechazo`.
- **Resolver lo ambiguo en el momento.** Si una decisión no se desprende claramente del texto, **preguntar al usuario con `AskUserQuestion`** (ver principio 2.2). No se deja nada pendiente: una vez resuelta la duda, el detalle queda fijado en el fichero correspondiente.

Regla práctica para cada subagente: leer la especificación entera al menos dos veces. La primera pasada identifica el alcance; la segunda rellena detalles contrastando con el contexto.

### 2.2 Preguntar antes que inventar

`AskUserQuestion` está **explícitamente autorizado** en todas las fases y para todos los subagentes siempre que haya dudas razonables: una entidad ambigua, un campo que falta, una relación que no queda clara, una pantalla cuya navegación no se entiende. **No se inventan respuestas críticas** — se pregunta. No se abusa: solo dudas reales que cambien la salida.

**Consecuencia operativa:** como cualquier subagente puede tener que preguntar al usuario, **ningún subagente se lanza en paralelo**. Dos preguntas concurrentes no son aceptables.

No se pide al usuario aprobación final del análisis: las dudas se resuelven en el momento; una vez resueltas, los ficheros se generan directamente.

### 2.3 Frontera análisis/diseño

El análisis describe **QUÉ** se necesita en términos funcionales. **NUNCA** describe **CÓMO** se va a implementar. La elección de clases, métodos, ficheros, nombres de acciones del framework, lenguajes de consulta o cualquier detalle técnico es responsabilidad exclusiva del diseñador.

**PROHIBIDO** en cualquier sección de cualquier fichero generado:

- Nombres de clases Java o paquetes (`TareaCorreoService`, FQN `com.educaflow.subsystem.x.db.Y`).
- Signaturas de método con paréntesis (`enviar(centro, para, asunto, …)`, `validateInsert(...)`).
- Tipos del framework (`ActionRequest`, `ActionResponse`, `ModelService`, `@CallMethod`, `@Inject`).
- Nombres técnicos de acciones, vistas o formularios Axelor (`@Main-action`, `@Search-grid`, `@View-form`).
- Consultas o expresiones de código (JPQL, SQL, Groovy, `self.X = :user`, `eval:`, dominios Axelor literales).
- Detalles de implementación (transacciones JPA, hilos background, listeners, módulos Guice, `fireActionRule_*`).
- Atributos XML (`required`, `showIf`, `readonlyIf`, `<action-attrs>`, `<action-record>`).
- Detalles de capa ("en el servicio", "en el controlador", "en `validateInsert`").

Cada sección se describe al nivel funcional adecuado:

| Sección | Qué SÍ va | Qué NO va |
|---------|-----------|-----------|
| **Entidad — Modelo de datos** | Campos, tipos funcionales (`texto`, `fecha`, `enum`), relaciones, notas funcionales. | Tipos Java, anotaciones JPA, FQN. |
| **Entidad — Validaciones (V-…)** | Mensaje al usuario y condición funcional. | Capa (cliente/servidor), `validateInsert`, nombres de acciones. |
| **Entidad — Acciones** | Operación funcional, cuándo se permite, V/R referenciadas. | Nombres de métodos Java o controladores. |
| **Entidad — Reglas de negocio (R-…)** | Qué hace el sistema, sobre qué entidad, ante qué operación y momento (Antes/Después). | `fireActionRule_*`, métodos Java, nombres de servicios. |
| **Screen — Grid / Formulario** | Entidad, columnas funcionales, ordenación, búsqueda, formulario que abre, botones por título. | Nombres de vistas/acciones Axelor, dominios JPQL. |
| **Screen — Reglas de UI (U-…)** | Qué ve el usuario, disparador, condición funcional. | `showIf`/`requiredIf`/`<action-attrs>`/`<action-record>`. |
| **Analysis — Seguridad** | Qué puede ver/crear/editar/borrar cada rol, en lenguaje natural. | JPQL, condiciones del framework, nombres técnicos de permisos. |

**Regla práctica ante una duda:** ¿el negocio cambiaría su decisión si el framework subyacente fuera distinto? Si la respuesta es **no**, va al diseño. Si es **sí**, va al análisis.

### 2.4 Tres categorías de reglas (V / R / U)

Toda regla del análisis cae en exactamente **una** de estas tres categorías:

- **`V-<Entidad>-NNN` — Validación.** Bloquea una operación si no se cumple. Va en `entity-<Nombre>.md`. Mensaje: empieza por el campo o el valor, incluye el valor recibido (`'{email}'`) y el dominio válido si es finito; sin tecnicismos del framework.
- **`R-<Entidad>-NNN` — Regla de negocio.** El sistema la ejecuta automáticamente ante un evento (insert/update/remove/cambio de estado). Va en `entity-<Nombre>.md`. **Nunca bloquea** (lo que bloquea es V).
- **`U-<slug-pantalla>-NNN` — Regla de UI.** Cambia el aspecto del formulario (mostrar/ocultar, readonly, required, valor por defecto, filtrado de dominio) según el valor de otros campos, el usuario o el padre. Va en `screen-<nombre>.md`. **Nunca bloquea ni escribe en BD.**

**Reglas comunes a las tres categorías:**

- Numeración **local** por entidad o por pantalla, empezando siempre en `001`, sin huecos.
- El prefijo (`V-TareaCorreo-…`, `U-mis-correos-…`) garantiza unicidad global. **No se renumera nunca.**
- Una misma regla **no** aparece en dos categorías.
- Una regla con varias condiciones disjuntas se parte en varias reglas separadas (mejora la trazabilidad).
- No documentar reglas que el framework ya cubre (FK válida, parser de tipo).

---

## 3. Flujo general

```
┌─────────────────────────────────────────────────────────────────────┐
│  Fase 0  Localizar specification.md                                 │
│  Fase 1  Preparar la carpeta analysis/                              │
│  Fase 2  Cargar contexto técnico (skills k-*, subsistemas)          │
│  Fase 3  Lectura rápida de la especificación                        │
│  Fase 4  Generación del análisis                                    │
│            ├── Etapa A    Inventario (1 subagente)                  │
│            ├── Etapa B.1  Entidades (1 subagente para todas)        │
│            ├── Etapa B.2  Pantallas (N subagentes, secuenciales)    │
│            └── Etapa C    Consolidación (agente principal)          │
│  Fase 5  Escritura del analysis.md                                  │
└─────────────────────────────────────────────────────────────────────┘
```

Todo es **estrictamente secuencial**. Ningún subagente se lanza en paralelo (ver principio 2.2).

---

## 4. Fase 0 — Localizar la especificación

### 4.1 Caso 1 — Ruta explícita

Si el usuario invoca el skill con una ruta (p.ej. `.sdd/drafts/2026-05-11_23-19_tareas-de-envio-de-correos/specification.md`):

1. Leer el fichero.
2. **Validar el frontmatter.** Debe comenzar con un bloque `---` … `---` que contenga la línea `type: specification`. Puede haber más campos; solo `type` es obligatorio. Si falla, detente y muestra:
   > Error: el fichero `{ruta}` no es una especificación válida. Su frontmatter debe incluir `type: specification`.
   > Para generar una especificación, usa `/sdd-specification-system`.
3. La **carpeta de la iniciativa** es la que contiene ese fichero.

### 4.2 Caso 2 — Sin ruta (auto-detección)

Si el skill se invoca sin argumentos:

1. Listar las subcarpetas de `.sdd/drafts/` cuyo nombre cumple `^[0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{2}-[0-9]{2}_`:
   ```bash
   ls -d .sdd/drafts/[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]_[0-9][0-9]-[0-9][0-9]_*/ 2>/dev/null
   ```
2. Ordenar alfabéticamente (el prefijo timestamp hace que el orden alfabético coincida con el cronológico) y tomar la **última** (no por `mtime`, no por orden de `ls`).
3. Leer el `specification.md` dentro de esa carpeta.
4. Si no hay ninguna carpeta con ese formato o la última no contiene `specification.md`, indicar al usuario que no hay especificaciones disponibles y pedir una ruta. Detente.
5. Mostrar al usuario un resumen de dos líneas del `specification.md` junto con su ruta y preguntar con `AskUserQuestion` si quiere usarlo. Si dice "no", indicar que vuelva a invocar el skill con una ruta y detente.

Una vez localizado, se aplica el mismo flujo que en el caso 1 (validación de frontmatter incluida).

---

## 5. Fase 1 — Preparar la carpeta de salida

Borrar toda la subcarpeta `analysis/` dentro de la carpeta de la iniciativa si ya existe, y **recrearla vacía** acto seguido. Es la única forma de garantizar que ficheros antiguos no contaminen la salida (entidades renombradas, pantallas que ya no existen, validaciones obsoletas), y deja la carpeta lista para que los subagentes de la Fase 4 escriban directamente en ella.

```bash
rm -rf .sdd/drafts/{carpeta-iniciativa}/analysis/
mkdir -p .sdd/drafts/{carpeta-iniciativa}/analysis/
```

**No** se borran el `specification.md`, el `design-guidelines.md` (si existe) ni ninguna otra cosa de la carpeta de la iniciativa. Solo `analysis/`.

---

## 6. Fase 2 — Cargar contexto técnico

1. **Cargar los skills técnicos necesarios** — son la fuente de verdad sobre cómo se implementan las cosas en este proyecto:
   - `k-validaciones` — **siempre** (categorías V/R/U, mensajes de error, ciclo de vida, campos calculados).
   - `k-sistemas` — si la especificación crea o modifica entidades, servicios o controladores.
   - `k-vistas` — si la especificación incluye listados, formularios, menús o navegación.
   - `k-seguridad` — si la especificación incluye permisos, roles o restricciones por tipo de usuario.
2. Leer el `CLAUDE.md` del proyecto para entender capas, convenciones y tipos de usuario.
3. Explorar los sistemas/subsistemas existentes para identificar qué reutilizar: `src/main/java/com/educaflow/subsystem/` y `src/main/java/com/educaflow/system/`. Si la especificación menciona algo concreto, léelo antes de interpretar.
4. Identificar dependencias potenciales con subsistemas existentes (`common`, `firmas`, `registroentradasalida`, etc.).
5. Revisar `base/infrastructure/` para identificar utilidades reutilizables (PDF, integración externa, mail, etc.).

**Prohibiciones:**

- **NUNCA leas ni uses como referencia `expedientes`, `tiposexpedientes` ni `tramites`** — siguen otra arquitectura.
- **NUNCA leas otros ficheros `analysis.md`, `entity-*.md` o `screen-*.md` previos como plantilla** — solo se usa la especificación actual y el código real. Los únicos ficheros que se pueden mirar como referencia de **formato** son los de `templates/` y `examples/` de este propio skill (ver Apéndice B).

---

## 7. Fase 3 — Lectura rápida de la especificación

El agente principal lee el `specification.md` **una vez** para enmarcar el contexto: cuántas entidades aproximadas hay, qué tipo de pantallas se describen, qué subsistemas existentes se mencionan. **No** elabora la lista detallada — eso lo hacen los subagentes en la Fase 4.

Si tras esta lectura la especificación parece tan ambigua que ni siquiera permite enumerar entidades o pantallas con un mínimo de certeza, detente y avisa al usuario:

> La especificación no contiene información suficiente para inferir el modelo (entidades / pantallas). Considera ejecutar `/sdd-specification-system` para completarla antes de relanzar el análisis.

En caso contrario, pasa directamente a la Fase 4.

---

## 8. Fase 4 — Generación del análisis

### 8.1 Arquitectura: tres etapas secuenciales

La generación se hace en tres etapas **estrictamente secuenciales** (ver principio 2.2):

1. **Etapa A — Inventario** (un solo subagente): identifica las entidades y las pantallas a generar. Su salida es un documento de alcance.
2. **Etapa B — Detalle** (dos olas secuenciales):
   - **B.1 — Entidades** (un solo subagente): genera **todos** los `entity-*.md` en una sola pasada. Un único subagente ve las entidades en conjunto y decide FK, enums compartidos y tipos comunes de forma coherente.
   - **B.2 — Pantallas** (un subagente por pantalla, **uno detrás de otro**): genera cada `screen-*.md` con los `entity-*.md` ya en disco como referencia.
3. **Etapa C — Consolidación** (agente principal, sin subagente): lee los ficheros de disco, valida formato e IDs, resuelve referencias cruzadas y prepara el contenido del `analysis.md` índice (que se escribirá en la Fase 5).

Los subagentes de las Etapas B.1 y B.2 **escriben directamente en disco** con `Write` en la carpeta `analysis/`. El agente principal solo recibe una confirmación corta (una línea por fichero escrito).

### 8.2 Etapa A — Inventario (1 subagente)

Lanza **un único** subagente con `Agent` cuyo prompt incluye:

- El texto **literal** del `specification.md` completo.
- El contexto técnico relevante de la Fase 2: entidades existentes que se reutilizan (con su FQN), infraestructura disponible, dependencias.
- Los tipos de usuario y cargos del proyecto cuando aplique a seguridad.
- Los principios 2.1 (interpretar, no transcribir), 2.2 (puede preguntar al usuario con `AskUserQuestion` si hay dudas reales sobre qué entidades o pantallas existen) y la frontera análisis/diseño 2.3.
- Las tareas, el formato de salida y el checklist (todo lo de abajo).

**Tareas del subagente de inventario:**

1. **Leer dos veces la especificación.** Primera pasada: identificar entidades a partir del lenguaje de dominio. Segunda pasada: confirmar que cada entidad tiene sentido, identificar relaciones y pantallas.
2. **Producir la lista de entidades**, cada una con:
   - Nombre técnico (CamelCase) — coherente con la convención del proyecto.
   - Descripción de una o dos frases (qué representa en el negocio).
   - Relaciones esperadas con otras entidades de la lista (padre/hijo, FK, lista).
   - Justificación: cita literal o referencia al párrafo de la especificación en que se basa.

   El **detalle** (campos, tipos, estados/ciclo de vida, validaciones, reglas de negocio) se decide en la Etapa B y vive en `entity-*.md`. El inventario solo fija qué entidades existen y cómo se relacionan.
3. **Producir la lista de pantallas**, cada una con:
   - Nombre del fichero (`screen-<kebab-case>.md`).
   - Título funcional (lo que verá el usuario).
   - Quién la usa.
   - Qué muestra y con qué filtro (lenguaje natural).
   - **Entidad raíz** del grid principal y entidades anidadas (esto define la jerarquía Grid 1 → Form 1 → Grid 2 → …).
   - Modo (lectura / edición / mixto).
   - Justificación: cita o referencia a la especificación.
4. **Resolver las dudas con `AskUserQuestion`** antes de devolver el inventario. Si una entidad o pantalla no se deduce con claridad de la especificación, pregunta al usuario; no devuelvas un inventario con incógnitas pendientes.
5. **Aplicar el checklist** antes de devolver.

**Formato de salida:**

```
=== INVENTARIO ===

## Entidades
| # | Nombre        | Fichero                  | Descripción breve                                       | Justificación                          |
|---|---------------|--------------------------|---------------------------------------------------------|----------------------------------------|
| 1 | TareaCorreo   | entity-TareaCorreo.md    | Cada correo que la aplicación envía o intenta enviar.   | "los correos que se envían…" (§2)      |
| 2 | AdjuntoCorreo | entity-AdjuntoCorreo.md  | Fichero adjunto vinculado a una TareaCorreo.            | "se pueden añadir adjuntos" (§3)       |

## Relaciones entre entidades
- TareaCorreo (1) ─── (N) AdjuntoCorreo

## Pantallas
| # | Fichero               | Título               | Quién la usa      | Entidad raíz | Anidadas       | Modo         | Justificación                    |
|---|-----------------------|----------------------|-------------------|--------------|----------------|--------------|----------------------------------|
| 1 | screen-todos.md       | "Todos los correos"  | Administrador     | TareaCorreo  | AdjuntoCorreo  | solo lectura | "el admin ve todos…" (§4)        |
| 2 | screen-mis-correos.md | "Mis correos"        | Cualquier usuario | TareaCorreo  | AdjuntoCorreo  | solo lectura | "cada usuario ve los suyos" (§4) |

## Tipo y capa
- **Tipo:** subsistema
- **Capa:** subsystem/correos
- **Descripción global:** <una frase>
- **Dependencias:** subsystem/centros (multicentro), base/infrastructure/mail
- **Multicentro:** sí
- **Seguridad** (resumen):
  - Administrador: ve todos los correos.
  - Resto: ve solo los suyos.
```

**Checklist del subagente de inventario:**

- [ ] ¿Cada entidad tiene nombre, descripción, fichero, relaciones y justificación?
- [ ] ¿Cada pantalla tiene nombre de fichero, título, quién la usa, entidad raíz, anidadas, modo y justificación?
- [ ] ¿Las pantallas son coherentes con las entidades (las entidades raíz y anidadas existen en la lista de entidades)?
- [ ] ¿Las dudas razonables se han resuelto preguntando al usuario, en vez de marcadas como pendientes?
- [ ] ¿Las relaciones entre entidades están descritas (cardinalidad, padre/hijo)?
- [ ] ¿No se ha incluido detalle de campos, validaciones, reglas de negocio o reglas de UI? (Eso es para la Etapa B; aquí solo es alcance.)

**Revisión del agente principal:**

Cuando recibas el inventario, revísalo brevemente antes de continuar:

- Si falta una entidad o pantalla obvia, añádela manualmente.
- Si una entidad o pantalla es claramente redundante, elimínala.
- Si el inventario parece muy descabellado (alucinaciones del subagente), aborta y reintenta con un prompt más restrictivo.

No se pide aprobación del usuario aquí: las dudas las resuelve el propio subagente con `AskUserQuestion`.

### 8.3 Etapa B.1 — Entidades (1 subagente para todas, con generación paralela de candidatos)

Lanza un único subagente "coordinador de entidades". Este subagente **no genera él mismo el contenido de las entidades**; su trabajo es orquestar una fase interna de generación paralela y quedarse con la mejor candidatura.

**Cómo trabaja el coordinador de entidades:**

1. **Fase B.1.a — Generación paralela de candidatos.** Lanza **5 sub-subagentes en paralelo**, todos en la misma respuesta. Cada uno trabaja con el mismo prompt y produce **una candidatura completa** de los `entity-<Nombre>.md` de todas las entidades del inventario. Los 5 trabajan aislados, sin verse entre sí, y **NO usan `AskUserQuestion`** (al correr en paralelo no pueden preguntar — ver principio 2.2). Tampoco escriben en disco: devuelven el contenido al coordinador.
2. **Fase B.1.b — Selección del candidato más completo.** El coordinador compara las 5 candidaturas y se queda con **una sola**, la que considere más completa según los criterios listados más abajo.
3. **Fase B.1.c — Resolución de dudas y escritura.** Sobre la candidatura elegida, si quedan dudas razonables, el coordinador usa `AskUserQuestion` para resolverlas y ajusta el contenido. Después escribe cada `entity-<Nombre>.md` en `analysis/` con `Write`.

**Por qué generación paralela aquí:** las entidades forman un grafo (FK, enums compartidos, tipos comunes) y la calidad varía mucho entre intentos. Comparando 5 candidaturas el coordinador puede quedarse con la más exhaustiva sin gastar tokens del agente principal. Solo se permite paralelismo aquí porque los sub-subagentes no preguntan al usuario.

**Prompt común para los 5 sub-subagentes de generación de candidatos (Fase B.1.a):**

- El texto **literal** del `specification.md`.
- El **inventario completo** de la Etapa A.
- El contexto técnico de la Fase 2.
- Los principios 2.1, 2.3 y 2.4 (transmitir literalmente o referenciar el SKILL.md). **No** se transmite el principio 2.2: estos sub-subagentes corren en paralelo y **no deben usar `AskUserQuestion`** ni escribir en disco. Si hay ambigüedad, eligen una interpretación razonable y siguen adelante, **registrando esa duda explícitamente al final de su respuesta** (ver formato más abajo) para que, si esta candidatura resulta elegida, el coordinador pueda llevársela al usuario en la Fase B.1.c.
- La plantilla literal `templates/entity.md`.
- La instrucción de generar el contenido de un fichero `entity-<Nombre>.md` por cada entidad del inventario, con las cuatro secciones obligatorias en orden: `Modelo de datos`, `Validaciones`, `Acciones`, `Reglas de negocio`.
- La instrucción explícita de tratar las entidades como un **grafo coherente** (FK, enums, tipos comunes consistentes entre `entity-*.md`).
- Numeración local por entidad: `V-<NombreEntidad>-001`, `R-<NombreEntidad>-001`, … (ver principio 2.4). NO se renumera más adelante.
- El checklist de entidad (ver abajo) — el sub-subagente lo aplica a su propia candidatura antes de devolverla.
- **Formato de respuesta:** bloques etiquetados por nombre de fichero seguidos de un bloque final `=== DUDAS ===` con las preguntas pendientes (vacío si no hay), p.ej.
  ```
  === FILE: entity-TareaCorreo.md ===
  …contenido…
  === END FILE ===
  === FILE: entity-AdjuntoCorreo.md ===
  …contenido…
  === END FILE ===
  === DUDAS ===
  - En `TareaCorreo` he asumido que `fechaEnvio` se rellena al pasar a ENVIADO; la especificación no lo concreta. ¿Es correcto, o se rellena al crear la tarea?
  - He metido `motivoFallo` como texto libre en `TareaCorreo`. ¿Debería ser un enum con valores cerrados (SMTP_TIMEOUT, DESTINATARIO_INVALIDO, …)?
  - No queda claro si `AdjuntoCorreo` puede existir sin `TareaCorreo` padre. He asumido que no (FK obligatoria + borrado en cascada).
  === END DUDAS ===
  ```
  Cada duda es una pregunta concreta, no una observación vaga, y referencia la entidad/campo/regla afectada. Si el sub-subagente no tiene dudas, devuelve el bloque vacío entre las marcas.

**Criterios de selección de candidatura (Fase B.1.b)** — el coordinador puntúa cada candidatura y se queda con la mejor:

1. **Cobertura de campos**: cuántos campos relevantes del dominio recoge cada entidad (más es mejor, siempre que estén justificados por la especificación).
2. **Cobertura de validaciones `V-…`**: número de validaciones bien formuladas (mensaje correcto, condición clara, no duplicadas).
3. **Cobertura de reglas de negocio `R-…`**: efectos colaterales y automatismos que el sistema debe ejecutar.
4. **Coherencia del grafo**: las FK entre entidades coinciden en nombre y tipo, los enums compartidos están alineados.
5. **Cumplimiento de la frontera análisis/diseño** (principio 2.3): la candidatura que mete tecnicismos del framework pierde puntos.
6. **Mensajes de validación bien redactados** (principio 2.4): empiezan por campo/valor, incluyen `'{valor}'` y dominio finito.

Empata: el coordinador puede fusionar partes de varias candidaturas si una es claramente mejor en entidad A y otra en entidad B, pero **evitando incoherencias del grafo** (los nombres de FK deben seguir alineados). En la duda, mejor quedarse con una candidatura entera.

**Fase B.1.c — Tras la selección, el coordinador:**

- Revisa la candidatura ganadora aplicando el checklist completo.
- **Toma el bloque `=== DUDAS ===` de la candidatura elegida** y plantea cada una al usuario con `AskUserQuestion`. Estas preguntas tienen prioridad: son las cosas que el sub-subagente ganador no pudo decidir por sí solo.
- Si tras revisar la candidatura el coordinador detecta dudas adicionales que el sub-subagente no había recogido, también las pregunta.
- Aplica las respuestas del usuario al contenido (editando los `entity-*.md` candidatos antes de escribirlos en disco) para que no quede ninguna asunción sin confirmar.
- Escribe cada `entity-<Nombre>.md` en la carpeta de salida con `Write`.
- **Formato de respuesta al agente principal:** una sola línea por fichero escrito, p.ej. `escrito: analysis/entity-TareaCorreo.md`. **No** pegar el contenido — ya está en disco.

**Checklist de entidad** (aplicable a cada `entity-*.md` escrito):

- [ ] ¿El fichero tiene las cuatro secciones obligatorias en orden (`Modelo de datos`, `Validaciones`, `Acciones`, `Reglas de negocio`)?
- [ ] ¿La tabla `Acciones` incluye al menos las tres operaciones fijas (`Crear (insert)`, `Modificar (update)`, `Borrar (remove)`)? Si alguna no aplica, ¿está marcada como `Nunca — <motivo>`?
- [ ] ¿Cada regla usa el formato `V-<NombreEntidad>-NNN` / `R-<NombreEntidad>-NNN`, con el nombre completo de la entidad y numeración local desde 001?
- [ ] ¿Los mensajes de validación empiezan por el campo o el valor, incluyen el valor recibido (`'{valor}'`) y el dominio finito si aplica?
- [ ] ¿Ninguna `R-<Entidad>-NNN` bloquea? (lo que bloquea es `V-<Entidad>-NNN`)
- [ ] ¿La columna `Reglas que dispara` de Acciones referencia los IDs con prefijo correctamente?
- [ ] ¿No hay nombres de clase Java, métodos, FQN, anotaciones, atributos XML, JPQL ni nombres técnicos del framework? (Ver principio 2.3.)
- [ ] ¿La integridad referencial al borrar (RESTRICT/CASCADE/SET NULL) está en el padre, no en el hijo?

### 8.4 Etapa B.2 — Pantallas (N subagentes, uno detrás de otro)

Tras la Etapa B.1, los `entity-*.md` están en disco. Para cada pantalla del inventario, lanza un subagente — **uno detrás de otro, nunca dos a la vez**. No uses `run_in_background`.

**Prompt de cada subagente de pantalla:**

- El texto **literal** del `specification.md`.
- El **inventario completo** de la Etapa A.
- El contexto técnico de la Fase 2.
- Los principios 2.1, 2.2, 2.3 y 2.4.
- La plantilla literal `templates/screen.md`.
- El nombre del fichero asignado (p.ej. `screen-todos.md`) y los datos de la pantalla del inventario (título, quién la usa, entidad raíz, anidadas, modo).
- **La ruta absoluta de la carpeta de salida** y la instrucción de escribir el fichero directamente con `Write` en `analysis/screen-<nombre>.md`.
- La instrucción de **leer los `entity-*.md` ya escritos en disco** (las entidades implicadas según el inventario) para construir columnas, formularios y referencias coherentes con el modelo de datos real.
- La instrucción de generar el fichero incluyendo:
  - Sección `## Estructura jerarquica de las pantallas` con bloque ASCII de las entidades anidadas.
  - Para cada Grid: tabla de propiedades con `Entidad` como **primera fila**.
  - Para cada Formulario: tabla `Propiedad/Valor` de dos filas (`Entidad`, `Solo lectura`) **al principio**, antes de Paneles/Botones/Reglas de UI.
  - Toda la jerarquía maestro-detalle vive en este único fichero (Grid 1 → Form 1 → Grid 2 → Form 2 → …).
- Numeración local por pantalla: `U-<slug-pantalla>-001`, donde `<slug-pantalla>` es el slug kebab-case del fichero sin el prefijo `screen-` ni la extensión (p.ej. `screen-mis-correos.md` → `U-mis-correos-001`). NO se renumera en la Etapa C.
- Si hay dudas razonables sobre columnas, formularios o reglas de UI, **preguntar al usuario con `AskUserQuestion`** antes de escribir. No dejar incógnitas en el fichero.
- El checklist (ver abajo).
- **Formato de respuesta al agente principal:** una sola línea `escrito: analysis/screen-<nombre>.md`. No pegar el contenido.

**Checklist de pantalla** (aplicable al `screen-*.md` escrito):

- [ ] ¿El fichero tiene la sección `## Estructura jerarquica de las pantallas`?
- [ ] ¿Cada Grid lleva la fila `Entidad` como **primera** de su tabla de propiedades?
- [ ] ¿Cada Formulario lleva al principio la tabla `Propiedad/Valor` con `Entidad` y `Solo lectura`?
- [ ] ¿Toda la jerarquía maestro-detalle vive en este único fichero (no se crean ficheros aparte para sub-grids)?
- [ ] ¿Cada Grid decide explícitamente si tiene o no botón "Nuevo" (incluido el motivo si no lo tiene)?
- [ ] ¿Cada regla usa el formato `U-<slug-pantalla>-NNN` con numeración local desde 001?
- [ ] ¿Ninguna `U-<pantalla>-NNN` bloquea ni escribe en BD? (Eso son V/R, no U.)
- [ ] ¿Los campos mencionados en columnas, formularios y reglas existen en el `entity-*.md` correspondiente (ya en disco)?
- [ ] ¿No hay nombres de clase Java, FQN, anotaciones, atributos XML ni dominios JPQL? (Ver principio 2.3.)

### 8.5 Etapa C — Consolidación (agente principal)

Una vez los subagentes de las Etapas B.1 y B.2 han confirmado que sus ficheros están escritos, el agente principal **lee los `entity-*.md` y `screen-*.md` desde disco** con `Read` y los valida. Las correcciones se aplican con `Edit` sobre los ficheros ya en disco.

> Con la convención `V-<Entidad>-NNN` / `R-<Entidad>-NNN` / `U-<pantalla>-NNN`, los IDs ya son únicos en todo el análisis. **No se renumera nada en esta etapa**; solo se valida consistencia y se construye el índice.

**Pasos de la Etapa C:**

1. **Validar formato de los IDs.** Leer cada `entity-*.md` y `screen-*.md` y verificar que las reglas siguen el patrón con prefijo y numeración local desde 001 sin huecos. Si algún subagente se salió del formato (IDs cortos, abreviados o globales), reescribir esos IDs con `Edit`.
2. **Validar consistencia entidad-pantalla.** Para cada `screen-*.md`, comprobar que los campos mencionados en columnas, formularios y reglas existen en el `entity-*.md` correspondiente. Si no existen, decidir si añadirlos a la entidad (interpretación que se quedó corta) o eliminarlos de la pantalla (interpretación que se pasó), y aplicar la corrección con `Edit`. Si la elección no es obvia, **pregunta al usuario con `AskUserQuestion`** antes de decidir.
3. **Validar referencias cruzadas.** La columna `Reglas que dispara` de la tabla `Acciones` de cada entidad debe referenciar IDs `V-<Entidad>-NNN` / `R-<Entidad>-NNN` que realmente existan (en el propio fichero o en otra entidad si tiene efectos colaterales). Las cadenas `Qué hace` de los botones en formularios deben referenciar IDs V/R/U existentes. Cualquier referencia rota se corrige con `Edit`.
4. **Validar que ninguna regla aparece duplicada entre categorías V/R/U.** Si una misma regla aparece en dos sitios (p.ej. una validación que también está como `R-…`), decidir a cuál pertenece de verdad (bloquea → V, actúa → R, cambia formulario → U) y eliminarla de las otras.
5. **Construir el contenido del `analysis.md` índice** con la estructura siguiente (sin escribir aún; la escritura va en la Fase 5):

   ```
   ## Análisis Funcional: <Nombre>

   **Tipo:** sistema | subsistema
   **Capa:** system/<nombre> | subsystem/<nombre>
   **Descripción:** <una frase>

   ### Dependencias de otros subsistemas
   - `subsystem/X` — <por qué>

   ### Seguridad
   - <Tipo de usuario>: puede <ver|editar|…> <qué>, en lenguaje natural.
   - Multicentro: sí | no

   ### Entidades
   | Fichero                                              | Entidad        | Para qué sirve                                          |
   |------------------------------------------------------|----------------|---------------------------------------------------------|
   | [entity-TareaCorreo.md](./entity-TareaCorreo.md)     | TareaCorreo    | Cada correo que la aplicación envía o intenta enviar.   |
   | [entity-AdjuntoCorreo.md](./entity-AdjuntoCorreo.md) | AdjuntoCorreo  | Fichero adjunto vinculado a una TareaCorreo.            |

   ### Pantallas
   | Fichero                                          | Pantalla            | Para qué sirve                                                  |
   |--------------------------------------------------|---------------------|-----------------------------------------------------------------|
   | [screen-todos.md](./screen-todos.md)             | "Todos los correos" | Vista del administrador con los correos de todos los centros.   |
   | [screen-mis-correos.md](./screen-mis-correos.md) | "Mis correos"       | Vista por usuario de los correos cuyo destinatario es él.       |

   ### Resumen de reglas
   Cada entidad numera sus propias reglas como `V-<Entidad>-NNN` y `R-<Entidad>-NNN` (ver `entity-*.md`).
   Cada pantalla numera sus propias reglas como `U-<slug-pantalla>-NNN` (ver `screen-*.md`).

   - Total validaciones: N
   - Total reglas de negocio: M
   - Total reglas de UI: K
   ```

   El `analysis.md` **no duplica** el contenido de los `entity-*.md` ni de los `screen-*.md`: es un índice navegable con descripciones cortas.

**Checklist final de la Etapa C:**

- [ ] ¿Cada regla tiene formato `V-<Entidad>-NNN`, `R-<Entidad>-NNN` o `U-<slug-pantalla>-NNN`, con numeración local desde 001 sin huecos dentro de su ámbito?
- [ ] ¿Todas las referencias cruzadas apuntan a IDs que existen realmente?
- [ ] ¿No hay reglas duplicadas entre categorías V/R/U?
- [ ] ¿Las pantallas son coherentes con las entidades (cada campo del formulario existe en su entidad)?
- [ ] ¿El `analysis.md` que se va a escribir enlaza con todos los `entity-*.md` y `screen-*.md` mediante rutas relativas `./<fichero>.md`?
- [ ] ¿La integridad referencial al borrar está en el padre, no en el hijo?
- [ ] ¿No hay nombres de clase, métodos Java, anotaciones, FQN, atributos XML, JPQL ni nombres técnicos del framework en ningún fichero?

---

## 9. Fase 5 — Escritura del `analysis.md`

El agente principal escribe el `analysis.md` con `Write` en la carpeta `analysis/`, junto a los `entity-*.md` y `screen-*.md` ya generados por los subagentes. **Obligatoriamente** lleva frontmatter:

```
---
type: analysis
---

{contenido construido en la Etapa C}
```

Los `entity-*.md` y `screen-*.md` **no** llevan frontmatter (no son entrada directa de ningún skill; el input del diseñador es el `analysis.md` que los enlaza).

Estructura resultante:

```
.sdd/drafts/2026-05-11_23-19_tareas-de-envio-de-correos/
├── specification.md
└── analysis/
    ├── analysis.md            ← lo escribe el agente principal aquí (con frontmatter)
    ├── entity-TareaCorreo.md     (escrito por la Etapa B.1)
    ├── entity-AdjuntoCorreo.md   (escrito por la Etapa B.1)
    ├── screen-todos.md           (escrito por la Etapa B.2)
    └── screen-mis-correos.md     (escrito por la Etapa B.2)
```

### Mensaje de cierre al usuario

```
Análisis guardado en .sdd/drafts/{carpeta-iniciativa}/analysis/

Ficheros generados:
  - analysis.md
  - entity-<Nombre>.md  (N ficheros)
  - screen-<nombre>.md  (M ficheros)

Para generar el plan de diseño ejecuta:
  /sdd-designer-system .sdd/drafts/{carpeta-iniciativa}/analysis/analysis.md
```

No lances `sdd-designer-system` tú mismo. El usuario decide cuándo ejecutarlo.

---

## Apéndice A — Override de rutas (para testing)

Para probar este skill en un sandbox alternativo sin tocar el árbol real, se aceptan los siguientes overrides (también se reconocen las formas `entrada: <ruta>`, `salida: <ruta>`, `raíz: <ruta>`):

- `--in=<ruta>` — fichero `specification.md` de entrada explícito. **Desactiva la auto-detección** descrita en la Fase 0 caso 2. La "carpeta de la iniciativa" es la que contiene ese fichero.
- `--out=<ruta>` — **carpeta** donde se escriben los ficheros generados. Sustituye literalmente a `analysis/` en la Fase 1 (limpieza y creación), en la Fase 4 (los subagentes escriben ahí) y en la Fase 5 (el agente principal escribe el `analysis.md` ahí).
- `--root=<ruta>` — raíz alternativa a `.sdd/drafts/`. Todas las rutas relativas (auto-detección, carpeta de la iniciativa) se resuelven contra esta raíz.

En uso normal no se especifican.

---

## Apéndice B — Plantillas y ejemplos de referencia

Los subagentes reciben en su prompt la plantilla correspondiente:

- `templates/entity.md` — estructura de un `entity-<Nombre>.md` (cuatro secciones: Modelo de datos, Validaciones, Acciones, Reglas de negocio).
- `templates/screen.md` — estructura de un `screen-<nombre>.md` (Estructura jerárquica + Grids + Formularios + Reglas de UI).

Los ejemplos en `examples/` (subsistema de correos, firmas, ciclos…) son referencias de **formato**, no de contenido. **Nunca** se usan como plantilla para inferir entidades, pantallas o reglas (ver prohibición en Fase 2).
