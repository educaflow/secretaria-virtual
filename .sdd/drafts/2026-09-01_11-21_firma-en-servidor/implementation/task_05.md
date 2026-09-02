---
type: implementation-task
---

# Tarea 05 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-validaciones
- k-code-quality

## Filas de la tabla «Ficheros a crear o modificar» del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/firmas/service/TareaFirmaService.java` | Modificar | k-sistemas (servicios.md) | Añade `firmarEnServidor`, `validateFirmarEnServidor` y `allowPropertiesFirmarEnServidor` |
| `src/main/java/com/educaflow/subsystem/firmas/service/impl/TareaFirmaServiceImpl.java` | Modificar | k-sistemas (servicios.md), k-secure-coding, k-validaciones | Implementa la acción de firma en servidor, sus validaciones, sus reglas y cierra la frontera de confianza de `insert`/`update` |

Las dos son `Acción: Modificar`: las clases **ya existen**. Se edita la clase existente añadiendo/cambiando
**solo** el delta que el diseño declara y **conservando** todo lo demás (métodos, campos e imports
preexistentes).

## Texto del diseño (verbatim)

### Paso 5 — Servicio de firmas: la acción `firmarEnServidor`

**Ficheros:**
`src/main/java/com/educaflow/subsystem/firmas/service/TareaFirmaService.java` (Modificar)
`src/main/java/com/educaflow/subsystem/firmas/service/impl/TareaFirmaServiceImpl.java` (Modificar)

#### 5.1 Delta del interface `TareaFirmaService`

El resto de la interfaz se conserva tal cual (`insert(DTO)`, `marcarComoFirmada`, `marcarComoRechazada`,
`validarDocumentosFirmados` y sus validadores y `allowProperties*`).

```java
// Clase: com.educaflow.subsystem.firmas.service.TareaFirmaService
TareaFirma firmarEnServidor(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal);
//   Firma en el servidor TODOS los documentos de la tarea con el certificado digital del firmante y deja la
//   tarea resuelta como FIRMADO.

Optional<BusinessMessages> validateFirmarEnServidor(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal);
//   Validador de la acción (V-TareaFirma-001 .. V-TareaFirma-007).

AllowProperties allowPropertiesFirmarEnServidor();
//   Whitelist del bind cliente→entidad de la acción. Se declara porque la acción recibe la entidad construida
//   desde el request (`ActionRequestHelper.getModel(...)`).
```

#### 5.2 Delta de `TareaFirmaServiceImpl`

Se conserva todo lo que no se menciona aquí: el constructor, `insert(TareaFirmaInsertDTO)`,
`marcarComoRechazada`, `validarDocumentosFirmados`, los cuatro `validate*` preexistentes, los tres
`allowProperties*` preexistentes y `fireActionRule_NotificarFirmaResuelta`.

**Campo nuevo de la clase:**

```java
// Dependencia nueva del *ServiceImpl:
@Inject
private ModelServiceFactory modelServiceFactory;
//   Es la forma canónica de obtener otro ModelService desde un ModelService
//   (k-sistemas/servicios.md §"Obtener otro servicio desde un servicio"): el CertificadoDigitalService se
//   resuelve dentro del método que lo necesita, NUNCA se inyecta con @Inject.
```

**Bloque (1) Acciones:**

```java
@Override
public TareaFirma firmarEnServidor(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal);
//   Cuerpo (patrón validate + throw, sin cuerpos implementados aquí):
//     try {
//       1. validateFirmarEnServidor(tareaFirma, tareaFirmaOriginal).ifPresent(BusinessMessages::throwIfInvalid);
//          — primera línea ejecutable del método.
//       2. fireActionRule_FirmarDocumentosEnServidor(tareaFirma);   // R-TareaFirma-001, Antes
//       3. fireActionRule_ResolverComoFirmada(tareaFirma);          // R-TareaFirma-002, Antes
//       4. tareaFirma = repository.save(tareaFirma);                // NUNCA super.update
//     } finally {
//       5. fireActionRule_DescartarClaveFirma(tareaFirma);          // R-TareaFirma-003, siempre
//     }
//       6. fireActionRule_NotificarFirmaResuelta(tareaFirma);       // R-TareaFirma-004, Después
//       7. return tareaFirma;
//   El try/finally envuelve también la validación a propósito: RN-TareaFirma-008 exige descartar la clave
//   también cuando la acción termina sin firmar (validación rechazada o firma fallida).

@Override
public TareaFirma marcarComoFirmada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal);
//   CAMBIA (delta mínimo): las dos asignaciones inline de estado y fecha de resolución se sustituyen por una
//   llamada a fireActionRule_ResolverComoFirmada(tareaFirma), la misma regla que usa firmarEnServidor.
//   El resto del método (validate + throw, repository.save, fireActionRule_NotificarFirmaResuelta) no cambia
//   y su comportamiento observable es idéntico. Motivo: RN-TareaFirma-003 queda ubicada en un único sitio.
```

