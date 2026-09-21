---
type: implementation-task
template: system
---

# Tarea 09 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-validaciones
- k-secure-coding
- k-code-quality

**Alcance de esta tarea:** crear la interfaz `NuevoExpedienteService` y su implementación `NuevoExpedienteServiceImpl` (dos filas `Crear`, un único componente: servicio con su impl). Es Java: se materializa delegando en `developer-code-implementer` (`implementation.md` §2). **Superficie cerrada:** solo las acciones, validadores, `allowProperties*` y métodos privados que el diseño lista. El dominio `NuevoExpediente` (tarea 01), `ExpedienteService` (07), `ContextoTramitacion` (03), `FormaPresentacion` (02) y `VistaExpediente` (06) ya están en el árbol. El borrado de `ContextoTramitacionService` y su test **no** es de esta tarea (tarea 10).

**Del diseño — filas de la tabla «Ficheros a crear o modificar» (verbatim)**

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/system/expedientes/service/NuevoExpedienteService.java` | Crear | k-sistemas (servicios.md) | Interfaz del servicio de la ventana (Paso 5) |
| `src/main/java/com/educaflow/system/expedientes/service/impl/NuevoExpedienteServiceImpl.java` | Crear | k-sistemas (servicios.md), k-validaciones | Calcula los campos de la ventana y convierte la ventana en un `ContextoTramitacion` (Paso 5) |

**Del diseño — Paso 5 (verbatim; el borrado de `ContextoTramitacionService` y su test es de la tarea 10)**

### Paso 5 — Servicio de la ventana: `NuevoExpedienteService`

```java
// Clase: com.educaflow.system.expedientes.service.NuevoExpedienteService
public interface NuevoExpedienteService extends ModelService<NuevoExpediente> {
    NuevoExpediente preparar(NuevoExpediente nuevoExpediente);
    Optional<BusinessMessages> validatePreparar(NuevoExpediente nuevoExpediente);
    AllowProperties allowPropertiesPreparar();

    NuevoExpediente recalcular(NuevoExpediente nuevoExpediente);
    Optional<BusinessMessages> validateRecalcular(NuevoExpediente nuevoExpediente);
    AllowProperties allowPropertiesRecalcular();

    VistaExpediente crear(NuevoExpediente nuevoExpediente) throws BusinessException;
    Optional<BusinessMessages> validateCrear(NuevoExpediente nuevoExpediente);
    AllowProperties allowPropertiesCrear();
}
```

La acción «Recalcular» del spec es una sola acción `recalcular`, que calcula a la vez CC-004, CC-005, CC-006 y CC-007 (y `hayQuePreguntarParaQuien`) con el trámite, el centro y la forma de presentar que hay en la ventana.
Cada llamada devuelve valores **definitivos** para el estado que recibe: CC-005/006 se calculan con la **forma de presentar vigente** (la deducida, CC-007, cuando la hay; la de la ventana cuando no), que decide solo el servidor (`getFormaPresentarVigente`) y devuelve en el campo `presentadoEnPapelVigente`; la vista la copia sin condición a «presentado en papel» (RUI-007). Ninguna etapa de la vista produce valores que otra tenga que corregir ni vuelve a decidir la forma vigente (D4).
La vista la llama una sola vez por evento, en la etapa `onChangePresentadoEnPapel` (al abrir, al cambiar el centro y al cambiar «presentado en papel»).
Ninguna acción persiste: `NuevoExpediente` es `persistable="false"`, así que **MUST NOT** llamarse a `repository.save`; el único alta real es la del expediente, en el motor.

```java
// Clase: com.educaflow.system.expedientes.service.impl.NuevoExpedienteServiceImpl
public class NuevoExpedienteServiceImpl extends DefaultModelService<NuevoExpediente> implements NuevoExpedienteService {

    // @Inject ExpedienteService expedienteService;   (no es un ModelService: se inyecta, k-sistemas/servicios.md)

