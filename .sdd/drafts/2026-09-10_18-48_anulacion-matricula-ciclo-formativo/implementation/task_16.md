---
type: implementation-task
template: expediente
---

# Tarea 16 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

Modifica las **cuatro bandejas genéricas existentes** y el `menus.xml`:

| Fichero a modificar | Cambio |
|---|---|
| `src/main/java/com/educaflow/tramites/views/Expediente-pendiente.xml` | Filtro de centro en el `<domain>`, **sin** excepción para el administrador (16.3 (a)) |
| `src/main/java/com/educaflow/tramites/views/Abierto-Expediente.xml` | Filtro de centro en los dos `<node domain=…>` (16.3) |
| `src/main/java/com/educaflow/tramites/views/Cerrado-Expediente.xml` | Filtro de centro en el `<domain>`, con excepción para el administrador (16.3) |
| `src/main/java/com/educaflow/tramites/views/Expediente-search.xml` | Filtro de centro en los dos `<node domain=…>` (16.3) |
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Los dos `menuitem` nuevos bajo «Expedientes» (16.4) |

**En los cuatro ficheros de vista existentes y en el `menus.xml` se inserta SOLO el fragmento que el paso indica, sobre la expresión real que hoy tiene cada uno, conservando todo lo demás.** **MUST NOT** reescribirse ninguno entero ni tocarse nada que el paso no nombre.

**CRITICAL — la excepción del administrador va en TRES de las cuatro bandejas, no en las cuatro**: **MUST NOT** llevarla «Expedientes Pendientes» (`Expediente-pendiente.xml`), que fija `_profile=CREADOR`.

**Nota del descomponedor (decisión documentada):** esta tarea es la segunda mitad del `### Paso 16` del diseño (los ficheros **existentes**: 16.3, 16.3-bis y 16.4); las piezas **nuevas** (16.0, 16.1 y 16.2) las materializa la tarea 15. El texto introductorio del Paso 16 va verbatim en las dos.

## Filas de la tabla `## 6. Ficheros a crear o modificar` del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/tramites/views/Expediente-pendiente.xml` | Modificar | `k-vistas` | Filtro de centro en el `<domain>`, **sin** excepción para el administrador: es la única genérica que fija `_profile=CREADOR` (§12.2 y Paso 16.3 (a)) |
| `src/main/java/com/educaflow/tramites/views/Abierto-Expediente.xml` | Modificar | `k-vistas` | Filtro de centro en los dos `<node domain=…>` del árbol; su `action-view` no tiene `<domain>` (§12) |
| `src/main/java/com/educaflow/tramites/views/Cerrado-Expediente.xml` | Modificar | `k-vistas` | Filtro de centro en el `<domain>`, con excepción para el administrador (§12) |
| `src/main/java/com/educaflow/tramites/views/Expediente-search.xml` | Modificar | `k-vistas` | Filtro de centro en los dos `<node domain=…>` de la búsqueda (§12) |
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | `k-vistas` | Dos `menuitem` nuevos bajo «Expedientes» para las dos bandejas |

## Paso del diseño (verbatim) — introducción del Paso 16 y apartados 16.3, 16.3-bis y 16.4

### Paso 16 — Bandejas de `SECRETARIO` y `DIRECTOR`, filtro de centro y menús

Este paso toca **seis** ficheros de vista del árbol más el `menus.xml`, y crea además la pieza de servidor a la que las dos bandejas nuevas preguntan (16.0). Los **dos ficheros de vista nuevos** van escritos enteros en 16.1 y 16.2: se crean **con ese contenido exacto**, no se interpretan. En los **cuatro existentes** y en el `menus.xml` se inserta **solo** el fragmento que se indica abajo, sobre la expresión real que hoy tiene cada uno, conservando todo lo demás (16.3 y 16.4). El apartado 16.3-bis declara la decisión de arquitectura que esto abre: dónde viven las vistas y los menús de un trámite concreto.


#### 16.3 — Filtro de centro en las cuatro bandejas existentes

Estas cuatro llevan **solo** el filtro de centro: no fijan ningún perfil propio de este trámite (pasan `CREADOR` o `RESPONSABLE`, que son los perfiles genéricos de la plataforma), así que la condición de perfil de §16.0 **MUST NOT** añadírseles.

