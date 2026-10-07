# Sanación: arreglar un `.spec.ts` que falla (feature libre)

Lo lee el **sanador** (README §2.3). Tarea: dado un `.spec.ts` rojo o declarado `INFIEL`, **arreglar el `.spec.ts`** (o `_support/auth.ts`) sin tocar el código de la app.

Carga `/k-playwright` si lo necesitas. **MUST** cerrar la sesión con `browser_close` al terminar.

---

## 1. Premisa de origen del fallo

La descripción ya pasó al depurarse con `/sdd-debug-with-test-e2e-desc`: el comportamiento esperado es **correcto**. Distingue el **origen**:

1. **Fallo del `.spec.ts`** → **sanable**. Causas típicas:
   - **No idempotente** (causa #1): nombres fijos, nada liberado, o **estado global no restaurado** por este test o por uno anterior (un aviso/configuración que quedó activo). Arréglalo con `generation.md` §4 (nombre único, `try/finally`, restaurar lo global, pre-limpieza). Verifica 2 veces seguidas en verde.
   - **Locator** desactualizado o **ambiguo** (los elementos del marco común se repiten en todas las pantallas: acota al contenedor).
   - **Timing**: `await expect(...).toBeVisible()`, nunca `waitForTimeout`. Para un efecto asíncrono (cron, «al cambiar de pantalla») navega a otra pantalla y vuelve o `page.reload()`; **nunca** `goto` a la misma URL con hash.
   - **Equivalencia semántica / idioma**: regex con las variantes es/ca, sin debilitar la aserción.
   - **Cambio de actor** mal hecho: falta `logout` antes del segundo `login`.
   - **Auth**: selectores del helper que no casan → ajustar el helper.
   - **Import** de `_support/auth` distinto de `'../../_support/auth'` (README §3.2).
2. **Fallo de la app** (la UI no hace lo que la descripción depurada espera) → **NO sanable**: regresión. `BLOQUEADO` con el detalle. **MUST NOT** ocultarla debilitando aserciones.
3. **Test `Manual: sí`**: solo llegas aquí por `INFIEL` (el motor no lo ejecuta). Arregla lo que el verificador señale (tag, puerta manual, aserción que falta). **MUST NOT** quitarle el `@manual` ni «hacerlo pasar» ejecutándolo.

---

## 2. Qué MUST NOT hacer

- Modificar `src/main/...` ni `.sdd/`.
- Borrar ni relajar aserciones del `Resultado esperado`.
- Añadir `waitForTimeout`, `test.skip`, `test.fixme`.
- Poner o quitar `@manual` en contra del campo `Manual` del `.desc.md`.

---

## 3. Cómo diagnosticar

1. Lee la salida de `npx playwright test` (error, locator, línea) o el motivo `INFIEL`, y el extracto del log de la app.
2. Lee el `.spec.ts` y su `.desc.md`.
3. Si hace falta ver la UI real: `browser_snapshot`, `browser_generate_locator`, `browser_console_messages`, `browser_network_requests`.
4. Si sospechas estado global sucio de un run anterior, compruébalo en la UI y añade la **pre-limpieza** al test (no limpies la BD a mano: el test debe curarse solo).
5. Aplica el arreglo **mínimo**.

---

## 4. Respuesta (token literal)

- `CORREGIDO: {T-NNN}` — ajustaste el `.spec.ts` (o el helper) y debería pasar / ser fiel.
- `BLOQUEADO: {T-NNN} — {motivo}` — el fallo no es del `.spec.ts`: posible regresión de la app o recurso del entorno. Repórtalo con precisión (qué esperaba la descripción vs qué hace la app).

- ✅ `CORREGIDO: T-001` — el `getByText` del banner tenía dos matches (cabecera y pantalla); acotado al contenedor del aviso.
- ✅ `BLOQUEADO: T-003 — con el aviso desactivado el banner sigue mostrándose; posible regresión de la app`
- ❌ `Arreglado ✅`, borrar una aserción, editar un servicio Java, quitar `@manual`.
