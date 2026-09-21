---
type: design
template: system
---

# Diseño: Sistema «expedientes» y desacople del tramitador

**Objetivo:** Crear `system/expedientes` con las pantallas «Nuevo expediente» y «Tipos de expediente», y dejar que el tramitador (`subsystem/expedientes`, que este diseño también modifica) inicie expedientes con solo cuatro datos que valida él mismo en su puerta de entrada.
**Capa:** system/expedientes
**Especificación de origen:** .sdd/drafts/2026-09-19_14-49_sistema-expedientes-y-desacople-tramitador/specification.md
**Skills necesarios para la implementación:** k-sistemas, k-code-quality, k-secure-coding, k-vistas, k-validaciones, k-datainit

Las decisiones difíciles, con sus alternativas descartadas, están en [`decisiones.md`](decisiones.md) (D1–D7); este índice las da por tomadas.

## Modelos del spec y dónde vive cada uno

| Modelo (spec) | Qué es en el diseño | Fichero |
|---|---|---|
| NuevoExpediente | Modelo Axelor `persistable="false"` del sistema: el formulario de la ventana, con sus 4 campos de entrada, sus 7 campos calculados y 3 campos `servidor` más (`hayUnSoloCentroDisponible`, `hayQuePreguntarParaQuien` y `presentadoEnPapelVigente`) que la vista consulta en vez de repetir una condición (guías: «se pueden añadir al modelo los campos que hagan falta»). | `domains/NuevoExpediente.xml` (nuevo) |
| ContextoTramitacion | Deja de ser un modelo Axelor: pasa a ser un `record` inmutable del motor con exactamente `tramite`, `centro`, `profile`, `presentadoEnRepresentacion` (D1). Por eso **no** tiene `domains/*.xml` en el diseño y su XML actual se borra. | `tramitacion/eventmanager/ContextoTramitacion.java` (nuevo) |
| TipoExpediente | Sin cambios en el modelo (el spec: «No cambia ninguno de sus campos»); por eso **no** hay `domains/TipoExpediente.xml` en el diseño. Solo cambia su pantalla y se protege su escritura en el servidor (D5). | `subsystem/expedientes/domains/TipoExpediente.xml` (se conserva tal cual) |

## Ficheros a crear o modificar

Rutas relativas a la raíz del proyecto.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/system/expedientes/domains/NuevoExpediente.xml` | Crear | k-sistemas (modelos.md) | Modelo de pantalla de «Nuevo expediente» (copia verbatim de `design/domains/NuevoExpediente.xml`) |
| `src/main/java/com/educaflow/subsystem/expedientes/views-models/ContextoTramitacion.xml` | Borrar | k-sistemas (modelos.md) | El contexto deja de ser un modelo Axelor (D1). Se borran también los `i18n_*.csv` generados de esa carpeta y la carpeta `views-models/`, que queda vacía |
| `src/main/java/com/educaflow/subsystem/expedientes/tramitacion/eventmanager/ContextoTramitacion.java` | Crear | k-code-quality | `record` de entrada del motor (Paso 2) |
| `src/main/java/com/educaflow/subsystem/expedientes/tramitacion/eventmanager/FormaPresentacion.java` | Crear | k-code-quality | `enum` dueño de la equivalencia perfil de inicio ↔ forma de presentar (Paso 2) |
| `src/main/java/com/educaflow/subsystem/expedientes/tramitacion/eventmanager/InitialEventContext.java` | Modificar | k-code-quality | Usa el nuevo `ContextoTramitacion` (mismo paquete) (Paso 2) |
| `src/main/java/com/educaflow/subsystem/expedientes/tramitacion/core/Tramitador.java` | Modificar | k-code-quality | Lee el contexto nuevo y deduce el papel del perfil (Paso 2) |
| `src/main/java/com/educaflow/subsystem/expedientes/services/VistaExpediente.java` | Crear | k-code-quality | `record` con lo necesario para abrir el formulario de un expediente (Paso 3) |
| `src/main/java/com/educaflow/subsystem/expedientes/services/ExpedienteService.java` | Modificar | k-secure-coding, k-validaciones | Puerta del motor: `crear` → `triggerInitialEvent` con validación, perfiles/centros de inicio y vista del expediente (Paso 3) |
| `src/main/java/com/educaflow/subsystem/expedientes/services/ContextoTramitacionService.java` | Borrar | — | Su lógica se reparte entre la puerta del motor y el servicio del sistema (D2) (Paso 5) |
| `src/main/java/com/educaflow/subsystem/expedientes/service/TipoExpedienteService.java` | Crear | k-sistemas (servicios.md) | `ModelService` de `TipoExpediente` (Paso 4) |
| `src/main/java/com/educaflow/subsystem/expedientes/service/impl/TipoExpedienteServiceImpl.java` | Crear | k-sistemas (servicios.md), k-secure-coding §9.2 | Rechaza toda escritura de tipos de expediente (Paso 4) |
| `src/main/java/com/educaflow/system/expedientes/service/NuevoExpedienteService.java` | Crear | k-sistemas (servicios.md) | Interfaz del servicio de la ventana (Paso 5) |
| `src/main/java/com/educaflow/system/expedientes/service/impl/NuevoExpedienteServiceImpl.java` | Crear | k-sistemas (servicios.md), k-validaciones | Calcula los campos de la ventana y convierte la ventana en un `ContextoTramitacion` (Paso 5) |
| `src/main/java/com/educaflow/subsystem/expedientes/controllers/ExpedienteController.java` | Modificar | k-sistemas (controladores.md) | Solo se borra `triggerInitialEvent` (pasa al sistema); `viewExpediente`, `triggerEvent` y `getTabName` se quedan exactamente como están (fuera de alcance del spec, D3) (Paso 6) |
| `src/main/java/com/educaflow/subsystem/expedientes/controllers/ContextoTramitacionController.java` | Borrar | — | Sustituido por `NuevoExpedienteController` (Paso 6) |
| `src/main/java/com/educaflow/system/expedientes/controller/NuevoExpedienteController.java` | Crear | k-sistemas (controladores.md) | Controlador de la ventana (Paso 6) |
| `src/main/java/com/educaflow/system/expedientes/views/Main-NuevoExpediente.xml` | Crear | k-vistas (forms.md, actions.md) | Ventana «Nuevo expediente» (copia verbatim de `design/views/Main-NuevoExpediente.xml`) |
| `src/main/java/com/educaflow/system/expedientes/views/Main-TipoExpediente.xml` | Crear | k-vistas (grids.md, forms.md) | Consulta de tipos de expediente (copia verbatim de `design/views/Main-TipoExpediente.xml`) |
| `src/main/java/com/educaflow/subsystem/expedientes/views/Main-ContextoTramitacion.xml` | Borrar | — | Sustituida por `Main-NuevoExpediente.xml` |
| `src/main/java/com/educaflow/subsystem/expedientes/views/TipoExpediente.xml` | Borrar | — | Sustituida por `Main-TipoExpediente.xml` |
| `src/main/java/com/educaflow/tramites/views/Tramites.xml` | Modificar | k-vistas (tree.md) | Las dos referencias a la acción que abre «Nuevo expediente» (copia verbatim de `design/views/Tramites.xml`) |
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | El menú «Tipos Expedientes» apunta a la acción nueva y solo lo ve `admins` |
| `src/main/java/com/educaflow/system/expedientes/data-init/input-config.xml` | Crear | k-datainit | Manifiesto del data-init del sistema (Paso 9) |
| `src/main/java/com/educaflow/system/expedientes/data-init/input/auth-expedientes.xml` | Crear | k-datainit | Permiso `NuevoExpediente.all` (Paso 9) |
| `src/main/java/com/educaflow/subsystem/expedientes/data-init/input/auth-expedientes.xml` | Modificar | k-datainit | Se borra el permiso `ContextoTramitacion.all` y se actualiza el comentario de `Tramite.registrador` (referencia obsoleta a `ExpedienteSecurity` → `ExpedienteService.getPerfilesDeInicio` / `validateTriggerInitialEvent`) (Paso 9) |
| `src/main/resources/data-init/input/auth.xml` | Modificar | k-datainit | En los grupos `admins` y `users`, `ContextoTramitacion.all` → `NuevoExpediente.all` (Paso 9) |
| `src/test/java/com/educaflow/subsystem/expedientes/services/ContextoTramitacionServiceTest.java` | Borrar | — | Prueba una clase que desaparece; la sustituyen los tests de `test-unit-desc.md` (Paso 5) |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.

## Pasos

### Paso 1 — Dominio `NuevoExpediente` y retirada del modelo `ContextoTramitacion`

**Crear** `system/expedientes/domains/NuevoExpediente.xml` copiando `design/domains/NuevoExpediente.xml`.
**Borrar** `subsystem/expedientes/views-models/ContextoTramitacion.xml` (y la carpeta `views-models/`, que solo conserva `i18n_*.csv` generados).

Resumen estructural de `domains/NuevoExpediente.xml` (módulo `sysexpedientes`, paquete `com.educaflow.system.expedientes.db`, entidad `persistable="false"`):

| Campo | Tipo | Origen | Spec |
|---|---|---|---|
| `tramite` | many-to-one `Tramite` | cliente | campo «trámite» |
| `centro` | many-to-one `Centro` | cliente | campo «centro» |
| `presentadoEnPapel` | boolean | cliente | campo «presentado en papel» |
| `presentadoEnRepresentacion` | boolean `nullable="true"` | cliente | campo «presentado en representación» («mientras no contesta, no tiene valor») |
| `nombreTramite` | string | servidor | CC-NuevoExpediente-001 |
| `ayudaTramite` | string `large` | servidor | CC-NuevoExpediente-002 |
| `centrosDisponibles` | many-to-many `Centro` | servidor | CC-NuevoExpediente-003 |
| `hayUnSoloCentroDisponible` | boolean | servidor | Condición «hay un solo centro disponible» de RUI-nuevo-expediente-formulario-003/004 (campo añadido por el diseño) |
| `hayQuePreguntarPresentacion` | boolean | servidor | CC-NuevoExpediente-004 |
| `sePuedeCrearParaMi` | boolean | servidor | CC-NuevoExpediente-005 |
| `sePuedeCrearEnRepresentacion` | boolean | servidor | CC-NuevoExpediente-006 |
| `hayQuePreguntarParaQuien` | boolean | servidor | Condición «se puede crear para uno mismo y en representación» de RUI-nuevo-expediente-formulario-011/012/013/016 (campo añadido por el diseño) |
| `presentadoEnPapelDeducido` | boolean `nullable="true"` | servidor | CC-NuevoExpediente-007 (`true` en papel, `false` telemático, `null` sin valor) |
| `presentadoEnPapelVigente` | boolean | servidor | Forma de presentar vigente (la deducida si la hay; si no, la de la ventana), que la vista copia a `presentadoEnPapel` en RUI-007 (campo añadido por el diseño, D4) |

`nullable="true"` es necesario en los dos booleanos que admiten «sin valor»: sin él el getter generado por Axelor devuelve `FALSE` cuando el valor es `null`, y `V-ContextoTramitacion-004` no podría distinguir «para mí» de «sin contestar».
Ningún campo lleva `profile`: el perfil lo decide el servidor (R-NuevoExpediente-001), así que la ventana no tiene por dónde enviarlo.
`hayUnSoloCentroDisponible`, `hayQuePreguntarParaQuien` y `presentadoEnPapelVigente` existen para que cada condición o decisión de la vista tenga un único dueño: el servidor la calcula una vez y todos los atributos y acciones de la vista que dependen de ella la consultan, en vez de repetir la expresión en JS y en Groovy.

Verificación: `validate.sh` da `VALIDACION-XML: OK` para `domains/NuevoExpediente.xml`; `grep -rn "views-models/ContextoTramitacion" src` sin resultados.
La compilación **no** se comprueba en los Pasos 1 a 5: hasta el Paso 6 quedan usos del modelo anterior (`ExpedienteController.triggerInitialEvent`, `ContextoTramitacionController`) que se retiran allí; el primer `./gradlew compileJava` que debe pasar es el del Paso 6.

### Paso 2 — `ContextoTramitacion` como entrada del motor

**Crear** `com.educaflow.subsystem.expedientes.tramitacion.eventmanager.FormaPresentacion` y `…eventmanager.ContextoTramitacion`.
`FormaPresentacion` es el dueño único de qué perfiles pueden iniciar un expediente y de la equivalencia perfil ↔ forma de presentar: una forma de presentar = una constante del enum (D1, D2).
**Invariante:** el enum tiene **exactamente dos constantes, una por cada valor de `presentadoEnPapel`**, porque la ventana (`NuevoExpediente.presentadoEnPapel`, pregunta de sí/no) y el expediente representan la forma de presentar con un booleano. Añadir una tercera forma **no** es tocar una constante: exige cambiar ese booleano en el modelo y en la ventana, y rediseñar `fromPresentadoEnPapel`, `getFormaPresentarVigente` y `toContextoTramitacion`. Cambiar el perfil de una forma existente sí es tocar solo su constante.
**Test unitario obligatorio** (para `test-unit-desc.md`): comprobar la invariante — para `true` y para `false` hay exactamente una constante en `values()` con ese `isPresentadoEnPapel()`, y `fromPresentadoEnPapel` devuelve esa constante.
`ContextoTramitacion` es un DTO puro de cuatro componentes.

```java
// Clase: com.educaflow.subsystem.expedientes.tramitacion.eventmanager.FormaPresentacion
public enum FormaPresentacion {
    TELEMATICA(Profile.CREADOR, false),
    EN_PAPEL(Profile.TRAMITADOR, true);

