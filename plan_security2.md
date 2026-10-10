# Plan — ACL declarativa de acciones, menús y entidades

## 1. Objetivo

Declarar en XML **quién** puede ejecutar cada método de controlador, ejecutar cada acción XML con nombre, sea del tipo que sea (y por tanto ver cada menú), y operar sobre cada entidad, con un único formato y un único sitio por sistema.
Lo declarado sobre métodos y acciones se carga en la **tabla** `AceRecurso`; lo declarado sobre entidades se convierte en `Permission` de Axelor cuya `condition` JPQL escribe EFBT a partir de los destinatarios (B.3).
Lo que no esté declarado está **prohibido**.

## 2. Por qué hace falta

AOP no tiene permisos por rol sobre acciones ni sobre métodos de controlador.
- `Action.execute` solo comprueba `READ` sobre el `model` de la acción (`Action.checkPermission`); un `action-method` sin `model` no comprueba nada.
- `POST /ws/action` ejecuta cualquier nombre que mande el cliente:
  - el nombre de una acción XML, aunque su botón esté oculto (`showIf`/`hidden`/`groups` no son defensa);
  - `com.x.MiController:metodo`, que `ActionGroup` convierte en un `ActionMethod` al vuelo y `ActionHandler.call` invoca **sin exigir `@CallMethod`** (solo la forma `metodo(args)` de `rpc` lo exige);
  - `form:vista:Modelo`, que construye un `action-view` al vuelo.
- `call:Ctrl:metodo` dentro de expresiones también crea un `ActionMethod` con nombre, así que pasa por el mismo sitio.
- Los controladores también pueden publicar **endpoints JAX-RS**, que no pasan por `Action.execute` y que Shiro deja entrar **sin sesión** si cuelgan de `/ws/public/**`.
  Hoy hay dos:
  - `RegistroSalidaController` (`subsystem/registroentradasalida`): `getDescargaByCsv` (`/ws/public/registro-salida/download`, al que lleva el QR) y `validateGetDescargaByCsv` (`/validate-download`).
  - `RegistroController` (`subsystem/registrousuario`, `@Path("/public/registro")`): `index`, `registro.css`, `registro.js`, `registrosPendientes` (POST), `validarCodigo` y `usuarios`.
- En entidades, el `auth.xml` global concede hoy sin condición `MetaFile.all`, `DMSFile.all`, `User.all`, `Persona.all`, `CentroUsuario.all`, `RegistroEntrada.all`, `RegistroSalida.all`… a todos los usuarios.
  Como Axelor une los `Permission` con OR, muchas condiciones de los `auth-*.xml` no restringen nada, y **cualquier usuario con sesión puede descargar cualquier `MetaFile` por su id** (ids correlativos).

El punto común de las acciones es `Action.execute`:
`checkPermission → firePreEvent(nombre) → evaluate → firePostEvent(nombre, resultado) → event.getResult()`.
Un observador `@Observes PreAction` **sin `@Named`** recibe los eventos de todas las acciones: ese es el filtro.

## 3. Resumen

La seguridad se escribe a mano en **XML** (`security/*.xml` de cada sistema) y nunca se lee tal cual en tiempo de ejecución.
EFBT la **valida** y la **transforma** en el build en dos salidas distintas, según el tipo de recurso:

| Recurso                                                  | Lo que genera EFBT                                                            | Dónde acaba             | Quién lo evalúa                          |
|----------------------------------------------------------|-------------------------------------------------------------------------------|-------------------------|------------------------------------------|
| Métodos de controlador (`<actionMethods>`)               | data-init de `AceRecurso`                                                     | tabla `AceRecurso`      | `subsystem/security` (Java), con caché   |
| Acciones XML con nombre (`<actions>`), y por tanto menús | data-init de `AceRecurso`                                                     | tabla `AceRecurso`      | `subsystem/security` (Java), con caché   |
| Entidades (`<entities>`)                                 | `<permission>` de Axelor con su `condition` JPQL y su `bind` al grupo `users` | tabla `auth_permission` | AOP (`AuthSecurity`), sin código nuestro |

```
 security/*.xml  (lo escribe el autor; un fichero o varios por sistema)
        │
        ▼
 EFBT  generateSecurity  (tarea JavaExec, antes de processResources)
   ├─ 1. VALIDAR      XSD + reglas semánticas (alcance, existencia, duplicados, destinatarios…)
   └─ 2. TRANSFORMAR  colectivos → recursos concretos → comodines → salidas:
          ├─ AceRecurso.xml (data-init) ──► BD: AceRecurso ──► subsystem/security
          │                                                     AclService.puedeEjecutar(user, recurso)
          │                                                     ├─ AclActionObserver   (POST /ws/action)
          │                                                     ├─ AclRestFilter       (endpoints JAX-RS)
          │                                                     └─ MenuSecurityService (if de los menús)
          ├─ <permission> + <group users> ──► BD: auth_permission ──► AOP (AuthSecurity / AuthResolver)
          └─ menus.xml: if="isVisible(...)/isAnyVisible(...)" + groups="admins,users"
```

Lo que no esté declarado está **prohibido**.

El trabajo se divide en tres partes, que son las secciones de este plan:
- **Parte A — El formato XML**: lo que escribe el autor (§5).
- **Parte B — EFBT**: validar los XML y transformarlos en data-init y `<permission>` (§6).
- **Parte C — `subsystem/security`**: leer `AceRecurso`, cachearla y responder «¿puede este usuario ejecutar este recurso?» (§7).

A ellas se suma la **Parte D — Migración** de los sistemas actuales (§8), y el **plan de trabajo** ordenado (§9).

## 4. Principios

1. **Una fuente**: el XML de `security/` es la única fuente de permisos de métodos, acciones, menús y entidades.
   Los `auth-*.xml` escritos a mano y la parte de permisos del `auth.xml` global desaparecen.
2. **Prohibido por defecto**.
3. **En tiempo de ejecución no hay comodines ni colectivos**: EFBT los expande; la tabla y los `Permission` solo tienen recursos concretos y destinatarios primitivos.
4. **Un destinatario (`<usuario>`) por fila; varias filas son O.** No hay Y entre destinatarios.
   Lo que en el futuro se añada a un `<ace>` que no sea «qué es el usuario» (§10, condiciones sobre el registro) irá en otros hijos y se combinará con Y sobre ese O.
5. **La ACL dice quién puede *llamar*, no sobre qué registro** (salvo el suelo de centro en entidades): el IDOR sigue siendo responsabilidad del método/servicio y del motor de tramitación.
6. **Todo lo de ejecución va en `subsystem/security`** (dominio, repositorio, servicio, evaluador, observador, filtro, menús, colectivos, tests).
   Fuera solo: el XSD y la generación (EFBT), los `security/*.xml` de cada sistema y el `if` que EFBT inyecta en los menús.
   Nada va a `base/infrastructure`, a `tramitador` ni a `common`.
7. **Migración sin romper**: modo `warn` global mientras se migra; los menús no se tocan hasta el final.
8. **Los `security/*.xml` los escribe Claude**, sistema a sistema, y el usuario los revisa.
9. El trámite `prueba` se borra antes y no se tiene en cuenta (con él desaparece `FormacionCentroTrabajo`).
10. Sin DNI ni usuarios concretos en el XML: las concesiones a personas son de tiempo de ejecución y quedan fuera.

---

## 5. Parte A — El formato XML (lo que escribe el autor)

### A.1 Ficheros

- Namespace `http://educaflow.com/xml/ns/security`, raíz `<security>`.
- En cualquier carpeta `security/` de `src/main/java` (normalmente `<sistema>/security/`), **con cualquier nombre**: se leen todos los del namespace.
- EFBT los busca por namespace en **cualquier** carpeta de `src/main/java`, no solo en las `security/`, para que uno mal colocado no se ignore en silencio: estar en una carpeta `security/` es una regla de arquitectura que EFBT comprueba (V-17), no lo que decide si se lee.
- Se puede repetir un bloque y mezclar tipos en un fichero, o tener un fichero por tipo.
- Sin `xsi:schemaLocation`: el XSD vive solo en EFBT (como `TramiteInstance.xml`).
  El IDE no autocompleta; a cambio no hay una copia que mantener sincronizada.
- Los colectivos **no** van en estos ficheros: van en el fichero de configuración de la seguridad (A.4), que tiene su propio namespace, su propio XSD y su propia raíz para que no se confunda con ellos.

### A.2 Bloques y flags

Un bloque por tipo de recurso, para que el XSD valide cada `<ace>` con su propia definición.
Los permisos son atributos booleanos, como en el `Permission` de Axelor; se añade `canExecute`.

| Bloque            | `object`                                                                                                                                                                                                      | Flags                                                                                               | Si no se escribe ninguno      |
|-------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------|-------------------------------|
| `<actionMethods>` | `Controlador.metodo`, relativo al paquete `controller` del sistema (métodos de acción y endpoints JAX-RS)                                                                                                     | solo `canExecute` (solo admite `true`)                                                              | EFBT pone `canExecute="true"` |
| `<actions>`       | nombre **entero** de cualquier acción XML con nombre (`action-view`, `action-record`, `action-attrs`, `action-group`, `action-script`, `action-export`, `action-report`…) del sistema; **no** `action-method` | solo `canExecute` (solo admite `true`)                                                              | EFBT pone `canExecute="true"` |
| `<entities>`      | nombre de la entidad, relativo al paquete `db` del sistema                                                                                                                                                    | `canRead`, `canWrite`, `canCreate`, `canRemove`, `canExport`, `canImport` (el que falta es `false`) | error: al menos uno `true`    |

Reglas de `<actionMethods>` y `<actions>`:
- Para que alguien no ejecute algo no se le declara; no existe `canExecute="false"`.
- Que el flag exista aunque sea opcional permite añadir más permisos en el futuro sin tocar los ficheros.
- **`action-method` no se declara en `<actions>`**: su permiso es el del método al que llama.
  Asegurar el nombre de la acción no protegería nada, porque `POST /ws/action` acepta `…Controller:metodo` sin pasar por ese nombre.
- Todas las acciones con nombre se bloquean igual, también las de dentro de un `action-group` (cada hija pasa por `Action.execute`), así que declarar el grupo **no** cubre a sus hijas.

Reglas de `<entities>`:
- Atributos opcionales **`condition`** y **`conditionParams`**, con la sintaxis del `Permission` de Axelor (JPQL sobre `self`, `?` posicionales, `conditionParams` separados por comas con `__user__` o expresiones Groovy).
  - Sirven para los permisos «sobre el propio registro» (firmante, destinatario, interesado…).
  - `condition` sin `conditionParams` es válida; `conditionParams` sin `condition` es error.
  - Se aplica a todos los flags y destinatarios del `<ace>`.
  - El autor escribe **solo sus `?` y sus `conditionParams`**: la parte de destinatarios y centro la añade EFBT delante (B.3, T-5).
  ```xml
  <ace object="TareaFirma" canRead="true" canWrite="true" condition="self.firmante = ?" conditionParams="__user__">
    <usuario autenticado="true"/>
  </ace>
  ```
- Atributo opcional **`propiedadCentro`**: el `many-to-one` a `Centro` por el que se filtra.
  Obligatorio si la entidad tiene más de uno; error si tiene uno o ninguno, o en un `object` con comodín.
