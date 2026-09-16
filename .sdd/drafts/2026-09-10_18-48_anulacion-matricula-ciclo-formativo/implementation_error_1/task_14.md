---
type: implementation-task
template: expediente
---

# Tarea 14 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-datainit

## Qué hay que hacer

Dos ficheros de datos de demo, los dos con acción **`Modificar`**:

1. **`src/main/resources/data-demo/input/permisos-demo.xml`** — es una **FUSIÓN, no una copia**: añade a ese fichero los elementos del fragmento `design/permisos.xml`, **dentro del bloque que corresponda a cada uno**, **conservando todo lo preexistente** (las asignaciones de `JustificacionFaltaProfesorado` **MUST** seguir ahí). Los perfiles `CREADOR` y `RESPONSABLE` ya están declarados: **MUST NOT** duplicarlos; se añaden solo `SECRETARIO` y `DIRECTOR`. Cómo se fusiona, en `implementation.md` §5 de la plantilla de implementación.
2. **`src/main/resources/data-demo/input/usuarios-demo.xml`** — añade al bloque `<centroUsuarios>` la línea que el `### Paso 15` indica, conservando todo lo demás.

**MUST NOT** escribirse ningún `auth-*.xml`: la `<permission name="AnulacionMatriculaCicloFormativoV1.all">` la genera el build en el data-init del tipo.

## Filas de la tabla «## 6. Ficheros a crear o modificar» del diseño (verbatim)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/resources/data-demo/input/permisos-demo.xml` | **Modificar** | `k-datainit` | Fusión del fragmento `design/permisos.xml` (§12) |
| `src/main/resources/data-demo/input/usuarios-demo.xml` | **Modificar** | `k-datainit` | Una línea `<centroUsuario>` para `admin`, sin la cual el administrador no puede abrir ningún expediente (§12.1 y §14 nota 13) |

## `### Paso 15 — Datos de demo: permisos y centro del administrador` del diseño (verbatim)

### Paso 15 — Datos de demo: permisos y centro del administrador

1. **`permisos-demo.xml`** — es una **fusión, no una copia**: añade a `src/main/resources/data-demo/input/permisos-demo.xml` los elementos de `design/permisos.xml`, **dentro del bloque que corresponda a cada uno**, **conservando todo lo preexistente**. Los perfiles `CREADOR` y `RESPONSABLE` ya están declarados allí: **MUST NOT** duplicarlos; se añaden solo `SECRETARIO` y `DIRECTOR`.

2. **`usuarios-demo.xml`** — añade al bloque `<centroUsuarios>`, junto a los de CIPFP Mislata, la línea:

```xml
<centroUsuario usuarioCode="admin" centroCode="46019660"/>
```

   El usuario `admin` lo crea el bootstrap de Axelor, no la demo, y **hoy no tiene ninguna fila `CentroUsuario`**, así que su `centroActivo` es `null`. `ExpedienteController.getEventContext` llama a `getCentroFromCurrentUser()`, que lanza `RuntimeException("El centro activo es null para el usuario: …")` cuando lo es: sin esta línea el administrador **no puede abrir ningún expediente** y ESC-025 (T-038) revienta antes de pintar la vista. El binding de `data-demo/input-config.xml` busca el `User` por `self.code = :usuarioCode` con `create="false"`, así que **reutiliza** el admin existente y solo le fija el `centroActivo`. El centro elegido no cambia lo que ESC-025 le pide ver —consulta por «Expedientes Esperando», «Expedientes Cerrados» y la búsqueda, que le exceptúan del filtro de centro—, pero **sí** acota lo que ve en «Expedientes Pendientes», que a propósito **no** le exceptúa (Paso 16.3 (a) y §14 notas 8(d) y 13).

**Verificación:** `permisos-demo.xml` conserva las asignaciones de `JustificacionFaltaProfesorado`, `<perfiles>` tiene cuatro `<perfil>` y hay tres asignaciones nuevas por tipo de usuario y tres por cargo; tras arrancar, el usuario `admin` tiene centro activo (`select centro_activo from auth_user where code='admin'` no es nulo) y abre un expediente sin excepción. Comprobación adicional, **sin cambio de ficheros** (el municipio de los centros ya lo siembra el catálogo `common`, §14 nota 16): `select c.name, m.name from centro c left join municipio m on m.id = c.municipio` devuelve «Mislata» y «Alcoy/Alcoi», de modo que un expediente nuevo nace con «Mislata» en «Localidad del centro». **MUST NOT** resetearse la base de datos para este paso.


