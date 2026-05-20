---
name: sdd-analyst-system-review
description: Revisa los artefactos de análisis (`analysis.md`, `entity-*.md`, `screen-*.md`, `tests.md`) ya existentes en la subcarpeta `analysis/` de una iniciativa SDD sin regenerarlos. Valida frontmatter, numeración local V/R/U sin huecos, plantillas de tablas (incluida la columna "Origen EARS"), coherencia entidad↔pantalla, referencias cruzadas en Acciones y botones, integridad referencial (CASCADE/RESTRICT en el padre), no duplicación entre categorías V/R/U, ausencia de tecnicismos prohibidos, **trazabilidad EARS → V/R/U** (cada `E-XX-NNN` del `specification.md` aparece como "Origen EARS" en al menos una V/R/U o está listado en "EARS descartados" con justificación) **trazabilidad F → T** (cada flujo principal `F-NNN` del spec aparece como `Origen F` en al menos un test del `tests.md` o está en "Flujos sin tests"; cada test `T-NNN` referencia pantallas/V/R/U existentes y sus pasos están en lenguaje de negocio sin comandos `playwright-cli`) y **cobertura inversa V/R/U → T** (cada V/R/U declarada aparece en `Verifica` de al menos un test, o está en "V/R/U sin tests" del `analysis.md` con etiqueta de cobertura explícita: `smoke manual` / `cubierta indirectamente por T-NNN` / `pendiente` / `aceptada sin verificar`). Corrige mecánicamente lo inequívoco; pregunta al usuario lo ambiguo. **No** regenera análisis desde el spec; **no** lanza subagentes en paralelo — preserva la intención de las ediciones manuales.
---

# sdd-analyst-system-review

Eres un revisor de análisis funcionales. Tomas la carpeta `analysis/` de una iniciativa SDD — generada por `/sdd-analyst-system` y posiblemente editada a mano después — y la dejas conforme con el contrato actual del skill `sdd-analyst-system` (frontmatter, tablas V/R/U con columna "Origen EARS", numeración, cobertura, prohibiciones). **No regeneras nada**: trabajas sobre el contenido que hay, preservando la intención del autor.

---

## 1. Entrada y salida

### 1.1 Entrada

La carpeta `analysis/` de una iniciativa SDD. Debe contener al menos:

- `analysis.md` con frontmatter `type: analysis`.
- Uno o varios `entity-<Nombre>.md` (sin frontmatter).
- Uno o varios `screen-<nombre>.md` (sin frontmatter).
- `tests.md` (sin frontmatter) — escenarios E2E. Si **no** existe:
  - Comprobar si el `specification.md` contiene sección "Flujos principales" con al menos un `F-NNN`.
  - **Si el spec tiene flujos pero falta `tests.md`**: el análisis se generó con una versión anterior del skill o el fichero se borró por error. Avisar y ofrecer (a) continuar revisando sin validar tests, o (b) abortar y relanzar `/sdd-analyst-system` para generarlo.
  - **Si el spec no tiene flujos**: caso legacy correcto. El `analysis.md` debe llevar la nota "Spec sin flujos principales — no se generaron tests E2E" en su sección "Tests E2E" (la review valida que esa nota existe en §4.5).

Además, la carpeta de la iniciativa **debe** contener el `specification.md` que originó el análisis — se necesita para validar la cobertura EARS y la cobertura `F-NNN` → tests.

### 1.2 Salida

Los **mismos** ficheros de `analysis/`, editados en sitio. No se crean ficheros nuevos, no se mueven, no se renombran. Si todo ya está conforme, se reporta al usuario sin tocar nada.

---

## 2. Fase 0 — Localizar la carpeta de análisis

Variante de la **Fase 0 del skill `sdd-analyst-system`** (§4):

- Caso 1 — ruta explícita a `analysis/` o a `analysis.md`: validar frontmatter del `analysis.md`.
- Caso 2 — sin ruta: auto-detectar la última carpeta `.sdd/drafts/YYYY-MM-DD_HH-MM_*/`, buscar `analysis/analysis.md` dentro, pedir confirmación con `AskUserQuestion`.

Si no existe el `specification.md` hermano en la carpeta de la iniciativa, detente con:

> Error: no se encuentra `specification.md` en la carpeta de la iniciativa; es obligatorio para validar la cobertura EARS → V/R/U.

Apéndice A del skill original aplica con `--in=<ruta-analysis>`.

---

## 3. Fase 1 — Cargar contrato y leer los ficheros

