# Pantalla: "Todos los correos"

## Identidad

- **Quién la usa:** Administrador.
- **Qué muestra:** la lista completa de TareaCorreo del sistema, con y sin centro asignado, en modo consulta. Permite abrir el detalle de cualquiera (solo lectura), crear un correo nuevo (mismo formulario en modo edición) y reenviar uno fallido.

## Menú

| Propiedad        | Valor                                  |
|------------------|----------------------------------------|
| Ruta jerárquica  | "Correos" → "Todos los correos"        |
| Título visible   | "Todos los correos"                    |
| Quién lo ve      | Administrador                          |

---

## Estructura jerarquica de las pantallas

```
TareaCorreo
└── AdjuntoCorreo
```

---

## Grid 1 — "Todos los correos"

### Propiedades

| Propiedad                          | Valor                                                                                                                                  |
|------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------|
| Entidad                            | TareaCorreo                                                                                                                            |
| Columnas (en orden)                | asunto, DNI del destinatario, centro, fecha de creación, estado                                                                        |
| Ordenación por defecto             | fecha de creación descendente                                                                                                          |
| ¿Permite buscar?                   | SÍ — filtros por asunto, DNI del destinatario, centro, estado y rango de fecha de creación; búsqueda libre por asunto y DNI            |
| Formulario que abre el onclick     | Formulario 1 — "Detalle de correo" (en modo solo lectura)                                                                              |
| Botones del toolbar                | "Nuevo correo"                                                                                                                         |
| Botones de las columnas            | —                                                                                                                                      |

### Botones

| Botón           | Qué hace                                                                                                |
|-----------------|---------------------------------------------------------------------------------------------------------|
| "Nuevo correo"  | Abre el Formulario 1 — "Detalle de correo" en modo edición sobre una TareaCorreo nueva (sin registro).  |

---

## Formulario 1 — "Detalle de correo"

### Propiedades

| Propiedad     | Valor                                                                                                                                 |
|---------------|---------------------------------------------------------------------------------------------------------------------------------------|
| Entidad       | TareaCorreo                                                                                                                           |
| Solo lectura  | no — solo lectura cuando se abre desde el grid (registro existente); editable cuando se abre desde el botón "Nuevo correo" (modo creación). |

### Paneles

| Panel (título)                      | Tipo                          | Campos                                                                                                                                          |
|-------------------------------------|-------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| "Correo"                            | normal                        | asunto, cuerpo, DNI del destinatario, dirección de correo del destinatario, centro                                                              |
| "Estado del envío"                  | normal (siempre solo lectura) | estado, fecha de creación, fecha del último intento, número de intentos, motivo del fallo                                                       |
| "Expediente relacionado"            | normal                        | historial de estado del expediente, enlace "Ver expediente"                                                                                     |
| "Adjuntos"                          | anidado → Grid 2 ("Adjuntos") | —                                                                                                                                               |
| (sin título)                        | botones                       | botón "Cancelar", botón "Guardar y enviar", botón "Cerrar", botón "Reenviar"                                                                    |

### Botones

Convención de orden (de izquierda a derecha): acción destructiva · cancelar · acción principal/guardar. En modo creación coexisten "Cancelar" (izquierda) y "Guardar y enviar" (derecha, principal); en modo detalle existente coexisten "Cerrar" (izquierda) y "Reenviar" (derecha, principal, solo si la TareaCorreo está en estado FALLADO). No hay botón de borrar porque V-TareaCorreo-008 prohíbe borrar TareaCorreo.

| Botón                | Qué hace                                                                                                                                                                                                                              |
|----------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| "Cancelar"           | Descarta los datos introducidos y vuelve a la pantalla "Todos los correos".                                                                                                                                                           |
| "Guardar y enviar"   | Valida V-TareaCorreo-001, V-TareaCorreo-002, V-TareaCorreo-003, V-TareaCorreo-004, V-TareaCorreo-005, V-TareaCorreo-006 → Ejecuta la operación "Crear (insert)" (R-TareaCorreo-001, R-TareaCorreo-002, R-AdjuntoCorreo-001) → Vuelve a la pantalla "Todos los correos". |
| "Cerrar"             | Cierra el formulario y vuelve al grid.                                                                                                                                                                                                |
| "Reenviar"           | Valida V-TareaCorreo-009 → Ejecuta la operación "Reenviar" (R-TareaCorreo-003) → Recarga el formulario.                                                                                                                               |

### Reglas de UI (U-todos-NNN)