**Bloque (2) Métodos de Validación:**

```java
@Override
public Optional<BusinessMessages> validateFirmarEnServidor(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal);
//   Acumula en un BusinessMessages y devuelve `messages.isValid() ? Optional.empty() : Optional.of(messages)`.
//   CRITICAL — todas las comprobaciones se hacen sobre el estado REAL del servidor, nunca sobre lo que la
//   pantalla tuviera pintado: el estado y el firmante se leen de la entidad cargada de BD y la situación de
//   firma se obtiene de tareaFirma.getSituacionFirma(), un getter que RECALCULA en cada llamada
//   (campo derivado del Paso 2). Es lo que exige entity-TareaFirma.md §"Acción: Firmar en el servidor".
//   Aplica:
//     - V-TareaFirma-001 (Origen spec: VAL-TareaFirma-001) estado de la tarea: comprueba que
//       estadoTareaFirma es PENDIENTE. Mensaje: el literal de la spec «Solo se pueden firmar las tareas
//       pendientes de firmar».
//     - V-TareaFirma-002 (Origen spec: VAL-TareaFirma-002) titularidad: comprueba que el firmante de la tarea
//       es el usuario autenticado, obtenido del servidor con AuthUtils.getUser(). Mensaje: el literal de la
//       spec «Solo puede firmar los documentos la persona a la que se le han encargado».
//       Es la defensa real de HU-004: el <domain> del action-view es solo UX (k-secure-coding §1).
//     - V-TareaFirma-003 (Origen spec: VAL-TareaFirma-003) DNI del firmante: comprueba que la situación de
//       firma NO es SIN_DNI. Mensaje: el literal de la spec «No es posible firmar los documentos porque su
//       usuario no tiene un DNI. Póngase en contacto con el administrador.».
//     - V-TareaFirma-004 (Origen spec: VAL-TareaFirma-004) certificado dado de alta: comprueba que la
//       situación de firma NO es SIN_CERTIFICADO. Mensaje: el literal de la spec «No es posible firmar en el
//       servidor porque no tiene un certificado digital dado de alta».
//     - V-TareaFirma-005 (Origen spec: VAL-TareaFirma-005) PIN obligatorio: si la situación es
//       DISPOSITIVO_SIN_PIN, comprueba que claveFirma no está vacía. Mensaje: el literal de la spec
//       «El PIN es obligatorio».
//     - V-TareaFirma-006 (Origen spec: VAL-TareaFirma-006) contraseña obligatoria: si la situación es
//       FICHERO_SIN_CLAVE, comprueba que claveFirma no está vacía. Mensaje: el literal de la spec
//       «La contraseña es obligatoria».
//     - V-TareaFirma-007 (Origen spec: VAL-TareaFirma-007) documentos: comprueba que la tarea tiene al menos
//       un DocumentoFirma. Mensaje: el literal de la spec «La tarea de firma no tiene ningún documento que
//       firmar».
//   MUST NOT incluir el valor de claveFirma en ningún mensaje (k-secure-coding §6).
//   MUST NOT usar el parámetro tareaFirmaOriginal para nada más que la simetría de firma con el resto de
//   acciones del servicio: la acción no compara con el original.
```

**Bloque (3) AllowProperties:**