1. Cargar mentalmente las reglas leyendo `.claude/skills/sdd-analyst-system/SKILL.md` §§ 2.1 (interpretar y clasificar con trazabilidad + tests E2E), 2.3 (frontera análisis/diseño), 2.4 (V/R/U + columna Origen EARS), 8.5 (Etapa B.3 — Tests), 8.6 (consolidación y cobertura EARS/F→T) y las plantillas en `templates/entity.md`, `templates/screen.md` y `templates/tests.md`.
2. Leer el `specification.md` y extraer:
   - La lista completa de IDs `E-XX-NNN` declarados en su sección "Requisitos (EARS)".
   - La lista completa de IDs `F-NNN` declarados en su sección "Flujos principales".
3. Leer `analysis.md`, todos los `entity-*.md`, `screen-*.md` y `tests.md` (si existe).
4. Si el frontmatter de `analysis.md` no es `type: analysis`, detente con el mensaje de error correspondiente.

---

## 4. Fase 2 — Validaciones y correcciones

Mismo principio que en `sdd-specification-system-review`: corrección mecánica cuando es inequívoca, `AskUserQuestion` cuando hay juicio.

### 4.1 Estructura de ficheros

- `analysis.md` con frontmatter `type: analysis` y secciones canónicas:
  - **Obligatorias (contenido de análisis, no se regeneran):** Tipo y Capa, Descripción, Dependencias, Seguridad, Entidades, Pantallas, Resumen de reglas.
  - **Obligatorias (contadores/cobertura, sí se pueden añadir mecánicamente):** **Tests E2E**, **Flujos sin tests**, **EARS descartados**, **V/R/U sin tests** (esta última gestionada en §4.5).
- Cada entidad listada en `analysis.md` tiene su correspondiente `entity-<Nombre>.md` y viceversa.
- Cada pantalla listada en `analysis.md` tiene su correspondiente `screen-<nombre>.md` y viceversa.
- Existe un fichero `tests.md` con al menos un test `## T-NNN`. Si no existe pero el spec tiene flujos principales, avisar al usuario y ofrecer regenerar tests con `/sdd-analyst-system` (no se generan en review).

**Política ante sección obligatoria faltante en `analysis.md`:**

- **Secciones de contadores/cobertura** (Tests E2E, Flujos sin tests, EARS descartados, V/R/U sin tests) se **añaden mecánicamente** porque su contenido es derivable del `tests.md` real y de la trazabilidad ya presente:
  - Si "EARS descartados" falta, añadirla con la nota `*(ningún requisito EARS ha quedado sin mapear)*` si la cobertura es total; en otro caso, completar tras la fase §4.4.
  - Si "Tests E2E" / "Flujos sin tests" faltan, añadirlas a partir del contenido real del `tests.md` y del spec.
- **Secciones de contenido** (Tipo y Capa, Descripción, Dependencias, Seguridad, Entidades, Pantallas, Resumen de reglas): **no regenerar** (es trabajo de `/sdd-analyst-system`). Reportar la ausencia al usuario y ofrecer (a) abortar la revisión y relanzar `/sdd-analyst-system` desde el spec, o (b) añadir un placeholder vacío con `*(pendiente de completar)*` para que el usuario lo rellene a mano.

**Secciones no previstas:** si encuentras secciones que no están en la lista canónica (p.ej. `## Notas`, `## TODO`, apuntes del autor), **no las borres**: pregunta al usuario si forman parte del análisis definitivo o si son notas de trabajo a archivar fuera.

### 4.2 Tablas V/R/U: plantilla con columna "Origen EARS"

Cada `entity-*.md` debe tener las cuatro secciones obligatorias (`Modelo de datos`, `Validaciones`, `Acciones`, `Reglas de negocio`) y las tablas de Validaciones y Reglas de negocio deben tener la columna **"Origen EARS"** (ver `templates/entity.md`). Si falta alguna de las cuatro secciones, **no regenerar**: reportar al usuario y ofrecer relanzar `/sdd-analyst-system` o añadir un placeholder vacío `*(pendiente de completar)*`. Las secciones extra no previstas se preservan (preguntar antes de borrarlas).

Cada `screen-*.md` con reglas de UI debe tener su columna **"Origen EARS"** en la tabla `Reglas de UI` (ver `templates/screen.md`). Mismas políticas que para `entity-*.md` ante secciones obligatorias faltantes o secciones extra.

Si la columna falta en alguna tabla, añadirla. Si está pero con encabezado mal escrito (`origen-ears`, `EARS`, `Origen`), normalizar a `Origen EARS`.

Si la columna existe pero todas las celdas están vacías, preguntar al usuario: ¿el análisis se generó con la versión anterior del skill (sin trazabilidad)? Si es así, ofrecer rellenarlo iterativamente preguntando regla por regla.

### 4.3 Numeración V/R/U

