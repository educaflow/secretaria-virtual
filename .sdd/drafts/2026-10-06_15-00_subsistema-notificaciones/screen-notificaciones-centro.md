# Pantalla: Notificaciones del centro

## Identidad

- **Quién la usa:** los gestores del centro (Supervisor, Administrativo, Director, Jefe de estudios y Secretario), para consultar y reenviar las fallidas.
- **Qué muestra:** las notificaciones de los centros en los que el usuario tiene el tipo de usuario Supervisor o Administrativo, o el cargo de Director, Jefe de estudios o Secretario, de cualquier canal y en cualquier estado, en un único listado común. No hay ningún «centro activo»: se ven las de todos esos centros y la columna centro permite filtrar. Cada notificación se abre en el formulario de su canal, siempre en solo lectura, con sus adjuntos si es un correo.

## Menú

- Notificaciones → Del centro — lo ven solo los usuarios que, en al menos un centro, tienen el tipo de usuario Supervisor o Administrativo, o el cargo de Director, Jefe de estudios o Secretario (no lo ve el Administrador, que usa «Todas»); lleva a esta pantalla.

## Estructura jerárquica de las vistas

```
Listado de notificaciones del centro
├── Formulario de correo   (al pulsar una fila de tipo «Correo»)
│   └── Listado de adjuntos   (panel maestro-detalle «Adjuntos» del formulario de correo; cada fila abre la pantalla compartida «Adjunto en consulta»)
└── Formulario de SMS   (al pulsar una fila de tipo «SMS»)
```

---

## Vista: Listado de notificaciones del centro

- **Slug:** listado
- **Tipo:** listado
- **Qué muestra:** las notificaciones de los centros del usuario donde tiene un tipo o cargo de gestión, en lectura.
- **Se abre desde:** es la vista de entrada de la pantalla.

### Propiedades

- **Columnas (en orden):** centro, tipo de notificación, estado, DNI del destinatario, nombre, apellidos, motivo, destino, expediente, fecha de creación, fecha de envío
- **Ordenación por defecto:** por fecha de creación, de la más reciente a la más antigua
- **Búsqueda / filtros:** sí, por cualquiera de las columnas (en particular, por centro cuando el usuario gestiona varios)
- **Al pulsar una fila abre:** el formulario del canal de esa notificación: el formulario de correo si es un correo y el formulario de SMS si es un SMS

### Botones

*(sin botones)*

### Reglas de UI

- RUI-notificaciones-centro-listado-001 — Las filas de notificaciones en estado «Fallido» se resaltan para distinguirlas del resto
  - disparador: continuo
  - condición: estado == FALLIDO

## Vista: Formulario de correo

- **Slug:** formulario-correo
- **Tipo:** formulario
- **Qué muestra:** todos los datos de un correo y de su envío, en lectura.
- **Se abre desde:** el listado de notificaciones del centro, al pulsar una fila de tipo «Correo».

### Propiedades

- **Modo:** siempre de solo lectura.

### Paneles

- **Datos del correo** (normal) — tipo de notificación, motivo, centro, DNI del destinatario, nombre, apellidos, para, en copia, en copia oculta, asunto, cuerpo, estado del expediente
- **Adjuntos** (maestro-detalle → «Listado de adjuntos») — los adjuntos del correo
- **Datos del envío** (normal) — estado, número de reintentos, fecha de creación, fecha del primer intento de envío, fecha del último intento de envío, fecha de envío, descripción del último fallo
- **Botones** (botonera) — Reenviar y Salir

### Botones

- **Reenviar** — Solo si el correo está «Fallido»: lanza de nuevo su envío.
- **Salir** — Vuelve al listado.
- Sin Guardar ni Borrar: el formulario es de solo lectura y un correo no se borra nunca.

### Reglas de UI

- RUI-notificaciones-centro-formulario-correo-001 — La fecha de envío solo se muestra si el estado es «Enviado»
  - disparador: continuo
  - condición: estado == ENVIADO
- RUI-notificaciones-centro-formulario-correo-002 — La descripción del último fallo solo se muestra si el estado es «Fallido»
  - disparador: continuo
  - condición: estado == FALLIDO
