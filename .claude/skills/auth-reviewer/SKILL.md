---
name: auth-reviewer
description: Revisa que los permisos Axelor en los ficheros auth-*.xml y su input-config.xml cumplen todas las reglas de /auth-knowledge — JPQL, conditionParams, separación read/write, roles y estructura.
---

# auth-reviewer — Lista de verificación de permisos

## Qué leer

1. Los ficheros `auth-*.xml` creados o modificados (en `subsystem/security/data-init/input/`).
2. El `input-config.xml` de security si se han añadido ficheros nuevos o modificado bindings.
3. El skill `/auth-knowledge` para tener presentes todas las reglas.

## JPQL en condiciones

- [ ] Los parámetros son `?` sin índice — nunca `?1`, `?2`, etc.
- [ ] El número de `?` en `condition` coincide exactamente con el número de valores en `conditionParams`.
- [ ] Los valores en `conditionParams` son **objetos entidad** (`__user__`, `__user__.centroActivo`, `__user__.centroUsuarioActivo`), nunca IDs numéricos.
- [ ] Si el mismo valor se necesita dos veces en la condición, aparece dos veces en `conditionParams`.
- [ ] Las subconsultas usan `self.campo IN (SELECT ...)` en lugar de correlaciones directas con `EXISTS` donde `self` puede no correlacionarse.

## Condición vs. serialización de relaciones

- [ ] Los objetos que aparecen como **referencia en formularios** de otros (ej. `User` como creador de un expediente, `Centro` como referencia en CentroUsuario) tienen un permiso de read **sin condición** separado. Si solo tienen permiso con condición, se produce `Authorization Error` al abrir formularios que los referencian.
- [ ] Los objetos que solo se gestionan directamente (ej. `UsuarioAutorizado`) pueden tener un único permiso con condición para read+write.

## Permisos demasiado permisivos en users.all

- [ ] `User` no tiene CRUD total sin condición. Debe tener `User.leer` (read sin condición) + `User.editar-propio` (write con `self = ?`).
- [ ] `Centro` no tiene write/create/remove sin condición. Los usuarios normales solo leen su centro activo.
- [ ] `CentroUsuario` no tiene create/write/remove en users.all. Los usuarios normales solo leen sus propios.
- [ ] `CentroUsuarioTipoUsuario` no tiene create/write/remove en users.all.
- [ ] `UsuarioAutorizado` no está en el rol `users.all`. Solo debe estar en `center.admin`.
- [ ] `ViewGestionCentroImport` no está en el rol `users.all`. Solo debe estar en `center.admin`.
- [ ] `ViewGestionCentroCambioCurso` no está en el rol `users.all`. Solo debe estar en `center.admin`.
- [ ] `TareaFirma` y `DocumentoFirma` tienen condición que limita al firmante (`self.firmante = ?` o subquery equivalente).

## input-config.xml

- [ ] Los `<input>` de ficheros `auth-*.xml` van **antes** del `<input>` de `auth.xml`. Los roles referencian permisos por nombre; si el permiso no existe al crear el rol, la asignación falla silenciosamente.
- [ ] Cada `<bind>` de condición usa `@condition` y `@conditionParams` (atributos XML con `@`), **no** elementos hijo como `<condition>` o `<domain>`.
- [ ] El `root` del `<input>` es `"auth"` para todos los ficheros `auth-*.xml`.
- [ ] Si se ha añadido un fichero `auth-*.xml` nuevo, hay un `<input>` correspondiente en `input-config.xml`.

## Estructura de ficheros

- [ ] Los permisos de cada área están en su fichero correcto:
  - Expedientes → `auth-expedientes.xml`
  - Firmas → `auth-firmas.xml`
  - Datos maestros (Centro, User, Departamento, Cargo) → `auth-common.xml`
  - Seguridad (CentroUsuario, TipoUsuario) → `auth-security.xml`
  - Admin centro (UsuarioAutorizado, ViewGestionCentroImport) → `auth-gestioncentro.xml`
- [ ] `auth.xml` solo contiene `<role>`, `<group>` y `<users>`, nunca `<permission>` directamente.
- [ ] El rol `center.admin` solo contiene permisos de gestión de centro — no duplica permisos de `users.all`.
- [ ] Los permisos del rol `center.admin` están definidos en `auth-gestioncentro.xml`.

## Grupos y roles

- [ ] El grupo `users` solo tiene el rol `users.all`.
- [ ] El grupo `center-admins` tiene los roles `users.all` + `center.admin`.
- [ ] El grupo `admins` no tiene roles explícitos (Axelor les da acceso total).