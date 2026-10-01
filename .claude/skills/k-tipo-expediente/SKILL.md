---
name: k-tipo-expediente
description: Cómo crear o modificar un tipo de expediente (una versión `v1`/`v2`… de un trámite) en `tramites/<tramite>/<vN>/`: el fichero maestro `TipoExpedienteInstance.xml` con sus **fases** y la máquina de estados, el modelo (`domains.xml`), y por cada fase su `PhaseEventManager`, su `StateEventValidator` y sus vistas preprocesadas; las funciones propias del tipo en `<Code>Util` (predicados para `Lambda`/`ifLambda`, guardas y mutaciones); los documentos PDF (`documentospdf/`, formato XML de definición) y las recetas de `recetas/`: presentar un documento que acaba en el registro de entrada (de la entrada de datos al resguardo), firmar documentos (el usuario al presentar, el certificado del centro, poner a firmar a otro) y duplicar un tipo para crear la versión siguiente. Cárgalo siempre que crees o modifiques cualquier fichero bajo una carpeta de versión de un trámite.
---

# k-tipo-expediente

Un tipo de expediente es la implementación **versionada** de un trámite: la carpeta `tramites/<tramite>/<vN>/` con su máquina de estados, entidad, vistas, validaciones y documentos. El trámite en sí (el `TramiteInstance.xml` padre) es de `k-tramite`.

Este skill contiene **solo lo que necesitas para escribir un tipo de expediente**: qué ficheros hay, qué va en cada uno y qué hace el motor por ti. Cómo está implementado el motor (`subsystem/tramitador`) no hace falta para crear ni modificar un tipo; **MUST NOT** tocar el motor desde una tarea de un trámite (`subsystem/tramitador/CLAUDE.md`).

**Convención de los ejemplos**: todos usan un trámite **inventado** —code `MiTramite`, carpeta `tramites/mi_tramite/`, versión `v1/`, entidad `MiTramiteV1`— con las fases `RECEPCION` y `TRAMITACION`. Sustituye `MiTramite` por el code de tu trámite y `mi_tramite/v1` por su ruta real, que **no** tiene por qué colgar directamente del trámite (`recetas/versionado.md`).

## Ficheros de este skill

| Fichero | Contenido |
|---------|-----------|
| `modelo.md` | El `domains.xml` del tipo (en la raíz de la versión, uno para todas las fases): entidad `extends="Expediente"`, campos heredados, personas, enums versionados, campos `MetaFile` para los PDF |
| `phaseeventmanager.md` | El `InitialEventManagerImpl` (alta) y el `PhaseEventManagerImpl` de cada fase: métodos `trigger*`/`onEnter*`, API de `EventContext` y el **catálogo de acciones** (generar PDF, registros de entrada/salida, firmas, correos…) |
| `validator.md` | El `StateEventValidatorImpl` de cada fase, en Kotlin: DSL de reglas por estado+evento, su doble función de whitelist de campos y el catálogo de reglas |
| `vistas.md` | Las vistas en formato **preprocesado** (NO sigue `k-vistas`): el form plantilla en la raíz, los `<form state=...>` de cada fase, `include-panels`, `footer`, paneles comunes, visores de PDF |
| `documentos.md` | Los documentos de `documentospdf/`: los **dos tipos** de documento, lo común a ambos (fragmentos, valenciano, expresiones Groovy, `visible`/`siOculto`, el hueco de la firma `campoFirma`, idioma) y el formato **FORMULARIO** (rejilla de 12 columnas) |
| `documentotexto.md` | El formato **TEXTO** (raíz `<documentoTexto>`): documento en prosa en un solo idioma |
| `perfiles.md` | `CREADOR`, `TRAMITADOR` y `presentadoEnPapel`: cómo se presenta un expediente y qué implica para el estado inicial y las vistas |
| `recetas/presentacion.md` | Receta: el usuario presenta un documento que acaba en el **registro de entrada**, de la entrada de datos al resguardo |
| `recetas/firma.md` | Receta: las tres formas de firmar (el usuario al presentar, el certificado del centro, poner a firmar a otro) |
| `recetas/versionado.md` | Receta: duplicar un tipo para crear la versión siguiente, y cuándo basta con modificar la actual |

