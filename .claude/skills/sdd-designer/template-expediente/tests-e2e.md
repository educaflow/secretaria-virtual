# Parte del diseño — los tests E2E

Define el contrato de **`design/test-e2e-desc.md`**: cómo se materializan los escenarios `ESC-NNN` de la especificación en tests E2E descritos en Given/When/Then, la cobertura obligatoria, los datos de demo a usar, la plantilla exacta del fichero, la trazabilidad y el checklist.

Lo escribe el **diseñador**, **siempre** (un tipo de expediente siempre tiene una máquina de estados que recorrer). Lo lee además el **verificador** (comprueba cobertura y formato, `validacion.md` §2 bloque L) y el **corrector** (solo si un fallo afecta a este fichero).

> **CRITICAL — este fichero es CONTRATO FIJO HACIA ABAJO.** `design/test-e2e-desc.md` no se queda en el diseño: `/sdd-implementer` lo propaga a `implementation/`, `/sdd-debug-with-test-e2e-desc` lo **descompone** en un `t-NNN-<slug>.desc.md` autocontenido por test y los **ejecuta contra la aplicación real**, y `/sdd-create-tests-e2e` persiste como regresión los que pasaron. Por eso su forma —la numeración `T-NNN`, el bloque de cabecera de cada test, los actores con credenciales— **MUST** respetarse al pie de la letra: cambiarla rompe skills de aguas abajo.

> **REGLA DE GENERALIDAD.** Este fichero describe **el patrón**. **MUST NOT** aparecer en la parte normativa el nombre de ningún trámite, fase, estado, evento, campo, perfil ni documento reales: se usan los placeholders de `design-contract.md` §0.1. Los ejemplos van en bloques `> **Ejemplo** (ilustrativo, NO normativo):` con nombres inventados. El **fichero producido**, en cambio, es concreto y usa los nombres reales del trámite diseñado.

---

## 1. Qué es y qué no es

- Es una descripción en **lenguaje de negocio**: lo que una persona hace en la aplicación y lo que la aplicación responde.
- **MUST NOT** contener código Playwright, comandos `playwright-cli`, selectores CSS, XPath, ni nombres de vista generados (`exp-<Entidad>-<FASE>-<ESTADO>-form`). La traducción a navegador la hace `/sdd-debug-with-test-e2e-desc`.
- **MUST NOT** contener código Java, ni referencias a clases, métodos o reglas del DSL. Los elementos que un test nombra son **lo que el usuario ve**: el título del botón, el título del panel, el nombre del campo tal como aparece en el formulario, el texto de la cabecera.
- La única nomenclatura técnica admitida —y **REQUIRED**— es la de la **máquina de estados**: `<FASE>`, `<ESTADO>`, `<EVENTO>` y `<PERFIL>`, porque son lo que hace verificable el test («el expediente pasa a la fase X, estado Y»).

- ✅ CORRECTO: `**When** pulsa el botón «<título del botón>» (evento <EVENTO>).`
- ❌ INCORRECTO: `await page.getByRole('button', {name: '...'}).click()`; `se invoca trigger<Evento>()`; `la vista exp-<Entidad>-<FASE>-<ESTADO>-CREADOR-form muestra…`

---

## 2. Actores y datos de demo

Los tests se ejecutan contra la aplicación real arrancada con datos de demo. El diseñador **MUST** leer los ficheros reales de `src/main/resources/data-demo/input/` —`usuarios-demo.xml`, `centros-demo.xml` y `permisos-demo.xml`— y **usar los que ya existen**; y **MUST** cuadrarlos con el fragmento `design/permisos.xml` que él mismo escribe.

Reglas:

