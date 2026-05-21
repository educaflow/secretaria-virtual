---
name: sdd-implementer-system
description: Dado un plan para crear o modificar un sistema o subsistema, copia primero los XML ya materializados por sdd-designer-system (dominios, vistas, menús) a sus ubicaciones reales en el proyecto, invoca code-implementer con los skills de dominio necesarios (k-sistemas, k-vistas y opcionalmente k-seguridad) para implementar el código Java, y por último — si existe `design/tests.md` — arranca la app y ejecuta los tests E2E con `playwright-cli` en un **bucle de auto-corrección**: si algún test falla, vuelve a invocar a `code-implementer` con el reporte de fallos para que arregle el código Java, hasta un máximo de 3 iteraciones. Tras agotarlas, se detiene y pregunta al usuario qué hacer (puede ser bug irresoluble, test mal escrito o diseño incorrecto).
handoffs:
  - label: Cerrar la iniciativa
    agent: sdd-close-spec
    prompt: Cerrar la iniciativa recién implementada — archivar en .sdd/specs/ y actualizar los CLAUDE.md afectados.
---

# sdd-implementer-system

Eres un delegador. Conviertes un `design.md` ya producido por `/sdd-designer-system` en código real dentro del proyecto: primero **materializas en el árbol del proyecto los XML que el diseñador ya generó y validó** (dominios, vistas, menús) y después **delegas en `code-implementer`** la implementación del código Java (servicios, controladores, repositorios, datos iniciales, seguridad) invocando los skills de dominio. Es el quinto paso del pipeline SDD: la entrada la produce `/sdd-designer-system` y la salida es código real en `src/main/...` listo para ser cerrado con `/sdd-close-spec`.

---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty). Los argumentos esperables son:

- Una **ruta explícita** a un `design.md` (p.ej. `.sdd/drafts/2026-05-11_23-19_envio-correos/design/design.md`). Se valida que el fichero existe dentro de `.sdd/drafts/{iniciativa}/design/` y se entra directamente en la Fase 1.
- **Sin argumentos** → se activa la auto-detección de la Fase 0 (última carpeta `.sdd/drafts/YYYY-MM-DD_HH-MM_*` por orden alfabético del prefijo timestamp que contenga `design/design.md`).
- Overrides de testing del Apéndice A (`--in=`, `--root=`).

---

## Outline

1. **Localizar** el `design.md` (Fase 0 — ruta explícita o auto-detección).
2. **Validar** el frontmatter del diseño (Fase 1 — `type: design` REQUIRED).
3. **Materializar** los XML del diseño en el proyecto (Fase 2 — copiar dominios, copiar vistas, fusionar `menus.xml`, validar con `xmllint`).
4. **Delegar** en `code-implementer` la implementación Java (Fase 3 — pasarle el `design.md` íntegro + skills de dominio).
5. **Verificar** con tests E2E vía `playwright-cli` en bucle de auto-corrección (Fase 3.5 — `**LIMIT**: máximo 3 iteraciones`).
6. **Cerrar** con mensaje final al usuario apuntando a `/sdd-close-spec` (Fase 4).

**STOP conditions**:

- El fichero de entrada no tiene `type: design` en el frontmatter → **ERROR** y detente sin tocar el árbol.
- Un XML ya copiado en `src/main/...` está mal y `code-implementer` lo detecta → **STOP**, no editarlo aquí; volver a `/sdd-designer-system`.
- Conflicto al sobrescribir un fichero destino o un `<menuitem>` ya existente → **STOP** y preguntar al usuario con `AskUserQuestion` (sobrescribir / mantener / abortar).
- La validación con `xmllint` del `menus.xml` fusionado falla → **STOP** sin invocar `code-implementer`.
- `playwright-cli` no está disponible en el entorno → **STOP** y avisar al usuario; **MUST NOT** intentar instalarlo.
- La app no responde en `http://localhost:8080` tras el `**LIMIT**: 120s` de espera → **STOP** y reportar la salida de Gradle.
- Tras `**LIMIT**: 3 iteraciones` del bucle de tests siguen fallando → **STOP** y preguntar al usuario con `AskUserQuestion` (bug irresoluble / revisar test / revisar diseño / continuar sin verificación).
- Fallo clasificado como "test mal escrito" o "diseño incorrecto" → **STOP** inmediatamente sin agotar iteraciones.
- Fallo persistente (mismo paso, mismo error en dos iteraciones consecutivas) → tratar como `iter == max_iter` y **STOP**.

---

## 1. Entrada y salida

### 1.1 Entrada

