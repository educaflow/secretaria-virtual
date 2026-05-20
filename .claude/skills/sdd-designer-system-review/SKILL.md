---
name: sdd-designer-system-review
description: Revisa la carpeta `design/` ya existente de una iniciativa SDD (`design.md` + `domains/*.xml` + `views/*.xml` + `menus.xml` + `tests.md` + opcionalmente `rules/R-*.md`) sin regenerarla. Valida frontmatter `type: design`, conformidad XML con los XSD de Axelor mediante `xmllint`, **cobertura total V/R/U** (cada V/R/U del análisis aparece en la matriz de trazabilidad y tiene una ubicación real en algún fichero del diseño), reglas arquitectónicas (un `<action-view>` por fichero, FQN coherentes, no código Java en cuerpos de método), referencias entre `design.md` y los ficheros `rules/R-*.md` para reglas complejas, ausencia de tecnicismos prohibidos en `design.md`, **y que `design/tests.md` sea copia idéntica de `analysis/tests.md`** (el diseñador no modifica los tests; son contrato fijo). Corrige mecánicamente lo inequívoco; pregunta al usuario lo ambiguo. **No** regenera el diseño desde el análisis; **no** lanza subagentes en paralelo — preserva la intención de las ediciones manuales.
---

# sdd-designer-system-review

Eres un revisor de planes de diseño. Tomas la carpeta `design/` de una iniciativa SDD — generada por `/sdd-designer-system` y posiblemente editada a mano después — y la dejas conforme con el contrato actual del skill `sdd-designer-system` (frontmatter, XML válidos contra XSD, cobertura V/R/U, reglas arquitectónicas, separación diseño/implementación). **No regeneras nada**: trabajas sobre el contenido que hay, preservando la intención del autor.

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

---

## 2. Fase 0 — Localizar la carpeta de diseño

Variante de la **Fase 0 del skill `sdd-designer-system`** (§4):

- Caso 1 — ruta explícita a `design/` o a `design.md`: validar frontmatter del `design.md`.
- Caso 2 — sin ruta: auto-detectar la última carpeta `.sdd/drafts/YYYY-MM-DD_HH-MM_*/`, buscar `design/design.md` dentro, confirmar con `AskUserQuestion`.

Si no existe `analysis/analysis.md` hermano en la carpeta de la iniciativa, detente con:

> Error: no se encuentra `analysis/` en la carpeta de la iniciativa; es obligatorio para validar la cobertura V/R/U → diseño.

Apéndice A del skill original aplica con `--in=<ruta-design>`.

---

## 3. Fase 1 — Cargar contrato y leer los ficheros

1. Cargar mentalmente las reglas leyendo `.claude/skills/sdd-designer-system/SKILL.md` §§ 2.1 (análisis = fuente de verdad), 2.2 (diseño vs implementación), 2.3 (XML real vs descripción), 2.4 (cobertura total V/R/U), 2.5 (mapeo de capas), 2.6 (validación XML con xmllint), 2.7 (reglas arquitectónicas), 2.8 (tests.md propaga sin modificar).
2. Leer todos los `entity-*.md` y `screen-*.md` del `analysis/` hermano y extraer la lista completa de IDs `V-<Entidad>-NNN`, `R-<Entidad>-NNN`, `U-<slug>-NNN`.
3. Leer `design.md`, todos los `domains/*.xml`, `views/*.xml`, `menus.xml` y los `rules/R-*.md` si existen.
4. Si el frontmatter de `design.md` no es `type: design`, detente con el mensaje de error correspondiente.

---

## 4. Fase 2 — Validaciones y correcciones

Mismo principio: corrección mecánica cuando es inequívoca, `AskUserQuestion` cuando hay juicio.

### 4.1 Estructura de ficheros

