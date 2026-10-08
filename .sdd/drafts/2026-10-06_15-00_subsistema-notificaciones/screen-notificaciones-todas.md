# Pantalla: Todas las notificaciones

## Identidad

- **Quién la usa:** el Administrador, para consultar, dar de alta y reenviar.
- **Qué muestra:** todas las notificaciones de todos los centros, de cualquier canal y en cualquier estado, en un único listado común. Desde él se da de alta un correo o un SMS (eligiendo antes el canal) y se abre cada notificación en el formulario de su canal, con sus adjuntos si es un correo. En alta los formularios son editables; en detalle, de solo lectura.

## Menú

- Notificaciones → Todas — lo ve solo el Administrador; lleva a esta pantalla.

## Estructura jerárquica de las vistas

```
Listado de notificaciones
├── Elección de canal   (se abre con «Nueva notificación»)
│   ├── Formulario de correo   (en alta, al elegir «Correo» y pulsar «Continuar»)
│   └── Formulario de SMS   (en alta, al elegir «SMS» y pulsar «Continuar»)
├── Formulario de correo   (en detalle, al pulsar una fila de tipo «Correo»)
│   └── Listado de adjuntos   (panel maestro-detalle «Adjuntos» del formulario de correo)
│       └── Formulario de adjunto   (en alta, con «Añadir adjunto»; en detalle se abre la pantalla compartida «Adjunto en consulta»)
└── Formulario de SMS   (en detalle, al pulsar una fila de tipo «SMS»)
```

---

## Vista: Listado de notificaciones

- **Slug:** listado
- **Tipo:** listado
- **Qué muestra:** todas las notificaciones de todos los centros y canales, en lectura.
- **Se abre desde:** es la vista de entrada de la pantalla.

### Propiedades

- **Columnas (en orden):** estado, tipo de notificación, motivo, DNI del destinatario, nombre, apellidos, destino, centro, expediente, fecha de creación, fecha de envío
- **Ordenación por defecto:** por fecha de creación, de la más reciente a la más antigua
- **Búsqueda / filtros:** sí, por cualquiera de las columnas (por ejemplo, solo los SMS, solo las fallidas o solo las de un centro)
- **Al pulsar una fila abre:** el formulario del canal de esa notificación: el formulario de correo si es un correo y el formulario de SMS si es un SMS; un canal nuevo abrirá el suyo

### Botones

- **Nueva notificación** (barra superior) — Abre la elección de canal para dar de alta una notificación.

## Vista: Elección de canal

- **Slug:** eleccion-canal
- **Tipo:** formulario
- **Qué muestra:** una ventana pequeña para elegir el canal de la notificación que se va a dar de alta; no guarda nada.
- **Se abre desde:** el listado de notificaciones, al pulsar «Nueva notificación».

### Propiedades

- **Modo:** editable.

### Paneles

- **Canal** (normal) — tipo de notificación a dar de alta: «Correo» o «SMS» (y los canales que se añadan en el futuro)
- **Botones** (botonera) — Cancelar y Continuar

### Botones

- **Continuar** — Cierra la ventana y abre en alta el formulario del canal elegido.
- **Cancelar** — Cierra la ventana y vuelve al listado sin crear nada.
- Sin Guardar ni Borrar: esta ventana no guarda ningún dato.

### Reglas de UI

- RUI-notificaciones-todas-eleccion-canal-001 — Al abrirse, no hay ningún canal elegido
  - disparador: al crear
  - condición: Siempre
- RUI-notificaciones-todas-eleccion-canal-002 — El botón «Continuar» solo está disponible cuando se ha elegido un canal
  - disparador: continuo
  - condición: hay un canal elegido

## Vista: Formulario de correo

- **Slug:** formulario-correo
- **Tipo:** formulario
- **Qué muestra:** un correo: en alta, para rellenarlo; en detalle, todos sus datos y los de su envío, en lectura.
- **Se abre desde:** la elección de canal, al elegir «Correo» y pulsar «Continuar» (alta); o el listado de notificaciones, al pulsar una fila de tipo «Correo» (detalle).