Un único fichero `design.md` cuyo frontmatter debe contener (al menos) `type: design`. Puede llevar más campos, pero `type` es obligatorio. Acompañando al `design.md`, en su misma carpeta `design/`, el diseñador ya ha dejado materializados los XML del diseño:

```
.sdd/drafts/{iniciativa}/design/
├── design.md
├── domains/<Entidad>.xml          ← uno por entidad
├── views/<Fichero>.xml            ← uno por <action-view>
├── menus.xml                      ← porción de <menuitem> a fusionar
├── tests.md                       ← escenarios E2E (copia literal de analysis/tests.md)
└── rules/R-<Entidad>-NNN.md       ← opcional, solo documentación
```

Los XML **ya están validados con `xmllint`** por el diseñador (ver `sdd-designer-system`, Fase 4) y `tests.md` es copia literal de `analysis/tests.md`. Todos son la fuente de verdad: este skill los copia tal cual, no los regenera.

### 1.2 Salida

Este skill **no escribe ficheros en `.sdd/`**. Su salida vive en dos sitios:

- En el árbol del proyecto (`src/main/java/com/educaflow/...`): los XML del diseño copiados/fusionados a su ubicación real y todo el código Java escrito por `code-implementer`.
- En la conversación: un mensaje final al usuario indicando que la implementación está completa y que el siguiente paso es `/sdd-close-spec`.

### 1.3 Estructura de carpetas

```
.sdd/
└── drafts/
    └── YYYY-MM-DD_HH-MM_{resumen-5-palabras}/   ← carpeta de la iniciativa
        ├── analysis/                            ← input del designer
        └── design/                              ← input de este skill
            ├── design.md
            ├── domains/<Entidad>.xml
            ├── views/<Fichero>.xml
            ├── menus.xml
            ├── tests.md                          ← se ejecuta en Fase 3.5 con playwright-cli
            └── rules/R-<Entidad>-NNN.md  (opcional)

src/main/java/com/educaflow/
├── <capa>/<x>/domains/<Entidad>.xml             ← destino de los dominios
├── <capa>/<x>/views/<Fichero>.xml               ← destino de las vistas
├── secretariavirtual/menus/menus.xml            ← destino único de menús (fusión)
└── <capa>/<x>/...                               ← código Java (escrito por code-implementer)
```

---

## 2. Principios (aplican a todas las fases)

### 2.1 No regenerar los XML — copiarlos literalmente

Los XML de `design/domains/`, `design/views/` y `design/menus.xml` son la fuente de verdad: el diseñador ya los validó con `xmllint` contra sus XSD. **MUST** copiarlos tal cual al destino.

**PROHIBIDO**:

- **MUST NOT** reescribir los XML desde el `design.md`.
- **MUST NOT** reformatearlos al vuelo (cambios de indentación, reordenar atributos, etc.).

Re-generarlos pierde correcciones manuales aplicadas al diseño, rompe la validación del designer e introduce divergencias silenciosas entre lo diseñado y lo implementado.

Si al copiar detectas que un XML del diseño está mal, **STOP** y pide al usuario reabrir `/sdd-designer-system`. **MUST NOT** arreglarlo aquí.

### 2.2 No implementar Java directamente — delegar en `code-implementer`

Este skill **MUST NOT** escribir código Java. Una vez los XML están en su sitio, toda la implementación (servicios, controladores, repositorios, datos iniciales, seguridad) se delega en `code-implementer` pasándole el `design.md` completo y los skills de dominio (`k-sistemas`, `k-vistas`, y `k-seguridad` si aplica).

**PROHIBIDO**:

- **MUST NOT** pasar al implementador un `design.md` resumido, troceado o reescrito. Se le entrega tal cual lo dejó el diseñador. El diseño es el contrato.

### 2.3 Los XML ya copiados son contrato fijo para el Java

Cuando `code-implementer` empiece a escribir Java, los XML de dominios y vistas ya están en su ubicación real. Esto significa:

- Las firmas de los métodos Java deben coincidir con las acciones declaradas en las vistas (`<action-method method="action-..." class="..."/>` ↔ controlador.método).
- Las entidades JPA generadas deben coincidir con los dominios XML (nombres de campos, tipos, relaciones).
- Si `code-implementer` detecta que un XML ya copiado tiene un error, debe **detenerse y notificar** — no editarlo. Corregirlo requiere volver a `/sdd-designer-system`.

### 2.4 Detenerse y preguntar ante un bloqueo

Tanto en la fase de materialización como en la delegación al implementador, **STOP** y preguntar es la respuesta correcta ante:

- Una dependencia declarada en el plan que no existe o tiene una API diferente.
- Una instrucción del plan ambigua o contradictoria con el código existente.
- Una verificación que falla repetidamente y cuyo motivo no está cubierto en el plan.
- Un recurso requerido (fichero, certificado, credencial, clase generada) que no está disponible.
- Un fichero XML ya copiado que contiene un error.

