# Contrato de plantilla — tests E2E de regresión de un tipo de expediente

Esta carpeta es el **contrato** que leen los subagentes de `/sdd-create-tests-e2e` cuando lo que se persiste son los tests de un **trámite y su tipo de expediente**. El `SKILL.md` es un motor agnóstico: **todo** lo específico de dónde van los tests, cómo se genera y cómo se sana un `.spec.ts` está aquí. Si cambias esta carpeta (o apuntas `--template-dir` a otra), cambias qué y cómo se genera **sin tocar el skill**.

Es la plantilla **hermana** de `template-system/`: mismo motor, mismos tres roles, misma gestión de la app. Cambian **la carpeta destino** (§3.1), **qué se pilota** (una máquina de estados recorrida por perfiles, §3.2) y **qué hace idempotente** a un test (§3.3 y `generation.md` §5).

---

## 0. REGLA DE GENERALIDAD — léela antes que nada

**CRITICAL.** Esta plantilla describe **el patrón**, nunca un trámite concreto.

- **MUST NOT** aparecer en la parte normativa el nombre de ningún trámite, fase, estado, evento, campo, perfil ni documento reales: se usan placeholders (`<tramite>`, `<Code>`, `<vN>`, `<FASE>`, `<ESTADO>`, `<EVENTO>`, `<PERFIL>`).
- **MUST NOT** escribirse ninguna regla que solo valga para un número fijo de fases, estados, eventos, documentos o perfiles.
- Todo ejemplo va en un bloque que empieza por `> **Ejemplo** (ilustrativo, NO normativo):`, con nombres inventados.

---

## 1. Ficheros de la plantilla

| Fichero | Lo lee | Para qué |
|---------|--------|----------|
| `README.md` (este) | los tres roles + el motor | índice del contrato, contexto del trámite y las tres secciones que ejecuta el motor: **«Carpeta destino»** (§3.1), **«Gestión de la app»** (§4) y **«Puerta de regresión»** (§6) |
| `generation.md` | **generador** | cómo convertir un `t-NNN-<slug>.desc.md` en su `.spec.ts`: los siete campos de cabecera, el ciclo de login/logout **por tramo**, la navegación real, la idempotencia por número de expediente, la plantilla del test —y la del test **manual** (§3.4)—, la trazabilidad y el checklist |
| `verification.md` | **verificador** | qué hace **fiel** a un test ya verde: una aserción por cada `Then`/`And`, la comprobación de **fase y estado** de llegada, el perfil correcto, sin atajos por REST ni aserciones debilitadas |
| `healing.md` | **sanador** | cómo diagnosticar y arreglar un `.spec.ts` rojo o declarado `INFIEL`, sin tocar el código ni los XML del trámite |

---

## 2. Roles

**CRITICAL — separación de poderes anti-trampa**: los tres roles corren en **contextos aislados**. El que **crea** el test (generador) **no** decide si vale; quien lo decide es el **runner mecánico** (lo ejecuta el motor) más un **verificador** que no escribió el test. Un test verde pero infiel (no comprueba la fase/estado de llegada, se salta un tramo) lo caza el verificador.

| Rol | Qué hace | Entrada propia | Lee de esta plantilla | Resultado |
|---|---|---|---|---|
| **generador** (§2.1) | **Genera** un `.spec.ts` desde su descripción, pilotando la app real. **No** declara si pasa | la ruta de **un** `t-NNN-<slug>.desc.md` (la copia en el destino) + la ruta destino del `.spec.ts` | `generation.md` (+ contexto §3) | el fichero `t-NNN-<slug>.spec.ts` hermano + token `ESCRITO:` |
| **verificador** (§2.2) | **Audita** de forma adversarial que un `.spec.ts` ya verde es **fiel** a su descripción. NO modifica el test | la ruta del `.spec.ts` (verde) y su `.desc.md` | `verification.md` (+ contexto §3) | token `OK:` (fiel) o `INFIEL: — {motivo}` |
| **sanador** (§2.3) | **Arregla** un `.spec.ts` rojo o declarado `INFIEL` | la ruta del `.spec.ts`, su `.desc.md` y el fallo (salida del runner o motivo `INFIEL`) | `healing.md` (+ contexto §3) | el `.spec.ts` corregido **en sitio** + token `CORREGIDO:`/`BLOQUEADO:` |

