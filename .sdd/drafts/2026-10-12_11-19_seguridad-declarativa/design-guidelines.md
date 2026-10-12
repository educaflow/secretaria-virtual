---
type: design-guidelines
---

- **Diseño ya decidido por el usuario.** Esta iniciativa no se diseña desde cero: el usuario ha fijado la solución técnica completa, que se recoge íntegra en este fichero. El diseñador **MUST** seguirla como fuente y limitarse a detallarla, no a sustituirla. Las desviaciones acordadas en la conversación de la spec están marcadas con **[Decisión spec]**.

- **Objetivo técnico.** Declarar en XML **quién** puede ejecutar cada método de controlador, cada acción XML con nombre (y por tanto ver cada menú) y operar sobre cada entidad, con un único formato y un único sitio por sistema. Lo declarado sobre métodos y acciones se carga en la tabla `AceRecurso`; lo declarado sobre entidades se convierte en `Permission` de Axelor cuya `condition` JPQL escribe EFBT a partir de los destinatarios. Lo que no esté declarado está **prohibido**.

- **Por qué hace falta (verificado en AOP).**
  - AOP no tiene permisos por rol sobre acciones ni sobre métodos de controlador. `Action.execute` solo comprueba `READ` sobre el `model` de la acción (`Action.checkPermission`); un `action-method` sin `model` no comprueba nada.
  - `POST /ws/action` ejecuta cualquier nombre que mande el cliente: el nombre de una acción XML aunque su botón esté oculto (`showIf`/`hidden`/`groups` no son defensa); `com.x.MiController:metodo`, que `ActionGroup` convierte en un `ActionMethod` al vuelo y `ActionHandler.call` invoca **sin exigir `@CallMethod`** (solo la forma `metodo(args)` de `rpc` lo exige); `form:vista:Modelo`, que construye un `action-view` al vuelo. `call:Ctrl:metodo` dentro de expresiones también crea un `ActionMethod` con nombre.
  - Los controladores también pueden publicar **endpoints JAX-RS**, que no pasan por `Action.execute` y que Shiro deja entrar **sin sesión** si cuelgan de `/ws/public/**`. Hoy hay dos: `RegistroSalidaController` (`subsystem/registroentradasalida`): `getDescargaByCsv` (`/ws/public/registro-salida/download`, al que lleva el QR) y `validateGetDescargaByCsv` (`/validate-download`); `RegistroController` (`subsystem/registrousuario`, `@Path("/public/registro")`): `index`, `registro.css`, `registro.js`, `registrosPendientes` (POST), `validarCodigo` y `usuarios`.
  - En entidades, el `auth.xml` global concede hoy sin condición `MetaFile.all`, `DMSFile.all`, `User.all`, `Persona.all`, `CentroUsuario.all`, `RegistroEntrada.all`, `RegistroSalida.all`… a todos los usuarios. Como Axelor une los `Permission` con OR, muchas condiciones de los `auth-*.xml` no restringen nada.
  - El punto común de las acciones es `Action.execute`: `checkPermission → firePreEvent(nombre) → evaluate → firePostEvent(nombre, resultado) → event.getResult()`. Un observador `@Observes PreAction` **sin `@Named`** recibe los eventos de todas las acciones: ese es el filtro.

- **Resumen de la arquitectura.** La seguridad se escribe a mano en XML (`security/*.xml` de cada sistema) y nunca se lee tal cual en tiempo de ejecución. EFBT la **valida** y la **transforma** en el build:
  - Métodos de controlador (`<actionMethods>`) → data-init de `AceRecurso` → tabla `AceRecurso` → evaluada por `subsystem/security` (Java), con caché.
  - Acciones XML con nombre (`<actions>`), y por tanto menús → data-init de `AceRecurso` → igual.
  - Entidades (`<entities>`) → `<permission>` de Axelor con su `condition` JPQL y su `bind` al grupo `users` → tabla `auth_permission` → evaluada por AOP (`AuthSecurity`), sin código nuestro.
  - Flujo: `security/*.xml` → EFBT `generateSecurity` (tarea `JavaExec`, antes de `processResources`): 1) VALIDAR (XSD + reglas semánticas), 2) TRANSFORMAR (colectivos → recursos concretos → comodines → salidas): `AceRecurso.xml` (data-init) → BD → `AclService.puedeEjecutar(user, recurso)` usado por `AclActionObserver` (`POST /ws/action`), `AclRestFilter` (endpoints JAX-RS) y `MenuSecurityService` (el `if` de los menús); `<permission>` + `<group users>` → `auth_permission` → AOP; `menus.xml` con `if="isVisible(...)/isAnyVisible(...)"` + `groups="admins,users"` inyectados.
  - Tres partes: **A** el formato XML (lo que escribe el autor), **B** EFBT (validar y transformar), **C** `subsystem/security` (leer `AceRecurso`, cachearla y responder «¿puede este usuario ejecutar este recurso?»), más **D** la migración de los sistemas actuales y el plan de trabajo.

- **Principios.**
  1. **Una fuente**: el XML de `security/` es la única fuente de permisos de métodos, acciones, menús y entidades. Los `auth-*.xml` escritos a mano y la parte de permisos del `auth.xml` global desaparecen (**[Decisión spec]** salvo `MetaFile`/`DMSFile`, ver más abajo).
  2. **Prohibido por defecto.**
  3. **En tiempo de ejecución no hay comodines ni colectivos**: EFBT los expande; la tabla y los `Permission` solo tienen recursos concretos y destinatarios primitivos.
  4. **Un destinatario (`<usuario>`) por fila; varias filas son O.** No hay Y entre destinatarios. Lo que en el futuro se añada a un `<ace>` que no sea «qué es el usuario» (condiciones sobre el registro) irá en otros hijos y se combinará con Y sobre ese O.
  5. **La ACL dice quién puede *llamar*, no sobre qué registro** (salvo el suelo de centro en entidades): el IDOR sigue siendo responsabilidad del método/servicio y del motor de tramitación (`k-secure-coding`).
  6. **Todo lo de ejecución va en `subsystem/security`** (dominio, repositorio, servicio, evaluador, observador, filtro, menús, colectivos, tests). Fuera solo: el XSD y la generación (EFBT), los `security/*.xml` de cada sistema y el `if` que EFBT inyecta en los menús. Nada va a `base/infrastructure`, a `tramitador` ni a `common`.
  7. **Migración sin romper**: modo `warn` global mientras se migra; los menús no se tocan hasta el final.
  8. **Los `security/*.xml` los escribe Claude**, sistema a sistema, y el usuario los revisa. **[Decisión spec]** El detalle de qué puede hacer cada rol sobre cada entidad no está en la spec a propósito: se decide aquí, fichero a fichero, con revisión del usuario; los `auth-*.xml` actuales sirven solo como lista de entidades, no como criterio.
  9. El trámite `prueba` se borra antes y no se tiene en cuenta (con él desaparece `FormacionCentroTrabajo`).
  10. Sin DNI ni usuarios concretos en el XML: las concesiones a personas son de tiempo de ejecución y quedan fuera.

- **Parte A.1 — Ficheros.** Namespace `http://educaflow.com/xml/ns/security`, raíz `<security>`. En cualquier carpeta `security/` de `src/main/java` (normalmente `<sistema>/security/`), **con cualquier nombre**: se leen todos los del namespace. EFBT los busca por namespace en **cualquier** carpeta de `src/main/java`, no solo en las `security/`, para que uno mal colocado no se ignore en silencio: estar en una carpeta `security/` es una regla de arquitectura que EFBT comprueba (V-17). Se puede repetir un bloque y mezclar tipos en un fichero, o tener un fichero por tipo. Sin `xsi:schemaLocation`: el XSD vive solo en EFBT (como `TramiteInstance.xml`). Los colectivos **no** van en estos ficheros: van en el fichero de configuración de la seguridad (A.4), con su propio namespace, XSD y raíz.

