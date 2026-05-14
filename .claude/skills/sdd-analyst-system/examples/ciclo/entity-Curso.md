# Entidad: Curso

## Modelo de datos

| Campo        | Tipo de dato | Relación                              | Notas                            |
|--------------|--------------|---------------------------------------|----------------------------------|
| code         | texto        | —                                     | código del curso                 |
| name         | texto        | —                                     | nombre legible                   |
| leyEducativa | relación     | → LeyEducativa                        |                                  |
| ciclo        | relación     | → Ciclo (padre)                       | asignado por el sistema          |
| modulos      | lista        | → CursoModulo (uno a varios, hijos)   | se mantiene desde la pantalla    |

## Validaciones (V-XXX)

*(no hay validaciones específicas de Curso)*

## Acciones

| Operación          | Cuándo se permite                              | Validaciones que aplican | Reglas que dispara |
|--------------------|------------------------------------------------|--------------------------|--------------------|
| Crear (insert)     | Solo dentro del contexto de un ciclo concreto  | —                        | —                  |
| Modificar (update) | Siempre                                        | —                        | —                  |
| Borrar (remove)    | Siempre                                        | —                        | —                  |

## Reglas de negocio (R-XXX)

*(no hay reglas de negocio asociadas a Curso)*