---

## 1. Conceptos clave

### 1.1 Todo se deriva de la carpeta de versión

**CRITICAL**: la carpeta `tramites/<tramite>/<vN>/` determina la identidad completa del tipo. Con el `TramiteInstance.xml` padre (`code=MiTramite`, `name=Mi trámite`) y la carpeta `v1`:

| Derivado | Regla | Ejemplo |
|---|---|---|
| `code` del tipo | code del trámite + versión en mayúsculas | `MiTramiteV1` |
| `name` del tipo | name del trámite + " " + versión | `Mi trámite V1` |
| Entidad JPA | = `code` | `MiTramiteV1` |
| Paquete base | la ruta tras `/java/` | `com.educaflow.tramites.mi_tramite.v1` |
| Paquete de una fase | el `name` de la fase en **minúsculas**, bajo el paquete base | `RECEPCION` → `<paquete base>.recepcion` |
| Clases de la fase | nombres **fijos**: `PhaseEventManagerImpl`, `StateEventValidatorImpl` | — |
| Clases del tipo | nombres **fijos** en el paquete base: `InitialEventManagerImpl`, `States` (generada) | — |

- `code` y `name` son defaults que se pueden sobrescribir con tags opcionales del `TipoExpedienteInstance.xml` (§2); **MUST NOT** hacerlo sin motivo.
- El motor encuentra las clases **por convención de nombre** (paquete + nombre fijo) y la entidad por el parámetro de tipo de `InitialEventManagerImpl`: **MUST NOT** renombrar esas clases ni sus carpetas.
- Mover la carpeta de un tipo a otra ruta se corrige solo en el siguiente arranque (el data-init reescribe el paquete base).

### 1.2 Fases

Una fase es una **agrupación de estados y de sus ficheros**, para que no haya `PhaseEventManagerImpl`/`StateEventValidatorImpl`/`views.xml` gigantes y para poder copiar una fase entera a otro tipo.

- Son **obligatorias**: todo tipo tiene al menos una y todo estado pertenece a exactamente una.
- Cada fase tiene su subcarpeta `<vN>/<fase en minúsculas>/`.
- Las transiciones **pueden cruzar fases** sin ningún tratamiento especial.
- El `title` de la fase **es texto de interfaz**: lo ve el usuario en la cabecera de los formularios, en los listados y en el historial. Sin él se muestra el `name` humanizado (`SUBSANACION_DOCUMENTOS` → `Subsanacion documentos`).
- Reparto habitual (no obligatorio): `RECEPCION` = los estados del interesado (perfil `CREADOR`); `TRAMITACION` = el resto.
- **CRITICAL**: mover un estado de fase le cambia el `codePhase`, así que los expedientes ya guardados en ese estado quedan huérfanos.

### 1.3 Contenido de la carpeta del tipo

En la **raíz de la versión** va lo que es de todo el tipo:

| Fichero | Quién lo escribe | Detalle en |
|---|---|---|
| `TipoExpedienteInstance.xml` | tú | §2 |
| `estados.puml` / `estados.png` | tú / el build | §2.3 |
| `domains.xml` | tú (esqueleto generado, §3.1) | `modelo.md` |
| `views.xml` (solo el form plantilla `exp-<Code>-Templates` y forms/grids auxiliares) | tú (esqueleto generado) | `vistas.md` |
| `InitialEventManagerImpl.java` | tú (esqueleto generado) | `phaseeventmanager.md` §2.1 |
| `<Code>Util.java` (`MiTramiteV1Util.java`) | tú, solo si el tipo necesita funciones propias; sin esqueleto | §1.6 |
| `documentospdf/` | tú | `documentos.md` |
| `i18n_es.csv` / `i18n_ca.csv` | el build — **MUST NOT** crearlos a mano | `CLAUDE.md` (i18n) |

En **cada subcarpeta de fase** (`<vN>/<fase en minúsculas>/`):

