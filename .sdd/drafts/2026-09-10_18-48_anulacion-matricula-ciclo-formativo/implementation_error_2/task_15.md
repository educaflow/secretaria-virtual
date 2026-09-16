---
type: implementation-task
template: expediente
---

# Tarea 15 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas
- k-secure-coding
- k-i18n

## Qué hay que hacer

Crear las **dos bandejas nuevas** (`SECRETARIO` y `DIRECTOR`), añadir el **filtro de centro** a las cuatro bandejas genéricas existentes y dar de alta los **dos `menuitem`**. Son siete ficheros.

**Los dos ficheros nuevos están materializados y se copian literalmente:**

- `design/tramites-views/Expediente-revision.xml` → `src/main/java/com/educaflow/tramites/views/Expediente-revision.xml`
- `design/tramites-views/Expediente-firma.xml` → `src/main/java/com/educaflow/tramites/views/Expediente-firma.xml`

**MUST NOT** modificarlos, reescribirlos ni regenerarlos: se copian tal cual, no se interpretan.

**Los cinco ficheros existentes se EDITAN insertando solo el fragmento que el paso indica**, sobre la expresión real que hoy tiene cada uno y **conservando todo lo demás**: `tramites/views/Expediente-pendiente.xml`, `tramites/views/Abierto-Expediente.xml`, `tramites/views/Cerrado-Expediente.xml`, `tramites/views/Expediente-search.xml` y `secretariavirtual/menus/menus.xml`. Estos cinco **no** están materializados en `design/` a propósito: un `<node domain=…>` suelto o dos `<menuitem/>` sueltos no forman un XML bien formado.

**La especificación del diseño es contrato fijo y la superficie es cerrada: MUST NOT añadirse ninguna condición, `<context>`, columna, menú ni atributo que el paso no liste**, y en particular:

- **MUST NOT** añadirse el `<context name="esAdministrador">` ni la excepción `OR :esAdministrador = true` a «Expedientes Pendientes».
- **MUST NOT** añadirse la condición de perfil de §16.0 a ninguna de las cuatro bandejas genéricas.
- **MUST NOT** acotarse por trámite ninguna de las cuatro genéricas.
- **MUST NOT** «uniformarse» con `self.` el `<node>` de `Expediente` de `Abierto-Expediente.xml`.
- **MUST NOT** cambiarse el `groups` de los dos `menuitem` para intentar ocultarlos.
- **MUST NOT** replicarse dentro de los `<domain>` nuevos la condición del permiso `Ace` de `auth-expedientes.xml`.

### Decisión de descomposición documentada

El diseño tiene además un **`### Paso 17 — Verificación final`** que **no crea ni modifica ningún fichero** (es `./run.sh`, los tests de `com/educaflow/tiposexpedientes` y `com/educaflow/views`, la reejecución de `npx playwright test src/test/e2e/subsystem/sistemaeducativo` y el recorrido en runtime). Por eso **no** se ha escrito una tarea propia para él: la compilación la verifica el paso de build del pipeline y el recorrido en runtime lo ejecuta `/sdd-debug-with-test-e2e-desc` con `implementation/test-e2e-desc.md`. Las comprobaciones en runtime que el Paso 16 declara **REQUIRED** (que los `<context>` llegan al `domain` de un `<node>` de `<tree>`, y que la condición de perfil vacía las dos bandejas nuevas para quien no ostenta el perfil) quedan, por tanto, dentro del alcance de esta tarea y de esa fase posterior.

