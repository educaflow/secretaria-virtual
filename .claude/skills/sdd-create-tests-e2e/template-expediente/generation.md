# Generación: de `t-NNN-<slug>.desc.md` a su `.spec.ts` (tipo de expediente)

Lo lee el **generador** (§2.1 del `README.md`). Tarea: convertir **una** descripción autocontenida en su test Playwright hermano, pilotando la app real.

**MUST** cargar `/k-playwright` antes de empezar. **MUST** leer la §3.2 del `README.md` (la UI de un expediente: cómo se crea, por qué bandeja se abre cada perfil, dónde está la fase/estado y dónde salen los errores). **MUST** usar las tools MCP `generator_setup_page` / `browser_*` / `generator_write_test` para grabar el test contra la app levantada.

**CRITICAL — disciplina de tiempo** (redescubrir toda la UI en cada test es el mayor coste del flujo):

1. **Reutiliza los `.spec.ts` hermanos ya verdes** de la misma carpeta destino **antes** de pilotar nada: ya tienen resueltos el login, la creación del expediente, la navegación por bandejas, los locators de Axelor y el patrón de idempotencia (§5). Cópialos como base y usa el navegador **solo** para lo específico de **este** test.
2. **MUST NOT** explorar la UI de forma exhaustiva si un spec hermano ya muestra el camino.
3. **MUST** cerrar tu sesión de navegador con `browser_close` al terminar: una sesión MCP huérfana bloquea al siguiente subagente durante minutos.

---

## 1. Qué leer de la descripción

El `.desc.md` es **autocontenido**. Extrae:

1. **Frontmatter** `id: T-NNN` y la línea `**Origen ESC:**` → para los comentarios de trazabilidad.
2. **Los siete campos de cabecera**, que son el **guion del test**:

   | Campo | Qué determina en el `.spec.ts` |
   |---|---|
   | `Perfil` | el **login** con el que actuar y **por qué bandeja** abrir el expediente (§4) |
   | `Desde` | dónde debe estar el expediente al empezar; `[*]` = hay que crearlo |
   | `Evento` | el **título del botón** del footer que hay que pulsar |
   | `Hasta` | la **fase y el estado** que hay que asertar al final; `[*]` = el expediente desaparece |
   | `Tipo` | `happy` / `error` / `solo-lectura` → **cambia qué se asierta** (§6) |
   | `Manual` | `sí — <motivo>` → el test lleva el tag `@manual` y una **puerta manual** (§6.1); `no` → test normal |

3. **`## Estado inicial de la base de datos`** → `### Actores` da **login y contraseña** de cada usuario que intervenga; `### Datos de demo` da el **juego de datos válido por fase** que hay que teclear.
4. **`## Pasos`** → los bullets `Given`/`When`/`Then`/`And`. Los `Given`/`When` son **acciones**; los `Then`/`And` son las **aserciones**: **MUST** materializar **todas**, ninguna omitida.

**CRITICAL** — en esta plantilla **no hay una sección `Resultado esperado` separada**: el resultado esperado son los bullets `Then` y `And`. Un `And` sin su `expect` es un test infiel y el verificador lo caza.

---

## 2. Nombre y ubicación del fichero

- **MUST** escribir el `.spec.ts` con **el mismo nombre base** que el `.desc.md` y en **la misma carpeta**: `t-004-el-responsable-devuelve-el-expediente.desc.md` → `t-004-el-responsable-devuelve-el-expediente.spec.ts`.
- ❌ INCORRECTO: nombre distinto, otra carpeta, repartir en varios ficheros.

---

## 3. Navegación real (lo que ningún locator adivina)

