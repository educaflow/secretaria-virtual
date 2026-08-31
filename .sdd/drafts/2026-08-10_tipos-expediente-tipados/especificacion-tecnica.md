# Especificación técnica: jerarquía tipada de fases y estados de los tipos de expediente

Sustituye al borrador *"Generación de Código Java desde XML de Dominio"*, alineándolo con lo que ya existe en el código.

Los tres desajustes del borrador que motivan esta reescritura:

- El XML de entrada de ejemplo (`<tiposExpediente>` con varios tipos) **no existe**.
  La entrada real es el `TipoExpedienteInstance.xml` de cada carpeta de versión, con un único tipo y bastante más información (§3).
- El borrador crea una interfaz `TipoExpediente` que **choca** con la entidad JPA `com.educaflow.subsystem.expedientes.db.TipoExpediente`.
  No se crea ninguna interfaz `TipoExpediente` paralela: los métodos de búsqueda se añaden a la entidad que ya existe (§4.2).
- El borrador da por bueno el estado del código actual, pero este refactor **elimina** `CodeStateNaming` y el nombre real `F_<fase>_S_<estado>`, sustituyéndolos por una columna `codePhase` persistida junto a `codeState` (§7).

Decisiones ya cerradas con el usuario (2026-08-10): API en inglés reusando `Phase`/`State`; la entidad `TipoExpediente` como punto de entrada; columna `codePhase` con `codeState` corto; alcance = refactor completo.
Cerradas también (mismo día, tras contrastar la spec con el código): la clase generada es pura — `Phase` no expone la entidad (§4.1) —; se borra la entidad `EstadoTipoExpediente` (§7.4); y el puente entidad→clase generada es un `INSTANCE` tipado, sin reflexión de métodos (§4.4, §8.3).
Y de la revisión de coherencia con el código (mismo día): `States` implementa `TipoExpedienteStates` directamente, sin duplicar los métodos de búsqueda como estáticos (§4.4, §5); un único mapa fase→estados en vez de un mapa y un `switch` por fase (§6.3); `getStateEventValidator` cambia de firma igual que `getEventManager` (§8.3, §8.5); y `States.java` se genera en una raíz propia `build/src-gen-states` para no solapar con `generateCode`/`RichDomainClassTask` (§11).
Y de la segunda revisión (mismo día): el locator expone un único `getTipoExpedienteStates(tipo)` cacheado en vez de duplicar los cinco métodos de búsqueda (§4.2, §8.3); `Phase` no expone el tipo de expediente — ni la entidad ni su código (§4.1) —; `addHistorialEstado` copia fase, estado y nombre del propio expediente sin re-resolver el `State` (§8.5); y el enum `Event` no se genera: los eventos quedan como strings (§6.5, §8.1).
Y de la discusión sobre los perfiles (mismo día): `Profile` es el enum **global** del dominio, único para todos los tipos — no se genera uno por tipo —; `State.getProfile()` lo devuelve tipado, `EventManager` y `EventContext` se quedan sin genéricos y el generador valida que los perfiles del XML pertenezcan a él (§4.1, §6.5, §8.1, §8.2, §10).
Y de la tercera revisión (mismo día): la búsqueda generada no usa excepciones de control de flujo ni copias por llamada — `PHASES` derivada de `values()` y un único mapa fase→lista de estados (§5, §6.3) —; los eventos conservan el orden de declaración en un `LinkedHashSet` (§5, §6.3), lo que cambia la justificación de la regla 10 de §10; `events=""` queda definido como conjunto vacío (§3.1); la migración no retoca `nameState` (§12); y `getPhase`/`getPhases`/`getStates` de la entidad y `States.CODE`/`States.NAME` se declaran API de completitud, sin consumidor en el runtime actual (§4.2).
Y de la cuarta revisión (mismo día): `ExpedienteUtil.updateState` comprueba por identidad que el `State` recibido pertenece al tipo del expediente, restaurando en runtime la barrera cross-tipo que se pierde al quitar los genéricos de `EventContext` (§8.2, §8.4); el generador valida la colisión `PascalCase` de estados y eventos de una fase, de la que salen los nombres de método (§10, regla 8); los inputs de `GenerateStatesTask` incluyen el `domains/TipoExpediente.xml` que lee la regla 11, y su ruta se pasa como argumento (§10, §11); `getStates()` devuelve una lista precalculada, sin copias por llamada (§5, §6.3); y §13 deja de anunciar un cambio de firma en los tests que el *erasure* hace invisible a `ClassFileImporter`.
Y de la quinta revisión (mismo día): el `extra-code` de la entidad queda en un único `getTipoExpedienteStates()` — los cinco métodos de búsqueda existen solo en la clase generada y se llega a ellos por la interfaz (§4.2, §8.4, §8.5, §15) —; el controlador valida el perfil parseado contra los perfiles de los estados del tipo, restaurando la detección en runtime que daba el `Enum.valueOf` sobre el enum por tipo ante un `profileName` del cliente, con lo que `getStates` gana un consumidor (§4.2, §8.6); el test nuevo de `States` carga la clase con reflexión normal en vez de `ClassFileImporter`, que no ve los argumentos de constructor del `<clinit>` (§13); los campos privados de la plantilla pasan a camelCase para que ningún nombre de fase pueda colisionar con ellos (§5, §6.3, regla 8 de §10); y la reserva de nombres de tipo anidado se generaliza a cualquier nombre simple importado por la plantilla (§10, regla 8).
Y de la sexta revisión (mismo día): las cuatro validaciones de colisión/reserva de identificadores se funden en una única regla sobre los identificadores **ya compuestos** — que detecta además la colisión cruzada estado/evento en `getForState*InEvent*`, invisible para la comparación por pares — y las tres reglas restantes se renumeran (§10); §4.2 aclara que `getPhase` forma parte del contrato público de §1 aunque el runtime del refactor no lo consuma; y `getFqcn` minusculiza la fase con `toLowerCase(Locale.ROOT)` (§8.3).
Y de la séptima revisión (mismo día): la regla 8 reserva también, para los nombres de método compuestos, los nombres de método público de las clases base `EventManager`/`StateEventValidator` — el caso real es el estado `STATE`, cuyo `onEnterState` sobrescribiría el dispatcher en silencio al quitar los genéricos de §8.2 —; como el generador corre antes de compilar `secretaria-virtual`, la lista es una constante en build-tools vigilada por reflexión desde un test nuevo de §13 (§9, §10, §13).
Y de la octava revisión (mismo día): la reserva de nombres de la API base deja de ser una constante en build-tools vigilada por un test y pasa a ser directamente **un test** de §13 que deriva la API real por reflexión y los nombres compuestos del XML — misma detección, en la fase de tests del mismo build, sin duplicación que vigilar (§9, §10, §13) —; `GenerateStatesTask` vacía su raíz de salida antes de emitir, para que el `States.java` de un tipo borrado o renombrado no sobreviva compilando en silencio en un build incremental (§11); §8.1 deja dicho que `CommonEvent` se conserva como fuente de los strings `DELETE`/`EXIT` aunque desaparezca del despacho; y §8.6 explicita el cambio de parámetro de `getEventContext` (el `TipoExpediente` en vez del `eventManager`).
Y de la puesta al día con el diseño hermano (mismo día): entran en el alcance `ExpedienteController` y `DataBaseStartup` (§2, §8.7), la tabla de §9 recoge los tres artefactos de build-tools que faltaban — `TextUtil`, `EventManagerFile` y `input-config-tipos-expedientes.template` —, y §16 se queda sin cuestiones abiertas: las dos que quedaban las cierra [`disenyo.md`](disenyo.md), que corrige además el recuento de `EventManagerImpl` a migrar (12, no seis, y 4 de ellos con lógica real).
Y de la revisión de cobertura spec↔diseño (mismo día): §12 deja de dar por hecho que «el proyecto ya usa Flyway» — está configurado pero sin ninguna migración, así que la de §12 sería la primera (hueco H3 del diseño).

---

## 1. Objetivo

Que la máquina de estados de un tipo de expediente exista **en tiempo de compilación** como una jerarquía fuertemente tipada, generada desde el `TipoExpedienteInstance.xml`, en vez de como un enum plano escrito a mano en cada `EventManagerImpl` y como cadenas compuestas a mano en runtime.

