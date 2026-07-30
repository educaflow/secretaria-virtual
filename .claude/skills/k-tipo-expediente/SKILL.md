---
name: k-tipo-expediente
description: Cómo crear un tipo de expediente (una versión `v1`/`v2`… de un trámite) en `tramites/<tramite>/<vN>/`: el fichero maestro `TipoExpedienteInstance.xml` y la máquina de estados, el modelo (`domains.xml`), el `EventManager`, el `StateEventValidator`, las vistas preprocesadas (`views.xml`), los documentos PDF (`documentospdf/`, formato XML de definición) y la receta para duplicar un tipo y crear la versión siguiente. Cárgalo siempre que crees o modifiques cualquier fichero bajo una carpeta de versión de un trámite.
---

# k-tipo-expediente

Un tipo de expediente es la implementación **versionada** de un trámite: la carpeta `tramites/<tramite>/<vN>/` con su máquina de estados, entidad, vistas, validaciones y documentos. El trámite en sí (el `TramiteInstance.xml` padre) es de `k-tramite`. La fuente de verdad de referencia es `tramites/justificacion_falta_profesorado/` (v1 y v2).

## Ficheros de este skill

| Fichero | Contenido |
|---------|-----------|
| `modelo.md` | El `domains.xml` del tipo: entidad `extends="Expediente"`, enums versionados, campos `MetaFile` para los PDF, `extra-code-model` |
| `eventmanager.md` | La máquina de estados en Java: métodos `trigger*`/`onEnter*`, API de `EventContext` y el **catálogo de acciones** (generar PDF, registros de entrada/salida, firmas, correos…) |
| `validator.md` | El `StateEventValidator` en Kotlin: DSL de reglas por estado+evento y su doble función de whitelist de campos |
| `vistas.md` | El `views.xml` en formato **preprocesado** (NO sigue `k-vistas`): form plantilla, `include-panels`, `footer`, visores de PDF, AutoFirma |
| `documentos.md` | El formato XML de los documentos de `documentospdf/` de los que el build genera los PDF rellenables |
| `versionado.md` | Receta para duplicar un tipo de expediente y crear la versión siguiente (`v1` → `v2`) |

---

## 1. Conceptos clave

### 1.1 Todo se deriva de la carpeta de versión

**CRITICAL**: la carpeta `tramites/<tramite>/<vN>/` determina la identidad completa del tipo. Con el `TramiteInstance.xml` padre (`code=JustificacionFaltaProfesorado`, `name=Justificación de falta del profesorado`) y la carpeta `v1`:

| Derivado | Regla | Ejemplo |
|---|---|---|
| Versión | nombre de la carpeta en mayúsculas | `v1` → `V1` |
| `code` del tipo | code del trámite + versión | `JustificacionFaltaProfesoradoV1` |
| `name` del tipo | name del trámite + " " + versión | `Justificación de falta del profesorado V1` |
| Entidad JPA | = `code` (el `domains.xml` **MUST** declarar `<entity name="<code>">`) | `JustificacionFaltaProfesoradoV1` |
| Paquete Java | la ruta tras `/java/` | `com.educaflow.tramites.justificacion_falta_profesorado.v1` |
| FQCN EventManager / Validator | `<paquete>.EventManagerImpl` / `.StateEventValidatorImpl` | — |

Todos son **defaults**: se pueden sobrescribir con tags opcionales del `TipoExpedienteInstance.xml` (§2), pero en la práctica no se hace.

### 1.2 Contenido de la carpeta del tipo

