---
type: implementation-task
template: expediente
---

# Tarea 15 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas
- k-secure-coding

## Qué hay que hacer

Los cuatro XML que el `### Paso 16` da **completos abajo se copian literalmente, no se interpretan**. Los dos nuevos se crean; en los cuatro existentes (más `menus.xml`) se inserta **solo** el fragmento indicado, **conservando todo lo demás**:

- `src/main/java/com/educaflow/tramites/views/Expediente-revision.xml` — **Crear** (contenido literal en el Paso 16.1)
- `src/main/java/com/educaflow/tramites/views/Expediente-firma.xml` — **Crear** (contenido literal en el Paso 16.2)
- `src/main/java/com/educaflow/tramites/views/Expediente-pendiente.xml` — **Modificar** (Paso 16.3 (a))
- `src/main/java/com/educaflow/tramites/views/Cerrado-Expediente.xml` — **Modificar** (Paso 16.3 (b))
- `src/main/java/com/educaflow/tramites/views/Abierto-Expediente.xml` — **Modificar** (Paso 16.3 (c))
- `src/main/java/com/educaflow/tramites/views/Expediente-search.xml` — **Modificar** (Paso 16.3 (d))
- `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` — **Modificar** (Paso 16.4)

La especificación es **contrato fijo** y la **superficie es cerrada**: **MUST NOT** añadirse ningún `<context>`, condición, columna, `menuitem` ni acotación que el Paso 16 no declare — en particular, **MUST NOT** añadirse la excepción del administrador a «Expedientes Pendientes» ni a las dos bandejas nuevas, ni acotarse por trámite ninguna de las cuatro bandejas genéricas.

## Filas de la tabla «## 6. Ficheros a crear o modificar» del diseño (verbatim)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/tramites/views/Expediente-revision.xml` | Crear | `k-vistas` | Bandeja de la administrativa: `action-view` + `grid` con `_profile=SECRETARIO`, acotada a este trámite, filtrada por centro y acotada a quien **ostenta** el perfil `SECRETARIO` sobre el expediente (§12.2 y Paso 16.0). Contenido literal en el Paso 16 |
| `src/main/java/com/educaflow/tramites/views/Expediente-firma.xml` | Crear | `k-vistas` | Bandeja del director: `action-view` + `grid` con `_profile=DIRECTOR`, acotada a este trámite, al estado `PENDIENTE_FIRMA_DIRECTOR`, filtrada por centro y acotada a quien **ostenta** el perfil `DIRECTOR` sobre el expediente (§12.2 y Paso 16.0). Contenido literal en el Paso 16 |
| `src/main/java/com/educaflow/tramites/views/Expediente-pendiente.xml` | Modificar | `k-vistas` | Filtro de centro en el `<domain>`, **sin** excepción para el administrador: es la única genérica que fija `_profile=CREADOR` (§12.2 y Paso 16.3 (a)) |
| `src/main/java/com/educaflow/tramites/views/Abierto-Expediente.xml` | Modificar | `k-vistas` | Filtro de centro en los dos `<node domain=…>` del árbol; su `action-view` no tiene `<domain>` (§12) |
| `src/main/java/com/educaflow/tramites/views/Cerrado-Expediente.xml` | Modificar | `k-vistas` | Filtro de centro en el `<domain>`, con excepción para el administrador (§12) |
| `src/main/java/com/educaflow/tramites/views/Expediente-search.xml` | Modificar | `k-vistas` | Filtro de centro en los dos `<node domain=…>` de la búsqueda (§12) |
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | `k-vistas` | Dos `menuitem` nuevos bajo «Expedientes» para las dos bandejas |

## `### Paso 16 — Bandejas de SECRETARIO y DIRECTOR, filtro de centro y menús` del diseño, ÍNTEGRO (verbatim)

### Paso 16 — Bandejas de `SECRETARIO` y `DIRECTOR`, filtro de centro y menús

Los cuatro XML de este paso van **completos abajo**: se **copian literalmente**, no se interpretan. Los dos nuevos se crean; en los cuatro existentes se inserta **solo** el fragmento indicado, conservando todo lo demás.

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

Notas de lectura de la condición:

- **CRITICAL — `aa.perfil.name` es el nombre del PERFIL, no el del cargo.** En este trámite coinciden en el literal: existe el **perfil** `SECRETARIO` (que ostenta el tipo de usuario `ADMINISTRATIVO`) y existe el **cargo** `SECRETARIO` del centro, que en cambio ostenta el perfil `RESPONSABLE` (§12.1). La condición mira `aa.perfil.name`, así que el secretario del centro **no** entra por la bandeja de secretaría — y es lo correcto: la spec le da consulta, no revisión. **MUST NOT** cambiarse por `aa.cargo.code`.
- **No necesita la exclusión de `CREADOR`** que `AceRepository` hace en la vía del trámite (`aa.perfil.name <> 'CREADOR'`): aquí el perfil está **fijado** al literal de la bandeja, que nunca es `CREADOR`.
- **`aa.centro.id` y `self.centro.id` leen la clave ajena**, no fuerzan `join`, así que la rama `aa.centro IS NULL` (la que usan todos los `Ace` del data-init) sigue casando.
- **Falla cerrado**: si el usuario no tiene `centroUsuarioActivo` (o no tiene centro activo), la condición no casa con ningún `Ace` y la bandeja sale **vacía**. Es la dirección segura, y coincide con lo que el usuario podría hacer: sin centro activo `ExpedienteController.getCentroFromCurrentUser` ni siquiera le deja abrir un expediente (§14 nota 13).
- **Es interfaz, no defensa** —igual que el filtro de centro (§14 nota 9)—: lo que de verdad impide **actuar** sin el perfil es `checkPerfilDelEstado`, que ya lo comprueba en el servidor en cada evento. Lo que esta condición evita es la **exhibición**: que la pantalla de otro perfil llegue a pintarse. La solución de fondo sigue siendo que la bandeja pase el perfil real del usuario, que es del motor y está declarada en §14 nota 8 como **decisión de arquitectura pendiente de aprobar**; el día en que se apruebe, esta condición **desaparece**.

#### 16.1 — `tramites/views/Expediente-revision.xml` (nuevo)

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<object-views xmlns="http://axelor.com/xml/ns/object-views"
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://axelor.com/xml/ns/object-views https://axelor.com/xml/ns/object-views/object-views_8.1.xsd">

    <action-view name="subsysExpedientes.Expediente@Revision-action" title="Anulaciones de matrícula de mi centro" model="com.educaflow.subsystem.expedientes.db.Expediente">
        <view type="grid" name="subsysExpedientes.Expediente@Revision-grid"/>
        <domain>self.abierto = true
            AND self.tipoExpediente.tramite.code = 'AnulacionMatriculaCicloFormativo'
            AND self.centro.id = :centroActivoId
            AND EXISTS (SELECT 1 FROM com.educaflow.subsystem.security.db.Ace aa
                        WHERE aa.perfil.name = 'SECRETARIO'
                          AND (aa.expediente = self OR aa.tipoExpediente = self.tipoExpediente OR aa.tramite = self.tipoExpediente.tramite)
                          AND (aa.centro IS NULL OR aa.centro.id = :centroActivoId)
                          AND (aa.centroUsuario.id = :centroUsuarioActivoId
                               OR aa.tipoUsuario IN (SELECT cut.tipoUsuario FROM com.educaflow.subsystem.common.db.CentroUsuarioTipoUsuario cut WHERE cut.centroUsuario.id = :centroUsuarioActivoId)
                               OR aa.cargo IN (SELECT cuc.cargo FROM com.educaflow.subsystem.common.db.CentroUsuarioCargo cuc WHERE cuc.centroUsuario.id = :centroUsuarioActivoId)))</domain>
        <context name="_profile" expr="SECRETARIO"/>
        <context name="centroActivoId" expr="eval: __user__?.centroActivo?.id"/>
        <context name="centroUsuarioActivoId" expr="eval: __user__?.centroUsuarioActivo?.id"/>
    </action-view>

    <grid name="subsysExpedientes.Expediente@Revision-grid" title="Expedientes" model="com.educaflow.subsystem.expedientes.db.Expediente"
          groups="admins,users"
          editable="false" edit-icon="false" x-selector="none" canNew="false" canEdit="false" canDelete="false" canSave="false"
          action="subsysExpedientes-event-view-action"
    >
        <field name="tipoExpediente.tramite.name" />
        <field name="numeroExpediente" width="110px"/>
        <field name="namePhase" width="150px"/>
        <field name="nameState" width="200px"/>
        <field name="fechaUltimoEstado" width="150px" />
    </grid>

