# Pantalla: Niveles

**Pantalla existente:** sí

## Identidad

- **Quién la usa:** Administrador, en edición.
- **Qué muestra:** el catálogo de niveles y, al entrar en uno, sus datos. El delta añade el grado al que pertenece el nivel, tanto en el listado como en el formulario.

## Menú

- Sistema educativo → Niveles — lo ve el Administrador; lleva a esta pantalla.

## Estructura jerárquica de las vistas

```
Listado de niveles
└── Formulario de nivel   (se abre al pulsar una fila o con «Añadir un nuevo nivel»)
```

## Vista: Listado de niveles

- **Slug:** listado
- **Tipo:** listado
- **Qué muestra:** los niveles del catálogo, en lectura.
- **Se abre desde:** es la vista de entrada de la pantalla.

### Propiedades

- **Columnas (en orden):** código, nombre, grado — y detrás de ellas, las demás columnas que el listado ya muestre

## Vista: Formulario de nivel

- **Slug:** formulario
- **Tipo:** formulario
- **Qué muestra:** los datos del nivel, en edición.
- **Se abre desde:** el listado de niveles, al pulsar una fila o «Añadir un nuevo nivel».

### Paneles

- **Nivel** (normal) — pasa a incluir, además de lo que ya tenía, el grado al que pertenece el nivel

### Reglas de UI

- RUI-niveles-formulario-001 — El campo Grado se señala como obligatorio, para que el administrador lo vea antes de guardar y no se entere solo al recibir el error
  - disparador: continuo
  - condición: Siempre
- RUI-niveles-formulario-002 — El selector del campo Grado ofrece todos los grados del catálogo, incluidos los que todavía no tienen ningún nivel
  - disparador: continuo
  - condición: Siempre