- Un mismo `object` **puede repetirse** con distintos flags, destinatarios o `condition`; Axelor une los `Permission` con OR.
  Solo es error repetir `object` con los mismos flags a `true` **y** la misma `condition`/`conditionParams` (hay que fusionar destinatarios).
  ```xml
  <ace object="Notificacion" canRead="true">
    <usuario tipoUsuario="PROFESOR"/>
    <usuario tipoUsuario="SUPERVISOR"/>
  </ace>
  <ace object="Notificacion" canWrite="true" canCreate="true" canRemove="true">
    <usuario tipoUsuario="SUPERVISOR"/>
  </ace>
  ```
  Con una condición distinta por destinatario (`<ace object="Notificacion" canRead="true" condition="A">` para PROFESOR y otro con `condition="B"` para SUPERVISOR), `canRead` queda «(PROFESOR del centro y A) o (SUPERVISOR del centro y B)».
- **Cada flag se evalúa por separado**, solo con los `<ace>` que lo tienen a `true`.
  Si `canWrite` debe estar acotado como `canRead`, se repite la condición: EFBT no puede saber si una JPQL contiene a otra.
  Si no se repite, quien solo lee las filas que cumplen la condición pero escribe sin ella puede modificar por REST, por id, filas que no ve en el grid.
- `canRemove="false"` es lo mismo que omitirlo; se admite para escribir los seis a la vista.

### A.3 Destinatarios: `<usuario>`

Cada hijo `<usuario>` de un `<ace>` es **un destinatario y una fila**; varios `<usuario>` son O.
Es la **misma forma** que ya usan los `<acl>` de perfiles (`AceProfile*`, `TramiteInstance.xml`, `TipoExpedienteInstance.xml`): `<usuario cargo="DIRECTOR"/>`.
Se llama `usuario` porque dice **qué es el usuario** que llama; deja sitio a otros hijos que digan otra cosa (p. ej. qué relación tiene el registro con él, que está pendiente de decidir, §10).
Un `<ace>` sin ningún `<usuario>` es error del XSD.

**Un solo atributo por `<usuario>`**: el atributo dice el tipo de destinatario y su valor el código; dos atributos en el mismo `<usuario>` es error (EFBT, V-18), igual que hoy en `AceProfile*`.
Los destinatarios sin código (`admin`, `autenticado`, `todos`) son atributos booleanos: se escriben `admin="true"`, porque XML no admite un atributo sin valor (`<usuario admin/>` no es XML bien formado); `admin="false"` es error del XSD (el tipo del atributo solo admite `true`).

| Destinatario                       | Quién                                                                                                                                                         | Bloques                                                               |
|------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------|
| `<usuario admin="true"/>`          | el administrador (`SecurityUtil.isAdmin`)                                                                                                                     | solo `actions`: en `<actionMethods>` y `<entities>` no existe (ver abajo) |
| `<usuario autenticado="true"/>`    | cualquier usuario con sesión                                                                                                                                  | todos                                                                 |
| `<usuario todos="true"/>`          | cualquiera, **con o sin sesión**                                                                                                                              | solo `actionMethods`, y solo en endpoints JAX-RS bajo `/ws/public/**` |
| `<usuario tipoUsuario="…"/>`       | ese tipo de usuario (ver multicentro)                                                                                                                         | todos                                                                 |
| `<usuario cargo="…"/>`             | ese cargo (ver multicentro)                                                                                                                                   | todos                                                                 |
| `<usuario perfil="…"/>`            | tiene ese perfil (`Profile`) en algún trámite de cualquier unidad tramitadora en cualquier centro (`PerfilesUsuarioService.tienePerfil(user, perfil)`, nuevo) | solo `actionMethods` y `actions` (derivado)                           |
| `<usuario unidadTramitadora="…"/>` | tramita algún trámite de esa unidad (`isTramitador(user, codigo)`)                                                                                            | solo `actionMethods` y `actions` (derivado)                           |
| `<usuario colectivo="…"/>`         | no es destinatario: EFBT lo sustituye por la lista de `<usuario>` del colectivo (A.4)                                                                         | donde quepan todos sus destinatarios                                  |

- **`<usuario todos="true"/>` frente a `<usuario autenticado="true"/>`**: fuera de `/ws/public/**` Axelor ya exige sesión, así que `<usuario todos="true"/>` mentiría: EFBT aborta.
  Y en un endpoint público cualquier otro destinatario no protegería nada: EFBT aborta.
  La fila `todos` **documenta** que es público; la defensa real sigue siendo la del endpoint (el CSV).
- **Derivados**: se calculan en Java (recorren `AceProfile*`), no caben en una `condition` JPQL; por eso no se admiten en `<entities>`.
- **El admin puede todo en `<actionMethods>` y `<entities>`, y `admin` no existe en esos bloques** (el XSD no lo admite en su `<usuario>`).
  - Entidades: las evalúa AOP, y `AuthSecurity.isPermitted` devuelve `true` para el admin antes de mirar ningún `Permission`: **el admin ve y edita todas las filas de todas las entidades** haga lo que haga el XML.
  - Métodos de controlador: `AclService.puedeEjecutarMetodo` devuelve `true` para el admin sin consultar la tabla (C.3), y `AclRestFilter` igual.
    Un `<usuario admin="true"/>` en cualquiera de los dos daría a entender que el `<ace>` acota al admin, cuando no puede.
  - En `<actions>` (y por tanto en los menús) **sí hace falta declararlo**: `AclService.puedeEjecutarAccion` es «prohibido por defecto» y **no exime al admin**, a propósito, porque si lo eximiera el admin vería todos los menús de todos los usuarios (A.5, T-8).
  Consecuencias:
  - «Solo el admin» en un método o una entidad se escribe **no declarándolo**: sin `<ace>` no hay filas ni `Permission`, y nadie salvo el admin pasa.
    No existe un `<ace>` sin `<usuario>` para decirlo a la vista (el XSD lo rechaza en los tres bloques): no daría ningún permiso, y el admin ya los tiene todos.
    Consecuencias: un método o una entidad olvidados no se distinguen de uno «solo admin», así que no hay test de cobertura de métodos ni de entidades (C.7); el lado seguro es el cerrado, y el olvido lo destapa el log de `warn` o la suite E2E, no un agujero.
    Y bajo un comodín (A.5) no se puede dejar un método solo al admin con una excepción: hay que estrechar el comodín.
  - Un colectivo con `admin` (`GESTOR`) se puede usar en `<actionMethods>` y `<entities>`: al expandirlo (T-1) EFBT descarta el `admin` sin error, para no tener dos versiones del colectivo según el bloque.
- **Multicentro**: en métodos, acciones y menús `tipoUsuario`/`cargo` se evalúan «en algún centro del usuario».
  En entidades, «en el centro del registro» si la entidad tiene propiedad `Centro`, y «en algún centro» si no.
- **Por qué solo O**: con Y habría columnas con nulos, todas las no nulas tendrían que cumplirse sobre el mismo `CentroUsuario`, y una fila toda nula daría acceso a todos. Ninguna regla actual necesita Y.
- **Gestor de notificaciones**: no es derivado; se escribe con el colectivo `GESTOR`, y el «en qué centros gestiona» lo da el filtro de centro de oficio en `Notificacion`.
  El usuario normal ve **sus** notificaciones con otro `<ace>` y su `condition` (`self.correo.dniDestinatario = ?` con `__user__.dni`).

### A.4 Colectivos

Un nombre para una lista de `<usuario>` primitivos (no se llama «rol» ni «grupo»: esas palabras ya son de Axelor y del proyecto).
Se usa desde un `<ace>` con `<usuario colectivo="GESTOR"/>`, con la misma forma que cualquier otro destinatario.

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

- **Fichero de configuración de la seguridad**: namespace `http://educaflow.com/xml/ns/security-config`, raíz `<securityConfig>` y XSD propio `security-config_1.0.xsd` (solo en EFBT), distintos de los de `<security>` para que no se confundan.
  - **Hay exactamente uno en todo el proyecto**: `subsystem/security/security/security-config.xml`.
    Que falte, que haya otro con ese namespace o que esté en otra ruta es error de EFBT.
  - El descubrimiento de los `<security>` lo ignora (no es de su namespace); EFBT lo lee aparte.
  - Hoy solo contiene `<colectivos>`; lo que en el futuro sea configuración global de la seguridad (no permisos de un sistema) va aquí.
- **Globales**: nombre único en todo el proyecto.
  Se usan desde cualquier fichero `<security>` (no son recursos, no entran en la regla de alcance).
- **Sin anidamiento** (solo `<usuario>` primitivos: `<usuario colectivo="…"/>` dentro de un colectivo es error del XSD) y **sin colectivos locales**.
- `<usuario todos="true"/>` no se admite dentro de un colectivo; `perfil`/`unidadTramitadora` sí, pero entonces ese colectivo no se usa en `<entities>`.
- Un colectivo que no usa nadie es error del test de cobertura.
- Cambiar la lista de un colectivo cambia, en el siguiente build, las filas de todos los recursos que lo usan.
- `GESTOR` incluye `<usuario admin="true"/>` a propósito (hoy `GestorNotificacionesUtil` no lo incluye, pero el admin debe ver el menú); en `<actionMethods>` y `<entities>` ese `admin` se descarta (T-1).

### A.5 `object`, alcance y comodines

**Alcance de un fichero** = el paquete que contiene su carpeta `security/` y sus subpaquetes.

| Dónde está el fichero                 | Alcance                   | Cómo se escribe `object`                                                                                                                                                       |
|---------------------------------------|---------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| bajo `system/<x>/` o `subsystem/<x>/` | el sistema `<x>`          | `<actionMethods>`/`<entities>`: relativo (`MiController.metodo`, `MiEntidad`), EFBT completa `<base>.controller.` / `<base>.db.`; no se admite FQN. `<actions>`: nombre entero |
| en cualquier otra carpeta             | el paquete de esa carpeta | nombre completo (`com.educaflow.tramites.util.firma.FirmaClienteController.metodo`)                                                                                            |

Ejemplo dentro de un sistema (`subsystem/criptografia/security/acl.xml`):

| Bloque            | Se escribe                                          | Nombre completo                                                                                  |
|-------------------|-----------------------------------------------------|--------------------------------------------------------------------------------------------------|
| `<actionMethods>` | `CertificadoDigitalController.getSituacionFirma`    | `com.educaflow.subsystem.criptografia.controller.CertificadoDigitalController.getSituacionFirma` |
| `<actions>`       | `subsysCriptografia.Main@CertificadoDigital-action` | el mismo                                                                                         |
| `<entities>`      | `CertificadoDigital`                                | `com.educaflow.subsystem.criptografia.db.CertificadoDigital`                                     |

**Forma de los comodines**: `*` solo al final y como segmento entero.

| Bloque            | `object` válidos                                                              |
|-------------------|-------------------------------------------------------------------------------|
| `<actionMethods>` | `Ctrl.metodo`, `Ctrl.*`, `*`                                                  |
| `<actions>`       | nombre entero o `*` (los `.` y `@` de los nombres de acción no son jerarquía) |
| `<entities>`      | nombre exacto o `*`                                                           |

No valen `get*`, `*Controller`, `subsysCriptografia.*`, `subsysCriptografia.Main@*` (lo valida el XSD con `pattern`).

Fuera de un sistema los comodines se escriben con el nombre completo (`com.educaflow.base.infrastructure.controller.MiController.*`), y `*` abarca todos los recursos del alcance del fichero.

