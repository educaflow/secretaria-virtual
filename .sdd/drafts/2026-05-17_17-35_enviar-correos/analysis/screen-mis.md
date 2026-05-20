# Pantalla: "Mis correos"

## Identidad

- **Quién la usa:** Profesor, Exprofesor, Alumno, Exalumno, Familiar, Externo.
- **Qué muestra:** las TareaCorreo cuyo DNI de destinatario coincide con el DNI del usuario que consulta, sin filtro adicional por centro. Modo solo lectura: no se permite crear, modificar, borrar ni reenviar.

## Menú

| Propiedad        | Valor                                                            |
|------------------|------------------------------------------------------------------|
| Ruta jerárquica  | "Carpeta ciudadana" → "Mis correos"                              |
| Título visible   | "Mis correos"                                                    |
| Quién lo ve      | Profesor, Exprofesor, Alumno, Exalumno, Familiar, Externo        |

---

## Estructura jerarquica de las pantallas

```
TareaCorreo
└── AdjuntoCorreo
```

---

## Grid 1 — "Mis correos"

### Propiedades

| Propiedad                          | Valor                                                                                                                                            |
|------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| Entidad                            | TareaCorreo                                                                                                                                      |
| Columnas (en orden)                | asunto, fecha de creación, estado                                                                                                                |
| Ordenación por defecto             | fecha de creación descendente                                                                                                                    |
| ¿Permite buscar?                   | SÍ — filtros por asunto, estado y rango de fecha de creación; búsqueda libre por asunto                                                          |
| Formulario que abre el onclick     | Formulario 1 — "Detalle de correo" (en modo solo lectura)                                                                                        |
| Botones del toolbar                | — (los correos solo los crea el Administrador desde "Todos los correos"; ver `entity-TareaCorreo.md` operación Crear)                            |
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
| "Correo"                            | normal (siempre solo lectura) | asunto, cuerpo, dirección de correo del destinatario                                                                                            |
| "Estado del envío"                  | normal (siempre solo lectura) | estado, fecha de creación                                                                                                                       |
| "Expediente relacionado"            | normal (siempre solo lectura) | historial de estado del expediente, enlace "Ver expediente"                                                                                     |
| "Adjuntos"                          | anidado → Grid 2 ("Adjuntos") | —                                                                                                                                               |
| (sin título)                        | botones                       | botón "Cerrar"                                                                                                                                  |

### Botones

| Botón        | Qué hace                                                          |
|--------------|-------------------------------------------------------------------|
| "Cerrar"     | Cierra el formulario y vuelve al grid.                            |

### Reglas de UI (U-mis-NNN)

| ID          | Disparador | Efecto           | Campo/Panel afectado                                          | Condición                                                                                | Origen EARS |
|-------------|------------|------------------|---------------------------------------------------------------|------------------------------------------------------------------------------------------|-------------|
| U-mis-001   | continuo   | Mostrar/ocultar  | enlace "Ver expediente" del panel "Expediente relacionado"    | Visible solo si la TareaCorreo tiene historial de estado de expediente informado.        | E-ST-002    |
| U-mis-002   | continuo   | Mostrar/ocultar  | panel "Expediente relacionado"                                | Visible solo si la TareaCorreo tiene historial de estado de expediente informado.        | E-ST-002    |

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
