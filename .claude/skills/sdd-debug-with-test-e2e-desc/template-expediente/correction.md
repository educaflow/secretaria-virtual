# Corrección: arreglar el código del tipo de expediente para que un test E2E pase

Lo lee el **corrector** (§3.3 del `README.md`). Tarea: ante un test que falla, **analizar la causa, cargar los skills necesarios y corregir el código Java/Kotlin** del tipo de expediente. La app la rearranca el motor después; tú solo corriges el código.

**CRITICAL — en un tipo de expediente casi todo lo que "es código" está en tres clases por fase, y casi todo lo demás es XML que MUST NOT tocarse** (§5). Antes de editar nada, clasifica el fallo con §2.

---

## 1. Localizar la causa

1. Lee tu `t-NNN-<slug>.desc.md` (qué debía pasar: los siete campos de cabecera te dan estado de partida, evento, estado de llegada y si el test es manual) y el bloque `=== FALLO ===` del ejecutor (qué pasó en la UI, con la fase/estado observados).
2. Lee el **extracto del log de la app** que te pasó el motor: busca la excepción del momento del fallo. En un expediente las más frecuentes son `BusinessException` (validación de negocio), `NoSuchMethodException`/`IllegalArgumentException` al resolver un `trigger*`/`onEnter*` por reflexión, NPE dentro de un `onEnter*`, y los errores del `Tramitador` al no encontrar la vista o el validador.
3. **Localiza la causa en el código con el MCP de IntelliJ** (`ide_search_text`, `ide_find_class`, `ide_find_definition`, `ide_find_references`, `ide_call_hierarchy`…), **NO** con `grep`/`find`.
4. **Sitúa el fichero por la fase**: las clases viven en `<carpeta de versión>/<fase en minúsculas>/`. El `codePhase` del expediente (visible como «Fase» en la cabecera) te dice **en qué carpeta** está la clase responsable.

---

## 2. Mapa síntoma → clase responsable

| Síntoma observado | Clase / fichero responsable |
|---|---|
| El expediente **no se crea** o nace con campos servidor vacíos (solicitante, DNI de firma, año…) | `InitialEventManagerImpl.triggerInitialEvent` (raíz de la versión, **no** en una fase) |
| El evento se dispara pero **no cambia de estado**, o va al estado equivocado | el `updateState(...)` de `trigger<Evento>` en el `PhaseEventManagerImpl` de la fase **de partida** |
| El evento ramifica y toma la **rama equivocada** | la guarda (el `if` sobre el campo) dentro de ese mismo `trigger<Evento>` |
| Transiciona bien pero **no se genera el PDF / el registro / la notificación** esperados | `onEnter<Estado>` del `PhaseEventManagerImpl` de la fase **de destino** |
| Un dato que teclea el usuario **llega vacío al servidor** | falta la entrada `field(model::getX)` en el `rules { }` de la pareja (estado, evento) del `StateEventValidatorImpl`: lo que no está en el `rules` **no se copia del request** |
| **No sale** el mensaje de validación que el test espera (`Tipo: error` que avanza) | falta la regla en `getForState<Estado>InEvent<Evento>` del `StateEventValidatorImpl` de la fase |
| Sale un mensaje de validación **que no debería** (`Tipo: happy` que no avanza) | regla de más, o guarda `ifValueIn`/`ifValueNotIn` mal condicionada, en ese mismo método |
| «No existe la vista en el expediente» | falta el `<form state="<ESTADO>">` genérico en el `views.xml` de la fase → **es XML del diseño**: `DESIGN-ERROR` (§5) |
| El botón del footer **no aparece** o no dispara nada | el `<footer>` del `views.xml` de la fase → **XML del diseño**: `DESIGN-ERROR` (§5) |
| «El perfil '…' no lo usa ningún estado de …» | el `profile` de un `<form>` o de un estado → **XML del diseño**: `DESIGN-ERROR` (§5) |
| Un usuario no ve el trámite en el árbol, o no ve su expediente en la bandeja | asignación de perfiles / permisos (`permisos-demo.xml`, data-init) |

---

## 3. Decidir y cargar los skills necesarios (OBLIGATORIO)

**Carga con `Skill` los skills aplicables ANTES de corregir.** Guía:

- **Siempre** `k-tipo-expediente` — y dentro de él, el fichero que toque: `phaseeventmanager.md` (los `trigger*`/`onEnter*` y el `InitialEventManager`), `validator.md` (el DSL de reglas y la whitelist de campos), `modelo.md` (el `domains.xml`), `vistas.md` (el formato preprocesado), `documentos.md` (los PDF de `documentospdf/`).
- Tocas la whitelist de campos, un campo `servidor` o un acceso a datos de otro centro → **MUST** cargar `k-secure-coding`: lo que no está en el `rules { }` no lo dicta el cliente, y **MUST NOT** darse entrada en `rules` a un campo que rellena el servidor.
- Escribes o reescribes lógica Java/Kotlin → `k-code-quality`.
- Reglas de negocio, validaciones, campos calculados → `k-validaciones`.
- Mensajes o títulos traducibles → `k-i18n` (**MUST NOT** escribir ningún `i18n_*.csv`: los genera un script).
- El fallo está en un servicio de otro subsistema que el trámite invoca → los skills de ese subsistema (`k-sistemas`, `k-datainit`, `k-scheduler`, `k-guice`…).
- **MUST NOT** cargar `k-seguridad` (**OBSOLETO**).

**MUST NOT** empezar a corregir sin haber cargado los skills que el fallo requiere.

