# Pantalla: "Todos los documentos firmados"

## Identidad

- **Quién la usa:** Administrador.
- **Qué muestra:** todas las tareas de firma del sistema en cualquier estado, con los documentos asociados, en modo consulta.

## Menú

| Propiedad        | Valor                                                  |
|------------------|--------------------------------------------------------|
| Ruta jerárquica  | *(pendiente)* → "Todos los documentos firmados"        |
| Título visible   | "Todos los documentos firmados"                        |
| Quién lo ve      | Administrador                                          |

---

## Grid 1 — "Firmas"

| Propiedad                          | Valor                                                                          |
|------------------------------------|--------------------------------------------------------------------------------|
| Columnas (en orden)                | fecha de solicitud, fecha de resolución, firmante, motivo de la firma, estado  |
| Ordenación por defecto             | fecha de solicitud ascendente                                                  |
| ¿Permite buscar?                   | SÍ — búsqueda por cualquiera de los campos visibles                            |
| Formulario que abre el onclick     | Formulario 1 — Tarea de firma (en modo solo lectura)                           |
| Botones del toolbar                | —                                                                              |
| Botones de las columnas            | —                                                                              |

## Formulario 1 — Tarea de firma

### Paneles

| Panel (título)                  | Tipo                              | Campos                                                                                  |
|---------------------------------|-----------------------------------|-----------------------------------------------------------------------------------------|
| "Estado de la tarea de firma"   | normal                            | motivo de la firma, fecha de solicitud, estado, fecha de resolución, motivo de rechazo  |
| "Documentos"                    | anidado → Grid 2 ("Documentos")   | —                                                                                       |
| (sin título)                    | botones                           | botón "Salir"                                                                           |

### Botones

| Botón     | Qué hace                                  |
|-----------|-------------------------------------------|
| "Salir"   | Cierra el formulario y vuelve al grid     |

### Reglas de UI (U-XXX)

| ID    | Disparador | Efecto           | Campo/Panel afectado          | Condición                                  |
|-------|------------|------------------|-------------------------------|--------------------------------------------|
| U-001 | continuo   | Mostrar/ocultar  | campo "fecha de resolución"   | Visible solo si la tarea ya está resuelta  |
| U-002 | continuo   | Mostrar/ocultar  | campo "motivo de rechazo"     | Visible solo si la tarea ha sido rechazada |

---

## Grid 2 — "Documentos"

| Propiedad                          | Valor                                                                |
|------------------------------------|----------------------------------------------------------------------|
| Columnas (en orden)                | documento original (nombre del fichero), documento firmado (nombre)  |
| Ordenación por defecto             | —                                                                    |
| ¿Permite buscar?                   | NO                                                                   |
| Formulario que abre el onclick     | Formulario 2 — Documento (en modo solo lectura)                      |
| Botones del toolbar                | —                                                                    |
| Botones de las columnas            | —                                                                    |

## Formulario 2 — Documento

### Paneles

| Panel (título)         | Tipo     | Campos                                          |
|------------------------|----------|-------------------------------------------------|
| "Documento original"   | pestaña  | visor PDF del documento original (incrustado)   |
| "Documento firmado"    | pestaña  | visor PDF del documento firmado (incrustado)    |

*(sin botones)*

### Reglas de UI (U-XXX)

| ID    | Disparador | Efecto           | Campo/Panel afectado          | Condición                                |
|-------|------------|------------------|-------------------------------|------------------------------------------|
| U-003 | continuo   | Mostrar/ocultar  | pestaña "Documento firmado"   | Visible solo si existe documento firmado |
