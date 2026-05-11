---
type: analysis
---

## Análisis Funcional: Correos

**Tipo:** subsistema
**Capa:** subsystem/correos
**Descripción:** Subsistema que envía correos electrónicos, registra su contenido completo y resultado de envío, y permite consultarlos desde tres vistas: administrador, personal de centro y carpeta ciudadana del destinatario.

---

### Entidades

**`Correo`**
| Campo              | Tipo                      | Restricción            | Descripción |
|--------------------|---------------------------|------------------------|-------------|
| id                 | Long                      | autogenerado           | Clave primaria |
| centro             | ManyToOne → Centro        | required               | Centro educativo propietario del correo |
| expediente         | ManyToOne → Expediente    | opcional               | Expediente vinculado, si aplica |
| asunto             | String                    | required, max 255      | Asunto del email |
| cuerpoHtml         | Text                      | required               | Contenido HTML completo del correo |
| destinatarioDni    | String                    | opcional, max 20       | DNI/NIE del destinatario; clave para carpeta ciudadana |
| destinatarioEmail  | String                    | required, max 254      | Dirección de envío |
| destinatarioNombre | String                    | opcional, max 200      | Nombre legible del destinatario |
| usuario            | ManyToOne → User          | opcional               | Usuario Axelor vinculado al envío (informativo) |
| fechaCreacion      | DateTime                  | autogenerado, readonly | Momento de persistencia |
| fechaEnvio         | DateTime                  | nullable               | Momento del envío exitoso; null si no enviado |
| estado             | Selection                 | required               | PENDIENTE / ENVIADO / FALLIDO |
| mensajeError       | Text                      | nullable               | Detalle técnico del fallo; null si estado ≠ FALLIDO |
| numeroIntentos     | Integer                   | required, default=0    | Número acumulado de intentos de envío |
| adjuntos           | OneToMany → CorreoAdjunto | —                      | Lista de adjuntos del correo |

> **Nota:** No existe campo `exito` (Boolean). El campo `estado` cubre toda la semántica del resultado. ENVIADO = éxito; FALLIDO = fallo.

---

**`CorreoAdjunto`** — modelo sin UI, gestionado exclusivamente por `CorreoService`
| Campo   | Tipo                 | Restricción  | Descripción |
|---------|----------------------|--------------|-------------|
| id      | Long                 | autogenerado | Clave primaria |
| correo  | ManyToOne → Correo   | required     | Correo al que pertenece el adjunto |
| archivo | ManyToOne → MetaFile | required     | Fichero almacenado en BD (Axelor MetaFile) |

---

### Relación `usuario` vs `destinatarioDni`

- **`usuario`** (ManyToOne → User): usuario Axelor que dispara o está vinculado al envío por lógica de negocio. Puede ser el propio destinatario (alumno con cuenta Axelor) o el actor que genera el correo (secretario). Es puramente informativo y no afecta a filtros de visibilidad ni permisos.
- **`destinatarioDni`** (String): DNI del destinatario real del correo. Es el campo que filtra la carpeta ciudadana (`self.destinatarioDni = :currentUser.dni`). Pueden coincidir con `usuario.dni` o no.

---

### Remitente del correo

El campo `from` del record `Mail` lo proporciona la configuración del MailSender (cuenta de correo del sistema o del centro). No es un campo del modelo `Correo`. El servicio delega esta responsabilidad en el `MailSender` configurado.

---

### Dependencias de otros subsistemas

- `subsystem/common` — Centro (multicentro) y User extendido (dni, centroActivo)
- `subsystem/expedientes` — Expediente (FK opcional, para trazabilidad)
- `base/infrastructure/mail` — MailSender, Mail, Attach (envío real del email)

---

### Operaciones

**Crear correo** (API de servicio, invocada por código de otros sistemas — no por UI directa):

```
CorreoService.crearYEnviar(CorreoRequest request)
```

`CorreoRequest` incluye: `centroId` (required), `asunto` (required), `cuerpoHtml` (required), `destinatarioEmail` (required), `destinatarioDni?`, `destinatarioNombre?`, `expedienteId?`, `usuarioId?`, `adjuntos?: List<Attach>`

Pasos:
1. Valida parámetros obligatorios
2. Persiste `Correo` con estado=PENDIENTE, fechaCreacion=now(), numeroIntentos=0
3. Por cada `Attach` recibido: crea `MetaFile` y persiste `CorreoAdjunto`
4. Construye `Mail` y llama `MailSender.send(mail)`
5a. Si éxito → estado=ENVIADO, fechaEnvio=now(), numeroIntentos=1, mensajeError=null
5b. Si excepción → estado=FALLIDO, mensajeError=ex.getMessage(), numeroIntentos=1
6. Persiste `Correo` con estado final

**Reintentar envío** (botón en vista admin y personal centro, solo visible si estado=FALLIDO):

```
CorreoService.reintentar(Long correoId)
```

