# Pantalla: Mis correos

## Identidad

- **Quién la usa:** cualquier usuario autenticado (alumno, profesor, familiar, etc.), en solo lectura.
- **Qué muestra:** el listado de los correos que se le han enviado **con éxito** —aquellos cuyo DNI de destinatario coincide con el del usuario y cuyo estado es SUCCESS— y, al entrar en uno, su contenido y sus adjuntos para descargar. No muestra los correos en estado PENDIENTE ni FAIL, ni datos de fallo, y no permite ninguna acción salvo consultar y descargar adjuntos.

## Menú

- Mis correos — lo ve cualquier usuario autenticado; lleva a esta pantalla.

## Estructura jerárquica de las vistas

```
Listado de mis correos
└── Formulario de mi correo   (se abre al pulsar una fila)
    └── Listado de adjuntos   (panel maestro-detalle «Adjuntos» del formulario de mi correo)
        └── Formulario de adjunto   (se abre al pulsar una fila del listado de adjuntos)
```

## Vista: Listado de mis correos

- **Tipo:** listado
- **Qué muestra:** los correos cuyo DNI de destinatario es el del usuario actual y cuyo estado es SUCCESS, en lectura.
- **Se abre desde:** es la vista de entrada de la pantalla.

### Propiedades

- **Columnas (en orden):** asunto, para, nombre del expediente, fecha de envío
- **Ordenación por defecto:** por fecha de envío, descendente
- **Búsqueda / filtros:** sí, por asunto
- **Al pulsar una fila abre:** el formulario de mi correo

### Botones

- *(sin botones)*

## Vista: Formulario de mi correo

- **Tipo:** formulario
- **Qué muestra:** el contenido de un correo enviado con éxito al usuario y sus adjuntos, en solo lectura.
- **Se abre desde:** el listado de mis correos, al pulsar una fila.

### Propiedades

- **Modo:** solo lectura.

### Paneles

- **Datos del correo** (normal) — asunto, cuerpo, para, en copia, fecha de envío
- **Adjuntos** (maestro-detalle → «Listado de adjuntos») — los adjuntos del correo

### Botones

*(formulario de solo lectura: sin botones)*

## Vista: Listado de adjuntos

- **Tipo:** listado
- **Qué muestra:** los adjuntos del correo, en lectura.
- **Se abre desde:** embebido como panel «Adjuntos» en el formulario de mi correo.

### Propiedades

- **Columnas (en orden):** nombre del fichero
- **Ordenación por defecto:** por nombre del fichero, ascendente
- **Búsqueda / filtros:** no
- **Al pulsar una fila abre:** el formulario de adjunto

### Botones

- *(sin botones)*

## Vista: Formulario de adjunto

- **Tipo:** formulario
- **Qué muestra:** los datos de un adjunto del correo, en solo lectura.
- **Se abre desde:** el listado de adjuntos, al pulsar una fila.

### Propiedades

- **Modo:** solo lectura.

### Paneles

- **Adjunto** (normal) — nombre del fichero, contenido

### Botones

*(formulario de solo lectura: sin botones)*
