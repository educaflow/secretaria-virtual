# Pantalla: Tipos de expediente

## Identidad

- **Quién la usa:** Administrador, en solo lectura.
- **Qué muestra:** el listado de todos los tipos de expediente dados de alta en la aplicación y, al entrar en uno, su detalle. Sobre el modelo TipoExpediente, sin filtro por centro (los tipos de expediente no pertenecen a ningún centro) y siempre en lectura: no se puede crear, modificar ni borrar ninguno. Sustituye a la pantalla actual del mismo nombre.

## Menú

- Expedientes → Tipos Expedientes — lo ve solo el Administrador; lleva a esta pantalla.

## Estructura jerárquica de las vistas

```
Listado de tipos de expediente
└── Formulario de tipo de expediente   (se abre al pulsar una fila, en solo lectura)
```

---

## Vista: Listado de tipos de expediente

- **Slug:** listado
- **Tipo:** listado
- **Qué muestra:** todos los tipos de expediente, en lectura.
- **Se abre desde:** es la vista de entrada de la pantalla.

### Propiedades

- **Columnas (en orden):** código, nombre, trámite
- **Ordenación por defecto:** sin orden definido
- **Búsqueda / filtros:** no
- **Al pulsar una fila abre:** el formulario de tipo de expediente, en solo lectura

### Botones

*(sin botones)*

## Vista: Formulario de tipo de expediente

- **Slug:** formulario
- **Tipo:** formulario
- **Qué muestra:** los datos de un tipo de expediente, en lectura.
- **Se abre desde:** el listado de tipos de expediente, al pulsar una fila.

### Propiedades

- **Modo:** siempre de solo lectura.

### Paneles

- **Datos** (normal) — código, nombre, trámite

### Botones

- Desviación del estándar: formulario de solo lectura, sin Guardar ni Borrar, y sin posibilidad de crear uno nuevo desde él.
