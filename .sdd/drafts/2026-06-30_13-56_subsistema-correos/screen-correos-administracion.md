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

*(el administrador solo puede crear correos, no modificarlos: durante el alta lleva los botones estándar Guardar y Cancelar para crear y guardar el correo nuevo, pero al reabrir un correo ya creado el formulario es de solo lectura, sin Guardar. El correo no se puede borrar nunca, para preservar el historial de envíos: sin botón Borrar)*

### Reglas de UI

- RUI-001 — El botón «Reenviar» solo se muestra cuando el estado del correo es FAIL
  - disparador: continuo
  - condición: estado == FAIL
- RUI-002 — El panel «Datos del envío» solo se muestra al consultar un correo ya creado, no durante el alta
  - disparador: continuo
  - condición: el correo ya existe

## Vista: Listado de adjuntos

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

## Vista: Formulario de adjunto

- **Tipo:** formulario
- **Qué muestra:** los datos de un adjunto del correo, en edición durante el alta.
- **Se abre desde:** el listado de adjuntos, al pulsar una fila o «Añadir adjunto».

### Propiedades

- **Modo:** editable durante el alta del correo; al consultar un correo ya creado, de solo lectura.

### Paneles

- **Adjunto** (normal) — nombre del fichero, contenido

### Botones

*(el adjunto solo se puede añadir durante el alta del correo, no modificar: mientras se da de alta lleva los botones estándar Guardar y Cancelar, pero al reabrir un correo ya creado es de solo lectura, sin Guardar. El adjunto no se puede borrar, igual que el correo al que pertenece, para preservar el historial de envíos: sin botón Borrar)*

### Reglas de UI

- RUI-003 — Al añadir un adjunto, su correo se fija con el correo desde el que se abre el formulario
  - disparador: al crear
  - condición: Siempre