**Especificidad: gana el más específico, y sustituye (no suma).**
Para cada recurso concreto se aplica **una sola** regla, la del `object` más específico que encaja con él; las menos específicas se ignoran para ese recurso, no se suman.
Así un comodín fija lo general y un `object` concreto declara la excepción sin tener que deshacer nada:
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
| Recurso                 | Regla que gana        | Quién puede                            |
|-------------------------|-----------------------|----------------------------------------|
| `OtroController.listar` | `*`                   | cualquier usuario con sesión           |
| `MiController.ver`      | `MiController.*`      | solo supervisores                      |
| `MiController.borrar`   | `MiController.borrar` | solo el director (no los supervisores) |

Si se sumaran, `MiController.borrar` lo podría ejecutar cualquier usuario con sesión por la regla `*`, y la única forma de restringir un recurso sería no tener ningún comodín por encima.
Con la sustitución, leer la regla de un recurso es buscar su `object` más específico, sin mirar el resto del fichero.
- Los `object` que encajan con un recurso son el propio recurso y sus prefijos por segmentos seguidos de `.*`, hasta `*`: forman una cadena, nunca hay empate.
- Las filas del ganador son **todas** las del recurso: un método puede ser más restrictivo que su controlador.
  Consecuencia en `<actions>`: si `*` tiene `<usuario admin="true"/>` y una acción solo `<usuario tipoUsuario="SUPERVISOR"/>`, el admin pierde esa acción y su menú salvo que se repita `<usuario admin="true"/>` (deliberado; documentarlo en el skill).
  En `<actionMethods>` no pasa: el admin está exento (A.3).
- **Un comodín no admite la excepción «solo el admin»**: como no existe el `<ace>` sin `<usuario>` (A.3), bajo `MiController.*` no hay forma de dejar `MiController.borrar` solo al admin; hay que estrechar el comodín (declarar los métodos uno a uno, o `MiController.*` con menos y los demás sueltos).
- En `<entities>` el ganador es el **conjunto** de `<ace>` con ese `object`, y sustituye a los menos específicos para **todos** los flags a la vez.
  En el ejemplo de `notificaciones` (A.8), el `<usuario autenticado="true"/>` del `*` pierde también el `canRead` sobre `Notificacion`, porque el `<ace>` de `Notificacion` solo lo da a SUPERVISOR y SECRETARIO.

### A.6 Recursos que no están en ningún sistema

| Recurso                                                                                                                           | Paquete                             | Fichero previsto                       |
|-----------------------------------------------------------------------------------------------------------------------------------|-------------------------------------|----------------------------------------|
| `base/infrastructure/controller/*` (`DefaultModelController`)                                                                     | `com.educaflow.base.infrastructure` | `base/infrastructure/security/acl.xml` |
| `FirmaClienteController`                                                                                                          | `com.educaflow.tramites.util.firma` | `tramites/util/firma/security/acl.xml` |
| acciones de `tramites/shared/template-views.xml` que no son por tipo (`subsysExpedientes.Expediente-verHistorialEstados-action`…) | `com.educaflow.tramites.shared`     | `tramites/shared/security/acl.xml`     |
| entidades de AOP (`MetaFile`, `DMSFile`, `User`)                                                                                  | `com.axelor`                        | `com/axelor/security/acl.xml`          |

- Fuera de un sistema la clase puede estar en cualquier paquete del alcance (sin `controller`/`db` fijo).
- Las entidades de AOP no son ninguna excepción: el proyecto ya tiene el paquete `com.axelor` (`src/main/java/com/axelor/auth/service/`), así que `com/axelor/security/acl.xml` tiene alcance `com.axelor` y las nombra con FQN por la regla general.
  Ningún otro alcance las contiene, así que nadie más puede declararlas y V-09 ve todos sus duplicados.
- `User` es de AOP aunque el proyecto la extienda en `subsystem/common/domains/User.xml` (`package="com.axelor.auth.db"`): se declara desde `com/axelor/security/`, no desde `subsystem/common/security/` (V-07).

### A.7 Acciones de los trámites

Las acciones con nombre de cada tipo de expediente (las de los `views.xml` de sus fases y las generadas desde `template-views.xml`) **no se declaran a mano**: EFBT genera por tipo un `<ace>` en `<actions>` con `<usuario autenticado="true"/>` (B.3, T-7).
- Nadie escribe un `security/` bajo `tramites/<tramite>/`.
- La defensa real es el motor: `Tramitador` comprueba estado, evento y perfil del usuario sobre el expediente concreto.
- Los métodos de `subsystem/tramitador` y `subsystem/expedientes` que mueven la máquina de estados se declaran en su `security/` con `<usuario autenticado="true"/>`.

### A.8 Ejemplos

`subsystem/notificaciones/security/acl.xml`:

```xml
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

`subsystem/registroentradasalida/security/acl.xml` (derivado y endpoint público):

```xml
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

`system/ventanilla/security/acl.xml` (derivados por perfil y por unidad tramitadora, las reglas de los menús de tramitación de D.4):

```xml
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
    <!-- tramita algún trámite de Secretaría -->
    <ace object="sysVentanilla.SecretariaAbiertos@Expediente-action">
      <usuario unidadTramitadora="SECRETARIA"/>
    </ace>
    <!-- tramita algún trámite de Jefatura de estudios -->
    <ace object="sysVentanilla.JefaturaAbiertos@Expediente-action">
      <usuario unidadTramitadora="JEFATURA_ESTUDIOS"/>
    </ace>
  </actions>

</security>
```

### A.9 Misma forma de destinatario que en `AceProfile*`

Los `<acl>` de perfiles (`AceProfileGlobal.xml`, `AceProfileTipoUsuarioTramite.xml`, `TramiteInstance.xml`, `TipoExpedienteInstance.xml`) ya escriben el destinatario como `<usuario cargo="DIRECTOR"/>` / `<usuario tipoUsuario="PROFESOR"/>`, con un solo atributo por `<usuario>` y abortando el build si hay dos (`files/ace/Ace.java` de EFBT).
El formato de `security/*.xml` adopta **exactamente esa forma**, así que `AceProfile*` **no cambia**: ni sus ficheros, ni sus `bind`, ni los generadores de EFBT.

Diferencias que siguen existiendo, y por qué no se unifican más:
- `AceProfile*` solo admite `tipoUsuario` y `cargo` (son las columnas que tienen); `admin`, `autenticado`, `todos`, `perfil`, `unidadTramitadora` y `colectivo` son solo de `security/*.xml`.
  Un colectivo con `<usuario admin="true"/>` no tendría columna en `AceProfile*`.
- `<tramite tipoUsuario="…"/>` de `AceProfileTipoUsuarioTramite` no es destinatario: se queda como está.
- La comprobación «un solo atributo» la hace en los dos sitios EFBT en Java (V-18), no el XSD: XSD 1.0 no puede exigir «exactamente uno de estos atributos» (haría falta `xs:assert` de XSD 1.1, que el validador del JDK no soporta).

---

## 6. Parte B — EFBT: validar y transformar

Tarea nueva `generateSecurity` (`JavaExec`, antes de `processResources`); al cambiar EFBT, `mvn install` en `../EducaFlowBuildTools`.
Los XSD `security_1.0.xsd` y `security-config_1.0.xsd` viven solo en EFBT.

### B.1 Descubrimiento

1. Recorre **todos** los `src/main/java/**/*.xml`, en cualquier carpeta, y se queda con los del namespace `security`.
   En el mismo recorrido localiza los del namespace `security-config` (A.4), comprueba que hay uno solo (V-11) y lo valida con su XSD.
   Que cada fichero esté en una carpeta `security/` lo comprueba después V-17: buscar en cualquier carpeta es lo que permite detectar uno mal colocado en vez de ignorarlo.
2. Por la ruta decide el alcance de cada fichero: si entre `com/educaflow/` y `security/` hay `system/<x>` o `subsystem/<x>`, es ese sistema; si no, el paquete de la carpeta.
3. Enumera los **recursos concretos** de cada alcance:
   - controladores leídos **del código fuente con el compilador del JDK** (`javax.tools` / Compiler Tree API, como los tests de `ordenmetodos`; la tarea va antes de compilar):
     - métodos públicos `(ActionRequest, ActionResponse)` (todos invocables, con o sin `@CallMethod`);
     - endpoints JAX-RS (`@GET`/`@POST`/`@PUT`/`@DELETE` en clase con `@Path`) con su ruta completa, para saber si cuelgan de `/ws/public/**`;
   - acciones con nombre de todos los XML de vistas Axelor (`object-views`) del alcance (`views/`, `controller/*.xml`, `views.xml` de fases…), salvo `action-method`;
   - las acciones que EFBT genera por tipo de expediente desde `template-views.xml` (las conoce porque las genera);
   - entidades de los `domains/*.xml`, con sus `many-to-one` a `Centro` (y los heredados de `Expediente` en las subclases);
   - en el alcance `com.axelor`, además, las entidades de AOP (clases del classpath de AOP que heredan de `Model`), con los campos que les añadan los `domains/*.xml` del proyecto;
   - las acciones de `menus.xml` y el árbol de menús (para T-8).

### B.2 Validaciones

Primero sobre los XML originales; las marcadas con † se comprueban **tras expandir colectivos** y el error nombra el colectivo.

