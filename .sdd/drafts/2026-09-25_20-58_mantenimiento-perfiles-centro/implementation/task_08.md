---
type: implementation-task
template: system
---

# Tarea 08 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-datainit
- k-secure-coding

# Diseño: Mantenimiento de perfiles de trámites por centro

**Objetivo:** que el supervisor (en los centros que supervisa) y el administrador (en cualquier centro) puedan ver, crear, modificar y borrar las filas de `AceProfileCentro`, con las reglas que garantizan que cada fila dice sin ambigüedad a quién se da el perfil.
**Capa:** subsystem/security
**Especificación de origen:** .sdd/drafts/2026-09-25_20-58_mantenimiento-perfiles-centro/specification.md
**Skills necesarios para la implementación:** k-sistemas, k-validaciones, k-code-quality, k-secure-coding, k-vistas, k-datainit

Las decisiones difíciles y sus alternativas están en `decisiones.md` (D1–D6); este documento es coherente con ellas.

## Ficheros a crear o modificar

Rutas relativas a `src/main/java/com/educaflow/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/security/data-init/input/auth-security.xml` | Modificar | k-datainit, k-secure-coding | + permisos `AceProfileCentro.supervisor` y `Tramite.supervisor` + su enlace al grupo `users`. |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño. Los fragmentos de `auth-security.xml`, `input-config.xml` y `CLAUDE.md` del Paso 7 se aplican **añadiendo** a lo que ya hay (no se borra nada).

**Acción `Modificar`:** el fichero ya existe; se **añade** al final de `<auth>` conservando todo lo existente. Esta tarea cubre solo el apartado 7.1 del Paso 7; el 7.2 (`input-config.xml`) y el 7.3 (`CLAUDE.md`) son tareas aparte (Tarea 09 y Tarea 10), que se copian abajo solo como contexto. Las decisiones D1–D3 citadas están en `design/decisiones.md`.

## Pasos

### Paso 7 — Seguridad (data-init del subsistema y documentación)

Todo dentro de `subsystem/security` (guía de diseño). Reglas de acceso en lenguaje natural:

- **Administrador** (grupo `admins`): ve, crea, modifica y borra filas de cualquier centro. No necesita permiso: Axelor no aplica permisos a `admins`, y el servicio no le aplica V-008.
- **Supervisor** (grupo `users` con el tipo de usuario `SUPERVISOR` en algún centro): ve, crea, modifica y borra solo filas de los centros que supervisa, y lee todos los trámites para poder elegirlos.
- **Cualquier otro usuario** del grupo `users`: tiene el permiso por pertenecer a `users`, pero la condición no le deja ver, modificar ni borrar ninguna fila, y V-008 le rechaza el alta.

**7.1 — `data-init/input/auth-security.xml`** (se añade al final de `<auth>`, conservando todo lo existente):

- Permiso `AceProfileCentro.supervisor`, objeto `com.educaflow.subsystem.security.db.AceProfileCentro`, `create/read/write/remove = true`, `export = false`, en literal:
  `condition="self.centro IN (SELECT cu.centro FROM com.educaflow.subsystem.common.db.CentroUsuario cu JOIN cu.centroUsuarioTipoUsuario cut JOIN cut.tipoUsuario tu WHERE cu.usuario = ? AND tu.codigo = 'SUPERVISOR')"` con `conditionParams="__user__"` (misma forma que `Correo.propio-centro-supervisor` de `auth-correos.xml`, sin `ADMINISTRATIVO`; nombres de entidad y campos comprobados contra ese permiso real).
  Comentario XML obligatorio: «repite en JPQL la consulta de `AceProfileCentroRepository.findCentrosSupervisados`; si cambia una, cambia la otra». En Axelor la condición se aplica al leer, modificar y borrar (filtra el listado y bloquea filas ajenas); en el alta no se evalúa sobre la fila nueva, por eso V-008 es la defensa del alta.
