---
name: auth-knowledge
description: Referencia completa sobre el sistema de permisos Axelor en EducaFlow — Permission, Role, Group, condiciones JPQL, patrón de actor, separación read/write y estructura de ficheros auth-*.xml.
---

# auth-knowledge — Permisos Axelor en EducaFlow

## Modelo de datos Axelor Auth

```
Permission  — un permiso sobre un objeto Java (clase FQCN)
  ├── name, object (FQCN)
  ├── condition, conditionParams  — filtro JPQL de filas
  └── canCreate, canRead, canWrite, canRemove, canExport

Role        — agrupa permisos con nombre
  └── permissions (Set<Permission>)

Group       — agrupa roles, se asigna a usuarios
  └── roles (Set<Role>)

User        → group  (un único grupo por usuario)
```

Axelor evalúa permisos con **lógica OR**: si cualquiera de los permisos asignados al usuario concede acceso a una fila, se concede. Esto permite separar un mismo objeto en múltiples permisos (ej. `User.leer` sin condición + `User.editar-propio` con condición).

## Reglas críticas de JPQL en condiciones

- Parámetros: **`?` sin índice**, nunca `?1`/`?2`. Axelor los renumera internamente al combinar condiciones con OR; `?1` se convierte en `?11` (roto).
- **Cada `?`** en el JPQL consume **una posición** de `conditionParams` en orden. Si el mismo valor aparece N veces en la condición, se repite N veces en `conditionParams`.
- Pasar **objetos entidad**, no IDs: `__user__` es el objeto `User`, `__user__.centroActivo` es el `Centro`.
- La condición actúa también como **check de autorización al serializar relaciones** — si es demasiado restrictiva causa `Authorization Error` al abrir formularios con referencias a ese objeto. Por eso `User.leer` no lleva condición aunque `User.editar-propio` sí.

## Patrón de actor estándar (AccessAssignment)

Para filtrar por actor (TipoUsuario o CentroUsuario vía AccessAssignment):

```xml
condition="self IN (
    SELECT aa.tramite
    FROM com.educaflow.subsystem.security.db.AccessAssignment aa
    WHERE aa.tramite IS NOT NULL
    AND (aa.centro IS NULL OR aa.centro = ?)
    AND (
        aa.actor IN (
            SELECT cut.tipoUsuario
            FROM com.educaflow.subsystem.security.db.CentroUsuarioTipoUsuario cut
            WHERE cut.centroUsuario = ?
        )
        OR aa.actor = ?
    )
)"
conditionParams="__user__.centroActivo, __user__.centroUsuarioActivo, __user__.centroUsuarioActivo"
```

`__user__.centroUsuarioActivo` es el `CentroUsuario` activo del usuario (método `User.getCentroUsuarioActivo()`).

## Condiciones simples (sin subquery de actor)

```xml
<!-- Solo el propio usuario -->
condition="self = ?"
conditionParams="__user__"

<!-- Solo el centro activo del usuario -->
condition="self.centro = ?"
conditionParams="__user__.centroActivo"

<!-- Propio usuario como firmante -->
condition="self.firmante = ?"
conditionParams="__user__"

<!-- Relación transitiva: TareaFirma via subquery -->
condition="self.tareaFirma IN (SELECT tf FROM com.educaflow.subsystem.firmas.db.TareaFirma tf WHERE tf.firmante = ?)"
conditionParams="__user__"
```

## Separación read / write en dos permisos

Cuando un objeto aparece como **referencia en formularios** de otros objetos, el permiso de read no puede llevar condición (causaría `Authorization Error` al serializar la relación). Se usan dos permisos:

```xml
<!-- Permiso 1: leer todos sin condición -->
<permission name="User.leer" object="com.axelor.auth.db.User">
  <can create="false" read="true" write="false" remove="false" export="false"/>
</permission>

<!-- Permiso 2: escribir solo el propio -->
<permission name="User.editar-propio" object="com.axelor.auth.db.User"
            condition="self = ?" conditionParams="__user__">
  <can create="false" read="false" write="true" remove="false" export="false"/>
</permission>
```

Axelor aplica OR: read concedido por `User.leer` (sin filtro), write concedido por `User.editar-propio` solo cuando `self = __user__`.

## Estructura de ficheros en el proyecto

Los permisos están en `subsystem/security/data-init/` divididos por área:

```
data-init/
  input-config.xml           — importa todos los auth-* (permisos) y luego auth.xml (roles/grupos)
  input/
    auth-expedientes.xml     — TipoExpediente, Tramite, Expediente, HistorialEstado, MetaFile, etc.
    auth-firmas.xml          — TareaFirma, DocumentoFirma
    auth-common.xml          — Centro, User, Departamento, Cargo
    auth-security.xml        — CentroUsuario, CentroUsuarioTipoUsuario, TipoUsuario
    auth-gestioncentro.xml   — permisos del rol center.admin (UsuarioAutorizado, ViewGestionCentroImport)
    auth.xml                 — roles, grupos, usuarios (referencia permisos por nombre)
    tiposUsuario.xml         — datos de TipoUsuario
```

**Orden crítico en input-config.xml**: todos los `auth-*.xml` (crean los Permission) deben ir **antes** de `auth.xml` (que crea los Role referenciando permisos por nombre).

## Binding en input-config.xml

```xml
<input file="auth-expedientes.xml" root="auth">
    <bind node="permission" type="com.axelor.auth.db.Permission" search="self.name = :name" create="true" update="true">
        <bind node="@name" to="name"/>
        <bind node="@object" to="object"/>
        <bind node="@condition" to="condition"/>
        <bind node="@conditionParams" to="conditionParams"/>
        <bind node="can/@create" to="canCreate"/>
        <bind node="can/@read" to="canRead"/>
        <bind node="can/@write" to="canWrite"/>
        <bind node="can/@remove" to="canRemove"/>
        <bind node="can/@export" to="canExport"/>
    </bind>
</input>
```

⚠️ Usar `@condition` y `@conditionParams` (atributos XML con `@`). Si se usa `<condition>` como elemento hijo, se importa como `null` y todos los permisos quedan sin filtro → acceso total.

## Roles y grupos en el proyecto

```
Grupo admins        → acceso total (gestionado por Axelor, sin roles explícitos)
Grupo center-admins → roles: users.all + center.admin
Grupo users         → rol: users.all
```

**`users.all`**: permisos mínimos para usuarios normales (expedientes, firmar, ver sus datos).
**`center.admin`**: permisos adicionales para admin del centro (UsuarioAutorizado, importación, editar centro).

## EducaFlowAuthResolver — herencia JPA

Axelor compara el campo `object` de un permiso por igualdad exacta de clase. Un permiso sobre `Expediente` **no se aplica** a `JustificacionFaltaProfesorado` aunque herede de ella. El resolver en `subsystem/security/EducaFlowAuthResolver.java` intercepta cualquier subclase de `Expediente` y devuelve los permisos de `Expediente`.