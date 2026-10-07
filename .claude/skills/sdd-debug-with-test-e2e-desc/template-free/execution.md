# Ejecución: pilotar un test E2E contra la app real (feature libre)

Lo lee el **ejecutor** (README §2.2). Tarea: ejecutar **un** `t-NNN-<slug>.desc.md` contra la aplicación real y devolver `SUCCESS`/`FAIL`.

---

## 1. Preparación

1. **Carga el skill `playwright-cli`** con la herramienta `Skill`: es el que pilota el navegador.
2. Lee tu `t-NNN-<slug>.desc.md`: estado inicial + **tabla de credenciales** + el bloque del test.
3. Si su campo **`Manual`** es `sí`: no lo ejecutes; devuelve `FAIL {T-NNN}` con `=== FALLO ===` «test manual: el motor no debía enviarlo». (El motor salta los `- [-]`; esto es solo una red de seguridad.)
4. **Premisa**: la app YA está levantada en `http://localhost:8080`. **MUST NOT** arrancarla, pararla ni recompilarla. Comprueba:
   ```bash
   curl -s -o /dev/null -w "%{http_code}" http://localhost:8080
   ```
   Si no responde `200` → `FAIL {T-NNN}` con motivo «app no disponible».

---

## 2. Ejecutar el test

1. **Login** con las credenciales del actor (tabla **Usuarios de acceso**). URL base `http://localhost:8080`.
2. **Interpreta** los `Pasos` (`Dado`/`Cuando`/`Y`/`Entonces`) en lenguaje de negocio y condúcelos en el navegador. Usa `snapshot` para localizar referencias. Una feature libre puede llevarte por **varias pantallas de partes distintas** de la aplicación (p. ej. configurar en administración y comprobar en otra pantalla con otro usuario): sigue los pasos tal cual, con los **cierres e inicios de sesión** que indiquen.
3. **Verifica** cada punto del `Resultado esperado`, incluidos los de **no-regresión** («la pantalla se muestra como siempre»: compara con lo que esa pantalla hacía, no asumas que pasa).
4. Si el test cambió algo **global** (una configuración, un aviso para todos) y el `Resultado esperado` dice que lo deshace, **ejecuta ese paso también**: si no, contaminas los tests siguientes.
5. Cierra el navegador (`playwright-cli close`).

**MUST NOT** modificar ningún fichero del proyecto.

---

## 3. Equivalencia semántica de los mensajes

Cuando el `Resultado esperado` cita el **texto de un mensaje**, el criterio es la **equivalencia semántica** (misma causa comunicada), no la coincidencia literal; también vale el mismo mensaje en el otro idioma (es/ca) si el usuario tiene ese idioma.

- ✅ SUCCESS: esperado «El texto del aviso es obligatorio» / observado «El texto es requerido».
- ❌ FAIL: mensaje sobre otra causa, ningún mensaje, o la operación **no** se rechaza.

**CRITICAL**: solo aplica al texto del mensaje. El resto del `Resultado esperado` (que se rechace/complete, el estado, lo que se ve o no se ve) **MUST** cumplirse exactamente.

---

## 4. Errores recurrentes a evitar (CRITICAL)

1. **El VALUE de un input no es texto visible.** Comprueba el `value` de un `<input>` con la aserción `toHaveValue` equivalente de tu herramienta, nunca esperando su texto (no resuelve y cuelga). Si el valor es incidental, no lo esperes.
2. **La SPA de Axelor cachea.** Navegar a la **misma** URL con routing por hash **no recarga**. Para sondear un cambio asíncrono (un cron, un job, un aviso que aparece «al cambiar de pantalla»): navega a **otra** pantalla y vuelve, recarga de verdad la página, o consulta el REST `/ws/rest/<FQN>/search` autenticado. **MUST NOT** sondear navegando a la misma URL.
3. **Toda espera lleva timeout acotado** (amplio si depende de un cron: lee su periodo en `axelor-config.properties`). **MUST NOT** esperas indefinidas.
4. **El editor de cuerpo es un `contenteditable`** (`.custom-html-editor-content`): clic + teclear, no rellenarlo como un input.
5. **Elementos del marco común** (banners, cabecera, menú lateral) se repiten en todas las pantallas: un `getByText` puede dar **varios matches**; acota al contenedor que el paso describe.
6. **Varios usuarios en un test**: tras «cierra sesión» confirma que estás en el login antes de entrar con el siguiente; una sesión a medias hace que el segundo login «funcione» con el usuario anterior.

---

## 5. Formato de salida (REQUIRED)

- Primera línea **exactamente** `SUCCESS {T-NNN}` o `FAIL {T-NNN}`.
- Si `FAIL`: bloque `=== FALLO ===` con **qué falló** (paso, esperado vs observado) y la **información de la UI** recogida antes de reportar: `snapshot` del panel relevante, mensajes/toasts, valores de campos, URL y título, errores de `console` y peticiones fallidas con status/cuerpo.

```
FAIL T-001
=== FALLO ===
Paso 6 ("el sistema muestra un banner con el texto «Mantenimiento el viernes a las 20:00»"): tras el login de profesor1@batoi.es no hay ningún banner.
UI: pantalla de inicio sin elemento de aviso. URL: #/. Consola: 403 en GET /ws/rest/...AvisoGlobal/search. Snapshot: …
```

- ✅ CORRECTO: `SUCCESS T-001` (solo eso) / `FAIL T-001` + bloque completo.
- ❌ INCORRECTO: `El test ha pasado`, `FAIL` sin `=== FALLO ===`, reportar sin snapshot/consola/requests.

**MUST NOT** usar `AskUserQuestion`.