| ID            | Disparador | Efecto             | Campo/Panel afectado                                                  | Condición                                                                                                                                          | Origen EARS |
|---------------|------------|--------------------|-----------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|-------------|
| U-todos-001   | continuo   | Mostrar/ocultar    | botón "Reenviar"                                                      | Visible solo si la TareaCorreo está en estado FALLADO (lo que implica además que el formulario está en modo edición de registro existente, no en modo creación). | E-ST-001    |
| U-todos-002   | continuo   | Mostrar/ocultar    | enlace "Ver expediente" del panel "Expediente relacionado"            | Visible solo si la TareaCorreo tiene historial de estado de expediente informado.                                                                  | E-ST-002    |
| U-todos-003   | continuo   | Mostrar/ocultar    | panel "Expediente relacionado"                                        | Visible solo si la TareaCorreo tiene historial de estado de expediente informado o si el formulario está en modo creación.                         | E-ST-002    |
| U-todos-004   | continuo   | Mostrar/ocultar    | campo "motivo del fallo"                                              | Visible solo si la TareaCorreo está en estado FALLADO y `motivoFallo` no está vacío.                                                                | —           |
| U-todos-005   | continuo   | Mostrar/ocultar    | panel "Estado del envío"                                              | Visible solo si el formulario está en modo edición de un registro existente (oculto en modo creación, donde aún no hay estado ni fechas).          | —           |
| U-todos-006   | continuo   | Mostrar/ocultar    | botón "Guardar y enviar", botón "Cancelar"                            | Visibles solo si el formulario está en modo creación.                                                                                              | —           |
| U-todos-007   | continuo   | Mostrar/ocultar    | botón "Cerrar"                                                        | Visible solo si el formulario está en modo edición de un registro existente (oculto en modo creación, donde el cierre se hace con "Cancelar").     | —           |
| U-todos-008   | continuo   | Marcar obligatorio | campo "asunto"                                                        | Solo en modo creación.                                                                                                                             | E-UN-001    |
| U-todos-009   | continuo   | Marcar obligatorio | campo "cuerpo"                                                        | Solo en modo creación.                                                                                                                             | E-UN-001    |
| U-todos-010   | continuo   | Marcar obligatorio | campo "DNI del destinatario"                                          | Solo en modo creación.                                                                                                                             | E-UN-001    |
| U-todos-011   | continuo   | Marcar obligatorio | campo "dirección de correo del destinatario"                          | Solo en modo creación.                                                                                                                             | E-UN-001    |

---

## Grid 2 — "Adjuntos"

### Propiedades

| Propiedad                          | Valor                                                                                                                          |
|------------------------------------|--------------------------------------------------------------------------------------------------------------------------------|
| Entidad                            | AdjuntoCorreo                                                                                                                  |
| Columnas (en orden)                | nombre del fichero                                                                                                             |
| Ordenación por defecto             | nombre del fichero ascendente                                                                                                  |
| ¿Permite buscar?                   | NO                                                                                                                             |
| Formulario que abre el onclick     | Formulario 2 — "Detalle del adjunto"                                                                                           |
| Botones del toolbar                | "Añadir adjunto"                                                                                                               |
| Botones de las columnas            | —                                                                                                                              |

### Botones

| Botón              | Qué hace                                                                                                              |
|--------------------|-----------------------------------------------------------------------------------------------------------------------|
| "Añadir adjunto"   | Abre el selector de ficheros y añade el fichero seleccionado como AdjuntoCorreo al correo que se está creando.        |

### Reglas de UI (U-todos-NNN)

| ID            | Disparador | Efecto           | Campo/Panel afectado                          | Condición                                                                                                  | Origen EARS |
|---------------|------------|------------------|-----------------------------------------------|------------------------------------------------------------------------------------------------------------|-------------|
| U-todos-012   | continuo   | Mostrar/ocultar  | botón "Añadir adjunto" (toolbar del grid)     | Visible solo si el formulario padre está en modo creación.                                                 | —           |

---

## Formulario 2 — "Detalle del adjunto"

### Propiedades

| Propiedad     | Valor                                                                                                                                       |
|---------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| Entidad       | AdjuntoCorreo                                                                                                                               |
| Solo lectura  | sí — los datos del adjunto nunca se editan desde aquí; los botones permiten quitarlo (solo en modo creación del padre) o descargar el fichero. |

### Paneles

| Panel (título)        | Tipo                          | Campos                                                       |
|-----------------------|-------------------------------|--------------------------------------------------------------|
| "Adjunto"             | normal (siempre solo lectura) | nombre del fichero                                           |
| (sin título)          | botones                       | botón "Quitar adjunto", botón "Cerrar", botón "Descargar"    |

### Botones

Convención de orden (de izquierda a derecha): acción destructiva · cancelar · acción principal. "Quitar adjunto" (izquierda) solo es visible mientras el padre TareaCorreo está en modo creación (la TareaCorreo aún no se ha guardado y su lista de adjuntos puede modificarse); "Cerrar" (centro) está siempre; "Descargar" (derecha, principal) está siempre.

| Botón              | Qué hace                                                                                                              |
|--------------------|-----------------------------------------------------------------------------------------------------------------------|
| "Quitar adjunto"   | Elimina este AdjuntoCorreo de la lista de adjuntos del correo en creación (aún no guardado) y vuelve al Formulario 1.  |
| "Cerrar"           | Cierra el formulario y vuelve al Formulario 1.                                                                        |
| "Descargar"        | Descarga el contenido del adjunto al equipo del usuario.                                                              |

### Reglas de UI (U-todos-NNN)

| ID            | Disparador | Efecto           | Campo/Panel afectado     | Condición                                                                                                  | Origen EARS |
|---------------|------------|------------------|--------------------------|------------------------------------------------------------------------------------------------------------|-------------|
| U-todos-013   | continuo   | Mostrar/ocultar  | botón "Quitar adjunto"   | Visible solo si el Formulario 1 padre está en modo creación.                                               | —           |
