# Diseño: jerarquía tipada de fases y estados de los tipos de expediente

Diseño ejecutable de la especificación técnica hermana ([`especificacion-tecnica.md`](especificacion-tecnica.md)).
Las referencias `§N` sin más son a esa spec; a este documento se le cita como `D§N`.

---

## 0. Alcance de este documento

La spec fija **qué** se construye y **por qué**, con ocho rondas de decisiones ya cerradas.
No las repite este diseño.
Lo que añade es lo que hace falta para poder programar sin volver a razonar:

- El **inventario cerrado** de ficheros a tocar, verificado contra el código real (D§1).
- El **código literal** de cada artefacto nuevo o modificado (D§2 a D§5).
- El **orden de ejecución** entre los dos repositorios, con las puertas de verificación y la ventana en la que el proyecto no compila a propósito (D§9), que cierra la cuestión abierta §16.2.
- Cuatro **huecos de la spec** detectados al contrastarla con el código, marcados con ⚠ allí donde aparecen.

### 0.1 Decisiones cerradas que este diseño da por fijas

| Decisión | Dónde | Valor |
|---|---|---|
| Forma de las constantes de estado | §6.1 / §16.1 | **`UPPER_SNAKE_CASE`** — `States.Recepcion.ENTRADA_DATOS`. `getCode()` se genera como `return name();` |
| Punto de entrada polimórfico | §4.2 | La entidad `TipoExpediente`, con **un solo** método `getTipoExpedienteStates()` |
| Enum de perfiles | §6.5 | El **global** del dominio, `com.educaflow.subsystem.expedientes.db.Profile` |
| Eventos | §6.5 | **Strings**. No se genera ningún enum `Event` |
| Raíz de salida del generador | §11 | `build/src-gen-states/main/java`, propia, vaciada en cada ejecución |

### 0.2 Los cuatro huecos de la spec

⚠ **H1 — `DataBaseStartup` nombra la tabla a borrar a mano.**
`src/main/java/com/educaflow/secretariavirtual/startup/DataBaseStartup.java:29` tiene
`Set<String> tablasIncluidas = Set.of("expedientes_estado_tipo_expediente");`.
Al borrar la entidad (§7.4) hay que quitar esa línea o el reseteo de BD fallará al truncar una tabla inexistente.
Se resuelve en D§5.7.

⚠ **H2 — son 12 `EventManagerImpl`, no 6.**
El borrador de §16 decía «los seis EventManagerImpl existentes»; la spec ya recoge la corrección (§16, punto 2), que salió de aquí.
Hay 6 tipos de expediente × 2 fases = **12** ficheros `EventManagerImpl.java` a migrar a mano (D§6), más 12 `StateEventValidatorImpl.kt` que **no** se tocan.

⚠ **H3 — no existe ninguna migración Flyway todavía.**
`DataBaseStartup` configura Flyway con `locations("classpath:com/educaflow/secretariavirtual/startup/database")` y `baselineOnMigrate(true)`, pero esa carpeta no existe y no hay ni un `.sql` en el repositorio.
La de §12 sería la primera migración del proyecto.
Se resuelve en D§7.3 fijando el reset de BD como camino primario.

⚠ **H4 — 4 de los 12 `EventManagerImpl` tienen lógica real, no son esqueletos vacíos.**
§16.2 da por hecho que migrarlos es mecánico.
Lo es en 8 de ellos, donde las referencias a estados están comentadas (`//eventContext.updateState(State.);`), pero los 4 de `justificacion_falta_profesorado` (`actual/v1` y `futuro/v2`, ambas fases) suman **18 llamadas reales** a `eventContext.updateState(State.F_..._S_...)`, varias de ellas **cruzando de fase**, y los dos de `recepcion` usan además el patrón `State.valueOf(expediente.getCodeState())` + `switch (state)`, que no tiene traducción mecánica: el enum desaparece y el `codeState` deja de llevar la fase.
Se resuelve en D§6.1 (pasos 6 y 7).
Tiene dos consecuencias fuera de D§6: son los únicos trámites que pueden ejercitar de verdad la fase 6 de D§9 — un `PruebaV2` con los cuerpos comentados no cambia nunca de estado —, y son por tanto la única cobertura real del riesgo R1.

---

## 1. Mapa del cambio

Inventario cerrado. Es la checklist de la implementación.

### 1.1 `EducaFlowBuildTools` (repositorio hermano, Maven)

| Ruta (desde la raíz del repo) | Acción |
|---|---|
| `src/main/java/.../files/tipoexpediente/CodeStateNaming.java` | **borrar** |
| `src/main/java/.../files/tipoexpediente/State.java` | modificar — `getCodeState()` devuelve el nombre corto; nuevo `getTitleOrHumanizedName()` |
| `src/main/java/.../files/tipoexpediente/Fase.java` | modificar — `getTitleOrHumanizedName()` y `getEventsUpperCamelCase()`; quitar del javadoc la referencia a `CodeStateNaming` |
| `src/main/java/.../common/TextUtil.java` | modificar — nuevo `humanize(String)` (D§3.6) y función Pebble `escapeJava` en `TemplateUtil` |
| `src/main/java/.../files/tipoexpediente/TipoExpedienteInstanceFileFinder.java` | modificar — `checkNombreFase`/`checkNombreEstado` propios (se van con `CodeStateNaming`) y las validaciones nuevas 8–11 |
| `src/main/java/.../createstates/Main.java` | **nuevo** |
| `src/main/java/.../createstates/StatesFile.java` | **nuevo** |
| `src/main/java/.../createstates/ProfilesDelDominio.java` | **nuevo** |
| `src/main/java/.../createstates/IdentificadoresGenerados.java` | **nuevo** |
| `src/main/resources/states.template` | **nuevo** |
| `src/main/resources/event-manager.template` | modificar — fuera los tres enums y los genéricos |
| `src/main/resources/event-manager-trigger-method.template` | modificar — `EventContext` sin genéricos |
| `src/main/resources/event-manager-onenter-method.template` | modificar — ídem |
| `src/main/resources/input-config-tipos-expedientes.template` | modificar — fuera el `<bind node="state" ...>` |
| `src/main/resources/input-config-tipos-expedientes-input-data.template` | modificar — fuera el bucle `<state>` |
| `src/main/java/.../viewprocessor/tags/Form.java` | modificar — nombre de vista con fase y estado como segmentos |
| `src/main/resources/state-event-validator*.template` | **sin cambios** |
| `src/main/java/.../files/eventmanagerfile/EventManagerFile.java` | modificar — el contexto de la plantilla pierde `states`/`events`/`profiles` |

### 1.2 `secretaria-virtual`

**Nuevos**

| Ruta (desde `src/main/java/com/educaflow/`) | Qué |
|---|---|
| `subsystem/expedientes/services/eventmanager/TipoExpedienteStates.java` | la interfaz puente de §4.4 |

**Modificados**

| Ruta | Qué |
|---|---|
| `subsystem/expedientes/services/eventmanager/Phase.java` | gana `getStates()` |
| `subsystem/expedientes/services/eventmanager/State.java` | `getCode()` en vez de `getSimpleCode()`/`getFullCode()`; `getProfile()` tipado |
| `subsystem/expedientes/services/eventmanager/EventManager.java` | un solo parámetro de tipo; despacho por string; vista con dos segmentos |
| `subsystem/expedientes/services/eventmanager/EventContext.java` | sin genéricos |
| `subsystem/expedientes/services/internal/ExpedienteLocator.java` | `getTipoExpedienteStates` cacheado; resolución por `phaseCode` |
| `subsystem/expedientes/services/internal/ExpedienteUtil.java` | `updateState(Expediente, State)` con barrera cross-tipo |
| `subsystem/expedientes/services/tramitacion/Tramitador.java` | 6 puntos (D§5.5) |
| `subsystem/expedientes/controllers/ExpedienteController.java` | `getEventContext(TipoExpediente, String)` + validación de perfil |
| `subsystem/expedientes/domains/TipoExpediente.xml` | `<extra-code>`; fuera `EstadoTipoExpediente` y la relación `estados` |
| `subsystem/expedientes/domains/Expediente.xml` | `<string name="codePhase" .../>` |
| `subsystem/expedientes/domains/HistorialEstado.xml` | ídem |
| `secretariavirtual/startup/DataBaseStartup.java` | ⚠ H1 |
| `build.gradle` | `GenerateStatesTask` + `srcDir` |
| `tramites/**/{recepcion,tramitacion}/EventManagerImpl.java` | ⚠ H2 — los 12 |

**Borrados**

| Ruta | Motivo |
|---|---|
| `subsystem/expedientes/services/internal/CodeStateNaming.java` | §7.3 |
| `subsystem/expedientes/services/internal/StateEnum.java` | §7.3 |

**Tests** (`src/test/java/com/educaflow/tiposexpedientes/`)

| Ruta | Acción |
|---|---|
| `eventmanager/EventManagerTest.java` | quitar E5 y con él el último uso de `CodeStateNaming` |
| `stateeventvalidator/StateEventValidatorTest.java` | **sin cambios** |
| `support/{TiposExpediente,Bytecode,Violacion}.java` | **sin cambios** |
| `states/StatesTest.java` | **nuevo** — concordancia `States` ↔ XML por reflexión |
| `eventmanager/ApiBaseReservadaTest.java` | **nuevo** — regla 8, familia «API heredada» |

**No se tocan**: los `views.xml` de las fases de los trámites (sus `<form state="ENTRADA_DATOS" profile="CREADOR">` ya llevan el nombre corto; lo que cambia es el nombre que compone el viewprocessor), los `StateEventValidatorImpl.kt`, y las vistas de listado de expedientes (`tramites/views/*.xml`, que solo muestran `nameState`).

---

## 2. Contratos

### 2.1 `Phase`

`subsystem/expedientes/services/eventmanager/Phase.java`

```java
package com.educaflow.subsystem.expedientes.services.eventmanager;

import java.util.List;

/**
 * Una fase de un tipo de expediente: la agrupación de estados cuyo EventManager,
 * StateEventValidator y views.xml viven juntos en la subcarpeta de la fase.
 *
 * <p>La implementa el enum privado {@code PhaseInternal} de la clase {@code States} generada de
 * cada tipo. Una fase se referencia siempre por el alias público de esa clase
 * ({@code States.RECEPCION}), tipado con esta interfaz.
 *
 * <p>No expone el tipo de expediente al que pertenece: la clase generada es una proyección pura del
 * XML, sin imports de entidades, y el camino de vuelta fase → tipo no tiene consumidor (§4.1).
 */
public interface Phase {

    /** El {@code name} de la fase en el XML, en UPPER_SNAKE_CASE: {@code "RECEPCION"}. */
    String getCode();

    /** El {@code title} del XML, o el {@code name} humanizado si no lo hay: {@code "Recepción"}. */
    String getName();

    /** Los estados de la fase, en orden de declaración. Lista inmutable, precalculada. */
    List<State> getStates();
}
```

### 2.2 `State`

