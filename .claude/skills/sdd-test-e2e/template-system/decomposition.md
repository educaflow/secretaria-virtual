# Descomposición: de `test-e2e-desc.md` a la carpeta `test-e2e/`

Lo lee el **descomponedor** (§2.1 del `README.md`). Tarea: trocear el `test-e2e-desc.md` (un fichero con muchos tests) en **un fichero autocontenido por test** + un **índice con checkbox por test**.

---

## 1. Qué leer y qué escribir

1. Lee el `test-e2e-desc.md` íntegro. Identifica:
   - La **cabecera común**: todo lo anterior al primer `## T-`, en particular la sección `## Estado inicial de la base de datos` con la lista de datos maestros y la **tabla de credenciales de login** (`| Login | Contraseña | Rol | Centro |`). Guárdala **verbatim**.
   - Los **bloques de test**: cada `## T-NNN — <nombre>` hasta el siguiente `## T-` (o el final), con sus `Origen ESC`/`Verifica`/`Pantalla principal`/`Tipo` y sus secciones `Precondiciones`/`Pasos`/`Resultado esperado`. Guárdalos **verbatim**.
2. Escribe en `{iniciativa}/test-e2e/`:
   - un `test-e2e-desc_NN.md` por test (§2),
   - el índice `tests-e2e-desc.md` (§3).

**MUST NOT** reescribir, resumir, renumerar ni "mejorar" el contenido de los tests: se copia tal cual del `test-e2e-desc.md`. Eres un troceador, no un autor.

---

## 2. Plantilla de `test-e2e-desc_NN.md` (un fichero por test)

**CRITICAL — autocontenido**: el ejecutor recibe **solo este fichero**, así que **MUST** llevar la cabecera común (estado inicial + credenciales) **además** del bloque del test. Sin las credenciales el ejecutor no puede hacer login.

- `NN` es el número del test con **dos dígitos** alineado con su `T-NNN`: `T-001 → test-e2e-desc_01.md`, `T-012 → test-e2e-desc_12.md`.

El fichero tiene **exactamente** esta estructura:

```markdown
---
type: test-e2e
id: T-NNN
---

# T-NNN — <nombre del test, tal cual en test-e2e-desc.md>

**Origen ESC:** <verbatim>
**Verifica:** <verbatim>
**Pantalla principal:** <verbatim>
**Tipo:** <happy | error | UI>

## Estado inicial de la base de datos

<la sección "Estado inicial de la base de datos" COMPLETA y VERBATIM del test-e2e-desc.md,
 incluida la lista de datos maestros y la tabla **Usuarios de acceso** con login y contraseña>

## Precondiciones

<verbatim del bloque del test>

## Pasos

<verbatim del bloque del test>

## Resultado esperado

<verbatim del bloque del test>
```

- ✅ CORRECTO: `test-e2e-desc_01.md` con `id: T-001`, su cabecera común completa y el bloque de T-001 verbatim.
- ❌ INCORRECTO: omitir la sección `## Estado inicial de la base de datos` (el ejecutor no podría hacer login → fallo de cobertura).
- ❌ INCORRECTO: `test-e2e-desc_1.md` (número sin dos dígitos), reescribir los pasos "con tus palabras", fusionar dos tests en un fichero.

---

## 3. Plantilla del índice `tests-e2e-desc.md`

Una línea por test, en orden, con un **checkbox sin marcar** `- [ ]`. El motor lo marca `- [x]` cuando el test pasa (es el progreso reanudable). **MUST** escribir todos sin marcar.

```markdown
---
type: test-e2e-index
---

# Tests E2E — <nombre de la iniciativa>

Índice de los tests E2E de esta iniciativa. Cada test vive en su propio fichero autocontenido. El checkbox se marca `[x]` cuando el test pasa contra la aplicación real (lo gestiona `/sdd-test-e2e`).

- [ ] [T-001 — <nombre>](test-e2e-desc_01.md)
- [ ] [T-002 — <nombre>](test-e2e-desc_02.md)
- [ ] [T-003 — <nombre>](test-e2e-desc_03.md)
```

- Un enlace por cada `test-e2e-desc_NN.md` creado, en orden, **precedido de `- [ ]`**, con el `T-NNN — <nombre>` como texto del enlace.
- ✅ CORRECTO: `- [ ] [T-001 — Crear un grupo](test-e2e-desc_01.md)`
- ❌ INCORRECTO: `- [x] [T-001 …]` (no se marca al crear), `- [T-001 …]` (sin checkbox), `- [ ] [T-1 …](test-e2e-desc_1.md)` (número sin dos dígitos / fichero que no coincide).

---

## 4. Checklist del descomponedor

**MUST NOT** terminar si queda algún punto sin cumplir. **LIMIT**: 3 iteraciones de autocorrección.

- [ ] ¿Se leyó el `test-e2e-desc.md` íntegro y se localizaron la cabecera común y todos los bloques `## T-NNN`?
- [ ] ¿Hay **exactamente un** `test-e2e-desc_NN.md` por cada `## T-NNN` del origen, con `NN` de dos dígitos alineado con el `T-NNN`?
- [ ] ¿Cada `test-e2e-desc_NN.md` tiene frontmatter `type: test-e2e` + `id: T-NNN`?
- [ ] ¿Cada `test-e2e-desc_NN.md` incluye la sección `## Estado inicial de la base de datos` completa y verbatim (con la tabla de credenciales)?
- [ ] ¿El bloque del test (cabeceras `Origen ESC`/`Verifica`/`Pantalla principal`/`Tipo` + `Precondiciones`/`Pasos`/`Resultado esperado`) se copió **verbatim**, sin reescribir?
- [ ] ¿Existe `tests-e2e-desc.md` con `type: test-e2e-index` y una línea `- [ ] [T-NNN — …](test-e2e-desc_NN.md)` por test, en orden, todas sin marcar?
- [ ] ¿La respuesta lleva `ESCRITO: test-e2e/` + el bloque `=== TESTS ===` con una línea por test?
