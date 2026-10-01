# Decisiones de diseño

## D1 — API de `NumeroTelefono`: tolerante o que lanza al construirse

**Problema:** las guías obligan a una clase `base.util.NumeroTelefono` que recibe el teléfono como `String` en el constructor, que oculte por completo `libphonenumber` y que sepa validar, decir si es móvil, decir si es de España y dar el E.164. El teléfono llega **del cliente** y puede ser cualquier cosa (`"60011"`, `"pepe"`, `null`). Quién trata el caso «no se puede ni parsear» admite más de una solución, y la elección decide si `validateInsert` necesita `try/catch` (algo que `k-sistemas` prohíbe: los `validate*` acumulan en `BusinessMessages` y no capturan excepciones).

**Alternativas:**
- **A — Constructor tolerante + consultas totales:** el constructor no lanza nunca; guarda el número parseado o `null` si no se pudo parsear. `esValido()`, `esMovil()` y `esDeEspana()` —los tres **privados**— y `esMovilDeEspana()` devuelven `false` cuando no hay número parseado (nunca lanzan), y `enFormatoE164()` lanza `IllegalStateException` si `esMovilDeEspana()` es `false`. Coste: hay que documentar que `enFormatoE164()` exige haber validado antes (lo garantiza el orden `validateInsert` → `fireActionRule_NormalizarTelefono`). El siguiente desarrollador tendrá que recordar: que el E.164 solo se pide después de validar; nada más (si se le olvida, salta un `IllegalStateException` inmediato y evidente, no un dato corrupto en BD).
- **B — Constructor que lanza `IllegalArgumentException` si no parsea:** el objeto solo existe si es un teléfono. Coste: `validateInsert` tendría que envolver la construcción en `try/catch` para convertir la excepción en `BusinessMessage` (anti-patrón de `k-sistemas`: «las validaciones nunca capturan excepciones, acumulan en `BusinessMessages`»), y el mismo `try/catch` se repetiría en cualquier otro sitio que valide un teléfono. El siguiente desarrollador tendrá que recordar: envolver toda construcción en `try/catch`, y que un teléfono inválido es una excepción y no un `false`.
- **C — Utilidad estática tipo `DniUtil.isValid(String)`:** coste: contradice la guía («recibe el teléfono como `String` en el constructor») y obliga a re-parsear el número una vez por pregunta (validar, móvil, España, E.164 = 4 parseos). El siguiente desarrollador tendrá que recordar: que cada llamada vuelve a parsear.

**Elegida:** A — es la única que deja los `validate*` limpios (sin `try/catch`) y mantiene cada método **total dentro de su rama** (`disenyo.md` §«El retorno defensivo que delega»): ninguna consulta devuelve «válido» ante un dato que no entiende, y el único método que no puede responder con un booleano (`enFormatoE164()`) **falla en lugar de inventar** un valor. Evita además el olor «dos piezas que se conocen entre sí» de B (validador + `catch` siempre emparejados) y el parseo repetido de C. Los métodos son exactamente las capacidades que enumera `design-guidelines.md` («validar, saber si es móvil, saber si es de España, obtener el formato E.164…»), con una más, `esMovilDeEspana()`, que **empaqueta la clasificación de negocio** de RES-Sms-004: la conjunción de las tres preguntas vive dentro de `NumeroTelefono`, que es quien sabe responderlas, y **no** compuesta a mano en `SmsServiceImpl.validarTelefono`. Así la clasificación tiene **un solo dueño** (`disenyo.md` §«Una decisión con varios dueños»): V-Sms-006 hace una única pregunta y `enFormatoE164()` usa esa misma pregunta como guarda, en vez de la más débil `esValido()` —que dejaría pasar un móvil francés válido y guardaría un `+33…` rompiendo RES-Sms-004—. No es superficie inventada: es la única forma de que no haya que acordarse de componer tres consultas en el mismo orden cada vez. Y por eso mismo la **superficie pública son solo dos métodos**, `esMovilDeEspana()` y `enFormatoE164()`: `esValido()`, `esMovil()` y `esDeEspana()` bajan a **privados** (siguen siendo los tres factores que el primero compone). No tienen ningún llamador en el diseño, y publicarlos invitaría justo a la composición a mano que esta decisión quiere evitar —además de dejar a la vista `esValido()`, que como guarda sería un bug documentado (dejaría pasar un móvil francés). Menos superficie, el mismo comportamiento (ISP, `k-code-quality/clases.md`).