`subsystem/expedientes/services/eventmanager/State.java`

```java
package com.educaflow.subsystem.expedientes.services.eventmanager;

import com.educaflow.subsystem.expedientes.db.Profile;

import java.util.Set;

/**
 * Un estado de un tipo de expediente. La implementa el enum público de su fase dentro de la clase
 * {@code States} generada, de modo que cada estado es un singleton comparable con {@code ==}.
 *
 * <p>La identidad de un estado es la pareja (fase, código): el código solo es único dentro de su
 * fase (§6.6), y por eso lo que se persiste son las dos columnas {@code codePhase} y
 * {@code codeState}.
 */
public interface State {

    Phase getPhase();

    /** El {@code name} del XML, en UPPER_SNAKE_CASE. Es lo que se persiste en {@code codeState}. */
    String getCode();

    /** El {@code title} del XML, o el {@code name} humanizado si no lo hay. Va a {@code nameState}. */
    String getName();

    /** El perfil que ve/opera el estado, o {@code null} si el estado no declara ninguno. */
    Profile getProfile();

    /** Los eventos disparables desde el estado, en orden de declaración. Conjunto inmutable. */
    Set<String> getEvents();

    boolean isInitial();

    /** El {@code closed="true"} del XML: el expediente queda cerrado al entrar aquí. */
    boolean isFinal();
}
```

Respecto de los ficheros actuales: `Phase` gana `getStates()`; `State` pierde `getSimpleCode()`/`getFullCode()` y gana `getCode()`, y su `getProfile()` pasa de `String` al enum.

### 2.3 `TipoExpedienteStates`

`subsystem/expedientes/services/eventmanager/TipoExpedienteStates.java` — **nuevo**

```java
package com.educaflow.subsystem.expedientes.services.eventmanager;

import java.util.List;
import java.util.Optional;

/**
 * La máquina de estados de UN tipo de expediente, vista sin conocer el tipo en compilación.
 *
 * <p>La única implementación es la clase {@code States} generada de cada tipo, que expone su único
 * ejemplar en {@code States.INSTANCE}. Es el puente que permite a {@code ExpedienteLocator} llegar a
 * la clase generada por reflexión de <b>clase</b>, sin reflexión de <b>métodos</b>: quien recibe un
 * {@code TipoExpedienteStates} hace llamadas normales.
 *
 * <p>Se llega a ella desde la entidad: {@code expediente.getTipoExpediente().getTipoExpedienteStates()}.
 */
public interface TipoExpedienteStates {

    /** La fase con ese código, o vacío. Comparación estricta, sin normalizar (§6.7). */
    Optional<Phase> getPhase(String phaseCode);

    /** El estado de esa fase con ese código, o vacío. Comparación estricta. */
    Optional<State> getState(String phaseCode, String stateCode);

    /** Las fases, en orden de declaración en el XML. */
    List<Phase> getPhases();

    /** TODOS los estados del tipo, de todas las fases, en orden de declaración. */
    List<State> getStates();

    /** El estado inicial. Resuelto en generación: hay exactamente uno. */
    State getInitialState();
}
```

### 2.4 El `<extra-code>` de la entidad

`subsystem/expedientes/domains/TipoExpediente.xml` — dentro de `<entity name="TipoExpediente">`, tras `<many-to-one name="tramite" .../>` y **sustituyendo** a `<one-to-many name="estados" .../>`:

```xml
        <!--
            El puente a la máquina de estados tipada del tipo de expediente (la clase States que el
            build genera desde su TipoExpedienteInstance.xml). La entidad es un objeto por tipo
            recuperado de la base de datos, así que hace de singleton polimórfico y es el punto de
            entrada cuando el tipo concreto no se conoce en compilación.

            Va en una sola línea a propósito: es Java embebido en XML, sin ayuda del IDE, y se
            inyecta sin control de imports (de ahí los FQCN). Los cinco métodos de búsqueda viven
            una sola vez, en la clase generada, y se llega a ellos por la interfaz.
        -->
        <extra-code><![CDATA[

            public com.educaflow.subsystem.expedientes.services.eventmanager.TipoExpedienteStates getTipoExpedienteStates() {
                return com.educaflow.subsystem.expedientes.services.internal.ExpedienteLocator.getTipoExpedienteStates(this);
            }

        ]]></extra-code>
```

Precedente de que una entidad llame a un servicio desde `extra-code`: `extra-code-domain-xml.template` de los build-tools ya inyecta `ExpedienteUtil.getDocumentoPdf(this, ...)` en las entidades de expediente.

**La regla que lo prohíbe existe y hay que tocarla** — comprobado, no queda como «revisar»:
`agent_docs/architecture-rules.md` §C13 («Las entidades de dominio son POJOs») dice *«ninguna [clase de `..db..`] depende de clases de `..service..` ni `..controller..`»*, y su Cumplimiento está redactado justo para este caso: *«⚠️ Previsiblemente CUMPLE …; si alguna entidad con `extra-code` referencia un servicio, pasar a ❌»*.
Dos hechos que fijan qué hacer:

- El `<extra-code>` de arriba referencia `ExpedienteLocator`, que está en `…expedientes.services.internal`. Y el precedente de `getDocumentoPdf` referencia `ExpedienteUtil`, del mismo paquete: la condición de C13 ya está incumplida **hoy**, antes de este refactor.
- El test derivado (`architecture/estructurainterna/EstructuraInternaTest.c13_entidadesDominioSonPojos`) escribe el paquete como `..service..`, en singular, y los paquetes reales del proyecto son `services`: el matcher **no** los captura, así que el test seguirá verde. Que no salte es un accidente del matcher, no una exención.

Decisión: se actualiza el **Cumplimiento** de C13 en `architecture-rules.md` documentando el incumplimiento conocido y su alcance —las entidades de expediente, cuyo `extra-code` es el punto de entrada tipado a la máquina de estados (`getTipoExpedienteStates`) y a los PDF (`getDocumentoPdf`)—, con la misma forma que ya usa C14 para su incumplimiento conocido.
**No** se toca el matcher del test para hacerlo saltar: ampliarlo a `..services..` es un cambio de alcance de la regla, ajeno a este refactor, y convertiría en rojo un test que hoy es verde por motivos que no tienen que ver con los tipos de expediente.
Va en la fase 7 (D§9), y no bloquea ninguna puerta anterior porque el test no cambia de color.

Y el enum `Profile` del mismo fichero **no cambia**: sigue siendo el global del dominio (`CREADOR`, `RESPONSABLE`, `SECRETARIO`, `DIRECTOR`, `AUDITOR`).

### 2.5 Las dos columnas nuevas

`subsystem/expedientes/domains/Expediente.xml`, justo antes de `codeState`:

```xml
        <string name="codePhase" title="Código de la fase" />
```

`subsystem/expedientes/domains/HistorialEstado.xml`, ídem.

`codeState` no cambia de declaración, solo de contenido: pasa de `F_RECEPCION_S_ENTRADA_DATOS` a `ENTRADA_DATOS`.

---

## 3. El generador

Paquete nuevo `com.educaflow.common.buildtools.createstates`, modelado sobre `createdatainittipoexpediente`, que es el generador de estructura más parecido (recorre todos los tipos, renderiza una plantilla Pebble por tipo y escribe fuera del árbol de fuentes).

### 3.1 `Main`

```java
package com.educaflow.common.buildtools.createstates;

import com.educaflow.common.buildtools.files.tipoexpediente.TipoExpedienteInstanceFile;
import com.educaflow.common.buildtools.files.tipoexpediente.TipoExpedienteInstanceFileFinder;
import com.educaflow.common.buildtools.files.tramite.TramitesLayout;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Genera la clase {@code States} de cada tipo de expediente a partir de su
 * {@code TipoExpedienteInstance.xml}.
 *
 * <p>{@code States} NO es un esqueleto: es una proyección literal del XML y se reemite entera en
 * cada build, así que no vive en {@code src/main/java} ni se versiona (§11). Por eso este generador
 * es independiente de {@code createfiles}, que es idempotente y nunca pisa lo escrito.
 *
 * <pre>
 *   Main &lt;origen&gt; &lt;destino&gt; [paqueteRaiz] &lt;domainsTipoExpedienteXml&gt;
 * </pre>
 *
 * donde {@code origen} es la raíz del árbol de fuentes ({@code ./src/main/java}), {@code destino}
 * la raíz de salida ({@code ./build/src-gen-states/main/java}) y {@code domainsTipoExpedienteXml} el
 * dominio del que sale el enum global {@code Profile} contra el que se validan los perfiles
 * (regla 11 de §10).
 */
public class Main {

    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Uso: Main <origen> <destino> <paqueteRaiz> <domainsTipoExpedienteXml>");
            System.exit(1);
        }

        Path rootPathSourceFiles = Paths.get(args[0]);
        Path rootPathDestino = Paths.get(args[1]);
        String paqueteRaizTramites = TramitesLayout.paqueteRaizFromArgs(args, 2);
        Path domainsTipoExpedienteXml = Paths.get(args[3]);

        System.out.println("Iniciando tarea de generar las clases States de los tipos de expediente....");
        System.out.println("rootPathSourceFiles=" + rootPathSourceFiles);
        System.out.println("rootPathDestino=" + rootPathDestino);

        ProfilesDelDominio profilesDelDominio = ProfilesDelDominio.leer(domainsTipoExpedienteXml);

        TramitesLayout tramitesLayout = new TramitesLayout(rootPathSourceFiles, paqueteRaizTramites);
        List<TipoExpedienteInstanceFile> tiposExpedientes =
                new TipoExpedienteInstanceFileFinder(tramitesLayout).findTiposExpedienteFile();

        for (TipoExpedienteInstanceFile tipoExpediente : tiposExpedientes) {
            System.out.println("Generando States de " + tipoExpediente.getCode()
                    + " en el paquete " + tipoExpediente.getBasePackageName());

            new StatesFile(tipoExpediente, rootPathDestino, profilesDelDominio).generateStates();
        }

        System.out.println("Finalizada tarea de generar las clases States de los tipos de expediente");
    }
}
```

El vaciado de la raíz de salida **no** lo hace el `Main`: lo hace el `doFirst` de la tarea Gradle (D§4), que es quien conoce la carpeta como output y quien tiene que borrarla antes de que Gradle calcule nada.

### 3.2 `ProfilesDelDominio`

Lee el enum global del `domains/TipoExpediente.xml` con DOM, sin JAXB: es un XML de Axelor con su propio esquema, y solo hace falta un dato.