**El `<context name="centroActivoId">` va en las cuatro; el `esAdministrador`, solo en tres.** El segundo se llama `esAdministrador` —el **hecho**, no una de sus consecuencias— para que se lea igual que la exención equivalente del servidor (`SecurityUtil.isAdmin` en `checkPerfilDelEstado`) y para no tener dos nombres del mismo hecho. Los dos `<context>` van dentro del `<action-view>`, junto al `_profile` que ya tiene:

```xml
        <context name="centroActivoId" expr="eval: __user__?.centroActivo?.id"/>
        <context name="esAdministrador" expr="eval: __user__?.group?.code == 'admins'"/>
```

**CRITICAL — «Expedientes Pendientes» (a) lleva SOLO el primero de esos dos `<context>` y su `<domain>` NO exceptúa al administrador.** Es la única de las cuatro que fija `_profile=CREADOR` (`Expediente-pendiente.xml` línea 10); las otras tres fijan `_profile=RESPONSABLE`, un perfil que este tipo declara en `ACEPTADA` y `RECHAZADA` pero para el que **ninguna fase escribe un `<form … profile="RESPONSABLE">`**, así que por ellas el expediente se abre siempre en la vista genérica de solo lectura. `CREADOR`, en cambio, **sí** tiene form en `DATOS_SOLICITUD` y en `PENDIENTE_FIRMA`, y `PhaseEventManager.getViewName` (`subsystem/expedientes/services/eventmanager/PhaseEventManager.java`) devuelve el form del perfil recibido en cuanto existe: exceptuar ahí al administrador le abriría la pantalla **editable** del creador en **cualquier** centro. **Disparar** desde ella ya no puede: los **cuatro** eventos de esos dos estados —`CONTINUAR`, `DELETE`, `VOLVER` y `PRESENTAR`— llevan la guarda `ControlDeAcceso.exigeSerElCreador` (§9.1 y §11, «Las cuatro guardas de autoría»), que es lo único que le para porque `checkPerfilDelEstado` le exime. Sin la excepción, además, la **exhibición** de esa pantalla queda acotada a los expedientes de su centro activo — **y no desaparece**: dentro de ese centro la pantalla editable se le sigue pintando, igual que a todo el que pueda leer el expediente (§12.2 y §14 nota 8(d)). Ningún escenario lo pide por esta bandeja: el administrador consulta por «Expedientes Esperando», «Expedientes Cerrados» y la búsqueda (ESC-025, T-038), que sí le exceptúan.

**(a) `Expediente-pendiente.xml`** — en `subsysExpedientes.Expediente@Pendiente-action`, el `<domain>` pasa de `self.abierto=true ` a:

```xml
        <domain>self.abierto=true AND self.centro.id = :centroActivoId</domain>
```

   **Sin excepción de administrador, y por tanto con un solo `<context>` nuevo** (`centroActivoId`): en esta bandeja **MUST NOT** añadirse el `<context name="esAdministrador">`, porque no se usa. El motivo está arriba, en el `CRITICAL` de este apartado.

**(b) `Cerrado-Expediente.xml`** — en `subsysExpedientes.Expediente@Cerrados-action`, el `<domain>` pasa de `self.abierto=false ` a:

```xml
        <domain>self.abierto=false AND (self.centro.id = :centroActivoId OR :esAdministrador = true)</domain>
```

**(c) `Abierto-Expediente.xml`** — su `action-view` `subsysExpedientes.Expediente@Esperando-action` **no tiene `<domain>`**: filtra en los dos `<node>` del `<tree>`, y ahí es donde hay que tocar (además de los dos `<context>` en el `action-view`):

```xml
        <node model="com.educaflow.subsystem.expedientes.db.Tramite" domain="EXISTS (SELECT 1 FROM Expediente e WHERE e.abierto=true AND (e.centro.id = :centroActivoId OR :esAdministrador = true) AND e.tipoExpediente.tramite=self)" >
```

```xml
        <node model="com.educaflow.subsystem.expedientes.db.Expediente" domain="abierto=true AND (centro.id = :centroActivoId OR :esAdministrador = true)" parent="tipoExpediente.tramite" draggable="false" onClick="subsysExpedientes-event-view-action">
```

   **CRITICAL — el segundo `<node>` NO lleva el prefijo `self.`.** Su `domain` real es hoy `domain="abierto=true"`, sin alias, y este paso **solo añade** la condición de centro conservando esa sintaxis; el `<node>` hermano de `Tramite` sí usa alias (`e.abierto`) porque el suyo es una subconsulta con su propio `FROM`. Cambiarlo a `self.` sería reescribir calladamente una expresión que hoy funciona, con una sintaxis que ese nodo no usa. **MUST NOT** «uniformarse» con los `<domain>` de los `action-view`.