**Patrón nuevo:** NO — es una clase de `base/util` como `DniUtil`/`EMailUtil`/`IbanUtil`; solo cambia que es de instancia (lo exige la guía) porque encapsula un parseo caro reutilizado por todas sus consultas.

---

## D2 — Dónde vive el cálculo de si el mensaje cabe en un SMS (GSM-7 / UCS-2)

**Problema:** VAL-Sms-008 exige el algoritmo de las guías: ver si todo el texto es codificable en GSM-7, contar unidades (los caracteres de la tabla extendida cuentan 2) con límite 160, y si no, contar caracteres UCS-2 con límite 70. Son dos tablas de caracteres y una cuenta: ni el spec ni las recetas dicen dónde vive esa pieza, y es lo primero que hará falta otra vez cuando otro subsistema mande un SMS.

**Alternativas:**
- **A — Métodos privados de `SmsServiceImpl`:** el servicio se queda con las dos tablas de caracteres y la cuenta. Coste: mete conocimiento de codificación de telefonía en un servicio de negocio, sube su complejidad (riesgo de `crapCheck`) y el siguiente subsistema que necesite la misma cuenta la copiará (y `cpdCheck` la parará, o peor, no llegará a los 100 tokens y quedará duplicada en silencio). El siguiente desarrollador tendrá que recordar: que la cuenta está escondida en el servicio de SMS y que hay que sacarla de ahí antes de reutilizarla.
- **B — `base/util/MensajeSmsUtil.cabeEnUnSms(String)`:** helper `static` sin estado, junto a `DniUtil`, `EMailUtil` e `IbanUtil`, que son exactamente lo mismo (validadores de formato de bajo nivel, sin dominio de negocio). Coste: un fichero nuevo en `base/util` y una línea en su `CLAUDE.md`. El siguiente desarrollador tendrá que recordar: nada — `base/util/CLAUDE.md` obliga a mirar ahí antes de escribir cualquier validación y lo encontrará listado.
- **C — Dentro de `base/infrastructure/sms`** (junto a `Sms`, `SmsSender`, `SmsSenderFactory`), p. ej. como método del record `Sms`. Coste: convierte «el texto cabe en un SMS» en un invariante del transporte, de modo que construir un `Sms` con un texto largo lanzaría una excepción técnica en vez de producir el mensaje de negocio de VAL-Sms-008; y el validador de negocio necesitaría `try/catch` (el problema de D1-B otra vez). El siguiente desarrollador tendrá que recordar: que la comprobación de negocio se hace construyendo un objeto de transporte y cazando su excepción.

**Elegida:** B — respeta el mandato explícito de `base/util/CLAUDE.md` («si necesitas un helper nuevo de carácter genérico y reutilizable, añádelo aquí y actualiza este fichero») y sigue el patrón de los tres validadores de formato que ya existen, así que no hay divergencia con los hermanos. Deja **un único dueño** de la clasificación «este texto cabe / no cabe» (`disenyo.md` §«Una decisión con varios dueños»: la vista no la replica, el modelo no la replica con un `max`, el servicio la consulta) y mantiene la regla de negocio como `BusinessMessage`, no como excepción. Evita además el olor de C, que reparte la misma decisión entre un invariante técnico y una validación de negocio.

Se valoró también moverla al paquete existente `base/infrastructure/sms` (donde viven `Sms`, `SmsSender`, `TwilioCredential` y `SmsSenderFactory`) para no partir el tema «SMS» entre dos paquetes de `base`, y **se descarta**: `base/infrastructure/CLAUDE.md` cierra con una regla explícita —«Las utilidades de **bajo nivel** (sin estado, `static`) viven en `base.util`, no aquí»— y `MensajeSmsUtil` es exactamente eso. Meterla ahí obligaría a reescribir esa regla global para una sola clase, y dejaría `base/util` y `base/infrastructure` diciendo cosas distintas sobre dónde va un helper estático. La cohesión temática la da el nombre de la clase, no el paquete: `DniUtil`, `EMailUtil` e `IbanUtil` están en el mismo caso.

