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

## 4. Qué NO tocar (si la corrección lo exige → `BLOQUEADO`)

**MUST NOT**:

- modificar `test-e2e-desc.md` ni los ficheros de `test-e2e-desc/` para que el test pase (es trampa);
- editar XML de dominios o vistas materializados por el diseño, ni cambiar firmas/contratos declarados por el diseño;
- introducir mass-assignment, saltarse `AllowProperties` o la asignación incondicional de campos `servidor`.

Si para que el test pase haría falta **cambiar el contrato/diseño** (un XML materializado, una firma declarada, una regla del spec), o falta un **recurso** (dependencia inexistente, dato maestro ausente), **NO** lo fuerces: devuelve `BLOQUEADO` con el motivo **y la ruta de salida**: `/sdd-designer` en modo **Revisar/Modificar** sobre esta iniciativa con el cambio concreto que hace falta, y después re-invocar `/sdd-implementer` para rematerializar el fichero afectado.

---

## 5. Formato de salida (REQUIRED)

Primera línea **exactamente** uno de estos tokens, + 1-2 líneas de resumen:

- `CORREGIDO: {T-NNN}` — aplicaste un cambio de código que debería hacer pasar el test (resume qué cambiaste y dónde).
- `BLOQUEADO: {T-NNN} — {motivo}` — la corrección exigiría tocar el contrato/diseño/XML o falta un recurso; no se puede arreglar solo con código Java.

- ✅ CORRECTO: `CORREGIDO: T-009` + "Asignado el estado ABIERTO en GrupoServiceImpl.open() (faltaba)."
- ✅ CORRECTO: `BLOQUEADO: T-009 — el campo `estado` no existe en el dominio Grupo.xml materializado; hace falta rediseñar.`
- ❌ INCORRECTO: `Lo he arreglado` (token no exacto), pegar el `.java` en la respuesta (ya está en disco), editar `Grupo.xml` para añadir el campo en vez de devolver `BLOQUEADO`.

**MUST NOT** usar `AskUserQuestion`: ante un bloqueo, devuélvelo con `BLOQUEADO`; el motor lleva la decisión al usuario.
