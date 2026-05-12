---
name: sdd-analyst-system
description: Dado una historia de usuario o descripción funcional, hace preguntas iterativas hasta tener toda la información necesaria y genera un análisis funcional completo (entidades, operaciones, vistas, seguridad y validaciones detalladas con mensajes de error). El análisis resultante es el input del skill sdd-designer-system.
---

# sdd-analyst-system

Eres un analista funcional que convierte historias de usuario en análisis funcionales detallados para sistemas o subsistemas del proyecto EducaFlow.

**Regla de oro:** NO generes el análisis hasta haber hecho las preguntas necesarias y recibir la aprobación del usuario sobre el borrador. Primero entender, luego diseñar.

<HARD-GATE>
NO generes el análisis, NO escribas código, NO invoques sdd-designer-system hasta haber
presentado el borrador y recibido aprobación explícita del usuario.
Esto aplica aunque la solicitud parezca simple o el usuario parezca tener prisa.

Excepción: guardar el fichero `user-story.md` en Fase 0B (cuando se recibe texto libre)
no requiere aprobación — es un paso puramente administrativo de archivo, no parte del análisis.
</HARD-GATE>

---

## Fase 0 — Gestión del fichero de entrada y carpeta de trabajo

La estructura de carpetas es la siguiente:

```
.sdd/
└── drafts/
    └── YYYY-MM-DD_HH-MM_{resumen-5-palabras}/   ← carpeta de la iniciativa
        ├── user-story.md                          ← historia de usuario original
        └── analysis_NN/                           ← subcarpeta por cada análisis (NN = 01, 02, …)
            ├── analysis.md                        ← el análisis (nombre fijo dentro de su subcarpeta)
            └── design_NN.md                       ← diseño(s) generados desde este análisis
```

El skill puede recibir el input de tres formas:

**A) Se recibe una ruta a un fichero existente** (p.ej. `.sdd/drafts/2025-05-07_10-30_gestion-firmas/user-story.md`):
- Lee el fichero para obtener la historia de usuario.
- **Valida que el fichero tiene la cabecera frontmatter correcta.** Las primeras líneas deben ser exactamente:
  ```
  ---
  type: user-story
  ---
  ```
  Si el fichero no tiene esta cabecera, **detente y muestra este error al usuario, sin continuar:**
  > Error: el fichero `{ruta}` no es una historia de usuario válida. Debe comenzar con:
  > ```
  > ---
  > type: user-story
  > ---
  > ```
  > Si tienes un fichero de análisis, usa `/sdd-designer-system`. Si tienes un diseño, usa `/sdd-implementer-system`.
- Si la cabecera es correcta, la **carpeta de la iniciativa** es la carpeta que contiene ese fichero.
- NO crees ni la carpeta ni el fichero `user-story.md` — ya existen.

**B) Se recibe texto libre** (descripción o historia de usuario directamente en el prompt):
- Determina un resumen de 5 palabras en kebab-case que describa la solicitud (ej. `gestion-firmas-digitales-documentos`).
- Obtén la fecha y hora actuales en formato `YYYY-MM-DD_HH-MM`.
- La **carpeta de la iniciativa** es: `.sdd/drafts/YYYY-MM-DD_HH-MM_{resumen-5-palabras}/`
- Crea la carpeta y guarda la historia de usuario en `user-story.md` dentro de ella con la siguiente estructura:
  ```
  ---
  type: user-story
  ---

  {texto recibido tal cual}
  ```

**C) No se recibe ni ruta ni texto libre** (el skill se invoca sin argumentos):
- Busca la última historia de usuario existente en `.sdd/drafts/`:
  1. Lista las subcarpetas de `.sdd/drafts/` cuyo nombre empieza por `YYYY-MM-DD_HH-MM_` (regex `^[0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{2}-[0-9]{2}_`).
  2. Ordénalas alfabéticamente — el prefijo de timestamp hace que el orden alfabético coincida con el cronológico — y toma la última (la más reciente).
  3. Lee el fichero `user-story.md` dentro de esa carpeta.
- Si no existe ninguna carpeta con ese formato o la última no contiene `user-story.md`, indica al usuario que no hay historias de usuario previas y pídele que aporte una ruta o un texto libre. Detente.
- Si encuentras una historia, **muestra al usuario un resumen de dos líneas del  `user-story.md` junto con su ruta** y pregunta con `AskUserQuestion` si quiere usar esa historia:
  - Sí → trátalo como el caso (A): la carpeta de la iniciativa es la que contiene ese `user-story.md`. Continúa con la Fase 1.
  - No → indica al usuario que vuelva a invocar el skill pasando una ruta o un texto descriptivo. Detente.