- **REQUIRED — tabla de actores.** El fichero **MUST** empezar con una tabla de **todos** los actores que algún test usa para iniciar sesión, con: `login`, `contraseña`, tipo de usuario o cargo, centro, el `<PERFIL>` que le da el diseño y **por qué vía** (`tramiteCode` o `tipoExpedienteCode`). `/sdd-debug-with-test-e2e-desc` necesita esas credenciales para hacer login real: un actor que inicia sesión sin figurar en la tabla es un **fallo de cobertura**.
- **MUST** usarse logins y contraseñas **que existan** en `usuarios-demo.xml`. **MUST NOT** inventarse un usuario. Si ningún usuario de demo encaja con un perfil del tipo, el diseño **MUST** declarar en «Notas y supuestos» que hay que añadirlo y **MUST** incluir su fila en la tabla de ficheros del `design.md` (como `Modificar` sobre el fichero de demo correspondiente).
- **MUST** usarse un solo **centro** de demo salvo que la especificación exija probar el aislamiento multicentro; en ese caso, un actor de otro centro y un test que compruebe que **no** ve el expediente.
- Todos los actores **MUST** pertenecer al mismo centro que el expediente bajo prueba, salvo el test de aislamiento anterior y el usuario `admin`, que ve cualquier centro.
- **REQUIRED — juego de datos válido.** Tras la tabla de actores, el fichero **MUST** declarar, **por fase**, un **juego de datos válido** (campo → valor) que los tests del camino feliz reutilizan en vez de repetirlo. Los valores **MUST** ser concretos y coherentes con el modelo y con las reglas del validador.
- Si el tipo **firma en cliente** (AutoFirma), los tests que lo ejerzan **MUST** declarar como precondición que hay un certificado válido cuyo DNI coincide con el que el `triggerInitialEvent` deja en el expediente, y **MUST** advertir de que la firma se hace en la máquina del usuario.
  **REQUIRED** — esos tests **MUST** llevar además `**Manual:** sí — <motivo>` en su cabecera (§4): un paso que exige una aplicación de escritorio y un certificado del usuario **no se puede pilotar en el navegador**, y la marca es lo que evita que atasquen el pipeline aguas abajo (§4.1).
- Si el tipo requiere **ficheros adjuntos**, el juego de datos **MUST** describir el fichero (tipo y tamaño aproximado) sin dar ninguna ruta del sistema de ficheros.

> **Ejemplo** (ilustrativo, NO normativo) de fila de la tabla de actores, con nombres inventados:
>
> | Login | Contraseña | Tipo / Cargo | Centro | Perfil | Vía |
> |---|---|---|---|---|---|
> | `ejemplo1@centro-x.es` | `clave-demo` | tipo de usuario `EJEMPLO_TIPO` | `CENTRO-X` | `CREADOR` | `tramiteCode` |

---

## 3. Cobertura obligatoria

La cobertura se mide contra **dos** fuentes: la especificación (los `ESC-NNN`) y el propio diseño (la tabla de transiciones y las tablas de estados). Ambas son obligatorias.

### 3.1 Un test por **transición** y por **perfil**

- **MUST** haber al menos un test por **cada fila de la tabla de transiciones** de la sección «Máquina de estados» del `design.md`, **incluidas** la fila del arranque (`[*] → estado inicial`) y las de `DELETE` (`<ESTADO> → [*]`).
- Un evento que **ramifica** produce **un test por rama**: una fila de la tabla por guarda, un test por fila.
- **CRITICAL — y por perfil.** Si una misma transición la puede disparar **más de un perfil** (porque el estado tiene botón para ese evento en los forms de varios `<PERFIL>`), **MUST** haber **un test por cada perfil**. Dos perfiles distintos son dos caminos de autorización distintos, y solo uno de ellos se prueba si se escribe un único test.
- Cada test **MUST** nombrar: el `<PERFIL>` con el que actúa el usuario y su login, la `<FASE>` y el `<ESTADO>` de partida, el `<EVENTO>` que dispara **y el título del botón que lo dispara**, los datos que introduce, y la `<FASE>`/`<ESTADO>` a los que debe llegar.
- Un evento que **no cambia de estado** también necesita su test: el Then comprueba que el expediente **sigue** en el mismo estado y qué ha cambiado (un PDF generado, un campo asignado).

### 3.2 Todos los estados, incluidos los `closed`