```java
package com.educaflow.common.buildtools.createstates;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Los perfiles del enum global {@code Profile} del dominio de expedientes.
 *
 * <p>Es contra esta lista contra la que se valida el atributo {@code profile} de cada estado
 * (regla 11 de §10): restaura la garantía que hasta ahora daba el data-init de
 * {@code EstadoTipoExpediente}, que desaparece al borrar la entidad, y ataja los typos que
 * producirían una referencia {@code Profile.<X>} que no compila.
 */
public final class ProfilesDelDominio {

    private static final String NOMBRE_ENUM = "Profile";

    private final Set<String> nombres;
    private final Path fichero;

    private ProfilesDelDominio(Set<String> nombres, Path fichero) {
        this.nombres = nombres;
        this.fichero = fichero;
    }

    public static ProfilesDelDominio leer(Path domainsTipoExpedienteXml) {
        if (Files.isRegularFile(domainsTipoExpedienteXml) == false) {
            throw new RuntimeException("No existe el dominio " + domainsTipoExpedienteXml
                    + ", del que sale el enum global " + NOMBRE_ENUM + " contra el que se validan los"
                    + " perfiles de los estados.");
        }

        try {
            Element raiz = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(domainsTipoExpedienteXml.toFile()).getDocumentElement();

            Set<String> nombres = new LinkedHashSet<>();
            NodeList enums = raiz.getElementsByTagName("enum");
            for (int i = 0; i < enums.getLength(); i++) {
                Element enumElement = (Element) enums.item(i);
                if (NOMBRE_ENUM.equals(enumElement.getAttribute("name")) == false) {
                    continue;
                }
                NodeList items = enumElement.getElementsByTagName("item");
                for (int j = 0; j < items.getLength(); j++) {
                    nombres.add(((Element) items.item(j)).getAttribute("name"));
                }
            }

            if (nombres.isEmpty()) {
                throw new RuntimeException("No se encontró el enum '" + NOMBRE_ENUM + "' (o está vacío) en "
                        + domainsTipoExpedienteXml + ".");
            }

            return new ProfilesDelDominio(nombres, domainsTipoExpedienteXml);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Fallo al leer el enum " + NOMBRE_ENUM + " de " + domainsTipoExpedienteXml, ex);
        }
    }

    public boolean contiene(String profile) {
        return nombres.contains(profile);
    }

    public Set<String> getNombres() {
        return nombres;
    }

    public Path getFichero() {
        return fichero;
    }
}
```

### 3.3 `IdentificadoresGenerados` — la regla 8

Regla 8 de §10: una sola comprobación sobre los identificadores **ya compuestos** que el build va a emitir para el tipo, en vez de comparaciones por pares.
Tres familias, y cada una con sus reservados.

```java
package com.educaflow.common.buildtools.createstates;

import com.educaflow.common.buildtools.files.eventmanagerfile.EventManagerFile;
import com.educaflow.common.buildtools.files.stateeventvalidator.StateEventValidatorFile;
import com.educaflow.common.buildtools.files.tipoexpediente.Fase;
import com.educaflow.common.buildtools.files.tipoexpediente.State;
import com.educaflow.common.buildtools.files.tipoexpediente.TipoExpedienteInstanceFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Regla 8 de §10: los identificadores Java que el build va a emitir para un tipo de expediente no
 * pueden repetirse entre sí ni pisar un nombre reservado.
 *
 * <p>Se comprueban los identificadores <b>ya compuestos</b>, no los trozos: la conversión
 * UPPER_SNAKE_CASE → PascalCase no es inyectiva (con dígitos por medio {@code AB2C} y {@code AB_2C}
 * dan ambos {@code Ab2c}) y la colisión puede además <b>cruzar</b> estado y evento — el estado
 * {@code A_IN_EVENT_B} con el evento {@code C} y el estado {@code A} con el evento
 * {@code B_IN_EVENT_C} producen ambos {@code getForStateAInEventBInEventC} —, cosa que ninguna
 * comparación por pares detecta.
 *
 * <p>Queda fuera una sola parte de la regla: la reserva de los nombres de método público de las
 * clases base {@code EventManager}/{@code StateEventValidator}. El generador corre ANTES de compilar
 * secretaria-virtual y los build-tools no comparten código con el runtime, así que ahí esa lista solo
 * podría existir duplicada a mano; la comprueba un test (D§8.3).
 */
public final class IdentificadoresGenerados {

    /** Reservados de los tipos anidados que no salen de los imports de la plantilla. */
    private static final Set<String> TIPOS_RESERVADOS_PROPIOS = Set.of("States", "PhaseInternal");

    /** Reservados de los campos de States: los que la propia plantilla declara. */
    private static final Set<String> CAMPOS_RESERVADOS = Set.of("CODE", "NAME", "INSTANCE");

    private IdentificadoresGenerados() {
    }

    public static void check(TipoExpedienteInstanceFile tipo, Set<String> nombresImportadosPorLaPlantilla) {
        List<String> errores = new ArrayList<>();

        checkTiposAnidados(tipo, nombresImportadosPorLaPlantilla, errores);
        checkCampos(tipo, errores);
        checkMetodos(tipo, errores);

        if (errores.isEmpty() == false) {
            throw new RuntimeException("El tipo de expediente " + tipo.getCode() + " (" + tipo.getPath() + ")"
                    + " produce identificadores Java en conflicto:\n  - " + String.join("\n  - ", errores));
        }
    }

    /** Un tipo anidado por fase: el PascalCase de su name. */
    private static void checkTiposAnidados(TipoExpedienteInstanceFile tipo, Set<String> importados, List<String> errores) {
        Map<String, String> vistos = new LinkedHashMap<>();

        for (Fase fase : tipo.getFases()) {
            String identificador = fase.getNameUpperCamelCase();

            if (TIPOS_RESERVADOS_PROPIOS.contains(identificador) || importados.contains(identificador)) {
                errores.add("la fase '" + fase.getName() + "' produce el tipo anidado '" + identificador
                        + "', que es un nombre reservado dentro de States (la clase misma, su enum interno"
                        + " o uno de los tipos que la plantilla importa: " + importados + ")."
                        + " Un tipo anidado con ese nombre haría shadowing del import y rompería la compilación.");
            }

            String previa = vistos.put(identificador, fase.getName());
            if (previa != null) {
                errores.add("las fases '" + previa + "' y '" + fase.getName() + "' producen el mismo tipo"
                        + " anidado '" + identificador + "'.");
            }
        }
    }

    /** Un campo public static final Phase por fase, con el name tal cual. */
    private static void checkCampos(TipoExpedienteInstanceFile tipo, List<String> errores) {
        for (Fase fase : tipo.getFases()) {
            if (CAMPOS_RESERVADOS.contains(fase.getName())) {
                errores.add("la fase '" + fase.getName() + "' produce un campo de States con un nombre"
                        + " reservado (" + CAMPOS_RESERVADOS + ").");
            }
        }
        // El duplicado de name de fase ya lo cubre checkFases del finder (regla 2 de §10); los demás
        // campos de la clase (phases, statesByPhase, allStates) van en camelCase a propósito y un
        // name de fase es UPPER_SNAKE_CASE, así que no pueden colisionar (§6.3).
    }

    /**
     * Los nombres de método de esqueletos y tests, compuestos y agrupados POR FASE: cada fase tiene
     * su propio EventManagerImpl y su propio StateEventValidatorImpl, así que la colisión es dentro
     * de la fase, no del tipo.
     */
    private static void checkMetodos(TipoExpedienteInstanceFile tipo, List<String> errores) {
        for (Fase fase : tipo.getFases()) {
            Map<String, String> vistos = new LinkedHashMap<>();

            for (State state : fase.getStates()) {
                anota(vistos, EventManagerFile.getMethodNameOnEnterEvent(state.getNameUpperCamelCase()),
                        "el estado '" + state.getName() + "'", fase, errores);
            }

            for (String evento : fase.getEventsUpperCamelCase()) {
                anota(vistos, EventManagerFile.getMethodNameTriggerEvent(evento),
                        "el evento '" + evento + "'", fase, errores);
            }

            for (State state : fase.getStates()) {
                for (String evento : state.getEventsUpperCamelCase()) {
                    anota(vistos, StateEventValidatorFile.getMethodNameBeanValidationRules(
                                    state.getNameUpperCamelCase(), evento),
                            "la pareja estado '" + state.getName() + "' / evento '" + evento + "'",
                            fase, errores);
                }
            }
        }
    }

    private static void anota(Map<String, String> vistos, String metodo, String procedencia, Fase fase, List<String> errores) {
        String previa = vistos.put(metodo, procedencia);
        if (previa != null) {
            errores.add("en la fase '" + fase.getName() + "', " + previa + " y " + procedencia
                    + " producen el mismo nombre de método '" + metodo + "'.");
        }
    }
}
```

Un solo apoyo nuevo en build-tools para que esto compile:

- `Fase.getEventsUpperCamelCase()` — `TextUtil.getUpperCamelCase(getEvents())`, simétrico al de `State`.

`StateEventValidatorFile.getMethodNameBeanValidationRules(String state, String event)` **ya existe** como `public static` y es el mismo convenio que usan los tests, así que se reutiliza tal cual: el nombre lo compone un solo sitio del lado del build, que es lo que exige la nota de coherencia de §9.

### 3.4 Las validaciones 9, 10 y 11

Van en `TipoExpedienteInstanceFileFinder`, junto a las que ya hay (`checkFases`), porque son validaciones del **XML**, no de la generación: cualquier herramienta que lea el fichero debe verlas.
La 8 no puede ir ahí (necesita saber los imports de `states.template`) y por eso vive en `createstates`.

```java
    /**
     * Reglas 9 y 10 de §10, sobre los eventos de cada estado.
     *
     * <p>9: un evento da nombre a un método {@code trigger<Evento>} de los esqueletos, así que tiene
     * que ser un identificador en UPPER_SNAKE_CASE. Hasta ahora no se validaba y un evento con un
     * guion o un espacio rompía la generación con un error críptico.
     *
     * <p>10: un evento repetido en el mismo estado lo deduplicaría en silencio el LinkedHashSet de
     * la clase generada, y el build es el único sitio donde el mensaje puede señalar el fichero.
     */
    private static void checkEventos(TipoExpedienteInstanceFile tipoExpediente) {
        for (Fase fase : tipoExpediente.getFases()) {
            for (State state : fase.getStates()) {
                if (state.getEvents() == null) {
                    continue;
                }

                Set<String> vistos = new HashSet<>();
                for (String evento : state.getEvents()) {
                    checkNombreIdentificador(evento, "evento");

                    if (vistos.add(evento) == false) {
                        throw new RuntimeException("El evento '" + evento + "' está repetido en el"
                                + " atributo events del estado '" + state.getName() + "' de la fase '"
                                + fase.getName() + "'.");
                    }
                }
            }
        }
    }

    /**
     * Regla 11 de §10: el perfil de un estado tiene que ser uno del enum global Profile del dominio,
     * porque la clase generada emite una referencia {@code Profile.<PERFIL>}.
     */
    private static void checkProfiles(TipoExpedienteInstanceFile tipoExpediente, ProfilesDelDominio profiles) {
        for (Fase fase : tipoExpediente.getFases()) {
            for (State state : fase.getStates()) {
                String profile = state.getProfile();
                if ((profile == null) || (profile.isBlank())) {
                    continue;
                }
                if (profiles.contiene(profile) == false) {
                    throw new RuntimeException("El perfil '" + profile + "' del estado '" + state.getName()
                            + "' de la fase '" + fase.getName() + "' no existe en el enum Profile de "
                            + profiles.getFichero() + ". Los perfiles válidos son: " + profiles.getNombres() + ".");
                }
            }
        }
    }
```