    FormaPresentacion(Profile profile, boolean presentadoEnPapel);

    public Profile getProfile();
    //   Perfil con el que se inicia el expediente presentado de esta forma.

    public boolean isPresentadoEnPapel();

    public static FormaPresentacion fromPresentadoEnPapel(boolean presentadoEnPapel);
    //   Filtra values() por isPresentadoEnPapel() == presentadoEnPapel y exige exactamente una: si encuentra cero o
    //   más de una lanza IllegalStateException con un mensaje que diga que se ha roto la invariante del enum (una
    //   constante por cada valor de presentadoEnPapel). No supone la invariante: la hace cumplir.
    //   Lo usa R-NuevoExpediente-001 (el perfil lo decide el sistema a partir de la forma de presentar).

    public static Optional<FormaPresentacion> fromProfile(Profile profile);
    //   La constante cuyo getProfile() coincide; vacío para cualquier perfil que no inicia, incluido null.

    public static boolean esPerfilDeInicio(Profile profile);
    //   fromProfile(profile).isPresent(). Único sitio donde se decide qué perfiles pueden iniciar un expediente
    //   (lo consulta ExpedienteService.getPerfilesDeInicio y, a través de ella, la ventana).
    //   Las tres consultas salen de values(), y la ventana deduce las formas de presentar posibles filtrando
    //   values() (R-NuevoExpediente-004): cambiar el perfil de una forma existente es tocar una sola constante.
    //   Añadir una forma NO lo es (invariante de dos constantes, una por valor de presentadoEnPapel; ver arriba).
}
```

```java
// Clase: com.educaflow.subsystem.expedientes.tramitacion.eventmanager.ContextoTramitacion
public record ContextoTramitacion(Tramite tramite, Centro centro, Profile profile, Boolean presentadoEnRepresentacion) {

    public ContextoTramitacion(Tramite tramite, Centro centro, Profile profile, Boolean presentadoEnRepresentacion);
    //   Constructor canónico, escrito en forma compacta.
    //   Objects.requireNonNull(tramite) con un mensaje que diga que un contexto sin trámite
    //   es un error de programación (la ventana lo fija siempre; un alta programática lo pasa por constructor).
    //   centro, profile y presentadoEnRepresentacion SÍ pueden ser null: es justo lo que comprueban
    //   V-ContextoTramitacion-001/003/004 en la puerta del motor.

    public boolean isPresentadoEnPapel();
    //   Aplica R-ContextoTramitacion-001 (Origen spec: RN-ContextoTramitacion-001):
    //   FormaPresentacion.fromProfile(profile).orElseThrow(() -> new IllegalStateException(<mensaje: solo se
    //   llama con un contexto ya validado por ExpedienteService, cuyo perfil es de inicio>)).isPresentadoEnPapel().
    //   Nunca devuelve un valor por defecto que tape un perfil inesperado.
    //   presentadoEnPapel ya no viaja: se deduce del perfil, así que no puede llegar un dato incoherente con él.
}
```

**Modificar** `tramitacion/eventmanager/InitialEventContext.java` (solo el delta; el resto de la clase se conserva):
- Quitar el import de `com.educaflow.subsystem.expedientes.db.ContextoTramitacion`: el record está en el mismo paquete.
- Javadoc de la clase: el contexto dice en qué centro, con qué perfil (del que se deduce si es en papel) y si se presenta en representación.

**Modificar** `tramitacion/core/Tramitador.java` (solo el delta; el resto de la clase se conserva):

```java
// Clase: com.educaflow.subsystem.expedientes.tramitacion.core.Tramitador
public Expediente triggerInitialEvent(ContextoTramitacion contextoTramitacion) throws BusinessException;
//   Misma firma; cambia el import al record de tramitacion.eventmanager y la lectura del contexto:
//     - tipoExpediente = contextoTramitacion.tramite().getDefaultTipoExpediente()
//     - centro = Objects.requireNonNull(contextoTramitacion.centro(), <mensaje: el contexto debe llegar validado por ExpedienteService>)
//     - presentadoEnPapel = contextoTramitacion.isPresentadoEnPapel()        ← R-ContextoTramitacion-001
//     - presentadoEnRepresentacion = Objects.requireNonNull(contextoTramitacion.presentadoEnRepresentacion(), <mismo mensaje>)
//     - el EventContext del alta se construye con contextoTramitacion.profile()
//   Precondición: solo lo invoca ExpedienteService.triggerInitialEvent, que ya ha validado el contexto
//   (V-ContextoTramitacion-001…005). Un centro o una representación null aquí es un error de programación:
//   los requireNonNull llevan un mensaje que lo dice, en vez de desempaquetar un null sin explicación.
//   Todo lo demás (personas, nombre, número, InitialEventManager, historial, onEnter, JPA.save) se conserva.
```

Verificación: `grep -rn "db.ContextoTramitacion" src/main/java/com/educaflow/subsystem/expedientes/tramitacion` sin resultados (compilación en el Paso 6).

### Paso 3 — La puerta del motor: `ExpedienteService`

**Crear** el record `com.educaflow.subsystem.expedientes.services.VistaExpediente`:

```java
// Clase: com.educaflow.subsystem.expedientes.services.VistaExpediente
public record VistaExpediente(String viewName, Class<? extends Expediente> modelClass, Expediente expediente, Profile profile) {