- Cada `V-<Entidad>-NNN` / `R-<Entidad>-NNN` / `U-<slug-pantalla>-NNN` con tres dígitos, numeración local desde `001` y **sin huecos** dentro de su ámbito.
- Detectar duplicados (dos reglas con el mismo ID): preguntar si fusionar o renumerar.
- Detectar huecos (V-001, V-003 sin V-002): renumerar para cerrar el hueco — el análisis sí puede renumerar internamente (a diferencia del spec, donde los huecos se conservan), porque el design todavía no ha consumido estos IDs en el caso normal. **Excepción:** si ya existe `design/` en la misma carpeta de iniciativa, **no** renumerar sin avisar — preguntar al usuario.
- Detectar IDs malformados (`V-001` sin nombre de entidad, `V-tareacorreo-001` en minúsculas, `U-todos-1` con un solo dígito): normalizar al formato canónico.

### 4.4 Trazabilidad EARS → V/R/U (núcleo de esta revisión)

Para cada V/R/U leída de los `entity-*.md` y `screen-*.md`:

- Si la celda "Origen EARS" está vacía sin `—`, normalizar a `—`.
- Si contiene IDs `E-XX-NNN`, comprobar que cada uno **existe** en la lista extraída del `specification.md`. Si hay un ID que no existe:
  - Si es un typo evidente (p.ej. `E-EV-01` vs `E-EV-001` real), corregir.
  - Si no, preguntar al usuario: ¿es un error y se debe vaciar (`—`)?, ¿se refiere a otro `E-XX-NNN` concreto?, ¿se debe añadir el requisito faltante al spec?

Para cada `E-XX-NNN` del `specification.md`:

- Buscar si aparece como Origen EARS en al menos una V/R/U **o** en la sección "EARS descartados" del `analysis.md`. Si no aparece en ninguno de los dos sitios, **preguntar al usuario**: ¿olvidaste mapear este requisito? ¿hay que descartarlo y, en tal caso, con qué justificación? Tras la respuesta, o se añade un Origen EARS en alguna V/R/U, o se añade una entrada en "EARS descartados" con el motivo dado por el usuario.

Cuando termines la cobertura, **actualizar los contadores** en la sección "Resumen de reglas" del `analysis.md`:

```
- Total validaciones: N (sin Origen EARS: n)
- Total reglas de negocio: M (sin Origen EARS: m)
- Total reglas de UI: K (sin Origen EARS: k)
```

### 4.5 Tests E2E (`tests.md`) y cobertura F → T

**Caso legacy: spec sin "Flujos principales".** Si el `specification.md` no tiene sección "Flujos principales" (o está vacía), entonces no debe existir `tests.md` ni sección "Flujos sin tests" en `analysis.md`. En su lugar, la sección "Tests E2E" del `analysis.md` debe contener exactamente la **nota de variante "B.3 saltada"** (ver §8.6 del skill original). Validar:

- Si falta la nota, añadirla (corrección mecánica).
- Si en `analysis.md` aparecen secciones "Tests E2E" con contadores reales o "Flujos sin tests" con contenido, son residuos de una iteración anterior: preguntar al usuario antes de borrarlos.
- **Si esta es la situación, omitir el resto de §4.5** y pasar a §4.6.

**Caso normal: spec con `F-NNN`.** Para cada test `## T-NNN` leído de `tests.md`:

- Tiene cabecera con `Origen F`, `Verifica`, `Pantalla principal` y `Tipo`. Si falta alguno, preguntar al usuario por el valor adecuado.
- `Origen F`: contiene al menos un ID `F-NNN`. Cada ID listado **debe existir** en la sección "Flujos principales" del `specification.md`. Si hay un ID inexistente, comprobar si es typo (corregir) o preguntar al usuario.
- `Verifica`: lista de IDs `V-…` / `R-…` / `U-…` o `—`. Cada ID debe existir en algún `entity-*.md` / `screen-*.md`. Referencias rotas se corrigen o se preguntan.
- `Pantalla principal`: referencia un fichero `screen-*.md` que existe en la carpeta.
- **Pasos** en lenguaje de negocio con `Dado` / `Cuando` / `Y` / `Entonces`. Si hay selectores CSS, refs `eN`, comandos `playwright-cli` o código embebido, avisar al usuario: el test ha invadido el territorio del implementer.
- **Nombres de pantalla, botón, campo y mensaje** mencionados en los pasos deben existir en el `screen-*.md` o `entity-*.md` correspondiente. Nombres inventados se reportan al usuario.
- **Numeración** `T-NNN` con tres dígitos, local al fichero, desde `001`, sin huecos. Mismas reglas de duplicados/huecos/malformados que en §4.3.
- **Independencia**: cada test prepara sus precondiciones; ningún test debe depender del estado dejado por otro. Si se detectan dependencias ("usa la TareaCorreo creada en T-001"), avisar.

