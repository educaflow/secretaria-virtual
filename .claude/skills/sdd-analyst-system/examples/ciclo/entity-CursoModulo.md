# Entidad: CursoModulo

Entidad puente que asocia un Curso con un Módulo del catálogo.

## Modelo de datos

| Campo  | Tipo de dato | Relación               | Origen del valor | Notas                        |
|--------|--------------|------------------------|------------------|------------------------------|
| curso  | relación     | → Curso (padre)        | servidor         | asignado por el sistema      |
| modulo | relación     | → Modulo (catálogo)    | cliente          | seleccionado por el usuario  |

## Validaciones (V-CursoModulo-NNN)

*(no hay validaciones específicas de CursoModulo)*

## Acciones

| Operación          | Cuándo se permite                              | Validaciones que aplican | Reglas que dispara |
|--------------------|------------------------------------------------|--------------------------|--------------------|
| Crear (insert)     | Solo dentro del contexto de un curso concreto  | —                        | R-CursoModulo-001  |
| Modificar (update) | Siempre                                        | —                        | —                  |
| Borrar (remove)    | Siempre                                        | —                        | —                  |

## Reglas de negocio (R-CursoModulo-NNN)

| ID                | Descripción                                           | Entidad     | Método | Momento | Más información                                                       | Origen spec |
|-------------------|-------------------------------------------------------|-------------|--------|---------|-----------------------------------------------------------------------|-------------|
| R-CursoModulo-001 | Asigna el curso padre al módulo que se está creando   | CursoModulo | insert | Antes   | El módulo siempre se crea desde la pantalla de un curso concreto; el usuario nunca elige el curso | —           |
