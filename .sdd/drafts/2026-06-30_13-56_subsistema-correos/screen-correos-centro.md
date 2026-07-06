# Pantalla: Correos del centro

## Identidad

- **Quién la usa:** el Supervisor, en consulta (revisa los correos de su centro y reintenta los fallidos).
- **Qué muestra:** el listado de los correos del **propio centro** del supervisor, en cualquier estado, y, al entrar en uno, su detalle completo con los datos del envío y sus adjuntos, con la posibilidad de reenviar los que han fallado. No permite crear ni editar correos.

## Menú

- Correos → Correos de mi centro — lo ve el Supervisor; lleva a esta pantalla.

## Estructura jerárquica de las vistas

```
Listado de correos del centro
└── Formulario de correo del centro   (se abre al pulsar una fila)
    └── Listado de adjuntos   (panel maestro-detalle «Adjuntos» del formulario de correo)
        └── Formulario de adjunto   (se abre al pulsar una fila del listado de adjuntos)
```

## Vista: Listado de correos del centro

- **Slug:** listado
- **Tipo:** listado
- **Qué muestra:** los correos cuyo centro es el centro del supervisor, en cualquier estado, en lectura.
- **Se abre desde:** es la vista de entrada de la pantalla.

### Propiedades

- **Columnas (en orden):** estado, dni del destinatario, nombre, apellidos, asunto, para, nombre del expediente, fecha de creación, fecha de envío
- **Ordenación por defecto:** por fecha de creación, descendente
- **Búsqueda / filtros:** sí, por estado, dni del destinatario y asunto
- **Al pulsar una fila abre:** el formulario de correo del centro

### Botones

- *(sin botones)*

## Vista: Formulario de correo del centro

- **Slug:** formulario
- **Tipo:** formulario
- **Qué muestra:** los datos de un correo del centro, sus adjuntos y el resultado de su envío, en solo lectura.
- **Se abre desde:** el listado de correos del centro, al pulsar una fila.

### Propiedades

- **Modo:** solo lectura.

### Paneles

- **Datos del correo** (normal) — dni del destinatario, nombre, apellidos, para, en copia, en copia oculta, asunto, cuerpo, centro, historial de estado
- **Adjuntos** (maestro-detalle → «Listado de adjuntos») — los adjuntos del correo
- **Datos del envío** (normal) — estado, fecha de creación, fecha del primer intento de envío, fecha del último intento de envío, fecha de envío, número de reintentos, descripción del último fallo

### Botones

- **Reenviar** — Vuelve a intentar el envío del correo; visible solo cuando el correo está en estado FAIL.


### Reglas de UI

- RUI-correos-centro-formulario-001 — El botón «Reenviar» solo se muestra cuando el estado del correo es FAIL
  - disparador: continuo
  - condición: estado == FAIL
- RUI-correos-centro-formulario-002 — La «descripción del último fallo» solo se muestra cuando el correo está en estado FAIL
  - disparador: continuo
  - condición: estado == FAIL
- RUI-correos-centro-formulario-003 — La «fecha de envío» solo se muestra cuando el correo está en estado SUCCESS
  - disparador: continuo
  - condición: estado == SUCCESS
- RUI-correos-centro-formulario-004 — Tras pulsar «Reenviar» se muestra un aviso breve de que el reenvío está en curso
  - disparador: al terminar la acción (tras pulsar Reenviar)
  - condición: Siempre

## Vista: Listado de adjuntos

- **Slug:** listado-adjuntos
- **Tipo:** listado
- **Qué muestra:** los adjuntos del correo, en lectura.
- **Se abre desde:** embebido como panel «Adjuntos» en el formulario de correo del centro.

### Propiedades

- **Columnas (en orden):** nombre del fichero
- **Ordenación por defecto:** por nombre del fichero, ascendente
- **Búsqueda / filtros:** no
- **Al pulsar una fila abre:** el formulario de adjunto

### Botones

- *(sin botones)*

## Vista: Formulario de adjunto

- **Slug:** formulario-adjunto
- **Tipo:** formulario
- **Qué muestra:** los datos de un adjunto del correo, en solo lectura.
- **Se abre desde:** el listado de adjuntos, al pulsar una fila.

### Propiedades

- **Modo:** solo lectura.

### Paneles

- **Adjunto** (normal) — nombre del fichero, contenido

### Botones

- **Salir**: Cierra la ventana