| Fichero | Detalle en |
|---|---|
| `PhaseEventManagerImpl.java` | `phaseeventmanager.md` |
| `StateEventValidatorImpl.kt` | `validator.md` |
| `views.xml` (los `<form state="...">` de sus estados) | `vistas.md` |

- Los tres son obligatorios en la práctica: sin ellos fallan los tests (§3.3) o el estado revienta al abrirse.
- **MUST NOT** dejar un `views.xml` sin ningún elemento hijo (todo comentado incluido): aborta la carga de vistas, menús y data-init al arrancar. Si una fase no tiene forms, se borra el fichero.

### 1.4 Qué le toca a cada fase

- **Estados**: los suyos. El `onEnter<Estado>` de un estado vive en el `PhaseEventManagerImpl` de su fase.
- **Eventos**: la unión, sin repetir, de los eventos de sus estados. Un mismo evento presente en dos fases lleva su propio `trigger<Evento>` en cada una.
- **Parejas (estado, evento)**: las de sus estados, en su `StateEventValidatorImpl`.
- **El evento inicial NO es de ninguna fase**: lo atiende el único `InitialEventManagerImpl` de la raíz de la versión. Un `PhaseEventManagerImpl` **MUST NOT** declarar un `triggerInitialEvent`.

### 1.5 La identidad de un estado: la pareja (fase, estado)

Un estado se identifica por **dos** códigos, que el expediente guarda en dos columnas (`codePhase = "RECEPCION"`, `codeState = "ENTRADA_DATOS"`). El nombre de un estado solo es único dentro de su fase.

- **MUST NOT** concatenarlos ni inventar un nombre compuesto (`F_<fase>_S_<estado>` no existe).
- En código, un estado se nombra **siempre** por su constante de la clase generada `States` (§2.2): `States.Recepcion.ENTRADA_DATOS`, nunca por sus strings.
- Los nombres de método (`onEnterEntradaDatos`, `getForStateEntradaDatosInEventPresentar`) y el `state="..."` de los forms llevan **solo** el estado: la fase la da la carpeta.
- Los textos visibles son el `title` de la fase y el del estado (o su `name` humanizado).

### 1.6 Qué hace el motor por ti

No hay que programar nada de esto; sí hay que saber que ocurre para escribir bien el tipo.

**Al crear un expediente**:

1. Comprueba que el usuario puede crear expedientes del trámite en ese centro (`k-tramite` §6).
2. Instancia la entidad y rellena: tipo, centro, `usuarioRegistrador`, `presentadoEnPapel`, `presentadoEnRepresentacion`, `personaSolicitante`, `personaInteresada` (`modelo.md` §2.1), `name` y `numeroExpediente`.
3. Llama al `triggerInitialEvent` del `InitialEventManagerImpl`, que **MUST** fijar el estado inicial.
4. Añade la primera línea del historial, llama al `onEnter<Estado>` del estado inicial y guarda.

**Al disparar un evento** (un botón del footer), en este orden:

1. Si el estado actual declara `profile`, exige que el usuario tenga ese perfil sobre el expediente. El administrador tiene todos los perfiles: si un evento no debe dispararlo ni él, el `trigger*` lo comprueba con una guarda de `<Code>Util` (§1.7).
2. Exige que el evento esté declarado en el estado actual.
3. Copia del formulario **solo los campos que tienen reglas** en el validador de esa pareja (estado, evento): es la whitelist (`validator.md` §1). Después restaura la identidad de las personas y `presentadoEnPapel`/`presentadoEnRepresentacion`, que el cliente no puede cambiar.
4. Valida. Si falla, muestra los mensajes en el footer y no guarda nada del expediente.
5. Llama a `trigger<Evento>(expediente, original, eventContext)`. `original` es el expediente **antes** de copiar los datos del formulario. El `trigger*` decide el destino con `eventContext.updateState(...)`.
6. Añade una línea al historial (con el registro de entrada/salida que se haya creado en el evento), llama al `onEnter<Estado>` del estado destino (en la clase de **su** fase) y guarda.
7. Muestra la vista del nuevo estado (`vistas.md` §2).

Consecuencias que afectan a lo que escribes:

- **CRITICAL**: una `BusinessException` lanzada en un `trigger*` descarta los cambios del expediente, pero **no** deshace lo que ya se haya creado fuera de él (registro de entrada o salida, `MetaFile`, `TareaFirma`, correo). Las comprobaciones que lanzan `BusinessException` **MUST** ir al principio del `trigger*`, antes de crear nada.
- Un `trigger*` que no llama a `updateState` deja el expediente en el mismo estado, pero el historial gana una línea y el `onEnter` de ese estado se vuelve a ejecutar.
- Eventos comunes, que existen sin declararlos en `events`: `EXIT` (cierra; no llega al `PhaseEventManagerImpl`). `DELETE` sí hay que declararlo en `events` para ofrecerlo, pero no valida, no copia campos ni deja historial: solo llama a `triggerDelete` y borra el expediente.
- `BACK` **NO** es común: si un estado necesita volver atrás, se declara e implementa como cualquier evento.

### 1.7 `<Code>Util` — las funciones propias del tipo

Cuando el tipo necesita una función que no es un `trigger*`/`onEnter*`/`triggerInitialEvent` ni un `rules { }`, es una **función estática** de una única clase `<Code>Util.java` en la raíz de la versión (`MiTramiteV1Util`), `final` y con constructor privado.

Cuándo una función va a `<Code>Util`:

- Va si la necesita el validador (el DSL solo admite referencias a función) **y ninguna regla del catálogo cubre la comprobación** (`validator.md` §3), o si el mismo código se llama desde **más de un sitio** (varios eventos, varias fases).
- **MUST NOT** ir si se usa en un solo sitio y es una línea o un bloque corto: se queda inline en el `trigger*`.

Tres familias de función:

- **Predicado** para el validador: recibe el expediente y devuelve `boolean` (`true` = válido). No lanza `BusinessException` ni conoce mensajes: el mensaje lo pone la regla `Lambda` que la usa.
- **Guarda de negocio** para un `trigger*`: recibe el expediente y el mensaje, es `void` y lanza `BusinessException(I18n.get(mensaje))` si no se cumple.
- **Mutación** compartida por varios eventos o fases: recibe el expediente, `void`.

Reglas:

- **MUST NOT** crear una `ValidationRule` propia del tipo: función `boolean` de `<Code>Util` + `+Lambda(...)`/`+ifLambda(...)`. Solo si la comparten **varios** tipos es una regla, y va a `tramites/util/<propósito>/` (`tramites/util/CLAUDE.md`) o al catálogo de `base/infrastructure/validation/rules`.
- **MUST NOT** repartir las funciones en varias clases por tema: una sola `<Code>Util` por tipo, con bloques comentados si crece.
- **Validación vs assert**: una función solo devuelve `false` o lanza `BusinessException` por algo que el usuario de la pantalla puede corregir. Lo que nunca debería darse (falta un dato que fija el servidor) es `IllegalStateException`, sin mensaje i18n.

- ✅ CORRECTO: `public static boolean sinOtraSolicitudEnCurso(MiTramiteV1 expediente)` en `MiTramiteV1Util` + `+Lambda(util::sinOtraSolicitudEnCurso, "Ya tiene una solicitud en curso")` en el validador.
- ✅ CORRECTO: `MiTramiteV1Util.exigeSerElCreador(expediente, "Solo puede modificar sus propias solicitudes")` como primera línea de un `trigger*`.
- ❌ INCORRECTO: `class SinOtraSolicitudEnCurso : ValidationRule` en `mi_tramite/v1/` (regla de un solo tipo: es una función de `MiTramiteV1Util` + `Lambda`).
- ❌ INCORRECTO: `ControlDeAcceso.java` + `DevolucionDelDirector.java` en la carpeta de la versión (varias clases de utilidad: todo va en `MiTramiteV1Util`).
- ❌ INCORRECTO: `return false` cuando `cursoAcademico` es nulo (lo fija el servidor al crear el expediente: es un assert → `IllegalStateException`).

---

## 2. `TipoExpedienteInstance.xml` — el fichero maestro