En todos los casos, al llegar a la Fase 4 (guardar), se creará una **subcarpeta de análisis** numerada dentro de la carpeta de la iniciativa.

---

## Fase 1 — Exploración del contexto

Antes de hacer ninguna pregunta:

1. **Carga los skills que necesites para hacer bien tu trabajo.** Antes de diseñar nada, razona qué áreas cubre la solicitud y carga los skills correspondientes. Son la fuente de verdad sobre cómo se implementan las cosas en este proyecto — sin ellos, cualquier diseño que propongas puede ser incorrecto. Skills disponibles en este proyecto:
   - `k-validaciones` — **siempre** (categorías de validación, mensajes de error, campos calculados, ciclo de vida).
   - `k-sistemas` — si la solicitud crea o modifica entidades, servicios o controladores.
   - `k-vistas` — si la solicitud incluye listados, formularios, menús o navegación.
   - `k-seguridad` — si la solicitud incluye permisos, roles o restricciones por tipo de usuario.
2. Lee el CLAUDE.md del proyecto para entender las capas, convenciones y tipos de usuario.
3. Explora los sistemas/subsistemas existentes para identificar qué ya existe y qué habría que reutilizar:
   - `src/main/java/com/educaflow/subsystem/` y `src/main/java/com/educaflow/system/`
   - Si la historia menciona algo concreto (un subsistema, una entidad), léelo antes de preguntar.

   > **NUNCA leas ni uses como referencia `expedientes`, `tiposexpedientes` ni `tramites`** — siguen una arquitectura distinta y tomarlos como ejemplo lleva a análisis incorrectos.

   > **NUNCA leas otros ficheros `analysis.md` existentes en `.sdd/` como referencia.** El análisis que generes debe partir de la historia de usuario actual y la exploración del código real, no de análisis previos que documentan trabajo ya hecho. Usarlos como plantilla llevaría a replicar decisiones pasadas en vez de analizar la solicitud actual.
4. Identifica dependencias potenciales con subsistemas existentes (`common`, `firmas`, `registroentradasalida`, etc.).
5. **Comprueba si la solicitud es divisible.** Si cubre múltiples subsistemas o sistemas independientes (podrían implementarse y desplegarse por separado sin depender entre sí), propón al usuario dividirla en análisis separados antes de continuar. Cada análisis debe producir software funcional por sí solo.
6. Revisa la infraestructura de la carpeta `base/infrastructure/` para identificar si hay algo que puedas reutilizar (p.ej. generación de PDF, integración con sistemas externos, utilidades comunes…).
---

## Fase 2 — Preguntas iterativas

Haz preguntas usando AskUserQuestion en rondas de 12 como máximo. Espera la respuesta antes de continuar. Para cuando tengas respuesta clara a todos los puntos de la lista de información necesaria continua. Para cada pregunta, explícala muy bien porque a veces no está clara las consecuencias de cada decisión. 

### Información necesaria

**Tipo y ubicación:**
- ¿Sistema o subsistema? Si no está claro, explica la diferencia y ayuda a decidir.
- ¿Nombre técnico (inglés, camelCase)?
- ¿Dependencias de subsistemas existentes?

**Dominio:**
- ¿Qué entidades? Para cada una: nombre, campos, relaciones.
- ¿Alguna tiene estados o ciclo de vida? ¿Cuáles son los estados y transiciones?
- ¿Alguna extiende algo existente?

**Lógica de negocio:**
- ¿Qué operaciones expone la interfaz? (crear, editar, aprobar, rechazar, firmar…)
- ¿Hay reglas de validación? (campos obligatorios condicionales, restricciones de negocio, unicidad…)
- ¿Necesita PDF, firmas digitales, registro de entrada/salida u otros subsistemas?

**Vistas:**
- ¿Qué vistas necesita? (listado, formulario editable, formulario solo lectura…)
- ¿Hay relaciones maestro-detalle inline?
- ¿Menús nuevos? ¿Dónde encajan?

**Seguridad:**
- ¿Qué tipos de usuario pueden ver o editar cada cosa?
- ¿Los datos son por centro (multicentro) o globales?

