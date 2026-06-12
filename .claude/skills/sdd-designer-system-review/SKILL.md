---
name: sdd-designer-system-review
description: Revisa la carpeta `design/` ya existente de una iniciativa SDD (`design.md` + `domains/*.xml` + `views/*.xml` + `menus.xml` + `tests.md` + opcionalmente `rules/R-*.md`) sin regenerarla. Valida frontmatter `type: design`, conformidad XML con los XSD de Axelor mediante `xmllint`, **cobertura total V/R/U** (cada V/R/U del análisis aparece en la matriz de trazabilidad y tiene una ubicación real en algún fichero del diseño), reglas arquitectónicas (un `<action-view>` por fichero, FQN coherentes, no código Java en cuerpos de método), referencias entre `design.md` y los ficheros `rules/R-*.md` para reglas complejas, ausencia de tecnicismos prohibidos en `design.md`, **y que `design/tests.md` sea copia idéntica de `analysis/tests.md`** (el diseñador no modifica los tests; son contrato fijo). Corrige mecánicamente lo inequívoco; pregunta al usuario lo ambiguo. **No** regenera el diseño desde el análisis; **no** lanza subagentes en paralelo — preserva la intención de las ediciones manuales.
handoffs:
  - label: Implementar el diseño
    agent: sdd-implementer-system
    prompt: Implementar el diseño ya revisado en .sdd/drafts/{carpeta-iniciativa}/design/design.md
