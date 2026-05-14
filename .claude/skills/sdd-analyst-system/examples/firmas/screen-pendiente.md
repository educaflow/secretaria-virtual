# Pantalla: "Documentos pendientes de firma"

## Identidad

- **Quién la usa:** cualquier usuario que tenga tareas pendientes de firma (firmante).
- **Qué muestra:** las tareas de firma del usuario actual cuyo estado es PENDIENTE. Permite firmar o rechazar la firma de los documentos a través de un asistente con tres pasos.

## Menú

| Propiedad        | Valor                                       |
|------------------|---------------------------------------------|
| Ruta jerárquica  | *(pendiente)* → "Pendientes de firma"       |
| Título visible   | "Documentos pendientes de firma"            |
| Quién lo ve      | Todo usuario con tareas asignadas           |

---

## Grid 1 — "Firmas"

| Propiedad                          | Valor                                                              |
|------------------------------------|--------------------------------------------------------------------|
| Columnas (en orden)                | fecha de solicitud, firmante, motivo de la firma, estado           |
| Ordenación por defecto             | fecha de solicitud ascendente                                      |
| ¿Permite buscar?                   | NO                                                                 |
| Formulario que abre el onclick     | Formulario 1 — Tarea pendiente (en modo edición)                   |
| Botones del toolbar                | —                                                                  |
| Botones de las columnas            | —                                                                  |

## Formulario 1 — Tarea pendiente

### Paneles

| Panel (título)                  | Tipo                                       | Campos                                                                                                          |
|---------------------------------|--------------------------------------------|-----------------------------------------------------------------------------------------------------------------|
| "Estado de la tarea a firmar"   | normal (siempre solo lectura)              | motivo de la firma, fecha de solicitud, estado, fecha de resolución                                             |
| "Documentos a firmar"           | anidado → Grid 2 ("Documentos a firmar")   | —                                                                                                               |
| "Resolver"                      | normal (asistente — paso 1)                | botón "Rechazar firmar", botón "Firmar todos los documentos"                                                    |
| "Rechazar firmar el documento"  | normal (asistente — paso 2 rechazo)        | motivo de rechazo, botón "Atrás", botón "Finalizar"                                                             |
| "Firmar el documento"           | normal (asistente — paso 2 firma)          | ayuda informativa sobre AutoFirma, botón "Atrás", botón "Firmar todos los documentos con AutoFirma y finalizar" |

### Botones

| Botón                                                   | Qué hace                                                                                                                                                                |
|---------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| "Rechazar firmar"                                       | Cambia el asistente al paso "rechazar"                                                                                                                                  |
| "Firmar todos los documentos"                           | Cambia el asistente al paso "firmar"                                                                                                                                    |
| "Atrás" (en panel "Rechazar firmar el documento")       | Vuelve el asistente al paso "Resolver" (paso 1)                                                                                                                         |
| "Finalizar"                                             | Valida V-006 → ejecuta la operación "Marcar como rechazada" (R-001, R-002) → cierra el formulario                                                                       |
| "Atrás" (en panel "Firmar el documento")                | Vuelve el asistente al paso "Resolver" (paso 1)                                                                                                                         |
| "Firmar todos los documentos con AutoFirma y finalizar" | Ejecuta la operación "Firmar documentos con AutoFirma" (R-005) → valida V-007 → ejecuta la operación "Marcar como firmada" (R-003, R-004) → cierra el formulario        |

### Reglas de UI (U-XXX)

| ID    | Disparador | Efecto                       | Campo/Panel afectado                  | Condición                                                  |
|-------|------------|------------------------------|---------------------------------------|------------------------------------------------------------|
| U-004 | continuo   | Mostrar/ocultar              | campo "fecha de resolución"           | Visible solo si la tarea ya está resuelta                  |
| U-005 | onLoad     | Iniciar paso del asistente   | asistente del formulario              | Al abrir, posicionarse en el paso "Resolver" (paso 1)      |
| U-006 | continuo   | Mostrar/ocultar              | panel "Resolver"                      | Visible solo cuando el asistente está en el paso 1         |
| U-007 | continuo   | Mostrar/ocultar              | panel "Rechazar firmar el documento"  | Visible solo cuando el asistente está en el paso "rechazar"|
| U-008 | continuo   | Mostrar/ocultar              | panel "Firmar el documento"           | Visible solo cuando el asistente está en el paso "firmar"  |

---

## Grid 2 — "Documentos a firmar"

| Propiedad                          | Valor                                                              |
|------------------------------------|--------------------------------------------------------------------|
| Columnas (en orden)                | documento original (nombre del fichero)                            |
| Ordenación por defecto             | —                                                                  |
| ¿Permite buscar?                   | NO                                                                 |
| Formulario que abre el onclick     | Formulario 2 — Documento (en modo solo lectura)                    |
| Botones del toolbar                | —                                                                  |
| Botones de las columnas            | —                                                                  |

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
| U-013 | continuo   | Mostrar/ocultar  | pestaña "Documento firmado"   | Visible solo si existe documento firmado |
