---
type: implementation-task
---

# Tarea 16 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-datainit

## Ficheros que cubre esta tarea (filas de la tabla "Ficheros a crear o modificar" de `design.md`)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/correos/data-init/input-config.xml` | Crear | k-datainit | Manifiesto de datos iniciales (solo permisos, sin datos maestros) |
| `src/main/java/com/educaflow/subsystem/correos/data-init/input/auth-correos.xml` | Crear | k-datainit | Permisos de `Correo`/`Adjunto` (ver paso 10, Seguridad) |

## Texto del diseño (verbatim, `design.md`, Paso 10 — Seguridad)

El Administrador (usuario nativo Axelor con el flag de administrador — `AuthUtils.isAdmin(user)`) **no** necesita ningún `Permission` explícito: en este proyecto los usuarios administradores no pasan por el ACL de Axelor (ningún `auth-*.xml` existente del proyecto define permisos para `admins`; es el comportamiento nativo de super-usuario de Axelor). El resto de roles necesita permisos declarativos (`data-init/input/auth-correos.xml`, formato `k-datainit`) con condición JPQL — la única defensa que también protege la Vía B (`/ws/rest/<FQN>` directo), no solo la UI:

| Permission | Objeto | Condición | Quién |
|---|---|---|---|
| `Correo.propio-destinatario` | `Correo` | `self.dniDestinatario = ? and self.estado = 'SUCCESS'`, `conditionParams="__user__.dni"` | Cualquier usuario ve solo sus propios correos ya enviados con éxito |
| `Correo.propio-centro-supervisor` | `Correo` | `self.centro IN (SELECT cu.centro FROM com.educaflow.subsystem.common.db.CentroUsuario cu JOIN cu.centroUsuarioTipoUsuario cut JOIN cut.tipoUsuario tu WHERE cu.usuario = ? AND tu.codigo = 'SUPERVISOR')`, `conditionParams="__user__"` | Solo quien tiene el `TipoUsuario` `SUPERVISOR` en algún centro ve **todos** los correos de ese centro (cualquier estado) — restringe de verdad a Supervisor, no a "cualquier usuario del centro" (ver Notas y supuestos) |
| `Adjunto.propio-destinatario` | `Adjunto` | `self.correo.dniDestinatario = ? and self.correo.estado = 'SUCCESS'`, `conditionParams="__user__.dni"` | Descarga de adjuntos de los propios correos con éxito |
| `Adjunto.propio-centro-supervisor` | `Adjunto` | `self.correo.centro IN (SELECT cu.centro FROM com.educaflow.subsystem.common.db.CentroUsuario cu JOIN cu.centroUsuarioTipoUsuario cut JOIN cut.tipoUsuario tu WHERE cu.usuario = ? AND tu.codigo = 'SUPERVISOR')`, `conditionParams="__user__"` | Descarga de adjuntos de los correos del propio centro (Supervisor) |

Los 4 permisos llevan `can create="false" read="true" write="false" remove="false" export="false"` (ninguno da de alta, edita ni borra: la creación es exclusiva del Administrador, que no pasa por ACL; la escritura/borrado están bloqueados a nivel de servicio para todo el mundo — ver `validateUpdate`/`update`/`validateRemove`/`remove` de las Tareas 04/05).

`data-init/input-config.xml` (formato estándar `k-datainit`, sin datos maestros — la spec dice explícitamente que este subsistema no aporta datos iniciales propios, solo permisos):

```xml
<?xml version="1.0"?>
<xml-inputs priority="10" xmlns="http://axelor.com/xml/ns/data-import"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://axelor.com/xml/ns/data-import
  https://axelor.com/xml/ns/data-import/data-import_8.0.xsd">

    <!-- Permisos del subsistema correos -->
    <input file="auth-correos.xml" root="auth">
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

</xml-inputs>
```

**Verificar:** con el usuario `supervisor1@mislata.es`, `GET /ws/rest/com.educaflow.subsystem.correos.db.Correo/search` con un filtro vacío solo devuelve correos de «CIPFP Mislata»; con `alumno1@mislata.es` solo devuelve sus propios correos en `SUCCESS`.

## Paso 11 — Datos iniciales (verbatim, `design.md`)

Ninguno propio más allá de los permisos del paso 10 (la spec lo dice explícitamente: se apoya en los datos de demo ya existentes de otros subsistemas — centros, usuarios, DNIs).

**MUST NOT** crear ningún otro `input-config.xml`/fichero de datos maestros: este subsistema no aporta datos iniciales propios además de los 4 permisos descritos.
