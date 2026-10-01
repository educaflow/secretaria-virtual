---
type: implementation-task
template: system
---

# Tarea 19 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-validaciones
- k-secure-coding
- k-code-quality
- k-guice

### Fichero(s) de esta tarea (de «Ficheros a crear o modificar» de `design/design.md`)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/sms/service/SmsService.java` | Crear | k-sistemas (servicios.md) | Interfaz: `reenviar` + su validador + su `allowProperties` |
| `src/main/java/com/educaflow/subsystem/sms/service/impl/SmsServiceImpl.java` | Crear | k-sistemas (servicios.md), k-validaciones, k-secure-coding | V-Sms-001…015, R-Sms-001…006, whitelists |

### Paso 5 — Servicio `SmsService` / `SmsServiceImpl` (de `design/design.md`)

**Ficheros:** `subsystem/sms/service/SmsService.java` (Crear) y `subsystem/sms/service/impl/SmsServiceImpl.java` (Crear). Ningún fichero de otro subsistema: V-Sms-015 usa los `tieneTipoUsuario` que `subsystem/common/domains/User.xml` ya tiene, y V-Sms-011 se queda con el `HistorialEstado` que la propia entidad trae.

`ModelServiceFactory` descubre la implementación por su FQN, así que **MUST NOT** crearse ningún binding Guice para ella.

#### Interfaz `com.educaflow.subsystem.sms.service.SmsService extends ModelService<Sms>`

```java
Sms reenviar(Sms entidad, Sms entidadOriginal);
Optional<BusinessMessages> validateReenviar(Sms entidad, Sms entidadOriginal);
AllowProperties allowPropertiesReenviar();
```

Solo declara la acción propia del subsistema y sus dos compañeras obligatorias. **MUST NOT** re-declarar
`insert`/`update`/`remove` ni sus `validate*`/`allowProperties*`: vienen de `ModelService<Sms>` con defaults en
`DefaultModelService<Sms>`.

#### Clase `com.educaflow.subsystem.sms.service.impl.SmsServiceImpl`

`extends DefaultModelService<Sms> implements SmsService`.

```java
public SmsServiceImpl(Class<Sms> model, Repository<Sms> repository);   // super(model, repository)

@Override public Sms insert(Sms sms);
@Override public Sms update(Sms nuevo, Sms original);
@Override public void remove(Sms sms);
@Override public Sms reenviar(Sms entidad, Sms entidadOriginal);

/****************************************************************************************/
/******************************** Métodos de Validación *********************************/
/****************************************************************************************/
@Override public Optional<BusinessMessages> validateInsert(Sms sms);
private void validarDestinatario(Sms sms, BusinessMessages messages);
private void validarTelefono(Sms sms, BusinessMessages messages);
private void validarMensaje(Sms sms, BusinessMessages messages);
private void validarCentro(Sms sms, BusinessMessages messages);
private void validarHistorialEstado(Sms sms, BusinessMessages messages);
@Override public Optional<BusinessMessages> validateUpdate(Sms nuevo, Sms original);
@Override public Optional<BusinessMessages> validateRemove(Sms sms);
@Override public Optional<BusinessMessages> validateReenviar(Sms entidad, Sms entidadOriginal);

/**************************************************************************************/
/********************************   AllowProperties   *********************************/
/**************************************************************************************/
@Override public AllowProperties allowPropertiesInsert();
@Override public AllowProperties allowPropertiesReenviar();
@Override public AllowProperties allowPropertiesUpdate();
@Override public AllowProperties allowPropertiesRemove();

/*************************************************************************************/
/********************************    Action Rules    *********************************/
/*************************************************************************************/
private void fireActionRule_NormalizarTelefono(Sms sms);
private void fireActionRule_AsignarValoresIniciales(Sms sms);
private void fireActionRule_ProgramarEnvioAsincrono(Sms sms);
private void fireActionRule_RegistrarIntentoEnvio(Sms sms);
private void fireActionRule_MarcarEnvioCorrecto(Sms sms);
private void fireActionRule_MarcarEnvioFallido(Sms sms, RuntimeException excepcion);

/*************************************************************************************/
/********************************    Otras funciones    ******************************/
/*************************************************************************************/
private void enviarSms(Long smsId);
```

**Dependencias** (ninguna es `ModelService`, así que van con `@Inject`):
`jakarta.inject.Provider<SmsSender> smsSenderProvider` —un `Provider`, no un `SmsSender`: ver `decisiones.md` D4
y «Notas y supuestos» 6—, que es **el único** que se bindea en `SmsModule` (`.toProvider(SmsSenderProvider.class)`,
paso 7); y `com.educaflow.base.infrastructure.async.EjecutorAsincrono ejecutorAsincrono`, el único de la
aplicación, que aquí **solo se inyecta**: su único binding vive en `SecretariaVirtualModule` (paso 3) y
`SmsModule` **MUST NOT** bindearlo. El constructor lo invoca `ModelServiceFactory` por reflexión, así que esa
firma es obligatoria.

El nombre simple `Sms` es el de la **entidad** (`com.educaflow.subsystem.sms.db.Sms`). El record de transporte
`com.educaflow.base.infrastructure.sms.Sms` se referencia por su FQN —solo aparece en la línea del `send`, dentro
de `enviarSms`— y **MUST NOT** importarse, para que no choque con la entidad (ver «Notas y supuestos» 11).

##### Acciones

- **`insert(Sms)`** — secuencia: (1) `validateInsert(sms).ifPresent(BusinessMessages::throwIfInvalid)` →
  V-Sms-001…011; (2) `fireActionRule_NormalizarTelefono(sms)` (R-Sms-001, Antes); (3)
  `fireActionRule_AsignarValoresIniciales(sms)` (R-Sms-002, Antes); (4) `sms = repository.save(sms)` —**nunca**
  `super.insert`; (5) `fireActionRule_ProgramarEnvioAsincrono(sms)` (R-Sms-003, Después); (6) `return sms`.
  Es también la puerta del alta programática de trámites y otros subsistemas
  (`modelServiceFactory.resolve(Sms.class).insert(sms)`): no hay DTO propio porque los siete campos `cliente` de
  la entidad son exactamente los datos del alta.
