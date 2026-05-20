# Pantalla: "Correos del centro"

## Identidad

- **Quién la usa:** Supervisor y Administrativa.
- **Qué muestra:** las TareaCorreo cuyo centro coincide con el centro del usuario que consulta. Modo solo lectura: no se permite crear, modificar, borrar ni reenviar. Las TareaCorreo sin centro asignado no aparecen en este listado.

## Menú

| Propiedad        | Valor                                            |
|------------------|--------------------------------------------------|
| Ruta jerárquica  | "Correos" → "Correos del centro"                 |
| Título visible   | "Correos del centro"                             |
| Quién lo ve      | Supervisor, Administrativa                       |

---

## Estructura jerarquica de las pantallas

```
TareaCorreo
└── AdjuntoCorreo
```

---

## Grid 1 — "Correos del centro"

### Propiedades

| Propiedad                          | Valor                                                                                                                                            |
|------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| Entidad                            | TareaCorreo                                                                                                                                      |
| Columnas (en orden)                | asunto, DNI del destinatario, fecha de creación, estado                                                                                          |
| Ordenación por defecto             | fecha de creación descendente                                                                                                                    |
| ¿Permite buscar?                   | SÍ — filtros por asunto, DNI del destinatario, estado y rango de fecha de creación; búsqueda libre por asunto y DNI                              |
| Formulario que abre el onclick     | Formulario 1 — "Detalle de correo" (en modo solo lectura)                                                                                        |
| Botones del toolbar                | — (las TareaCorreo solo las crea el Administrador desde "Todos los correos"; ver `entity-TareaCorreo.md` operación Crear)                        |
| Botones de las columnas            | —                                                                                                                                                |

---

## Formulario 1 — "Detalle de correo"

### Propiedades

| Propiedad     | Valor                                                                       |
|---------------|-----------------------------------------------------------------------------|
| Entidad       | TareaCorreo                                                                 |
| Solo lectura  | sí                                                                          |

### Paneles

| Panel (título)                      | Tipo                          | Campos                                                                                                                                          |
|-------------------------------------|-------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| "Correo"                            | normal (siempre solo lectura) | asunto, cuerpo, DNI del destinatario, dirección de correo del destinatario, centro                                                              |
| "Estado del envío"                  | normal (siempre solo lectura) | estado, fecha de creación, fecha del último intento, número de intentos, motivo del fallo                                                       |
| "Expediente relacionado"            | normal (siempre solo lectura) | historial de estado del expediente, enlace "Ver expediente"                                                                                     |
| "Adjuntos"                          | anidado → Grid 2 ("Adjuntos") | —                                                                                                                                               |
| (sin título)                        | botones                       | botón "Cerrar"                                                                                                                                  |

### Botones

| Botón        | Qué hace                                                          |
|--------------|-------------------------------------------------------------------|
| "Cerrar"     | Cierra el formulario y vuelve al grid.                            |

### Reglas de UI (U-centro-NNN)

| ID             | Disparador | Efecto           | Campo/Panel afectado                                          | Condición                                                                                | Origen EARS |
|----------------|------------|------------------|---------------------------------------------------------------|------------------------------------------------------------------------------------------|-------------|
| U-centro-001   | continuo   | Mostrar/ocultar  | enlace "Ver expediente" del panel "Expediente relacionado"    | Visible solo si la TareaCorreo tiene historial de estado de expediente informado.        | E-ST-002    |
| U-centro-002   | continuo   | Mostrar/ocultar  | panel "Expediente relacionado"                                | Visible solo si la TareaCorreo tiene historial de estado de expediente informado.        | E-ST-002    |
| U-centro-003   | continuo   | Mostrar/ocultar  | campo "motivo del fallo"                                      | Visible solo si la TareaCorreo está en estado FALLADO y `motivoFallo` no está vacío.     | —           |

---

## Grid 2 — "Adjuntos"

### Propiedades

| Propiedad                          | Valor                                                          |
|------------------------------------|----------------------------------------------------------------|
| Entidad                            | AdjuntoCorreo                                                  |
| Columnas (en orden)                | nombre del fichero                                             |
| Ordenación por defecto             | nombre del fichero ascendente                                  |
| ¿Permite buscar?                   | NO                                                             |
| Formulario que abre el onclick     | Formulario 2 — "Detalle del adjunto"                           |
| Botones del toolbar                | — (los adjuntos no se crean desde esta pantalla; solo lectura) |
| Botones de las columnas            | —                                                              |

### Botones

*(sin botones — las acciones sobre cada adjunto se realizan abriendo su detalle.)*

---

## Formulario 2 — "Detalle del adjunto"

### Propiedades

| Propiedad     | Valor                                                                                          |
|---------------|------------------------------------------------------------------------------------------------|
| Entidad       | AdjuntoCorreo                                                                                  |
| Solo lectura  | sí                                                                                             |

### Paneles

| Panel (título) | Tipo                          | Campos                              |
|----------------|-------------------------------|-------------------------------------|
| "Adjunto"      | normal (siempre solo lectura) | nombre del fichero                  |
| (sin título)   | botones                       | botón "Cerrar", botón "Descargar"   |

### Botones

Convención de orden (de izquierda a derecha): cancelar · acción principal. No hay botón destructivo porque V-AdjuntoCorreo-004 prohíbe borrar adjuntos.

| Botón        | Qué hace                                                          |
|--------------|-------------------------------------------------------------------|
| "Cerrar"     | Cierra el formulario y vuelve al Formulario 1.                    |
| "Descargar"  | Descarga el contenido del adjunto al equipo del usuario.          |