| Id     | Comprobación                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  | Dónde           |
|--------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------|
| V-01   | El fichero cumple el XSD (bloques, tipo de `<ace>` por bloque, flags admitidos, `canExecute` solo `true`, `pattern` de comodines, `<ace>` sin ningún `<usuario>` (en los tres bloques), atributos de `<usuario>` admitidos en cada bloque —`admin` solo existe en el de `<actions>`; `todos`, `perfil` y `unidadTramitadora` no existen en el de `<entities>`—, booleanos solo `true`, `conditionParams` sin `condition`); el de configuración cumple `security-config_1.0.xsd` (el `<usuario>` de un colectivo no admite `todos` ni `colectivo`) | XSD             |
| V-02   | Un fichero fuera de un sistema no tiene ningún `system/<x>`/`subsystem/<x>` por debajo (si no, un `security/` en `com/educaflow/` daría permisos sobre cualquier cosa)                                                                                                                                                                                                                                                                                                                        | EFBT            |
| V-03   | Las carpetas `security/` de fuera de sistemas no se anidan                                                                                                                                                                                                                                                                                                                                                                                                                                    | EFBT            |
| V-04   | No hay `security/` bajo `tramites/<tramite>/`                                                                                                                                                                                                                                                                                                                                                                                                                                                 | EFBT            |
| V-05   | Ningún `object` sale de su alcance: en sistema la clase está **directamente** en `<base>.controller` (sin subpaquetes, y aborta si encuentra un controlador en uno) o es una entidad de los `domains` del sistema cuyo `package` es `<base>.db`; la acción está declarada en un XML de vistas del alcance (no basta el prefijo); fuera de sistema el FQN empieza por el paquete de la carpeta                                                                                                 | EFBT            |
| V-06   | Todo `object` exacto existe (método, acción, entidad)                                                                                                                                                                                                                                                                                                                                                                                                                                         | EFBT            |
| V-07   | Una entidad de un `domains/*.xml` de un sistema cuyo `package` no es `<base>.db` (p. ej. `User`, `com.axelor.auth.db`) no se declara desde ese sistema, solo desde el alcance de su paquete                                                                                                                                                                                                                                                                                                   | EFBT            |
| V-08   | Ningún `<ace>` de `<actions>` nombra un `action-method`                                                                                                                                                                                                                                                                                                                                                                                                                                       | EFBT            |
| V-09   | `object` duplicado en el mismo alcance y bloque (en todos sus ficheros); en `<entities>` solo si coinciden flags a `true`, `condition` y `conditionParams`                                                                                                                                                                                                                                                                                                                                    | EFBT            |
| V-10   | Ningún controlador tiene dos métodos invocables con el mismo nombre (sobrecarga)                                                                                                                                                                                                                                                                                                                                                                                                              | EFBT            |
| V-11   | Hay exactamente un fichero del namespace `security-config` y está en `subsystem/security/security/security-config.xml`; nombre de colectivo único; todo `<usuario colectivo="…"/>` usado existe                                                                                                                                                                                                                                                                                               | EFBT            |
| V-12 † | Ningún derivado llega a `<entities>` a través de un colectivo                                                                                                                                                                                                                                                                                                                                                                                                                                 | EFBT            |
| V-13 † | `<usuario todos="true"/>` solo en endpoints JAX-RS bajo `/ws/public/**`, y en esos es el único destinatario                                                                                                                                                                                                                                                                                                                                                                                   | EFBT            |
| V-14   | Un `<ace>` de `<entities>` tiene al menos un flag a `true`                                                                                                                                                                                                                                                                                                                                                                                                                                    | EFBT            |
| V-15   | `propiedadCentro`: obligatorio con dos o más `many-to-one` a `Centro`; error con uno o ninguno, si no existe o no es `many-to-one` a `Centro`, o en un `object` con comodín; aborta si un comodín gana una entidad con dos                                                                                                                                                                                                                                                                    | EFBT            |
| V-16   | Para una entidad y un flag no se generan a la vez un `Permission` sin `condition` y otro con `condition` (AOP los trata distinto por id y en el grid); nombra los `<ace>` implicados. Solo puede pasar en una entidad sin propiedad `Centro`, con un `<usuario autenticado="true"/>` sin `condition` propia                                                                                                                                                                                   | EFBT (tras T-5) |
| V-17   | Todo fichero del namespace `security` está directamente en una carpeta `security/` (por arquitectura, en sistema es `<sistema>/security/`); si no, error con su ruta                                                                                                                                                                                                                                                                                                                          | EFBT            |
| V-18   | Cada `<usuario>` (en `security/*.xml` y en los colectivos de `security-config.xml`) tiene **exactamente un** atributo; con cero o con dos, error que nombra el fichero y el `object` (XSD 1.0 no puede expresarlo; misma regla que `Ace.java` aplica hoy a `AceProfile*`)                                                                                                                                                                                                                     | EFBT            |

### B.3 Transformaciones

En este orden:

**T-1. Expandir colectivos.**
Sustituye cada `<usuario colectivo="…"/>` por sus destinatarios, **antes** de los comodines; un destinatario repetido se deduplica en silencio.
En `<actionMethods>` y `<entities>` el `admin` del colectivo se descarta en silencio (A.3); si tras descartarlo el `<ace>` se queda sin `<usuario>`, error que nombra el colectivo.

**T-2. Completar `canExecute`** en `<actionMethods>`/`<actions>` cuando falta.

**T-3. Expandir comodines** por cada recurso concreto de cada alcance:
1. busca un `<ace>` con `object` igual al recurso;
2. si no, quita el último segmento y busca `<prefijo>.*`, subiendo;
3. si no, `*`;
4. el primero que encuentra gana y genera las filas del recurso (una por destinatario y permiso);
5. sin ninguno, el recurso no tiene filas: prohibido (en métodos y entidades, prohibido para todos menos el admin).
- Un `Ctrl.*` o `*` que caiga sobre un endpoint público **no le da filas**: cada ruta sin login tiene que estar escrita a la vista con `<usuario todos="true"/>`.

**T-4. Generar el data-init de `AceRecurso`** (`AceRecurso.xml` + `input-config.xml`) con las filas de métodos y acciones ya expandidas, en el data-init que copia el build.
Las FK (`TipoUsuario`, `Cargo`, `UnidadTramitadora`) se enlazan por `search` sobre su código, como en `AceProfile*`; `perfil` es un enum y va por su valor, como el `perfil` de `AceProfile*`.

**T-5. Generar los `Permission` de entidades**, con su `<bind node="group">` al grupo `users`:
- **Uno por `<ace>` ganador y entidad** (un `Permission` lleva los seis flags y una sola `condition`), con los flags que el `<ace>` tenga a `true`; `object` = FQN de la entidad.
- **Nombre** `acl.<Entidad>.<flags>[.<n>]`:
  - `<Entidad>` nombre simple (único en la unidad de persistencia);
  - `<flags>` en orden canónico `read-write-create-remove-export-import`, sin `can`, en minúsculas, unidos por `-`;
  - `.<n>` (desde `2`) cuando dos `<ace>` ganadores de la misma entidad tienen los mismos flags, por orden de fichero (alfabético) y de documento; renombrar es inocuo porque `auth_permission` se vacía en cada arranque;
  - ejemplos: `acl.TareaFirma.read-write`, `acl.Expediente.read.2`, `acl.MetaFile.create`;
  - con los dos `<ace>` `canRead` con `condition` A y B de A.2 y otro `canWrite`/`canCreate`/`canRemove` sin `condition`, `Notificacion` tiene **tres** `Permission`: `acl.Notificacion.read`, `acl.Notificacion.read.2` y `acl.Notificacion.write-create-remove`;
  - el prefijo `acl.` los separa de los `Permission` escritos a mano que sigan vivos durante la migración (`SELECT name FROM auth_permission WHERE name LIKE 'acl.%'`).
- **`condition` = ancla + fragmentos + condición propia**, con **un único `?`** para el usuario:
  ```
  EXISTS (SELECT cu FROM com.educaflow.subsystem.common.db.CentroUsuario cu
          WHERE cu.usuario = ?
            AND cu.centro = self.<propiedadCentro>        ← solo si la entidad tiene propiedad Centro
            AND (<fragmento 1> OR <fragmento 2> OR …))    ← solo si hay tipoUsuario o cargo
  ```
  | Destinatario                    | Fragmento                                                                                                                                                                                      |
  |---------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
  | (admin)                         | no existe en `<entities>` (A.3): AOP se salta los `Permission` del admin (`AuthSecurity.getUser()` es `null`). Una entidad sin `<ace>` **no tiene `Permission`**: solo el admin                   |
  | `<usuario autenticado="true"/>` | ninguno (basta el ancla); si convive con `tipoUsuario`/`cargo` se omite el paréntesis. Sin `Centro` y sin `condition` propia, el `Permission` va **sin `condition`** y `conditionParams` vacío |
  | `<usuario tipoUsuario="X"/>`    | `EXISTS (SELECT cut FROM com.educaflow.subsystem.common.db.CentroUsuarioTipoUsuario cut WHERE cut.centroUsuario = cu AND cut.tipoUsuario.codigo IN ('X', …))` — todos en un `IN`               |
  | `<usuario cargo="X"/>`          | `EXISTS (SELECT cuc FROM com.educaflow.subsystem.common.db.CentroUsuarioCargo cuc WHERE cuc.centroUsuario = cu AND cuc.cargo.code IN ('X', …))` — ojo: `Cargo.code`, no `codigo`               |
  - Condición propia: `(<ancla>) AND (<condition del ace>)` con `conditionParams="__user__,<los del ace>"`.
  - No se puede reutilizar el `__user__` del ancla desde un `?` del `<ace>` ni escribir `?1`: AOP enlaza por posición y `Filter.build` renumera todos los `?`.
  - Propiedad `Centro`: un `many-to-one` a `Centro` declarado o heredado; las colecciones no cuentan.
    Hoy tienen una `Expediente` (y subclases), `TareaFirma`, `Notificacion`, `RegistroEntrada`, `RegistroSalida`, `CentroUsuario`, `AceProfileCentro`, `UsuarioAutorizado`, `TareaImportacion` y `AsistenteNuevoExpediente`; ninguna tiene dos.
  - El filtro de centro es un **suelo**, no el permiso, y solo protege filas que ya existen (`create` no tiene fila): que una fila nazca o se quede en un centro del usuario sigue en el servicio (`k-secure-coding`).
    No hay forma de desactivarlo en una entidad que tenga `Centro`; si aparece un caso, se decidirá entonces.
  - `Permission.condition` admite 4096 caracteres en el fork de AOP (`Permission.xml:21`, antes 1024); EFBT comprueba la longitud generada contra esa constante y aborta diciendo cuánto ocupa y en qué `<ace>`.
- Ejemplo: `<ace object="TareaFirma" canRead="true" canWrite="true" condition="self.firmante = ?" conditionParams="__user__">` con `<usuario tipoUsuario="SUPERVISOR"/>` y `<usuario cargo="SECRETARIO"/>` da `acl.TareaFirma.read-write`, `conditionParams="__user__,__user__"` y:
  ```
  (EXISTS (SELECT cu FROM com.educaflow.subsystem.common.db.CentroUsuario cu
           WHERE cu.usuario = ? AND cu.centro = self.centro
             AND (EXISTS (SELECT cut FROM com.educaflow.subsystem.common.db.CentroUsuarioTipoUsuario cut
                          WHERE cut.centroUsuario = cu AND cut.tipoUsuario.codigo IN ('SUPERVISOR'))
                  OR EXISTS (SELECT cuc FROM com.educaflow.subsystem.common.db.CentroUsuarioCargo cuc
                             WHERE cuc.centroUsuario = cu AND cuc.cargo.code IN ('SECRETARIO')))))
  AND (self.firmante = ?)
  ```

**T-6. Copiar los `<ace>` de `Expediente` a cada subclase** (en el generador por tipo, `createdatainittipoexpediente`):
- copia los `<ace>` de `<entities>` de `Expediente` (condiciones incluidas) cambiando el `object` y dejando **solo `canRead`**;
- descarta los que se quedan sin flags y **fusiona** los idénticos (misma `condition`/`conditionParams`) uniendo destinatarios, sin abortar;
- numera `.<n>` como en T-5.
Nunca `canCreate`/`canWrite`/`canRemove` en una subclase: `POST /ws/rest/<Subclase>` y el `remove` devuelven 403, y un expediente solo cambia por un evento que valida `Tramitador` (que guarda con `JPA.save`, sin pasar por esa puerta).

**T-7. Seguridad de los trámites**: por cada tipo de expediente, un `<ace>` en `<actions>` con `<usuario autenticado="true"/>` que cubre todas sus acciones con nombre (las de sus fases y las generadas desde `template-views.xml`), en el mismo sitio donde genera su data-init.

**T-8. Menús** (solo en el bloque 9 del plan de trabajo):
- `<menuitem>` con `action` → `if="__config__.menuSecurity.isVisible('<action>')"`;
- `<menuitem>` padre → `if="__config__.menuSecurity.isAnyVisible('<a1>', '<a2>', …)"` con las acciones de **todos** sus descendientes (AOP no oculta un padre cuyos hijos están todos ocultos);
- inyecta `groups="admins,users"` en todo `<menuitem>` (sin él un menú raíz no se ve para no administradores, `MenuChecker.isAllowed`) y deja de escribirse a mano;
- el admin se salta `groups` pero **no** el `if`: donde deba ver el menú necesita `<usuario admin="true"/>`.