**(d) `Expediente-search.xml`** — la búsqueda de expedientes es **una lista más** por la que un usuario de otro centro ve el expediente ajeno, y ESC-021 exige que no aparezca «ni en esa lista ni en ninguna otra lista de expedientes que la administrativa de CIPFP Batoi pueda abrir». Los dos `<context>` van en `subsysExpedientes.Expediente@PruebaSearch-buscar-action` (que ya fija `_profile=RESPONSABLE`), y sus dos `<node>` **conservan** su condición actual sobre `abierto` y `fechaUltimoEstado`:

```xml
        <node model="com.educaflow.subsystem.expedientes.db.Tramite" domain="EXISTS (SELECT 1 FROM Expediente e WHERE e.abierto=:estado and ((:anyo is null) OR (:anyo=0) OR (YEAR(e.fechaUltimoEstado)=:anyo)) AND (e.centro.id = :centroActivoId OR :esAdministrador = true) AND e.tipoExpediente.tramite=self)" >
```

```xml
        <node model="com.educaflow.subsystem.expedientes.db.Expediente" domain="(self.abierto=:estado) and ((:anyo is null) OR (:anyo=0) OR (YEAR(self.fechaUltimoEstado)=:anyo)) and (self.centro.id = :centroActivoId OR :esAdministrador = true)" parent="tipoExpediente.tramite" draggable="false" onClick="subsysExpedientes-event-view-action">
```

**MUST NOT** acotarse por trámite ninguna de estas cuatro: son bandejas **genéricas** de la plataforma y sirven a todos los tipos de expediente. Solo se les añade el centro.

#### 16.3-bis — DECISIÓN DE ARQUITECTURA PENDIENTE DE APROBAR: dónde viven las vistas y los menús de un trámite concreto

**Qué se está haciendo, dicho sin rodeos.** Este paso crea **dos ficheros de vista específicos de este trámite** (`<domain>` con `tramite.code = 'AnulacionMatriculaCicloFormativo'`, `codeState = 'PENDIENTE_FIRMA_DIRECTOR'`, títulos «Anulaciones de matrícula…») dentro de `src/main/java/com/educaflow/tramites/views/`, que `tramites/util/CLAUDE.md` describe como **recursos XML compartidos** y donde hoy los cinco ficheros existentes son **genéricos**; y añade al `menus.xml` **global** dos `menuitem` de este trámite, visibles para todo usuario de todo centro. Es exactamente la señal que `subsystem/expedientes/CLAUDE.md` marca como «esta pieza no es de aquí» —«nombra un trámite, una fase, un estado, un evento, un perfil o un campo concretos» → «la necesita un solo tipo de expediente → su carpeta de versión»—, y roza la misma regla que este diseño enuncia en §16.3 y en §14 nota 8(d) para prohibir parchear desde un trámite una bandeja compartida.

**Por qué se hace igualmente.** No hay hoy ningún otro sitio donde puedan vivir: las vistas de la carpeta de versión están en el **formato preprocesado** de un tipo de expediente (`<form state= profile=>`, `<include-panels>`, `<footer>`), que no admite un `action-view` ni un `grid` de Axelor; y un `menuitem` **MUST** estar en un `menus.xml` que el build cargue, y el único que existe es el global. Es decir: el patrón no es una preferencia del diseño, es lo único disponible.

**Qué cuesta.** Cada trámite que declare perfiles propios añadirá **dos ficheros y dos menús** al común, y cada uno de esos menús lo verá todo usuario de todos los centros (con la lista vacía si no ostenta el perfil, §16.0 y 16.4). Con N trámites así, `tramites/views/` deja de ser «los recursos compartidos» y pasa a ser un cajón mezclado, y el menú «Expedientes» crece con una entrada por trámite y perfil.

