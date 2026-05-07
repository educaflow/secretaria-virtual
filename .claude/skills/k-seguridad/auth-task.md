# Pasos para crear o modificar permisos

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
- Si hay condición, usar `?` sin índice y repetir en `conditionParams` tantas veces como aparezca en la condición.

## 3. Asignar el permiso a un rol en auth.xml

```xml
<role name="users.all" description="Permisos de usuarios normales">
  ...
  <permission name="NombreClase.descripcion"/>
</role>
```

- Rol `users.all` → usuarios normales (grupo `users`).
- Rol `center.admin` → permisos adicionales del admin del centro (grupo `center-admins`).

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