```java
State estado = States.Recepcion.ENTRADA_DATOS;          // referencia directa, comparable con ==

estado.getName();                                        // "Entrada datos"
estado.getPhase().getName();                             // "Recepción"
States.NAME;                                             // "Prueba V2" — sin tocar la base de datos

States.RECEPCION;                                        // la fase, tipada como Phase
```

Y que, cuando el tipo concreto no se conoce en compilación, la resolución sea polimórfica **desde la entidad**:

```java
TipoExpediente tipo = expediente.getTipoExpediente();
Optional<Phase> fase   = tipo.getTipoExpedienteStates().getPhase("RECEPCION");
Optional<State> estado = tipo.getTipoExpedienteStates().getState("RECEPCION", "ENTRADA_DATOS");
```

Objetivos derivados, todos consecuencia de lo anterior:

- Eliminar `CodeStateNaming` (las dos copias) y con él la codificación `F_<fase>_S_<estado>`.
- Eliminar `StateEnum`, que hoy lee por reflexión los campos `profile`, `events`, `initial` y `closed` del enum generado.
- Vaciar de enums el `EventManagerImpl` de cada fase: `State` pasa a la clase generada, `Event` desaparece — los eventos quedan como strings — y `Profile` pasa a ser el enum global del dominio, único para todos los tipos (§6.5).

## 2. Alcance

**Dentro**: el generador y todo lo que arrastra — interfaces `Phase`/`State`, entidad `TipoExpediente`, borrado de `EstadoTipoExpediente`, columna `codePhase`, `EventManager`, `EventContext`, `ExpedienteLocator`, `ExpedienteUtil`, `Tramitador`, `ExpedienteController`, `DataBaseStartup` (§8.7), nombres de vista, `EducaFlowBuildTools` (generador de esqueletos, viewprocessor, data-init), tests de `com.educaflow.tiposexpedientes`, migración de datos y skills a actualizar.

**Fuera**: cualquier cambio en la semántica de la tramitación (qué eventos hay, quién puede dispararlos, cómo se validan los campos).
El refactor es a coste funcional cero: lo que hoy funciona debe seguir funcionando igual.

---

## 3. Entrada: el `TipoExpedienteInstance.xml` real

### 3.1 Formato

Uno por carpeta de versión (`tramites/<tramite>/<vN>/`), con **un solo** tipo de expediente.
Ejemplo literal, el de `tramites/prueba/v2/`:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<TipoExpediente>

    <ambitoCreador>INDIVIDUAL</ambitoCreador>
    <ambitoResponsable>CENTRO</ambitoResponsable>
    <ambitoAuditor>DEPARTAMENTO</ambitoAuditor>
    <fases>
        <fase name="RECEPCION" title="Recepción">
            <state initial="true" name="ENTRADA_DATOS" profile="CREADOR" events="DELETE,PRESENTAR" />
            <state name="FIRMA_POR_USUARIO" profile="CREADOR" events="BACK,PRESENTAR_DOCUMENTOS_FIRMADOS" />
        </fase>
        <fase name="TRAMITACION" title="Tramitación">
            <state name="RESOLVER_PERMITIR_COMISION" title="Resolver permitir comisión" profile="RESPONSABLE" events="RESOLVER"/>
            <state name="ENTREGA_TICKETS" events="RESOLVER"/>
            <state closed="true" name="ACEPTADO" profile="" events=""/>
        </fase>
    </fases>
</TipoExpediente>
```

Tags opcionales que el ejemplo no usa: `<code>`, `<name>` y `<tramite>`, que sobrescriben los valores derivados de la carpeta.

| Elemento | Atributo | Obligatorio | Significado |
|---|---|---|---|
| `fase` | `name` | sí | Código de la fase, `UPPER_SNAKE_CASE`. Da nombre a la subcarpeta/paquete (en minúsculas) |
| `fase` | `title` | no | Nombre legible. Si falta, se humaniza el `name` |
| `state` | `name` | sí | Código del estado, `UPPER_SNAKE_CASE`. Único **dentro de su fase** |
| `state` | `title` | no | Nombre legible. Si falta, se humaniza el `name` |
| `state` | `profile` | no | Perfil que ve/opera ese estado, uno del enum global `Profile` del dominio (§6.5, §10). Vacío o ausente = sin perfil |
| `state` | `initial` | no | Exactamente uno en todo el tipo lo tiene a `true` |
| `state` | `closed` | no | Estado final (expediente cerrado) |
| `state` | `events` | no | Eventos disparables desde el estado, separados por comas. Vacío o ausente = ninguno: `events=""` produce el conjunto vacío, no un evento `""` (y por tanto no dispara la regla 9 de §10) |

### 3.2 Convención de nombres

- `name` de fase y de estado: `UPPER_SNAKE_CASE` sin acentos ni diacríticos, validado contra `[A-Z][A-Z0-9]*(_[A-Z0-9]+)*`.
- `title`: es el único sitio donde van acentos, y se usa tal cual para `getName()`.
- Del `name` se derivan identificadores Java por conversión directa `UPPER_SNAKE_CASE` → `PascalCase` (`RECEPCION` → `Recepcion`, `EN_TRAMITACION` → `EnTramitacion`).
  No hay transliteración de diacríticos porque no puede haberlos.

**Cambio respecto de hoy**: desaparece la restricción de que ningún segmento del nombre de fase pueda ser `S`.
Existía solo para que `F_<fase>_S_<estado>` fuese descomponible sin ambigüedad, y sin esa codificación deja de tener sentido.

### 3.3 Lo que se deriva de la carpeta

Sin cambios respecto de hoy (`k-tipo-expediente` §1.1): `code`, `name`, entidad JPA, `basePackageName`, carpeta y paquete de cada fase, FQCN de `EventManagerImpl` y `StateEventValidatorImpl`.

---

## 4. Contrato Java

### 4.1 Interfaces `Phase` y `State`

Los ficheros ya existen en `subsystem/expedientes/services/eventmanager/`, todavía sin usar.
Quedan así:

```java
public interface Phase {

    String getCode();          // "RECEPCION"
    String getName();          // "Recepción"
    List<State> getStates();   // los estados de la fase, en orden de declaración
}
```

```java
public interface State {

    Phase getPhase();
    String getCode();          // "ENTRADA_DATOS" — el nombre CORTO, que es lo que se persiste
    String getName();          // "Entrada datos"
    Profile getProfile();      // el enum global del dominio (db.Profile), o null si el estado no tiene perfil
    Set<String> getEvents();
    boolean isInitial();
    boolean isFinal();         // el closed="true" del XML
}
```

Cambios respecto de los ficheros actuales:

- `Phase` gana `getStates()`.
- `State` pierde `getSimpleCode()`/`getFullCode()` — ya no hay dos códigos —, gana `getCode()` (`getName()` ya existe en el fichero actual) y su `getProfile()` pasa de `String` al enum global `Profile` (§6.5).

`Phase` no expone el tipo de expediente — ni la entidad JPA ni su código —: la clase generada es una proyección pura del XML, sin imports de entidades ni necesidad de sesión de JPA — del dominio solo importa el enum global `Profile`, que es un enum plano (§5) —, y el camino de vuelta fase→tipo no tiene ningún consumidor.
En el caso polimórfico el punto de partida ya es la propia entidad (§4.2), y quien conoce el tipo en compilación tiene `States.CODE`.

### 4.2 La entidad `TipoExpediente` como punto de entrada

**No se crea ninguna interfaz nueva ni se duplica la API de búsqueda.**
La entidad `com.educaflow.subsystem.expedientes.db.TipoExpediente` gana, vía `<extra-code>` en `subsystem/expedientes/domains/TipoExpediente.xml`, un único método:

```java
public com.educaflow.subsystem.expedientes.services.eventmanager.TipoExpedienteStates getTipoExpedienteStates();
```

que delega en `ExpedienteLocator.getTipoExpedienteStates(this)` (§8.3), que localiza la clase generada por `basePackageName` y cachea su `INSTANCE`.
Los cinco métodos de búsqueda (§4.4) existen una sola vez, en la clase generada, y se llega a ellos a través de la interfaz: `expediente.getTipoExpediente().getTipoExpedienteStates().getState(...)`.
El `extra-code` queda en una línea a propósito: es Java embebido en XML — sin ayuda del IDE y, al inyectarse sin control de imports, con el tipo de retorno en FQCN —, el sitio más caro del proyecto para mantener código.
La entidad ya es un objeto por tipo de expediente recuperado de la base de datos, así que **hace de singleton polimórfico**: no hace falta el `INSTANCE` del borrador ni una interfaz `TipoExpediente` paralela (el `INSTANCE` interno que sí se genera es otra cosa: el puente del locator, §4.4 y §6.4).
Del contrato de búsqueda, el runtime de este refactor consume `getState` (§8.4, §8.5), `getInitialState` (§8.3, §8.5) y `getStates` (§8.6, la validación del perfil); `getPhase` y `getPhases` — igual que las constantes `States.CODE`/`States.NAME` (§5) — se incluyen por completitud y simetría de la API — `getPhase` forma además parte del contrato público que muestra §1 —, sin consumidor en el runtime de este refactor: viven en la clase generada, que se reemite entera en cada build, así que mantenerlos no cuesta nada.

Precedente de que la entidad llame a un servicio desde `extra-code`: la plantilla `extra-code-domain-xml.template` de los build-tools ya genera `ExpedienteUtil.getDocumentoPdf(this, ...)` dentro de las entidades de expediente.

### 4.3 La clase generada

| | |
|---|---|
| Nombre | `States` — **fijo**, igual en todos los tipos |
| Paquete | el `basePackageName` del tipo (p.ej. `com.educaflow.tramites.prueba.v2`) |
| Fichero | `build/src-gen-states/main/java/<paquete>/States.java` — generado en cada build (§11), **no** versionado |
| Interfaces que implementa | `TipoExpedienteStates` (§4.4); `Phase`/`State` los implementan sus enums anidados |

Nombre fijo por las mismas razones que `EventManagerImpl` y `StateEventValidatorImpl`: lo que distingue la clase de un tipo de otro es el paquete, y eso es justo lo que permite a `ExpedienteLocator` componer el FQCN.
Nombrarla como el tipo (`PruebaV2`, siguiendo el `Matricula` del borrador) **no vale**: choca con el nombre de la entidad JPA del tipo, que es exactamente `PruebaV2`, y todo `EventManagerImpl` necesita importar las dos.

### 4.4 La interfaz `TipoExpedienteStates` — el puente interno

Para que `ExpedienteLocator` llegue a la clase generada sin invocar métodos por reflexión, se añade en `services/eventmanager/` una interfaz con los cinco métodos de búsqueda:

```java
public interface TipoExpedienteStates {