- **Parte A.2 — Bloques y flags.** Un bloque por tipo de recurso, para que el XSD valide cada `<ace>` con su propia definición. Los permisos son atributos booleanos, como en el `Permission` de Axelor; se añade `canExecute`.
  - `<actionMethods>`: `object` = `Controlador.metodo`, relativo al paquete `controller` del sistema (métodos de acción y endpoints JAX-RS); flag solo `canExecute` (solo admite `true`); si falta, EFBT pone `canExecute="true"`.
  - `<actions>`: `object` = nombre **entero** de cualquier acción XML con nombre (`action-view`, `action-record`, `action-attrs`, `action-group`, `action-script`, `action-export`, `action-report`…) del sistema; **no** `action-method`; flag solo `canExecute`; si falta, EFBT lo pone.
  - `<entities>`: `object` = nombre de la entidad, relativo al paquete `db` del sistema; flags `canRead`, `canWrite`, `canCreate`, `canRemove`, `canExport`, `canImport` (el que falta es `false`); si no hay ninguno a `true`, error.
  - Reglas de `<actionMethods>` y `<actions>`: para que alguien no ejecute algo no se le declara; no existe `canExecute="false"`. **`action-method` no se declara en `<actions>`**: su permiso es el del método al que llama (asegurar el nombre de la acción no protegería nada, porque `POST /ws/action` acepta `…Controller:metodo` sin pasar por ese nombre). Todas las acciones con nombre se bloquean igual, también las de dentro de un `action-group` (cada hija pasa por `Action.execute`), así que declarar el grupo **no** cubre a sus hijas.
  - Reglas de `<entities>`: atributos opcionales **`condition`** y **`conditionParams`**, con la sintaxis del `Permission` de Axelor (JPQL sobre `self`, `?` posicionales, `conditionParams` separados por comas con `__user__` o expresiones Groovy). Sirven para los permisos «sobre el propio registro» (firmante, destinatario, interesado…). `condition` sin `conditionParams` es válida; `conditionParams` sin `condition` es error. Se aplica a todos los flags y destinatarios del `<ace>`. El autor escribe **solo sus `?` y sus `conditionParams`**: la parte de destinatarios y centro la añade EFBT delante (T-5). Ejemplo:
    ```xml
    <ace object="TareaFirma" canRead="true" canWrite="true" condition="self.firmante = ?" conditionParams="__user__">
      <usuario autenticado="true"/>
    </ace>
    ```
  - Atributo opcional **`propiedadCentro`**: el `many-to-one` a `Centro` por el que se filtra. Obligatorio si la entidad tiene más de uno; error si tiene uno o ninguno, o en un `object` con comodín.
  - Un mismo `object` **puede repetirse** con distintos flags, destinatarios o `condition`; Axelor une los `Permission` con OR. Solo es error repetir `object` con los mismos flags a `true` **y** la misma `condition`/`conditionParams` (hay que fusionar destinatarios). Con una condición distinta por destinatario, `canRead` queda «(PROFESOR del centro y A) o (SUPERVISOR del centro y B)».
  - **Cada flag se evalúa por separado**, solo con los `<ace>` que lo tienen a `true`. Si `canWrite` debe estar acotado como `canRead`, se repite la condición: EFBT no puede saber si una JPQL contiene a otra. Si no se repite, quien solo lee las filas que cumplen la condición pero escribe sin ella puede modificar por REST, por id, filas que no ve en el grid.
  - `canRemove="false"` es lo mismo que omitirlo; se admite para escribir los seis a la vista.

- **Parte A.3 — Destinatarios `<usuario>`.** Cada hijo `<usuario>` de un `<ace>` es **un destinatario y una fila**; varios `<usuario>` son O. Es la **misma forma** que ya usan los `<acl>` de perfiles (`AceProfile*`, `TramiteInstance.xml`, `TipoExpedienteInstance.xml`): `<usuario cargo="DIRECTOR"/>`. Un `<ace>` sin ningún `<usuario>` es error del XSD. **Un solo atributo por `<usuario>`**: el atributo dice el tipo de destinatario y su valor el código; dos atributos es error (EFBT, V-18). Los destinatarios sin código (`admin`, `autenticado`, `todos`) son atributos booleanos `="true"`; `admin="false"` es error del XSD.
  - `<usuario admin="true"/>`: el administrador (`SecurityUtil.isAdmin`). Solo en `<actions>`; en `<actionMethods>` y `<entities>` no existe.
  - `<usuario autenticado="true"/>`: cualquier usuario con sesión. Todos los bloques.
  - `<usuario todos="true"/>`: cualquiera, con o sin sesión. Solo `<actionMethods>`, y solo en endpoints JAX-RS bajo `/ws/public/**`.
  - `<usuario tipoUsuario="…"/>` y `<usuario cargo="…"/>`: ese tipo o cargo (ver multicentro). Todos los bloques.
  - `<usuario perfil="…"/>`: tiene ese perfil (`Profile`) en algún trámite de cualquier unidad tramitadora en cualquier centro (`PerfilesUsuarioService.tienePerfil(user, perfil)`, nuevo). Solo `<actionMethods>` y `<actions>` (derivado).
  - `<usuario unidadTramitadora="…"/>`: tramita algún trámite de esa unidad (`isTramitador(user, codigo)`). Solo `<actionMethods>` y `<actions>` (derivado).
  - `<usuario colectivo="…"/>`: no es destinatario: EFBT lo sustituye por la lista de `<usuario>` del colectivo (A.4); donde quepan todos sus destinatarios.
  - `todos` frente a `autenticado`: fuera de `/ws/public/**` Axelor ya exige sesión, así que `todos` mentiría: EFBT aborta. En un endpoint público cualquier otro destinatario no protegería nada: EFBT aborta. La fila `todos` **documenta** que es público; la defensa real sigue siendo la del endpoint (el CSV).
  - **Derivados** (`perfil`, `unidadTramitadora`): se calculan en Java (recorren `AceProfile*`), no caben en una `condition` JPQL; por eso no se admiten en `<entities>`.
  - **El admin puede todo en `<actionMethods>` y `<entities>`, y `admin` no existe en esos bloques.** Entidades: `AuthSecurity.isPermitted` devuelve `true` para el admin antes de mirar ningún `Permission`. Métodos: `AclService.puedeEjecutarMetodo` devuelve `true` para el admin sin consultar la tabla, y `AclRestFilter` igual. En `<actions>` (y por tanto en los menús) **sí hace falta declararlo**: `AclService.puedeEjecutarAccion` es «prohibido por defecto» y **no exime al admin**, a propósito, porque si lo eximiera el admin vería todos los menús de todos los usuarios. Consecuencias: «solo el admin» en un método o una entidad se escribe **no declarándolo**; un método o una entidad olvidados no se distinguen de uno «solo admin», así que no hay test de cobertura de métodos ni de entidades; el lado seguro es el cerrado, y el olvido lo destapa el log de `warn` o la suite E2E. Bajo un comodín no se puede dejar un método solo al admin con una excepción: hay que estrechar el comodín. Un colectivo con `admin` (`GESTOR`) se puede usar en `<actionMethods>` y `<entities>`: al expandirlo (T-1) EFBT descarta el `admin` sin error.
  - **[Decisión spec]** La exención del admin se extiende también a los expedientes (REQ-032): el admin puede crear, modificar y borrar un `Expediente` directamente; no añadir ninguna defensa extra para él.
  - **Multicentro**: en métodos, acciones y menús `tipoUsuario`/`cargo` se evalúan «en algún centro del usuario». En entidades, «en el centro del registro» si la entidad tiene propiedad `Centro`, y «en algún centro» si no.
  - **Por qué solo O**: con Y habría columnas con nulos, todas las no nulas tendrían que cumplirse sobre el mismo `CentroUsuario`, y una fila toda nula daría acceso a todos. Ninguna regla actual necesita Y.
  - **Gestor de notificaciones**: no es derivado; se escribe con el colectivo `GESTOR`, y el «en qué centros gestiona» lo da el filtro de centro de oficio en `Notificacion`. El usuario normal ve **sus** notificaciones con otro `<ace>` y su `condition` (`self.correo.dniDestinatario = ?` con `__user__.dni`).

- **Parte A.4 — Colectivos.** Un nombre para una lista de `<usuario>` primitivos (no se llama «rol» ni «grupo»). Se usa desde un `<ace>` con `<usuario colectivo="GESTOR"/>`.
  ```xml
  <!-- subsystem/security/security/security-config.xml -->
  <securityConfig xmlns="http://educaflow.com/xml/ns/security-config">
    <colectivos>
      <colectivo nombre="GESTOR">
        <usuario admin="true"/>
        <usuario tipoUsuario="SUPERVISOR"/>
        <usuario tipoUsuario="ADMINISTRATIVO"/>
        <usuario cargo="DIRECTOR"/>
        <usuario cargo="JEFE_ESTUDIOS"/>
        <usuario cargo="SECRETARIO"/>
      </colectivo>
    </colectivos>
  </securityConfig>
  ```
  - Fichero de configuración de la seguridad: namespace `http://educaflow.com/xml/ns/security-config`, raíz `<securityConfig>`, XSD propio `security-config_1.0.xsd` (solo en EFBT). **Hay exactamente uno en todo el proyecto**: `subsystem/security/security/security-config.xml`. Que falte, que haya otro o que esté en otra ruta es error de EFBT. El descubrimiento de los `<security>` lo ignora; EFBT lo lee aparte. Hoy solo contiene `<colectivos>`; lo que en el futuro sea configuración global de la seguridad va aquí.
  - **Globales**: nombre único en todo el proyecto; se usan desde cualquier fichero `<security>` (no son recursos, no entran en la regla de alcance). **Sin anidamiento** (`<usuario colectivo="…"/>` dentro de un colectivo es error del XSD) y **sin colectivos locales**. `<usuario todos="true"/>` no se admite dentro de un colectivo; `perfil`/`unidadTramitadora` sí, pero entonces ese colectivo no se usa en `<entities>`. Un colectivo que no usa nadie es error del test de cobertura. Cambiar la lista de un colectivo cambia, en el siguiente build, las filas de todos los recursos que lo usan. `GESTOR` incluye `admin` a propósito (hoy `GestorNotificacionesUtil` no lo incluye, pero el admin debe ver el menú).