1. **Crear el expediente** (`Desde: [*]`): menú **«Expedientes» → «Trámites»**, desplegar el tipo de trámite y pulsar el **nodo del trámite**. El expediente se crea y se abre en el estado inicial. **MUST NOT** buscar un botón «Nuevo» de un grid.
2. **Llegar al estado de partida** (`Desde: <FASE>/<ESTADO>`): recorrer las transiciones previas **por la UI**, con el usuario y la bandeja que corresponda a cada tramo (§4). **MUST NOT** atajar por REST (`page.request`) ni por SQL: el test dejaría de probar la máquina de estados.
3. **Disparar el evento**: pulsar el **botón del footer por su título**. **MUST NOT** usar el botón de guardar de Axelor esperando que transicione: en un expediente la transición la dispara el botón del footer.
4. **Tras el evento la vista cambia entera**: espera al nuevo estado con `await expect(...)`, no reutilices locators de la pantalla anterior.
5. **`EXIT` y `DELETE` recargan la app** (`refresh-app`): después hay que volver a navegar desde el menú.

---

## 4. Ciclo de autenticación — uno por TRAMO, no uno por test

Un test de expediente suele recorrer **varios perfiles** (el `CREADOR` lo presenta, el `RESPONSABLE` lo resuelve). Cada cambio de actor es un **tramo** con su propio ciclo:

```ts
await ensureLoggedOut(page);
await login(page, '<login del actor del tramo>', '<contraseña de ### Actores>');
// … acciones de ese tramo …
await logout(page);   // en el ÚLTIMO tramo NO: ese logout va en el `finally`, tras el teardown (regla 2)
```

Reglas:

1. El **primer** tramo empieza con `ensureLoggedOut` (logout defensivo si quedó sesión de otro test).
2. El `logout` **final** va en el `finally`, **después** del teardown (§5.2): el borrado del expediente necesita la sesión abierta, así que cerrarla antes lo condena a fallar en silencio.
3. Entre tramos: `logout` del actor anterior y `login` del siguiente. **MUST NOT** encadenar dos `login` sin `logout` en medio.
4. El actor del tramo **final** (el que dispara el evento del campo `Evento`) **MUST** ser el del campo `Perfil`. Los tramos previos usan los actores que digan los `Given`.
5. **MUST** abrir el expediente por la **bandeja del perfil del tramo**: `CREADOR` → «Expedientes Pendientes»; `RESPONSABLE` → «Expedientes Esperando» (abiertos) o «Expedientes Cerrados» (cerrados). Entrar por la bandeja equivocada da la vista genérica de solo lectura y el test falla por un motivo falso.
6. **Un tramo cuyo perfil no sea `CREADOR` ni `RESPONSABLE`** (`SECRETARIO`, `DIRECTOR`, `AUDITOR`) **no tiene bandeja**: se entra por la pantalla que declare el propio trámite (`README.md` §3.2). Si no hay ninguna, devuelve `BLOQUEADO` (§9); **MUST NOT** entrar por la bandeja más parecida.

**El helper `_support/auth.ts` es test code, no código de la app** (§7): si sus selectores no casan con la UI real, **MUST** ajustarlo. **MUST NOT** tocar `src/main/...`.

---

## 5. Idempotencia OBLIGATORIA — por número de expediente

**CRITICAL** — la BD es **compartida y NO se resetea**: cada run deja expedientes nuevos del mismo trámite, con el mismo nombre y en los mismos estados. Un `.spec.ts` que pasa una vez pero falla al reejecutarse es un test **ROTO**.

1. **Captura el número del expediente que el test crea.** Al crearlo, la aplicación abre una pestaña titulada **`<número>-<nombre del tipo de expediente>`**; el número también aparece como «Num. Exped.» en las bandejas y en el popup «Ver el historial de estados». **MUST** guardarlo en una variable y usarlo para **localizar el expediente** en cualquier listado posterior.
   - ✅ CORRECTO: `const numero = (await page.getByRole('tab').last().textContent())!.split('-')[0].trim();` y después `page.getByRole('row', { name: new RegExp(numero) }).click();`
   - ❌ INCORRECTO: `page.getByRole('row').first().click()` (al segundo run hay varios expedientes y actúa sobre el equivocado), o localizarlo por el nombre del trámite (lo comparten todos).
