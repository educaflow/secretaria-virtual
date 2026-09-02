# Descomposición: de `test-e2e-desc.md` a la carpeta `test-e2e-desc/`

Lo lee el **descomponedor** (§3.1 del `README.md`). Tarea: trocear el `test-e2e-desc.md` de un trámite (un fichero con muchos tests) en **un fichero autocontenido por test** + un **índice con checkbox por test**.

---

## 1. Qué leer y qué escribir

1. Lee el `test-e2e-desc.md` íntegro. Identifica:
   - La **cabecera común**: las secciones `## Actores` (tabla de credenciales) y `## Datos de demo` (estado previo + un juego de datos válido por fase). Guárdalas **verbatim**.
   - La tabla `## Cobertura de transiciones`: es una tabla de **control del diseño**. **MUST NOT** copiarse a ningún fichero de test; solo se usa en el checklist (§4) para comprobar que todo `T-NNN` que referencia existe como bloque.
   - Los **bloques de test**: cada `## T-NNN — <nombre>` hasta el siguiente `## T-` (o el final), con sus **siete campos de cabecera** (`Origen ESC`, `Perfil`, `Desde`, `Evento`, `Hasta`, `Tipo`, `Manual`) y sus bullets `Given`/`When`/`Then`/`And` (o `Dado`/`Cuando`/`Entonces`/`Y`). Guárdalos **verbatim**.
     El campo `Manual` (`no` | `sí — <motivo>`) es además el que decide el estado inicial de ese test en el índice (§3).
2. Escribe en `{iniciativa}/test-e2e-desc/`:
   - un `t-NNN-<slug>.desc.md` por test (§2),
   - el índice `tests-e2e-desc.md` (§3).

**MUST NOT** reescribir, resumir, renumerar ni "mejorar" el contenido de los tests: se copia tal cual del `test-e2e-desc.md`. Eres un troceador, no un autor.

**Única excepción — los encabezados envoltorio.** El fichero de test usa los encabezados fijos de la plantilla de §2 (`## Estado inicial de la base de datos` con sus `### Actores` / `### Datos de demo`, y `## Pasos`). Como esas dos secciones pasan a colgar de `## Estado inicial de la base de datos`, **todo su árbol de encabezados baja un nivel**: `##` → `###`, `###` → `####`, y así sucesivamente. Es el **único** cambio permitido; el contenido (tablas, listas, valores) va **verbatim**.

**CRITICAL** — bajar solo los dos encabezados de cabecera y dejar sus hijos donde estaban los deja **colgando**: el `### Juego de datos válido — fase <FASE>` que el diseño mete **dentro** de «Datos de demo» quedaría como **hermano** de `### Datos de demo`, y con varias fases se leería como una sección independiente del estado inicial.

- ✅ CORRECTO: `## Actores` del origen → `### Actores` dentro de `## Estado inicial de la base de datos`, con la tabla intacta.
- ✅ CORRECTO: `## Datos de demo` → `### Datos de demo`, y **sus** `### Juego de datos válido — fase <FASE>` → `#### Juego de datos válido — fase <FASE>`.
- ❌ INCORRECTO: bajar `## Datos de demo` a `### Datos de demo` y dejar sus subsecciones en `###` (quedan como hermanas suyas, no dentro).
- ❌ INCORRECTO: resumir la tabla de actores a las filas del actor de este test (el ejecutor puede necesitar otro login para una precondición), reordenar sus columnas, o traducir los valores.

---

## 2. Plantilla de `t-NNN-<slug>.desc.md` (un fichero por test)

**CRITICAL — autocontenido**: el ejecutor recibe **solo este fichero**, así que **MUST** llevar la cabecera común (actores + datos de demo) **además** del bloque del test. Sin las credenciales no puede hacer login; sin el juego de datos no sabe qué teclear.

**Nombre del fichero** `t-NNN-<slug>.desc.md` (este nombre base es la fuente única de la trazabilidad; `/sdd-create-tests-e2e` lo copia tal cual, no lo recalcula):

- `t-NNN` = el `T-NNN` del test en **minúsculas**, conservando los tres dígitos: `T-001 → t-001`, `T-012 → t-012`.
- `<slug>` = el `<nombre>` del test en **kebab-case**: minúsculas, acentos y signos eliminados (`á→a`, `ñ→n`, sin `«»".,()`), espacios y separadores colapsados a un único `-`.
- Extensión **MUST** ser `.desc.md`.

El fichero tiene **exactamente** esta estructura:

```markdown
---
type: test-e2e
id: T-NNN
---

# T-NNN — <nombre del test, tal cual en test-e2e-desc.md>

**Origen ESC:** <verbatim>
**Perfil:** <verbatim: `<PERFIL>` (login <login>)>
**Desde:** <verbatim: `<FASE>` / `<ESTADO>`, o `[*]`>
**Evento:** <verbatim: `<EVENTO>` — botón «<título>», o `—`>
**Hasta:** <verbatim: `<FASE>` / `<ESTADO>`, o `[*]`>
**Tipo:** <happy | error | solo-lectura>
**Manual:** <verbatim: `no`, o `sí — <motivo>`>

## Estado inicial de la base de datos

### Actores

<la sección "## Actores" COMPLETA y VERBATIM del test-e2e-desc.md, con su tabla de logins y contraseñas>

### Datos de demo

<la sección "## Datos de demo" COMPLETA y VERBATIM, incluidos TODOS los juegos de datos por fase,
 cada uno como #### (sus encabezados bajan un nivel con la sección, §1)>

## Pasos

<los bullets Given / When / Then / And del bloque del test, VERBATIM y en su orden>
```