### Propiedades

- **Modo:** editable en el alta; en detalle, solo lectura.

### Paneles

- **Datos del correo** (normal) — tipo de notificación, motivo, centro, DNI del destinatario, nombre, apellidos, para, en copia, en copia oculta, asunto, cuerpo, estado del expediente
- **Adjuntos** (maestro-detalle → «Listado de adjuntos») — los adjuntos del correo
- **Datos del envío** (normal) — estado, número de reintentos, fecha de creación, fecha del primer intento de envío, fecha del último intento de envío, fecha de envío, descripción del último fallo
- **Botones** (botonera) — los botones de abajo

### Botones

- **Guardar** — Solo en el alta: valida y da de alta el correo, y vuelve al listado.
- **Cancelar** — Solo en el alta: vuelve al listado sin crear nada.
- **Reenviar** — Solo en detalle y si el correo está «Fallido»: lanza de nuevo su envío.
- **Salir** — Solo en detalle: vuelve al listado.
- Sin Borrar: un correo no se borra nunca. En detalle, sin Guardar: un correo no se modifica.

### Reglas de UI

- RUI-notificaciones-todas-formulario-correo-001 — El tipo de notificación se muestra siempre en solo lectura con el valor «Correo»
  - disparador: continuo
  - condición: Siempre
- RUI-notificaciones-todas-formulario-correo-002 — En detalle, todos los campos de «Datos del correo» se muestran en solo lectura
  - disparador: continuo
  - condición: el correo ya está guardado
- RUI-notificaciones-todas-formulario-correo-003 — El panel «Datos del envío» solo se muestra en detalle
  - disparador: continuo
  - condición: el correo ya está guardado
- RUI-notificaciones-todas-formulario-correo-004 — La fecha de envío solo se muestra si el estado es «Enviado»
  - disparador: continuo
  - condición: estado == ENVIADO
- RUI-notificaciones-todas-formulario-correo-005 — La descripción del último fallo solo se muestra si el estado es «Fallido»
  - disparador: continuo
  - condición: estado == FALLIDO
- RUI-notificaciones-todas-formulario-correo-006 — Los botones «Guardar» y «Cancelar» solo se muestran en el alta; el botón «Salir», solo en detalle
  - disparador: continuo
  - condición: el correo no está guardado (Guardar, Cancelar) / el correo ya está guardado (Salir)
- RUI-notificaciones-todas-formulario-correo-007 — El botón «Reenviar» solo se muestra en detalle y si el estado es «Fallido»
  - disparador: continuo
  - condición: el correo ya está guardado y estado == FALLIDO
- RUI-notificaciones-todas-formulario-correo-008 — Al pulsar «Guardar» en el alta, si falta el motivo, el centro, el DNI del destinatario, el nombre, los apellidos, el «para», el asunto o el cuerpo, el formulario avisa con el mensaje de la validación correspondiente sin enviar el correo
  - disparador: al pulsar «Guardar»
  - condición: falta alguno de esos datos
- RUI-notificaciones-todas-formulario-correo-009 — Tras pulsar «Reenviar» con éxito, se muestra el aviso «El reenvío del correo se ha puesto en marcha.»
  - disparador: al pulsar «Reenviar»
  - condición: el reenvío se ha aceptado
- RUI-notificaciones-todas-formulario-correo-010 — En el alta, el selector del estado del expediente solo ofrece estados de expedientes del centro elegido
  - disparador: al cambiar centro
  - condición: hay un centro elegido
- RUI-notificaciones-todas-formulario-correo-011 — Al cambiar el centro en el alta, el estado del expediente elegido se vacía
  - disparador: al cambiar centro
  - condición: ya había un estado del expediente elegido
- RUI-notificaciones-todas-formulario-correo-012 — En el alta, el motivo, el centro, el DNI del destinatario, el nombre, los apellidos, el «para», el asunto y el cuerpo se marcan como obligatorios
  - disparador: continuo
  - condición: el correo no está guardado

## Vista: Listado de adjuntos