Los tres roles:

- **MUST** cargar el skill `/k-playwright` (convenciones del proyecto: estructura, locators, baseURL, `_support/auth.ts`, login común).
- **MUST NOT** modificar código Java/Kotlin (`src/main/...`), los XML del trámite (`TipoExpedienteInstance.xml`, `domains.xml`, `views.xml`, `documentospdf/`) ni la fuente en `.sdd/`.
- **MUST NOT** usar `AskUserQuestion`: ante un bloqueo, devuelven su token.
- **MUST NOT** pegar el contenido de los ficheros en la respuesta (ya está en disco): solo el token de estado.
- El **generador MUST NOT** declarar si el test pasa; el **verificador MUST NOT** editar el test; el **sanador MUST NOT** debilitar ni borrar aserciones.
- **CRITICAL** — al terminar, **MUST** cerrar su sesión de navegador con `browser_close`. Una sesión MCP de Playwright (o su Chromium headless) que queda viva al cerrarse el contexto del subagente **bloquea al siguiente subagente** durante minutos.

---

## 3. Contexto del proyecto y carpeta destino

- La app es una secretaría virtual sobre **Axelor 8.1**, servida en `http://localhost:8080/`; login en `http://localhost:8080/#/login`. El `baseURL` de `playwright.config.ts` ya es `http://localhost:8080`: **MUST** usar rutas relativas (`page.goto('/#/login')`).
- Convenciones de tests, locators y estructura: las define `/k-playwright` (cárgalo). Pares `t-NNN-<slug>.desc.md` ↔ `t-NNN-<slug>.spec.ts`, **mismo nombre base y misma carpeta**; helper compartido `src/test/e2e/_support/auth.ts`.
- La app es **multicentro y bilingüe (es/ca)**: los locators por texto asumen español salvo que el test diga lo contrario.
- **CRITICAL — la BD es compartida y NO se resetea entre ejecuciones**: cada run deja expedientes nuevos. Por eso cada `.spec.ts` **MUST** ser **idempotente** por el **número de expediente** que él mismo crea (§3.3 y `generation.md` §5). Un test que pasa una vez pero falla al reejecutarse está **roto**.
- **CRITICAL — la carpeta destino la comparten varias iniciativas.** Dos versiones del mismo trámite, o dos iniciativas sobre la misma versión, escriben en la misma carpeta (§3.1). Los `.spec.ts` hermanos que encuentres allí **pueden ser de otra iniciativa**: **reutilízalos como referencia** (login, navegación, locators de Axelor, patrón de idempotencia) —es lo que manda la «disciplina de tiempo» de `generation.md`—, pero **MUST NOT** modificarlos, renombrarlos ni "adaptarlos" a tu test. Tocar uno rompe un test ajeno, y la **puerta de regresión** lo detectará como una REGRESIÓN que nadie causó en el código, parando la pasada.

### 3.1 Carpeta destino (la resuelve el MOTOR en la Fase 1)

El destino de los tests de un **tipo de expediente** es el espejo de su **carpeta de versión**: `src/test/e2e/tramites/<tramite>/…/<vN>/`, espejo de `src/main/java/com/educaflow/tramites/<tramite>/…/<vN>/`. Los tests son **de la versión**, no del trámite: dos versiones del mismo trámite tienen carpetas de código distintas y por tanto carpetas de tests distintas, y los tests de `v1` **siguen siendo válidos** cuando nace `v2`.

Procedimiento:

1. Lee del `design/design.md` de la iniciativa, en la tabla de la sección **«Identidad del trámite y del tipo»**, la fila **`Carpeta de la versión`** (el dato que `/sdd-designer` obliga a poner en el diseño de un expediente: `sdd-designer/template-expediente/design-contract.md`). Su valor es una ruta desde la raíz del proyecto.
   ```bash
   grep -m1 '^| Carpeta de la versión |' {carpeta-iniciativa}/design/design.md
   ```
