# Parte del diseño: tests E2E

Como parte del diseño, **el diseñador** escribe `design_<n>/test-e2e-desc.md` a partir de los escenarios del spec.

**Cuándo se incluye:** solo si `specification.md` contiene al menos un escenario `ESC-NNN`. Si el spec no tiene escenarios, **no se crea** `test-e2e-desc.md` (y `/sdd-debug-with-test-e2e-desc` no tendrá tests que ejecutar).

**Quién más lo usa** (`README.md` §2): el **verificador** comprueba que `test-e2e-desc.md` existe cuando el spec tiene escenarios y que cubre cada `ESC-NNN` (`validacion.md` §2.i); el **corrector** solo consulta este fichero si un fallo reportado afecta a `test-e2e-desc.md`.

`test-e2e-desc.md` se materializa a partir de los escenarios `ESC-NNN` embebidos bajo cada historia de usuario `HU-NNN` de `specification.md`, usando el propio diseño (las V/R/U y su `Origen spec`) y los `screen-*.md` / `entity-*.md` del spec como referencia de nombres reales (pantallas, botones, campos, mensajes). Cada `ESC-NNN` se convierte en uno o más tests `T-NNN` Given/When/Then en lenguaje de negocio. **MUST**: cada `ESC-NNN` tiene al menos un test asociado. **MUST NOT** incluir comandos `playwright-cli` ni selectores CSS — la traducción la hace `/sdd-debug-with-test-e2e-desc` al ejecutarlos.

---

## 1. Reglas de materialización

- Numeración `T-NNN` de tres dígitos, global al fichero y sin huecos. **Arranca en el primer número libre de la carpeta de tests del sistema**, no siempre en `001`:
  - Carpeta vacía o inexistente (sistema nuevo) → `T-001`.
  - Carpeta con tests ya persistidos (el caso de una iniciativa que **modifica** un sistema existente) → el siguiente al mayor `NNN` que ya haya.

  **CRITICAL — es el único punto del pipeline donde se puede evitar el choque.** El `T-NNN` viaja intacto hasta el nombre del fichero persistido (`t-NNN-<slug>.desc.md` / `.spec.ts`), en una carpeta que **comparten varias iniciativas**: `/sdd-debug-with-test-e2e-desc` es un troceador que copia verbatim y `/sdd-create-tests-e2e` tiene prohibido renumerar (desincronizaría el nombre del fichero con el `id:` del frontmatter). La carpeta es `src/test/e2e/<capa>/<sistema>/`, con el `<capa>/<sistema>` del campo `**Capa:**` del `design.md`:

  ```bash
  ls src/test/e2e/<capa>/<sistema>/t-*.desc.md 2>/dev/null
  ```

  **MUST NOT** rellenarse un hueco dejado por un test retirado: el bloque de esta iniciativa arranca por encima de **todos** los existentes y es contiguo.

  - ✅ CORRECTO: la carpeta tiene `t-001…t-026`; la iniciativa describe 4 tests → `T-027`…`T-030`.
  - ❌ INCORRECTO: numerar `T-001`… sobre una carpeta poblada (chocaría con ficheros ya persistidos de otra iniciativa).
- Cada test declara en su cabecera: `Origen ESC` (lista de `ESC-NNN` que materializa, **mínimo 1**), `Verifica` (lista de `V-`/`R-`/`U-` que ejerce, o `—`), `Pantalla principal` (un `screen-*.md`) y `Tipo` (`happy` | `error` | `UI`).
- **Cobertura mínima obligatoria**: cada `ESC-NNN` del spec aparece como `Origen ESC` en **al menos un test**. Un escenario con ramas condicionales puede dar lugar a **más de un test** (uno por rama).
- Pasos en lenguaje de negocio con `Dado`/`Cuando`/`Y`/`Entonces` (o `Given`/`When`/`And`/`Then`), usando nombres reales de pantallas (entrecomillados), botones, campos y mensajes. **MUST NOT** selectores CSS ni comandos `playwright-cli`.
- Cada test es **autosuficiente e independiente**: empieza por el login del actor, prepara sus propios datos (el único estado previo admisible es el de la sección «Estado inicial de la base de datos» de este fichero — ver abajo), realiza la acción y verifica la respuesta — igual que exige el escenario del spec.
- **Estado inicial de la base de datos (precondición común).** `test-e2e-desc.md` **MUST** empezar con una sección `## Estado inicial de la base de datos` que **materialice** el estado previo que el spec describe en su apartado de recursos/datos iniciales (p.ej. "Recursos y datos iniciales"): los datos maestros que gestionan otros subsistemas y de los que parten **todos** los escenarios (centros, catálogo educativo, usuarios…). Es el **único** estado previo que un test puede presuponer; cada test lo referencia en sus `Precondiciones` en lugar de repetirlo.
- **Credenciales de acceso.** Esa sección **MUST** incluir una **tabla de usuarios** con el `login` y la `contraseña` de **cada** usuario/actor que algún test usa para iniciar sesión (más su rol/tipo y, si el spec es multi-centro, su centro). `/sdd-debug-with-test-e2e-desc` necesita esas credenciales para hacer login real contra la aplicación: un test cuyo actor inicia sesión sin figurar en la tabla es un fallo de cobertura. Si el spec no fija las credenciales, el diseñador define una convención coherente (logins derivados del nombre/rol del usuario; una contraseña común salvo que el spec diga otra cosa) y la documenta en esa misma sección.