2. **Teardown en `try/finally`**: si el estado en el que queda el expediente ofrece el evento `DELETE`, bórralo en el `finally` **aunque una aserción falle**. El borrado arrastra sus hijos (composición/orphanRemoval).
   **CRITICAL — el teardown necesita sesión y perfil.** `DELETE` es un evento como cualquier otro: se dispara desde el footer, exige **sesión abierta** y solo lo puede lanzar un usuario con el **perfil del estado** en el que quedó el expediente. Por eso el `logout` final va **después** del borrado (§4.2), y si el último tramo lo hizo otro actor, el teardown **MUST** volver a hacer `login` con el que tiene el turno antes de borrar.
   El `.catch(() => {})` que envuelve el teardown **tapa cualquier error**: con el `logout` delante, el borrado fallaría siempre sin que nadie se entere y la BD acumularía expedientes que el test creía haber borrado.
3. **Excepción documentada**: si en el estado final no hay `DELETE` (lo normal en un estado `closed`), el expediente **queda vivo**: es correcto y no rompe la idempotencia porque el test siempre trabaja con **su** número. **MUST** documentarlo en un comentario, y que el `.catch(() => {})` del teardown es intencional.
4. **MUST NOT** depender de que la bandeja esté vacía, ni contar filas, ni asertar totales de un listado: crecen con cada run.
5. **Datos poco contendidos**: si el test necesita un dato de demo compartido (un alumno, un centro), elige uno que otros tests no fijen y documenta en comentario por qué.

**MUST** comprobar la idempotencia ejecutando el test **2 veces seguidas** en verde **sin** limpiar la BD entre medias antes de devolver `ESCRITO`.

**Excepción — `Manual: sí`** (§6.1): no se puede ejecutar, así que tampoco se puede comprobar así. Las reglas de idempotencia (capturar el número, no usar `.first()`, teardown) **siguen siendo obligatorias**: se revisan leyendo el test, no ejecutándolo.

---

## 6. Qué asertar según el `Tipo`

| `Tipo` | Aserciones **obligatorias**, además de las de cada `Then`/`And` |
|---|---|
| `happy` | que **«Fase»** y **«Estado»** de la cabecera son los del campo `Hasta` (por su título visible) |
| `error` | **las dos**: que aparece el mensaje en el **recuadro rojo del footer**, **y** que «Fase»/«Estado» **siguen** siendo los de `Desde` |
| `solo-lectura` | **las tres**: que los campos **no son editables**, que el footer **no** ofrece el botón del evento (solo el de salir, si lo hay), y que «Fase»/«Estado» no cambian |
| `Hasta: [*]` (`DELETE`) | que el expediente **ya no aparece** en la bandeja, buscándolo por su número |

- **CRITICAL** — en un `Tipo: error` **MUST NOT** bastar con comprobar el mensaje: sin la aserción de que el expediente no transicionó, el test pasaría aunque la máquina de estados avanzase mal. El verificador lo marca `INFIEL`.
- **CRITICAL** — en un `Tipo: solo-lectura` **MUST NOT** bastar con la ausencia del botón: la mitad que da valor a la red de seguridad `X1` es que la vista sea **de verdad** de solo lectura. Asierta la no-editabilidad sobre un campo que en la vista del perfil **sí** sería editable (`toBeDisabled()`/`toHaveAttribute('readonly', …)` según cómo lo pinte la UI real; compruébalo pilotando), no sobre uno que es readonly en todas las vistas como «Fase» o «Estado».
- Para el **texto de un mensaje**, usa un locator tolerante (regex con las variantes es/ca) sobre el recuadro rojo, **sin** debilitar lo que se comprueba.

### 6.1 `Manual: sí` — el test lleva tag `@manual` y una puerta manual

Solo si el campo `Manual` de la cabecera es `sí` (`README.md` §3.4). El test se escribe **entero**: mismo camino, mismos tramos, **todas** las aserciones. Cambian **tres** cosas:

1. **Tag `@manual`** en las opciones del `test(...)`. Es lo que hace que la suite lo excluya por defecto (también en CI/CD).
2. **`test.setTimeout(...)`** amplio en la primera línea del test: el `timeout` global de `playwright.config.ts` (90 s) no da para que una persona firme.
3. **La puerta manual**, justo donde está el paso no automatizable: un comentario `// === PASO MANUAL ===` que diga **exactamente qué tiene que hacer la persona**, y una aserción con timeout largo que **espera al efecto** de ese paso en la UI. **MUST NOT** ser un `page.pause()` a secas ni un `waitForTimeout`: se espera a un **hecho observable**, para que el test siga fallando si la persona hace lo que no toca.

