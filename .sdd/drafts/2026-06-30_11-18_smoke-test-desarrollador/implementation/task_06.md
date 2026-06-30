---
type: implementation-task
---

# Tarea 06 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-datainit

Esta tarea materializa la **seguridad (data-init)** del subsistema: el manifiesto de binding, la definición del permiso `SmokeTest.all` y el enlace grupo→permiso en el `auth.xml` global. El contenido XML está **dado verbatim** en este diseño: `input-config.xml` y `auth-smoketest.xml` se crean **literalmente** con el contenido de abajo; el `auth.xml` global se **modifica** según se indica. **MUST NOT** inventar otros binds ni permisos.

Filas de la tabla "Ficheros a crear o modificar":

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/smoketest/data-init/input-config.xml` | Crear | k-datainit | Manifiesto de binding del permiso del subsistema |
| `subsystem/smoketest/data-init/input/auth-smoketest.xml` | Crear | k-datainit | **Solo** la definición del permiso `SmokeTest.all` (el enlace grupo→permiso vive en el `auth.xml` global) |
| `src/main/resources/data-init/input/auth.xml` | Modificar | k-datainit | Añadir `SmokeTest.all` al grupo `admins` **y** quitar `PdfUtilities.all` del grupo `users` (ver §Notas) |

> Raíz de los ficheros del subsistema: `src/main/java/com/educaflow/subsystem/smoketest/`.

### Paso 8 — Seguridad (data-init del subsistema)

El subsistema es dueño de su permiso (k-datainit). Crear `subsystem/smoketest/data-init/` con:

`data-init/input-config.xml`:
```xml
<?xml version="1.0"?>
<xml-inputs priority="10" xmlns="http://axelor.com/xml/ns/data-import"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://axelor.com/xml/ns/data-import
  https://axelor.com/xml/ns/data-import/data-import_8.0.xsd">

    <input file="auth-smoketest.xml" root="auth">
        <bind node="permission" type="com.axelor.auth.db.Permission" search="self.name = :name" create="true" update="true">
            <bind node="@name" to="name"/>
            <bind node="@object" to="object"/>
            <bind node="can/@create" to="canCreate"/>
            <bind node="can/@read" to="canRead"/>
            <bind node="can/@write" to="canWrite"/>
            <bind node="can/@remove" to="canRemove"/>
            <bind node="can/@export" to="canExport"/>
        </bind>
    </input>
</xml-inputs>
```

`data-init/input/auth-smoketest.xml` (**solo** la definición del permiso; el enlace grupo→permiso NO va aquí — ver más abajo):
```xml
<?xml version="1.0"?>
<auth>
  <permission name="SmokeTest.all" object="com.educaflow.subsystem.smoketest.db.SmokeTest">
    <can create="true" read="true" write="true" remove="true" export="true"/>
  </permission>
</auth>
```

> **Por qué el enlace grupo→permiso NO va en el `data-init` del subsistema** (k-datainit; orden de carga por `priority` descendente): este `input-config.xml` tiene `priority="10"`, pero los grupos `admins`/`users` se crean en el `auth.xml` **global** (`src/main/resources/data-init/input/auth.xml`), cuyo manifiesto tiene `priority="-1"` y por tanto se carga el **ÚLTIMO**. Si se intentara enlazar `SmokeTest.all` al grupo `admins` aquí (en `priority=10`), el grupo `admins` **aún no existiría** y un `group`-bind con `create="false"` no encontraría nada → el enlace nunca se crearía. Por eso el enlace se hace en el `auth.xml` global (en `priority=-1`, cuando el permiso `SmokeTest.all` ya está creado por este `data-init`). Esto respeta además la convención de k-datainit: el `auth-<sistema>.xml` del subsistema define **solo** permisos; el enlace grupo→permiso vive en el `auth.xml` global.

**Enlazar `SmokeTest.all` al grupo `admins`** (Seguridad del spec: el Administrador tiene CRUD sobre `SmokeTest`): **modificar** `src/main/resources/data-init/input/auth.xml` para **añadir** la línea `<permission name="SmokeTest.all"/>` dentro del bloque `<group code="admins">`. El grupo `users` **no** recibe el permiso.

Regla de acceso (lenguaje natural): **solo el grupo `admins`** (Administrador, login `admin`) tiene CRUD completo sobre `SmokeTest`. El grupo `users` no recibe el permiso → no accede ni por menú ni por el endpoint REST genérico. No hay filtrado multicentro (el spec lo excluye: `SmokeTest` no tiene `centro`).

**Restricción de "Utilidades de PDF" al Administrador** (Seguridad del spec): además del menú `groups="admins"` (Paso 7), **modificar** `src/main/resources/data-init/input/auth.xml` para quitar `<permission name="PdfUtilities.all"/>` del bloque `<group code="users">` (el grupo `admins` lo mantiene). Ver §Notas sobre el alcance real de esta modificación.

Verificar: arrancar con `./run.sh`; `admin` accede a "Smoke test"; un usuario `users` no ve el permiso `SmokeTest.all`.

### Paso 9 — Datos iniciales

No aplica: la tabla `SmokeTest` arranca **vacía** (los propios escenarios crean y borran sus datos). No hay catálogos precargados.

## Notas relevantes (del diseño)

2. **«Administrador» = grupo `admins`.** El spec define un único usuario administrador global (login `admin`/`admin`, alcance global). Se mapea al grupo Axelor `admins` (igual que el resto de menús `groups="admins"`), no a un tipo de usuario `ADMINISTRADOR` por centro. Por eso menús, grid, form y permiso usan `groups="admins"` / grupo `admins`.
3. **Restricción de «Utilidades de PDF» al Administrador.** Mecanismo inmediato y suficiente para la UI: el menú pasa a `groups="admins"` (Paso 7) → desaparece para `users`. Para revocar también el permiso de **objeto** `PdfUtilities.all` del grupo `users` se quita esa línea del `<group code="users">` en el `auth.xml` global (Paso 8). **Caveat data-import:** el data-import de Axelor sobre una colección (`to="permissions"`) **añade/actualiza**, no **elimina**; quitar la línea surte efecto en una **BD recreada** (`./run.sh` con reset), pero en una BD ya poblada el permiso ya concedido al grupo `users` debe retirarse manualmente (o por migración). Como `PdfUtilities` no expone datos de negocio reales y el spec marca "no se modifican las pantallas", la combinación menú `groups="admins"` + ausencia del permiso en `users` (en BD limpia) cumple el requisito.
4. **`SmokeTest` sin centro.** Por mandato del spec (Fuera de alcance: sin asociación a centro ni filtrado multicentro), no se aplica el patrón multicentro de `k-secure-coding` §4: el Administrador tiene alcance global y ve todos los registros.