    Optional<Phase> getPhase(String phaseCode);
    Optional<State> getState(String phaseCode, String stateCode);
    List<Phase> getPhases();
    List<State> getStates();
    State getInitialState();
}
```

Cada `States` generada la implementa **directamente** y expone su único ejemplar en `public static final States INSTANCE = new States()` (§5): los cinco métodos existen una sola vez, como métodos de instancia, sin duplicarlos como estáticos ni envolverlos en una clase anónima.
Solo la implementa la clase generada y su consumidor normal es `ExpedienteLocator.getTipoExpedienteStates` (§8.3), a través del cual delega el `getTipoExpedienteStates()` de la entidad (§4.2); el código de trámites usa las constantes (uso tipado) o la entidad (uso polimórfico), y solo nombra `States.INSTANCE` en el caso raro de reconstruir desde strings conociendo el tipo en compilación (§15, ejemplo 4).

---

## 5. Patrón de código generado

Para `tramites/prueba/v2/` (§3.1) el generador emite:

```java
package com.educaflow.tramites.prueba.v2;

import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.subsystem.expedientes.services.eventmanager.Phase;
import com.educaflow.subsystem.expedientes.services.eventmanager.State;
import com.educaflow.subsystem.expedientes.services.eventmanager.TipoExpedienteStates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * GENERADO a partir de TipoExpedienteInstance.xml — NO EDITAR.
 * Cualquier cambio se pierde en el siguiente build.
 */
public final class States implements TipoExpedienteStates {

    /** El code del tipo de expediente. No toca la base de datos. */
    public static final String CODE = "PruebaV2";

    /** El name del tipo de expediente. No toca la base de datos. */
    public static final String NAME = "Prueba V2";

    /** Único ejemplar: la única implementación de los métodos de búsqueda (§4.4). ExpedienteLocator lo resuelve por reflexión una sola vez por tipo (§8.3). */
    public static final States INSTANCE = new States();

    // Alias públicos de fase — la API con la que se referencia una fase desde fuera.
    public static final Phase RECEPCION = PhaseInternal.RECEPCION;
    public static final Phase TRAMITACION = PhaseInternal.TRAMITACION;

    private States() {
    }

    // =====================================================================
    // FASES — enum privado: detalle de implementación, nunca se nombra fuera
    // =====================================================================
    private enum PhaseInternal implements Phase {

        RECEPCION("Recepción"),
        TRAMITACION("Tramitación");

        private final String name;

        PhaseInternal(String name) {
            this.name = name;
        }

        @Override public String getCode() { return name(); }
        @Override public String getName() { return name; }
        @Override public List<State> getStates() { return statesByPhase.get(name()); }
    }

    // =====================================================================
    // FASE: RECEPCION
    // =====================================================================
    public enum Recepcion implements State {

        ENTRADA_DATOS("Entrada datos", Profile.CREADOR, true, false, "DELETE", "PRESENTAR"),
        FIRMA_POR_USUARIO("Firma por usuario", Profile.CREADOR, false, false, "BACK", "PRESENTAR_DOCUMENTOS_FIRMADOS");

        private final String name;
        private final Profile profile;
        private final boolean initial;
        private final boolean closed;
        private final Set<String> events;

