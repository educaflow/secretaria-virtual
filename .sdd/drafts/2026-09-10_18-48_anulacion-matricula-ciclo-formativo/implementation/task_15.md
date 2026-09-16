---
type: implementation-task
template: expediente
---

# Tarea 15 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-vistas
- k-code-quality

Crea las **dos bandejas nuevas** del trámite y la pieza de servidor a la que preguntan:

| Fichero a crear | Contenido |
|---|---|
| `src/main/java/com/educaflow/tramites/util/bandeja/BandejaPorPerfilController.java` | El punto de consulta del apartado **16.0** del paso del diseño |
| `src/main/java/com/educaflow/tramites/views/Expediente-revision.xml` | La bandeja de la administrativa, **escrita entera** en el apartado **16.1** |
| `src/main/java/com/educaflow/tramites/views/Expediente-firma.xml` | La bandeja del director, **escrita entera** en el apartado **16.2** |

**Los dos ficheros de vista van escritos enteros en 16.1 y 16.2: se crean con ese contenido exacto, no se interpretan.**

**Nota del descomponedor (decisión documentada):** el `### Paso 16` del diseño toca nueve ficheros de golpe; se ha partido en **dos** tareas atómicas —esta (las piezas **nuevas**: 16.0, 16.1 y 16.2) y la tarea 16 (los ficheros **existentes**: 16.3 y 16.4)—, de modo que cada fichero de la tabla `## 6` queda cubierto por **exactamente una** tarea. El texto introductorio del Paso 16 va verbatim en las dos.

**CRITICAL — la especificación es contrato fijo y la superficie es cerrada: MUST NOT crearse ningún método, vista, acción ni parámetro que no liste.** **MUST NOT** reescribirse en JPQL dentro de las vistas la lógica de «¿ostenta este usuario el perfil P sobre este expediente?»: el dueño de esa pregunta es `PerfilesUsuarioService.getPerfilesSobreExpediente` y el controlador **le pregunta**.

Para obtener el usuario autenticado **MUST** usarse `SecurityUtil.getUser()`; **NUNCA** `AuthUtils.getUser()`.

## Filas de la tabla `## 6. Ficheros a crear o modificar` del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/tramites/util/bandeja/BandejaPorPerfilController.java` | Crear | `k-sistemas`, `k-secure-coding`, `k-vistas` | Punto de consulta de las dos bandejas nuevas: `@CallMethod List<Long> idsExpedientesConPerfil(nombrePerfil, tramiteCode)`, que **pregunta** a `PerfilesUsuarioService.getPerfilesSobreExpediente` en vez de reescribir su JPQL en la vista. Lo especifica el **Paso 16.0** |
| `src/main/java/com/educaflow/tramites/views/Expediente-revision.xml` | Crear | `k-vistas` | Bandeja de la administrativa: `action-view` + `grid` con `_profile=SECRETARIO` y `<domain>self.id IN (:expedientesConPerfil)</domain>` alimentado por el punto de consulta de 16.0 (abiertos, de este trámite, de su centro y con el perfil `SECRETARIO`). Escrito entero en el **Paso 16.1** |
| `src/main/java/com/educaflow/tramites/views/Expediente-firma.xml` | Crear | `k-vistas` | Bandeja del director: lo mismo con `_profile=DIRECTOR` y una condición propia más en el `<domain>`, `self.codeState = 'PENDIENTE_FIRMA_DIRECTOR'`. Escrito entero en el **Paso 16.2** |

## Paso del diseño (verbatim) — introducción del Paso 16 y apartados 16.0, 16.1 y 16.2

### Paso 16 — Bandejas de `SECRETARIO` y `DIRECTOR`, filtro de centro y menús

Este paso toca **seis** ficheros de vista del árbol más el `menus.xml`, y crea además la pieza de servidor a la que las dos bandejas nuevas preguntan (16.0). Los **dos ficheros de vista nuevos** van escritos enteros en 16.1 y 16.2: se crean **con ese contenido exacto**, no se interpretan. En los **cuatro existentes** y en el `menus.xml` se inserta **solo** el fragmento que se indica abajo, sobre la expresión real que hoy tiene cada uno, conservando todo lo demás (16.3 y 16.4). El apartado 16.3-bis declara la decisión de arquitectura que esto abre: dónde viven las vistas y los menús de un trámite concreto.