2. **MUST NORMALIZAR la celda antes de validar nada.** El diseño escribe el valor **entre backticks y con barra final** (`design-contract.md`), y la celda trae además los pipes y sus espacios. Quédate solo con la ruta: quita el `| Carpeta de la versión |` inicial y el `|` final, los espacios de los extremos, los **backticks** y la **barra final**.
   - ✅ CORRECTO: `` | Carpeta de la versión | `src/main/java/com/educaflow/tramites/mi_tramite/v1/` | `` → `src/main/java/com/educaflow/tramites/mi_tramite/v1`
   - ❌ INCORRECTO: validar la celda tal cual y abortar con un **ERROR falso** porque "no empieza por `src/main/java/`" (empieza por un backtick).
3. **MUST** validar que declara **una sola** ruta y que empieza por `src/main/java/com/educaflow/tramites/`. Un valor con varias rutas o fuera de `tramites/` falla aquí.
4. **MUST** validar que esa carpeta **existe** en el árbol y contiene un `TipoExpedienteInstance.xml`. Si no, el diseño declara una versión que no está en el código: **MUST NOT** continuar (falta ejecutar `/sdd-implementer`).
5. Compón el destino **sustituyendo el prefijo** `src/main/java/com/educaflow/` por `src/test/e2e/`, conservando **todos** los segmentos intermedios de agrupación.
6. Si el `design/design.md` no existe, no tiene esa fila, su valor no valida o la carpeta no existe → **ERROR** y detente. **MUST NOT** inventar la carpeta ni caer al nombre del draft.

- ✅ CORRECTO: `src/main/java/com/educaflow/tramites/mi_tramite/v1/` → `src/test/e2e/tramites/mi_tramite/v1/`
- ✅ CORRECTO (con segmentos de agrupación): `…/tramites/mi_tramite/alumno/v2/` → `src/test/e2e/tramites/mi_tramite/alumno/v2/`
- ❌ INCORRECTO: `src/test/e2e/tramites/mi_tramite/` (la carpeta del **trámite**, no la de la versión: mezclaría los tests de `v1` y `v2`)
- ❌ INCORRECTO: `src/test/e2e/subsystem/expedientes/` (el subsistema que tramita no es el artefacto que se prueba)
- ❌ INCORRECTO: `src/test/e2e/mi-tramite-nuevo/` (nombre del draft, no replica el código)

**CRITICAL — la profundidad del destino es variable.** A diferencia de un sistema, aquí hay **3 o más** niveles bajo `src/test/e2e/`. El import de `_support/auth.ts` **MUST** calcularse contando los segmentos reales del destino (`generation.md` §6); una profundidad copiada de otro test deja el `.spec.ts` rojo con «Cannot find module».

### 3.2 Qué es la UI de un expediente

Lo necesitan el generador (para pilotar) y el sanador (para diagnosticar):