    public String title();
    //   Método calculado, no componente: título de la pestaña del expediente = número de expediente + "-" + nombre
    //   traducido (I18n.get) del tipo de expediente. Es la fórmula que hoy usaba ExpedienteController.triggerInitialEvent
    //   (que se borra) a través de getTabName.
    //   DEUDA TEMPORAL DELIBERADA (D3, Notas §10): ExpedienteController.getTabName se conserva con la misma fórmula,
    //   porque viewExpediente y triggerEvent están fuera del alcance del spec; las dos copias se unifican en la
    //   iniciativa de renombrado a tramitador. No es un olor a corregir en esta iniciativa.
}
//   Lo necesario para abrir el formulario de un expediente recién creado con un perfil; quien lo pinta
//   (NuevoExpedienteController.crear) se lo pasa tal cual a
//   ActionResponseHelper.doResponseViewForm(viewName, modelClass, expediente, title(), profile.name()) (D3).
```

**Modificar** `services/ExpedienteService.java` (solo el delta; `triggerEvent`, `getExpediente` y `validateChild` se conservan):

```java
// Clase: com.educaflow.subsystem.expedientes.services.ExpedienteService
// Campos @Inject nuevos: ExpedienteLocator expedienteLocator; ModelServiceFactory modelServiceFactory.
// PerfilesUsuarioService perfilesUsuarioService ya está inyectado (hoy sin uso): lo usa getPerfilesDeInicio.
// Javadoc de la clase: sustituir la referencia a la autorización del alta por la puerta real —
// triggerInitialEvent valida siempre, entre por donde entre la petición.

@Transactional
public Expediente triggerInitialEvent(ContextoTramitacion contextoTramitacion) throws BusinessException;
//   Renombrado de crear(ContextoTramitacion) (guías: lo que hace es disparar el evento inicial).
//   1. validateTriggerInitialEvent(contextoTramitacion).ifPresent(BusinessMessages::throwIfInvalid):
//      las validaciones de «Iniciar expediente» se cumplen aquí aunque quien llame no haya pedido antes la
//      validación (la ventana sí la pide; un alta programática o un cliente por /ws/action puede no hacerlo).
//   2. return tramitador.triggerInitialEvent(contextoTramitacion).

public Optional<BusinessMessages> validateTriggerInitialEvent(ContextoTramitacion contextoTramitacion);
//   Quien inicia es SIEMPRE SecurityUtil.getUser(), nunca un dato del cliente.
//   Acumula todos los fallos (la ventana los enseña juntos, U-nuevo-expediente-019), cada uno como
//   BusinessMessage sin nombre de campo para que salga como una línea de texto. Dos ramas independientes:
//   Rama centro/perfil (complementaria por construcción):
//     - V-ContextoTramitacion-001 (Origen spec: VAL-ContextoTramitacion-001) centro null → mensaje de
//       VAL-ContextoTramitacion-001 del spec. En esta rama no se evalúan 002/003: sin centro no hay perfiles
//       que comprobar.
//     - si hay centro, perfilesDeInicio = getPerfilesDeInicio(tramite, centro):
//       - V-ContextoTramitacion-002 (Origen spec: VAL-ContextoTramitacion-002) perfilesDeInicio vacío →
//         mensaje de VAL-ContextoTramitacion-002 (no puede crear expedientes de ese trámite en ese centro).
//       - si no, V-ContextoTramitacion-003 (Origen spec: VAL-ContextoTramitacion-003)
//         !FormaPresentacion.esPerfilDeInicio(profile) || !perfilesDeInicio.contains(profile)
//         → mensaje de VAL-ContextoTramitacion-003. El caso «perfil null» (y «no es creador ni tramitador») lo
//         rechaza explícitamente la primera condición (esPerfilDeInicio trata null como «no inicia»), evaluada
//         ANTES del contains: la rama no depende de qué implementación de Set devuelva getPerfilesDeInicio (un
//         Set.of()/Set.copyOf lanza NullPointerException con contains(null)). La segunda cubre «no es uno de los suyos».
//         Test unitario obligatorio (para test-unit-desc.md): con centro y perfilesDeInicio no vacío, profile null
//         → un único mensaje de VAL-ContextoTramitacion-003 y ninguna excepción; y lo mismo con
//         getPerfilesDeInicio devolviendo un Set inmutable (Set.of(CREADOR)).
//   Rama representación:
//     - V-ContextoTramitacion-004 (Origen spec: VAL-ContextoTramitacion-004) presentadoEnRepresentacion null →
//       mensaje de VAL-ContextoTramitacion-004.
//     - si no, V-ContextoTramitacion-005 (Origen spec: VAL-ContextoTramitacion-005) presentadoEnRepresentacion
//       true y tramite.getPermitidoPresentarEnRepresentacion() false → mensaje de VAL-ContextoTramitacion-005.
//   Mensajes: los literales de cada VAL- del spec, traducidos con I18n.get.

public Set<Profile> getPerfilesDeInicio(Tramite tramite, Centro centro);
//   Perfiles con los que el usuario autenticado (SecurityUtil.getUser()) puede iniciar expedientes del trámite
//   en el centro. Total por construcción, con dos ramas explícitas:
//     - centro null → Set.of() SIN llamar a PerfilesUsuarioService: sin centro no hay perfiles de inicio (regla
//       propia de esta función, documentada en su Javadoc; no se apoya en cómo trate el null otra pieza).
//     - centro con valor → perfilesUsuarioService.getPerfilesSobreTramite(tramite, usuario, centro) filtrados con
//       FormaPresentacion::esPerfilDeInicio.
//   Contrato del Set devuelto: nunca contiene null y NO garantiza admitir contains(null); quien consulte un
//   perfil que pueda ser null lo descarta antes (validateTriggerInitialEvent lo hace con esPerfilDeInicio).
//   Único dueño de «qué perfiles de inicio tiene este usuario aquí»: lo usan la validación y la ventana (D2).

public VistaExpediente getVistaExpediente(Expediente expediente, Profile profile);
//   Vista del expediente en su fase actual. Movido de ExpedienteController.triggerInitialEvent (que se borra); su
//   único llamador es NuevoExpedienteServiceImpl.fireActionRule_IniciarExpediente (→ NuevoExpedienteController.crear).
//   ExpedienteController.viewExpediente hace hoy una resolución equivalente y se queda como está (fuera de alcance,
//   D3, Notas §10).
//   EventContext(expediente, profile, modelServiceFactory); phaseEventManager =
//   expedienteLocator.getPhaseEventManager(expediente.getTipoExpediente(), expediente.getCodePhase());
//   viewName = phaseEventManager.getViewName(expediente, eventContext); devuelve
//   VistaExpediente(viewName, phaseEventManager.getModelClass(), expediente, profile).
```

El método `crear(ContextoTramitacion)` desaparece (renombrado); no queda ningún llamador.

Verificación: `grep -rn "expedienteService.crear\|db.ContextoTramitacion" src/main/java/com/educaflow/subsystem/expedientes/services` sin resultados (compilación en el Paso 6).

### Paso 4 — `TipoExpediente` de solo consulta en el servidor

**Crear** en `subsystem/expedientes` (la ubicación la impone `ModelServiceFactory`: `…expedientes.db.TipoExpediente` → `…expedientes.service[.impl]`; D5):

```java
// Clase: com.educaflow.subsystem.expedientes.service.TipoExpedienteService
public interface TipoExpedienteService extends ModelService<TipoExpediente> {}
//   Sin acciones propias: solo existe para que ModelServiceFactory encuentre la implementación.

