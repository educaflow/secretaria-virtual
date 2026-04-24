---
name: seguridad
description: Subsistema de seguridad de EducaFlow (subsystem/security): modelo de dominio SecurityActor/AccessProfile/AccessAssignment, permisos Axelor con filtros de dominio y patrones de data-import para AccessAssignment.
---

# Subsistema de seguridad — EducaFlow

Permisos de fila estilo NTFS: cada recurso (tramite / tipoExpediente / expediente) tiene actores con perfiles de acceso, con ámbito global o por centro.

## Modelo de dominio

```
SecurityActor  (herencia JOINED)
  ├── TipoUsuario     — rol genérico: PROFESOR, ALUMNO, JEFE_ESTUDIOS…
  └── CentroUsuario   — usuario concreto en un centro (usuario + centro)
        └── via CentroUsuarioTipoUsuario → TipoUsuario

AccessProfile    — nombre del permiso: CREADOR, RESPONSABLE, REVISOR…
AccessAssignment — actor + accessProfile + centro? + recurso?
```

**Recursos en AccessAssignment** (opcionales, mutuamente excluyentes en uso):
- `tramite` — permiso sobre el proceso genérico (CREADOR va aquí)
- `tipoExpediente` — sobre una versión concreta
- `expediente` — sobre una instancia concreta

**`centro` en AccessAssignment:** `null` = global; valor = específico de ese centro.

**AccessProfile** no tiene flags booleanos. La lógica de negocio interpreta el nombre.

**TipoUsuario precargados:** `PROFESOR`, `ALUMNO`, `ADMINISTRATIVO`, `CONSERJE`, `EXPROFESOR`, `EXALUMNO`, `PROFESOR_EXTERNO`, `FAMILIAR`, `ADMINISTRADOR`, `DIRECTOR`, `JEFE_ESTUDIOS`, `SECRETARIO`, `TUTOR`, `VICEDIRECTOR`, `VICESECRETARIO`

---

## Permisos en auth.xml

Fichero: `subsystem/security/data-init/input/auth.xml`

### Reglas críticas de parámetros

- Parámetros **`?` sin índice**, nunca `?1`/`?2`. `Filter.build()` de Axelor renumera los `?` en secuencia al combinar múltiples condiciones con OR; si ya hay `?1`, lo convierte en `?11` (roto).
- **Cada `?` en el JPQL consume una posición** de `conditionParams` en orden. Si el mismo valor se usa N veces en la condición, debe repetirse N veces en `conditionParams`.
- Pasar **objetos entidad** (no IDs): `__user__` es el objeto User, `__user__.centroActivo` es el Centro.
- Las condiciones se declaran como **atributos XML** `condition`/`conditionParams` en `<permission>`, no como elementos `<domain>`/`<domain-params>`. El `input-config.xml` las mapea con `@condition` y `@conditionParams`.
- Usar `self IN (SELECT ...)`, nunca `self.fk = ?` directamente.
- La `condition` actúa también como **check de autorización individual** al serializar relaciones — si es demasiado restrictiva causa `Authorization Error` al abrir formularios.

### Patrón estándar del actor subquery

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

Asignar al rol en auth.xml: `<permission name="Tramite.creador"/>`

### input-config.xml — binding de permisos

El `input-config.xml` de seguridad debe mapear los atributos con `@`:

```xml
<input file="auth.xml" root="auth">
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
    <!-- roles, groups, users... -->
</input>
```

⚠️ Si se usa `<bind node="domain/text()" to="condition"/>` en lugar de `@condition`, las condiciones se importan como `null` en BD → todos los permisos quedan sin filtro → acceso total.

### Filtro del árbol (domain en nodo de vista)

El `domain` de una vista usa parámetros **nombrados** (`:__user__` como objeto User, se puede navegar con `.centroActivo`):

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

## Permisos para Expediente y subclases (herencia JPA)