- `design.md` con frontmatter `type: design`.
- Por cada entidad del `analysis/` existe un `domains/<Entidad>.xml`. Si falta uno, preguntar al usuario antes de hacer nada — falta diseño, no es trabajo de la revisión generarlo.
- Por cada `<action-view>` referenciado desde `design.md`, existe un `views/<Fichero>.xml` (regla "un `<action-view>` por fichero").
- Existe `menus.xml` con los `<menuitem>` del subsistema.
- Cada `rules/R-<Entidad>-NNN.md` está referenciado desde el comentario del método `fireActionRule_*` correspondiente en `design.md`, y cada referencia a `rules/R-*.md` desde `design.md` corresponde a un fichero existente. Las inconsistencias se reportan y se preguntan.

### 4.1.1 Secciones canónicas de `design.md`

Según la plantilla §6.2.2 del skill original, `design.md` debe contener — además del frontmatter `type: design` — estas piezas obligatorias:

1. **Cabecera** con título `# Diseño: <Nombre>` y las cuatro líneas de metadatos: `**Objetivo:**`, `**Capa:**`, `**Análisis de origen:**` y `**Skills necesarios para la implementación:**`.
2. **`## Ficheros a crear o modificar`** — tabla con columnas `Fichero | Acción | Skill | Descripción` y al menos una fila.
3. **`## Pasos`** — al menos un paso (`### Paso N — <Título>`), respetando el orden obligatorio del §6.3 del skill original (estáticos → dominios → servicios → repositorios → controladores → vistas → menús → seguridad → datos iniciales → verificación final). El review **no** reordena los pasos automáticamente: si detecta desorden, lo reporta y pregunta.
4. **`## Trazabilidad V/R/U → ubicación`** — tabla/matriz con una entrada por V/R/U del análisis (su validación de cobertura vive en §4.3, pero la presencia de la sección se valida aquí).

Secciones opcionales (no obligatorias, no reportar si faltan):

- `## Notas de unificación` — solo si el agente principal dejó constancia de decisiones tomadas durante la unificación (§6.5 del skill original).
- `## Conflictos detectados con guías` — solo si hubo contradicciones entre guías de diseño y análisis (§6.2.1).
- `## Invariantes de las guías` — **obligatoria si y solo si** existe `design-guidelines.md` en la raíz de la iniciativa. Validada en §4.10.
- `### Excepciones a las invariantes` — solo si alguna invariante quedó como excepción explícita aceptada por el usuario (§6.7 opción c del skill original).

Para cada sección **obligatoria** que falte:

- Si es la **cabecera** o la línea de metadatos: corrección mecánica si el dato se puede deducir sin ambigüedad del análisis (p.ej. `Análisis de origen` apunta a `analysis/analysis.md` hermano). Si no es deducible (p.ej. el `Objetivo` o las `Skills necesarios`), preguntar al usuario.
- Si es **`## Ficheros a crear o modificar`**, **`## Pasos`** o **`## Trazabilidad V/R/U → ubicación`**: **no** regenerar el contenido — son el núcleo del diseño. Reportar la ausencia al usuario y ofrecer (a) abortar la revisión y relanzar `/sdd-designer-system`, o (b) añadir un placeholder vacío para que el usuario lo complete a mano.

Si encuentras secciones **adicionales** no previstas en la plantilla (p.ej. apuntes del autor en un `## Decisiones` o un `## TODO`), **no** las borres: pregunta si forman parte del diseño o si son notas de trabajo a archivar.

### 4.2 Validación XML con xmllint

Para cada XML del diseño, lanzar `xmllint --noout --schema <xsd>` contra el XSD que corresponda:

- `design/domains/*.xml` → `../axelor-open-platform/axelor-core/src/main/resources/domain-models.xsd`.
- `design/views/*.xml` y `design/menus.xml` → `../axelor-open-platform/axelor-core/src/main/resources/object-views.xsd`.

Las rutas concretas y los comandos están en el §2.6 del skill original. Si `xmllint` falla en algún fichero, reportar el error literal y preguntar al usuario si la revisión debe intentar arreglarlo (en errores de sintaxis evidentes — atributo faltante, orden de elementos — sí; en errores semánticos — referencia a entidad inexistente — no, hay que volver al diseño).

### 4.3 Cobertura V/R/U → ubicación (núcleo de esta revisión)