**Patrón nuevo:** NO — pieza común en `src/main/java/com/educaflow/base/util/MensajeSmsUtil.java`, familia ya documentada en `base/util/CLAUDE.md` (que este diseño manda actualizar). El público `cabeEnUnSms(String)` es **una sola expresión** que compone dos privados (`esCodificableEnGsm7`, `contarUnidadesGsm7`), porque decidir la codificación y contar unidades son dos responsabilidades distintas (`k-code-quality/metodos.md` §«Descomposición de métodos»), y rechaza el mensaje ausente o en blanco con `TextUtil.requireNonBlank` (nunca `Objects.requireNonNull` sobre un `String`).

---

## D3 — Envío asíncrono tras el commit sin duplicar el mecanismo de correos

**Problema:** las guías obligan a replicar el envío asíncrono de `subsystem/correos`, que son dos piezas: `PostCommitRunner` (registra una tarea para que corra solo si la transacción hace commit) y `CorreoAsyncExecutor` (pool fijo de hilos daemon con parada ordenada). Copiarlas a `subsystem/sms` **rompe el build**: `cpdCheck` está enganchado a `check` y salta con duplicados de ≥100 tokens, y `CorreoAsyncExecutor` (~50 líneas de código real) los supera de largo. Y `CLAUDE.md` dice que un duplicado se arregla «extrayendo el código común en su sitio», no con `// CPD-OFF` (reservado a duplicación deliberada).

**Alternativas:**
- **A — Copiar las dos clases a `subsystem/sms/infrastructure/`:** coste: `./run.sh` falla en `cpdCheck`; taparlo con `// CPD-OFF` sería declarar deliberada una duplicación que no lo es. El siguiente desarrollador tendrá que recordar: mantener a mano dos (luego tres) copias del mismo pool de hilos.
- **B — Que `subsystem/sms` importe las clases de `subsystem/correos`:** coste: acopla dos subsistemas sin relación funcional (los SMS no dependen del correo) y deja infraestructura genérica enterrada en el subsistema que la escribió primero. El siguiente desarrollador tendrá que recordar: que para mandar algo en segundo plano hay que depender del subsistema de correos.
- **C — Extraer **una** clase a `base/infrastructure/async/EjecutorAsincrono`, fundiendo las dos piezas en un único método `ejecutarTrasCommit(Runnable)`, y darle a cada subsistema su propio pool con una subclase de 5 líneas (`SmsAsyncExecutor`, `CorreoAsyncExecutor`) que fija su tamaño y el prefijo del nombre de sus hilos:** cada subclase da además a Guice una clave distinta por pool sin inventar anotaciones. Coste: el andamiaje que rodea al pool (subclase + `Provider` que lee su `*.pool-size` + observer que lo para) hay que **repetirlo en cada subsistema**, así que son 3 clases y 2 bindings por subsistema y una clave de configuración por subsistema — seis clases para «tener un pool de hilos». Y el aislamiento que compra no lo pide nadie: ni la spec ni `design-guidelines.md` hablan de separar los pools. El siguiente desarrollador tendrá que recordar: que para lanzar algo en segundo plano hay que escribir su trío de clases y su clave de configuración.
- **D — Sustituir el pool por un job Quartz que barra los PENDIENTE:** coste: contradice la guía («igual que correos»), añade un `MetaSchedule` y retrasa el envío al siguiente tick. El siguiente desarrollador tendrá que recordar: que el envío no ocurre al guardar sino cuando pasa el cron.
- **E — Extraer esa misma clase y tener UN SOLO ejecutor para toda la aplicación:** `EjecutorAsincrono` se bindea como singleton en `SecretariaVirtualModule` (donde ya se cablea lo transversal), su tamaño lo lee un único `EjecutorAsincronoProvider` de `async.pool-size` y lo para el `AppEventObserver` que la aplicación **ya tiene**; quien necesite segundo plano lo **inyecta**. Correos deja de tener pool, `Provider` y observer propios. Coste: toca más ficheros de correos (4 clases que desaparecen, `CorreosModule` con un `bind` menos, `CorreoServiceImpl` con el campo inyectado cambiado, sus dos `fireActionRule_Programar*Asincrono` —copias byte a byte— fundidos en uno solo al que llaman `insert` y `reenviar`, y su privado `trazaCompleta` movido a `TextUtil` para que el texto de `descripcionUltimoFallo` no tenga un dueño por subsistema —aprovechando que el fichero se reescribe igualmente—) y los correos y los SMS comparten los hilos, así que un envío lento de uno puede hacer esperar al otro. El siguiente desarrollador tendrá que recordar: nada — `@Inject EjecutorAsincrono` y `ejecutarTrasCommit(...)`; no hay nada que escribir. Y lo único que podría olvidarse (llamarlo fuera de transacción) **no queda a su memoria**: el contrato del método declara esa tercera entrada y lanza `IllegalStateException` en el acto, en vez de descartar la tarea en silencio o ejecutarla antes de tiempo.