```java
@Override
public AllowProperties allowPropertiesFirmarEnServidor();
//   return AllowProperties.createAllowProperties(Map.of("claveFirma", Map.of()));
//   Whitelist con UN solo campo: es el único `cliente` de la acción (entity-TareaFirma.md §"Acción: Firmar en
//   el servidor" → Input AllowProperties: clave de firma). Todo lo demás (estado, fechas, firmante, recuadro,
//   documentos, situación de firma) lo dicta el servidor y queda FUERA de la whitelist.

@Override
public AllowProperties allowPropertiesInsert();
//   return AllowProperties.createDenyAllProperties();
//   entity-TareaFirma.md §"Acción: Crear" declara «Input AllowProperties: (ninguna)»: las tareas de firma no
//   se dan de alta desde la interfaz, solo con el DTO programático insert(TareaFirmaInsertDTO). Cerrar la
//   whitelist impide que el endpoint REST automático /ws/rest/<FQN> cuele campos (k-secure-coding §3.2).

@Override
public AllowProperties allowPropertiesUpdate();
//   return AllowProperties.createDenyAllProperties();
//   entity-TareaFirma.md §"Acción: Modificar" declara «Input AllowProperties: (ninguna)»: la tarea solo cambia
//   mediante sus acciones propias (marcarComoFirmada, marcarComoRechazada, firmarEnServidor), nunca guardando
//   el formulario. Ninguna vista de TareaFirma usa `save`, así que no hay flujo legítimo que esto rompa.
```

**Bloque (4) Action Rules:**

```java
private void fireActionRule_FirmarDocumentosEnServidor(TareaFirma tareaFirma);
//   Implementa R-TareaFirma-001 (Origen spec: RN-TareaFirma-001, RN-TareaFirma-002, RN-TareaFirma-006,
//   RN-TareaFirma-007). Momento: Antes de repository.save.
//   Diseño detallado en design/rules/R-TareaFirma-001.md (incluye la regla CRITICAL de pedir un AlmacenClave
//   nuevo por cada documento y el esquema en dos fases que garantiza el «todo o nada»).
//   Resumen de la secuencia: resolver CertificadoDigitalService con modelServiceFactory; construir el
//   CampoFirma con el recuadro y la página de la propia tarea (MUST convertir x/y/width/height con
//   .floatValue(): son BigDecimal y Rectangulo es un record de cuatro `float`, igual que hace
//   TareaFirmaController.firmarDocumentosConAutoFirma); firmar TODOS los documentos en memoria; y solo
//   si todos han salido bien, crear sus MetaFile y asignarlos a cada DocumentoFirma. Cualquier fallo se
//   convierte en un error de negocio cuyo mensaje empieza por «No se han podido firmar los documentos: ».

private void fireActionRule_ResolverComoFirmada(TareaFirma tareaFirma);
//   Implementa R-TareaFirma-002 (Origen spec: RN-TareaFirma-003; campos `estadoTareaFirma` y `fechaResolucion`
//   clasificados `servidor`). Momento: Antes de repository.save.
//   Asignación INCONDICIONAL: tareaFirma.setEstadoTareaFirma(EstadoTareaFirma.FIRMADO) y
//   tareaFirma.setFechaResolucion(LocalDateTime.now()).
//   MUST NOT añadir guardas `if (tareaFirma.getEstadoTareaFirma() == null)` ni
//   `if (tareaFirma.getFechaResolucion() == null)`: permitirían al cliente dictar el estado o falsificar la
//   fecha de resolución por el endpoint REST genérico (ver k-secure-coding §3.3).
//   Lo llaman las dos acciones que resuelven una tarea como firmada: marcarComoFirmada y firmarEnServidor.

private void fireActionRule_DescartarClaveFirma(TareaFirma tareaFirma);
//   Implementa R-TareaFirma-003 (Origen spec: RN-TareaFirma-004, RN-TareaFirma-008). Momento: siempre, en el
//   `finally` de firmarEnServidor — tanto si la firma se completó como si la abortó una validación o un fallo.
//   Asignación INCONDICIONAL: tareaFirma.setClaveFirma(null).
//   El campo es `transient`, así que nunca llega a BD; esta regla se ocupa del resto del rastro: la referencia
//   en memoria se suelta en cuanto termina la acción.
//   MUST NOT loguear la clave, ni entera ni truncada, ni devolverla en el ActionResponse (k-secure-coding §6, §8).
//   El campo lleva `password="true"` en el dominio, así que Resource.toMap tampoco la serializa de vuelta.

private void fireActionRule_NotificarFirmaResuelta(TareaFirma tareaFirma);
//   PREEXISTENTE, no cambia. Implementa R-TareaFirma-004 (Origen spec: RN-TareaFirma-005). Momento: Después de
//   repository.save. firmarEnServidor lo invoca igual que marcarComoFirmada y marcarComoRechazada, de modo que
//   el proceso que encargó la firma se entera exactamente igual que con AutoFirma.
```

