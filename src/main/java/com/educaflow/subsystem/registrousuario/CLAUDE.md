# CLAUDE.md — registrousuario

## ¿Para qué sirve esto?

Gestiona el autoregistro público de usuarios (sin sesión Axelor) y el catálogo de usuarios autorizados a registrarse. El autoregistro es un flujo de 3 pasos: email/DNI → validación de código → creación de cuenta Axelor con sus `CentroUsuario` y `CentroUsuarioTipoUsuario` asociados.

## Lo no obvio

- **Todo el flujo de autoregistro está comentado.** `RegistroPendienteServiceImpl.insertar`, `validarCodigo`, `RegistroServiceImpl.registrarUsuario` y `RegistroPendienteRepository.findByToken`/`findTiposUsuarioByDni` tienen su cuerpo entero dentro de bloques `/* ... */`. El subsistema compila y está parcialmente integrado, pero el flujo real no se ejecuta. El único endpoint activo que hace algo es `POST /ws/public/registro/registrosPendientes` (devuelve siempre `null`) y `POST /ws/public/registro/validarCodigo` (vacío).
- **`RegistroModule.configure()` está vacío** (todos los `bind(...)` comentados), por lo que `RegistroPendienteService` y `UsuarioAutorizadoService` no están enlazados por DI. El único servicio operativo es `UsuarioAutorizadoServiceImpl`, enlazado implícitamente vía `ModelServiceFactory` por el subsistema de importación.
- **`RegistroController` es un JAX-RS REST público** (`@Path("/public/registro")`), no un controlador Axelor con `@CallMethod`. Expone además los estáticos `/registro.css` y `/registro.js` que sirve desde el classpath (`/web/`).
- **`UsuarioAutorizado` es el origen de datos para calcular qué tipos de usuario (incluyendo los "ex-") corresponden a cada DNI** al crear la cuenta. La lógica de conversión (`PROFESOR→EXPROFESOR`, etc.) está en `RegistroPendienteRepository.resolverPerfiles` (comentada).
- **`UsuarioAutorizado.activo`** no existe en el XML del dominio actual — el campo `activo` referenciado en `resolverPerfiles` fue eliminado o está pendiente de añadir.
- **`DatosBasicosUsuario`** es un `record` con validaciones inline en el constructor compacto (longitud mínima de contraseña, coincidencia, idioma). Lanzar con datos inválidos produce `IllegalArgumentException`, no `BusinessException`.

## Controladores y métodos

### `RegistroController` (JAX-RS, `@Path("/public/registro")`)

| Método | Qué hace en una línea |
|---|---|
| `index()` | `GET /ws/public/registro` — sirve `registro.html` desde el classpath |
| `guardarRegistroPendiente(body)` | `POST /ws/public/registrosPendientes` — limpia el DNI si es de tipo DNI y llama a `RegistroPendienteService.insertar`; devuelve `{"token":"..."}` o errores |
| `verificarEmail(body)` | `POST /ws/public/validarCodigo` — delega en `RegistroPendienteService.validarCodigo`; devuelve `{"ok":true}` o errores |
| `registrarUsuario(body)` | `POST /ws/public/usuarios` — cuerpo comentado; devuelve `{"ok":true}` incondicionalmente |

## Servicios y métodos públicos

### `RegistroPendienteService` / `RegistroPendienteServiceImpl`

| Método | Qué hace en una línea |
|---|---|
| `insertar(registroPendiente)` | Valida email/DNI, genera código+token, persiste `RegistroPendiente` y envía el código por email — **cuerpo comentado, retorna `null`** |
| `validarCodigo(codigo, token)` | Busca el `RegistroPendiente` por token, comprueba expiración (30 min) y marca `verificado=true` — **cuerpo comentado, no hace nada** |

### `RegistroService` / `RegistroServiceImpl`

| Método | Qué hace en una línea |
|---|---|
| `registrarUsuario(datos, token)` | Crea el `User` de Axelor a partir del `RegistroPendiente` verificado, le asigna grupo "users", crea `CentroUsuario`/`CentroUsuarioTipoUsuario` por centro y elimina el pendiente — **cuerpo comentado, retorna `null`** |

### `UsuarioAutorizadoService` / `UsuarioAutorizadoServiceImpl`

| Método | Qué hace en una línea |
|---|---|
| `findByCentroDniTipoUsuarioCurso(centro, dni, tipoUsuario, curso)` | Busca un `UsuarioAutorizado` por la clave natural (centro + dni + tipoUsuario + curso); retorna `Optional` |
| `validateInsert(entidad)` | Hook de `DefaultModelService` — rechaza duplicados por clave natural con mensaje descriptivo |

## Repositorios y métodos públicos

### `RegistroPendienteRepository`

| Método | Qué hace en una línea |
|---|---|
| `findByToken(token)` | Busca un `RegistroPendiente` por su UUID de sesión — **comentado** |
| `findTiposUsuarioByDni(dni)` | Devuelve `Map<Centro, List<TipoUsuario>>` con los tipos activos/ex para un DNI — **comentado** |

### `UsuarioAutorizadoRepository`

Sin métodos propios; hereda `findByCentroAndDniAndTipoUsuarioAndCurso` del repositorio abstracto generado por el XML.

## Vistas

No hay vistas XML en este subsistema. `UsuarioAutorizado` se gestiona desde el subsistema de importación; no tiene pantalla propia aquí.

## Dependencias

### Otros subsistemas

| Subsistema | Para qué |
|---|---|
| `subsystem/common` | `Centro`, `TipoUsuario`, `CentroUsuario`, `CentroUsuarioTipoUsuario` — son las entidades que se crean al completar el registro |
| `subsystem/importacion` | Consume `UsuarioAutorizadoService` para insertar/buscar `UsuarioAutorizado` durante la importación CSV |

### Infraestructura

| Infraestructura | Para qué |
|---|---|
| `base.util.DniUtil` | Limpieza y validación del DNI/NIE antes de persistir |
| `base.infrastructure.validation` | `BusinessException` / `BusinessMessages` para propagar errores de validación al cliente REST |
