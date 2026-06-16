# Pantalla: Mis notas (alumno)

## Identidad

- **Quién la usa:** el Alumno, en solo lectura, únicamente sobre sus propios grupos y notas.
- **Qué muestra:** el listado de los grupos a los que pertenece el alumno con su nota media en cada uno y, al entrar en un grupo, sus notas por módulo.

## Menú

- Mis notas — lo ve el Alumno; lleva a esta pantalla.

## Estructura jerárquica de las vistas

```
Listado de mis grupos
└── Formulario de mi grupo   (se abre al pulsar una fila)
    └── Listado de mis notas   (panel maestro-detalle «Mis notas» del formulario de mi grupo)
        └── Formulario de mi nota   (se abre al pulsar una fila del listado de mis notas)
```

---

## Vista: Listado de mis grupos

- **Tipo:** listado
- **Qué muestra:** los grupos a los que pertenece el alumno conectado, con su nota media en cada uno, en solo lectura.
- **Se abre desde:** es la vista de entrada de la pantalla.

### Propiedades

- **Columnas (en orden):** curso académico, ciclo, nombre, nota media
- **Ordenación por defecto:** por curso académico, descendente
- **Búsqueda / filtros:** no
- **Al pulsar una fila abre:** el formulario de mi grupo

### Botones

*(sin botones)*

(Sin reglas de UI propias: que el alumno solo vea sus propios grupos es alcance de rol y vive en Seguridad.)

---

## Vista: Formulario de mi grupo

- **Tipo:** formulario
- **Qué muestra:** los datos del grupo del alumno, su nota media y sus notas por módulo, en solo lectura.
- **Se abre desde:** el listado de mis grupos, al pulsar una fila.

### Propiedades

- **Modo:** solo lectura.

### Paneles

- **Mi grupo** (normal) — nombre, curso, curso académico, nota media
- **Mis notas** (maestro-detalle → «Listado de mis notas») — la nota del alumno en cada módulo del grupo

### Botones

*(sin botones)*

---

## Vista: Listado de mis notas

- **Tipo:** listado
- **Qué muestra:** la nota del alumno en cada módulo del grupo, en solo lectura.
- **Se abre desde:** embebido como panel «Mis notas» en el formulario de mi grupo.

### Propiedades

- **Columnas (en orden):** módulo, valor, fecha de calificación
- **Ordenación por defecto:** por nombre del módulo, ascendente
- **Búsqueda / filtros:** no
- **Al pulsar una fila abre:** el formulario de mi nota

### Botones

*(sin botones)*

---

## Vista: Formulario de mi nota

- **Tipo:** formulario
- **Qué muestra:** la nota del alumno en un módulo, en solo lectura.
- **Se abre desde:** el listado de mis notas, al pulsar una fila.

### Propiedades

- **Modo:** solo lectura.

### Paneles

- **Mi nota** (normal) — módulo, valor, fecha de calificación, fecha de última modificación

### Botones

*(sin botones)*
