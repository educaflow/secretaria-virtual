---
type: implementation-task
template: system
---

# Tarea 10 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-datainit
- k-secure-coding

Referencia el permiso del asistente en los grupos del `auth.xml` global del proyecto.

`src/main/resources/data-init/input/auth.xml` es un fichero **que ya existe** (`Acción: Modificar`): **MUST** editarse
añadiendo **solo** lo que el diseño declara y **conservando** todo lo demás (el resto de grupos, permisos y roles).
Antes de guardar, **MUST** comprobarse que no se ha perdido ningún grupo, permiso ni rol preexistente
(comprobación de conservación); si el cambio los perdería, responde `CONFLICT` en vez de fusionar a mano.
El permiso `AsistenteNuevoExpediente.all` lo **define** el `auth-ventanilla.xml` del sistema (tarea 08); aquí solo se
**referencia**.

## Fila de la tabla «Ficheros a crear o modificar» del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/resources/data-init/input/auth.xml` | Modificar | k-datainit | Referenciar `AsistenteNuevoExpediente.all` en los grupos `admins` y `users` |

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

### Cambios necesarios fuera del sistema (punto 2)

Los dos primeros son ficheros de datos del proyecto; el tercero es la única ampliación normativa pendiente. De las reglas de vistas no queda ninguna: tanto el `<grid action="…">` de los pasos 1 y 2 (D4) como la rama del asistente de `VAR-7.2` (D8) ya están recogidos en `agent_docs/view-rules.md` y en los skills `k-vistas`. **MUST NOT** aplicar ninguno de los tres el diseñador: los aplica `/sdd-implementer`.

2. **`src/main/resources/data-init/input/auth.xml`** — añadir `<permission name="AsistenteNuevoExpediente.all"/>` dentro de los grupos `admins` y `users` (solo la referencia, sin `<can>`).