- **Parte A.5 — `object`, alcance y comodines.** **Alcance de un fichero** = el paquete que contiene su carpeta `security/` y sus subpaquetes.
  - Bajo `system/<x>/` o `subsystem/<x>/`: alcance el sistema `<x>`; `<actionMethods>`/`<entities>` relativo (`MiController.metodo`, `MiEntidad`), EFBT completa `<base>.controller.` / `<base>.db.`; no se admite FQN. `<actions>`: nombre entero.
  - En cualquier otra carpeta: alcance el paquete de esa carpeta; nombre completo (`com.educaflow.tramites.util.firma.FirmaClienteController.metodo`).
  - Ejemplo en `subsystem/criptografia/security/acl.xml`: `CertificadoDigitalController.getSituacionFirma` → `com.educaflow.subsystem.criptografia.controller.CertificadoDigitalController.getSituacionFirma`; `subsysCriptografia.Main@CertificadoDigital-action` → el mismo; `CertificadoDigital` → `com.educaflow.subsystem.criptografia.db.CertificadoDigital`.
  - **Comodines**: `*` solo al final y como segmento entero. `<actionMethods>`: `Ctrl.metodo`, `Ctrl.*`, `*`. `<actions>`: nombre entero o `*` (los `.` y `@` de los nombres de acción no son jerarquía). `<entities>`: nombre exacto o `*`. No valen `get*`, `*Controller`, `subsysCriptografia.*`, `subsysCriptografia.Main@*` (lo valida el XSD con `pattern`). Fuera de un sistema se escriben con el nombre completo (`com.educaflow.base.infrastructure.controller.MiController.*`), y `*` abarca todos los recursos del alcance del fichero.
  - **Especificidad: gana el más específico, y sustituye (no suma).** Para cada recurso concreto se aplica **una sola** regla, la del `object` más específico que encaja; las menos específicas se ignoran para ese recurso.
    ```xml
    <ace object="*">                     <!-- regla general del sistema -->
      <usuario autenticado="true"/>
    </ace>
    <ace object="MiController.*">        <!-- excepción para todo un controlador -->
      <usuario tipoUsuario="SUPERVISOR"/>
    </ace>
    <ace object="MiController.borrar">   <!-- excepción dentro de la excepción -->
      <usuario cargo="DIRECTOR"/>
    </ace>
    ```
    `OtroController.listar` → gana `*` → cualquier usuario con sesión; `MiController.ver` → gana `MiController.*` → solo supervisores; `MiController.borrar` → gana `MiController.borrar` → solo el director (no los supervisores). Si se sumaran, `MiController.borrar` lo podría ejecutar cualquiera por la regla `*`. Con la sustitución, leer la regla de un recurso es buscar su `object` más específico.
  - Los `object` que encajan con un recurso son el propio recurso y sus prefijos por segmentos seguidos de `.*`, hasta `*`: forman una cadena, nunca hay empate. Las filas del ganador son **todas** las del recurso. Consecuencia en `<actions>`: si `*` tiene `<usuario admin="true"/>` y una acción solo `<usuario tipoUsuario="SUPERVISOR"/>`, el admin pierde esa acción y su menú salvo que se repita `<usuario admin="true"/>` (deliberado; documentarlo en el skill). En `<actionMethods>` no pasa: el admin está exento. **Un comodín no admite la excepción «solo el admin»**: hay que estrechar el comodín. En `<entities>` el ganador es el **conjunto** de `<ace>` con ese `object`, y sustituye a los menos específicos para **todos** los flags a la vez.

- **Parte A.6 — Recursos que no están en ningún sistema.**
  - `base/infrastructure/controller/*` (`DefaultModelController`), paquete `com.educaflow.base.infrastructure` → `base/infrastructure/security/acl.xml`.
  - `FirmaClienteController`, paquete `com.educaflow.tramites.util.firma` → `tramites/util/firma/security/acl.xml`.
  - Acciones de `tramites/shared/template-views.xml` que no son por tipo (`subsysExpedientes.Expediente-verHistorialEstados-action`…), paquete `com.educaflow.tramites.shared` → `tramites/shared/security/acl.xml`.
  - Entidades de AOP (`User`; **[Decisión spec]** `MetaFile` y `DMSFile` quedan fuera, ver abajo), paquete `com.axelor` → `com/axelor/security/acl.xml`. El proyecto ya tiene el paquete `com.axelor` (`src/main/java/com/axelor/auth/service/`), así que ese fichero tiene alcance `com.axelor` y las nombra con FQN. `User` es de AOP aunque el proyecto la extienda en `subsystem/common/domains/User.xml` (`package="com.axelor.auth.db"`): se declara desde `com/axelor/security/`, no desde `subsystem/common/security/` (V-07).
  - Fuera de un sistema la clase puede estar en cualquier paquete del alcance (sin `controller`/`db` fijo).

- **Parte A.7 — Acciones de los trámites.** Las acciones con nombre de cada tipo de expediente (las de los `views.xml` de sus fases y las generadas desde `template-views.xml`) **no se declaran a mano**: EFBT genera por tipo un `<ace>` en `<actions>` con `<usuario autenticado="true"/>` (T-7). Nadie escribe un `security/` bajo `tramites/<tramite>/`. La defensa real es el motor: `Tramitador` comprueba estado, evento y perfil del usuario sobre el expediente concreto. Los métodos de `subsystem/tramitador` y `subsystem/expedientes` que mueven la máquina de estados se declaran en su `security/` con `<usuario autenticado="true"/>`.

- **Parte A.8 — Ejemplos.**
  ```xml
  <!-- subsystem/notificaciones/security/acl.xml -->
  <security xmlns="http://educaflow.com/xml/ns/security">
    <actionMethods>
      <!-- el resto de NotificacionController no se declara: solo el admin -->
      <ace object="NotificacionController.abrirRecibida" canExecute="true">
        <usuario autenticado="true"/>
      </ace>
      <ace object="NotificacionController.reenviar">
        <usuario tipoUsuario="SUPERVISOR"/>
        <usuario cargo="SECRETARIO"/>
        <usuario cargo="DIRECTOR"/>
      </ace>
    </actionMethods>
    <actions>
      <ace object="subsysNotificaciones.Mis@Notificacion-action">
        <usuario autenticado="true"/>
      </ace>
      <ace object="subsysNotificaciones.Main@Notificacion-action">
        <usuario admin="true"/>
      </ace>
      <ace object="subsysNotificaciones.Centro@Notificacion-action">
        <usuario colectivo="GESTOR"/>
      </ace>
    </actions>
    <entities>
      <ace object="*" canRead="true">
        <usuario autenticado="true"/>
      </ace>
      <ace object="Notificacion" canRead="true" canWrite="true" canCreate="true" canRemove="true">
        <usuario tipoUsuario="SUPERVISOR"/>
        <usuario cargo="SECRETARIO"/>
      </ace>
    </entities>
  </security>
  ```
  ```xml
  <!-- subsystem/registroentradasalida/security/acl.xml (derivado y endpoint público) -->
  <security xmlns="http://educaflow.com/xml/ns/security">
    <actionMethods>
      <ace object="RegistroSalidaController.*">
        <usuario unidadTramitadora="SECRETARIA"/>
        <usuario tipoUsuario="SUPERVISOR"/>
      </ace>
      <!-- público: quien tiene el CSV (el QR) tiene el documento -->
      <ace object="RegistroSalidaController.getDescargaByCsv">
        <usuario todos="true"/>
      </ace>
      <ace object="RegistroSalidaController.validateGetDescargaByCsv">
        <usuario todos="true"/>
      </ace>
    </actionMethods>
    <actions>
      <ace object="*">
        <usuario admin="true"/>
        <usuario unidadTramitadora="SECRETARIA"/>
        <usuario tipoUsuario="SUPERVISOR"/>
      </ace>
    </actions>
    <entities>
      <!-- aquí no cabe unidadTramitadora: es derivado -->
      <ace object="*" canRead="true">
        <usuario tipoUsuario="SUPERVISOR"/>
        <usuario cargo="SECRETARIO"/>
      </ace>
    </entities>
  </security>
  ```
  ```xml
  <!-- system/ventanilla/security/acl.xml (derivados por perfil y por unidad tramitadora) -->
  <security xmlns="http://educaflow.com/xml/ns/security">
    <actions>
      <!-- tiene algún perfil de tramitación (todos salvo CREADOR) en algún trámite -->
      <ace object="sysVentanilla.PendientesDeMi@Expediente-action">
        <usuario admin="true"/>
        <usuario perfil="TRAMITADOR"/>
        <usuario perfil="COLABORADOR"/>
        <usuario perfil="AFECTADO"/>
        <usuario perfil="SECRETARIO"/>
        <usuario perfil="DIRECTOR"/>
        <usuario perfil="AUDITOR"/>
      </ace>
      <ace object="sysVentanilla.SecretariaAbiertos@Expediente-action">
        <usuario unidadTramitadora="SECRETARIA"/>
      </ace>
      <ace object="sysVentanilla.JefaturaAbiertos@Expediente-action">
        <usuario unidadTramitadora="JEFATURA_ESTUDIOS"/>
      </ace>
    </actions>
  </security>
  ```

- **Parte A.9 — Misma forma de destinatario que en `AceProfile*`.** Los `<acl>` de perfiles (`AceProfileGlobal.xml`, `AceProfileTipoUsuarioTramite.xml`, `TramiteInstance.xml`, `TipoExpedienteInstance.xml`) ya escriben el destinatario como `<usuario cargo="DIRECTOR"/>`, con un solo atributo y abortando el build si hay dos (`files/ace/Ace.java` de EFBT). El formato nuevo adopta exactamente esa forma, así que `AceProfile*` **no cambia**: ni ficheros, ni `bind`, ni generadores. Diferencias que siguen: `AceProfile*` solo admite `tipoUsuario` y `cargo`; `admin`, `autenticado`, `todos`, `perfil`, `unidadTramitadora` y `colectivo` son solo de `security/*.xml`. `<tramite tipoUsuario="…"/>` de `AceProfileTipoUsuarioTramite` no es destinatario. La comprobación «un solo atributo» la hace EFBT en Java (V-18), no el XSD (XSD 1.0 no puede exigir «exactamente uno»; el validador del JDK no soporta `xs:assert`).