**Cuál sería su sitio definitivo.** El mismo que cierra la nota 8: cuando la bandeja pase el **perfil real del usuario** en vez de fijarlo, deja de hacer falta una bandeja por perfil y por trámite, y estas dos vistas y estos dos menús **desaparecen** sin sustituto. Si esa solución tardara, el sitio propio sería una carpeta de vistas **por trámite** —`tramites/<tramite>/views/`, cargada por el build igual que `tramites/views/`— y un mecanismo de menús por trámite; las dos cosas son capacidades del **motor** y del build, no de este trámite, así que **MUST NOT** darse por hechas aquí.

**Este diseño no aprueba el patrón: lo declara.** Queda escrito también en §14 nota 26 y en `decisiones.md` **D11**, con el mismo formato de «decisión de arquitectura pendiente de aprobar» que se usa para el `_profile`.

#### 16.4 — Los dos `menuitem`

En `secretariavirtual/menus/menus.xml`, a continuación de `expedientes-pruebaBusqueda-menuitem` y con la **misma sangría de cuatro espacios** que sus hermanos (`order` 1…6 ya usados, así que estos van con 7 y 8):

```xml
    <menuitem name="expedientes-anulacionesMiCentro-menuitem" parent="expedientes-menuitem" title="Anulaciones de matrícula de mi centro" action="subsysExpedientes.Expediente@Revision-action" groups="admins,users" order="7"/>
    <menuitem name="expedientes-anulacionesMiFirma-menuitem" parent="expedientes-menuitem" title="Anulaciones de matrícula pendientes de mi firma" action="subsysExpedientes.Expediente@Firma-action" groups="admins,users" order="8"/>
```

`menus.xml` **sí** está sujeto a `agent_docs/view-rules.md` (Categoría 10): de ahí el sufijo `-menuitem`, el prefijo del padre en el `name`, el `groups` canónico `admins,users`, el `order` entero único entre hermanos, el orden fijo de atributos y una sola línea por menú.

**CRITICAL — los dos menús los ve TODO usuario, y es correcto que así sea.** `VAR-10.1` admite en `groups` exactamente `admins`, `admins,users` o `users`, así que **no se puede** restringir un menú a «los que ostentan el perfil `SECRETARIO`»: un `groups` con un grupo propio rompería los tests de `com.educaflow.views`. Quien no ostenta el perfil ve la entrada de menú y abre **una lista vacía**, porque la condición de perfil de §16.0 no le lista ningún expediente. Es exactamente lo que hace falta: lo que había que impedir no es ver el menú, sino **abrir un expediente con un perfil que no se tiene**. **MUST NOT** cambiarse el `groups` de estos dos `menuitem` para intentar ocultarlos.

**REQUIRED — comprobación en runtime de que los `<context>` llegan al `domain` de un `<node>` de `<tree>`.** Ningún `<node>` del proyecto usa hoy un parámetro procedente de un `<context>` del `action-view`: los dos únicos parámetros que aparecen en un `<node domain=…>` (`:estado` y `:anyo`, en `Expediente-search.xml`) vienen del `searchModel` del propio `<tree>`, no del `action-view`. Este paso lo da por hecho en **dos** ficheros (`Abierto-Expediente.xml` 16.3 (c) y `Expediente-search.xml` 16.3 (d)), así que **MUST** comprobarse abriendo «Expedientes Esperando» y la búsqueda: si `:centroActivoId` o `:esAdministrador` no llegaran al nodo, el árbol saldría vacío o lanzaría al resolver el parámetro, y entonces el filtro de centro de esas dos listas **MUST** rehacerse por otra vía (por ejemplo, resolviendo el valor en la propia expresión en vez de por parámetro) **antes** de dar el paso por bueno. Es una de las **dos** partes del Paso 16 sin precedente en el árbol; la otra es el `<context expr="call:…">` con lista de ids de las dos bandejas nuevas (16.0), que se comprueba igual.

**Verificación:** compila y arranca; los tests de `com.educaflow.views` siguen en verde (los `tramites/views/*.xml` están **exentos** de esas reglas —`ViewFiles.PAQUETES_EXENTOS` incluye `tramites`—, pero `menus.xml` **no** lo está, y las reglas de la Categoría 10 se le aplican enteras); la administrativa ve su bandeja y el director la suya, y en las dos solo aparecen expedientes de este trámite —y en la del director, **solo** los que están en «Pendiente de la firma del director»: un expediente en «Pendiente de revisión» no aparece en ella—; un usuario de otro centro no ve los expedientes ajenos en **ninguna** de las seis listas (las dos nuevas, «Expedientes Pendientes», «Expedientes Esperando», «Expedientes Cerrados» y la búsqueda); el administrador ve los de los **dos** centros en las **tres genéricas de consulta** («Expedientes Esperando», «Expedientes Cerrados» y la búsqueda), en «Expedientes Pendientes» ve **solo los de su centro activo** (16.3 (a): esa bandeja no le exceptúa) y las dos nuevas le salen vacías a propósito (Paso 16.0), porque en esta spec solo consulta y lo hace en solo lectura (ESC-025, T-038).