**Verificación:** `./gradlew clean build`; `grep -n "if *(.*== *null).*set" src/main/java/com/educaflow/subsystem/firmas/service/impl/TareaFirmaServiceImpl.java`
no debe devolver nada.

### Documentación referenciada

El diseño detallado de `fireActionRule_FirmarDocumentosEnServidor` (R-TareaFirma-001) está en
`design/rules/R-TareaFirma-001.md`: incluye la regla CRITICAL de pedir un `AlmacenClave` **nuevo por cada
documento**, el esquema en dos fases que garantiza el «todo o nada» y la sección «Cuál clave se usa».
**MUST** leerse entero antes de implementar la regla.

### Seguridad (Paso 9 del diseño — sin cambios de fichero, es el respaldo de V-TareaFirma-002)

### Paso 9 — Seguridad

**Fichero:** `src/main/java/com/educaflow/subsystem/firmas/data-init/input/auth-firmas.xml` (**sin cambios**)

Los permisos actuales ya materializan exactamente lo que pide el spec §Seguridad y **no hace falta tocarlos**:

- `TareaFirma.firmante` — `condition="self.firmante = ?"` con `conditionParams="__user__"`, y
  `read`/`write` a `true`, `create`/`remove`/`export` a `false`. Cada usuario ve y resuelve **solo** sus propias
  tareas, sin alcance por centro (la tarea no pertenece a un centro), y nadie —tampoco el Administrador— puede
  tocar la de otro.
- `DocumentoFirma.propio` — condición equivalente a través de la tarea.

No hay ningún permiso nuevo, ningún rol nuevo y ningún grupo nuevo. En lenguaje natural: **cualquier usuario
autenticado puede leer y resolver las tareas de firma en las que él es el firmante, y ninguna otra**. La firma
en el servidor no añade ninguna restricción por tipo de usuario ni por cargo: el control real está en a quién
le da de alta un certificado el Administrador (spec §Seguridad).

**Defensa en el servidor.** El permiso de Axelor y el `<domain>` del `action-view` filtran, pero la garantía la
da **V-TareaFirma-002**, que compara el firmante de la tarea con `AuthUtils.getUser()` dentro de
`validateFirmarEnServidor`: es la única capa por la que pasan todas las vías de entrada (k-secure-coding §1).

**Verificación:** `git diff --stat src/main/java/com/educaflow/subsystem/firmas/data-init/` vacío.

### Frontera de confianza — AllowProperties por acción

## Frontera de confianza — AllowProperties por acción

### `TareaFirmaServiceImpl.firmarEnServidor` (invocado desde `TareaFirmaController.firmarEnServidor` y `TareaFirmaController.validateFirmarEnServidor`)

Entidad: `TareaFirma`. **Forma elegida**: `createAllowProperties`.
**Origen spec:** `Input AllowProperties` de la acción `Firmar en el servidor` de `entity-TareaFirma.md`.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `claveFirma` | cliente | sí | Único input del usuario en esta acción (`Input AllowProperties: clave de firma`). Es transitorio: no se persiste nunca y `fireActionRule_DescartarClaveFirma` lo anula al terminar. |
| `situacionFirma` | servidor | **NO** | Campo derivado (`CC-TareaFirma-001`, momento lectura): lo recalcula el getter en cada lectura desde el DNI del firmante y el subsistema de criptografía. Aunque el cliente lo mandara, el getter lo sobrescribe. |
| `estadoTareaFirma` | servidor | **NO** | Asignado en `firmarEnServidor` → `fireActionRule_ResolverComoFirmada` (incondicional). |
| `fechaResolucion` | servidor | **NO** | Asignado en `firmarEnServidor` → `fireActionRule_ResolverComoFirmada` (incondicional). |
| `documentosFirma` | servidor | **NO** | Las versiones firmadas las produce el servidor en `fireActionRule_FirmarDocumentosEnServidor`. Dejarlo fuera es lo que impide que el cliente cuele un PDF «firmado» propio. |
| `firmante` | servidor | **NO** | Lo fijó quien creó la tarea; la acción no lo toca. Que esté fuera es lo que evita que alguien se autoasigne la tarea de otro. |
| `fechaSolicitud`, `motivoFirma`, `motivoRechazo`, `firmaRapida` | servidor | **NO** | Fijados al crear la tarea; esta acción no los toca. |
| `fqcnFirmaNotifier`, `fqcnCallBackData`, `callBackData` | servidor | **NO** | Los fija quien encarga la firma. Son especialmente sensibles: `fqcnFirmaNotifier` acaba en un `Class.forName` + `Beans.get`, así que dejarlos fuera de la whitelist es obligatorio. |
| `x`, `y`, `width`, `height`, `page` | servidor | **NO** | El recuadro y la página de la firma los fija quien encarga la firma; la acción los lee, no los escribe. |

