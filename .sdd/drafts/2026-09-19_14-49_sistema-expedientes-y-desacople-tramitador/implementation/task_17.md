---
type: implementation-task
template: system
---

# Tarea 17 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-datainit

**Alcance de esta tarea:** crear la carpeta `data-init` del sistema `system/expedientes`: `input-config.xml` y `input/auth-expedientes.xml` (dos filas `Crear`, un único componente). **Estos XML NO están materializados en `design/`**: se escriben a partir de la descripción del Paso 9 (abajo). Según `implementation.md` §3 los datos iniciales/seguridad son tarea «de Java»: carga los skills y delega en `developer-code-implementer` (`implementation.md` §2), con la **superficie cerrada** de estos dos ficheros. El diseño remite a `system/gestioncentro/data-init` como modelo del `input-config.xml` (léelo como referencia de formato; **MUST NOT** copiar de él nada que el Paso 9 no pida). Las modificaciones de los otros dos ficheros de seguridad son de las tareas 18 y 19.

**Del diseño — filas de la tabla «Ficheros a crear o modificar» (verbatim)**

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/system/expedientes/data-init/input-config.xml` | Crear | k-datainit | Manifiesto del data-init del sistema (Paso 9) |
| `src/main/java/com/educaflow/system/expedientes/data-init/input/auth-expedientes.xml` | Crear | k-datainit | Permiso `NuevoExpediente.all` (Paso 9) |

**Del diseño — Paso 9 (verbatim; los ficheros `Modificar` son de las tareas 18 y 19)**

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

`TipoExpediente.all` (grupo `admins`) se deja como está: el grupo `admins` se salta los permisos (`AuthUtils.isAdmin`), así que cambiarlo no protegería nada; la protección es la del Paso 4.

Verificación: arrancar con `./run.sh` y comprobar en el log que el data-init no falla; `grep -rn "ContextoTramitacion.all" src` sin resultados.