- **Slug:** listado-adjuntos
- **Tipo:** listado
- **Qué muestra:** los adjuntos del correo, en lectura.
- **Se abre desde:** embebido como panel «Adjuntos» en el formulario de correo.

### Propiedades

- **Columnas (en orden):** nombre del fichero
- **Ordenación por defecto:** por nombre del fichero, ascendente
- **Búsqueda / filtros:** no
- **Al pulsar una fila abre:** en el alta, el formulario de adjunto; en detalle, la pantalla compartida «Adjunto en consulta»

### Botones

- **Añadir adjunto** (barra superior) — Solo en el alta del correo: abre el formulario de alta de un adjunto.

### Reglas de UI

- RUI-notificaciones-todas-listado-adjuntos-001 — El botón «Añadir adjunto» solo se muestra en el alta del correo
  - disparador: continuo
  - condición: el correo no está guardado

## Vista: Formulario de adjunto

- **Slug:** formulario-adjunto
- **Tipo:** formulario
- **Qué muestra:** un adjunto que se está añadiendo al correo en alta, en edición.
- **Se abre desde:** el listado de adjuntos, con «Añadir adjunto» o al pulsar una fila mientras el correo está en alta.

### Propiedades

- **Modo:** editable.

### Paneles

- **Adjunto** (normal) — nombre del fichero, contenido (el fichero, que se sube)
- **Botones** (botonera) — los botones de abajo

### Botones

- **Guardar** — Añade el adjunto a la lista del correo (se guarda de verdad al guardar el correo).
- **Cancelar** — Cierra la ventana sin añadirlo.
- **Borrar** — Solo para un adjunto ya añadido a la lista y mientras el correo sigue en alta: lo quita de la lista.

### Reglas de UI

- RUI-notificaciones-todas-formulario-adjunto-001 — Al añadir un adjunto, el correo al que pertenece se rellena con el correo que se está dando de alta
  - disparador: al crear
  - condición: Siempre
- RUI-notificaciones-todas-formulario-adjunto-002 — El nombre del fichero se marca como obligatorio y, si falta al pulsar «Guardar», se avisa con «El nombre del fichero es obligatorio»
  - disparador: continuo
  - condición: Siempre
- RUI-notificaciones-todas-formulario-adjunto-003 — El contenido se marca como obligatorio y, si falta al pulsar «Guardar», se avisa con «Debe adjuntar el fichero»
  - disparador: continuo
  - condición: Siempre
- RUI-notificaciones-todas-formulario-adjunto-004 — Si el fichero subido supera los 10 MB, al subir el fichero, la plataforma avisa de que no se puede subir un fichero de más de 10 MB y no lo adjunta
  - disparador: al subir el fichero
  - condición: el fichero supera los 10 MB
- RUI-notificaciones-todas-formulario-adjunto-005 — Si el nombre del fichero contiene «/», «\» o caracteres de control, al pulsar «Guardar» en la ventana del adjunto se avisa con «El nombre del fichero no puede contener los caracteres / \ ni caracteres de control» y no se añade
  - disparador: al pulsar «Guardar»
  - condición: el nombre del fichero contiene alguno de esos caracteres
- RUI-notificaciones-todas-formulario-adjunto-006 — El botón «Borrar» solo se muestra en un adjunto que ya se había añadido a la lista; al añadir uno nuevo no aparece
  - disparador: continuo
  - condición: el adjunto ya está en la lista del correo en alta

## Vista: Formulario de SMS

- **Slug:** formulario-sms
- **Tipo:** formulario
- **Qué muestra:** un SMS: en alta, para rellenarlo; en detalle, todos sus datos y los de su envío, en lectura.
- **Se abre desde:** la elección de canal, al elegir «SMS» y pulsar «Continuar» (alta); o el listado de notificaciones, al pulsar una fila de tipo «SMS» (detalle).

### Propiedades

- **Modo:** editable en el alta; en detalle, solo lectura.

### Paneles