#### 16.0 — La condición de perfil de las dos bandejas nuevas

**El problema que resuelve.** Las dos bandejas nuevas fijan literalmente `_profile=SECRETARIO` y `_profile=DIRECTOR` (§12.2), y su `menuitem` las enseña a **todo** usuario: `VAR-10.1` fija el atributo `groups` a exactamente `admins`, `admins,users` o `users`, así que **no existe** la variante «restringir el menú a un grupo propio» — un `groups="secretario"` rompería los tests de `com.educaflow.views` (Categoría 10). Y `ExpedienteController.getEventContext` solo comprueba, con `checkProfileDelTipoExpediente`, que el perfil lo **use algún estado del tipo**; nunca que el usuario lo **ostente**. Sin más filtro, cualquiera con lectura sobre el expediente —el propio alumno por `Expediente.creador`, el secretario o el vicesecretario por `Expediente.porTipoExpediente`, el supervisor por `Expediente.porTramite`— que entrase por «Anulaciones de matrícula de mi centro» abriría `PENDIENTE_REVISION` en la pantalla **editable** del `SECRETARIO` (el sentido de la revisión, el motivo del rechazo y el texto de la subsanación), y quien entrase por «Anulaciones de matrícula pendientes de mi firma» abriría `PENDIENTE_FIRMA_DIRECTOR` en la del `DIRECTOR` (la decisión de secretaría y la resolución sin firmar). Eso contradice literalmente RUI-PENDIENTE_REVISION-GENERICA-001, RUI-PENDIENTE_FIRMA_DIRECTOR-GENERICA-001, ESC-008 paso 6, ESC-009 paso 8, ESC-020 paso 4, ESC-024 paso 4 y ESC-029 paso 11: «la decisión de secretaría no se enseña a nadie hasta que el director firma».

**La condición: se le PREGUNTA al dueño, no se reescribe.** La pregunta «¿ostenta este usuario el perfil P sobre este expediente?» **ya tiene un dueño único en el servidor**: `PerfilesUsuarioService.getPerfilesSobreExpediente` (`subsystem/security`), implementado sobre `AceRepository.findNombresPerfilesByExpediente` con su constante `FILTRO_USUARIO`, y es lo que `Tramitador.checkPerfilDelEstado` usa para **autorizar** cada evento. Las dos bandejas nuevas **no** vuelven a escribir esa lógica: **le preguntan**. El `<domain>` de cada una queda reducido a una referencia al resultado:

```xml
        <domain>self.id IN (:expedientesConPerfil)</domain>
        <context name="expedientesConPerfil" expr="call:com.educaflow.tramites.util.bandeja.BandejaPorPerfilController:idsExpedientesConPerfil('&lt;PERFIL&gt;','AnulacionMatriculaCicloFormativo')"/>
```

**MUST NOT** aparecer en ninguno de los dos `<domain>` una sola línea de JPQL sobre `Ace`, ni sobre `CentroUsuarioTipoUsuario`, ni sobre `CentroUsuarioCargo`: esa era una **tercera escritura** del dueño, dentro de dos ficheros de vista, sostenida por una nota de «mantenlo sincronizado a mano» que ningún test destapaba. Con el punto de consulta, quien toque `AceRepository.findNombresPerfilesByExpediente` —sus vías de asignación, sus vías de pertenencia o su tratamiento del centro— cambia el comportamiento de las dos bandejas **automáticamente**, porque pasan por él.

**Dónde vive el punto de consulta.** En `tramites/util/bandeja/`, **no** en `subsystem/expedientes` (que este diseño **MUST NOT** ampliar) y **no** duplicado en la carpeta de versión. Cumple las seis condiciones de entrada de `tramites/util/CLAUDE.md`: lo necesita **cualquier** trámite que declare perfiles propios (1), necesita `subsystem/security` y `subsystem/expedientes` para responder (2), no es dominio de ninguno de los dos —la pregunta de seguridad la sigue respondiendo `subsystem/security`; esto es el **pegamento** que la proyecta a una bandeja, igual que `tramites/util/firma/` es el pegamento entre `subsystem/criptografia` y un `trigger*` (3)—, el motor de expedientes no la necesita (4), no se acopla a ninguna entidad concreta —trabaja sobre `Expediente` y recibe el perfil y el trámite **como parámetros** (5)— y abre un subpaquete por propósito, `bandeja/` (6).