- ✅ CORRECTO: `t-004-el-responsable-devuelve-el-expediente-al-creador.desc.md` con `id: T-004`, los siete campos de cabecera, la cabecera común completa y los bullets verbatim.
- ❌ INCORRECTO: omitir la sección `## Estado inicial de la base de datos` (el ejecutor no podría hacer login → fallo de cobertura).
- ❌ INCORRECTO: omitir alguno de los **siete** campos de cabecera porque "no aplica" — un campo que no aplica ya viene como `—` en el origen y se copia así.
- ❌ INCORRECTO: omitir la línea `**Manual:**` o "deducirla" — se copia verbatim del origen. Si el bloque del origen **no la trae** (diseño anterior a este contrato), escríbela como `**Manual:** no` y **anótalo en la respuesta**: no inventes un `sí`.
- ❌ INCORRECTO: `t-4-...` (id sin tres dígitos), `t-004-El-Responsable.desc.md` (slug sin kebab-case), `t-004.desc.md` (sin slug), separar los `Then`/`And` en una sección propia inventada, fusionar dos tests en un fichero.

---

## 3. Plantilla del índice `tests-e2e-desc.md`

Una línea por test, en orden. El estado inicial de cada línea lo decide **el campo `Manual`** de su cabecera:

| Estado | Cuándo lo escribe el descomponedor | Qué significa |
|---|---|---|
| `- [ ]` | `Manual: no` | pendiente de ejecutar; es lo que recorre el motor |
| `- [-]` | `Manual: sí — <motivo>` | **no automatizable**: requiere una persona. El motor lo **salta** por defecto y lo reporta aparte |

El motor marca `- [x]` cuando el test pasa (es el progreso reanudable). **MUST NOT** escribir ningún `- [x]` al crear el índice.

```markdown
---
type: test-e2e-index
---

# Tests E2E — <nombre visible del trámite>

Índice de los tests E2E de esta iniciativa. Cada test vive en su propio fichero autocontenido. El checkbox se marca `[x]` cuando el test pasa contra la aplicación real (lo gestiona `/sdd-debug-with-test-e2e-desc`).

Estados: `[ ]` pendiente · `[x]` pasado · `[-]` no automatizable (requiere atención manual; se salta por defecto).

- [ ] [T-001 — <nombre>](t-001-<slug>.desc.md)
- [ ] [T-002 — <nombre>](t-002-<slug>.desc.md)
- [-] [T-003 — <nombre>](t-003-<slug>.desc.md) — manual: <motivo, en una línea>
```

- ✅ CORRECTO: `- [ ] [T-001 — Alta del expediente desde el árbol de trámites](t-001-alta-del-expediente-desde-el-arbol-de-tramites.desc.md)`
- ✅ CORRECTO: `- [-] [T-007 — El interesado firma la solicitud](t-007-el-interesado-firma-la-solicitud.desc.md) — manual: la firma abre AutoFirma__!! en la máquina del interesado`
- ❌ INCORRECTO: `- [x] [T-001 …]` (no se marca al crear), `- [T-001 …]` (sin checkbox), un destino que no coincide con el fichero creado.
- ❌ INCORRECTO: dejar en `- [ ]` un test con `Manual: sí` (atascaría al ejecutor en un paso imposible), o poner `- [-]` a un test con `Manual: no` (se quedaría sin depurar en silencio).

---

## 4. Checklist del descomponedor

**MUST NOT** terminar si queda algún punto sin cumplir. **LIMIT**: 3 iteraciones de autocorrección.

- [ ] ¿Se leyó el `test-e2e-desc.md` íntegro y se localizaron `## Actores`, `## Datos de demo` y todos los bloques `## T-NNN`?
- [ ] ¿Hay **exactamente un** `t-NNN-<slug>.desc.md` por cada `## T-NNN` del origen, con `t-NNN` de tres dígitos y `<slug>` en kebab-case?
- [ ] ¿Cada fichero tiene frontmatter `type: test-e2e` + `id: T-NNN`?
- [ ] ¿Cada fichero lleva los **siete** campos de cabecera, en orden y verbatim (los que no aplican, con `—`), incluida la línea `**Manual:**`?
- [ ] ¿Cada fichero incluye `## Estado inicial de la base de datos` con `### Actores` y `### Datos de demo` completos y verbatim?
- [ ] ¿**Todo el árbol** de esas dos secciones bajó un nivel — en particular, cada `### Juego de datos válido — fase <FASE>` quedó como `####` **dentro** de `### Datos de demo`, y no como hermano suyo (§1)?
- [ ] ¿Los bullets `Given`/`When`/`Then`/`And` se copiaron verbatim y en su orden, sin reescribir?
- [ ] ¿**NO** se copió la tabla `## Cobertura de transiciones` a ningún fichero de test?
- [ ] ¿Todo `T-NNN` referenciado en la columna `test` de esa tabla existe como bloque y tiene su fichero? (si alguno no existe, anótalo en la respuesta: es un fallo del diseño, **no** lo inventes)
- [ ] ¿Existe `tests-e2e-desc.md` con `type: test-e2e-index` y una línea por test, en orden, con su destino correcto y **ninguna** marcada `[x]`?
- [ ] ¿El estado de cada línea cuadra con el campo `Manual` de su fichero: `- [-]` (con su motivo en la línea) para los `Manual: sí`, `- [ ]` para los `Manual: no`?
- [ ] ¿La respuesta lleva `ESCRITO: test-e2e-desc/` + el bloque `=== TESTS ===` con una línea por test?