## `## 12. Asignación de perfiles` del diseño, completa (verbatim)

## 12. Asignación de perfiles

### 12.1 Tabla

| perfil | actor | tipo de actor | vía | bloque de `permisos-demo.xml` |
|---|---|---|---|---|
| `CREADOR` | `ALUMNO` | `TipoUsuario` | `tramiteCode="AnulacionMatriculaCicloFormativo"` | `<asignacionesTipoUsuario>` |
| `SECRETARIO` | `ADMINISTRATIVO` | `TipoUsuario` | `tramiteCode="AnulacionMatriculaCicloFormativo"` | `<asignacionesTipoUsuario>` |
| `RESPONSABLE` | `SUPERVISOR` | `TipoUsuario` | `tramiteCode="AnulacionMatriculaCicloFormativo"` | `<asignacionesTipoUsuario>` |
| `DIRECTOR` | `DIRECTOR` | `Cargo` | `tipoExpedienteCode="AnulacionMatriculaCicloFormativoV1"` | `<asignacionesCargoTipoExpediente>` |
| `RESPONSABLE` | `SECRETARIO` | `Cargo` | `tipoExpedienteCode="AnulacionMatriculaCicloFormativoV1"` | `<asignacionesCargoTipoExpediente>` |
| `RESPONSABLE` | `VICESECRETARIO` | `Cargo` | `tipoExpedienteCode="AnulacionMatriculaCicloFormativoV1"` | `<asignacionesCargoTipoExpediente>` |