**Especificación de `BandejaPorPerfilController`** — fichero `src/main/java/com/educaflow/tramites/util/bandeja/BandejaPorPerfilController.java`:

```java
package com.educaflow.tramites.util.bandeja;

public class BandejaPorPerfilController { … }
```

- Dependencia: `@Inject PerfilesUsuarioService perfilesUsuarioService;` (campo, `jakarta.inject.Inject`, igual que `PerfilesUsuarioServiceImpl` inyecta su repositorio). Su binding **ya existe** (`SecurityModule`: `bind(PerfilesUsuarioService.class).to(PerfilesUsuarioServiceImpl.class)`), así que **MUST NOT** tocarse ningún módulo Guice.
- Constante: `private static final List<Long> NINGUNO = List.of(-1L);` — la lista que se devuelve cuando no hay nada que listar. **MUST NOT** devolverse una lista vacía: `self.id IN (:lista)` con la lista vacía no es portable, y `-1` no es el `id` de ninguna fila.
- Un único método público, anotado `@CallMethod` (`com.axelor.meta.CallMethod`), igual que `FirmaServidorController`:

  ```java
  @CallMethod
  public List<Long> idsExpedientesConPerfil(String nombrePerfil, String tramiteCode)
  ```

  1. `User user = SecurityUtil.getUser()` — **siempre** el usuario autenticado, **nunca** un usuario que venga por parámetro. Si `user` es nulo o `user.getCentroActivo()` es nulo → `return NINGUNO` (**falla cerrado**).
  2. Candidatos: los expedientes **abiertos** de ese trámite en el **centro activo** del usuario — `JPA.all(Expediente.class).filter("self.abierto = true AND self.tipoExpediente.tramite.code = :tramiteCode AND self.centro = :centro")` con los dos `bind`. Es el único JPQL de la pieza, y **no** habla de perfiles: son las tres condiciones que la bandeja ya prometía en su título (abiertos, de este trámite, de mi centro).
  3. Se queda con aquellos para los que `perfilesUsuarioService.getPerfilesSobreExpediente(expediente, user)` **contiene** `nombrePerfil`. Esta es la consulta al dueño, y es la **única** línea que decide el perfil.
  4. Devuelve sus `id`; si no queda ninguno, `NINGUNO`.

- **Seguridad (`k-secure-coding`).** El método es invocable por cualquier usuario autenticado, y no pasa nada: solo responde **sobre el usuario autenticado**, resuelto con `SecurityUtil.getUser()` dentro del propio método. Quien lo llamara con otro perfil o con otro trámite obtendría los expedientes sobre los que **él mismo** ostenta ese perfil, que es justamente lo que ya puede ver. **MUST NOT** añadírsele un parámetro de usuario, de centro ni de estado.
- **Coste, declarado.** Pregunta al dueño **una vez por expediente candidato**, en vez de resolverlo todo en una sola consulta. El conjunto candidato está acotado por construcción (los expedientes **abiertos** de **un** trámite en **un** centro), así que es el tamaño de la propia bandeja. Si algún día ese coste molestara, el arreglo **MUST** hacerse en el **dueño** —un método masivo en `PerfilesUsuarioService`/`AceRepository`, donde vive el `FILTRO_USUARIO`—, **nunca** devolviendo el JPQL a los `<domain>` de las vistas.

Notas de lectura:

- **CRITICAL — el literal que viaja en la llamada es el nombre del PERFIL, no el de un cargo.** En este trámite coinciden en el texto: existe el **perfil** `SECRETARIO` (que ostenta el tipo de usuario `ADMINISTRATIVO`) y existe el **cargo** `SECRETARIO` del centro, que en cambio ostenta el perfil `RESPONSABLE` (§12.1). `getPerfilesSobreExpediente` devuelve **nombres de perfil**, así que el secretario del centro **no** entra por la bandeja de secretaría — y es lo correcto: la spec le da consulta, no revisión.
- **Falla cerrado**: sin centro activo (o sin `centroUsuarioActivo`, que es lo que el dueño necesita para resolver las vías de pertenencia) no se lista nada. Es la dirección segura y coincide con lo que el usuario podría hacer: sin centro activo `ExpedienteController.getCentroFromCurrentUser` ni siquiera le deja abrir un expediente (§14 nota 13).
- **Estas dos bandejas NO llevan la excepción del administrador** (`esAdministrador`) que sí llevan **tres** de las cuatro genéricas del Paso 16.3 —todas menos «Expedientes Pendientes», que tampoco la lleva (16.3 (a))—, y es deliberado: el administrador **no tiene filas `Ace`**, así que no ostenta `SECRETARIO` ni `DIRECTOR` sobre ningún expediente y las dos bandejas le salen **vacías**. Es exactamente lo que la spec quiere — en ella el administrador **solo consulta** (ESC-025, T-038) y consulta por las bandejas genéricas, que sí le exceptúan del centro y le abren el expediente en la vista de solo lectura. Exceptuarle aquí le pintaría la pantalla **editable** del secretario o la del director, y hasta se la pintaría con los botones puestos: una pantalla que ningún escenario le concede. **Disparar** desde ella no podría —para eso están las cuatro guardas de perfil de §9.2 y §9.3, que existen precisamente porque `checkPerfilDelEstado` le exime (`SecurityUtil.isAdmin`)—, pero cada botón le fallaría con un mensaje de negocio, que es peor interfaz que no listarle el expediente. Por eso tampoco hace falta exceptuarle del centro: el filtro de centro lo aplica el punto de consulta para todos, y para el único usuario al que se exceptuaría la pregunta del perfil ya decide que no.
- **Es interfaz, no defensa** —igual que el filtro de centro (§14 nota 9)—: lo que de verdad impide **actuar** sin el perfil es `checkPerfilDelEstado`, que ya lo comprueba en el servidor en cada evento **para todo el mundo menos para el administrador**, al que exime (`Tramitador.java` línea 232); a él le para la guarda de perfil `ControlDeAcceso.exigeOstentarElPerfilDelEstado` que llevan los cuatro `trigger*` de `REVISION` y `RESOLUCION`, que exige **el perfil que el `<state>` declara** —se lo pregunta a `States`, no lo repite— (§9.0.1, §11 «Las ocho guardas de identidad», y `decisiones.md` D13). Lo que esta condición evita es la **exhibición**: que la pantalla de otro perfil llegue a pintarse. La solución de fondo sigue siendo que la bandeja pase el perfil real del usuario, que es del motor y está declarada en §14 nota 8 como **decisión de arquitectura pendiente de aprobar**; el día en que se apruebe, **desaparecen** la condición, el `<context>` y esta pieza de `tramites/util/bandeja/`.
- **REQUIRED — comprobación en runtime de las dos mecánicas que esta pieza estrena.** Ningún `action-view` del proyecto usa hoy un `<context expr="call:…">` (el precedente de `call:` que sí existe es un `<action-record>`: `subsysFirmas.Pendiente@TareaFirma-set-situacionFirma-action`), ni ningún `<domain>` recibe una **lista** por parámetro. Las dos las soporta la plataforma —`ActionView.evaluate` pasa cada `<context>` por `ActionHandler.evaluate`, que trata el prefijo `call:`, y un `IN (:lista)` se enlaza como cualquier otro parámetro—, pero **MUST** comprobarse abriendo las dos bandejas: si el `call:` no se resolviera o la lista no se enlazara, la bandeja saldría vacía o lanzaría al resolver el parámetro, y **MUST NOT** darse el paso por bueno sin haberlo visto listar. El resultado se calcula **al abrir la bandeja**: un expediente creado después no aparece hasta que se vuelve a abrir, igual que el `centroActivoId` de las genéricas.

#### 16.1 — `tramites/views/Expediente-revision.xml` (nuevo)

