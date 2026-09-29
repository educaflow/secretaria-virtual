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
- Pasar **objetos entidad**, no IDs: `__user__` es el objeto `User` completo.
- La condición actúa también como **check de autorización al serializar relaciones** — si es demasiado restrictiva causa `Authorization Error` al abrir formularios con referencias a ese objeto.
- En dominios de **action-view/panel**: los parámetros son nombrados (`:nombre`). `:__user__` se admite como objeto entidad completo, pero **`:__user__.campo` NO funciona** — Hibernate no admite puntos en nombres de parámetro. Solución: definir un `<context name="campo" expr="eval:__user__.campo"/>` dentro del `<action-view>` y referenciarlo como `:campo` en el `<domain>`.
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

### Filtrado por los centros del usuario
```xml
<permission name="UsuarioAutorizado.admin"
            object="com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado"
            condition="self.centro IN (SELECT cu.centro FROM com.educaflow.subsystem.common.db.CentroUsuario cu JOIN cu.centroUsuarioTipoUsuario cut JOIN cut.tipoUsuario tu WHERE cu.usuario = ? AND tu.codigo = 'SUPERVISOR')"
            conditionParams="__user__">
  <can create="true" read="true" write="true" remove="true" export="false"/>
</permission>
```

> No existe ningún «centro activo» (el antiguo `User.centroActivo` se eliminó): el alcance de un permiso son los centros del usuario según su tipo (p. ej. `SUPERVISOR`), o el centro del expediente en los permisos de expedientes.

### Patrón de actor estándar (AceProfile*)

Las condiciones de expedientes siguen todas el mismo patrón `EXISTS`: los `AceProfile<Level>` que aplican al usuario por tipo de usuario o cargo, en el centro del expediente (`cu.centro = self.centro`). Un solo `?`, el propio usuario:

```xml
<permission name="Expediente.porGlobal"
            object="com.educaflow.subsystem.expedientes.db.Expediente"
            condition="EXISTS (
        SELECT a FROM com.educaflow.subsystem.security.db.AceProfileGlobal a, com.educaflow.subsystem.common.db.CentroUsuario cu
        WHERE cu.usuario = ?
        AND cu.centro = self.centro
        AND a.perfil != com.educaflow.subsystem.expedientes.db.Profile.CREADOR
        AND (a.tipoUsuario IN (SELECT cut.tipoUsuario FROM com.educaflow.subsystem.common.db.CentroUsuarioTipoUsuario cut WHERE cut.centroUsuario = cu)
            OR a.cargo IN (SELECT cuc.cargo FROM com.educaflow.subsystem.common.db.CentroUsuarioCargo cuc WHERE cuc.centroUsuario = cu))
    )"
            conditionParams="__user__"
>
  <can create="false" read="true" write="false" remove="false" export="false"/>
</permission>
```

### Filtro de domain en vistas (parámetros nombrados)

En `domain` de `<action-view>` o `<panel>` se usan parámetros **nombrados** (`:nombre`), a diferencia de los permisos que usan `?`. `:__user__` puede aparecer **sin punto** (recibe el objeto `User` completo); para acceder a un atributo del usuario hay que definir un `<context>` con `eval:__user__.atributo` y referenciarlo por su nombre plano:

```xml
<action-view ...>
    <domain>self.centro.id IN (SELECT cu.centro.id FROM CentroUsuario cu JOIN cu.centroUsuarioTipoUsuario cut WHERE cu.usuario.id = :usuarioId AND cut.tipoUsuario.codigo = 'SUPERVISOR')</domain>
    <context name="usuarioId" expr="eval: __user__?.id"/>
</action-view>
```

> **No usar nunca** `:__user__.campo` (con punto) en un `<domain>`: Hibernate no admite puntos en nombres de parámetro y produce `no viable alternative at input '.'`.

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

## Los permisos de Expediente

Todos con el mismo patrón `EXISTS` de AceProfile (ver arriba), uno por nivel, siempre con `conditionParams="__user__"`; el alcance es el centro del propio expediente (`cu.centro = self.centro`), nunca los centros del usuario:

```xml
<!-- 1. El usuario creó el expediente (y es de ese centro) -->
<permission name="Expediente.creador"
            object="com.educaflow.subsystem.expedientes.db.Expediente"
            condition="self.usuarioRegistrador = ?
        AND EXISTS (
            SELECT cu FROM com.educaflow.subsystem.common.db.CentroUsuario cu
            WHERE cu.usuario = self.usuarioRegistrador
            AND cu.centro = self.centro
        )"
            conditionParams="__user__">
  <can create="false" read="true" write="false" remove="false" export="true"/>
</permission>

<!-- 2. Niveles por AceProfile: porGlobal, porTipoTramite, porTramite, porCentro,
     porTipoExpediente y porExpediente, con la misma forma que Expediente.porGlobal
     (arriba) variando el nivel de a. Ver auth-expedientes.xml. -->
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