```ts
// T-NNN — <nombre del test>
// origen: ESC-NNN  |  <PERFIL> | <FASE>/<ESTADO> --<EVENTO>--> <FASE>/<ESTADO>  |  tipo: <happy|error|solo-lectura>
// MANUAL: <el motivo verbatim del campo `Manual` del .desc.md>
// Ejecutar con:  E2E_MANUAL=1 npx playwright test --grep @manual --headed
// fuente: .sdd/drafts/<iniciativa>/test-e2e-desc/t-NNN-<slug>.desc.md
test('<nombre del test>', { tag: '@manual' }, async ({ page }) => {
  test.setTimeout(600_000);   // la persona necesita su tiempo para el paso manual
  // … todo el camino automatizable, igual que un test normal …

  // === PASO MANUAL ===
  // <qué debe hacer la persona, en una frase: qué botón pulsa, con qué certificado>
  // El test continúa solo cuando el efecto es visible en la UI:
  await expect(page.getByLabel('Estado')).toHaveValue('<título del estado de llegada>', { timeout: 600_000 });

  // … el resto de aserciones, sin recortar ninguna …
});
```

- **MUST NOT** acortar el test "porque no se va a ejecutar en CI": es el único registro ejecutable de esa transición.
- **MUST NOT** usar `test.skip` / `test.fixme`: dan el test por muerto y el verificador los trata como señal de trampa. El tag lo excluye por defecto **y** deja poder lanzarlo.
- **MUST NOT** poner el tag a un test cuyo campo `Manual` sea `no`.
- **El motor NO ejecuta este test**: **MUST NOT** intentar dejarlo verde ni declarar que pasa. Devuelve `ESCRITO:` igual que siempre.

---

## 7. Profundidad del import y plantilla literal del `.spec.ts`

**CRITICAL — la profundidad NO es fija.** El test vive en `src/test/e2e/tramites/<tramite>/…/<vN>/`, así que `_support/` está a **tantos niveles arriba como segmentos tenga el destino tras `src/test/e2e/`**. Cuéntalos y escribe el import exacto; una profundidad copiada de otra plantilla deja el test rojo con «Cannot find module».

- ✅ CORRECTO: destino `src/test/e2e/tramites/mi_tramite/v1/` (3 segmentos) → `'../../../_support/auth'`
- ✅ CORRECTO: destino `src/test/e2e/tramites/mi_tramite/alumno/v2/` (4 segmentos) → `'../../../../_support/auth'`
- ❌ INCORRECTO: `'../../_support/auth'` (la profundidad de los tests de un sistema, no la de un trámite)

```ts
import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../../_support/auth';   // ← ajusta la profundidad al destino real

// T-NNN — <nombre del test>
// origen: ESC-NNN  |  <PERFIL> | <FASE>/<ESTADO> --<EVENTO>--> <FASE>/<ESTADO>  |  tipo: <happy|error|solo-lectura>
// fuente: .sdd/drafts/<iniciativa>/test-e2e-desc/t-NNN-<slug>.desc.md
test.describe('<nombre visible del trámite> — <FASE>', () => {
  test('<nombre del test, tal cual el título del .desc.md>', async ({ page }) => {
    let numero = '';
    try {
      // --- Tramo 1: <PERFIL del tramo> ---
      await ensureLoggedOut(page);
      await login(page, '<login>', '<contraseña>');

      // Given: <texto del bullet Given>
      // Crear el expediente desde el árbol de trámites
      await page.goto('/');
      // …navegar a Expedientes → Trámites y pulsar el nodo del trámite…
      numero = /* capturar el número del expediente creado (§5.1) */ '';

      // When: <texto del bullet When>
      // …rellenar los campos del juego de datos y pulsar el botón del footer…

      // Then: <texto del bullet Then>
      await expect(page.getByLabel('Fase')).toHaveValue('<título de la fase de llegada>');
      await expect(page.getByLabel('Estado')).toHaveValue('<título del estado de llegada>');
      // And: <texto de cada bullet And> → una aserción por bullet

    } finally {
      // Teardown: borrar el expediente si su estado final ofrece DELETE (§5.2);
      // si no lo ofrece, documentar aquí por qué queda vivo.
      // CRITICAL: el borrado necesita SESIÓN ABIERTA y el PERFIL del estado final,
      // así que va ANTES del logout y, si el último tramo fue de otro actor,
      // reautenticándose con el que tiene el turno.
      // …
      await logout(page).catch(() => {});
    }
  });
});
```