**REQUIRED — la condición de perfil de §16.0 se comprueba en runtime**, porque ningún test de build la ve: con un expediente en «Pendiente de revisión», el **alumno que lo creó**, el **secretario del centro** (cargo `SECRETARIO`, perfil `RESPONSABLE`) y el **supervisor** abren las dos bandejas nuevas y las ven **vacías**; entrando por «Expedientes Esperando» siguen viendo el expediente, en la pantalla genérica de solo lectura y sin el sentido de la revisión ni el motivo del rechazo. La administrativa y el director, en cambio, **sí** lo ven en su bandeja y lo abren en su pantalla. Es T-056.


## `### 12.2 Bandejas` — el filtro de centro y la excepción del administrador (verbatim)

Y las **cuatro** bandejas genéricas existentes de `tramites/views/` añaden el **filtro por centro** (ESC-021, ESC-024, ESC-025): «Expedientes Pendientes» (`Expediente-pendiente.xml`) y «Expedientes Cerrados» (`Cerrado-Expediente.xml`) en su `<domain>`, y «Expedientes Esperando» (`Abierto-Expediente.xml`) y la **búsqueda de expedientes** (`Expediente-search.xml`) en los `domain` de sus dos `<node>`, que es donde filtran. La búsqueda entra en la lista por derecho propio: fija `_profile=RESPONSABLE` y hoy no filtra por centro, así que es otra lista por la que un usuario de otro centro vería el expediente ajeno, y ESC-021 exige que no aparezca **en ninguna**. Ese filtro es **interfaz, no defensa**: la condición de servidor que de verdad aislaría los centros vive en `auth-expedientes.xml`, que es del motor — ver §14 nota 9.

**La excepción del administrador va en TRES de esas cuatro, no en las cuatro** (Paso 16.3). La llevan las tres de **consulta** —«Expedientes Esperando», «Expedientes Cerrados» y la búsqueda—, que fijan `_profile=RESPONSABLE` y por las que, al no existir ningún `<form … profile="RESPONSABLE">` en este tipo, el expediente se le abre siempre en la vista genérica de solo lectura: es exactamente lo que ESC-025 pide y lo que comprueba T-038. **MUST NOT** llevarla «Expedientes Pendientes», que fija `_profile=CREADOR` (`Expediente-pendiente.xml` línea 10) — un perfil que **sí** tiene form en `DATOS_SOLICITUD` y en `PENDIENTE_FIRMA`—, porque `ExpedienteController` solo comprueba que el perfil lo **use** el tipo y `PhaseEventManager.getViewName` (`subsystem/expedientes/services/eventmanager/PhaseEventManager.java`) devuelve entonces el form del `CREADOR`: exceptuar ahí al administrador le **pintaría** la pantalla editable del creador de **cualquier centro**. Actuar desde ella no puede: los cuatro eventos de esos dos estados llevan la guarda `ControlDeAcceso.exigeSerElCreador` (§9.1 y §11), que es lo único que le para, porque `checkPerfilDelEstado` le exime (`Tramitador.java` línea 232) — y lo mismo hacen, con la guarda de perfil, los cuatro eventos de `REVISION` y `RESOLUCION`. Con el filtro sin excepción, la exhibición queda acotada a los expedientes de su centro activo; ningún escenario pierde nada, porque el administrador consulta por las tres de arriba.

**Y esa pantalla editable se le pinta igualmente dentro de su centro, y a todo el que pueda leer el expediente** —el secretario, el vicesecretario y el supervisor por `Expediente.porTipoExpediente` / `porTramite`—: el filtro de centro solo **acota** el problema, no lo cierra. Lo que sí está cerrado es **actuar**, y lo está en **los ocho eventos del tipo**: a esos tres les rechaza el evento `checkPerfilDelEstado` por no ostentar el perfil del estado, y al administrador —al que el motor exime en **todos** los estados— le rechazan los cuatro eventos de `CREADOR` las guardas de autoría de §9.1 y los cuatro de `SECRETARIO`/`DIRECTOR` las guardas de perfil de §9.2 y §9.3. Queda abierta **solo la exhibición**, que es la parte **no mitigada** de §14 nota 8(d) y **MUST NOT** darse por resuelta: la solución de fondo es del motor.