- **`update(Sms nuevo, Sms original)`** — RES-Sms-001: el SMS es inmutable tras su creación, así que no hay flujo
  normal, solo el rechazo: `throw new UnsupportedOperationException(I18n.get("El SMS es inmutable tras su creación."))`
  (`k-secure-coding` §9.2: el par `validateUpdate` que siempre rechaza + esta excepción).
- **`remove(Sms)`** — RES-Sms-002, mismo patrón:
  `throw new UnsupportedOperationException(I18n.get("Los SMS no se pueden borrar."))`.
- **`reenviar(Sms entidad, Sms entidadOriginal)`** — secuencia: (1)
  `validateReenviar(entidad, entidadOriginal).ifPresent(BusinessMessages::throwIfInvalid)` → la guarda de
  `entidadOriginal == null`, V-Sms-014 y V-Sms-015; con `entidadOriginal` a `null` esa guarda siempre devuelve
  mensajes, así que el paso 2 nunca recibe `null`; (2) `fireActionRule_ProgramarEnvioAsincrono(entidadOriginal)`
  (R-Sms-003, el **mismo** método que `insert`); (3) `return entidadOriginal`. **MUST NOT** `repository.save`
  aquí: el reenvío no cambia ningún campo de forma síncrona; todo el cambio de estado ocurre dentro de
  `enviarSms`, en la transacción del hilo del pool (`rules/R-Sms-003.md`).

##### Métodos de validación (V-)

`validateInsert(Sms)` acumula en un `BusinessMessages` y devuelve `messages.isValid() ? empty : of(messages)`,
delegando en los cinco privados para que cada grupo de reglas se lea junto: `validarDestinatario` (V-Sms-001…004),
`validarTelefono` (V-Sms-005, V-Sms-006), `validarMensaje` (V-Sms-007, V-Sms-008), `validarCentro` (V-Sms-009,
V-Sms-010) y `validarHistorialEstado` (V-Sms-011).

**`validarDestinatario(Sms, BusinessMessages)`**

- **V-Sms-001** (Origen spec: VAL-Sms-001) `dniDestinatario` indicado: `null` o blanco → mensaje que transmita
  que el DNI del destinatario es obligatorio.
- **V-Sms-002** (Origen spec: VAL-Sms-002) `dniDestinatario` válido: **solo** si el anterior pasó (rama
  `else if`, porque sin DNI no se puede comprobar su letra); se comprueba con `DniUtil.isValid(...)`. Mensaje:
  que el DNI no es válido y que compruebe la letra. **MUST NOT** volcar el DNI completo en el mensaje ni en
  ningún log (`DniUtil.enmascarar` si hiciera falta).
- **V-Sms-003** (Origen spec: VAL-Sms-003) `nombre` indicado, y **V-Sms-004** (Origen spec: VAL-Sms-004)
  `apellidos` indicados. Independientes entre sí: si faltan los dos, el usuario ve los dos mensajes (lo exige
  ESC-005).

**`validarTelefono(Sms, BusinessMessages)`**

- **V-Sms-005** (Origen spec: VAL-Sms-005) `telefono` indicado: `null` o blanco → mensaje de obligatorio.
- **V-Sms-006** (Origen spec: VAL-Sms-006, RES-Sms-004) móvil de España válido: **solo** si el anterior pasó;
  construye `new NumeroTelefono(sms.getTelefono())` y exige `esMovilDeEspana()` — **una** sola pregunta, porque
  la clasificación de negocio es de `NumeroTelefono`, no del servicio (un número sin prefijo se entiende de
  España, que es el país por defecto de `NumeroTelefono`). Un fijo (`963000000`) o un número incompleto
  (`60011`) fallan por la misma regla y con el mismo mensaje, que debe transmitir qué se espera y un ejemplo
  (`600111222`).

**`validarMensaje(Sms, BusinessMessages)`**

- **V-Sms-007** (Origen spec: VAL-Sms-007) `mensaje` indicado: `null` o blanco
  (`TextUtil.isNullOrBlank(sms.getMensaje())`) → mensaje de obligatorio. **MUST** redactarse así —null **o
  blanco**, igual que V-Sms-001 y V-Sms-005— y no como «indicado»: es la **única** guarda de
  `MensajeSmsUtil.cabeEnUnSms(...)`, cuya precondición es `TextUtil.requireNonBlank` (lanza
  `IllegalArgumentException` ante `null`, `""` y `" "`). El `<check field="mensaje">` del cliente usa `isEmpty()`,
  que deja pasar `"   "`, así que un mensaje de solo espacios llegaría al servicio; si esta condición no
  cubriera el blanco, V-Sms-008 llamaría a `cabeEnUnSms` y convertiría el `BusinessMessage` en un 500
  (`k-validaciones` §2: los `validate*` **nunca** lanzan).
- **V-Sms-008** (Origen spec: VAL-Sms-008) cabe en un solo SMS: **solo** si el anterior pasó; delega
  **enteramente** en `MensajeSmsUtil.cabeEnUnSms(...)` — el servicio **MUST NOT** contar caracteres ni conocer
  las tablas GSM-7. Mensaje: que no cabe en un solo SMS, con los dos límites (160, o 70 si contiene acentos u
  otros caracteres especiales).

**`validarCentro(Sms, BusinessMessages)`**

