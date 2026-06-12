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

## Estructura jerarquica de las pantallas
```
TareaFirma
└── DocumentoFirma
```


## Grid 1 — "Firmas"

### Propiedades

| Propiedad                          | Valor                                                              |
|------------------------------------|--------------------------------------------------------------------|
| Entidad                            | TareaFirma                                                         |
| Columnas (en orden)                | fecha de solicitud, firmante, motivo de la firma, estado           |
| Ordenación por defecto             | fecha de solicitud ascendente                                      |
| ¿Permite buscar?                   | NO                                                                 |
| Formulario que abre el onclick     | Formulario 1 — Tarea pendiente (en modo edición)                   |
| Botones del toolbar                | — (las tareas las crean otros sistemas que solicitan la firma)     |
| Botones de las columnas            | —                                                                  |

## Formulario 1 — Tarea pendiente

### Propiedades

| Propiedad     | Valor                                                                                                          |
|---------------|----------------------------------------------------------------------------------------------------------------|
| Entidad       | TareaFirma                                                                                                     |
| Solo lectura  | no — los campos de estado son solo lectura, pero el formulario permite firmar/rechazar mediante el asistente   |

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
| "Finalizar"                                             | Valida V-TareaFirma-006 → ejecuta la operación "Marcar como rechazada" (R-TareaFirma-001, R-TareaFirma-002) → cierra el formulario                                                                       |
| "Atrás" (en panel "Firmar el documento")                | Vuelve el asistente al paso "Resolver" (paso 1)                                                                                                                         |
| "Firmar todos los documentos con AutoFirma y finalizar" | Ejecuta la operación "Firmar documentos con AutoFirma" (R-TareaFirma-005) → valida V-TareaFirma-007 → ejecuta la operación "Marcar como firmada" (R-TareaFirma-003, R-TareaFirma-004) → cierra el formulario        |

### Reglas de UI (U-pendiente-NNN)

| ID    | Disparador | Efecto                       | Campo/Panel afectado                  | Condición                                                  | Origen spec |
|-------|------------|------------------------------|---------------------------------------|------------------------------------------------------------|-------------|
| U-pendiente-001 | continuo   | Mostrar/ocultar              | campo "fecha de resolución"           | Visible solo si la tarea ya está resuelta                  | —           |
| U-pendiente-002 | onLoad     | Iniciar paso del asistente   | asistente del formulario              | Al abrir, posicionarse en el paso "Resolver" (paso 1)      | —           |
| U-pendiente-003 | continuo   | Mostrar/ocultar              | panel "Resolver"                      | Visible solo cuando el asistente está en el paso 1         | —           |
| U-pendiente-004 | continuo   | Mostrar/ocultar              | panel "Rechazar firmar el documento"  | Visible solo cuando el asistente está en el paso "rechazar"| —           |
| U-pendiente-005 | continuo   | Mostrar/ocultar              | panel "Firmar el documento"           | Visible solo cuando el asistente está en el paso "firmar"  | —           |

---

## Grid 2 — "Documentos a firmar"

### Propiedades

| Propiedad                          | Valor                                                              |
|------------------------------------|--------------------------------------------------------------------|
| Entidad                            | DocumentoFirma                                                     |
| Columnas (en orden)                | documento original (nombre del fichero)                            |
| Ordenación por defecto             | —                                                                  |
| ¿Permite buscar?                   | NO                                                                 |
| Formulario que abre el onclick     | Formulario 2 — Documento (en modo solo lectura)                    |
| Botones del toolbar                | — (los documentos se crean junto con la tarea, no manualmente)     |
| Botones de las columnas            | —                                                                  |

## Formulario 2 — Documento

### Propiedades

| Propiedad     | Valor           |
|---------------|-----------------|
| Entidad       | DocumentoFirma  |
| Solo lectura  | sí              |

### Paneles

| Panel (título)         | Tipo     | Campos                                          |
|------------------------|----------|-------------------------------------------------|
| "Documento original"   | pestaña  | visor PDF del documento original (incrustado)   |
| "Documento firmado"    | pestaña  | visor PDF del documento firmado (incrustado)    |

*(sin botones)*

### Reglas de UI (U-pendiente-NNN)

| ID    | Disparador | Efecto           | Campo/Panel afectado          | Condición                                | Origen spec |
|-------|------------|------------------|-------------------------------|------------------------------------------|-------------|
| U-pendiente-006 | continuo   | Mostrar/ocultar  | pestaña "Documento firmado"   | Visible solo si existe documento firmado | —           |
