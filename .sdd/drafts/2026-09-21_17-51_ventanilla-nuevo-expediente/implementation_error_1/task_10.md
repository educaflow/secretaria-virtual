---
type: implementation-task
template: system
---

# Tarea 10 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-datainit

Referencia el permiso `AsistenteNuevoExpediente.all` desde los grupos `admins` y `users` del data-init global.

La acción es `Modificar`: el destino `src/main/resources/data-init/input/auth.xml` **ya existe**.
**MUST NOT** sobrescribirse ni regenerarse: se **añaden** las dos referencias conservando íntegro todo el
contenido previo del fichero (comprobación de conservación antes de escribir: ningún grupo, permiso ni
referencia existente puede desaparecer).

El permiso en sí (con su `<can>`) lo define `auth-ventanilla.xml`, creado en la **tarea 08**.

## Fila de la tabla «Ficheros a crear o modificar»


| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/resources/data-init/input/auth.xml` | Modificar | k-datainit | Referenciar `AsistenteNuevoExpediente.all` en los grupos `admins` y `users` |

## Texto del diseño — Paso 10 (Seguridad), modificación de `auth.xml`

Modificación de `src/main/resources/data-init/input/auth.xml`: añadir `<permission name="AsistenteNuevoExpediente.all"/>` dentro de los grupos `admins` y `users`. **MUST NOT** redefinir allí su `<can>`: ese fichero tiene `priority="-1"` y se carga el último, así que sobrescribiría al del sistema.

**Verificar al final:** `grep -n "AsistenteNuevoExpediente.all" src/main/java/com/educaflow/system/ventanilla/data-init/input/auth-ventanilla.xml src/main/resources/data-init/input/auth.xml` devuelve tres líneas: la definición del permiso en `auth-ventanilla.xml` y sus dos referencias en `auth.xml` (grupos `admins` y `users`), ninguna de ellas con un `<can>` propio; además `grep -n "auth-ventanilla.xml" src/main/java/com/educaflow/system/ventanilla/data-init/input-config.xml` devuelve el `<input>` del manifiesto.

## Texto del diseño — «Cambios necesarios fuera del sistema», punto 2

2. **`src/main/resources/data-init/input/auth.xml`** — añadir `<permission name="AsistenteNuevoExpediente.all"/>` dentro de los grupos `admins` y `users` (solo la referencia, sin `<can>`).