Fichero nuevo `src/main/java/com/educaflow/tramites/views/Expediente-revision.xml`, con este contenido **exacto**:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<object-views xmlns="http://axelor.com/xml/ns/object-views"
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://axelor.com/xml/ns/object-views https://axelor.com/xml/ns/object-views/object-views_8.1.xsd">

    <action-view name="subsysExpedientes.Expediente@Revision-action" title="Anulaciones de matrícula de mi centro" model="com.educaflow.subsystem.expedientes.db.Expediente">
        <view type="grid" name="subsysExpedientes.Expediente@Revision-grid"/>
        <domain>self.id IN (:expedientesConPerfil)</domain>
        <context name="_profile" expr="SECRETARIO"/>
        <context name="expedientesConPerfil" expr="call:com.educaflow.tramites.util.bandeja.BandejaPorPerfilController:idsExpedientesConPerfil('SECRETARIO','AnulacionMatriculaCicloFormativo')"/>
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

**Qué decide cada parte, para que no se «mejore» ninguna por su cuenta:** el `_profile=SECRETARIO` es lo que hace que `PENDIENTE_REVISION` se abra en la pantalla de la administrativa; el `<context name="expedientesConPerfil">` es la consulta al dueño del Paso 16.0, y con ella viajan las tres condiciones que esta bandeja promete —abiertos, de **este** trámite y del **centro activo**—, que por eso **ya no** se repiten en el `<domain>`. **MUST NOT** volver a escribirse en el `<domain>` `self.abierto`, el `tramite.code` ni `self.centro`: se duplicaría el literal del trámite dentro del mismo fichero y volvería a haber dos sitios diciendo lo mismo.

#### 16.2 — `tramites/views/Expediente-firma.xml` (nuevo)

Fichero nuevo `src/main/java/com/educaflow/tramites/views/Expediente-firma.xml`, con este contenido **exacto**:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<object-views xmlns="http://axelor.com/xml/ns/object-views"
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://axelor.com/xml/ns/object-views https://axelor.com/xml/ns/object-views/object-views_8.1.xsd">

    <action-view name="subsysExpedientes.Expediente@Firma-action" title="Anulaciones de matrícula pendientes de mi firma" model="com.educaflow.subsystem.expedientes.db.Expediente">
        <view type="grid" name="subsysExpedientes.Expediente@Firma-grid"/>
        <domain>self.id IN (:expedientesConPerfil) AND self.codeState = 'PENDIENTE_FIRMA_DIRECTOR'</domain>
        <context name="_profile" expr="DIRECTOR"/>
        <context name="expedientesConPerfil" expr="call:com.educaflow.tramites.util.bandeja.BandejaPorPerfilController:idsExpedientesConPerfil('DIRECTOR','AnulacionMatriculaCicloFormativo')"/>
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

**CRITICAL — la bandeja del director lleva además `AND self.codeState = 'PENDIENTE_FIRMA_DIRECTOR'`, y esa sí va en el `<domain>`.** Es la única condición propia de **esta** bandeja y no de la pregunta del perfil, así que es suya: su título promete «pendientes de mi firma», y sin ella listaría **todos** los expedientes abiertos del trámite, también los que están en `DATOS_SOLICITUD`, `PENDIENTE_FIRMA` y `PENDIENTE_REVISION`, que no están pendientes de nada suyo. Es además el único estado en el que existe un form para `profile="DIRECTOR"`: en cualquier otro, entrar por esta bandeja llevaría a la vista genérica de solo lectura. La bandeja de la administrativa **no** lleva condición de estado, y su título tampoco la promete: tiene que ver los expedientes de su centro en cualquier estado (ESC-045, T-039).

**CRITICAL — la acotación al trámite sigue siendo obligatoria en las dos, y ahora viaja en el argumento de la llamada.** Sin ella la bandeja listaría expedientes de **cualquier** tipo, y abrir desde ella uno cuyo tipo no declare `SECRETARIO`/`DIRECTOR` lanza «El perfil … no lo usa ningún estado del tipo de expediente …» (`ExpedienteController.checkProfileDelTipoExpediente`). Ver §14 nota 8(c).

**CRITICAL — MUST NOT** replicarse en estos `<domain>` la condición del permiso `Ace` de `auth-expedientes.xml` (la de «quién puede **leer** el expediente»). Sería la misma decisión de autorización con **tres dueños** (el permiso, el dominio de la bandeja y la guarda del trigger). El `<domain>` acota **qué se lista** y con qué perfil se abre; quién puede **leer** lo decide el permiso y quién puede **actuar**, `checkPerfilDelEstado` del motor más la guarda de centro del trigger (§9.0.1).


## `### 12.2 Bandejas` (verbatim)