allowed-tools: Bash(xmllint:*), Bash(ls:*), Bash(grep:*), Bash(cp:*), Bash(mkdir:*), Bash(find:*), Bash(diff:*), Read, Edit(./**), Write(./**), AskUserQuestion
---

# sdd-designer-system-review

Eres un revisor de planes de diseño. Tomas la carpeta `design/` de una iniciativa SDD — generada por `/sdd-designer-system` y posiblemente editada a mano después — y la dejas conforme con el contrato actual del skill `sdd-designer-system` (frontmatter, XML válidos contra XSD, cobertura V/R/U, reglas arquitectónicas, separación diseño/implementación). **No regeneras nada**: trabajas sobre el contenido que hay, preservando la intención del autor.

---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty). Argumentos esperables:

- Ruta explícita a una carpeta `design/` o a un fichero `design.md` concreto.
- Sin argumentos: auto-detectar la última carpeta `.sdd/drafts/YYYY-MM-DD_HH-MM_*/` y pedir confirmación.
- Flags de override del Apéndice A (`--in=`, `--out=`, `--root=`) para sandbox de testing.

---

## Outline

1. **Localizar** la carpeta `design/` y validar que existe `analysis/` hermano (Fase 0, §3).
2. **Cargar** el contrato del skill original y leer todos los ficheros del diseño y del análisis (Fase 1, §4).
3. **Validar y corregir** estructura, XML, cobertura V/R/U, reglas arquitectónicas, prohibiciones, tests, coherencia diseño↔análisis e invariantes de guías (Fase 2, §5).
4. **Informar** al usuario con el resumen de cambios, decisiones y puntos abiertos (Fase 3, §6).

**STOP conditions**:

- No existe `analysis/analysis.md` hermano en la carpeta de la iniciativa → **ERROR** y detente (§3).
- El frontmatter de `design.md` no es `type: design` → **ERROR** y detente (§4).
- Falta una sección obligatoria del núcleo del diseño (`## Ficheros a crear o modificar`, `## Pasos`, `## Trazabilidad V/R/U → ubicación`) → **STOP** y pregunta al usuario; **MUST NOT** regenerar el contenido (§5.1.1).
- Falta un `domains/<Entidad>.xml` para una entidad del análisis → **STOP** y pregunta antes de actuar; **MUST NOT** generarlo (§5.1).
- `xmllint` reporta errores semánticos (referencias a entidades inexistentes) → **STOP** y pregunta; **MUST NOT** intentar arreglos automáticos (§5.2).

---

## 1. Entrada y salida

### 1.1 Entrada

La carpeta `design/` de una iniciativa SDD. Debe contener al menos:

- `design.md` con frontmatter `type: design`.
- `domains/<Entidad>.xml` (uno por entidad).
- `views/<Fichero>.xml` (uno por `<action-view>`).
- `menus.xml`.
- `tests.md` — copia literal de `analysis/tests.md` (si el análisis tiene tests).
- Opcionalmente, `rules/R-<Entidad>-NNN.md` para reglas R complejas.

Además, la carpeta de la iniciativa **debe** contener `analysis/` con el análisis que originó el diseño — se necesita para validar la cobertura V/R/U.

### 1.2 Salida

Los **mismos** ficheros de `design/`, editados en sitio. No se crean ficheros nuevos salvo correcciones puntuales muy concretas (p.ej. añadir un `rules/R-*.md` cuyo placeholder figuraba en `design.md` pero no existía). No se mueven, no se renombran sin avisar.

### 1.3 Estructura de carpetas

```
.sdd/drafts/YYYY-MM-DD_HH-MM_<slug>/
├── design-guidelines.md          # opcional
├── specification.md
├── analysis/                     # REQUIRED como hermano del design/
│   ├── analysis.md
│   ├── entity-<Nombre>.md
│   ├── screen-<nombre>.md
│   └── tests.md                  # opcional (solo si el spec tiene escenarios ESC-NNN)
└── design/                       # objeto de esta revisión
    ├── design.md
    ├── domains/<Entidad>.xml
    ├── views/<Fichero>.xml
    ├── menus.xml
    ├── tests.md                  # copia literal de analysis/tests.md
    └── rules/R-<Entidad>-NNN.md  # opcional
```

---

## 2. Principios

### 2.1 No regenerar

**MUST NOT** reconstruir el diseño desde el análisis. Esta revisión solo edita lo que ya está, preservando la intención del autor original (humano o subagente). Si falta una pieza estructural (entidad, sección obligatoria), **STOP** y pregunta — no la inventes.

### 2.2 Corrección mecánica vs pregunta al usuario

**MUST** aplicar correcciones directamente solo cuando la respuesta correcta es inequívoca (frontmatter inválido y deducible, falta de copia literal de `tests.md`, error de sintaxis XML evidente). **MUST** usar `AskUserQuestion` para todo lo que requiera juicio (qué nombre canónico tiene un campo, si una regla R es compleja o trivial, cómo resolver una divergencia diff).

### 2.3 Cobertura V/R/U es el núcleo

La razón de ser de esta revisión es garantizar que **cada V/R/U del análisis tiene una ubicación real en el diseño** y que **toda ubicación referenciada en la matriz existe** en algún fichero. Es la primera invariante que se verifica y la última que se cierra (§5.3).

---

## 3. Fase 0 — Localizar la carpeta de diseño

Variante de la **Fase 0 del skill `sdd-designer-system`** (§4):

- Caso 1 — ruta explícita a `design/` o a `design.md`: validar frontmatter del `design.md`.
- Caso 2 — sin ruta: auto-detectar la última carpeta `.sdd/drafts/YYYY-MM-DD_HH-MM_*/`, buscar `design/design.md` dentro, confirmar con `AskUserQuestion`.

Si no existe `analysis/analysis.md` hermano en la carpeta de la iniciativa, **ERROR** y detente con:

> Error: no se encuentra `analysis/` en la carpeta de la iniciativa; es obligatorio para validar la cobertura V/R/U → diseño.

Apéndice A del skill original aplica con `--in=<ruta-design>`.

---

## 4. Fase 1 — Cargar contrato y leer los ficheros

1. **MUST** cargar el contrato leyendo `.claude/skills/sdd-designer-system/SKILL.md` (secciones §2.x — principios — y §6.x — plantilla de `design.md`). No recitar su contenido aquí; consultarlo en tiempo real.
2. Leer todos los `entity-*.md` y `screen-*.md` del `analysis/` hermano y extraer la lista completa de IDs `V-<Entidad>-NNN`, `R-<Entidad>-NNN`, `U-<slug>-NNN`.
3. Leer `design.md`, todos los `domains/*.xml`, `views/*.xml`, `menus.xml` y los `rules/R-*.md` si existen.
4. Si el frontmatter de `design.md` no es `type: design`: **ERROR** y detente. **MUST NOT** continuar la revisión sin frontmatter válido.

---

## 5. Fase 2 — Validaciones y correcciones

**MUST** aplicar el principio §2.2 en cada subsección: corrección mecánica solo si es inequívoca; `AskUserQuestion` si hay juicio.

### 5.1 Estructura de ficheros

- `design.md` con frontmatter `type: design`.
- Por cada entidad del `analysis/` existe un `domains/<Entidad>.xml`. Si falta uno, preguntar al usuario antes de hacer nada — falta diseño, no es trabajo de la revisión generarlo.
- Por cada `<action-view>` referenciado desde `design.md`, existe un `views/<Fichero>.xml` (regla "un `<action-view>` por fichero").
- Existe `menus.xml` con los `<menuitem>` del subsistema.
- Cada `rules/R-<Entidad>-NNN.md` está referenciado desde el comentario del método `fireActionRule_*` correspondiente en `design.md`, y cada referencia a `rules/R-*.md` desde `design.md` corresponde a un fichero existente. Las inconsistencias se reportan y se preguntan.

### 5.1.1 Secciones canónicas de `design.md`

Según la plantilla §6.2.2 del skill original, `design.md` debe contener — además del frontmatter `type: design` — estas piezas obligatorias:

1. **Cabecera** con título `# Diseño: <Nombre>` y las cuatro líneas de metadatos: `**Objetivo:**`, `**Capa:**`, `**Análisis de origen:**` y `**Skills necesarios para la implementación:**`.
2. **`## Ficheros a crear o modificar`** — tabla con columnas `Fichero | Acción | Skill | Descripción` y al menos una fila.
3. **`## Pasos`** — al menos un paso (`### Paso N — <Título>`), respetando el orden obligatorio del §6.3 del skill original (estáticos → dominios → servicios → repositorios → controladores → vistas → menús → seguridad → datos iniciales → verificación final). El review **no** reordena los pasos automáticamente: si detecta desorden, lo reporta y pregunta.
4. **`## Trazabilidad V/R/U → ubicación`** — tabla/matriz con una entrada por V/R/U del análisis (su validación de cobertura vive en §5.3, pero la presencia de la sección se valida aquí).

Secciones condicionales/opcionales:

- `## Frontera de confianza — AllowProperties por acción` — **obligatoria si y solo si** el diseño declara acciones invocadas desde `@CallMethod`. Validada en §5.3.1.
- `## Notas de unificación` — solo si el agente principal dejó constancia de decisiones tomadas durante la unificación (§6.5 del skill original).
- `## Conflictos detectados con guías` — solo si hubo contradicciones entre guías de diseño y análisis (§6.2 del skill original).
- `## Invariantes de las guías` — **obligatoria si y solo si** existe `design-guidelines.md` en la raíz de la iniciativa. Validada en §5.9.
- `### Excepciones a las invariantes` — solo si alguna invariante quedó como excepción explícita aceptada por el usuario (§6.7 opción c del skill original).

Para cada sección **obligatoria** que falte:

- Si es la **cabecera** o la línea de metadatos: corrección mecánica si el dato se puede deducir sin ambigüedad del análisis (p.ej. `Análisis de origen` apunta a `analysis/analysis.md` hermano). Si no es deducible (p.ej. el `Objetivo` o las `Skills necesarios`), preguntar al usuario.
- Si es **`## Ficheros a crear o modificar`**, **`## Pasos`** o **`## Trazabilidad V/R/U → ubicación`**: **no** regenerar el contenido — son el núcleo del diseño. Reportar la ausencia al usuario y ofrecer (a) abortar la revisión y relanzar `/sdd-designer-system`, o (b) añadir un placeholder vacío para que el usuario lo complete a mano.

Si encuentras secciones **adicionales** no previstas en la plantilla (p.ej. apuntes del autor en un `## Decisiones` o un `## TODO`), **MUST NOT** borrarlas: pregunta si forman parte del diseño o si son notas de trabajo a archivar.

### 5.2 Validación XML con xmllint

Para cada XML del diseño, lanzar `xmllint --noout --schema <xsd>` contra el XSD que corresponda:

- ✅ CORRECTO: `xmllint --noout --schema ../axelor-open-platform/axelor-core/src/main/resources/domain-models.xsd design/domains/Alumno.xml`
- ✅ CORRECTO: `xmllint --noout --schema ../axelor-open-platform/axelor-core/src/main/resources/object-views.xsd design/views/Alumno-form.xml`
- ✅ CORRECTO: `xmllint --noout --schema ../axelor-open-platform/axelor-core/src/main/resources/object-views.xsd design/menus.xml`
- ❌ INCORRECTO: validar `design/menus.xml` contra `domain-models.xsd` (los menús son vistas, no dominios).
- ❌ INCORRECTO: omitir `--noout` (genera ruido en stdout que enmascara el resultado).

Tratamiento de errores:

- Error de **sintaxis** evidente (atributo faltante, orden de elementos) → corrección mecánica y reintentar `xmllint`.
- Error **semántico** (referencia a entidad inexistente, FQN inválido) → **STOP** y pregunta al usuario. **MUST NOT** intentar arreglos automáticos.

### 5.3 Cobertura V/R/U → ubicación (núcleo de esta revisión)

`design.md` debe contener una sección **"Trazabilidad V/R/U → ubicación"** (ver §6 del skill original) con una fila por cada V/R/U del análisis indicando dónde está implementada (qué fichero, qué clase/método, qué `<action-attrs>`/`<action-validate>`/etc.). El review:

1. Construye la lista completa de IDs V/R/U leyendo el `analysis/`.
2. Compara contra la matriz de trazabilidad del `design.md`.
3. Para cada V/R/U **sin entrada** en la matriz: preguntar al usuario si la ubicación se le ha olvidado documentar (en cuyo caso se añade tras la respuesta) o si la regla se ha descartado deliberadamente (en cuyo caso se añade entrada con motivo).
4. Para cada V/R/U **con entrada en la matriz pero cuya ubicación referenciada no existe** en los XML/markdown del diseño (p.ej. la matriz dice "en `views/TareaCorreo-form.xml` → `<action-attrs name="action-tarea-correo-attrs-001">`" pero ese fichero o esa acción no existen): preguntar si hay que crear la pieza, corregir la referencia o eliminar la entrada de la matriz.

Reportar los contadores finales: V/R/U cubiertos, V/R/U sin cubrir, entradas con referencia rota.

### 5.3.1 Frontera de confianza — AllowProperties y campos `servidor` (ver `[[k-secure-coding]]` §3)

Si el diseño declara al menos una acción del servicio invocada desde un `@CallMethod`, el `design.md` **MUST** contener la sección `## Frontera de confianza — AllowProperties por acción` con una tabla por cada una de esas acciones (haya o no campos `servidor` en el análisis).

Validaciones:

1. **Sección presente:** si el diseño tiene acciones `@CallMethod` y la sección falta, reportar al usuario y ofrecer (a) relanzar `/sdd-designer-system`, (b) añadir un placeholder a completar a mano. **MUST NOT** inventar la sección automáticamente.
2. **Coherencia con `entity-*.md`:** la columna `Origen` de cada tabla **MUST** coincidir con la columna "Origen del valor" del `entity-*.md` correspondiente. Cualquier divergencia es un error: preguntar al usuario qué versión es la canónica.
3. **Reglas de `[[k-secure-coding]]` §3:** para cada tabla, aplicar las reglas del §3 (forma elegida válida según los `servidor` que la acción cubre, ningún `servidor` en una whitelist, cobertura completa si `createAllowAllProperties`). Cualquier fallo se reporta como **vulnerabilidad de mass-assignment** al usuario para corregir. Si `createAllowAllProperties` carece de la lista de ubicaciones de asignación incondicional, no es admisible → preguntar si pasa a whitelist o completa la cobertura.
4. **Asignación incondicional en servicios:** para cada R-Antes-de-Crear que asigne un campo `servidor`, el comentario de `fireActionRule_*` correspondiente en el `design.md` **MUST** documentar asignación **incondicional**. Detector mecánico (`[[k-secure-coding]]` §3.4):

```
grep -nE "if\s*\(.*==\s*null\s*\).*set[A-Z]" design.md
```

   Cualquier coincidencia que afecte a un campo `servidor` es un fallo: preguntar al usuario antes de corregir (la corrección es eliminar el `if`).

5. **DTO de alta programática:** si el diseño documenta un DTO `record` para alta programática, comprobar que cualquier campo `servidor` listado en el DTO tiene justificación explícita. Si no la tiene, preguntar.

### 5.4 Reglas arquitectónicas (ver §2.7 del skill original)

- **Un `<action-view>` por fichero** en `design/views/`. Si un fichero tiene varios, preguntar para partirlo.
- **FQN consistentes** con la arquitectura del proyecto.
  - ✅ CORRECTO: `com.educaflow.subsystem.firmas.service.SolicitudFirmaService`
  - ✅ CORRECTO: `com.educaflow.system.gestioncentro.controller.AlumnoController`
  - ❌ INCORRECTO: `com.educaflow.firmas.SolicitudFirmaService` (falta el segmento `subsystem`/`system`).
  - ❌ INCORRECTO: `com.axelor.apps.firmas.SolicitudFirmaService` (paquete raíz fuera del proyecto).
- **Sin código Java de implementación** en los comentarios de métodos en `design.md`: solo descripción del cuerpo (qué reglas aplica, qué llamadas hace, efectos colaterales).
  - ✅ CORRECTO: `// Aplica V-Alumno-001 (DNI no vacío) y delega en RegistroService.crear().`
  - ❌ INCORRECTO: `// if (alumno.getDni() == null) throw new BusinessException(...);` (cuerpo Java real → reportarlo).
- **Capas correctas** según §2.5 del skill original: cada V en atributos declarativos del modelo XML (`required`, `unique`, `min`, `max`), en cliente (`<action-validate>`/`<action-condition>`) o en servidor (`validateInsert`/`validateUpdate`/`validateRemove` del `*ServiceImpl`); cada R **solo** en servidor como `fireActionRule_*` del `*ServiceImpl` (Antes/Después de `super.*`); cada U en la vista como atributo `showIf`/`hideIf`/`readonlyIf`/`requiredIf` o como `<action-attrs>`/`<action-record>` desde `onNew`/`onLoad`/`onChange`.

### 5.5 Reglas R complejas en `rules/`

Para cada `R-<Entidad>-NNN` que el análisis describe como compleja (estado, integración externa, algoritmo no trivial — criterio §6.6 del skill original), comprobar que existe un `rules/R-<Entidad>-NNN.md` con el diseño detallado (clases auxiliares con FQN, interfaces, enums, secuencia de invocación). Si el `design.md` no marca la regla como compleja pero el contenido del análisis sugiere que lo es, preguntar al usuario.

Para cada `rules/R-*.md` existente: comprobar que está referenciado desde `design.md` y que no contiene cuerpos de método en Java (descripción y firmas, sí; implementación, no).

### 5.6 Prohibiciones en `design.md`

Aplicar las prohibiciones del §2.2 del skill original al texto markdown (no a los XML, que tienen su validación XSD aparte). Ejemplos:

- **Cuerpos de método Java** en bloque de código.
  - ✅ CORRECTO (descripción): `` `crear(SolicitudFirmaDTO dto)` — valida V-Solicitud-001/002, persiste y notifica al firmante. ``
  - ❌ INCORRECTO: bloque ```` ```java public void crear(...) { ... } ``` ```` con implementación real.
- **JPQL real** en tablas o ejemplos.
  - ✅ CORRECTO: "Busca solicitudes pendientes del usuario actual."
  - ❌ INCORRECTO: `SELECT s FROM SolicitudFirma s WHERE s.estado = 'PENDIENTE'` (es implementación).
- **Acoplamiento a `expedientes`/`tiposexpedientes`/`tramites`** (arquitectura distinta — **MUST NOT** como referencia salvo iniciativas dentro de ese subsistema).
  - ❌ INCORRECTO: `extends com.educaflow.subsystem.expedientes.AbstractExpedienteService`.

### 5.7 Tests E2E (`tests.md`) — copia literal del análisis

El `design/tests.md` debe ser **una copia idéntica** del `analysis/tests.md` (mismo principio que para los XML: el diseñador es un paso transparente para los tests, no los reescribe).

1. Si **`analysis/tests.md` existe pero `design/tests.md` no**: copiarlo con `cp` y avisar al usuario. La revisión sí puede materializar esta copia porque es 100% mecánica.
2. Si **ambos existen pero su contenido difiere**: comparar con `diff`. Si la divergencia parece edición intencional sobre el diseño (cambios marcados, ajustes de pasos), preguntar al usuario qué versión es la canónica:
   - Mantener `design/tests.md` (y propagar al `analysis/`).
   - Mantener `analysis/tests.md` (y sobrescribir `design/tests.md`).
   - Mezclar manualmente (abortar la revisión, pedir al usuario que lo resuelva fuera del skill).
3. Si **ninguno existe**: comprobar si el `specification.md` de la iniciativa tiene sección "Escenarios" con `ESC-NNN`. **Si tiene escenarios**, es un olvido: avisar y ofrecer relanzar `/sdd-analyst-system` para generar `tests.md`. **Si no tiene escenarios**, es el caso legacy correcto: la iniciativa no usa tests E2E y `/sdd-debug-app` no tendrá tests que ejecutar. Sin acción.

**MUST NOT** reformatear, recortar o "limpiar" `design/tests.md` durante la revisión: su contenido es contrato. **MUST NOT** modificarlo salvo en el caso 1 (copia íntegra desde `analysis/tests.md`) o en el caso 2 con decisión explícita del usuario.

### 5.8 Coherencia diseño ↔ análisis (sin redibujar el diseño)

- Cada `domains/<Entidad>.xml` corresponde a una entidad del análisis. Los campos del XML coinciden con los del `entity-*.md` (mismos nombres, mismos enums).
  - ✅ CORRECTO: `entity-Solicitud.md` declara `motivoFallo: String` y `domains/Solicitud.xml` contiene `<string name="motivoFallo"/>`.
  - ❌ INCORRECTO: `entity-Solicitud.md` declara `motivoFallo` y el XML lo nombra `motivoError` → **MUST** preguntar a cuál renombrar; no decidir unilateralmente.
- Cada `<action-view>` corresponde a una pantalla del análisis. Las columnas de Grid en `views/*.xml` coinciden con las del `screen-*.md`. Los formularios incluyen los campos descritos en los paneles.
- Los `<menuitem>` de `menus.xml` corresponden a los menús del `analysis.md`.

### 5.9 Re-verificación de las invariantes derivadas de las guías

Si existe `design-guidelines.md` en la raíz de la iniciativa, el `design.md` **debe** contener la sección `## Invariantes de las guías` (tabla `G-NNN | Invariante | Ubicación | Verificación`). Esta sección la genera el designer en §8.4 del skill original a partir de §5.4 y se verifica mecánicamente en §6.7 contra el diseño antes de materializarse.

Este review **re-ejecuta** la verificación mecánica contra el diseño ya materializado:

1. Comprobar que la sección `## Invariantes de las guías` existe en `design.md`. Si no existe y `design-guidelines.md` está presente: reportar como error y preguntar al usuario si (a) reabrir `/sdd-designer-system`, (b) añadir la sección manualmente, o (c) borrar el `design-guidelines.md` si ya no aplica. **No** intentar derivar las invariantes aquí — eso es trabajo del designer.
2. Para cada fila `G-NNN` cuya columna "Verificación" sea un comando `grep`, ejecutar el grep tal cual contra `design/` y `design.md`. Filtrar las coincidencias que la invariante autoriza (las que están en la "Ubicación que la cumple" declarada). Si quedan coincidencias no autorizadas, la invariante está violada.
3. Para verificaciones `manual`, releer el diseño y juzgar. No automatizable.
4. Si la invariante figura en `### Excepciones a las invariantes`, **saltarla** — el usuario ya aceptó la fuga explícitamente; el review no la vuelve a discutir.

Cada invariante violada se reporta como un hallazgo individual. Si hay violaciones, ofrecer al usuario las mismas opciones que el designer en §6.7: corregir localmente, reformular la guía, o documentar como excepción.

**CRITICAL**: este paso solo valida el contrato (invariantes ↔ diseño). **MUST NOT** re-derivar invariantes nuevas a partir del `design-guidelines.md`; eso es responsabilidad del designer y reabrirlo es la vía correcta si la lista parece incompleta.

### 5.10 Checklist final del review

Aplicar el **checklist §6.4 del skill original** entero al terminar las correcciones. Adicionalmente, **MUST** marcar todos los puntos de este checklist local antes de pasar a la Fase 3:

- [ ] Frontmatter `type: design` presente y bien formado.
- [ ] Secciones obligatorias del `design.md` presentes (§5.1.1): cabecera + metadatos, `## Ficheros a crear o modificar`, `## Pasos`, `## Trazabilidad V/R/U → ubicación`.
- [ ] Todos los XML pasan `xmllint --noout --schema` contra el XSD correspondiente (§5.2).
- [ ] Cada V/R/U del `analysis/` aparece en la matriz de trazabilidad y su ubicación referenciada existe en algún fichero del diseño (§5.3).
- [ ] Si el diseño declara acciones invocadas desde `@CallMethod`: sección `## Frontera de confianza — AllowProperties por acción` presente con una tabla por cada una de esas acciones, columnas `Origen` coherentes con `entity-*.md`, y todas las reglas de `[[k-secure-coding]]` §3 satisfechas. Ningún `if (campo == null) setCampo(...)` para campos `servidor` en comentarios o cuerpos (§5.3.1).
- [ ] Un `<action-view>` por fichero en `design/views/` y FQN coherentes (§5.4).
- [ ] Ningún comentario de método en `design.md` contiene código Java de implementación (§5.4).
- [ ] Cada `R-*` compleja tiene su `rules/R-*.md` y viceversa (§5.5).
- [ ] `design.md` sin tecnicismos prohibidos (cuerpos Java, JPQL, acoplamiento a `expedientes`/`tiposexpedientes`/`tramites`) (§5.6).
- [ ] `design/tests.md` idéntico a `analysis/tests.md` o caso legacy documentado (§5.7).
- [ ] Coherencia diseño↔análisis verificada (campos, columnas grid, menús) (§5.8).
- [ ] Si existe `design-guidelines.md`: sección `## Invariantes de las guías` presente y sin violaciones abiertas (§5.9).

**LIMIT**: máximo 3 iteraciones de corrección. Si tras la 3ª iteración siguen quedando puntos sin marcar, **MUST NOT** dar la revisión por buena: documenta los hallazgos residuales en la Fase 3 y avísalo al usuario.

**CRITICAL**: un review **MUST NOT** cerrarse mientras quede una invariante violada y sin excepción documentada en `### Excepciones a las invariantes` (§5.9).

---

## 6. Fase 3 — Informe al usuario

```
Revisión de design/ completada.

Ficheros revisados:
  - design.md
  - domains/*.xml (N)
  - views/*.xml (M)
  - menus.xml
  - tests.md (idéntico a analysis/tests.md: sí/no)
  - rules/R-*.md (K)

Validación XML (xmllint):
  - OK: N
  - Errores: M (listados abajo si los hubo)

Cobertura V/R/U:
  - Total V/R/U en analysis/: X
  - Cubiertos en la matriz de trazabilidad: Y
  - Con referencia rota a fichero/acción inexistente: Z
  - Sin entrada en la matriz (pendiente del usuario): W

Correcciones aplicadas mecánicamente (N):
  - <lista corta>

Decisiones tomadas tras pregunta al usuario (N):
  - <lista corta>

Puntos del checklist que siguen abiertos (N):
  - <lista corta>
```

Si nada hubo que tocar:

```
La carpeta design/ ya está conforme con el contrato actual. No se ha modificado nada.
```

---

## Quick Guidelines

- **No** regeneras el diseño: trabajas sobre los ficheros que hay y preservas la intención del autor.
- Corrección **mecánica** solo cuando es inequívoca; `AskUserQuestion` para todo lo que requiera juicio.
- `analysis/` hermano es **REQUIRED**: sin él no se puede validar la cobertura V/R/U → diseño.
- Cobertura V/R/U es el núcleo: cada V/R/U del análisis aparece en la matriz **y** su ubicación referenciada existe en un fichero real.
- XML del diseño **MUST** validar contra los XSD de Axelor con `xmllint`; los errores semánticos no se autoarreglan.
- `design/tests.md` es **contrato**: idéntico a `analysis/tests.md` salvo decisión explícita del usuario.
- **MUST NOT** en `design.md`: cuerpos Java, JPQL, acoplamiento a `expedientes`/`tiposexpedientes`/`tramites`.
- **LIMIT**: máximo 3 iteraciones del checklist §5.10 antes de reportar residuos al usuario.

---

## Apéndice A — Override de rutas (para testing)

Análogo al Apéndice A del skill `sdd-designer-system` (aquí las rutas apuntan a la carpeta `design/` a revisar):

- `--in=<ruta>` — fichero `design.md` o carpeta `design/` de entrada explícita.
- `--out=<ruta>` — carpeta de salida si se quiere revisar copiando en vez de editar en sitio.
- `--root=<ruta>` — raíz alternativa a `.sdd/drafts/`.

En uso normal no se especifican.
