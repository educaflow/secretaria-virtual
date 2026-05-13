# Pantalla: "Documentos que se han rechazado firmar"

## Identidad

- **Quién la usa:** cualquier usuario firmante.
- **Qué muestra:** las tareas de firma del usuario actual cuyo estado es RECHAZADO, en modo consulta. Permite revisar qué se rechazó y el motivo introducido.

## Menú

| Propiedad        | Valor                                                  |
|------------------|--------------------------------------------------------|
| Ruta jerárquica  | *(pendiente)* → "Rechazados"                           |
| Título visible   | "Documentos que se han rechazado firmar"               |
| Quién lo ve      | Todo usuario firmante                                  |

---

## Grid 1 — "Firmas"

| Propiedad                          | Valor                                                                          |
|------------------------------------|--------------------------------------------------------------------------------|
| Columnas (en orden)                | fecha de solicitud, fecha de resolución, firmante, motivo de la firma, estado  |
| Ordenación por defecto             | fecha de solicitud ascendente                                                  |
| ¿Permite buscar?                   | NO                                                                             |
| Formulario que abre el onclick     | Formulario 1 — Tarea rechazada (en modo solo lectura)                          |
| Botones del toolbar                | — (las tareas las crean otros sistemas que solicitan la firma)                 |
| Botones de las columnas            | —                                                                              |

## Formulario 1 — Tarea rechazada

### Paneles

| Panel (título)                          | Tipo                              | Campos                                                                                  |
|-----------------------------------------|-----------------------------------|-----------------------------------------------------------------------------------------|
| "Estado de la tarea de firma rechazada" | normal (siempre solo lectura)     | motivo de la firma, fecha de solicitud, estado, fecha de resolución, motivo de rechazo  |
| "Documentos"                            | anidado → Grid 2 ("Documentos")   | —                                                                                       |
| (sin título)                            | botones                           | botón "Salir"                                                                           |

### Botones

| Botón     | Qué hace                                  |
|-----------|-------------------------------------------|
| "Salir"   | Cierra el formulario y vuelve al grid     |

### Reglas de UI (U-XXX)

*(no aplica)*

---

## Grid 2 — "Documentos"

| Propiedad                          | Valor                                                                |
|------------------------------------|----------------------------------------------------------------------|
| Columnas (en orden)                | documento original (nombre del fichero)                              |
| Ordenación por defecto             | —                                                                    |
| ¿Permite buscar?                   | NO                                                                   |
| Formulario que abre el onclick     | Formulario 2 — Documento original (en modo solo lectura)             |
| Botones del toolbar                | — (los documentos se crean junto con la tarea, no manualmente)       |
| Botones de las columnas            | —                                                                    |

## Formulario 2 — Documento original

### Paneles

| Panel (título)         | Tipo     | Campos                                          |
|------------------------|----------|-------------------------------------------------|
| "Documento original"   | pestaña  | visor PDF del documento original (incrustado)   |

*(sin botones)*

### Reglas de UI (U-XXX)

*(no aplica)*