- Comentario con el texto del bullet **antes** de cada acción y antes de cada aserción.
- Locators por rol/label (`getByRole`, `getByLabel`), **nunca** IDs autogenerados de Axelor ni `waitForTimeout` (usa `expect(...).toBeVisible()`). Lo detalla `/k-playwright`.
- **«Fase» y «Estado» son campos readonly**: su contenido es el `value` de un input, no texto del DOM. **MUST** comprobarlos con `toHaveValue`, **nunca** con `toBeVisible`/`getByText`. Si en la UI real se pintan como texto, ajústalo pilotando; lo que **MUST NOT** hacerse es asertar sobre un elemento que siempre está.

---

## 8. Plantilla literal de `_support/auth.ts`

El motor **comprueba y crea** este helper al lanzar (Fase 2) y **valida su login/logout contra la app real una vez** antes de generar ningún test (Fase 4 §9.0). Por eso, al generar cada test, **asume que ya funciona** y solo corrígelo si aún ves un selector roto.

**CRITICAL — es COMPARTIDO por toda la suite** (`src/test/e2e/_support/auth.ts`), también con los tests de los sistemas: si ya existe, **MUST NOT** sobrescribirlo ni "adaptarlo a expedientes". Solo se crea si falta, y con esta plantilla literal:

```ts
import { Page, expect } from '@playwright/test';

const LOGIN_PATH = '/#/login';
const LOGIN_BTN = /Entrar|Sign in|Iniciar sesión|Iniciar sessió|Login/;
const USER_FIELD = /Usuario|Usuari/;
const PASS_FIELD = /Contraseña|Contrasenya|Password/;

async function enLogin(page: Page): Promise<boolean> {
  return await page.getByRole('button', { name: LOGIN_BTN }).isVisible().catch(() => false);
}

// Logout defensivo: si quedó sesión abierta, ciérrala. Deja la app en el login.
export async function ensureLoggedOut(page: Page): Promise<void> {
  await page.goto(LOGIN_PATH);
  // Esperar a que la SPA renderice antes de decidir: o aparece el login
  // (sesión cerrada) o la barra de usuario de Axelor (sesión abierta).
  // `isVisible()` es instantáneo y daría falsos negativos justo tras el goto.
  const loginBtn = page.getByRole('button', { name: LOGIN_BTN });
  const userMenu = page.getByRole('toolbar', { name: /User Menu/i });
  await loginBtn.or(userMenu).first().waitFor();
  if (!(await enLogin(page))) {
    await logout(page);
  }
  await expect(page.getByLabel(USER_FIELD)).toBeVisible();
}

export async function login(page: Page, usuario: string, contrasena: string): Promise<void> {
  await page.goto(LOGIN_PATH);
  await page.getByLabel(USER_FIELD).fill(usuario);
  await page.getByLabel(PASS_FIELD).fill(contrasena);
  await page.getByRole('button', { name: LOGIN_BTN }).click();
  await expect(page).toHaveURL(/#\/(?!login)/);
}

export async function logout(page: Page): Promise<void> {
  // Menú de usuario (arriba a la derecha) → Cerrar sesión.
  // El botón muestra las iniciales del usuario (dinámicas), así que se localiza
  // por la toolbar "User Menu" de Axelor, no por el nombre del botón.
  await page.getByRole('toolbar', { name: /User Menu/i }).getByRole('button').first().click().catch(() => {});
  await page.getByRole('menuitem', { name: /Cerrar sesión|Tancar sessió|Logout|Log out/i }).click().catch(() => {});
  await expect(page.getByRole('button', { name: LOGIN_BTN })).toBeVisible();
}
```

