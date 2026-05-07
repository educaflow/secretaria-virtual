# Referencia de permisos Axelor en EducaFlow

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

---

## Reglas críticas de JPQL en condiciones

- Parámetros: **`?` sin índice**, nunca `?1`/`?2`. Axelor los renumera internamente al combinar condiciones con OR; `?1` se convierte en `?11` (roto).
- **Cada `?`** en el JPQL consume **una posición** de `conditionParams` en orden. Si el mismo valor aparece N veces en la condición, se repite N veces en `conditionParams`.
- Pasar **objetos entidad**, no IDs: `__user__` es el objeto `User`, `__user__.centroActivo` es el `Centro`.
- La condición actúa también como **check de autorización al serializar relaciones** — si es demasiado restrictiva causa `Authorization Error` al abrir formularios con referencias a ese objeto.
- En dominios de **action-view/panel**: los parámetros son nombrados (`:__user__`), pero `:__user__.campo` NO funciona — usar subquery scalar en su lugar.
- `self` en subconsultas `EXISTS` puede no correlacionarse correctamente → usar patrón `self.id IN (SELECT ...)`.
- Preferir comparaciones de entidad directas en lugar de comparar IDs: `self IN (SELECT aa.tramite ...)`, `aa.actor IN (SELECT cut.tipoUsuario ...)`.
- Entidades con herencia JOINED (`TipoUsuario`, `CentroUsuario`) → navegar a campos de subtipo puede fallar; usar subselect explícito:
  `t.tipoUsuario IN (SELECT tu FROM TipoUsuario tu WHERE tu.code = 'X')`

---

## Roles y grupos del proyecto

```
Grupo admins        → acceso total (gestionado por Axelor, sin roles explícitos)
Grupo center-admins → roles: users.all + center.admin
Grupo users         → rol: users.all
```

**`users.all`**: permisos mínimos para usuarios normales (expedientes, firmar, ver sus datos).
**`center.admin`**: permisos adicionales para admin del centro (UsuarioAutorizado, importación, editar centro).

---

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

---

## Patrones de condición

### Sin condición (lectura global de datos maestros)
```xml
<permission name="NombreClase.leer"
            object="com.educaflow.subsystem.{modulo}.db.NombreClase">
  <can create="false" read="true" write="false" remove="false" export="false"/>
</permission>
```

### Filtrado por el propio usuario
```xml
<permission name="NombreClase.editar-propio"
            object="com.educaflow.subsystem.{modulo}.db.NombreClase"
            condition="self.usuario = ?"
            conditionParams="__user__">
  <can create="false" read="false" write="true" remove="false" export="false"/>
</permission>
```

### Filtrado por el centro activo
```xml
<permission name="NombreClase.admin"
            object="com.educaflow.subsystem.{modulo}.db.NombreClase"
            condition="self.centro = ?"
            conditionParams="__user__.centroActivo">
  <can create="true" read="true" write="true" remove="true" export="false"/>
</permission>
```

### Patrón de actor estándar (AccessAssignment)

El subquery de actor es el mismo en todas las condiciones. El orden de los `?` es siempre: `centro`, `user`, `centro`, `user`, `centro` → `conditionParams` repite 5 valores:

```xml
<permission name="Tramite.creador"
            object="com.educaflow.subsystem.expedientes.db.Tramite"
            condition="self IN (
    SELECT aa.tramite
    FROM com.educaflow.subsystem.security.db.AccessAssignment aa
    WHERE aa.accessProfile.name = 'CREADOR'
    AND aa.tramite IS NOT NULL
    AND (aa.centro IS NULL OR aa.centro = ?)
    AND (
        aa.actor IN (
            SELECT cut.tipoUsuario
            FROM com.educaflow.subsystem.security.db.CentroUsuarioTipoUsuario cut
            WHERE cut.centroUsuario.usuario = ?
            AND cut.centroUsuario.centro = ?
        )
        OR
        aa.actor IN (
            SELECT cu
            FROM com.educaflow.subsystem.security.db.CentroUsuario cu
            WHERE cu.usuario = ?
            AND cu.centro = ?
        )
    )
)"
            conditionParams="__user__.centroActivo, __user__, __user__.centroActivo, __user__, __user__.centroActivo"
>
  <can create="false" read="true" write="false" remove="false" export="false"/>
</permission>
```

