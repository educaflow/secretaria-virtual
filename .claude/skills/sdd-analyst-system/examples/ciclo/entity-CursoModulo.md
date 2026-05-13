# Entidad: CursoModulo

Entidad puente que asocia un Curso con un Módulo del catálogo.

## Modelo de datos

| Campo  | Tipo de dato | Relación               | Notas                        |
|--------|--------------|------------------------|------------------------------|
| curso  | relación     | → Curso (padre)        | asignado por el sistema      |
| modulo | relación     | → Modulo (catálogo)    | seleccionado por el usuario  |

## Validaciones (V-XXX)

*(no hay validaciones específicas de CursoModulo)*

## Acciones

| Operación          | Cuándo se permite                              | Validaciones que aplican | Reglas que dispara |
|--------------------|------------------------------------------------|--------------------------|--------------------|
| Crear (insert)     | Solo dentro del contexto de un curso concreto  | —                        | —                  |
| Modificar (update) | Siempre                                        | —                        | —                  |
| Borrar (remove)    | Siempre                                        | —                        | —                  |

## Reglas de negocio (R-XXX)

*(no hay reglas de negocio asociadas a CursoModulo)*