Axelor compara `object` de un permiso por igualdad exacta de nombre de clase. Un permiso sobre `Expediente` **no se aplica automáticamente** a `JustificacionFaltaProfesorado` aunque herede de ella. Para cubrir toda la jerarquía se usa `EducaFlowAuthResolver`.

### EducaFlowAuthResolver

Fichero: `subsystem/security/EducaFlowAuthResolver.java`  
Registrado en: `subsystem/security/module/SecurityModule.java`

El resolver intercepta cualquier modelo que sea subclase de `Expediente` y devuelve los 4 permisos de `Expediente` recuperados de BD:

```java
public class EducaFlowAuthResolver implements EduFlowAuthResolver {

    private static final String PKG_EXPEDIENTE = "com.educaflow.subsystem.expedientes.db.Expediente";

    @Override
    public Optional<Set<Permission>> resolve(User user, String object, AccessType type, Long... ids) {
        Class<?> modelClass;
        try {
            modelClass = Class.forName(object, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }

        if (!Expediente.class.isAssignableFrom(modelClass)) return Optional.empty();

        // Cubre Expediente y todas sus subclases. Usar JPA.em() (raw) para evitar
        // recursión en la capa de seguridad al consultar Permission.
        var perms = JPA.em()
            .createQuery("SELECT p FROM Permission p WHERE p.object = :obj", Permission.class)
            .setParameter("obj", PKG_EXPEDIENTE)
            .getResultList();

        return Optional.of(new HashSet<>(perms));
    }
}
```

**⚠️ No hacer `return Optional.empty()` para `Expediente` mismo.** Si el resolver no lo intercepta, `authResolver` estándar puede encontrar un permiso `Expediente.all` sin condición en BD (de una importación previa) y conceder acceso total sin filtro.

### Los 4 permisos de Expediente en auth.xml

```xml
<!-- 1. El usuario creó el expediente -->
<permission name="Expediente.creador"
            object="com.educaflow.subsystem.expedientes.db.Expediente"
            condition="self.creador = ?"
            conditionParams="__user__"
>
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
            conditionParams="__user__.centroActivo, __user__, __user__.centroActivo, __user__, __user__.centroActivo"
>
  <can create="false" read="true" write="false" remove="false" export="false"/>
</permission>

<!-- 3. Hay un AccessAssignment sobre el TipoExpediente -->
<permission name="Expediente.porTipoExpediente" ...
            condition="self.tipoExpediente IN (SELECT aa.tipoExpediente ...)"
            conditionParams="__user__.centroActivo, __user__, __user__.centroActivo, __user__, __user__.centroActivo"
/>

<!-- 4. Hay un AccessAssignment sobre el Tramite (excluye CREADOR) -->
<permission name="Expediente.porTramite" ...
            condition="self.tipoExpediente.tramite IN (SELECT aa.tramite ... AND aa.accessProfile.name != 'CREADOR' ...)"
            conditionParams="__user__.centroActivo, __user__, __user__.centroActivo, __user__, __user__.centroActivo"
/>
```

Todos asignados al rol `users.all`.

---

## Data-import de AccessAssignment

XML de datos (`data-demo/input/permisos-demo.xml`):

```xml
<datos>
  <perfiles>
    <perfil name="CREADOR"/>
    <perfil name="RESPONSABLE"/>
  </perfiles>
  <asignacionesTipoUsuario>
    <!-- actor = TipoUsuario, recurso = tramite -->
    <asignacion tipoUsuarioCode="PROFESOR" perfilName="CREADOR" tramiteCode="JustificacionFaltaProfesorado"/>
  </asignacionesTipoUsuario>
  <asignacionesTipoUsuarioTipoExpediente>
    <!-- actor = TipoUsuario, recurso = tipoExpediente -->
    <asignacion tipoUsuarioCode="JEFE_ESTUDIOS" perfilName="RESPONSABLE" tipoExpedienteCode="JustificacionFaltaProfesorado"/>
  </asignacionesTipoUsuarioTipoExpediente>
  <asignacionesCentroUsuario>
    <!-- actor = CentroUsuario (usuario concreto), recurso = tramite -->
    <!-- <asignacion usuarioCode="c.guijarro@edu.gva.es" centroCode="46019660" perfilName="CREADOR" tramiteCode="..."/> -->
  </asignacionesCentroUsuario>
</datos>
```