`checkEventos` se llama desde `parseTipoExpedienteXml`, junto a `checkFases`.
`checkProfiles` necesita los perfiles del dominio, que solo tiene `createstates`, así que se expone como `public static` del finder y la llama `StatesFile`.
El `checkNombreIdentificador(String, String)` es el `checkNombre` privado que hoy vive en `CodeStateNaming`: se **mueve** al finder al borrar esa clase, y de él salen también `checkNombreFase` (ya sin la prohibición del segmento `S`, §3.2) y `checkNombreEstado`.

### 3.5 `StatesFile`

```java
package com.educaflow.common.buildtools.createstates;

import com.educaflow.common.buildtools.common.TemplateUtil;
import com.educaflow.common.buildtools.files.tipoexpediente.TipoExpedienteInstanceFileFinder;
import com.educaflow.common.buildtools.files.tipoexpediente.TipoExpedienteInstanceFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Emite el {@code States.java} de un tipo de expediente en la raíz de salida. */
public class StatesFile {

    private static final String TEMPLATE = "states.template";
    private static final String CLASS_NAME = "States";

    /** {@code import a.b.C;} → {@code C}. De aquí sale la reserva de nombres de tipo anidado. */
    private static final Pattern IMPORT = Pattern.compile("(?m)^\\s*import\\s+(?:static\\s+)?([\\w.]+)\\s*;");

    private final TipoExpedienteInstanceFile tipoExpediente;
    private final Path rootPathDestino;
    private final ProfilesDelDominio profiles;

    public StatesFile(TipoExpedienteInstanceFile tipoExpediente, Path rootPathDestino, ProfilesDelDominio profiles) {
        this.tipoExpediente = tipoExpediente;
        this.rootPathDestino = rootPathDestino;
        this.profiles = profiles;
    }

    public void generateStates() {
        try {
            // Todo lo que puede fallar, falla ANTES de escribir nada (§10).
            TipoExpedienteInstanceFileFinder.checkProfiles(tipoExpediente, profiles);
            IdentificadoresGenerados.check(tipoExpediente, nombresImportadosPorLaPlantilla());

            Map<String, Object> context = new HashMap<>();
            context.put("tipoExpediente", tipoExpediente);
            context.put("packageName", tipoExpediente.getBasePackageName());
            context.put("className", CLASS_NAME);
            context.put("newLine", "\n");

            String content = TemplateUtil.evaluateTemplate(TEMPLATE, context);

            Path destino = rootPathDestino
                    .resolve(tipoExpediente.getBasePackageName().replace('.', '/'))
                    .resolve(CLASS_NAME + ".java");

            Files.createDirectories(destino.getParent());
            // No se usa TemplateUtil.createFileWithContent: ese exige que el fichero NO exista, que
            // es la semántica de un esqueleto. States se reemite entero en cada build.
            Files.writeString(destino, content, StandardCharsets.UTF_8);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Fallo al generar el States de " + tipoExpediente.getCode(), ex);
        }
    }

    /**
     * Los nombres simples que importa la plantilla. Se derivan de la propia plantilla y no se
     * mantienen en una lista aparte: si states.template cambia sus imports, la reserva cambia sola
     * (§10, regla 8).
     */
    private Set<String> nombresImportadosPorLaPlantilla() throws Exception {
        try (InputStream in = StatesFile.class.getClassLoader().getResourceAsStream(TEMPLATE)) {
            if (in == null) {
                throw new RuntimeException("No se encuentra la plantilla " + TEMPLATE + " en el classpath.");
            }

            String plantilla = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            Set<String> nombres = new LinkedHashSet<>();
            Matcher matcher = IMPORT.matcher(plantilla);
            while (matcher.find()) {
                String fqcn = matcher.group(1);
                nombres.add(fqcn.substring(fqcn.lastIndexOf('.') + 1));
            }

            return nombres;
        }
    }
}
```

### 3.6 `states.template`

Pebble, cargada por `ClasspathLoader` con autoescape desactivado, igual que las demás.
Produce exactamente el patrón de §5 con constantes `UPPER_SNAKE_CASE`.

```
package {{ packageName }};

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
public final class {{ className }} implements TipoExpedienteStates {

    /** El code del tipo de expediente. No toca la base de datos. */
    public static final String CODE = "{{ escapeJava(input=tipoExpediente.code) }}";

    /** El name del tipo de expediente. No toca la base de datos. */
    public static final String NAME = "{{ escapeJava(input=tipoExpediente.name) }}";

    /** Único ejemplar: la única implementación de los métodos de búsqueda. */
    public static final {{ className }} INSTANCE = new {{ className }}();

    // Alias públicos de fase — la API con la que se referencia una fase desde fuera.
{% for fase in tipoExpediente.fases %}    public static final Phase {{ fase.name }} = PhaseInternal.{{ fase.name }};
{% endfor %}
    private {{ className }}() {
    }

    // =====================================================================
    // FASES — enum privado: detalle de implementación, nunca se nombra fuera
    // =====================================================================
    private enum PhaseInternal implements Phase {

{% for fase in tipoExpediente.fases %}        {{ fase.name }}("{{ escapeJava(input=fase.titleOrHumanizedName) }}"){% if loop.last %};{% else %},{{ newLine }}{% endif %}{% endfor %}

        private final String name;

        PhaseInternal(String name) {
            this.name = name;
        }

        @Override public String getCode() { return name(); }
        @Override public String getName() { return name; }
        @Override public List<State> getStates() { return statesByPhase.get(name()); }
    }
{% for fase in tipoExpediente.fases %}
    // =====================================================================
    // FASE: {{ fase.name }}
    // =====================================================================
    public enum {{ fase.nameUpperCamelCase }} implements State {

{% for state in fase.states %}        {{ state.name }}("{{ escapeJava(input=state.titleOrHumanizedName) }}", {% if state.profile is not empty %}Profile.{{ state.profile }}{% else %}null{% endif %}, {{ state.initial }}, {{ state.closed }}{% if state.events is not empty %}{% for event in state.events %}, "{{ escapeJava(input=event) }}"{% endfor %}{% endif %}){% if loop.last %};{% else %},{{ newLine }}{% endif %}{% endfor %}

        private final String name;
        private final Profile profile;
        private final boolean initial;
        private final boolean closed;
        private final Set<String> events;

        {{ fase.nameUpperCamelCase }}(String name, Profile profile, boolean initial, boolean closed, String... events) {
            this.name = name;
            this.profile = profile;
            this.initial = initial;
            this.closed = closed;
            // LinkedHashSet inmodificable: conserva el orden de declaración del XML.
            this.events = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(events)));
        }

        @Override public Phase getPhase() { return PhaseInternal.{{ fase.name }}; }
        @Override public String getCode() { return name(); }
        @Override public String getName() { return name; }
        @Override public Profile getProfile() { return profile; }
        @Override public Set<String> getEvents() { return events; }
        @Override public boolean isInitial() { return initial; }
        @Override public boolean isFinal() { return closed; }
    }
{% endfor %}
    // =====================================================================
    // BÚSQUEDA (BD -> jerarquía tipada)
    // =====================================================================
    // Los campos privados van en camelCase a propósito: los alias de fase son campos
    // UPPER_SNAKE_CASE de esta misma clase, así que con minúsculas por medio ningún name de
    // fase puede colisionar con ellos.

    private static final List<Phase> phases = List.of(PhaseInternal.values());

    private static final Map<String, List<State>> statesByPhase = Map.ofEntries(
{% for fase in tipoExpediente.fases %}        Map.entry("{{ fase.name }}", List.<State>of({{ fase.nameUpperCamelCase }}.values())){% if not loop.last %},{{ newLine }}{% endif %}{% endfor %}

    );

    // TODOS los estados del tipo, en orden de declaración. Se recorre phases, no statesByPhase,
    // porque Map.ofEntries no especifica su orden de iteración.
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
        return {{ tipoExpediente.initialState.fase.nameUpperCamelCase }}.{{ tipoExpediente.initialState.name }};
    }
}
```

Tres detalles de la plantilla que hay que respetar al escribirla:

- El `name` del tipo puede llevar comillas o barras invertidas (sale del `TramiteInstance.xml`).
  `CODE`/`NAME` se emiten dentro de literales Java, así que hay que escaparlos; lo mismo para los `title` de fases y estados, que son texto libre con acentos.
  Se añade una función Pebble `escapeJava` al `TemplateUtil` (junto a `escapeXml`, que ya existe con el mismo motivo, y con su misma forma de invocación `escapeJava(input=...)`), y la plantilla de arriba **ya la usa** en los cinco sitios que emiten un literal: `CODE`, `NAME`, el `title` de la fase, el `title` del estado y cada evento.
  En los eventos es un no-op mientras la regla 9 de §10 los obligue a ser identificadores `UPPER_SNAKE_CASE`; va igualmente para que ningún literal de la plantilla dependa de que otra validación siga vigente.
- `state.events` es `null` cuando el atributo `events` falta del todo (JAXB no llama al adaptador), y lista vacía cuando es `events=""` (el `CommaSeparatedAdapter` ya lo trata).
  El `{% if state.events is not empty %}` cubre los dos casos.
- `titleOrHumanizedName` es el getter nuevo de `State` y `Fase`: el `title` si lo hay, y si no el `name` humanizado.
  ⚠ El `TextUtil` de build-tools **no tiene** `humanize`; el del runtime lo delega en `Inflector` de Axelor, que build-tools no tiene en el classpath (y meterlo por esto no compensa).
  Hay que añadir a `com.educaflow.common.buildtools.common.TextUtil` un `humanize(String)` propio que reproduzca el resultado de `Inflector` sobre UPPER_SNAKE_CASE — guiones bajos a espacios, todo a minúsculas y primera letra a mayúscula: `ENTRADA_DATOS` → `"Entrada datos"` —, porque §2 exige coste funcional cero y ese es exactamente el `nameState` que hoy calcula el runtime para un estado sin `title`.
  Es el único sitio del refactor donde una diferencia de implementación cambiaría un texto visible para el usuario.

### 3.7 Cambios en las plantillas de esqueleto

`event-manager.template`: desaparece todo el bloque «Máquina de Estados del expediente» (los tres enums), cambia la declaración de la clase y el `super(...)`:

```
public class {{eventManagerClassName}} extends com.educaflow.subsystem.expedientes.services.eventmanager.EventManager<{{ code }}> {
...
    public {{eventManagerClassName}}({{ code }}Repository repository) {
        super({{ code }}.class);
        this.repository = repository;
    }
```

y todos los `EventContext<Profile,State>` pasan a `EventContext`, aquí y en `event-manager-trigger-method.template` / `event-manager-onenter-method.template`.
El generador emite además, junto a los imports actuales, `import {{ basePackageName }}.States;` — que es lo que hace que el esqueleto recién creado ya pueda escribir `eventContext.updateState(States.Fase.ESTADO)`.