- **Parte B — EFBT: tarea `generateSecurity`** (`JavaExec`, antes de `processResources`; al cambiar EFBT, `mvn install` en `../EducaFlowBuildTools`). Los XSD `security_1.0.xsd` y `security-config_1.0.xsd` viven solo en EFBT.
  - **B.1 Descubrimiento.** 1) Recorre **todos** los `src/main/java/**/*.xml` y se queda con los del namespace `security`; en el mismo recorrido localiza los del namespace `security-config`, comprueba que hay uno solo (V-11) y lo valida con su XSD. 2) Por la ruta decide el alcance: si entre `com/educaflow/` y `security/` hay `system/<x>` o `subsystem/<x>`, es ese sistema; si no, el paquete de la carpeta. 3) Enumera los **recursos concretos** de cada alcance: controladores leídos **del código fuente con el compilador del JDK** (`javax.tools` / Compiler Tree API, como los tests de `ordenmetodos`; la tarea va antes de compilar): métodos públicos `(ActionRequest, ActionResponse)` (todos invocables, con o sin `@CallMethod`) y endpoints JAX-RS (`@GET`/`@POST`/`@PUT`/`@DELETE` en clase con `@Path`) con su ruta completa; acciones con nombre de todos los XML de vistas Axelor (`object-views`) del alcance (`views/`, `controller/*.xml`, `views.xml` de fases…), salvo `action-method`; las acciones que EFBT genera por tipo de expediente desde `template-views.xml`; entidades de los `domains/*.xml`, con sus `many-to-one` a `Centro` (y los heredados de `Expediente`); en el alcance `com.axelor`, además, las entidades de AOP (clases del classpath que heredan de `Model`) con los campos que les añadan los `domains/*.xml`; las acciones de `menus.xml` y el árbol de menús (para T-8).
  - **B.2 Validaciones** (primero sobre los XML originales; las marcadas † tras expandir colectivos, y el error nombra el colectivo):
    - V-01 (XSD): bloques, tipo de `<ace>` por bloque, flags admitidos, `canExecute` solo `true`, `pattern` de comodines, `<ace>` sin `<usuario>` (en los tres bloques), atributos de `<usuario>` admitidos en cada bloque (`admin` solo en `<actions>`; `todos`, `perfil` y `unidadTramitadora` no en `<entities>`), booleanos solo `true`, `conditionParams` sin `condition`; el de configuración cumple `security-config_1.0.xsd` (el `<usuario>` de un colectivo no admite `todos` ni `colectivo`).
    - V-02: un fichero fuera de un sistema no tiene ningún `system/<x>`/`subsystem/<x>` por debajo.
    - V-03: las carpetas `security/` de fuera de sistemas no se anidan.
    - V-04: no hay `security/` bajo `tramites/<tramite>/`.
    - V-05: ningún `object` sale de su alcance: en sistema la clase está **directamente** en `<base>.controller` (sin subpaquetes; aborta si encuentra un controlador en uno) o es una entidad de los `domains` del sistema cuyo `package` es `<base>.db`; la acción está declarada en un XML de vistas del alcance (no basta el prefijo); fuera de sistema el FQN empieza por el paquete de la carpeta.
    - V-06: todo `object` exacto existe (método, acción, entidad).
    - V-07: una entidad de un `domains/*.xml` de un sistema cuyo `package` no es `<base>.db` (p. ej. `User`) no se declara desde ese sistema, solo desde el alcance de su paquete.
    - V-08: ningún `<ace>` de `<actions>` nombra un `action-method`.
    - V-09: `object` duplicado en el mismo alcance y bloque (en todos sus ficheros); en `<entities>` solo si coinciden flags a `true`, `condition` y `conditionParams`.
    - V-10: ningún controlador tiene dos métodos invocables con el mismo nombre (sobrecarga).
    - V-11: exactamente un fichero del namespace `security-config` y está en `subsystem/security/security/security-config.xml`; nombre de colectivo único; todo `<usuario colectivo="…"/>` usado existe.
    - V-12 †: ningún derivado llega a `<entities>` a través de un colectivo.
    - V-13 †: `<usuario todos="true"/>` solo en endpoints JAX-RS bajo `/ws/public/**`, y en esos es el único destinatario.
    - V-14: un `<ace>` de `<entities>` tiene al menos un flag a `true`.
    - V-15: `propiedadCentro` obligatorio con dos o más `many-to-one` a `Centro`; error con uno o ninguno, si no existe o no es `many-to-one` a `Centro`, o en un `object` con comodín; aborta si un comodín gana una entidad con dos.
    - V-16 (tras T-5): para una entidad y un flag no se generan a la vez un `Permission` sin `condition` y otro con `condition` (AOP los trata distinto por id y en el grid); nombra los `<ace>` implicados. Solo puede pasar en una entidad sin propiedad `Centro`, con un `<usuario autenticado="true"/>` sin `condition` propia.
    - V-17: todo fichero del namespace `security` está directamente en una carpeta `security/`; si no, error con su ruta.
    - V-18: cada `<usuario>` (en `security/*.xml` y en los colectivos) tiene **exactamente un** atributo; con cero o dos, error que nombra fichero y `object`.
  - **B.3 Transformaciones**, en este orden:
    - **T-1 Expandir colectivos.** Sustituye cada `<usuario colectivo="…"/>` por sus destinatarios, **antes** de los comodines; un destinatario repetido se deduplica en silencio. En `<actionMethods>` y `<entities>` el `admin` del colectivo se descarta en silencio; si tras descartarlo el `<ace>` se queda sin `<usuario>`, error que nombra el colectivo.
    - **T-2 Completar `canExecute`** en `<actionMethods>`/`<actions>` cuando falta.
    - **T-3 Expandir comodines** por cada recurso concreto de cada alcance: 1) busca un `<ace>` con `object` igual al recurso; 2) si no, quita el último segmento y busca `<prefijo>.*`, subiendo; 3) si no, `*`; 4) el primero que encuentra gana y genera las filas del recurso (una por destinatario y permiso); 5) sin ninguno, el recurso no tiene filas: prohibido (en métodos y entidades, prohibido para todos menos el admin). Un `Ctrl.*` o `*` que caiga sobre un endpoint público **no le da filas**: cada ruta sin login tiene que estar escrita a la vista con `<usuario todos="true"/>`.
    - **T-4 Generar el data-init de `AceRecurso`** (`AceRecurso.xml` + `input-config.xml`) con las filas de métodos y acciones ya expandidas, en el data-init que copia el build. Las FK (`TipoUsuario`, `Cargo`, `UnidadTramitadora`) se enlazan por `search` sobre su código, como en `AceProfile*`; `perfil` es un enum y va por su valor.
    - **T-5 Generar los `Permission` de entidades**, con su `<bind node="group">` al grupo `users`. **Uno por `<ace>` ganador y entidad** (un `Permission` lleva los seis flags y una sola `condition`), con los flags que el `<ace>` tenga a `true`; `object` = FQN de la entidad. **Nombre** `acl.<Entidad>.<flags>[.<n>]`: `<Entidad>` nombre simple; `<flags>` en orden canónico `read-write-create-remove-export-import`, sin `can`, en minúsculas, unidos por `-`; `.<n>` (desde `2`) cuando dos `<ace>` ganadores de la misma entidad tienen los mismos flags, por orden de fichero (alfabético) y de documento; ejemplos: `acl.TareaFirma.read-write`, `acl.Expediente.read.2`; el prefijo `acl.` los separa de los `Permission` escritos a mano que sigan vivos durante la migración (`SELECT name FROM auth_permission WHERE name LIKE 'acl.%'`). **`condition` = ancla + fragmentos + condición propia**, con **un único `?`** para el usuario:
      ```
      EXISTS (SELECT cu FROM com.educaflow.subsystem.common.db.CentroUsuario cu
              WHERE cu.usuario = ?
                AND cu.centro = self.<propiedadCentro>        ← solo si la entidad tiene propiedad Centro
                AND (<fragmento 1> OR <fragmento 2> OR …))    ← solo si hay tipoUsuario o cargo
      ```
      Fragmentos: `<usuario autenticado="true"/>` → ninguno (basta el ancla); si convive con `tipoUsuario`/`cargo` se omite el paréntesis; sin `Centro` y sin `condition` propia, el `Permission` va **sin `condition`** y `conditionParams` vacío. `<usuario tipoUsuario="X"/>` → `EXISTS (SELECT cut FROM com.educaflow.subsystem.common.db.CentroUsuarioTipoUsuario cut WHERE cut.centroUsuario = cu AND cut.tipoUsuario.codigo IN ('X', …))` (todos en un `IN`). `<usuario cargo="X"/>` → `EXISTS (SELECT cuc FROM com.educaflow.subsystem.common.db.CentroUsuarioCargo cuc WHERE cuc.centroUsuario = cu AND cuc.cargo.code IN ('X', …))` (ojo: `Cargo.code`, no `codigo`). Admin: no existe en `<entities>` (AOP se salta los `Permission` del admin; `AuthSecurity.getUser()` es `null`); una entidad sin `<ace>` **no tiene `Permission`**: solo el admin. Condición propia: `(<ancla>) AND (<condition del ace>)` con `conditionParams="__user__,<los del ace>"`. No se puede reutilizar el `__user__` del ancla desde un `?` del `<ace>` ni escribir `?1`: AOP enlaza por posición y `Filter.build` renumera todos los `?`. Propiedad `Centro`: un `many-to-one` a `Centro` declarado o heredado; las colecciones no cuentan. Hoy tienen una `Expediente` (y subclases), `TareaFirma`, `Notificacion`, `RegistroEntrada`, `RegistroSalida`, `CentroUsuario`, `AceProfileCentro`, `UsuarioAutorizado`, `TareaImportacion` y `AsistenteNuevoExpediente`; ninguna tiene dos. El filtro de centro es un **suelo**, no el permiso, y solo protege filas que ya existen (`create` no tiene fila): que una fila nazca o se quede en un centro del usuario sigue en el servicio (`k-secure-coding`). `Permission.condition` admite 4096 caracteres en el fork de AOP (`Permission.xml:21`); EFBT comprueba la longitud generada y aborta diciendo cuánto ocupa y en qué `<ace>`. Ejemplo completo: `<ace object="TareaFirma" canRead="true" canWrite="true" condition="self.firmante = ?" conditionParams="__user__">` con `<usuario tipoUsuario="SUPERVISOR"/>` y `<usuario cargo="SECRETARIO"/>` da `acl.TareaFirma.read-write`, `conditionParams="__user__,__user__"` y:
      ```
      (EXISTS (SELECT cu FROM com.educaflow.subsystem.common.db.CentroUsuario cu
               WHERE cu.usuario = ? AND cu.centro = self.centro
                 AND (EXISTS (SELECT cut FROM com.educaflow.subsystem.common.db.CentroUsuarioTipoUsuario cut
                              WHERE cut.centroUsuario = cu AND cut.tipoUsuario.codigo IN ('SUPERVISOR'))
                      OR EXISTS (SELECT cuc FROM com.educaflow.subsystem.common.db.CentroUsuarioCargo cuc
                                 WHERE cuc.centroUsuario = cu AND cuc.cargo.code IN ('SECRETARIO')))))
      AND (self.firmante = ?)
      ```
    - **T-6 Copiar los `<ace>` de `Expediente` a cada subclase** (en el generador por tipo, `createdatainittipoexpediente`): copia los `<ace>` de `<entities>` de `Expediente` (condiciones incluidas) cambiando el `object` y dejando **solo `canRead`**; descarta los que se quedan sin flags y **fusiona** los idénticos (misma `condition`/`conditionParams`) uniendo destinatarios, sin abortar; numera `.<n>` como en T-5. Nunca `canCreate`/`canWrite`/`canRemove` en una subclase: `POST /ws/rest/<Subclase>` y el `remove` devuelven 403, y un expediente solo cambia por un evento que valida `Tramitador` (que guarda con `JPA.save`, sin pasar por esa puerta).
    - **T-7 Seguridad de los trámites**: por cada tipo de expediente, un `<ace>` en `<actions>` con `<usuario autenticado="true"/>` que cubre todas sus acciones con nombre, en el mismo sitio donde genera su data-init.
    - **T-8 Menús** (solo en el bloque 9 del plan de trabajo): `<menuitem>` con `action` → `if="__config__.menuSecurity.isVisible('<action>')"`; `<menuitem>` padre → `if="__config__.menuSecurity.isAnyVisible('<a1>', '<a2>', …)"` con las acciones de **todos** sus descendientes (AOP no oculta un padre cuyos hijos están todos ocultos); inyecta `groups="admins,users"` en todo `<menuitem>` (sin él un menú raíz no se ve para no administradores, `MenuChecker.isAllowed`) y deja de escribirse a mano; el admin se salta `groups` pero **no** el `if`: donde deba ver el menú necesita `<usuario admin="true"/>`. Hoy EFBT (`viewprocessor/tags/MenuItem.java`) ya inyecta `__config__.menuSecurity.isVisible("<name>")` y aborta si el menú trae `if`.
  - **B.4 Tests unitarios de EFBT**: expansión (cadena de prefijos, sustitución no suma, duplicados, recurso sin `<ace>`, endpoint público bajo comodín); colectivos (sustitución, deduplicado, nombre inexistente, derivado en `<entities>` a través de un colectivo); `canExecute` que falta; `<usuario>` con un atributo válido, con cero y con dos (error), atributo no admitido en el bloque (error del XSD, incluido `admin` en `<entities>`), `admin="false"` (error del XSD); `<ace>` sin `<usuario>` es error en los tres bloques; colectivo con `admin` en `<actionMethods>`/`<entities>` (se descarta; si se queda vacío, error); `Permission`: `condition` exacta por combinación (entidad sin `<ace>` → sin `Permission`; solo `autenticado` sin centro → sin `condition`; `tipoUsuario`+`cargo` → un `IN` por tabla), ancla con y sin `Centro`, AND con la condición propia y orden de `conditionParams`, `propiedadCentro` (0, 1, 2 propiedades; colección; sobra, falta, en comodín), nombre (orden canónico y `.2`), mismo `object` (distintos flags válido; mismos flags y condición error), sin flags (error), mezcla incondicional/condicional (error), copia a subclases (descarte y fusión).

