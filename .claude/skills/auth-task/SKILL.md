---
name: auth-task
description: Pasos para crear o modificar permisos Axelor en EducaFlow — añadir Permission, Role, Group, o ajustar condiciones JPQL en los ficheros auth-*.xml y su input-config.xml.
---

# auth-task — Crear o modificar permisos en EducaFlow

Usa `/auth-knowledge` como referencia de reglas y `/auth-reviewer` para validar el resultado.

## 1. Decidir dónde va el permiso

| Área | Fichero |
|------|---------|
| Expedientes (TipoExpediente, Tramite, Expediente, HistorialEstado, MetaFile…) | `auth-expedientes.xml` |
| Firmas (TareaFirma, DocumentoFirma) | `auth-firmas.xml` |
| Datos maestros (Centro, User, Departamento, Cargo) | `auth-common.xml` |
| Seguridad (CentroUsuario, CentroUsuarioTipoUsuario, TipoUsuario) | `auth-security.xml` |
| Admin centro (UsuarioAutorizado, ViewGestionCentroImport, ViewGestionCentroCambioCurso) | `auth-gestioncentro.xml` |
| Roles, Grupos, Usuarios | `auth.xml` |

Todos los ficheros están en `subsystem/security/data-init/input/`.

## 2. Añadir el permiso en el fichero correspondiente

```xml
<permission name="NombreClase.descripcion"
            object="com.educaflow.subsystem.{modulo}.db.NombreClase"
            condition="..."
            conditionParams="..."
>
  <can create="false" read="true" write="false" remove="false" export="false"/>
</permission>
```

- Nombre: `NombreClase.descripcion` (PascalCase + punto + descripción breve en minúsculas).
- Si no hay condición, omitir los atributos `condition` y `conditionParams`.
- Si hay condición, usar `?` sin índice y repetir en `conditionParams` tantas veces como aparezca.

## 3. Asignar el permiso a un rol en auth.xml

```xml
<role name="users.all" description="Permisos de usuarios normales">
  ...
  <permission name="NombreClase.descripcion"/>
</role>
```

Rol `users.all` → usuarios normales (grupo `users`).
Rol `center.admin` → permisos adicionales del admin del centro (grupo `center-admins`).

## 4. Si se crea un fichero auth-*.xml nuevo

Añadir en `input-config.xml` un `<input>` para el nuevo fichero **antes** del bloque de `auth.xml`:

```xml
<input file="auth-nuevo-subsistema.xml" root="auth">
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

El `<input>` de `auth.xml` (roles/grupos) debe ir siempre al final.

## Plantillas de condición

### Permiso sin condición (lectura global de datos maestros)
```xml
<permission name="NombreClase.leer"
            object="com.educaflow.subsystem.{modulo}.db.NombreClase">
  <can create="false" read="true" write="false" remove="false" export="false"/>
</permission>
```

### Permiso filtrado por el propio usuario
```xml
<permission name="NombreClase.editar-propio"
            object="com.educaflow.subsystem.{modulo}.db.NombreClase"
            condition="self.usuario = ?"
            conditionParams="__user__">
  <can create="false" read="false" write="true" remove="false" export="false"/>
</permission>
```

### Permiso filtrado por el centro activo
```xml
<permission name="NombreClase.admin"
            object="com.educaflow.subsystem.{modulo}.db.NombreClase"
            condition="self.centro = ?"
            conditionParams="__user__.centroActivo">
  <can create="true" read="true" write="true" remove="true" export="false"/>
</permission>
```

### Cuándo separar read y write

Si el objeto aparece como referencia en formularios de otros objetos, el permiso de read **no puede llevar condición** (causaría `Authorization Error` al serializar la relación). Crear dos permisos separados:

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

Axelor aplica OR: el read lo concede el primer permiso (sin filtro), el write solo cuando pasa la condición del segundo.