- **V-Sms-009** (Origen spec: VAL-Sms-009) `centro` indicado: `sms.getCentro() == null` → mensaje de obligatorio.
- **V-Sms-010** (Origen spec: VAL-Sms-010) **solo** si el anterior pasó (va en el `else` de V-Sms-009, igual que
  las otras tres parejas de este `validateInsert` y que `CorreoServiceImpl.validarCentro`): si el usuario
  autenticado **no** es administrador (`SecurityUtil.isAdmin(SecurityUtil.getUser()) == false`), el centro
  indicado **MUST** ser uno de los suyos (`user.perteneceAlCentro(sms.getCentro())`). Rama explícita para el
  administrador, nunca quitar el filtro (`k-secure-coding` §4). El usuario se obtiene **siempre** con
  `SecurityUtil.getUser()`, nunca con `AuthUtils.getUser()` ni del JSON. Mensaje: que no puede crear SMS para un
  centro que no es suyo. Al ir en el `else`, un alta sin centro produce **un** solo mensaje (el de V-Sms-009) y
  no el falso «no es suyo» sobre un centro que no existe.

**`validarHistorialEstado(Sms, BusinessMessages)`**

- **V-Sms-011** (Origen spec: VAL-Sms-011) la regla es **solo** de los SMS que traen `historialEstado`, así que
  el método empieza excluyéndose de lo que no es suyo —`if (sms.getHistorialEstado() == null || sms.getCentro() == null) return;`—
  y después, con un **único** `if` null-safe, comprueba en una sola expresión (cortocircuito de `||`) las dos
  mitades que exige `k-secure-coding` §3.6 para una referencia al padre que dicta el cliente, sobre el
  `HistorialEstado` que la propia entidad ya trae (`HistorialEstado padre = sms.getHistorialEstado();`):
  `padre.getId() == null || padre.getExpediente() == null || !Objects.equals(padre.getExpediente().getCentro(), sms.getCentro())`.
  Mensaje: que el estado del expediente indicado no existe.

Las dos mitades del `if` y la guarda de entrada:

- **(a) Que la referencia sea un `HistorialEstado` real y no una cáscara**: `padre.getId() == null` cubre el alta
  con `{"historialEstado": {}}` por `/ws/rest` (una instancia transitoria sin `id`) y cualquier referencia que el
  llamador programático no haya resuelto. Un `id` **inexistente** no llega hasta aquí: `JPA.edit` resuelve la
  referencia contra la BD antes de que `Resource.save` invoque a `insert` y aborta con `OptimisticLockException`
  si no existe.
- **(b) Que el usuario esté autorizado sobre ese padre**, y aquí eso es que el padre pertenezca al **mismo
  centro** que el SMS. `HistorialEstado.expediente` es un many-to-one **sin** `required`, así que un padre
  huérfano de expediente cae en el mismo rechazo antes de comparar.
- El **primer** corte es el `return` de arriba, y lleva juntos los **dos** casos que no son de esta regla porque
  son la misma decisión —excluirse del caso ajeno en vez de apoyarse en que otra regla haya corrido—:
  `historialEstado == null` es un many-to-one **sin** `required` y **ningún** escenario del spec crea un SMS con
  él, así que es el caso **normal**; **MUST** cortarse antes de tocar `padre`, porque desreferenciarlo sin esta
  guarda convertiría cada alta corriente en un NPE dentro de `validateInsert` (500 en vez de
  `BusinessMessage`), y dejarlo caer en el `if` añadiría el mensaje **falso** «El estado del expediente indicado
  no existe» sobre un SMS que no indica ninguno. Y `centro == null` es el fallo de V-Sms-009, que ya ha añadido
  su propio mensaje en el mismo `validateInsert`: V-Sms-011 **MUST NOT** añadir además ese mismo mensaje falso
  (el `historialEstado` puede ser perfectamente correcto).

El método **MUST** ser total: para cualquier entrada que le llegue, o la acepta o añade el mensaje; **nunca**
lanza (`k-validaciones` §2: los `validate*` devuelven `Optional<BusinessMessages>` y nunca lanzan excepciones),
así que ni un `historialEstado` ausente, ni un centro ausente, ni una referencia sin `id`, ni un padre huérfano
de expediente pueden convertir la validación en un 500. Si falla cualquiera de las mitades, el mismo y **único**
`BusinessMessage` de V-Sms-011: no se distingue cuál falló, porque distinguirlo sería decirle al atacante qué
ids existen.

**MUST NOT** quedarse en (a): por `/ws/rest` se puede mandar cualquier id, y como `nombreExpediente` es un
`formula="true"` que navega `historialEstado → expediente.name` y es columna de los tres grids, colgar el SMS de
un `HistorialEstado` de otro centro filtraría el nombre de un expediente ajeno a los gestores del centro del SMS
y al destinatario en «Mis SMS». El centro del SMS ya está defendido por V-Sms-010, que corre en el mismo
`validateInsert`.

Y **MUST NOT** releer el padre con `JpaRepository.of(HistorialEstado.class).find(...)`: `HistorialEstado` es una
entidad de `subsystem/expedientes` y `k-code-quality/proyecto.md` §«Fronteras entre subsistemas» prohíbe usar el
repositorio de otra unidad (es además la regla **C27** de `agent_docs/architecture-rules.md`, cuyo test está
congelado). La lectura sería **redundante**: por la puerta del endpoint REST automático `JPA.edit` ya resolvió la
referencia con `em().find` antes de llamar al servicio, así que `sms.getHistorialEstado()` **es** esa misma
instancia del contexto de persistencia y volver a pedirla por su id devolvería el mismo objeto; y cuando no está
resuelta (sin `id`), el primer corte del `if` la rechaza. Esto deja la referencia que dicta el cliente tratada
igual que la otra del mismo `validateInsert`: `validarCentro` compara `sms.getCentro()` tal cual, sin releerlo.

**Resto de validadores**

- **`validateUpdate(Sms nuevo, Sms original)`** — **V-Sms-012** (Origen spec: RES-Sms-001): sin condición —el SMS
  es inmutable, siempre se rechaza con `BusinessMessages.single(I18n.get("El SMS es inmutable tras su creación."))`.
- **`validateRemove(Sms)`** — **V-Sms-013** (Origen spec: RES-Sms-002): siempre se rechaza con
  `BusinessMessages.single(I18n.get("Los SMS no se pueden borrar."))`.