- Permiso `Tramite.supervisor`, objeto `com.educaflow.subsystem.expedientes.db.Tramite`, solo `read = true`, en literal:
  `condition="EXISTS (SELECT cu FROM com.educaflow.subsystem.common.db.CentroUsuario cu JOIN cu.centroUsuarioTipoUsuario cut JOIN cut.tipoUsuario tu WHERE cu.usuario = ? AND tu.codigo = 'SUPERVISOR')"` con `conditionParams="__user__"` (la subconsulta no referencia `self`: da lectura de todos los trámites a quien es `SUPERVISOR` en algún centro). Si al arrancar Axelor no admitiera `EXISTS` sin correlación, la forma equivalente es `? IN (SELECT cu.usuario FROM … mismo JOIN … WHERE tu.codigo = 'SUPERVISOR')` con el mismo `conditionParams`.
  Comentario XML obligatorio: «repite en JPQL la consulta de `AceProfileCentroRepository.findCentrosSupervisados`; si cambia una, cambia la otra» (decisiones D1), y que existe solo para que el selector de trámite de «Mi centro → Perfiles de trámites» no salga vacío (decisiones D3); no da ningún perfil ni cambia lo que ofrece «Nuevo trámite».
- `<group code="users">` con `<permission name="AceProfileCentro.supervisor"/>` y `<permission name="Tramite.supervisor"/>` (decisiones D2).

**7.2 — `data-init/input-config.xml`**: dentro del `<input file="auth-security.xml" root="auth">` existente, tras el `<bind node="permission" …>`, añadir el mismo bind de grupo que usa el data-init global: `<bind node="group" type="com.axelor.auth.db.Group" search="self.code = :code" create="false" update="true">` con `<bind node="@code" to="code"/>` y `<bind node="permission" to="permissions" search="self.name = :name"><bind node="@name" to="name"/></bind>`. Suma a los permisos del grupo (`XMLBinder` hace `addAll`), no los reemplaza; el grupo `users` existe antes de cualquier data-init (`ModuleManager.createDefault`). La `priority="8"` no cambia.

**7.3 — `subsystem/security/CLAUDE.md`** (añadidos, sin quitar nada):

- En la tabla «Las tablas», la fila de `AceProfileCentro` dice que la rellenan las pantallas «Mi centro → Perfiles de trámites» (supervisor, sus centros) y «Administración → Perfiles de trámites por centro» (administrador), a través de `AceProfileCentroService`, que valida el destinatario único.
- En «Permisos de Axelor»: los permisos propios de este subsistema se enlazan al grupo `users` desde su propio `auth-security.xml` (bind de `group` en su `input-config.xml`), no desde el `auth.xml` global. Y la excepción `Tramite.supervisor`: el supervisor **lee** todos los trámites aunque no tenga perfil sobre ellos, solo para poder asignarlos; por eso ahí lo que ve no coincide con lo que puede hacer.

**Verificar:** arrancar con `./run.sh` y comprobar en BD que el grupo `users` tiene los dos permisos (`psql` según `agent_docs/deploy.md`: `auth_group_permissions` del grupo `users` incluye `AceProfileCentro.supervisor` y `Tramite.supervisor`) y que tras un segundo arranque siguen ahí.

## Notas y supuestos (aplicables)

- **El listado del supervisor no lleva `<domain>`.** Lo filtra el permiso `AceProfileCentro.supervisor`; repetir la condición en la vista sería una tercera copia (decisiones D1). El administrador no tiene grupo `users` ni ve el menú del supervisor.
- **Las guías de diseño se respetan sin excepción:** solo se tocan `subsystem/security` y `menus.xml` (el enlace de permisos al grupo va en el data-init de `security`, D2); no se tocan `auth-expedientes.xml`, `PerfilesUsuarioService` ni `AceProfileCentroRepository.findPerfiles`; el «centro del supervisor» son todos sus centros `SUPERVISOR`, nunca `User.centroActivo`.
- **`Tramite.supervisor`** amplía lo que el supervisor puede **leer** de `Tramite` (decisiones D3). No cambia qué trámites le ofrece «Nuevo trámite» (la ventanilla filtra por perfiles), ni qué expedientes ve, ni el cálculo de perfiles: lo que el «Fuera de alcance» protege sigue intacto.