**Elegida:** E — como C, no rompe `cpdCheck` ni acopla subsistemas hermanos por dependencia de código, y la fusión de las dos piezas en `ejecutarTrasCommit` elimina el conocimiento tácito que hoy tiene correos: con dos piezas separadas, quien las use **tiene que acordarse** de envolver el `submit` en `runAfterCommit` (si no, el hilo puede no ver todavía la fila y el bug es intermitente) — exactamente el «acuérdate de hacer también W» que hace fallar la **prueba del segundo desarrollador** de `disenyo.md`, y el olor «dos piezas que se conocen entre sí». De paso simplifica los tests de correos (dejan de necesitar `Mockito.mockStatic(PostCommitRunner.class)`: basta verificar el mock del ejecutor).

Frente a C, E gana en lo único que estaba en juego: **cuántas piezas hay que recordar**. C deja el mecanismo extraído pero vuelve a duplicar todo lo que lo rodea (subclase + `Provider` + observer + clave de configuración, una vez por subsistema): seis clases y dos claves para tener un pool de hilos, y la promesa implícita de escribir otras tres en el siguiente subsistema. E deja **una** clase reutilizable, **un** `Provider`, **una** clave y **ningún** observer nuevo, y hace que `subsystem/sms` no aporte ni una sola clase de infraestructura asíncrona. El aislamiento de pools por subsistema que C compra **no lo pide nadie** —ni la spec ni las guías— y su precio (repetir el andamiaje) se paga en cada subsistema nuevo; si algún día un subsistema necesita de verdad su propio pool, se le da entonces y se justifica entonces. La contrapartida real de E, que correos y SMS compartan hilos, es asumible con el mismo tamaño de pool por defecto (2) y volumen actual, y se corrige subiendo `async.pool-size` sin tocar código.

**Patrón nuevo:** SÍ — pieza común en `src/main/java/com/educaflow/base/infrastructure/async/EjecutorAsincrono.java` (nuevo paquete `async` de `base/infrastructure`, a documentar en `base/infrastructure/CLAUDE.md`). Receta que faltaría escribir: en `k-sistemas` (o en un `k-async`), «cómo se lanza trabajo en segundo plano tras el commit desde un `*ServiceImpl`», y cabe en una línea: **inyectar `EjecutorAsincrono` y llamar a `ejecutarTrasCommit(...)` desde un `fireActionRule_*` `Después`, dentro de la transacción de la acción** (sin transacción activa el método falla con `IllegalStateException`, que es su tercera entrada declarada); no hay pool, `Provider` ni observer que escribir por subsistema. Hasta que exista, este diseño y su `rules/R-Sms-003.md` son la referencia.

---

## D4 — Qué pasa cuando no hay credenciales del proveedor de SMS

