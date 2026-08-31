---
name: k-tipo-expediente
description: Cómo crear un tipo de expediente (una versión `v1`/`v2`… de un trámite) en `tramites/<tramite>/<vN>/`: el fichero maestro `TipoExpedienteInstance.xml` con sus **fases** y la máquina de estados, el modelo (`domains.xml`), y por cada fase su `PhaseEventManager`, su `StateEventValidator` y sus vistas preprocesadas; los documentos PDF (`documentospdf/`, formato XML de definición) y la receta para duplicar un tipo y crear la versión siguiente. Cárgalo siempre que crees o modifiques cualquier fichero bajo una carpeta de versión de un trámite.
---

# k-tipo-expediente

Un tipo de expediente es la implementación **versionada** de un trámite: la carpeta `tramites/<tramite>/<vN>/` con su máquina de estados, entidad, vistas, validaciones y documentos. El trámite en sí (el `TramiteInstance.xml` padre) es de `k-tramite`, que delega aquí la creación de cada versión.

**Convención de los ejemplos de este skill**: todos usan un trámite **inventado** —code `MiTramite`, carpeta `tramites/mi_tramite/`, versión `v1/`, entidad `MiTramiteV1`— con las fases `RECEPCION` y `TRAMITACION`.
  Es deliberado que no apunten a ningún trámite del árbol: los trámites van y vienen, y un ejemplo que nombra una carpeta concreta se queda mintiendo en cuanto esa carpeta se renombra o se borra.
  Sustituye `MiTramite` por el code de tu trámite y `mi_tramite/v1` por su ruta real, que **no** tiene por qué colgar directamente del trámite ni ser plana (`versionado.md` §1).

Los estados se agrupan en **fases**, y cada fase tiene su propia subcarpeta con su `PhaseEventManagerImpl`, su `StateEventValidatorImpl` y su `views.xml` (§1.4).

## Ficheros de este skill

| Fichero | Contenido |
|---------|-----------|
| `modelo.md` | El `domains.xml` del tipo (en la raíz de la versión, uno para todas las fases): entidad `extends="Expediente"`, enums versionados, campos `MetaFile` para los PDF, `extra-code-model` |
| `phaseeventmanager.md` | La máquina de estados en Java: el `PhaseEventManagerImpl` de cada fase, métodos `trigger*`/`onEnter*`, API de `EventContext` y el **catálogo de acciones** (generar PDF, registros de entrada/salida, firmas, correos…) |
| `validator.md` | El `StateEventValidatorImpl` de cada fase, en Kotlin: DSL de reglas por estado+evento y su doble función de whitelist de campos |
| `vistas.md` | Las vistas en formato **preprocesado** (NO sigue `k-vistas`): el form plantilla en la raíz, los `<form state=...>` repartidos por fase, `include-panels`, `footer`, visores de PDF, AutoFirma |
| `documentos.md` | El formato XML de los documentos de `documentospdf/` de los que el build genera los PDF rellenables |
| `versionado.md` | Receta para duplicar un tipo de expediente y crear la versión siguiente (`vN` → `v(N+1)`, con rutas completas: las dos carpetas no tienen por qué ser hermanas) |

---

## 1. Conceptos clave

### 1.1 Todo se deriva de la carpeta de versión

**CRITICAL**: la carpeta `tramites/<tramite>/<vN>/` determina la identidad completa del tipo. Con el `TramiteInstance.xml` padre (`code=MiTramite`, `name=Mi trámite`) y la carpeta `v1`:

| Derivado | Regla | Ejemplo |
|---|---|---|
| Versión | nombre de la carpeta en mayúsculas | `v1` → `V1` |
| `code` del tipo | code del trámite + versión | `MiTramiteV1` |
| `name` del tipo | name del trámite + " " + versión | `Mi trámite V1` |
| Entidad JPA | = `code` (el `domains.xml` **MUST** declarar `<entity name="<code>">`) | `MiTramiteV1` |
| Paquete base | la ruta tras `/java/` | `com.educaflow.tramites.mi_tramite.v1` |
| Carpeta y paquete de una fase | el `name` de la fase en **minúsculas**, colgando del paquete base | `RECEPCION` → `<paquete base>.recepcion` |
| FQCN PhaseEventManager / Validator | `<paquete de la fase>.PhaseEventManagerImpl` / `.StateEventValidatorImpl` | — |

`code` y `name` son **defaults**: se pueden sobrescribir con tags opcionales del `TipoExpedienteInstance.xml` (§2). El paquete y los FQCN **NO**: son convención pura, porque es lo que usa `ExpedienteLocator` para resolver en runtime la clase de cada estado (§1.5).

### 1.2 Fases

Una fase es **solo una forma de agrupar ficheros**:

- **Para qué existe**: que no queden `PhaseEventManagerImpl`/`StateEventValidatorImpl`/`views.xml` gigantes, y poder copiar una fase entera a otro tipo de expediente al crear tipos nuevos.
- **NO es una entidad del dominio**: no se persiste como dato maestro, no hay tabla ni entidad JPA `Fase`. Pero **sí existe en ejecución**: `Tramitador` lee el `codePhase` del expediente y resuelve por fase dos veces en cada transición (la de origen atiende el evento, la de destino el `onEnter`).
- **Qué sobrevive de ella en ejecución**: su código, que viaja en la columna `codePhase` del expediente (§1.5), su `title` en `namePhase`, y el paquete de sus clases (§1.6).
- Son **obligatorias**: todo tipo de expediente tiene al menos una y todo estado pertenece a exactamente una.
- Las transiciones **pueden cruzar fases** con toda normalidad; no hay ningún tratamiento especial.
- El `title` de la fase **es texto de interfaz**, no un comentario: viaja al `namePhase` del expediente (§1.5) y se pinta en la cabecera de los formularios de estado, en los listados de expedientes y en el historial. Si lo omites se muestra el `name` humanizado (`SUBSANACION_DOCUMENTOS` → `Subsanacion documentos`).
- **Reparto habitual** (el de los dos tipos actuales, no obligatorio): `RECEPCION` = los estados de perfil `CREADOR` (el interesado prepara y presenta); `TRAMITACION` = el resto.

### 1.3 Contenido de la carpeta del tipo

En la **raíz de la versión** va lo que es de todo el tipo:

| Fichero | Quién lo escribe | Detalle en |
|---|---|---|
| `TipoExpedienteInstance.xml` | tú | §2 (este fichero) |
| `estados.puml` / `estados.png` | tú / build (renderiza el `.puml`) | §2.4 |
| `domains.xml` | tú (esqueleto generado, §3.1) | `modelo.md` |
| `views.xml` | tú (esqueleto generado, §3.1) | `vistas.md` |
| `InitialEventManagerImpl.java` | tú (esqueleto generado, §3.1) | `phaseeventmanager.md` §2.1 |
| `documentospdf/` | tú | `documentos.md` |
| `i18n_es.csv` / `i18n_ca.csv` | build — **MUST NOT** crearlos a mano | `CLAUDE.md` (i18n) |

El `views.xml` de la raíz contiene **solo** el form de plantilla `exp-<Code>-Templates` con el catálogo de paneles (y, si hace falta, formularios y grids auxiliares con nombre propio). Los formularios de estado van repartidos por fase.

El `InitialEventManagerImpl.java` está en la raíz, y no en una fase, porque el **evento inicial es del tipo de expediente**: se dispara al crear el expediente, cuando todavía no hay estado del que partir, así que no pertenece a ninguna fase. Hay **exactamente uno por tipo** (`phaseeventmanager.md` §2.1).

En **cada subcarpeta de fase** (`<vN>/<fase en minúsculas>/`) van los tres ficheros de esa fase:

| Fichero | Obligatorio | Detalle en |
|---|---|---|
| `PhaseEventManagerImpl.java` | sí — lo exigen los tests (§3.3) y `ExpedienteLocator` en runtime | `phaseeventmanager.md` |
| `StateEventValidatorImpl.kt` | sí — ídem | `validator.md` |
| `views.xml` (los `<form state="...">` de sus estados) | **no para el build**, pero sí para los tests: sin él ningún estado de la fase tiene vista y los estados revientan al navegar (§3.3) | `vistas.md` |

Si una fase no tiene forms de estado, **MUST NOT** dejar un `views.xml` vacío: un `<object-views>` sin hijos no valida contra el XSD y aborta la carga de vistas, menús y data-init — se omite el fichero entero.

### 1.4 Qué le toca a cada fase

- **Estados**: los suyos, y solo los suyos. El `onEnter<Estado>` de un estado vive en el `PhaseEventManagerImpl` de su fase.
- **Eventos**: la unión, sin repetir, de los eventos de sus estados — un evento siempre se dispara *desde* un estado. Un mismo evento presente en dos fases lleva su propio `trigger<Evento>` en cada una.
- **Parejas (estado, evento)**: las de sus estados, en su `StateEventValidatorImpl`.
- **El evento inicial NO le toca a ninguna fase**: es del tipo de expediente entero y lo atiende el `InitialEventManagerImpl` de la raíz de la versión (§1.3). Un `PhaseEventManagerImpl` **MUST NOT** declarar un `triggerInitialEvent`: nadie lo llamaría.

### 1.5 La identidad de un estado: la pareja (fase, estado)

Un estado se identifica por **dos** códigos, porque el suyo solo es único dentro de su fase. Se persisten en dos columnas:

```
Expediente.codePhase = "RECEPCION"      Expediente.codeState = "ENTRADA_DATOS"
```

**MUST NOT** concatenarlos ni volver a inventar un nombre compuesto: no existe ningún `F_<fase>_S_<estado>`.

Dónde aparece cada código:

| Sitio | Qué lleva |
|---|---|
| `Expediente` / `HistorialEstado` en BD | las dos columnas `codePhase` y `codeState` |
| `namePhase` (el texto que ve el usuario en la cabecera de cada formulario de estado, en los listados y en el historial) | el `title` de la fase, o su `name` humanizado |
| Constantes del enum de fase en `States` (§2.3) | el `name` del estado tal cual (`States.Recepcion.ENTRADA_DATOS`) |
| Nombre de vista `exp-<Code>-<FASE>-<ESTADO>[-<PROFILE>]-form` | la fase y el estado, como dos segmentos |
| `<state name="...">` del `TipoExpedienteInstance.xml` | el estado; la fase la da su `<fase>` |
| `state="..."` de los `<form>` del `views.xml` de la fase | el estado; la fase la da la carpeta |
| Nombres de método (`onEnterEntradaDatos`, `getForStateEntradaDatosInEventPresentar`) | el estado; la fase la da el paquete |
| `nameState` (el texto que ve el usuario en listados e historial) | el `title` del estado, o su `name` humanizado |