    public NuevoExpedienteServiceImpl(Class<NuevoExpediente> model, Repository<NuevoExpediente> repository);
    //   super(model, repository).

    // ---- Acciones ----

    @Override public NuevoExpediente preparar(NuevoExpediente nuevoExpediente);
    //   validatePreparar(...).ifPresent(throwIfInvalid); fireActionRule_AsignarDatosTramite;
    //   fireActionRule_AsignarCentrosDisponibles; devuelve el mismo objeto.

    @Override public NuevoExpediente recalcular(NuevoExpediente nuevoExpediente);
    //   validateRecalcular(...).ifPresent(throwIfInvalid); fireActionRule_AsignarFormaDePresentar;
    //   fireActionRule_AsignarOpcionesParaQuien; devuelve el mismo objeto.

    @Override public VistaExpediente crear(NuevoExpediente nuevoExpediente) throws BusinessException;
    //   validateCrear(...).ifPresent(throwIfInvalid); return fireActionRule_IniciarExpediente(nuevoExpediente).

    // ---- Métodos de Validación ----

    @Override public Optional<BusinessMessages> validatePreparar(NuevoExpediente nuevoExpediente);
    //   V-NuevoExpediente-001 (Origen spec: VAL-NuevoExpediente-001): getCentrosDisponibles(tramite)
    //   vacío → mensaje de VAL-NuevoExpediente-001 (no puede crear expedientes de este trámite en ninguno de sus
    //   centros). El trámite se obtiene con getTramiteObligatorio.

    @Override public Optional<BusinessMessages> validateRecalcular(NuevoExpediente nuevoExpediente);
    //   El spec no declara ninguna validación para «Recalcular»: devuelve siempre Optional.empty(). Existe porque
    //   el par acción + validador es contrato de toda acción propia (k-sistemas/servicios.md, «sin excepciones»).

    @Override public Optional<BusinessMessages> validateCrear(NuevoExpediente nuevoExpediente);
    //   V-NuevoExpediente-002 (Origen spec: VAL-ContextoTramitacion-001…005, tal como las pide la ventana antes de
    //   crear): return expedienteService.validateTriggerInitialEvent(toContextoTramitacion(nuevoExpediente)).
    //   La ventana no repite ninguna condición: pregunta a la puerta del motor con el mismo contexto que usará
    //   fireActionRule_IniciarExpediente.

    // ---- AllowProperties ---- (tabla completa en «Frontera de confianza»)

    @Override public AllowProperties allowPropertiesPreparar();               // createAllowProperties: tramite
    @Override public AllowProperties allowPropertiesRecalcular();             // createAllowProperties: tramite, centro, presentadoEnPapel
    @Override public AllowProperties allowPropertiesCrear();                  // createAllowProperties: tramite, centro, presentadoEnPapel, presentadoEnRepresentacion
    //   Las relaciones (tramite, centro) con submapa vacío: solo viaja su id.

    @Override public AllowProperties allowPropertiesInsert();                 // AllowProperties.createDenyAllProperties()
    @Override public AllowProperties allowPropertiesUpdate();                 // AllowProperties.createDenyAllProperties()
    //   Origen spec: Input AllowProperties «ninguna» de la acción Modificar de entity-NuevoExpediente.md (el modelo
    //   no se guarda). Comentario: el endpoint REST automático no acepta ningún campo de NuevoExpediente.

    // ---- Action Rules ----

    private void fireActionRule_AsignarDatosTramite(NuevoExpediente nuevoExpediente);
    //   R-NuevoExpediente-002 (Origen spec: CC-NuevoExpediente-001, CC-NuevoExpediente-002), campos `servidor`,
    //   momento Antes (el modelo no se persiste: se devuelve a la vista). Asignación INCONDICIONAL
    //   (k-secure-coding §3.3): nombreTramite = I18n.get(tramite.getName()); ayudaTramite = tramite.getHelp().
    //   MUST NOT envolverse en if (campo == null): lo que mande el cliente en esos campos se machaca siempre.