**Problema:** `TwilioCredential` rechaza en su constructor cualquier valor `null`/blanco, y las guías dicen que las tres propiedades nuevas van **vacías** en `axelor-config.properties` (los valores reales, en la config privada). Correos inyecta su `MailSender` directamente (`@Inject MailSender`), así que con credenciales vacías el `Provider` reventaría al construir el servicio: un `500` al guardar y ni una fila en BD. Pero el spec exige lo contrario (ESC-001: «Si no [se acepta], el SMS aparece con estado Fallido y sin fecha de envío»; ESC-020 igual), es decir, que la falta de proveedor se **registre como fallo del SMS**.

**Alternativas:**
- **A — Copiar correos: `@Inject SmsSender smsSender`:** coste: el `Provider` se ejecuta al construir `SmsServiceImpl`, así que sin credenciales reales *ni se puede dar de alta* un SMS; el alta muere con un error técnico en vez de quedar FALLIDO y ESC-001/016/017/020/023 se vuelven inejecutables en un entorno sin cuenta de Twilio. El siguiente desarrollador tendrá que recordar: que el subsistema entero no arranca sin credenciales, aunque solo quiera dar de alta SMS.
- **B — `@Inject jakarta.inject.Provider<SmsSender>` y resolverlo **dentro** del `try` del intento de envío:** la construcción del emisor (leer configuración + validar credenciales) pasa a ser parte del intento, así que una credencial vacía o mal puesta cae por el mismo camino que un rechazo del proveedor: `fireActionRule_MarcarEnvioFallido` con la descripción del fallo. Coste: una línea distinta de correos (`smsSenderProvider.get().send(...)`) que hay que justificar. El siguiente desarrollador tendrá que recordar: nada — el tipo `Provider<SmsSender>` ya dice que el emisor se fabrica en cada intento.
- **C — Que `SmsSenderProvider` devuelva un emisor «nulo» cuando falten credenciales:** coste: un `SmsSender` que no envía y no falla es un `return` defensivo que miente; el SMS quedaría ENVIADO sin haberse enviado. El siguiente desarrollador tendrá que recordar: que «Enviado» puede significar «no configurado».

**Elegida:** B — es lo que el spec pide (el fallo del proveedor es un estado del SMS, no un error de la aplicación) y deja el mecanismo **total dentro de su rama**: todo intento de envío termina en ENVIADO o en FALLIDO con motivo, sin ningún camino que se escape (`disenyo.md` §«Ramas que no cubren todos los casos»). Evita el olor de C (estado que miente) y la fragilidad de A, sin tocar correos. La divergencia con correos es de **una línea** y queda documentada aquí y en `rules/R-Sms-003.md`.

**Patrón nuevo:** NO — `Provider<T>` inyectado es Guice estándar (`k-guice` §3.3/§3.4: inyectar la fábrica en vez del objeto cuando la construcción depende de configuración).

---

## D5 — Visibilidad del menú «SMS → Del centro»

**Problema:** en correos, `MenuSecurityServiceImpl` **oculta** `correos-delCentro-menuitem` a quien no es SUPERVISOR ni ADMINISTRATIVO. El spec de SMS pide lo contrario de forma explícita: ESC-025 hace que un **alumno** «abra el menú SMS → Del centro» y vea el listado vacío, y `screen-sms-centro.md` dice «lo ven los usuarios de los centros (no el Administrador); **solo muestra datos** a quien es Supervisor o Administrativo». Hay que elegir entre imitar a correos o cumplir el escenario.

**Alternativas:**
- **A — Replicar correos: añadir `case "sms-delCentro-menuitem" -> supervisor || administrativo` en `MenuSecurityServiceImpl`:** coste: ESC-025 pasa a ser imposible de ejecutar (el paso 9 abre un menú que no existe para el alumno) y habría que declarar el escenario descartado. El siguiente desarrollador tendrá que recordar: que el menú de SMS y el de correos se ocultan igual, pero que el spec de SMS decía otra cosa.
- **B — No añadir ningún `case`: el menú se rige solo por `groups="users"` y el filtro de datos lo hace el `<domain>` del `action-view` (y el permiso `Sms.propio-centro-gestion`):** un usuario sin cargo de gestión ve la entrada y un listado vacío. Coste: divergencia visible con correos; `MenuSecurityServiceImpl` no se toca (menos superficie de cambio). El siguiente desarrollador tendrá que recordar: que en SMS la entrada «Del centro» la ven todos los `users` **a propósito**, porque el spec lo pide (está escrito en «Notas y supuestos»).

