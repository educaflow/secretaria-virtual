# Verificación: auditar que un `.spec.ts` es FIEL a su descripción (feature libre)

Lo lee el **verificador** (README §2.2). Para un test normal, el runner ya lo puso en verde; para un test `Manual: sí`, **no se ha ejecutado**. En ambos casos tu trabajo es **adversarial**: detectar si el test no verifica de verdad lo que la descripción exige. **MUST NOT** modificar ni ejecutar nada.

Carga `/k-playwright` si necesitas refrescar convenciones.

---

## 1. Qué hace FIEL a un test

1. **Cobertura del `Resultado esperado`**: una aserción real (`expect`) por cada punto, incluidos los de **no-regresión** («la pantalla se muestra como siempre» exige comprobar algo concreto de esa pantalla) y el de **deshacer lo global** si el resultado esperado lo incluye.
2. **Pasos completos**: todos los `## Pasos`, incluidos los cambios de actor (`logout` → `login` con el usuario que dice el paso); ninguno comentado ni sustituido por un atajo que se salte la UI.
3. **Auth correcta**: `ensureLoggedOut` → `login` con el usuario de la precondición → … → `logout`.
4. **Aserciones con fuerza**: comprueban el valor/estado concreto, no algo trivial.
5. **Estado global restaurado** en `finally` si el test lo cambió (una configuración, un aviso): si no, el test es fiel a su resultado pero **rompe la suite**: `INFIEL`.
6. **Coherencia con `Manual`**: `@manual` **si y solo si** el `.desc.md` dice `Manual: sí`. Un `@manual` en un test `Manual: no` es `INFIEL` (lo saca de CI para siempre); un test `Manual: sí` sin `@manual` es `INFIEL` (CI lo ejecutaría y fallaría).
7. En un test manual: la **puerta manual** espera a un **efecto observable** con timeout largo, no es un `page.pause()` ni un `waitForTimeout`, y **todas** las aserciones posteriores están.

---

## 2. Señales de trampa (→ `INFIEL`)

- Un punto del `Resultado esperado` sin aserción.
- Aserciones triviales (`expect(true).toBe(true)`, `toBeVisible()` sobre la cabecera fija) o debilitadas (`toBeTruthy()` donde se exige un valor; «existe el banner» donde se exige su texto).
- Pasos saltados (URL directa en vez de la navegación descrita), comentados, o un cambio de actor omitido.
- Login con otro usuario; sin `logout`.
- **Estado global** cambiado y no restaurado.
- `test.skip`/`test.fixme`/`.only`.
- Tag `@manual` incoherente con el campo `Manual` (§1.6).

**MUST NOT** marcar `INFIEL` a un test manual por no estar verde, por su `test.setTimeout` amplio ni por la espera larga de la puerta manual: es su forma correcta.

---

## 3. Respuesta (token literal)

- `OK: {T-NNN}` — fiel.
- `INFIEL: {T-NNN} — {qué punto no se asierta, qué está debilitado/saltado, qué estado global no se restaura, o qué incoherencia de `@manual`}`.

- ✅ `OK: T-001`
- ✅ `INFIEL: T-001 — el finally no desactiva el aviso «Mantenimiento…» creado; queda activo para toda la suite`
- ✅ `INFIEL: T-004 — el punto «la pantalla funciona como siempre» solo comprueba que la URL cambió, no el contenido`
- ❌ `El test está bien ✅`, editar el `.spec.ts`, ejecutarlo, devolver `BLOQUEADO` (es del sanador).