- **Crear un expediente**: menú **«Expedientes» → «Trámites»**, desplegar el tipo de trámite y pulsar el **nodo del trámite**. Eso dispara el evento inicial y abre el formulario del estado inicial con el expediente ya creado. **No** hay botón «Nuevo» de un grid.
- **Abrir un expediente ya creado**: por una **bandeja**, y **cada bandeja fija el perfil** con el que se pinta la vista: «Expedientes Pendientes» → `CREADOR`; «Expedientes Esperando» → `RESPONSABLE`; «Expedientes Cerrados» → `RESPONSABLE`. Entrar por la bandeja equivocada da la vista genérica de **solo lectura**, sin botones.
- **CRITICAL — ver los botones no es poder disparar el evento.** La vista la elige el perfil de la **bandeja**; los perfiles que tenga el usuario no intervienen ahí. El perfil **real** del usuario se comprueba al **disparar** el evento: si no lo tiene, la app responde un **error de acceso** y el expediente **no** transiciona. Un test de solo lectura, por tanto, se monta eligiendo la **bandeja**, no el usuario.
- **CRITICAL — hay perfiles SIN bandeja propia.** El enum `Profile` tiene cinco valores y el diseño puede asignar cualquiera, pero solo `CREADOR` y `RESPONSABLE` tienen bandeja en el menú «Expedientes». A un estado cuyo `profile` sea `SECRETARIO`, `DIRECTOR` o `AUDITOR` se llega por **la pantalla que el propio trámite declare**; búscala en las vistas de la carpeta de la versión. Si el trámite no ofrece ninguna, no hay por dónde entrar: es un **fallo de diseño**, y se reporta con el token de tu rol (`BLOQUEADO`), **MUST NOT** entrarse "por la bandeja más parecida" — daría la vista de otro perfil y un rojo con causa falsa.
- **Cabecera** (panel «Información general»): campos **«Creado por»**, **«Fase»**, **«Estado»**, **«Fecha último estado»** y botón «Ver el historial de estados». **Es donde se comprueba a qué fase y estado llega el expediente**, por su título visible (no por el `UPPER_SNAKE_CASE`).
- **Footer**: un botón por evento disponible. **El título del botón es lo que se pulsa; su `name` es el `<EVENTO>`.** Los mensajes de validación salen en un **recuadro rojo** del footer (lista de mensajes con el título del campo en negrita), **no** como toast ni modal.
- **`EXIT` y `DELETE` recargan la aplicación entera** (`refresh-app`): tras ellos hay que volver a navegar desde el menú.
- **La firma en cliente con AutoFirma NO es automatizable** (exige certificado y aplicación de escritorio en la máquina del usuario). Un test así llega marcado `[-]` en el índice y se materializa por la **vía manual** (§3.4): **MUST NOT** devolverse `BLOQUEADO` por ese motivo, ni intentar pilotarla, ni simularla.

### 3.3 Qué hace idempotente a un test de expediente

No hay "nombres únicos": lo que identifica a lo creado es el **número de expediente**, que asigna el servidor. Regla base (el detalle, en `generation.md` §5):

1. El test **MUST** capturar el número del expediente que crea y usarlo para **localizarlo** en cualquier listado posterior.
2. **MUST NOT** localizar el expediente como "el primero de la bandeja" ni por el nombre del trámite: al segundo run hay varios y el test se cuelga o actúa sobre el equivocado.
3. El teardown borra el expediente **solo si** el estado en el que queda ofrece el evento `DELETE`; si no, se documenta en comentario por qué queda vivo.

### 3.4 Tests manuales — los que necesitan a una persona

Un test que el índice de entrada marcó `- [-]` (su descripción lo declara `Manual: sí`) tiene algún paso que **ninguna automatización puede ejecutar**. El motor lo materializa igual, pero **sin ejecutarlo**. Lo que esta plantilla fija:

| Qué | Cómo |
|---|---|
| **Marca en el `.spec.ts`** | el tag `@manual` en las opciones del `test(...)`, más un comentario de cabecera con el motivo (`generation.md` §6.1) |
| **Cómo se ejecuta** | `E2E_MANUAL=1 npx playwright test --grep @manual --headed` — con una persona delante que realiza el paso manual |
| **Qué pasa en CI/CD** | **nada**: `playwright.config.ts` lleva `grepInvert: /@manual/` salvo que se pase `E2E_MANUAL=1`, así que la suite los excluye **por defecto**, sin depender de que nadie recuerde un flag |
| **Puerta de regresión** (§6) | por lo anterior, la suite completa **no** los ejecuta: no pueden ponerla roja ni colgarla |

- **MUST NOT** usar el tag `@manual` en un test que sí se puede automatizar: quedaría fuera de CI para siempre y nadie lo notaría.
- **MUST NOT** usar `test.skip`/`test.fixme` en su lugar: un test así se marca como manual y se **puede** ejecutar; `skip`/`fixme` lo dan por muerto y el verificador los trata como señal de trampa.

---

## 4. Gestión de la app (la ejecuta el MOTOR, no los subagentes)

El motor deja la app respondiendo `200` antes de generar el primer test y la para al terminar. **MUST** seguir estos comandos al pie de la letra.