### 12.2 Bandejas

La vista que ve cada actor la elige la pareja `(estado, _profile)`, y el `_profile` **lo fija el `action-view` de la bandeja**, no el usuario. Las bandejas existentes solo pasan `CREADOR` (Trámites y «Expedientes Pendientes») y `RESPONSABLE` («Expedientes Esperando», «Expedientes Cerrados» y la búsqueda), así que este trámite necesita dos más:

| Bandeja (nueva) | `_profile` | Dominio | Quién la usa |
|---|---|---|---|
| «Anulaciones de matrícula de mi centro» (`subsysExpedientes.Expediente@Revision-action`) | `SECRETARIO` | `self.id IN (:expedientesConPerfil)`, donde la lista la calcula el punto de consulta del Paso 16.0: abiertos **de este trámite**, en el centro activo y **sobre los que el usuario ostenta el perfil `SECRETARIO`** | la administrativa |
| «Anulaciones de matrícula pendientes de mi firma» (`subsysExpedientes.Expediente@Firma-action`) | `DIRECTOR` | lo mismo con el perfil `DIRECTOR`, **más** la condición propia de la bandeja `self.codeState = 'PENDIENTE_FIRMA_DIRECTOR'` | el director |

La del director va además acotada a `self.codeState = 'PENDIENTE_FIRMA_DIRECTOR'`, que es lo que su título anuncia y el único estado con form para `DIRECTOR`. Las dos van **acotadas al trámite** (`self.tipoExpediente.tramite.code = 'AnulacionMatriculaCicloFormativo'`) y llevan el trámite en el título, porque su `_profile` es un perfil que otros tipos de expediente pueden no declarar: listar expedientes ajenos haría reventar al abrirlos (§14 nota 8(c)). El Paso 16 las da escritas enteras.

**Las dos van acotadas además a quien OSTENTA el perfil que fijan** (la condición del Paso 16.0: `self.id IN (:expedientesConPerfil)`, alimentada por el punto de consulta que **pregunta** al dueño de esa pregunta en el servidor). Sin ella, cualquiera con lectura sobre el expediente —el propio alumno por `Expediente.creador`, el secretario o el vicesecretario por `Expediente.porTipoExpediente`, el supervisor por `Expediente.porTramite`— abriría por esas bandejas la pantalla **editable** del `SECRETARIO` o la del `DIRECTOR` y vería la decisión de secretaría antes de que el director firme, contra RUI-PENDIENTE_REVISION-GENERICA-001, RUI-PENDIENTE_FIRMA_DIRECTOR-GENERICA-001, ESC-008, ESC-009, ESC-020, ESC-024 y ESC-029: `ExpedienteController` comprueba que el perfil lo **use** el tipo, nunca que el usuario lo **tenga**. Restringir en cambio el `menuitem` a un grupo propio **no es una opción**: `VAR-10.1` fija el `groups` de todo menú a `admins`, `admins,users` o `users` (Paso 16.4). El **dueño** de la pregunta «¿ostenta este usuario el perfil P sobre este expediente?» es `PerfilesUsuarioService.getPerfilesSobreExpediente` en el servidor, que es lo que `Tramitador.checkPerfilDelEstado` usa para autorizar cada evento, y las dos bandejas **le preguntan**: su `<domain>` es `self.id IN (:expedientesConPerfil)` y la lista la calcula `tramites/util/bandeja/BandejaPorPerfilController` llamando a ese servicio (Paso 16.0). **MUST NOT** reescribirse esa lógica en JPQL dentro de las vistas. Es **interfaz y no defensa**, y la pieza entera —`<context>`, `<domain>` y controlador— desaparece el día en que se apruebe la solución de fondo de §14 nota 8 (que la bandeja pase el perfil real del usuario). **Y solo cubre el camino normal de navegación:** quien pida la vista con `_profile=SECRETARIO` o `DIRECTOR` **sin pasar por la bandeja** la sigue obteniendo, porque el `_profile` viaja en la petición y `ExpedienteController` nunca comprueba que el usuario **ostente** ese perfil para elegir la vista. Por eso las dos `RUI-*-GENERICA-*` de §11 **no** quedan garantizadas por esta condición y el residuo está declarado en §14 nota 8(d).