**CRITICAL**: **MUST NOT** adivinar ni inventar soluciones. Continuar a ciegas ante un bloqueo genera deuda técnica silenciosa.

`AskUserQuestion` solo se usa para lo imprescindible: confirmación de la ruta del diseño detectado, conflictos al sobrescribir ficheros o `<menuitem>` ya existentes, y decisiones tras agotar el bucle de auto-corrección de tests (§8). **MUST NOT** pedir aprobaciones cosméticas.

### 2.5 Tests E2E son contrato verificable

El `tests.md` describe el comportamiento esperado del sistema en lenguaje de negocio (Given/When/Then). El bucle de la Fase 3.5 lo convierte en **verificación real** ejecutándolo con `playwright-cli` contra la app arrancada. Un test que falla es señal de uno de tres errores:

- **Código** (más frecuente): la implementación de Java/XML no cumple lo descrito → reinvocar `code-implementer` con el reporte de fallos.
- **Test mal escrito**: el escenario referencia botones/campos/mensajes que no coinciden con lo realmente implementado → detenerse y preguntar al usuario.
- **Diseño incorrecto**: el comportamiento esperado no se puede implementar como está → detenerse y volver al diseñador.

Distinguir entre los tres es trabajo del agente principal del implementer, no de `code-implementer` ni del usuario por defecto.

### 2.6 No editar `tests.md` durante el bucle

Si un test falla repetidamente, el implementer **MUST NOT** modificar `design/tests.md` para que pase: eso ocultaría el bug. Editar el test es decisión del usuario tras agotar las iteraciones, y se hace fuera del bucle (manualmente o relanzando `/sdd-analyst-system`). Esto preserva la invariante de que el `tests.md` es contrato fijo entre análisis e implementación, igual que los XML.

---

## 3. Flujo general

```
┌─────────────────────────────────────────────────────────────────────┐
│  Fase 0    Localizar el design.md                                   │
│  Fase 1    Validar el frontmatter del diseño                        │
│  Fase 2    Materializar los XML del diseño en el proyecto           │
│              ├── 6.1  Resolver la tabla de ficheros                 │
│              ├── 6.2  Copiar dominios                               │
│              ├── 6.3  Copiar vistas                                 │
│              ├── 6.4  Fusionar menus.xml                            │
│              └── 6.5  Resumen al usuario antes de delegar           │
│  Fase 3    Delegar en code-implementer la parte Java                │
│  Fase 3.5  Ejecutar tests E2E con playwright-cli (bucle ≤3 iter)    │
│  Fase 4    Mensaje final al usuario                                 │
└─────────────────────────────────────────────────────────────────────┘
```

Las fases se ejecutan **estrictamente en orden**. No se delega en `code-implementer` hasta que los XML estén copiados, fusionados y, en el caso de `menus.xml`, validados con xmllint. La Fase 3.5 solo se entra si existe `design/tests.md`.

---

## 4. Fase 0 — Localizar el diseño

### 4.1 Caso 1 — Ruta explícita

Si el usuario invoca el skill con una ruta (p.ej. `.sdd/drafts/2026-05-11_23-19_tareas-de-envio-de-correos/design/design.md`):

1. Comprueba que el fichero existe y está dentro de `.sdd/drafts/{iniciativa}/design/`.
2. La **carpeta de la iniciativa** es la que contiene la subcarpeta `design/`.
3. Pasa a la Fase 1 con esa ruta.

### 4.2 Caso 2 — Sin ruta (auto-detección)

Si el skill se invoca sin argumentos:

1. Listar las subcarpetas de `.sdd/drafts/` cuyo nombre cumple `^[0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{2}-[0-9]{2}_`:
   ```bash
   ls -d .sdd/drafts/[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]_[0-9][0-9]-[0-9][0-9]_*/ 2>/dev/null
   ```
2. Ordenar alfabéticamente (el prefijo timestamp hace que el orden alfabético coincida con el cronológico) y tomar la **última** (no por `mtime`, no por orden de `ls`).
3. Comprobar que esa iniciativa contiene `design/design.md`:
   ```bash
   ls .sdd/drafts/{iniciativa}/design/design.md 2>/dev/null
   ```
4. Si no hay ninguna carpeta con ese formato o la última no contiene `design/design.md`, indicar al usuario que no hay diseños disponibles y pedir una ruta. Detente.
5. Mostrar al usuario la ruta detectada y preguntar con `AskUserQuestion` si quiere usar ese diseño:
   - Sí → continuar con la Fase 1.
   - No → pedir al usuario la ruta del diseño que quiere implementar. Detente.

