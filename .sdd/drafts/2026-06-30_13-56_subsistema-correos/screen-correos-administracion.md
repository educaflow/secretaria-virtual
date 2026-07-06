# Pantalla: Administración de correos

## Identidad

- **Quién la usa:** el Administrador, en edición (crea correos) y consulta (revisa cualquier correo y reenvía los fallidos).
- **Qué muestra:** el listado de todos los correos de todos los centros y, al entrar en uno, su detalle completo (contenido, adjuntos y datos del envío), con la posibilidad de crear correos nuevos y de reenviar los que han fallado.

## Menú

- Correos → Administración de correos — lo ve el Administrador; lleva a esta pantalla.

## Estructura jerárquica de las vistas

```
Listado de correos
└── Formulario de correo   (se abre al pulsar una fila o con «Nuevo correo»)
    └── Listado de adjuntos   (panel maestro-detalle «Adjuntos» del formulario de correo)
        └── Formulario de adjunto   (se abre al pulsar una fila del listado de adjuntos o con «Añadir adjunto»)
```

## Vista: Listado de correos

- **Slug:** listado
- **Tipo:** listado
- **Qué muestra:** todos los correos de todos los centros, en lectura.
- **Se abre desde:** es la vista de entrada de la pantalla.

### Propiedades

- **Columnas (en orden):** estado, dni del destinatario, nombre, apellidos, asunto, para, centro, nombre del expediente, fecha de creación, fecha de envío
- **Ordenación por defecto:** por fecha de creación, descendente
- **Búsqueda / filtros:** sí, por estado, centro, dni del destinatario y asunto
- **Al pulsar una fila abre:** el formulario de correo

### Botones

- **Nuevo correo** (barra superior) — Abre el formulario de alta de un correo.

## Vista: Formulario de correo

- **Slug:** formulario
- **Tipo:** formulario
- **Qué muestra:** los datos de un correo, sus adjuntos y el resultado de su envío.
- **Se abre desde:** el listado de correos, al pulsar una fila o «Nuevo correo».

### Propiedades

- **Modo:** editable en el alta; al abrir un correo ya creado, de solo lectura (sus datos son inmutables).

### Paneles

- **Datos del correo** (normal) — dni del destinatario, nombre, apellidos, para, en copia, en copia oculta, asunto, cuerpo, centro, historial de estado
- **Adjuntos** (maestro-detalle → «Listado de adjuntos») — los adjuntos del correo
- **Datos del envío** (normal) — estado, fecha de creación, fecha del primer intento de envío, fecha del último intento de envío, fecha de envío, número de reintentos, descripción del último fallo

### Botones

- **Reenviar** — Vuelve a intentar el envío del correo; visible solo cuando el correo está en estado FAIL.
- **Cancelar/Salir** — Sale de la pantalla.
- **Guardar** — Guardar el correo.

### Reglas de UI

- RUI-correos-administracion-formulario-001 — El botón «Reenviar» solo se muestra cuando el estado del correo es FAIL
  - disparador: continuo
  - condición: estado == FAIL
- RUI-correos-administracion-formulario-002 — El panel «Datos del envío» solo se muestra al consultar un correo ya creado, no durante el alta
  - disparador: continuo
  - condición: el correo ya existe
- RUI-correos-administracion-formulario-003 — El label del botón «Cancelar/Salir» es "Cancelar" si es una alta.
  - disparador: continuo
  - condición: el correo aún no existe 
- RUI-correos-administracion-formulario-004 — El label del botón «Cancelar/Salir» es "Salir" si NO es una alta.
  - disparador: continuo
  - condición: el correo existe
- RUI-correos-administracion-formulario-005 — El botón «Guardar» solo se muestra si es un Alta.
  - disparador: continuo
  - condición: el correo aún no existe
- RUI-correos-administracion-formulario-006 — Al consultar un correo ya creado, el formulario y sus datos (dni, nombre, apellidos, para, en copia, en copia oculta, asunto, cuerpo, centro, historial de estado) se muestran en solo lectura; solo son editables durante el alta.
  - disparador: continuo
  - condición: el correo ya existe
- RUI-correos-administracion-formulario-007 — La «descripción del último fallo» solo se muestra cuando el correo está en estado FAIL.
  - disparador: continuo
  - condición: estado == FAIL
- RUI-correos-administracion-formulario-008 — La «fecha de envío» solo se muestra cuando el correo está en estado SUCCESS.
  - disparador: continuo
  - condición: estado == SUCCESS

## Vista: Listado de adjuntos

- **Slug:** listado-adjuntos
- **Tipo:** listado
- **Qué muestra:** los adjuntos del correo, en lectura.
- **Se abre desde:** embebido como panel «Adjuntos» en el formulario de correo.

### Propiedades

- **Columnas (en orden):** nombre del fichero
- **Ordenación por defecto:** por nombre del fichero, ascendente
- **Búsqueda / filtros:** no
- **Al pulsar una fila abre:** el formulario de adjunto

### Botones

- **Añadir adjunto** (barra superior) — Abre el formulario de alta de un adjunto; visible solo durante el alta del correo.

### Reglas de UI

- RUI-correos-administracion-listado-adjuntos-001 — El botón «Añadir adjunto» solo se muestra durante el alta del correo, no al consultar un correo ya creado
  - disparador: continuo
  - condición: el correo aún no existe

## Vista: Formulario de adjunto

- **Slug:** formulario-adjunto
- **Tipo:** formulario
- **Qué muestra:** los datos de un adjunto del correo, en edición durante el alta.
- **Se abre desde:** el listado de adjuntos, al pulsar una fila o «Añadir adjunto».

### Propiedades

- **Modo:** editable durante el alta del correo; al consultar un correo ya creado, de solo lectura.

### Paneles

- **Adjunto** (normal) — nombre del fichero, contenido

### Botones

- **Cancelar/Salir** — Sale de la pantalla.
- **Guardar** — Guardar el correo.

### Reglas de UI

- RUI-correos-administracion-formulario-adjunto-001 — Al añadir un adjunto, su correo se fija con el correo desde el que se abre el formulario
  - disparador: al crear
  - condición: Siempre
- RUI-correos-administracion-formulario-adjunto-002 — El label del botón «Cancelar/Salir» es "Cancelar" si es una alta.
  - disparador: continuo
  - condición: el adjunto aún no existe
- RUI-correos-administracion-formulario-adjunto-003 — El label del botón «Cancelar/Salir» es "Salir" si NO es una alta.
  - disparador: continuo
  - condición: el adjunto existe
- RUI-correos-administracion-formulario-adjunto-004 — El botón «Guardar» solo se muestra si es un Alta.
  - disparador: continuo
  - condición: el adjunto aún no existe
- RUI-correos-administracion-formulario-adjunto-005 — Al añadir un adjunto, el nombre del fichero se marca como obligatorio
  - disparador: continuo
  - condición: Siempre
- RUI-correos-administracion-formulario-adjunto-006 — Al añadir un adjunto, el contenido se marca como obligatorio
  - disparador: continuo
  - condición: Siempre
- RUI-correos-administracion-formulario-adjunto-007 — Al consultar un adjunto de un correo ya creado, sus campos (nombre del fichero y contenido) se muestran en solo lectura; solo son editables durante el alta del correo.
  - disparador: continuo
  - condición: el correo ya existe