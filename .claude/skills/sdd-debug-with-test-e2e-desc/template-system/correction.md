# Corrección: arreglar el código para que un test E2E pase

Lo lee el **corrector** (§2.3 del `README.md`). Tarea: ante un test que falla, **analizar la causa, cargar los skills necesarios y corregir el código Java** para que el test pase. La app la rearranca el motor después; tú solo corriges el código.

---

## 1. Localizar la causa

1. Lee tu `t-NNN-<slug>.desc.md` (qué debía pasar) y el bloque `=== FALLO ===` del ejecutor (qué pasó en la UI).
2. Lee el **extracto del log de la app** que te pasó el motor: busca la excepción/traza del momento del fallo (NPE, fallo de validación, error de permisos, JPQL, fallo del cron…).
3. **Localiza la causa en el código con el MCP de IntelliJ** (`ide_search_text`, `ide_find_class`, `ide_find_definition`, `ide_find_references`, `ide_call_hierarchy`…), **NO** con `grep`/`find`. Identifica el fichero, clase y método responsables.

---

## 2. Decidir y cargar los skills necesarios (OBLIGATORIO)

**Analiza qué parte de la arquitectura toca el fallo y carga con `Skill` los skills de dominio aplicables ANTES de corregir.** Guía:

- Tocas entidades, servicios o controladores → **MUST** cargar `k-secure-coding` **y** `k-code-quality` (la corrección **MUST NOT** introducir mass-assignment, saltarse `AllowProperties` ni la asignación incondicional de campos `servidor`).
- Estructura de sistema/subsistema, servicios, modelos → `k-sistemas`.
- Validaciones / reglas de negocio / reglas de UI / campos calculados → `k-validaciones`.
- Vistas, grids, formularios, acciones → `k-vistas`.
- Tareas programadas / cron / Quartz → `k-scheduler`.
- Datos iniciales/semilla → `k-datainit`.
- Inyección de dependencias Guice → `k-guice`.
- Mensajes/títulos traducibles → `k-i18n`.

**MUST NOT** empezar a corregir sin haber cargado los skills que el fallo requiere.

---

## 3. Aplicar la corrección

1. Construye un **plan de corrección pequeño**: un paso por causa localizada (fichero/clase, qué cambiar y por qué), con la descripción del fallo y el extracto del log.
2. **Delega el código en `developer-code-implementer`** (herramienta `Skill`), pasándole ese plan y los skills de dominio aplicables. `developer-code-implementer` implementa, verifica y revisa cada paso. **MUST NOT** corregir XML del diseño para cuadrar el Java.
3. **MUST NOT** pasar todos los tests ni reescribir lo que ya funciona: corrige **solo** lo que causa este fallo.

---

## 4. Qué NO tocar (si la corrección lo exige → `DESIGN-ERROR` o `BLOQUEADO`)

**MUST NOT**:

- modificar `test-e2e-desc.md` ni los ficheros de `test-e2e-desc/` para que el test pase (es trampa);
- editar XML de dominios o vistas materializados por el diseño, ni cambiar firmas/contratos declarados por el diseño;
- introducir mass-assignment, saltarse `AllowProperties` o la asignación incondicional de campos `servidor`.

**CRITICAL — son dos bloqueos distintos y NO se reportan igual.** Distingue el **origen** antes de elegir el token:

- **Error de diseño → `DESIGN-ERROR`.** El test **no se puede hacer pasar sin tocar el diseño**: un XML de dominio o de vista materializado es inconsistente con lo que el test exige, el diseño no declara el método o la firma que el test espera, o el propio `test-e2e-desc.md` describe algo que contradice al diseño. **MUST NOT** editar el diseño para forzarlo.
  Da el **máximo detalle** en el motivo: qué fichero del diseño, qué falta o es inconsistente, y por qué no se resuelve escribiendo código Java. El motor lo vuelca en `test-e2e-desc/error_design.log` y **detiene el skill**; la ruta de salida es `/sdd-designer` en modo **Revisar/Modificar** sobre esta iniciativa y después re-invocar `/sdd-implementer` para rematerializar el fichero afectado.
- **Recurso del entorno → `BLOQUEADO`.** Falta algo **ajeno al diseño** que alguien tiene que proveer: una dependencia inexistente, un dato maestro ausente, un usuario de demo que no existe, un servicio externo caído. El motor **para y pregunta al usuario**.

- ✅ CORRECTO (`DESIGN-ERROR`): el test espera un campo `estado` que el `Grupo.xml` materializado no declara.
- ✅ CORRECTO (`BLOQUEADO`): el test hace login con un usuario que no está en los datos de demo.
- ❌ INCORRECTO: devolver `BLOQUEADO` para un error de diseño — el motor preguntaría al usuario en vez de escribir `error_design.log` y encaminarlo a `/sdd-designer`, que es lo que de verdad hay que hacer.

> El token `MANUAL` **no forma parte del contrato de esta plantilla**: aquí ningún test se declara no automatizable. **MUST NOT** devolverlo.

---

## 5. Formato de salida (REQUIRED)

Primera línea **exactamente** uno de estos tokens, + 1-2 líneas de resumen:

- `CORREGIDO: {T-NNN}` — aplicaste un cambio de código que debería hacer pasar el test (resume qué cambiaste y dónde).
- `DESIGN-ERROR: {T-NNN} — {motivo detallado}` — el test no se puede hacer pasar sin tocar el diseño (§4). **MUST** dar el máximo detalle: el motor lo vuelca verbatim en `error_design.log` y detiene el skill.
- `BLOQUEADO: {T-NNN} — {motivo}` — falta un **recurso del entorno** ajeno al diseño (§4).

- ✅ CORRECTO: `CORREGIDO: T-009` + "Asignado el estado ABIERTO en GrupoServiceImpl.open() (faltaba)."
- ✅ CORRECTO: `DESIGN-ERROR: T-009 — el campo `estado` no existe en el dominio Grupo.xml materializado por el diseño y el test lo exige; no se puede arreglar con código Java.`
- ✅ CORRECTO: `BLOQUEADO: T-012 — el test hace login con `ejemplo@centro-x.es`, que no existe en los datos de demo.`
- ❌ INCORRECTO: `Lo he arreglado` (token no exacto), pegar el `.java` en la respuesta (ya está en disco), editar `Grupo.xml` para añadir el campo en vez de devolver `DESIGN-ERROR`, o degradar un error de diseño a `BLOQUEADO`.

**MUST NOT** usar `AskUserQuestion`: ante un bloqueo, devuélvelo con su token; el motor decide qué hacer con cada uno.