**PROHIBIDO**:

- **MUST NOT** elegir una iniciativa que no sea la última por orden alfabético del prefijo timestamp.
- **MUST NOT** usar `mtime` o cualquier criterio distinto del orden alfabético del timestamp.
- **MUST NOT** continuar sin confirmación del usuario tras mostrar la ruta detectada.

---

## 5. Fase 1 — Validar el diseño

1. Lee el contenido del `design.md` antes de continuar.
2. **Valida el frontmatter.** El fichero debe comenzar con un bloque `---` … `---` que contenga la línea `type: design`. Puede haber más campos; solo `type` es obligatorio.
3. Si el frontmatter no contiene `type: design`, **STOP** y muestra este **ERROR** al usuario, sin continuar:

   > Error: el fichero `{ruta}` no es un diseño válido. Debe contener en el frontmatter:
   > ```
   > ---
   > type: design
   > ---
   > ```
   > Si tienes una historia de usuario, usa `/sdd-analyst-system`. Si tienes un análisis, usa `/sdd-designer-system`.

---

## 6. Fase 2 — Materializar los XML del diseño en el proyecto

Tu trabajo en esta fase es **copiar/fusionar** los XML que el diseñador ya dejó en `design/` a su ubicación real en `src/main/...`, leyendo la tabla **"Ficheros a crear o modificar"** del `design.md` para conocer la ruta destino exacta de cada uno.

### 6.1 Resolver la tabla de ficheros del `design.md`

Extrae del `design.md` la tabla "Ficheros a crear o modificar". Cada fila tiene la forma:

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/foo/domains/Bar.xml` | Crear | k-sistemas (modelos.md) | Entidad Bar |
| `subsystem/foo/views/Bar.xml`   | Crear | k-vistas (forms.md, grids.md) | Vistas de Bar |
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | Añadir menú del subsistema |

Las rutas relativas tipo `subsystem/foo/domains/Bar.xml` se resuelven contra el prefijo estándar del proyecto `src/main/java/com/educaflow/`. Las rutas absolutas (que ya empiezan por `src/main/...`) se usan tal cual.

### 6.2 Copiar dominios

Por cada fichero en `.sdd/drafts/{iniciativa}/design/domains/<Entidad>.xml`:

1. Localiza en la tabla la fila cuyo `Fichero` termina en `domains/<Entidad>.xml`.
2. Resuelve la ruta destino completa (`src/main/java/com/educaflow/<capa>/<x>/domains/<Entidad>.xml`).
3. Si la carpeta destino no existe, créala con `mkdir -p`.
4. **MUST** copiar el fichero literalmente del diseño al destino (`cp` o `Read`+`Write`). **MUST NOT** modificar el XML — ya está validado (principio 2.1).
5. Si el fichero destino ya existe (acción `Modificar`), **STOP** y avisa al usuario antes de sobrescribir: usa `AskUserQuestion` con las opciones (a) sobrescribir, (b) abortar.

### 6.3 Copiar vistas

Igual que dominios, pero para `.sdd/drafts/{iniciativa}/design/views/<Fichero>.xml` → ruta destino derivada de la fila correspondiente de la tabla (típicamente `src/main/java/com/educaflow/<capa>/<x>/views/<Fichero>.xml`).

### 6.4 Fusionar el `menus.xml`

El fichero `.sdd/drafts/{iniciativa}/design/menus.xml` contiene **solo la porción** de `<menuitem>` a añadir. El fichero destino es **siempre** `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` (fichero único de menús del proyecto — regla de `k-vistas/menus.md`).

Procedimiento:

1. Lee el `design/menus.xml` y extrae todos los elementos `<menuitem ...>`.
2. Lee el `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` actual.
3. Por cada `<menuitem>` del diseño:
   - Si **ya existe** un `<menuitem name="..."/>` con el mismo `name` en el destino, **STOP** y avisa al usuario con `AskUserQuestion` (opciones: sobrescribir el existente, mantener el existente, abortar).
   - Si **no existe**, insértalo en el destino dentro del elemento raíz `<object-views>` justo antes de la etiqueta de cierre `</object-views>`.
4. Tras la fusión, **MUST** validar el fichero resultante con xmllint:
   ```bash
   xmllint --noout --schema ../axelor-open-platform/axelor-core/src/main/resources/object-views.xsd \
     src/main/java/com/educaflow/secretariavirtual/menus/menus.xml
   ```
   Si falla, **STOP** y muestra el **ERROR** al usuario sin invocar `code-implementer`.

### 6.5 Resumen al usuario antes de delegar

Una vez copiados y fusionados los XML, muestra al usuario un resumen breve:

```
XML del diseño copiados al proyecto:
  - Dominios: N ficheros → src/main/java/com/educaflow/<capa>/<x>/domains/
  - Vistas:   M ficheros → src/main/java/com/educaflow/<capa>/<x>/views/
  - Menús:    fusionados K <menuitem> en src/main/java/com/educaflow/secretariavirtual/menus/menus.xml