- El perfil del estado **inicial** (`CREADOR`) va por `tramiteCode`, como exige el motor: al crear todavía no hay expediente y el `Tramitador` contrasta el perfil contra los `Ace` **sobre el trámite**.
- `SECRETARIO` va también por `tramiteCode` (sobrevive a las versiones). `DIRECTOR`, `SECRETARIO` (cargo) y `VICESECRETARIO` van por `tipoExpedienteCode` porque **no existe** bloque de cargo por trámite: hay que repetir esas filas en cada versión nueva.
- El perfil `SECRETARIO` del trámite lo ostenta el **tipo de usuario** `ADMINISTRATIVO`, no el cargo `SECRETARIO` del centro: son cosas distintas y en `cargos.xml` no hay ningún cargo de administrativo.
- `RESPONSABLE` es el perfil de **consulta**: lo ostentan el secretario y el vicesecretario del centro (por cargo) y el supervisor (por tipo de usuario). Ningún estado abierto tiene formulario para él, así que siempre cae en la vista genérica de solo lectura; y como no coincide con el `profile` de ningún estado con eventos, `checkPerfilDelEstado` le impide disparar nada.
- El **exalumno** no tiene ninguna asignación, así que no puede crear expedientes (ESC-023); sigue viendo los que creó siendo alumno gracias al permiso `Expediente.creador` de `auth-expedientes.xml`, cuya condición es `self.usuarioRegistrador = ?` y **no** depende del tipo de usuario. **MUST** comprobarse en runtime que ese acceso sigue funcionando tras dejar de ser `ALUMNO`.
- El **administrador** no necesita `Ace`: `Tramitador.checkPerfilDelEstado` le exime (`subsystem/expedientes/services/tramitacion/Tramitador.java` línea 232, `SecurityUtil.isAdmin`) y los permisos no le aplican. **Pero sí necesita un centro activo**: `ExpedienteController.getEventContext` llama a `getCentroFromCurrentUser()`, que **lanza** si `centroActivo` es nulo, así que sin él no puede abrir **ningún** expediente; por eso el Paso 15 le da una fila `<centroUsuario>` en `usuarios-demo.xml`. Ver §14 nota 13.
- **CRITICAL — la vista que se le resuelve depende de la bandeja por la que entre, y MUST NOT afirmarse que «ninguna vista de perfil se resuelve para él» ni que «cae siempre en la genérica de solo lectura».** Por las **tres** bandejas genéricas de **consulta** sí cae en la genérica: «Expedientes Esperando» (`src/main/java/com/educaflow/tramites/views/Abierto-Expediente.xml` línea 10), «Expedientes Cerrados» (`Cerrado-Expediente.xml` línea 9) y la búsqueda (`Expediente-search.xml` línea 23) fijan `_profile=RESPONSABLE`, y aunque `ACEPTADA` y `RECHAZADA` declaren ese perfil en el `TipoExpedienteInstance.xml` (que es lo que hace pasar a `checkProfileDelTipoExpediente`, §14 nota 8(b)), **ninguna fase escribe un `<form … profile="RESPONSABLE">`**, así que `getViewName` cae siempre en el form sin perfil. Es por ahí por donde consulta (ESC-025, T-038). **Pero «Expedientes Pendientes» fija `_profile=CREADOR`** (`src/main/java/com/educaflow/tramites/views/Expediente-pendiente.xml` línea 10: `<context name="_profile" expr="CREADOR"/>`), y `CREADOR` **sí** tiene form en `DATOS_SOLICITUD` y en `PENDIENTE_FIRMA` (`fases/solicitud/views.xml`): `ExpedienteController.getEventContext` solo comprueba con `checkProfileDelTipoExpediente` (línea 239) que el perfil lo **use algún estado del tipo**, nunca que el usuario lo **ostente**, y `PhaseEventManager.getViewName` (líneas 97-103) devuelve el form del perfil recibido en cuanto existe. Por esa bandeja, por tanto, en esos dos estados se abre la pantalla **editable** del creador —con «Siguiente»/«Borrar el expediente» o «Atrás»/«Firmar y presentar la solicitud»— a **cualquiera que pueda leer el expediente**: el administrador y también el secretario, el vicesecretario o el supervisor, que leen por `Expediente.porTipoExpediente` / `Expediente.porTramite` (`subsystem/expedientes/data-init/input/auth-expedientes.xml`). La diferencia del administrador es que además **puede disparar** desde ella, porque `checkPerfilDelEstado` le exime: de los eventos de esos dos estados solo `CONTINUAR` y `DELETE` llevan la guarda `ControlDeAcceso.exigeSerElCreador` (§9.1), así que `VOLVER` sobre la solicitud de otro **le pasa** (`PRESENTAR` tampoco lleva guarda de creador, pero lo para el validador: sin documento de identidad cae en la rama de AutoFirma__!! y `FirmaPdf` lo rechaza, §10.1).
- **Qué se hace con eso: acotar el alcance, no taparlo.** El Paso 16.0 **no** lo resuelve —vacía las dos bandejas **nuevas**, donde el administrador no tiene `Ace`, y no toca la genérica—, y el filtro de centro tampoco lo cierra. Lo único que se hace es **reducir el alcance**: el Paso 16.3(a) añade a «Expedientes Pendientes» el filtro de centro **sin** excepción para el administrador, de modo que esa pantalla editable solo se le abre para los expedientes de **su centro activo** y no para los de todos los centros. Lo que queda —que en su propio centro siga abriéndose— es la parte **no mitigada**, declarada en §14 nota 8(d); cerrarla de verdad es la solución de fondo del motor (que la bandeja pase el perfil real del usuario).
- La `<permission name="AnulacionMatriculaCicloFormativoV1.all">` la genera el build en el `auth-<Code>.xml` del data-init del tipo: el diseño **MUST NOT** escribirla en ningún `auth-*.xml`. Se genera con `create/read/write/remove` **sin `condition`**: es el agujero conocido que documenta `CLAUDE.md`, y este diseño **no** intenta taparlo por su cuenta.

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

### 12.3 Fragmento

El fragmento a fusionar está materializado en `design/permisos.xml`: declara solo los perfiles `SECRETARIO` y `DIRECTOR` (que **no** existen todavía en `permisos-demo.xml`) y las seis asignaciones de §12.1. `CREADOR` y `RESPONSABLE` ya están declarados y **MUST NOT** duplicarse.