## `### Paso 17 — Verificación final` (verbatim) — REFERENCIA

**Nota del descomponedor:** el Paso 17 del diseño no crea ni modifica ningún fichero, así que no es una tarea propia; va aquí, en la última tarea de código de aplicación, como criterio de «terminado» del conjunto. La compilación y la corrección del build las lleva el motor (verificador-build / corrector-build) después de todas las tareas.

### Paso 17 — Verificación final

```
./run.sh
```

- `BUILD SUCCESSFUL`, con los tests de `com/educaflow/tiposexpedientes` y `com/educaflow/views` en verde (los ejecuta ese mismo build).
- Se regeneró `…/v1/estados.png` (`GenerateDocs` va enganchada a `build` con `finalizedBy`).
- **REQUIRED — comprobación en runtime.** Los tests cubren la forma, no el comportamiento. Hay que recorrer **todos** los estados con usuarios de los perfiles adecuados y comprobar en particular lo que nada verifica en build:
  - Que al presentar no salta un NPE: `personaSolicitante` y `personaInteresada` los rellena el `triggerInitialEvent` (§8) y `createRegistroEntrada` revienta si son nulos.
  - Que las diez transiciones de §3 llevan donde dice el `.puml`.
  - Que los dos PDF generados **no tienen huecos en blanco**: las expresiones Groovy de `documentospdf/` fallan en silencio (log + campo vacío). Revisar en especial `self.ciclo.gradoNivel`, las dos líneas de lugar y fecha y la fórmula de estimación/desestimación de la resolución.
  - Que la firma cae dentro del recuadro «Firma:» de la solicitud (celda `y 415,52 → 472,22`) y dentro del recuadro vacío bajo «El director / la directora del centro» en la resolución (celda `y 315,28 → 372,00`); si no, ajustar las constantes `Rectangulo` **en los dos sitios** (el trigger y la `<action-method>`, para la solicitud). Ver §5 y §14 nota 12.
  - Que **ningún campo inline se sale del marco** del documento (borde derecho en `x = 566,93`): son los cuatro de ancho completo de `resolucion.xml`, que por eso llevan `;11.8` y no `;12` (§5).
  - Que las **tres fechas en valenciano** salen bien escritas —«1 de gener de 2026», «1 d’abril de 2026», «1 d’agost de 2026», «1 d’octubre de 2026»— y **no** «1 de de gener de 2026»: el patrón del valenciano es `"d MMMM 'de' yyyy"`, sin el literal `'de'` (§5).
  - Que el `<extra-code-model>` del `…/v1/domains.xml` **sigue siendo el que genera el build**: `grep -c esRechazo …/v1/domains.xml` devuelve `0` y el predicado vive en `ReglasAnulacionMatricula.kt` (§9.0.3).
  - Que el campo punteado `ciclo.gradoNivel` se refresca en la pantalla del alumno **al cambiar el ciclo**, sin guardar.
  - Que el usuario `admin` **puede abrir** un expediente (de los dos centros, entrando por «Expedientes Esperando»): si el Paso 15 no le dio centro activo, `getCentroFromCurrentUser` lanza y la pantalla no llega a pintarse (§14 nota 13).
  - Que el `admin` ve los expedientes de los **dos** centros en «Expedientes Esperando», «Expedientes Cerrados» y la búsqueda, y **solo los de su centro activo** en «Expedientes Pendientes» (16.3 (a) no le exceptúa). Esa asimetría es **deliberada**: **MUST NOT** «arreglarse» añadiendo `OR :esAdministrador = true` a esa bandeja.
  - Que se confirma el agujero **conocido y no mitigado** de §14 nota 8(d), para no confundirlo con una regresión: entrando por «Expedientes Pendientes» (que fija `_profile=CREADOR`) a un expediente **de su centro** en `DATOS_SOLICITUD` o en `PENDIENTE_FIRMA`, al `admin` —y a cualquiera que pueda leerlo— se le **pinta** el formulario editable del creador. Lo que **MUST** comprobarse a continuación es que no puede **actuar**: los **cuatro** botones de esos dos estados —«Siguiente», «Borrar el expediente», «Atrás» y «Firmar y presentar la solicitud»— le tienen que fallar con el mensaje de negocio de la guarda `exigeSerElCreador` («Solo puede modificar sus propias solicitudes», «Solo puede borrar sus propias solicitudes», «Solo puede volver atrás en sus propias solicitudes» y «Solo puede presentar sus propias solicitudes»), y el expediente **MUST** quedar exactamente en el estado en que estaba. Que la pantalla se pinte **MUST NOT** taparse desde este trámite: la solución es del motor.
  - Que el `admin` **tampoco puede actuar en `REVISION` ni en `RESOLUCION`**, que es lo que cierran las cuatro guardas de perfil (§9.2, §9.3 y §11). No hay bandeja que le lleve ahí —las dos nuevas le salen vacías (16.0)—, así que la comprobación se hace **pidiendo la vista con el `_profile` a mano**, que es justo el camino que sigue abierto (§14 nota 8(d)): se abre un expediente de **su centro activo** en `PENDIENTE_REVISION` con `_profile=SECRETARIO` y otro en `PENDIENTE_FIRMA_DIRECTOR` con `_profile=DIRECTOR`, y los cuatro botones —«Enviar a la firma del director», «Pedir subsanación al alumno», «Firmar la resolución» y «Devolver a la secretaría»— **MUST** fallar con el mensaje de negocio de la guarda («Solo la secretaría del centro puede revisar esta solicitud», «Solo el director del centro puede firmar la resolución» y «Solo el director del centro puede devolver la resolución a la secretaría»), quedando el expediente exactamente en el estado en que estaba y **sin** resolución firmada ni registro de salida. Sin estas guardas el administrador **pasa** la de centro y firma: es el caso que declara §14 nota 25 y `decisiones.md` D13, y **ningún test de build lo ve**.
  - Que las dos bandejas nuevas **solo** listan expedientes de este trámite, y que ninguna de las seis listas (las dos nuevas, «Expedientes Pendientes», «Expedientes Esperando», «Expedientes Cerrados» y la búsqueda) muestra expedientes de otro centro a un usuario **que no sea el administrador** (a él, y solo a él, se los muestran las tres de consulta; ver el punto siguiente).
  - Que las dos bandejas nuevas **listan algo**: es la comprobación de que el `<context expr="call:…">` se resuelve y de que la lista de ids se enlaza en el `<domain>` (Paso 16.0, dos mecánicas sin precedente en el árbol). Si el `call:` fallara, la bandeja saldría vacía **para todos** —incluida la administrativa, que sí ostenta el perfil— o lanzaría al resolver el parámetro; **MUST NOT** confundirse ese vacío con el vacío legítimo del punto siguiente.
  - Que las dos bandejas nuevas **solo** las ve con contenido quien ostenta su perfil: al alumno que creó el expediente, al secretario del centro y al supervisor les salen **vacías**, aunque los tres puedan leer el expediente por las bandejas genéricas (condición de perfil del Paso 16.0; es T-056). Es lo único que les corta el **camino normal** hacia la pantalla editable del `SECRETARIO` o la del `DIRECTOR`, y **ningún test de build lo ve**. **MUST NOT** leerse como que esa pantalla ya no se les puede pintar: el `_profile` viaja en la petición y nada en el servidor comprueba que el usuario ostente el perfil para **elegir** la vista, así que pedirla con ese `_profile` fuera de la bandeja sigue abriéndola (residuo declarado en §14 nota 8(d) y en las dos filas `RUI-*-GENERICA-*` de §11).
- **REQUIRED — no-regresión de la suite E2E ya persistida del catálogo.** Con la aplicación arrancada:

  ```
  npx playwright test src/test/e2e/subsystem/sistemaeducativo
  ```

  Los **16** ficheros `.spec.ts` (`t-001`…`t-015` más `crear-ley-educativa.spec.ts`) **MUST** quedar en verde. Es la misma comprobación del Paso 4, repetida al final porque entre medias se tocan el data-init (`IABD`, `nombreCorto`) y las vistas del catálogo, y `./run.sh` **no** ejecuta los tests Playwright. Un fallo aquí es una regresión de esta iniciativa y se arregla en el código de la aplicación: **MUST NOT** editarse ningún fichero de `src/test/e2e/**` (§14 nota 22).