</object-views>
```

#### 16.2 — `tramites/views/Expediente-firma.xml` (nuevo)

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<object-views xmlns="http://axelor.com/xml/ns/object-views"
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://axelor.com/xml/ns/object-views https://axelor.com/xml/ns/object-views/object-views_8.1.xsd">

    <action-view name="subsysExpedientes.Expediente@Firma-action" title="Anulaciones de matrícula pendientes de mi firma" model="com.educaflow.subsystem.expedientes.db.Expediente">
        <view type="grid" name="subsysExpedientes.Expediente@Firma-grid"/>
        <domain>self.abierto = true
            AND self.tipoExpediente.tramite.code = 'AnulacionMatriculaCicloFormativo'
            AND self.codeState = 'PENDIENTE_FIRMA_DIRECTOR'
            AND self.centro.id = :centroActivoId
            AND EXISTS (SELECT 1 FROM com.educaflow.subsystem.security.db.Ace aa
                        WHERE aa.perfil.name = 'DIRECTOR'
                          AND (aa.expediente = self OR aa.tipoExpediente = self.tipoExpediente OR aa.tramite = self.tipoExpediente.tramite)
                          AND (aa.centro IS NULL OR aa.centro.id = :centroActivoId)
                          AND (aa.centroUsuario.id = :centroUsuarioActivoId
                               OR aa.tipoUsuario IN (SELECT cut.tipoUsuario FROM com.educaflow.subsystem.common.db.CentroUsuarioTipoUsuario cut WHERE cut.centroUsuario.id = :centroUsuarioActivoId)
                               OR aa.cargo IN (SELECT cuc.cargo FROM com.educaflow.subsystem.common.db.CentroUsuarioCargo cuc WHERE cuc.centroUsuario.id = :centroUsuarioActivoId)))</domain>
        <context name="_profile" expr="DIRECTOR"/>
        <context name="centroActivoId" expr="eval: __user__?.centroActivo?.id"/>
        <context name="centroUsuarioActivoId" expr="eval: __user__?.centroUsuarioActivo?.id"/>
    </action-view>

    <grid name="subsysExpedientes.Expediente@Firma-grid" title="Expedientes" model="com.educaflow.subsystem.expedientes.db.Expediente"
          groups="admins,users"
          editable="false" edit-icon="false" x-selector="none" canNew="false" canEdit="false" canDelete="false" canSave="false"
          action="subsysExpedientes-event-view-action"
    >
        <field name="tipoExpediente.tramite.name" />
        <field name="numeroExpediente" width="110px"/>
        <field name="namePhase" width="150px"/>
        <field name="nameState" width="200px"/>
        <field name="fechaUltimoEstado" width="150px" />
    </grid>

</object-views>
```

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

**CRITICAL — «Expedientes Pendientes» (a) lleva SOLO el primero de esos dos `<context>` y su `<domain>` NO exceptúa al administrador.** Es la única de las cuatro que fija `_profile=CREADOR` (`Expediente-pendiente.xml` línea 10); las otras tres fijan `_profile=RESPONSABLE`, un perfil que este tipo declara en `ACEPTADA` y `RECHAZADA` pero para el que **ninguna fase escribe un `<form … profile="RESPONSABLE">`**, así que por ellas el expediente se abre siempre en la vista genérica de solo lectura. `CREADOR`, en cambio, **sí** tiene form en `DATOS_SOLICITUD` y en `PENDIENTE_FIRMA`, y `PhaseEventManager.getViewName` (líneas 97-103) devuelve el form del perfil recibido en cuanto existe: exceptuar ahí al administrador le abriría la pantalla **editable** del creador en **cualquier** centro y, como `checkPerfilDelEstado` le exime (`Tramitador.java` línea 232), le dejaría disparar `VOLVER` sobre la solicitud de otro (`CONTINUAR` y `DELETE` los para `ControlDeAcceso.exigeSerElCreador`, §9.1). Sin la excepción, ese alcance queda acotado a los expedientes de su centro activo — **y no desaparece**: dentro de ese centro la pantalla editable se le sigue abriendo, igual que a todo el que pueda leer el expediente (§12.2 y §14 nota 8(d)). Ningún escenario lo pide por esta bandeja: el administrador consulta por «Expedientes Esperando», «Expedientes Cerrados» y la búsqueda (ESC-025, T-038), que sí le exceptúan.

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
        <node model="com.educaflow.subsystem.expedientes.db.Expediente" domain="self.abierto=true AND (self.centro.id = :centroActivoId OR :esAdministrador = true)" parent="tipoExpediente.tramite" draggable="false" onClick="subsysExpedientes-event-view-action">
```

**(d) `Expediente-search.xml`** — la búsqueda de expedientes es **una lista más** por la que un usuario de otro centro ve el expediente ajeno, y ESC-021 exige que no aparezca «ni en esa lista ni en ninguna otra lista de expedientes que la administrativa de CIPFP Batoi pueda abrir». Los dos `<context>` van en `subsysExpedientes.Expediente@PruebaSearch-buscar-action` (que ya fija `_profile=RESPONSABLE`), y sus dos `<node>` **conservan** su condición actual sobre `abierto` y `fechaUltimoEstado`:

```xml
        <node model="com.educaflow.subsystem.expedientes.db.Tramite" domain="EXISTS (SELECT 1 FROM Expediente e WHERE e.abierto=:estado and ((:anyo is null) OR (:anyo=0) OR (YEAR(e.fechaUltimoEstado)=:anyo)) AND (e.centro.id = :centroActivoId OR :esAdministrador = true) AND e.tipoExpediente.tramite=self)" >
