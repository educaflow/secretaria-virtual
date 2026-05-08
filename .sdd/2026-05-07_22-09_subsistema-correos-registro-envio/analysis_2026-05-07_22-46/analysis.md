---
type: analysis
user-story-file: ../user-story.md
---

## Análisis Funcional: correos

**Tipo:** subsistema
**Capa:** `subsystem/correos`
**Descripción:** Subsistema de envío y registro de correos electrónicos; persiste cada correo con su contenido completo y estado de envío, y permite reenviarlo manualmente si falló.

---

### Entidades

**`Correo`**
- `centro` (ManyToOne → `Centro`, **required**)
- `para` (String, **required**) — dirección email del destinatario
- `de` (String, **required**) — remitente; auto-rellenado desde config SMTP al crear
- `asunto` (String, **required**)
- `cuerpoHtml` (Text, optional)
- `cuerpoTexto` (Text, optional) — al menos uno de los dos cuerpos debe estar presente
- `dni` (String, **required**) — DNI del destinatario
- `usuario` (ManyToOne → `User`, optional) — auto-resuelto al crear: si existe un `User` con ese DNI, se vincula
- `expediente` (ManyToOne → `Expediente` de `subsystem/expedientes`, optional)
- `estado` (Enum `EstadoCorreo`: `PENDIENTE` / `ENVIADO` / `ERROR`)
- `enviadoEn` (DateTime, optional) — fecha/hora del envío exitoso
- `ultimoFalloEn` (DateTime, optional) — fecha/hora del último fallo
- `adjuntos` (ManyToMany → `MetaFile`)
- `creadoEn` → campo `createdOn` heredado de `AuditableModel` de Axelor

**Enum `EstadoCorreo`**: `PENDIENTE`, `ENVIADO`, `ERROR`

---

### Dependencias de otros subsistemas

- `subsystem/common` — `Centro` (campo obligatorio en `Correo`)
- `base/infrastructure/mail` — `MailSender` / `Mail` para el envío real por SMTP
- `subsystem/expedientes` — FK opcional a `Expediente`

---

### Operaciones

**Crear correo** (desde UI o por código desde otros sistemas):
- Persiste el correo con `estado=PENDIENTE`
- Auto-rellena `de` desde la configuración SMTP del sistema
- Auto-resuelve `usuario`: busca un `User` cuyo DNI coincida con `correo.dni`; si lo encuentra, lo vincula
- Lanza un hilo en background que llama a `MailSender.send()`:
  - Éxito → `estado=ENVIADO`, `enviadoEn=ahora`
  - Fallo → `estado=ERROR`, `ultimoFalloEn=ahora`

**Reenviar** (botón en UI, solo cuando `estado=ERROR`):
- Disponible para todos los roles que pueden ver el correo, **excepto** el propio `usuario` vinculado al correo
- Lanza un nuevo hilo en background con el mismo comportamiento que la operación anterior
- Éxito → `estado=ENVIADO`, `enviadoEn=ahora`
- Fallo → `estado=ERROR`, `ultimoFalloEn=ahora` (actualiza la fecha)

---

### Vistas

- **`Correo@Main-grid`** (administradores — todos los centros): columnas estado, asunto, para, dni, centro, creadoEn. Filtros por estado, centro, fecha.
- **`Correo@Main-form`** (editable, crear desde UI): campos `para`, `asunto`, `cuerpoHtml`, `cuerpoTexto`, `dni`, `expediente` (opcional), `adjuntos`. El campo `centro` se auto-rellena con el centro del usuario logueado. El campo `de`, `usuario` y `estado` no son visibles ni editables.
- **`Correo@View-form`** (solo lectura — ver detalle): todos los campos + botón **"Reenviar"** visible únicamente cuando `estado=ERROR` Y el usuario logueado no es el `usuario` vinculado al correo.

---

### Menús

- Notificaciones → Correos → **Todos los correos** (administradores) → `Correo@Main-grid` sin filtro de centro
- Notificaciones → Correos → **Correos del centro** (supervisores, jefes de estudios, director, secretario, administrativas) → `Correo@Main-grid` filtrado por su centro
- Notificaciones → Correos → **Mis correos** (resto de usuarios) → `Correo@Main-grid` filtrado por `usuario = usuario actual`

