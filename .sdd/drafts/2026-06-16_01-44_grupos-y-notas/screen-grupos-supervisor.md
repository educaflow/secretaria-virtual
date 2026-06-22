# Pantalla: Grupos (supervisor)

## Identidad

- **Quién la usa:** el Supervisor, en edición, sobre los grupos de su propio centro.
- **Qué muestra:** el listado de los grupos del centro del supervisor y, al entrar en uno, su detalle con los módulos del grupo y los alumnos con su nota media; bajando por los módulos se llega a la nota de cada alumno.

## Menú

- Notas → Grupos — lo ve el Supervisor; lleva a esta pantalla.

## Estructura jerárquica de las vistas

```
Listado de grupos
└── Formulario de grupo   (se abre al pulsar una fila o con «Nuevo grupo»)
    ├── Listado de módulos del grupo   (panel maestro-detalle «Módulos» del formulario de grupo)
    │   └── Formulario de módulo del grupo   (se abre al pulsar una fila del listado de módulos)
    │       └── Listado de alumnos del módulo   (panel maestro-detalle «Alumnos» del formulario de módulo: las notas de ese módulo)
    │           └── Formulario de nota   (se abre al pulsar una fila del listado de alumnos del módulo)
    └── Listado de alumnos del grupo   (panel maestro-detalle «Alumnos» del formulario de grupo)
        └── Formulario de alumno del grupo   (se abre al pulsar una fila o con «Añadir alumno»)
```

---

## Vista: Listado de grupos

- **Tipo:** listado
- **Qué muestra:** los grupos del centro del supervisor, en lectura.
- **Se abre desde:** es la vista de entrada de la pantalla.

### Propiedades

- **Columnas (en orden):** curso académico, ciclo, nombre, estado
- **Ordenación por defecto:** por nombre, ascendente
- **Búsqueda / filtros:** sí, por estado
- **Al pulsar una fila abre:** el formulario de grupo

### Botones

- **Nuevo grupo** (barra superior) — Abre el formulario de alta de un grupo.

---

## Vista: Formulario de grupo

- **Tipo:** formulario
- **Qué muestra:** los datos del grupo, sus módulos y sus alumnos con la nota media.
- **Se abre desde:** el listado de grupos, al pulsar una fila o «Nuevo grupo».

### Propiedades

- **Modo:** editable mientras el grupo está ABIERTO; en cuanto está CERRADO, todo en solo lectura.

### Paneles

- **Datos del grupo** (normal) — nombre, curso, centro, curso académico, estado, fecha de cierre
- **Módulos** (maestro-detalle → «Listado de módulos del grupo») — los módulos del grupo
- **Alumnos** (maestro-detalle → «Listado de alumnos del grupo») — los alumnos del grupo con su nota media

### Botones

- **Guardar** — Guarda el grupo.
- **Cerrar grupo** — Cierra el grupo; pasa a CERRADO y registra la fecha de cierre. Visible solo si está ABIERTO.

### Reglas de UI

- RUI-001 — Al crear un grupo, el centro se rellena con el centro del usuario y queda en solo lectura
  - disparador: al crear
  - condición: Siempre
- RUI-002 — Al crear un grupo, el curso académico se rellena con el del centro del usuario y queda en solo lectura
  - disparador: al crear
  - condición: Siempre
- RUI-003 — El botón «Cerrar grupo» solo se muestra cuando el grupo está ABIERTO
  - disparador: continuo
  - condición: estado == ABIERTO
- RUI-004 — Cuando el grupo está CERRADO, el formulario y sus paneles quedan en solo lectura
  - disparador: continuo
  - condición: estado == CERRADO

---

## Vista: Listado de módulos del grupo

- **Tipo:** listado
- **Qué muestra:** los módulos del grupo, en lectura.
- **Se abre desde:** embebido como panel «Módulos» en el formulario de grupo.

### Propiedades

- **Columnas (en orden):** módulo
- **Ordenación por defecto:** por nombre del módulo, ascendente
- **Búsqueda / filtros:** no
- **Al pulsar una fila abre:** el formulario de módulo del grupo

### Botones

*(sin botones)*

---

## Vista: Formulario de módulo del grupo

- **Tipo:** formulario
- **Qué muestra:** el módulo del grupo y la lista de alumnos con su nota en ese módulo.
- **Se abre desde:** el listado de módulos del grupo, al pulsar una fila.

### Propiedades

- **Modo:** solo lectura (el módulo no se edita; las notas se ponen entrando en cada alumno).

### Paneles

- **Módulo** (normal) — módulo
- **Alumnos** (maestro-detalle → «Listado de alumnos del módulo») — la nota de cada alumno del grupo en este módulo

### Botones

*(sin botones)*

---

## Vista: Listado de alumnos del módulo

- **Tipo:** listado
- **Qué muestra:** la nota de cada alumno del grupo en este módulo, en lectura.
- **Se abre desde:** embebido como panel «Alumnos» en el formulario de módulo del grupo.

### Propiedades

- **Columnas (en orden):** alumno, valor, fecha de calificación, fecha de última modificación
- **Ordenación por defecto:** por alumno, ascendente
- **Búsqueda / filtros:** no
- **Al pulsar una fila abre:** el formulario de nota

### Botones

*(sin botones)*

---

## Vista: Formulario de nota

- **Tipo:** formulario
- **Qué muestra:** la nota de un alumno en un módulo.
- **Se abre desde:** el listado de alumnos del módulo, al pulsar una fila.

### Propiedades

- **Modo:** el valor es editable mientras el grupo está ABIERTO; el resto es solo lectura.

### Paneles

- **Nota** (normal) — alumno, módulo, valor, fecha de calificación, fecha de última modificación

### Botones

- **Guardar** — Guarda la nota.

### Reglas de UI

- RUI-005 — El valor de la nota solo es editable cuando el grupo está ABIERTO
  - disparador: continuo
  - condición: estado del grupo == ABIERTO

---

## Vista: Listado de alumnos del grupo

- **Tipo:** listado
- **Qué muestra:** los alumnos del grupo con su nota media, en lectura.
- **Se abre desde:** embebido como panel «Alumnos» en el formulario de grupo.

### Propiedades

- **Columnas (en orden):** alumno, nota media
- **Ordenación por defecto:** por alumno, ascendente
- **Búsqueda / filtros:** no
- **Al pulsar una fila abre:** el formulario de alumno del grupo

### Botones

- **Añadir alumno** (barra superior) — Abre el formulario para añadir un alumno al grupo. Visible solo si el grupo está ABIERTO.

---

## Vista: Formulario de alumno del grupo

- **Tipo:** formulario
- **Qué muestra:** el alumno y su nota media en el grupo.
- **Se abre desde:** el listado de alumnos del grupo, al pulsar una fila o «Añadir alumno».

### Propiedades

- **Modo:** al añadir, se elige el alumno; en detalle, solo lectura.

### Paneles

- **Alumno** (normal) — alumno (se elige entre los usuarios de tipo Alumno del centro del grupo), nota media

### Botones

- **Guardar** — Añade el alumno al grupo.
