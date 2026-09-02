# Ejecución: pilotar un test E2E de un tipo de expediente contra la app real

Lo lee el **ejecutor** (§3.2 del `README.md`). Tarea: ejecutar **un** `t-NNN-<slug>.desc.md` contra la aplicación real y devolver `SUCCESS`/`FAIL`. **NO** modificar código.

---

## 1. Preparación

1. **Carga el skill `playwright-cli`** con la herramienta `Skill`: es el que pilota el navegador interpretando el Given/When/Then.
2. Lee tu `t-NNN-<slug>.desc.md` (autocontenido): trae los **siete campos de cabecera** (`Origen ESC`, `Perfil`, `Desde`, `Evento`, `Hasta`, `Tipo`, `Manual`), el `## Estado inicial de la base de datos` (actores + juegos de datos) y los `## Pasos`.
3. **Lee la §4 del `README.md`** de esta plantilla: la UI del subsistema de expedientes (cómo se crea un expediente, por qué bandeja se abre cada perfil, dónde está la fase/estado y dónde salen los errores). Sin eso, pilotar a ciegas produce falsos negativos.
4. **Premisa**: la app YA está levantada en `http://localhost:8080` (la arrancó el orquestador). **MUST NOT** arrancarla, pararla ni recompilarla. Comprueba que responde:
   ```bash
   curl -s -o /dev/null -w "%{http_code}" http://localhost:8080
   ```
   Si no responde `200`, devuelve `FAIL {T-NNN}` con motivo "app no disponible".

---

## 2. Leer la cabecera del test antes de pilotar

Los siete campos dicen **qué camino recorrer**, antes incluso de leer los pasos:

| Campo | Qué determina |
|---|---|
| `Perfil` | con qué **login** entrar **y por qué bandeja** abrir el expediente (§3.2) |
| `Desde` | en qué **fase/estado** debe estar el expediente al empezar; `[*]` = hay que crearlo |
| `Evento` | qué **botón del footer** pulsar (el campo trae el `<EVENTO>` **y** el título del botón) |
| `Hasta` | qué **fase/estado** comprobar al final; `[*]` = el expediente deja de existir (`DELETE`) |
| `Tipo` | `happy` (transiciona), `error` (NO transiciona y sale mensaje), `solo-lectura` (no hay botones de evento) |
| `Manual` | `no` = pilotable entero. `sí — <motivo>` = **no deberías estar ejecutando este test**: el motor los salta (`README.md` §4.4). Si te llega uno, ejecútalo hasta el paso imposible y reporta `FAIL` con el motivo literal de §5, punto 8 |

**CRITICAL** — `Tipo: error` invierte el criterio de éxito: el test pasa **si y solo si** la aplicación **rechaza** la operación y el expediente **sigue** en el estado de `Desde`. Que el expediente avance en un test de tipo `error` es `FAIL`, aunque no salga ninguna excepción.

---

## 3. Ejecutar el test

### 3.1 Login

Inicia sesión con las credenciales del actor del test (la fila de `### Actores` cuyo login cita el campo `Perfil`). La URL base es `http://localhost:8080`.

### 3.2 Situar el expediente en el estado de partida

1. **`Desde: [*]`** → crea el expediente: menú **«Expedientes» → «Trámites»**, despliega el tipo de trámite y pulsa el nodo del trámite. El expediente se crea y se abre ya en el estado inicial. **MUST NOT** buscar un botón «Nuevo» de un grid: no existe.
2. **`Desde: <FASE>/<ESTADO>`** → el `Given` describe cómo se llega. **MUST** recorrer las transiciones previas **por la UI** (con los usuarios y perfiles que correspondan a cada tramo), no atajar por REST ni por base de datos.
3. **Abre el expediente por la bandeja del perfil del test** (`README.md` §4.2): `CREADOR` → «Expedientes Pendientes»; `RESPONSABLE` → «Expedientes Esperando» (abiertos) o «Expedientes Cerrados» (cerrados). Localízalo por su **número de expediente**, que la cabecera y los listados muestran.

- ❌ INCORRECTO: abrir el expediente por «Expedientes Esperando» en un test de perfil `CREADOR` y reportar `FAIL` porque "todo sale en solo lectura" (es la vista genérica: bandeja equivocada, no un fallo).

### 3.3 Ejecutar los pasos y disparar el evento

1. Rellena los campos que pida el `When` con los valores del **juego de datos de esa fase**.
2. Pulsa el botón del footer **por su título** (el que da el campo `Evento`). **MUST NOT** guardar con el botón de guardar de Axelor esperando que transicione: en un expediente **la transición la dispara el botón del footer**, no un guardado.
3. Espera a que el formulario se repinte: tras un evento la app **sustituye la vista** por la del nuevo estado.

### 3.4 Verificar

- **`Tipo: happy`** — **MUST** comprobar en la cabecera «Información general» que **«Fase»** y **«Estado»** son los del campo `Hasta` (por su título visible, no por el `UPPER_SNAKE_CASE`), más todo lo que digan los `Then`/`And` (documentos generados, campos asignados, paneles y botones de la nueva pantalla).
- **`Tipo: error`** — **MUST** comprobar las **dos** cosas: que aparece el mensaje en el recuadro rojo del footer **y** que «Fase»/«Estado» **siguen** siendo los de `Desde`.
- **`Tipo: solo-lectura`** — **MUST** comprobar que los campos no son editables, que el footer **no** ofrece ningún botón de evento (solo el de salir, si lo hay) y que el expediente no cambia de estado.
- **`Hasta: [*]`** (`DELETE`) — **MUST** comprobar que el expediente ya no aparece en la bandeja de la que se abrió. Tras `DELETE` (y tras `EXIT`) la aplicación **se recarga entera**: vuelve a navegar desde el menú, no esperes seguir en el formulario.

