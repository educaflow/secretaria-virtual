---
type: implementation-task
---

# Tarea 18 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-secure-coding
- k-datainit

Crea los permisos de seguridad del sistema de grupos y notas y regístralos en la configuración de datos iniciales.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/system/gruposnotas/data-init/input/auth-gruposnotas.xml` (+ entrada en su `system/gruposnotas/data-init/input-config.xml`) | Crear/Modificar | k-secure-coding, k-datainit | Permisos del grupo `admins`/`users` sobre las 4 entidades (descrito en Seguridad) |

> No hay XML materializado en `design/` para este fichero: se construye siguiendo el patrón de los ficheros existentes `subsystem/security/data-init/input/auth-security.xml` / `subsystem/expedientes/data-init/input/auth-expedientes.xml`, según describe el Paso 9 del diseño. Cada sistema/subsistema lleva su seguridad en su **propia** carpeta `data-init` (ver `k-datainit`), por eso este fichero va en `system/gruposnotas/data-init/input/`, no en el `data-init` global. **MUST** localizar y replicar exactamente ese patrón de `Permission` + asignación a grupos. **MUST NOT** inventar un formato distinto; si el patrón existente no cuadra con lo descrito, reporta `BLOCKED`.

### Descripción del diseño (Paso 9 — Seguridad)

Modelo de roles del proyecto (código real en `subsystem/security`, NO k-seguridad): grupos `admins` y `users`, y tipos de usuario (`TipoUsuario.codigo`: SUPERVISOR, ALUMNO, …). Reglas de acceso, en lenguaje natural:

- **Administrador** (grupo `admins`): permisos CREATE/READ/WRITE/REMOVE sobre `Grupo`, `ModuloGrupo`, `AlumnoGrupo`, `Nota` sin restricción de centro. Ve "Grupos (administración)" y puede reabrir (V-Grupo-008 lo confirma en servidor).
- **Supervisor** (tipo activo SUPERVISOR, grupo `users`): permisos CREATE/READ/WRITE/REMOVE sobre las 4 entidades **restringidos a su centro**; la restricción operativa la imponen el `<domain>` de la action-view del supervisor y las reglas de servidor (R-Grupo-002 fija centro/cursoAcademico; nunca puede reabrir). No ve "Grupos (administración)".
- **Alumno** (tipo activo ALUMNO, grupo `users`): permiso **solo READ** sobre `AlumnoGrupo` y `Nota` (y lectura de `Grupo`/`ModuloGrupo` para mostrar nombres), restringido a sus propias pertenencias por el `<domain>self.alumno = :__user__`. No crea ni modifica nada.

Materialización: fichero `system/gruposnotas/data-init/input/auth-gruposnotas.xml` con los permisos (`Permission`) y su asignación a los grupos `admins`/`users` siguiendo el patrón de `auth-security.xml`/`auth-expedientes.xml` existentes, más la entrada en `system/gruposnotas/data-init/input-config.xml`. El control fino por centro y por propiedad lo dan el diseño de servicio/vista descrito arriba (k-secure-coding), no un ACL por campo.

### Descripción del diseño (Paso 10 — Datos iniciales)

No se precargan catálogos propios del sistema (Grupo/ModuloGrupo/AlumnoGrupo/Nota son datos de explotación, no maestros). El tipo `SUPERVISOR` ya existe en `subsystem/common/data-init/input/tiposUsuario.xml`. Solo se añaden los permisos del Paso 9.
