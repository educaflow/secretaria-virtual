# Contrato de materialización — de una tarea al árbol del proyecto

Lo lee el **implementador** (README §2.2) y lo consulta el **corrector-build** (README §2.4) para saber qué puede tocar. Define **cómo materializar UNA tarea** en el árbol del proyecto. El implementador recibe la ruta de **una** `task_NN.md`; este contrato dice cómo ejecutarla según su naturaleza.

> Si la tarea es de **tests** (unitarios o de arquitectura), su materialización la define `tests-code.md`.

---

## 1. Principio: no regenerar los XML materializados — copiarlos literalmente

Los XML de `design/domains/`, `design/views/` y `design/menus.xml` son la **fuente de verdad**: el diseñador ya los validó con `xmllint` contra sus XSD. **MUST** copiarlos tal cual al destino.

**MUST NOT**:

- **MUST NOT** reescribir los XML desde el `design.md`.
- **MUST NOT** reformatearlos al vuelo (indentación, reordenar atributos, etc.).

Re-generarlos pierde correcciones manuales aplicadas al diseño, rompe la validación del designer e introduce divergencias silenciosas. Si al copiar detectas que un XML del diseño está **mal**, responde `BLOCKED: {tarea} — XML del diseño incorrecto: {detalle}`. **MUST NOT** arreglarlo aquí (hay que volver a `/sdd-designer`).

---

## 2. Principio: no escribir Java a mano — delegar en `code-implementer`

El implementador **MUST NOT** escribir código Java directamente. Toda la implementación Java (servicios, controladores, repositorios, DTOs, jobs, datos iniciales, seguridad, **y el código de los tests** — ver `tests-code.md`) se delega en `code-implementer`, pasándole **el texto de la tarea tal cual** y sus skills de dominio.

**OBLIGATORIO**, en este orden:

1. Lee la sección `## Skills a usar` de la tarea y **carga cada skill listado con la herramienta `Skill`** **antes** de implementar nada.
2. Una vez cargados, **invoca `code-implementer`** pasándole el `<texto del prompt>` de la tarea **verbatim**.

Al invocar `code-implementer`, inclúyele además:

- Una **nota** de que los XML de dominios/vistas/menús de los que dependa **ya están colocados** en `src/main/...` y son **contrato fijo**: **NO** debe regenerarlos ni editarlos; las firmas Java deben coincidir con las acciones de las vistas (`<action-method method="action-..." class="..."/>` ↔ controlador.método) y las entidades JPA con los dominios. Si detecta un XML mal, **detenerse y notificar**, no editarlo.
- La instrucción de **parar y reportar** ante cualquier bloqueo. **MUST NOT** adivinar.

---

## 3. Materializar según la naturaleza de la tarea

Según los ficheros que la tarea cubre:

- **Tarea de XML ya materializado** (dominio, vista): localiza el XML en `design/...`, crea la carpeta destino con `mkdir -p` si no existe y **copia el fichero literalmente** (`cp`) a su ruta destino (§1). Si el destino **ya existe**, responde `CONFLICT: {tarea} — ya existe {ruta destino}` (el motor preguntará al usuario).
- **Tarea de `menus.xml`**: lee `design/menus.xml`, extrae sus `<menuitem>` e **insértalos** en `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` justo antes de `</object-views>`. Si ya existe un `<menuitem name="...">` con el mismo `name`, responde `CONFLICT: {tarea} — <menuitem name="..."> ya existe`. Tras fusionar, **MUST** validar con `xmllint`:
  ```bash
  xmllint --noout --schema ../axelor-open-platform/axelor-core/src/main/resources/object-views.xsd \
    src/main/java/com/educaflow/secretariavirtual/menus/menus.xml
  ```
  Si falla, responde `BLOCKED: {tarea} — xmllint no valida el menus.xml fusionado: {detalle}`.
- **Tarea de Java** (servicios, controladores, repositorios, DTOs, jobs, datos iniciales, seguridad): aplica §2 (cargar skills → invocar `code-implementer`).
- **Tarea de tests** (unitarios / arquitectura): ver `tests-code.md` (también delega en `code-implementer`).

**MUST NOT** que `code-implementer` lea otros `design.md`/`analysis.md` de otras iniciativas como referencia: implementa **únicamente** la tarea recibida.

---

## 4. Los XML ya colocados son contrato fijo para el Java

Cuando se implemente el Java, los XML de dominios y vistas ya están en su ubicación real (las tareas se implementan en orden: dominios y vistas antes que el Java que los usa). Esto significa:

- Las firmas de los métodos Java **deben coincidir** con las acciones declaradas en las vistas.
- Las entidades JPA generadas **deben coincidir** con los dominios XML (nombres de campos, tipos, relaciones).
- Si al implementar el Java se detecta que un XML ya colocado tiene un error, **detente y notifica** (`BLOCKED`) — no lo edites. Corregirlo requiere volver a `/sdd-designer`.

---

## 5. Detenerse y reportar ante un bloqueo

Reportar (no adivinar) es la respuesta correcta ante:

- Una dependencia declarada en el diseño que no existe o tiene una API diferente.
- Una instrucción del diseño ambigua o contradictoria con el código existente.
- Un recurso requerido (fichero, certificado, credencial, clase generada) que no está disponible.
- Un fichero XML del diseño que contiene un error.

**CRITICAL**: **MUST NOT** adivinar ni inventar soluciones. Continuar a ciegas ante un bloqueo genera deuda técnica silenciosa. Reporta con el token adecuado:

- `CONFLICT: {tarea} — {qué destino ya existe}` — cuando el problema es de sobrescritura (lo decide el usuario).
- `BLOCKED: {tarea} — {motivo}` — cualquier otro bloqueo.
- `DONE: {tarea}` — solo cuando la tarea quedó materializada correctamente.

**MUST NOT** pegar el código generado en la respuesta (ya está en disco): solo el token + 1-2 líneas de resumen.