---

## 4. Aplicar la corrección

1. Construye un **plan de corrección pequeño**: un paso por causa localizada (fichero/clase/método, qué cambiar y por qué), con la descripción del fallo y el extracto del log.
2. **Delega el código en `developer-code-implementer`** (herramienta `Skill`), pasándole ese plan y los skills de dominio aplicables.
3. **MUST NOT** corregir el XML del diseño para cuadrar el Java, ni al revés: corrige **solo** lo que causa este fallo.
4. **CRITICAL — la clase `States` es generada**: la proyecta `GenerateStatesTask` desde el `TipoExpedienteInstance.xml` en `build/src-gen-states/main/java`. **MUST NOT** editarla ni versionarla. Si un estado o un evento no existe en `States`, el problema está en el XML → `DESIGN-ERROR`.
5. **CRITICAL — respeta las firmas**: `trigger<Evento>` lleva **dos** parámetros de entidad (actual y original) más el `EventContext`; `onEnter<Estado>` lleva **uno**; `getForState<Estado>InEvent<Evento>` lleva **cero** y devuelve `BeanValidationRules`. El nombre del estado/evento va **sin la fase**. Los tests de `src/test/java/com/educaflow/tiposexpedientes/` verifican esto y son **fuente de verdad**: **MUST NOT** editarlos, debilitarlos ni excluir de ellos el tipo.
6. Una `BusinessException` es para lo que el usuario puede corregir; una guarda de código imposible va con `RuntimeException`.

---

## 5. Qué NO tocar

**MUST NOT**:

- modificar `test-e2e-desc.md` ni los ficheros de `test-e2e-desc/` para que el test pase (es trampa);
- editar los **XML materializados por el diseño**: `TipoExpedienteInstance.xml` (fases, estados, eventos, perfiles), `domains.xml`, los `views.xml` (raíz y fases), los `documentospdf/*.xml`, el `TramiteInstance.xml`;
- editar los tests de `src/test/java/com/educaflow/tiposexpedientes/`;
- escribir un `i18n_es.csv` / `i18n_ca.csv`;
- dar entrada en el `rules { }` a un campo que rellena el servidor para "que el test pase".

Si para que el test pase haría falta **cambiar el contrato/diseño** (cualquier XML de la lista anterior, una firma declarada, una transición, una regla del spec), **NO** lo fuerces: devuelve `DESIGN-ERROR` con la explicación. La ruta de salida es `/sdd-designer` en modo **Revisar/Modificar** sobre esta iniciativa y después re-invocar `/sdd-implementer` para rematerializar el fichero afectado.

Si lo que falta es un **recurso del entorno** ajeno al diseño (un usuario o un dato de demo inexistente, un servicio externo caído), devuelve `BLOQUEADO`: hace falta que alguien lo provea.

Si el fallo es que **el paso no lo puede ejecutar ninguna automatización** (la firma en cliente con AutoFirma, `README.md` §4.4), devuelve `MANUAL`, **no** `BLOQUEADO`: no hay nada roto que arreglar ni nada que el usuario deba proveer, el test simplemente necesita una persona. El motor lo pasa a `[-]` en el índice y **sigue con los demás tests**; un `BLOQUEADO` en cambio detiene la pasada y pregunta.

- ✅ CORRECTO (`MANUAL`): el `When` es «pulsa Firmar», se abre AutoFirma y hace falta el certificado del interesado en su máquina.
- ❌ INCORRECTO (`MANUAL`): el locator del botón no se encuentra, la vista no abre, el mensaje no coincide, el test tarda demasiado. Eso es un fallo a **corregir**, y usar `MANUAL` para eso esconde un bug real detrás de una etiqueta.

---

## 6. Formato de salida (REQUIRED)

Primera línea **exactamente** uno de estos tokens, + 1-2 líneas de resumen:

- `CORREGIDO: {T-NNN}` — aplicaste un cambio de código que debería hacer pasar el test (resume qué cambiaste y dónde).
- `MANUAL: {T-NNN} — {motivo}` — el test **no es automatizable**: necesita una persona (§5). El motor lo marca `[-]` y continúa.
- `BLOQUEADO: {T-NNN} — {motivo}` — falta un recurso del **entorno** ajeno al diseño.
- `DESIGN-ERROR: {T-NNN} — {motivo}` — la corrección exigiría tocar un XML del diseño, una transición o una firma declarada.

- ✅ CORRECTO: `CORREGIDO: T-006` + "Faltaba el `eventContext.updateState(States.Revision.PENDIENTE_REVISAR)` en `trigger<Evento>` de la fase de recepción."
- ✅ CORRECTO: `DESIGN-ERROR: T-011 — el estado de destino no declara el evento en su `events`; hay que corregir el TipoExpedienteInstance.xml en el diseño.`
- ✅ CORRECTO: `MANUAL: T-014 — el paso exige firmar con AutoFirma en la máquina del usuario; ninguna automatización puede hacerlo.`
- ✅ CORRECTO: `BLOQUEADO: T-016 — el juego de datos usa el login `ejemplo@centro-x.es`, que no existe en `usuarios-demo.xml`.`
- ❌ INCORRECTO: `Lo he arreglado` (token no exacto), pegar el `.java` en la respuesta (ya está en disco), añadir a mano el `<form state=…>` que falta en vez de devolver `DESIGN-ERROR`.

**MUST NOT** usar `AskUserQuestion`: ante un bloqueo, devuélvelo con su token; el motor lleva la decisión al usuario.