### `TareaFirmaServiceImpl.marcarComoFirmada` (invocado desde `TareaFirmaController.marcarComoFirmada`) — PREEXISTENTE

Entidad: `TareaFirma`. **Forma elegida**: `createAllowProperties` (**PREEXISTENTE, no cambia**).
**Origen spec:** ninguna acción del `entity-TareaFirma.md` la declara (solo declara `Crear`, `Modificar` y `Firmar en el servidor`): es una acción **preexistente** del flujo de AutoFirma que esta iniciativa conserva.

Whitelist real: `Map.of("documentosFirma", Map.of("documentoFirmado", Map.of()))`.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `documentosFirma.documentoFirmado` | cliente | sí | Es el flujo de **AutoFirma**: el PDF lo firma el navegador del usuario y sube ya firmado, así que el cliente **tiene** que poder dictarlo. `validarDocumentosFirmados` comprueba antes que lo subido es una firma válida del documento original. |
| `estadoTareaFirma`, `fechaResolucion` | servidor | **NO** | Los asigna `fireActionRule_ResolverComoFirmada` (incondicional). Tras el delta del Paso 5.2 es **la misma** regla que usa `firmarEnServidor`. |
| *(el resto de campos)* | servidor | **NO** | Fijados al crear la tarea; la acción no los toca. |

**No cambia en esta iniciativa**: se documenta porque `design-contract.md` §8.3 pide una tabla por **cada**
acción del servicio invocada desde un `@CallMethod`, y el diseño mantiene ese `@CallMethod` intacto.

### `TareaFirmaServiceImpl.marcarComoRechazada` (invocado desde `TareaFirmaController.marcarComoRechazada`) — PREEXISTENTE

Entidad: `TareaFirma`. **Forma elegida**: `createAllowProperties` (**PREEXISTENTE, no cambia**).
**Origen spec:** ninguna acción del `entity-TareaFirma.md` la declara: es una acción **preexistente** del flujo de rechazo que esta iniciativa conserva.

Whitelist real: `Map.of("motivoRechazo", Map.of())`.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `motivoRechazo` | cliente | sí | Único input del usuario al rechazar: lo teclea en el panel `paso2Rechazado`. |
| `estadoTareaFirma`, `fechaResolucion` | servidor | **NO** | Los asigna el propio `marcarComoRechazada` (incondicional). |
| *(el resto de campos)* | servidor | **NO** | Fijados al crear la tarea; la acción no los toca. |

**No cambia en esta iniciativa**: se documenta por la misma razón que la anterior.

### `TareaFirmaServiceImpl.validarDocumentosFirmados` (invocado desde `TareaFirmaController.validarDocumentosFirmados`) — PREEXISTENTE, **deuda de seguridad**

Entidad: `TareaFirma`. **Forma elegida**: `createAllowAllProperties` (**PREEXISTENTE**).
**Origen spec:** ninguna acción del `entity-TareaFirma.md` la declara: es una acción **preexistente** del flujo de AutoFirma que esta iniciativa conserva.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| *(todos)* | cliente | **sí — allow-all** | ⚠️ **DEUDA PREEXISTENTE.** `allowPropertiesValidarDocumentosFirmados()` devuelve hoy `AllowProperties.createAllowAllProperties()`, así que en esa llamada el cliente puede dictar **cualquier** campo de la entidad gestionada (incluidos `estadoTareaFirma`, `fechaResolucion`, `firmante` o `fqcnFirmaNotifier`). La acción solo valida y no persiste, pero el bean queda contaminado dentro de la petición: en particular `validarDocumentosFirmados` comprueba la firma contra `tareaFirma.getFirmante().getDni()`, así que un cliente que dicte `firmante` valida el PDF contra el DNI que él elija. Contraviene `k-secure-coding` §3.2, que exige whitelist explícita sobre una entidad gestionada. |