Fichero mínimo real (todo lo demás se deriva, §1.1):

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<TipoExpediente>
    <fases>
        <fase name="RECEPCION" title="Recepción">
            <state name="ENTRADA_DATOS"          events="DELETE,GUARDAR_DATOS" profile="CREADOR"     title="Entrada de datos"          />
            <state name="PENDIENTE_PRESENTACION" events="BACK,PRESENTAR"       profile="CREADOR"     title="Pendiente de presentación" />
        </fase>
        <fase name="TRAMITACION" title="Tramitación">
            <state name="PENDIENTE_RESOLUCION"   events="RESOLVER"             profile="TRAMITADOR"  title="Pendiente de resolución"   />
            <state name="ACEPTADO"               events=""                     profile="TRAMITADOR"  title="Aceptado"   closed="true"  />
            <state name="RECHAZADO"              events=""                     profile="TRAMITADOR"  title="Rechazado"  closed="true"  />
        </fase>
    </fases>
</TipoExpediente>
```

- Tags opcionales antes de `<fases>`: `name`, `code` y `tramite` (sobrescriben los defaults de §1.1).
- **MUST NOT** usar `ambitoCreador`/`ambitoResponsable`/`ambitoAuditor`: el XML los acepta pero no hacen nada.
- **MUST NOT** usar un `<states>` suelto en la raíz: es el formato anterior a las fases y el build aborta.
- JAXB **ignora en silencio los tags y atributos desconocidos**: un typo en un tag opcional no da error, simplemente aplica el default.

Tag opcional `<acl>` (hermano de `<fases>`): perfiles que da **este** tipo de expediente en todos los centros. Mismo formato que el `<acl>` de `TramiteInstance.xml` (`k-tramite` §3).

```xml
    <acl>
        <ace perfil="TRAMITADOR">
            <usuario cargo="JEFE_ESTUDIOS"/>
        </ace>
    </acl>