- **Parte C — `subsystem/security` en tiempo de ejecución.** Solo afecta a métodos y acciones (y por ellos, menús): las entidades las evalúa AOP con los `Permission` generados. Hoy el único permiso es `EXECUTE`.
  - **C.1 Tabla `AceRecurso`**: dominio `subsystem/security/domains/AceRecurso.xml`, repositorio en `db/repo/`, tabla `security_ace_recurso`. Columnas: `tipoRecurso` (`ACTION_METHOD`, `ACTION`; las entidades no tienen filas), `recurso` (nombre completo y concreto: `com.x.controller.Ctrl.metodo` o el nombre de la acción), `permiso` (hoy solo `EXECUTE`), destinatario (**exactamente uno**): `admin` (bool), `autenticado` (bool), `todos` (bool), `tipoUsuario` (FK), `cargo` (FK), `perfil` (enum `Profile`, de `subsystem/expedientes`), `unidadTramitadora` (FK, de `subsystem/expedientes`). Una sola tabla, separada de `AceProfile*` a propósito (comparten la **forma** del destinatario, no la tabla). **MUST** añadirse a `tablasIncluidas` de `DataBaseStartup`: el data-init solo hace upsert y las filas quitadas no se borrarían. `auth_permission` no hace falta: `BulkTables` ya trunca `auth_%` (con `CASCADE`) en cada arranque, incluidas `auth_group_permissions` y `auth_user_permissions` (excluidas: `meta_file`, `meta_sequence`, `auth_user`, `auth_group`, `meta_filter`), así que al borrar un `auth-*.xml` sus `Permission` desaparecen en el siguiente arranque.
  - **C.2 Carga y caché con JPA (opción recomendada, en vez de una caché a mano).** AOP activa la caché de segundo nivel y la de consultas de Hibernate cuando `jakarta.persistence.sharedCache.mode` no es `NONE` (`DBHelper.isCacheEnabled`, `JpaModule#configureCache`). El proyecto ya la tiene activa: `jakarta.persistence.sharedCache.mode = ENABLE_SELECTIVE` y `application.cache.provider = caffeine` (`axelor-config.properties`), aunque hoy ninguna entidad la usa. Hay que: `<entity name="AceRecurso" cacheable="true">` en el `domains.xml` (genera `@Cacheable`) y la consulta del repositorio con `.cacheable()`: `all().filter("self.recurso = ?1 AND self.permiso = ?2", …).cacheable().fetch()`. Ventajas: sin código de caché; desaparece el problema del orden de arranque (la caché se llena perezosamente); si el data-init reescribe la tabla vía Hibernate, Hibernate invalida la región solo; el truncado de arranque es JDBC directo pero ocurre antes de crear el `EntityManagerFactory`. Cuidado: la caché de consultas guarda ids y las filas salen de la de segundo nivel, así que `AceRecurso` **MUST** ser `cacheable`; al evaluar, comparar las FK **por id** (`fila.getTipoUsuario().getId()` no inicializa el proxy) o marcar también `TipoUsuario`, `Cargo` y `UnidadTramitadora` como `cacheable`; una consulta cacheada por recurso sigue siendo más cara que un `HashMap` y los menús llaman a `isVisible` muchas veces por render: se mide en la tarea C-3 (log `org.hibernate.orm.cache` en `DEBUG`); si no basta, caché a mano cargada en un observador de `StartupEvent`. Abierto: cachear los tipos de usuario, cargos y perfiles **del usuario** (consultas a `CentroUsuario*` y `AceProfile*` en cada comprobación), por petición, por sesión o marcando esas entidades `cacheable`; decidirlo en C-3.
  - **C.3 API Java.** `AclService`: `puedeEjecutarMetodo(User user, String metodo)` (`com.x.controller.Ctrl.metodo`) y `puedeEjecutarAccion(User user, String accion)`. `puedeEjecutarMetodo` devuelve `true` para el admin (`SecurityUtil.isAdmin`) sin consultar la tabla; `puedeEjecutarAccion` **no**. Lee las filas `EXECUTE` del recurso (caché de C.2) y devuelve `true` si algún destinatario encaja (O). Sin lógica de comodines: la tabla ya es concreta. Sin filas → `false`. `DestinatarioEvaluator`: decide si un usuario encaja con el destinatario de una fila: `todos` siempre, sin mirar si hay usuario (en un endpoint público `SecurityUtil.getUser()` es `null`); `admin` (solo hay filas en acciones), `autenticado`, `tipoUsuario`, `cargo` (en algún centro) con `SecurityUtil.getUser()`, `SecurityUtil.isAdmin` y `user.tieneTipoUsuario`/cargo; sin usuario ninguno encaja; `perfil` → `PerfilesUsuarioService.tienePerfil`; `unidadTramitadora` → `PerfilesUsuarioService.isTramitador` (mismo subsistema). Ambos con tests unitarios (cuentan para `crapCheck`).
  - **C.4 Puntos de control.** **`AclActionObserver`**: `@Observes PreAction` sin `@Named`, enlazado con `bind(AclActionObserver.class)` en `SecurityModule` (sin binding `EventBus` no lo ve; `EventBus` recorre `injector.getAllBindings()`). 1) Averigua el recurso por el nombre: `vista:…` (primer segmento tipo de vista) → `action-view` ad hoc: **se bloquea** (no es declarable; `axelor-front` no los construye); con `:` → `ActionMethod` ad hoc: `controlador.metodo` (quitando `(args)`); sin `:` → `MetaStore.getAction(nombre)`: si es `ActionMethod`, su `getCall()` (`ACTION_METHOD`); si no, su nombre (`ACTION`). 2) Solo filtra lo nuestro: un `ActionMethod` si su clase está en `com.educaflow`; cualquier otra acción si el `module` de su `MetaAction` es `secretaria-virtual` (las de AOP son `axelor-core`/`axelor-web`; el módulo viene de `rootProject.name`). Un `action-group` ad hoc (`a,b,c`) no es recurso: se comprueba cada hija al pasar por `Action.execute`. 3) Sin usuario en sesión (proceso de sistema) deja pasar, como `AuthSecurity.isPermitted`; con el admin deja pasar los `ACTION_METHOD` y comprueba los `ACTION`. 4) Sin permiso → `AclDeniedException extends org.apache.shiro.authz.UnauthorizedException`, la misma para todo tipo de acción, con el recurso en el mensaje: el cliente ve «Access error», el log una línea sin traza y se hace rollback (`ResponseInterceptor`: `AuthorizationException` → «Access error», `STATUS_FAILURE`). Sin pantalla «sin permiso». **MUST NOT** hacerse en `PostAction` (la acción ya se habría ejecutado). **`AclRestFilter`**: `ContainerRequestFilter` con `@Provider`, enlazado con `bind(AclRestFilter.class)` (RESTEasy registra lo enlazado en Guice con `@Path`/`@Provider`, `AppContextListener`/`ModuleProcessor`). Método destino con `@Context ResourceInfo` (`getResourceClass()` + `getResourceMethod()`). Solo controladores de `com.educaflow`; `/ws/rest`, `/ws/meta`, `/ws/files`… no se tocan. El admin pasa siempre. Sin permiso → `abortWith(403)`. **`MenuSecurityServiceImpl`** (solo en el bloque 9): `isVisible(action)` consulta `AclService.puedeEjecutarAccion`; `isAnyVisible(...)` cualquiera de ellas; desaparece el `switch`. Como los destinatarios derivados (B7) llegan antes, el `switch` desaparece de golpe. Efecto añadido: quien no ve el menú tampoco puede abrir su acción (hoy cualquiera abre `sysVentanilla.SecretariaAbiertos@Expediente-action`). Alguna acción puede abrirse además desde un botón para otros usuarios: lo destapará el log de `warn`.
  - **C.5 Modo transitorio `warn` / `enforce`.** `security.acl.mode = warn | enforce` en `axelor-config.properties` (`warn` mientras dure la migración). `warn`: observador y filtro no bloquean; registran cada denegación (recurso, login, motivo), sin más datos personales. Interruptor **global**; un sistema está revisado cuando la suite E2E pasa sin denegaciones de sus recursos en el log. Menús y `Permission` de entidades no tienen modo `warn`. Se elimina el modo y la propiedad al terminar. **[Decisión spec]** Es una herramienta de la implementación, no un requisito del producto: la spec describe el estado final en `enforce`.
  - **[Decisión spec] Registro de seguridad (REQ-015 a REQ-019, REQ-025, REQ-030, REQ-034).** Toda denegación (observador de acciones y filtro JAX-RS, en `enforce`) se escribe en un **logger/appender propio de seguridad**, en un fichero separado del log de la aplicación, con **rotación semanal** y **retención de un año**; cada línea lleva fecha y hora, login del usuario (o «sin usuario identificado» si no había sesión), recurso denegado, motivo e IP de origen, y nada más de datos personales. Las operaciones permitidas y los listados recortados por los `Permission` no se registran. Un fallo al escribir el log no impide la denegación ni rompe la aplicación. Configurarlo en la configuración de logging del proyecto (`axelor-config.properties`), no con una tabla ni una pantalla.
  - **C.6 Lo que desaparece.** `EducaFlowAuthResolverImpl` (sustituye los `Permission` de una subclase por los de `Expediente`), su registro en `SecurityModule` y, si no le queda uso, el parche `EducaFlowAuthResolverRegistry` de AOP: con T-6 cada subclase tiene sus propios `Permission`. `GestorNotificacionesUtil` (lo cubren el colectivo `GESTOR` y el filtro de centro; hoy lo usan `auth-notificaciones.xml` y el `switch`). El `switch` de `MenuSecurityServiceImpl` y los `groups` escritos en `menus.xml`.
  - **C.7 Tests del proyecto** (a mano, en `src/test/java/com/educaflow/security`): **sin cobertura de métodos ni de entidades** (un recurso sin `<ace>` es «solo el admin» y no se distingue de un olvido). **Endpoints públicos**: todo endpoint bajo `/ws/public/**` tiene su `<ace>` con `<usuario todos="true"/>` como único destinatario, y ningún `<usuario todos="true"/>` cae fuera. **Cobertura de acciones**: toda acción con nombre (salvo `action-method`) cubierta por `<actions>`, y ningún `<ace>` de `<actions>` nombra un `action-method`. **Comodines vivos**: todo comodín gana al menos un recurso. **Colectivos**: todo colectivo se usa; un único `security-config.xml` en `subsystem/security/security/`; nombres únicos. **Menús**: toda `action` de `menus.xml` cubierta en `<actions>`. Los de cobertura recorren también los recursos de fuera de los sistemas (A.6): no hay lista de exclusión. Nacen con `@Disabled("hasta migrar todos los sistemas")` y se lanzan a mano como inventario. Unitarios de `AclService` y `DestinatarioEvaluator`.

