# Pantalla: Ciclos

**Pantalla existente:** sí

## Identidad

- **Quién la usa:** Administrador, en edición.
- **Qué muestra:** el listado de ciclos y, al entrar en uno, su configuración con sus cursos y, dentro de cada curso, sus módulos. El delta afecta al listado (que pasa a mostrar el grado y el nivel) y al formulario del ciclo (que decide si pide el nivel).

## Menú

- Sistema educativo → Ciclos — lo ve el Administrador; lleva a esta pantalla.

## Estructura jerárquica de las vistas

```
Listado de ciclos
└── Formulario de ciclo   (se abre al pulsar una fila o con «Añadir un nuevo ciclo»)
    └── Listado de cursos   (panel maestro-detalle «Cursos» del formulario de ciclo)
        └── Formulario de curso   (se abre al pulsar una fila del listado de cursos o con «Añadir un nuevo curso»)
            └── Listado de módulos   (panel maestro-detalle «Módulos» del formulario de curso)
                └── Formulario de módulo   (se abre al pulsar una fila del listado de módulos o con «Añadir un nuevo módulo»)
```

Las vistas de cursos y de módulos no cambian; el delta solo alcanza al listado de ciclos y al formulario de ciclo.

## Vista: Listado de ciclos

- **Slug:** listado
- **Tipo:** listado
- **Qué muestra:** los ciclos, en lectura.
- **Se abre desde:** es la vista de entrada de la pantalla.

### Propiedades

- **Columnas (en orden):** código, nombre, familia profesional, grado, nivel

## Vista: Formulario de ciclo

- **Slug:** formulario
- **Tipo:** formulario
- **Qué muestra:** los datos del ciclo, en edición.
- **Se abre desde:** el listado de ciclos, al pulsar una fila o «Añadir un nuevo ciclo».

### Reglas de UI

- RUI-ciclos-formulario-001 — El campo Nivel solo se muestra cuando el grado elegido admite nivel
  - disparador: continuo
  - condición: el grado elegido admite nivel
- RUI-ciclos-formulario-002 — El campo Nivel se señala como obligatorio cuando el grado elegido admite nivel
  - disparador: continuo
  - condición: el grado elegido admite nivel
- RUI-ciclos-formulario-003 — Al cambiar el grado, el nivel que hubiera elegido se vacía, de modo que el usuario ve lo que va a quedar guardado
  - disparador: al cambiar grado
  - condición: Siempre
- RUI-ciclos-formulario-004 — El selector del campo Nivel ofrece únicamente los niveles del grado elegido
  - disparador: continuo
  - condición: Siempre
- RUI-ciclos-formulario-005 — El selector del campo Grado ofrece todos los grados del catálogo
  - disparador: continuo
  - condición: Siempre