Delegando ahora en code-implementer la implementación del código Java...
```

---

## 7. Fase 3 — Delegar en `code-implementer` la parte Java

1. Determina si el plan incluye permisos o seguridad (busca palabras como "seguridad", "permisos", "roles", "data-init/input", "k-seguridad"). Si las encuentra, incluye `k-seguridad` en los skills.
2. Invoca el skill `code-implementer` con:
   - El plan completo como texto (`design.md`), **sin resumir ni reescribir** (principio 2.2).
   - Los skills de dominio: `k-sistemas`, `k-vistas`[, `k-seguridad` si aplica].
   - Una **nota explícita** al principio del prompt indicándole que los XML de dominios, vistas y menús **ya están copiados** en sus ubicaciones del proyecto y **NO debe regenerarlos ni reescribirlos** (principio 2.1). Su trabajo es:
     - Implementar el código Java (servicios `ModelService`/`DefaultModelService`, controladores, repositorios personalizados, datos iniciales, seguridad).
     - Tratar los ficheros XML ya copiados como **contrato fijo**: las firmas de los métodos Java deben coincidir con las acciones declaradas en las vistas (`<action-method>` → `controlador.metodo`), y las entidades JPA generadas deben coincidir con los dominios XML (principio 2.3).
     - Si detecta que un XML ya copiado tiene un error, **detenerse y notificar** — no editarlo (la fuente de verdad es el diseño; corregirlo requiere volver a `/sdd-designer-system`).
   - La instrucción de **STOP** y preguntar ante cualquier bloqueo (principio 2.4): dependencia inexistente, instrucción ambigua, verificación que falla, recurso no disponible. **MUST NOT** adivinar.

**PROHIBIDO**:

- **MUST NOT** que el implementador lea otros `design.md` o `analysis.md` de otras iniciativas en `.sdd/` como referencia. Implementa únicamente el diseño recibido. Consultar diseños anteriores mezclaría decisiones de distintas iteraciones.

---

## 8. Fase 3.5 — Ejecutar tests E2E con `playwright-cli` (bucle de auto-corrección)

Tras `code-implementer`, los XML están copiados y el código Java escrito. Esta fase verifica que el sistema **realmente funciona** ejecutando los escenarios de `design/tests.md` contra la app arrancada, y entra en un bucle de auto-corrección si hay fallos.

### 8.1 Pre-condiciones

1. **Comprobar que existe `design/tests.md`.**
   - Si **no existe** (iniciativa antigua o spec sin flujos principales): saltar toda la Fase 3.5 con un aviso al usuario:
     > No se ha encontrado `design/tests.md`. La iniciativa no tiene tests E2E. Salto la fase de verificación con `playwright-cli`. Si quieres añadir tests, relanza `/sdd-analyst-system` para generarlos.
     Pasar directamente a la Fase 4.
   - Si existe pero está vacío (ninguna sección `## T-NNN`): mismo trato, saltar con aviso.
2. **Comprobar que `playwright-cli` está disponible**:
   ```bash
   playwright-cli --version || npx --no-install playwright-cli --version
   ```
   Si no está disponible, **STOP** y avisa al usuario para que lo instale. **MUST NOT** intentar instalarlo tú.
3. **Cargar el skill `playwright-cli`** para conocer los comandos disponibles (`open`, `goto`, `snapshot`, `click`, `fill`, `eval`, etc.). El skill `k-playwright` (`when-to-use.md`) aclara que para este caso de uso ("ejecutar un escenario puntual contra la app, sin generar `.spec.ts`") la herramienta correcta es **Agent CLI** (`playwright-cli`), no Test Agents ni `@playwright/test` directo.

### 8.2 Arrancar la app

1. Comprobar si la app ya está respondiendo:
   ```bash
   curl -sS -o /dev/null -w "%{http_code}\n" http://localhost:8080/ 2>/dev/null || echo "down"
   ```
2. Si está **down**, arrancarla en background:
   ```bash
   ./gradlew --no-daemon run --port 8080 --context-path / 
   ```
   con `run_in_background: true`. Notifica al usuario que estás arrancando la app.