- **Parte D — Migración.**
  - **D.1 Ficheros de seguridad a crear** (uno por alcance, escritos por Claude y revisados por el usuario; controladores / acciones aprox. / entidades / sustituye):
    - `subsystem/criptografia/security/acl.xml`: 2 / 19 / 3 / `data-init/input/security.xml` (formato inexistente, `input-config.xml` vacío: no carga nada).
    - `subsystem/notificaciones/security/acl.xml`: 1 / 35 / 4 / `auth-notificaciones.xml`.
    - `subsystem/firmas/security/acl.xml`: 1 / 25 / 2 / `auth-firmas.xml`.
    - `subsystem/registroentradasalida/security/acl.xml`: 1 (JAX-RS público) / 6 / 2 / —.
    - `subsystem/registrousuario/security/acl.xml`: 1 (JAX-RS público) / 0 / 2 / —. **[Decisión spec]** Este subsistema (alta pública) está a medio hacer: no completarlo en esta iniciativa; solo declarar sus endpoints públicos con `<usuario todos="true"/>` y asegurar que la petición de alta no puede fijar tipo de usuario, cargos, perfiles ni centros (`AllowProperties` del servicio de alta, `k-secure-coding`, REQ-038).
    - `subsystem/security/security/acl.xml` + `security-config.xml`: 1 / 10 / 6 (+ `AceRecurso`) / `auth-security.xml`.
    - `subsystem/common/security/acl.xml`: 0 / 30 / 14 / `auth-common.xml`.
    - `subsystem/sistemaeducativo/security/acl.xml`: 0 / 66 / 8 / `auth-sistemaeducativo.xml`.
    - `subsystem/importacion/security/acl.xml`: 0 / 7 / 1 / —.
    - `subsystem/pdfutilities/security/acl.xml`: 1 / 10 / 1 / —.
    - `subsystem/expedientes/security/acl.xml`: 1 / 2 / 7 (`Expediente` solo `read` para no admin) / `auth-expedientes.xml` (el último).
    - `subsystem/tramitador/security/acl.xml`: 1 (`<usuario autenticado="true"/>`) / 0 / 0 / —.
    - `subsystem/smoketest/security/acl.xml`: 0 / 5 / 1 / `auth-smoketest.xml`.
    - `system/gestioncentro/security/acl.xml`: 1 / 22 / 0 / `auth-gestioncentro.xml`.
    - `system/ventanilla/security/acl.xml`: 2 / 37 / 2 / `auth-ventanilla.xml`.
    - `base/infrastructure/security/acl.xml`: `DefaultModelController.*` (`<usuario autenticado="true"/>`) / — / 0 / —.
    - `com/axelor/security/acl.xml`: 0 / 0 / `User` (FQN) / permisos del `auth.xml` global. **[Decisión spec]** `MetaFile` y `DMSFile` **no** se declaran aquí (ver abajo).
    - `tramites/util/firma/security/acl.xml`: `FirmaClienteController` / 0 / 0 / —.
    - `tramites/shared/security/acl.xml`: 0 / las de `template-views.xml` que no son por tipo / 0 / —.
    - `correos` y `sms` no tienen recursos ni fichero.
  - **De dónde sale el contenido**: `<actions>` → la regla de visibilidad de hoy del menú que abre cada acción (`switch` de `MenuSecurityServiceImpl` y `groups` de `menus.xml`, tabla D.4); para las que no abre ningún menú (`onNew`, `onLoad`, `onChange`, botones), los destinatarios del form que las usa; lo que falte lo destapa el log de `warn`. `<actionMethods>` → los destinatarios de la acción o form desde el que se llama; los del motor con `<usuario autenticado="true"/>`. `<entities>` → **se escribe de nuevo desde lo que debe ser**, aplicando los principios de la spec (REQ-008 a REQ-012, REQ-039); los `auth-*.xml` actuales solo sirven como lista de entidades. La fuente de verdad son los tests existentes: un sistema está migrado cuando se ha borrado su `auth-*.xml` y los tests siguen en verde.
  - **D.2 Casos especiales de entidades.**
    - **`Expediente` y subclases**: `Expediente` solo `read` para no admin, con las condiciones de hoy (`Expediente.creador`, `Expediente.porGlobal`…, una por `<ace>`); las subclases por copia (T-6). Antes de darlo por bueno, en `warn` con la suite E2E: ningún form de expediente usa el guardado estándar (`canSave="false"` explícito en el form de `template-views.xml`; los grids ya lo llevan), y ninguna pantalla de administración edita expedientes por REST (si lo hace, se convierte en acción). **MUST NOT** volver a un `ModelService` con deny-all. Al implementarlo, quitar «PENDIENTE (importante)» de `CLAUDE.md` y dejar la regla en `k-secure-coding`. `auth-expedientes.xml` se migra el último (sus `condition` consultan `AceProfile*`).
    - **`HistorialEstado`**: «existe un expediente legible del que cuelga» (`EXISTS (SELECT e FROM Expediente e WHERE e = self.expediente AND …)` con las mismas subconsultas que `Expediente`).
    - **`Tramite`**: el `read` para supervisores (`Tramite.supervisor` de `auth-security.xml`, para el selector de «Mi centro → Perfiles de trámites») se reescribe en `subsystem/expedientes/security/acl.xml`.
    - **`User`**: `read` y `write` con `condition="self = ?"` para el propio usuario; lo de los supervisores (`gestioncentro`) con su condición.
    - **[Decisión spec] `MetaFile` y `DMSFile` quedan FUERA de esta iniciativa** (REQ-028): no crear sus `<ace>`, no quitar `MetaFile.all`/`DMSFile.all` del `auth.xml` global, no añadir `parentId`/`parentModel` a los `<iframe>` de `template-views.xml` ni de los `views.xml` de fases. Los permisos de ficheros se mantienen como hoy (riesgo conocido: descarga por id con sesión) y se abordan en otra iniciativa. Para esa iniciativa futura queda anotado el diseño previsto: `MetaFile` `create` para `<usuario autenticado="true"/>` y ningún `read` general (la lectura la decide Axelor por el padre en `RestService.download`: creador, o `parentId`/`parentModel` con permiso de lectura del padre y el fichero colgando de un campo suyo), por lo que los `<iframe>` deberían añadir `&parentId=${id}&parentModel=<FQN de la subclase>`; `DMSFile` nada para `users`.
    - **`auth.xml` global**: tras la migración solo conserva los grupos `admins`/`users`, los usuarios y (**[Decisión spec]**) los permisos de `MetaFile`/`DMSFile`; los demás `bind` los genera EFBT. Es lo último en irse.
  - **D.3 Otros cambios**: borrar el trámite `prueba` (y con él `FormacionCentroTrabajo`) antes de tocar `expedientes` (REQ-027, REQ-040); `AceProfile*` no se toca.
  - **D.4 Visibilidad de los menús de hoy en el formato nuevo** (fuente para REQ-003 y los escenarios de HU-001):
    - `groups="admins"` (`firmas-todas`, `tramitacion-todosLosCentros*`, `notificaciones-todas`, `administracion*`, `criptografia*`, `sistemaEducativo*`, `desarrollador*`): solo admin → `<usuario admin="true"/>`.
    - `groups="admins,users"` sin regla en el `switch`: cualquiera → `<usuario autenticado="true"/>`.
    - `firmas-delCentro` (`groups="users"`): supervisor → `<usuario tipoUsuario="SUPERVISOR"/>` (el admin no lo ve).
    - `miCentro` (padre de 3 acciones): supervisor → `<usuario tipoUsuario="SUPERVISOR"/>` en las 3 hijas.
    - `notificaciones-delCentro`: `esGestorEnAlgunCentro` → `<usuario colectivo="GESTOR"/>` (hoy `GestorNotificacionesUtil` no incluye al admin, pero el colectivo `GESTOR` sí lo incluye a propósito: el admin pasa a ver «Notificaciones → Del centro», y así lo recogen ESC-006 y Seguridad).
    - `tramitacion` (padre): admin o `isTramitador(user)` → `<usuario admin="true"/>` + un `<usuario perfil="…"/>` por cada perfil de tramitación (`TRAMITADOR`, `COLABORADOR`, `AFECTADO`, `SECRETARIO`, `DIRECTOR`, `AUDITOR`: todos salvo `CREADOR`) en cada hija (`tramiteEnPapel`, `pendientesDeMi`).
    - `tramitacion-jefaturaDeEstudios` (padre): `isTramitador(user, JEFATURA_ESTUDIOS)` → `<usuario unidadTramitadora="JEFATURA_ESTUDIOS"/>` en las hijas.
    - `tramitacion-secretaria` (padre): `isTramitador(user, SECRETARIA)` → `<usuario unidadTramitadora="SECRETARIA"/>` en las hijas.
    - `registro` (padre): admin o `isTramitador(user, SECRETARIA)` o supervisor → `<usuario admin="true"/>` + `<usuario tipoUsuario="SUPERVISOR"/>` + `<usuario unidadTramitadora="SECRETARIA"/>`.
    - Perfiles de tramitación de hoy (data-init `AceProfile*`): `ADMINISTRATIVO` es `TRAMITADOR` de los trámites de `ALUMNO` (Secretaría); `JEFE_ESTUDIOS` es `TRAMITADOR` de los de `PROFESOR` (Jefatura); `DIRECTOR` y `SECRETARIO` tienen perfil global `DIRECTOR`/`SECRETARIO` (tramitan en ambas unidades); `PROFESOR`, `ALUMNO` y `FAMILIAR` son `CREADOR`.