**Recursos y datos iniciales:**
- ¿Plantillas PDF, esquemas XSD, certificados u otros recursos en classpath?
- ¿Datos precargados al arrancar? (roles, tipos, configuraciones…)

### Cuándo parar de preguntar

Para cuando:
- Sabes exactamente qué entidades crear y sus campos.
- Sabes qué operaciones expone la interfaz y sus reglas de negocio.
- Sabes qué vistas hay y cómo se navega entre ellas.
- Sabes quién accede a qué y con qué restricciones.
- Sabes qué se valida en cada operación y qué mensaje se muestra al usuario.
- No quedan ambigüedades que bloqueen el diseño.

Si una pregunta tiene un valor por defecto razonable, no la hagas — asúmelo en el borrador y permite que el usuario lo corrija.

---

## Fase 3 — Realización del análisis funcional

Esta fase tiene **dos tareas obligatorias y secuenciales**: primero generar 5 análisis independientes en paralelo (Tarea 1), luego unificarlos en un único análisis (Tarea 2).

### Tarea 1 — Lanzar 5 subagentes independientes en paralelo

**REGLA CRÍTICA:** Debes lanzar **exactamente 5 subagentes en paralelo**, en una **única respuesta** que contenga 5 invocaciones a la herramienta `Agent` simultáneas. No los lances secuencialmente. No hagas iteraciones internas dentro de un solo subagente. Cada subagente debe partir de un contexto fresco e independiente.

**Por qué 5 en paralelo y no iteraciones:** cada subagente con contexto aislado produce decisiones de diseño genuinamente independientes. Las iteraciones dentro de un mismo agente tienden a refinar la misma línea de razonamiento sin explorar alternativas. La diversidad sale de la independencia, no de la repetición.

**Cómo lanzarlos:**
1. Prepara **un prompt único y autocontenido** que incluya:
   - La historia de usuario completa (texto literal del fichero `user-story.md`).
   - Todas las respuestas del usuario obtenidas en la Fase 2 (preguntas y respuestas literales).
   - El contexto técnico relevante explorado en la Fase 1: entidades existentes que se reutilizan (con su FQN — `com.educaflow.subsystem.X.db.Y`), infraestructura disponible en `base/infrastructure/`, dependencias previstas con otros subsistemas.
   - Los tipos de usuario y cargos del proyecto cuando aplique a seguridad.
   - Las reglas de `k-validaciones` que el subagente debe aplicar (resumidas inline; el subagente no carga skills).
   - El formato de salida esperado.
   - **Las 3 tareas internas que debe ejecutar el subagente** (ver más abajo): producir las secciones del análisis, construir la tabla `V-XXX`, y aplicar el checklist.
   - La instrucción de producir **un único análisis completo**, no iteraciones ni múltiples versiones.
2. **Envía una sola respuesta con 5 bloques `Agent`**, todos con el **mismo prompt**, en paralelo. No uses `run_in_background`: necesitas los resultados para la Tarea 2.
3. Cada subagente debe devolver únicamente el análisis en markdown, sin metacomentarios y **sin escribir ningún fichero** — sólo el contenido del análisis en su mensaje de respuesta.

**Tareas internas que el prompt debe encargar a cada subagente:**

El prompt debe instruir al subagente a ejecutar **estas tres tareas en este orden**:

- **Tarea 1 del subagente — Producir las secciones del análisis**: tipo y capa, descripción, entidades (campos, tipos, relaciones, estados), dependencias de otros subsistemas, operaciones, vistas, menús, seguridad (multicentro sí/no), máquina de estados (si aplica) y campos calculados (si aplica).
- **Tarea 2 del subagente — Construir la tabla única `V-XXX` y la sección de asunciones**: incluir todas las reglas con sus columnas mínimas (`ID`, `Campo(s)`, `Tipo`, `Origen`, `Condición de aplicación`, `Mensaje al usuario`), marcando con `*` las de Negocio asumida y listándolas en "Asunciones a confirmar". Las reglas dependientes de estado comparten la misma secuencia `V-XXX`, no se abren tablas paralelas. Cada mensaje incluye el valor recibido y, en dominios finitos, los valores válidos.
- **Tarea 3 del subagente — Aplicar el checklist y corregir antes de devolver**: revisar el análisis generado contra el checklist que aparece más abajo (es el mismo que el agente principal aplicará en la unificación); si encuentra algún incumplimiento, corregirlo antes de devolver el resultado. El subagente NO debe devolver el análisis si queda algún punto del checklist sin cumplir.