**Elegida:** B — el spec manda sobre la imitación: ESC-022 y ESC-025 fijan exactamente qué ve cada actor, y son verificables por E2E. Además la seguridad real no cambia (el alcance lo pone el `<domain>` + el permiso condicional, nunca el menú: `k-vistas/menus.md` «la visibilidad no autoriza nada»), así que la diferencia es de UX, no de defensa, y ahorra modificar una clase de otro subsistema (`subsystem/security`). La divergencia queda declarada aquí y en «Notas y supuestos» para que nadie la lea como un olvido.

**Corolario — dónde vive la lista `{SUPERVISOR, ADMINISTRATIVO}` y por qué no se envuelve en un método de `User`.**
La misma decisión de no tocar `subsystem/security` alcanza a la clasificación «quién gestiona un centro», que
V-Sms-015 necesita en el servidor. **MUST NOT** añadirse a la entidad compartida `User` un
`esGestorDelCentro(Centro)` para envolver los dos `tieneTipoUsuario(centro, …)`: no unificaría ninguna copia de la
lista —el `<domain>` de `views/Centro-Sms.xml` y la `condition` de `Sms.propio-centro-gestion` de
`data-init/input/auth-sms.xml` son **JPQL** y **no** pueden llamar a un método de la entidad, así que seguirían
llevando el literal `('SUPERVISOR','ADMINISTRATIVO')`— y pondría un método público más en una entidad de **otro**
subsistema para un solo llamador. **AVISO para quien cambie la lista** (añadir Director, Secretario…): está escrita
en **tres** sitios de este subsistema —la expresión de V-Sms-015 en `SmsServiceImpl.validateReenviar`, el `<domain>`
de `views/Centro-Sms.xml` y la `condition` de `Sms.propio-centro-gestion`— y en las dos copias hermanas de correos;
olvidar una falla **en silencio** (el grid muestra el SMS y el reenvío lo rechaza, o al revés). `MenuSecurityServiceImpl`
**no** es una cuarta copia de la misma pregunta: pregunta por «gestor de **algún** centro» (`tieneTipoUsuario(codigo)`,
sin centro), así que queda fuera del alcance igual que el resto de `subsystem/security`.

**Corolario — V-Sms-015 exige gestionar el centro, no pertenecer a él (diverge de correos).**
El hermano `CorreoServiceImpl.validateReenviar` (V-Correo-018) se conforma con `isAdmin` + `perteneceAlCentro`.
V-Sms-015 **MUST NOT** quedarse ahí: `perteneceAlCentro` es cierto para **cualquier** `CentroUsuario` del centro
(alumno, profesor, familiar, externo) y dejaría a un alumno relanzar envíos de SMS a terceros con un POST a la
acción. Que el botón no se vea, que el `<domain>` de `Centro-Sms.xml` filtre y que el permiso sea de solo lectura
son UI y lectura, no autorización (`k-secure-coding` §9 y §4: una VAL con actor se comprueba también en el
`validate*`), así que V-Sms-015 es la autorización real del reenvío. La expresión usa los `tieneTipoUsuario(centro, …)`
que ya existen en `User.xml` (devuelven `false` si el usuario no pertenece al centro: es null-safe). Y **MUST NOT**
unificarse con V-Sms-010: el alta la puede hacer cualquier usuario del centro (VAL-Sms-010) y el reenvío
solo la gestión del centro (VAL-Sms-013 + «Actores» del spec); son dos reglas distintas. Es más estricta que la
letra de VAL-Sms-013 a propósito (defensa en profundidad, `k-secure-coding` §9), con el mensaje del spec. Correos
**no** se toca en esta iniciativa: V-Correo-018 es candidata a converger.

**Patrón nuevo:** NO.

---

## D6 — RN-Sms-001: normalizar a E.164 un campo que aporta el cliente

