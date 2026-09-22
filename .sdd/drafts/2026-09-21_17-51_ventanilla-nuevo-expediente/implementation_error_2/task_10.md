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

El permiso en sí (con su `<can>`) lo define `auth-ventanilla.xml`, creado en la **tarea 08**; aquí
**MUST NOT** redefinirse su `<can>`.


## Estado del árbol al empezar (decisión del descomponedor, no del diseño)

Una ejecución anterior de `/sdd-implementer` se detuvo por un error del diseño (ver
`.sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/implementation_error_1/error_design.log`) y dejó
**ya materializados** en el árbol los ficheros de esta tarea, escritos contra una versión anterior del
diseño. La **fuente de verdad es el diseño de hoy**: si el fichero ya existe, **MUST** comprobarse que
coincide exactamente con lo que este texto prescribe y, si no coincide, dejarlo como el diseño manda
(para los XML materializados, copia literal desde `design/`). Que el destino ya exista **NO** es un
`CONFLICT`: la acción `Crear` de la tabla se refiere al estado previo a la iniciativa.

## Fila de la tabla «Ficheros a crear o modificar»

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/resources/data-init/input/auth.xml` | Modificar | k-datainit | Referenciar `AsistenteNuevoExpediente.all` en los grupos `admins` y `users` |

## Paso 10 del diseño — Seguridad (verbatim)

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

Regla de acceso en lenguaje natural: **cualquier usuario con sesión iniciada** ve el menú y puede usar el asistente; el permiso solo le deja operar sobre la ficha de pantalla, que no se guarda. El alcance real —qué centros se le ofrecen, qué trámites, cómo puede presentar y para quién— lo decide el servidor en cada paso a partir de sus `CentroUsuario` y de `PerfilesUsuarioService`, **nunca** de `User.centroActivo`: el centro es el que el usuario elige en el paso 1, exactamente como en los expedientes que este asistente crea. El único listado de centros que el asistente ofrece lo construye el servidor a partir de los `CentroUsuario` del usuario autenticado, y el `centro` que llega del cliente no le concede nada por sí mismo: `getPerfilesDeInicioSobreTramite` devuelve vacío para un centro al que no pertenece —así que el paso 2 no le ofrece ningún trámite— y la puerta 5 de `validateCrear` lo rechaza con el literal del motor. El Administrador no tiene aquí ningún alcance especial: se le tratan sus centros como a cualquier otro.

Modificación de `src/main/resources/data-init/input/auth.xml`: añadir `<permission name="AsistenteNuevoExpediente.all"/>` dentro de los grupos `admins` y `users`. **MUST NOT** redefinir allí su `<can>`: ese fichero tiene `priority="-1"` y se carga el último, así que sobrescribiría al del sistema.

**Verificar al final:** `grep -n "AsistenteNuevoExpediente.all" src/main/java/com/educaflow/system/ventanilla/data-init/input/auth-ventanilla.xml src/main/resources/data-init/input/auth.xml` devuelve tres líneas: la definición del permiso en `auth-ventanilla.xml` y sus dos referencias en `auth.xml` (grupos `admins` y `users`), ninguna de ellas con un `<can>` propio; además `grep -n "auth-ventanilla.xml" src/main/java/com/educaflow/system/ventanilla/data-init/input-config.xml` devuelve el `<input>` del manifiesto.


## «Cambios necesarios fuera del sistema», punto 2 (verbatim)

2. **`src/main/resources/data-init/input/auth.xml`** — añadir `<permission name="AsistenteNuevoExpediente.all"/>` dentro de los grupos `admins` y `users` (solo la referencia, sin `<can>`).
