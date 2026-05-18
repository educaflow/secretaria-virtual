## ¿Para qué sirve esto?

Núcleo de datos compartidos de la aplicación multicentro: define las entidades Centro, Usuario (extensión de `com.axelor.auth.db.User`), el modelo de pertenencia usuario-centro y los catálogos geográficos/organizativos. Todo el resto de subsistemas y sistemas depende de este módulo para conocer a qué centro pertenece el usuario autenticado y qué tipos de usuario tiene asignados en ese centro.

## Lo no obvio

- **`User` no es una entidad propia sino una extensión** del `User` de Axelor (`com.axelor.auth.db.User`). `User.xml` añade campos de negocio (`dni`, `nombre`, `apellidos`, `centroActivo`, `centroUsuarios`) y dos métodos de conveniencia vía `<extra-code-model>`: `getCentroUsuarioActivo()` y `getTiposUsuarioActivos()`. Cualquier repositorio o servicio que opere con usuarios de Axelor recibirá un objeto con estos campos extra.

- **El modelo de pertenencia es una cadena de tres niveles:** `Centro` → `CentroUsuario` (join entre Centro y User, con unique constraint) → `CentroUsuarioTipoUsuario` (uno por cada tipo de usuario asignado en ese centro). Un mismo usuario puede estar en varios centros y tener distintos tipos de usuario en cada uno.

- **`centroActivo` es el centro seleccionado en sesión.** El campo vive en `User` (no en sesión/cookie) y se actualiza desde las preferencias de usuario. Las vistas de otros subsistemas filtran por `centroActivo` para la restricción multicentro.

- **Las vistas de preferencias de usuario (`user-preferences-form-view.xml`) usan `extension="true`** para sobreescribir el formulario estándar de Axelor: ocultan campos de tema/imagen/MFA y añaden `centroActivo` como selector de centro activo filtrado a los centros del usuario autenticado.

- **`Centro.xml` tiene comentadas las fórmulas SQL** para director/secretario/etc. El diseño actual no las usa; los roles de cargo se gestionan mediante `Cargo` (entidad catálogo ligada a `TipoUsuario`) pero sin lógica de servicio todavía.

- **`TipoUsuario.findByCodigo` existe a dos niveles:** en el repositorio (vía `<finder-method>` del XML) y en `TipoUsuarioService`. Usar siempre el servicio desde código de negocio para respetar la arquitectura de capas.

- **No hay controladores en este subsistema.** Toda la gestión de centros y usuarios se realiza directamente desde las vistas XML (acciones `save`, `back`, `delete`, acciones-record) sin llamadas a métodos Java del servidor.

## Servicios y métodos públicos

### TipoUsuarioService
| Método | Qué hace en una línea |
|---|---|
| `TipoUsuarioService.findByCodigo(String codigo)` | Busca un `TipoUsuario` por su campo `codigo`; devuelve `Optional.empty()` si no existe |

## Repositorios y métodos públicos

### CentroRepository
| Método | Qué hace en una línea |
|---|---|
| `CentroRepository.findByCodigo(String codigoCentro)` | Busca un `Centro` por el campo `code` (código del centro); devuelve `Optional` |

### TipoUsuarioRepository
Sin métodos adicionales. El `findByCodigo` lo aporta `AbstractTipoUsuarioRepository` generado desde el `<finder-method>` del XML de dominio.

## Vistas

| Vista | Para qué |
|---|---|
| `subsysCommon.Centro@Main-action` | Entrada principal a la gestión de centros (grid + formulario con tabs) |
| `subsysCommon.Centro@Main-grid` | Lista de centros (código, nombre, curso) |
| `subsysCommon.Centro@Main-form` | Formulario de centro con pestaña de datos geográficos y dashlet embebido de usuarios |
| `subsysCommon.Centro@Search-grid` | Grid de selección de centro para campos relacionales de otros formularios |
| `subsysCommon.Centro@View-form` | Formulario de solo lectura de centro para popups de consulta |
| `subsysCommon.Centro.CentroUsuario@Main-action` | Popup de usuarios de un centro (filtrado por `centroId` del contexto padre) |
| `subsysCommon.Centro.CentroUsuario@Main-grid` | Lista de usuarios de un centro (apellidos, nombre, dni) |
| `subsysCommon.Centro.CentroUsuario@Main-form` | Formulario de asignación usuario-centro con panel-related de tipos de usuario |
| `subsysCommon.Centro.CentroUsuario.CentroUsuarioTipoUsuario@Main-grid` | Lista de tipos de usuario asignados a un CentroUsuario |
| `subsysCommon.Centro.CentroUsuario.CentroUsuarioTipoUsuario@Main-form` | Formulario de asignación de un tipo de usuario |
| `subsysCommon.Usuario@Main-action` | Gestión de usuarios Axelor (solo admins, grid+form) |
| `subsysCommon.Usuario@Main-grid` | Lista de usuarios del sistema (restringida a grupo `admins`) |
| `subsysCommon.Usuario@Main-form` | Formulario de usuario con campos de negocio (dni, nombre, apellidos, centroActivo) |
| `subsysCommon.Usuario@Search-grid` | Grid de selección de usuario para campos relacionales (restringida a `admins`) |
| `subsysCommon.Usuario@View-form` | Formulario de solo lectura de usuario para popups |
| `user-preferences-form` (extension) | Sobrescribe el formulario de preferencias de Axelor: añade `centroActivo`, oculta tema/imagen/MFA/tokens |

## Dependencias

### Otros subsistemas
| Subsistema | Para qué |
|---|---|
| `subsystem/importacion` | `Centro` tiene una relación `one-to-many` con `TareaImportacion`; el subsistema de importación depende de `Centro` como entidad padre |

### Infraestructura
Ninguna. Este subsistema no usa ninguna clase de `base/infrastructure`.