- **MUST** mencionarse **todos** los estados del tipo. Un estado al que no llega ninguna transición probada es un estado sin cobertura.
- Cada estado `closed` **MUST** tener su test de llegada **y** la comprobación de que su **vista genérica** se abre en **solo lectura**, con el único botón de salida y sin ningún evento disponible.
- **MUST** haber al menos un test de la **vista genérica de solo lectura** de los estados **abiertos** que tienen `profile`: el expediente se abre por una **bandeja cuyo perfil no es el del estado** (y por tanto no hay form para ese perfil), se ve todo en solo lectura, solo el botón de salir, y el expediente no cambia de estado. Es la red de seguridad que exige `X1` (`vistas.md` §4).
  **CRITICAL — este test se describe por BANDEJA, no por usuario.** La vista la elige el perfil que fija el `action-view` de la bandeja por la que se entra, **no** los perfiles que tenga quien mira: el servidor solo comprueba que ese perfil lo use algún estado del tipo, y cae a la vista genérica cuando no existe form para él.
  El perfil **real** del usuario se comprueba después, al **disparar** el evento.
  Por eso el `Given` de este test **MUST** decir **por qué bandeja se entra**.
  Describirlo como «un usuario sin el perfil abre el expediente» da un test que no comprueba lo que dice: ese mismo usuario, entrando por la bandeja del perfil del estado, vería la vista completa **con** sus botones.

### 3.3 Validaciones fallidas

- **MUST** haber al menos un test de **validación fallida** por **cada pareja (estado, evento) que tenga reglas** en el validador. El usuario deja vacío un campo obligatorio o mete un valor inválido, y el sistema **no transiciona**.
- El Then de estos tests **MUST** comprobar dos cosas: que se muestra el mensaje de error, **y** que el expediente **sigue** en el estado de partida.
- Una pareja con muchas reglas puede resolverse en **un solo test** con una lista de variantes en el `And`, siempre que cada variante nombre el campo y el motivo. Las reglas que la especificación distinga como escenarios propios (`ESC-NNN` de error) **MUST** ir en tests separados.
- Las parejas cuyo validador es `rules { }` vacío y el evento `DELETE` (exento de validación) **MUST NOT** tener test de validación fallida: no hay nada que fallar.

### 3.4 Trazabilidad con la especificación

- **MUST** haber al menos un test por cada `ESC-NNN` de la especificación: cada `ESC-NNN` aparece en el campo `Origen ESC` de **al menos un** test.
- Un escenario con ramas condicionales puede dar lugar a **varios** tests (uno por rama); un test puede materializar **varios** escenarios.
- Un test que cubre una transición del diseño que **ningún** escenario de la spec describe lleva `Origen ESC: —` y es legítimo (la cobertura de §3.1 es más exigente que la spec). **MUST NOT**, en cambio, quedar ningún `ESC-NNN` sin test.

---

## 4. Plantilla de `design/test-e2e-desc.md`

El diseñador escribe un fichero con **esta estructura exacta**. Los `<…>` se sustituyen por los valores reales del trámite diseñado.