`EventManagerFile.createEventManagerFile` deja de poner en el contexto `states`, `events` y `profiles` (ya no los usa nadie) y añade `basePackageName`.
`caseStates` y `caseEvents` se quedan: siguen siendo los métodos de la fase.

`input-config-tipos-expedientes.template` pierde el bloque

```xml
            <bind node="state" to="estados"  create="true" update="true">
                <bind node="@codeState" to="codeState" />
                <bind node="@profile" to="profile" search="self.code = :profile"  />
            </bind>
```

y `input-config-tipos-expedientes-input-data.template` pierde el `{% for state in tipoExpediente.states %}` entero, quedándose el `<tipoExpediente .../>` como elemento vacío.

### 3.8 `viewprocessor/tags/Form.java`

Sustituir el uso de `CodeStateNaming` por los dos segmentos:

```java
            formElement.setAttribute("name", getFormName(nombreExpediente, fase.getName(), state, profile));
```

```java
    /**
     * El nombre global de la vista. Debe casar con VIEW_NAME_STATE_PROFILE_FORMAT y
     * VIEW_NAME_STATE_FORMAT de {@code EventManager}, en secretaria-virtual.
     */
    private static String getFormName(String nombreExpediente, String phaseCode, String stateCode, String profile) {
        if ((profile == null) || (profile.trim().isEmpty())) {
            return "exp-" + nombreExpediente + "-" + phaseCode + "-" + stateCode + "-form";
        } else {
            return "exp-" + nombreExpediente + "-" + phaseCode + "-" + stateCode + "-" + profile + "-form";
        }
    }
```

`checkEstadoDeLaFase` no cambia: sigue exigiendo que el `state=` del XML sea un estado de la propia fase.

---

## 4. Cableado de Gradle

En `build.gradle`, tras el bloque de `RichDomainXmlTask`:

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
    // domains/TipoExpediente.xml del que sale el enum global Profile (sin él, renombrar un perfil
    // dejaría la tarea up-to-date y esa validación sin re-ejecutar). Los outputs van a una raíz
    // PROPIA (no build/src-gen) para no solapar con lo que escriben generateCode y
    // RichDomainClassTask, que confundiría los up-to-date checks de Gradle.
    inputs.files(fileTree(layout.projectDirectory.dir('src/main/java/' + paqueteRaizTramites.replace('.', '/'))) { include '**/TipoExpedienteInstance.xml' })
    inputs.file('src/main/java/com/educaflow/subsystem/expedientes/domains/TipoExpediente.xml')
    outputs.dir(layout.buildDirectory.dir('src-gen-states/main/java'))
}
sourceSets.main.java.srcDir(layout.buildDirectory.dir('src-gen-states/main/java'))
tasks.named('compileJava')   { dependsOn tasks.GenerateStatesTask }
tasks.named('compileKotlin') { dependsOn tasks.GenerateStatesTask }
```

Por qué así:

- **`dependsOn` sobre las tareas de compilación**, y no `generateCode { finalizedBy ... }` como hace `RichDomainClassTask`: un finalizador solo garantiza ejecutarse *después* de la tarea finalizada, no *antes* de la compilación.
- **Raíz propia** `build/src-gen-states/main/java` en vez de `build/src-gen`: ahí escriben ya `generateCode` (Axelor) y `RichDomainClassTask`, y dos tareas con outputs solapados desactivan los up-to-date checks de Gradle. Con raíz propia no hay solape que declarar ni orden que imponer.
- **No depende de `generateCode`**: solo lee XML de `src/main/java`. El `States` generado sí *referencia* la entidad `Profile` que produce `generateCode`, pero eso lo resuelve `compileJava`, que ya depende de las dos.
- Esto **no** rompe la regla de que el build no escriba en `src/main/java`: sigue sin hacerlo.

---

## 5. Runtime, fichero a fichero

### 5.1 `EventManager`

```java
public abstract class EventManager<T extends Expediente> {

    final private String VIEW_NAME_STATE_PROFILE_FORMAT = "exp-${EXPEDIENT_CODE}-${PHASE_CODE}-${STATE_CODE}-${PROFILE_CODE}-form";
    final private String VIEW_NAME_STATE_FORMAT = "exp-${EXPEDIENT_CODE}-${PHASE_CODE}-${STATE_CODE}-form";

    private final Class<T> modelClass;

    public EventManager(Class<T> modelClass) {
        this.modelClass = modelClass;
    }
```

- `triggerInitialEvent(T, EventContext)` y `onEnterState(T, EventContext)`: mismo cuerpo, `EventContext` sin genéricos.
- `triggerEvent(String strEvent, T expediente, T expedienteOriginal, EventContext eventContext)`: el nombre del método sale del propio string.

```java
            String methodName = "trigger" + CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, strEvent);
```

  Desaparecen el `Enum.valueOf(eventClass, strEvent)` y su `catch` con fallback a `CommonEvent`.
  Ese fallback era código muerto: `DELETE` va declarado en los `events` del XML de los estados que lo permiten y `EXIT` lo corta `ExpedienteController` antes de llegar a `Tramitador`.
  Que el evento sea disparable desde el estado ya lo ha comprobado `Tramitador` contra `State.getEvents()`.
- `onEnterState`: el nombre sale de `expediente.getCodeState()`, que ya es corto.

```java
    public void onEnterState(T expediente, EventContext eventContext) {
        String codeState = expediente.getCodeState();
        try {
            String methodName = "onEnter" + CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, codeState);
            Method method = ReflectionUtil.getMethod(this.getClass(), methodName, void.class, OnEnterState.class,
                    new Class<?>[]{modelClass, EventContext.class});

            method.invoke(this, expediente, eventContext);
        } catch (Exception ex) {
            throw new RuntimeException("Error al invocar el state: " + codeState, ex);
        }
    }
```

- `getViewName(T, EventContext)`: interpola tres cosas y ya no resuelve ninguna constante de enum.

```java
    private String interpolateViewName(String template, String tipoExpedienteCode, Expediente expediente, Profile profile) {
        return template.replace("${EXPEDIENT_CODE}", tipoExpedienteCode)
                .replace("${PROFILE_CODE}", profile.name())
                .replace("${PHASE_CODE}", expediente.getCodePhase())
                .replace("${STATE_CODE}", expediente.getCodeState());
    }
```

- Desaparecen `getStateClass()`, `getEventClass()` y `getProfileClass()`; `getModelClass()` se queda.
- El import de `CodeStateNaming` y el de `CommonEvent` se van del fichero. `CommonEvent` **no se borra**: sigue siendo la fuente de los strings `DELETE`/`EXIT` que usan `Tramitador` y `ExpedienteController`.

### 5.2 `EventContext`

```java
public class EventContext {

    final private Expediente expediente;
    final private Profile profile;                // com.educaflow.subsystem.expedientes.db.Profile
    final private Centro centro;
    ...
    public Profile getProfile() { return profile; }

    public void updateState(State state) {        // ...services.eventmanager.State
        ExpedienteUtil.updateState(expediente, state);
    }
```

El resto del fichero (registros de entrada/salida, anexos) no cambia.
Consecuencia mecánica pero masiva: **todos** los `trigger*`/`onEnter*` de los 12 `EventManagerImpl` pasan de `EventContext<Profile,State>` a `EventContext` (D§6).
La barrera de compilación que impedía pasar a `updateState` un `State` de otro tipo de expediente se pierde aquí y se restaura en runtime en D§5.4.

### 5.3 `ExpedienteLocator`

```java
public class ExpedienteLocator {

    private static final String CLASE_EVENT_MANAGER = "EventManagerImpl";
    private static final String CLASE_STATE_EVENT_VALIDATOR = "StateEventValidatorImpl";
    private static final String CLASE_STATES = "States";

    /** El INSTANCE de la clase States de cada tipo, por basePackageName. Se resuelve en cada tramitación. */
    private static final ConcurrentHashMap<String, TipoExpedienteStates> STATES_POR_PAQUETE = new ConcurrentHashMap<>();

    /**
     * La máquina de estados tipada del tipo de expediente. Es a lo que delega el
     * {@code getTipoExpedienteStates()} de la entidad.
     *
     * <p>La reflexión acaba aquí: quien recibe el {@link TipoExpedienteStates} hace llamadas
     * normales, sin {@code Method}.
     */
    public static TipoExpedienteStates getTipoExpedienteStates(TipoExpediente tipoExpediente) {
        String basePackageName = getBasePackageName(tipoExpediente);

        return STATES_POR_PAQUETE.computeIfAbsent(basePackageName, ExpedienteLocator::cargarStates);
    }

    private static TipoExpedienteStates cargarStates(String basePackageName) {
        String fqcn = basePackageName + "." + CLASE_STATES;
        try {
            Object instance = Class.forName(fqcn).getField("INSTANCE").get(null);

            return (TipoExpedienteStates) instance;
        } catch (Exception ex) {
            throw new RuntimeException("No se ha podido obtener el INSTANCE de " + fqcn + ", que es la clase"
                    + " que el build genera desde el TipoExpedienteInstance.xml del tipo. Si la clase no"
                    + " existe, es que GenerateStatesTask no ha corrido o que el basePackageName del tipo"
                    + " no corresponde a ninguna carpeta de versión.", ex);
        }
    }

    /** El {@code EventManager} de la fase. */
    public static EventManager getEventManager(TipoExpediente tipoExpediente, String phaseCode) { ... }

    /** El {@code StateEventValidator} de la fase. */
    public static StateEventValidator getStateEventValidator(TipoExpediente tipoExpediente, String phaseCode) { ... }

    public static Class<EventManager> getClaseEventManager(TipoExpediente tipoExpediente, String phaseCode) { ... }

    public static Class<StateEventValidator> getClaseStateEventValidator(TipoExpediente tipoExpediente, String phaseCode) { ... }

    /**
     * El {@code EventManager} de la fase del estado inicial, que es el que atiende la creación del
     * expediente: cuando se dispara el evento inicial todavía no hay estado del que partir.
     */
    public static EventManager getEventManagerDelEstadoInicial(TipoExpediente tipoExpediente) {
        State initialState = getTipoExpedienteStates(tipoExpediente).getInitialState();

        return getEventManager(tipoExpediente, initialState.getPhase().getCode());
    }

    /** La clase de la entidad del tipo de expediente, que es la misma en todas sus fases. */
    public static Class getModelClass(TipoExpediente tipoExpediente) {
        return getEventManagerDelEstadoInicial(tipoExpediente).getModelClass();
    }

