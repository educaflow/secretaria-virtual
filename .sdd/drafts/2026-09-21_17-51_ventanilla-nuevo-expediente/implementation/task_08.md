---
type: implementation-task
template: system
---

# Tarea 08 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-datainit
- k-secure-coding

Crea el data-init propio del sistema `system/ventanilla`: el manifiesto y el fichero de permisos. Los dos ficheros van
en la misma tarea porque son un único componente lógico (el manifiesto declara el `<input>` que carga el otro fichero).

Los dos son **ficheros nuevos** (`Acción: Crear`): si alguno de los destinos ya existiera, responde `CONFLICT`.

## Filas de la tabla «Ficheros a crear o modificar» del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/system/ventanilla/data-init/input-config.xml` | Crear | k-datainit | Manifiesto del data-init del sistema |
| `src/main/java/com/educaflow/system/ventanilla/data-init/input/auth-ventanilla.xml` | Crear | k-datainit | Permiso del modelo de pantalla |

## Diseño (verbatim)

### Paso 10 — Seguridad

`src/main/java/com/educaflow/system/ventanilla/data-init/input/auth-ventanilla.xml` con un único permiso:

```xml
<?xml version="1.0"?>
<auth>
  <!-- AsistenteNuevoExpediente es persistable="false": no tiene filas que filtrar, así que el
       permiso no lleva condition. Quién puede iniciar qué y en qué centro lo decide el servidor
       (AsistenteNuevoExpedienteServiceImpl.validateTriggerInitialEvent y TramitadorService). -->
  <permission name="AsistenteNuevoExpediente.all"
              object="com.educaflow.system.ventanilla.db.AsistenteNuevoExpediente">
    <can create="true" read="true" write="true" remove="false" export="false"/>
  </permission>
</auth>
```

Y su manifiesto `src/main/java/com/educaflow/system/ventanilla/data-init/input-config.xml`, con `priority="10"` y un `<input file="auth-ventanilla.xml" root="auth">` que enlaza `name`, `object`, `condition`, `conditionParams` y los cinco `can/@*` sobre `com.axelor.auth.db.Permission` (`search="self.name = :name"`, `create="true" update="true"`), igual que el de `system/expedientes`.

Regla de acceso en lenguaje natural: **cualquier usuario con sesión iniciada** ve el menú y puede usar el asistente; el permiso solo le deja operar sobre la ficha de pantalla, que no se guarda. El alcance real —qué centros se le ofrecen, qué trámites, cómo puede presentar y para quién— lo decide el servidor en cada paso, y de qué centro se fía para ello es una **desviación declarada** de `k-secure-coding` §4: su dueño es **D9** de `decisiones.md`, que fija el alcance y la ampliación normativa pendiente.

Modificación de `src/main/resources/data-init/input/auth.xml`: añadir `<permission name="AsistenteNuevoExpediente.all"/>` dentro de los grupos `admins` y `users`. **MUST NOT** redefinir allí su `<can>`: ese fichero tiene `priority="-1"` y se carga el último, así que sobrescribiría al del sistema.

**Verificar al final:** `grep -n "AsistenteNuevoExpediente.all" src/main/java/com/educaflow/system/ventanilla/data-init/input/auth-ventanilla.xml src/main/resources/data-init/input/auth.xml` devuelve tres líneas: la definición del permiso en `auth-ventanilla.xml` y sus dos referencias en `auth.xml` (grupos `admins` y `users`), ninguna de ellas con un `<can>` propio; además `grep -n "auth-ventanilla.xml" src/main/java/com/educaflow/system/ventanilla/data-init/input-config.xml` devuelve el `<input>` del manifiesto.

---

### Paso 11 — Datos iniciales

No hay catálogos propios. La ventanilla se apoya en datos que ya cargan otros subsistemas: los tipos de trámite y los trámites (`subsystem/expedientes` y el data-init que el build genera por trámite) y los perfiles por defecto para iniciarlos (`subsystem/security`: `AceProfileGlobal` y `AceProfileTipoTramite`).

---