## Filas de la tabla `## 6. Ficheros a crear o modificar` del diseño (verbatim)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/tramites/views/Expediente-revision.xml` | Crear | `k-vistas` | Bandeja de la administrativa: `action-view` + `grid` con `_profile=SECRETARIO`, acotada a este trámite, filtrada por centro y acotada a quien **ostenta** el perfil `SECRETARIO` sobre el expediente (§12.2 y Paso 16.0). Copia de `design/tramites-views/Expediente-revision.xml` |
| `src/main/java/com/educaflow/tramites/views/Expediente-firma.xml` | Crear | `k-vistas` | Bandeja del director: `action-view` + `grid` con `_profile=DIRECTOR`, acotada a este trámite, al estado `PENDIENTE_FIRMA_DIRECTOR`, filtrada por centro y acotada a quien **ostenta** el perfil `DIRECTOR` sobre el expediente (§12.2 y Paso 16.0). Copia de `design/tramites-views/Expediente-firma.xml` |
| `src/main/java/com/educaflow/tramites/views/Expediente-pendiente.xml` | Modificar | `k-vistas` | Filtro de centro en el `<domain>`, **sin** excepción para el administrador: es la única genérica que fija `_profile=CREADOR` (§12.2 y Paso 16.3 (a)) |
| `src/main/java/com/educaflow/tramites/views/Abierto-Expediente.xml` | Modificar | `k-vistas` | Filtro de centro en los dos `<node domain=…>` del árbol; su `action-view` no tiene `<domain>` (§12) |
| `src/main/java/com/educaflow/tramites/views/Cerrado-Expediente.xml` | Modificar | `k-vistas` | Filtro de centro en el `<domain>`, con excepción para el administrador (§12) |
| `src/main/java/com/educaflow/tramites/views/Expediente-search.xml` | Modificar | `k-vistas` | Filtro de centro en los dos `<node domain=…>` de la búsqueda (§12) |
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | `k-vistas` | Dos `menuitem` nuevos bajo «Expedientes» para las dos bandejas |

## `### Paso 16 — Bandejas de SECRETARIO y DIRECTOR, filtro de centro y menús`, ÍNTEGRO (verbatim)

### Paso 16 — Bandejas de `SECRETARIO` y `DIRECTOR`, filtro de centro y menús

Este paso toca **seis** ficheros del árbol más el `menus.xml`. Los **dos nuevos** están materializados en `design/tramites-views/` (§14 nota 28) y se **copian literalmente**, no se interpretan (16.1 y 16.2). En los **cuatro existentes** y en el `menus.xml` se inserta **solo** el fragmento que se indica abajo, sobre la expresión real que hoy tiene cada uno, conservando todo lo demás (16.3 y 16.4). El apartado 16.3-bis declara la decisión de arquitectura que esto abre: dónde viven las vistas y los menús de un trámite concreto.

#### 16.0 — La condición de perfil de las dos bandejas nuevas

**El problema que resuelve.** Las dos bandejas nuevas fijan literalmente `_profile=SECRETARIO` y `_profile=DIRECTOR` (§12.2), y su `menuitem` las enseña a **todo** usuario: `VAR-10.1` fija el atributo `groups` a exactamente `admins`, `admins,users` o `users`, así que **no existe** la variante «restringir el menú a un grupo propio» — un `groups="secretario"` rompería los tests de `com.educaflow.views` (Categoría 10). Y `ExpedienteController.getEventContext` solo comprueba, con `checkProfileDelTipoExpediente`, que el perfil lo **use algún estado del tipo**; nunca que el usuario lo **ostente**. Sin más filtro, cualquiera con lectura sobre el expediente —el propio alumno por `Expediente.creador`, el secretario o el vicesecretario por `Expediente.porTipoExpediente`, el supervisor por `Expediente.porTramite`— que entrase por «Anulaciones de matrícula de mi centro» abriría `PENDIENTE_REVISION` en la pantalla **editable** del `SECRETARIO` (el sentido de la revisión, el motivo del rechazo y el texto de la subsanación), y quien entrase por «Anulaciones de matrícula pendientes de mi firma» abriría `PENDIENTE_FIRMA_DIRECTOR` en la del `DIRECTOR` (la decisión de secretaría y la resolución sin firmar). Eso contradice literalmente RUI-PENDIENTE_REVISION-GENERICA-001, RUI-PENDIENTE_FIRMA_DIRECTOR-GENERICA-001, ESC-008 paso 6, ESC-009 paso 8, ESC-020 paso 4, ESC-024 paso 4 y ESC-029 paso 11: «la decisión de secretaría no se enseña a nadie hasta que el director firma».

**La condición.** Por eso el `<domain>` de las dos bandejas nuevas —y **solo** el de esas dos— lista un expediente únicamente si el usuario **ostenta sobre él el perfil que la bandeja fija**. Se escribe **una sola vez**, aquí, y en cada `<domain>` aparece con la **única** variación admitida: el literal del perfil (`'SECRETARIO'` o `'DIRECTOR'`).