    private static String getFqcn(TipoExpediente tipoExpediente, String phaseCode, String simpleClassName) {
        // Locale.ROOT a propósito: con la JVM en locale turco una fase con I minusculizaría a 'ı' y
        // el Class.forName fallaría con un ClassNotFoundException desconcertante sobre un paquete
        // que sí existe.
        return getBasePackageName(tipoExpediente) + "." + phaseCode.toLowerCase(Locale.ROOT) + "." + simpleClassName;
    }
```

- `getBasePackageName(TipoExpediente)` es el trozo que hoy está inline en `getFqcn` (con su comprobación de vacío y su mensaje sobre el data-init), extraído porque ahora lo usan dos caminos.
- **Desaparecen** `getCualquierCodeState(...)` — el único lector de la relación `estados`, cuya desaparición es lo que permite borrar `EstadoTipoExpediente` (§7.4) — y `getEventManagerDeCualquierFase(...)`, cuyo único uso real era `getModelClass`.
- El javadoc de clase se reescribe: la resolución ya no es «en función del estado» sino **en función de la fase**, que ahora viaja en su propia columna.

### 5.4 `ExpedienteUtil.updateState`

Es el punto más delicado del refactor.

```java
    public static void updateState(Expediente expediente, State state) {
        if (state == null) {
            throw new IllegalArgumentException("El state no puede ser nulo.");
        }

        String phaseCode = state.getPhase().getCode();
        String stateCode = state.getCode();

        // Barrera cross-tipo: los State generados son singletons, así que el estado de ESTE tipo de
        // expediente con estos códigos debe ser el mismo objeto que el recibido. Sin genéricos en
        // EventContext, un State de otro tipo compila, y el caso es realista: crear una versión
        // nueva es duplicar la carpeta de la anterior, y las dos tienen una clase llamada States.
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

Tres cambios de fondo respecto de hoy:

1. El corte por «no ha cambiado» pasa a ser sobre la **pareja** (fase, estado): con códigos cortos, `ENTRADA_DATOS` en dos fases distintas ya no es el mismo estado.
   El orden importa: se compara *antes* de escribir nada, igual que hoy.
2. `nameState` sale de `State.getName()` — el `title` del XML si lo hay — en vez de humanizar el código.
   Mejora colateral: `RESOLVER_PERMITIR_COMISION` deja de mostrarse como «Resolver permitir comision».
3. El check de identidad es una búsqueda lineal sobre listas minúsculas, y va contra el `INSTANCE` cacheado del locator, así que su coste por cambio de estado es despreciable.

`StateEnum` desaparece del fichero (y del proyecto), y con él el import de `CodeStateNaming`.
El resto de `ExpedienteUtil` (`getDocumentoPdf`, `getExpedienteFromIdExpediente`, `getJpaRepository`) no cambia.

### 5.5 `Tramitador`

Seis puntos:

1. **`triggerInitialEvent`** — el estado inicial sale de la entidad, tipado:

```java
            EventManager eventManager = ExpedienteLocator.getEventManagerDelEstadoInicial(tipoExpediente);
            JpaRepository<Expediente> expedienteRepository = JpaRepository.of(eventManager.getModelClass());
            State initialState = tipoExpediente.getTipoExpedienteStates().getInitialState();
            ...
            ExpedienteUtil.updateState(expediente, initialState);
```

   Desaparece `StateEnum.getInitialState(eventManager.getStateClass())`, que era el último uso de `getStateClass()`.
   (De paso se corrige el nombre de la variable: hoy se llama `initialEvent` y es un estado.)

2. **`triggerEvent`** — el estado actual se resuelve desde la entidad y la fase entra en las tres resoluciones del locator:

```java
        String codePhaseOrigen = expediente.getCodePhase();
        EventManager eventManager = ExpedienteLocator.getEventManager(tipoExpediente, codePhaseOrigen);
        ...
        StateEventValidator stateEventValidator = ExpedienteLocator.getStateEventValidator(tipoExpediente, codePhaseOrigen);
        ...
        State state = tipoExpediente.getTipoExpedienteStates()
                .getState(codePhaseOrigen, expediente.getCodeState())
                .orElseThrow(() -> new RuntimeException("El estado '" + codePhaseOrigen + "/"
                        + expediente.getCodeState() + "' no existe en el tipo de expediente "
                        + tipoExpediente.getCode() + "."));

        if (state.getEvents().contains(eventName) == false) {
            throw new RuntimeException("El evento '" + eventName + "' no es válido para el estado '"
                    + codePhaseOrigen + "/" + expediente.getCodeState() + "'");
        }
```

   Y la resolución del destino, después del evento:

```java
            EventManager eventManagerDestino = ExpedienteLocator.getEventManager(tipoExpediente, expediente.getCodePhase());
```

3. **`addHistorialEstado`** — guarda las dos columnas y ya no humaniza nada.
   Corre siempre después de `ExpedienteUtil.updateState`, así que se copia del propio expediente sin volver a resolver el `State`:

```java
        historialEstado.setCodePhase(expediente.getCodePhase());
        historialEstado.setCodeState(expediente.getCodeState());
        historialEstado.setNameState(expediente.getNameState());
```

4. **`getEstadoUpperCamelCase(codeState)`** — deja de pasar por `CodeStateNaming.getEstado`; el `codeState` ya es corto:

```java
    private static String getEstadoUpperCamelCase(String codeState) {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, codeState);
    }
```

5. **`validateChild`** — su resolución del validator pasa el `codePhase`:

```java
        StateEventValidator stateEventValidator = ExpedienteLocator.getStateEventValidator(tipoExpediente, expediente.getCodePhase());
```

   Los nombres de los métodos de validación no cambian: ya usaban el nombre corto.

6. **`assertValidState(...)`** — hoy es código muerto (nadie lo llama) y con este cambio deja de compilar tal cual. **Borrarlo**, junto con los imports de `Arrays` y `StateEnum` que quedan huérfanos.

### 5.6 `ExpedienteController`

```java
    /**
     * El contexto del evento. El perfil viene de la petición del cliente, así que además de parsearlo
     * contra el enum global hay que comprobar que es uno de los perfiles que USA este tipo de
     * expediente: el enum global acepta perfiles que el tipo no tiene, y getViewName tiene fallback a
     * la vista sin perfil, de modo que sin esta comprobación una petición con un perfil ajeno podría
     * llegar a renderizar una vista en vez de fallar. Es la detección que hasta ahora daba el
     * Enum.valueOf sobre el enum Profile por tipo.
     */
    private EventContext getEventContext(Expediente expediente, TipoExpediente tipoExpediente, String profileName) {
        Profile profile;
        try {
            profile = Profile.valueOf(profileName);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("El perfil '" + profileName + "' no existe.", ex);
        }

        checkProfileDelTipoExpediente(profile, tipoExpediente);

        Centro centro = getCentroFromCurrentUser();

        return new EventContext(expediente, profile, centro);
    }

    private static void checkProfileDelTipoExpediente(Profile profile, TipoExpediente tipoExpediente) {
        for (State state : tipoExpediente.getTipoExpedienteStates().getStates()) {
            if (profile.equals(state.getProfile())) {
                return;
            }
        }

        throw new RuntimeException("El perfil '" + profile.name() + "' no lo usa ningún estado del tipo de"
                + " expediente " + tipoExpediente.getCode() + ".");
    }
```

- El método deja de ser `public` y genérico: nadie fuera lo llama.
- **Cambia de parámetro**: recibía el `eventManager` solo para `getProfileClass()`, que desaparece.
  En `triggerInitialEvent` se le pasa el `TipoExpediente` del trámite; en `triggerEvent` y `viewExpediente`, el del expediente.
- Las dos llamadas a `ExpedienteLocator.getEventManager(...)` de `triggerEvent` y `viewExpediente` pasan `expediente.getCodePhase()`.
- Esta validación es lo que da a `getStates()` su consumidor en el runtime; `getPhase`/`getPhases` y `States.CODE`/`States.NAME` se quedan sin consumidor a propósito, por completitud de la API (§4.2).

Es también una defensa de `k-secure-coding`: `profileName` es un dato del cliente y no puede dictar un perfil que el tipo no contempla.

### 5.7 ⚠ `DataBaseStartup` (hueco H1)

```java
        Set<String> tablasIncluidas = Set.of("expedientes_estado_tipo_expediente");
```

`expedientes_estado_tipo_expediente` es la tabla de la entidad que se borra.
Queda:

```java
        Set<String> tablasIncluidas = Set.of();
```

y `BulkTables.truncateTables` sigue recibiendo el mismo número de argumentos.
Si `Set.of()` vacío le resultase problemático a esa utilidad, se comprueba al hacer el cambio; el reseteo de BD (`agent_docs/deploy.md`) es la puerta de verificación de este punto.

---

## 6. Migración de los 12 `EventManagerImpl` (⚠ huecos H2 y H4)

Los 12 ficheros, todos con la misma receta.
Los pasos 1–5 y 8 son mecánicos en los 12; los pasos 6 y 7 solo tienen trabajo en los 4 que llevan lógica real (⚠ H4), marcados con ⚠ en la tabla:

| # | Tipo de expediente | Fases | Entidad |
|---|---|---|---|
| 1–2 | `tramites/prueba/v1` | `recepcion`, `tramitacion` | `PruebaV1` |
| 3–4 | `tramites/prueba/v2` | `recepcion`, `tramitacion` | `PruebaV2` |
| 5–6 | `tramites/certificado_tutor/v1` | `recepcion`, `tramitacion` | `CertificadoTutorV1` |
| 7–8 | `tramites/certificado_tutor/abstractsimplesolicitudresolucion` | `recepcion`, `tramitacion` | (según su `code`) |
| 9–10 ⚠ | `tramites/profesores/justificacion_falta_profesorado/actual/v1` | `recepcion`, `tramitacion` | `JustificacionFaltaProfesoradoV1` |
| 11–12 ⚠ | `tramites/profesores/justificacion_falta_profesorado/futuro/v2` | `recepcion`, `tramitacion` | (según su `code`) |

Los 12 `StateEventValidatorImpl.kt` **no se tocan**.

⚠ Los cuatro marcados son los del hueco H4: 18 `updateState` reales entre los cuatro, y un `State.valueOf` + `switch` en cada uno de los dos `recepcion`.
Los otros ocho tienen todas las referencias a estados comentadas.

### 6.1 Receta

1. Añadir `import <basePackageName>.States;`.
2. Cambiar la declaración de la clase: `EventManager<Entidad, EventManagerImpl.State, EventManagerImpl.Event, EventManagerImpl.Profile>` → `EventManager<Entidad>`.
3. Cambiar el `super(...)`: `super(Entidad.class, State.class, Event.class, Profile.class)` → `super(Entidad.class)`.
4. En **todas** las firmas, `EventContext<Profile,State>` → `EventContext`.
5. Borrar el bloque entero «Máquina de Estados del expediente» (los tres enums y su comentario).
6. Reescribir las referencias a estados del cuerpo de los métodos: `State.F_TRAMITACION_S_ACEPTADO` → `States.Tramitacion.ACEPTADO`.
   El enum destino sale del prefijo `F_<fase>_S_` de la constante vieja, **no** de la fase del fichero: el enum `State` de hoy lleva todos los estados del tipo, así que un `EventManagerImpl` de `recepcion` referencia estados de `tramitacion` con normalidad (`actual/v1/recepcion/EventManagerImpl.java:107` hace `updateState(State.F_TRAMITACION_S_PENDIENTE_RESOLUCION)`, que pasa a `States.Tramitacion.PENDIENTE_RESOLUCION`).
   ⚠ H4: en 8 ficheros estas referencias están comentadas y el paso es casi vacío; en los 4 de `justificacion_falta_profesorado` son 18 llamadas reales.
7. ⚠ H4 — traducir el patrón `State.valueOf(...)` + `switch`, que está en los dos `recepcion` de `justificacion_falta_profesorado` (método `triggerBack`) y no tiene equivalente mecánico:

```java
// antes
State state = State.valueOf(justificacionFaltaProfesorado.getCodeState());
switch (state) {
    case F_RECEPCION_S_PENDIENTE_PRESENTACION:
        eventContext.updateState(State.F_RECEPCION_S_ENTRADA_DATOS);
        break;
    ...
    default:
        throw new IllegalArgumentException("State no reconocido: " + state);
}

// después
State state = States.INSTANCE
        .getState(justificacionFaltaProfesorado.getCodePhase(), justificacionFaltaProfesorado.getCodeState())
        .orElseThrow(() -> new IllegalArgumentException("State no reconocido: "
                + justificacionFaltaProfesorado.getCodePhase() + "/" + justificacionFaltaProfesorado.getCodeState()));

switch (state) {
    case States.Recepcion.PENDIENTE_PRESENTACION:
        eventContext.updateState(States.Recepcion.ENTRADA_DATOS);
        break;
    ...
    default:
        throw new IllegalArgumentException("State no reconocido: " + state);
}
```

   Dos cosas que hacen que esto funcione y conviene no re-derivar:

   - `State.valueOf(codeState)` desaparece porque el `codeState` ya no lleva la fase: hacen falta las dos columnas, y como el tipo se conoce en compilación se resuelve con `States.INSTANCE.getState(...)` (§15, ejemplo 4), que es justo el «caso raro» que ese método existe para cubrir.
   - El `switch` sobre una variable tipada con la **interfaz** `State` sigue siendo legal: desde Java 21 (JEP 441) las etiquetas `case` admiten constantes de enum cualificadas aunque el selector no sea un enum, y el proyecto compila con `languageVersion = 21` (`build.gradle`). Verificado compilando el patrón con dos enums anidados distintos.
     Como `State` **no** es `sealed`, el `default` es obligatorio — que es lo que ya hacen los dos métodos.
8. Referencias a perfiles: `Profile.CREADOR` sigue compilando **tal cual** si se añade `import com.educaflow.subsystem.expedientes.db.Profile;`, porque el enum global tiene los mismos nombres de constante.

### 6.2 Ejemplo trabajado: `tramites/prueba/v2/recepcion/EventManagerImpl.java`

Uno de los 8 mecánicos: sin cuerpos, así que los pasos 6 y 7 no tienen nada que hacer en él.

```java
package com.educaflow.tramites.prueba.v2.recepcion;

import com.axelor.inject.Beans;
import com.educaflow.subsystem.expedientes.services.eventmanager.EventContext;
import com.educaflow.subsystem.expedientes.services.eventmanager.OnEnterState;
import com.educaflow.subsystem.expedientes.services.eventmanager.WhenEvent;
import com.educaflow.subsystem.expedientes.db.PruebaV2;
import com.educaflow.subsystem.expedientes.db.repo.PruebaV2Repository;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.tramites.prueba.v2.States;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EventManagerImpl extends com.educaflow.subsystem.expedientes.services.eventmanager.EventManager<PruebaV2> {

    private final PruebaV2Repository repository;
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    public EventManagerImpl(PruebaV2Repository repository) {
        super(PruebaV2.class);
        this.repository = repository;
    }

    @Override
    public void triggerInitialEvent(PruebaV2 pruebaV2, EventContext eventContext) throws BusinessException {


    }


    @WhenEvent
    public void triggerDelete(PruebaV2 pruebaV2, PruebaV2 original, EventContext eventContext) throws BusinessException {
        //eventContext.updateState(States.Recepcion.);
    }

    @WhenEvent
    public void triggerPresentar(PruebaV2 pruebaV2, PruebaV2 original, EventContext eventContext) throws BusinessException {
        //eventContext.updateState(States.Recepcion.);
    }

    @WhenEvent
    public void triggerBack(PruebaV2 pruebaV2, PruebaV2 original, EventContext eventContext) throws BusinessException {
        //eventContext.updateState(States.Recepcion.);
    }

    @WhenEvent
    public void triggerPresentarDocumentosFirmados(PruebaV2 pruebaV2, PruebaV2 original, EventContext eventContext) throws BusinessException {
        //eventContext.updateState(States.Recepcion.);
    }


/***************************************************************************************/
/*************************************** Estados ***************************************/
/***************************************************************************************/

    @OnEnterState
    public void onEnterEntradaDatos(PruebaV2 pruebaV2, EventContext eventContext) {

    }

    @OnEnterState
    public void onEnterFirmaPorUsuario(PruebaV2 pruebaV2, EventContext eventContext) {

    }

}
```

Nótese que el fichero resultante es **exactamente** lo que emitiría la plantilla modificada de D§3.7, que es el invariante que exige §9.

---

## 7. Datos

### 7.1 Borrado de `EstadoTipoExpediente`

En `subsystem/expedientes/domains/TipoExpediente.xml`:

- Fuera `<one-to-many name="estados" ref="EstadoTipoExpediente" mappedBy="tipoExpediente" />` de la entidad `TipoExpediente` (la sustituye el `<extra-code>` de D§2.4).
- Fuera la `<entity name="EstadoTipoExpediente">` entera.
- El `<enum name="Profile">` se queda: es el enum global.

Único consumidor de la entidad en todo el repositorio: `ExpedienteLocator.getCualquierCodeState`, que este mismo refactor elimina (D§5.3).
Ni la seguridad, ni las vistas, ni ningún otro código o XML la consultan — comprobado con búsqueda sobre `.java`, `.kt` y `.xml`.

### 7.2 Data-init

Los cambios están en las dos plantillas de build-tools (D§3.7).
`createdatainittipoexpediente` no cambia de estructura: sigue emitiendo `input-config.xml` + `<Code>-data.xml` por tipo, solo que el segundo se queda con el `<tipoExpediente .../>` sin hijos.

### 7.3 Migración de las filas existentes (⚠ hueco H3)

**Camino primario: resetear la base de datos en desarrollo** (`agent_docs/deploy.md`).
Es lo que corresponde: el refactor cambia el formato de `codeState` de todas las filas y borra una tabla, y no hay datos de producción.

**Camino alternativo, solo si hubiera datos que conservar.**
Sería la **primera** migración Flyway del proyecto: `DataBaseStartup` ya tiene Flyway configurado con `locations("classpath:com/educaflow/secretariavirtual/startup/database")` y `baselineOnMigrate(true)`, pero esa carpeta no existe y no hay ningún `.sql` en el repositorio.
El fichero iría en `src/main/java/com/educaflow/secretariavirtual/startup/database/V1__tipos_expediente_tipados.sql` (la tarea `copySQL` de `build.gradle` copia `**/*.sql` de `src/main/java` a `build/resources/main` conservando la estructura, así que acaba en la ruta de classpath que Flyway espera):

```sql
-- Comprobar los nombres reales de tabla y columna antes de ejecutar.
UPDATE expedientes_expediente
   SET code_phase = substring(code_state from '^F_(.*?)_S_'),
       code_state = substring(code_state from '^F_.*?_S_(.*)$')
 WHERE code_state LIKE 'F\_%\_S\_%';

UPDATE expedientes_historial_estado
   SET code_phase = substring(code_state from '^F_(.*?)_S_'),
       code_state = substring(code_state from '^F_.*?_S_(.*)$')
 WHERE code_state LIKE 'F\_%\_S\_%';

DROP TABLE IF EXISTS expedientes_estado_tipo_expediente;
```

La migración **no** recalcula `nameState`: las filas ya persistidas conservan el texto humanizado antiguo (sin acentos), y la mejora del `title` se ve desde el siguiente cambio de estado de cada expediente.
Ojo al orden: Axelor crea las columnas nuevas al arrancar (`hbm2ddl`) y Flyway corre en `DataBaseStartup.startup()`; hay que verificar cuál de los dos va primero antes de fiarse de este camino.

---

## 8. Tests

Todos en `src/test/java/com/educaflow/tiposexpedientes`, **escritos a mano**: los `.java` son la fuente de verdad (`CLAUDE.md`).
**MUST NOT** crear un `agent_docs/*-rules.md` ni un skill generador para ellos.

### 8.1 Lo que no cambia

`EventManagerTest` E0–E4 y `StateEventValidatorTest` entero.
La comprobación de «un método por evento / por estado / por pareja estado-evento de la fase» siempre usó el nombre corto.
La firma que esperan tampoco cambia en la práctica: el tipo crudo del parámetro sigue siendo `EventContext`, y los genéricos que desaparecen son invisibles para `ClassFileImporter`, que lee los parámetros ya con *erasure*.
Los tres `support/` tampoco cambian.

### 8.2 Lo que desaparece

**E5** (`e5_elEnumStateTieneTodosLosEstadosDelTipo`) y su ayuda `pistaNombreCorto`: ese enum ya no lo escribe nadie a mano.
Con ellos se va el único uso que los tests hacen del `CodeStateNaming` de build-tools (`isCodeState`), que es justo lo que permite borrar la clase (§7.3).

### 8.3 `states/StatesTest.java` — nuevo

Verifica que la clase `States` generada de cada tipo concuerda con su XML: fases (código, nombre y orden), estados (código, nombre, orden, `initial`, `closed`, perfil y eventos) y el estado inicial.

A diferencia de los otros dos, **no** usa `ClassFileImporter`: ArchUnit expone estructura (tipos, miembros, firmas), y los valores por constante — `initial`, `closed`, el perfil y los eventos — son argumentos de constructor enterrados en el `<clinit>` del bytecode, invisibles para él.
Como `States` es pura a propósito (§4.1) — sin JPA ni sesión, del dominio solo el enum plano `Profile` — y está en el classpath de los tests, el test la carga con reflexión normal:

```java
        Class<?> clase = Class.forName(tipo.getBasePackageName() + ".States");
        TipoExpedienteStates states = (TipoExpedienteStates) clase.getField("INSTANCE").get(null);
```

y compara lo que devuelven los getters con lo que dice el `TipoExpedienteInstanceFile` (obtenido de `support/TiposExpediente`, como los demás).
Reporta con `Violacion.assertNone`, en el mismo estilo.
Reglas sugeridas: `S1` fases, `S2` estados de cada fase, `S3` metadatos de cada estado, `S4` estado inicial, `S5` `CODE`/`NAME`.
Es el que sustituye a E5.

### 8.4 `eventmanager/ApiBaseReservadaTest.java` — nuevo

Cubre la única parte de la regla 8 de §10 que el generador no puede comprobar: que ningún nombre de método compuesto pise un método público de las clases base.

```java
        Set<String> apiBase = new HashSet<>();
        for (Method m : EventManager.class.getMethods())        { apiBase.add(m.getName()); }
        for (Method m : StateEventValidator.class.getMethods()) { apiBase.add(m.getName()); }
```

y por cada fase de cada tipo compone `onEnter<Estado>`, `trigger<Evento>` y `getForState<Estado>InEvent<Evento>` con las **mismas** clases de build-tools con las que estos tests ya parsean el XML, fallando si alguno coincide.

Por qué esto es un test y no una constante en build-tools: el generador corre **antes** de compilar `secretaria-virtual` y los build-tools no comparten código con el runtime, así que ahí la lista solo podría existir duplicada a mano y haría falta otro test para vigilar la copia.
Los tests, en cambio, tienen a la vez el XML y las clases base reales en el classpath, y derivan la lista por reflexión sin duplicar nada: si la base gana, pierde o renombra un método público, el test se ajusta solo.

El caso real que motiva la regla: un estado llamado `STATE`.
Su `onEnterState(<Modelo>, EventContext)` tiene, sin los genéricos de §8.2, **exactamente** la firma del dispatcher `EventManager.onEnterState`, de modo que lo sobrescribiría y el despacho ejecutaría el handler de ese estado para **cualquier** estado de la fase, sin error de compilación ni de runtime.
Se compara solo el **nombre**, sin firmas, a propósito: así se prohíben también las sobrecargas legales pero confusas (un evento `INITIAL_EVENT` y su `triggerInitialEvent` de 3 parámetros) y no hay que mantener aridades sincronizadas con la base.

Trade-off asumido: el error salta en la fase de tests del build en vez de en `GenerateStatesTask` — más tarde dentro del **mismo** build, con cobertura idéntica porque el build siempre ejecuta los tests — y un esqueleto conflictivo de `CreateFilesTask` llega a generarse y lo denuncia el build siguiente, no la propia generación.

---

## 9. Orden de ejecución

Es un refactor en dos repositorios que rompe la compilación por el medio.
Siete fases; cada una dice qué se toca y con qué se verifica.
**La ventana en la que `secretaria-virtual` no compila va de la fase 3 a la 5**, y es a propósito: no tiene sentido intentar dejarlo verde en medio.

### Fase 1 — `EducaFlowBuildTools`, todo de una vez

Todo lo de D§1.1: borrar `CodeStateNaming`, tocar `State`/`Fase`/el finder, crear `createstates/` y `states.template`, cambiar las plantillas de esqueleto, las de data-init y `Form.java`.

**Puerta**: `mvn clean install` verde en `EducaFlowBuildTools` (es lo que hace `install.sh`).
Publica `com.educaflow:EducaFlowBuildTools:1.0-SNAPSHOT` en `mavenLocal()`, que es de donde lo toma `secretaria-virtual`.

⚠ Este es el punto de sincronización entre repos. Sin él, la fase 2 no ve nada.

### Fase 2 — contratos y modelo en `secretaria-virtual`

`Phase`, `State`, `TipoExpedienteStates`; las columnas `codePhase` en `Expediente.xml` y `HistorialEstado.xml`; el `<extra-code>` y el borrado de `EstadoTipoExpediente` en `TipoExpediente.xml`; el bloque `GenerateStatesTask` + `srcDir` en `build.gradle`.

**Puerta parcial**: `./gradlew GenerateStatesTask` verde y `build/src-gen-states/main/java/com/educaflow/tramites/prueba/v2/States.java` existente y con la pinta de §5.
`compileJava` **todavía falla** (el `extra-code` llama a un `ExpedienteLocator.getTipoExpedienteStates` que aún no existe, y los `EventManagerImpl` siguen con los genéricos viejos). Es lo esperado.

Aquí es donde se leen los errores del generador si algún `TipoExpedienteInstance.xml` real incumple alguna de las validaciones nuevas 8–11.

### Fase 3 — el runtime del subsistema

`EventManager`, `EventContext`, `ExpedienteLocator`, `ExpedienteUtil`, `Tramitador`, `ExpedienteController`, `DataBaseStartup`; borrar `CodeStateNaming.java` y `StateEnum.java`.

**Puerta**: ninguna intermedia. Al terminar, los únicos errores de compilación que deben quedar son los de los 12 `EventManagerImpl`.
Si aparece alguno fuera de `com/educaflow/tramites/`, es que el inventario de D§1.2 se quedó corto.

### Fase 4 — los 12 `EventManagerImpl`

D§6, fichero a fichero.
⚠ H4: los 4 de `justificacion_falta_profesorado` no son mecánicos — llevan 18 `updateState` reales y dos `State.valueOf` + `switch` (pasos 6 y 7 de D§6.1) — y son los que sostienen la verificación funcional de la fase 6, así que conviene dejarlos para el final de esta fase y revisarlos con calma.

**Puerta**: `./gradlew compileJava compileKotlin` **verde**. Es el primer build verde del refactor.

### Fase 5 — tests

Quitar E5 de `EventManagerTest`, añadir `StatesTest` y `ApiBaseReservadaTest`.

**Puerta**: `./gradlew test` verde, y en particular la suite de `com.educaflow.tiposexpedientes` completa.

### Fase 6 — verificación funcional

**Puerta**: `./run.sh` (que hace `clean build` con los tests y arranca en el 8080), con la base de datos reseteada según `agent_docs/deploy.md`.
Comprobar en la aplicación:

1. Que el data-init carga los tipos de expediente (sin estados) y rellena `basePackageName`.
2. Crear un expediente de `PruebaV2`: debe quedar en `codePhase=RECEPCION`, `codeState=ENTRADA_DATOS`, `nameState="Entrada datos"`.
3. Que se abre su vista `exp-PruebaV2-RECEPCION-ENTRADA_DATOS-CREADOR-form` (comprobable en `meta_view`).
4. ⚠ H4 — la tramitación real, que **debe** hacerse con `JustificacionFaltaProfesoradoV1` (`actual/v1`): `PruebaV2` tiene los cuerpos comentados y no cambia nunca de estado, así que con él este paso no verifica nada.
   Recorrido mínimo, que ejercita los tres casos que importan:
   - `GUARDAR_DATOS`: `RECEPCION/ENTRADA_DATOS` → `RECEPCION/PENDIENTE_PRESENTACION` (cambio dentro de la fase).
   - `PRESENTAR`: → `TRAMITACION/PENDIENTE_RESOLUCION` (**cruza de fase**); comprobar que el `onEnter` que se ejecuta es el del `EventManagerImpl` de `tramitacion` y que el historial guarda las dos columnas.
   - `BACK` desde `TRAMITACION`: vuelve a `RECEPCION/ENTRADA_DATOS`, que es lo que ejercita el `switch` traducido en el paso 7 de D§6.1.
5. Que un estado con `title` (`RESOLVER_PERMITIR_COMISION`) se muestra con acento en el listado de expedientes.

Comprobación negativa de D§5.6: una petición con un `profileName` que el tipo no usa (p.ej. `AUDITOR` en `PruebaV2`) debe fallar en seco, no renderizar la vista sin perfil.

### Fase 7 — documentación

D§10. No bloquea nada, pero cierra el refactor.

---

## 10. Documentación a actualizar

| Fichero | Qué |
|---|---|
| `CLAUDE.md` | El párrafo de tipos de expediente: fuera «`CodeStateNaming` … duplicada en este repo y en EducaFlowBuildTools» y «el nombre real de un estado es `F_{fase}_S_{estado}`». En su lugar: la pareja `(codePhase, codeState)` y la clase `States` generada |
| `.claude/skills/k-tipo-expediente/SKILL.md` | §1.5 entera (el nombre real del estado), §1.6 (`ExpedienteLocator`, que ahora resuelve por fase), §2.3 (el enum `State`, que desaparece), §3.3 (tests: E5 fuera, `StatesTest` y `ApiBaseReservadaTest` dentro) |
| `.claude/skills/k-tipo-expediente/eventmanager.md` | Firma sin genéricos, `EventContext` pelado, `eventContext.updateState(States.Fase.ESTADO)` |
| `.claude/skills/k-tipo-expediente/validator.md` | Referencias a `CodeStateNaming` |
| `.claude/skills/k-tipo-expediente/vistas.md` | Nombre de vista con fase y estado como segmentos |
| `.claude/skills/k-tipo-expediente/versionado.md` | Qué se copia al duplicar un tipo: `States.java` **no**, es generado. Y la advertencia del import de `States` sin actualizar, que es justo lo que ataja la barrera de D§5.4 |
| `.claude/skills/k-tipo-expediente/modelo.md` | Si menciona `EstadoTipoExpediente` o la relación `estados` |
| `agent_docs/architecture-rules.md` | **C13**: actualizar su Cumplimiento al incumplimiento conocido de las entidades de expediente con `<extra-code>` (`getTipoExpedienteStates`, `getDocumentoPdf`), sin tocar el matcher del test — la decisión y sus dos hechos están en D§2.4. Y comprobar si alguna regla nombra `build/src-gen` (ahora también existe `build/src-gen-states`) |

Al tocar cualquier `SKILL.md` hay que aplicar `/k-skill` (`CLAUDE.md`).

---

## 11. Riesgos

**R1 — la pareja `(codePhase, codeState)` como clave nueva.**
`ExpedienteUtil.updateState` es el único sitio que escribe las dos columnas y el único que decide si «no ha cambiado».
Un fallo ahí no rompe la compilación ni ningún test de estructura: se manifiesta como un expediente que se queda clavado o que salta de fase sin ejecutar su `onEnter`.
*Mitigación*: el paso 4 de la fase 6 (tramitación real cruzando de fase) es la única verificación que lo cubre, y por eso no es opcional.
⚠ H4 refuerza esto: solo `justificacion_falta_profesorado` tiene transiciones reales, así que es el único trámite con el que ese paso verifica algo.

**R2 — la ventana de no-compilación entre repositorios.**
De la fase 3 a la 5 el proyecto no compila, y el `install.sh` de la fase 1 es un paso manual fácil de olvidar: si no se ejecuta, la fase 2 falla con errores que parecen del código nuevo y son del JAR viejo.
*Mitigación*: la puerta explícita de la fase 1, y ante cualquier error raro en las fases 2–4, volver a ejecutarlo antes de investigar.

**R3 — `States.java` huérfano en build incremental.**
`States` es pura a propósito, así que el `States.java` de un tipo borrado o de una carpeta renombrada no referencia nada que haya dejado de existir y **seguiría compilando en silencio** desde `build/src-gen-states`.
`./run.sh` hace `clean`, pero no es el único camino por el que se compila.
*Mitigación*: el `doFirst { delete(...) }` de la tarea (D§4), que garantiza que en esa raíz solo hay proyecciones de XMLs que existen.

**R4 (menor) — el orden entre Flyway y el `hbm2ddl` de Axelor**, solo si se toma el camino alternativo de D§7.3.
Irrelevante mientras el camino sea resetear la base de datos.
