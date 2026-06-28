# Ejecución: pilotar un test E2E contra la app real

Lo lee el **ejecutor** (§2.2 del `README.md`). Tarea: ejecutar **un** `t-NNN-<slug>.desc.md` contra la aplicación real y devolver `SUCCESS`/`FAIL`. **NO** modificar código.

---

## 1. Preparación

1. **Carga el skill `playwright-cli`** con la herramienta `Skill`: es el que pilota el navegador interpretando el Given/When/Then.
2. Lee tu `t-NNN-<slug>.desc.md` (autocontenido): trae el `## Estado inicial de la base de datos` (datos maestros + **tabla de credenciales de login**) y el bloque del test (`Precondiciones`/`Pasos`/`Resultado esperado`).
3. **Premisa**: la app YA está levantada en `http://localhost:8080` (la arrancó el orquestador). **MUST NOT** arrancarla, pararla ni recompilarla. Comprueba que responde:
   ```bash
   curl -s -o /dev/null -w "%{http_code}" http://localhost:8080
   ```
   Si no responde `200`, devuelve `FAIL {T-NNN}` con motivo "app no disponible".

---

## 2. Ejecutar el test

1. **Login**: inicia sesión con las credenciales del actor del test (de la tabla **Usuarios de acceso** de tu fichero). La URL base es `http://localhost:8080`.
2. **Interpreta** los `Pasos` (`Dado`/`Cuando`/`Y`/`Entonces`) en lenguaje de negocio y condúcelos en el navegador (navegación por menús, alta de datos, pulsar botones). Usa `snapshot` para localizar referencias.
3. **Verifica** cada punto del `Resultado esperado`.
4. Al terminar, cierra el navegador (`playwright-cli close`).

**MUST NOT** modificar `t-NNN-<slug>.desc.md` ni ningún fichero del proyecto. Solo lees y pilotas.

---

## 3. Equivalencia semántica de los mensajes (criterio de éxito)

Cuando el `Resultado esperado` cita el **texto de un mensaje** de validación/error/aviso, el criterio es la **equivalencia semántica**, no la coincidencia literal carácter a carácter. Si el sistema rechaza/acepta la operación en el momento esperado y el mensaje comunica la **misma causa**, es `SUCCESS` aunque la redacción difiera.

- ✅ SUCCESS: esperado "El DNI del destinatario es obligatorio." / observado "DNI destinatario es requerido" (misma causa).
- ✅ SUCCESS: esperado "La fecha final no puede ser anterior a la inicial." / observado "La fecha de fin debe ser posterior a la de inicio" (misma causa).
- ❌ FAIL: esperado un mensaje de DNI obligatorio / observado uno sobre el email, o ningún mensaje, o la operación **no** se rechaza (causa distinta o comportamiento incorrecto).

**CRITICAL**: esto aplica **solo al texto del mensaje**. El resto del `Resultado esperado` (que la operación se rechace/complete, el estado de la entidad, los campos afectados) **MUST** cumplirse exactamente.

---

## 4. Errores recurrentes a evitar (CRITICAL)

Estos patrones cuelgan el pilotaje o dan falsos negativos. Evítalos siempre:

1. **El VALUE de un input no es texto visible.** Los campos que rellena el servidor (autocompletados; p. ej. el email que aparece al teclear el DNI) son el `value` de un `<input>`, **no** texto del DOM. Comprobarlos con `wait_for(text:…)`/`verify_text` **no resuelve nunca** y cuelga.
   - **MUST** usar `browser_verify_value` (`toHaveValue`) para el valor de un input; **NUNCA** `wait_for`/`verify_text` sobre él.
   - Si ese valor es **incidental** (no es lo que el test verifica), **NO** lo esperes ni lo compruebes: teclea y sigue.
2. **La SPA de Axelor cachea el formulario y el grid.** Con routing por hash, `goto` a la **misma** URL solo cambia el hash y **NO** recarga. Para sondear un cambio **asíncrono** (p. ej. que una tarea periódica/cron pase un registro de un estado a otro):
   - recarga la **LISTA** o usa `page.reload()` (recarga dura), o consulta REST autenticado con `page.request` contra `/ws/rest/<FQN>/search`;
   - **MUST NOT** sondear con `goto(mismaUrlDetalle)` ni `goto(mismaUrlLista)` (sirven datos cacheados).
3. **Toda espera lleva timeout acotado.** **MUST NOT** introducir esperas indefinidas: cualquier `wait_for`/sondeo lleva `timeout` explícito (amplio si depende del cron).
4. **El editor de cuerpo es un `contenteditable`.** Rellenar `.custom-html-editor-content` con `.fill()` deja el campo vacío para el servidor. **MUST** usar `click()` + teclear con el teclado.

---

## 5. Formato de salida (REQUIRED)

- Primera línea **exactamente** `SUCCESS {T-NNN}` o `FAIL {T-NNN}`.
- Si `FAIL`: debajo, un bloque `=== FALLO ===` con:
  - **Qué falló**: el paso concreto, el valor/estado esperado vs el observado.
  - **Información de la UI en el momento del fallo** (recógela ANTES de reportar, con `playwright-cli`): `snapshot` de la pantalla/panel relevante, texto de mensajes/alertas/toasts de error visibles, valores de los campos implicados, URL y título de la página, y los errores de `console` y las peticiones (`requests`) fallidas con su status/cuerpo.

```
FAIL T-009
=== FALLO ===
Paso 7 ("el grupo se crea en estado «Abierto»"): tras pulsar "Guardar" el grupo quedó en estado «Borrador», no «Abierto».
UI: toast rojo "No se pudo abrir el grupo: …". URL: #/ds/...Grupo.../edit. Consola: 500 en POST /ws/rest/...Grupo. Campo "Estado" = "Borrador".
```

- ✅ CORRECTO: `SUCCESS T-001` (solo eso) / `FAIL T-009` + bloque `=== FALLO ===` con la info de UI.
- ❌ INCORRECTO: `El test ha pasado` (token no exacto), `FAIL` sin `=== FALLO ===`, reportar el fallo sin haber recogido el snapshot/consola/requests.

**MUST NOT** usar `AskUserQuestion`. **MUST NOT** corregir código (eso es del corrector).