Pasos:
1. Carga el `Correo`. Verifica que estado=FALLIDO (si no, excepción técnica — V-010)
2. Reconstruye `Mail` desde los campos persistidos + lista de adjuntos
3. Llama `MailSender.send(mail)`
4a. Si éxito → estado=ENVIADO, fechaEnvio=now(), mensajeError=null
4b. Si excepción → mensajeError=ex.getMessage(), numeroIntentos++
5. Persiste `Correo` actualizado

---

### Vistas

**Vista administrador** (`correos-admin`)
- Quién: rol Administrador
- Sin filtro de seguridad por centro (ve todos)
- Grid: centro, asunto, destinatarioEmail, destinatarioNombre, fechaCreacion, fechaEnvio, estado
- Form (readonly): todos los campos; `mensajeError` visible solo si estado=FALLIDO; panel `adjuntos` con descarga; botón "Reintentar" visible solo si estado=FALLIDO
- Filtros sugeridos: centro, estado, destinatarioEmail, rango de fechaCreacion

**Vista personal centro** (`correos-centro`)
- Quién: Secretario, Director, Jefes de estudio, Administrativos
- Filtro de seguridad JPQL: `self.centro.id = :currentUser.centroActivo.id`
- Grid: asunto, destinatarioEmail, destinatarioNombre, fechaCreacion, fechaEnvio, estado
- Form (readonly): todos los campos excepto `centro` (implícito del contexto); `mensajeError` si FALLIDO; panel adjuntos; botón "Reintentar" si FALLIDO
- Filtros sugeridos: estado, destinatarioEmail, rango de fechaCreacion

**Vista carpeta ciudadana** (`correos-ciudadano`)
- Quién: Alumno, Familiar, Externo autenticado
- Filtro de seguridad JPQL: `self.destinatarioDni = :currentUser.dni`
- Grid: asunto, fechaCreacion, fechaEnvio, estado
- Form (readonly): asunto, cuerpoHtml (renderizado HTML), panel adjuntos descargables
- Campos ocultos al ciudadano: mensajeError, numeroIntentos, expediente, usuario, centro, destinatarioDni, destinatarioEmail
- Sin botón reintentar. Completamente readonly.

---

### Menús

- Administración > Notificaciones > Correos → acción `correos-admin`
- Mi Centro > Notificaciones > Correos → acción `correos-centro`
- Carpeta ciudadana: integrado como panel o acción dentro del subsistema `carpeta-ciudadana`; sin menú propio en este subsistema

---

### Seguridad

| Rol / Tipo usuario                                  | Acceso |
|-----------------------------------------------------|--------|
| Administrador                                       | Leer y reintentar todos los correos de todos los centros |
| Supervisor                                          | Ver asunción A5 |
| Director, Secretario, Jefe estudio, Administrativo  | Leer y reintentar correos de su centroActivo únicamente |
| Conserje                                            | Sin acceso |
| Profesor, Exprofesor                                | Sin acceso (salvo que sean destinatarios: visible en carpeta ciudadana) |
| Alumno, Exalumno, Familiar, Externo                 | Solo lectura de correos donde destinatarioDni = su dni (carpeta ciudadana) |

**Multicentro:** sí. El campo `centro` es obligatorio en todo correo. Los filtros de seguridad usan `centroActivo` del usuario en sesión.

---

### Validaciones

| ID    | Campo(s)               | Tipo           | Origen              | Condición de aplicación               | Mensaje al usuario |
|-------|------------------------|----------------|---------------------|---------------------------------------|-------------------|
| V-001 | centro                 | Obligatoriedad | Modelo              | Siempre                               | "Indique el centro al que pertenece el correo" |
| V-002 | asunto                 | Obligatoriedad | Modelo              | Siempre                               | "Introduzca el asunto del correo" |
| V-003 | cuerpoHtml             | Obligatoriedad | Modelo              | Siempre                               | "El cuerpo del correo no puede estar vacío" |
| V-004 | destinatarioEmail      | Obligatoriedad | Modelo              | Siempre                               | "Introduzca la dirección de email del destinatario" |
| V-005 | destinatarioEmail      | Formato        | Catálogo            | Siempre                               | "El email '{valor}' del destinatario no tiene el formato correcto (usuario@dominio.com)" |
| V-006 | destinatarioEmail      | Longitud       | Catálogo            | Siempre                               | Invariante técnica (servicio): "destinatarioEmail supera 254 caracteres (RFC 5321): longitud={len}" |
| V-007 | destinatarioDni        | Formato + ctrl | Catálogo            | Si destinatarioDni no es null         | "El DNI '{valor}' del destinatario no es válido. Compruebe la letra verificadora" |
| V-008 | expediente, centro     | Consistencia   | Negocio (asumida)*  | Si expediente no es null              | Invariante técnica (servicio): "El expediente '{nombreExp}' pertenece al centro '{centroExp}', que no coincide con el centro del correo '{centro}'" |
| V-009 | estado                 | Inmutabilidad  | Negocio (asumida)*  | Si estado actual = ENVIADO            | Invariante técnica (servicio): "Correo {id} ya enviado; estado ENVIADO es inmutable. Transición rechazada a: {estadoNuevo}" |
| V-010 | estado                 | De estado      | Modelo (sin UI)     | Al llamar reintentar()                | Invariante técnica (servicio): "Solo se puede reintentar un correo en estado FALLIDO. Estado actual: {estado}" |
| V-011 | numeroIntentos         | Monotonía      | Modelo (sin UI)     | Siempre                               | Invariante técnica (servicio): "Correo.numeroIntentos no puede decrecer: actual={anterior}, propuesto={nuevo}" |
| V-012 | correo (CorreoAdjunto) | Obligatoriedad | Modelo (sin UI)     | Siempre                               | Invariante técnica (servicio): "CorreoAdjunto.correo no puede ser null" |
| V-013 | archivo (CorreoAdjunto)| Obligatoriedad | Modelo (sin UI)     | Siempre                               | Invariante técnica (servicio): "CorreoAdjunto.archivo no puede ser null" |