- **Datos del SMS** (normal) — tipo de notificación, motivo, centro, DNI del destinatario, nombre, apellidos, teléfono (solo móviles de España), estado del expediente, mensaje
- **Datos del envío** (normal) — estado, número de reintentos, fecha de creación, fecha del primer intento de envío, fecha del último intento de envío, fecha de envío, descripción del último fallo
- **Botones** (botonera) — los botones de abajo

### Botones

- **Guardar** — Solo en el alta: valida y da de alta el SMS, y vuelve al listado.
- **Cancelar** — Solo en el alta: vuelve al listado sin crear nada.
- **Reenviar** — Solo en detalle y si el SMS está «Fallido»: lanza de nuevo su envío.
- **Salir** — Solo en detalle: vuelve al listado.
- Sin Borrar: un SMS no se borra nunca. En detalle, sin Guardar: un SMS no se modifica.

### Reglas de UI

- RUI-notificaciones-todas-formulario-sms-001 — El tipo de notificación se muestra siempre en solo lectura con el valor «SMS»
  - disparador: continuo
  - condición: Siempre
- RUI-notificaciones-todas-formulario-sms-002 — En detalle, todos los campos de «Datos del SMS» se muestran en solo lectura
  - disparador: continuo
  - condición: el SMS ya está guardado
- RUI-notificaciones-todas-formulario-sms-003 — El panel «Datos del envío» solo se muestra en detalle
  - disparador: continuo
  - condición: el SMS ya está guardado
- RUI-notificaciones-todas-formulario-sms-004 — La fecha de envío solo se muestra si el estado es «Enviado»
  - disparador: continuo
  - condición: estado == ENVIADO
- RUI-notificaciones-todas-formulario-sms-005 — La descripción del último fallo solo se muestra si el estado es «Fallido»
  - disparador: continuo
  - condición: estado == FALLIDO
- RUI-notificaciones-todas-formulario-sms-006 — Los botones «Guardar» y «Cancelar» solo se muestran en el alta; el botón «Salir», solo en detalle
  - disparador: continuo
  - condición: el SMS no está guardado (Guardar, Cancelar) / el SMS ya está guardado (Salir)
- RUI-notificaciones-todas-formulario-sms-007 — El botón «Reenviar» solo se muestra en detalle y si el estado es «Fallido»
  - disparador: continuo
  - condición: el SMS ya está guardado y estado == FALLIDO
- RUI-notificaciones-todas-formulario-sms-008 — Al pulsar «Guardar» en el alta, si falta el motivo, el centro, el DNI del destinatario, el nombre, los apellidos, el teléfono o el mensaje, el formulario avisa con el mensaje de la validación correspondiente sin enviar el SMS
  - disparador: al pulsar «Guardar»
  - condición: falta alguno de esos datos
- RUI-notificaciones-todas-formulario-sms-009 — El teléfono se introduce con un campo de teléfono limitado a números de España
  - disparador: continuo
  - condición: Siempre
- RUI-notificaciones-todas-formulario-sms-010 — Tras pulsar «Reenviar» con éxito, se muestra el aviso «El reenvío del SMS se ha puesto en marcha.»
  - disparador: al pulsar «Reenviar»
  - condición: el reenvío se ha aceptado
- RUI-notificaciones-todas-formulario-sms-011 — En el alta, el selector del estado del expediente solo ofrece estados de expedientes del centro elegido
  - disparador: al cambiar centro
  - condición: hay un centro elegido
- RUI-notificaciones-todas-formulario-sms-012 — Al cambiar el centro en el alta, el estado del expediente elegido se vacía
  - disparador: al cambiar centro
  - condición: ya había un estado del expediente elegido
- RUI-notificaciones-todas-formulario-sms-013 — En el alta, el motivo, el centro, el DNI del destinatario, el nombre, los apellidos, el teléfono y el mensaje se marcan como obligatorios
  - disparador: continuo
  - condición: el SMS no está guardado
- RUI-notificaciones-todas-formulario-sms-014 — Al pulsar «Reenviar» se pide confirmación con el aviso «Reenviar un SMS tiene coste. ¿Desea reenviarlo?»; si el usuario cancela, no se reenvía
  - disparador: al pulsar «Reenviar»
  - condición: Siempre