- RUI-notificaciones-centro-formulario-correo-003 — El botón «Reenviar» solo se muestra si el estado es «Fallido»
  - disparador: continuo
  - condición: estado == FALLIDO
- RUI-notificaciones-centro-formulario-correo-004 — Tras pulsar «Reenviar» con éxito, se muestra el aviso «El reenvío del correo se ha puesto en marcha.»
  - disparador: al pulsar «Reenviar»
  - condición: el reenvío se ha aceptado
- RUI-notificaciones-centro-formulario-correo-005 — La fecha del primer intento de envío y la del último intento solo se muestran si el estado no es «Pendiente»
  - disparador: continuo
  - condición: estado != PENDIENTE
- RUI-notificaciones-centro-formulario-correo-006 — El estado del expediente solo se muestra si la notificación está ligada a un estado de expediente
  - disparador: continuo
  - condición: el estado del expediente está indicado

## Vista: Listado de adjuntos

- **Slug:** listado-adjuntos
- **Tipo:** listado
- **Qué muestra:** los adjuntos del correo, en lectura.
- **Se abre desde:** embebido como panel «Adjuntos» en el formulario de correo.

### Propiedades

- **Columnas (en orden):** nombre del fichero
- **Ordenación por defecto:** por nombre del fichero, ascendente
- **Búsqueda / filtros:** no
- **Al pulsar una fila abre:** la pantalla compartida «Adjunto en consulta»

### Botones

*(sin botones)*

## Vista: Formulario de SMS

- **Slug:** formulario-sms
- **Tipo:** formulario
- **Qué muestra:** todos los datos de un SMS y de su envío, en lectura.
- **Se abre desde:** el listado de notificaciones del centro, al pulsar una fila de tipo «SMS».

### Propiedades

- **Modo:** siempre de solo lectura.

### Paneles

- **Datos del SMS** (normal) — tipo de notificación, motivo, centro, DNI del destinatario, nombre, apellidos, teléfono, estado del expediente, mensaje
- **Datos del envío** (normal) — estado, número de reintentos, fecha de creación, fecha del primer intento de envío, fecha del último intento de envío, fecha de envío, descripción del último fallo
- **Botones** (botonera) — Reenviar y Salir

### Botones

- **Reenviar** — Solo si el SMS está «Fallido»: lanza de nuevo su envío.
- **Salir** — Vuelve al listado.
- Sin Guardar ni Borrar: el formulario es de solo lectura y un SMS no se borra nunca.

### Reglas de UI

- RUI-notificaciones-centro-formulario-sms-001 — La fecha de envío solo se muestra si el estado es «Enviado»
  - disparador: continuo
  - condición: estado == ENVIADO
- RUI-notificaciones-centro-formulario-sms-002 — La descripción del último fallo solo se muestra si el estado es «Fallido»
  - disparador: continuo
  - condición: estado == FALLIDO
- RUI-notificaciones-centro-formulario-sms-003 — El botón «Reenviar» solo se muestra si el estado es «Fallido»
  - disparador: continuo
  - condición: estado == FALLIDO
- RUI-notificaciones-centro-formulario-sms-004 — Tras pulsar «Reenviar» con éxito, se muestra el aviso «El reenvío del SMS se ha puesto en marcha.»
  - disparador: al pulsar «Reenviar»
  - condición: el reenvío se ha aceptado
- RUI-notificaciones-centro-formulario-sms-005 — Al pulsar «Reenviar» se pide confirmación con el aviso «Reenviar un SMS tiene coste. ¿Desea reenviarlo?»; si el usuario cancela, no se reenvía
  - disparador: al pulsar «Reenviar»
  - condición: Siempre
- RUI-notificaciones-centro-formulario-sms-006 — La fecha del primer intento de envío y la del último intento solo se muestran si el estado no es «Pendiente»
  - disparador: continuo
  - condición: estado != PENDIENTE
- RUI-notificaciones-centro-formulario-sms-007 — El estado del expediente solo se muestra si la notificación está ligada a un estado de expediente
  - disparador: continuo
  - condición: el estado del expediente está indicado