```markdown
# Tests E2E — <nombre visible del trámite> (`<Entidad>`)

Tests en lenguaje de negocio, Given/When/Then, materializados a partir de los escenarios (`ESC-NNN`) de la especificación y de la tabla de transiciones del diseño. **Sin código Playwright y sin selectores.**

## Actores

| Login | Contraseña | Tipo / Cargo | Centro | Perfil | Vía |
|---|---|---|---|---|---|
| <login> | <contraseña> | <tipo de usuario o cargo> | <centro> | <PERFIL> | tramiteCode \| tipoExpedienteCode |

## Datos de demo

Estado previo del que parten **todos** los tests: la carga de demo (`data.import.demo-data = true`) con sus centros, usuarios y perfiles, más el trámite publicado en el árbol de trámites del centro. Ningún test puede presuponer más estado que este.

### Juego de datos válido — fase `<FASE>`

| campo | valor |
|---|---|
| `<campo>` | `<valor concreto>` |

(una subsección por fase que pida datos al usuario)

## Cobertura de transiciones

| # | fase origen | estado origen | evento | guarda | fase destino | estado destino | perfil | test |
|---|---|---|---|---|---|---|---|---|
| 1 | `[*]` | `[*]` | — | — | `<FASE>` | `<ESTADO INICIAL>` | `<PERFIL>` | T-001 |
| 2 | `<FASE>` | `<ESTADO>` | `<EVENTO>` | `<campo>=<VALOR>` | `<FASE>` | `<ESTADO>` | `<PERFIL>` | T-002 |
| … | | | | | | | | |

Tests de validación fallida: <lista de T-NNN>.
Tests de vistas genéricas de solo lectura: <lista de T-NNN>.
Tests **manuales** (no automatizables, §4.1): <lista de T-NNN, o «ninguno»>.

---

## T-001 — <nombre corto y descriptivo>

**Origen ESC:** ESC-001 (o `—`)
**Perfil:** `<PERFIL>` (login `<login>`)
**Desde:** `<FASE>` / `<ESTADO>` (o `[*]` en el arranque)
**Evento:** `<EVENTO>` — botón «<título del botón>» (o `—` en el arranque)
**Hasta:** `<FASE>` / `<ESTADO>` (o `[*]` en un `DELETE`)
**Tipo:** happy | error | solo-lectura
**Manual:** no (o `sí — <motivo>` si algún paso no es automatizable, §4.1)

- **Given** <situación de partida: quién es el usuario, qué perfil tiene, dónde está el expediente y qué ve>.
- **When** <la acción concreta: qué datos introduce y qué botón pulsa>.
- **Then** <el resultado observable principal: a qué fase y estado llega, qué muestra la cabecera>.
- **And** <lo demás que debe comprobarse: qué documento se ha generado, qué registro se ha creado, qué paneles y botones ofrece la nueva pantalla>.

---

## T-002 — <nombre corto y descriptivo>

…
```

Reglas de forma:

- Numeración `T-001`, `T-002`, … **global al fichero, con tres dígitos, sin huecos y empezando en `001`**. Es lo que `/sdd-debug-with-test-e2e-desc` usa para nombrar cada `t-NNN-<slug>.desc.md`.
- El bloque de cabecera de cada test lleva **los siete campos** (`Origen ESC`, `Perfil`, `Desde`, `Evento`, `Hasta`, `Tipo`, `Manual`), cada uno en su línea, siempre en ese orden. Un campo que no aplica se escribe `—`; **MUST NOT** omitirse la línea. `Manual` nunca es `—`: es `no` o `sí — <motivo>`.
- Los pasos van en `Given` / `When` / `Then` / `And` (o `Dado` / `Cuando` / `Entonces` / `Y`, pero **uno de los dos idiomas en todo el fichero**, sin mezclarlos).
- Cada test es **autosuficiente**: su `Given` describe entero el punto de partida y cómo se llega a él, sin depender de que otro test se haya ejecutado antes.
- Los títulos de botones, paneles y campos van **entre comillas angulares** y **MUST** coincidir literalmente con los `title` que el diseño pone en los `views.xml` y en el `domains.xml`.

### 4.1 El campo `Manual` — tests que necesitan una persona

Declara si el test se puede ejecutar **entero** sin intervención humana. Es el campo que decide qué pasa con el test en todo el pipeline aguas abajo, así que **MUST** escribirse siempre y con criterio.

- `Manual: no` — el caso normal: todo el test es pilotable en el navegador.
- `Manual: sí — <motivo>` — algún paso exige **algo que no vive en el navegador**: hoy, en la práctica, la **firma en cliente con AutoFirma** (aplicación de escritorio + certificado en la máquina de quien firma).

Reglas:

- **MUST NOT** marcarse `sí` porque el test sea largo, tenga muchos tramos o su locator sea difícil: eso es trabajo del ejecutor, no una imposibilidad.
  La marca es para lo que **ninguna** automatización puede hacer.
- El `<motivo>` **MUST** ser concreto y nombrar el paso: sirve de instrucción a la persona que luego ejecute el test a mano.
- Un test manual **cuenta igual** para la cobertura obligatoria de §3: marcarlo no exime de escribirlo ni de que su `Then` compruebe la fase y el estado de llegada.
- **MUST** acotarse el alcance: si la transición **anterior** a la firma se puede probar sin firmar, va en su propio test `Manual: no`, y el test manual cubre solo el tramo que de verdad necesita el certificado.

