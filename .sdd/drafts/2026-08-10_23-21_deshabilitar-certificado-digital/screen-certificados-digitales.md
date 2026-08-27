# Pantalla: Certificados digitales

**Pantalla existente:** sí

Este fichero declara solo el **delta** de la pantalla existente: lo no mencionado se conserva tal cual.

## Identidad

- **Quién la usa:** el Administrador, en modo edición.
- **Qué muestra:** las entradas de certificados digitales de toda la aplicación. El cambio añade la casilla «Habilitado» al formulario y su columna al listado.

## Menú

- Administración SV → Certificados digitales — lo ve el Administrador; lleva a esta pantalla. (Menú existente, sin cambios.)

## Estructura jerárquica de las vistas

```
Listado de certificados digitales
└── Formulario de certificado digital  (se abre al pulsar una fila o con «Añadir certificado digital»)
```

## Vista: Listado de certificados digitales

- **Slug:** listado
- **Tipo:** listado
- **Qué muestra:** sin cambios; se añade una columna.

### Propiedades

- **Columnas (en orden):** se añade la columna «Habilitado» al final de las existentes.

## Vista: Formulario de certificado digital

- **Slug:** formulario
- **Tipo:** formulario
- **Qué muestra:** sin cambios; se añade un campo.

### Paneles

- **Certificado digital** (normal) — se añade la casilla «Habilitado» junto a los datos generales de la entrada (DNI y tipo de certificado).

### Reglas de UI

- RUI-certificados-digitales-formulario-001 — Al crear una entrada nueva, la casilla «Habilitado» aparece marcada
  - disparador: al crear