- **`validateReenviar(Sms entidad, Sms entidadOriginal)`** — se comprueba **siempre** sobre `entidadOriginal` (el
  estado real en BD): `entidad` solo trae el id, porque `allowPropertiesReenviar()` es una whitelist vacía.
  - **`entidadOriginal` puede ser `null`**: sale de `ActionRequestHelper.getOriginalModel()`, que lee de BD por el
    id del contexto y devuelve `null` cuando el contexto no trae `id` (o el id no existe). Un POST a
    `subsysSms.Main@Sms-Remote-validateReenviar-action` con un contexto sin `id` la dejaría a `null`, y ese caso
    se resuelve **una** sola vez, en una rama visible al principio del método y **antes** de declarar las dos
    reglas, que así se escriben sin volver a preguntar por `null`: si `entidadOriginal == null`, se añade el
    mensaje de V-Sms-014 y se devuelve (sin SMS en BD no hay nada sobre lo que decidir). Es el mismo corte que
    abre `validarHistorialEstado`: cuando falta el sujeto se corta arriba, no dentro de cada regla. **MUST NOT**
    escribirse el `== null` como término de la condición de V-Sms-014 ni un `!= null &&` en la de V-Sms-015: una
    regla **MUST NOT** decidir por dentro si le toca y callarse porque otra ya habló
    (`k-code-quality/disenyo.md` §«Una regla que decide sola si aplica»), y con la guarda arriba ningún cambio de
    forma de V-Sms-014 puede dejar un contexto sin `id` sin mensaje y con `reenviar` siguiendo hasta
    `fireActionRule_ProgramarEnvioAsincrono(null)`. Un `validate*` **nunca** lanza (`k-validaciones` §2), así que
    aquí no puede salir un NPE/500 en vez de un `BusinessMessage`.
  - **V-Sms-014** (Origen spec: VAL-Sms-012) `estado == FALLIDO`: `entidadOriginal.getEstado() != EstadoSms.FALLIDO`
    → mensaje que transmita que solo se pueden reenviar SMS que han fallado. Es el **mismo** texto que usa la
    guarda del `null` de arriba: sin `entidadOriginal` no hay ningún SMS fallido que reenviar.
  - **V-Sms-015** (Origen spec: VAL-Sms-013) si el usuario **no** es administrador, **MUST gestionar** el centro
    del SMS, es decir ser SUPERVISOR o ADMINISTRATIVO en él: con `User user = SecurityUtil.getUser()` y
    `Centro centro = entidadOriginal.getCentro()`, la condición es
    `!SecurityUtil.isAdmin(user) && !(user.tieneTipoUsuario(centro, TipoUsuarioCodigo.SUPERVISOR) || user.tieneTipoUsuario(centro, TipoUsuarioCodigo.ADMINISTRATIVO))`.
    Mensaje: el de VAL-Sms-013 («No puede reenviar SMS de un centro que no es suyo»).
  - Las dos reglas son independientes entre sí y se acumulan en el mismo `BusinessMessages`.

V-Sms-015 se compone **en una sola expresión y en un solo sitio del lado Java**, con los
`tieneTipoUsuario(centro, …)` que **ya** existen en el `<extra-code-model>` de `subsystem/common/domains/User.xml`
(devuelven `false` si el usuario no pertenece a ese centro, así que la expresión es null-safe y VAL-Sms-013 queda
cubierta), igual que `CorreoServiceImpl.validateReenviar` compone su `isAdmin` + `perteneceAlCentro`. El usuario
se obtiene **siempre** con `SecurityUtil.getUser()`, nunca con `AuthUtils.getUser()` ni del JSON, y la rama del
administrador es explícita: nunca se quita el filtro (`k-secure-coding` §4).
**MUST NOT** quedarse en `perteneceAlCentro(...)`: eso es cierto para **cualquier** `CentroUsuario` del centro
(alumno, profesor, familiar, externo) y dejaría a un alumno del centro relanzar envíos de SMS a terceros con un
POST a la acción. Que el botón no se vea, que el `<domain>` de `Centro-Sms.xml` filtre y que el permiso sea de
solo lectura son UI y lectura, no autorización (`k-secure-coding` §9 y §4: una VAL con actor se comprueba también
en el `validate*`). Y esta condición **no** es la de V-Sms-010: el alta la puede hacer cualquier usuario del
centro (VAL-Sms-010, `perteneceAlCentro`) y el reenvío solo la gestión del centro (VAL-Sms-013 + «Actores» del
spec), así que son dos reglas distintas y **MUST NOT** unificarse en un predicado común.
Por qué no se envuelven las dos preguntas en un `esGestorDelCentro(Centro)` de `User`, y dónde más está escrita
la lista `{SUPERVISOR, ADMINISTRATIVO}` (con el aviso para quien la cambie): `decisiones.md` D5.

##### AllowProperties

- **`allowPropertiesInsert()`** — whitelist **explícita** (`createAllowProperties`) con los siete campos
  `cliente`: `dniDestinatario`, `nombre`, `apellidos`, `telefono`, `mensaje`, `centro`, `historialEstado`. Ver
  «Frontera de confianza».
- **`allowPropertiesReenviar()`** — whitelist **vacía** (`createAllowProperties` de un `Map` vacío). Ver
  «Frontera de confianza».
- **`allowPropertiesUpdate()`** y **`allowPropertiesRemove()`** — las dos, whitelist **vacía**
  (`createAllowProperties` de un `Map` vacío), una línea cada una. Ver «Frontera de confianza».

##### Action rules (R-)

- **`fireActionRule_NormalizarTelefono(Sms)`** — aplica **R-Sms-001** (Origen spec: RN-Sms-001, RES-Sms-004).
  Momento: **Antes** de `repository.save`. Asignación **incondicional**:
  `sms.setTelefono(new NumeroTelefono(sms.getTelefono()).enFormatoE164())`. **MUST NOT** añadir ninguna guarda
  del tipo `if (…startsWith("+34"))`: la canonicalización tiene que ocurrir en todas las entradas (formulario,
  alta programática y endpoint REST automático) para que RES-Sms-004 se cumpla siempre. Es seguro porque
  V-Sms-006 ya rechazó todo lo que no sea un móvil de España, de modo que `enFormatoE164()` nunca puede lanzar
  aquí. `telefono` sigue clasificado `cliente`: el servidor no aporta información nueva, solo reescribe en forma
  canónica el valor del usuario (ver `decisiones.md` D6 y «Notas y supuestos» 2).
