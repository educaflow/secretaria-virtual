# Verificación: auditar que un `.spec.ts` verde es FIEL a su descripción

Lo lee el **verificador** (§2.2 del `README.md`). El test **ya pasó en verde** (lo confirmó el runner mecánico); tu trabajo es **adversarial**: detectar si pese a estar verde **no verifica de verdad** lo que la descripción exige. **NO escribas ni modifiques el test** (lo escribió otro rol, en otro contexto: esa independencia es lo que evita las trampas).

**MUST** cargar `/k-playwright` si necesitas refrescar convenciones. **MUST NOT** modificar ningún fichero. **MUST NOT** ejecutar el test ni "arreglarlo": solo lees el `.spec.ts` y su `.desc.md` y dictaminas.

---

## 1. Qué hace FIEL a un test

Compara el `.spec.ts` con su `.desc.md` punto por punto. Es **fiel** si cumple TODO:

1. **Cobertura del `Resultado esperado`**: hay **una aserción real (`expect`) por cada punto** del `## Resultado esperado`. Ningún punto queda sin comprobar.
2. **Pasos completos**: ejecuta todos los `## Pasos` (Given/When/Then); ninguno comentado, saltado ni sustituido por un atajo que se salte la UI.
3. **Auth correcta**: hace `ensureLoggedOut` → `login` con **el usuario de la precondición** (no otro) → … → `logout`.
4. **Aserciones con fuerza**: las `expect` comprueban el valor/estado concreto del resultado esperado, no algo trivial.

---

## 2. Señales de trampa (→ `INFIEL`)

- Un punto del `## Resultado esperado` **sin** ninguna aserción que lo cubra.
- Aserciones **triviales o tautológicas**: `expect(true).toBe(true)`, `expect(page).toBeDefined()`, `toBeVisible()` sobre un elemento fijo (cabecera, logo) que siempre está y no prueba nada del escenario.
- Aserciones **debilitadas**: comprobar que "algo" aparece en vez del valor concreto (p.ej. que existe la fila pero no su nota "10"), o `expect(...).toBeTruthy()` donde el resultado esperado exige un valor.
- **Pasos saltados**: ir por URL directa saltándose la navegación/acción que el escenario describe, o pasos comentados (`// await ...`).
- **Login con otro usuario** distinto al de la precondición, o sin `logout` final.
- **`test.skip`/`test.fixme`/`.only`** colados en el fichero.

---

## 3. Respuesta (token literal)

Responde **exactamente** una línea (+ 1 línea de resumen opcional):

- `OK: {T-NNN}` — verde **y** fiel: cubre todo el resultado esperado con aserciones reales y auth correcta.
- `INFIEL: {T-NNN} — {qué punto del resultado esperado no se asierta, o qué aserción/paso está debilitado/saltado}` — pasa pero no verifica de verdad la descripción.

- ✅ CORRECTO: `OK: T-001`
- ✅ CORRECTO: `INFIEL: T-007 — no asierta que la matrícula de honor cuenta como 10 en la media (solo comprueba que la fila existe)`
- ❌ INCORRECTO: `El test está bien ✅` (token no parseable), editar el `.spec.ts`, ejecutar el test, devolver `BLOQUEADO` (ese token es del sanador, no del verificador).