### Filtro de domain en vistas (parámetros nombrados)

En `domain` de `<action-view>` o `<panel>` se usan parámetros **nombrados** (`:__user__`), a diferencia de los permisos que usan `?`:

```xml
domain="self IN (
    SELECT aa.tramite FROM ...AccessAssignment aa
    WHERE aa.accessProfile.name = 'CREADOR'
    AND aa.tramite IS NOT NULL
    AND (aa.centro IS NULL OR aa.centro = :__user__.centroActivo)
    AND (aa.actor IN (SELECT cut.tipoUsuario FROM ...CentroUsuarioTipoUsuario cut
                      WHERE cut.centroUsuario.usuario = :__user__)
         OR aa.actor IN (SELECT cu FROM ...CentroUsuario cu WHERE cu.usuario = :__user__))
)"
```

---

## Separación read / write en dos permisos

Cuando un objeto aparece como **referencia en formularios** de otros objetos, el permiso de read no puede llevar condición (causaría `Authorization Error` al serializar la relación). Se crean dos permisos:

```xml
<!-- 1. Leer todos sin condición -->
<permission name="NombreClase.leer" object="...NombreClase">
  <can create="false" read="true" write="false" remove="false" export="false"/>
</permission>

<!-- 2. Escribir solo el propio -->
<permission name="NombreClase.editar-propio" object="...NombreClase"
            condition="self = ?" conditionParams="__user__">
  <can create="false" read="false" write="true" remove="false" export="false"/>
</permission>
```

Axelor aplica OR: read concedido por el primer permiso (sin filtro), write concedido por el segundo solo cuando pasa la condición.

---

## Los 4 permisos de Expediente

```xml
<!-- 1. El usuario creó el expediente -->
<permission name="Expediente.creador"
            object="com.educaflow.subsystem.expedientes.db.Expediente"
            condition="self.creador = ?"
            conditionParams="__user__">
  <can create="false" read="true" write="false" remove="false" export="true"/>
</permission>

<!-- 2. Hay un AccessAssignment sobre esta instancia concreta -->
<permission name="Expediente.porExpediente"
            object="com.educaflow.subsystem.expedientes.db.Expediente"
            condition="self IN (
    SELECT aa.expediente FROM ...AccessAssignment aa
    WHERE aa.expediente IS NOT NULL
    AND (aa.centro IS NULL OR aa.centro = ?)
    AND (aa.actor IN (SELECT cut.tipoUsuario FROM ...CentroUsuarioTipoUsuario cut
                      WHERE cut.centroUsuario.usuario = ? AND cut.centroUsuario.centro = ?)
         OR aa.actor IN (SELECT cu FROM ...CentroUsuario cu WHERE cu.usuario = ? AND cu.centro = ?))
)"
            conditionParams="__user__.centroActivo, __user__, __user__.centroActivo, __user__, __user__.centroActivo">
  <can create="false" read="true" write="false" remove="false" export="false"/>
</permission>

<!-- 3. Hay un AccessAssignment sobre el TipoExpediente -->
<permission name="Expediente.porTipoExpediente"
            object="com.educaflow.subsystem.expedientes.db.Expediente"
            condition="self.tipoExpediente IN (SELECT aa.tipoExpediente ...)"
            conditionParams="__user__.centroActivo, __user__, __user__.centroActivo, __user__, __user__.centroActivo"/>

<!-- 4. Hay un AccessAssignment sobre el Tramite (excluye CREADOR) -->
<permission name="Expediente.porTramite"
            object="com.educaflow.subsystem.expedientes.db.Expediente"
            condition="self.tipoExpediente.tramite IN (SELECT aa.tramite ... AND aa.accessProfile.name != 'CREADOR' ...)"
            conditionParams="__user__.centroActivo, __user__, __user__.centroActivo, __user__, __user__.centroActivo"/>
```

Todos asignados al rol `users.all`.

---

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