- **`fireActionRule_AsignarValoresIniciales(Sms)`** — aplica **R-Sms-002** (Origen spec: CC-Sms-001…CC-Sms-007 y
  RES-Sms-003). Momento: **Antes** de `repository.save`. Asignación **incondicional** de los siete campos
  `servidor` (`k-secure-coding` §3.3): `estado = EstadoSms.PENDIENTE`,
  `fechaCreacion = LocalDateTime.now(Convert.defaultZoneId)`, `numeroReintentos = 0`, y
  `fechaPrimerIntentoEnvio`, `fechaUltimoIntentoEnvio`, `fechaEnvio` y `descripcionUltimoFallo` a `null`.
  **MUST NOT** envolver ninguna en `if (campo == null)`: con la guarda, un atacante que mandara el campo relleno
  por el endpoint REST genérico vería respetado su valor (`k-secure-coding` §3.3). Poner `fechaEnvio` a `null`
  también sostiene RES-Sms-003 (solo hay fecha de envío en ENVIADO).
- **`fireActionRule_ProgramarEnvioAsincrono(Sms)`** — aplica **R-Sms-003** (Origen spec: RN-Sms-002, RN-Sms-003).
  Momento: **Después** de `repository.save`. Diseño detallado en `design/rules/R-Sms-003.md`.
- **`fireActionRule_RegistrarIntentoEnvio(Sms)`** — aplica **R-Sms-004** (Origen spec: CC-Sms-003, CC-Sms-004,
  CC-Sms-005). Momento: **Antes** del `repository.save` de `enviarSms`. Asignación **incondicional**:
  `fechaUltimoIntentoEnvio = ahora` y `numeroReintentos = numeroReintentos + 1`; `fechaPrimerIntentoEnvio = ahora`
  **solo** si seguía a `null` (CC-Sms-003: «ya no cambia en los reintentos»). Ese `if` **no** es el antipatrón de
  mass-assignment: no depende de nada que mande el cliente (el único parámetro externo es el id), sino de si ya
  había un primer intento en BD.
- **`fireActionRule_MarcarEnvioCorrecto(Sms)`** — aplica **R-Sms-005** (Origen spec: CC-Sms-001, CC-Sms-006,
  RES-Sms-003). Asignación **incondicional**: `estado = ENVIADO`, `fechaEnvio = ahora`,
  `descripcionUltimoFallo = null`.
- **`fireActionRule_MarcarEnvioFallido(Sms, RuntimeException)`** — aplica **R-Sms-006** (Origen spec: CC-Sms-001,
  CC-Sms-007, RES-Sms-003). Asignación **incondicional**: `estado = FALLIDO`,
  `descripcionUltimoFallo = TextUtil.trazaCompleta(excepcion)`, `fechaEnvio = null` (RES-Sms-003: fuera de
  ENVIADO nunca hay fecha de envío). **MUST** usarse `TextUtil.trazaCompleta` (paso 2), **nunca** un helper
  privado: «qué texto se guarda en `descripcionUltimoFallo`» tiene un único dueño, compartido con
  `CorreoServiceImpl`.

##### Otras funciones

**`enviarSms(Long smsId)`** — cuerpo de la tarea asíncrona de R-Sms-003 (`design/rules/R-Sms-003.md`). Es
**privado** a propósito: no es una acción del contrato público del subsistema (quien quiere mandar un SMS hace
`insert`, y el envío se programa solo), así que no necesita ni validador ni whitelist —y `k-sistemas` prohíbe
declarar un `validateXxx` que sería un stub vacío—. Se ejecuta en un hilo del pool, con su propia transacción:
`JPA.runInTransaction(() -> { … })`. Secuencia:

- (a) `Sms sms = repository.find(smsId)`; si es `null`, `throw new IllegalStateException("No existe el SMS " + smsId)`.
  RES-Sms-002 garantiza que un SMS no se borra jamás, así que un `null` aquí solo puede ser un error de
  programación o de datos: **MUST** fallar, no callar (`k-code-quality/disenyo.md` §«El retorno defensivo que
  delega»). El envoltorio de `EjecutorAsincrono` lo registra con `log.error` y el hilo del pool sobrevive.
- (b) Si `sms.getEstado() == EstadoSms.ENVIADO`, `return` — idempotencia: ENVIADO es terminal (`entity-Sms.md`),
  así que un doble disparo no reenvía. Es una salida legítima, y por eso va en su propia rama: mezclarla con la
  anterior escondería el error real bajo esa palabra.
- (c) `fireActionRule_RegistrarIntentoEnvio` y, dentro de un `try`,
  `smsSenderProvider.get().send(new com.educaflow.base.infrastructure.sms.Sms(sms.getTelefono(), sms.getMensaje()))`
  y `fireActionRule_MarcarEnvioCorrecto`; en `catch (RuntimeException ex)`,
  `fireActionRule_MarcarEnvioFallido(sms, ex)`; y por último un único `repository.save(sms)`.

El record de transporte se construye **en esa línea**, sin método auxiliar: no hay ninguna traducción que hacer
(dos campos que se copian tal cual), así que un privado de un solo llamador sería indirección gratuita. Al estar
dentro del `try`, si el record rechazara sus argumentos el fallo se registra como cualquier otro fallo de envío.
El `Provider` se resuelve **dentro** del `try`: así una credencial ausente o mal puesta (`TwilioCredential`
rechaza los valores blancos) deja el SMS FALLIDO con su descripción, en vez de reventar al construir el servicio
(`decisiones.md` D4).