| Fichero | Quién lo escribe | Detalle en |
|---|---|---|
| `TipoExpedienteInstance.xml` | tú | §2 (este fichero) |
| `estados.puml` / `estados.png` | tú / build (renderiza el `.puml`) | §2.3 |
| `domains.xml` | tú (esqueleto generado por el build) | `modelo.md` |
| `EventManagerImpl.java` | tú (esqueleto generado) | `eventmanager.md` |
| `StateEventValidatorImpl.kt` | tú (esqueleto generado) | `validator.md` |
| `views.xml` | tú (esqueleto generado) | `vistas.md` |
| `documentospdf/` | tú | `documentos.md` |
| `i18n_es.csv` / `i18n_ca.csv` | build — **MUST NOT** crearlos a mano | `CLAUDE.md` (i18n) |

### 1.3 Flujo runtime de un evento

Cuando el usuario pulsa un botón (= dispara un evento), `Tramitador.triggerEvent` hace, en orden:

1. Valida que el evento es legal en el estado actual (enum `State` del EventManager).
2. Obtiene las reglas del validator para (estado, evento).
3. **Copia del request SOLO los campos que tienen reglas** (whitelist anti mass-assignment → `validator.md`).
4. Ejecuta las validaciones; si fallan, `BusinessException` y los mensajes se pintan en el panel de error del footer.
5. Llama a `trigger<Evento>` del EventManager (ahí se decide el estado destino → `eventmanager.md`).
6. Guarda el historial de estados, llama a `onEnter<Estado>` del estado destino y persiste.
7. Muestra la vista del nuevo estado (`vistas.md` §"Cómo elige la vista el runtime").

**Eventos comunes** (existen sin declararlos): `EXIT` (cerrar la pestaña; lo intercepta `ExpedienteController`, no llega al EventManager) y `DELETE` (borra sin validar ni copiar campos). **`BACK` NO es común**: si un estado necesita "volver atrás" hay que declararlo como evento normal.

---

## 2. `TipoExpedienteInstance.xml` — el fichero maestro

Fichero mínimo real (todo lo demás se deriva, §1.1):

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<TipoExpediente>
    <states>
        <state name="ENTRADA_DATOS"          events="DELETE,GUARDAR_DATOS" profile="CREADOR"      title="Entrada de datos"           initial="true"  />
        <state name="PENDIENTE_PRESENTACION" events="BACK,PRESENTAR"       profile="CREADOR"      title="Pendiente de presentación"                  />
        <state name="PENDIENTE_RESOLUCION"   events="RESOLVER"             profile="RESPONSABLE"  title="Pendiente de resolución"                    />
        <state name="ACEPTADO"               events=""                     profile="RESPONSABLE"                                     closed="true"   />
        <state name="RECHAZADO"              events=""                     profile="RESPONSABLE"                                     closed="true"   />
    </states>
