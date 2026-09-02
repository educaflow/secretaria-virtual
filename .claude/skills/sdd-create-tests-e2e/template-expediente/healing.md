# Sanación: arreglar un `.spec.ts` de expediente que falla

Lo lee el **sanador** (§2.3 del `README.md`). Tarea: dado un `.spec.ts` que falla al ejecutarse (o que el verificador declaró `INFIEL`), **arreglar el `.spec.ts`** (o el helper `_support/auth.ts`) para que pase, **sin tocar el código ni los XML del trámite**.

**MUST** cargar `/k-playwright` si necesitas refrescar convenciones de locators, y leer la §3.2 del `README.md` (la UI de un expediente).

---

## 1. Premisa de origen del fallo

La descripción ya pasó al depurarse con `/sdd-debug-with-test-e2e-desc`: el comportamiento esperado es **correcto**. Ante un fallo, distingue el **origen**:

1. **Fallo del `.spec.ts`** (lo habitual) → **sanable**. Causas típicas, por frecuencia:
   - **Bandeja equivocada** (causa #1 de "todo readonly y sin botones"): el test abre el expediente por una bandeja que no corresponde al perfil del tramo, así que ve la vista genérica de solo lectura. Arréglalo entrando por la bandeja correcta: `CREADOR` → «Expedientes Pendientes»; `RESPONSABLE` → «Expedientes Esperando» (abiertos) o «Expedientes Cerrados» (cerrados).
     Si el perfil del tramo es `SECRETARIO`, `DIRECTOR` o `AUDITOR` **no hay bandeja**: se entra por la pantalla que declare el trámite (`README.md` §3.2). Si no existe ninguna, es un fallo de diseño → `BLOQUEADO`, **MUST NOT** apañarlo entrando por otra bandeja.
   - **No idempotente** (causa #1 de RED al reejecutar; la BD es compartida): el test localiza el expediente con `.first()`, por el nombre del trámite o contando filas, y al segundo run actúa sobre uno ajeno. Arréglalo con el patrón de `generation.md` §5: **capturar el número de expediente** y localizarlo siempre por él.
   - **Profundidad del import** de `_support/auth`: «Cannot find module». La carpeta de un trámite tiene 3 o más niveles bajo `src/test/e2e/`; cuenta los segmentos reales (`generation.md` §7).
   - **Aserción sobre «Fase»/«Estado» mal hecha**: son campos readonly, su contenido es el `value` de un input. Usa `toHaveValue`, no `getByText`/`toBeVisible`.
   - **Locator** desactualizado o ambiguo: el **título** del botón del footer no coincide con el que dice el `.desc.md`, o hay varios matches.
   - **Timing**: tras un evento la vista se sustituye entera; se asierta antes de que se repinte. Usa `await expect(...)` (espera por defecto), **nunca** `waitForTimeout`. Recuerda que `EXIT`/`DELETE` **recargan la aplicación entera**.
   - **Equivalencia semántica de textos**: el mensaje real dice lo mismo con otras palabras u otro idioma (es/ca) → ajustar el locator (regex con ambas variantes), **sin** debilitar la aserción.
   - **Auth**: selectores de login/logout del helper `_support/auth.ts` que no casan con la UI real → ajustar el helper (es test code, compartido con toda la suite: cámbialo solo si de verdad está roto).
   - **`INFIEL` del verificador**: falta una aserción → **añádela** (típicamente la de no-transición en un `Tipo: error`, o la de fase/estado en un `happy`).
2. **Fallo de la app** (la UI no hace lo que la descripción ya depurada espera) → **NO sanable** aquí: es una **regresión**. Devuelve `BLOQUEADO` con el detalle. **MUST NOT** ocultarla debilitando o borrando aserciones.
   - Señal inequívoca: «No existe la vista en el expediente», un `500` al pulsar el botón del footer, o el expediente que transiciona a un estado distinto del declarado en `Hasta`.

---

## 2. Qué MUST NOT hacer

- **MUST NOT** modificar código Java/Kotlin (`src/main/...`), los XML del trámite (`TipoExpedienteInstance.xml`, `domains.xml`, `views.xml`, `documentospdf/`) ni la fuente en `.sdd/`.
- **MUST NOT** borrar ni relajar aserciones de los bullets `Then`/`And` para que el test "pase" en falso; en particular **MUST NOT** quitar la aserción de no-transición de un `Tipo: error`.
- **MUST NOT** sustituir un tramo de la UI por una llamada REST para "que sea más estable": el test dejaría de probar la máquina de estados.
- **MUST NOT** añadir `waitForTimeout` como parche de timing.
- **MUST NOT** reescribir `_support/auth.ts` para adaptarlo a este test: es compartido con toda la suite.
- **MUST** cerrar tu sesión de navegador (`browser_close`) al terminar.

---

## 3. Cómo diagnosticar

1. Lee la **salida de `npx playwright test`** (error, locator que falló, línea) **o** el motivo `INFIEL` del verificador, y el extracto del log de la app que te pase el motor.
2. Lee el `.spec.ts` y su `.desc.md` (autocontenido) para confirmar qué debería pasar: los siete campos de cabecera te dan el camino exacto.
3. Si necesitas ver la UI real, usa las tools MCP de Playwright (`browser_snapshot`, `browser_generate_locator`, `browser_console_messages`, `browser_network_requests`) contra la app levantada.
4. Aplica el arreglo **mínimo** (bandeja, localización por número, profundidad del import, locator, espera correcta, regex es/ca, o la aserción que falta).
5. Comprueba la idempotencia: **2 ejecuciones seguidas** en verde sin limpiar la BD.

**Si el test es `Manual: sí`** (`README.md` §3.4) solo puedes llegar aquí por un `INFIEL` del verificador, nunca por un rojo del runner: el motor **no lo ejecuta**. En ese caso, **MUST NOT** ejecutarlo tú (se quedaría colgado esperando a una persona) ni quitarle el tag `@manual`, su `test.setTimeout(...)` o su puerta manual para "poder probarlo": arregla lo que el verificador señale y responde `CORREGIDO`.

---

## 4. Respuesta (token literal)

Responde **exactamente** una de estas líneas (+ 1 línea de resumen opcional):

- `CORREGIDO: {T-NNN}` — ajustaste el `.spec.ts` (o `_support/auth.ts`) y debería pasar.
- `BLOQUEADO: {T-NNN} — {motivo}` — el fallo no es del `.spec.ts`: posible regresión de la app o falta un recurso del entorno. El motor lo registrará y saltará el test de forma autónoma; tu trabajo es **reportarlo con precisión** (qué esperaba la descripción vs. qué hace la app).
  **MUST NOT** devolver `BLOQUEADO` alegando que el paso no es automatizable: eso lo declara la cabecera (`Manual`) y se resuelve con el tag `@manual`, no aquí.

- ✅ CORRECTO: `CORREGIDO: T-005` — abría el expediente por «Expedientes Esperando» siendo un tramo de perfil CREADOR; corregida la bandeja.
- ✅ CORRECTO: `CORREGIDO: T-011` — localizaba el expediente con `.first()`; ahora lo busca por su número capturado al crearlo.
- ✅ CORRECTO: `BLOQUEADO: T-014 — al pulsar el botón del evento la app responde «No existe la vista en el expediente»; falta el form genérico del estado de destino: regresión.`
- ❌ INCORRECTO: `Arreglado ✅` (token no parseable), borrar la aserción de no-transición de un test de error, editar el `views.xml` de la fase para añadir el form que falta.