    private void fireActionRule_AsignarCentrosDisponibles(NuevoExpediente nuevoExpediente);
    //   R-NuevoExpediente-003 (Origen spec: CC-NuevoExpediente-003; condición de RUI-003/004), campos `servidor`.
    //   Asignación INCONDICIONAL de los dos (k-secure-coding §3.3):
    //     centrosDisponibles        = conjunto que conserva el orden (LinkedHashSet) de getCentrosDisponibles(tramite)
    //     hayUnSoloCentroDisponible = centrosDisponibles.size() == 1
    //   Único dueño de «hay un solo centro disponible»: la vista lo consulta en el readonlyIf de centro (U-004) y en
    //   el if de set-centro-unicoDisponible (U-003), sin repetir la condición.
    //   El cliente NO puede dictar estos campos: no están en ninguna whitelist y se sobrescriben siempre, aunque
    //   lleguen rellenos en el JSON. MUST NOT envolverse en if (campo == null).

    private void fireActionRule_AsignarFormaDePresentar(NuevoExpediente nuevoExpediente);
    //   R-NuevoExpediente-004 (Origen spec: CC-NuevoExpediente-004, CC-NuevoExpediente-007; valor de RUI-007), campos `servidor`,
    //   asignación INCONDICIONAL de los tres (k-secure-coding §3.3). El cliente NO puede dictar estos campos: no
    //   están en ninguna whitelist y se sobrescriben siempre, aunque lleguen rellenos en el JSON. MUST NOT
    //   envolverse en if (campo == null).
    //   formasPosibles = getFormasPosibles(tramite, centro). Las ramas son complementarias por construcción
    //   (vacío / exactamente una / más de una); «más de una» son las dos formas de la invariante de FormaPresentacion
    //   (la ventana solo puede preguntar entre dos: presentadoEnPapel es un booleano):
    //     hayQuePreguntarPresentacion = formasPosibles.size() > 1
    //     presentadoEnPapelDeducido   = getFormaDeducida(tramite, centro).map(FormaPresentacion::isPresentadoEnPapel)
    //                                   .orElse(null)
    //   (CC-007 NO se recalcula aquí contando formas: lo decide solo getFormaDeducida, el mismo helper del que sale
    //   presentadoEnPapelVigente, así que las dos no pueden divergir.)
    //   Sin centro elegido (getPerfilesDeInicio devuelve vacío por su rama explícita de centro null) o sin perfil de
    //   inicio, formasPosibles está vacío: no se pregunta y no hay forma deducida.
    //     presentadoEnPapelVigente    = getFormaPresentarVigente(nuevoExpediente).isPresentadoEnPapel()
    //   (campo servidor añadido por el diseño; la vista lo copia SIN condición a presentadoEnPapel, U-007, D4).
    //   R-NuevoExpediente-005 NO lee ninguno de los campos que escribe esta regla: el orden entre las dos es
    //   indiferente.

    private void fireActionRule_AsignarOpcionesParaQuien(NuevoExpediente nuevoExpediente);
    //   R-NuevoExpediente-005 (Origen spec: CC-NuevoExpediente-005, CC-NuevoExpediente-006; condición de
    //   RUI-011/012/013/016), campos `servidor`, asignación INCONDICIONAL de los tres (k-secure-coding §3.3).
    //   El cliente NO puede dictar estos campos: no están en ninguna whitelist y se sobrescriben siempre, aunque
    //   lleguen rellenos en el JSON. MUST NOT envolverse en if (campo == null).
    //   Pregunta a la puerta del motor (D2) con la forma de presentar VIGENTE, que obtiene del mismo helper que
    //   R-NuevoExpediente-004 (NO lee presentadoEnPapelDeducido ni presentadoEnPapelVigente del modelo):
    //     perfil = getFormaPresentarVigente(nuevoExpediente).getProfile()
    //     sePuedeCrearParaMi           = admiteElAlta(new ContextoTramitacion(tramite, centro, perfil, false))
    //     sePuedeCrearEnRepresentacion = admiteElAlta(new ContextoTramitacion(tramite, centro, perfil, true))
    //     hayQuePreguntarParaQuien     = sePuedeCrearParaMi && sePuedeCrearEnRepresentacion
    //   Por qué la vigente y no la de la ventana: cuando la forma está deducida (un único perfil) es la forma de
    //   presentar del expediente (la ventana ni la pregunta, RUI-006) y la vista copia presentadoEnPapelVigente a
    //   presentadoEnPapel sin volver a decidir nada; como las dos reglas salen del mismo helper, CC-005/006 se
    //   calculan exactamente para la forma que la ventana acaba mostrando, en la misma llamada que CC-007 (D4).
    //   Sin centro los tres salen false (V-ContextoTramitacion-001); si el trámite no admite representación,
    //   sePuedeCrearEnRepresentacion sale false (V-ContextoTramitacion-005); si el usuario no tiene ese perfil,
    //   todos false. hayQuePreguntarParaQuien es el único dueño de «caben las dos opciones»: la vista lo consulta en
    //   showIf, requiredIf y set-presentadoEnRepresentacion-segunOpciones sin repetir la condición.