En código, un estado se nombra **siempre** por su constante en la clase generada `States` (§2.3), nunca por sus strings.

### 1.6 `ExpedienteLocator`

Con fases hay N `PhaseEventManagerImpl` y N `StateEventValidatorImpl` por tipo de expediente, así que hace falta resolver cuál toca. Lo hace `ExpedienteLocator`, **en función de la fase**, que viaja en su propia columna `codePhase`.

Es un **bean inyectable** (`@Singleton`), no una clase de estáticos: quien lo necesita lo declara con `@Inject` (`Tramitador`, `ExpedienteController`) y las clases que resuelve por reflexión las instancia con el `Injector` inyectado.
Solo se pide con `Beans.get(ExpedienteLocator.class)` desde donde no hay inyección posible: el `<extra-code-model>` de la entidad `TipoExpediente` (a las entidades JPA no las construye Guice) y los métodos estáticos de `ExpedienteUtil`.

```
tipoExpediente.basePackageName + "." + fase.toLowerCase() + ".PhaseEventManagerImpl"
tipoExpediente.basePackageName + ".States"
tipoExpediente.basePackageName + ".InitialEventManagerImpl"
```

El `InitialEventManagerImpl` es el único que **no** se resuelve por fase, porque el evento inicial no es de ninguna: `getInitialEventManager(tipoExpediente)` lo compone directamente sobre el `basePackageName`.

La **entidad** del tipo es lo único suyo que **no** se puede componer por convención de nombre: no vive en el paquete de la versión sino en `com.educaflow.subsystem.expedientes.db`.
`ExpedienteLocator.getModelClass(tipoExpediente)` la resuelve leyendo el parámetro de tipo con el que el `InitialEventManagerImpl` implementa `InitialEventManager<…>`; se le pregunta a él, y no al `PhaseEventManager` de una fase, porque la entidad es del **tipo entero**, igual que él.
Que ese parámetro sea el mismo en todas las clases del tipo y coincida con el `domains.xml` lo vigila el test M1 (§3.3).

El único dato que se guarda en BD es `TipoExpediente.basePackageName` (el paquete de la carpeta de versión), que el data-init reescribe en cada arranque: mover la carpeta de un tipo se corrige solo. Los antiguos campos `fqcnPhaseEventManager` y `fqcnStateEventValidator` **ya no existen**.

`ExpedienteLocator.getTipoExpedienteStates(tipoExpediente)` devuelve el `States.INSTANCE` del tipo (cacheado) tipado como `TipoExpedienteStates`. Desde una entidad se llega con `expediente.getTipoExpediente().getTipoExpedienteStates()`, que es el punto de entrada cuando el tipo concreto **no** se conoce en compilación; cuando sí se conoce, se usa `States.INSTANCE` directamente.

Las vistas **no** pasan por el localizador: sus nombres son globales y llevan la fase y el estado como segmentos, así que `PhaseEventManager.getViewName` los compone sin más aunque los `views.xml` estén repartidos.

### 1.7 Flujo runtime de un evento

Cuando el usuario pulsa un botón (= dispara un evento), `Tramitador.triggerEvent` hace, en orden:

1. Resuelve, con `ExpedienteLocator` y el `codePhase` **actual**, el PhaseEventManager y el validator de la fase **desde la que** se dispara el evento.
2. Valida que el evento es legal en el estado actual (`State.getEvents()` de la clase `States`).
3. Obtiene las reglas del validator para (estado, evento).
4. **Copia del request SOLO los campos que tienen reglas** (whitelist anti mass-assignment → `validator.md`).
5. Ejecuta las validaciones; si fallan, `BusinessException` y los mensajes se pintan en el panel de error del footer.
6. Llama a `trigger<Evento>` del PhaseEventManager (ahí se decide el estado destino → `phaseeventmanager.md`).
7. Guarda el historial de estados; **vuelve a resolver** el PhaseEventManager con el estado **destino** (la transición ha podido cruzar de fase) y llama a su `onEnter<Estado>`; persiste.
8. Muestra la vista del nuevo estado (`vistas.md` §2 "Dos forms por estado y cómo elige el runtime").

**Eventos comunes** (existen sin declararlos): `EXIT` (cerrar la pestaña; lo intercepta `ExpedienteController`, no llega al PhaseEventManager) y `DELETE` (borra sin validar ni copiar campos). **`BACK` NO es común**: si un estado necesita "volver atrás" hay que declararlo como evento normal.

---

## 2. `TipoExpedienteInstance.xml` — el fichero maestro

Fichero mínimo real (todo lo demás se deriva, §1.1):

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<TipoExpediente>
    <fases>
        <fase name="RECEPCION" title="Recepción">
            <state name="ENTRADA_DATOS"          events="DELETE,GUARDAR_DATOS" profile="CREADOR"      title="Entrada de datos"           initial="true"  />
            <state name="PENDIENTE_PRESENTACION" events="BACK,PRESENTAR"       profile="CREADOR"      title="Pendiente de presentación"                  />
        </fase>
        <fase name="TRAMITACION" title="Tramitación">
            <state name="PENDIENTE_RESOLUCION"   events="RESOLVER"             profile="RESPONSABLE"  title="Pendiente de resolución"                    />
            <state name="ACEPTADO"               events=""                     profile="RESPONSABLE"                                     closed="true"   />
            <state name="RECHAZADO"              events=""                     profile="RESPONSABLE"                                     closed="true"   />
        </fase>
    </fases>
