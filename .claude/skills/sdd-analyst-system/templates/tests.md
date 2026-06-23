# Tests E2E

Tests concretos de prueba end-to-end materializados a partir de los escenarios (`ESC-NNN`) del `specification.md` y de las V/R/U inferidas en `entity-*.md` / `screen-*.md`.

Cada test es **independiente** (no depende del estado dejado por otro test) y **trazable** (cada uno declara qué `ESC-NNN` materializa y qué V/R/U verifica).

`/sdd-test-e2e` lee este fichero una vez implementado el código: trocea los tests en una carpeta `test-e2e/`, traduce cada escenario a comandos `playwright-cli` y ejecuta un **bucle de auto-corrección**: si un test falla, corrige el código y reintenta.

---

## T-001 — <Nombre corto descriptivo del escenario>

**Origen ESC:** ESC-001
**Verifica:** V-TareaCorreo-001, U-mis-correos-002
**Pantalla principal:** screen-mis-correos.md
**Tipo:** happy | error | UI

### Precondiciones
- El usuario `<rol>` ha iniciado sesión.
- Existe una `<Entidad>` "X1" en estado `<ESTADO>` con `<campo>` = "<valor>".
- (Si aplica) Existen N filas de `<Entidad>` adicionales para que el listado no esté vacío.

### Pasos
1. **Dado** que el usuario está en la pantalla "Mis correos".
2. **Cuando** abre el detalle de "X1".
3. **Y** pulsa el botón "<Botón tal cual aparece en screen-*.md>".
4. **Y** deja el campo "<Campo tal cual aparece en screen-*.md>" vacío.
5. **Y** pulsa "Confirmar".

### Resultado esperado
- El sistema muestra el mensaje "<Mensaje exacto definido en V-…-NNN del entity-*.md>".
- "X1" sigue en estado `<ESTADO>` (no se ha modificado).
- (Si aplica) La fila correspondiente del listado mantiene `<campo>` = "<valor>".

---

## T-002 — <Otro escenario>

**Origen ESC:** ESC-001, ESC-003
**Verifica:** —
**Pantalla principal:** screen-todos.md
**Tipo:** happy

### Precondiciones
- (vacío si no se asume nada)

### Pasos
1. **Dado** …
2. **Cuando** …
3. **Entonces** …

### Resultado esperado
- …

---

## Reglas de redacción

- **Nombres**: pantallas, botones, campos y mensajes se citan **exactamente** como aparecen en `screen-*.md` / `entity-*.md`. Si una validación tiene mensaje `"El motivo es obligatorio"`, ese es el texto que va en el resultado esperado.
- **Lenguaje**: usar `Dado` / `Cuando` / `Y` / `Entonces` (o `Given` / `When` / `And` / `Then`). No usar selectores CSS, refs `eN`, ni comandos `playwright-cli`. La traducción a comandos la hace `/sdd-test-e2e`.
- **Atomicidad**: cada test cubre **un** escenario completo. No mezclar varios casos en el mismo test (un happy y un error en el mismo `T-NNN` rompe el diagnóstico cuando falla).
- **Independencia**: cada test prepara sus propias precondiciones desde un estado conocido. **No** asumir datos creados por un test anterior.
- **Cobertura mínima**: cada `ESC-NNN` del spec aparece como `Origen ESC` en al menos un test. Los tests adicionales para V/R/U críticas son opcionales (criterio del analista).
- **Trazabilidad**: si un test no verifica ninguna V/R/U concreta (happy path puro), poner `Verifica: —`. No inventar IDs.
- **Numeración**: `T-001`, `T-002`… global al fichero, sin huecos.
