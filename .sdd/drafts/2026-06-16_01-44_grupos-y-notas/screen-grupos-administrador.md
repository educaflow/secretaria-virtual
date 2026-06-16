# Pantalla: Grupos (administración)

## Identidad

- **Quién la usa:** el Administrador, en edición, sobre los grupos de cualquier centro.
- **Qué muestra:** el listado de los grupos de todos los centros y, al entrar en uno, su detalle con los módulos del grupo y los alumnos con su nota media; bajando por los módulos se llega a la nota de cada alumno. Es una pantalla independiente de la del supervisor: añade la columna de centro, permite elegir el centro y el curso académico al crear, y permite reabrir un grupo cerrado.

## Menú

- Administración → Grupos (administración) — lo ve el Administrador; lleva a esta pantalla.

## Estructura jerárquica de las vistas

```
Listado de grupos (administración)
└── Formulario de grupo (administración)   (se abre al pulsar una fila o con «Nuevo grupo»)
    ├── Listado de módulos del grupo (administración)   (panel maestro-detalle «Módulos» del formulario de grupo)
    │   └── Formulario de módulo del grupo (administración)   (se abre al pulsar una fila del listado de módulos)
    │       └── Listado de alumnos del módulo (administración)   (panel maestro-detalle «Alumnos» del formulario de módulo: las notas de ese módulo)
    │           └── Formulario de nota (administración)   (se abre al pulsar una fila del listado de alumnos del módulo)
    └── Listado de alumnos del grupo (administración)   (panel maestro-detalle «Alumnos» del formulario de grupo)
        └── Formulario de alumno del grupo (administración)   (se abre al pulsar una fila o con «Añadir alumno»)
```

---

## Vista: Listado de grupos (administración)

- **Tipo:** listado
- **Qué muestra:** los grupos de todos los centros, en lectura.
- **Se abre desde:** es la vista de entrada de la pantalla.

### Propiedades

- **Columnas (en orden):** centro, curso académico, ciclo, nombre, estado
- **Ordenación por defecto:** por centro y luego por nombre, ascendente
- **Búsqueda / filtros:** sí, por centro y por estado
- **Al pulsar una fila abre:** el formulario de grupo (administración)

### Botones

- **Nuevo grupo** (barra superior) — Abre el formulario de alta de un grupo.

---

## Vista: Formulario de grupo (administración)

- **Tipo:** formulario
- **Qué muestra:** los datos del grupo, sus módulos y sus alumnos con la nota media.
- **Se abre desde:** el listado de grupos (administración), al pulsar una fila o «Nuevo grupo».

### Propiedades

- **Modo:** editable mientras el grupo está ABIERTO; en cuanto está CERRADO, todo en solo lectura salvo el botón «Reabrir grupo».

### Paneles

- **Datos del grupo** (normal) — nombre, centro, curso académico, curso, estado, fecha de cierre
- **Módulos** (maestro-detalle → «Listado de módulos del grupo (administración)») — los módulos del grupo
- **Alumnos** (maestro-detalle → «Listado de alumnos del grupo (administración)») — los alumnos del grupo con su nota media

### Botones

- **Guardar** — Guarda el grupo.
- **Cerrar grupo** — Cierra el grupo; pasa a CERRADO y registra la fecha de cierre. Visible solo si está ABIERTO.
- **Reabrir grupo** — Reabre el grupo cerrado; pasa a ABIERTO y borra la fecha de cierre. Visible solo si está CERRADO.

### Reglas de UI

- RUI-006 — El administrador elige el centro y el curso académico al crear el grupo (ambos editables en el alta)
  - disparador: al crear
  - condición: Siempre
- RUI-007 — El botón «Cerrar grupo» solo se muestra cuando el grupo está ABIERTO
  - disparador: continuo
  - condición: estado == ABIERTO
- RUI-008 — El botón «Reabrir grupo» solo se muestra cuando el grupo está CERRADO
  - disparador: continuo
  - condición: estado == CERRADO