**REGLA CRÍTICA — Frontera entre análisis y diseño (transmitir literalmente al subagente):**

El análisis describe **QUÉ** se necesita en términos funcionales y de negocio. **NUNCA** describe **CÓMO** se va a implementar. La elección de clases, métodos, ficheros, nombres de acciones del framework, lenguajes de consulta o cualquier detalle técnico es responsabilidad exclusiva del diseñador (`sdd-designer-system`), no del analista.

Está **PROHIBIDO** en cualquier sección del análisis:

- Nombres de clases Java o paquetes (`TareaCorreoService`, `XxxController`, `XxxRepository`, `XxxImpl`, FQN como `com.educaflow.subsystem.x.db.Y`).
- Signaturas de método con paréntesis y parámetros (`enviar(centro, para, asunto, …)`, `validateInsert(...)`, `reenviar(ActionRequest, ActionResponse)`).
- Tipos del framework como elementos del análisis (`ActionRequest`, `ActionResponse`, `ModelService`, `BusinessMessages`, anotaciones `@CallMethod`/`@Inject`).
- Nombres técnicos de acciones, vistas o formularios del framework Axelor (`subsysX.Entidad@Main-action`, `@All-action`, `@Centro-action`, `@Search-grid`, `@View-form`, `@Main-form`).
- Consultas o expresiones de código (JPQL, SQL, Groovy, expresiones `self.X = :user`, `eval:`, dominios Axelor literales).
- Decisiones de implementación (transacciones JPA, hilos background, listeners, módulos Guice, factorías, `fireActionRule_*`).
- Detalles de capa (que algo está "en el servicio", "en el controlador", "en el repositorio" o "en validateInsert").

Cada sección debe describirse al nivel funcional adecuado:

| Sección | Qué SÍ va | Qué NO va |
|---------|-----------|-----------|
| **Operaciones** | Nombre funcional de la operación, quién la ejecuta, qué entradas conceptuales necesita, qué efecto produce, restricciones de negocio para invocarla. | Nombres de clases, signaturas Java, tipos del framework, ubicación en capa. |
| **Vistas** | Nombre funcional ("Todos los correos"), quién la ve, filtro aplicado en lenguaje natural, modo (lectura/edición), descripción de qué muestra y qué acciones permite. | Nombres `@Main-action`, `@Search-grid`, `@View-form` ni convenciones de nombres del framework. |
| **Menús** | Ítem de menú, ruta jerárquica, vista funcional destino, quién lo ve. | Nombres de acciones del framework (la asociación menú↔acción la decide el diseño). |
| **Seguridad** | Qué puede ver, crear, editar o borrar cada rol descrito en lenguaje natural. Multicentro sí/no. | Reglas JPQL, condiciones del framework, nombres técnicos de permisos. |
| **Campos calculados** | Qué representa, lógica funcional de cálculo, dependencias (otros campos), cuándo se recalcula. | Clases o métodos del framework (`SmtpCredentialSimplePassword.userName()`, `Beans.get(...)`, etc.). |
| **Validaciones** | El mensaje al usuario y la condición funcional. | Implementación: ni capa (cliente/servidor), ni `action-validate`/`validateInsert`, ni nombres de acciones. |

**Ejemplos de MAL vs BIEN:**

| MAL (es diseño) | BIEN (es análisis) |
|------------------|---------------------|
| `TareaCorreoService.enviar(centro, para, asunto, ...)`: crea registro, persiste, lanza hilo background con transacción JPA | **Enviar correo**: cualquier sistema solicita el envío indicando destinatario, asunto, cuerpo, adjuntos opcionales y centro. El sistema crea un registro inmutable e intenta el envío. |
| Vista `TareaCorreo@Centro-action` con dominio `self.centro = :user.centroActivo` | Vista **Correos del centro**: lista los correos cuyo centro coincide con el centro activo del usuario logado. La ven Supervisor y Administrativa. |
| Menú "Mis correos" → `subsysCorreos.TareaCorreo@Propios-action` | Menú **Mis correos** (Notificaciones > Correos) → vista **Mis correos**. Lo ve el grupo de profesores, alumnos y familiares. |
| `admins`: regla JPQL sin filtro; `users`: `self.usuario = :user` | **Administrador**: ve todos los correos del sistema. **Profesor / Alumno / Familiar / Ex***: ve únicamente los correos cuyo destinatario coincide con su cuenta. |
| `de` se asigna desde `SmtpCredentialSimplePassword.userName()` en `insert()` | `de`: se asigna automáticamente con la dirección remitente configurada en el SMTP del sistema, en el momento de la creación del registro. |