    private VistaExpediente fireActionRule_IniciarExpediente(NuevoExpediente nuevoExpediente) throws BusinessException;
    //   R-NuevoExpediente-001 (Origen spec: RN-NuevoExpediente-001), momento Antes del commit (dentro de la
    //   transacción del controlador). Secuencia:
    //     1. contexto = toContextoTramitacion(nuevoExpediente)   ← el perfil lo decide el sistema
    //     2. expediente = expedienteService.triggerInitialEvent(contexto)
    //     3. return expedienteService.getVistaExpediente(expediente, contexto.profile())

    // ---- Otras funciones ----

    private ContextoTramitacion toContextoTramitacion(NuevoExpediente nuevoExpediente);
    //   new ContextoTramitacion(tramite, centro, FormaPresentacion.fromPresentadoEnPapel(presentadoEnPapel).getProfile(),
    //   presentadoEnRepresentacion). Es el núcleo de R-NuevoExpediente-001: el perfil NUNCA sale de la ventana
    //   (NuevoExpediente no tiene campo profile), sale de la forma de presentar. La usan validateCrear y
    //   fireActionRule_IniciarExpediente, así que se valida exactamente el contexto que luego se inicia.

    private List<Centro> getCentrosDisponibles(Tramite tramite);
    //   Centros del usuario autenticado (los centros de los CentroUsuario de SecurityUtil.getUser(); lista vacía si
    //   no tiene), sin nulls, filtrados por !expedienteService.getPerfilesDeInicio(tramite, centro).isEmpty(),
    //   ordenados por nombre con nulls al final. Movido de ContextoTramitacionService.getCentros.
    //   Alcance multi-centro: solo los centros del propio usuario, nunca centroActivo (CLAUDE.md, expedientes).
    //   «Qué perfiles de inicio tiene el usuario en un centro» sigue viviendo solo en getPerfilesDeInicio (D2):
    //   esta función solo recorre los centros del usuario preguntándoselo. La usan validatePreparar y
    //   fireActionRule_AsignarCentrosDisponibles.

    private boolean admiteElAlta(ContextoTramitacion contexto);
    //   expedienteService.validateTriggerInitialEvent(contexto).isEmpty().

    private FormaPresentacion getFormaPresentarVigente(NuevoExpediente nuevoExpediente);
    //   Único dueño de «qué forma de presentar vale ahora» (D4). Total, dos ramas complementarias (se apoya en la
    //   invariante de FormaPresentacion; si se rompe, fromPresentadoEnPapel lanza IllegalStateException):
    //     getFormaDeducida(tramite, centro).orElseGet(() -> FormaPresentacion.fromPresentadoEnPapel(presentadoEnPapel
    //     de la ventana))
    //     - hay forma deducida (CC-007) → esa;
    //     - si no (ninguna o varias formas posibles) → la de la ventana: si hay que preguntar es lo que el usuario ha
    //       contestado; sin formas posibles da igual cuál, porque la puerta rechaza cualquier perfil (CC-005/006
    //       salen false).
    //   La usan R-NuevoExpediente-004 (presentadoEnPapelVigente) y R-NuevoExpediente-005 (perfil de CC-005/006).
    //   La vista MUST NOT repetir esta decisión: solo copia presentadoEnPapelVigente.