### B.4 Tests unitarios de EFBT

- Expansión: cadena de prefijos, sustitución (no suma), duplicados, recurso sin `<ace>`, endpoint público bajo comodín.
- Colectivos: sustitución, deduplicado, nombre inexistente, derivado en `<entities>` a través de un colectivo.
- `canExecute` que falta.
- `<usuario>`: con un atributo válido, con cero y con dos (error), atributo no admitido en el bloque (error del XSD, incluido `admin` en `<entities>`), `admin="false"` (error del XSD).
- `<ace>` sin `<usuario>` es error en los tres bloques; colectivo con `admin` en `<actionMethods>`/`<entities>` (se descarta; si se queda vacío, error).
- `Permission`: `condition` exacta por combinación (entidad sin `<ace>` → sin `Permission`; solo `<usuario autenticado="true"/>` sin centro → sin `condition`; `tipoUsuario`+`cargo` → un `IN` por tabla), ancla con y sin `Centro`, AND con la condición propia y orden de `conditionParams`, `propiedadCentro` (0, 1, 2 propiedades; colección; sobra, falta, en comodín), nombre (orden canónico y `.2`), mismo `object` (distintos flags válido; mismos flags y condición error), sin flags (error), mezcla incondicional/condicional (error), copia a subclases (descarte y fusión).

---

## 7. Parte C — `subsystem/security` en tiempo de ejecución

Solo afecta a métodos y acciones (y por ellos, menús): las entidades las evalúa AOP con los `Permission` generados, sin código nuestro.
Hoy el único permiso es `EXECUTE`.

### C.1 Tabla `AceRecurso`

Dominio `subsystem/security/domains/AceRecurso.xml`, repositorio en `db/repo/`, tabla `security_ace_recurso`.

| Columna                            | Contenido                                                                                                                                                                                           |
|------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `tipoRecurso`                      | `ACTION_METHOD`, `ACTION` (las entidades no tienen filas)                                                                                                                                           |
| `recurso`                          | nombre completo y concreto: `com.x.controller.Ctrl.metodo` o el nombre de la acción                                                                                                                 |
| `permiso`                          | hoy solo `EXECUTE`                                                                                                                                                                                  |
| destinatario (**exactamente uno**) | `admin` (bool), `autenticado` (bool), `todos` (bool), `tipoUsuario` (FK), `cargo` (FK), `perfil` (enum `Profile`, de `subsystem/expedientes`), `unidadTramitadora` (FK, de `subsystem/expedientes`) |

- Una sola tabla: todo sale del mismo XML y todas las filas tienen las mismas columnas; con `permiso` como columna, métodos y acciones se guardan, cargan y consultan igual (`recurso = … AND permiso = …`).
- Separada de `AceProfile*` a propósito: comparten la **forma** del destinatario, no la tabla.
- **MUST** añadirse a `tablasIncluidas` de `DataBaseStartup`: el data-init solo hace upsert y las filas quitadas no se borrarían.
- `auth_permission` no hace falta añadirla: `BulkTables` ya trunca `auth_%` (con `CASCADE`) en cada arranque.
  Eso incluye sus intermedias `auth_group_permissions` y `auth_user_permissions` (las excluidas son `meta_file`, `meta_sequence`, `auth_user`, `auth_group` y `meta_filter`), así que al borrar un `auth-*.xml` sus `Permission` desaparecen en el siguiente arranque sin hacer nada más.

### C.2 Carga y caché: ¿puede hacerlo JPA solo?

**Sí, y es la opción recomendada**, en lugar de una caché en memoria escrita a mano.
(Respuesta guardada en `../axelor-open-platform/preguntas/modelos.md`.)

- AOP activa la caché de segundo nivel **y** la de consultas de Hibernate cuando `jakarta.persistence.sharedCache.mode` no es `NONE` (`DBHelper.isCacheEnabled`, `JpaModule#configureCache`).
- El proyecto ya la tiene activa: `jakarta.persistence.sharedCache.mode = ENABLE_SELECTIVE` y `application.cache.provider = caffeine` (`axelor-config.properties:157,246`), aunque hoy ninguna entidad la usa.
- Lo que hay que hacer:
  - `<entity name="AceRecurso" cacheable="true">` en el `domains.xml` (genera `@Cacheable`);
  - la consulta del repositorio con `.cacheable()`: `all().filter("self.recurso = ?1 AND self.permiso = ?2", …).cacheable().fetch()`.
- Ventajas frente a la caché a mano:
  - no hay código de caché que escribir ni probar;
  - desaparece el problema del **orden de arranque** (cargar antes del data-init dejaría la caché vacía y, en `enforce`, todo prohibido): la caché se llena perezosamente en la primera consulta;
  - si el data-init reescribe la tabla vía Hibernate, Hibernate invalida la región solo;
  - el truncado de arranque es JDBC directo pero ocurre antes de crear el `EntityManagerFactory`, con la caché (en memoria) aún vacía.
- Cuidado:
  - la caché de consultas guarda ids; las filas salen de la caché de segundo nivel, así que `AceRecurso` **MUST** ser `cacheable`;
  - al evaluar, comparar las FK **por id** (`fila.getTipoUsuario().getId()` no inicializa el proxy) o marcar también `TipoUsuario`, `Cargo` y `UnidadTramitadora` como `cacheable`, para no hacer una consulta por fila;
  - una consulta cacheada por recurso sigue siendo más cara que un `HashMap`; los menús llaman a `isVisible` muchas veces por render.
    Se mide en la tarea C-3 (log `org.hibernate.orm.cache` en `DEBUG` para ver aciertos); si no basta, se vuelve a la caché a mano cargada en un observador de `StartupEvent`.
- Lo que **no** cachea JPA es la otra mitad de la pregunta: los tipos de usuario, cargos y perfiles de tramitador **del usuario** (consultas a `CentroUsuario*` y `AceProfile*` en cada comprobación).
  Abierto: decidir en C-3 si se cachea por petición o por sesión, o si basta con marcar esas entidades como `cacheable`.

### C.3 API Java

- **`AclService`**
  - `puedeEjecutarMetodo(User user, String metodo)` (`com.x.controller.Ctrl.metodo`) y `puedeEjecutarAccion(User user, String accion)`.
  - `puedeEjecutarMetodo` devuelve `true` para el admin (`SecurityUtil.isAdmin`) sin consultar la tabla (A.3).
    `puedeEjecutarAccion` **no**: el admin solo ejecuta (y ve en el menú) las acciones que lo declaran.
  - Lee las filas `EXECUTE` del recurso (caché de C.2) y devuelve `true` si algún destinatario encaja (O).
  - Sin lógica de comodines: la tabla ya es concreta. Sin filas → `false`.
- **`DestinatarioEvaluator`**: decide si un usuario encaja con el destinatario de una fila.
  - `todos`: siempre, sin mirar si hay usuario (en un endpoint público `SecurityUtil.getUser()` es `null`).
  - `admin` (solo hay filas en acciones), `autenticado`, `tipoUsuario`, `cargo` (en algún centro): con `SecurityUtil.getUser()`, `SecurityUtil.isAdmin` y `user.tieneTipoUsuario`/cargo; sin usuario ninguno encaja.
  - `perfil`: `PerfilesUsuarioService.tienePerfil` (mismo subsistema).
  - `unidadTramitadora`: `PerfilesUsuarioService.isTramitador` (mismo subsistema).
- Ambos con tests unitarios (cuentan para `crapCheck`).

### C.4 Puntos de control (quién llama a la API)

**`AclActionObserver`** — `@Observes PreAction` sin `@Named`, enlazado con `bind(AclActionObserver.class)` en `SecurityModule` (sin binding `EventBus` no lo ve).
1. Averigua el recurso por el nombre:
   - `vista:…` (primer segmento tipo de vista) → `action-view` ad hoc: **se bloquea** (no es declarable; `axelor-front` no los construye, así que solo vienen de peticiones a mano; el log de `warn` confirmará que no aparece ninguno);
   - con `:` → `ActionMethod` ad hoc: `controlador.metodo` (quitando `(args)`);
   - sin `:` → `MetaStore.getAction(nombre)`: si es `ActionMethod`, su `getCall()` (`ACTION_METHOD`); si no, su nombre (`ACTION`).
2. Solo filtra lo nuestro: un `ActionMethod` si su clase está en `com.educaflow`; cualquier otra acción si el `module` de su `MetaAction` es `secretaria-virtual` (las de AOP son `axelor-core`/`axelor-web`).
   Un `action-group` ad hoc (`a,b,c`) no es recurso: se comprueba cada hija al pasar por `Action.execute`.
3. Sin usuario en sesión (proceso de sistema) deja pasar, como `AuthSecurity.isPermitted`; con el admin deja pasar los `ACTION_METHOD` y comprueba los `ACTION` (A.3).
   Hoy ningún código del proyecto ejecuta acciones XML por nombre desde Java, pero si algún día lo hace un job no debe bloquearse.
4. Sin permiso → `AclDeniedException extends org.apache.shiro.authz.UnauthorizedException`, la misma para todo tipo de acción, con el recurso en el mensaje: el cliente ve «Access error», el log una línea sin traza y se hace rollback.
   Sin pantalla «sin permiso»: quien llega ahí ha saltado la interfaz.
   **MUST NOT** hacerse en `PostAction` (la acción ya se habría ejecutado).

**`AclRestFilter`** — `ContainerRequestFilter` con `@Provider`, enlazado con `bind(AclRestFilter.class)` (RESTEasy registra lo enlazado en Guice con `@Path`/`@Provider`).
- Método destino con `@Context ResourceInfo` (`getResourceClass()` + `getResourceMethod()`).
- Solo controladores de `com.educaflow`; `/ws/rest`, `/ws/meta`, `/ws/files`… no se tocan. El admin pasa siempre (A.3).
- Sin permiso → `abortWith(403)`.

**`MenuSecurityServiceImpl`** (solo en el bloque 9): `isVisible(action)` consulta `AclService.puedeEjecutarAccion`; `isAnyVisible(...)` cualquiera de ellas; desaparece el `switch`.
Como los destinatarios derivados (B7) llegan antes, el `switch` desaparece de golpe: no hay un estado intermedio con unos menús en el `switch` y otros en acciones.
Efecto añadido: quien no ve el menú tampoco puede abrir su acción (hoy cualquiera abre `sysVentanilla.SecretariaAbiertos@Expediente-action`).
Ninguna acción se repite entre menús, pero alguna puede abrirse además desde un botón para otros usuarios: lo destapará el log de `warn`.

### C.5 Modo transitorio `warn` / `enforce`

- `security.acl.mode = warn | enforce` en `axelor-config.properties` (`warn` mientras dure la migración).
- `warn`: observador y filtro no bloquean; registran cada denegación (recurso, login, motivo), sin más datos personales.
- Interruptor **global**; un sistema está revisado cuando la suite E2E pasa sin denegaciones de sus recursos en el log.
- Menús y `Permission` de entidades no tienen modo `warn` (los menús se cambian al final; las entidades se activan sistema a sistema).
- Se elimina el modo y la propiedad al terminar.

### C.6 Lo que desaparece