---

### Seguridad

| Perfil | Puede ver | Puede crear | Puede reenviar |
|--------|-----------|-------------|----------------|
| Administrador | Todos (cualquier centro) | Sí | Sí |
| Supervisor / Jefe estudios / Director / Secretario / Administrativas | Los de su centro | Sí | Sí |
| Resto de usuarios | Solo los suyos (`usuario = ellos`) | No | No (son el `usuario` vinculado) |

**Multicentro:** sí — campo `centro` en la entidad `Correo`.

---

### Validaciones

**Operación: Crear correo**

*Validaciones de cliente (`action-validate`):*
- `para` — Nivel 1A (obligatoriedad). Mensaje: `"El campo 'Para' es obligatorio."`
- `para` — Nivel 1D (formato email: contiene `@` y dominio con `.`). Mensaje: `"El formato del email '{para}' no es válido."`
- `asunto` — Nivel 1A (obligatoriedad). Mensaje: `"El campo 'Asunto' es obligatorio."`
- `dni` — Nivel 1A (obligatoriedad). Mensaje: `"El campo 'DNI' es obligatorio."`
- `dni` — Nivel 1I (dígito de control DNI/NIE: 8 dígitos + letra, o X/Y/Z + 7 dígitos + letra). Mensaje: `"El formato del DNI/NIE '{dni}' no es válido."`
- `cuerpoHtml` + `cuerpoTexto` — Nivel 2A (al menos uno informado). Mensaje: `"Debe indicar el cuerpo del correo en formato HTML o en texto plano."`
- `centro` — Nivel 1A (obligatoriedad, aunque se auto-rellena). Mensaje: `"El campo 'Centro' es obligatorio."`

*Validaciones de servidor (`validateInsert`):* no aplican validaciones adicionales.

**Operación: Reenviar**

*Validación de servidor (controlador):*
- `estado` — Nivel 4A (transición válida): solo se puede reenviar si `estado=ERROR`. Mensaje: `"No se puede reenviar el correo '{asunto}' porque su estado actual es '{estado}'. Solo se pueden reenviar correos en estado ERROR."`

**Campos calculados:**
- `de`: calculado al insertar desde configuración SMTP. No editable. No se recalcula.
- `usuario`: calculado al insertar buscando `User` por `dni`. No editable. No se recalcula.
- `estado`: gestionado por el sistema tras cada intento de envío. No editable directamente.
- `enviadoEn`: asignado por el sistema al confirmar envío exitoso.
- `ultimoFalloEn`: asignado/actualizado por el sistema tras cada fallo.

**Ciclo de vida:**

| Estado | Quién lo asigna | Transiciones posibles | Campos editables en este estado |
|--------|-----------------|-----------------------|---------------------------------|
| `PENDIENTE` | Sistema al crear | → `ENVIADO`, → `ERROR` (background thread) | Ninguno |
| `ENVIADO` | Background thread | — (estado final) | Ninguno |
| `ERROR` | Background thread | → `ENVIADO`, → `ERROR` (botón Reenviar → background thread) | Ninguno |

---

### Asunciones tomadas

1. El campo `de` se almacena en BD para conservar el registro completo del correo tal y como se envió.
2. La configuración SMTP (host, usuario, contraseña, dirección `de`) es **global del sistema**, no por centro.
3. El campo `de` no es visible ni editable por el usuario en la UI.
4. El botón "Reenviar" aparece solo en `@View-form`, no en el grid.
5. No existe estado "borrador": al guardar desde UI el correo se envía inmediatamente en background.
6. La resolución de `usuario` a partir del `dni` se hace al crear y no se recalcula aunque cambie el DNI.
7. Los adjuntos se persisten como `MetaFile` de Axelor y se incluyen en el envío.
8. No hay reintentos automáticos; el reintento es exclusivamente manual vía botón "Reenviar".
9. El campo `cuerpoTexto` actúa como fallback para clientes de correo sin soporte HTML; no es obligatorio si hay `cuerpoHtml`.
10. El campo `expediente` es simplemente una FK de referencia; el subsistema `correos` no tiene dependencia funcional de la arquitectura interna de `expedientes`.
