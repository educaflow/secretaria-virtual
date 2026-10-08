# Pantalla: Notificaciones recibidas

## Identidad

- **Quién la usa:** cualquier usuario autenticado, como destinatario, en lectura.
- **Qué muestra:** las notificaciones cuyo DNI del destinatario es el DNI del usuario y que están en estado «Enviado», de cualquier canal y de cualquier centro, en un único listado común. Cada una se abre en el formulario de su canal, en solo lectura, con sus adjuntos si es un correo. El destinatario nunca ve el motivo ni los datos internos del envío.

## Menú

- Notificaciones → Recibidas — lo ven todos los usuarios autenticados, incluido el Administrador; lleva a esta pantalla.

## Estructura jerárquica de las vistas

```
Listado de notificaciones recibidas
├── Formulario de correo   (al pulsar una fila de tipo «Correo»)
│   └── Listado de adjuntos   (panel maestro-detalle «Adjuntos» del formulario de correo; cada fila abre la pantalla compartida «Adjunto en consulta»)
└── Formulario de SMS   (al pulsar una fila de tipo «SMS»)
```

---

## Vista: Listado de notificaciones recibidas

- **Slug:** listado
- **Tipo:** listado
- **Qué muestra:** las notificaciones enviadas con éxito al DNI del usuario, en lectura.
- **Se abre desde:** es la vista de entrada de la pantalla.

### Propiedades

- **Columnas (en orden):** tipo de notificación, destino, expediente, fecha de envío
- **Ordenación por defecto:** por fecha de envío, de la más reciente a la más antigua
- **Búsqueda / filtros:** sí, por cualquiera de las columnas
- **Al pulsar una fila abre:** el formulario del canal de esa notificación: el formulario de correo si es un correo y el formulario de SMS si es un SMS

### Botones

*(sin botones)*

## Vista: Formulario de correo

- **Slug:** formulario-correo
- **Tipo:** formulario
- **Qué muestra:** el correo tal como lo recibió el destinatario, en lectura.
- **Se abre desde:** el listado de notificaciones recibidas, al pulsar una fila de tipo «Correo».

### Propiedades

- **Modo:** siempre de solo lectura.

### Paneles

- **Datos del correo** (normal) — asunto, fecha de envío, cuerpo, para, en copia
- **Adjuntos** (maestro-detalle → «Listado de adjuntos») — los adjuntos del correo
- **Botones** (botonera) — Salir

### Botones

- **Salir** — Vuelve al listado.
- Sin Guardar ni Borrar: el formulario es de solo lectura y un correo no se borra nunca.

### Reglas de UI

- RUI-notificaciones-recibidas-formulario-correo-001 — El campo «en copia» solo se muestra si el correo tiene alguna dirección en copia
  - disparador: continuo
  - condición: el «en copia» está indicado
- RUI-notificaciones-recibidas-formulario-correo-002 — El panel «Adjuntos» solo se muestra si el correo tiene al menos un adjunto
  - disparador: al cargar
  - condición: el correo tiene adjuntos

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
- **Qué muestra:** el SMS tal como lo recibió el destinatario, en lectura.
- **Se abre desde:** el listado de notificaciones recibidas, al pulsar una fila de tipo «SMS».

### Propiedades

- **Modo:** siempre de solo lectura.

### Paneles

- **Datos del SMS** (normal) — teléfono, fecha de envío, mensaje
- **Botones** (botonera) — Salir

### Botones

- **Salir** — Vuelve al listado.
- Sin Guardar ni Borrar: el formulario es de solo lectura y un SMS no se borra nunca.