### 4.1 Comprobar si está levantada

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080
```

### 4.2 Limpiar el puerto 8080 (antes de arrancar)

**CRITICAL**: una instancia previa colgada hace que el connector falle el bind en silencio. Limpia de verdad, **excluyendo** IntelliJ y similares:

```bash
fuser -k 8080/tcp 2>/dev/null || lsof -ti tcp:8080 | xargs -r kill
pkill -f 'TomcatRunner|GradleWrapperMain.*run' 2>/dev/null
ss -ltn | grep ':8080' || echo "8080 libre"
```

### 4.3 Arrancar (tarea tracked en segundo plano)

Usa **siempre** `./run.sh`. Lánzalo con `Bash`, `run_in_background: true` y `dangerouslyDisableSandbox: true` (`run.sh` escribe en `~/.gradle`, fuera del sandbox), redirigiendo el log:

```bash
exec ./run.sh > src/test/e2e/.app.log 2>&1
```

**Sondea** hasta `200` con margen amplio: repite el `curl` de §4.1 con `Monitor`/reintentos, **LIMIT** de sondeo ~420 s por ventana, varias ventanas si hace falta.

**CRITICAL — el arranque de un trámite compila más cosas**: `GenerateStatesTask` proyecta la clase `States` desde el `TipoExpedienteInstance.xml` y `viewProcessorTask` preprocesa los `views.xml` de cada fase. Un fallo de cualquiera de los dos sale como `BUILD FAILED` en el log **antes** de levantar. Y un `<object-views>` sin hijos **tumba el arranque sin `BUILD FAILED`**: el síntoma es «The content of element 'object-views' is not complete» y una app en pie **sin vistas ni menús**.

### 4.4 Ejecutar un `.spec.ts` (lo hace el motor tras el generador / tras cada `CORREGIDO`)

```bash
npx playwright test {ruta del .spec.ts} --project=chromium --reporter=line
```

Exit code `0` = **PASS**; distinto de `0` = **FAIL** (pasa la salida al sanador). La app **MUST** estar en `200` antes de ejecutar.

**CRITICAL — `--reporter=line` es obligatorio aquí**: el reporter `html` del `playwright.config.ts` tiene `open: 'on-failure'`, así que al fallar **arranca el servidor del informe y deja el comando colgado**. `--reporter=line` lo evita.

### 4.5 Parar (al terminar, §10 del skill)

Siempre **por puerto**, nunca por handle de proceso:

```bash
fuser -k 8080/tcp 2>/dev/null || lsof -ti tcp:8080 | xargs -r kill
```

> El log `src/test/e2e/.app.log` es del motor; no se commitea y los subagentes lo ignoran.

### 4.6 Limpiar sesiones de navegador huérfanas (entre subagentes)

**CRITICAL**: un Chromium headless o un worker de Playwright **huérfano** bloquea la sesión MCP del siguiente generador/sanador durante minutos. El motor **MUST** barrerlos **antes de lanzar cada generador**. **MUST NOT** matar el **server MCP** de Playwright (`run-test-mcp-server`), solo los procesos de navegador/worker de test:

```bash
pkill -9 -f 'workerProcessEntry|chrome-headless-shell' 2>/dev/null; true
```

---

## 5. Cabecera-banner del snapshot (la escribe el MOTOR en la Fase 2)

Al copiar un `t-NNN-<slug>.desc.md` de `test-e2e-desc/` al destino, el motor **antepone** este bloque **justo después del frontmatter** (para no romper `type:`/`id:`), dejando el resto del contenido **verbatim**:

```markdown
<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/{carpeta-iniciativa}/test-e2e-desc/{fichero}.desc.md
     Iniciativa: {carpeta-iniciativa}
     Test: {T-NNN}  |  Origen ESC: {ESC-NNN, leído de la línea "Origen ESC:" del propio fichero}
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->
```

**CRITICAL — `Iniciativa:` es parte de la identidad del test, no decoración.** Varias iniciativas comparten esta carpeta (una modificación de la versión escribe donde ya hay tests de quien la creó), y `T-NNN` y `ESC-NNN` son **locales a cada iniciativa**: sin este campo, dos tests distintos con el mismo `T-001`/`ESC-001` se confundirían y el nuevo se descartaría como "ya materializado". Su valor es el **nombre de la carpeta** de la iniciativa, sin `.sdd/drafts/` ni barra final. **MUST NOT** omitirse ni abreviarse.

**Variante para un test MANUAL** (el que venía `- [-]` en el índice, §3.4). El banner de arriba afirma que la descripción **pasó** al depurar, y en un test manual eso es **falso**: nadie lo ha ejecutado. Para esos, el motor sustituye la segunda línea por estas dos:

```markdown
     Snapshot NO VERIFICADO: este test es MANUAL y no se ha ejecutado nunca de forma desatendida.
     Su .spec.ts lleva el tag @manual; se lanza con: E2E_MANUAL=1 npx playwright test --grep @manual --headed
