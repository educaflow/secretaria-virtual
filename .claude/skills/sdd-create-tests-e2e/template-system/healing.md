# Sanación: arreglar un `.spec.ts` que falla

Lo lee el **sanador** (§2.2 del `README.md`). Tarea: dado un `.spec.ts` que falla al ejecutarse, **arreglar el `.spec.ts`** (o el helper `_support/auth.ts`) para que pase, **sin tocar el código Java**.

**MUST** cargar `/k-playwright` si necesitas refrescar convenciones de locators.

---

## 1. Premisa de origen del fallo

La descripción ya pasó al depurarse con `/sdd-debug-with-test-e2e-desc`: el comportamiento esperado es **correcto**. Por tanto, ante un fallo, distingue el **origen**:

1. **Fallo del `.spec.ts`** (lo habitual) → **sanable**. Causas típicas:
   - **Locator** desactualizado o ambiguo (texto/rol que no casa, varios matches).
   - **Timing**: se asierta antes de que la UI actualice → usar `await expect(...).toBeVisible()` (espera por defecto), **nunca** `waitForTimeout`.
   - **Equivalencia semántica de textos**: el mensaje real dice lo mismo con otras palabras / otro idioma (es/ca) → ajustar el locator (regex con ambas variantes), **sin** debilitar la aserción.
   - **Auth**: selectores de login/logout del helper `_support/auth.ts` que no casan con la UI real → ajustar el helper (es test code).
2. **Fallo de la app** (la UI no hace lo que la descripción ya depurada espera) → **NO sanable** aquí: es una **regresión**. Devuelve `BLOQUEADO` con el detalle. **MUST NOT** ocultarla debilitando o borrando aserciones.

---

## 2. Qué MUST NOT hacer

- **MUST NOT** modificar código Java (`src/main/...`) ni la fuente en `.sdd/`.
- **MUST NOT** borrar ni relajar aserciones del `## Resultado esperado` para que el test "pase" en falso.
- **MUST NOT** añadir `waitForTimeout` como parche de timing.

---

## 3. Cómo diagnosticar

1. Lee la **salida de `npx playwright test`** (error, locator que falló, línea) y el extracto del log de la app que te pase el motor.
2. Lee el `.spec.ts` y su `.desc.md` (autocontenido) para confirmar qué debería pasar.
3. Si necesitas ver la UI real, usa las tools MCP de Playwright (`browser_snapshot`, `browser_generate_locator`, `browser_console_messages`, `browser_network_requests`) contra la app levantada.
4. Aplica el arreglo **mínimo** (locator, espera correcta, regex es/ca, o el helper de auth).

---

## 4. Respuesta (token literal)

Responde **exactamente** una de estas líneas (+ 1 línea de resumen opcional):

- `CORREGIDO: {T-NNN}` — ajustaste el `.spec.ts` (o `_support/auth.ts`) y debería pasar.
- `BLOQUEADO: {T-NNN} — {motivo}` — el fallo no es del `.spec.ts`: posible regresión de la app o recurso del entorno; requiere decisión del usuario.

- ✅ CORRECTO: `CORREGIDO: T-005` — el botón era "Quitar", no "Eliminar"; ajustado el locator por rol.
- ✅ CORRECTO: `BLOQUEADO: T-014 — la pantalla de notas del alumno no carga (500 en /ws/rest/...); posible regresión de la app`
- ❌ INCORRECTO: `Arreglado ✅` (token no parseable), borrar una aserción del resultado esperado, editar un servicio Java.
