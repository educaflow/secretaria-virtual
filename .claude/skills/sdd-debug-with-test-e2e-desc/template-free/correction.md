# Corrección: arreglar el código para que un test E2E pase (feature libre)

Lo lee el **corrector** (README §2.3). Tarea: ante un test que falla, **analizar la causa, cargar los skills de la parte afectada y corregir el código**. La app la rearranca el motor; tú solo corriges.

---

## 1. Localizar la causa

1. Lee tu `t-NNN-<slug>.desc.md` (qué debía pasar), el bloque `=== FALLO ===` (qué pasó) y el **extracto del log** (excepción, validación, permisos, JPQL, job…).
2. Lee del `design.md` de la iniciativa la cabecera `**Ubicación del código:**` y la subsección de «Piezas» de la parte que falla: ahí está **dónde** vive el código de la feature y **qué** debía hacer. Léelo, no lo escribas.
3. **Localiza la causa con el MCP de IntelliJ** (`ide_search_text`, `ide_find_class`, `ide_find_definition`, `ide_find_references`, `ide_call_hierarchy`), nunca con `grep`/`find`.
4. **Distingue** si el fallo está en el código de la feature, en código **preexistente** que la feature debía modificar y no modificó, o en código preexistente **que la feature rompió** (un test de no-regresión en rojo). Los tres se corrigen aquí; lo que cambia es el cuidado: en código preexistente, **MUST** cambiar lo mínimo y comprobar con `ide_find_references` quién más lo usa.

---

## 2. Decidir y cargar los skills necesarios (REQUIRED)

**Carga con `Skill`, ANTES de corregir, los skills de la parte que toca el fallo:**

| Qué toca el fallo | Skills a cargar |
|---|---|
| Cualquier Java/Kotlin (entidades, servicios, controladores, componentes de `base`) | **MUST** `k-secure-coding` y `k-code-quality` |
| Estructura de sistema/subsistema, servicios, modelos | `k-sistemas` |
| Validaciones / reglas de negocio / reglas de UI / campos calculados | `k-validaciones` |
| Vistas, grids, formularios, menús, acciones | `k-vistas` |
| Tareas programadas / cron / Quartz | `k-scheduler` |
| Datos iniciales / semilla / permisos `auth-*.xml` | `k-datainit` |
| Inyección con Guice (`Guice/MissingConstructor`, `Provider`) | `k-guice` |
| Textos traducibles | `k-i18n` |
| Un trámite o tipo de expediente que la feature roza | `k-tramite` / `k-tipo-expediente` |
| Tests E2E / Playwright (solo para entender el fallo del ejecutor, nunca para «arreglar» el test) | `k-playwright` |

**MUST NOT** empezar a corregir sin los skills que el fallo requiere. **MUST NOT** cargar `k-seguridad` (obsoleto): para permisos, lee `subsystem/security/`.

---

## 3. Aplicar la corrección

1. Construye un **plan de corrección pequeño**: un paso por causa (fichero, qué cambiar y por qué), con el fallo y el log.
2. **Delega el código en `developer-code-implementer`** (`Skill`), con el plan y los skills. Recuérdale que aplican `CLAUDE.md` y los skills cargados; la ubicación del código la fijó el diseño.
3. **Si la causa es que falta un test unitario** (la lógica estaba mal y ningún test lo cazó): la corrección **MUST** incluir, en el mismo plan, el test unitario que lo habría detectado (TDD también en la corrección). `./run.sh` lo ejecutará al rearrancar.
4. Corrige **solo** lo que causa este fallo. **MUST NOT** reescribir lo que funciona.

---

## 4. Qué NO tocar (→ `DESIGN-ERROR` o `BLOQUEADO`)

**MUST NOT**:

- modificar `test-e2e-desc.md` ni `test-e2e-desc/` (es trampa);
- editar los XML materializados por el diseño ni cambiar firmas/contratos que el diseño declara;
- **mover código a otra carpeta** que la de `**Ubicación del código:**` del diseño;
- introducir mass-assignment, saltarse `AllowProperties` o la asignación incondicional de campos `servidor`;
- añadir nada a `subsystem/tramitador` que el diseño no puso ahí;
- editar o relajar los tests de arquitectura, vistas, orden de métodos ni `architecture-rules.md`/`view-rules.md` para que el build pase.

**CRITICAL — tres bloqueos distintos:**

- **`DESIGN-ERROR`** — el test no pasa sin tocar el diseño: la pieza diseñada no puede cumplir el escenario (ubicación que viola una regla de arquitectura, XML inconsistente, firma que falta, `test-e2e-desc.md` contradice al diseño). **Máximo detalle**: el motor escribe `error_design.log` y detiene el skill; la salida es `/sdd-designer` en modo Revisar/Modificar.
- **`BLOQUEADO`** — falta un **recurso del entorno** ajeno al diseño (usuario de demo inexistente, servicio externo caído, dependencia ausente). El motor pregunta al usuario.
- **`MANUAL`** — el paso **no lo puede dar ninguna automatización** (firma con certificado en la máquina del usuario, leer un correo real, un dispositivo físico). El motor lo pasa a `[-]` y **sigue**. **MUST NOT** usar `MANUAL` para un locator que no aparece, un timing, un mensaje distinto o un flujo largo: eso es un bug a corregir.

- ✅ `DESIGN-ERROR`: el diseño puso el banner en `base/web` dependiendo de `system/gestioncentro` y ArchUnit lo rechaza en el build.
- ✅ `BLOQUEADO`: el test hace login con un usuario que no está en `usuarios-demo.xml`.
- ✅ `MANUAL`: el `Cuando` es «firma con AutoFirma__!!» y se abre la aplicación de escritorio.
- ❌ `MANUAL` porque el banner tarda en aparecer (es un fallo de sondeo/caché: corrige o indica al ejecutor cómo sondear).

---

## 5. Formato de salida (REQUIRED)

Primera línea **exactamente** uno de estos tokens, + 1-2 líneas de resumen:

- `CORREGIDO: {T-NNN}` — aplicaste un cambio que debería hacer pasar el test.
- `MANUAL: {T-NNN} — {motivo}` — el test necesita una persona (§4).
- `DESIGN-ERROR: {T-NNN} — {motivo detallado}` — no se resuelve sin tocar el diseño.
- `BLOQUEADO: {T-NNN} — {motivo}` — recurso del entorno.

- ✅ `CORREGIDO: T-001` + «`AvisoGlobalServiceImpl.findActivos()` filtraba por centro del usuario; los avisos son globales (REQ-002). Añadido `findActivos_devuelveTodosLosCentros` al test unitario.»
- ❌ `Lo he arreglado`, pegar el `.java`, editar el XML del diseño, mover la clase a otra carpeta, degradar un error de diseño a `BLOQUEADO`.

**MUST NOT** usar `AskUserQuestion`.