- ✅ CORRECTO `Origen ESC`: `ESC-001`, `ESC-002, ESC-005`
- ❌ INCORRECTO: `ESC-1` (sin 3 dígitos), `Escenario 1` (sin prefijo), celda vacía en `Origen ESC` (mínimo 1 ID)

---

## 2. Plantilla de `test-e2e-desc.md`

El subagente devuelve un fichero con esta estructura exacta:

```markdown
# Tests E2E

Tests concretos end-to-end materializados a partir de los escenarios (`ESC-NNN`) de las historias de usuario del `specification.md` y de las V/R/U del diseño.

Cada test es **independiente** (no depende del estado dejado por otro) y **trazable** (declara qué `ESC-NNN` materializa y qué V/R/U verifica). `/sdd-debug-with-test-e2e-desc` lo ejecuta contra la aplicación real tras la implementación (bucle de auto-corrección).

---

## Estado inicial de la base de datos

Estado previo (datos maestros gestionados por otros subsistemas) del que parten **todos** los tests. Ningún test puede presuponer más estado que este; cada test lo referencia en sus `Precondiciones`.

- <Dato maestro 1 materializado del spec, p.ej. centros «…» y «…»>
- <Dato maestro 2, p.ej. cursos y módulos del catálogo educativo>
- …

**Usuarios de acceso** (login y contraseña que `/sdd-debug-with-test-e2e-desc` usará para iniciar sesión):

| Login | Contraseña | Rol / Tipo | Centro |
|---|---|---|---|
| <login> | <contraseña> | <rol/tipo de usuario> | <centro o —> |
| … | … | … | … |

---

## T-001 — <Nombre corto descriptivo del escenario>

**Origen ESC:** ESC-001
**Verifica:** V-SolicitudCertificado-005, U-mis-solicitudes-002
**Pantalla principal:** screen-mis-solicitudes.md
**Tipo:** happy | error | UI

### Precondiciones
- El usuario `<rol>` ha iniciado sesión.
- (Si aplica) Existe una `<Entidad>` "X1" en estado `<ESTADO>` con `<campo>` = "<valor>".

### Pasos
1. **Dado** que el usuario está en la pantalla "Mis solicitudes".
2. **Cuando** abre el detalle de "X1".
3. **Y** pulsa el botón "<Botón tal cual aparece en screen-*.md>".
4. **Y** deja el campo "<Campo tal cual aparece en screen-*.md>" vacío.
5. **Y** pulsa "Confirmar".

### Resultado esperado
- El sistema muestra el mensaje "<Mensaje exacto definido en la VAL-/RES- del spec>".
- "X1" sigue en estado `<ESTADO>` (no se ha modificado).

---

## T-002 — <Otro escenario>

**Origen ESC:** ESC-002, ESC-003
**Verifica:** —
**Pantalla principal:** screen-solicitudes-centro.md
**Tipo:** happy

### Precondiciones
- (vacío si no se asume nada más allá del "Estado inicial de la base de datos")

### Pasos
1. **Dado** …
2. **Cuando** …
3. **Entonces** …

### Resultado esperado
- …
```

---

## 3. Checklist de los tests

- [ ] ¿`test-e2e-desc.md` empieza con la sección "Estado inicial de la base de datos" que materializa el estado previo (datos maestros) del spec?
- [ ] ¿Esa sección incluye una tabla de usuarios con `login` y `contraseña` para **cada** actor que inicia sesión en algún test?
- [ ] ¿Ningún test presupone más estado previo que el descrito en esa sección (cada test lo referencia en `Precondiciones`)?
- [ ] ¿Cada `ESC-NNN` del spec aparece como `Origen ESC` en al menos un test?
- [ ] ¿Cada test tiene `Origen ESC` (mínimo 1), `Verifica` (o `—`), `Pantalla principal` y `Tipo`?
- [ ] ¿Cada `Pantalla principal` referencia un `screen-*.md` que existe?
- [ ] ¿Cada `V-`/`R-`/`U-` de `Verifica` existe en el diseño?
- [ ] ¿Cada campo, botón o mensaje de los pasos existe en el `screen-*.md` / `entity-*.md` correspondiente (no inventado)?
- [ ] ¿Los pasos están en `Dado`/`Cuando`/`Y`/`Entonces`, sin selectores CSS ni comandos `playwright-cli`?
- [ ] ¿Cada test es independiente y prepara sus propias precondiciones desde el login (sin presuponer estado salvo "Recursos y datos iniciales")?
- [ ] ¿La numeración `T-NNN` es global al fichero, sin huecos, y empieza en el **primer libre de `src/test/e2e/<capa>/<sistema>/`** —`001` solo si esa carpeta no tiene ningún `t-*.desc.md` (§1)?