Qué provoca la marca aguas abajo (el diseñador no lo gestiona, pero lo declara):
`/sdd-debug-with-test-e2e-desc` escribe esos tests como `- [-]` en su índice y **los salta** en vez de atascarse en ellos, y `/sdd-create-tests-e2e` los persiste como `.spec.ts` con el tag `@manual`, que la suite **excluye por defecto** (también en CI/CD) y solo se lanza pidiéndolo expresamente.

> **Ejemplo** (ilustrativo, NO normativo): `**Manual:** sí — el paso «Firmar» abre AutoFirma__!! y exige el certificado del interesado en su máquina.`

---

## 5. Checklist de los tests E2E

El diseñador lo aplica antes de dar el diseño por terminado; el verificador lo reaplica.

**Actores y datos**

- [ ] ¿El fichero empieza con la tabla de **actores**, con `login`, `contraseña`, tipo/cargo, centro, perfil y vía, para **cada** usuario que inicia sesión en algún test?
- [ ] ¿Todos los logins y contraseñas existen realmente en `usuarios-demo.xml` (o su alta está declarada en «Notas y supuestos» y en la tabla de ficheros)?
- [ ] ¿Los perfiles de la tabla de actores cuadran con las asignaciones de `design/permisos.xml`, y el del estado inicial va por `tramiteCode`?
- [ ] ¿Hay un **juego de datos válido** por cada fase que pide datos al usuario, con valores concretos y coherentes con el validador?

**Cobertura**

- [ ] ¿Hay al menos un test por **cada fila** de la tabla de transiciones, incluidos el arranque y los `DELETE`?
- [ ] ¿Cada rama de un evento ramificado tiene su propio test, con su guarda?
- [ ] ¿Cada transición que pueden disparar **varios perfiles** tiene un test **por perfil**?
- [ ] ¿Se mencionan **todos** los estados del tipo, incluidos los `closed` y los que no tienen eventos?
- [ ] ¿Cada estado `closed` tiene su test de vista genérica en solo lectura, con el único botón de salir?
- [ ] ¿Hay al menos un test de vista genérica de solo lectura para los estados abiertos con `profile`, y su `Given` dice **por qué bandeja** se entra (no «un usuario sin el perfil», §3.2)?
- [ ] ¿Hay al menos un test de **validación fallida** por cada pareja (estado, evento) con reglas, y **ninguno** para las parejas con `rules { }` vacío ni para `DELETE`?
- [ ] ¿Cada `ESC-NNN` de la especificación aparece en el `Origen ESC` de al menos un test?

**Forma**

- [ ] ¿La numeración es `T-NNN` de tres dígitos, global, sin huecos y empezando en `001`?
- [ ] ¿Cada test lleva los **siete** campos de cabecera, en orden, sin omitir ninguna línea?
- [ ] ¿El campo `Manual` es `no` o `sí — <motivo>` (nunca `—`), y está en `sí` **exactamente** en los tests con un paso no automatizable (§4.1), con su motivo concreto?
- [ ] ¿Los tests marcados `Manual: sí` figuran en la línea «Tests manuales» de la tabla de cobertura, y esa lista cuadra con las cabeceras?
- [ ] ¿Cada test es autosuficiente y su `Given` describe entero el punto de partida?
- [ ] ¿Cada `Then` de un test de camino feliz comprueba la **fase y el estado de llegada**?
- [ ] ¿Cada `Then` de un test de validación fallida comprueba el mensaje **y** que el expediente **no** ha transicionado?
- [ ] ¿Los títulos de botones, paneles y campos coinciden literalmente con los del diseño?
- [ ] ¿No hay **ningún** selector, comando de navegador, nombre de vista generado, clase Java ni regla del DSL?
- [ ] ¿La tabla «Cobertura de transiciones» cuadra fila a fila con la tabla de transiciones del `design.md`, y cada fila referencia un `T-NNN` que existe?