`design.md` debe contener una sección **"Trazabilidad V/R/U → ubicación"** (ver §6 del skill original) con una fila por cada V/R/U del análisis indicando dónde está implementada (qué fichero, qué clase/método, qué `<action-attrs>`/`<action-validate>`/etc.). El review:

1. Construye la lista completa de IDs V/R/U leyendo el `analysis/`.
2. Compara contra la matriz de trazabilidad del `design.md`.
3. Para cada V/R/U **sin entrada** en la matriz: preguntar al usuario si la ubicación se le ha olvidado documentar (en cuyo caso se añade tras la respuesta) o si la regla se ha descartado deliberadamente (en cuyo caso se añade entrada con motivo).
4. Para cada V/R/U **con entrada en la matriz pero cuya ubicación referenciada no existe** en los XML/markdown del diseño (p.ej. la matriz dice "en `views/TareaCorreo-form.xml` → `<action-attrs name="action-tarea-correo-attrs-001">`" pero ese fichero o esa acción no existen): preguntar si hay que crear la pieza, corregir la referencia o eliminar la entrada de la matriz.

Reportar los contadores finales: V/R/U cubiertos, V/R/U sin cubrir, entradas con referencia rota.

### 4.4 Reglas arquitectónicas (ver §2.7 del skill original)

- **Un `<action-view>` por fichero** en `design/views/`. Si un fichero tiene varios, preguntar para partirlo.
- **FQN consistentes** con la arquitectura del proyecto (`com.educaflow.subsystem.<x>.*`, `com.educaflow.system.<x>.*`). Reportar FQN que no encajen.
- **Sin código Java de implementación** en los comentarios de métodos en `design.md`: solo descripción del cuerpo (qué reglas aplica, qué llamadas hace, efectos colaterales). Si encuentras bloques de código Java reales en comentarios, reportarlo.
- **Capas correctas** según §2.5: dónde va cada V (validator / `<action-validate>` / `validateInsert`), cada R (servicio / controlador / `<action-method>`) y cada U (`<action-attrs>` / `<action-record>` / `<action-condition>` / atributo del campo).

### 4.5 Reglas R complejas en `rules/`

Para cada `R-<Entidad>-NNN` que el análisis describe como compleja (estado, integración externa, algoritmo no trivial — criterio §6.6 del skill original), comprobar que existe un `rules/R-<Entidad>-NNN.md` con el diseño detallado (clases auxiliares con FQN, interfaces, enums, secuencia de invocación). Si el `design.md` no marca la regla como compleja pero el contenido del análisis sugiere que lo es, preguntar al usuario.

Para cada `rules/R-*.md` existente: comprobar que está referenciado desde `design.md` y que no contiene cuerpos de método en Java (descripción y firmas, sí; implementación, no).

### 4.6 Prohibiciones en `design.md`

Aplicar las prohibiciones del §2.2 del skill original al texto markdown (no a los XML, que tienen su validación XSD aparte):

- Cuerpos de método Java en bloque de código.
- Tablas con JPQL real (`SELECT … FROM …`).
- Acoplamiento a clases concretas de `expedientes`, `tiposexpedientes`, `tramites` (arquitectura distinta — prohibido como referencia).

### 4.7 Tests E2E (`tests.md`) — copia literal del análisis

El `design/tests.md` debe ser **una copia idéntica** del `analysis/tests.md` (mismo principio que para los XML: el diseñador es un paso transparente para los tests, no los reescribe).

1. Si **`analysis/tests.md` existe pero `design/tests.md` no**: copiarlo con `cp` y avisar al usuario. La revisión sí puede materializar esta copia porque es 100% mecánica.
2. Si **ambos existen pero su contenido difiere**: comparar con `diff`. Si la divergencia parece edición intencional sobre el diseño (cambios marcados, ajustes de pasos), preguntar al usuario qué versión es la canónica:
   - Mantener `design/tests.md` (y propagar al `analysis/`).
   - Mantener `analysis/tests.md` (y sobrescribir `design/tests.md`).
   - Mezclar manualmente (abortar la revisión, pedir al usuario que lo resuelva fuera del skill).