**Fuera del alcance de esta iniciativa.** Se anota aquí para que no se lea como un descuido de este diseño:
la spec no pide tocar el flujo de AutoFirma y cerrarla exige comprobar qué campos necesita realmente
`validarDocumentosFirmados` (probablemente solo `documentosFirma.documentoFirmado`, como
`marcarComoFirmada`), lo que es un cambio de comportamiento del flujo preexistente. **MUST NOT** cerrarla «de
paso» al implementar esta iniciativa: hay que abordarla con sus propios tests. Ver también la nota 6 de
«Notas y supuestos».

### `TareaFirmaServiceImpl.insert` (endpoint REST automático `/ws/rest/<FQN>`)

Entidad: `TareaFirma`. **Forma elegida**: `createDenyAllProperties`.
**Origen spec:** `Input AllowProperties` de la acción `Crear` de `entity-TareaFirma.md` — «(ninguna)».

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| *(todos)* | servidor | **NO** | Las tareas de firma no se dan de alta desde la interfaz: las crea el proceso que necesita la firma, por la vía programática `insert(TareaFirmaInsertDTO)` (§DTO de alta programática). El alta por el endpoint REST genérico no es un flujo legítimo. |

### `TareaFirmaServiceImpl.update` (endpoint REST automático `/ws/rest/<FQN>`)

Entidad: `TareaFirma`. **Forma elegida**: `createDenyAllProperties`.
**Origen spec:** `Input AllowProperties` de la acción `Modificar` de `entity-TareaFirma.md` — «(ninguna)».

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| *(todos)* | servidor | **NO** | La tarea solo cambia mediante sus acciones propias (`marcarComoFirmada`, `marcarComoRechazada`, `firmarEnServidor`), cada una con su propia whitelist. Ninguna vista de `TareaFirma` usa la acción `save`, así que cerrar esta puerta no rompe ningún flujo. |

### DTO de alta programática

`TareaFirmaInsertDTO` (preexistente, **no cambia**): `firmante`, `documentos`, `motivoFirma`, `areaFirma`,
`page`, `firmaNotifierClass`, `callBackData`. El DTO **es** la whitelist de esa vía (k-secure-coding §3.5) y no
llega del cliente: lo construye código de servidor (un `PhaseEventManager` de un trámite, o el
`TareaFirmaDemoLoader` de los datos de demo). Todos sus campos son legítimos porque quien encarga la firma es
precisamente quien decide a quién, qué documentos y dónde va el recuadro.

### Trazabilidad Origen spec → V/R/U → ubicación

### Validaciones `V-TareaFirma-NNN`

| V | Origen spec | Ubicación | Qué comprueba |
|---|---|---|---|
| V-TareaFirma-001 | VAL-TareaFirma-001 | `TareaFirmaServiceImpl.validateFirmarEnServidor` | La tarea está en estado `PENDIENTE` |
| V-TareaFirma-002 | VAL-TareaFirma-002 | `TareaFirmaServiceImpl.validateFirmarEnServidor` | El firmante de la tarea es el usuario autenticado (`AuthUtils.getUser()`) |
| V-TareaFirma-003 | VAL-TareaFirma-003 | `TareaFirmaServiceImpl.validateFirmarEnServidor` | La situación de firma no es `SIN_DNI` |
| V-TareaFirma-004 | VAL-TareaFirma-004 | `TareaFirmaServiceImpl.validateFirmarEnServidor` | La situación de firma no es `SIN_CERTIFICADO` |
| V-TareaFirma-005 | VAL-TareaFirma-005 | **Servidor (fuente de verdad):** `TareaFirmaServiceImpl.validateFirmarEnServidor`. **Cliente (refuerzo opcional):** `<error>` «El PIN es obligatorio» de `…-Local-validateFirmarEnServidor-action` en `views/Pendiente-TareaFirma.xml` | Si la situación es `DISPOSITIVO_SIN_PIN`, `claveFirma` no está vacía |
| V-TareaFirma-006 | VAL-TareaFirma-006 | **Servidor (fuente de verdad):** `TareaFirmaServiceImpl.validateFirmarEnServidor`. **Cliente (refuerzo opcional):** `<error>` «La contraseña es obligatoria» de `…-Local-validateFirmarEnServidor-action` en `views/Pendiente-TareaFirma.xml` | Si la situación es `FICHERO_SIN_CLAVE`, `claveFirma` no está vacía |
| V-TareaFirma-007 | VAL-TareaFirma-007 | `TareaFirmaServiceImpl.validateFirmarEnServidor` | La tarea tiene al menos un `DocumentoFirma` |