        Recepcion(String name, Profile profile, boolean initial, boolean closed, String... events) {
            this.name = name;
            this.profile = profile;
            this.initial = initial;
            this.closed = closed;
            // LinkedHashSet inmodificable: conserva el orden de declaración del XML (§6.3).
            this.events = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(events)));
        }

        @Override public Phase getPhase() { return PhaseInternal.RECEPCION; }
        @Override public String getCode() { return name(); }
        @Override public String getName() { return name; }
        @Override public Profile getProfile() { return profile; }
        @Override public Set<String> getEvents() { return events; }
        @Override public boolean isInitial() { return initial; }
        @Override public boolean isFinal() { return closed; }
    }

    // =====================================================================
    // FASE: TRAMITACION
    // =====================================================================
    public enum Tramitacion implements State {

        RESOLVER_PERMITIR_COMISION("Resolver permitir comisión", Profile.RESPONSABLE, false, false, "RESOLVER"),
        ENTREGA_TICKETS("Entrega tickets", null, false, false, "RESOLVER"),
        ACEPTADO("Aceptado", null, false, true);

        // … idéntico al de Recepcion, con getPhase() → PhaseInternal.TRAMITACION
    }

    // =====================================================================
    // BÚSQUEDA (BD -> jerarquía tipada)
    // =====================================================================
    // Los campos privados van en camelCase a propósito: los alias de fase son campos
    // UPPER_SNAKE_CASE de esta misma clase, así que con minúsculas por medio ningún name de
    // fase puede colisionar con ellos (§10, regla 8).

    // Las fases en orden de declaración, derivadas de values(): getPhases() no vuelve a
    // enumerarlas en la plantilla (§6.3).
    private static final List<Phase> phases = List.of(PhaseInternal.values());

    // Único mapa fase -> estados de la fase, en orden de declaración: de aquí salen tanto la
    // búsqueda por strings como las listas de Phase.getStates(), ya precalculadas (§6.3).
    private static final Map<String, List<State>> statesByPhase = Map.ofEntries(
        Map.entry("RECEPCION", List.<State>of(Recepcion.values())),
        Map.entry("TRAMITACION", List.<State>of(Tramitacion.values()))
    );

    // TODOS los estados del tipo, en orden de declaración, precalculado como phases y
    // statesByPhase: ninguna búsqueda crea copias por llamada (§6.3). Se recorre phases, no
    // statesByPhase, porque Map.ofEntries no especifica su orden de iteración.
    private static final List<State> allStates;

    static {
        List<State> todos = new ArrayList<>();
        for (Phase phase : phases) {
            todos.addAll(phase.getStates());
        }

        allStates = Collections.unmodifiableList(todos);
    }

    @Override
    public Optional<Phase> getPhase(String phaseCode) {
        for (Phase phase : phases) {
            if (phase.getCode().equals(phaseCode)) {
                return Optional.of(phase);
            }
        }

        return Optional.empty();
    }

    @Override
    public Optional<State> getState(String phaseCode, String stateCode) {
        // Los mapas inmutables de Map.ofEntries lanzan NPE con clave null.
        if (phaseCode == null) {
            return Optional.empty();
        }

        List<State> statesOfPhase = statesByPhase.get(phaseCode);
        if (statesOfPhase == null) {
            return Optional.empty();
        }

        for (State state : statesOfPhase) {
            if (state.getCode().equals(stateCode)) {
                return Optional.of(state);
            }
        }

        return Optional.empty();
    }

    /** Las fases, en orden de declaración en el XML. */
    @Override
    public List<Phase> getPhases() {
        return phases;
    }

    /** TODOS los estados del tipo, de todas las fases, en orden de declaración. */
    @Override
    public List<State> getStates() {
        return allStates;
    }

    /** El estado inicial. Está resuelto en generación: hay exactamente uno. */
    @Override
    public State getInitialState() {
        return Recepcion.ENTRADA_DATOS;
    }
}
```

---

## 6. Decisiones de diseño

### 6.1 Constantes de estado en `UPPER_SNAKE_CASE`, no en `PascalCase`

El borrador escribía `Matricula.Solicitud.Pendiente`.
Aquí las constantes conservan el `UPPER_SNAKE_CASE` del XML: `States.Recepcion.ENTRADA_DATOS`.

Motivos:

- Mantiene el invariante **nombre de la constante == código persistido == `name` del XML**, que es lo que permite generar `getCode()` como `return name();` y lo que hace que un `grep ENTRADA_DATOS` encuentre el XML, la vista, el método `onEnterEntradaDatos` y la constante a la vez.
- Es la convención de Java para constantes de enum, y es la que ya tiene el enum `State` actual.
- Elimina de raíz una familia entera de colisiones: dos códigos distintos que normalizan al mismo `PascalCase` solo pueden colisionar ya en los **tipos** anidados (las fases), no en las constantes.
  En los nombres de **método** derivados de estados y eventos (`onEnter*`, `trigger*`, `getForState*InEvent*`) la normalización sigue existiendo; esa colisión la valida la regla 8 de §10.

Solo los **tipos** anidados van en `PascalCase`, porque son tipos Java: `Recepcion`, `Tramitacion`.

> Es la decisión más fácilmente reversible de toda la spec: afecta a una sola plantilla del generador. Si se prefiere la legibilidad del borrador, cambia solo cómo se emiten los nombres de constante; la colisión `PascalCase` entre estados de una fase ya la cubre la regla 8 de §10, que pasaría a proteger también las constantes.

### 6.2 `PhaseInternal` privado + alias públicos

La representación enum de las fases es `private` y existe solo para tener gratis `==`, `valueOf(String)` y `values()`.
Fuera de `States` nadie la nombra: una fase se referencia siempre por el campo `public static final Phase <CODIGO>`, tipado con la interfaz.
Un enum `private` puede implementar una interfaz pública sin problema — sus instancias circulan tipadas como `Phase`.

### 6.3 Un único mapa fase→estados

Toda la búsqueda por strings sale de dos estructuras precalculadas: la lista `phases` — `List.of(PhaseInternal.values())`, de la que sale `getPhases()` sin que la plantilla enumere las fases una vez más — y el único mapa `statesByPhase`, fase → lista de sus estados en orden de declaración, del que salen tanto `getState` como las listas de `Phase.getStates()`, ya inmutables y sin copias por llamada: una estructura sirve para las dos cosas.
`getPhase` y `getState` recorren esas listas linealmente: las fases y los estados de un tipo se cuentan con los dedos, así que el O(n) es irrelevante, y a cambio no hay excepciones de control de flujo (nada de `valueOf` + `catch`) ni `switch` que la plantilla tenga que mantener sincronizado con la lista de fases.
El mapa exterior usa `Map.ofEntries` y no `Map.of` porque `Map.of` está limitado a 10 parejas.
De ahí se deriva también, en la inicialización de la clase, la lista inmutable `allStates` que devuelve `getStates()`: se construye recorriendo `phases` — no iterando `statesByPhase`, cuyo orden de iteración `Map.ofEntries` no especifica — para conservar el orden de declaración.
Los tres campos (`phases`, `statesByPhase`, `allStates`) van en camelCase a propósito, contra la convención de constantes: los alias públicos de fase son campos `UPPER_SNAKE_CASE` de la misma clase, y con minúsculas por medio ningún `name` de fase puede colisionar con ellos, así que no hay que reservarlos (§10, regla 8).
Los eventos de cada estado van en un `LinkedHashSet` inmodificable construido en el constructor del enum a partir de varargs: hoy son una lista en orden de declaración y ningún consumidor depende del orden, pero conservarlo es gratis y evita que un listado futuro de eventos cambie de orden entre builds (con `Set.of` el orden de iteración es aleatorio).

### 6.4 `INSTANCE` es la única implementación; la entidad sigue siendo el punto de entrada polimórfico

El borrador necesitaba un singleton porque su `TipoExpediente` era una interfaz sin representación en la base de datos.
Aquí la entidad JPA ya es ese objeto, así que el punto de entrada polimórfico es la fila (§4.2).
`States` implementa `TipoExpedienteStates` y su único ejemplar es `INSTANCE` (§4.4): es el puente con el que `ExpedienteLocator` evita la reflexión de métodos, no una segunda API.
El código de trámites referencia constantes (`States.Recepcion.ENTRADA_DATOS`, `States.RECEPCION`) y solo nombra `States.INSTANCE` para reconstruir desde strings conociendo el tipo en compilación, que es un caso raro (§15, ejemplo 4).

### 6.5 Ni `Event` ni `Profile` se generan: eventos como strings, perfil global

Hoy los enums `State`, `Event` y `Profile` están anidados en `EventManagerImpl`, **duplicados en cada fase** (son idénticos por construcción, porque describen el tipo entero).
`State` pasa a la clase `States`; los otros dos no se generan, y el `EventManagerImpl` se queda solo con lógica escrita a mano.

`Event` no se sustituye por nada: los eventos quedan como strings.
Ningún código escrito a mano referencia `Event` fuera del boilerplate que este refactor borra (verificado: solo aparece en las declaraciones del enum `State` y en el `super(...)` del constructor), la pertenencia de un evento a un estado la valida `Tramitador` contra `State.getEvents()` — que ya son strings — y el nombre del método `trigger*` sale del propio string del evento (§8.1).

`Profile` es el enum **global** del dominio (`db.Profile`, definido en `domains/TipoExpediente.xml`), único para todos los tipos de expediente.
Se descartó generar un `Profile` por tipo con solo el subconjunto de perfiles que el tipo usa: la barrera de compilación que aportaba — impedir que un `EventManagerImpl` compare contra un perfil que su tipo no tiene — protege contra un error que el flujo de trabajo real ya cubre, porque el código lo genera y revisa la IA contrastando con el `TipoExpedienteInstance.xml`, y que además es trivial de ver.
A cambio desaparece el último parámetro genérico — `EventManager<T>` y `EventContext` sin genéricos (§8.1, §8.2) — y `State.getProfile()` queda tipado (§4.1).
Que los perfiles del XML pertenezcan al enum global lo valida el generador (§10, regla 11), restaurando la garantía que hoy da el data-init de `EstadoTipoExpediente` y que desaparece al borrar la entidad (§7.4).

Un mismo identificador puede ser a la vez evento y perfil sin ambigüedad — el perfil es una constante de enum y el evento un string —, así que el generador **no** lo valida (§10); se deja dicho aquí para que quede claro que es intencionado.

### 6.6 Códigos de estado repetidos entre fases

`PENDIENTE` puede estar en dos fases del mismo tipo sin ambigüedad: son constantes de enums distintos, `States.Recepcion.PENDIENTE` y `States.Tramitacion.PENDIENTE`, y por tanto objetos distintos.
Lo único que garantiza el diseño es que cada constante pertenece a exactamente una fase, recuperable con `getPhase()`.
Esto es lo que hace que la pareja `(codePhase, codeState)` sea la clave, y no `codeState` solo.

### 6.7 Búsqueda estricta, sin `toUpperCase()`

`getPhase`/`getState` comparan el código tal cual llega.
El borrador normalizaba con `toUpperCase()`; aquí no, porque todos los códigos son `UPPER_SNAKE_CASE` por validación y aceptar `"recepcion"` solo serviría para esconder un bug de quien llama.

---

## 7. Desaparición de `CodeStateNaming` y de `EstadoTipoExpediente`

### 7.1 La fase pasa a persistirse

Hoy la fase no se guarda: se codifica dentro del propio código de estado, `F_RECEPCION_S_ENTRADA_DATOS`, y se extrae con `CodeStateNaming`.
Pasa a ser una columna más.

| Entidad | Campo nuevo | Cambio en `codeState` |
|---|---|---|
| `Expediente` | `<string name="codePhase" title="Código de la fase" />` | pasa a guardar el nombre corto (`ENTRADA_DATOS`) |
| `HistorialEstado` | ídem | ídem |

`EstadoTipoExpediente` no gana la columna: se borra entera (§7.4).

`nameState` no cambia de significado (el texto que ve el usuario), pero pasa a salir de `State.getName()` — es decir, del `title` del XML si lo hay — en vez de humanizar el código.
Eso es una mejora colateral: `RESOLVER_PERMITIR_COMISION` deja de mostrarse como "Resolver permitir comision" y pasa a mostrarse como "Resolver permitir comisión".

### 7.2 Nombres de vista

La fase y el estado pasan a ser dos segmentos:

```
antes:    exp-PruebaV2-F_RECEPCION_S_ENTRADA_DATOS-CREADOR-form
después:  exp-PruebaV2-RECEPCION-ENTRADA_DATOS-CREADOR-form
```

Las plantillas de `EventManager` quedan:

```
exp-${EXPEDIENT_CODE}-${PHASE_CODE}-${STATE_CODE}-${PROFILE_CODE}-form
exp-${EXPEDIENT_CODE}-${PHASE_CODE}-${STATE_CODE}-form
```

El nombre lo componen dos sitios que **deben seguir coincidiendo**: `EventManager.getViewName` en runtime y `viewprocessor/tags/Form.java` en el build.
Sigue sin hacer falta localizador de vistas: el nombre es global y lleva ya la fase.

### 7.3 Qué se borra

- `secretaria-virtual`: `services/internal/CodeStateNaming.java` y `services/internal/StateEnum.java`.
- `EducaFlowBuildTools`: `files/tipoexpediente/CodeStateNaming.java`.
- Con ellos desaparece la nota de "clase duplicada en dos repositorios, cualquier cambio va a las dos copias" de `CLAUDE.md` y del skill `k-tipo-expediente`.

### 7.4 `EstadoTipoExpediente` se borra

Su único consumidor en todo el repositorio es `ExpedienteLocator.getCualquierCodeState`, que este mismo refactor elimina (§8.3): ni la seguridad, ni las vistas, ni ningún otro código o XML consultan la entidad ni la relación `estados`.
Su contenido (código de estado + perfil) pasa a existir completo y tipado en la clase `States`, así que conservar la tabla sería duplicar la máquina de estados en la base de datos sin ningún lector.

- Se borra la entidad de `subsystem/expedientes/domains/TipoExpediente.xml`, junto con la relación `estados` de `TipoExpediente`.
- El data-init de tipos de expediente deja de emitir estados (§9) y se queda solo con el tipo (code, name, basePackageName, tramite).

---

## 8. Impacto en el runtime, fichero a fichero

### 8.1 `EventManager`

Firma genérica: desaparecen los tres parámetros de tipo — el estado porque ya no hay **un** enum de estados sino uno por fase, el evento porque su enum desaparece y el perfil porque es el enum global (§6.5).

```java
// antes
public abstract class EventManager<T extends Expediente, State extends Enum<State>, Event extends Enum<Event>, Profile extends Enum<Profile>>
// después
public abstract class EventManager<T extends Expediente>
```

- El constructor se queda solo con `modelClass`; `getStateClass()`, `getEventClass()` y `getProfileClass()` desaparecen.
- `triggerEvent(...)`: el nombre del método `trigger*` sale directamente del string del evento, sin `Enum.valueOf(eventClass, ...)` ni fallback a `CommonEvent`; que el evento sea disparable desde el estado ya lo ha comprobado `Tramitador` contra `State.getEvents()` (§8.5).
  El enum `CommonEvent` **se conserva**: solo deja de participar en la resolución del método — el fallback era código muerto, porque `DELETE` va declarado en los `events` del XML y `EXIT` lo corta `ExpedienteController` antes de llegar a `Tramitador` — y sigue siendo la fuente de los strings `DELETE`/`EXIT` que usan `Tramitador` y el controlador.
- `onEnterState(...)`: el nombre del método sale de `expediente.getCodeState()` directamente (ya es el nombre corto), sin pasar por `CodeStateNaming.getEstado(...)`.
- `getViewName(...)`: interpola `expediente.getCodePhase()` y `expediente.getCodeState()`; ya no necesita resolver ninguna constante de enum.

### 8.2 `EventContext`

```java
// antes
public class EventContext<Profile extends Enum<Profile>, State extends Enum<State>>
public void updateState(State state)
// después
public class EventContext                 // sin genéricos
public Profile getProfile()               // Profile = el enum global del dominio
public void updateState(State state)      // State = la interfaz
```

Consecuencia mecánica pero masiva: **todos** los métodos `trigger*`/`onEnter*` de todos los `EventManagerImpl` cambian de `EventContext<Profile,State>` a `EventContext`.
Con los genéricos se pierde la barrera de compilación que impedía pasar a `updateState` un `State` de **otro** tipo de expediente; la restaura, en runtime, el check de identidad de `ExpedienteUtil.updateState` (§8.4).

### 8.3 `ExpedienteLocator`

Gana la responsabilidad de localizar también la clase `States` y de servir de puente entre la entidad y ella, con **un único método nuevo** — la API de búsqueda de cinco métodos no se duplica en el locator:

```java
public static TipoExpedienteStates getTipoExpedienteStates(TipoExpediente tipo);
```

- Resolución: `Class.forName(basePackageName + ".States")` y lectura del campo `INSTANCE`, tipado como `TipoExpedienteStates` (§4.4).
  La reflexión acaba ahí: quien lo recibe hace llamadas normales, sin `Method`.
- **MUST** cachear el `INSTANCE` en un `ConcurrentHashMap` con clave `basePackageName`: se resuelve en cada tramitación.
- `getEventManager(tipo, ...)` y `getStateEventValidator(tipo, ...)` — con sus variantes `getClase*` — pasan a recibir el **código de fase**, no el `codeState`: la resolución compartida (`getFqcn`) deja de extraer la fase con `CodeStateNaming` y el FQCN es `basePackageName + "." + phaseCode.toLowerCase(Locale.ROOT) + "." + <clase>`.
  El `Locale.ROOT` es a propósito (hoy `getFqcn` minusculiza sin locale): con la JVM en locale turco, una fase con `I` minusculizaría a `ı` y el `Class.forName` fallaría con un `ClassNotFoundException` desconcertante sobre un paquete que sí existe.
- `getEventManagerDelEstadoInicial(tipo)` deja de necesitar "un EventManager cualquiera" para preguntarle por el estado inicial: se lo pregunta a `getTipoExpedienteStates(tipo).getInitialState()`.
- Desaparecen `getCualquierCodeState(...)` — el único lector de la relación `estados` (§7.4) — y `getEventManagerDeCualquierFase(...)`: su único uso real era `getModelClass(tipo)`, que pasa a apoyarse en el EventManager de la fase del estado inicial (la clase del modelo es la misma en todas las fases).

### 8.4 `ExpedienteUtil.updateState`

```java
public static void updateState(Expediente expediente, State state) {
    if (state == null) { throw new IllegalArgumentException("El state no puede ser nulo."); }

    String phaseCode = state.getPhase().getCode();
    String stateCode = state.getCode();

    // Barrera cross-tipo: los State generados son singletons, así que el estado de ESTE tipo de
    // expediente con estos códigos debe ser el mismo objeto que el recibido (§8.2).
    State propio = expediente.getTipoExpediente().getTipoExpedienteStates()
            .getState(phaseCode, stateCode).orElse(null);
    if (propio != state) {
        throw new IllegalArgumentException("El estado " + phaseCode + "/" + stateCode
                + " no es del tipo de expediente " + expediente.getTipoExpediente().getCode()
                + ": o no existe en su máquina de estados o el State es de la clase States de"
                + " otro tipo (típicamente un import sin actualizar al duplicar una versión).");
    }

    if (stateCode.equals(expediente.getCodeState())
            && phaseCode.equals(expediente.getCodePhase())) {
        return;
    }

    expediente.setCodePhase(phaseCode);
    expediente.setCodeState(stateCode);
    expediente.setNameState(state.getName());
    expediente.setFechaUltimoEstado(LocalDateTime.now());
    expediente.setAbierto(state.isFinal() == false);
}
```

La comparación de "no ha cambiado" pasa a ser sobre la **pareja** (fase, estado): con códigos cortos, `ENTRADA_DATOS` en dos fases distintas ya no es el mismo estado.
Es el punto más delicado del refactor.

El check de identidad restaura en runtime la barrera cross-tipo de §8.2: sin genéricos, un `State` de **otro** tipo de expediente compila, y el caso es realista porque crear una versión nueva consiste en duplicar la carpeta de la anterior (`versionado.md`) y ambas versiones tienen una clase llamada exactamente `States` — un import sin actualizar en un `EventManagerImpl` copiado persistiría en silencio fase y estado de la versión equivocada mientras las dos máquinas coincidieran.
Como los `State` generados son singletons, basta comparar con `==` contra lo que devuelve el `getTipoExpedienteStates()` del tipo del expediente — que va al `INSTANCE` cacheado del locator (§8.3) —: una búsqueda lineal sobre listas minúsculas por cambio de estado.

### 8.5 `Tramitador`

- `triggerInitialEvent`: el estado inicial sale de `tipoExpediente.getTipoExpedienteStates().getInitialState()` (§4.2), tipado como `State`, y es lo que se pasa a `ExpedienteUtil.updateState`.
  Desaparece `StateEnum.getInitialState(eventManager.getStateClass())`, que era el último uso de `getStateClass()`.
- `triggerEvent`: resuelve el estado actual con `tipoExpediente.getTipoExpedienteStates().getState(expediente.getCodePhase(), expediente.getCodeState()).orElseThrow(...)` y comprueba `state.getEvents().contains(eventName)`.
  Desaparece `new StateEnum(ReflectionUtil.getEnumConstant(...))`.
- `addHistorialEstado`: guarda `codePhase` además de `codeState`.
  Corre siempre después de `ExpedienteUtil.updateState`, así que `codePhase`, `codeState` y `nameState` se copian del propio expediente, sin re-resolver el `State`; `nameState` deja de humanizarse aquí porque `updateState` ya lo puso desde `State.getName()` (§8.4).
- `getEstadoUpperCamelCase(codeState)`: deja de llamar a `CodeStateNaming.getEstado`; el `codeState` ya es corto.
- Las tres resoluciones de `ExpedienteLocator` de `triggerEvent` (el `EventManager` y el `StateEventValidator` del estado origen, y el `EventManager` del destino) pasan el código de fase.
- `validateChild(...)`: su resolución del `StateEventValidator` pasa también el `codePhase` del expediente; los nombres de los métodos de validación no cambian (ya usaban el nombre corto).
- `assertValidState(...)`: hoy es código muerto (nadie lo llama) y con este cambio deja de compilar tal cual. Borrarlo.

### 8.6 `ExpedienteController`

Las dos llamadas a `ExpedienteLocator.getEventManager(...)` pasan el `codePhase` del expediente.
El perfil del contexto se resuelve con `Profile.valueOf(profileName)` sobre el enum global, en vez de `Enum.valueOf(eventManager.getProfileClass(), profileName)`.
Como el enum global acepta perfiles que el tipo no usa — y el `profileName` viene de la petición del cliente —, `getEventContext` (común a los tres endpoints) valida además que el perfil parseado esté entre los perfiles de los estados del tipo (`tipoExpediente.getTipoExpedienteStates().getStates()`, comparando con `State.getProfile()`) y falla en seco si no.
Eso restaura la detección en runtime que hoy da el `Enum.valueOf` sobre el enum por tipo, que solo lleva el subconjunto de perfiles que el tipo usa; sin ella, un perfil ajeno al tipo no siempre acabaría en error, porque `getViewName` tiene fallback a la vista sin perfil y la petición podría llegar a renderizar una vista — un cambio de comportamiento que §2 prohíbe.
`getEventContext` cambia además de parámetro: recibía el `eventManager` solo para `getProfileClass()`, que desaparece (§8.1), y pasa a recibir el `TipoExpediente` — en `triggerInitialEvent` el del trámite, en los otros dos el del expediente —, que es lo que necesita para esa validación.

### 8.7 `DataBaseStartup`

Su `Set<String> tablasIncluidas` nombra a mano `expedientes_estado_tipo_expediente`, que es la tabla de la entidad que se borra (§7.4).
Hay que quitarla de ahí o el reseteo de la base de datos fallará al truncar una tabla que ya no existe.

---

## 9. Impacto en `EducaFlowBuildTools`

| Clase / plantilla | Cambio |
|---|---|
| `files/tipoexpediente/CodeStateNaming` | **borrar** |
| `files/tipoexpediente/State` | `getCodeState()` pasa a devolver el `name` corto; se le añade `getTitleOrHumanizedName()` |
| `files/tipoexpediente/Fase` | igual, para el `title`; se le quita la referencia a `CodeStateNaming` en el javadoc |
| `files/tipoexpediente/TipoExpedienteInstanceFileFinder` | `checkFases` pierde la validación del segmento `S` y gana las de §10 |
| `common/TextUtil` | gana un `humanize(String)` propio (el del runtime lo delega en el `Inflector` de Axelor, que build-tools no tiene en el classpath) y una función Pebble `escapeJava` para los literales de la plantilla |
| `createstates/` (nuevo) | `Main` + `StatesFile`: el generador de `States.java` |
| `states.template` (nuevo) | la plantilla de §5 |
| `event-manager.template` | quita los tres enums anidados (`Event` desaparece y `Profile` es el global del dominio, §6.5), cambia la firma genérica y los `EventContext<...>`; el `super(...)` queda `super(<Modelo>.class)` |
| `event-manager-trigger-method.template`, `event-manager-onenter-method.template` | `EventContext` sin genéricos |
| `files/eventmanagerfile/EventManagerFile` | el contexto de la plantilla pierde `states`/`events`/`profiles` (ya no los usa nadie) y gana `basePackageName`, para el `import <basePackageName>.States;` del esqueleto |
| `state-event-validator*.template` | sin cambios: ya usan el nombre corto del estado |
| `viewprocessor/tags/Form.java` | nombre de vista con fase y estado como segmentos (§7.2) |
| `input-config-tipos-expedientes.template` | desaparece el `<bind node="state" to="estados" …>`: la relación `estados` ya no existe (§7.4) |
| `input-config-tipos-expedientes-input-data.template` | desaparece el bloque `<state>`: sin `EstadoTipoExpediente` (§7.4), el data-init solo carga el tipo |
| `createdatainittipoexpediente/` | deja de emitir estados; sin más cambios de estructura |

**Nota de coherencia**: el skill `k-tipo-expediente` dice que el generador de esqueletos y los tests de `com.educaflow.tiposexpedientes` comparten estas mismas clases y plantillas, de modo que "el código del método que el test dice que falta es literalmente el que el generador habría escrito".
Ese invariante **MUST** conservarse.

---

## 10. Validaciones del generador

Falla en fase de parseo/validación, **antes** de emitir nada, con un mensaje que identifique el conflicto exacto.
No se genera código Java parcial ni se ignora ningún conflicto en silencio.

Ya existen (`TipoExpedienteInstanceFileFinder`) y se conservan:

1. `code` de tipo de expediente repetido en todo el árbol.
2. `name` de fase repetido dentro del tipo.
3. `name` de estado repetido dentro de la misma fase.
4. Fase sin ningún estado.
5. Ningún estado inicial, o más de uno.
6. `<states>` en la raíz (formato antiguo).
7. `name` de fase y de estado en `UPPER_SNAKE_CASE` válido.

Nuevas, propias de la generación de Java:

8. **Colisión o reserva de identificadores generados** — una sola regla en vez de comprobaciones por pares (fase-fase, estado-estado, evento-evento, fase-reservado): el generador construye la lista completa de identificadores Java que va a emitir para el tipo y falla si hay algún duplicado o algún nombre reservado pisado.
   Tres familias de identificadores: los **tipos anidados** (el `PascalCase` de cada fase), los **campos** de `States` (el `name` de cada fase como alias `public static final Phase`, en la misma clase que `CODE`, `NAME` e `INSTANCE`) y los **nombres de método** de esqueletos y tests **ya compuestos** (`onEnter<Estado>` por estado, `trigger<Evento>` por evento y `getForState<Estado>InEvent<Evento>` por pareja, todos dentro de su fase).
   Reservados para los tipos anidados: `States` (Java prohíbe que una clase anidada se llame como la que la contiene), `PhaseInternal` (colisionaría con el tipo anidado generado) y **cualquier nombre simple que importe la plantilla** — hoy `Phase`, `State`, `Profile`, `TipoExpedienteStates` y los de `java.util` (`ArrayList`, `Arrays`, `Collections`, `LinkedHashSet`, `List`, `Map`, `Optional`, `Set`) —, porque un tipo anidado con ese nombre hace *shadowing* del import dentro de `States` y rompe la compilación (los `implements`, las referencias `Profile.<PERFIL>` o los propios `List`/`Map` de la plantilla) con un error críptico; la lista se deriva de los imports de `states.template`, no se mantiene aparte: si la plantilla cambia sus imports, la validación cambia sola.
   Los demás campos de la clase generada (`phases`, `statesByPhase`, `allStates`) no necesitan reserva: van en camelCase (§5, §6.3) y un `name` de fase es `UPPER_SNAKE_CASE`, así que no puede colisionar con ellos.
   Se validan los nombres de método **compuestos**, y no el `PascalCase` de cada estado/evento por separado, porque la conversión no es inyectiva — con dígitos por medio, `AB2C` y `AB_2C` producen ambos `Ab2c` — y la colisión puede además **cruzar** estado y evento: el estado `A_IN_EVENT_B` con el evento `C` y el estado `A` con el evento `B_IN_EVENT_C` producen ambos `getForStateAInEventBInEventC`, cosa que ninguna comparación por pares detecta.
   Reservados para los nombres de método compuestos: los nombres de método público de la clase base que los hereda — `EventManager` para `onEnter<Estado>` y `trigger<Evento>`, `StateEventValidator` para `getForState<Estado>InEvent<Evento>` (hoy es una interfaz vacía, así que aporta lista vacía) —, porque un método generado con uno de esos nombres no da error de compilación: o **sobrescribe en silencio** el método heredado — el caso real es el estado `STATE`, cuyo `onEnterState(<Modelo>, EventContext)` tiene, sin los genéricos de §8.2, exactamente la firma del dispatcher `EventManager.onEnterState`, de modo que el despacho ejecutaría el handler de ese estado para **cualquier** estado de la fase, sin error de compilación ni de runtime — o crea una sobrecarga legal pero confusa (un evento `INITIAL_EVENT` y su `triggerInitialEvent` de 3 parámetros).
   Se compara solo el **nombre**, sin firmas, a propósito: prohíbe también esas sobrecargas y evita mantener aridades sincronizadas con la base.
   Esta reserva es la única de la regla que **no** comprueba el generador, sino un test a mano de §13: el generador corre **antes** de compilar `secretaria-virtual` y los build-tools no comparten código con el runtime, así que ahí la lista solo podría existir como una constante duplicada a mano que otro test tendría que vigilar; los tests, en cambio, tienen a la vez el XML — que ya parsean con las clases de build-tools — y las clases base reales en el classpath, y derivan la lista por reflexión sin duplicar nada.
   El trade-off exacto (el error salta en la fase de tests del mismo build, no al generar) está en §13.
   En los demás casos el resultado serían tipos o métodos duplicados en `States`, esqueletos y tests, que no compilan — el mismo error críptico que motiva la regla 9; el de la API heredada es peor, porque compila y rompe el despacho en silencio, y por eso se detecta con un test aunque el generador no pueda atajarlo.
9. **Evento que no es identificador Java válido** en `UPPER_SNAKE_CASE`: hoy no se valida y rompe los nombres de método `trigger*` de los esqueletos con un error críptico (los perfiles no necesitan esta regla: los cubre entera la 11).
10. **Evento repetido en el `events` de un mismo estado**: el `LinkedHashSet` generado (§5, §6.3) los deduplicaría en silencio, así que sin esta validación el error del XML pasaría desapercibido; el build es además el único sitio donde el mensaje puede señalar el fichero exacto.
11. **Perfil que no pertenece al enum global `Profile` del dominio** (`domains/TipoExpediente.xml`, cuya ruta recibe `GenerateStatesTask` como argumento y forma parte de sus inputs, §11): restaura la garantía que hoy da el data-init de `EstadoTipoExpediente` — que desaparece al borrar la entidad (§7.4) — y ataja typos que producirían una referencia `Profile.<X>` que no compila.

La colisión evento/perfil (un mismo identificador en los dos roles) **no** está en la lista a propósito: no genera constantes ambiguas porque el perfil es una constante de enum y el evento un string (§6.5).

---

## 11. Mecanismo de generación

Este es el apartado que el borrador dejaba "pendiente de definir".

`States.java` **no** es un esqueleto: es una proyección literal del XML y se regenera entera en cada build.
Por eso **MUST NOT** vivir en `src/main/java` ni versionarse, y por eso **no** se genera con `CreateFilesTask` (que es idempotente y nunca pisa lo escrito).

Va a una raíz propia, `build/src-gen-states/main/java`, registrada como `srcDir` adicional.
**No** comparte `build/src-gen` a propósito: ahí escriben tanto el `generateCode` de Axelor como `RichDomainClassTask` — que edita la clase de entidad de cada tipo justo bajo el subárbol de trámites —, y dos tareas con outputs solapados desactivan los up-to-date checks de Gradle.
Con raíz propia no hay solape que declarar ni orden que imponer respecto a `generateCode`/`RichDomainClassTask`.
Esto no rompe la regla de "el build no escribe en `src/main/java`": sigue sin hacerlo.

La tarea **vacía su raíz de salida antes de emitir**: como `States` es pura a propósito (§4.1), el `States.java` de un tipo borrado o de una carpeta renombrada no referencia nada que haya dejado de existir y **seguiría compilando en silencio** desde `build/src-gen-states` en un build incremental (`./run.sh` hace `clean`, pero no es el único camino por el que se compila).
Vaciar y reemitir garantiza que en esa raíz solo hay proyecciones de XMLs que existen.

```groovy
tasks.register('GenerateStatesTask', JavaExec) {
    description = 'Genera la clase States de cada tipo de expediente a partir de su TipoExpedienteInstance.xml.'
    group = 'build'

    mainClass = 'com.educaflow.common.buildtools.createstates.Main'
    classpath = configurations.educaFlowBuildToolsDependency

    args './src/main/java', './build/src-gen-states/main/java', paqueteRaizTramites, './src/main/java/com/educaflow/subsystem/expedientes/domains/TipoExpediente.xml'

    workingDir = layout.projectDirectory.dir('.')

    // La raíz de salida se vacía antes de emitir: un States.java huérfano (tipo borrado, carpeta
    // renombrada) es puro y seguiría compilando en silencio en un build incremental.
    doFirst { delete(layout.buildDirectory.dir('src-gen-states')) }

    // Inputs acotados a lo que la tarea lee: los TipoExpedienteInstance.xml y el
    // domains/TipoExpediente.xml del que la regla 11 de §10 saca el enum global Profile (sin él,
    // renombrar un perfil dejaría la tarea up-to-date y esa validación sin re-ejecutar). Un cambio
    // en cualquier otro fichero (incluidos los .java del propio árbol de trámites) no la
    // re-ejecuta. Los outputs van a una raíz PROPIA (no build/src-gen) para no solapar con lo que
    // escriben generateCode y RichDomainClassTask, que confundiría los up-to-date checks de Gradle.
    inputs.files(fileTree(layout.projectDirectory.dir('src/main/java/' + paqueteRaizTramites.replace('.', '/'))) { include '**/TipoExpedienteInstance.xml' })
    inputs.file('src/main/java/com/educaflow/subsystem/expedientes/domains/TipoExpediente.xml')
    outputs.dir(layout.buildDirectory.dir('src-gen-states/main/java'))
}
sourceSets.main.java.srcDir(layout.buildDirectory.dir('src-gen-states/main/java'))
tasks.named('compileJava')   { dependsOn tasks.GenerateStatesTask }
tasks.named('compileKotlin') { dependsOn tasks.GenerateStatesTask }
```

`dependsOn` explícito sobre `compileJava`/`compileKotlin`, y no `generateCode { finalizedBy ... }` como `RichDomainClassTask`: un finalizador solo garantiza ejecutarse *después* de la tarea finalizada, no *antes* de la compilación.
La tarea no depende de `generateCode` porque solo lee XML de `src/main/java`.
El cuarto argumento es la ruta del `domains/TipoExpediente.xml`: la regla 11 de §10 valida los perfiles del tipo contra el enum global `Profile` que se define ahí, y por eso ese fichero es también input de la tarea.

---

## 12. Migración de datos

Los `codeState` ya persistidos tienen el formato antiguo y hay que partirlos en dos columnas.

En desarrollo basta con resetear la base de datos (`agent_docs/deploy.md`), que es el camino primario.
Si hay que conservar datos, va como migración Flyway — con el matiz que levanta [`disenyo.md`](disenyo.md) §0.2 (hueco H3): `DataBaseStartup` **tiene Flyway configurado**, pero su carpeta de `locations` no existe y no hay ni un `.sql` en el repositorio, así que esta sería la **primera** migración del proyecto, no una más.
La ruta del fichero y el riesgo de orden entre Flyway y el `hbm2ddl` de Axelor están en `disenyo.md` §7.3.

```sql
-- comprobar los nombres reales de tabla/columna antes de ejecutar
UPDATE expedientes_expediente
   SET code_phase = substring(code_state from '^F_(.*?)_S_'),
       code_state = substring(code_state from '^F_.*?_S_(.*)$')
 WHERE code_state LIKE 'F\_%\_S\_%';