// Clase: com.educaflow.subsystem.expedientes.service.impl.TipoExpedienteServiceImpl
public class TipoExpedienteServiceImpl extends DefaultModelService<TipoExpediente> implements TipoExpedienteService {

    public TipoExpedienteServiceImpl(Class<TipoExpediente> model, Repository<TipoExpediente> repository);
    //   super(model, repository).

    @Override public TipoExpediente insert(TipoExpediente tipoExpediente);
    @Override public TipoExpediente update(TipoExpediente tipoExpediente, TipoExpediente original);
    @Override public void remove(TipoExpediente tipoExpediente);
    //   Los tres lanzan UnsupportedOperationException incondicional (k-secure-coding §9.2): la entidad nunca
    //   admite la operación, así que no hay flujo normal; es el cinturón si alguien se salta el validate*.

    @Override public Optional<BusinessMessages> validateInsert(TipoExpediente tipoExpediente);
    @Override public Optional<BusinessMessages> validateUpdate(TipoExpediente tipoExpediente, TipoExpediente original);
    @Override public Optional<BusinessMessages> validateRemove(TipoExpediente tipoExpediente);
    //   V-TipoExpediente-001 (Origen spec: RES-TipoExpediente-001): los tres devuelven SIEMPRE un mensaje, sin if.
    //   Mensaje debe transmitir: los tipos de expediente los registra la aplicación al arrancar y no se pueden
    //   crear/modificar/borrar desde la aplicación (tampoco el Administrador).

    @Override public AllowProperties allowPropertiesInsert();
    @Override public AllowProperties allowPropertiesUpdate();
    //   Los dos devuelven AllowProperties.createDenyAllProperties().
    //   Origen spec: Input AllowProperties «ninguna» de las acciones Crear y Modificar de entity-TipoExpediente.md.
    //   Comentario: el endpoint REST automático no acepta ningún campo; complementa a V-TipoExpediente-001.
}
```

Por qué basta y no rompe nada: el data-init que registra los tipos al arrancar no pasa por `ModelServiceFactory` (en AOP solo lo usan `Resource` y `ModelServiceValidationWalker`) y ningún código del proyecto persiste `TipoExpediente` con un `ModelService`.
Es la defensa real también frente al Administrador, que se salta los permisos (`AuthUtils.isAdmin`).

Verificación: `grep -rn "class TipoExpedienteServiceImpl" src/main/java/com/educaflow/subsystem/expedientes/service/impl` encuentra la clase (compilación en el Paso 6).

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

**Borrar** `subsystem/expedientes/services/ContextoTramitacionService.java` (su lógica queda repartida entre `ExpedienteService` y este servicio, D2) y su test `src/test/java/com/educaflow/subsystem/expedientes/services/ContextoTramitacionServiceTest.java`: sus casos de centros ordenados pasan a los tests de `NuevoExpedienteServiceImpl`, y los de papel deducido y opciones de «para quién» a los de `ExpedienteService`, `FormaPresentacion`, `ContextoTramitacion` y `NuevoExpedienteServiceImpl` que describe `test-unit-desc.md`.

Verificación: los 5 headers de bloque de `NuevoExpedienteServiceImpl` con sus 3 líneas de igual longitud (`awk` de `k-sistemas/servicios.md`) (compilación en el Paso 6).

### Paso 6 — Controladores

**Modificar** `subsystem/expedientes/controllers/ExpedienteController.java` (solo el delta; el resto de la clase se conserva):
- **Borrar** el método `triggerInitialEvent(ActionRequest, ActionResponse)`: la creación la atiende ahora `NuevoExpedienteController.crear`, y el motor ya no lee ninguna ventana.
- **MUST NOT** tocarse `viewExpediente`, `triggerEvent` ni `getTabName`: atienden a las pantallas del expediente ya creado, que el spec deja fuera de alcance («se quedan como están») y las guías piden tocar solo lo imprescindible del motor. Siguen construyendo su vista y su título como hoy (D3; la fórmula del título queda repetida con `VistaExpediente.title()` como deuda deliberada, Notas §10).
- Quitar el import de `com.educaflow.subsystem.expedientes.db.ContextoTramitacion`, que queda sin uso, y cualquier otro que quede sin uso **solo** por borrar `triggerInitialEvent`.

**Borrar** `subsystem/expedientes/controllers/ContextoTramitacionController.java`.

**Crear** el controlador del sistema. Solo devuelve valores; **MUST NOT** fijar ningún atributo de vista (`hidden`, `readonly`, `domain`, `title`…): eso lo decide la vista (guías).

```java
// Clase: com.educaflow.system.expedientes.controller.NuevoExpedienteController
public class NuevoExpedienteController {

    // @Inject ModelServiceFactory modelServiceFactory;   única inyección.
    // Todos los métodos: servicio = (NuevoExpedienteService) modelServiceFactory.resolve(NuevoExpediente.class);
    // modelo = new ActionRequestHelper<>(actionRequest, NuevoExpediente.class).getModel(servicio.allowProperties<Acción>()).

    @CallMethod
    public void validatePreparar(ActionRequest actionRequest, ActionResponse actionResponse);
    //   Delegación: servicio.validatePreparar(modelo); si hay mensajes →
    //   actionResponseHelper.doResponseBusinessMessagesAsError(mensajes) (sin título), como exige k-sistemas
    //   (controladores.md, MUST NOT usar actionResponse.setError(String) para errores de negocio). D7: se acepta el
    //   único cambio visual — el mensaje de ESC-006 sale como un elemento de lista (<ul><li>, con viñeta) en vez de
    //   en texto plano como hoy; el texto es el mismo. Acción de la vista: Remote-validatePreparar (al abrirse; ESC-006).

    @CallMethod
    public void preparar(ActionRequest actionRequest, ActionResponse actionResponse);
    //   Delegación: resultado = servicio.preparar(modelo); responde con setValue de nombreTramite, ayudaTramite,
    //   centrosDisponibles (cada centro como mapa {id, name}, en el orden devuelto) y hayUnSoloCentroDisponible.

    @CallMethod
    public void recalcular(ActionRequest actionRequest, ActionResponse actionResponse);
    //   Delegación: servicio.recalcular(modelo); setValue de hayQuePreguntarPresentacion,
    //   presentadoEnPapelDeducido, presentadoEnPapelVigente, sePuedeCrearParaMi, sePuedeCrearEnRepresentacion y
    //   hayQuePreguntarParaQuien.

    @CallMethod
    public void validateCrear(ActionRequest actionRequest, ActionResponse actionResponse);
    //   Delegación: servicio.validateCrear(modelo); si hay mensajes →
    //   actionResponseHelper.doResponseBusinessMessagesAsError(I18n.get(<título de RUI-019>), mensajes)
    //   (U-nuevo-expediente-019: un aviso de error con ese título y un mensaje por línea).