**Problema:** el teléfono lo escribe el usuario (`600111222` o `+34600111222`) y RES-Sms-004 exige que en BD esté **siempre** en E.164. Pero `design-contract.md` §3 dice que un campo `cliente` **no** debe aparecer asignado por una regla `Antes` de crear, porque eso lo convertiría de hecho en `servidor`. Hay que materializar RN-Sms-001 sin romper esa regla ni abrir un agujero de mass-assignment.

**Alternativas:**
- **A — `telefono` sigue siendo `cliente` (está en la whitelist de `insert`) y `fireActionRule_NormalizarTelefono` lo reescribe de forma incondicional antes de `repository.save`, documentado como *canonicalización*:** el valor sigue siendo el del cliente; el servidor solo lo escribe en forma canónica, y no puede inventarlo (si no era un móvil de España válido, V-Sms-006 ya abortó la operación). Coste: hay que justificar por qué esta asignación `Antes` sobre un campo `cliente` no es la que §3 prohíbe. El siguiente desarrollador tendrá que recordar: que `telefono` se guarda normalizado y que la normalización va después de la validación.
- **B — Dos campos: `telefono` (`cliente`, tal cual) y `telefonoE164` (`servidor`, calculado):** coste: un campo que el spec no pide, dos valores que pueden divergir, dos columnas en el grid y una decisión más («¿cuál muestro?»). El siguiente desarrollador tendrá que recordar: cuál de los dos es el bueno para enviar y cuál para mostrar.
- **C — Normalizar en la vista (`onChange` del campo con el widget `phone`):** coste: la vista no es defensa (`k-secure-coding` §1): por `POST /ws/rest/<FQN>` entraría cualquier cadena y RES-Sms-004 dejaría de cumplirse. El siguiente desarrollador tendrá que recordar: que la única normalización vive en el cliente y que cualquier alta programática se la salta.

**Elegida:** A — es lo que el propio spec declara (RN-Sms-001, `fase: antes_de_commit`) y la única que garantiza el invariante en **todas** las puertas de entrada, incluida la del endpoint REST automático; C lo dejaría solo en la UI, el olor «defensa solo en la vista» de `disenyo.md`, y B añade una pieza que nadie pidió y una segunda verdad sobre el mismo dato. La tensión con `design-contract.md` §3 se resuelve por el **efecto real**: la regla de §3 existe para que un valor que dicta el servidor no se disfrace de entrada del usuario; aquí el servidor no aporta información nueva (el número es el del cliente, en otro formato) y no puede ejecutarse sobre un valor no validado, porque `validateInsert` corre antes y rechaza cualquier teléfono que no sea un móvil de España. Queda explícito en la tabla de «Frontera de confianza» y en «Notas y supuestos» para que el verificador no lo lea como una clasificación incoherente.

**Patrón nuevo:** NO.

---

## D7 — V-Sms-011: cómo se valida la referencia `historialEstado` que dicta el cliente (diverge de correos)

**Problema:** el hermano `CorreoServiceImpl.validarHistorialEstado` (V-Correo-014) busca el padre con `JpaRepository.of(HistorialEstado.class).find(id)` y **no** compara el centro. `k-secure-coding` §3.6 exige, para una referencia al padre que dicta el cliente, dos cosas: **(a)** que sea un padre real y **(b)** que el usuario esté autorizado sobre él. Hay que elegir entre copiar a correos o cubrir las dos mitades.