`SmsServiceImpl` **MUST NOT** declarar ningún `trazaCompleta` propio: el texto que se guarda en
`descripcionUltimoFallo` (CC-Sms-007, «el detalle completo del error») lo da
`TextUtil.trazaCompleta(Throwable)` (ver paso 2), que es el **único** dueño de esa decisión para todo
el proyecto. `fireActionRule_MarcarEnvioFallido` lo llama, igual que hace `CorreoServiceImpl` tras el
delta del paso 3. Sigue vigente la regla de que **MUST NOT** loguearse ni guardarse ningún dato
personal adicional (`k-secure-coding` §6).

`subsystem/common/domains/User.xml` **NO se toca**: V-Sms-015 se apoya en los `tieneTipoUsuario(Centro,
TipoUsuarioCodigo)` que su `<extra-code-model>` ya ofrece y compone los dos en su propio `if` (arriba),
sin añadir ningún método a una entidad compartida de otro subsistema. Reescribir
`MenuSecurityServiceImpl` queda igualmente **fuera del alcance**: `subsystem/security` no se toca
(`decisiones.md` D5).

**Verificar:** `./gradlew -q test` pasa; `grep -n "super.insert\|super.update\|super.remove" src/main/java/com/educaflow/subsystem/sms/service/impl/SmsServiceImpl.java` no devuelve nada; `grep -n "== null)" …/SmsServiceImpl.java` solo aparece en validaciones, en la guarda de `fechaPrimerIntentoEnvio` y en el `if (sms == null) throw` de `enviarSms`; `grep -n "JpaRepository" …/SmsServiceImpl.java` no devuelve nada (V-Sms-011 no lee por repositorio ninguna entidad de otro subsistema, así que la regla **C27** de `agent_docs/architecture-rules.md` no se roza); `grep -nE "V-Sms-|VAL-Sms-|RES-Sms-|CC-Sms-|RN-Sms-|ESC-|k-secure-coding|k-validaciones" …/SmsServiceImpl.java` no devuelve ninguna línea de mensaje ni de excepción (los identificadores de spec no se vuelcan al código ni a los mensajes).

### `SmsServiceImpl.insert` (endpoint REST automático `POST /ws/rest/com.educaflow.subsystem.sms.db.Sms`)

Entidad: `Sms`. **Forma elegida**: `createAllowProperties` (whitelist).
**Origen spec:** `Input AllowProperties` de la acción `Crear` de `entity-Sms.md`.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|-------|--------|--------------|--------------------------------------------|
| `dniDestinatario` | cliente | sí | Input directo del usuario (en `Input AllowProperties`). Validado por V-Sms-001/002. |
| `nombre` | cliente | sí | Input directo del usuario. V-Sms-003. |
| `apellidos` | cliente | sí | Input directo del usuario. V-Sms-004. |
| `telefono` | cliente | sí | Input directo del usuario. V-Sms-005/006 lo validan y R-Sms-001 lo **canonicaliza** a E.164 de forma incondicional (no aporta valor nuevo: reescribe el del cliente; ver `decisiones.md` D6). |
| `mensaje` | cliente | sí | Input directo del usuario. V-Sms-007/008. |
| `centro` | cliente | sí | Lo elige el usuario entre sus centros; la defensa es V-Sms-010 en el servicio (el Administrador, cualquiera), nunca la vista. |
| `historialEstado` | cliente | sí | Referencia que dicta el cliente (`k-secure-coding` §3.6): en whitelist de `insert` y validada por V-Sms-011 **cuando el cliente la manda** (el campo es opcional y la regla se excluye si no viene, que es el caso normal), comprobando **(a)** que la referencia llega resuelta (con `id`, no una cáscara) y **(b)** que su expediente es del **mismo centro** que el SMS (si no, el `formula` `nombreExpediente` filtraría a los grids el nombre de un expediente de otro centro). |
| `estado` | servidor | **NO** | Asignado en `insert` → `fireActionRule_AsignarValoresIniciales` (PENDIENTE) y recalculado en `enviarSms` → `fireActionRule_MarcarEnvioCorrecto`/`MarcarEnvioFallido`. |
| `fechaCreacion` | servidor | **NO** | Asignada en `insert` → `fireActionRule_AsignarValoresIniciales` (`LocalDateTime.now(Convert.defaultZoneId)`). |
| `fechaPrimerIntentoEnvio` | servidor | **NO** | `null` al alta; la fija `fireActionRule_RegistrarIntentoEnvio` en el primer intento. |
| `fechaUltimoIntentoEnvio` | servidor | **NO** | `null` al alta; la fija `fireActionRule_RegistrarIntentoEnvio` en cada intento. |
| `fechaEnvio` | servidor | **NO** | `null` al alta y en cada fallo; la fija `fireActionRule_MarcarEnvioCorrecto` (RES-Sms-003). |
| `numeroReintentos` | servidor | **NO** | `0` al alta; lo incrementa `fireActionRule_RegistrarIntentoEnvio`. |
| `descripcionUltimoFallo` | servidor | **NO** | `null` al alta y al enviar bien; la fija `fireActionRule_MarcarEnvioFallido`. |
| `nombreExpediente` | servidor | **NO** | Campo derivado `formula="true"` (CC-Sms-008, `momento: lectura`): no se persiste, no hay setter que el cliente pueda dictar. |

### `SmsServiceImpl.reenviar` (invocado desde `SmsController.reenviar`)

Entidad: `Sms`. **Forma elegida**: `createAllowProperties` con un mapa **vacío**.
**Origen spec:** acción `Reenviar` de `entity-Sms.md` (no declara ninguna `Input AllowProperties`).

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|-------|--------|--------------|--------------------------------------------|
| *(ninguno)* | — | **NO** | El reenvío no acepta ningún dato del cliente: solo el `id`, que `ActionRequestHelper` resuelve al margen de la whitelist. Todo lo que se valida (estado FALLIDO y centro del usuario) se lee de `entidadOriginal`, es decir de BD, no del JSON. |

### `SmsServiceImpl.update` y `SmsServiceImpl.remove`

