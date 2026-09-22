---
type: implementation-task
template: system
---

# Tarea 08 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-datainit

Crea la carpeta `data-init` del sistema `system/ventanilla` con sus **dos** ficheros, que son un único
componente lógico (el manifiesto y el fichero de datos que referencia):

- `src/main/java/com/educaflow/system/ventanilla/data-init/input-config.xml` (Acción: `Crear`)
- `src/main/java/com/educaflow/system/ventanilla/data-init/input/auth-ventanilla.xml` (Acción: `Crear`)

**ALCANCE**: en esta tarea **solo** se crean esos dos ficheros. La modificación de
`src/main/resources/data-init/input/auth.xml` (referenciar el permiso desde los grupos `admins` y `users`) es
de la **tarea 10** y **MUST NOT** hacerse aquí, aunque el texto del diseño que sigue la mencione.

## Filas de la tabla «Ficheros a crear o modificar»


| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/system/ventanilla/data-init/input-config.xml` | Crear | k-datainit | Manifiesto del data-init del sistema |
| `src/main/java/com/educaflow/system/ventanilla/data-init/input/auth-ventanilla.xml` | Crear | k-datainit | Permiso del modelo de pantalla |

## Texto del diseño — Paso 10 (Seguridad)

### Paso 10 — Seguridad

`src/main/java/com/educaflow/system/ventanilla/data-init/input/auth-ventanilla.xml` con un único permiso:

```xml
<?xml version="1.0"?>
<auth>
  <!-- AsistenteNuevoExpediente es persistable="false": no tiene filas que filtrar, así que el
       permiso no lleva condition. Quién puede iniciar qué y en qué centro lo decide el servidor
       (AsistenteNuevoExpedienteServiceImpl.validateCrear y TramitadorService). -->
  <permission name="AsistenteNuevoExpediente.all"
              object="com.educaflow.system.ventanilla.db.AsistenteNuevoExpediente">
    <can create="true" read="true" write="true" remove="false" export="false"/>
  </permission>
</auth>
```

Y su manifiesto `src/main/java/com/educaflow/system/ventanilla/data-init/input-config.xml`, con `priority="10"` y un `<input file="auth-ventanilla.xml" root="auth">` que enlaza `name`, `object`, `condition`, `conditionParams` y los cinco `can/@*` sobre `com.axelor.auth.db.Permission` (`search="self.name = :name"`, `create="true" update="true"`), igual que el de `system/expedientes`.

Regla de acceso en lenguaje natural: **cualquier usuario con sesión iniciada** ve el menú y puede usar el asistente; el permiso solo le deja operar sobre la ficha de pantalla, que no se guarda. El alcance real —qué centros se le ofrecen, qué trámites, cómo puede presentar y para quién— lo decide el servidor en cada paso a partir de sus `CentroUsuario` y de `PerfilesUsuarioService`, **nunca** de `User.centroActivo`: el centro es el que el usuario elige en el paso 1, exactamente como en los expedientes que este asistente crea. Eso es la «Excepción — expedientes» de `[[k-secure-coding]]` § 4, que hoy enumera tres paquetes y **no** incluye ninguno de `system/**`; la ampliación al sistema que crea expedientes está declarada en el **punto 6** de «Cambios necesarios fuera del sistema» y **MUST NOT** darse por supuesta aquí. El Administrador no tiene aquí ningún alcance especial: se le tratan sus centros como a cualquier otro.


**Verificar al final:** `grep -n "AsistenteNuevoExpediente.all" src/main/java/com/educaflow/system/ventanilla/data-init/input/auth-ventanilla.xml src/main/resources/data-init/input/auth.xml` devuelve tres líneas: la definición del permiso en `auth-ventanilla.xml` y sus dos referencias en `auth.xml` (grupos `admins` y `users`), ninguna de ellas con un `<can>` propio; además `grep -n "auth-ventanilla.xml" src/main/java/com/educaflow/system/ventanilla/data-init/input-config.xml` devuelve el `<input>` del manifiesto.

## Texto del diseño — Paso 11 (Datos iniciales)

### Paso 11 — Datos iniciales

No hay catálogos propios. La ventanilla se apoya en datos que ya cargan otros subsistemas: los tipos de trámite y los trámites (`subsystem/expedientes` y el data-init que el build genera por trámite) y los perfiles por defecto para iniciarlos (`subsystem/security`: `AceProfileGlobal` y `AceProfileTipoTramite`).

---