### Reglas de negocio `R-TareaFirma-NNN`

| R | Origen spec | Ubicación | Momento | Detalle |
|---|---|---|---|---|
| R-TareaFirma-001 | RN-TareaFirma-001, RN-TareaFirma-002, RN-TareaFirma-006, RN-TareaFirma-007 | `TareaFirmaServiceImpl.fireActionRule_FirmarDocumentosEnServidor` | Antes de `repository.save` | Detalle: `design/rules/R-TareaFirma-001.md` |
| R-TareaFirma-002 | RN-TareaFirma-003 | `TareaFirmaServiceImpl.fireActionRule_ResolverComoFirmada` | Antes de `repository.save` | Estado `FIRMADO` + `fechaResolucion` (asignación incondicional) |
| R-TareaFirma-003 | RN-TareaFirma-004, RN-TareaFirma-008 | `TareaFirmaServiceImpl.fireActionRule_DescartarClaveFirma` | Siempre (`finally` de `firmarEnServidor`) | `claveFirma` a `null` pase lo que pase |
| R-TareaFirma-004 | RN-TareaFirma-005 | `TareaFirmaServiceImpl.fireActionRule_NotificarFirmaResuelta` (preexistente) | Después de `repository.save` | Callback al proceso que encargó la firma |

### Notas y supuestos aplicables

6. **Cerrar `allowPropertiesInsert`/`allowPropertiesUpdate` de `TareaFirma`.** El `entity-TareaFirma.md` declara
   «(ninguna)» para `Crear` y `Modificar`, así que el diseño las materializa como whitelist vacía. Se ha
   comprobado que **ninguna** de las cuatro vistas de `TareaFirma` (`Pendiente`, `Todos`, `Firmado`,
   `Rechazado`) usa la acción `save`, así que no hay flujo legítimo que esto rompa. Queda un flanco conocido y
   **fuera del alcance de esta iniciativa**: con la whitelist vacía, un alta por `POST /ws/rest/<FQN>` llega a
   `JPA.edit` con el mapa vacío, y el `CLAUDE.md` del proyecto advierte de que no está comprobado si crea una
   fila vacía o revienta contra los `NOT NULL`. Aquí no es explotable para falsificar datos (no se puede dictar
   ningún campo), pero **MUST NOT** darse por resuelto el problema general que describe el `CLAUDE.md`.
   Quedan además dos deudas **preexistentes** que ni la spec ni esta iniciativa abordan, anotadas para que no
   se lean como descuidos de este diseño:
   - `allowPropertiesValidarDocumentosFirmados()` devuelve `AllowProperties.createAllowAllProperties()` sobre
     una entidad gestionada, así que en esa llamada el cliente puede dictar cualquier campo (ver la tabla
     `TareaFirmaServiceImpl.validarDocumentosFirmados` de §Frontera de confianza). **MUST NOT** cerrarse «de
     paso» al implementar esta iniciativa: cambia el flujo preexistente de AutoFirma y necesita sus propios
     tests.
   - `DocumentoFirma` sigue sin `ModelService` propio.

8. **El nombre del fichero firmado.** La firma en el servidor conserva el `fileName` del documento original
   (`DocumentoPdf.firmar` no lo cambia), a diferencia de AutoFirma, que añade el sufijo `_signed`. Ninguna regla
   del spec depende del nombre y los escenarios solo comprueban que el documento «tiene versión firmada»; se
   opta por no añadir código solo para renombrar.