    @CallMethod
    @Transactional
    public void crear(ActionRequest actionRequest, ActionResponse actionResponse);
    //   La transacción abarca también la resolución de la vista: si esta falla, el alta se deshace.
    //   Delegación: vista = servicio.crear(modelo); actionResponseHelper.doResponseViewForm(vista.viewName(),
    //   vista.modelClass(), vista.expediente(), vista.title(), vista.profile().name()); actionResponse.setCanClose(true)
    //   (cierra la ventana y abre el expediente en su primer estado, como hoy).
    //   Manejo de errores (Notas §11). Hoy NUNCA llega aquí una BusinessException:
    //   - Si falla la validación de la puerta (alta que se salta validateCrear, o algo cambió entre validateCrear
    //     y crear), ExpedienteService.triggerInitialEvent lanza jakarta.validation.ValidationException (unchecked:
    //     es lo que lanza BusinessMessages.throwIfInvalid de AOP), que se propaga SIN envolver y Axelor la muestra
    //     como error de validación.
    //   - La BusinessException del InitialEventManager de cada tipo no llega como tal: Tramitador.triggerInitialEvent
    //     (Paso 2, sin cambios en su try/catch) envuelve cualquier Exception salvo UnauthorizedException en
    //     RuntimeException.
    //   - UnauthorizedException se propaga sin envolver para que Axelor responda un error de acceso.
    //   La BusinessException de la firma de servicio.crear solo existe porque la declara Tramitador. El controlador
    //   solo satisface el checked relanzándola envuelta en RuntimeException (MUST NOT capturarla para extraer sus
    //   mensajes, k-sistemas/controladores.md). MUST NOT capturar UnauthorizedException ni ValidationException.
}
```

Verificación: `./gradlew compileJava` (primer punto en el que todo el código compila); `grep -rn "ContextoTramitacionController\|ContextoTramitacionService\|prepararContextoTramitacion" src/main` sin resultados.

### Paso 7 — Vistas

**Crear** `system/expedientes/views/Main-NuevoExpediente.xml` y `system/expedientes/views/Main-TipoExpediente.xml` copiando los de `design/views/`.
**Modificar** `tramites/views/Tramites.xml` copiando `design/views/Tramites.xml`.
**Borrar** `subsystem/expedientes/views/Main-ContextoTramitacion.xml` y `subsystem/expedientes/views/TipoExpediente.xml`.

#### `views/Main-NuevoExpediente.xml` (nuevo)

Un bloque `sysExpedientes.Main@NuevoExpediente`.
- `action-view` `sysExpedientes.Main@NuevoExpediente-action` («Nuevo expediente»): solo form, ventana emergente (`popup`, sin toolbar, `popup-save=false`, `show-confirm=false`), contexto `_tramiteId = id` del trámite pulsado en «Trámites». Ya no lleva `_profile`: el perfil lo decide el servidor.
- `form` `sysExpedientes.Main@NuevoExpediente-form`, `can*="false"`, sin `canBackOnSave` (no tiene `btnSave`), `onNew` en tres etapas `serial:` (D4).
- Acciones principales:
  - `-btnCancel-action` = `close` (U-018).
  - `-btnCrear-action` = `validate` (obligatorios de cliente, U-005) → `Remote-validateCrear` → `Remote-crear`.
  - Etapa `-onNew-action` = `set-tramite-seleccionado` (U-014).
  - Etapa `-onNew-preparar-action` = `Remote-validatePreparar` → `Remote-preparar` → `set-centro-unicoDisponible` (U-003, `if="hayUnSoloCentroDisponible"`).
  - Etapa `-onChangeCentro-action` = `set-presentadoEnPapel-sinMarcar` (parte «arranca sin marcar» de U-007): deja la entrada lista antes de recalcular; no llama al servidor ni consume ningún valor calculado.
  - Etapa `-onChangePresentadoEnPapel-action` = `Remote-recalcular` → `set-presentadoEnPapel-vigente` (U-007, copia sin condición `presentadoEnPapelVigente`: la vista no decide la forma vigente) → `set-presentadoEnRepresentacion-segunOpciones` (U-008, U-009, U-012, U-016) → `set-presentadoEnRepresentacion.title-formaPresentar` (U-010). Es la **única** etapa que recalcula, y todo lo que recibe es definitivo para el estado de la ventana (R-NuevoExpediente-004 y R-NuevoExpediente-005 salen del mismo `getFormaPresentarVigente`), así que ninguna etapa deja valores para que otra los corrija.
  - `set-presentadoEnRepresentacion-segunOpciones` es un único `action-record` total, sin `if`: si `hayQuePreguntarParaQuien`, la deja sin valor; si no, la contesta con la única opción posible (`false` si se puede para mí, `true` si solo en representación) o la deja sin valor si no cabe ninguna.
- Encadenamiento de etapas (a lo sumo una remota por etapa, al principio; las reglas de vista que dependen de lo que devolvió van detrás, en la misma etapa). Nombre de cada etapa (D4): la primera etapa de un evento es `{evento}-action` y las siguientes exclusivas suyas `{evento}-{sufijo}-action`; una etapa compartida por varios eventos se llama por el **único** evento que la dispara él solo (`onChangePresentadoEnPapel`, el `onChange` de `presentadoEnPapel`) y los demás la reutilizan como última etapa de su `serial:`:
  - al abrir: `onNew → onNew-preparar → onChangePresentadoEnPapel`;
  - al cambiar el centro: `onChangeCentro → onChangePresentadoEnPapel`;
  - al cambiar «presentado en papel»: `onChangePresentadoEnPapel`.
- Reglas de vista continuas en los campos: `centro` `domain="self IN (:centrosDisponibles)"` (U-002, parámetro con nombre que AOP resuelve del contexto, sin interpolar ids), `required` (U-005), `readonlyIf="hayUnSoloCentroDisponible"` (U-004; antes de `preparar` vale `false`), `canNew/canEdit/canView="false"` (U-015); `presentadoEnPapel` `showIf="hayQuePreguntarPresentacion"` (U-006); `presentadoEnRepresentacion` `showIf`/`requiredIf` `hayQuePreguntarParaQuien` (U-011, U-013). Ninguna condición se escribe dos veces: cada una es un campo `servidor` que la vista solo consulta.
- La tarjeta del trámite es el `viewer` de `ayudaTramite` con `nombreTramite` como cabecera y la ayuda como HTML (U-001, U-017), igual que hoy.
- Botones: solo «Cancelar» y «Crear expediente», sin Guardar ni Borrar: es la desviación del estándar que declara `screen-nuevo-expediente.md` (el formulario no se guarda).
- Remotas: cinco `action-method` sobre `NuevoExpedienteController` (`validatePreparar`, `preparar`, `recalcular`, `validateCrear`, `crear`); cada `Remote-{X}` llama al método `X`.

ASCII Layout de `sysExpedientes.Main@NuevoExpediente-form` (se conserva el de la ventana actual, que el spec exige que se vea exactamente igual):

```
tramitePanel (sin marco) — los 9 campos hidden no se pintan
aaaaaaaaaaaa   ← ayudaTramite(12): tarjeta con nombre + ayuda del trámite

presentacionPanel «Presentación» — un dibujo por estado
Estado 1: sin preguntas (un solo perfil en el centro y una sola opción de «para quién», o sin centro)
cccccccccccc   ← centro(12)
Estado 2: hay que preguntar cómo se presenta
cccccccccccc   ← centro(12)
pppppppppppp   ← presentadoEnPapel(12) interruptor, showIf
Estado 3: hay que preguntar para quién
cccccccccccc   ← centro(12)
rrrrrrrrrrrr   ← presentadoEnRepresentacion(12) radio, showIf
Estado 4: las dos preguntas
cccccccccccc   ← centro(12)
pppppppppppp   ← presentadoEnPapel(12)
rrrrrrrrrrrr   ← presentadoEnRepresentacion(12)