```
EXISTS (SELECT 1 FROM com.educaflow.subsystem.security.db.Ace aa
        WHERE aa.perfil.name = '<PERFIL>'
          AND (aa.expediente = self OR aa.tipoExpediente = self.tipoExpediente OR aa.tramite = self.tipoExpediente.tramite)
          AND (aa.centro IS NULL OR aa.centro.id = :centroActivoId)
          AND (aa.centroUsuario.id = :centroUsuarioActivoId
               OR aa.tipoUsuario IN (SELECT cut.tipoUsuario FROM com.educaflow.subsystem.common.db.CentroUsuarioTipoUsuario cut WHERE cut.centroUsuario.id = :centroUsuarioActivoId)
               OR aa.cargo IN (SELECT cuc.cargo FROM com.educaflow.subsystem.common.db.CentroUsuarioCargo cuc WHERE cuc.centroUsuario.id = :centroUsuarioActivoId)))
```

Los dos `<context>` que la alimentan son **los mismos** en las dos bandejas:

```xml
        <context name="centroActivoId" expr="eval: __user__?.centroActivo?.id"/>
        <context name="centroUsuarioActivoId" expr="eval: __user__?.centroUsuarioActivo?.id"/>
```

**Estas dos bandejas NO llevan la excepción del administrador** (`esAdministrador`) que sí llevan **tres** de las cuatro genéricas del Paso 16.3 —todas menos «Expedientes Pendientes», que tampoco la lleva (16.3 (a))—, y es deliberado: el administrador **no tiene filas `Ace`**, así que no ostenta `SECRETARIO` ni `DIRECTOR` sobre ningún expediente y las dos bandejas le salen **vacías**. Es exactamente lo que la spec quiere — en ella el administrador **solo consulta** (ESC-025, T-038) y consulta por las bandejas genéricas, que sí le exceptúan del centro y le abren el expediente en la vista de solo lectura. Exceptuarle también aquí le pintaría la pantalla **editable** del secretario o la del director y, como `checkPerfilDelEstado` le exime (`SecurityUtil.isAdmin`), le dejaría además **disparar** sus eventos: una capacidad que ningún escenario le concede. Por eso el filtro de centro de estas dos va **sin** excepción: para el único usuario al que exceptuaría, la condición de perfil ya decide que no.

**Quién es su dueño.** La pregunta que responde —«¿ostenta este usuario el perfil P sobre este expediente?»— **ya tiene dueño en el servidor**: `PerfilesUsuarioService.getPerfilesSobreExpediente`, implementado por `AceRepository.findNombresPerfilesByExpediente` con su constante `FILTRO_USUARIO`, y es lo que `Tramitador.checkPerfilDelEstado` usa para **autorizar** cada evento. Esta condición es la **proyección a la lista** de esa misma pregunta —las tres vías de asignación (`expediente`, `tipoExpediente`, `tramite`) y las tres de pertenencia (tipo de usuario, el propio usuario, cargo) son las de ese método—, no una segunda decisión de autorización: **no** decide quién puede leer (eso es `auth-expedientes.xml`) ni quién puede actuar (eso es `checkPerfilDelEstado`). Por eso **MUST NOT** añadírsele ninguna condición que las otras dos capas ya decidan, y en particular **MUST NOT** replicarse aquí la condición del permiso `Ace` de `auth-expedientes.xml`: sería la misma decisión con tres dueños.

**COPIA DELIBERADA, DECLARADA COMO TAL — MUST leerse antes de tocar `AceRepository`.** Lo escrito arriba **no** es una llamada al dueño: es una **tercera escritura** de su lógica, en JPQL y dentro de dos ficheros de vista. La alternativa de que las bandejas **pregunten** al dueño —exponer desde el servidor la lista de expedientes sobre los que el usuario ostenta el perfil (un `action-method` o un `<context>` alimentado por `PerfilesUsuarioService`) y dejar en el XML solo `self.id IN (:ids…)`— se evaluó y se descartó por dos motivos que están escritos enteros en `decisiones.md` **D8**: exigiría una pieza de servidor nueva —y la única que serviría a **cualquier** trámite viviría en `subsystem/expedientes`, que este diseño **MUST NOT** ampliar— y obligaría a materializar en memoria, en cada apertura de la bandeja, la lista completa de ids de expedientes del trámite. Como la condición es **temporal por construcción** (desaparece el día en que se apruebe la solución de fondo de §14 nota 8), se asume la copia, pero **declarada**:

- **Dueño real:** `AceRepository.findNombresPerfilesByExpediente` (con su constante `FILTRO_USUARIO`), a través de `PerfilesUsuarioService.getPerfilesSobreExpediente`.
- **Punto exacto en el que esta copia diverge del dueño, y por qué:** el repositorio excluye `aa.perfil.name <> 'CREADOR'` en la vía del trámite; esta copia **no** lo hace, porque el perfil está **fijado** al literal de la bandeja (`'SECRETARIO'` / `'DIRECTOR'`), que nunca es `CREADOR`, así que la exclusión sería inerte. Es la **única** divergencia admitida: cualquier otra es un error.
- **Quién la mantiene sincronizada:** quien toque `AceRepository.findNombresPerfilesByExpediente` —sus vías de asignación (`expediente`, `tipoExpediente`, `tramite`), sus vías de pertenencia (usuario, tipo de usuario, cargo) o su tratamiento del centro— **MUST** revisar y actualizar esta condición en los dos ficheros de bandeja. No hay ningún test que lo destape: lo que se rompería es que la bandeja liste de más o de menos, y eso solo se ve en runtime (T-056).

Notas de lectura de la condición:

- **CRITICAL — `aa.perfil.name` es el nombre del PERFIL, no el del cargo.** En este trámite coinciden en el literal: existe el **perfil** `SECRETARIO` (que ostenta el tipo de usuario `ADMINISTRATIVO`) y existe el **cargo** `SECRETARIO` del centro, que en cambio ostenta el perfil `RESPONSABLE` (§12.1). La condición mira `aa.perfil.name`, así que el secretario del centro **no** entra por la bandeja de secretaría — y es lo correcto: la spec le da consulta, no revisión. **MUST NOT** cambiarse por `aa.cargo.code`.
- **No necesita la exclusión de `CREADOR`** que `AceRepository` hace en la vía del trámite (`aa.perfil.name <> 'CREADOR'`): aquí el perfil está **fijado** al literal de la bandeja, que nunca es `CREADOR`.
- **`aa.centro.id` y `self.centro.id` leen la clave ajena**, no fuerzan `join`, así que la rama `aa.centro IS NULL` (la que usan todos los `Ace` del data-init) sigue casando.
- **Falla cerrado**: si el usuario no tiene `centroUsuarioActivo` (o no tiene centro activo), la condición no casa con ningún `Ace` y la bandeja sale **vacía**. Es la dirección segura, y coincide con lo que el usuario podría hacer: sin centro activo `ExpedienteController.getCentroFromCurrentUser` ni siquiera le deja abrir un expediente (§14 nota 13).
- **Es interfaz, no defensa** —igual que el filtro de centro (§14 nota 9)—: lo que de verdad impide **actuar** sin el perfil es `checkPerfilDelEstado`, que ya lo comprueba en el servidor en cada evento. Lo que esta condición evita es la **exhibición**: que la pantalla de otro perfil llegue a pintarse. La solución de fondo sigue siendo que la bandeja pase el perfil real del usuario, que es del motor y está declarada en §14 nota 8 como **decisión de arquitectura pendiente de aprobar**; el día en que se apruebe, esta condición **desaparece**.

#### 16.1 — `tramites/views/Expediente-revision.xml` (nuevo)

El fichero está materializado en `design/tramites-views/Expediente-revision.xml`. **Cópialo literalmente** a `src/main/java/com/educaflow/tramites/views/Expediente-revision.xml`. **MUST NOT** modificarlo, reescribirlo ni regenerarlo.

**Resumen estructural** — un `action-view` `subsysExpedientes.Expediente@Revision-action` («Anulaciones de matrícula de mi centro») sobre `Expediente`, con `<context name="_profile" expr="SECRETARIO"/>`, los dos `<context>` de §16.0 y un `<domain>` que combina cuatro condiciones —`abierto`, el trámite, el centro activo y la condición de perfil de §16.0 con el literal `'SECRETARIO'`—; más su `grid` `subsysExpedientes.Expediente@Revision-grid` de cinco columnas, no editable, que abre con `subsysExpedientes-event-view-action`.