```

- ✅ CORRECTO: el banner va entre el `---` de cierre del frontmatter y el `# T-NNN — …`.
- ❌ INCORRECTO: ponerlo **antes** del frontmatter (rompería el parseo de `type:`/`id:`), o reescribir el cuerpo del test.
- ❌ INCORRECTO: dejarle a un test manual el banner «as-tested» — el fichero afirmaría una verificación que no ha ocurrido.

---

## 6. Puerta de regresión (la ejecuta el MOTOR, tras persistir los tests nuevos)

Tras dejar verdes y verificados todos los tests de la iniciativa (y **antes** de parar la app, §4.5), el motor **MUST** ejecutar **toda** la suite E2E persistida — la de esta versión, la de otras versiones y la de los sistemas:

```bash
npx playwright test src/test/e2e --project=chromium --reporter=line
```

**CRITICAL — una versión NUEVA nunca superseda; una MODIFICACIÓN in situ sí puede.** Son los dos únicos casos, y se distinguen por el `design.md`:

- Una versión nueva (`<vN+1>`) vive en **su propia carpeta** de código y de tests, así que **no invalida** los tests de la anterior. Su `design.md` **no** lleva subsección de supersedidos.
- Una **modificación in situ** de una versión existente (el `design.md` trae la fila «Modificación de» en su sección «Identidad del trámite y del tipo») cambia la versión **bajo los tests que ya existen de ella**, así que puede invalidar alguno **a propósito**. Esos —y solo esos— los declara su `design.md` en la subsección `### Tests E2E supersedidos` de `## 13. Tests` (`sdd-designer/template-expediente/design-contract.md` §15.3), cada línea con la ruta del `.spec.ts`, el ID de spec que lo invalida y el motivo.

Por cada test **de otra iniciativa** que salga rojo:

- Si su ruta figura en `### Tests E2E supersedidos` del `design.md` de **esta** iniciativa → el delta lo invalidó a propósito: el motor **retira** el par (`git rm` del `.desc.md` y del `.spec.ts`) y lo lista en el informe final como "supersedido por {ID de spec}".
- Si **no** figura → es una **REGRESIÓN**: el motor **MUST** reportarlo al usuario y **parar**, sin retirar el test y sin tocar código. Salidas: `/sdd-debug-with-test-e2e-desc` si el arreglo es de código, `/sdd-designer` en modo Revisar si el fallo es del diseño, o declarar el superseding en el diseño si el cambio de comportamiento era intencionado y se olvidó declarar.

**MUST NOT** retirar un par que no esté declarado, ni siquiera "porque ya hay una versión nueva": es exactamente lo que oculta una regresión. Si no hay `design.md` (modo `--out=`, sin draft completo) no existe la subsección: **MUST** tratar **cualquier** rojo ajeno como REGRESIÓN y **MUST NOT** retirar nada.

Un test rojo **de esta misma iniciativa** en este punto no debería existir (todos pasaron el bucle generar→verificar→sanar); si ocurre, trátalo como FAIL normal del bucle (§4.4).

- ✅ CORRECTO: un `t-0NN-*.spec.ts` de la `v1` sale rojo tras implementar la `v2` → **REGRESIÓN**: reportar y parar (la `v2` no debería haber tocado la `v1`; su `design.md` no puede supersedir tests de otra versión).
- ✅ CORRECTO: `t-004-rechazo-sin-motivo.spec.ts` de la `v1` sale rojo tras una **modificación** de la `v1` que hace obligatorio el motivo, y está listado como supersedido por `VAL-012` → retirar el par y reportarlo.
- ❌ INCORRECTO: retirar un rojo ajeno que **no** está declarado como supersedido, o dar el skill por terminado sin ejecutar la suite completa.
