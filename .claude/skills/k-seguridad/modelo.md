# Modelo de dominio del subsistema de seguridad

Permisos de fila estilo NTFS: cada recurso (tramite / tipoExpediente / expediente) tiene actores con perfiles de acceso, con ámbito global o por centro.

## Entidades

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

## EducaFlowAuthResolver — herencia JPA

Axelor compara el campo `object` de un permiso por igualdad exacta de clase. Un permiso sobre `Expediente` **no se aplica automáticamente** a `JustificacionFaltaProfesorado` aunque herede de ella.

El resolver `EducaFlowAuthResolver` intercepta cualquier subclase de `Expediente` y devuelve los permisos de `Expediente`:

- Fichero: `subsystem/security/EducaFlowAuthResolver.java`
- Registrado en: `subsystem/security/module/SecurityModule.java`

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

**⚠️ No hacer `return Optional.empty()` para `Expediente` mismo.** Si el resolver no lo intercepta, el `authResolver` estándar puede encontrar un permiso `Expediente.all` sin condición en BD y conceder acceso total sin filtro.

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