</TipoExpediente>
```

Tags opcionales (antes de `<fases>`): `name`, `code` y `tramite` (sobrescriben los defaults de §1.1). El XML acepta además `ambitoCreador`/`ambitoResponsable`/`ambitoAuditor`, pero hoy son **inertes**: la entidad `TipoExpediente` tiene esas tres propiedades y su enum `AmbitoTipoExpediente` **comentados**, el generador los lee como texto libre sin validar y el data-init no los persiste. **MUST NOT** usarlos en tipos nuevos.

**MUST NOT** usar un `<states>` suelto en la raíz: es el formato anterior a las fases y el parseo aborta con un error que lo explica.

### 2.1 Reglas de `<fase>`

- **MUST** haber al menos una fase, y todo `<state>` va dentro de una.
- El `name` es `UPPER_SNAKE` (el parser lo comprueba): da nombre a la constante y al enum anidado de la fase en `States`.
- Los `name` de las fases son únicos dentro del tipo.
- **MUST** existir la subcarpeta `<vN>/<name en minúsculas>/` con el `PhaseEventManagerImpl` y el `StateEventValidatorImpl` de la fase; su `views.xml` es opcional (§1.3). El parseo del `TipoExpedienteInstance.xml` **no** comprueba que la carpeta exista: quien lo detecta son los tests (§3.3) al no encontrar las clases compiladas. El nombre de la carpeta es lo que el viewprocessor usa para saber a qué fase pertenece un `views.xml` **que tenga forms de estado**, y lo que `ExpedienteLocator` compone para encontrar las clases.
- `title` es opcional, pero **lo ve el usuario** (§1.5): sin él se muestra el `name` humanizado, que en UPPER_SNAKE queda feo.

- ✅ CORRECTO: `<fase name="RECEPCION" title="Recepción">` → carpeta `recepcion/`
- ✅ CORRECTO: `<fase name="SUBSANACION_DOCUMENTOS" title="Subsanación">` → carpeta `subsanacion_documentos/`
- ❌ INCORRECTO: `<fase name="Recepcion">` (no es UPPER_SNAKE)
- ❌ INCORRECTO: `<fase name="STATES">` (su enum anidado se llamaría `States`, el nombre de la propia clase generada)

### 2.2 Reglas de `<state>`

- `events` es **obligatorio aunque esté vacío** (`events=""`). Omitirlo no da ningún error: equivale en silencio a `events=""`. Escríbelo siempre para que el estado declare de forma explícita que no dispara ningún evento.
- `profile` es opcional (estado sin dueño → `getProfile()` devuelve `null`) y **MUST** ser una constante del enum global `Profile` de `subsystem/expedientes/domains/TipoExpediente.xml`; el generador lo valida.
- **MUST** haber exactamente **un** estado `initial="true"` en todo el tipo (no uno por fase).
- `closed="true"` marca los estados terminales (el expediente queda cerrado pero existe).
- El `name` solo tiene que ser único **dentro de su fase**: lo que se persiste es la pareja (fase, estado) (§1.5). Dos fases pueden tener un estado que se llame igual — pero ten en cuenta que el `nameState` que ve el usuario sale de ese nombre, así que en los listados se verían idénticos salvo que les pongas `title` distinto.
- Nombres de estados y eventos: `UPPER_SNAKE` y **MUST** ser identificadores Java válidos — van a las constantes de `States` y a los nombres de método (`GUARDAR_DATOS` → `triggerGuardarDatos`). Un evento **MUST NOT** repetirse dentro del mismo estado.
- JAXB **ignora en silencio los tags desconocidos**: un typo en un tag opcional (p.ej. `<tramitee>`) no da error, simplemente aplica el default.

- ✅ CORRECTO: `<state name="PENDIENTE_FIRMA" events="" profile="RESPONSABLE"/>`
- ❌ INCORRECTO: `<state name="PENDIENTE_FIRMA" profile="RESPONSABLE"/>` (falta `events`, aunque sea vacío)
- ❌ INCORRECTO: `<state name="PendienteFirma" .../>` (no es UPPER_SNAKE: produce métodos inesperados como `triggerPendientefirma`)

### 2.3 Del XML sale la clase `States`

`GenerateStatesTask` proyecta el `TipoExpedienteInstance.xml` entero en **una** clase por tipo de expediente, `<basePackageName>.States`, que el build emite en `build/src-gen-states/main/java`. **MUST NOT** editarla ni versionarla: se reemite en cada build.

Su forma:

- Un **enum público por fase**, con el `name` de la fase en UpperCamelCase (`States.Recepcion`, `States.Tramitacion`), que implementa `State`. Cada constante lleva su nombre visible, su `Profile`, `initial`, `closed` y sus eventos.
- Un **alias de fase** por cada `<fase>` (`States.RECEPCION`), tipado como `Phase`.
- `States.INSTANCE`, que implementa `TipoExpedienteStates`: `getPhase`, `getState(phaseCode, stateCode)`, `getPhases`, `getStates` y `getInitialState`.
- `States.CODE` y `States.NAME`, el code y el name del tipo.

**La máquina de estados vive únicamente en esa clase en runtime** — el data-init no persiste ni estados, ni eventos, ni flags. Como lleva **todas** las fases, una transición que cruza fases se escribe sin más: `eventContext.updateState(States.Tramitacion.PENDIENTE_RESOLUCION)`.

Los eventos son **strings**, no un enum: el `trigger<Evento>` se busca por nombre a partir del propio string.

### 2.4 `estados.puml`

Dibuja la máquina antes de escribir el XML. El build renderiza el `.png` (se salta el render si el `.png` es más nuevo).
**MUST** existir en la raíz de la carpeta de versión, junto al `TipoExpedienteInstance.xml`: es el único sitio donde se ve la máquina entera, porque el destino de cada evento no está en el XML sino en el `updateState` de su `PhaseEventManagerImpl`.
Lo comprueba la regla D1 (§3.3).
Convenciones:

- Los estados de cada fase van **anidados dentro de un estado compuesto** que representa la fase (solo a efectos visuales).
- **MUST** declarar cada estado con el alias `<FASE>_<ESTADO>` y su nombre corto como etiqueta, y usar ese alias en las transiciones: en PlantUML el identificador de un estado es **global**, así que dos estados que se llamen igual en fases distintas —cosa que el XML permite (§1.5)— se fundirían en un único nodo. El dibujo renderizado es el mismo, porque la etiqueta sigue siendo el nombre corto.
- Estado inicial `[*] --> <FASE>_<INICIAL>`; transición `A --> B : EVENTO`; guardas para las condicionales (`RESOLVER[tipoResolucion=ACEPTAR]`).
- **MUST NOT** marcar los estados terminales con `--> [*]`: se anotan con `<alias> : closed`, porque en estos diagramas `[*]` como destino significa borrado físico (`DELETE`).

```plantuml
state RECEPCION {
    state "ENTRADA_DATOS" as RECEPCION_ENTRADA_DATOS
    state "PENDIENTE_PRESENTACION" as RECEPCION_PENDIENTE_PRESENTACION
}
state TRAMITACION {
    state "PENDIENTE_RESOLUCION" as TRAMITACION_PENDIENTE_RESOLUCION
    state "ACEPTADO" as TRAMITACION_ACEPTADO
}
[*] --> RECEPCION_ENTRADA_DATOS
RECEPCION_PENDIENTE_PRESENTACION --> TRAMITACION_PENDIENTE_RESOLUCION : PRESENTAR
TRAMITACION_PENDIENTE_RESOLUCION --> TRAMITACION_ACEPTADO : RESOLVER[tipoResolucion=ACEPTAR]
TRAMITACION_ACEPTADO : closed
```

- ❌ INCORRECTO: `state ENTRADA_DATOS` y `[*] --> ENTRADA_DATOS` (sin alias: el nombre es un identificador global de PlantUML y colisiona con el mismo estado de otra fase)
- ❌ INCORRECTO: `TRAMITACION_ACEPTADO --> [*]` (un terminal no es un borrado; se anota `: closed`)

---

## 3. Qué genera y comprueba el build

### 3.1 Esqueletos — se generan a mano, NO al compilar

```bash
./gradlew -q CreateFilesTask -Ptipo=src/main/java/com/educaflow/tramites/<tramite>/<vN>
```

Crea, en la raíz de la versión, `domains.xml`, el `views.xml` con el form de plantilla y el `InitialEventManagerImpl.java`; y por **cada fase**, su subcarpeta con `PhaseEventManagerImpl.java`, `StateEventValidatorImpl.kt` y `views.xml`, con **todos los métodos/forms requeridos** ya presentes y vacíos. Imprime una línea `CREADO <ruta>` por cada fichero creado. Es idempotente: se puede relanzar cuantas veces haga falta, nunca pisa lo ya escrito.

- `-Ptipo=` admite la carpeta del tipo o la ruta de su `TipoExpedienteInstance.xml`. Si no casa con ningún tipo, falla con `ERROR: No hay ningún tipo de expediente en: …` en vez de callarse.
- `-Pfase=<FASE>` acota además a una sola fase; con él **no** se generan los ficheros de la raíz de la versión, porque no son de ninguna fase. Si la fase no existe, falla diciendo cuáles hay. **MUST** usarse junto con `-Ptipo`: sin él se aplica a todos los trámites y aborta a medias en el primer tipo que no tenga esa fase. **No** hace falta para **añadir una fase nueva** sin tocar las demás — de eso ya se encarga la idempotencia.
- Sin `-Ptipo` recorre **todos** los trámites.
- El `-q` quita el ruido de Gradle; los mensajes de la herramienta (incluidos los de error) se ven igual.

```bash
# generar solo los esqueletos de la fase RESOLUCION (las demás fases se generan igual de bien sin -Pfase)
./gradlew -q CreateFilesTask -Ptipo=src/main/java/com/educaflow/tramites/<tramite>/<vN> -Pfase=RESOLUCION
```

**CRITICAL**: compilar **NO** genera los esqueletos. `CreateFilesTask` no cuelga de `generateCode`, de forma que el build no escribe en `src/main/java`. Flujo: escribir el `TipoExpedienteInstance.xml` → **lanzar la tarea** → rellenar lo generado → compilar.

Si compilas sin haberla lanzado, el build falla en `RichDomainXmlTask` con `ERROR: No se encontró el fichero en el directorio: …/<vN>/domains.xml`. Todos los ficheros se versionan en git, así que solo puede pasar con un tipo (o una fase) recién creado.

### 3.2 Comprobaciones que hacen fallar el build

1. `TipoExpedienteInstance.xml` parseable, con `<fases>` (no `<states>`), al menos una fase con al menos un estado, nombres de fase válidos y únicos, nombres de estado únicos dentro de su fase, exactamente un `initial` en todo el tipo, nombres de evento `UPPER_SNAKE` y sin repetir dentro de un estado, y cada `profile` no vacío existente en el enum global `Profile`.
   - `GenerateStatesTask` además rechaza los identificadores Java en conflicto: dos fases que produzcan el mismo enum anidado, una fase que pise un nombre reservado de `States`, o dos métodos generados de la misma fase que colisionen.
2. `domains.xml` con `<module>` único y `<entity name="<code>">` (el nombre de la entidad = code derivado).
3. Vistas (detalle en `vistas.md`) — las cuatro primeras reglas se comprueban **solo si el tipo tiene forms de estado**: el preprocesador ignora todo `views.xml` que no lleve ni un `<form state="…">` ni un form plantilla.
   - Cada `views.xml` con `<form state="…">` **MUST** estar en una carpeta que corresponda a una fase declarada.
   - Cada uno **MUST** declarar solo estados **de esa misma fase**.
   - La raíz de la versión **MUST** tener su `views.xml` con un form plantilla `exp-<Code>-Templates`, cuyo `<Code>` **MUST** ser el del propio tipo (es lo que caza un `<Code>` sin actualizar al duplicar una versión).
   - Todos los paneles de sus `<include-panels>` **MUST** existir.
   - **Siempre**, haya o no forms de estado: un mismo fichero **MUST NOT** tener dos forms `exp-<Code>-Templates`.
4. i18n: si apertium no logra una traducción fiable de un texto nuevo, el build falla (se arregla con `__!!` o escribiendo el valenciano a mano).
5. Documentos: los XML de `documentospdf/` se validan contra XSD y sus filas deben sumar múltiplos de 12 (detalle en `documentos.md`).

### 3.3 Comprobaciones que hacen fallar los **tests** (`./gradlew test`, y por tanto `./run.sh`)

`src/test/java/com/educaflow/tiposexpedientes` comprueba que lo que se escribe a mano en un tipo de expediente y en **cada una de sus fases** concuerda con lo que declaran su `TipoExpedienteInstance.xml` y su `domains.xml`: el código, leyendo el bytecode compilado, y las vistas, leyendo el `views.xml` de la fase. El mensaje de fallo dice qué tipo y fase, qué estado o evento, y trae el **código del método o el form que falta listo para pegar**.

Cómo están construidos: el bytecode se lee con el `ClassFileImporter` de ArchUnit (como lector, **no** con su DSL de reglas: estas reglas están cuantificadas sobre un XML externo, y con el DSL una clase que faltara del todo haría que la regla se cumpliese en vacío), y el `TipoExpedienteInstance.xml` con las mismas clases de `EducaFlowBuildTools` que usa el generador de esqueletos (§3.1), de forma que el código del método que el test dice que falta es literalmente el que ese generador habría escrito. Los `domains.xml` y los `views.xml` se leen con JAXP.

Dos reglas **no** leen el bytecode, cada una por su motivo, y se señalan en su bullet:
- `StatesTest` carga las clases con **reflexión** normal (`Class.forName` + el campo `INSTANCE`): lo que compara —`initial`, `closed`, el perfil y los eventos de cada estado— se construye en el `<clinit>`, y ahí ArchUnit no llega.
- `ClasesDeFaseHuerfanasTest` mira el **árbol de fuentes**, para no denunciar restos de una compilación sin `clean`.

- **PhaseEventManager**: **exactamente un** `@WhenEvent trigger<Evento>(<Entidad>, <Entidad>, EventContext)` por evento **de la fase** y **exactamente un** `@OnEnterState onEnter<Estado>(<Entidad>, EventContext)` por estado **de la fase**; ni faltar ni sobrar. Y **ningún** `triggerInitialEvent` en ninguna fase (detalle en `phaseeventmanager.md` §7).
- **InitialEventManager**: **exactamente un** `InitialEventManagerImpl` por tipo de expediente, en la raíz de la versión, que implementa `InitialEventManager<Entidad>` y declara `void triggerInitialEvent(<Entidad>, EventContext)` (detalle en `phaseeventmanager.md` §7).
- **Entidad del tipo** (`ModeloDelTipoTest`): el `InitialEventManagerImpl` y el `PhaseEventManagerImpl` de **cada fase** llevan en su parámetro de tipo la **misma** entidad, y es la **primera** `<entity>` del `domains.xml` de la versión (`modelo.md` §1). Son varias declaraciones de un mismo hecho, así que pueden divergir sin que el compilador diga nada; y la del `InitialEventManagerImpl` es además la que `ExpedienteLocator.getModelClass` lee en runtime para saber qué instanciar al crear un expediente (§1.6), de modo que la divergencia no falla al compilar sino al tramitar.
- **Validator**: **exactamente un** `@BeanValidationRulesForStateAndEvent getForState<Estado>InEvent<Evento>()` por cada **pareja** (estado, evento) **de la fase**, **salvo las de `DELETE`**, que no se exigen porque el runtime nunca las invoca; y ninguno cuya pareja no sea de la fase (detalle en `validator.md` §5).
- **`States`** (`StatesTest`): la clase generada de cada tipo concuerda con su XML — fases, estados de cada fase, y por cada estado su nombre, perfil, eventos, `initial` y `closed`, más el estado inicial y `CODE`/`NAME`. Comprueba también el **orden de declaración** de fases, estados y eventos, no solo el conjunto. Es la regla que se lee con reflexión y no con el `ClassFileImporter`.
- **API base reservada** (`ApiBaseReservadaTest`): ningún nombre de método compuesto a partir de un estado o un evento pisa un método público de `PhaseEventManager` o `StateEventValidator` (un estado llamado `STATE` sobrescribiría el dispatcher `onEnterState` en silencio).
- **Referencias a `States`** (`ReferenciasAStatesTest`): ninguna clase de un tipo de expediente referencia la clase `States` de **otro** tipo. Como todos los tipos tienen estados que se llaman igual, el `import` que se queda apuntando a la versión vieja al duplicar una carpeta compila sin error (`versionado.md`).
- **Clases de fase huérfanas** (`ClasesDeFaseHuerfanasTest`): no hay ningún `PhaseEventManagerImpl` ni `StateEventValidatorImpl` en una carpeta que no sea la de una fase declarada — ni bajo un tipo de expediente, ni suelto bajo `tramites/` sin pertenecer a ningún tipo. Es la dirección contraria a **E0** (existe el `PhaseEventManagerImpl` de cada fase declarada) y **V0** (existe su `StateEventValidatorImpl`): aquellas van de la fase al fichero y esta del fichero a la fase, así que caza la carpeta que se queda atrás al renombrar o quitar una fase, y que sigue compilando aunque `ExpedienteLocator` ya no llegue a ella. Esta regla mira el **árbol de fuentes**, no el bytecode, para no denunciar restos de una compilación sin `clean`.
- **Vistas por estado** (`VistasPorEstadoTest`): cada estado tiene en el `views.xml` de su fase su `<form state="…">` **genérico** —el de reserva al que cae el runtime cuando el perfil actuante no tiene el suyo— y, si tiene `profile` y al menos un evento, también el `<form state="…" profile="…">` de su perfil, sin el cual su dueño cae en la vista de solo lectura y el expediente se queda atascado sin ningún error. Y no hay dos forms de la misma fase con el mismo `(state, profile)`, que producirían el mismo nombre de vista.
- **Botones del footer** (`BotonesDelFooterTest`): el `name` de cada botón del `<footer>` es un evento declarado en ese estado o uno de los comunes; cada evento declarado tiene botón en alguno de los forms de su estado (si no, es un evento que existe en el código y no se puede disparar); y el `onClick` de todos incluye `subsysExpedientes-event-action`.
- **Diagrama de estados** (`DiagramaDeEstadosTest`): cada tipo de expediente tiene su `estados.puml` en la raíz de su carpeta de versión, junto al `TipoExpedienteInstance.xml` (D1, §2.4), y los estados que ese diagrama nombra son **exactamente** los del XML: ninguno dibujado que el XML no declare (D2) y ninguno declarado que el diagrama no dibuje (D3). D2 cuenta tanto las declaraciones `state "<ESTADO>" as <FASE>_<ESTADO>` como los identificadores usados en transiciones y anotaciones, porque en PlantUML un identificador usado sin declarar no da error: crea un nodo nuevo en silencio, que es como un typo en el destino de una transición acaba dibujando un estado inexistente. Las **transiciones** no se comprueban: su destino no está en el XML, así que no hay con qué contrastarlas. El mensaje de D1 trae el esqueleto entero derivado del XML (fases, estados, inicial, cerrados y una línea comentada por transición pendiente de destino).

### 3.4 Lo que NO comprueba nada (falla en runtime)

- Lo que el `triggerInitialEvent` del `InitialEventManagerImpl` deja **sin** rellenar. `Tramitador` no exige ningún campo, así que el expediente se crea igual y el fallo llega después y en otro sitio: `dniFirmaDocumentoEntrada` revienta al **firmar** (en `FirmaController.firmarDocumentoEntrada`) y `personaSolicitante` con un NPE al crear el **registro de entrada** (ver `phaseeventmanager.md` §2.1).

### 3.5 Qué genera en BD el data-init del tipo

`generateDataInitTiposExpedientes` genera por cada tipo un data-init (en `build/`, nunca en `src`) que hace bind del `TipoExpediente` por `code` con `create`+`update` — por eso la fila **se refresca en cada arranque**. Persiste:

- `code`, `name`, el `tramite` (resuelto buscando su `code` en BD — de ahí que el trámite cargue antes, con `priority` mayor) y el `basePackageName`, que es lo que usa `ExpedienteLocator` para encontrar las clases de cada fase y la clase `States` (§1.6).
- **Nada más**: ni fases, ni estados, ni eventos, ni flags. La máquina de estados no toca la base de datos; vive entera en la clase `States` (§2.3).

(El data-init del **trámite** — qué persiste el `TramiteInstance.xml` — está en `/k-tramite` §4.)

---

## 4. Checklist: crear un tipo de expediente nuevo

1. **Trámite**: asegúrate de que existe `tramites/<tramite>/TramiteInstance.xml` (`/k-tramite`); si es la primera versión, sin `<defaultTipoExpediente>` aún.
2. **Carpeta**: crea `tramites/<tramite>/v1/` con solo `estados.puml` (dibuja la máquina, con las fases como estados compuestos) y `TipoExpedienteInstance.xml` con sus `<fases>` (§2).
3. **Genera los esqueletos**: `./gradlew -q CreateFilesTask -Ptipo=src/main/java/com/educaflow/tramites/<tramite>/v1` (§3.1). Crea la raíz y una subcarpeta por fase. Compilar **no** los genera.
4. **Modelo**: añade los campos a `domains.xml` → `modelo.md`.
5. **Documentos**: crea `documentospdf/` con los XML de definición → `documentos.md`.
6. **Evento inicial y PhaseEventManager de cada fase**: rellena el `triggerInitialEvent` del `InitialEventManagerImpl` de la raíz de la versión (uno por tipo) y, en cada fase, sus `trigger<Evento>` y `onEnter<Estado>` → `phaseeventmanager.md`.
7. **Validator de cada fase**: rellena las `rules { }` de cada pareja estado+evento de la fase → `validator.md`.
8. **Vistas**: monta los paneles del form plantilla en el `views.xml` de la raíz y compón cada `<form state=...>` en el `views.xml` de su fase → `vistas.md`.
9. **Permisos**: verifica que el perfil de cada estado (`CREADOR`, `RESPONSABLE`…) está asignado a alguien (`/k-tramite` §6).
10. **Activa** la versión en el `TramiteInstance.xml` (`<defaultTipoExpediente>v1</defaultTipoExpediente>`), compila y arranca.
11. **Prueba en runtime** navegando por **todos** los estados con usuarios de los perfiles adecuados (menú Expedientes → Trámites): los tests comprueban que cada estado tiene sus forms y sus botones (§3.3), pero no que lo que pintan tenga sentido.

Para **añadir una fase** a un tipo que ya existe:

1. Añade su `<fase>` al `TipoExpedienteInstance.xml`, moviendo a ella los `<state>` que le tocan.
2. Lanza `CreateFilesTask` con `-Ptipo=` (`-Pfase=<FASE>` solo acota la salida: la idempotencia ya protege a las fases que ya están).
3. Mueve a la nueva carpeta los métodos y forms de esos estados.

**CRITICAL**: mover un estado de fase le cambia el `codePhase`, así que los expedientes ya guardados en ese estado quedan huérfanos.

Para crear una **versión nueva de un tipo existente** → `versionado.md`.

---

## Quick Guidelines

- Todo se deriva de la carpeta `tramites/<tramite>/<vN>/`: code = code del trámite + `VN`, entidad = code, paquete base = ruta. No declares lo que el default ya resuelve.
- Los estados se agrupan en **fases**, obligatorias, una subcarpeta por fase con su `PhaseEventManagerImpl`, su `StateEventValidatorImpl` y su `views.xml`. La fase agrupa ficheros y **no es una entidad del dominio** (no se persiste como dato maestro), pero **sí existe en ejecución**: viaja en `codePhase` y da el paquete desde el que `ExpedienteLocator` resuelve las clases.
- `TipoExpedienteInstance.xml` mínimo = solo `<fases>` con sus `<state>` dentro. `events` obligatorio aunque vacío; exactamente un `initial` en todo el tipo; nombres `UPPER_SNAKE`; el `name` de un estado solo tiene que ser único dentro de su fase.
- Un estado se identifica por la **pareja** (fase, estado), que se persiste en `codePhase` + `codeState`. **MUST NOT** concatenarlos: no hay nombre compuesto.
- El **evento inicial es del tipo**, no de una fase: lo atiende un único `InitialEventManagerImpl` en la raíz de la versión, que implementa `InitialEventManager<Entidad>`. Un `PhaseEventManagerImpl` **MUST NOT** declarar un `triggerInitialEvent`.
- `ExpedienteLocator` resuelve las clases de la fase por convención: `basePackageName` (lo único que hay en BD) + el `codePhase` del expediente; y las del tipo entero directamente sobre el `basePackageName` (`.States`, `.InitialEventManagerImpl`).
- Flujo: XML de fases y estados → `./gradlew -q CreateFilesTask -Ptipo=<carpeta del tipo>` (compilar **no** genera los esqueletos; `-Pfase=` solo acota la salida a una fase) → rellenar modelo, PhaseEventManager, validator, vistas y documentos → activar versión → probar todos los estados en runtime.
- La máquina de estados en runtime vive en la clase generada `States` (una por tipo, todas sus fases), no en BD. Un estado se referencia como `States.<Fase>.<ESTADO>`, con la fase en **UpperCamelCase** (`States.Recepcion.ENTRADA_DATOS`); `States.RECEPCION` es otra cosa: el alias de la fase, tipado `Phase`, y no lleva estados. Los eventos son strings.
- El build comprueba modelo, vistas-plantilla, correspondencia carpeta↔fase, i18n, documentos y los identificadores que genera `States`; los **tests** comprueban PhaseEventManager y validator fase a fase (con el código del método que falta listo para pegar), el `InitialEventManagerImpl` por tipo, que `States` concuerda con el XML y que ningún tipo usa el `States` de otro, y que cada estado tiene sus forms y cada evento su botón.
- `EXIT` y `DELETE` son eventos comunes gratis; `BACK` no — se declara e implementa como uno más.
- **MUST NOT** crear `i18n_*.csv` a mano; **MUST NOT** editar el `<extra-code-model>` ni el `estados.png` (los regenera el build).