10. **`required="true"` en el campo de la clave, y las dos capas de la validación.** El `required` lo pide
    RUI-…-010 y aporta solo la **marca visual** del cliente: el `required` de Axelor bloquea el flujo de `save`,
    y esta pantalla no guarda —resuelve con una acción propia—, así que por sí solo no impide pulsar el botón.
    Por eso la clave obligatoria tiene **dos capas de validación**, no una:
    - **Cliente (opcional, refuerzo):** `…-Local-validateFirmarEnServidor-action`, un `<action-validate>` con
      los dos `<error>` y los literales exactos del spec, encadenado el primero del `action-group` del botón.
      Evita el roundtrip.
    - **Servidor (fuente de verdad, obligatoria):** V-TareaFirma-005 / V-TareaFirma-006 en
      `validateFirmarEnServidor`, con los mismos literales. Es la única capa por la que pasan todas las vías
      de entrada.

    Como los literales coinciden, ESC-004 ve el mismo mensaje («La contraseña es obligatoria») lo produzca el
    cliente o el servidor: no queda ninguna duda que resolver al ejecutar el E2E. **MUST NOT** quitar la
    validación de servidor en ningún caso; si algún día molestara la marca de `required`, la corrección sería
    cambiarla por `requiredIf` sobre el mismo panel.

11. **Ambigüedad resuelta — nombre de la acción.** La spec llama a la acción «Firmar en el servidor» y al botón
    «Firmar todos los documentos y finalizar». El diseño nombra el método del servicio, el `@CallMethod` y las
    acciones de vista como `firmarEnServidor` (y `validateFirmarEnServidor`), que es el nombre de la **acción**
    del `entity-*.md`, no el rótulo del botón; así el par acción ↔ validador y el `Remote-{nombreFuncionJava}`
    quedan alineados sin ambigüedad.

13. **Desviación acotada de RN-TareaFirma-006 — en `DISPOSITIVO_SIN_PIN` el PIN tecleado se descarta.**
    RN-TareaFirma-006 dice que «la clave que teclea el firmante solo se usa cuando no hay ninguna guardada». El
    diseño lo cumple en las dos ramas de **fichero** (`FICHERO_CON_CLAVE` ignora la tecleada, `FICHERO_SIN_CLAVE`
    la usa), pero **NO** en la rama de **dispositivo**, y conviene decirlo sin rodeos:
    - Lo que sí se hace: el panel `paso2FirmarDispositivoSinPin` pide el PIN (U-…-003, U-…-007, U-…-010) y
      V-TareaFirma-005 exige que no venga vacío, con el literal «El PIN es obligatorio».
    - Lo que **no** se hace: ese PIN **no llega nunca a la firma**. `getAlmacenClaveByDni(dni, claveAcceso)`
      construye para `DISPOSITIVO_PKCS11` un `AlmacenClaveDispositivo(slot, alias)`, cuyo **único** constructor
      útil recibe `(int slot, String alias)` y **no tiene forma de recibir un PIN**; quien aporta el PIN es
      `EntornoCriptografico.getDispositivoCriptografico(slot)`, que lo lee de la **configuración del servidor**
      (`entornoCriptografico.*` de `axelor-config.properties`), no de la pantalla. Por eso el `claveAcceso`
      recibido se descarta en esa rama (Paso 3 y `rules/R-TareaFirma-001.md` §«Cuál clave se usa»).
    - Por qué se acepta: hoy el caso es **inalcanzable** (`DispositivoCriptografico.pin` es `required="true"`,
      así que `getTipoAlmacenClaveByDni` nunca devuelve `DISPOSITIVO_SIN_PIN`), y el `specification.md`
      §"Fuera de alcance" deja explícitamente fuera hacer opcional ese PIN. La desviación, por tanto, **no tiene
      efecto observable** en ningún escenario del spec.
    - **Qué haría falta para cerrarla de verdad** (fuera del alcance de esta iniciativa, anotado para
      `/sdd-implementer` y para quien retome el tema): **no basta** con hacer opcional el PIN del dispositivo.
      Hace falta además **ampliar la infraestructura de criptografía** (`base/infrastructure/criptografia/`) con
      un `AlmacenClaveDispositivo` que acepte el PIN y con el camino en `DocumentoPdfImplIText` para que ese PIN
      llegue al `KeyStore` PKCS#11, en vez de tomarlo siempre de `EntornoCriptografico`. Son ficheros de
      infraestructura compartida, ajenos al subsistema `firmas`, y esta iniciativa **MUST NOT** tocarlos.
    - Mientras tanto, **MUST NOT** documentarse ni en el código ni en las vistas que el PIN tecleado «se usa
      para firmar»: se pide, se valida y se descarta.