```

Repetir para `expedientes_historial_estado`.
La migración **no** recalcula `nameState`: las filas ya persistidas conservan el texto humanizado antiguo (sin acentos), y la mejora del `title` (§7.1) se ve desde el siguiente cambio de estado de cada expediente.
La tabla de `EstadoTipoExpediente` no se migra: la entidad se borra (§7.4), así que la misma migración elimina su tabla (`DROP TABLE`).

---

## 13. Tests

Los de `src/test/java/com/educaflow/tiposexpedientes` se escriben a mano y los `.java` son la fuente de verdad (`CLAUDE.md`).

- `EventManagerTest` y `StateEventValidatorTest`: la comprobación de "un método por evento / por estado / por pareja estado-evento de la fase" **no cambia**, porque siempre usó el nombre corto.
  La firma que esperan tampoco cambia en la práctica: el tipo raw del parámetro sigue siendo `EventContext`, y los genéricos que desaparecen (§8.2) son invisibles para `ClassFileImporter`, que lee los parámetros ya con *erasure*.
- **Desaparece** la comprobación de que "el enum `State` de cada fase lleva todos los estados del tipo con su nombre real": ese enum ya no lo escribe nadie a mano.
  Con ella se va el uso que `EventManagerTest` hace del `CodeStateNaming` de build-tools (`isCodeState`, para reconocer las constantes de ese enum), que es lo que permite borrar la clase (§7.3).
- **Se añade** un test que verifica que la clase `States` generada de cada tipo concuerda con su XML (fases, estados, códigos, inicial/final, perfiles y eventos).
  A diferencia de los otros dos, **no** usa `ClassFileImporter`: ArchUnit expone estructura (tipos, miembros, firmas), y los valores por constante — `initial`, `closed`, el perfil y los eventos — son argumentos de constructor enterrados en el `<clinit>` del bytecode, invisibles para él.
  Como `States` es pura a propósito (§4.1) — sin JPA ni sesión — y está en el classpath de los tests, el test la carga con reflexión normal (`Class.forName` sobre el `basePackageName` derivado del XML) y compara lo que devuelven los getters con el XML: más simple y más fuerte, porque verifica los valores reales.
  Es el que sustituye al que desaparece.
- **Se añade** también el test que cubre la reserva de nombres de la API base de la regla 8 de §10, la única de esa regla que el generador no comprueba: compone los nombres de método `onEnter<Estado>`/`trigger<Evento>`/`getForState<Estado>InEvent<Evento>` de cada tipo a partir de su XML — con las mismas clases de build-tools con las que estos tests ya lo parsean — y falla si alguno coincide con un método público de `EventManager` o `StateEventValidator`, leídos por reflexión.
  No hay constante duplicada en build-tools ni test que la vigile: la lista de la API se lee de las propias clases base, así que si la base gana, pierde o renombra un método público el test se ajusta solo.
  El trade-off respecto de validarlo en el generador: el error salta en la fase de tests del build, no en `GenerateStatesTask` — más tarde dentro del **mismo** build, con cobertura idéntica porque el build siempre ejecuta los tests — y un esqueleto conflictivo de `CreateFilesTask` llega a generarse y lo denuncia el build siguiente, no la propia generación.

## 14. Documentación a actualizar

| Fichero | Qué |
|---|---|
| `CLAUDE.md` | El párrafo de tipos de expediente: `CodeStateNaming` duplicada y "nombre real `F_{fase}_S_{estado}`" |
| `.claude/skills/k-tipo-expediente/SKILL.md` | §1.5 entera (el nombre real del estado), §1.6 (`ExpedienteLocator`), §2.3 (el enum `State`), §3.3 (tests) |
| `.claude/skills/k-tipo-expediente/eventmanager.md` | firma genérica, `EventContext` sin genéricos, `eventContext.updateState(States.Fase.ESTADO)` |
| `.claude/skills/k-tipo-expediente/validator.md` | referencias a `CodeStateNaming` |
| `.claude/skills/k-tipo-expediente/vistas.md` | nombre de vista con fase y estado |
| `.claude/skills/k-tipo-expediente/versionado.md` | qué se copia al duplicar un tipo (`States.java` ya no, es generado) |
| `agent_docs/architecture-rules.md` | comprobar si alguna regla nombra `build/src-gen` (ahora también existe `build/src-gen-states`, §11) o la dirección `db → services` |

## 15. Ejemplos de uso esperado

```java
// 1. Asignación y comparación con ==
State estado = States.Recepcion.ENTRADA_DATOS;
if (estado == States.Recepcion.ENTRADA_DATOS) { … }