- `EducaFlowAuthResolverImpl` (sustituye los `Permission` de una subclase por los de `Expediente`), su registro en `SecurityModule` y, si no le queda uso, el parche `EducaFlowAuthResolverRegistry` de AOP: con T-6 cada subclase tiene sus propios `Permission`.
- `GestorNotificacionesUtil` (lo cubren el colectivo `GESTOR` y el filtro de centro).
- El `switch` de `MenuSecurityServiceImpl` y los `groups` escritos en `menus.xml`.

### C.7 Tests del proyecto (a mano, en `src/test/java/com/educaflow/security`)

- **Sin cobertura de métodos ni de entidades**: un método o una entidad sin `<ace>` es «solo el admin» (A.3) y no se distingue de un olvido; el olvido lo destapa el log de `warn` o la suite E2E, y el lado por defecto es el cerrado.
- **Endpoints públicos**: todo endpoint bajo `/ws/public/**` tiene su `<ace>` con `<usuario todos="true"/>` como único destinatario, y ningún `<usuario todos="true"/>` cae fuera (inventario de lo accesible sin sesión).
- **Cobertura de acciones**: toda acción con nombre (salvo `action-method`) cubierta por `<actions>`, y ningún `<ace>` de `<actions>` nombra un `action-method`.
- **Comodines vivos**: todo comodín gana al menos un recurso.
- **Colectivos**: todo colectivo se usa; un único `security-config.xml`, en `subsystem/security/security/`; nombres únicos.
- **Menús**: toda `action` de `menus.xml` cubierta en `<actions>`.
- Los de cobertura (acciones y menús) recorren también los recursos de fuera de los sistemas (A.6): no hay lista de exclusión.
- Los de cobertura nacen con `@Disabled("hasta migrar todos los sistemas, plan_security")`: `./run.sh` fallaría durante la migración; se lanzan a mano como inventario.
- Unitarios de `AclService` y `DestinatarioEvaluator`.

---

## 8. Parte D — Migración

### D.1 Ficheros de seguridad a crear

Uno por alcance, escritos por Claude y revisados por el usuario.

| Fichero                                                       | Controladores                                                | Acciones (aprox.)                               | Entidades                                  | Sustituye                                                                                     |
|---------------------------------------------------------------|--------------------------------------------------------------|-------------------------------------------------|--------------------------------------------|-----------------------------------------------------------------------------------------------|
| `subsystem/criptografia/security/acl.xml`                     | 2                                                            | 19                                              | 3                                          | `data-init/input/security.xml` (formato inexistente, `input-config.xml` vacío: no carga nada) |
| `subsystem/notificaciones/security/acl.xml`                   | 1                                                            | 35                                              | 4                                          | `auth-notificaciones.xml`                                                                     |
| `subsystem/firmas/security/acl.xml`                           | 1                                                            | 25                                              | 2                                          | `auth-firmas.xml`                                                                             |
| `subsystem/registroentradasalida/security/acl.xml`            | 1 (JAX-RS público)                                           | 6                                               | 2                                          | —                                                                                             |
| `subsystem/registrousuario/security/acl.xml`                  | 1 (JAX-RS público)                                           | 0                                               | 2                                          | —                                                                                             |
| `subsystem/security/security/acl.xml` + `security-config.xml` | 1                                                            | 10                                              | 6 (+ `AceRecurso`)                         | `auth-security.xml`                                                                           |
| `subsystem/common/security/acl.xml`                           | 0                                                            | 30                                              | 14                                         | `auth-common.xml`                                                                             |
| `subsystem/sistemaeducativo/security/acl.xml`                 | 0                                                            | 66                                              | 8                                          | `auth-sistemaeducativo.xml`                                                                   |
| `subsystem/importacion/security/acl.xml`                      | 0                                                            | 7                                               | 1                                          | —                                                                                             |
| `subsystem/pdfutilities/security/acl.xml`                     | 1                                                            | 10                                              | 1                                          | —                                                                                             |
| `subsystem/expedientes/security/acl.xml`                      | 1                                                            | 2                                               | 7 (`Expediente` solo `read` para no admin) | `auth-expedientes.xml` (el último)                                                            |
| `subsystem/tramitador/security/acl.xml`                       | 1 (`<usuario autenticado="true"/>`)                          | 0                                               | 0                                          | —                                                                                             |
| `subsystem/smoketest/security/acl.xml`                        | 0                                                            | 5                                               | 1                                          | `auth-smoketest.xml`                                                                          |
| `system/gestioncentro/security/acl.xml`                       | 1                                                            | 22                                              | 0                                          | `auth-gestioncentro.xml`                                                                      |
| `system/ventanilla/security/acl.xml`                          | 2                                                            | 37                                              | 2                                          | `auth-ventanilla.xml`                                                                         |
| `base/infrastructure/security/acl.xml`                        | `DefaultModelController.*` (`<usuario autenticado="true"/>`) | —                                               | 0                                          | —                                                                                             |
| `com/axelor/security/acl.xml`                                 | 0                                                            | 0                                               | `MetaFile`, `DMSFile`, `User` (FQN)        | permisos del `auth.xml` global                                                                |
| `tramites/util/firma/security/acl.xml`                        | `FirmaClienteController`                                     | 0                                               | 0                                          | —                                                                                             |
| `tramites/shared/security/acl.xml`                            | 0                                                            | las de `template-views.xml` que no son por tipo | 0                                          | —                                                                                             |

`correos` y `sms` no tienen recursos ni fichero.

**De dónde sale el contenido**:
- `<actions>`: la regla de visibilidad de hoy del menú que abre cada acción (`switch` de `MenuSecurityServiceImpl` y `groups` de `menus.xml`, tabla D.4); para las que no abre ningún menú (`onNew`, `onLoad`, `onChange`, botones), los destinatarios del form que las usa; lo que falte lo destapa el log de `warn`.
- `<actionMethods>`: los destinatarios de la acción o form desde el que se llama; los del motor con `<usuario autenticado="true"/>`.
- `<entities>`: **se escribe de nuevo desde lo que debe ser**; los `auth-*.xml` actuales están mal y solo sirven como lista de entidades.
  La fuente de verdad son los tests existentes: un sistema está migrado cuando se ha borrado su `auth-*.xml` y los tests siguen en verde.

### D.2 Casos especiales de entidades

- **`Expediente` y subclases**: `Expediente` solo `read` para no admin, con las condiciones de hoy (`Expediente.creador`, `Expediente.porGlobal`…, una por `<ace>`); las subclases por copia (T-6).
  - Antes de darlo por bueno, en `warn` con la suite E2E: ningún form de expediente usa el guardado estándar (`canSave="false"` explícito en el form de `template-views.xml`; los grids de la plantilla ya lo llevan), y ninguna pantalla de administración edita expedientes por REST (si lo hace, se convierte en acción).
  - **MUST NOT** volver a un `ModelService` con deny-all.
  - Al implementarlo, quitar «PENDIENTE (importante)» de `CLAUDE.md` y dejar la regla en `k-secure-coding`.
  - `auth-expedientes.xml` se migra el último (sus `condition` consultan `AceProfile*`).
- **`HistorialEstado`**: «existe un expediente legible del que cuelga» (`EXISTS (SELECT e FROM Expediente e WHERE e = self.expediente AND …)` con las mismas subconsultas que `Expediente`).
- **`Tramite`**: el `read` para supervisores (`Tramite.supervisor` de `auth-security.xml`, para el selector de «Mi centro → Perfiles de trámites») se reescribe en `subsystem/expedientes/security/acl.xml`.
- **`MetaFile`**: `create` para `<usuario autenticado="true"/>` y **ningún `read` general**.
  La lectura la decide Axelor por el padre (`RestService.download`: creador, o `parentId`/`parentModel` con permiso de lectura del padre y el fichero colgando de un campo suyo).
  Por eso los `<iframe>` de `template-views.xml` y de los `views.xml` de fases **MUST** añadir `&parentId=${id}&parentModel=<FQN de la subclase>`.
- **`DMSFile`**: nada para `users` (todos los `canAttach` son `false`).
- **`User`**: `read` y `write` con `condition="self = ?"` para el propio usuario; lo de los supervisores (`gestioncentro`) con su condición.
- **`auth.xml` global**: tras la migración solo conserva los grupos `admins`/`users` y los usuarios; los `bind` los genera EFBT. Es lo último en irse.

### D.3 Otros cambios de la migración

- Borrar el trámite `prueba` antes de tocar `expedientes`.
- `AceProfile*` no se toca: ya tiene la forma `<usuario …/>` que adopta el formato nuevo (A.9).

### D.4 Visibilidad de los menús de hoy en el formato nuevo

| Menú                                                                                                                                                                  | Regla actual                                          | Con el formato nuevo                                                                                                                                                                                                                        |
|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `groups="admins"` (`firmas-todas`, `tramitacion-todosLosCentros*`, `notificaciones-todas`, `administracion*`, `criptografia*`, `sistemaEducativo*`, `desarrollador*`) | solo admin                                            | `<usuario admin="true"/>`                                                                                                                                                                                                                   |
| `groups="admins,users"` sin regla en el `switch`                                                                                                                      | cualquiera                                            | `<usuario autenticado="true"/>`                                                                                                                                                                                                             |
| `firmas-delCentro`                                                                                                                                                    | supervisor                                            | `<usuario tipoUsuario="SUPERVISOR"/>`                                                                                                                                                                                                       |
| `miCentro` (padre de 3 acciones)                                                                                                                                      | supervisor                                            | `<usuario tipoUsuario="SUPERVISOR"/>` en las 3 hijas                                                                                                                                                                                        |
| `notificaciones-delCentro`                                                                                                                                            | `esGestorEnAlgunCentro`                               | `<usuario colectivo="GESTOR"/>`                                                                                                                                                                                                             |
| `tramitacion` (padre)                                                                                                                                                 | admin o `isTramitador(user)`                          | `<usuario admin="true"/>` + un `<usuario perfil="…"/>` por cada perfil de tramitación (`TRAMITADOR`, `COLABORADOR`, `AFECTADO`, `SECRETARIO`, `DIRECTOR`, `AUDITOR`: todos salvo `CREADOR`) en cada hija (`nuevoTramite`, `pendientesDeMi`) |
| `tramitacion-jefaturaDeEstudios` (padre)                                                                                                                              | `isTramitador(user, JEFATURA_ESTUDIOS)`               | `<usuario unidadTramitadora="JEFATURA_ESTUDIOS"/>` en las hijas                                                                                                                                                                             |
| `tramitacion-secretaria` (padre)                                                                                                                                      | `isTramitador(user, SECRETARIA)`                      | `<usuario unidadTramitadora="SECRETARIA"/>` en las hijas                                                                                                                                                                                    |
| `registro` (padre)                                                                                                                                                    | admin o `isTramitador(user, SECRETARIA)` o supervisor | `<usuario admin="true"/>` + `<usuario tipoUsuario="SUPERVISOR"/>` + `<usuario unidadTramitadora="SECRETARIA"/>`                                                                                                                             |

---

## 9. Plan de trabajo

Cada tarea dice qué parte del plan implementa y cuándo está hecha.
El orden respeta las dependencias del diagrama; lo que está en la misma columna puede hacerse en paralelo.