</TipoExpediente>
```

Tags opcionales (antes de `<states>`): `name`, `code`, `tramite`, `fqcnEventManager`, `fqcnStateEventValidator` (sobrescriben los defaults de §1.1; el paquete de un FQCN custom **MUST** ser el de la propia carpeta del tipo) y `ambitoCreador`/`ambitoResponsable`/`ambitoAuditor` (`INDIVIDUAL|CENTRO|DEPARTAMENTO`).

### 2.1 Reglas de `<state>`

- `events` es **obligatorio aunque esté vacío** (`events=""`). Omitir el atributo revienta el parseo con un error genérico sin pista de la causa.
- `profile` es opcional (estado sin dueño → perfil `null` en el enum).
- **MUST** haber exactamente **un** estado `initial="true"`.
- `closed="true"` marca los estados terminales (el expediente queda cerrado pero existe).
- Nombres de estados, eventos y perfiles: `UPPER_SNAKE` y **MUST** ser identificadores Java válidos — van tal cual a los enums `State`/`Event`/`Profile` y a los nombres de método (`GUARDAR_DATOS` → `triggerGuardarDatos`).
- JAXB **ignora en silencio los tags desconocidos**: un typo en un tag opcional (p.ej. `<fqcnEventmanager>`) no da error, simplemente aplica el default.

- ✅ CORRECTO: `<state name="PENDIENTE_FIRMA" events="" profile="RESPONSABLE"/>`
- ❌ INCORRECTO: `<state name="PENDIENTE_FIRMA" profile="RESPONSABLE"/>` (falta `events`, aunque sea vacío)
- ❌ INCORRECTO: `<state name="PendienteFirma" .../>` (no es UPPER_SNAKE: produce métodos inesperados como `triggerPendientefirma`)

### 2.2 De los estados salen los enums

Los `events` y `profile` de todos los estados forman los enums `Event` y `Profile` (deduplicados, en orden de primera aparición). El enum `State` generado es "rico": cada constante lleva su perfil dueño, `initial`, `closed` y sus eventos permitidos. **La máquina de estados vive únicamente en ese enum en runtime** — el data-init no persiste ni eventos ni flags (solo `codeState` y `profile` de cada estado).

### 2.3 `estados.puml`

Dibuja la máquina antes de escribir el XML. Convenciones: `[*] --> <INICIAL>`; `A --> B : EVENTO`; guardas para transiciones condicionales (`RESOLVER[tipoResolucion=ACEPTAR]`); `<estado> : closed` para anotar los terminales (**MUST NOT** usar `--> [*]` para ellos: en estos diagramas `[*]` como destino significa borrado físico, `DELETE`). El build renderiza el `.png` (se salta el render si el `.png` es más nuevo).

---

## 3. Qué genera y comprueba el build

### 3.1 Esqueletos (`CreateFilesTask`, solo si no existen)

`domains.xml`, `views.xml`, `EventManagerImpl.java` y `StateEventValidatorImpl.kt` se generan con **todos los métodos/forms requeridos** ya presentes y vacíos. Flujo normal: escribir primero el `TipoExpedienteInstance.xml`, compilar para generar los esqueletos, y rellenarlos.

### 3.2 Comprobaciones que hacen fallar el build

1. `TipoExpedienteInstance.xml` parseable, exactamente un `initial`, `events` presente en cada estado.
2. EventManager (check con Spoon): **exactamente un** método `@WhenEvent trigger<Evento>(<Entidad>, <Entidad>, EventContext)` por evento y **exactamente un** `@OnEnterState onEnter<Estado>(<Entidad>, EventContext)` por estado; ni faltar ni sobrar (detalle y trampas en `eventmanager.md` §7).
3. `domains.xml` con `<module>` único y `<entity name="<code>">` (el nombre de la entidad = code derivado).
4. `views.xml` parseable, con exactamente un form plantilla `exp-<Code>-Templates`, y todos los paneles de `<include-panels>` existentes (detalle en `vistas.md`).
5. i18n: si apertium no logra una traducción fiable de un texto nuevo, el build falla (se arregla con `__!!` o escribiendo el valenciano a mano).
6. Documentos: los XML de `documentospdf/` se validan contra XSD y sus filas deben sumar múltiplos de 12 (detalle en `documentos.md`).

### 3.3 Lo que el build NO comprueba (falla en runtime)

- El **validator**: su check de build está vacío. Un método que falte para un (estado, evento) solo se detecta al disparar el evento ("No se ha encontrado el método: getForState…"). Mantenerlo a mano al tocar eventos.
- Las **vistas por estado**: para cada (estado, perfil) alcanzable debe existir `exp-<Code>-<STATE>-<PROFILE>-form` o la genérica `exp-<Code>-<STATE>-form`; si no, excepción al navegar.
- `triggerInitialEvent` **MUST** dejar rellenos `dniFirmaDocumentoEntrada` (DNI válido) y `personaSolicitante` (ver `eventmanager.md` §2).
- Los `profile` de los estados **MUST** existir como perfiles en BD: los carga el data-init `subsystem/expedientes/data-init/input/PerfilesExpedientes.xml` (hoy `CREADOR` y `RESPONSABLE`). Si un estado usa un perfil nuevo, añádelo ahí; si no existe, la resolución del perfil falla en silencio al cargar el tipo.

### 3.4 Qué genera en BD el data-init del tipo

`generateDataInitTiposExpedientes` genera por cada tipo un data-init (en `build/`, nunca en `src`) que hace bind del `TipoExpediente` por `code` con `create`+`update` — por eso la fila **se refresca en cada arranque**. Persiste:

- `code`, `name`, los tres `ambito*` (siempre, aunque queden vacíos), el `tramite` (resuelto buscando su `code` en BD — de ahí que el trámite cargue antes, con `priority` mayor) y los dos FQCN, **siempre ya resueltos** (aunque el XML los omita, en BD queda el default explícito `<paquete>.EventManagerImpl` / `.StateEventValidatorImpl`).
- De cada `<state>`, **solo** `codeState` y su `profile` (resuelto por `code` contra los perfiles de BD).
- `events`, `initial`, `closed` y `title` **NO van a BD**: la máquina vive solo en el enum `State` (§2.2).
- Caveat: el bind de los estados no lleva `search`, así que un reimport puede crear **filas de estado duplicadas**.

(El data-init del **trámite** — qué persiste el `TramiteInstance.xml` — está en `/k-tramite` §4.)

---

## 4. Checklist: crear un tipo de expediente nuevo

1. **Trámite**: asegúrate de que existe `tramites/<tramite>/TramiteInstance.xml` (`/k-tramite`); si es la primera versión, sin `<defaultTipoExpediente>` aún.
2. **Carpeta**: crea `tramites/<tramite>/v1/` con solo `estados.puml` (dibuja la máquina) y `TipoExpedienteInstance.xml` (§2).
3. **Compila** (`./gradlew clean build`): se generan los cuatro esqueletos.
4. **Modelo**: añade los campos a `domains.xml` → `modelo.md`.
5. **Documentos**: crea `documentospdf/` con los XML de definición → `documentos.md`.
6. **EventManager**: rellena `triggerInitialEvent`, cada `trigger<Evento>` y `onEnter<Estado>` → `eventmanager.md`.
7. **Validator**: rellena las `rules { }` de cada estado+evento → `validator.md`.
8. **Vistas**: monta los paneles del form plantilla y compón cada `<form state=...>` → `vistas.md`.
9. **Permisos**: verifica que el perfil de cada estado (`CREADOR`, `RESPONSABLE`…) está asignado a alguien (`/k-tramite` §6).
10. **Activa** la versión en el `TramiteInstance.xml` (`<defaultTipoExpediente>v1</defaultTipoExpediente>`), compila y arranca.
11. **Prueba en runtime** navegando por **todos** los estados con usuarios de los perfiles adecuados (menú Expedientes → Trámites): el validator y las vistas por estado solo fallan en runtime (§3.3).

Para crear una **versión nueva de un tipo existente** → `versionado.md`.

---

## Quick Guidelines

- Todo se deriva de la carpeta `tramites/<tramite>/<vN>/`: code = code del trámite + `VN`, entidad = code, paquete = ruta. No declares lo que el default ya resuelve.
- `TipoExpedienteInstance.xml` mínimo = solo `<states>`. `events` obligatorio aunque vacío; exactamente un `initial`; nombres `UPPER_SNAKE`.
- Flujo: XML de estados → compilar (esqueletos) → rellenar modelo, EventManager, validator, vistas y documentos → activar versión → probar todos los estados en runtime.
- La máquina de estados en runtime vive en el enum `State` del EventManager, no en BD.
- El build comprueba EventManager, modelo, vistas-plantilla, i18n y documentos; el validator y las vistas por estado solo fallan en runtime.
- `EXIT` y `DELETE` son eventos comunes gratis; `BACK` no — se declara e implementa como uno más.
- **MUST NOT** crear `i18n_*.csv` a mano; **MUST NOT** editar el `<extra-code-model>` ni el `estados.png` (los regenera el build).