#### 16.2 — `tramites/views/Expediente-firma.xml` (nuevo)

El fichero está materializado en `design/tramites-views/Expediente-firma.xml`. **Cópialo literalmente** a `src/main/java/com/educaflow/tramites/views/Expediente-firma.xml`. **MUST NOT** modificarlo, reescribirlo ni regenerarlo.

**Resumen estructural** — el mismo esqueleto que 16.1 (`subsysExpedientes.Expediente@Firma-action`, «Anulaciones de matrícula pendientes de mi firma»), con `_profile=DIRECTOR`, el literal `'DIRECTOR'` en la condición de perfil y **una condición más** en el `<domain>`: `self.codeState = 'PENDIENTE_FIRMA_DIRECTOR'`.

**CRITICAL — la bandeja del director lleva además `AND self.codeState = 'PENDIENTE_FIRMA_DIRECTOR'`.** Su título promete «pendientes de mi firma», y sin esa condición listaría **todos** los expedientes abiertos del trámite, también los que están en `DATOS_SOLICITUD`, `PENDIENTE_FIRMA` y `PENDIENTE_REVISION`, que no están pendientes de nada suyo. Es además el único estado en el que existe un form para `profile="DIRECTOR"`: en cualquier otro, entrar por esta bandeja llevaría a la vista genérica de solo lectura. La bandeja de la administrativa **no** lleva condición de estado, y su título tampoco la promete: tiene que ver los expedientes de su centro en cualquier estado (ESC-045, T-039).

**CRITICAL — la acotación `self.tipoExpediente.tramite.code = 'AnulacionMatriculaCicloFormativo'` es obligatoria en las dos.** Sin ella la bandeja listaría expedientes de **cualquier** tipo, y abrir desde ella uno cuyo tipo no declare `SECRETARIO`/`DIRECTOR` lanza «El perfil … no lo usa ningún estado del tipo de expediente …» (`ExpedienteController.checkProfileDelTipoExpediente`). `TipoExpediente.tramite` y `Tramite.code` existen y son consultables. Ver §14 nota 8(c).

**CRITICAL — las dos llevan la condición de perfil de §16.0**, que es lo que impide que quien **no** ostenta `SECRETARIO`/`DIRECTOR` abra un expediente con la pantalla de ese perfil. Su formulación es la de §16.0 palabra por palabra y la **única** variación admitida entre las dos es el literal `'SECRETARIO'` / `'DIRECTOR'`.

**CRITICAL — MUST NOT** replicarse dentro de estos `<domain>` la condición del permiso `Ace` de `auth-expedientes.xml` (la de «quién puede **leer** el expediente»). Sería la misma decisión de autorización con **tres dueños** (el permiso, el dominio de la bandeja y la guarda del trigger) y una nota de «manténlo sincronizado a mano». El `<domain>` acota **qué se lista** y con qué perfil se abre; quién puede **leer** lo decide el permiso y quién puede **actuar**, `checkPerfilDelEstado` del motor más la guarda de centro del trigger (§9.0.1).

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

**REQUIRED — comprobación en runtime de que los `<context>` llegan al `domain` de un `<node>` de `<tree>`.** Ningún `<node>` del proyecto usa hoy un parámetro procedente de un `<context>` del `action-view`: los dos únicos parámetros que aparecen en un `<node domain=…>` (`:estado` y `:anyo`, en `Expediente-search.xml`) vienen del `searchModel` del propio `<tree>`, no del `action-view`. Este paso lo da por hecho en **dos** ficheros (`Abierto-Expediente.xml` 16.3 (c) y `Expediente-search.xml` 16.3 (d)), así que **MUST** comprobarse abriendo «Expedientes Esperando» y la búsqueda: si `:centroActivoId` o `:esAdministrador` no llegaran al nodo, el árbol saldría vacío o lanzaría al resolver el parámetro, y entonces el filtro de centro de esas dos listas **MUST** rehacerse por otra vía (por ejemplo, resolviendo el valor en la propia expresión en vez de por parámetro) **antes** de dar el paso por bueno. Es la única parte del Paso 16 sin precedente en el árbol.