```
 B0 Preparación ─┐
                 ├─► B1 Base ─► B2 EFBT métodos/acciones ─► B3 Runtime ─► B4 Control en warn ─► B5 Piloto criptografia
 (P-2 en paralelo)                       │                        │                                     │
                                         └─► B6 EFBT entidades     └─► B7 Derivados                       │
                                                    │                     │                              │
                                                    └──────────┬──────────┴──────────────────────────────┘
                                                               ▼
                                                     B8 Migración sistema a sistema ─► B9 Menús ─► B10 Enforce ─► B11 Docs
```

### B0 — Preparación (independiente)

| Tarea | Qué                                                                                                                                                                           | Hecho cuando                                              |
|-------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------|
| P-1   | Borrar el trámite `prueba` (y con él `FormacionCentroTrabajo`)                                                                                                                | build y suite E2E en verde                                |
| P-2   | Extraer de `files/ace/Ace.java` (EFBT) la lectura de `<usuario>` con «un solo atributo» a una pieza reutilizable para `generateSecurity` (A.9, V-18); `AceProfile*` no cambia | las filas de `AceProfile*` en BD son las mismas que antes |

### B1 — Base

| Tarea | Qué                                                                                                                                                                                            | Hecho cuando                                           |
|-------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------|
| F-1   | XSD `security_1.0.xsd` en EFBT con los tres bloques, los tres tipos de `<ace>` y su `<usuario>` (A.2, A.3, A.5), y XSD `security-config_1.0.xsd` con `<securityConfig>` y `<colectivos>` (A.4) | valida los ejemplos de A.8 y rechaza los casos de V-01 |
| F-2   | Dominio `AceRecurso` (`cacheable="true"`), repositorio con consulta `.cacheable()`, `security_ace_recurso` en `tablasIncluidas` (C.1, C.2)                                                     | arranca y la tabla existe vacía                        |

### B2 — EFBT: métodos y acciones

| Tarea | Qué                                                                                                              | Hecho cuando                                                                |
|-------|------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------|
| E-1   | Tarea `generateSecurity` en `build.gradle`; descubrimiento y enumeración de recursos de métodos y acciones (B.1) | lista los recursos de cada alcance                                          |
| E-2   | Validaciones V-01 a V-11, V-13, V-17 y V-18 (B.2)                                                                | cada una con su test unitario                                               |
| E-3   | Transformaciones T-1 a T-4 y T-7 (B.3)                                                                           | `AceRecurso.xml` generado y cargado; tests de B.4 de expansión y colectivos |
| E-4   | `subsystem/security/security/security-config.xml` con el colectivo `GESTOR`                                      | —                                                                           |
| —     | `mvn install` en EFBT                                                                                            | el build del proyecto lo usa                                                |

### B3 — Runtime en `subsystem/security`

| Tarea | Qué                                                                                                                 | Hecho cuando                                    |
|-------|---------------------------------------------------------------------------------------------------------------------|-------------------------------------------------|
| C-1   | `AclService` (C.3) sobre la consulta cacheada                                                                       | tests unitarios                                 |
| C-2   | `DestinatarioEvaluator` sin derivados (`admin`, `autenticado`, `todos`, `tipoUsuario`, `cargo`)                     | tests unitarios, `crapCheck` en verde           |
| C-3   | Verificar la caché de JPA (C.2) con `org.hibernate.orm.cache` en `DEBUG`; decidir la caché de los datos del usuario | aciertos en el log; decisión escrita en el plan |

### B4 — Puntos de control en `warn`

| Tarea | Qué                                              | Hecho cuando                                         |
|-------|--------------------------------------------------|------------------------------------------------------|
| W-1   | Propiedad `security.acl.mode` (C.5)              | —                                                    |
| W-2   | `AclActionObserver` + `AclDeniedException` (C.4) | en `warn` registra las denegaciones; nada se bloquea |
| W-3   | `AclRestFilter` (C.4)                            | ídem para endpoints JAX-RS                           |

### B5 — Piloto y tests de cobertura

| Tarea | Qué                                                                                                     | Hecho cuando                                              |
|-------|---------------------------------------------------------------------------------------------------------|-----------------------------------------------------------|
| M-1   | Migrar `criptografia` sin `<entities>`: borrar `data-init/input/security.xml`, crear `security/acl.xml` | E2E en verde sin denegaciones de `criptografia` en el log |
| M-2   | Tests de cobertura de C.7, deshabilitados                                                               | lanzados a mano dan el inventario de lo que falta         |

### B6 — EFBT: entidades (puede empezar tras B2)

| Tarea | Qué                                                                                  | Hecho cuando                           |
|-------|--------------------------------------------------------------------------------------|----------------------------------------|
| E-5   | Enumeración de entidades y propiedades `Centro`; validaciones V-12, V-14, V-15, V-16 | tests unitarios                        |
| E-6   | T-5: `Permission` con ancla, fragmentos, condición propia, nombre y `bind` a `users` | tests unitarios de B.4 de `Permission` |
| E-7   | T-6: copia de `Expediente` a subclases en `createdatainittipoexpediente`             | tests de descarte y fusión             |

### B7 — Destinatarios derivados (puede empezar tras B3)

| Tarea | Qué                                                                                       | Hecho cuando    |
|-------|-------------------------------------------------------------------------------------------|-----------------|
| D-1   | `perfil` y `unidadTramitadora` en XSD, columnas de `AceRecurso` y `DestinatarioEvaluator` | tests unitarios |

### B8 — Migración sistema a sistema

Por cada fichero de D.1, en un solo paso `<actionMethods>`, `<actions>` y `<entities>`, borrando su `auth-*.xml`.
Orden: los sistemas sin `auth-*.xml` y los simples primero; `ventanilla`, `gestioncentro` y `security` después; `expedientes` (con D.2) el último de los sistemas, y al final la parte de permisos del `auth.xml` global junto con `com/axelor/security/acl.xml` (`MetaFile` con `parentId`/`parentModel` en los `<iframe>`).
Al terminar `notificaciones` y `expedientes`, borrar `GestorNotificacionesUtil` y `EducaFlowAuthResolverImpl` (C.6).

| Hecho cuando (por sistema)                                                                                                            |
|---------------------------------------------------------------------------------------------------------------------------------------|
| el usuario aprueba el fichero, se ha borrado su `auth-*.xml`, y la suite E2E pasa sin denegaciones de ese sistema en el log de `warn` |

El bloque termina cuando los tests de cobertura, lanzados a mano, pasan.

### B9 — Menús

| Tarea | Qué                                                                           | Hecho cuando                                                     |
|-------|-------------------------------------------------------------------------------|------------------------------------------------------------------|
| N-1   | T-8 en EFBT (`if` con acciones, `isAnyVisible` en padres, `groups` inyectado) | `menus.xml` sin `groups`                                         |
| N-2   | `MenuSecurityServiceImpl` consulta `AclService`; fuera el `switch`            | cada usuario de prueba ve los mismos menús que antes (tabla D.4) |

### B10 — `enforce` y cierre

| Tarea | Qué                                                                    | Hecho cuando        |
|-------|------------------------------------------------------------------------|---------------------|
| X-1   | Revisar el log de `warn`, pasar a `enforce`, suite E2E                 | en verde            |
| X-2   | Habilitar los tests de cobertura; quitar el modo `warn` y la propiedad | `./run.sh` en verde |

### B11 — Documentación

- Skill nuevo `k-acl` (o sección en `k-secure-coding`), `subsystem/security/CLAUDE.md`.
  El skill **MUST** incluir la explicación de la especificidad de A.5 («gana el más específico y sustituye, no suma»), con su ejemplo XML de tres niveles, la tabla de qué regla gana para cada recurso y el porqué, y la consecuencia de que en `<actions>` el admin pierde una acción (y su menú) si su regla específica no repite `<usuario admin="true"/>`, mientras que en métodos y entidades está exento y «solo el admin» es no declarar el recurso.
- Referencias desde `k-sistemas`/`k-vistas`: al crear un controlador o una acción con nombre **MUST** declararse su `<ace>`; antes de repetir una lista de destinatarios, buscar o crear un colectivo; «gana el más específico y sustituye».
- Revisar `k-vistas`/`view-rules.md`: `groups`/`showIf` dejan de ser criterio de acceso y `groups` ya no se escribe en `menus.xml`.
- Quitar «PENDIENTE (importante)» de `CLAUDE.md` (D.2).

---

## 10. Pendiente (fuera de este plan)

- **IDOR / multicentro**:
  - en acciones, `tipoUsuario`/`cargo` son «en algún centro», así que cada método/servicio **MUST** seguir comprobando el centro del registro (`k-secure-coding`);
  - en entidades el filtro de centro solo cierra la parte multicentro; qué registro concreto ve cada usuario sigue en la `condition` propia de cada `<ace>`, sin forma declarativa todavía.
  - **En discusión (no entra en este plan hasta decidirlo)**: añadir al `<ace>` de `<entities>` hijos que hablen del **registro** y no del usuario, combinados en Y con el O de los `<usuario>`: `(usuario OR usuario …) AND (condición de registro AND condición de registro …)`.
    Candidatos: «el registro está en un centro del usuario» (hoy es el suelo de oficio de T-5, pasaría a ser explícito) y «el registro lo creó el usuario» (`createdBy`).
    La sintaxis (`<object …>`, `<registro …>`, o atributos) y si sustituye a `condition`/`conditionParams` o convive con ellos están sin decidir; cuando se decida, se añade a A.2/A.3, al XSD (F-1), a T-5 y a B.4.
- **Concesiones a usuarios concretos** (DNI, login): tabla de tiempo de ejecución con pantalla, que no se vacía al arrancar.
- **Endpoint REST automático** para el resto de entidades: lo cubren el `Permission` generado y el `AllowProperties` de su `ModelService`.
- **Otros destinatarios**, **colectivos locales o anidados**.
- Abierto hasta el log de `warn` de B8: si algún form usa adjuntos DMS, o si la subida de ficheros necesita algo más que `create` sobre `MetaFile`.

---

## Anexo A — Verificado en AOP (referencia para implementar)

Comprobado leyendo `../axelor-open-platform` (y donde se indica, el proyecto o la BD de desarrollo); no hace falta volver a buscarlo.

### Acciones y eventos