Binding en `input-config.xml` — usar `eval` con `Query.of()` para el campo `actor` (es `SecurityActor` base JOINED; nested bind con `type` falla con `No such field 'code' in SecurityActor`):

```xml
<!-- AccessAssignment con TipoUsuario sobre Tramite -->
<input file="permisos-demo.xml" root="datos">
  <bind node="asignacionesTipoUsuario/asignacion" type="...AccessAssignment"
        search="self.actor.id IN (SELECT tu.id FROM ...TipoUsuario tu WHERE tu.code = :tipoUsuarioCode) AND self.accessProfile.name = :perfilName AND self.tramite.code = :tramiteCode"
        create="true" update="false">
    <bind node="@tipoUsuarioCode" alias="tipoUsuarioCode"/>
    <bind node="@perfilName"      alias="perfilName"/>
    <bind node="@tramiteCode"     alias="tramiteCode"/>
    <bind to="actor" eval="com.axelor.db.Query.of(...TipoUsuario.class).filter('self.code = ?1', tipoUsuarioCode).fetchOne()"/>
    <bind to="accessProfile" type="...AccessProfile" search="self.name = :perfilName" update="false" create="false"/>
    <bind to="tramite" type="...Tramite" search="self.code = :tramiteCode" update="false" create="false"/>
  </bind>
</input>

<!-- AccessAssignment con TipoUsuario sobre TipoExpediente -->
<input file="permisos-demo.xml" root="datos">
  <bind node="asignacionesTipoUsuarioTipoExpediente/asignacion" type="...AccessAssignment"
        search="self.actor.id IN (SELECT tu.id FROM ...TipoUsuario tu WHERE tu.code = :tipoUsuarioCode) AND self.accessProfile.name = :perfilName AND self.tipoExpediente.code = :tipoExpedienteCode"
        create="true" update="false">
    <bind node="@tipoUsuarioCode"    alias="tipoUsuarioCode"/>
    <bind node="@perfilName"         alias="perfilName"/>
    <bind node="@tipoExpedienteCode" alias="tipoExpedienteCode"/>
    <bind to="actor" eval="com.axelor.db.Query.of(...TipoUsuario.class).filter('self.code = ?1', tipoUsuarioCode).fetchOne()"/>
    <bind to="accessProfile" type="...AccessProfile" search="self.name = :perfilName" update="false" create="false"/>
    <bind to="tipoExpediente" type="...TipoExpediente" search="self.code = :tipoExpedienteCode" update="false" create="false"/>
  </bind>
</input>

<!-- AccessAssignment con CentroUsuario -->
<input file="permisos-demo.xml" root="datos">
  <bind node="asignacionesCentroUsuario/asignacion" type="...AccessAssignment"
        search="self.actor.id IN (SELECT cu.id FROM ...CentroUsuario cu WHERE cu.usuario.code = :usuarioCode AND cu.centro.code = :centroCode) AND self.accessProfile.name = :perfilName AND self.tramite.code = :tramiteCode"
        create="true" update="false">
    <bind node="@usuarioCode" alias="usuarioCode"/>
    <bind node="@centroCode"  alias="centroCode"/>
    <bind node="@perfilName"  alias="perfilName"/>
    <bind node="@tramiteCode" alias="tramiteCode"/>
    <bind to="actor" eval="com.axelor.db.Query.of(...CentroUsuario.class).filter('self.usuario.code = ?1 AND self.centro.code = ?2', usuarioCode, centroCode).fetchOne()"/>
    <bind to="accessProfile" type="...AccessProfile" search="self.name = :perfilName" update="false" create="false"/>
    <bind to="tramite" type="...Tramite" search="self.code = :tramiteCode" update="false" create="false"/>
  </bind>
</input>
```