// 2. Cambio de estado desde un EventManagerImpl, cruzando de fase
eventContext.updateState(States.Tramitacion.ACEPTADO);

// 3. La fase, y su nombre legible
States.RECEPCION.getName();                    // "Recepción"
estado.getPhase() == States.RECEPCION;         // true

// 4. Reconstrucción desde la BD conociendo el tipo en compilación (caso raro, §6.4)
Optional<State> s = States.INSTANCE.getState("TRAMITACION", "ACEPTADO");

// 5. Reconstrucción polimórfica, sin conocer el tipo concreto
TipoExpediente tipo = expediente.getTipoExpediente();
State actual = tipo.getTipoExpedienteStates()
                   .getState(expediente.getCodePhase(), expediente.getCodeState())
                   .orElseThrow(() -> new RuntimeException("Estado desconocido"));
if (actual.getEvents().contains("PRESENTAR")) { … }
```

## 16. Cuestiones abiertas

**No queda ninguna.**

Las que había sobre `EstadoTipoExpediente` y sobre `Phase.getTipoExpediente()` quedaron cerradas al contrastar la spec con el código: la entidad se borra (§7.4) y `Phase` no expone el tipo (§4.1).
La del enum `Profile` quedó cerrada también: se adopta el global del dominio, único para todos los tipos (§6.5).

Las dos últimas las cierra el diseño hermano ([`disenyo.md`](disenyo.md)):

1. **Forma de las constantes de estado** (§6.1): se mantiene `UPPER_SNAKE_CASE` (`States.Recepcion.ENTRADA_DATOS`), y no el `PascalCase` del borrador original.
   Sigue siendo la decisión más fácilmente reversible de la spec, por lo que dice §6.1.
2. **Orden de ejecución del refactor**: `disenyo.md` §9, siete fases con sus puertas de verificación.
   La secuencia es: build-tools completo → publicar el JAR (punto de sincronización entre repos) → contratos, columnas y `build.gradle` → runtime del subsistema → los **12** `EventManagerImpl` → tests → verificación funcional → documentación.
   La ventana en la que `secretaria-virtual` no compila va de la fase 3 a la 5, y es a propósito.
   Son 12 `EventManagerImpl` y no seis (6 tipos de expediente × 2 fases), y 4 de ellos —los de `justificacion_falta_profesorado`— llevan lógica real y no se migran mecánicamente: el detalle está en `disenyo.md` §0.2 (hueco H4) y §6.1.