Al terminar, cierra el navegador (`playwright-cli close`).

**MUST NOT** modificar `t-NNN-<slug>.desc.md` ni ningún fichero del proyecto. Solo lees y pilotas.

---

## 4. Equivalencia semántica de los mensajes (criterio de éxito)

Cuando un `Then`/`And` cita el **texto de un mensaje** de validación/error/aviso, el criterio es la **equivalencia semántica**, no la coincidencia literal carácter a carácter. Si el sistema rechaza/acepta la operación en el momento esperado y el mensaje comunica la **misma causa**, es `SUCCESS` aunque la redacción difiera.

- ✅ SUCCESS: esperado "El motivo de la solicitud es obligatorio." / observado "Motivo: debe rellenarse" (misma causa).
- ❌ FAIL: esperado un mensaje sobre un campo / observado uno sobre otro campo, ningún mensaje, o la operación **no** se rechaza.

**CRITICAL**: esto aplica **solo al texto del mensaje**. El resto (que la operación se rechace/complete, la **fase y el estado** de llegada, los documentos generados) **MUST** cumplirse exactamente. **MUST NOT** dar por equivalentes dos estados distintos porque "significan algo parecido".

---

## 5. Errores recurrentes a evitar (CRITICAL)

Estos patrones cuelgan el pilotaje o dan falsos negativos:

1. **Bandeja equivocada → vista genérica.** El síntoma es "todo readonly y sin botones". Antes de reportar `FAIL`, comprueba que entraste por la bandeja del perfil del test (§3.2, `README.md` §4.2).
2. **La SPA de Axelor cachea el formulario y el grid.** Con routing por hash, `goto` a la **misma** URL solo cambia el hash y **NO** recarga. Para volver a mirar un expediente tras un cambio, **MUST** recargar la **lista** o usar `page.reload()`; **MUST NOT** sondear con `goto(misma URL de detalle)`.
3. **Tras un evento, la vista cambia entera.** No reutilices referencias del snapshot anterior: vuelve a tomar `snapshot` después de pulsar el botón.
4. **`EXIT` y `DELETE` recargan la aplicación** (`refresh-app`). Cualquier espera sobre el formulario anterior se queda colgada.
5. **El VALUE de un input no es texto visible.** Los campos que rellena el servidor (número de expediente, campos calculados) son el `value` de un `<input>`, **no** texto del DOM: compruébalos con `toHaveValue`/`verify_value`, **nunca** con `wait_for(text:…)`/`verify_text`.
6. **Toda espera lleva timeout acotado.** **MUST NOT** introducir esperas indefinidas.
7. **La generación de un PDF y la firma en servidor tardan.** Si un `And` espera un documento, espera al panel/visor con un timeout amplio, no con un sondeo fijo.
8. **AutoFirma no es automatizable** (`README.md` §4.4): un paso que exija firmar en cliente se reporta `FAIL {T-NNN}` con motivo literal «paso no automatizable (AutoFirma)». **MUST NOT** intentar simularlo ni saltárselo declarando `SUCCESS`.
   Estos tests vienen marcados `Manual: sí` y el motor los salta, así que si te llega uno es que el diseño olvidó la marca: **recórrelo hasta el paso imposible** (todo lo anterior sí se comprueba) y reporta ahí, para que el corrector lo reconduzca con su token `MANUAL`.
   **MUST NOT** usar este motivo para un paso que simplemente no te sale: es solo para lo que ninguna automatización puede hacer.

---

## 6. Formato de salida (REQUIRED)

- Primera línea **exactamente** `SUCCESS {T-NNN}` o `FAIL {T-NNN}`.
- Si `FAIL`: debajo, un bloque `=== FALLO ===` con:
  - **Qué falló**: el paso concreto, y —cuando aplique— la **fase/estado esperados vs los observados**.
  - **Información de la UI en el momento del fallo** (recógela ANTES de reportar, con `playwright-cli`): `snapshot` de la pantalla, texto del recuadro rojo del footer si lo hay, valores de los campos implicados, contenido de «Fase» y «Estado» de la cabecera, URL y título de la página, y los errores de `console` y las peticiones (`requests`) fallidas con su status/cuerpo.

```
FAIL T-006
=== FALLO ===
Paso 3 (pulsar «Enviar a revisión», evento ENVIAR): el expediente debía quedar en fase «Revisión» / estado «Pendiente de revisar» y sigue en «Recepción» / «Entrada de datos».
UI: sin recuadro rojo en el footer. Cabecera: Fase="Recepción", Estado="Entrada de datos". Consola: 500 en POST /ws/action. Requests: {"message":"..."}.
```

- ✅ CORRECTO: `SUCCESS T-001` (solo eso) / `FAIL T-006` + bloque `=== FALLO ===` con la fase/estado observados.
- ❌ INCORRECTO: `El test ha pasado` (token no exacto), `FAIL` sin `=== FALLO ===`, reportar el fallo sin la fase/estado observados (el corrector no puede localizar el `trigger*` culpable).

**MUST NOT** usar `AskUserQuestion`. **MUST NOT** corregir código (eso es del corrector).
