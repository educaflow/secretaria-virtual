# Entidad: Ciclo

## Modelo de datos

| Campo                | Tipo de dato | Relación                          | Origen del valor | Notas                            |
|----------------------|--------------|-----------------------------------|------------------|----------------------------------|
| code                 | texto        | —                                 | cliente          | código identificador del ciclo   |
| name                 | texto        | —                                 | cliente          | nombre legible                   |
| familiaProfesional   | relación     | → FamiliaProfesional              | cliente          |                                  |
| grado                | relación     | → Grado                           | cliente          | dominio limitado a "D" o "E"     |
| nivel                | relación     | → Nivel                           | cliente          | dominio limitado a "D" o "E"     |
| cursos               | lista        | → Curso (uno a varios, hijos)     | cliente          | se mantiene desde la pantalla    |

## Validaciones (V-Ciclo-NNN)

| ID    | Campo(s) | Descripción                                | Condición            | Mensaje al usuario                                | Origen spec |
|-------|----------|--------------------------------------------|----------------------|---------------------------------------------------|-------------|
| V-Ciclo-001 | nivel    | El nivel debe estar relleno                | Solo si grado = "D"  | "El nivel es obligatorio cuando el grado es 'D'." | —           |

## Acciones

| Operación          | Cuándo se permite | Validaciones que aplican | Reglas que dispara |
|--------------------|-------------------|--------------------------|--------------------|
| Crear (insert)     | Siempre           | V-Ciclo-001                    | —                  |
| Modificar (update) | Siempre           | V-Ciclo-001                    | —                  |
| Borrar (remove)    | Siempre           | —                        | —                  |

## Reglas de negocio (R-Ciclo-NNN)

*(no hay reglas de negocio asociadas a Ciclo)*
