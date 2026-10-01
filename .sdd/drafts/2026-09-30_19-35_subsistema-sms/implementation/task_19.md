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

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/sms/service/SmsService.java` | Crear | k-sistemas (servicios.md) | Interfaz: `reenviar` + su validador + su `allowProperties` |
| `src/main/java/com/educaflow/subsystem/sms/service/impl/SmsServiceImpl.java` | Crear | k-sistemas (servicios.md), k-validaciones, k-secure-coding | V-Sms-001…015, R-Sms-001…006, whitelists |

### Paso 5 — Servicio `SmsService` / `SmsServiceImpl`

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
`jakarta.inject.Provider<SmsSender> smsSenderProvider` (ver `decisiones.md` D4) y
`com.educaflow.base.infrastructure.async.EjecutorAsincrono ejecutorAsincrono` (solo se inyecta; ver paso 3).
El constructor lo invoca `ModelServiceFactory` por reflexión, así que esa firma es obligatoria.

El nombre simple `Sms` es el de la **entidad** (`com.educaflow.subsystem.sms.db.Sms`). El record de transporte
`com.educaflow.base.infrastructure.sms.Sms` se referencia por su FQN —solo aparece en la línea del `send`, dentro
de `enviarSms`— y **MUST NOT** importarse, para que no choque con la entidad.

##### Acciones

- **`insert(Sms)`** — secuencia: (1) `validateInsert(sms).ifPresent(BusinessMessages::throwIfInvalid)` →
  V-Sms-001…011; (2) `fireActionRule_NormalizarTelefono(sms)` (R-Sms-001, Antes); (3)
  `fireActionRule_AsignarValoresIniciales(sms)` (R-Sms-002, Antes); (4) `sms = repository.save(sms)` —**nunca**
  `super.insert`; (5) `fireActionRule_ProgramarEnvioAsincrono(sms)` (R-Sms-003, Después); (6) `return sms`.
  Es también la puerta del alta programática de trámites y otros subsistemas
  (`modelServiceFactory.resolve(Sms.class).insert(sms)`): no hay DTO propio porque los siete campos `cliente` de
  la entidad son exactamente los datos del alta.
- **`update(Sms nuevo, Sms original)`** — RES-Sms-001: el SMS es inmutable tras su creación, así que no hay flujo
  normal, solo el rechazo: lanza `UnsupportedOperationException` con un mensaje traducible (`I18n.get`) que
  transmita que el SMS no se puede modificar una vez creado (el mismo que V-Sms-012)
  (`k-secure-coding` §9.2: el par `validateUpdate` que siempre rechaza + esta excepción).
- **`remove(Sms)`** — RES-Sms-002, mismo patrón: lanza `UnsupportedOperationException` con un mensaje
  traducible (`I18n.get`) que transmita que los SMS no se pueden borrar (el mismo que V-Sms-013).
- **`reenviar(Sms entidad, Sms entidadOriginal)`** — secuencia: (1)
  `validateReenviar(entidad, entidadOriginal).ifPresent(BusinessMessages::throwIfInvalid)` → la guarda de
  `entidadOriginal == null`, V-Sms-014 y V-Sms-015; (2) `fireActionRule_ProgramarEnvioAsincrono(entidadOriginal)`
  (R-Sms-003, el **mismo** método que `insert`); (3) `return entidadOriginal`. **MUST NOT** `repository.save`
  aquí: el reenvío no cambia ningún campo de forma síncrona; todo el cambio de estado ocurre dentro de
  `enviarSms`, en la transacción del hilo del pool (`rules/R-Sms-003.md`).

##### Métodos de validación (V-)

`validateInsert(Sms)` acumula en un `BusinessMessages` y devuelve `messages.isValid() ? empty : of(messages)`,
delegando en los cinco privados para que cada grupo de reglas se lea junto: `validarDestinatario` (V-Sms-001…004),
`validarTelefono` (V-Sms-005, V-Sms-006), `validarMensaje` (V-Sms-007, V-Sms-008), `validarCentro` (V-Sms-009,
V-Sms-010) y `validarHistorialEstado` (V-Sms-011). Los cuatro primeros se llaman sin condición; el de historial,
**solo** cuando el SMS trae centro **y** estado de expediente, condición que se ve en el propio
`validateInsert` (no dentro del privado): sin `historialEstado` la regla no aplica (es el caso normal: ningún escenario del spec crea un
SMS con él, y entrar convertiría cada alta en un NPE o en el mensaje falso «El estado del expediente indicado no
existe»), y sin centro ya ha hablado V-Sms-009. La condición de cuándo aplica está donde se declaran las reglas,
no dentro del privado (`k-code-quality/disenyo.md` §«Una regla que decide sola si aplica»).

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
  (`TextUtil.isNullOrBlank(sms.getMensaje())`) → obligatorio (null o blanco: es la precondición de `cabeEnUnSms`,
  ver `decisiones.md` D2).
- **V-Sms-008** (Origen spec: VAL-Sms-008) cabe en un solo SMS: **solo** si el anterior pasó; delega
  **enteramente** en `MensajeSmsUtil.cabeEnUnSms(...)` — el servicio **MUST NOT** contar caracteres ni conocer
  las tablas GSM-7. Mensaje: que no cabe en un solo SMS, con los dos límites (160, o 70 si contiene acentos u
  otros caracteres especiales).

**`validarCentro(Sms, BusinessMessages)`**

- **V-Sms-009** (Origen spec: VAL-Sms-009) `centro` indicado: `sms.getCentro() == null` → mensaje de obligatorio.
- **V-Sms-010** (Origen spec: VAL-Sms-010) **solo** si el anterior pasó (va en el `else` de V-Sms-009, igual que
  las otras tres parejas de este `validateInsert` y que `CorreoServiceImpl.validarCentro`): si el usuario
  autenticado (`SecurityUtil.getUser()`) **no** es administrador (`SecurityUtil.isAdmin`), el centro
  indicado **MUST** ser uno de los suyos (lo responde `User.perteneceAlCentro`). Rama explícita para el
  administrador, nunca quitar el filtro (`k-secure-coding` §4). El usuario se obtiene **siempre** con
  `SecurityUtil.getUser()`, nunca con `AuthUtils.getUser()` ni del JSON. Mensaje: que no puede crear SMS para un
  centro que no es suyo. Al ir en el `else`, un alta sin centro produce **un** solo mensaje (el de V-Sms-009) y
  no el falso «no es suyo» sobre un centro que no existe.

**`validarHistorialEstado(Sms, BusinessMessages)`**

- **V-Sms-011** (Origen spec: VAL-Sms-011) solo se evalúa si el SMS trae centro y estado de expediente (la
  condición está en `validateInsert`, arriba), así que el método **no** tiene guarda de entrada. Comprueba, en
  una única condición null-safe, las dos mitades que exige `k-secure-coding` §3.6 para una referencia al padre
  que dicta el cliente, sobre el `HistorialEstado` que la propia entidad ya trae: falla si la referencia no está
  resuelta (no tiene id o no tiene expediente) o si el centro de su expediente es distinto del centro del SMS.
  Mensaje: que el estado del expediente indicado no existe.

Las dos mitades —(a) referencia resuelta, (b) mismo centro—, por qué no basta con (a), por qué no se relee por
repositorio (C27) y por qué diverge de correos (V-Correo-014): ver `decisiones.md` D7.

**Resto de validadores**

- **`validateUpdate(Sms nuevo, Sms original)`** — **V-Sms-012** (Origen spec: RES-Sms-001): sin condición —el SMS
  es inmutable, siempre se rechaza con un único mensaje (`BusinessMessages.single`) que transmita que el SMS no
  se puede modificar una vez creado.
- **`validateRemove(Sms)`** — **V-Sms-013** (Origen spec: RES-Sms-002): siempre se rechaza con un único mensaje
  (`BusinessMessages.single`) que transmita que los SMS no se pueden borrar.
- **`validateReenviar(Sms entidad, Sms entidadOriginal)`** — se comprueba **siempre** sobre `entidadOriginal` (el
  estado real en BD): `entidad` solo trae el id, porque `allowPropertiesReenviar()` es una whitelist vacía.
  - Lo primero: si no hay `entidadOriginal` (el contexto puede no traer id), devuelve ya el mensaje de V-Sms-014
    sin evaluar nada más.
  - **V-Sms-014** (Origen spec: VAL-Sms-012) el estado del SMS original **MUST** ser `FALLIDO`; si es otro
    → mensaje que transmita que solo se pueden reenviar SMS que han fallado. Es el **mismo** texto que usa la
    guarda del `null` de arriba: sin `entidadOriginal` no hay ningún SMS fallido que reenviar.
  - **V-Sms-015** (Origen spec: VAL-Sms-013) si el usuario **no** es administrador, **MUST gestionar** el centro
    del SMS, es decir ser SUPERVISOR o ADMINISTRATIVO en él. El usuario sale de `SecurityUtil.getUser()` y el
    centro, del SMS original; falla cuando el usuario no es administrador (`SecurityUtil.isAdmin`) y
    `User.tieneTipoUsuario` no le reconoce en ese centro ni el tipo `SUPERVISOR` ni el `ADMINISTRATIVO`.
    Mensaje: que transmita que el usuario no puede reenviar SMS de un centro que no gestiona (el de VAL-Sms-013).
  - Las dos reglas son independientes entre sí y se acumulan en el mismo `BusinessMessages`.

Por qué V-Sms-015 no se queda en `perteneceAlCentro` (diverge de V-Correo-018), por qué no se unifica con V-Sms-010
y dónde más está la lista `{SUPERVISOR, ADMINISTRATIVO}`: ver `decisiones.md` D5.

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
  Momento: **Antes** de `repository.save`. Asignación **incondicional**: sustituye el `telefono` del SMS por su
  forma E.164, que calcula `NumeroTelefono.enFormatoE164()` a partir del teléfono recibido. **MUST NOT** añadir
  ninguna guarda que se salte la normalización cuando el teléfono «ya parece» estar en E.164: la canonicalización tiene que ocurrir en todas las entradas (formulario,
  alta programática y endpoint REST automático) para que RES-Sms-004 se cumpla siempre. Es seguro porque
  V-Sms-006 ya rechazó todo lo que no sea un móvil de España, de modo que `enFormatoE164()` nunca puede lanzar
  aquí (ver `decisiones.md` D6).
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
  ENVIADO nunca hay fecha de envío).

##### Otras funciones

```java
private void enviarSms(Long smsId);
```

Cuerpo de la tarea asíncrona de R-Sms-003, en un hilo del pool con su propia transacción:
`JPA.runInTransaction(() -> { … })`. Por qué es privado y lanza ante un SMS inexistente (diverge de
`CorreoService.enviarCorreo`): `decisiones.md` D8. Secuencia, errores y diagrama: `design/rules/R-Sms-003.md`.

**Verificar:** `./gradlew -q test` pasa; `grep -n "super.insert\|super.update\|super.remove" src/main/java/com/educaflow/subsystem/sms/service/impl/SmsServiceImpl.java` no devuelve nada; `grep -n "== null)" …/SmsServiceImpl.java` solo aparece en validaciones, en la guarda de `fechaPrimerIntentoEnvio` y en el `if (sms == null) throw` de `enviarSms`; `grep -n "JpaRepository" …/SmsServiceImpl.java` no devuelve nada (V-Sms-011 no lee por repositorio ninguna entidad de otro subsistema, así que la regla **C27** de `agent_docs/architecture-rules.md` no se roza); `grep -nE "V-Sms-|VAL-Sms-|RES-Sms-|CC-Sms-|RN-Sms-|ESC-|k-secure-coding|k-validaciones" …/SmsServiceImpl.java` no devuelve ninguna línea de mensaje ni de excepción (los identificadores de spec no se vuelcan al código ni a los mensajes).



## Frontera de confianza — AllowProperties por acción

### `SmsServiceImpl.insert` (endpoint REST automático `POST /ws/rest/com.educaflow.subsystem.sms.db.Sms`)

Entidad: `Sms`. **Forma elegida**: `createAllowProperties` (whitelist).
**Origen spec:** `Input AllowProperties` de la acción `Crear` de `entity-Sms.md`.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|-------|--------|--------------|--------------------------------------------|
| `dniDestinatario` | cliente | sí | Input directo del usuario (en `Input AllowProperties`). Validado por V-Sms-001/002. |
| `nombre` | cliente | sí | Input directo del usuario. V-Sms-003. |
| `apellidos` | cliente | sí | Input directo del usuario. V-Sms-004. |
| `telefono` | cliente | sí | Input directo del usuario. V-Sms-005/006 lo validan y R-Sms-001 lo **canonicaliza** a E.164 de forma incondicional (ver `decisiones.md` D6). |
| `mensaje` | cliente | sí | Input directo del usuario. V-Sms-007/008. |
| `centro` | cliente | sí | Lo elige el usuario entre sus centros; la defensa es V-Sms-010 en el servicio (el Administrador, cualquiera), nunca la vista. |
| `historialEstado` | cliente | sí | Referencia que dicta el cliente (`k-secure-coding` §3.6): en whitelist de `insert` y validada por V-Sms-011 **cuando el cliente la manda** (el campo es opcional y la regla se excluye si no viene, que es el caso normal), comprobando que la referencia llega resuelta y que su expediente es del **mismo centro** que el SMS: ver `decisiones.md` D7. |
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
**Origen spec:** acción `Modificar` de `entity-Sms.md` (Input AllowProperties: ninguna) y RES-Sms-002 para remove (el spec no declara ninguna acción Borrar).

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



## Trazabilidad Origen spec → V/R/U → ubicación

### Validaciones (`V-Sms-NNN`)

| V | Origen spec | Ubicación |
|---|-------------|-----------|
| V-Sms-001 | VAL-Sms-001 | `SmsServiceImpl.validateInsert` → `validarDestinatario` (+ capa cliente `subsysSms.Main@Sms-Local-validateSave-action`, `<check field="dniDestinatario">`) |
| V-Sms-002 | VAL-Sms-002 | `SmsServiceImpl.validateInsert` → `validarDestinatario` (solo servidor: `DniUtil.isValid`) |
| V-Sms-003 | VAL-Sms-003 | `SmsServiceImpl.validateInsert` → `validarDestinatario` (+ `<check field="nombre">`) |
| V-Sms-004 | VAL-Sms-004 | `SmsServiceImpl.validateInsert` → `validarDestinatario` (+ `<check field="apellidos">`) |
| V-Sms-005 | VAL-Sms-005 | `SmsServiceImpl.validateInsert` → `validarTelefono` (+ `<check field="telefono">`) |
| V-Sms-006 | VAL-Sms-006, RES-Sms-004 | `SmsServiceImpl.validateInsert` → `validarTelefono` (`NumeroTelefono.esMovilDeEspana()`) |
| V-Sms-007 | VAL-Sms-007 | `SmsServiceImpl.validateInsert` → `validarMensaje` (`TextUtil.isNullOrBlank`) (+ `<check field="mensaje">`) |
| V-Sms-008 | VAL-Sms-008 | `SmsServiceImpl.validateInsert` → `validarMensaje` (`MensajeSmsUtil.cabeEnUnSms`) |
| V-Sms-009 | VAL-Sms-009 | `SmsServiceImpl.validateInsert` → `validarCentro` (+ `<check field="centro">`) |
| V-Sms-010 | VAL-Sms-010 | `SmsServiceImpl.validateInsert` → `validarCentro`, en el `else` de V-Sms-009 (`SecurityUtil.isAdmin` / `perteneceAlCentro`) |
| V-Sms-011 | VAL-Sms-011 | `SmsServiceImpl.validateInsert`, solo con centro y estado de expediente presentes → `validarHistorialEstado` (la referencia llega resuelta **y** su expediente es del mismo centro que el SMS, sin releerla por repositorio; ver `decisiones.md` D7) |
| V-Sms-012 | RES-Sms-001 | `SmsServiceImpl.validateUpdate` (siempre rechaza) + `SmsServiceImpl.update` (`UnsupportedOperationException`) |
| V-Sms-013 | RES-Sms-002 | `SmsServiceImpl.validateRemove` (siempre rechaza) + `SmsServiceImpl.remove` (`UnsupportedOperationException`) |
| V-Sms-014 | VAL-Sms-012 | `SmsServiceImpl.validateReenviar` (estado != FALLIDO), invocada por `SmsController.validateReenviar` desde `subsysSms.Main@Sms-Remote-validateReenviar-action` |
| V-Sms-015 | VAL-Sms-013 | `SmsServiceImpl.validateReenviar` (administrador, o SUPERVISOR/ADMINISTRATIVO del centro del SMS según `User.tieneTipoUsuario`) |

### Reglas de negocio (`R-Sms-NNN`)

| R | Origen spec | Ubicación |
|---|-------------|-----------|
| R-Sms-001 | RN-Sms-001, RES-Sms-004 | `SmsServiceImpl.fireActionRule_NormalizarTelefono` (Antes de `repository.save`, en `insert`) |
| R-Sms-002 | CC-Sms-001, CC-Sms-002, CC-Sms-003, CC-Sms-004, CC-Sms-005, CC-Sms-006, CC-Sms-007, RES-Sms-003 | `SmsServiceImpl.fireActionRule_AsignarValoresIniciales` (Antes de `repository.save`, en `insert`) |
| R-Sms-003 | RN-Sms-002, RN-Sms-003 | `SmsServiceImpl.fireActionRule_ProgramarEnvioAsincrono` (Después de `repository.save` en `insert`, y en `reenviar`). Detalle: `design/rules/R-Sms-003.md` |
| R-Sms-004 | CC-Sms-003, CC-Sms-004, CC-Sms-005 | `SmsServiceImpl.fireActionRule_RegistrarIntentoEnvio` (Antes del `repository.save` de `enviarSms`) |
| R-Sms-005 | CC-Sms-001, CC-Sms-006, RES-Sms-003 | `SmsServiceImpl.fireActionRule_MarcarEnvioCorrecto` (Antes del `repository.save` de `enviarSms`) |
| R-Sms-006 | CC-Sms-001, CC-Sms-007, RES-Sms-003 | `SmsServiceImpl.fireActionRule_MarcarEnvioFallido` (Antes del `repository.save` de `enviarSms`; el texto lo da `TextUtil.trazaCompleta`) |

Regla compleja citada para `fireActionRule_ProgramarEnvioAsincrono` y `enviarSms`: `design/rules/R-Sms-003.md` (análisis, diagrama de secuencia, errores y contenido del método). Decisiones citadas: `design/decisiones.md` D4, D5, D6, D7 y D8.

## Notas y supuestos



2. **`telefono` es `cliente` aunque R-Sms-001 lo reescriba a E.164**: ver `decisiones.md` D6.

5. **`SmsSender` se inyecta como `Provider<SmsSender>`**, no como `SmsSender` (correos hace lo segundo). Ver `decisiones.md` D4.

8. **V-Sms-010 también se aplica al alta programática.** Un trámite que cree un SMS para un centro que no sea uno de los del usuario autenticado verá la operación rechazada, porque VAL-Sms-010 está escrita así en el spec (con la única excepción del Administrador). Si en el futuro un trámite necesita crear SMS para el centro del expediente con independencia de los centros del usuario, habrá que ampliar esa regla en la spec antes que en el código.
9. **La entidad y el record de transporte se llaman los dos `Sms`**: ver paso 5.
10. **V-Sms-011 (VAL-Sms-011) es más estricta que la letra del spec, a propósito** (`k-secure-coding` §9, «defensa en profundidad»; el mensaje es el que fija el spec): exige además el **mismo centro**. Ver `decisiones.md` D7.