---

### Máquina de estados (campo `estado`)

**Estados:**
- `PENDIENTE` — inicial, transitorio (nunca observable si el envío es síncrono)
- `ENVIADO` — final, completamente inmutable (V-009)
- `FALLIDO` — reintentable mediante `reintentar()`

**Transiciones:**

| Desde     | Hacia   | Condición                         | Actor            | Acción posterior |
|-----------|---------|-----------------------------------|------------------|-----------------|
| PENDIENTE | ENVIADO | MailSender.send() sin excepción   | Sistema          | fechaEnvio=now(), intentos=1, mensajeError=null |
| PENDIENTE | FALLIDO | MailSender.send() lanza excepción | Sistema          | mensajeError=detalle, intentos=1, fechaEnvio=null |
| FALLIDO   | ENVIADO | Reintento exitoso                 | Admin / Personal | fechaEnvio=now(), mensajeError=null |
| FALLIDO   | FALLIDO | Reintento fallido                 | Admin / Personal | mensajeError actualizado, intentos++ |

**Transiciones inválidas:**
- ENVIADO → cualquier estado (V-009)
- Llamar `reintentar()` con estado ≠ FALLIDO (V-010)

**Campos editables por estado** (todos gestionados por el servicio — no editables por el usuario):

| Campo              | PENDIENTE | ENVIADO | FALLIDO |
|--------------------|-----------|---------|---------| 
| centro             | Auto      | R       | R       |
| expediente         | Auto      | R       | R       |
| asunto             | Auto      | R       | R       |
| cuerpoHtml         | Auto      | R       | R       |
| destinatarioDni    | Auto      | R       | R       |
| destinatarioEmail  | Auto      | R       | R       |
| destinatarioNombre | Auto      | R       | R       |
| usuario            | Auto      | R       | R       |
| fechaCreacion      | Auto      | R       | R       |
| fechaEnvio         | N         | R       | N       |
| estado             | Auto      | R       | Auto    |
| mensajeError       | N         | N       | R       |
| numeroIntentos     | Auto      | R       | Auto    |

(Auto = asignado por el servicio; R = readonly; N = no visible / null)

---

### Campos calculados

| Campo          | Fórmula / Origen                              | Cuándo se recalcula               | Editable |
|----------------|-----------------------------------------------|-----------------------------------|----------|
| fechaCreacion  | ORM: now() al persistir                       | Solo al crear                     | No       |
| fechaEnvio     | Servicio: now() al transicionar a ENVIADO     | Al enviar con éxito               | No       |
| estado         | Servicio: resultado de MailSender.send()      | Al crear y al reintentar          | No       |
| numeroIntentos | Servicio: contador incremental de envíos      | Cada llamada a MailSender         | No       |
| mensajeError   | Servicio: ex.getMessage(); null si éxito      | Cada intento; se limpia al éxito  | No       |

---

### Asunciones a confirmar

- **A1\*** (V-008): Si el correo referencia un expediente, el centro del expediente debe coincidir con el centro del correo. Si no coinciden, el servicio lanza excepción técnica y no persiste el correo.
- **A2\*** (V-009): Un correo en estado ENVIADO es completamente inmutable. Ningún campo admite modificación posterior.
- **A3\***: El envío es síncrono al crear. El estado PENDIENTE es transitorio y nunca se persiste de forma observable. Si en el futuro el envío fuese asíncrono, PENDIENTE pasa a ser un estado observable y la máquina de estados debe revisarse.
- **A4\***: El filtro de carpeta ciudadana usa exclusivamente `destinatarioDni`. Si el User autenticado no tiene DNI en su perfil, no verá correos en su carpeta ciudadana. Confirmar si debe haber fallback por `destinatarioEmail`.
- **A5\***: El rol Supervisor tiene acceso equivalente al Administrador (todos los centros). Confirmar.
- **A6\***: El campo `usuario` es puramente informativo. No afecta a filtros de visibilidad ni a permisos de ningún rol.