```

- **SHOULD** usarse el `<acl>` del trámite cuando el perfil no dependa de la versión: el del tipo hay que repetirlo en cada versión nueva.

### 2.1 Reglas de `<fase>` y `<state>`

`<fase>`:

- `name` en `UPPER_SNAKE`, único dentro del tipo; da nombre a su carpeta (en minúsculas) y a su enum en `States` (en UpperCamelCase).
- `title` opcional, pero lo ve el usuario (§1.2): ponlo siempre.

`<state>`:

- `events` **MUST** escribirse siempre, aunque esté vacío (`events=""`): omitirlo equivale en silencio a vacío.
- `profile` es opcional (estado sin dueño) y **MUST** ser una constante del enum `Profile` (`subsystem/expedientes/domains/Profile.xml`). Solo `CREADOR` y `TRAMITADOR` tienen significado especial (`perfiles.md`); los demás solo dan el turno en el estado y eligen vista.
- **MUST NOT** marcar ningún estado como inicial: no existe el atributo `initial` (JAXB lo ignora en silencio). El estado inicial lo fija el `InitialEventManagerImpl`.
- `closed="true"` marca los estados terminales: el expediente queda cerrado, no borrado.
- El `name` solo tiene que ser único **dentro de su fase**; si se repite en otra fase, dale `title` distinto o en los listados se verán iguales.
- Nombres de estados y eventos: `UPPER_SNAKE` e **identificadores Java válidos** (van a constantes y a nombres de método: `GUARDAR_DATOS` → `triggerGuardarDatos`). Un evento **MUST NOT** repetirse dentro del mismo estado.
- **MUST NOT** llamar a un estado o evento de forma que su método pise uno de la clase base (`STATE` → `onEnterState`, `INITIAL_EVENT` → `triggerInitialEvent`): lo caza el test A1.

- ✅ CORRECTO: `<fase name="SUBSANACION_DOCUMENTOS" title="Subsanación">` → carpeta `subsanacion_documentos/`
- ❌ INCORRECTO: `<fase name="Recepcion">` (no es UPPER_SNAKE)
- ❌ INCORRECTO: `<fase name="STATES">` (su enum anidado se llamaría `States`, el nombre de la propia clase generada)
- ✅ CORRECTO: `<state name="PENDIENTE_FIRMA" events="" profile="TRAMITADOR" title="Pendiente de firma"/>`
- ❌ INCORRECTO: `<state name="PENDIENTE_FIRMA" profile="TRAMITADOR"/>` (falta `events`, aunque sea vacío)
- ❌ INCORRECTO: `<state name="PendienteFirma" .../>` (no es UPPER_SNAKE: produce métodos inesperados como `triggerPendientefirma`)

### 2.2 Del XML sale la clase `States`

El build genera de cada `TipoExpedienteInstance.xml` la clase `<paquete base>.States` (en `build/`). **MUST NOT** editarla ni versionarla.

- Un **enum por fase**, con el nombre de la fase en UpperCamelCase: `States.Recepcion.ENTRADA_DATOS`.
- `States.RECEPCION` es otra cosa: el alias de la fase (tipo `Phase`), sin estados.
- `States.INSTANCE.getState(codePhase, codeState)` resuelve el estado de un expediente (`phaseeventmanager.md` §5).
- La máquina de estados vive **solo** ahí: no se guarda en BD. Cambiar estados o eventos en el XML se propaga solo; los métodos que faltan o sobran te los dicen los tests (§3.3).
- Los eventos son **strings**, no un enum.

### 2.3 `estados.puml`

Dibuja la máquina antes de escribir el XML. **MUST** existir en la raíz de la versión (test D1): es el único sitio donde se ve la máquina entera, porque el destino de cada evento está en el código, no en el XML. El build renderiza el `.png`.

- Los estados de cada fase van **anidados en un estado compuesto** que representa la fase.
- **MUST** declarar cada estado con el alias `<FASE>_<ESTADO>` y usar el alias en las transiciones: en PlantUML el identificador es global y dos estados con el mismo nombre en fases distintas se fundirían.
- Inicial: `[*] --> <FASE>_<INICIAL>`, uno por cada estado en que pueda nacer el expediente, con guarda si depende de algo (`: [presentadoEnPapel=true]`).
- Transición: `A --> B : EVENTO`, con guarda si es condicional (`RESOLVER[tipoResolucion=ACEPTAR]`).
- **MUST NOT** marcar los terminales con `--> [*]`: se anotan `<alias> : closed`. `[*]` como destino significa borrado (`DELETE`).
- Los estados que nombra el diagrama **MUST** ser exactamente los del XML (tests D2/D3).

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
RECEPCION_ENTRADA_DATOS -> [*] : DELETE
RECEPCION_PENDIENTE_PRESENTACION --> TRAMITACION_PENDIENTE_RESOLUCION : PRESENTAR
TRAMITACION_PENDIENTE_RESOLUCION --> TRAMITACION_ACEPTADO : RESOLVER[tipoResolucion=ACEPTAR]
TRAMITACION_ACEPTADO : closed
```

- ❌ INCORRECTO: `state ENTRADA_DATOS` y `[*] --> ENTRADA_DATOS` (sin alias: colisiona con el mismo estado de otra fase)
- ❌ INCORRECTO: `TRAMITACION_ACEPTADO --> [*]` (un terminal no es un borrado; se anota `: closed`)

---

## 3. Generar, compilar y comprobar

### 3.1 Esqueletos — se generan a mano, NO al compilar

```bash
./gradlew -q CreateFilesTask -Ptipo=src/main/java/com/educaflow/tramites/<tramite>/<vN>
```

- Crea lo que falte: en la raíz `domains.xml`, `views.xml` e `InitialEventManagerImpl.java`; en cada fase `PhaseEventManagerImpl.java`, `StateEventValidatorImpl.kt` y `views.xml`, con todos los métodos y forms requeridos vacíos. Imprime `CREADO <ruta>` por fichero.
- Es idempotente: nunca pisa lo ya escrito. Para **añadir una fase** basta con relanzarla.
- `-Pfase=<FASE>` acota a una fase (sin los ficheros de la raíz) y **MUST** ir junto con `-Ptipo`.
- **CRITICAL**: compilar **no** genera los esqueletos. Si compilas sin haberla lanzado, el build falla con `No se encontró el fichero en el directorio: …/<vN>/domains.xml`.
- Los esqueletos generan dos forms idénticos por estado; tienes que rellenarlos tú (`vistas.md` §2).