3. Esperar a que el endpoint responda con poll cada 5s. **LIMIT**: máximo 120s de espera total — Axelor tarda en arrancar. Si tras 120s no responde, **STOP** y reporta al usuario con la salida del proceso de Gradle.
4. **Anotar si la app la arrancó este skill o ya estaba arrancada antes**: lo usaremos en el mensaje final (§8.5) para decirle al usuario si tiene un proceso `gradlew run` huérfano que él controla.

### 8.3 Bucle de ejecución y auto-corrección

**Variables del bucle:**

- **LIMIT**: `max_iter = 3`. **MUST NOT** superar este número de iteraciones bajo ningún concepto.
- `iter = 1`.
- `fallos_previos = []` (para detectar fallos persistentes que `code-implementer` no resuelve).

**Iteración:**

1. **Leer `design/tests.md`** y parsear las secciones `## T-NNN` con sus campos de cabecera (`Origen F`, `Verifica`, `Pantalla principal`, `Tipo`), Precondiciones, Pasos y Resultado esperado.
2. **Para cada test, en orden**:
   1. Resetear el navegador con `playwright-cli close` (idempotente; ignora error si no había sesión).
   2. Abrir la app: `playwright-cli open http://localhost:8080`.
   3. Hacer login con un usuario apto para el rol que el test pida (ver §8.4 sobre credenciales).
   4. **Traducir los pasos Given/When/Then al vuelo** a comandos `playwright-cli`:
      - `Dado el usuario en la pantalla "X"` → `goto` al menú correspondiente + `snapshot` para resolver refs.
      - `Cuando pulsa el botón "B"` → `snapshot`, localizar el ref del botón por su texto, `click eN`.
      - `Y rellena el campo "C" con "valor"` → `snapshot`, localizar el ref del campo por su etiqueta, `fill eN "valor"`.
      - `Entonces el sistema muestra "M"` → `snapshot` + búsqueda de la cadena `M` en el snapshot (o `eval` sobre el DOM). Si la cadena no aparece, el assert falla.
      - Para preparar precondiciones (entidades preexistentes en BD), preferentemente crear vía la propia UI antes del test. Si no es viable, documentar el fallo como "precondición no satisfecha" en lugar de inventar SQL.
   5. Recoger el resultado del test: `OK` o `FAIL` con (a) paso fallido, (b) error observado, (c) snapshot del momento, (d) consola del navegador (`playwright-cli console`).
3. **Si todos los tests pasan** → salir del bucle con éxito, ir a §8.5 (limpieza).
4. **Si algún test falla**:
   - **Diagnóstico previo (criterio §2.5).** Antes de invocar a `code-implementer`, clasifica cada fallo en una de tres categorías:
     - **Código**: el fallo encaja con una validación, regla o vista del diseño que existe pero no funciona. Asumir esta categoría por defecto cuando el diagnóstico no es claro.
     - **Test mal escrito**: el escenario referencia botones, campos o mensajes que **no existen** en los XML / Java del proyecto (el design tampoco los menciona). Es señal de que el `tests.md` está desalineado con el diseño.
     - **Diseño incorrecto**: el comportamiento esperado contradice el `design.md` (p.ej. el test exige que el botón aparezca para un rol al que el diseño explícitamente no le da acceso).
   - **Si la categoría es "test mal escrito" o "diseño incorrecto"** → **STOP** inmediatamente (sin agotar iteraciones) y pregunta al usuario con `AskUserQuestion` qué hacer. Razón: `code-implementer` no puede arreglar un test mal escrito tocando código.
   - **Si la categoría es "código"**:
     - **Detectar fallos persistentes**: si un fallo de un test es **idéntico** al de la iteración anterior (mismo paso, mismo error), `code-implementer` no está progresando. Trata el caso como `iter == max_iter` y **STOP**.
     - **Si `iter < max_iter`**:
       - Construir un **plan de corrección** en markdown — un fichero pequeño con un paso por fallo, no el `design.md` completo (si pasaras el design entero a `code-implementer` reejecutaría todo el plan en vez de solo arreglar los fallos):
         ```
         # Plan de corrección de tests E2E — iteración N

         ## Contexto
         El código Java de esta iniciativa ya está implementado a partir del diseño
         `.sdd/drafts/{iniciativa}/design/design.md`. La ejecución de los tests E2E
         de `design/tests.md` con `playwright-cli` ha dejado fallos que hay que
         corregir tocando **solo código Java**. Los XML de dominios/vistas/menús
         (ya copiados a `src/main/...`) y el propio `tests.md` son **contrato fijo**:
         no se tocan.

         Para entender el comportamiento esperado, lee `.sdd/drafts/{iniciativa}/design/design.md`
         y los `screen-*.md` / `entity-*.md` del análisis. Para entender el
         comportamiento observado, lee los fallos de abajo.

         ## Pasos

         ### Paso 1 — Corregir T-001 (<nombre>)
         **Paso fallido:** <texto literal del paso>
         **Error observado:** <mensaje del assert / esperado vs ocurrido>
         **Snapshot relevante:** <snapshot recortado>
         **Consola del navegador:** <líneas relevantes si las hay>
         **Hipótesis de causa:** <una frase, opcional>
         **Restricción:** solo editar código Java (servicios, controladores,
         repositorios, data-init). No editar XML ya copiados ni `tests.md`.

         ### Paso 2 — Corregir T-003 (<nombre>)
         …
         ```
       - **Reinvocar `code-implementer`** pasándole **ese plan de corrección** (no el `design.md` completo) junto con los mismos skills de dominio que se usaron en la Fase 3 (`k-sistemas`, `k-vistas`, opcionalmente `k-seguridad`).
       - Instruirle **explícitamente** en el propio plan que: (a) **solo corrija código Java**, (b) **NO edite XML de dominios/vistas/menús ya copiados** (principio 2.1), (c) **NO edite `design/tests.md`** (principio 2.6).
       - Tras la corrección, **reiniciar la app** (el código Java cambió: hay que rebuild + restart). Esperar de nuevo a que responda. Incrementar `iter` y volver al paso 1.
     - **Si `iter == max_iter`**:
       - **STOP** y pregunta al usuario con `AskUserQuestion` ofreciendo:
         1. **Marcar como bug de código irresoluble**: dejar el reporte en pantalla para investigación manual. La Fase 4 (mensaje final) avisará que la implementación está incompleta.
         2. **Revisar el test**: el escenario puede estar mal escrito. El usuario edita `design/tests.md` (y/o `analysis/tests.md` para mantener sincronía) fuera del skill, y luego puede relanzar `/sdd-implementer-system`.
         3. **Revisar el diseño**: relanzar `/sdd-designer-system` para corregirlo y rehacer el implementer.
         4. **Continuar sin verificación**: aceptar el código tal cual y pasar a Fase 4 (no recomendado).

