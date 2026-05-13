# Pantalla: "Todos los correos"

Pantalla que ilustra el uso del botón **"Reintentar"** como **botón de columna** del grid, visible solo en las filas cuyo estado es FALLADO. La regla de visibilidad se documenta en la sección `Reglas de UI` del grid (no del formulario), porque depende de los datos de cada fila.

## Identidad

- **Quién la usa:** Administrador.
- **Qué muestra:** todos los correos del sistema sin filtro de centro, en cualquier estado.

## Menú

| Propiedad        | Valor                                                  |
|------------------|--------------------------------------------------------|
| Ruta jerárquica  | "Notificaciones" → "Correos" → "Todos los correos"     |
| Título visible   | "Todos los correos"                                    |
| Quién lo ve      | Administrador                                          |

---

## Grid 1 — "Correos"

### Propiedades

| Propiedad                          | Valor                                                                              |
|------------------------------------|------------------------------------------------------------------------------------|
| Columnas (en orden)                | estado, fecha de creación, centro, email destinatario, DNI destinatario, asunto, nº intentos, fecha de envío OK |
| Ordenación por defecto             | fecha de creación descendente                                                      |
| ¿Permite buscar?                   | SÍ — búsqueda libre + filtros por estado, fecha de creación, centro, DNI destinatario, email destinatario |
| Formulario que abre el onclick     | Formulario 1 — Correo (en modo solo lectura)                                       |
| Botones del toolbar                | "Nuevo correo"                                                                     |
| Botones de las columnas            | "Reintentar"                                                                       |

### Botones

| Botón          | Qué hace                                                                                         |
|----------------|--------------------------------------------------------------------------------------------------|
| "Nuevo correo" | Abre el formulario para crear un nuevo correo                                                    |
| "Reintentar"   | Ejecuta la operación "Reintentar envío" sobre el correo de la fila (R-002) → recarga el listado  |

### Reglas de UI (U-XXX)

| ID    | Disparador | Efecto          | Campo/Panel afectado            | Condición                              |
|-------|------------|-----------------|---------------------------------|----------------------------------------|
| U-001 | continuo   | Mostrar/ocultar | botón "Reintentar" de la fila   | Visible solo si la fila tiene estado FALLADO |

## Formulario 1 — Correo

### Paneles

| Panel (título)             | Tipo                              | Campos                                                                          |
|----------------------------|-----------------------------------|---------------------------------------------------------------------------------|
| "Datos generales"          | normal                            | centro, de, asunto                                                              |
| "Destinatario"             | normal                            | DNI/NIE, email, nombre                                                          |
| "Contenido"                | normal                            | cuerpo HTML, cuerpo texto plano                                                 |
| "Adjuntos"                 | anidado → Grid 2 ("Adjuntos")     | —                                                                               |
| "Estado y trazabilidad"    | normal (siempre solo lectura)     | estado, fecha de creación, fecha último intento, nº intentos, fecha de envío OK |
| "Log de errores"           | normal (siempre solo lectura)     | log de errores                                                                  |
| "Expediente relacionado"   | normal (siempre solo lectura)     | expediente                                                                      |

*(sin botones — la acción "Reintentar" se ejecuta desde la columna del grid)*

### Reglas de UI (U-XXX)

| ID    | Disparador | Efecto             | Campo/Panel afectado                                                          | Condición                                                  |
|-------|------------|--------------------|-------------------------------------------------------------------------------|------------------------------------------------------------|
| U-002 | continuo   | Solo lectura       | centro, asunto, DNI/NIE, email, nombre, cuerpo HTML, cuerpo texto plano       | El registro ya existe (no es nuevo)                        |
| U-003 | continuo   | Solo lectura       | campo "de"                                                                    | Siempre (lo asigna el sistema)                             |
| U-004 | continuo   | Mostrar/ocultar    | panel "Log de errores"                                                        | Visible solo si hay errores que mostrar                    |
| U-005 | continuo   | Mostrar/ocultar    | panel "Expediente relacionado"                                                | Visible solo si el correo proviene de un expediente        |

---

## Grid 2 — "Adjuntos"

### Propiedades

| Propiedad                          | Valor                                          |
|------------------------------------|------------------------------------------------|
| Columnas (en orden)                | nombre del fichero                             |
| Ordenación por defecto             | —                                              |
| ¿Permite buscar?                   | NO                                             |
| Formulario que abre el onclick     | Formulario 2 — Adjunto (en modo solo lectura)  |
| Botones del toolbar                | "Añadir adjunto"                               |
| Botones de las columnas            | —                                              |

### Botones

| Botón            | Qué hace                                                       |
|------------------|----------------------------------------------------------------|
| "Añadir adjunto" | Abre el formulario para subir un nuevo fichero adjunto         |

## Formulario 2 — Adjunto

### Paneles

| Panel (título) | Tipo    | Campos              |
|----------------|---------|---------------------|
| (sin título)   | normal  | fichero, descripción|

*(sin botones)*