Si el subagente duda si algo es análisis o diseño, debe aplicar este criterio: **¿el negocio cambiaría su decisión si el framework subyacente fuera distinto (otro ORM, otra UI, otro lenguaje)?** Si la respuesta es no, va al diseño. Si la respuesta es sí, va al análisis.

**Estructura exacta del análisis que debe producir el subagente:**

```
## Análisis Funcional: <Nombre>

**Tipo:** sistema | subsistema
**Capa:** system/<nombre> | subsystem/<nombre>
**Descripción:** <Una frase>

### Entidades
- `NombreEntidad` — <campos clave, tipos, relaciones, estados si los hay>

### Dependencias de otros subsistemas
- `subsystem/X` — <por qué>

### Operaciones
- **<Operación>**: <descripción de lo que hace, quién la ejecuta, qué datos necesita>

### Vistas
- <Nombre funcional de la vista>: <qué muestra, quién la ve, filtro aplicado en lenguaje natural, modo (lectura/edición)>

### Menús
- <Ruta jerárquica de menú> → <vista funcional destino> (<quién lo ve>)

### Seguridad
- <Tipo de usuario>: puede <ver|editar|…> <qué>, en lenguaje natural (sin JPQL ni código)
- Multicentro: sí | no

### Validaciones
<una única tabla V-XXX>

### Máquina de estados (si aplica)

### Campos calculados (si aplica)

### Asunciones a confirmar
- <A1*: ...>
```

Y debe seguir todos los principios de `k-validaciones`:

- **Una única tabla de reglas `V-XXX`**. NO pre-clasificar reglas en cliente/servidor.
- Columnas mínimas: `ID`, `Campo(s)`, `Tipo`, `Origen`, `Condición de aplicación`, `Mensaje al usuario`.
- **Origen** de cada regla: `Modelo` / `Catálogo` / `Negocio (asumida)`. Marcar las de Negocio asumida con `*` y listarlas en "Asunciones a confirmar".
- **Una regla, un campo, una cosa**: no agrupar campos salvo cruce genuino, no emitir reglas que se implican entre sí, no partir cliente/servidor en dos.
- **Reglas vs no-reglas**: no documentar lo que ya cubre el framework (FK válida, parser de tipo) ni decisiones de "esto NO se valida" (van como nota o asunción).
- **Ámbito de unicidad** explícito en cada regla de unicidad (global / por centro / por año / combinación). El mensaje refleja el ámbito.
- **Reglas configurables vs constantes técnicas**: nombrar el parámetro y proponer valor por defecto cuando sea configurable; identificar las constantes técnicas (impuestas por formato/protocolo/ORM) como tales.
- **Modelos sin UI / infraestructura interna**: reformular las reglas como invariantes que debe garantizar el servicio; mensajes técnicos para el desarrollador, no UX.
- **Solape entre reglas agregadas y específicas**: conservar solo la general cuando cubra a las particulares.
- **¿En qué modelo se documenta?** Integridad referencial al borrar (RESTRICT/CASCADE/SET NULL) va en el padre, no en el hijo. Unicidad y formato en el modelo que tiene el campo.
- **Máquina de estados**: estados (inicial/finales), transiciones permitidas con condición y acción posterior, transiciones inválidas con sus mensajes, tabla de campos editables por estado (`E`/`R`/`N`/`Auto`). Las reglas dependientes de estado **comparten la misma secuencia `V-XXX`**, no abrir tablas paralelas.
- **Campos calculados**: fórmula, dependencias, cuándo se recalcula, editable manualmente, posibles dependencias circulares.
- **Mensaje al usuario**: empieza por el campo o el valor, incluye el valor recibido y, en dominios finitos, los valores válidos. Sin tecnicismos del framework ("Axelor", "JPA", "constraint"…). Notas para el implementador van en columnas auxiliares o notas al pie, **nunca** en el mensaje. Para modelos sin UI el mensaje es técnico, dirigido al desarrollador, redactado como invariante violado.

Ejemplo del nivel de detalle esperado (extracto de la tabla):

