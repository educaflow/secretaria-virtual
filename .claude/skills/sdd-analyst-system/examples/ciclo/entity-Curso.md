# Entidad: Curso

## Modelo de datos

| Campo        | Tipo de dato | Relación                              | Origen del valor | Notas                            |
|--------------|--------------|---------------------------------------|------------------|----------------------------------|
| code         | texto        | —                                     | cliente          | código del curso                 |
| name         | texto        | —                                     | cliente          | nombre legible                   |
| leyEducativa | relación     | → LeyEducativa                        | cliente          |                                  |
| ciclo        | relación     | → Ciclo (padre)                       | servidor         | asignado por el sistema          |
| modulos      | lista        | → CursoModulo (uno a varios, hijos)   | cliente          | se mantiene desde la pantalla    |

## Validaciones (V-Curso-NNN)

*(no hay validaciones específicas de Curso)*

## Acciones

| Operación          | Cuándo se permite                              | Validaciones que aplican | Reglas que dispara |
|--------------------|------------------------------------------------|--------------------------|--------------------|
| Crear (insert)     | Solo dentro del contexto de un ciclo concreto  | —                        | R-Curso-001        |
| Modificar (update) | Siempre                                        | —                        | —                  |
| Borrar (remove)    | Siempre                                        | —                        | —                  |

## Reglas de negocio (R-Curso-NNN)

| ID          | Descripción                                          | Entidad | Método | Momento | Más información                                                      | Origen spec |
|-------------|------------------------------------------------------|---------|--------|---------|----------------------------------------------------------------------|-------------|
| R-Curso-001 | Asigna el ciclo padre al curso que se está creando   | Curso   | insert | Antes   | El curso siempre se crea desde la pantalla de un ciclo concreto; el usuario nunca elige el ciclo | —           |