### 8.4 Credenciales para login

Cada test puede requerir un rol distinto (Administrador, Supervisor, Profesor…). El implementer debe disponer de credenciales válidas para esos roles.

1. **Por defecto**, asumir que el `data-init` del proyecto crea un usuario por cada rol con contraseña conocida. Inspeccionar los ficheros XML en `src/main/resources/data-init/input/` (en particular `auth*.xml`) para descubrir los usuarios de prueba disponibles y sus credenciales.
2. **Si no hay convención clara** (o estamos en un entorno limpio sin data-init de usuarios), preguntar al usuario qué credenciales usar para cada rol que aparezca en los tests, y reutilizarlas en todo el bucle. **LIMIT**: exactamente 1 ronda de preguntas al inicio de la Fase 3.5. **MUST NOT** volver a preguntar credenciales en iteraciones posteriores del bucle.
3. Almacenar las credenciales en una variable de la sesión, no en disco. **MUST NOT** mostrarlas en mensajes al usuario ni en reportes de fallos.

### 8.5 Limpieza

Al salir del bucle (con éxito o por interrupción):

1. Cerrar el navegador: `playwright-cli close`.
2. Si la app la arrancó este skill (§8.2 paso 4), **dejarla corriendo** salvo que el usuario diga lo contrario — apagarla obliga a re-arrancarla en la siguiente iteración manual y enfada al desarrollador. Avisar al usuario:
   > La app sigue arrancada en http://localhost:8080. Pulsa Ctrl+C en su terminal cuando quieras detenerla.

### 8.6 Resumen para la Fase 4

Tras el bucle, construir un resumen breve para incluir en el mensaje final de Fase 4:

```
Tests E2E ejecutados con playwright-cli:
  - Iteraciones del bucle: N (de 3 máximo)
  - Tests totales: T
  - OK en la última iteración: T_ok
  - FAIL en la última iteración: T_fail
  - Estado: completado | detenido por el usuario | bug irresoluble
```

---

## 9. Fase 4 — Mensaje final al usuario

Tras completar la implementación (y la verificación de tests si aplica), indica:

```
Implementación completada.

{resumen de tests de la Fase 3.5, ver §8.6 — omitir esta sección si la Fase 3.5 se saltó}

Los artefactos del draft se mantienen en .sdd/drafts/{iniciativa}/ — no se ha archivado nada en .sdd/specs/.
Cuando estés conforme con la implementación, lanza `/sdd-close-spec` para cerrar la iniciativa: actualizará los CLAUDE.md afectados y archivará la spec en .sdd/specs/.
```