- **Plan de trabajo** (el orden respeta las dependencias; lo de la misma columna puede ir en paralelo): `B0 Preparación` → `B1 Base` → `B2 EFBT métodos/acciones` → `B3 Runtime` → `B4 Control en warn` → `B5 Piloto criptografia`; `B6 EFBT entidades` tras B2; `B7 Derivados` tras B3; todos → `B8 Migración sistema a sistema` → `B9 Menús` → `B10 Enforce` → `B11 Docs`.
  - **B0**: P-1 borrar el trámite `prueba` (build y suite E2E en verde); P-2 extraer de `files/ace/Ace.java` (EFBT) la lectura de `<usuario>` con «un solo atributo» a una pieza reutilizable (las filas de `AceProfile*` en BD quedan iguales).
  - **B1**: F-1 XSD `security_1.0.xsd` (tres bloques, tres tipos de `<ace>` y su `<usuario>`) y `security-config_1.0.xsd` (valida los ejemplos de A.8 y rechaza los casos de V-01); F-2 dominio `AceRecurso` (`cacheable="true"`), repositorio con consulta `.cacheable()`, `security_ace_recurso` en `tablasIncluidas` (arranca y la tabla existe vacía).
  - **B2**: E-1 tarea `generateSecurity` en `build.gradle`, descubrimiento y enumeración de recursos de métodos y acciones; E-2 validaciones V-01 a V-11, V-13, V-17 y V-18 (cada una con su test unitario); E-3 transformaciones T-1 a T-4 y T-7 (`AceRecurso.xml` generado y cargado; tests de B.4); E-4 `security-config.xml` con el colectivo `GESTOR`; `mvn install` en EFBT.
  - **B3**: C-1 `AclService` sobre la consulta cacheada (tests unitarios); C-2 `DestinatarioEvaluator` sin derivados (tests, `crapCheck` en verde); C-3 verificar la caché de JPA con `org.hibernate.orm.cache` en `DEBUG` y decidir la caché de los datos del usuario (decisión escrita).
  - **B4**: W-1 propiedad `security.acl.mode`; W-2 `AclActionObserver` + `AclDeniedException` (en `warn` registra, nada se bloquea); W-3 `AclRestFilter` (ídem JAX-RS). **[Decisión spec]** aquí se monta también el logger de seguridad con rotación semanal y retención anual.
  - **B5**: M-1 migrar `criptografia` sin `<entities>` (borrar `data-init/input/security.xml`, crear `security/acl.xml`; E2E en verde sin denegaciones de `criptografia` en el log); M-2 tests de cobertura de C.7, deshabilitados (lanzados a mano dan el inventario).
  - **B6**: E-5 enumeración de entidades y propiedades `Centro`; validaciones V-12, V-14, V-15, V-16; E-6 T-5 (`Permission` con ancla, fragmentos, condición propia, nombre y `bind` a `users`); E-7 T-6 (copia de `Expediente` a subclases en `createdatainittipoexpediente`; tests de descarte y fusión).
  - **B7**: D-1 `perfil` y `unidadTramitadora` en XSD, columnas de `AceRecurso` y `DestinatarioEvaluator` (tests unitarios).
  - **B8**: por cada fichero de D.1, en un solo paso `<actionMethods>`, `<actions>` y `<entities>`, borrando su `auth-*.xml`. Orden: los sistemas sin `auth-*.xml` y los simples primero; `ventanilla`, `gestioncentro` y `security` después; `expedientes` (con D.2) el último de los sistemas, y al final la parte de permisos del `auth.xml` global junto con `com/axelor/security/acl.xml` (solo `User`). Al terminar `notificaciones` y `expedientes`, borrar `GestorNotificacionesUtil` y `EducaFlowAuthResolverImpl`. Hecho por sistema cuando el usuario aprueba el fichero, se ha borrado su `auth-*.xml` y la suite E2E pasa sin denegaciones de ese sistema en el log de `warn`. El bloque termina cuando los tests de cobertura, lanzados a mano, pasan.
  - **B9**: N-1 T-8 en EFBT (`if` con acciones, `isAnyVisible` en padres, `groups` inyectado; `menus.xml` sin `groups`); N-2 `MenuSecurityServiceImpl` consulta `AclService`, fuera el `switch` (cada usuario de prueba ve los mismos menús que antes, tabla D.4).
  - **B10**: X-1 revisar el log de `warn`, pasar a `enforce`, suite E2E en verde; X-2 habilitar los tests de cobertura, quitar el modo `warn` y la propiedad (`./run.sh` en verde).
  - **B11 Documentación**: skill nuevo `k-acl` (o sección en `k-secure-coding`) y `subsystem/security/CLAUDE.md`. El skill **MUST** incluir la especificidad de A.5 («gana el más específico y sustituye, no suma») con su ejemplo XML de tres niveles, la tabla de qué regla gana para cada recurso y el porqué, y la consecuencia de que en `<actions>` el admin pierde una acción (y su menú) si su regla específica no repite `<usuario admin="true"/>`, mientras que en métodos y entidades está exento y «solo el admin» es no declarar el recurso. Referencias desde `k-sistemas`/`k-vistas`: al crear un controlador o una acción con nombre **MUST** declararse su `<ace>`; antes de repetir una lista de destinatarios, buscar o crear un colectivo. Revisar `k-vistas`/`view-rules.md`: `groups`/`showIf` dejan de ser criterio de acceso y `groups` ya no se escribe en `menus.xml`. Quitar «PENDIENTE (importante)» de `CLAUDE.md`.