| Qué                            | Dónde                                                                        | Lo que hace                                                                                                                                                                                                                                               |
|--------------------------------|------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Punto común                    | `axelor-core/.../meta/schema/actions/Action.java:81-95` (`execute`)          | sin nombre solo `evaluate`; con nombre `checkPermission` → `firePreEvent` → `evaluate` → `firePostEvent` → `getResult`. `checkPermission` (l. 97) es `CAN_READ` sobre `getModel()`.                                                                       |
| `wrap`                         | `Action.java:101-104`                                                        | `execute(handler)` y envuelve el resultado; lo llama `ActionGroup` por cada hija.                                                                                                                                                                         |
| Entrada de `POST /ws/action`   | `axelor-core/.../meta/ActionHandler.java:474-500`                            | parte por comas, `ActionGroup` **sin nombre** (no dispara evento), `action.wrap(this)`. Sin `try/catch`: la excepción del observador sube a `ResponseInterceptor`.                                                                                        |
| Resolución de un nombre        | `axelor-core/.../meta/schema/actions/ActionGroup.java:91-150` (`findAction`) | con `:` y tipo de vista (`XMLViews.java:178`): `ActionView` ad hoc con `setName(actionName)` (l. 110), dispara `PreAction`; con `:` sin tipo de vista: `ActionMethod` ad hoc con `setName` (l. 136), dispara `PreAction`; sin `:`: `MetaStore.getAction`. |
| Iteración del grupo            | `ActionGroup.java:213-227`                                                   | por cada hija `findAction` → `test` (su `if`) → `wrap`: cada hija dispara su `PreAction`.                                                                                                                                                                 |
| Invocación del método          | `ActionHandler.java:224-236` (`call`), `238-…` (`rpc`)                       | `call`: `getMethod(method, ActionRequest, ActionResponse)` + `Beans.get` + `invoke`, **sin mirar `@CallMethod`**. `rpc` busca por nombre. `ActionMethod.java:62-63` elige.                                                                                |
| `ActionMethod.Call`            | `ActionMethod.java:17-32`                                                    | `getController()` (FQN) y `getMethod()` (con `(args)` si es rpc).                                                                                                                                                                                         |
| Disparo de eventos             | `ActionHandler.java:155-163`                                                 | `preActionEvent.select(NamedLiteral.of(name)).fire(new PreAction(name, context))`.                                                                                                                                                                        |
| Observador sin `@Named`        | `axelor-core/.../event/Observer.java:77-83`                                  | sin qualifiers recibe todo.                                                                                                                                                                                                                               |
| Descubrimiento de observadores | `axelor-core/.../event/EventBus.java:49-58`                                  | recorre `injector.getAllBindings()`: hace falta `bind(AclActionObserver.class)`.                                                                                                                                                                          |
| `MetaStore.getAction`          | `axelor-core/.../meta/MetaStore.java:79-89`                                  | `null` si no existe o su módulo no está instalado.                                                                                                                                                                                                        |
| Módulo de una acción           | `axelor-core/.../meta/loader/ViewLoader.java:347,410,544,627`                | `module` = módulo Axelor que la cargó; en la BD: `secretaria-virtual` (335) y `axelor-core` (71); viene de `rootProject.name` (`settings.gradle:54`).                                                                                                     |
| Excepción → cliente            | `axelor-core/.../rpc/ResponseInterceptor.java:64-110`                        | `AuthorizationException` → «Access error», `STATUS_FAILURE`, log de una línea; otra excepción → `log.error` con traza. Rollback (l. 70-72).                                                                                                               |

### Entidades (`Permission`)

| Qué                                    | Dónde                                                                                                           | Lo que hace                                                                                                                                                                          |
|----------------------------------------|-----------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `isPermitted`                          | `axelor-core/.../auth/AuthSecurity.java:155-184`                                                                | usuario `null` → `true`; sin `Permission` → **`false`**; uno sin `condition` → `true`; sin `ids` (create) → `true`; con `ids`, filtra y cuenta.                                      |
| `getFilter`                            | `AuthSecurity.java:116-153`                                                                                     | `Filter.or` de las `condition` aplicables, AND `id IN (ids)`.                                                                                                                        |
| `Condition`                            | `AuthSecurity.java:29-50`, `eval` 52-59                                                                         | JPQL con `?` posicionales; `conditionParams` por comas, `__user__` o Groovy con `__user__`. `__user__` en el texto no se sustituye.                                                  |
| Renumerado de `?`                      | `Filter.java:27-30` (`Filter.build`)                                                                            | renumera todos los `?` en orden: ni reutilizar uno ni escribir `?1`.                                                                                                                 |
| Resolución (fork)                      | `AuthSecurity.java:71-72`, `199-206`                                                                            | primero `EducaFlowAuthResolverRegistry.get()`, si vacío el `AuthResolver` estándar.                                                                                                  |
| `AuthResolver.resolve`                 | `axelor-core/.../auth/AuthResolver.java:98-123`, `filterPermissions` 59-71                                      | une usuario, roles, grupo y roles del grupo; `object` por igualdad exacta, **sin superclases**.                                                                                      |
| `EducaFlowAuthResolverImpl`            | `subsystem/security/EducaFlowAuthResolverImpl.java:16-32`; registro en `SecurityModule.java:20`                 | sustituye (no une) los de la subclase por los de `Expediente`.                                                                                                                       |
| Admin                                  | `axelor-core/.../auth/AuthUtils.java:100-103`; `AuthSecurity.java:75-79`                                        | `code == "admin"` o grupo `admins`; `getUser()` devuelve `null` si es admin. En BD: todos en `users` salvo `admin` en `admins`; `RegistroServiceImpl.java:25` da de alta en `users`. |
| Dónde se comprueba create/write/remove | `axelor-core/.../rpc/Resource.java:1288-1291, 1379-1393, 1434, 1497, 1536`                                      | **solo en `Resource`** (`/ws/rest`); ni `JPA` ni `ActionRecord` (`ActionRecord.java:228`) comprueban.                                                                                |
| Vaciado al arrancar                    | `base/infrastructure/db/BulkTables.java:12-38`; `DataBaseStartup.java:28-35`; `SecretariaVirtualModule.java:22` | JDBC directo: trunca `meta_%`, `auth_%` (menos excluidas) y `tablasIncluidas` (hoy las cuatro `security_ace_profile_*`).                                                             |
| `StartupEvent`                         | `axelor-web/.../web/AppStartup.java:58`, tras `ModuleManager.java:77`                                           | después de cargar los data-init; ejemplo `AppEventObserver.java:22`.                                                                                                                 |
| `Permission.condition`                 | `Permission.xml:21`                                                                                             | 4096 caracteres en el fork (commit `5e071d30c`; el original de AOP tiene 1024). Se puede ampliar más si hace falta.                                                                 |

### Menús

| Qué                      | Dónde                                                                                | Lo que hace                                                                                                            |
|--------------------------|--------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------|
| `groups`                 | `axelor-core/.../meta/service/menu/MenuChecker.java:56-70`; `MenuService.java:72-73` | admin, o grupo/rol en la lista, o sin `groups`/`roles` **y con padre**: un menú raíz sin `groups` solo lo ve el admin. |
| `if` inyectado hoy       | EFBT `viewprocessor/tags/MenuItem.java:15-25`                                        | aborta si ya trae `if`; inyecta `__config__.menuSecurity.isVisible("<name>")` (`MenuSecurityService.java:10`).         |
| `switch` actual          | `subsystem/security/service/impl/MenuSecurityServiceImpl.java:22-40`                 | las reglas de D.4; `default -> true`.                                                                                  |
| Gestor de notificaciones | `subsystem/notificaciones/util/GestorNotificacionesUtil.java:23-26, 33-37, 39-48`    | tipos y cargos de `GESTOR` sin admin; `idsCentrosGestionados` lo usan `auth-notificaciones.xml:39,47,55,63`.           |
| Tramitador               | `subsystem/security/service/PerfilesUsuarioService.java:54,57`                       | `isTramitador(User)` e `isTramitador(User, UnidadTramitadoraCodigo)`.                                                  |

### JAX-RS y Shiro

| Qué                              | Dónde                                                                                    | Lo que hace                                                                                             |
|----------------------------------|------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------|
| Rutas sin sesión                 | `axelor-core/.../auth/AuthWebModule.java:23-31`                                          | `/ws/public/**` (`ANON`), `/public/**`, estáticos. Todo lo demás exige login.                           |
| Registro de recursos y providers | `axelor-web/.../web/AppContextListener.java:16,70`                                       | `ModuleProcessor` registra lo enlazado en Guice con `@Path`/`@Provider`; `ResourceInfo` por `@Context`. |
| Endpoints públicos               | `RegistroSalidaController.java:29,39,76`; `RegistroController.java:24,46,80,106,134,141` | no hay controladores Kotlin ni en subpaquetes de `controller`.                                          |

### Ficheros (`MetaFile`)

| Qué                  | Dónde                                                                     | Lo que hace                                                                                                                                                               |
|----------------------|---------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Descarga por REST    | `axelor-web/.../web/service/RestService.java:557-580`, `download` 477-510 | `MetaFile`: permitido si `createdBy` es el usuario o `checkMetaFileParentPermission`; si no, `CAN_READ` → 403. URL de los `<iframe>` en `template-views.xml:114,124,134`. |
| Permiso por el padre | `RestService.java:589-650`                                                | `parentId`+`parentModel`: `CAN_READ` sobre el padre **y** el fichero cuelga de una de sus propiedades.                                                                    |
| Estado hoy           | `src/main/resources/data-init/input/auth.xml:8-10,205-208`                | `MetaFile.all` y otros `*.all` sin condición para `users` (por código y BD, no con petición real).                                                                        |
| DMS                  | `axelor-web/.../web/service/DmsService.java:378-405`                      | irrelevante mientras no se use el gestor documental.                                                                                                                      |

### Caché de JPA

| Qué                | Dónde                                                                                            | Lo que hace                                                               |
|--------------------|--------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------|
| Activación         | `axelor-core/.../db/internal/DBHelper.java:202-217`; `axelor-core/.../db/JpaModule.java:170-177` | con `sharedCache.mode` distinto de `NONE` activa L2 y caché de consultas. |
| Proveedor          | `JpaModule.java:234-240`; `axelor-core/.../cache/CacheConfig.java:24`                            | JCache con Caffeine por defecto.                                          |
| Entidad cacheable  | `axelor-tools/.../entity/model/Entity.java:86,617-626`                                           | `cacheable="true"` → `@Cacheable`.                                        |
| Consulta cacheable | `axelor-core/.../db/Query.java:249-252,384,418`                                                  | `Query#cacheable()`.                                                      |
| Proyecto           | `src/main/resources/axelor-config.properties:157,246`                                            | `caffeine` + `ENABLE_SELECTIVE`.                                          |

## Anexo B — Por qué de las decisiones menos obvias

1. **Condición propia en `<entities>`**: casi todos los permisos condicionales de hoy son por relación con el registro (firmante, destinatario, interesado, centro supervisado); sin ella no se podían migrar `firmas`, `notificaciones`, `security` ni `expedientes`.
2. **Un solo `?` en el ancla**: AOP enlaza por posición y renumera todos los `?`; un único `EXISTS` sobre `CentroUsuario` fija a la vez usuario, centro, tipos y cargos.
3. **Entidades fuera de `AceRecurso`**: con el centro en el ancla, una condición genérica sobre `AceRecurso` habría necesitado igualmente `self.<centro>` por entidad, y nadie más leía esas filas.
4. **Copia a subclases y no resolver**: `EducaFlowAuthResolverImpl` sustituía, no unía, y un `write` en `Expediente` lo habrían heredado todas las subclases por detrás del generador.
5. **Prohibido mezclar incondicional y condicional en un flag**: por id gana el incondicional; en el grid (`getFilter` sin id) se pierde, y el grid mostraría menos filas de las que se abren por id (`../axelor-open-platform/preguntas/seguridad.md`).
6. **Sobrecargas abortan**: AOP las tolera a medias (`call` por firma exacta, `rpc` por nombre) pero la clave `Ctrl.metodo` no las distingue.
7. **`AclDeniedException` hereda de `UnauthorizedException`**: el cliente recibe «Access error» y el log no se llena de trazas.
8. **El admin exento en métodos y entidades pero no en acciones**: en entidades lo impone AOP; en métodos no aporta nada obligar a declararlo, porque el admin administra; en acciones se declara porque las acciones son los menús, y el admin no debe ver los menús de cada tipo de usuario.
9. **Modo `warn` global**: no existe `enforce` por sistema; el criterio de «sistema revisado» es la suite E2E sin denegaciones de ese sistema.