**Verificación:** compila y arranca; los tests de `com.educaflow.views` siguen en verde (los `tramites/views/*.xml` están **exentos** de esas reglas —`ViewFiles.PAQUETES_EXENTOS` incluye `tramites`—, pero `menus.xml` **no** lo está, y las reglas de la Categoría 10 se le aplican enteras); la administrativa ve su bandeja y el director la suya, y en las dos solo aparecen expedientes de este trámite —y en la del director, **solo** los que están en «Pendiente de la firma del director»: un expediente en «Pendiente de revisión» no aparece en ella—; un usuario de otro centro no ve los expedientes ajenos en **ninguna** de las seis listas (las dos nuevas, «Expedientes Pendientes», «Expedientes Esperando», «Expedientes Cerrados» y la búsqueda); el administrador ve los de los **dos** centros en las **tres genéricas de consulta** («Expedientes Esperando», «Expedientes Cerrados» y la búsqueda), en «Expedientes Pendientes» ve **solo los de su centro activo** (16.3 (a): esa bandeja no le exceptúa) y las dos nuevas le salen vacías a propósito (Paso 16.0), porque en esta spec solo consulta y lo hace en solo lectura (ESC-025, T-038).

**REQUIRED — la condición de perfil de §16.0 se comprueba en runtime**, porque ningún test de build la ve: con un expediente en «Pendiente de revisión», el **alumno que lo creó**, el **secretario del centro** (cargo `SECRETARIO`, perfil `RESPONSABLE`) y el **supervisor** abren las dos bandejas nuevas y las ven **vacías**; entrando por «Expedientes Esperando» siguen viendo el expediente, en la pantalla genérica de solo lectura y sin el sentido de la revisión ni el motivo del rechazo. La administrativa y el director, en cambio, **sí** lo ven en su bandeja y lo abren en su pantalla. Es T-056.

## `### 12.2 Bandejas` del diseño (verbatim)

### 12.2 Bandejas

La vista que ve cada actor la elige la pareja `(estado, _profile)`, y el `_profile` **lo fija el `action-view` de la bandeja**, no el usuario. Las bandejas existentes solo pasan `CREADOR` (Trámites y «Expedientes Pendientes») y `RESPONSABLE` («Expedientes Esperando», «Expedientes Cerrados» y la búsqueda), así que este trámite necesita dos más:

| Bandeja (nueva) | `_profile` | Dominio | Quién la usa |
|---|---|---|---|
| «Anulaciones de matrícula de mi centro» (`subsysExpedientes.Expediente@Revision-action`) | `SECRETARIO` | abiertos **de este trámite** en el centro activo **sobre los que el usuario ostenta el perfil `SECRETARIO`** | la administrativa |
| «Anulaciones de matrícula pendientes de mi firma» (`subsysExpedientes.Expediente@Firma-action`) | `DIRECTOR` | abiertos **de este trámite** en el centro activo, **en el estado `PENDIENTE_FIRMA_DIRECTOR`** y **sobre los que el usuario ostenta el perfil `DIRECTOR`** | el director |

La del director va además acotada a `self.codeState = 'PENDIENTE_FIRMA_DIRECTOR'`, que es lo que su título anuncia y el único estado con form para `DIRECTOR`. Las dos van **acotadas al trámite** (`self.tipoExpediente.tramite.code = 'AnulacionMatriculaCicloFormativo'`) y llevan el trámite en el título, porque su `_profile` es un perfil que otros tipos de expediente pueden no declarar: listar expedientes ajenos haría reventar al abrirlos (§14 nota 8(c)). El Paso 16 las da escritas enteras.