3. Si **ninguno existe**: comprobar si el `specification.md` de la iniciativa tiene sección "Flujos principales" con `F-NNN`. **Si tiene flujos**, es un olvido: avisar y ofrecer relanzar `/sdd-analyst-system` para generar `tests.md`. **Si no tiene flujos**, es el caso legacy correcto: la iniciativa no usa tests E2E y `/sdd-implementer-system` saltará la Fase 3.5 sin error. Sin acción.

**Prohibido** reformatear, recortar o "limpiar" `design/tests.md` durante la revisión: su contenido es contrato.

### 4.8 Coherencia diseño ↔ análisis (sin redibujar el diseño)

- Cada `domains/<Entidad>.xml` corresponde a una entidad del análisis. Los campos del XML coinciden con los del `entity-*.md` (mismos nombres, mismos enums). Los desajustes se reportan: en `Modelo de datos` del análisis hay `motivoFallo` pero en el XML está como `motivoError` → preguntar a cuál renombrar.
- Cada `<action-view>` corresponde a una pantalla del análisis. Las columnas de Grid en `views/*.xml` coinciden con las del `screen-*.md`. Los formularios incluyen los campos descritos en los paneles.
- Los `<menuitem>` de `menus.xml` corresponden a los menús del `analysis.md`.

### 4.9 Re-verificación de las invariantes derivadas de las guías

Si existe `design-guidelines.md` en la raíz de la iniciativa, el `design.md` **debe** contener la sección `## Invariantes de las guías` (tabla `G-NNN | Invariante | Ubicación | Verificación`). Esta sección la genera el designer en §8.4 del skill original a partir de §5.3.bis y se verifica mecánicamente en §6.7 contra el diseño antes de materializarse.

Este review **re-ejecuta** la verificación mecánica contra el diseño ya materializado:

1. Comprobar que la sección `## Invariantes de las guías` existe en `design.md`. Si no existe y `design-guidelines.md` está presente: reportar como error y preguntar al usuario si (a) reabrir `/sdd-designer-system`, (b) añadir la sección manualmente, o (c) borrar el `design-guidelines.md` si ya no aplica. **No** intentar derivar las invariantes aquí — eso es trabajo del designer.
2. Para cada fila `G-NNN` cuya columna "Verificación" sea un comando `grep`, ejecutar el grep tal cual contra `design/` y `design.md`. Filtrar las coincidencias que la invariante autoriza (las que están en la "Ubicación que la cumple" declarada). Si quedan coincidencias no autorizadas, la invariante está violada.
3. Para verificaciones `manual`, releer el diseño y juzgar. No automatizable.
4. Si la invariante figura en `### Excepciones a las invariantes`, **saltarla** — el usuario ya aceptó la fuga explícitamente; el review no la vuelve a discutir.

Cada invariante violada se reporta como un hallazgo individual. Si hay violaciones, ofrecer al usuario las mismas opciones que el designer en §6.7: corregir localmente, reformular la guía, o documentar como excepción.

**Importante:** este paso solo valida el contrato (invariantes ↔ diseño). No re-deriva invariantes nuevas a partir del `design-guidelines.md`; eso es responsabilidad del designer y reabrirlo es la vía correcta si la lista parece incompleta.

### 4.10 Checklist completo

Aplicar el **checklist §6.4 del skill original** (subagente de diseño) y los **checks de la Fase 3 §7** (revisión del diseño unificado) entero al terminar las correcciones. Incluye explícitamente la verificación de §4.9 (invariantes) — un review no se cierra mientras quede una invariante violada y sin excepción documentada.

---

## 5. Fase 3 — Informe al usuario

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

## Apéndice A — Override de rutas (para testing)

Idéntico al Apéndice A del skill `sdd-designer-system`:

- `--in=<ruta>` — fichero `design.md` o carpeta `design/` de entrada explícita.
- `--out=<ruta>` — carpeta de salida si se quiere revisar copiando en vez de editar en sitio.
- `--root=<ruta>` — raíz alternativa a `.sdd/drafts/`.

En uso normal no se especifican.