**Cobertura F → T**: cada `F-NNN` del `specification.md` debe aparecer como `Origen F` en **al menos un test** o estar en "Flujos sin tests" del `analysis.md` con justificación. Si falta un flujo:

- Preguntar al usuario qué hacer: ¿añadir un test para él (no lo hace este skill: relanzar `/sdd-analyst-system` o pedir al usuario que lo redacte a mano)?, ¿descartarlo con justificación?
- Lo normal es **generar el test**, no descartar.

Tras la cobertura, **actualizar los contadores** en la sección "Tests E2E" del `analysis.md`:

```
- Total tests: T
- Flujos del spec cubiertos: F1 / F2
```

**Cobertura inversa V/R/U → T**: invertir la columna `Verifica` de todos los tests y, para cada V/R/U declarada en los `entity-*.md` y `screen-*.md`, comprobar que aparece en `Verifica` de al menos un `T-NNN`. Las V/R/U sin test deben estar listadas en la sección **"V/R/U sin tests"** del `analysis.md` con etiqueta de cobertura (`smoke manual` / `cubierta indirectamente por T-NNN` / `pendiente` / `aceptada sin verificar`). Si la sección no existe, añadirla. Para cada V/R/U sin test que no esté ya listada, **preguntar al usuario** qué etiqueta aplicar (default: `smoke manual` para U, `pendiente` para V y R) y añadirla a la tabla. Esto **no bloquea** la revisión: el objetivo es que la decisión de no testear sea explícita y justificada, no impedir el cierre.

### 4.6 Coherencia interna del análisis

- **No duplicación entre V/R/U:** ninguna regla debe aparecer en dos categorías (una validación clonada como `R-…`, por ejemplo). Si se detecta, preguntar a cuál pertenece (bloquea → V, actúa → R, cambia formulario → U) y eliminar de las otras.
- **Coherencia entidad ↔ pantalla:** cada campo mencionado en columnas de Grid, paneles de Formulario o reglas U de un `screen-*.md` debe existir en el `entity-*.md` correspondiente. Si no existe, preguntar si añadirlo a la entidad o quitarlo de la pantalla.
- **Referencias cruzadas en Acciones:** la columna `Reglas que dispara` de cada Acción debe referenciar IDs V/R que existen realmente. Los `Qué hace` de los Botones en formularios referencian IDs V/R/U existentes. Cualquier referencia rota se corrige (typo) o se pregunta.
- **Integridad referencial:** en Modelo de datos, las relaciones `→ <Entidad> (uno a varios, hijos)` (lado padre) son las que llevan la decisión de `CASCADE / RESTRICT / SET NULL` en su nota; el hijo no la lleva. Si está al revés, corregir.

### 4.7 Prohibiciones (ver §2.3 del skill original)

Mismo barrido de tecnicismos que en `sdd-specification-system-review`: clases Java, FQN, signaturas de método, tipos del framework, JPQL, atributos XML (`showIf`, `requiredIf`, `<action-attrs>`, `<action-record>`), nombres de método (`fireActionRule_*`, `validateInsert`). Reportar y, cuando sea trivial, eliminar.

### 4.8 Checklist completo

Aplicar el **checklist final §8.6 del skill original** entero al terminar las correcciones. Lo no resuelto se reporta al usuario.

---

## 5. Fase 3 — Informe al usuario

```
Revisión de analysis/ completada.

Ficheros revisados:
  - analysis.md
  - entity-<Nombre>.md (N)
  - screen-<nombre>.md (M)
  - tests.md (T tests)

Correcciones aplicadas mecánicamente (N):
  - <lista corta>

Decisiones tomadas tras pregunta al usuario (N):
  - <lista corta>

Cobertura EARS:
  - Total requisitos en specification.md: X
  - Mapeados a V/R/U: Y
  - En "EARS descartados" con justificación: Z
  - Sin mapeo y sin descarte (acción pendiente del usuario): W

Cobertura tests (F → T):
  - Total flujos principales en specification.md: F
  - Cubiertos por al menos un test: F1
  - En "Flujos sin tests" con justificación: F2
  - Sin cubrir y sin justificar (acción pendiente del usuario): F3

Puntos del checklist que siguen abiertos (N):
  - <lista corta>
```

Si nada hubo que tocar:

```
La carpeta analysis/ ya está conforme con el contrato actual. No se ha modificado nada.
```

---

## Apéndice A — Override de rutas (para testing)

Idéntico al Apéndice A del skill `sdd-analyst-system`:

- `--in=<ruta>` — fichero `analysis.md` o carpeta `analysis/` de entrada explícita.
- `--out=<ruta>` — carpeta de salida si se quiere revisar copiando en vez de editar en sitio.
- `--root=<ruta>` — raíz alternativa a `.sdd/drafts/`.

En uso normal no se especifican.