    private Optional<FormaPresentacion> getFormaDeducida(Tramite tramite, Centro centro);
    //   Único dueño de CC-007 («la forma deducida es la única forma posible; no hay deducida si hay cero o varias»):
    //   la forma si getFormasPosibles(tramite, centro) tiene exactamente una, vacío si no.
    //   La usan R-NuevoExpediente-004 (presentadoEnPapelDeducido) y getFormaPresentarVigente (presentadoEnPapelVigente
    //   y, a través de ella, R-NuevoExpediente-005): cambiar el criterio de deducción es tocar solo esta función.

    private Set<FormaPresentacion> getFormasPosibles(Tramite tramite, Centro centro);
    //   Las constantes de FormaPresentacion.values() cuyo getProfile() está en
    //   expedienteService.getPerfilesDeInicio(tramite, centro), en un EnumSet. Deriva las formas del dueño (D1, D2)
    //   en vez de contar perfiles; como mucho devuelve las dos formas de la invariante de FormaPresentacion.
    //   La usan R-NuevoExpediente-004 (hayQuePreguntarPresentacion) y getFormaDeducida.

    private Tramite getTramiteObligatorio(NuevoExpediente nuevoExpediente);
    //   Devuelve el trámite o lanza IllegalStateException: la vista lo fija siempre al abrirse
    //   (U-nuevo-expediente-014), así que un NuevoExpediente sin trámite solo llega por un error de programación
    //   o una petición manipulada; no es una validación que el usuario pueda corregir. La usan todas las acciones.
}
```


Verificación: los 5 headers de bloque de `NuevoExpedienteServiceImpl` con sus 3 líneas de igual longitud (`awk` de `k-sistemas/servicios.md`) (compilación en el Paso 6).

**Del diseño — Frontera de confianza — AllowProperties por acción (verbatim)**

## Frontera de confianza — AllowProperties por acción

### `NuevoExpedienteServiceImpl.preparar` y `validatePreparar` (invocados desde `NuevoExpedienteController.preparar` y `.validatePreparar`)

Entidad: `NuevoExpediente`. **Forma elegida**: `createAllowProperties`.
**Origen spec:** `Input AllowProperties` de la acción `Preparar` de `entity-NuevoExpediente.md`.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `tramite` | cliente | sí (solo `id`) | Input de la acción; lo fija la vista con el trámite elegido en «Trámites» (U-014). |
| `centro`, `presentadoEnPapel`, `presentadoEnRepresentacion` | cliente | **NO** | No son entrada de `Preparar`. |
| `nombreTramite`, `ayudaTramite` | servidor | **NO** | Asignados incondicionalmente en `fireActionRule_AsignarDatosTramite` (R-NuevoExpediente-002). |
| `centrosDisponibles`, `hayUnSoloCentroDisponible` | servidor | **NO** | Asignados incondicionalmente en `fireActionRule_AsignarCentrosDisponibles` (R-NuevoExpediente-003). |
| `hayQuePreguntarPresentacion`, `presentadoEnPapelDeducido`, `presentadoEnPapelVigente`, `sePuedeCrearParaMi`, `sePuedeCrearEnRepresentacion`, `hayQuePreguntarParaQuien` | servidor | **NO** | No los usa `Preparar`; los calcula `Recalcular`. |

### `NuevoExpedienteServiceImpl.recalcular` (invocado desde `NuevoExpedienteController.recalcular`)

Entidad: `NuevoExpediente`. **Forma elegida**: `createAllowProperties`.
**Origen spec:** `Input AllowProperties` de la acción `Recalcular` de `entity-NuevoExpediente.md` (trámite, centro, presentado en papel).

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `tramite` | cliente | sí (solo `id`) | Input de `Recalcular`. |
| `centro` | cliente | sí (solo `id`) | Input de `Recalcular`; no se confía en él: solo sirve para calcular, y la puerta del motor lo vuelve a validar al crear. |
| `presentadoEnPapel` | cliente | sí | Input de `Recalcular`; cuando no hay forma deducida, de él sale el perfil con el que se pregunta a la puerta del motor (CC-005/006). Cuando la hay, se ignora y manda la deducida (`getFormaPresentarVigente`, que usan R-NuevoExpediente-004/005). |
| `presentadoEnRepresentacion` | cliente | **NO** | No es entrada de `Recalcular`. |
| `hayQuePreguntarPresentacion`, `presentadoEnPapelDeducido`, `presentadoEnPapelVigente` | servidor | **NO** | Asignados incondicionalmente en `fireActionRule_AsignarFormaDePresentar` (R-NuevoExpediente-004). |
| `sePuedeCrearParaMi`, `sePuedeCrearEnRepresentacion`, `hayQuePreguntarParaQuien` | servidor | **NO** | Asignados incondicionalmente en `fireActionRule_AsignarOpcionesParaQuien` (R-NuevoExpediente-005). |
| `nombreTramite`, `ayudaTramite`, `centrosDisponibles`, `hayUnSoloCentroDisponible` | servidor | **NO** | No los toca esta acción. |

### `NuevoExpedienteServiceImpl.crear` y `validateCrear` (invocados desde `NuevoExpedienteController.crear` y `.validateCrear`)

Entidad: `NuevoExpediente`. **Forma elegida**: `createAllowProperties`.
**Origen spec:** `Input AllowProperties` de la acción `Crear` de `entity-NuevoExpediente.md`.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `tramite` | cliente | sí (solo `id`) | Input de `Crear`; lo revalida la puerta del motor (V-ContextoTramitacion-002). |
| `centro` | cliente | sí (solo `id`) | Input de `Crear`; lo revalida la puerta del motor (V-ContextoTramitacion-001/002). |
| `presentadoEnPapel` | cliente | sí | Input de `Crear`; solo sirve para deducir el perfil en `toContextoTramitacion` (R-NuevoExpediente-001), que la puerta revalida (V-ContextoTramitacion-003). |
| `presentadoEnRepresentacion` | cliente | sí | Input de `Crear`; lo revalida la puerta del motor (V-ContextoTramitacion-004/005). |
| perfil | servidor | **NO existe en el modelo** | Lo decide `toContextoTramitacion` a partir de la forma de presentar; la ventana no tiene campo por el que enviarlo (RN-NuevoExpediente-001). |
| Campos calculados (CC-001…007), `hayUnSoloCentroDisponible`, `hayQuePreguntarParaQuien` | servidor | **NO** | `Crear` no los lee. |

`allowPropertiesInsert()` y `allowPropertiesUpdate()` devuelven `AllowProperties.createDenyAllProperties()` en `NuevoExpedienteServiceImpl` (Origen spec: Input AllowProperties «ninguna» de `Modificar` en `entity-NuevoExpediente.md`: el modelo no se guarda) y en `TipoExpedienteServiceImpl` (Origen spec: Input AllowProperties «ninguna» de `Crear` y `Modificar` en `entity-TipoExpediente.md`), así que el endpoint REST automático no acepta ningún campo de ninguna de las dos entidades.
No es el parche de `Expediente` que `CLAUDE.md` prohíbe reintroducir: son una entidad de pantalla y una de catálogo, fuera de la tramitación.

**Del diseño — Trazabilidad de validaciones (V), reglas de negocio (R) y campos calculados (CC) (verbatim)**

### Validaciones (V)

| V | Origen spec | Ubicación |
|---|---|---|
| V-NuevoExpediente-001 | VAL-NuevoExpediente-001 | `NuevoExpedienteServiceImpl.validatePreparar` (vista: `Remote-validatePreparar` al abrir) |
| V-NuevoExpediente-002 | VAL-ContextoTramitacion-001, VAL-ContextoTramitacion-002, VAL-ContextoTramitacion-003, VAL-ContextoTramitacion-004, VAL-ContextoTramitacion-005 | `NuevoExpedienteServiceImpl.validateCrear` → `ExpedienteService.validateTriggerInitialEvent` (vista: `Remote-validateCrear` en `btnCrear`) |

### Reglas de negocio (R)

| R | Origen spec | Ubicación |
|---|---|---|
| R-NuevoExpediente-001 | RN-NuevoExpediente-001 | `NuevoExpedienteServiceImpl.fireActionRule_IniciarExpediente` (+ `toContextoTramitacion`, que usa `FormaPresentacion.fromPresentadoEnPapel(...).getProfile()`) |
| R-NuevoExpediente-002 | CC-NuevoExpediente-001, CC-NuevoExpediente-002 | `NuevoExpedienteServiceImpl.fireActionRule_AsignarDatosTramite` (acción `preparar`) |
| R-NuevoExpediente-003 | CC-NuevoExpediente-003 (+ condición de RUI-003/004 en `hayUnSoloCentroDisponible`) | `NuevoExpedienteServiceImpl.fireActionRule_AsignarCentrosDisponibles` (acción `preparar`) |
| R-NuevoExpediente-004 | CC-NuevoExpediente-004, CC-NuevoExpediente-007 (+ valor de RUI-007 en `presentadoEnPapelVigente`) | `NuevoExpedienteServiceImpl.fireActionRule_AsignarFormaDePresentar` (+ `getFormasPosibles`, `getFormaDeducida`, `getFormaPresentarVigente`) (acción `recalcular`) |
| R-NuevoExpediente-005 | CC-NuevoExpediente-005, CC-NuevoExpediente-006 (+ condición de RUI-011/012/013/016 en `hayQuePreguntarParaQuien`) | `NuevoExpedienteServiceImpl.fireActionRule_AsignarOpcionesParaQuien` (+ `getFormaPresentarVigente`; independiente del orden respecto a R-NuevoExpediente-004) (acción `recalcular`) |

### Campos calculados (CC, `momento: lectura`)

Son campos `servidor` de un modelo que no se persiste: no hay columna ni `formula`, porque su cálculo necesita al usuario y sus perfiles (servicios); los calcula la acción correspondiente y se devuelven a la vista.

| CC | Campo del modelo | Lo calcula |
|---|---|---|
| CC-NuevoExpediente-001 | `nombreTramite` | R-NuevoExpediente-002 |
| CC-NuevoExpediente-002 | `ayudaTramite` | R-NuevoExpediente-002 |
| CC-NuevoExpediente-003 | `centrosDisponibles` | R-NuevoExpediente-003 |
| CC-NuevoExpediente-004 | `hayQuePreguntarPresentacion` | R-NuevoExpediente-004 |
| CC-NuevoExpediente-005 | `sePuedeCrearParaMi` | R-NuevoExpediente-005 |
| CC-NuevoExpediente-006 | `sePuedeCrearEnRepresentacion` | R-NuevoExpediente-005 |
| CC-NuevoExpediente-007 | `presentadoEnPapelDeducido` | R-NuevoExpediente-004 |

Campos `servidor` añadidos por el diseño (no son CC del spec: son la condición o el valor de una regla de UI, calculados una sola vez para que la vista no los repita): `hayUnSoloCentroDisponible` (R-NuevoExpediente-003), `presentadoEnPapelVigente` (R-NuevoExpediente-004) y `hayQuePreguntarParaQuien` (R-NuevoExpediente-005).

**Del diseño — Notas y supuestos aplicables (verbatim)**

12. **`validateRecalcular` sin reglas:** es el único validador vacío; existe por el contrato «acción + validador» de `k-sistemas/servicios.md`; el spec no define validaciones para «Recalcular».