### 3.2 Lo que hace fallar el build

- `TipoExpedienteInstance.xml` mal formado o que incumple §2.1 (nombres, perfiles inexistentes, `<states>` antiguo…).
- `domains.xml` sin `<entity name="<code>">` (`modelo.md` §1).
- Vistas (`vistas.md` §10): un panel de `<include-panels>` que no existe, un form de estado en una carpeta que no es de una fase o con un estado de otra fase, sin form plantilla `exp-<Code>-Templates` (o con el `<Code>` de otro tipo), un `profile` que no usa ningún estado del tipo.
- i18n: un texto nuevo que apertium no traduce de forma fiable (se arregla con `__!!` o escribiendo el valenciano a mano).
- Documentos de `documentospdf/` que no validan contra su XSD (`documentos.md`).

### 3.3 Lo que hace fallar los tests (`./gradlew test`, y por tanto `./run.sh`)

Los tests de `src/test/java/com/educaflow/tiposexpedientes` comprueban, **fase a fase**, que lo escrito a mano concuerda con el `TipoExpedienteInstance.xml` y el `domains.xml`. **El mensaje de fallo dice qué tipo, fase, estado o evento falla y trae el código del método o del form que falta, listo para pegar.**

| Regla | Qué exige |
|---|---|
| E0–E5 | Por fase: existe `PhaseEventManagerImpl`, con **exactamente un** `trigger<Evento>` por evento de la fase y **un** `onEnter<Estado>` por estado de la fase; ninguno de más; ningún `triggerInitialEvent` (`phaseeventmanager.md` §7) |
| I1–I2 | Por tipo: existe `InitialEventManagerImpl` en la raíz con su `triggerInitialEvent` |
| V0–V2 | Por fase: existe `StateEventValidatorImpl`, con **un** `getForState<Estado>InEvent<Evento>` por cada pareja de la fase salvo las de `DELETE`; ninguno de más (`validator.md` §5) |
| M1 | El `InitialEventManagerImpl` y todos los `PhaseEventManagerImpl` usan como entidad la **primera** `<entity>` del `domains.xml` |
| M2 | La entidad no declara campos que se llamen como los de `Persona` (`modelo.md` §2.1) |
| S1–S4 | La clase `States` concuerda con el XML |
| A1 | Ningún nombre de estado o evento genera un método que pise uno de la clase base |
| R1 | Ninguna clase usa el `States` de **otro** tipo (típico al duplicar una versión: compila, pero falla en runtime) |
| H1 | No queda un `PhaseEventManagerImpl`/`StateEventValidatorImpl` en una carpeta que ya no es de ninguna fase |
| X1–X3 | Cada estado tiene su form genérico y, si tiene `profile` y eventos, el de su perfil; no hay dos forms con el mismo `(state, profile)` (`vistas.md` §2) |
| Y1–Y3 | Cada botón del footer es un evento del estado o uno común, cada evento tiene botón y todos usan `subsysTramitador-event-action` |
| D1–D3 | Existe `estados.puml` y dibuja exactamente los estados del XML (§2.3) |
| P1 | Toda expresión Groovy de `documentospdf/` compila contra la entidad (`documentos.md` §2.8) |

- Solo cuentan los métodos **declarados en la propia clase** de la fase: **MUST NOT** heredarlos de una superclase común.
- Al añadir, quitar o renombrar estados o eventos en el XML, ejecuta los tests y aplica lo que digan.

### 3.4 Lo que NO comprueba nada (falla en runtime)

- Que el `triggerInitialEvent` fije el estado inicial: si no lo hace, el alta revienta al crear el primer expediente.
- Que el `triggerInitialEvent` rellene los campos propios del tipo: el fallo aparece después, donde se usan.
- Que el validador pida los datos del interesado cuando el trámite admite representación o papel: si no, el registro de entrada sale sin interesado (`modelo.md` §2.1).
- Lo que en una expresión Groovy de un documento depende de los **datos** (una relación nula en mitad de una cadena): aborta el evento (`documentos.md` §2.8).
- Que el nombre de campo de firma que usan el `trigger*` y la `<action-method>` de AutoFirma sea un `campoFirma` del documento que se firma: si no existe, firmar aborta el evento (`documentos.md` §2.10).
- Que lo que pinta cada form tenga sentido: hay que navegar por todos los estados con los usuarios de cada perfil.