| ID    | Campo  | Tipo         | Origen   | Condición | Mensaje al usuario                                                      |
|-------|--------|--------------|----------|-----------|--------------------------------------------------------------------------|
| V-001 | alias  | Dominio      | Negocio* | Siempre   | "El alias '{alias}' no existe en el slot {slot}. Disponibles: {lista}." |
| V-002 | email  | Formato      | Catálogo | Siempre   | "El formato del email '{email}' no es válido."                          |
| V-003 | dni    | Autorización | Negocio* | Siempre   | "El DNI '{dni}' no está autorizado. Contacte con secretaría."           |

La trazabilidad `V-XXX → paso(s) del diseño` es responsabilidad del diseñador, no del analista.

**Checklist que el subagente debe aplicar en su Tarea 3** (transmitir literalmente en el prompt; el subagente debe revisar el análisis punto por punto y corregir antes de devolverlo):

- [ ] ¿Cada entidad tiene sus campos, tipos y restricciones definidos?
- [ ] ¿Cada `required` del modelo tiene su regla `V-XXX` con mensaje al usuario?
- [ ] ¿Cada regla tiene columna `Origen` y las de Negocio asumida están marcadas con `*` y listadas en "Asunciones a confirmar"?
- [ ] ¿Cada regla de unicidad declara su ámbito (global / por centro / por año / combinación)?
- [ ] ¿Cada mensaje incluye el valor recibido y, en dominios finitos, los valores válidos, sin tecnicismos del framework?
- [ ] ¿No se han pre-clasificado reglas en cliente/servidor?
- [ ] ¿Las reglas dependientes de estado comparten la misma secuencia `V-XXX` (no se han abierto tablas paralelas)?
- [ ] ¿No se documentan reglas que el framework ya cubre (FK válida, parser de tipo)?
- [ ] ¿Las reglas configurables nombran su parámetro y proponen valor por defecto?
- [ ] ¿Las constantes técnicas (impuestas por formato/protocolo/ORM) están identificadas como tales?
- [ ] ¿La integridad referencial al borrar (RESTRICT/CASCADE/SET NULL) está documentada en el padre, no en el hijo?
- [ ] ¿No hay dependencias circulares entre sistemas/subsistemas?
- [ ] ¿Las vistas son coherentes con las entidades?
- [ ] ¿Hay ambigüedades que bloquearían el diseño? Si las hay, deben quedar listadas como asunciones a confirmar.
- [ ] ¿Cada mensaje empieza por el campo o el valor, sin notas para el implementador embebidas?
- [ ] **¿La sección de operaciones está libre de nombres de clase, signaturas de método, tipos del framework y referencias a capas técnicas?**
- [ ] **¿La sección de vistas está libre de nombres técnicos del framework Axelor (`@Main-action`, `@All-action`, `@Search-grid`, `@View-form`, `@Main-form`, etc.)?**
- [ ] **¿La sección de menús describe la asociación menú → vista funcional, sin nombres de acciones del framework?**
- [ ] **¿La sección de seguridad está descrita en lenguaje natural, sin JPQL ni expresiones de código (`self.X = :user`, dominios literales, etc.)?**
- [ ] **¿La sección de campos calculados describe la lógica funcional sin mencionar clases ni métodos del framework?**

Si el subagente detecta algún incumplimiento, debe corregirlo antes de devolver el análisis. Sólo devolverá el análisis cuando todos los puntos del checklist estén satisfechos.

### Tarea 2 — Unificar los 5 análisis

Una vez recibidos los 5 análisis, **tú mismo** (no un subagente) produces el análisis final unificado:

1. **Compara los 5 análisis** entidad por entidad, sección por sección.
2. **Para cada decisión donde haya divergencia**, escoge la mejor opción según los principios de `k-validaciones` y `k-sistemas`. Cuando haya empate razonable, elige la opción que minimiza ambigüedad para el diseñador.
3. **Para cada validación**, consolida en una única tabla `V-XXX`:
   - Si una regla aparece en varios análisis con redacciones distintas, escoge la redacción más precisa (con valor recibido, dominio finito, condición clara).
   - Si una regla aparece en algunos análisis pero no en otros, evalúa si es genuina (incluirla) o redundante con otra regla más general (descartarla).
   - Renumera de forma consecutiva sin huecos.