```

```xml
        <node model="com.educaflow.subsystem.expedientes.db.Expediente" domain="(self.abierto=:estado) and ((:anyo is null) OR (:anyo=0) OR (YEAR(self.fechaUltimoEstado)=:anyo)) and (self.centro.id = :centroActivoId OR :esAdministrador = true)" parent="tipoExpediente.tramite" draggable="false" onClick="subsysExpedientes-event-view-action">
```

**MUST NOT** acotarse por trámite ninguna de estas cuatro: son bandejas **genéricas** de la plataforma y sirven a todos los tipos de expediente. Solo se les añade el centro.

#### 16.4 — Los dos `menuitem`

En `secretariavirtual/menus/menus.xml`, a continuación de `expedientes-pruebaBusqueda-menuitem` y con la **misma sangría de cuatro espacios** que sus hermanos (`order` 1…6 ya usados, así que estos van con 7 y 8):

```xml
    <menuitem name="expedientes-anulacionesMiCentro-menuitem" parent="expedientes-menuitem" title="Anulaciones de matrícula de mi centro" action="subsysExpedientes.Expediente@Revision-action" groups="admins,users" order="7"/>
    <menuitem name="expedientes-anulacionesMiFirma-menuitem" parent="expedientes-menuitem" title="Anulaciones de matrícula pendientes de mi firma" action="subsysExpedientes.Expediente@Firma-action" groups="admins,users" order="8"/>
```

`menus.xml` **sí** está sujeto a `agent_docs/view-rules.md` (Categoría 10): de ahí el sufijo `-menuitem`, el prefijo del padre en el `name`, el `groups` canónico `admins,users`, el `order` entero único entre hermanos, el orden fijo de atributos y una sola línea por menú.

**CRITICAL — los dos menús los ve TODO usuario, y es correcto que así sea.** `VAR-10.1` admite en `groups` exactamente `admins`, `admins,users` o `users`, así que **no se puede** restringir un menú a «los que ostentan el perfil `SECRETARIO`»: un `groups` con un grupo propio rompería los tests de `com.educaflow.views`. Quien no ostenta el perfil ve la entrada de menú y abre **una lista vacía**, porque la condición de perfil de §16.0 no le lista ningún expediente. Es exactamente lo que hace falta: lo que había que impedir no es ver el menú, sino **abrir un expediente con un perfil que no se tiene**. **MUST NOT** cambiarse el `groups` de estos dos `menuitem` para intentar ocultarlos.

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

**La excepción del administrador va en TRES de esas cuatro, no en las cuatro** (Paso 16.3). La llevan las tres de **consulta** —«Expedientes Esperando», «Expedientes Cerrados» y la búsqueda—, que fijan `_profile=RESPONSABLE` y por las que, al no existir ningún `<form … profile="RESPONSABLE">` en este tipo, el expediente se le abre siempre en la vista genérica de solo lectura: es exactamente lo que ESC-025 pide y lo que comprueba T-038. **MUST NOT** llevarla «Expedientes Pendientes», que fija `_profile=CREADOR` (`Expediente-pendiente.xml` línea 10) — un perfil que **sí** tiene form en `DATOS_SOLICITUD` y en `PENDIENTE_FIRMA`—, porque `ExpedienteController` solo comprueba que el perfil lo **use** el tipo y `PhaseEventManager.getViewName` (líneas 97-103) devuelve entonces el form del `CREADOR`: exceptuar ahí al administrador le abriría la pantalla **editable** del creador de **cualquier centro**, y como `checkPerfilDelEstado` le exime (`Tramitador.java` línea 232) podría además disparar `VOLVER` sobre la solicitud de otro (`CONTINUAR` y `DELETE` los para `ControlDeAcceso.exigeSerElCreador`). Con el filtro sin excepción, eso queda acotado a los expedientes de su centro activo; ningún escenario pierde nada, porque el administrador consulta por las tres de arriba.

**Y esa pantalla editable se le abre igualmente dentro de su centro, y a todo el que pueda leer el expediente** —el secretario, el vicesecretario y el supervisor por `Expediente.porTipoExpediente` / `porTramite`, aunque ellos no puedan **disparar** nada porque no ostentan `CREADOR`—: el filtro de centro solo **acota** el problema, no lo cierra. Es la parte **no mitigada** de §14 nota 8(d) y **MUST NOT** darse por resuelta: la solución de fondo es del motor.
