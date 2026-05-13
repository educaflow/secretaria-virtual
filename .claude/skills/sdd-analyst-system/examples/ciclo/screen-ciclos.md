# Pantalla: "Ciclos"

## Identidad

- **Quién la usa:** *(pendiente — no se infiere del XML)*.
- **Qué muestra:** todos los ciclos del sistema, con sus cursos y los módulos de cada curso navegables como paneles anidados.

## Menú

| Propiedad        | Valor                                |
|------------------|--------------------------------------|
| Ruta jerárquica  | *(pendiente)* → "Ciclos"             |
| Título visible   | "Ciclos"                             |
| Quién lo ve      | *(pendiente)*                        |

---

## Grid 1 — "Ciclos"

| Propiedad                          | Valor                                                  |
|------------------------------------|--------------------------------------------------------|
| Columnas (en orden)                | código, nombre, familia profesional                    |
| Ordenación por defecto             | nombre ascendente                                      |
| ¿Permite buscar?                   | SÍ — búsqueda por cualquiera de los campos visibles    |
| Formulario que abre el onclick     | Formulario 1 — Ciclo (en modo edición)                 |
| Botones del toolbar                | "Añadir un nuevo ciclo"                                |
| Botones de las columnas            | —                                                      |

### Botones

| Botón                  | Qué hace                                                |
|------------------------|---------------------------------------------------------|
| "Añadir un nuevo ciclo"| Abre el formulario para crear un nuevo ciclo            |

## Formulario 1 — Ciclo

### Paneles

| Panel (título) | Tipo                            | Campos                                                  |
|----------------|---------------------------------|---------------------------------------------------------|
| "Ciclo"        | normal                          | código, nombre, familia profesional, grado, nivel       |
| "Cursos"       | anidado → Grid 2 ("Cursos")     | —                                                       |
| (sin título)   | botones                         | botón "Borrar", botón "Cancelar", botón "Guardar"       |

### Botones

| Botón       | Qué hace                                                                                          |
|-------------|---------------------------------------------------------------------------------------------------|
| "Borrar"    | Ejecuta la operación "Borrar (remove)" sobre el ciclo → vuelve a la pantalla anterior             |
| "Cancelar"  | Descarta los cambios y vuelve a la pantalla anterior                                              |
| "Guardar"   | Valida V-001 → ejecuta la operación "Crear (insert)" o "Modificar (update)" según corresponda    |

### Reglas de UI (U-XXX)

| ID    | Disparador | Efecto           | Campo/Panel afectado | Condición                                  |
|-------|------------|------------------|----------------------|--------------------------------------------|
| U-001 | continuo   | Mostrar/ocultar  | campo "nivel"        | Visible solo si grado = "D"                |
| U-002 | continuo   | Filtrar dominio  | campo "grado"        | Mostrar solo grados con código "D" o "E"   |
| U-003 | continuo   | Filtrar dominio  | campo "nivel"        | Mostrar solo niveles con código "D" o "E"  |
| U-004 | continuo   | Mostrar/ocultar  | botón "Borrar"       | Visible solo si el registro ya existe      |

---

## Grid 2 — "Cursos"

| Propiedad                          | Valor                                                                |
|------------------------------------|----------------------------------------------------------------------|
| Columnas (en orden)                | código, nombre, ley educativa                                        |
| Ordenación por defecto             | nombre ascendente                                                    |
| ¿Permite buscar?                   | SÍ — búsqueda por cualquiera de los campos visibles                  |
| Formulario que abre el onclick     | Formulario 2 — Curso (en modo edición, como ventana modal)           |
| Botones del toolbar                | "Añadir un nuevo curso"                                              |
| Botones de las columnas            | —                                                                    |

### Botones

| Botón                  | Qué hace                                                |
|------------------------|---------------------------------------------------------|
| "Añadir un nuevo curso"| Abre el formulario para crear un nuevo curso            |

## Formulario 2 — Curso

### Paneles

| Panel (título) | Tipo                            | Campos                                                  |
|----------------|---------------------------------|---------------------------------------------------------|
| (sin título)   | normal                          | código, nombre, ley educativa                           |
| "Módulos"      | anidado → Grid 3 ("Módulos")    | —                                                       |
| (sin título)   | botones                         | botón "Borrar", botón "Cancelar", botón "Guardar"       |

### Botones

| Botón       | Qué hace                                                                                          |
|-------------|---------------------------------------------------------------------------------------------------|
| "Borrar"    | Ejecuta la operación "Borrar (remove)" sobre el curso → cierra el modal                           |
| "Cancelar"  | Descarta los cambios y cierra el modal                                                            |
| "Guardar"   | Ejecuta la operación "Crear (insert)" o "Modificar (update)" según corresponda → cierra el modal  |

### Reglas de UI (U-XXX)

| ID    | Disparador | Efecto             | Campo/Panel afectado | Condición                                                 |
|-------|------------|--------------------|----------------------|-----------------------------------------------------------|
| U-005 | onNew      | Valor por defecto  | campo "ciclo"        | Al crear desde la pantalla anidada, fijar al ciclo padre  |
| U-006 | continuo   | Mostrar/ocultar    | botón "Borrar"       | Visible solo si el registro ya existe                     |

---

## Grid 3 — "Módulos"

| Propiedad                          | Valor                                                                |
|------------------------------------|----------------------------------------------------------------------|
| Columnas (en orden)                | módulo                                                               |
| Ordenación por defecto             | nombre del módulo ascendente                                         |
| ¿Permite buscar?                   | SÍ — búsqueda por el campo módulo                                    |
| Formulario que abre el onclick     | Formulario 3 — Módulo (en modo edición, como ventana modal)          |
| Botones del toolbar                | "Añadir un nuevo módulo"                                             |
| Botones de las columnas            | —                                                                    |

### Botones

| Botón                   | Qué hace                                                |
|-------------------------|---------------------------------------------------------|
| "Añadir un nuevo módulo"| Abre el formulario para crear un nuevo módulo           |

## Formulario 3 — Módulo (CursoModulo)

### Paneles

| Panel (título) | Tipo     | Campos                                              |
|----------------|----------|-----------------------------------------------------|
| (sin título)   | normal   | módulo                                              |
| (sin título)   | botones  | botón "Borrar", botón "Cancelar", botón "Guardar"   |

### Botones

| Botón       | Qué hace                                                                                          |
|-------------|---------------------------------------------------------------------------------------------------|
| "Borrar"    | Ejecuta la operación "Borrar (remove)" sobre el módulo → cierra el modal                          |
| "Cancelar"  | Descarta los cambios y cierra el modal                                                            |
| "Guardar"   | Ejecuta la operación "Crear (insert)" o "Modificar (update)" según corresponda → cierra el modal  |

### Reglas de UI (U-XXX)

| ID    | Disparador | Efecto             | Campo/Panel afectado | Condición                                                 |
|-------|------------|--------------------|----------------------|-----------------------------------------------------------|
| U-007 | onNew      | Valor por defecto  | campo "curso"        | Al crear desde la pantalla anidada, fijar al curso padre  |
| U-008 | continuo   | Mostrar/ocultar    | botón "Borrar"       | Visible solo si el registro ya existe                     |