---

## 9. Cuándo devolver `BLOQUEADO`

**MUST** devolver `BLOQUEADO: {T-NNN} — {motivo}` (y **MUST NOT** inventar un test que finja pasar) cuando:

- el perfil del test **no tiene bandeja** por la que abrir el expediente y el trámite no ofrece otra pantalla;
- falta un recurso del entorno (un usuario de demo que no existe, un fichero adjunto que la descripción no describe);
- la app **no se comporta** como la descripción ya depurada espera (posible regresión): repórtalo con qué esperaba y qué hace.

**MUST NOT** devolver `BLOQUEADO` porque el test exija **firmar con AutoFirma** ni por ningún otro paso no automatizable: eso viene marcado `Manual: sí` en la cabecera y se materializa por §6.1. Si el paso es imposible y la cabecera dice `Manual: no`, genera el test hasta donde puedas y devuelve `BLOQUEADO` diciendo **exactamente eso**: que el diseño no lo marcó como manual.

---

## 10. Checklist del generador

**MUST NOT** terminar si queda algún punto sin cumplir. **LIMIT**: 3 iteraciones de autocorrección.

- [ ] ¿El `.spec.ts` tiene **el mismo nombre base** que el `.desc.md` y está en **la misma carpeta**?
- [ ] ¿El import de `_support/auth` tiene la **profundidad real** del destino (§7)?
- [ ] ¿Cada tramo hace `login`/`logout` con el actor correcto, y el tramo final con el del campo `Perfil`?
- [ ] ¿Se abre el expediente por la **bandeja del perfil** de cada tramo (§4.5), o por la pantalla que declare el trámite si ese perfil no tiene bandeja (§4.6)?
- [ ] ¿**NO** has modificado ningún `.spec.ts` hermano de la carpeta destino? Pueden ser de otra iniciativa: se reutilizan como referencia, nunca se editan (`README.md` §3).
- [ ] ¿El expediente se crea desde el **árbol de trámites** cuando `Desde: [*]`, y los tramos previos se recorren **por la UI**, sin atajos por REST?
- [ ] ¿El evento se dispara pulsando el **botón del footer por su título**, no guardando?
- [ ] ¿Hay una **aserción por cada bullet `Then` y `And`**, ninguna omitida ni debilitada?
- [ ] ¿Están las aserciones **obligatorias del `Tipo`** (§6): fase/estado de llegada en `happy`; mensaje **y** no-transición en `error`; ausencia de botones en `solo-lectura`; desaparición en `DELETE`?
- [ ] ¿El test **captura el número de expediente** y localiza el expediente por él en todos los listados (§5.1)?
- [ ] ¿Hay teardown en `try/finally`, o un comentario que explique por qué el expediente queda vivo (§5.2, §5.3)?
- [ ] ¿El borrado del teardown corre **con sesión abierta** y con el **perfil del estado final** (el `logout` va después, §5.2)?
- [ ] ¿Los locators son por rol/label, sin IDs autogenerados ni `waitForTimeout`?
- [ ] ¿Has comprobado que pasa **2 veces seguidas** en verde sin limpiar la BD? (no aplica si `Manual: sí`, §5)
- [ ] Si `Manual: sí`: ¿lleva el tag `@manual`, su `test.setTimeout(...)`, el comentario `MANUAL:` con el motivo y la **puerta manual** esperando a un hecho observable — y **ninguna** aserción recortada, ningún `skip`/`fixme` (§6.1)?
- [ ] Si `Manual: no`: ¿te has asegurado de **no** poner el tag `@manual` (lo excluiría de CI para siempre)?
- [ ] ¿Lleva los comentarios de trazabilidad (`// T-NNN`, `// origen: ESC-NNN`, la línea de transición, `// fuente: …`)?
- [ ] ¿`_support/auth.ts` existe y **no** lo has sobrescrito (§8)?
- [ ] ¿Has **cerrado la sesión de navegador** (`browser_close`) al terminar?
- [ ] ¿La respuesta es **exactamente** `ESCRITO: {ruta del .spec.ts}` (o `BLOQUEADO: {T-NNN} — {motivo}`)?