Entidad: `Sms`. **Forma elegida**: `createAllowProperties` con un mapa **vacío**, en
`allowPropertiesUpdate()` y en `allowPropertiesRemove()`.
**Origen spec:** acciones `Modificar` y `Borrar` de `entity-Sms.md` (ninguna declara `Input AllowProperties`).

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|-------|--------|--------------|--------------------------------------------|
| *(ninguno)* | — | **NO** | RES-Sms-001 / RES-Sms-002: el SMS es inmutable y no se borra, así que ninguna de las dos acciones acepta ni asigna nada. |

Las dos van **además** del rechazo de negocio (V-Sms-012 / V-Sms-013 siempre devuelven mensaje y los
métodos lanzan `UnsupportedOperationException`), no en su lugar: son **dos puertas distintas**. Sin
sobrescribirlas quedaría en pie el `createAllowAllProperties()` que `DefaultModelService` da por
defecto, que es literalmente el detector de vulnerabilidad de `k-secure-coding` §3.4 y contradice la
tabla de §3.2 («whitelist obligatoria si hay algún campo `servidor` que la acción no asigna»:
`update`/`remove` no asignan ninguno de los siete). Son dos métodos de una línea y cierran el filtro
del JSON en la capa que `k-secure-coding` señala como la única que filtra el endpoint REST automático.

No hay alta programática vía DTO: los siete campos `cliente` de la entidad son exactamente los datos
del alta, así que otros subsistemas llaman a `insert(Sms)` y pasan por la misma whitelist y las mismas
V-.

### Trazabilidad — Validaciones (de `design/design.md`)

| V | Origen spec | Ubicación |
|---|-------------|-----------|
| V-Sms-001 | VAL-Sms-001 | `SmsServiceImpl.validateInsert` → `validarDestinatario` (+ capa cliente `subsysSms.Main@Sms-Local-validateSave-action`, `<check field="dniDestinatario">`) |
| V-Sms-002 | VAL-Sms-002 | `SmsServiceImpl.validateInsert` → `validarDestinatario` (solo servidor: `DniUtil.isValid`) |
| V-Sms-003 | VAL-Sms-003 | `SmsServiceImpl.validateInsert` → `validarDestinatario` (+ `<check field="nombre">`) |
| V-Sms-004 | VAL-Sms-004 | `SmsServiceImpl.validateInsert` → `validarDestinatario` (+ `<check field="apellidos">`) |
| V-Sms-005 | VAL-Sms-005 | `SmsServiceImpl.validateInsert` → `validarTelefono` (+ `<check field="telefono">`) |
| V-Sms-006 | VAL-Sms-006, RES-Sms-004 | `SmsServiceImpl.validateInsert` → `validarTelefono` (`NumeroTelefono.esMovilDeEspana()`) |
| V-Sms-007 | VAL-Sms-007 | `SmsServiceImpl.validateInsert` → `validarMensaje` (`TextUtil.isNullOrBlank`: null **o blanco**, porque es la única guarda de `MensajeSmsUtil.cabeEnUnSms`) (+ `<check field="mensaje">`) |
| V-Sms-008 | VAL-Sms-008 | `SmsServiceImpl.validateInsert` → `validarMensaje` (`MensajeSmsUtil.cabeEnUnSms`) |
| V-Sms-009 | VAL-Sms-009 | `SmsServiceImpl.validateInsert` → `validarCentro` (+ `<check field="centro">`) |
| V-Sms-010 | VAL-Sms-010 | `SmsServiceImpl.validateInsert` → `validarCentro`, en el `else` de V-Sms-009 (`SecurityUtil.isAdmin` / `perteneceAlCentro`) |
| V-Sms-011 | VAL-Sms-011 | `SmsServiceImpl.validateInsert` → `validarHistorialEstado` (solo si el SMS trae `historialEstado` **y** centro: la referencia llega resuelta **y** su expediente es del mismo centro que el SMS, sin releerla por repositorio) |
| V-Sms-012 | RES-Sms-001 | `SmsServiceImpl.validateUpdate` (siempre rechaza) + `SmsServiceImpl.update` (`UnsupportedOperationException`) |
| V-Sms-013 | RES-Sms-002 | `SmsServiceImpl.validateRemove` (siempre rechaza) + `SmsServiceImpl.remove` (`UnsupportedOperationException`) |
| V-Sms-014 | VAL-Sms-012 | `SmsServiceImpl.validateReenviar` (estado != FALLIDO; su mismo mensaje lo da también la guarda `entidadOriginal == null` con la que abre el método), invocada por `SmsController.validateReenviar` desde `subsysSms.Main@Sms-Remote-validateReenviar-action` |
| V-Sms-015 | VAL-Sms-013 | `SmsServiceImpl.validateReenviar` (`SecurityUtil.isAdmin` o `user.tieneTipoUsuario(centro, SUPERVISOR/ADMINISTRATIVO)`, compuestos en su propio `if`; corre tras la guarda del null, así que no vuelve a preguntar por él) |

### Trazabilidad — Reglas de negocio (de `design/design.md`)

| R | Origen spec | Ubicación |
|---|-------------|-----------|
| R-Sms-001 | RN-Sms-001, RES-Sms-004 | `SmsServiceImpl.fireActionRule_NormalizarTelefono` (Antes de `repository.save`, en `insert`) |
| R-Sms-002 | CC-Sms-001, CC-Sms-002, CC-Sms-003, CC-Sms-004, CC-Sms-005, CC-Sms-006, CC-Sms-007, RES-Sms-003 | `SmsServiceImpl.fireActionRule_AsignarValoresIniciales` (Antes de `repository.save`, en `insert`) |
| R-Sms-003 | RN-Sms-002, RN-Sms-003 | `SmsServiceImpl.fireActionRule_ProgramarEnvioAsincrono` (Después de `repository.save` en `insert`, y en `reenviar`). Detalle: `design/rules/R-Sms-003.md` |
| R-Sms-004 | CC-Sms-003, CC-Sms-004, CC-Sms-005 | `SmsServiceImpl.fireActionRule_RegistrarIntentoEnvio` (Antes del `repository.save` de `enviarSms`) |
| R-Sms-005 | CC-Sms-001, CC-Sms-006, RES-Sms-003 | `SmsServiceImpl.fireActionRule_MarcarEnvioCorrecto` (Antes del `repository.save` de `enviarSms`) |
| R-Sms-006 | CC-Sms-001, CC-Sms-007, RES-Sms-003 | `SmsServiceImpl.fireActionRule_MarcarEnvioFallido` (Antes del `repository.save` de `enviarSms`; el texto lo da `TextUtil.trazaCompleta`) |

