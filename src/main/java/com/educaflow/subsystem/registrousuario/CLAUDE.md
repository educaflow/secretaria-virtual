# CLAUDE.md — subsystem/registro

## Estructura de paquetes

```
registro/
  controllers/   — RegistroController.java (JAX-RS, /ws/public/registro)
  db/
    repo/        — RegistroPendienteRepository.java, UsuarioAutorizadoRepository.java
  domains/       — DatosBasicosUsuario.java (record input paso 3)
                   RegistroPendiente.xml → entidad JPA en package db
                   UsuarioAutorizado.xml → entidad JPA en package db
  module/        — RegistroModule.java (bindings Guice)
  service/       — RegistroService.java, UsuarioAutorizadoService.java + impl/
  RegistroException.java
```

Los XML de dominio están en `domains/` pero declaran `package="com.educaflow.subsystem.registro.db"`,
por lo que las clases generadas quedan en ese paquete.

## Ficheros principales

- `resources/web/registro.html` — formulario multistep (3 pasos), estilo login Axelor, enlace "← Volver al acceso"
- `resources/web/registro.css` — card blanca con sombra 2xl sobre fondo gris, sin cabecera de color
- `resources/web/registro.js` — lógica cliente
- `controllers/RegistroController.java` — endpoints JAX-RS; traduce `Map<String,String>` HTTP → dominio
- `service/RegistroService.java` — interfaz del flujo de registro
- `service/impl/RegistroServiceImpl.java` — implementación
- `service/UsuarioAutorizadoService.java` + `impl/` — solo expone `isAuthorized(String)`
- `domains/DatosBasicosUsuario.java` — record: nombre, apellidos, password, passwordRepeat, idioma (input paso 3)
- `domains/RegistroPendiente.xml` — entidad JPA: email, dni, codigo, verificado, token
- `domains/UsuarioAutorizado.xml` — entidad JPA: centro, curso, dni, tipoUsuario
- `db/repo/RegistroPendienteRepository.java` — `findByToken(String)`, `findTiposUsuarioByDni(String)`
- `db/repo/UsuarioAutorizadoRepository.java` — `isAuthorized(String)`, `findAllByDni(String)`
- `RegistroException.java` — RuntimeException con `List<String> errors`; constructor `(String)` y `(List<String>)`

El botón "Registrarse" en el login está configurado en `axelor-config.properties`:
```properties
application.sign-in.buttons.registrar.title = Registrarse
application.sign-in.buttons.registrar.type = link
application.sign-in.buttons.registrar.link = /ws/public/registro
```

## Endpoints JAX-RS

| Método | Path | Descripción |
|--------|------|-------------|
| `GET`  | `/ws/public/registro` | Sirve `registro.html` |
| `POST` | `/ws/public/registro/registrosPendientes` | Paso 1: valida email/DNI, crea `RegistroPendiente`, envía código |
| `POST` | `/ws/public/registro/validarCodigo` | Paso 2: verifica el código recibido por email |
| `POST` | `/ws/public/registro/usuarios` | Paso 3: crea el usuario |
| `GET`  | `/ws/public/registro/registro.css` | Estático |
| `GET`  | `/ws/public/registro/registro.js` | Estático |

## Flujo de 3 pasos

1. **Paso 1 — `POST /registrosPendientes`**
   - Body: `{ email, dni, tipoDoc }`
   - Valida formato de email, formato de DNI/NIE y que el DNI esté en `UsuarioAutorizado`
   - Guarda `RegistroPendiente`, envía código por email
   - Respuesta OK: `{ "token": "..." }`

2. **Paso 2 — `POST /validarCodigo`**
   - Body: `{ token, codigo }`
   - Verifica el código; si coincide marca `RegistroPendiente.verificado = true`
   - Respuesta OK: `{ "ok": true }`

3. **Paso 3 — `POST /usuarios`**
   - Body: `{ token, nombre, apellidos, password, passwordRepeat, idioma }`
   - Valida contraseñas coincidan; crea usuario con `CentroUsuario` + `CentroUsuarioTipoUsuario`
   - Elimina el `RegistroPendiente`
   - Respuesta OK: `{ "ok": true }`

## Manejo de errores

- `RegistroException` tiene `getErrors()` → `List<String>`
- El controller siempre devuelve HTTP 400 con `{ "errors": ["...", "..."] }` (array)
- El JS lee `e.errors` y llama a `mostrarError(msgs)`:
  - 1 error → `textContent`
  - \>1 errores → `<ul>` con un `<li>` por error

## Separación de capas

- El servicio solo trabaja con `domains.*` y entidades de `db.*`; nunca importa clases Axelor de vista
- Los repositorios (`db/repo/`) son la única frontera con JPA
- El controller traduce `Map<String,String>` HTTP → parámetros/records y devuelve JSON plano

## Reglas de negocio — resolución de TipoUsuario

Lógica en `RegistroPendienteRepository.findTiposUsuarioByDni(dni)`, parte pura testeable en el método estático `resolverPerfiles`.

- `EX_MAPPING = { PROFESOR→EXPROFESOR, ALUMNO→EXALUMNO }`
- La query devuelve un registro por `(centro, tipoUsuario)` con el `curso` más alto (subconsulta MAX)
- Por cada entrada: si `ua.curso == centro.curso` → tipo activo directo; si no → versión "ex" (o se omite si no tiene)
- **FAMILIAR** y otros sin versión "ex": omitidos si no son del curso actual
- `centro.curso = null` → nadie es activo (todos los con versión "ex" devuelven la versión ex)
- Los `TipoUsuario` para los códigos EX se cargan en una sola query antes del bucle (sin N+1)

## Campos del usuario creado

- `code` = email, `dni` = DNI normalizado, `name` = nombre + apellidos
- `language` = `es` o `ca` (cualquier otro valor se normaliza a `es`), `centroActivo` = primer centro del registry
- `group` = grupo con code `users`
- Se crean `CentroUsuario` + `CentroUsuarioTipoUsuario` por cada centro del registry

## Validaciones de seguridad

- Email: no vacío + formato `^[^@\s]+@[^@\s]+\.[^@\s]+$`
- DNI: formato válido vía `DniUtil.isValid` + presente en `UsuarioAutorizado`
- Password: mínimo 8 caracteres + `password == passwordRepeat` (validado en servicio, no solo en JS)
- Idioma: restringido a `["es", "ca"]`, default `"es"`

## Email

SMTP pendiente de configurar. El código de verificación se loguea siempre en consola:
`[REGISTRO] Código de verificación para X: Y`

Config en `axelor-config.properties`: `mail.smtp.host`, `mail.smtp.user`, `mail.smtp.password`.
Para activar el envío real: descomentar el bloque `Transport` en `MailSenderImpl.java`.

## Tests

`src/test/.../registro/db/repo/RegistroPendienteRepositoryTest.java` — 13 tests JUnit 5 sobre `resolverPerfiles`:
- Profesor/Alumno del curso actual → tipo activo
- Profesor/Alumno de curso anterior → versión "ex"
- Familiar de curso anterior → omitido
- Profesor en dos centros, activo en uno y ex en otro
- Combinaciones: PROFESOR + ALUMNO mismo centro, centro sin curso configurado, lista vacía

## Regla de scope

Solo modificar ficheros bajo `subsystem/registro/` y `resources/web/registro.*`.
Para cambios fuera de ese scope, preguntar antes.