**Las dos van acotadas además a quien OSTENTA el perfil que fijan** (la condición `EXISTS` sobre `Ace` del Paso 16.0, escrita una sola vez y con el literal del perfil como única variación). Sin ella, cualquiera con lectura sobre el expediente —el propio alumno por `Expediente.creador`, el secretario o el vicesecretario por `Expediente.porTipoExpediente`, el supervisor por `Expediente.porTramite`— abriría por esas bandejas la pantalla **editable** del `SECRETARIO` o la del `DIRECTOR` y vería la decisión de secretaría antes de que el director firme, contra RUI-PENDIENTE_REVISION-GENERICA-001, RUI-PENDIENTE_FIRMA_DIRECTOR-GENERICA-001, ESC-008, ESC-009, ESC-020, ESC-024 y ESC-029: `ExpedienteController` comprueba que el perfil lo **use** el tipo, nunca que el usuario lo **tenga**. Restringir en cambio el `menuitem` a un grupo propio **no es una opción**: `VAR-10.1` fija el `groups` de todo menú a `admins`, `admins,users` o `users` (Paso 16.4). El **dueño** de la pregunta «¿ostenta este usuario el perfil P sobre este expediente?» sigue siendo `PerfilesUsuarioService.getPerfilesSobreExpediente` en el servidor, que es lo que `Tramitador.checkPerfilDelEstado` usa para autorizar cada evento; el `<domain>` es su **proyección a la lista**, es **interfaz y no defensa**, y desaparece el día en que se apruebe la solución de fondo de §14 nota 8 (que la bandeja pase el perfil real del usuario).

Y las **cuatro** bandejas genéricas existentes de `tramites/views/` añaden el **filtro por centro** (ESC-021, ESC-024, ESC-025): «Expedientes Pendientes» (`Expediente-pendiente.xml`) y «Expedientes Cerrados» (`Cerrado-Expediente.xml`) en su `<domain>`, y «Expedientes Esperando» (`Abierto-Expediente.xml`) y la **búsqueda de expedientes** (`Expediente-search.xml`) en los `domain` de sus dos `<node>`, que es donde filtran. La búsqueda entra en la lista por derecho propio: fija `_profile=RESPONSABLE` y hoy no filtra por centro, así que es otra lista por la que un usuario de otro centro vería el expediente ajeno, y ESC-021 exige que no aparezca **en ninguna**. Ese filtro es **interfaz, no defensa**: la condición de servidor que de verdad aislaría los centros vive en `auth-expedientes.xml`, que es del motor — ver §14 nota 9.

**La excepción del administrador va en TRES de esas cuatro, no en las cuatro** (Paso 16.3). La llevan las tres de **consulta** —«Expedientes Esperando», «Expedientes Cerrados» y la búsqueda—, que fijan `_profile=RESPONSABLE` y por las que, al no existir ningún `<form … profile="RESPONSABLE">` en este tipo, el expediente se le abre siempre en la vista genérica de solo lectura: es exactamente lo que ESC-025 pide y lo que comprueba T-038. **MUST NOT** llevarla «Expedientes Pendientes», que fija `_profile=CREADOR` (`Expediente-pendiente.xml` línea 10) — un perfil que **sí** tiene form en `DATOS_SOLICITUD` y en `PENDIENTE_FIRMA`—, porque `ExpedienteController` solo comprueba que el perfil lo **use** el tipo y `PhaseEventManager.getViewName` (`subsystem/expedientes/services/eventmanager/PhaseEventManager.java`) devuelve entonces el form del `CREADOR`: exceptuar ahí al administrador le **pintaría** la pantalla editable del creador de **cualquier centro**. Actuar desde ella no puede: los cuatro eventos de esos dos estados llevan la guarda `ControlDeAcceso.exigeSerElCreador` (§9.1 y §11), que es lo único que le para, porque `checkPerfilDelEstado` le exime (`Tramitador.java` línea 232). Con el filtro sin excepción, la exhibición queda acotada a los expedientes de su centro activo; ningún escenario pierde nada, porque el administrador consulta por las tres de arriba.

**Y esa pantalla editable se le pinta igualmente dentro de su centro, y a todo el que pueda leer el expediente** —el secretario, el vicesecretario y el supervisor por `Expediente.porTipoExpediente` / `porTramite`—: el filtro de centro solo **acota** el problema, no lo cierra. Lo que sí está cerrado es **actuar**: a esos tres les rechaza el evento `checkPerfilDelEstado` por no ostentar `CREADOR`, y al administrador —al que el motor exime— le rechazan los cuatro eventos las guardas de autoría de §9.1. Queda abierta **solo la exhibición**, que es la parte **no mitigada** de §14 nota 8(d) y **MUST NOT** darse por resuelta: la solución de fondo es del motor.