### Trazabilidad — Seguridad (de `design/design.md`)

| Regla del spec (apartado «Seguridad») | Ubicación |
|---|---|
| Administrador: ve todos los centros, solo él da de alta, reenvía cualquiera; no modifica ni borra | Menú `sms-todos-menuitem` con `groups="admins"`; `Main-Sms.xml` sin `<domain>`; V-Sms-010/V-Sms-015 con rama explícita de administrador; V-Sms-012/V-Sms-013 |
| Supervisor y Administrativo: solo lectura de sus centros + reenvío | `Centro-Sms.xml` (`<domain>` por `CentroUsuario` con `SUPERVISOR`/`ADMINISTRATIVO`) + permiso `Sms.propio-centro-gestion` (solo `read`) + **V-Sms-015, que es la autorización real**: exige ser `SUPERVISOR` o `ADMINISTRATIVO` en el centro del SMS (o ser Administrador), no la mera pertenencia al centro. El `<domain>` y el permiso son filtro de lectura, no defensa de la acción |
| Cualquier usuario: solo lectura de los SMS ENVIADOS a su propio DNI, de cualquier centro | `Mis-Sms.xml` (`<domain>` por DNI y estado) + permiso `Sms.propio-destinatario` (solo `read`) |
| Nadie del grupo `users` puede crear, modificar ni borrar | Los dos permisos con `create/write/remove/export="false"`; `Main-Sms.xml` solo accesible desde un menú `groups="admins"` |

### Notas y supuestos 2, 5, 6, 10, 11 y 12 (de `design/design.md`)

2. **`telefono` es `cliente` aunque una R-Antes lo reescriba.** `design-contract.md` §3 pide que ningún campo `cliente` sea asignado por una R-Antes-de-Crear. Aquí la excepción es deliberada y está en el spec (RN-Sms-001, `fase: antes_de_commit`): R-Sms-001 no **dicta** el valor, lo **canonicaliza** a E.164 para sostener RES-Sms-004 en todas las puertas de entrada (incluido `/ws/rest/<FQN>`), y solo puede ejecutarse sobre un teléfono que V-Sms-006 ya aceptó como móvil de España. Ver `decisiones.md` D6.
5. **`enviarSms` es privado y no hay `listarSmsEnFail`.** El contrato público del subsistema es `insert` + `reenviar`: quien quiera mandar un SMS da de alta la entidad y el envío se programa solo. Se descartan a propósito dos piezas que sí tiene correos: un `enviarSms` público con un `validateEnviarSms` que sería un stub vacío (`k-sistemas` lo prohíbe expresamente) y un `listarSmsEnFail` con su `finder-method findByEstado`, porque «reenviar en bloque todos los SMS fallidos» está en el «Fuera de alcance» del spec.
6. **`SmsSender` se inyecta como `Provider<SmsSender>`**, no como `SmsSender` (correos hace lo segundo). `TwilioCredential` rechaza credenciales blancas y este diseño las deja vacías en `axelor-config.properties`, así que inyectar el emisor haría fallar la construcción del servicio y ni se podría dar de alta un SMS; con el `Provider` la falta de credenciales cae por el camino normal y el SMS queda FALLIDO con su descripción, que es lo que piden ESC-001 y ESC-020. Ver `decisiones.md` D4.
10. **V-Sms-010 también se aplica al alta programática.** Un trámite que cree un SMS para un centro que no sea uno de los del usuario autenticado verá la operación rechazada, porque VAL-Sms-010 está escrita así en el spec (con la única excepción del Administrador). Si en el futuro un trámite necesita crear SMS para el centro del expediente con independencia de los centros del usuario, habrá que ampliar esa regla en la spec antes que en el código.
11. **El nombre simple `Sms` está usado dos veces en el proyecto**: la entidad `com.educaflow.subsystem.sms.db.Sms` y el record de transporte `com.educaflow.base.infrastructure.sms.Sms` que ya existía. En `SmsServiceImpl` se importa la **entidad** y el record se referencia por su FQN en el único sitio donde aparece (la línea del `send` dentro de `enviarSms`). No se renombra ninguno de los dos: el del spec es `Sms` y el de infraestructura ya está en uso.
12. **Dos validaciones del servidor son más estrictas que la letra del spec, a propósito** (`k-secure-coding` §9, «defensa en profundidad»; los mensajes al usuario son los que fija el spec, sin cambios). El razonamiento de cada una está en el paso 5:
    - **V-Sms-015 (VAL-Sms-013)** exige **gestionar** el centro del SMS, no solo pertenecer a él.
    - **V-Sms-011 (VAL-Sms-011)** exige además que el expediente del `HistorialEstado` sea del **mismo centro** que el SMS.


### Regla compleja referenciada

R-Sms-003 (`fireActionRule_ProgramarEnvioAsincrono` + `enviarSms`): diseño detallado en `design/rules/R-Sms-003.md` (análisis, diagrama de secuencia, tabla de «Errores» y «Contenido del método `fireActionRule_*`»). **MUST** leerse entero antes de implementar esos dos métodos.

### Decisión del descomponedor (no es texto del diseño)

`SmsService` y `SmsServiceImpl` van en una sola tarea (interfaz + implementación, acoplamiento fuerte). Se añade `k-guice` porque la clase inyecta `Provider<SmsSender>` y `EjecutorAsincrono` con `@Inject` (el diseño lo lista entre los skills necesarios). El dominio `Sms.xml` ya está colocado y es contrato fijo.