- RUI-009 — Cuando el grupo está CERRADO, el formulario y sus paneles quedan en solo lectura (salvo «Reabrir grupo»)
  - disparador: continuo
  - condición: estado == CERRADO

---

## Vista: Listado de módulos del grupo (administración)

- **Tipo:** listado
- **Qué muestra:** los módulos del grupo, en lectura.
- **Se abre desde:** embebido como panel «Módulos» en el formulario de grupo (administración).

### Propiedades

- **Columnas (en orden):** módulo
- **Ordenación por defecto:** por nombre del módulo, ascendente
- **Búsqueda / filtros:** no
- **Al pulsar una fila abre:** el formulario de módulo del grupo (administración)

### Botones

*(sin botones)*

---

## Vista: Formulario de módulo del grupo (administración)

- **Tipo:** formulario
- **Qué muestra:** el módulo del grupo y la lista de alumnos con su nota en ese módulo.
- **Se abre desde:** el listado de módulos del grupo (administración), al pulsar una fila.

### Propiedades

- **Modo:** solo lectura (el módulo no se edita; las notas se ponen entrando en cada alumno).

### Paneles

- **Módulo** (normal) — módulo
- **Alumnos** (maestro-detalle → «Listado de alumnos del módulo (administración)») — la nota de cada alumno del grupo en este módulo

### Botones

*(sin botones)*

---

## Vista: Listado de alumnos del módulo (administración)

- **Tipo:** listado
- **Qué muestra:** la nota de cada alumno del grupo en este módulo, en lectura.
- **Se abre desde:** embebido como panel «Alumnos» en el formulario de módulo del grupo (administración).

### Propiedades

- **Columnas (en orden):** alumno, valor, fecha de calificación, fecha de última modificación
- **Ordenación por defecto:** por alumno, ascendente
- **Búsqueda / filtros:** no
- **Al pulsar una fila abre:** el formulario de nota (administración)

### Botones

*(sin botones)*

---

## Vista: Formulario de nota (administración)

- **Tipo:** formulario
- **Qué muestra:** la nota de un alumno en un módulo.
- **Se abre desde:** el listado de alumnos del módulo (administración), al pulsar una fila.

### Propiedades

- **Modo:** el valor es editable mientras el grupo está ABIERTO; el resto es solo lectura.

### Paneles

- **Nota** (normal) — alumno, módulo, valor, fecha de calificación, fecha de última modificación

### Botones

- **Guardar** — Guarda la nota.

### Reglas de UI

- RUI-010 — El valor de la nota solo es editable cuando el grupo está ABIERTO
  - disparador: continuo
  - condición: estado del grupo == ABIERTO

---

## Vista: Listado de alumnos del grupo (administración)

- **Tipo:** listado
- **Qué muestra:** los alumnos del grupo con su nota media, en lectura.
- **Se abre desde:** embebido como panel «Alumnos» en el formulario de grupo (administración).

### Propiedades

- **Columnas (en orden):** alumno, nota media
- **Ordenación por defecto:** por alumno, ascendente
- **Búsqueda / filtros:** no
- **Al pulsar una fila abre:** el formulario de alumno del grupo (administración)

### Botones

- **Añadir alumno** (barra superior) — Abre el formulario para añadir un alumno al grupo. Visible solo si el grupo está ABIERTO.

---

## Vista: Formulario de alumno del grupo (administración)

- **Tipo:** formulario
- **Qué muestra:** el alumno y su nota media en el grupo.
- **Se abre desde:** el listado de alumnos del grupo (administración), al pulsar una fila o «Añadir alumno».

### Propiedades

- **Modo:** al añadir, se elige el alumno; en detalle, solo lectura.

### Paneles

- **Alumno** (normal) — alumno (se elige entre los usuarios de tipo Alumno del centro del grupo), nota media

### Botones

- **Guardar** — Añade el alumno al grupo.