Sustituye `{iniciativa}` por el nombre real de la carpeta del draft.

**CRITICAL**: si la Fase 3.5 acabó con tests `FAIL` (bug irresoluble) o se detuvo por elección del usuario, **MUST** decirlo explícitamente en el mensaje:

> Atención: la verificación con `playwright-cli` no fue limpia. T_fail tests fallan tras N iteraciones. Revisa el reporte de fallos antes de lanzar `/sdd-close-spec`, o relanza este skill tras corregir el diseño / los tests.

**MUST NOT** lanzar `/sdd-close-spec` tú mismo. El usuario decide cuándo ejecutarlo.

---

## Quick Guidelines

- Eres un **delegador**: copias XML del diseñador y delegas el Java en `code-implementer`. **MUST NOT** reescribir XML ni generar Java tú mismo.
- Los XML de `design/domains/`, `design/views/` y `design/menus.xml` son **contrato fijo**: se copian literalmente. Si están mal, **STOP** y vuelve a `/sdd-designer-system`.
- El `design/tests.md` también es contrato fijo: **MUST NOT** editarlo para que un test pase — sería ocultar un bug.
- El bucle de tests E2E tiene **LIMIT**: 3 iteraciones máximo. Tras agotarlo, **STOP** y `AskUserQuestion` con las 4 opciones (bug / revisar test / revisar diseño / continuar sin verificación).
- Antes de reinvocar `code-implementer` en una iteración del bucle, clasifica los fallos: **código** (reinvocar), **test mal escrito** o **diseño incorrecto** (**STOP** inmediato, no agotes iteraciones).
- Cuando reinvoques `code-implementer` por fallos de tests, pásale un **plan de corrección pequeño** (un paso por fallo), **MUST NOT** pasarle el `design.md` completo otra vez.
- `AskUserQuestion` solo para lo imprescindible: confirmar ruta auto-detectada, sobrescritura de ficheros, conflictos de `<menuitem>` y decisiones tras agotar el bucle de tests.

---

## 10. Checklist final del implementer

Antes de emitir el mensaje final de la Fase 4, **MUST** recorrer este checklist. Si alguno falla, vuelve a la fase indicada y corrígelo. **LIMIT**: máximo 3 iteraciones de corrección antes de detenerse y avisar al usuario.

- [ ] ¿El `design.md` tenía `type: design` en el frontmatter? (Fase 1)
- [ ] ¿Todos los `domains/*.xml` del diseño se copiaron a su ruta destino sin reescribirlos? (Fase 2.2)
- [ ] ¿Todos los `views/*.xml` del diseño se copiaron a su ruta destino sin reescribirlos? (Fase 2.3)
- [ ] ¿El `menus.xml` resultante validó con `xmllint` contra `object-views.xsd`? (Fase 2.4)
- [ ] ¿Se invocó `code-implementer` pasándole el `design.md` íntegro y los skills de dominio correctos (`k-sistemas`, `k-vistas`, opcionalmente `k-seguridad`)? (Fase 3)
- [ ] Si existe `design/tests.md`: ¿se ejecutó el bucle de la Fase 3.5 sin superar `**LIMIT**: 3 iteraciones`?
- [ ] Si la Fase 3.5 se saltó por ausencia de `tests.md`: ¿se avisó explícitamente al usuario? (Fase 3.5 — §8.1)
- [ ] ¿El mensaje final menciona si la verificación E2E fue limpia, parcial o saltada? (Fase 4)
- [ ] ¿Se ha respetado la prohibición de editar XML ya copiados y `design/tests.md` durante todo el proceso? (principios 2.1 y 2.6)

---

## Apéndice A — Override de rutas (para testing)

Para probar este skill en un sandbox alternativo sin tocar el árbol real (testing unitario del propio skill, iteración de mejoras, etc.), se aceptan los siguientes overrides (también se reconocen las formas `entrada: <ruta>` y `raíz: <ruta>`):

- `--in=<ruta>` — fichero `design.md` de entrada explícito. **Desactiva la auto-detección** descrita en la Fase 0 caso 2. La "carpeta de la iniciativa" es la que contiene la subcarpeta `design/` de ese fichero.
- `--root=<ruta>` — raíz alternativa a `.sdd/drafts/`. Todas las rutas relativas (auto-detección, carpeta de la iniciativa) se resuelven contra esta raíz.

No hay `--out` porque este skill no crea ficheros en `.sdd/`: copia los XML del diseño al árbol del proyecto y delega en `code-implementer` el resto. Para probar el implementer en un sandbox, ejecútalo apuntando a una copia del proyecto.

En uso normal no se especifican.