buttons-panel
.......cckkk   ← colOffset(7) + Cancelar(2) + Crear expediente(3)   [7+2+3 = 12]
```

Cada pregunta condicional está sola en su fila (borde izquierdo sin `colOffset` y borde derecho a la vez), así que su `showIf` va en el propio campo y al ocultarse no desplaza nada.
Las tres filas son preguntas apiladas: el interruptor (título de 57 caracteres) y la pregunta de radio (opción de 80 caracteres) necesitan la fila entera, y el centro ocupa la misma columna para mantener los bordes alineados; es además el layout actual, que el spec pide conservar.
No hay botones secundarios: los dos son principales y quedan pegados al borde derecho (`7 + 2 + 3 = 12`).

#### `views/Main-TipoExpediente.xml` (nuevo)

Un bloque `sysExpedientes.Main@TipoExpediente`.
- `action-view` `sysExpedientes.Main@TipoExpediente-action` («Tipos de expediente»): grid → form, sin toolbar de grid ni de form, sin `forceEdit` (se abre en lectura).
- `grid` `sysExpedientes.Main@TipoExpediente-grid`: columnas `code`, `name`, `tramite`; `canNew="false"` (sin «Nuevo»), `canViewOnClick="true"`, `allowSearchFields="false"` (sin búsqueda), `orderBy="code"`.
- `form` `sysExpedientes.Main@TipoExpediente-form`: panel `TipoExpediente` «Datos» con `code`, `name`, `tramite` en `readonly="true"`; `tramite` con `canNew/canEdit/canView="false"` para que no se pueda abrir ni editar el trámite desde aquí (el spec prohíbe mantener otras tablas); sin Guardar ni Borrar (desviación del estándar que declara `screen-tipos-de-expediente.md`); un único botón «Salir» (`btnCancel` → `back`).
- Quedan fuera `versionExpediente`, `openDate`, `closeDate` (no existen en la entidad) y `basePackageName` (dato interno), como piden las guías.

ASCII Layout de `sysExpedientes.Main@TipoExpediente-form`:

```
panel TipoExpediente «Datos»
ccccnnnntttt   ← code(4) + name(4) + tramite(4)   [código ~34 car., nombre y trámite ~40 car.: longitudes parecidas]
buttons-panel
..........ss   ← colOffset(10) + Salir(2)          [10+2 = 12]
```

#### `views/Tramites.xml` (modificado)

- Preexistente (se conserva): todo el fichero (árbol `sysTramites-nuevo-tree`, ayuda, `cards`).
- Delta: el `onClick` del nodo `Tramite` del árbol y el `$action(...)` del botón de las `cards` pasan de `subsysExpedientes.Main@ContextoTramitacion-action` a `sysExpedientes.Main@NuevoExpediente-action`. Es el único cambio permitido en `tramites/views/` (guías).

Verificación: `bash .claude/skills/sdd-designer/template-system/validate.sh .sdd/drafts/2026-09-19_14-49_sistema-expedientes-y-desacople-tramitador/design` → `VALIDACION-XML: OK`; `grep -rn "Main@ContextoTramitacion\|subsysExpedientes.TipoExpediente@Main" src` sin resultados.

### Paso 8 — Menús

**Modificar** `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`: la línea existente con `name="expedientes-tiposExpedientes-menuitem"` se **sustituye** por la de `design/menus.xml`:
- `action` pasa a `sysExpedientes.Main@TipoExpediente-action`;
- `groups` pasa de `admins,users` a `admins` (solo lo ve el Administrador, ESC-008).

Se conservan su posición, su padre `expedientes-menuitem` y su `order="5"`. El resto del fichero no cambia.

Verificación: `grep -n "expedientes-tiposExpedientes-menuitem" src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` muestra una sola línea con `groups="admins"`.

### Paso 9 — Seguridad

Reglas de acceso en lenguaje natural:
- **Cualquier usuario con perfil de creador o de tramitador sobre un trámite** abre «Nuevo expediente» para ese trámite y solo puede iniciarlo en los centros donde tiene ese perfil, y solo de la forma que su perfil permite. Lo garantiza la puerta del motor (`ExpedienteService.validateTriggerInitialEvent`, V-ContextoTramitacion-001…005) con el usuario del servidor, entre por donde entre la petición; la lista de centros y las preguntas de la ventana son solo comodidad.
- **Administrador**: ve el listado y el detalle de todos los tipos de expediente (no pertenecen a ningún centro, sin filtro). Nadie, tampoco él, puede crear, modificar ni borrar un tipo de expediente: lo impide `TipoExpedienteServiceImpl` (V-TipoExpediente-001); la vista en lectura y el menú solo para `admins` son cortesía.
- **«Solo el Administrador ve la pantalla» se garantiza ÚNICAMENTE con el menú (`groups="admins"`), es una restricción de la vista y es aceptable a propósito (ver «Notas y supuestos» 17).** Un usuario de `users` con un `Ace` sobre algún tipo puede abrir `sysExpedientes.Main@TipoExpediente-action` por URL, pero solo ve las filas que el permiso `TipoExpediente.conAce` (sin cambios) ya le deja leer. No se añade defensa de servidor sobre la visibilidad.

**Crear** `src/main/java/com/educaflow/system/expedientes/data-init/input-config.xml`: `<xml-inputs priority="10">` con un único `<input file="auth-expedientes.xml" root="auth">` que enlaza `permission` → `com.axelor.auth.db.Permission` (`search="self.name = :name"`, `create/update="true"`, binds de `@name`, `@object`, `@condition`, `@conditionParams` y `can/@create|read|write|remove|export`), igual que el de `system/gestioncentro`.

**Crear** `src/main/java/com/educaflow/system/expedientes/data-init/input/auth-expedientes.xml` con un permiso:

| name | object | condition | create | read | write | remove | export |
|---|---|---|---|---|---|---|---|
| `NuevoExpediente.all` | `com.educaflow.system.expedientes.db.NuevoExpediente` | — | true | true | true | false | false |

Con un comentario: `NuevoExpediente` es `persistable="false"` y no tiene filas; quién puede iniciar qué y dónde lo decide `ExpedienteService.validateTriggerInitialEvent`.

**Modificar** `src/main/java/com/educaflow/subsystem/expedientes/data-init/input/auth-expedientes.xml`: borrar el `<permission name="ContextoTramitacion.all">` y su comentario (ya no existe el modelo).
Además, en el comentario del permiso `Tramite.registrador`, sustituir la última frase («Qué centros y qué opciones se le ofrecen al crear lo decide ExpedienteSecurity, que es la misma regla.», clase que ya no existe) por: «Qué centros y qué opciones se le ofrecen al crear lo decide la puerta del motor, ExpedienteService.getPerfilesDeInicio / validateTriggerInitialEvent, que es la misma regla.».
El resto del fichero no cambia.

**Modificar** `src/main/resources/data-init/input/auth.xml`: en los grupos `admins` y `users`, sustituir `<permission name="ContextoTramitacion.all"/>` por `<permission name="NuevoExpediente.all"/>`. Nada más. El data-init global (priority `-1`) carga después del del sistema (priority `10`), así que el permiso ya existe cuando el grupo lo referencia.

`TipoExpediente.all` (grupo `admins`) se deja como está: el grupo `admins` se salta los permisos (`AuthUtils.isAdmin`), así que cambiarlo no protegería nada; la protección es la del Paso 4.

Verificación: arrancar con `./run.sh` y comprobar en el log que el data-init no falla; `grep -rn "ContextoTramitacion.all" src` sin resultados.

### Paso 10 — Verificación final

- Compilar y ejecutar todos los tests (incluidos los de arquitectura y vistas): `./gradlew clean build --info`.
- Sin restos del modelo anterior: `grep -rn "db.ContextoTramitacion\|ContextoTramitacionService\|ContextoTramitacionController\|Main@ContextoTramitacion\|subsysExpedientes.TipoExpediente@Main" src` sin resultados.
- Sin warnings de Error Prone en los ficheros tocados (`agent_docs/deploy.md`, «Análisis estático»).
- Arrancar con `./run.sh` y comprobar que el data-init carga sin errores. Los tests E2E **no** se ejecutan en esta iniciativa (ver «Tests»).

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

### DTO de alta programática — `ContextoTramitacion`

Record del motor con exactamente cuatro componentes; el propio record **es** la whitelist (`k-secure-coding` §3.5): no existe ningún otro campo que un llamador pueda colar.

| Componente | Origen | Justificación |
|---|---|---|
| `tramite` | cliente | Obligatorio por constructor (error de programación si falta). |
| `centro` | cliente | Validado en la puerta: V-ContextoTramitacion-001/002. |
| `profile` | cliente (alta programática) / servidor (ventana, R-NuevoExpediente-001) | Validado en la puerta: V-ContextoTramitacion-003 (solo creador o tramitador, y uno de los que el usuario tiene ahí). |
| `presentadoEnRepresentacion` | cliente | Validado en la puerta: V-ContextoTramitacion-004/005. |

`presentadoEnPapel` ya no es un componente: se deduce del perfil (R-ContextoTramitacion-001). El usuario que inicia no es un componente: es siempre `SecurityUtil.getUser()` en el servidor.

## Trazabilidad Origen spec → V/R/U → ubicación

### Validaciones (V)

| V | Origen spec | Ubicación |
|---|---|---|
| V-ContextoTramitacion-001 | VAL-ContextoTramitacion-001 | `ExpedienteService.validateTriggerInitialEvent` (rama centro) |
| V-ContextoTramitacion-002 | VAL-ContextoTramitacion-002 | `ExpedienteService.validateTriggerInitialEvent` (rama centro, con centro) |
| V-ContextoTramitacion-003 | VAL-ContextoTramitacion-003 | `ExpedienteService.validateTriggerInitialEvent` (rama centro, con perfiles de inicio) |
| V-ContextoTramitacion-004 | VAL-ContextoTramitacion-004 | `ExpedienteService.validateTriggerInitialEvent` (rama representación) |
| V-ContextoTramitacion-005 | VAL-ContextoTramitacion-005 | `ExpedienteService.validateTriggerInitialEvent` (rama representación, con respuesta) |
| V-NuevoExpediente-001 | VAL-NuevoExpediente-001 | `NuevoExpedienteServiceImpl.validatePreparar` (vista: `Remote-validatePreparar` al abrir) |
| V-NuevoExpediente-002 | VAL-ContextoTramitacion-001, VAL-ContextoTramitacion-002, VAL-ContextoTramitacion-003, VAL-ContextoTramitacion-004, VAL-ContextoTramitacion-005 | `NuevoExpedienteServiceImpl.validateCrear` → `ExpedienteService.validateTriggerInitialEvent` (vista: `Remote-validateCrear` en `btnCrear`) |
| V-TipoExpediente-001 | RES-TipoExpediente-001 | `TipoExpedienteServiceImpl.validateInsert` / `validateUpdate` / `validateRemove` (+ `insert`/`update`/`remove` con `UnsupportedOperationException`) |

### Reglas de negocio (R)

| R | Origen spec | Ubicación |
|---|---|---|
| R-ContextoTramitacion-001 | RN-ContextoTramitacion-001 | `ContextoTramitacion.isPresentadoEnPapel` (equivalencia en `FormaPresentacion.fromProfile`), aplicado en `Tramitador.triggerInitialEvent` (Antes de `JPA.save`) |
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

### Reglas de UI (U) — pantalla `nuevo-expediente`

Todas en `views/Main-NuevoExpediente.xml`; los nombres de acción omiten el prefijo `sysExpedientes.Main@NuevoExpediente-`.

| U | Origen spec | Ubicación |
|---|---|---|
| U-nuevo-expediente-001 | RUI-nuevo-expediente-formulario-001 | `viewer` del campo `ayudaTramite` (`depends="nombreTramite,ayudaTramite"`), rellenado por `Remote-preparar` en la etapa `onNew-preparar` |
| U-nuevo-expediente-002 | RUI-nuevo-expediente-formulario-002 | domain declarativo `self IN (:centrosDisponibles)` en el campo `centro` (continuo) |
| U-nuevo-expediente-003 | RUI-nuevo-expediente-formulario-003 | `set-centro-unicoDisponible-action` (`action-record` con `if="hayUnSoloCentroDisponible"`), etapa `onNew-preparar` |
| U-nuevo-expediente-004 | RUI-nuevo-expediente-formulario-004 | `readonlyIf="hayUnSoloCentroDisponible"` en `centro` |
| U-nuevo-expediente-005 | RUI-nuevo-expediente-formulario-005 | `required="true"` en `centro` + acción predefinida `validate` en `btnCrear-action` |
| U-nuevo-expediente-006 | RUI-nuevo-expediente-formulario-006 | `showIf="hayQuePreguntarPresentacion"` en `presentadoEnPapel` |
| U-nuevo-expediente-007 | RUI-nuevo-expediente-formulario-007 | `set-presentadoEnPapel-sinMarcar-action` (etapa `onChangeCentro`: sin marcar al cambiar el centro) + `set-presentadoEnPapel-vigente-action` (copia sin condición `presentadoEnPapelVigente`, que decide el servidor, etapa `onChangePresentadoEnPapel`: toma la forma deducida al abrir y al cambiar el centro); sin forma deducida el servidor devuelve la de la ventana, que `onChangeCentro` dejó sin marcar (o la que contestó el usuario) |
| U-nuevo-expediente-008 | RUI-nuevo-expediente-formulario-008 | `set-presentadoEnRepresentacion-segunOpciones-action` (sin valor si `hayQuePreguntarParaQuien`) en la etapa `onChangePresentadoEnPapel`, que cierra el `serial:` del `onChange` de `centro` |
| U-nuevo-expediente-009 | RUI-nuevo-expediente-formulario-009 | `set-presentadoEnRepresentacion-segunOpciones-action` (sin valor si `hayQuePreguntarParaQuien`) en la etapa `onChangePresentadoEnPapel` (`onChange` de `presentadoEnPapel`) |
| U-nuevo-expediente-010 | RUI-nuevo-expediente-formulario-010 | `set-presentadoEnRepresentacion.title-formaPresentar-action` (`action-attrs` `title` con `if` por `presentadoEnPapel`), etapa `onChangePresentadoEnPapel` (corre al abrir y en cada cambio del que depende) |
| U-nuevo-expediente-011 | RUI-nuevo-expediente-formulario-011 | `showIf="hayQuePreguntarParaQuien"` en `presentadoEnRepresentacion` (sin centro vale `false`: R-NuevoExpediente-005) |
| U-nuevo-expediente-012 | RUI-nuevo-expediente-formulario-012 | `set-presentadoEnRepresentacion-segunOpciones-action` (sin `hayQuePreguntarParaQuien`, contestada con la única opción posible), etapa `onChangePresentadoEnPapel` |
| U-nuevo-expediente-013 | RUI-nuevo-expediente-formulario-013 | `requiredIf="hayQuePreguntarParaQuien"` en `presentadoEnRepresentacion` (el mismo campo que su `showIf`) |
| U-nuevo-expediente-014 | RUI-nuevo-expediente-formulario-014 | `set-tramite-seleccionado-action` (etapa `onNew`, desde `_tramiteId`) + campo `tramite` `hidden` |
| U-nuevo-expediente-015 | RUI-nuevo-expediente-formulario-015 | `canNew="false" canEdit="false" canView="false"` en `centro` |
| U-nuevo-expediente-016 | RUI-nuevo-expediente-formulario-016 | `set-presentadoEnRepresentacion-segunOpciones-action` (sin valor si `hayQuePreguntarParaQuien`) en la última etapa del `onNew` + `nullable="true"` en el dominio (no nace a `false`) |
| U-nuevo-expediente-017 | RUI-nuevo-expediente-formulario-017 | `viewer` de `ayudaTramite` con `dangerouslySetInnerHTML` |
| U-nuevo-expediente-018 | RUI-nuevo-expediente-formulario-018 | `btnCancel-action` = `close` + `view-param show-confirm=false` en el `action-view` |
| U-nuevo-expediente-019 | RUI-nuevo-expediente-formulario-019 | `NuevoExpedienteController.validateCrear` → `ActionResponseHelper.doResponseBusinessMessagesAsError(título, mensajes)` (lista `<ul>`, un mensaje por línea); `.crear` no captura `BusinessException` (Notas §11). Único U fuera del XML: el aviso de error lo compone el servidor al devolver los mensajes. |

## Tests

- **Tests unitarios** (JUnit + Mockito): descritos en `test-unit-desc.md` (lo materializa una fase posterior del pipeline).
- **Tests E2E**: descritos en `test-e2e-desc.md`, uno por escenario del spec. **MUST NOT** ejecutarse ni persistirse en esta iniciativa: las guías prohíben lanzar `/sdd-debug-with-test-e2e-desc` y `/sdd-create-tests-e2e` porque `PerfilesUsuarioServiceImpl` aún no está terminada (D6).

## Reglas del spec descartadas

Ninguna.

## Eliminaciones declaradas

| Elemento eliminado | Fichero | Justificación (spec) |
|---|---|---|
| Fichero completo (entidad `ContextoTramitacion` como modelo Axelor, con `nombreTramite`, `ayudaTramite`, `presentadoEnPapel`) | `subsystem/expedientes/views-models/ContextoTramitacion.xml` | `entity-ContextoTramitacion.md` «Qué cambia»: se retiran nombre, ayuda y presentado en papel; pasa a ser solo la entrada del tramitador (D1) |
| Fichero completo (ventana actual) | `subsystem/expedientes/views/Main-ContextoTramitacion.xml` | Objetivo del spec: la ventana pasa al sistema nuevo con el modelo `NuevoExpediente` |
| Fichero completo (pantalla actual de tipos) | `subsystem/expedientes/views/TipoExpediente.xml` | `screen-tipos-de-expediente.md`: «Sustituye a la pantalla actual del mismo nombre» |
| Clase completa | `subsystem/expedientes/controllers/ContextoTramitacionController.java` | Objetivo del spec + guías («de él salen ContextoTramitacionController…») |
| Clase completa | `subsystem/expedientes/services/ContextoTramitacionService.java` | Guías («de él salen … ContextoTramitacionService») |
| Método `triggerInitialEvent(ActionRequest, ActionResponse)` | `subsystem/expedientes/controllers/ExpedienteController.java` | Objetivo del spec: el tramitador deja de depender de la ventana |
| Método `crear(ContextoTramitacion)` (renombrado a `triggerInitialEvent`) | `subsystem/expedientes/services/ExpedienteService.java` | Guías («`ExpedienteService.crear` se renombra a `triggerInitialEvent`») |
| Permiso `ContextoTramitacion.all` | `subsystem/expedientes/data-init/input/auth-expedientes.xml` | El modelo deja de existir (D1) |
| Referencias `ContextoTramitacion.all` en los grupos `admins` y `users` (sustituidas por `NuevoExpediente.all`) | `src/main/resources/data-init/input/auth.xml` | El modelo de la ventana pasa a ser `NuevoExpediente` |
| `groups="admins,users"` del menú «Tipos Expedientes» (pasa a `admins`) | `secretariavirtual/menus/menus.xml` | `screen-tipos-de-expediente.md` «lo ve solo el Administrador»; ESC-008 |
| Test completo | `src/test/java/com/educaflow/subsystem/expedientes/services/ContextoTramitacionServiceTest.java` | La clase probada desaparece (Paso 5) |

## Notas y supuestos

1. **Toques al motor (`subsystem/expedientes`).** El diseño toca el motor solo donde el spec y las guías lo piden (sacar la ventana, validar en la puerta, reducir el contexto) y en `TipoExpedienteService` (D5).
   Ninguno añade una capacidad nueva (ni punto de extensión, ni tipo de acción, ni capacidad de la máquina de estados): `ContextoTramitacion` sustituye al modelo anterior y `FormaPresentacion` a la equivalencia perfil ↔ papel que hoy está repartida; los métodos nuevos de `ExpedienteService` quedan en `validateTriggerInitialEvent`, `getPerfilesDeInicio` y `getVistaExpediente` (la validación de la puerta que piden las guías y lógica movida desde las clases que salen; lo que solo necesita la ventana, como la lista de centros, vive en el sistema), y `TipoExpedienteService` protege un dato del propio motor.
   Aun así, `TipoExpedienteService` crea un paquete `service/` (singular) en el motor junto al `services/` (plural): el humano debería confirmarlo antes de implementar (el `CLAUDE.md` del motor pide consultar cualquier ampliación).
2. **Documentación que queda desactualizada** (no se toca aquí: son skills y exige `/k-skill`): `.claude/skills/k-tipo-expediente/perfiles.md` (habla de `profile` y `presentadoEnPapel` del `ContextoTramitacion` «en sintonía» y de `ContextoTramitacionService`) y `.claude/skills/k-vistas/tree.md` (ejemplo con `subsysExpedientes.Main@ContextoTramitacion-action`). Conviene actualizarlos en un cambio aparte.
3. **Receta pendiente del patrón nuevo de D4** (`serial:` como frontera de etapa en eventos de formulario): `k-vistas/actions.md` y `agent_docs/view-rules.md` (`VAR-7.1`) solo contemplan `serial:` en botones con AutoFirma. No se modifican en esta iniciativa. La receta incluirá cómo se nombran las etapas de un `serial:` de evento: la primera etapa de un evento es `{evento}-action`, las siguientes exclusivas suyas `{evento}-{sufijo}-action` (análogo a `VAR-7.1`), y una etapa compartida por varios eventos lleva el nombre del evento que la dispara él solo (aquí `onChangePresentadoEnPapel`), reutilizada como última etapa por los demás.
4. **El sistema nuevo queda fuera de los tests automáticos de vistas y de arquitectura.** `ViewFiles.PAQUETES_EXENTOS` excluye cualquier ruta con un segmento `expedientes`, y los tests ArchUnit excluyen `..expedientes..`: al llamarse igual que el subsistema, `system/expedientes` también queda exento. El diseño sigue el estándar igualmente; afinar esas exenciones (p. ej. a `subsystem/expedientes`) queda para la iniciativa de renombrado del subsistema a `tramitador`, que lo resuelve sola.
5. **Nombre de módulo del dominio:** `sysexpedientes` (paquete `com.educaflow.system.expedientes.db`), distinto del `expedientes` del subsistema para no mezclar los dos módulos mientras conviven con el mismo nombre de carpeta (como `gestioncentro` usa `gestion`).
6. **`btnCancel` de «Nuevo expediente» usa `close`, no `back`** (lo que `VAR-7.2` pide a un maestro): es una ventana emergente y el spec pide cerrarla sin preguntar (RUI-018).
7. **«Salir» en el formulario de tipos de expediente** (Origen spec: —): el formulario de solo lectura necesita un botón para volver al listado porque la toolbar está oculta (`VAR-6.1`); no es Guardar ni Borrar, que es lo que el spec excluye.
8. **`orderBy="code"` en el listado de tipos:** el spec dice «sin orden definido»; `VAR-5.1` exige `orderBy` en todo grid y el código es la clave natural.
9. **Orden del desplegable de centros (ESC-002):** lo da la búsqueda de `Centro` restringida por el domain declarativo con parámetro `self IN (:centrosDisponibles)` (AOP resuelve el parámetro con nombre del contexto; no se interpolan ids en el JPQL); `centrosDisponibles` va además ordenado por nombre.
10. **Deuda temporal deliberada: resolución de la vista y fórmula del título repetidas (D3).** `ExpedienteService.getVistaExpediente` / `VistaExpediente.title()` recogen solo lo que hacía `ExpedienteController.triggerInitialEvent` (que se borra) y solo los usa el alta desde «Nuevo expediente». `ExpedienteController.viewExpediente`, `triggerEvent` y `getTabName` se quedan **exactamente** como están, así que la fórmula del título (número + «-» + nombre traducido del tipo) vive a la vez en `VistaExpediente.title()` y en `getTabName`, y `viewExpediente` hace una resolución de vista equivalente a `getVistaExpediente`.
    Es **deliberado y no es un olor a corregir en esta iniciativa**: el spec deja fuera de alcance, de forma explícita, reorganizar las piezas del tramitador que atienden a las pantallas del expediente ya creado, y las guías piden tocar en `subsystem/expedientes` solo lo imprescindible. Unificar las dos copias (que `viewExpediente`/`triggerEvent` pinten con `VistaExpediente` y desaparezca `getTabName`) queda para la iniciativa de renombrado del subsistema a `tramitador`.
11. **`NuevoExpedienteController.crear` no captura `BusinessException`** (`k-sistemas/controladores.md`, MUST NOT): la relanza envuelta en `RuntimeException`.
    Hoy `ExpedienteController.triggerInitialEvent` la captura y la pinta con el título de RUI-019, pero esa captura ya es código muerto para los errores del evento inicial: `Tramitador.triggerInitialEvent` envuelve cualquier `Exception` (incluida la `BusinessException` del `InitialEventManager`) en `RuntimeException`, salvo `UnauthorizedException`.
    Hoy no llega al controlador ninguna `BusinessException`: la `BusinessException` de la firma de `crear` solo existe porque la declara `Tramitador`, y el controlador solo satisface el checked relanzándola envuelta.
    Si falla la validación de la puerta (un alta que se salta `validateCrear`), `ExpedienteService.triggerInitialEvent` lanza `jakarta.validation.ValidationException` (unchecked, la que lanza `BusinessMessages.throwIfInvalid` de AOP), que se propaga sin envolver y Axelor muestra como error de validación; en la ventana ese caso no se da porque `validateCrear` ya enseña antes los mismos mensajes con el título de RUI-019.
    El controlador **MUST NOT** capturar `UnauthorizedException` ni `ValidationException`.
    Que los errores de negocio del evento inicial salieran con el aviso de RUI-019 exigiría que `Tramitador` dejara pasar `BusinessException` sin envolverla: es un cambio del motor que no forma parte del alcance y habría que decidirlo aparte.
12. **`validateRecalcular` sin reglas:** es el único validador vacío; existe por el contrato «acción + validador» de `k-sistemas/servicios.md`; el spec no define validaciones para «Recalcular».
13. **`presentadoEnRepresentacion` en `ContextoTramitacion` es `Boolean`** (no `boolean`) porque «no contestado» es un estado que `V-ContextoTramitacion-004` tiene que poder ver.
14. **Fila antigua de permiso:** el data-init hace upsert y no borra, así que la fila `ContextoTramitacion.all` de la tabla de permisos queda huérfana en las bases de datos existentes (sin efecto: su objeto ya no existe). Borrarla es opcional y no forma parte del diseño.
15. **Datos de demostración:** no se añaden (fuera de alcance). Los escenarios con un usuario con los dos perfiles o un trámite con representación no se pueden reproducir con los datos actuales; `test-e2e-desc.md` solo describe los del spec.
16. **Error de «Nuevo expediente» sin centros (ESC-006) con viñeta — PUNTO A CONFIRMAR POR EL HUMANO (D7).** `NuevoExpedienteController.validatePreparar` responde con `actionResponseHelper.doResponseBusinessMessagesAsError(mensajes)`, como exige `k-sistemas/controladores.md` (MUST NOT `setError(String)` para errores de negocio) y piden las guías («sigue al 100 % el estándar de `k-sistemas`»).
    Coste aceptado: el mensaje «No puede crear expedientes de este trámite en ninguno de sus centros» sale como un elemento de lista con viñeta, cuando hoy sale en texto plano; el texto no cambia. Choca con la guía «el comportamiento visible MUST conservarse exactamente igual»: si el humano prefiere conservar el texto plano, la alternativa (`setError(texto)`) es una desviación declarada del MUST NOT de `k-sistemas` (ver D7).
17. **Visibilidad de «Tipos de expediente» solo por menú (aceptado a propósito, no es un defecto a reportar).** El spec dice que la pantalla es «visible únicamente para el Administrador»; el diseño lo cumple solo ocultando el menú (`groups="admins"`, Paso 8), sin defensa en el servidor. Motivos: (a) el grupo `users` tiene que conservar la lectura `TipoExpediente.conAce` porque otras pantallas del expediente (tramitación, listados de expedientes) leen el tipo de expediente; (b) ese permiso ya filtra las filas a los tipos sobre los que el usuario tiene un `Ace` en uno de sus centros, así que abrir la acción por URL no enseña ningún dato que el usuario no pudiera leer ya por REST o por esas pantallas; (c) la pantalla es de solo lectura y la escritura la bloquea el servidor para todos (`TipoExpedienteServiceImpl`, V-TipoExpediente-001, D5). Una defensa real (un `domain` o permiso sobre la acción solo para `admins`) añadiría piezas sin proteger ningún dato. Si en el futuro la pantalla enseñara datos que `conAce` no da, habría que añadirla.
