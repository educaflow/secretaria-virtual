# Pantalla: Smoke test

## Identidad

- **Quién la usa:** el Administrador, en edición (es el único rol con acceso).
- **Qué muestra:** el listado de los registros de smoke test y, al entrar en uno, su formulario de detalle con el texto y las dos fechas selladas por el servidor.

## Menú

La pantalla cuelga de un **menú de primer nivel nuevo llamado «Desarrollador»**, visible solo para el Administrador. Dentro de «Desarrollador»:

- **Smoke test** — lleva al listado de smoke test (vista de entrada de esta pantalla).
- **Utilidades de PDF** — el menú ya existente (Información, Posiciones Firma, Posición Autofirma) deja de estar en primer nivel y pasa a colgar aquí; queda restringido también solo al Administrador. Sus pantallas no se modifican; solo cambia su ubicación y su acceso.

## Estructura jerárquica de las vistas

```
Listado de smoke tests
└── Formulario de smoke test   (se abre al pulsar una fila o con «Nuevo»)
```

## Vista: Listado de smoke tests

- **Tipo:** listado
- **Qué muestra:** todos los registros de smoke test, en lectura.
- **Se abre desde:** es la vista de entrada de la pantalla (menú «Desarrollador» → «Smoke test»).

### Propiedades

- **Columnas (en orden):** texto, fecha de creación, fecha de última modificación
- **Ordenación por defecto:** por fecha de creación, descendente (los más recientes primero)
- **Búsqueda / filtros:** sí, por texto
- **Al pulsar una fila abre:** el formulario de smoke test

### Botones

- **Nuevo** (barra superior) — Abre el formulario de alta de un registro de smoke test.
- **Eliminar** (fila) — Borra el registro seleccionado, previa confirmación.

## Vista: Formulario de smoke test

- **Tipo:** formulario
- **Qué muestra:** los datos de un registro de smoke test, en edición.
- **Se abre desde:** el listado de smoke tests, al pulsar una fila o «Nuevo».

### Propiedades

- **Modo:** editable tanto en el alta como en la modificación.

### Paneles

- **Smoke test** (normal) — texto, fecha de creación, fecha de última modificación

### Botones

- **Guardar** — Guarda el registro (alta o modificación).

### Reglas de UI

- RUI-001 — Las fechas de creación y de última modificación se muestran como solo lectura, porque las fija el servidor y el Administrador no las edita.
  - disparador: continuo
  - condición: Siempre