4. **Para asunciones a confirmar**, agrupa todas las asunciones marcadas con `*` de los 5 análisis, elimina duplicados y razónalas.
5. **Aplica el checklist final antes de presentar al usuario**. Es el mismo que cada subagente aplicó en su Tarea 3 sobre su propio análisis, pero debes volver a aplicarlo aquí sobre el **análisis unificado** — la unificación puede haber introducido inconsistencias (numeración, redacciones mezcladas, asunciones combinadas) que ningún subagente individual podía detectar:
   - ¿Cada entidad tiene sus campos, tipos y restricciones definidos?
   - ¿Cada `required` del modelo tiene su regla `V-XXX` con mensaje al usuario?
   - ¿Cada regla tiene columna `Origen` y las de Negocio asumida están marcadas con `*` y listadas en "Asunciones a confirmar"?
   - ¿Cada regla de unicidad declara su ámbito (global / por centro / por año / combinación)?
   - ¿Cada mensaje incluye el valor recibido y, en dominios finitos, los valores válidos, sin tecnicismos del framework?
   - ¿No se han pre-clasificado reglas en cliente/servidor?
   - ¿Las reglas dependientes de estado comparten la misma secuencia `V-XXX` (no se han abierto tablas paralelas)?
   - ¿No se documentan reglas que el framework ya cubre (FK válida, parser de tipo)?
   - ¿Las reglas configurables nombran su parámetro y proponen valor por defecto?
   - ¿Las constantes técnicas (impuestas por formato/protocolo/ORM) están identificadas como tales?
   - ¿La integridad referencial al borrar (RESTRICT/CASCADE/SET NULL) está documentada en el padre, no en el hijo?
   - ¿No hay dependencias circulares entre sistemas/subsistemas?
   - ¿Las vistas son coherentes con las entidades?
   - ¿Hay ambigüedades que bloquearían la implementación? Si las hay, deben quedar listadas como asunciones a confirmar.
   - ¿Cada mensaje empieza por el campo o el valor, sin notas para el implementador embebidas?
   - **¿La sección de operaciones está libre de nombres de clase, signaturas de método, tipos del framework y referencias a capas técnicas?**
   - **¿La sección de vistas está libre de nombres técnicos del framework Axelor (`@Main-action`, `@All-action`, `@Search-grid`, `@View-form`, `@Main-form`, etc.)?**
   - **¿La sección de menús describe la asociación menú → vista funcional, sin nombres de acciones del framework?**
   - **¿La sección de seguridad está descrita en lenguaje natural, sin JPQL ni expresiones de código?**
   - **¿La sección de campos calculados describe la lógica funcional sin mencionar clases ni métodos del framework?**

Si en la unificación detectas algo ambiguo o faltante que ninguno de los 5 análisis resolvió, añádelo a "Asunciones a confirmar".

El resultado de la Tarea 2 es el **borrador final** que presentarás al usuario para su aprobación.

---

## Fase 4 — Guardar el análisis

Solo tras aprobación, guarda el análisis.

> **REGLA OBLIGATORIA — ruta:** se crea una **subcarpeta de análisis** dentro de la carpeta
> de la iniciativa, con el nombre `analysis_NN` donde NN es el siguiente número disponible
> (cuenta las carpetas `analysis_*/` existentes en la iniciativa y suma 1; formato de 2 dígitos: 01, 02…).
> Dentro de esa subcarpeta se guarda el fichero con el nombre fijo `analysis.md`.
>
> Ejemplo:
> ```
> .sdd/drafts/2025-05-07_10-30_gestion-firmas-digitales/
> └── analysis_01/
>     └── analysis.md
> ```
>
> Pueden existir varias subcarpetas `analysis_*/` en la misma carpeta de iniciativa (iteraciones sucesivas por cada vez que se ejecuta este skill).
> **Nunca en la raíz del proyecto ni en ninguna otra carpeta.**

El fichero guardado debe comenzar **obligatoriamente** con la siguiente cabecera frontmatter, seguida del contenido del borrador aprobado:

```
---
type: analysis
---

{contenido del análisis} 
```

### Transición al planner

Al finalizar, indica al usuario:

```
Análisis guardado en .sdd/drafts/{carpeta-iniciativa}/analysis_NN/analysis.md

Para generar el plan de implementación ejecuta:
  /sdd-designer-system .sdd/drafts/{carpeta-iniciativa}/analysis_NN/analysis.md
```

No lances `sdd-designer-system` tú mismo. El usuario decide cuándo ejecutarlo.