- **Pendiente, fuera de esta iniciativa** (no diseñar): IDOR/multicentro más allá del suelo de centro (en acciones `tipoUsuario`/`cargo` son «en algún centro», así que cada método/servicio **MUST** seguir comprobando el centro del registro); hijos del `<ace>` de `<entities>` que hablen del **registro** (combinados en Y con el O de los `<usuario>`: «el registro está en un centro del usuario», «el registro lo creó el usuario»), sintaxis sin decidir; concesiones a usuarios concretos (tabla de tiempo de ejecución con pantalla); otros destinatarios, colectivos locales o anidados; `MetaFile`/`DMSFile` (ver D.2).

- **Referencias verificadas en AOP** (`../axelor-open-platform`; no hace falta volver a buscarlo): `Action.execute` en `axelor-core/.../meta/schema/actions/Action.java:81-95` (sin nombre solo `evaluate`; `checkPermission` l. 97 es `CAN_READ` sobre `getModel()`); `Action.wrap` l. 101-104; entrada de `POST /ws/action` en `axelor-core/.../meta/ActionHandler.java:474-500` (`ActionGroup` **sin nombre**, sin `try/catch`: la excepción del observador sube a `ResponseInterceptor`); resolución de nombre en `ActionGroup.java:91-150` (`findAction`: con `:` y tipo de vista → `ActionView` ad hoc `setName` l. 110; con `:` sin tipo de vista → `ActionMethod` ad hoc l. 136; sin `:` → `MetaStore.getAction`); iteración del grupo l. 213-227; `ActionHandler.call` l. 224-236 (sin mirar `@CallMethod`), `rpc` l. 238-…; `ActionMethod.Call` l. 17-32; disparo de eventos `ActionHandler.java:155-163`; `Observer.java:77-83` (sin qualifiers recibe todo); `EventBus.java:49-58`; `MetaStore.getAction` l. 79-89; módulo de una acción en `ViewLoader.java:347,410,544,627` (en BD: `secretaria-virtual` y `axelor-core`); `ResponseInterceptor.java:64-110`. Entidades: `AuthSecurity.isPermitted` l. 155-184 (usuario `null` → `true`; sin `Permission` → `false`; uno sin `condition` → `true`; sin `ids` (create) → `true`); `getFilter` l. 116-153; `Condition` l. 29-50; `Filter.build` renumera `?` (`Filter.java:27-30`); resolución del fork l. 71-72, 199-206 (`EducaFlowAuthResolverRegistry`); `AuthResolver.resolve` l. 98-123, `filterPermissions` l. 59-71 (`object` por igualdad exacta, **sin superclases**); `EducaFlowAuthResolverImpl` l. 16-32, registro en `SecurityModule.java:20`; admin en `AuthUtils.java:100-103` y `AuthSecurity.java:75-79`; create/write/remove solo en `Resource.java:1288-1291, 1379-1393, 1434, 1497, 1536` (ni `JPA` ni `ActionRecord` comprueban); vaciado al arrancar en `base/infrastructure/db/BulkTables.java:12-38`, `DataBaseStartup.java:28-35`, `SecretariaVirtualModule.java:22`; `StartupEvent` en `axelor-web/.../web/AppStartup.java:58`; `Permission.condition` 4096 caracteres en el fork (commit `5e071d30c`). Menús: `MenuChecker.java:56-70`, `MenuService.java:72-73` (un menú raíz sin `groups` solo lo ve el admin); `if` inyectado hoy por EFBT `viewprocessor/tags/MenuItem.java:15-25`; `switch` actual en `MenuSecurityServiceImpl.java:22-40`; `GestorNotificacionesUtil.java:23-26, 33-37, 39-48`; `PerfilesUsuarioService.java:54,57` (`isTramitador`). JAX-RS y Shiro: rutas sin sesión en `AuthWebModule.java:23-31` (`/ws/public/**`, `/public/**`, estáticos); registro de providers en `AppContextListener.java:16,70`; endpoints públicos en `RegistroSalidaController.java:29,39,76` y `RegistroController.java:24,46,80,106,134,141` (no hay controladores Kotlin ni en subpaquetes de `controller`). Ficheros: `RestService.java:557-580`, `download` 477-510, permiso por el padre 589-650; estado hoy `data-init/input/auth.xml:8-10,205-208`. Caché JPA: `DBHelper.java:202-217`, `JpaModule.java:170-177, 234-240`, `CacheConfig.java:24`, `Entity.java:86,617-626` (`cacheable="true"` → `@Cacheable`), `Query.java:249-252,384,418` (`Query#cacheable()`), `axelor-config.properties:157,246`.

- **Por qué de las decisiones menos obvias.** 1) Condición propia en `<entities>`: casi todos los permisos condicionales de hoy son por relación con el registro; sin ella no se podían migrar `firmas`, `notificaciones`, `security` ni `expedientes`. 2) Un solo `?` en el ancla: AOP enlaza por posición y renumera; un único `EXISTS` sobre `CentroUsuario` fija a la vez usuario, centro, tipos y cargos. 3) Entidades fuera de `AceRecurso`: con el centro en el ancla, una condición genérica habría necesitado igualmente `self.<centro>` por entidad. 4) Copia a subclases y no resolver: `EducaFlowAuthResolverImpl` sustituía, no unía, y un `write` en `Expediente` lo habrían heredado todas las subclases por detrás del generador. 5) Prohibido mezclar incondicional y condicional en un flag: por id gana el incondicional; en el grid se pierde y mostraría menos filas de las que se abren por id. 6) Sobrecargas abortan: AOP las tolera a medias pero la clave `Ctrl.metodo` no las distingue. 7) `AclDeniedException` hereda de `UnauthorizedException`: el cliente recibe «Access error» y el log no se llena de trazas. 8) El admin exento en métodos y entidades pero no en acciones: en entidades lo impone AOP; en métodos no aporta nada obligar a declararlo; en acciones se declara porque las acciones son los menús, y el admin no debe ver los menús de cada tipo de usuario. 9) Modo `warn` global: no existe `enforce` por sistema; el criterio de «sistema revisado» es la suite E2E sin denegaciones de ese sistema.
