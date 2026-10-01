# Pantalla: Mis SMS

## Identidad

- **Quién la usa:** cualquier usuario de la aplicación (incluido el Administrador), en solo lectura.
- **Qué muestra:** los SMS enviados con éxito (estado ENVIADO) al DNI del usuario, de cualquier centro; el detalle de cada uno en solo lectura.

## Menú

- SMS → Recibidos — lo ven todos los usuarios, incluido el Administrador; lleva a esta pantalla.

## Estructura jerárquica de las vistas

```
Listado de mis SMS
└── Formulario de mi SMS   (se abre al pulsar una fila)
```

---

## Vista: Listado de mis SMS

- **Slug:** listado
- **Tipo:** listado
- **Qué muestra:** los SMS en estado ENVIADO cuyo DNI del destinatario es el del usuario, en lectura.
- **Se abre desde:** es la vista de entrada de la pantalla.

### Propiedades

- **Columnas (en orden):** mensaje, teléfono, nombre del expediente, fecha de envío
- **Ordenación por defecto:** por fecha de envío, del más reciente al más antiguo
- **Búsqueda / filtros:** sí, por cualquiera de las columnas
- **Al pulsar una fila abre:** el formulario de mi SMS

### Botones

*(sin botones)*

## Vista: Formulario de mi SMS

- **Slug:** formulario
- **Tipo:** formulario
- **Qué muestra:** el mensaje, el teléfono y la fecha de envío de uno de los SMS del usuario, en solo lectura.
- **Se abre desde:** el listado de mis SMS, al pulsar una fila.

### Propiedades

- **Modo:** siempre de solo lectura.

### Paneles

- **Datos del SMS** (normal) — mensaje, fecha de envío, teléfono

### Botones

- **Salir** — Vuelve al listado.
- Desviación del estándar: formulario de solo lectura, **sin «Guardar» ni «Borrar»**.