---

## 4. Checklist: crear un tipo de expediente nuevo

1. **Trámite**: asegúrate de que existe `tramites/<tramite>/TramiteInstance.xml` (`k-tramite`); si es la primera versión, sin `<defaultTipoExpediente>` aún.
2. **Carpeta**: crea `tramites/<tramite>/v1/` con `estados.puml` (§2.3) y `TipoExpedienteInstance.xml` (§2).
3. **Esqueletos**: `./gradlew -q CreateFilesTask -Ptipo=src/main/java/com/educaflow/tramites/<tramite>/v1` (§3.1).
4. **Modelo**: añade los campos a `domains.xml` → `modelo.md`.
5. **Documentos**: crea `documentospdf/` → `documentos.md` (y `documentotexto.md` si es en prosa). Si hay más de un documento, **MUST** extraer cada sección idéntica a un fragmento `_<contenido>.xml`.
6. **Evento inicial y fases**: rellena el `triggerInitialEvent` y, en cada fase, sus `trigger<Evento>` y `onEnter<Estado>` → `phaseeventmanager.md`. Lo compartido, en `<Code>Util` (§1.7).
7. **Validadores**: rellena las `rules { }` de cada pareja (estado, evento) → `validator.md`.
8. **Vistas**: paneles en el form plantilla de la raíz y un `<form state=...>` por estado y perfil en el `views.xml` de su fase → `vistas.md`.
9. **Permisos**: el perfil de cada estado (`CREADOR`, `TRAMITADOR`…) **MUST** estar asignado a alguien (`k-tramite` §6).
10. **Activa** la versión en el `TramiteInstance.xml` (`<defaultTipoExpediente>v1</defaultTipoExpediente>`) y compila y arranca con `./run.sh`.
11. **Prueba** navegando por **todos** los estados con usuarios de cada perfil (menú Expedientes → Trámites) (§3.4).

Para **modificar** un tipo que ya existe, primero decide si el cambio es compatible con los expedientes abiertos o si hace falta una versión nueva (`recetas/versionado.md` §1).

Para **añadir una fase** a un tipo que ya existe: añade su `<fase>` al XML moviendo a ella sus `<state>`, relanza `CreateFilesTask` y mueve a la nueva carpeta los métodos y forms de esos estados (y recuerda el CRITICAL de §1.2).

---

## Quick Guidelines

- Todo se deriva de la carpeta `tramites/<tramite>/<vN>/`: code = code del trámite + `VN`, entidad = code, paquete = ruta. Los nombres de clase son fijos: **MUST NOT** renombrarlos.
- Fases obligatorias, una subcarpeta por fase con `PhaseEventManagerImpl`, `StateEventValidatorImpl` y `views.xml`. Su `title` lo ve el usuario.
- Un estado = la pareja (fase, estado); en código siempre `States.<Fase>.<ESTADO>`, con la fase en UpperCamelCase.
- `events` obligatorio aunque vacío; sin `initial`: el estado inicial lo fija el `InitialEventManagerImpl`, uno por tipo.
- El motor ya comprueba el perfil del estado, filtra los campos por la whitelist del validador, valida, guarda el historial y elige la vista. Tú escribes el `trigger*` que decide el destino.
- Las `BusinessException` de un `trigger*` van al principio: lo que ya se haya creado fuera del expediente no se deshace.
- Flujo: XML + `estados.puml` → `CreateFilesTask` → modelo, documentos, fases, validadores y vistas → tests → activar versión → probar todos los estados.
- Funciones propias en una única `<Code>Util`; nunca `ValidationRule` propias del tipo.
- **MUST NOT** crear `i18n_*.csv` a mano ni editar `States`, `estados.png` ni el `<extra-code-model>`.
