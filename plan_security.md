# Plan — ACL declarativa de acciones, menús y entidades

## Objetivo

Declarar en XML **quién** puede ejecutar cada método de controlador, ejecutar cada acción XML con nombre, sea del tipo que sea (y por tanto ver cada menú), y operar sobre cada entidad, con un único formato y un único sitio por sistema.
Lo declarado sobre métodos y acciones se carga en **tablas**, igual que `AceProfile*`; lo declarado sobre entidades se convierte en `Permission` de Axelor cuya `condition` JPQL escribe EFBT a partir de los destinatarios (ver [Condición JPQL de los permisos de entidad](#condición-jpql-de-los-permisos-de-entidad)).
Lo que no esté declarado está **prohibido**.

## Por qué hace falta

AOP no tiene permisos por rol sobre acciones ni sobre métodos de controlador.
- `Action.execute` solo comprueba `READ` sobre el `model` de la acción (`Action.checkPermission`); un `action-method` sin `model` no comprueba nada.
- `POST /ws/action` ejecuta cualquier nombre que mande el cliente:
  - el nombre de una acción XML, aunque su botón esté oculto (`showIf`/`hidden`/`groups` no son defensa);
  - `com.x.MiController:metodo`, que `ActionGroup` convierte en un `ActionMethod` al vuelo y `ActionHandler.call` invoca **sin exigir `@CallMethod`** (solo la forma `metodo(args)` de `rpc` lo exige);
  - `form:vista:Modelo`, que construye un `action-view` al vuelo.
- `call:Ctrl:metodo` dentro de expresiones también crea un `ActionMethod` con nombre, así que pasa por el mismo sitio.
- Los controladores también pueden publicar **endpoints JAX-RS** (`@Path` en la clase y `@GET`/`@POST`… en el método), que no pasan por `Action.execute` y que Shiro deja entrar **sin sesión** si cuelgan de `/ws/public/**` (`AuthWebModule`, filtro `ANON`).
  Hoy hay dos controladores así:
  - `RegistroSalidaController` (`subsystem/registroentradasalida`), con la descarga pública de un registro de salida por su CSV (`getDescargaByCsv`, `/ws/public/registro-salida/download`, a la que lleva el QR del documento) y su validación (`validateGetDescargaByCsv`, `/validate-download`).
  - `RegistroController` (`subsystem/registrousuario`, `@Path("/public/registro")`), con la página de registro de usuarios y sus recursos (`index`, `registro.css`, `registro.js`) y sus tres endpoints de datos (`registrosPendientes` en POST, `validarCodigo` y `usuarios`).

El punto común de las acciones es `Action.execute`, que para toda acción con nombre hace:
`checkPermission → firePreEvent(nombre) → evaluate → firePostEvent(nombre, resultado) → event.getResult()`.
Un observador `@Observes PreAction` / `@Observes PostAction` **sin `@Named`** recibe los eventos de todas las acciones (`Observer.hasQualifiers`): ese es el filtro.

## Decisiones tomadas

1. Un nuevo esquema XML de seguridad, con **un bloque por tipo de recurso** (no un atributo `type`), para que el XSD valide cada tipo con su propia definición de `<ace>`.
2. Ficheros en una carpeta `security/` (normalmente `<sistema>/security/*.xml`), **con cualquier nombre**: se leen todos los que sean del nuevo esquema (por su namespace).
   Se puede repetir un bloque y mezclar tipos en un fichero, o tener un fichero por tipo; en los ejemplos van juntos por brevedad.
3. Un fichero de seguridad **solo puede referirse a recursos del paquete donde está su carpeta `security/` y de sus subpaquetes**, y EFBT lo comprueba.
   Dentro de un sistema, ese paquete es el del sistema: en `<actionMethods>` y `<entities>` el `on` empieza en la clase y EFBT completa el paquete; en `<actions>` es el nombre entero de la acción.
   Fuera de un sistema (`base/infrastructure`, `tramites/util`, `tramites/shared`…), el `on` lleva el nombre completo de la clase, que tiene que empezar por ese paquete, y la carpeta no puede tener ningún sistema por debajo (ver [Recursos que no están en ningún sistema](#recursos-que-no-están-en-ningún-sistema)).
4. **Prohibido por defecto**.
5. Lo declarado sobre métodos y acciones se guarda en **tablas** (como `AceProfile*`), no en memoria a partir del XML.
   Lo declarado sobre entidades no va a la tabla: es la `condition` del `Permission` que genera EFBT (decisión 24).
6. **Cada fila tiene un único destinatario y varias filas son O**, igual que en `AceProfile*`.
   No hay Y entre destinatarios.
7. El destinatario es un **elemento** (`<tipoUsuario codigo="…"/>`, `<cargo codigo="…"/>`, `<admin/>`, `<autenticado/>`, `<todos/>`…), no un atributo de `<usuario>`, para que el XSD impida escribir dos en la misma fila.
   `<autenticado/>` es cualquiera con sesión; `<todos/>` es cualquiera **sin exigir sesión**, para los endpoints públicos (`/ws/public/**`) como la descarga de un registro de salida por su CSV.
   **`AceProfile*` migra a la misma forma.**
8. Los destinatarios son siempre primitivos (tipo de usuario, cargo, tramitador de una unidad…); un **colectivo** es solo un nombre para una lista de ellos, que **EFBT expande al compilar** como hace con los comodines (ver [Colectivos](#colectivos)).
   En la tabla y en tiempo de ejecución no existe el concepto: solo destinatarios primitivos.
9. Sin DNI ni usuarios concretos en el XML: las concesiones a personas son de tiempo de ejecución (tabla + pantalla, como `AceProfileCentro`) y quedan fuera de este plan.
10. El bloque `<entities>` es la **única fuente** de los permisos de entidad: EFBT genera a partir de él los `Permission` de Axelor.
11. Comodines «el más específico gana», **expandidos por EFBT al compilar**: en la tabla solo hay recursos concretos.
12. Modo transitorio `warn`/`enforce`, que se quitará cuando todo esté migrado.
13. Los menús se ven si se tiene permiso `canExecute` sobre la acción del menú.
14. **Toda acción XML con nombre es el mismo tipo de recurso**, sin distinguir `action-view`, `action-record`, `action-attrs`, `action-group`, `action-script`, `action-export`, `action-report`…: todas pasan por `Action.execute` (las de dentro de un `action-group` también, una a una, porque `ActionGroup.evaluate` llama a `action.wrap(handler)` por cada una), así que un solo bloque `<actions>` y un solo observador las cubren.
    La única excepción es `action-method`, que no se declara en `<actions>`: su permiso es el del método de controlador al que llama (`<actionMethods>`).
    No es solo por no declarar dos veces lo mismo: asegurar el nombre de la acción (`subsysExpedientes-Remote-addNote-action`) **no protegería nada**, porque `POST /ws/action` acepta `…ExpedienteController:addNote` sin pasar por ese nombre.
    El método es lo único que no se puede rodear, y de paso varios nombres de acción sobre el mismo método se aseguran una sola vez.
15. Fuera de un sistema, las carpetas `security/` **no pueden anidarse**: una no puede estar dentro del alcance de otra.
16. **Al implementar, todo lo que se pueda va en `subsystem/security`**: el dominio `AceRecurso` y su repositorio, `AclService`, `DestinatarioEvaluator`, `AclActionObserver`, `AclRestFilter`, `MenuSecurityServiceImpl`, el fichero `colectivos.xml` y los tests unitarios.
    Fuera de él solo lo que no puede estar dentro: el XSD y la generación (EFBT), los `security/*.xml` de cada sistema (que declaran sus propios recursos) y el `if` que EFBT inyecta en los menús.
    Nada de esto va a `base/infrastructure`, a `tramitador` ni a `common`.
17. **Denegar es lo más simple posible**: una excepción en `PreAction`, igual para todos los tipos de acción, sin vista «sin permiso».
    Quien llega a una acción sin permiso es alguien que ha saltado la interfaz (el botón o el menú no se le muestran), así que no hace falta una pantalla amable.
18. **El código de los menús no se toca hasta que todo lo demás esté migrado** (fase 12): mientras dure la migración los menús siguen con el `switch` y los `groups` de hoy, sin modo `warn`.
19. **El trámite `prueba` se va a borrar**: no se migra ni se tiene en cuenta (con él desaparece `FormacionCentroTrabajo`).
20. **Los ficheros `security/*.xml` los escribe Claude**, sistema a sistema, y el usuario los revisa (ver [Ficheros de seguridad a crear](#ficheros-de-seguridad-a-crear)).
21. **Un `<ace>` de `<entities>` admite `condition` y `conditionParams`** (opcionales), con la misma sintaxis que el `Permission` de Axelor (JPQL con `?` posicionales y `self`), para los permisos «sobre el propio registro» (el firmante de una tarea, el destinatario de un correo, el interesado de un expediente, los centros supervisados).
    EFBT la ANDea con la condición de destinatarios y centro al generar el `Permission` (ver [Condición JPQL de los permisos de entidad](#condición-jpql-de-los-permisos-de-entidad)).
    Así los `auth-*.xml` desaparecen del todo y la decisión 10 se mantiene.
22. **Las subclases de `Expediente` heredan por copia los `<ace>` de `Expediente`**: el generador por tipo de expediente (`createdatainittipoexpediente`) copia los `<ace>` de `<entities>` de `Expediente` (los de `subsystem/expedientes/security/acl.xml`, condiciones incluidas) cambiando el `on` por la subclase y quedándose solo con `canRead` (los que quedan idénticos se fusionan; ver §2, punto 9).
    Es la única forma de que un trámite nuevo nazca con los mismos permisos de lectura que el resto sin escribir nada, y sustituye al parche de `EducaFlowAuthResolverImpl`, que se elimina (ver fase 9).
23. **El `auth.xml` global (`src/main/resources/data-init/input/auth.xml`) también se sustituye.**
    Es el fichero que hoy enlaza los `Permission` de todos los sistemas al grupo `users` y el que concede sin condición `MetaFile.all`, `DMSFile.all`, `User.all`, `Persona.all`, `CentroUsuario.all`, `RegistroEntrada.all`, `RegistroSalida.all`… a todos los usuarios (ver [Flecos resueltos](#flecos-resueltos-en-la-revisión), punto 3).
    Tras la migración solo conserva lo que no es un permiso: los grupos `admins`/`users` y los usuarios; el `<bind node="group">` de cada `Permission` lo genera EFBT.
    Las entidades de AOP que la aplicación usa (`MetaFile`, `DMSFile`, `User`) se declaran en `base/infrastructure/security/acl.xml` con FQN (ver [Recursos que no están en ningún sistema](#recursos-que-no-están-en-ningún-sistema)).
24. **La `condition` de cada `Permission` la escribe EFBT a partir de los destinatarios del `<ace>`, no consulta `AceRecurso`.**
    Cada destinatario admitido en `<entities>` tiene definido su fragmento JPQL (ver [Condición JPQL de los permisos de entidad](#condición-jpql-de-los-permisos-de-entidad)), todos anclados a **un único `?`** que recibe `__user__`.
    Por eso la tabla `AceRecurso` solo guarda filas de métodos y acciones.
25. **Filtro por centro de oficio.**
    Si la entidad tiene una propiedad de tipo `Centro`, la `condition` exige que el usuario pertenezca al centro del registro (y su tipo de usuario o cargo se evalúa **en ese centro**, no «en algún centro»).
    Si tiene más de una, el `<ace>` **MUST** decir cuál con `propiedadCentro="…"`.
    El administrador no se filtra: AOP se salta los `Permission` del admin, así que el «si es admin OR (filtro por centro)» se cumple sin escribirlo.
    Es un **suelo**, no el permiso: ser del centro no da acceso a todo lo del centro (un alumno no ve cualquier expediente de su centro, ni un usuario cualquier notificación de su centro); lo que relaciona al usuario con el registro concreto sigue en la `condition` propia del `<ace>` (decisión 21) y el IDOR sigue en [Pendiente](#pendiente-fuera-de-este-plan).
26. **Un `Permission` por `<ace>` ganador y entidad, con nombre `acl.<Entidad>.<flags>[.<n>]`** (ver [Nombre de cada `Permission`](#nombre-de-cada-permission)).

## El formato

Como cada fichero solo puede referirse a su propio alcance, el ejemplo son cuatro ficheros de cuatro sistemas.
Los nombres de métodos y de acciones son reales (en los ejemplos son `action-view`, pero en `<actions>` iría igual cualquier otro tipo de acción); los destinatarios son ilustrativos.

`subsystem/criptografia/security/acl.xml` — tipo de usuario, cargo y admin; un `*` por defecto y excepciones más específicas:

```xml
<security xmlns="http://educaflow.com/xml/ns/security">

  <actionMethods>
    <ace on="*">
      <admin/>
    </ace>
    <ace on="CertificadoDigitalController.getSituacionFirma" canExecute="true">
      <admin/>
      <tipoUsuario codigo="SUPERVISOR"/>
      <cargo codigo="SECRETARIO"/>
      <cargo codigo="DIRECTOR"/>
    </ace>
  </actionMethods>

  <actions>
    <ace on="*">
      <admin/>
    </ace>
    <ace on="subsysCriptografia.Main@CertificadoDigital-action" canExecute="true">
      <admin/>
      <cargo codigo="SECRETARIO"/>
    </ace>
  </actions>

  <entities>
    <ace on="*" canRead="true" canWrite="true" canCreate="true" canRemove="true">
      <admin/>
    </ace>
  </entities>

</security>
```

`system/ventanilla/security/acl.xml` — tramitador de cualquier unidad o de una unidad concreta:

```xml
<security xmlns="http://educaflow.com/xml/ns/security">

  <actionMethods>
    <ace on="BandejaController.abrirExpediente" canExecute="true">
      <tramitador/>
    </ace>
  </actionMethods>

  <actions>
    <!-- tramita algún trámite, de cualquier unidad tramitadora -->
    <ace on="sysVentanilla.PendientesDeMi@Expediente-action" canExecute="true">
      <admin/>
      <tramitador/>
    </ace>
    <!-- tramita algún trámite de Secretaría -->
    <ace on="sysVentanilla.SecretariaAbiertos@Expediente-action">
      <unidadTramitadora codigo="SECRETARIA"/>
    </ace>
    <!-- tramita algún trámite de Jefatura de estudios -->
    <ace on="sysVentanilla.JefaturaAbiertos@Expediente-action">
      <unidadTramitadora codigo="JEFATURA_ESTUDIOS"/>
    </ace>
  </actions>

</security>
```

`subsystem/notificaciones/security/acl.xml` — admin, cualquier autenticado, tipo de usuario, cargo y un colectivo; `canExecute` escrito y omitido:

```xml
<security xmlns="http://educaflow.com/xml/ns/security">

  <actionMethods>
    <!-- por defecto, todo el controlador solo para el administrador -->
    <ace on="NotificacionController.*">
      <admin/>
    </ace>
    <!-- canExecute="true" es el único valor posible: escribirlo u omitirlo es lo mismo -->
    <ace on="NotificacionController.abrirRecibida" canExecute="true">
      <autenticado/>
    </ace>
    <ace on="NotificacionController.reenviar">
      <admin/>
      <tipoUsuario codigo="SUPERVISOR"/>
      <cargo codigo="SECRETARIO"/>
      <cargo codigo="DIRECTOR"/>
    </ace>
  </actionMethods>

  <actions>
    <!-- también aquí canExecute="true" es el único valor posible -->
    <ace on="subsysNotificaciones.Mis@Notificacion-action" canExecute="true">
      <autenticado/>
    </ace>
    <ace on="subsysNotificaciones.Main@Notificacion-action">
      <admin/>
    </ace>
    <!-- la regla de hoy de GestorNotificacionesUtil.esGestorEnAlgunCentro, escrita con el colectivo GESTOR -->
    <ace on="subsysNotificaciones.Centro@Notificacion-action">
      <colectivo nombre="GESTOR"/>
    </ace>
  </actions>

  <entities>
    <!-- en entities los flags que falten son false, y al menos uno tiene que ser true -->
    <ace on="*" canRead="true">
      <autenticado/>
    </ace>
    <ace on="Notificacion" canRead="true" canWrite="true" canCreate="true" canRemove="true">
      <tipoUsuario codigo="SUPERVISOR"/>
      <cargo codigo="SECRETARIO"/>
    </ace>
  </entities>

</security>
```

`subsystem/registroentradasalida/security/acl.xml` — tramitador de una unidad tramitadora (la regla del menú «Registro» de hoy: admin **o** tramitador de Secretaría **o** supervisor) y un endpoint público para todos:

```xml
<security xmlns="http://educaflow.com/xml/ns/security">

  <actionMethods>
    <ace on="RegistroSalidaController.*" canExecute="true">
      <admin/>
      <unidadTramitadora codigo="SECRETARIA"/>
      <tipoUsuario codigo="SUPERVISOR"/>
    </ace>
    <!-- la descarga por CSV es pública: quien tiene el CSV (el QR del documento) tiene el documento -->
    <ace on="RegistroSalidaController.getDescargaByCsv">
      <todos/>
    </ace>
    <ace on="RegistroSalidaController.validateGetDescargaByCsv">
      <todos/>
    </ace>
  </actionMethods>

  <actions>
    <ace on="*" canExecute="true">
      <admin/>
      <unidadTramitadora codigo="SECRETARIA"/>
      <tipoUsuario codigo="SUPERVISOR"/>
    </ace>
  </actions>

  <entities>
    <!-- aquí no se puede usar <unidadTramitadora>: es un destinatario derivado -->
    <ace on="*" canRead="true">
      <tipoUsuario codigo="SUPERVISOR"/>
      <cargo codigo="SECRETARIO"/>
    </ace>
  </entities>

</security>
```

`<tramitador/>` (tramita algún trámite de cualquier unidad) se escribe igual que `<unidadTramitadora>`, sin `codigo`.
Los dos son destinatarios **derivados**: se calculan en Java, así que solo se admiten en `<actionMethods>` y `<actions>`, nunca en `<entities>`.

### Bloques y flags

Los permisos de cada `<ace>` se escriben como en el `Permission` de Axelor: un atributo booleano por permiso (`canRead="true"`, `canWrite="true"`…), no una lista.
A los flags de `Permission` se suma uno nuevo, `canExecute`, que es el único de los dos bloques que no son de entidades.

| Bloque            | `on`                                                               | Flags que admite                                                                                     | Si no se escribe ninguno                          |
|-------------------|--------------------------------------------------------------------|------------------------------------------------------------------------------------------------------|---------------------------------------------------|
| `<actionMethods>` | `Controlador.metodo`, relativo al paquete `controller` del sistema (métodos de acción y endpoints JAX-RS) | solo `canExecute`                                                                                    | **EFBT pone `canExecute="true"`**                 |
| `<actions>`       | nombre entero de cualquier acción XML con nombre (`action-view`, `action-record`, `action-attrs`, `action-group`, `action-script`, `action-export`, `action-report`…) declarada en el mismo sistema; **no** `action-method` | solo `canExecute`                                                                                    | **EFBT pone `canExecute="true"`**                 |
| `<entities>`      | nombre de la entidad, relativo al paquete `db` del sistema         | `canRead`, `canWrite`, `canCreate`, `canRemove`, `canExport`, `canImport` (los de `Permission` de Axelor) | error: al menos uno tiene que ser `true`          |

- Solo el `<ace>` de `<entities>` admite además los atributos opcionales **`condition`** y **`conditionParams`** (decisión 21), con la sintaxis exacta del `Permission` de Axelor: JPQL sobre `self`, con `?` posicionales que se enlazan por orden a `conditionParams` (lista separada por comas de `__user__` o expresiones Groovy sobre `__user__`).
  Ejemplo, la tarea de firma del propio firmante:
  ```xml
  <ace on="TareaFirma" canRead="true" canWrite="true" condition="self.firmante = ?" conditionParams="__user__">
    <autenticado/>
  </ace>
  ```
  Una `condition` sin `conditionParams` es válida (`self.estado = 'ENVIADO'`); `conditionParams` sin `condition` es error del XSD.
  La condición se aplica a **todos** los flags de ese `<ace>` y a todos sus destinatarios.
- En `<entities>` cada flag vale `true` o `false` y **el que no se escribe es `false`**, igual que en `Permission`.
  Escribir `canRemove="false"` es lo mismo que omitirlo; se admite para que quien venga de un `auth-*.xml` pueda escribir los seis a la vista.
  Un `<ace>` sin ningún flag a `true` no concede nada y es error de EFBT (el XSD no lo puede expresar).
- En `<entities>` un mismo `on` **puede repetirse** en varios `<ace>`, con distintos flags, distintos destinatarios o distinta `condition`, y Axelor une con OR los `Permission` resultantes (es la única excepción a la regla del `on` duplicado).
  Solo es error que dos `<ace>` del mismo `on` tengan los mismos flags a `true` **y** la misma `condition` y `conditionParams`: son el mismo permiso partido en dos y EFBT pide fusionar sus destinatarios en un solo `<ace>`.
  Ejemplo válido, sin ninguna condición propia:
  ```xml
  <ace on="Notificacion" canRead="true">
    <tipoUsuario codigo="PROFESOR"/>
    <tipoUsuario codigo="SUPERVISOR"/>
  </ace>
  <ace on="Notificacion" canWrite="true" canCreate="true" canRemove="true">
    <tipoUsuario codigo="SUPERVISOR"/>
  </ace>
  ```
  Y también con una condición distinta por destinatario: `<ace on="Notificacion" canRead="true" condition="A">` para PROFESOR y otro `<ace on="Notificacion" canRead="true" condition="B">` para SUPERVISOR dan, para `canRead`, «(PROFESOR del centro y A) o (SUPERVISOR del centro y B)».
- **Cada flag se evalúa por separado**, solo con los `<ace>` que lo tienen a `true` (`AuthResolver.hasAccess`).
  En el ejemplo anterior con condiciones, el SUPERVISOR puede escribir cualquier notificación de su centro aunque solo lea las que cumplen B, y por REST podría modificar por id filas que no ve en el grid.
  EFBT no puede comprobar que una condición JPQL esté contenida en otra, así que no es error: si `canWrite` tiene que estar acotado como `canRead`, se repite la condición en el `<ace>` de `canWrite`.
  En el `<ace>` solo se escriben los `?` y los `conditionParams` **propios**: la parte que mira destinatarios y centro la añade EFBT delante (ver [Condición JPQL de los permisos de entidad](#condición-jpql-de-los-permisos-de-entidad)).
- Solo el `<ace>` de `<entities>` admite el atributo opcional **`propiedadCentro`** (decisión 25): el nombre de la propiedad `many-to-one` a `Centro` por la que se filtra el registro.
  Es **obligatorio** si la entidad tiene más de una, y **error** si tiene una o ninguna (no hay nada que elegir) o si se escribe en un `on` con comodín.
- En `<actionMethods>` y `<actions>` el único flag es `canExecute`, así que si falta **EFBT lo pone a `true`** y genera las mismas filas que si estuviera escrito.
  El único valor que admite el XSD es `true`: para que alguien no ejecute un método o una acción no se le declara, no se escribe `false`.
- Que el flag exista aunque sea opcional permite que más adelante un bloque tenga más de un permiso sin cambiar los ficheros ya escritos.
- El XSD (1.0) da a cada bloque su propio tipo de `<ace>`, así que valida él solo qué flags admite cada bloque.
- `<actions>` no distingue el tipo de la acción porque todas se ejecutan por el mismo sitio y se bloquean igual: el flag es `canExecute` para todas, también para un `action-view`.
  Un `action-method` declarado ahí es error de EFBT (se declara su método en `<actionMethods>`).

### Destinatarios

Cada hijo de un `<ace>` es **un destinatario y una fila**; varios hijos son O.

| Elemento                          | Quién                                                                                                                              | En qué bloques                       |
|-----------------------------------|------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------|
| `<admin/>`                        | el administrador (`SecurityUtil.isAdmin`)                                                                                          | todos                                |
| `<autenticado/>`                  | cualquier usuario autenticado                                                                                                      | todos                                |
| `<todos/>`                        | cualquiera, **con sesión o sin ella** (p. ej. quien llega desde el QR de un documento)                                             | solo `actionMethods`, y solo en endpoints JAX-RS bajo `/ws/public/**` |
| `<tipoUsuario codigo="…"/>`       | quien tenga ese tipo de usuario en algún centro                                                                                    | todos                                |
| `<cargo codigo="…"/>`             | quien tenga ese cargo en algún centro                                                                                              | todos                                |
| `<tramitador/>`                   | quien tramita algún trámite, de cualquier unidad (`PerfilesUsuarioService.isTramitador(user)`)                                     | solo `actionMethods` y `actions` |
| `<unidadTramitadora codigo="…"/>` | quien tramita algún trámite de esa unidad (`PerfilesUsuarioService.isTramitador(user, codigo)`; `SECRETARIA`, `JEFATURA_ESTUDIOS`) | solo `actionMethods` y `actions` |
| `<colectivo nombre="…"/>`         | **no es un destinatario**: EFBT lo sustituye al compilar por los destinatarios primitivos del colectivo (ver [Colectivos](#colectivos))      | donde quepan todos sus destinatarios |

El gestor de notificaciones (`GestorNotificacionesUtil.esGestorEnAlgunCentro`) **no es un destinatario derivado**: la regla de hoy ya es por tipo de usuario (`SUPERVISOR`, `ADMINISTRATIVO`) o por cargo (`DIRECTOR`, `JEFE_ESTUDIOS`, `SECRETARIO`), así que se escribe con el colectivo `GESTOR`.
Lo que queda de `GestorNotificacionesUtil` (`idsCentrosGestionados`, en **qué centros** gestiona, que usan las `condition` de entidad de notificaciones) lo cubre el filtro por centro de oficio (decisión 25): `Notificacion` tiene propiedad `centro`, así que `<ace on="Notificacion" canRead="true"><colectivo nombre="GESTOR"/></ace>` ya significa «gestor **en el centro de la notificación**», que es lo que hoy calcula `idsCentrosGestionados`.
La clase desaparece entera en la fase 9, con el `auth-notificaciones.xml`.
Ese `<ace>` es solo el de los **gestores**; el usuario normal no ve las notificaciones de su centro, sino **las suyas**, con su propio `<ace>` y su `condition` (`self.correo.dniDestinatario = ?` con `__user__.dni`, como hoy `Notificacion.propio`): el centro es un suelo, no el permiso (decisión 25).

- Un `<ace>` sin hijos es un error del XSD: «todos» se escribe explícitamente con `<todos/>`.
- **`<todos/>` frente a `<autenticado/>`**: `<autenticado/>` exige sesión; `<todos/>` no.
  Solo tiene sentido donde de verdad puede llegar alguien sin sesión: un endpoint JAX-RS cuya ruta cuelgue de `/ws/public/**`, que es lo único que Shiro deja pasar sin login.
  En cualquier otro recurso (métodos de acción de `/ws/action`, acciones XML, entidades) Axelor ya ha exigido sesión antes, así que `<todos/>` sería un `<autenticado/>` disfrazado que miente sobre quién entra: EFBT **aborta** si se usa ahí.
  Y al revés, un endpoint bajo `/ws/public/**` es público por construcción: EFBT **aborta** si su `<ace>` tiene cualquier destinatario que no sea `<todos/>`, porque declarar `<admin/>` en él no lo protegería.
- **Por qué solo O**: con un destinatario por fila, la consulta es una igualdad por columna unida con OR; con Y habría que guardar columnas con nulos («nulo = cualquiera»), exigir que todas las no nulas se cumplan en el **mismo** `CentroUsuario`, y una fila con todo nulo daría acceso a todos.
  Ninguna regla actual necesita Y; si algún día hace falta, será un concepto aparte.
- **Destinatarios derivados**: lo que se calcula en Java (p. ej. `isTramitador`, que recorre las seis tablas `AceProfile*`) no se puede llevar razonablemente a una `condition` JPQL.
  Por eso solo se admiten en los bloques que se evalúan en Java (`actionMethods`, `actions`); en `entities` el XSD no los acepta.
- **Multicentro**: en acciones y menús, `tipoUsuario`/`cargo` se evalúan «en algún centro del usuario», igual que `MenuSecurityServiceImpl` hoy.
  En entidades se evalúan **en el centro del registro** si la entidad tiene propiedad `Centro` (decisión 25), y «en algún centro» si no la tiene.

### Condición JPQL de los permisos de entidad

Cada `<ace>` de `<entities>` acaba en un `Permission` de Axelor, y su `condition` la escribe EFBT a partir de los destinatarios del `<ace>`, de la propiedad `Centro` de la entidad y de la `condition` propia del `<ace>` (decisiones 24 y 25).
Aquí está definida pieza a pieza para no tener que pensarla al implementar.

**El ancla.**
Toda la parte generada cuelga de **una sola subconsulta** sobre `CentroUsuario`, que es la tabla que une usuario, centro, tipos de usuario y cargos (`subsystem/common`: `CentroUsuario.usuario` es el `User` de Axelor y `CentroUsuario.centro` el `Centro`; de ella cuelgan `CentroUsuarioTipoUsuario.tipoUsuario` y `CentroUsuarioCargo.cargo`):

```
EXISTS (SELECT cu FROM com.educaflow.subsystem.common.db.CentroUsuario cu
        WHERE cu.usuario = ?
          AND cu.centro = self.<propiedadCentro>        ← solo si la entidad tiene propiedad Centro
          AND (<fragmento 1> OR <fragmento 2> OR …))    ← solo si hay <tipoUsuario> o <cargo>
```

- Tiene **exactamente un `?`**, y es el que recibe `__user__`.
  Así el `conditionParams` generado empieza siempre por `__user__`, una sola vez, y el resto de destinatarios no añaden parámetros: sus códigos van como literales en el JPQL.
- Como el centro y el tipo/cargo se comprueban sobre el **mismo** `cu`, «supervisor» significa «supervisor **del centro de este registro**».
  Eso cierra la parte **multicentro** del IDOR (la secretaria del centro A ya no pasa con un registro del centro B), pero **no el IDOR entero**: ser del centro no da derecho a ver todo lo del centro.
  Un alumno no puede ver cualquier expediente de su centro, ni un usuario cualquier notificación de su centro: la relación del usuario con **el registro concreto** (interesado, destinatario, firmante…) sigue escribiéndose en la `condition` propia del `<ace>` (decisión 21), y queda anotada en [Pendiente](#pendiente-fuera-de-este-plan).
- Sin propiedad `Centro` la condición es «en algún centro del usuario», como en acciones.

**Fragmento de cada destinatario** (dentro del paréntesis del ancla; los `<colectivo>` ya están expandidos):

| Destinatario | Fragmento | Notas |
|---|---|---|
| `<admin/>` | **ninguno** | AOP se salta los `Permission` del administrador: `AuthSecurity.getUser()` devuelve `null` si `isAdmin` (`AuthSecurity.java:75-79`), y con usuario nulo `isPermitted` es `true` y `getFilter` es `null`. Es el «si es admin OR (filtro por centro)» de la decisión 25, sin escribirlo. Un `<ace>` que solo tenga `<admin/>` **no genera `Permission`** (para `users` queda prohibido, y el admin entra igual). |
| `<autenticado/>` | **ninguno** | Basta el ancla: con propiedad `Centro` significa «cualquier usuario del centro del registro»; sin ella y sin `condition` propia, el `Permission` se genera **sin `condition`** (concede sin más, y entonces `conditionParams` va vacío: es el único caso en que `__user__` no aparece). Si además hay `<tipoUsuario>`/`<cargo>` en el mismo `<ace>`, sobran: EFBT omite el paréntesis entero. |
| `<tipoUsuario codigo="X"/>` | `EXISTS (SELECT cut FROM com.educaflow.subsystem.common.db.CentroUsuarioTipoUsuario cut WHERE cut.centroUsuario = cu AND cut.tipoUsuario.codigo IN ('X', …))` | Todos los `<tipoUsuario>` del `<ace>` van en un solo `IN`. `TipoUsuario.codigo` es el enum `TipoUsuarioCodigo`; se compara con el literal, como ya hacen `auth-firmas.xml:24` y `gestion-centro-usuario.xml:15`. |
| `<cargo codigo="X"/>` | `EXISTS (SELECT cuc FROM com.educaflow.subsystem.common.db.CentroUsuarioCargo cuc WHERE cuc.centroUsuario = cu AND cuc.cargo.code IN ('X', …))` | Ojo: la propiedad de `Cargo` se llama **`code`**, no `codigo` (`Cargo.xml:9`). |
| `<todos/>`, `<tramitador/>`, `<unidadTramitadora>` | — | No se admiten en `<entities>` (XSD); un colectivo que los contenga tampoco. |

**La `condition` propia del `<ace>`** (decisión 21) se ANDea detrás del ancla:
`(<ancla>) AND (<condition del ace>)`, con `conditionParams="__user__,<conditionParams del ace>"`.

- El autor del `<ace>` escribe **solo sus `?` y sus `conditionParams`**; el `__user__` del ancla lo pone EFBT y va siempre el primero, así que no hay que contar posiciones.
- **No se puede compartir el `__user__` del ancla con un `?` del `<ace>`**: AOP enlaza por posición y cada `?` consume una entrada de `conditionParams` (`AuthSecurity.Condition`, `AuthSecurity.java:33-48`, parte la lista por comas y la pasa a `JPQLFilter`; `Filter.build`, `Filter.java:27-30`, renumera **todos** los `?` del texto en orden, así que ni siquiera se puede escribir `?1`: se convertiría en `?11`).
  Por tanto, si la `condition` del `<ace>` necesita al usuario, lleva su propio `?` y su propio `__user__` en los `conditionParams` del `<ace>`, y en el `Permission` generado `__user__` aparece una vez por cada `?` que lo use.
  Es lo único que AOP admite; lo que sí se garantiza es que el ancla no gasta más que uno.

**Ejemplo completo**, `TareaFirma` (tiene propiedad `centro`):

```xml
<ace on="TareaFirma" canRead="true" canWrite="true" condition="self.firmante = ?" conditionParams="__user__">
  <admin/>
  <tipoUsuario codigo="SUPERVISOR"/>
  <cargo codigo="SECRETARIO"/>
</ace>
```

genera un único `Permission` `acl.TareaFirma.read-write` sobre `com.educaflow.subsystem.firmas.db.TareaFirma`, con `canRead`/`canWrite`, `conditionParams="__user__,__user__"` y:

```
(EXISTS (SELECT cu FROM com.educaflow.subsystem.common.db.CentroUsuario cu
         WHERE cu.usuario = ? AND cu.centro = self.centro
           AND (EXISTS (SELECT cut FROM com.educaflow.subsystem.common.db.CentroUsuarioTipoUsuario cut
                        WHERE cut.centroUsuario = cu AND cut.tipoUsuario.codigo IN ('SUPERVISOR'))
                OR EXISTS (SELECT cuc FROM com.educaflow.subsystem.common.db.CentroUsuarioCargo cuc
                           WHERE cuc.centroUsuario = cu AND cuc.cargo.code IN ('SECRETARIO')))))
AND (self.firmante = ?)
```

(`<admin/>` no deja rastro.)
Cabe de sobra en los 1024 caracteres de `Permission.condition` (`Permission.xml:21`): el tamaño solo crece con los literales del `IN`.

**La propiedad `Centro`** (decisión 25):
- Cuenta como «propiedad de tipo `Centro`» un `many-to-one` a `com.educaflow.subsystem.common.db.Centro` declarado en la entidad (en las subclases de `Expediente`, el `centro` heredado).
  Las colecciones no cuentan: `AsistenteNuevoExpediente.centrosDisponibles` (`many-to-many`) no es «el centro de la fila».
- Con **una**, EFBT la usa de oficio y `propiedadCentro` es error (no hay nada que elegir).
  Con **más de una**, `propiedadCentro` es obligatorio y EFBT aborta si falta, si nombra una propiedad que no existe o que no es un `many-to-one` a `Centro`.
  Solo se admite en un `on` exacto: si un `<ace>` con comodín gana una entidad con dos propiedades `Centro`, EFBT aborta pidiendo un `<ace>` propio para esa entidad.
- Inventario de hoy: `Expediente` (y sus subclases), `TareaFirma`, `Notificacion`, `RegistroEntrada`, `RegistroSalida`, `CentroUsuario`, `AceProfileCentro`, `UsuarioAutorizado`, `TareaImportacion` y `AsistenteNuevoExpediente` tienen **una**; ninguna tiene dos, así que `propiedadCentro` queda definido pero sin uso por ahora.
- No hay forma de desactivar el filtro en una entidad que tenga `Centro`; si aparece un caso, se decidirá entonces.
- El filtro protege `read`, `write` y `remove` de filas que **ya existen**: Axelor comprueba la condición sobre la fila guardada y con `create` no hay fila (`isPermitted` sin `ids` → `true`).
  Que una fila nueva nazca en un centro del usuario, o que un `write` no la mueva a otro, sigue siendo responsabilidad del servicio (`k-secure-coding`: asignación incondicional de `centro` en `insert`/`update`).

**Prohibido mezclar, para una entidad y un flag, un `Permission` sin `condition` y otros con `condition`.**
- AOP no los trata igual al pedir una fila que al buscar filas (`../axelor-open-platform/preguntas/seguridad.md`, primera pregunta):
  - `isPermitted` con id: el incondicional gana y concede cualquier fila;
  - `getFilter` sin id (el grid, `/ws/rest/<FQN>/search`): el incondicional **se pierde** y el filtro queda en el OR de los condicionales.
  Resultado: el grid muestra menos filas de las que se pueden abrir por id.
- Solo puede pasar en una entidad **sin** propiedad `Centro` (con ella el ancla siempre pone condición), cuando un `<ace>` de `<autenticado/>` sin `condition` propia comparte flag con otro `<ace>` condicional.
- EFBT **aborta** nombrando los `<ace>` implicados: para ese flag el condicional sobra, porque el incondicional ya concede todo.

### Nombre de cada `Permission`

- **Un `Permission` por `<ace>` ganador y entidad**, no uno por permiso: `Permission` de Axelor lleva los seis flags (`canRead`, `canWrite`, `canCreate`, `canRemove`, `canExport`, `canImport`) y **una** `condition`, así que `canRead="true" canWrite="true"` es un `Permission` con esos dos flags.
  `AuthResolver.filterPermissions` ya filtra por flag (`hasAccess(permission, type)`), así que un `Permission` solo `read` no vale para `write`.
- `name` = **`acl.<Entidad>.<flags>[.<n>]`**:
  - `<Entidad>` es el nombre simple (`TareaFirma`, `AnulacionMatriculaCicloFormativoV1`, `User`), que es único porque JPA exige nombres de entidad únicos en la unidad de persistencia.
  - `<flags>` son los flags del `<ace>` que están a `true`, sin el prefijo `can` y en minúsculas, en orden canónico `read-write-create-remove-export-import`, unidos por `-`, estén en el orden que estén escritos en el XML (`canWrite="true" canRead="true"` → `read-write`).
  - `.<n>` (desde `2`) solo cuando dos `<ace>` ganadores de la misma entidad tienen los mismos `<flags>` (y por tanto distinta `condition`), numerados por orden de fichero (alfabético) y de documento.
    El número depende de ese orden: reordenar los `<ace>` renombra los `Permission`, y es inocuo porque `auth_permission` se vacía en cada arranque (ver [Carga](#carga)).
- Ejemplos: `acl.TareaFirma.read-write`, `acl.Expediente.read`, `acl.Expediente.read.2`, `acl.MetaFile.create`, `acl.User.read-write`.
- Ejemplo con varios `<ace>` sobre `Notificacion`: uno `canRead` con `condition` A para PROFESOR, otro `canRead` con `condition` B para SUPERVISOR y otro `canWrite`/`canCreate`/`canRemove` sin `condition` para SUPERVISOR generan **tres** `Permission`: `acl.Notificacion.read`, `acl.Notificacion.read.2` y `acl.Notificacion.write-create-remove`.
  Sin el segundo serían dos: `acl.Notificacion.read` y `acl.Notificacion.write-create-remove`.
- El prefijo `acl.` separa los generados de cualquier `Permission` escrito a mano que siga vivo durante la migración y hace trivial el inventario (`SELECT name FROM auth_permission WHERE name LIKE 'acl.%'`).
- `object` es siempre el FQN de la entidad.

### Colectivos

Un colectivo es un **nombre para una lista de destinatarios primitivos**, para no repetir la misma lista en varios `<ace>` de varios sistemas (el gestor de notificaciones, «la dirección del centro»…).
No se llama «rol» ni «grupo» a propósito: `Role` y `Group` son entidades de Axelor y `Perfil`/`AceProfile` ya significa otra cosa en el proyecto.

```xml
<!-- subsystem/security/security/colectivos.xml -->
<security xmlns="http://educaflow.com/xml/ns/security">

  <colectivos>
    <colectivo nombre="GESTOR">
      <admin/>
      <tipoUsuario codigo="SUPERVISOR"/>
      <tipoUsuario codigo="ADMINISTRATIVO"/>
      <cargo codigo="DIRECTOR"/>
      <cargo codigo="JEFE_ESTUDIOS"/>
      <cargo codigo="SECRETARIO"/>
    </colectivo>
  </colectivos>

</security>
```

Y en cualquier `<ace>` de cualquier fichero: `<colectivo nombre="GESTOR"/>`.

Reglas:
- **Solo globales y en un único sitio**: el bloque `<colectivos>` solo se admite en los ficheros de `subsystem/security/security/`, y el nombre es único en todo el proyecto (EFBT aborta si se repite o si otro fichero lo declara).
  Un colectivo no es un recurso, así que no entra en la regla de alcance de los `on`: se usa desde cualquier fichero.
  Sin colectivos locales ni sombreado; si algún día hacen falta, se decidirá entonces.
- **Sin anidamiento**: un colectivo contiene solo destinatarios primitivos, nunca otro colectivo.
- **Expansión**: EFBT sustituye `<colectivo>` por sus hijos **antes** de aplicar los comodines; el resultado es un `<ace>` normal (varios hijos, O).
  Si tras expandir queda un destinatario repetido (un `<admin/>` suelto más el del colectivo) se deduplica en silencio: no es error.
- **Las restricciones por bloque se comprueban tras expandir**, y como el XSD no ve lo que hay dentro, esas comprobaciones las hace EFBT y el mensaje de error nombra el colectivo:
  - `<todos/>` **no se admite dentro de un colectivo**: cada endpoint público tiene que estar escrito a la vista en su propio `<ace>`.
  - `<tramitador/>` y `<unidadTramitadora>` sí se admiten, pero entonces ese colectivo no puede usarse en `<entities>`.
- **Colectivo muerto**: un colectivo que no se usa en ningún `<ace>` es un error del test de cobertura (§7), como un comodín que no gana ningún recurso.
- En la tabla `AceRecurso` no queda rastro del colectivo: solo las filas de sus destinatarios.
  Cambiar la lista de un colectivo cambia por tanto todas las filas de todos los recursos que lo usan, en el siguiente build.
- Los `<acl>` de `AceProfile*` (§8) **no** admiten colectivos de momento: solo caben `tipoUsuario`/`cargo` y un colectivo con `<admin/>` no tendría columna.

### `on` y el sistema del fichero

Para un fichero dentro de un sistema, el sistema es la carpeta que contiene su `security/`: `src/main/java/com/educaflow/subsystem/criptografia/security/*.xml` → paquete base `com.educaflow.subsystem.criptografia`.
(Los ficheros fuera de un sistema se tratan en [Recursos que no están en ningún sistema](#recursos-que-no-están-en-ningún-sistema).)

| Bloque            | Se escribe                                                                                      | Nombre completo                                                                                  |
|-------------------|-------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------|
| `<actionMethods>` | `CertificadoDigitalController.getSituacionFirma` (relativo: desde la clase)                     | `com.educaflow.subsystem.criptografia.controller.CertificadoDigitalController.getSituacionFirma` |
| `<actions>`       | `subsysCriptografia.Main@CertificadoDigital-action` (el nombre **entero**, tal cual se declara) | el mismo                                                                                         |
| `<entities>`      | `CertificadoDigital` (relativo: desde la clase)                                                 | `com.educaflow.subsystem.criptografia.db.CertificadoDigital`                                     |

- En `<actionMethods>` y `<entities>` EFBT completa el paquete (`<base>.controller.` / `<base>.db.`).
- En `<actions>` se escribe el nombre entero porque leer solo una parte del nombre de una acción resulta confuso; EFBT no completa nada.
- Un `on` **no puede** salir de su sistema, y EFBT aborta si lo hace:
  - `<actionMethods>`: la clase tiene que estar **directamente** en `<base>.controller` (no se admiten subpaquetes: EFBT aborta si encuentra un controlador en uno) y el método existir en ella, sea un método de acción `(ActionRequest, ActionResponse)` o un endpoint JAX-RS; no se admite un FQN.
  - `<actions>`: la acción tiene que estar **declarada en un XML de vistas Axelor (`object-views`) de ese mismo sistema**, esté en `views/`, en `controller/` (p. ej. `actions-expedientes.xml`) o donde sea; no basta con que el nombre empiece por su prefijo.
  - `<entities>`: la entidad tiene que estar declarada en los `domains/*.xml` de ese mismo sistema.

### Recursos que no están en ningún sistema

Con «prohibido por defecto», todo recurso que no esté en un sistema tiene que poder declararse en algún sitio, o queda bloqueado.
Hoy hay tres casos:

| Recurso | Dónde está | Por qué no encaja en un sistema |
|---|---|---|
| `base/infrastructure/controller/*` | `com.educaflow.base.infrastructure` | no es `system/` ni `subsystem/` |
| `tramites/util/firma/FirmaClienteController` (`@CallMethod`) | `com.educaflow.tramites.util.firma` | no está bajo un paquete `controller` ni en un sistema |
| `action-view` de `tramites/shared/template-views.xml` (p. ej. `subsysExpedientes.Expediente-verHistorialEstados-action`) y las vistas generadas por tipo de expediente | `tramites/` | llevan prefijo de `subsysExpedientes` pero no están declarados en `subsystem/expedientes/views/` |

**Decisión: un fichero de seguridad puede estar en cualquier carpeta `security/` de `src/main/java`, y su alcance es siempre el mismo: el paquete que contiene esa carpeta `security/` y sus subpaquetes.**
Lo que cambia según dónde esté es cómo se escribe el `on`.

| Dónde está el fichero | Alcance | Cómo se escribe `on` |
|---|---|---|
| bajo `system/<x>/` o `subsystem/<x>/` | el sistema `<x>` | relativo, como hasta ahora (`MiController.metodo`, `MiEntidad`): EFBT completa con `<base>.controller.` / `<base>.db.` |
| en cualquier otra carpeta | el paquete de esa carpeta y sus subpaquetes | **nombre completo** (`com.educaflow.tramites.util.firma.FirmaClienteController.metodo`), porque no hay un `controller`/`db` fijo que completar |

- EFBT decide por la ruta del fichero: si entre `src/main/java/com/educaflow/` y `security/` hay un `system/<x>` o `subsystem/<x>`, es de ese sistema; si no, su alcance es el paquete de la carpeta donde está `security/`.
- **Un fichero no puede salir de su alcance.**
  Fuera de un sistema, el nombre completo del `on` tiene que empezar por el paquete de su carpeta: `tramites/util/firma/security/*.xml` solo puede nombrar clases de `com.educaflow.tramites.util.firma` o subpaquetes, y EFBT aborta si no.
  Es la misma regla que dentro de un sistema, solo que ahí el paquete lo fija el sistema.
- **Un fichero de fuera no puede tener sistemas por debajo.**
  EFBT aborta si bajo el paquete de la carpeta hay algún `system/<x>` o `subsystem/<x>`: si no, bastaría un `security/` en `com/educaflow/` para dar permisos sobre cualquier cosa.
  Así los recursos de un sistema solo se pueden declarar desde dentro de ese sistema.
- En los `<actions>` el `on` ya era el nombre entero, así que no cambia nada; la acción tiene que estar declarada en un XML de vistas Axelor dentro del alcance del fichero (los `views.xml` de las fases de los trámites incluidos), o salir de `tramites/shared/template-views.xml` para un fichero cuyo alcance lo contenga (incluidas las vistas que EFBT genera por tipo de expediente a partir de él, que conoce porque las genera).
- Fuera de un sistema, la clase de `<actionMethods>` y `<entities>` puede estar en cualquier paquete del alcance: para `FirmaClienteController` no hace falta un subpaquete `controller`.
- Los comodines siguen la misma forma, con nombre completo (`com.educaflow.base.infrastructure.controller.MiController.*`, `*`); un `*` abarca todos los recursos del alcance del fichero.
- **Las carpetas `security/` de fuera no pueden anidarse**: EFBT aborta si una está dentro del alcance de otra (p. ej. `tramites/security/` y `tramites/util/firma/security/` a la vez).
  Así cada recurso de fuera de los sistemas tiene **un solo** alcance que lo contenga, y el algoritmo de especificidad se aplica dentro de él, igual que dentro de un sistema.
- El `on` duplicado se detecta sobre el nombre completo y en todos los ficheros del mismo alcance a la vez.
- **Entidades de AOP** (`com.axelor.*`: `MetaFile`, `DMSFile`, `User`): no están en ningún `domains/` del proyecto, así que no las abarca ningún alcance.
  Se declaran en `<entities>` con **FQN** y **solo** desde `base/infrastructure/security/`, que es el único alcance al que EFBT se lo admite; en cualquier otro fichero un `on` que empiece por `com.axelor.` es error.
  EFBT comprueba que la clase existe en el classpath de AOP y hereda de `Model`.
- Ubicaciones previstas (no obligatorias, cualquier `security/` vale): `base/infrastructure/security/` y `tramites/util/firma/security/`.
  Las acciones de los trámites no se declaran a mano: las genera EFBT (ver [Acciones de los trámites](#acciones-de-los-trámites)).
- Los tests de cobertura (§7) recorren también estos recursos: ya no hay lista de exclusión.

### Acciones de los trámites

Cada fase de cada tipo de expediente declara acciones con nombre en su `views.xml` (p. ej. `exp-AnulacionMatriculaCicloFormativoV1-firmarSolicitud-action`), y a ellas se suman las que EFBT genera por tipo a partir de `tramites/shared/template-views.xml`.
Con «prohibido por defecto» habría que declararlas todas, y no hay destinatario que valga: quién puede ejecutarlas depende del **expediente concreto** (interesado, tramitador de ese trámite según `AceProfile*`), no de un tipo de usuario ni de un cargo.

**Decisión: EFBT genera la seguridad de los trámites.**
- Por cada tipo de expediente, EFBT genera (en el mismo sitio donde genera su data-init) un `<ace>` en `<actions>` con `<autenticado/>` que cubre **todas** las acciones con nombre de esa versión: las de los `views.xml` de sus fases y las generadas desde `template-views.xml`.
- Nadie escribe un `security/` por trámite, y EFBT aborta si encuentra uno bajo `tramites/<tramite>/`.
- Los métodos de los controladores de `subsystem/tramitador` y `subsystem/expedientes` que mueven la máquina de estados se declaran en el `security/` de su subsistema con `<autenticado/>`.
- La defensa real **no es la ACL** sino el motor: `Tramitador` ya comprueba, sobre el expediente concreto, el estado, el evento y el perfil del usuario (`AceProfile*`).
  La ACL solo dice «cualquier autenticado puede intentar llamar»; el motor decide si ese usuario puede hacer ese evento en ese expediente.
- Es la misma relación que en [Pendiente](#pendiente-fuera-de-este-plan) con el IDOR: la ACL dice quién puede *llamar*, el método sigue siendo responsable de *sobre qué*.

### Comodines y especificidad

**Forma permitida.**
El `*` solo puede ir **al final y como segmento entero** (separador `.`).

| Bloque | `on` válidos |
|---|---|
| `<actionMethods>` | `Ctrl.metodo`, `Ctrl.*`, `*` |
| `<actions>` | nombre entero o `*` (= todas las acciones con nombre declaradas en el sistema, salvo `action-method`; los nombres de acción llevan `.` y `@` que no son jerarquía) |
| `<entities>` | nombre exacto o `*` |

No valen `get*`, `*Controller`, `subsysCriptografia.*` ni `subsysCriptografia.Main@*` (lo valida el XSD con un `pattern`).

**Por qué nunca hay empate.**
Con esa forma, los `on` que encajan con un recurso son **el propio recurso** y **los prefijos de su nombre por segmentos** seguidos de `.*`, hasta `*`.
Todos son prefijos del mismo nombre, así que siempre uno contiene al otro: forman una **cadena**, nunca dos ramas.
Ejemplo para `CertificadoDigitalController.getSituacionFirma`:
1. `CertificadoDigitalController.getSituacionFirma`
2. `CertificadoDigitalController.*`
3. `*`

El único empate posible es el mismo `on` escrito dos veces, y eso es **error del build** (en el mismo alcance y bloque, en cualquiera de sus ficheros).

**Algoritmo de EFBT (no hay que ordenar nada).**
Por cada recurso concreto de cada alcance (cada método de controlador, cada acción con nombre, cada entidad):
1. Busca un `<ace>` con `on` igual al nombre del recurso.
2. Si no hay, quita el último segmento y busca `<prefijo>.*`; repite subiendo de nivel.
3. Si no hay ninguno, busca `*`.
4. El primero que encuentra **gana** y genera las filas de ese recurso (una por destinatario y por permiso).
5. Si no encuentra ninguno, el recurso no tiene filas: queda prohibido (y el test de cobertura lo señala).

**Gana y sustituye, no suma.**
Las filas del `<ace>` ganador son **todas** las del recurso; las de los `<ace>` menos específicos no se añaden.
Así un método puede ser más restrictivo que su controlador.
Consecuencia: si `*` da acceso a `<admin/>` y un método concreto se declara solo para `<tipoUsuario codigo="SUPERVISOR"/>`, el administrador **pierde** el acceso a ese método salvo que se repita `<admin/>`.
Es deliberado (sumando no se podría restringir nunca), pero hay que documentarlo en el skill.

En `<entities>`, donde un mismo `on` puede repetirse, el ganador es el **conjunto** de todos los `<ace>` con ese `on`, y sustituye a los menos específicos para **todos** los flags a la vez, no flag a flag.
Por ejemplo, con `<ace on="*" canRead="true"><autenticado/></ace>` y un `<ace on="Notificacion" canRead="true" canWrite="true" canCreate="true" canRemove="true">` solo para SUPERVISOR, el `<autenticado/>` del `*` **pierde** también el `canRead` sobre `Notificacion`.

## Tablas

### Una sola tabla: `AceRecurso`

Columnas:
- `tipoRecurso`: `ACTION_METHOD`, `ACTION`.
  Las entidades **no** tienen filas: su permiso es el `Permission` que genera EFBT (decisión 24).
- `recurso`: nombre completo y **concreto** (sin comodines).
- `permiso`: hoy solo `EXECUTE` (de `canExecute`), en las filas `ACTION_METHOD` y en las `ACTION`.
  La columna existe por lo mismo que el flag es un atributo: que un bloque pueda tener más de un permiso sin cambiar la tabla.
- Destinatario, **exactamente uno** por fila: `admin` (bool), `autenticado` (bool), `todos` (bool), `tipoUsuario` (FK a `TipoUsuario`), `cargo` (FK a `Cargo`), `tramitador` (bool) y `unidadTramitadora` (FK a `UnidadTramitadora`, de `subsystem/expedientes`).
  Las tres FK se cargan en el data-init por `search` sobre su código, como en `AceProfile*`.
  Las dos últimas solo aparecen en filas `ACTION_METHOD`/`ACTION`, porque son destinatarios derivados; `todos` solo en filas `ACTION_METHOD` de endpoints públicos.

Por qué una tabla y no varias:
- Las `AceProfile*` se parten porque cada una se rellena en un sitio distinto y tienen columnas distintas (`tramite`, `centro`, `expediente`…).
  Aquí todo sale del mismo XML y todas las filas tienen las mismas columnas.
- Con una columna `permiso` en vez de booleanos por permiso, métodos y acciones se guardan igual: un solo cargador, una sola caché y una sola forma de consulta (`recurso = … AND permiso = …`).
- Mantener la tabla aparte de `AceProfile*` (y no fundirlas en una) es a propósito: comparten **la forma del destinatario** (mismas columnas y mismos elementos XML), no la tabla.

### Carga

- EFBT genera un data-init (`AceRecurso.xml` + su `input-config.xml`) con las filas ya expandidas, en el data-init que copia el build.
- `AceRecurso` **MUST** añadirse a `tablasIncluidas` de `DataBaseStartup`: el data-init solo hace upsert y las filas quitadas del XML no se borrarían.
- `auth_permission` (la tabla de `Permission`) **no hace falta añadirla**: `BulkTables.truncateTables` ya vacía en cada arranque todas las tablas `meta_%` y `auth_%` salvo las excluidas (`meta_file`, `meta_sequence`, `auth_user`, `auth_group`, `meta_filter`), y eso incluye `auth_permission` y sus intermedias con `Group` y `User` (`auth_group_permissions`, `auth_user_permissions`).
  Además trunca con `CASCADE`, así que una intermedia que no entrara por el patrón se vaciaría igual.
  Por tanto, al borrar un `auth-*.xml` sus `Permission` desaparecen en el siguiente arranque sin hacer nada más.
- Dominio en `subsystem/security/domains/AceRecurso.xml`, repositorio propio en `db/repo/`.

### Uso

- **Métodos, acciones y menús** (evaluados en Java): `AclService` carga las filas `ACTION_METHOD`/`ACTION` una vez al arrancar y las guarda en memoria (solo cambian al arrancar).
  No hay una consulta por acción ni por menú.
- **Entidades** (evaluadas por Axelor): EFBT genera, por cada `<ace>` ganador y entidad, un `Permission` cuya `condition` sale de los destinatarios, de la propiedad `Centro` y de la `condition` propia del `<ace>`, tal como está definido en [Condición JPQL de los permisos de entidad](#condición-jpql-de-los-permisos-de-entidad), y con el nombre de [Nombre de cada `Permission`](#nombre-de-cada-permission).
  Axelor **no** sustituye `__user__` dentro del texto de la `condition`; cada `?` se enlaza por posición con un parámetro de `conditionParams` (`AuthSecurity.Condition`, ver [Verificado en AOP](#verificado-en-aop-referencia-para-implementar)).
  Los nombres de entidad van con FQN, como en `auth-expedientes.xml`.
  - Cada `Permission` generado se enlaza al grupo `users` (`<group code="users"><permission name="…"/></group>`, como hoy hace `auth.xml`).
  - Verificado en `AuthSecurity.isPermitted` / `getFilter` de AOP:
    - si no hay ningún `Permission` para el modelo devuelve `false`, así que «prohibido por defecto» se cumple en entidades sin hacer nada más;
    - un `Permission` sin `condition` concede sin más, y las `condition` de todos los que aplican al mismo objeto se unen con **OR** (`Filter.or`), así que un `Permission` por permiso con su propia `condition` funciona;
    - con solo permisos condicionales y sin ids (el caso de `create`, que no tiene registro) devuelve `true`, así que `create` con `condition` funciona;
    - con usuario nulo (procesos de sistema) devuelve `true`, y el administrador cuenta como usuario nulo (`getUser()`, l. 75-79): por eso `<admin/>` no necesita fragmento.
  - **`EducaFlowAuthResolverImpl` se elimina** (y con él el registro en `SecurityModule` y, si no le queda otro uso, el parche `EducaFlowAuthResolverRegistry` de AOP): hoy **sustituye** los `Permission` de una subclase por los de `Expediente` (no los une), y si `Expediente` tuviera `write` para alguien lo heredarían todas las subclases por detrás del generador.
    Con la decisión 22 ya no hace falta: cada subclase tiene sus propios `Permission` generados (copia de los de `Expediente`, solo `canRead`) y `AuthResolver` de AOP los resuelve por nombre exacto.
  - Precedente: `auth-expedientes.xml` ya usa `condition` con subconsultas a tablas `AceProfile*`.

## Componentes

### 1. XSD

- `security_1.0.xsd` con namespace `http://educaflow.com/xml/ns/security`.
- Define los bloques `<actionMethods>`, `<actions>`, `<entities>` y `<colectivos>`; `<colectivo nombre="…"/>` es hijo válido de los tres tipos de `<ace>`, y dentro de `<colectivos>` solo caben destinatarios primitivos (ni `<todos/>` ni otro `<colectivo>`).
- Solo el `<ace>` de `<entities>` tiene los atributos opcionales `condition`, `conditionParams` (decisión 21) y `propiedadCentro` (decisión 25).
- Lo usan EFBT para validar y los tests.
- Ubicación: **solo en EFBT**, sin copia en el proyecto ni `xsi:schemaLocation` en los ficheros, igual que con `TramiteInstance.xml` y `TipoExpedienteInstance.xml` (que tampoco referencian XSD y valida EFBT al compilar).
  El IDE no autocompleta; a cambio no hay una copia que mantener sincronizada.

### 2. Generación en el build (EFBT)

Tarea nueva (`generateSecurity`, `JavaExec`), antes de `processResources`, que:
1. Recorre `src/main/java/**/security/*.xml` y se queda con los del namespace de seguridad; por la ruta de cada uno decide su alcance (un sistema, o el paquete de su carpeta si está fuera de uno, abortando si ese paquete tiene sistemas por debajo).
2. Valida cada uno contra el XSD.
3. Lee los `<colectivos>` (solo admitidos en `subsystem/security/security/`; nombre único en todo el proyecto) y sustituye cada `<colectivo nombre="…"/>` de cada `<ace>` por sus destinatarios, deduplicando; un nombre que no existe aborta.
   Después comprueba las restricciones por bloque sobre el `<ace>` ya expandido (derivados fuera de `<entities>`), nombrando el colectivo en el error.
4. Comprueba `on` duplicados (mismo alcance y bloque; en `<entities>` mismo `on`, mismos flags a `true` y misma `condition` y `conditionParams`) y que cada `on` exacto existe.
   Aborta si un controlador tiene **dos métodos invocables con el mismo nombre** (sobrecarga): la clave `Ctrl.metodo` de la tabla no los distingue.
   AOP tampoco los distingue de verdad: `ActionHandler.call` busca la firma exacta `(ActionRequest, ActionResponse)` y `rpc` resuelve por nombre y valida por argumentos (ver [Verificado en AOP](#ejecución-de-acciones-y-eventos)), así que un método de acción y un endpoint JAX-RS con el mismo nombre compilan pero son una fuente de confusión que no se admite.
5. Enumera los recursos concretos de cada alcance.
   Los controladores los lee **del código fuente con el compilador del JDK** (`javax.tools` / Compiler Tree API), como hacen ya los tests de `ordenmetodos`: la tarea va antes de compilar, así que no hay bytecode, y así solo hay una forma de leer fuentes en el proyecto.
   - métodos públicos `(ActionRequest, ActionResponse)` de sus controladores (todos son invocables, tengan o no `@CallMethod`);
   - endpoints JAX-RS de sus controladores (método con `@GET`/`@POST`/`@PUT`/`@DELETE` en una clase con `@Path`), anotando su ruta completa para saber si cuelga de `/ws/public/**`;
   - acciones con nombre de todos sus XML de vistas Axelor (`views/`, `controller/*.xml`, `views.xml` de fases…), de cualquier tipo salvo `action-method`;
   - entidades de sus `domains/*.xml`.
6. Expande los comodines con el algoritmo de especificidad.
   Comprueba `<todos/>`: solo en endpoints JAX-RS públicos, y en esos no se admite otro destinatario (ver [Destinatarios](#destinatarios)).
   Un `Ctrl.*` o `*` que caiga sobre un endpoint público **no le da filas**: el endpoint público tiene que declararse con su propio `<ace>` y `<todos/>`, para que cada ruta sin login esté escrita a la vista.
7. Genera el data-init de `AceRecurso` (solo métodos y acciones).
8. Genera los `Permission` de entidades (con su `<bind node="group">` al grupo `users`): uno por `<ace>` ganador y entidad, con los flags que el `<ace>` tenga a `true`, el nombre `acl.<Entidad>.<flags>[.<n>]` y la `condition` compuesta de ancla + fragmentos + `condition` propia (ver [Condición JPQL de los permisos de entidad](#condición-jpql-de-los-permisos-de-entidad)).
   Para el ancla lee los `many-to-one` a `Centro` de la entidad en sus `domains/*.xml` (y los heredados de `Expediente` en las subclases) y resuelve `propiedadCentro`, abortando en los casos de la decisión 25.
   Aborta si, para una entidad y un flag, genera a la vez un `Permission` sin `condition` y otro con `condition` (ver [Condición JPQL de los permisos de entidad](#condición-jpql-de-los-permisos-de-entidad)).
9. Por cada tipo de expediente, copia los `<ace>` de `<entities>` de `Expediente` a la subclase, solo `canRead` (decisión 22).
   Al quitar los demás flags, descarta los `<ace>` que se quedan sin ninguno (los que solo tenían `canWrite`, `canCreate`…) y **fusiona** los que quedan idénticos (misma `condition` y `conditionParams`) uniendo sus destinatarios, que con OR es lo mismo; no aborta, porque el duplicado no lo ha escrito nadie.
   Después numera `.<n>` igual que en el resto.

### 3. Lectura en la app

- `AclService` (en `subsystem/security`): caché en memoria de las filas de métodos y acciones; `puedeEjecutarMetodo(user, metodo)`, `puedeEjecutarAccion(user, accion)`.
  Sin lógica de comodines: la tabla ya es concreta.
  - **Orden de arranque.** La caché se carga en un observador de `StartupEvent` (como `AppEventObserver`), nunca en el `configure()` de un módulo Guice ni en un constructor.
    El orden real es: `SecretariaVirtualModule.configure()` trunca las `tablasIncluidas` → `AppStartup.init` llama a `ModuleManager.initialize`, que carga los data-init (ahí entra `AceRecurso`) → se dispara `StartupEvent` → se carga la caché.
    Cargarla antes dejaría la caché vacía y, en `enforce`, todo prohibido.
- `DestinatarioEvaluator`: decide si un usuario encaja con el destinatario de una fila.
  - `todos`: siempre encaja, sin consultar siquiera si hay usuario (en un endpoint público `SecurityUtil.getUser()` es `null`).
  - `admin`, `autenticado`, `tipoUsuario` y `cargo` (en algún centro): con `SecurityUtil.getUser()`, `SecurityUtil.isAdmin` y `user.tieneTipoUsuario`/cargo; sin usuario ninguno de ellos encaja.
  - `tramitador` y `unidadTramitadora`: con `PerfilesUsuarioService.isTramitador`, que está en el mismo subsistema `security`.
- Ambos con tests unitarios (cuentan para `crapCheck`).

### 4. Filtro de acciones (observador)

`AclActionObserver` en `subsystem/security`, enlazado en `SecurityModule` con `bind(AclActionObserver.class)` (sin eso `EventBus` no lo encuentra).

- `@Observes PreAction`:
  1. Averigua qué hay detrás del nombre:
     - con `:` y primer segmento tipo de vista (`form:`, `grid:`…) → `action-view` ad hoc;
     - con `:` → `ActionMethod` ad hoc: `controlador` + `metodo` (quitando `(args)` si los hay);
     - sin `:` → `MetaStore.getAction(nombre)`: si es `ActionMethod`, su `getCall()` (recurso `ACTION_METHOD`); cualquier otro tipo, su nombre (recurso `ACTION`).
  2. Solo filtra lo **nuestro**, y lo decide **por módulo**:
     - un `ActionMethod` es nuestro si la clase de su `getCall()` está en `com.educaflow`;
     - cualquier otra acción es nuestra si el `module` de su `MetaAction` (el módulo Axelor que la declaró, que `MetaStore` conoce) es el de la aplicación (`secretaria-virtual`, el nombre del módulo del `build.gradle`); las de AOP llevan `axelor-core`/`axelor-web`.
     Así las acciones de AOP (login, preferencias, mail…) no se tocan.
     Un `action-group` ad hoc (`a,b,c` separados por coma) no es un recurso: no se comprueba el grupo, sino cada acción de dentro cuando le toca pasar por `Action.execute`.
  3. Sin permiso `EXECUTE` → se lanza una excepción, **la misma para todos los tipos de acción**, `action-view` incluido (decisión 17).
     Se usa la excepción más simple que `ActionResource` de AOP convierta en un error de acción para el cliente (una `RuntimeException` propia, `AclDeniedException`, con el nombre del recurso en el mensaje); no hace falta un 403 HTTP ni una pantalla.
     **MUST NOT** hacerse en `PostAction`: el método, el `action-record` o el `action-script` ya se habrían ejecutado.
     Sin usuario en sesión (`SecurityUtil.getUser()` nulo: un proceso de sistema) el observador **deja pasar**: hoy ningún código del proyecto ejecuta acciones XML por nombre desde Java, pero si algún día un job lo hiciera no debe bloquearse, igual que `AuthSecurity.isPermitted` devuelve `true` sin usuario.
- `action-view` ad hoc (`form:vista:Modelo`): AOP admite como nombre de acción `tipoDeVista:nombreDeVista[:Modelo]` y `ActionGroup.findAction` construye con él un `action-view` al vuelo que abre esa vista, sin que nadie lo haya declarado en ningún XML.
  No tiene nombre declarable, así que con «prohibido por defecto» se bloquea.
  **Decisión: se bloquea.** El cliente web de Axelor (`axelor-front`) no construye nombres así por sí mismo (no hay ninguno en su código), así que solo pueden venir de una petición hecha a mano; el modo `warn` confirmará que no aparece ninguno.
- Las acciones de dentro de un `action-group` declarado pasan una a una por el observador (`ActionGroup.evaluate` → `action.wrap(handler)`), así que declarar el grupo **no** cubre a sus hijas: cada acción con nombre tiene su propio `<ace>` (o cae en un comodín).

### 4b. Filtro de endpoints JAX-RS

Los endpoints JAX-RS no pasan por `Action.execute`, así que el observador no los ve: los filtra `AclRestFilter`, un `ContainerRequestFilter` anotado con `@Provider` en `subsystem/security`.
Un `ContainerRequestFilter` es el interceptor estándar de JAX-RS: se ejecuta antes que el método de cualquier endpoint REST y puede cortar la petición.
Para que RESTEasy lo cargue basta con `bind(AclRestFilter.class)` en `SecurityModule`: `AppContextListener` de AOP pasa el `Injector` de Guice a un `ModuleProcessor` de RESTEasy, que registra como recurso o provider toda clase enlazada en Guice que lleve `@Path` o `@Provider` (así se registran hoy `RegistroSalidaController` y los providers de AOP).

- Averigua el método destino con `ResourceInfo` (`getResourceClass()` + `getResourceMethod()`), que es el mismo nombre `Controlador.metodo` de la tabla.
- Solo filtra controladores de `com.educaflow`; los recursos REST de AOP (`/ws/rest`, `/ws/meta`, `/ws/files`…) no se tocan.
- Sin permiso `EXECUTE` → `abortWith(403)`; en modo `warn`, solo lo registra.
- Para un endpoint público la única fila posible es `todos`, así que el filtro deja pasar sin mirar la sesión.
  La defensa real de ese endpoint sigue siendo la suya (el CSV, que valida el servicio): la fila `todos` **documenta** que es público, no lo protege.

### 5. Menús

- Un `<menuitem>` con `action` se ve si el usuario tiene `EXECUTE` (`canExecute`) sobre esa acción.
- Un `<menuitem>` sin `action` (padre) se ve si se ve alguno de sus descendientes con `action`.
  AOP **no** lo hace solo: `MenuService` corta el subárbol de un padre oculto, pero deja visible un padre aunque todos sus hijos estén ocultos (por eso hoy el `switch` oculta padres a mano).
- Se engancha donde ya está, el `if` que EFBT inyecta en cada `<menuitem>` (`MenuItem.java:23`), pero pasando **acciones** en vez del nombre del menú:
  - menú con `action` → `__config__.menuSecurity.isVisible("<action>")`;
  - menú padre → `__config__.menuSecurity.isAnyVisible("<action1>", "<action2>", …)` con las acciones de **todos** sus descendientes, que EFBT calcula al procesar `menus.xml`.
  Así en tiempo de ejecución no hay que recorrer el árbol de `MetaMenu`.
- `MenuSecurityServiceImpl` deja de tener el `switch`: `isVisible(action)` solo consulta `AclService`.
- `groups` de `menus.xml` sigue siendo obligatorio mientras exista `MenuChecker.isAllowed`: para un no administrador, un menú raíz sin `groups` no se ve nunca.
  **Decisión: lo inyecta EFBT** (`groups="admins,users"` en todo `<menuitem>`, igual que inyecta el `if`), y en `menus.xml` deja de escribirse; el criterio real es solo el `if`.
  Así un menú nuevo no puede nacer invisible por olvidar el atributo.
  Ojo: el administrador se salta `groups` pero **no** el `if`, así que donde deba verlo tiene que estar `<admin/>` en el `<ace>`.
- **Nada de esta sección se implementa hasta la fase 12** (decisión 18): mientras se migran los sistemas los menús siguen exactamente como hoy (`switch` + `groups`), para que lo que ve cada usuario y la suite E2E no cambien a medias.
- Efecto añadido: como la visibilidad pasa a depender de la acción, el observador también **bloquea abrir esa acción** a quien no ve el menú.
  Hoy, por ejemplo, cualquiera puede abrir `sysVentanilla.SecretariaAbiertos@Expediente-action` aunque no vea su menú.
  Ninguna acción se repite entre menús, pero alguna puede abrirse además desde un botón para otros usuarios: lo destapará el modo `warn`.

#### ¿Se puede migrar hoy la visibilidad de los menús?

Casi toda. Solo faltan las reglas de tramitador, hasta que existan sus destinatarios derivados (fase 10); como los menús se migran en la fase 12, para entonces ya existen.

| Menú | Regla actual (`switch` o `groups`) | Con el formato nuevo |
|---|---|---|
| Los de `groups="admins"` (`firmas-todas`, `tramitacion-todosLosCentros*`, `notificaciones-todas`, `administracion*`, `criptografia*`, `sistemaEducativo*`, `desarrollador*`) | solo administrador | ✅ `<admin/>` |
| Los de `groups="admins,users"` sin regla en el `switch` | cualquiera | ✅ `<autenticado/>` |
| `firmas-delCentro` | supervisor | ✅ `<tipoUsuario codigo="SUPERVISOR"/>` |
| `miCentro` (padre de 3 acciones) | supervisor | ✅ `<tipoUsuario codigo="SUPERVISOR"/>` en las 3 acciones hijas; el padre sale de ellas |
| `notificaciones-delCentro` | `GestorNotificacionesUtil.esGestorEnAlgunCentro` (tipos `SUPERVISOR`/`ADMINISTRATIVO` o cargos `DIRECTOR`/`JEFE_ESTUDIOS`/`SECRETARIO`) | ✅ `<colectivo nombre="GESTOR"/>` |
| `tramitacion` (padre) | admin **o** `isTramitador(user)` | ⏳ con `<tramitador/>` (fase 10) |
| `tramitacion-jefaturaDeEstudios` (padre) | `isTramitador(user, JEFATURA_ESTUDIOS)` | ⏳ con `<unidadTramitadora codigo="JEFATURA_ESTUDIOS"/>` (fase 10) |
| `tramitacion-secretaria` (padre) | `isTramitador(user, SECRETARIA)` | ⏳ con `<unidadTramitadora codigo="SECRETARIA"/>` (fase 10) |
| `registro` (padre) | admin **o** `isTramitador(user, SECRETARIA)` **o** supervisor | ⏳ `<admin/>` + `<tipoUsuario codigo="SUPERVISOR"/>` + `<unidadTramitadora codigo="SECRETARIA"/>` (fase 10) |

- `isTramitador` no depende de tipo de usuario ni de cargo sino de los perfiles de las tablas `AceProfile*` (`PerfilesUsuarioService.tramitaAlgunTramite`).
- Los destinatarios derivados `<tramitador/>` y `<unidadTramitadora codigo="…"/>` los evalúa `DestinatarioEvaluator` llamando a `PerfilesUsuarioService`, que está en el mismo subsistema `security`.
- Como ahora la regla va en las **acciones hijas** y no en el padre, los hijos de `tramitacion` (`nuevoTramite` en papel, `pendientesDeMi`) necesitan cada uno su `<tramitador/>` (+ `<admin/>`), y los de `jefaturaDeEstudios`/`secretaria` su `<unidadTramitadora>`.
- Como los menús se migran al final (fase 12) y los destinatarios derivados llegan antes (fase 10), el `switch` desaparece de golpe en la fase 12: no hay un estado intermedio con 4 menús en el `switch` y el resto en acciones.

### 6. Modo transitorio `warn` / `enforce`

- Propiedad en `axelor-config.properties`: `security.acl.mode = warn | enforce` (por defecto `warn` mientras dure la migración).
- `warn`: el observador y el filtro JAX-RS **no** bloquean; registran en el log cada denegación que habrían hecho (acción, usuario, motivo), sin datos personales más allá del login.
  Es un único interruptor **global**; no hay modo por sistema.
  Durante la migración un sistema se da por revisado cuando la suite E2E pasa en verde **sin que el log de `warn` registre ninguna denegación de recursos de ese sistema**.
- `enforce`: bloquean.
- Los menús no tienen modo `warn`: no se tocan hasta la fase 12 (decisión 18), y entonces pasan directamente a consultar `AclService`.
- Los `Permission` de entidades no tienen modo `warn`: se activan sistema a sistema al migrar su bloque `<entities>`.
- Se elimina la propiedad y el modo `warn` cuando todos los sistemas estén migrados y los tests en verde.

### 7. Tests (escritos a mano, como `tiposexpedientes` y `ordenmetodos`)

En `src/test/java/com/educaflow/security`:
- **Cobertura de métodos**: todo método público `(ActionRequest, ActionResponse)` y todo endpoint JAX-RS de un controlador de `com.educaflow`, esté o no en un sistema, queda cubierto por algún `<ace>` de `<actionMethods>`.
- **Endpoints públicos**: todo endpoint JAX-RS bajo `/ws/public/**` tiene su propio `<ace>` con `<todos/>` como único destinatario, y ningún `<todos/>` cae fuera de uno de ellos.
  Es el inventario de lo que se puede llamar sin sesión.
- **Cobertura de acciones**: toda acción con nombre de cualquier XML de vistas Axelor de `com.educaflow`, de cualquier tipo salvo `action-method`, queda cubierta por algún `<ace>` de `<actions>`; y ningún `<ace>` de `<actions>` nombra un `action-method`.
- **Cobertura de entidades**: toda entidad de `domains/` queda cubierta por algún `<ace>` de `<entities>`.
- **Comodines vivos**: todo `on` con comodín es el ganador de al menos un recurso (si no, es una regla muerta).
- **Colectivos**: todo colectivo se usa en al menos un `<ace>`, el bloque `<colectivos>` solo aparece en `subsystem/security/security/` y ningún nombre se repite.
- **Menús**: toda `action` de `menus.xml` está cubierta en `<actions>`.
- **Los tests de cobertura se escriben en la fase 6 pero nacen deshabilitados** (`@Disabled("hasta migrar todos los sistemas, plan_security fase 13")` en cada clase), porque `./run.sh` ejecuta los tests y durante la migración fallarían para los sistemas aún no migrados.
  Se pueden lanzar a mano para ver el inventario de lo que falta; se habilitan en la fase 13.
- Unitarios de `AclService` y `DestinatarioEvaluator`.
- Unitarios en EFBT del algoritmo de expansión (cadena de prefijos, sustitución, duplicados, recurso sin `<ace>`, colectivos: sustitución, deduplicado, nombre inexistente, derivado en `<entities>` a través de un colectivo).
- Unitarios en EFBT de la generación de `Permission`: la `condition` exacta de cada combinación de destinatarios (solo `<admin/>` → sin `Permission`; solo `<autenticado/>` sin centro → sin `condition`; `<tipoUsuario>`+`<cargo>` → un `IN` por tabla), el ancla con y sin propiedad `Centro`, el AND con la `condition` propia y el orden de `conditionParams`, la resolución de `propiedadCentro` (cero, una y dos propiedades; colección no cuenta; error si sobra, falta o va en un comodín) el nombre `acl.<Entidad>.<flags>[.<n>]` (orden canónico de los flags y sufijo `.2`), varios `<ace>` con el mismo `on` (distintos flags sin condición: válido; mismos flags y misma condición: error), un `<ace>` de `<entities>` sin ningún flag a `true` (error), el `canExecute` que falta (EFBT lo pone a `true`), el aborto por mezclar incondicional y condicional en un flag, y la copia a las subclases de `Expediente` (descarte de los que se quedan sin flags y fusión de los idénticos).

### 8. Migrar `AceProfile*` a destinatarios como elementos

Mismo cambio de forma en todos los `<acl>` de perfiles, para que «destinatario» se escriba igual en todo el proyecto:

```xml
<!-- antes -->
<ace perfil="DIRECTOR">
    <usuario cargo="DIRECTOR"/>
</ace>

<!-- después -->
<ace perfil="DIRECTOR">
    <cargo codigo="DIRECTOR"/>
</ace>
```

- Ficheros afectados:
  - `subsystem/security/data-init/input/AceProfileGlobal.xml` y `AceProfileTipoUsuarioTramite.xml`, y los `bind` de su `input-config.xml` (`acl/ace[usuario/@cargo]` → `acl/ace/cargo`, `usuario/@cargo` → `cargo/@codigo`, igual con `tipoUsuario`);
  - los `<acl>` de `TramiteInstance.xml` y `TipoExpedienteInstance.xml` (hoy solo en `justificacion_falta_profesorado`, y de prueba);
  - en EFBT, el modelo `files/ace/Ace.java` y los generadores del data-init de `AceProfileTramite`/`AceProfileTipoExpediente`.
- En `AceProfileTipoUsuarioTramite`, `<tramite tipoUsuario="…"/>` no es un destinatario sino «sobre qué», y se queda como está.
- Solo admiten los destinatarios que tienen columna en esas tablas (`tipoUsuario`, `cargo`); `<admin/>`, `<autenticado/>`, `<todos/>` y los derivados no.
  Con elementos, el XSD (o EFBT para los ficheros maestros) lo impide; ya no hace falta el test de «un solo atributo».
- Hoy no hay ningún `<usuario>` con dos atributos, así que el cambio no altera ninguna fila.
- Documentación a actualizar: `subsystem/security/CLAUDE.md`, y los skills `k-tramite` y `k-tipo-expediente` (que describen el `<acl>` de los ficheros maestros).

## Ficheros de seguridad a crear

Los escribe Claude, uno por alcance, y el usuario los revisa.
El inventario de hoy (controladores, acciones con nombre sin contar `action-method`, entidades y el `auth-*.xml` que sustituyen) es el punto de partida de cada uno:

| Fichero | Controladores | Acciones (aprox.) | Entidades | Sustituye |
|---|---|---|---|---|
| `subsystem/criptografia/security/acl.xml` | 2 | 19 | 3 | `data-init/input/security.xml` (formato antiguo, sin commitear) |
| `subsystem/notificaciones/security/acl.xml` | 1 | 35 | 4 | `auth-notificaciones.xml` |
| `subsystem/firmas/security/acl.xml` | 1 | 25 | 2 | `auth-firmas.xml` |
| `subsystem/registroentradasalida/security/acl.xml` | 1 (JAX-RS público) | 6 | 2 | — |
| `subsystem/registrousuario/security/acl.xml` | 1 (JAX-RS público) | 0 | 2 | — |
| `subsystem/security/security/acl.xml` + `colectivos.xml` | 1 | 10 | 6 (+ `AceRecurso`) | `auth-security.xml` |
| `subsystem/common/security/acl.xml` | 0 | 30 | 14 | `auth-common.xml` |
| `subsystem/sistemaeducativo/security/acl.xml` | 0 | 66 | 8 | `auth-sistemaeducativo.xml` |
| `subsystem/importacion/security/acl.xml` | 0 | 7 | 1 | — |
| `subsystem/pdfutilities/security/acl.xml` | 1 | 10 | 1 | — |
| `subsystem/expedientes/security/acl.xml` | 1 | 2 | 7 (`Expediente` solo `read` para no admin) | `auth-expedientes.xml` (el último) |
| `subsystem/tramitador/security/acl.xml` | 1 (`<autenticado/>`) | 0 | 0 | — |
| `subsystem/smoketest/security/acl.xml` | 0 | 5 | 1 | `auth-smoketest.xml` |
| `system/gestioncentro/security/acl.xml` | 1 | 22 | 0 | `auth-gestioncentro.xml` |
| `system/ventanilla/security/acl.xml` | 2 | 37 | 2 | `auth-ventanilla.xml` |
| `base/infrastructure/security/acl.xml` | `DefaultModelController` (nombre completo, `<autenticado/>`) | las `action-method` de `DefaultModelController.xml` no se declaran | `MetaFile`, `DMSFile`, `User` (FQN, decisión 23) | la parte de permisos de `src/main/resources/data-init/input/auth.xml` |
| `tramites/util/firma/security/acl.xml` | `FirmaClienteController` (nombre completo) | 0 | 0 | — |
| `tramites/shared/security/acl.xml` | 0 | `subsysExpedientes.Expediente-verHistorialEstados-action` y las demás de `template-views.xml` que no sean por tipo | 0 | — |

- `correos` y `sms` no tienen recursos: no llevan fichero (y el test de cobertura no les pide nada).
- Las acciones de cada tipo de expediente (`tramites/alumnos/*`, `tramites/profesores/*`) las genera EFBT (ver [Acciones de los trámites](#acciones-de-los-trámites)); `tramites/prueba` se borra antes (decisión 19).
- De dónde sale el contenido de cada fichero, por bloque:
  - `<actions>`: de la regla de visibilidad de hoy del menú que abre cada acción (`switch` de `MenuSecurityServiceImpl` y `groups` de `menus.xml`, ver la tabla de [Menús](#se-puede-migrar-hoy-la-visibilidad-de-los-menús)); para las acciones que no abre ningún menú (`onNew`, `onLoad`, `onChange`, botones), los destinatarios del form que las usa, y lo que falte lo destapa el log de `warn`.
  - `<actionMethods>`: los mismos destinatarios que la acción o el form desde el que se llama al método; los del motor (`tramitador`, `expedientes`) con `<autenticado/>`.
  - `<entities>`: se escribe de nuevo desde lo que debe ser (fase 9); el `auth-*.xml` actual solo sirve como lista de nombres de entidades.
- Cada fichero se da por revisado cuando el usuario lo aprueba y la suite E2E sigue en verde sin denegaciones de ese sistema en el log de `warn` (el modo es global, ver §6).
- `base/infrastructure/security/acl.xml` declara `DefaultModelController.*` con `<autenticado/>`: sus tres métodos (`refreshTab`, `validateSave`, `validateDelete`) los llama cualquier form y la defensa real es el `Permission` de la entidad que toca cada uno.
  Y declara las entidades de AOP (decisión 23):
  - `MetaFile`: `create` para `<autenticado/>` (la subida de ficheros) y **ningún `read` general**.
    La lectura la decide Axelor por el padre: `RestService.download` permite descargar un `MetaFile` si lo creó el propio usuario o si la URL lleva `parentId`/`parentModel` y el usuario puede leer ese padre **y** el fichero cuelga de un campo suyo (ver [Verificado en AOP](#ficheros-metafile)).
    Por eso los `<iframe>` de `template-views.xml` y de los `views.xml` de las fases **MUST** añadir `&parentId=${id}&parentModel=<FQN de la subclase>` a la URL de descarga.
  - `DMSFile`: nada para `users` (la aplicación no usa el gestor documental: todos los `canAttach` del proyecto son `false`); si el log de `warn` lo desmiente, se decide entonces.
  - `User`: `read` con `condition="self = ?"` y `write` con la misma para el propio usuario; lo que necesiten los supervisores (`gestioncentro`) se escribe en la fase 9 con su condición.

## Fases

1. **XSD + `AceRecurso`** (dominio, repositorio, `tablasIncluidas` con `security_ace_recurso`); el XSD incluye ya `<colectivos>`.
2. **EFBT `generateSecurity`** para `actionMethods` y `actions`: validación, expansión de colectivos y comodines, y data-init de `AceRecurso`; `mvn install` en EFBT.
   Crear `subsystem/security/security/colectivos.xml` con `GESTOR`.
3. **`AclService` + `DestinatarioEvaluator`** con sus tests unitarios.
4. **Observador** (`PreAction`) y **filtro JAX-RS** (`AclRestFilter`) en modo `warn`, con `AclDeniedException`.
5. **Migrar `criptografia`**: borrar `data-init/input/security.xml` y crear `security/acl.xml` con el formato nuevo (sin `<entities>` todavía).
6. **Tests de cobertura** (§7), escritos y deshabilitados con `@Disabled` hasta la fase 13; lanzados a mano son el inventario de la migración.
7. **Migrar `AceProfile*` a elementos** (§8). Es independiente y puede hacerse en cualquier momento.
8. **Borrar el trámite `prueba`** (decisión 19), antes de tocar `expedientes`.
9. **`<entities>` en EFBT**: `Permission` por `<ace>` ganador y entidad, con la `condition` compuesta de ancla, fragmentos por destinatario, filtro por centro de oficio y `condition` propia (decisiones 21, 24 y 25) y el nombre `acl.<Entidad>.<flags>[.<n>]` (decisión 26); copia de los `<ace>` de `Expediente` a cada subclase (decisión 22); eliminar `EducaFlowAuthResolverImpl` y `GestorNotificacionesUtil`; sustituir los `auth-*.xml` escritos a mano (`common`, `expedientes`, `firmas`, `notificaciones`, `security`, `sistemaeducativo`, `smoketest`, `gestioncentro`, `ventanilla`) sistema a sistema, **y al final la parte de permisos del `auth.xml` global** (decisión 23), que es la que hoy enlaza todo al grupo `users` y por tanto la última en irse.
   `HistorialEstado` se declara con la condición «existe un expediente legible del que cuelga» (`EXISTS (SELECT e FROM Expediente e WHERE e = self.expediente AND …)` con las mismas subconsultas que `Expediente`), cerrando el pendiente que anota el propio `auth-expedientes.xml`.
   **No se intenta reproducir lo que hay**: los `auth-*.xml` actuales están mal y no sirven de referencia.
   Cada bloque `<entities>` se escribe de nuevo desde lo que debe ser, y la **fuente de verdad son los tests que ya existen** (unitarios y suite E2E): un sistema está migrado cuando se ha borrado su `auth-*.xml` y los tests siguen en verde.
   `auth-expedientes.xml` es el delicado: sus `condition` consultan `AceProfile*`; se migra el último.
   **Decisión sobre el endpoint REST automático y las subclases de `Expediente`** (cierra el PENDIENTE de `CLAUDE.md`):
   - Axelor solo comprueba `create`/`write`/`remove` en la capa REST (`Resource.save` y `remove`), nunca en `JPA.save` ni en los repositorios; `Tramitador` guarda con `JPA.save`/`expedienteRepository.save` y los botones de evento mandan los datos del form en el contexto de la acción, así que el motor **no pasa por esa puerta**.
   - Por eso el generador de EFBT por tipo de expediente (`createdatainittipoexpediente`) genera para cada subclase de `Expediente` un `<entities>` con **solo `canRead`** y su `condition`; nunca `canCreate`, `canWrite` ni `canRemove`.
     Así `POST /ws/rest/<Subclase>` y el `remove` devuelven 403 antes de tocar nada, y desaparece la duda del alta con el mapa vacío: no hay alta.
     Un expediente solo cambia por un evento, y el evento lo valida `Tramitador` sobre el expediente concreto.
   - `Expediente` en `subsystem/expedientes/security/acl.xml` también es solo `read` para quien no sea administrador, con las condiciones de hoy (`Expediente.creador`, `Expediente.porGlobal`… de `auth-expedientes.xml`, una por `<ace>` con su `condition`); las subclases reciben esas mismas `<ace>` por copia del generador (decisión 22), no por herencia.
   - **MUST NOT** volver a un `ModelService` con deny-all: si no se puede guardar por REST no hay campos que tapar.
   - Se decide en el generador, no tipo a tipo, para que un trámite nuevo nazca cerrado.
   - Antes de darlo por bueno, verificar en modo `warn` con la suite E2E:
     - que ningún form de expediente usa el guardado estándar; poner `canSave="false"` explícito en el form plantilla de `template-views.xml` para que quede escrito (los grids de la plantilla ya lo llevan);
     - que ninguna pantalla de administración (p. ej. «Tramitación → Todos los centros») edita expedientes por REST; si alguna lo hace, se convierte en acción.
   - Al implementarlo, quitar la sección «PENDIENTE (importante)» de `CLAUDE.md` y dejar la regla en `k-secure-coding`.
10. **Destinatarios derivados** `<tramitador/>` y `<unidadTramitadora>` (XSD, columnas de `AceRecurso`, `DestinatarioEvaluator`).
11. **Migrar el resto de sistemas y los recursos fuera de sistema** según la tabla de [Ficheros de seguridad a crear](#ficheros-de-seguridad-a-crear), hasta que los tests de cobertura (lanzados a mano) pasen; las acciones de los trámites las genera EFBT por tipo con `<autenticado/>`.
12. **Menús** (§5): EFBT inyecta `groups` y el `if` con `isVisible(action)` / `isAnyVisible(...)`, `MenuSecurityServiceImpl` consulta `AclService` y el `switch` desaparece entero.
    Es el primer momento en que se toca el código de los menús (decisión 18).
13. **Pasar a `enforce`**, revisar el log de `warn` por si quedaba algo, ejecutar la suite E2E, habilitar los tests de cobertura y quitar el modo `warn` y la propiedad.
14. **Documentación**: skill nuevo `k-acl` (o sección en `k-secure-coding`), `subsystem/security/CLAUDE.md`, y referencias desde `k-sistemas`/`k-vistas` (al crear un controlador o una acción XML con nombre **MUST** declararse su `<ace>`; antes de repetir una lista de destinatarios, mirar si ya hay un colectivo o crear uno).
    Revisar `k-vistas`/`view-rules.md`: los `groups`/`showIf` de botones y menús dejan de ser el criterio de acceso, y `groups` ya no se escribe en `menus.xml`.

## Pendiente (fuera de este plan)

- **IDOR / multicentro**: la ACL dice quién puede *llamar* a un método, no sobre qué registro.
  - En **acciones**, con `tipoUsuario`/`cargo` «en algún centro», la secretaria del centro A pasa el filtro de la acción con un registro del centro B, así que cada método/servicio **MUST** seguir comprobando el centro del registro (`k-secure-coding`) hasta que se decida.
  - En **entidades**, el filtro por centro de oficio (decisión 25) solo cierra la parte multicentro: ser del centro no da acceso a todo lo del centro.
    Quién puede ver **qué registro** de su centro (el interesado su expediente, el destinatario su notificación, el firmante su tarea) sigue siendo la `condition` propia de cada `<ace>` (decisión 21), escrita a mano entidad a entidad en la fase 9; no hay todavía una forma declarativa de expresar esa relación, y es lo que queda pendiente.
- **Concesiones a usuarios concretos** (DNI, login): tabla de tiempo de ejecución con su pantalla, que nunca se vacía al arrancar.
- **Endpoint REST automático** (`/ws/rest/<FQN>`) para el resto de entidades: lo cubre solo el `Permission` de entidad generado desde `<entities>` más el `AllowProperties` del `ModelService` de cada una (`k-secure-coding`).
  El caso de las subclases de `Expediente` ya está decidido en la fase 9 (solo `read`).
- **Otros destinatarios** (perfil de trámite…).
- **Colectivos locales o anidados**: descartados de momento (ver [Colectivos](#colectivos)); se reabrirá solo si aparece la necesidad.

## Verificado en AOP (referencia para implementar)

Todo lo de esta sección se ha comprobado leyendo el código de `../axelor-open-platform` (y, donde se indica, el del proyecto o la BD de desarrollo).
Al implementar **no hace falta volver a buscarlo**: aquí está el fichero, el método y la línea.

### Ejecución de acciones y eventos

| Qué | Dónde | Lo que hace |
|---|---|---|
| Punto común de toda acción con nombre | `axelor-core/.../meta/schema/actions/Action.java:81-95` (`execute`) | si `getName()` está en blanco solo `evaluate`; si no: `checkPermission(handler)` → `handler.firePreEvent(getName())` → `evaluate` → `firePostEvent` → `event.getResult()`. `checkPermission` (l. 97) es `handler.checkPermission(CAN_READ, getModel())`. |
| `wrap` | `Action.java:101-104` | `execute(handler)` y envuelve el resultado; es lo que llama `ActionGroup` por cada hija. |
| Entrada de `POST /ws/action` | `axelor-core/.../meta/ActionHandler.java:474-500` (`execute`) | parte `request.getAction()` por comas, crea un `ActionGroup` **sin nombre** (así que el grupo ad hoc no dispara evento) y llama `action.wrap(this)`. **No hay `try/catch`**: una excepción del observador sube hasta `ResponseInterceptor`. |
| Resolución de un nombre | `axelor-core/.../meta/schema/actions/ActionGroup.java:91-150` (`findAction`) | con `:`: si `XMLViews.isViewType(parts[0])` (`XMLViews.java:178`) construye un `ActionView` ad hoc y le pone **`setName(actionName)`** (l. 110), así que **sí dispara `PreAction`** con el nombre `form:vista:Modelo`; si no, `ActionMethod` ad hoc con `Call(controller=parts[0], method=parts[1])` y **`setName(actionName)`** (l. 136): también dispara `PreAction`, con nombre `Ctrl:metodo` o `Ctrl:metodo(args)`. Sin `:`: `MetaStore.getAction(name)`. |
| Iteración del grupo | `ActionGroup.java:213-227` (`evaluate`) | por cada hija `findAction(name)` → `element.test(handler)` (su `if`) → `action.wrap(handler)`: cada hija pasa por `Action.execute` y dispara su propio `PreAction`. |
| Invocación del método | `ActionHandler.java:224-236` (`call`) y `238-…` (`rpc`) | `call` hace `klass.getMethod(method, ActionRequest.class, ActionResponse.class)` + `Beans.get(klass)` + `invoke`, **sin mirar `@CallMethod`**; captura la excepción del método y hace `response.setException(e)`. `rpc` (forma `metodo(args)`) busca por nombre entre `getMethods()`. `ActionMethod.java:62-63` decide: `isRpc(call.getMethod())` → `rpc`, si no `call`. |
| `ActionMethod.Call` | `ActionMethod.java:17-32` | `getController()` (FQN de la clase) y `getMethod()` (nombre, con `(args)` si es rpc). Es lo que el observador convierte al nombre `Ctrl.metodo` de la tabla. |
| Disparo de eventos | `ActionHandler.java:155-163` | `preActionEvent.select(NamedLiteral.of(name)).fire(new PreAction(name, context))`; igual `PostAction` con el resultado. `PreAction extends ActionEvent` (`axelor-core/.../events/PreAction.java:9-11`), constructor `(String name, Context context)`. |
| Observador sin `@Named` recibe todo | `axelor-core/.../event/Observer.java:77-83` (`hasQualifiers`) | «always match observers with no qualifiers»: si el observador no tiene qualifiers devuelve `true`. |
| Cómo se descubren los observadores | `axelor-core/.../event/EventBus.java:49-58` (`findObservers`) | recorre **`injector.getAllBindings()`** (saltando `LinkedKeyBinding`) y busca métodos `@Observes` en cada clase enlazada (`getAllObservers`, l. 65-83, incluye superclases). Por eso `AclActionObserver` necesita `bind(AclActionObserver.class)` en `SecurityModule`; sin binding no existe. |
| `MetaStore.getAction` | `axelor-core/.../meta/MetaStore.java:79-89` | `ACTIONS.get(name)`; devuelve `null` si no existe o si su `moduleToCheck` no está instalado. |
| Módulo de una acción | `axelor-core/.../meta/loader/ViewLoader.java:347,410,544,627` (`entity.setModule(module.getName())`) | el `module` de `MetaAction`/`MetaView`/`MetaMenu` es el nombre del módulo Axelor que lo cargó. En la BD de desarrollo (`SELECT module, count(*) FROM meta_action GROUP BY module`): **`secretaria-virtual`** (335) y `axelor-core` (71). El nombre viene de `rootProject.name` de `settings.gradle:54`. |
| Cómo llega al cliente una excepción del observador | `axelor-core/.../rpc/ResponseInterceptor.java:64-110` (`onException`) | mira la excepción, su causa y la raíz: si es `org.apache.shiro.authz.AuthorizationException` → `onAuthorizationException` (l. 110): `title = I18n.get("Access error")`, `status = STATUS_FAILURE`, log de una línea (`Authorization Error: …`) sin traza; si es `AuthSecurityException` → `onAuthSecurityException` (l. 134); cualquier otra → `response.setException(throwable)` **y `log.error` con la traza completa**. **Decisión concreta para la 17**: `AclDeniedException extends org.apache.shiro.authz.UnauthorizedException` (subclase de `AuthorizationException`): el cliente recibe el diálogo «Access error» con el mensaje y el log no se llena de trazas. También hace rollback de la transacción (l. 70-72). |

### Seguridad de entidades (`Permission`)

| Qué | Dónde | Lo que hace |
|---|---|---|
| `isPermitted` | `axelor-core/.../auth/AuthSecurity.java:155-184` | `user == null` → `true`; `resolvePermissions` vacío → **`false`** (prohibido por defecto); un `Permission` sin `condition` con el flag del tipo → `true`; sin `ids` (p. ej. `create`) → `true`; con `ids`, construye el filtro y cuenta. |
| `getFilter` | `AuthSecurity.java:116-153` | une las `condition` de todos los `Permission` aplicables con **`Filter.or`** y las ANDea con `id IN (ids)`. |
| `Condition` (cómo se evalúa `condition`) | `AuthSecurity.java:29-50` | **`condition` es JPQL con `?` posicionales** (`new JPQLFilter(condition, args)`); `conditionParams` se parte por comas y cada parámetro es `__user__` (el `User`) o una expresión Groovy evaluada con `__user__` en el binding (`eval`, l. 52-59). `__user__` dentro del texto de la `condition` **no se sustituye**. |
| Resolución de permisos (fork del proyecto) | `AuthSecurity.java:71-72` y `199-206` (`resolvePermissions`) | AOP está parcheado: primero `EducaFlowAuthResolverRegistry.get()`; si devuelve `Optional` vacío cae al `AuthResolver` estándar. |
| `AuthResolver.resolve` | `axelor-core/.../auth/AuthResolver.java:98-123` | une permisos del usuario, de sus roles, de su grupo y de los roles del grupo; `filterPermissions` (l. 59-71) compara **`Objects.equals(object, permission.getObject())`**: nombre exacto, **no recorre superclases**. |
| `EducaFlowAuthResolverImpl` (proyecto) | `subsystem/security/EducaFlowAuthResolverImpl.java:16-32` | para toda subclase de `Expediente` devuelve los `Permission` de **`Expediente`** resueltos para ese usuario y tipo, y solo si ese conjunto está vacío cae a los de la subclase. Ojo: **no «une»**, sustituye: con el cambio de la fase 9 debe devolver `Optional.empty()` para las subclases (o borrarse el resolver). Se registra en `SecurityModule.configure()` (`subsystem/security/module/SecurityModule.java:20`). |
| `isAdmin` | `axelor-core/.../auth/AuthUtils.java:100-103` | `"admin".equals(user.getCode()) || grupo "admins"`. `SecurityUtil.isAdmin` (`base/util/SecurityUtil.java:14`) delega en él, así que «administrador» del plan = administrador de Axelor. En la BD de desarrollo todos los usuarios están en `users` (69) salvo `admin` en `admins` (1); `RegistroServiceImpl.java:25` da de alta en `users`. |
| Dónde comprueba Axelor `create`/`write`/`remove` | `axelor-core/.../rpc/Resource.java:1288-1291` (`save`: `CAN_CREATE` si no hay id, `CAN_WRITE` si lo hay), `1379-1393` (relaciones y `updateMass`), `1434` y `1497` (`remove`), `1536` (`copy`) | **solo en `Resource`**, la capa de `/ws/rest`. `JPA.java` no contiene ninguna llamada a `JpaSecurity`. `ActionRecord` guarda con `JPA.save` (`ActionRecord.java:228`) sin comprobar. |
| Cómo se vacían los `Permission` al arrancar | `base/infrastructure/db/BulkTables.java:12-38` | `truncateTablesByLike("meta\\_%")`, `truncateTablesByLike("auth\\_%")` (menos `tablasExcluidas`) y `truncateTablesByName(tablasIncluidas)`. `DataBaseStartup.java:28-35` define hoy `tablasIncluidas` = las cuatro `security_ace_profile_*` (nombre de tabla: `security_` + snake case de la entidad → `security_ace_recurso`). Lo llama `SecretariaVirtualModule.configure()` (`secretariavirtual/module/SecretariaVirtualModule.java:22`). |
| Cuándo se dispara `StartupEvent` | `axelor-web/.../web/AppStartup.java:58` (`startupEvent.fire(new StartupEvent())`) | después de `ModuleManager.initialize` (`axelor-core/.../meta/loader/ModuleManager.java:77`), que es quien carga los data-init. Ejemplo en el proyecto: `secretariavirtual/startup/AppEventObserver.java:22` (`@Observes StartupEvent`). |

### Menús

| Qué | Dónde | Lo que hace |
|---|---|---|
| `groups` y menús raíz | `axelor-core/.../meta/service/menu/MenuChecker.java:56-70` (`isAllowed`) | `isAdmin(user) || grupo ∈ groups || rol ∈ roles || (sin groups ni roles **y con padre**)`. Es decir: un menú **raíz** sin `groups`/`roles` no lo ve nadie salvo el administrador; un hijo sin ellos sí. Lo usa `MenuService.java:72-73` junto con `canShow` (el `if`). |
| El `if` inyectado hoy | EFBT `viewprocessor/tags/MenuItem.java:15-25` (`doMenuItem`) | aborta si el `<menuitem>` ya trae `if`; inyecta `__config__.menuSecurity.isVisible("<name>")`. `MenuSecurityService` se expone como `context.menuSecurity` (ver `subsystem/security/service/MenuSecurityService.java:10`). |
| El `switch` actual | `subsystem/security/service/impl/MenuSecurityServiceImpl.java:22-40` | las 7 reglas de la tabla de [Menús](#se-puede-migrar-hoy-la-visibilidad-de-los-menús); `default -> true`. |
| Regla del gestor de notificaciones | `subsystem/notificaciones/util/GestorNotificacionesUtil.java:23-26, 33-37` | tipos `SUPERVISOR`, `ADMINISTRATIVO`; cargos `DIRECTOR`, `JEFE_ESTUDIOS`, `SECRETARIO`; **no incluye al administrador** (el colectivo `GESTOR` del plan sí lo añade: es a propósito, para que el admin vea el menú). `idsCentrosGestionados` (l. 39-48) lo invocan por Groovy los `conditionParams` de `auth-notificaciones.xml:39,47,55,63`. |
| Tramitador | `subsystem/security/service/PerfilesUsuarioService.java:54,57` | `isTramitador(User)` e `isTramitador(User, UnidadTramitadoraCodigo)`. |

### JAX-RS y Shiro

| Qué | Dónde | Lo que hace |
|---|---|---|
| Rutas sin sesión | `axelor-core/.../auth/AuthWebModule.java:23-31` | `addFilterChain("/ws/public/**", ANON)`, `/public/**`, `/dist/**`, `/lib/**`, `/img/**`, `/ico/**`, `/css/**`, `/js/**`, `/favicon.ico`. Todo lo demás exige login. |
| Registro de recursos y providers | `axelor-web/.../web/AppContextListener.java:16,70` | `new dev.resteasy.guice.ModuleProcessor(registry, providerFactory)` procesa el `Injector`: toda clase enlazada con `@Path` o `@Provider` se registra. AOP no usa `ResourceInfo` en ningún sitio: el filtro lo inyecta con `@Context ResourceInfo` como manda JAX-RS. |
| Endpoints públicos del proyecto | `subsystem/registroentradasalida/controller/RegistroSalidaController.java:29,39,76` y `subsystem/registrousuario/controller/RegistroController.java:24,46,80,106,134,141` | los de la tabla de [Ficheros de seguridad a crear](#ficheros-de-seguridad-a-crear). No hay controladores Kotlin ni controladores en subpaquetes de `controller` (comprobado con `find`). |

### Ficheros (`MetaFile`)

| Qué | Dónde | Lo que hace |
|---|---|---|
| Descarga por REST | `axelor-web/.../web/service/RestService.java:557-580` (`{id}/{field}/download`, con `QueryParam` `parentId`, `parentModel`, `fileName`, `inline`) → `download(...)` l. 477-510 | si el modelo es `MetaFile`: permitido si `createdBy == usuario actual` **o** `checkMetaFileParentPermission`; si no, exige `isPermitted(CAN_READ, id)` sobre `MetaFile` → `403`. Es la URL que usan los `<iframe>` del proyecto (`ws/rest/com.axelor.meta.db.MetaFile/<id>/content/download`, en `tramites/shared/template-views.xml:114,124,134` y en los `views.xml` de las fases). |
| Permiso por el padre | `RestService.java:589-650` (`checkMetaFileParentPermission`, `checkMetaFileParent`) | con `parentId`+`parentModel`: carga el padre, exige `isPermitted(CAN_READ, parentClass, parentId)` **y** recorre las propiedades del padre para comprobar que el `MetaFile` cuelga de una de ellas (referencia, colección o JSON). Sin ellos: busca un `DMSFile` con ese `metaFile` y permiso para el grupo o el usuario. |
| Estado hoy (BD de desarrollo) | `src/main/resources/data-init/input/auth.xml:8-10,205-208` y `auth_group_permissions` | `MetaFile.all` (`create/read/write/remove/export`, sin condición) está enlazado a `users` y `admins`: **cualquier usuario con sesión descarga cualquier `MetaFile` por su id** (los ids son correlativos), y también `DMSFile.all`, `User.all`, `Persona.all`, `CentroUsuario.all`, `RegistroEntrada.all`, `RegistroSalida.all`… Comprobado por código y por la BD, no con una petición real. |
| Descarga por DMS | `axelor-web/.../web/service/DmsService.java:378-405` (`download/{id}`, `inline/{id}`) | `getAttachmentResponse` resuelve por `DMSFile`; irrelevante mientras la aplicación no use el gestor documental. |

### Estado real de `criptografia`

`subsystem/criptografia/data-init/input/security.xml` (sin commitear) está en un formato que no existe (`<auth><acl><ace allow="read" on="…"><usuario isAdmin="true"/>`) y su `input-config.xml` está **vacío**: hoy no se carga nada de él.
Borrarlo en la fase 5 no quita ningún permiso que exista.

## Flecos resueltos en la revisión

Revisión del plan contra el código el 2026-10-09; las decisiones se han incorporado arriba (decisiones 21-23, §Bloques, §Uso, §Recursos fuera de sistema, §6, §Ficheros a crear y fase 9).
Queda aquí el porqué de cada una.

1. **Condiciones «sobre el propio registro»** → decisión 21: `condition` y `conditionParams` en el `<ace>` de `<entities>`.
   Casi todos los permisos condicionales de hoy son por relación del usuario con el registro (firmante, destinatario, interesado, centro supervisado), no por tipo de usuario o cargo, y sin esto no se podían migrar `firmas`, `notificaciones`, `security` ni `expedientes`.
2. **Subclases de `Expediente`** → decisión 22: el generador copia los `<ace>` de `Expediente` (solo `canRead`) a cada subclase.
3. **Entidades de AOP y el `auth.xml` global** → decisión 23.
   Lo que ha salido al mirarlo:
   - El plan no mencionaba `src/main/resources/data-init/input/auth.xml`, que es el fichero que enlaza al grupo `users` los `Permission` de todos los sistemas y el que concede sin condición `MetaFile.all`, `DMSFile.all`, `User.all`, `Persona.all`, `CentroUsuario.all`, `RegistroEntrada.all`, `RegistroSalida.all`…
     Como Axelor une los `Permission` con OR, muchas condiciones de los `auth-*.xml` de los sistemas hoy no restringen nada (p. ej. `CentroUsuario.propio` convive con `CentroUsuario.all`).
   - **Sí: cualquier usuario con sesión puede descargar cualquier `MetaFile` por su id** (ver [Ficheros](#ficheros-metafile)), y los ids son correlativos.
     Es un problema grave y real hoy; se cierra en la fase 9 quitando el `read` general de `MetaFile` y pasando `parentId`/`parentModel` en las URL de descarga, que es el mecanismo de Axelor para «puedes ver el fichero si puedes ver el registro del que cuelga».
   - `DMSFile` es el gestor documental de Axelor (el clip de «adjuntos» de cualquier form, que guarda un `DMSFile` por cada `MetaFile` adjuntado con sus propios permisos por usuario/grupo).
     La aplicación no lo usa (todos los `canAttach` son `false`), así que no recibe permisos.
   - `Tramite.supervisor` es un `Permission` de `auth-security.xml` que da `read` sobre `Tramite` a los supervisores solo para que el selector de trámite de «Mi centro → Perfiles de trámites» no salga vacío; como `Tramite` es de `expedientes`, se reescribe en `subsystem/expedientes/security/acl.xml`.
4. **Modo `warn`**: es global y solo avisa; el criterio de «sistema revisado» del plan hablaba de ponerlo en `enforce` sistema a sistema, cosa que no existe. Corregido en §6 y en §Ficheros a crear.
5. **Destinatario** = quién puede ejecutar el recurso (los hijos del `<ace>`: `<admin/>`, `<autenticado/>`, `<tipoUsuario>`…).
   El plan listaba `DefaultModelController` pero no decía quién puede llamarlo; ahora dice `<autenticado/>`.
6. **Sobrecargas**: EFBT aborta. AOP las tolera a medias (`call` por firma exacta, `rpc` por nombre y argumentos), pero la clave `Ctrl.metodo` no las distingue.
7. **`AclDeniedException`** hereda de `UnauthorizedException` de Shiro (ver [Verificado en AOP](#ejecución-de-acciones-y-eventos)).
8. **`EducaFlowAuthResolverImpl`** sustituía, no unía; con la decisión 22 se elimina (corregido en §Uso y fase 9).
9. **`HistorialEstado`**: condición por el expediente del que cuelga (fase 9).
10. **La `condition` de los `Permission`, pieza a pieza** → decisiones 24-26 y [Condición JPQL de los permisos de entidad](#condición-jpql-de-los-permisos-de-entidad).
    Lo que ha salido al definirla contra el código:
    - AOP enlaza los `?` **por posición** y renumera todos los del texto (`Filter.build`), así que un `__user__` de `conditionParams` no se puede reutilizar desde dos `?` ni escribir `?1`.
      Por eso la parte generada se anclaba a **un solo `?`**: un único `EXISTS` sobre `CentroUsuario` que fija a la vez usuario, centro, tipos y cargos; la `condition` propia del `<ace>` trae sus propios `?` detrás.
    - El administrador ya se salta los `Permission` (`AuthSecurity.getUser()` devuelve `null` si `isAdmin`), así que el «admin OR filtro por centro» no hay que escribirlo y `<admin/>` no genera fragmento.
    - Con el centro en el ancla, la condición genérica basada en `AceRecurso` del plan anterior dejó de tener sentido: habría que haberle añadido igualmente `self.<centro>` por entidad, y las filas `ENTITY` no las leía nadie más.
      La tabla queda solo para métodos y acciones.
    - El filtro por centro **no cierra el IDOR en entidades**: solo garantiza que el registro es de un centro del usuario.
      Que un alumno vea solo su expediente o un usuario solo sus notificaciones sigue en la `condition` propia de cada `<ace>`, y sigue en Pendiente.
    - `Permission` lleva los seis flags y una sola `condition`, así que es un `Permission` por `<ace>`, no por permiso; de ahí el nombre `acl.<Entidad>.<flags>[.<n>]`.
    - La propiedad de `Cargo` se llama `code` y la de `TipoUsuario` `codigo`; las dos son enums y se comparan con el literal, como ya hacen los `auth-*.xml` de hoy.
    - Hoy ninguna entidad tiene dos `many-to-one` a `Centro` (`AsistenteNuevoExpediente` tiene uno más un `many-to-many`, que no cuenta): `propiedadCentro` nace sin uso.

Sigue **abierto** y se decide con el log de `warn` de la fase 9: si algún form usa de verdad adjuntos DMS o si la subida de ficheros necesita algo más que `create` sobre `MetaFile`.