**Alternativas:**
- **A — Copiar a correos (releer por repositorio, sin comparar centro):** coste: viola **C27** de `agent_docs/architecture-rules.md` / `k-code-quality/proyecto.md` §«Fronteras entre subsistemas» (`HistorialEstado` es de `subsystem/expedientes`), y se queda en (a): por `/ws/rest` se puede mandar cualquier id, y como `nombreExpediente` es un `formula="true"` que navega `historialEstado → expediente.name` y es columna de los tres grids, colgar el SMS de un `HistorialEstado` de otro centro filtraría el nombre de un expediente ajeno a los gestores del centro del SMS y al destinatario en «Mis SMS». El siguiente desarrollador tendrá que recordar: que la referencia no está defendida.
- **B — Un único `if` sobre la instancia que ya trae la entidad (`sms.getHistorialEstado()`), sin releerla:** **(a)** `padre.getId() == null` rechaza la cáscara (`{"historialEstado": {}}` o una referencia programática sin resolver); un id **inexistente** no llega al servicio, porque `JPA.edit` resuelve la referencia con `em().find` antes de `insert` y aborta con `OptimisticLockException`. **(b)** el padre es del **mismo centro** que el SMS (`padre.getExpediente() == null` cae en el mismo rechazo, porque `HistorialEstado.expediente` no es `required`); el centro del SMS ya lo defiende V-Sms-010 en el mismo `validateInsert`. Releer por repositorio sería además **redundante**: por la puerta REST la instancia ya es la del contexto de persistencia. Coste: diverge de correos. El siguiente desarrollador tendrá que recordar: nada — la regla es una sola expresión y trata la referencia igual que `validarCentro` trata `sms.getCentro()`.

**Elegida:** B — cubre las dos mitades de `k-secure-coding` §3.6 sin cruzar la frontera C27. Si falla cualquier mitad, el mismo y **único** `BusinessMessage` de V-Sms-011 (no se distingue cuál, para no revelar qué ids existen), y el método **nunca** lanza (`k-validaciones` §2). Es más estricta que la letra de VAL-Sms-011, a propósito (`k-secure-coding` §9, defensa en profundidad), con el mensaje que fija el spec.

**Patrón nuevo:** SÍ — validar una referencia al padre de otro subsistema sobre la instancia que ya trae la entidad (`id != null` + mismo centro), sin releerla por repositorio. Correos **no** se toca en esta iniciativa (V-Correo-014 sigue como está): es su candidata a converger.

---

## D8 — `enviarSms`: privado y ruidoso ante un SMS inexistente (diverge de correos)

**Problema:** el hermano `CorreoService.enviarCorreo(Long)` es **público**, tiene en la interfaz a
`validateEnviarCorreo` y a `listarCorreosEnFail`, y ante un correo inexistente hace `return` silencioso (en la misma
rama que «ya enviado»). Hay que elegir entre copiarlo o resolver la tarea asíncrona de SMS de otra forma.

**Alternativas:**
- **A — Copiar a correos (método público + `validateEnviarSms` + `listarSmsEnFail`, `return` si no existe):** coste:
  el `validateEnviarSms` sería un stub vacío (`k-sistemas` lo prohíbe), `listarSmsEnFail` solo sirve al reenvío en
  bloque que el spec deja fuera de alcance (y arrastraría un `findByEstado`), y el `return` ante un `null` escondería
  un error real bajo la palabra «idempotencia». El siguiente desarrollador tendrá que recordar: que un SMS que no
  aparece no deja rastro.
- **B — `private void enviarSms(Long smsId)` y dos ramas distintas:** si `repository.find` devuelve `null`, lanza
  `IllegalStateException("No existe el SMS " + smsId)` —RES-Sms-002 («un SMS nunca se borra») garantiza la fila, así
  que un `null` es un error de programación o de datos y **MUST** fallar, no callar (`k-code-quality/disenyo.md`
  §«El retorno defensivo que delega»); el envoltorio de `EjecutorAsincrono` lo registra con `log.error` y el hilo
  sobrevive—; si ya está ENVIADO, `return` (idempotencia legítima: ENVIADO es terminal). Coste: diverge de correos.
  El siguiente desarrollador tendrá que recordar: nada — quien quiere mandar un SMS hace `insert` y el envío se
  programa solo.

**Elegida:** B — no es una acción del contrato público del subsistema, así que no necesita validador ni whitelist,
y separa el error real (SMS desaparecido) de la idempotencia legítima (ENVIADO).

**Patrón nuevo:** SÍ — tarea asíncrona privada del `*ServiceImpl`, que falla ante la entidad inexistente. Correos
**no** se toca en esta iniciativa (`enviarCorreo` sigue como está): es su candidata a converger, igual que en D7.
