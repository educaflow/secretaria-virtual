# Pantalla: "Todos los correos"

## Identidad

- **Quién la usa:** Administrador.
- **Qué muestra:** todas las TareaCorreo del sistema, sin filtro. Permite abrir el detalle, crear un correo nuevo y reenviar uno fallido.

## Menú

| Propiedad | Valor |
|-----------|-------|
| Ruta jerárquica | "Correos" → "Todos los correos" |
| Título visible | "Todos los correos" |
| Quién lo ve | Administrador |

---

## Estructura jerarquica de las pantallas

```
TareaCorreo
└── AdjuntoCorreo
```

---

## Grid 1 — "Todos los correos"

### Propiedades

| Propiedad | Valor |
|-----------|-------|
| Entidad | TareaCorreo |
| Columnas (en orden) | fecha de creación, asunto, DNI del destinatario, dirección de correo, centro, estado |
| Ordenación por defecto | fecha de creación descendente |
| ¿Permite buscar? | SÍ — búsqueda libre por todos los campos visibles, filtros por estado y por centro |
| Formulario que abre el onclick | Formulario 1 — "Detalle de correo" (en modo solo lectura) |
| Botones del toolbar | "Nuevo correo" |
| Botones de las columnas | — |

### Botones

| Botón | Qué hace |
|-------|----------|
| "Nuevo correo" | Abre el Formulario 1 en modo nuevo para crear una TareaCorreo |

---

## Formulario 1 — "Detalle de correo"

### Propiedades

| Propiedad | Valor |
|-----------|-------|
| Entidad | TareaCorreo |
| Solo lectura | no — solo lectura al abrir desde el grid; editable al crear nuevo |

### Paneles

| Panel (título) | Tipo | Campos |
|----------------|------|--------|
| "Datos del correo" | normal | asunto, DNI del destinatario, dirección de correo, centro, historial de estado del expediente |
| "Contenido" | normal | cuerpo |
| "Adjuntos" | anidado → Grid 2 ("Adjuntos") | — |
| "Estado del envío" | normal (siempre solo lectura) | estado, fecha de creación, fecha del último intento, número de intentos, motivo del fallo |
| "(sin título)" | botones | botón "Cancelar", botón "Guardar", botón "Reenviar" |

### Botones

| Botón | Qué hace |
|-------|----------|
| "Cancelar" | Cierra el formulario sin guardar |
| "Guardar" | Valida V-TareaCorreo-001, V-TareaCorreo-002, V-TareaCorreo-003, V-TareaCorreo-004, V-TareaCorreo-005, V-TareaCorreo-006, V-TareaCorreo-007 → Ejecuta la operación "Crear (insert)" (R-TareaCorreo-001, R-TareaCorreo-002, R-TareaCorreo-003) → Cierra el formulario |
| "Reenviar" | Ejecuta la operación "Reenviar" (R-TareaCorreo-008) → Cierra el formulario |

### Reglas de UI (U-todos-NNN)

| ID | Disparador | Efecto | Campo/Panel afectado | Condición |
|----|------------|--------|----------------------|-----------|
| U-todos-001 | continuo | Mostrar/ocultar | botón "Reenviar" | Visible solo si el estado es FALLADO |
| U-todos-002 | continuo | Mostrar/ocultar | botón "Guardar" | Visible solo cuando el formulario está en modo nuevo (no se ha creado aún) |
| U-todos-003 | continuo | Marcar solo lectura | panel "Datos del correo", panel "Contenido", panel "Adjuntos" | Solo lectura cuando el formulario abre un registro existente (no en modo nuevo) |

---

## Grid 2 — "Adjuntos"

### Propiedades

| Propiedad | Valor |
|-----------|-------|
| Entidad | AdjuntoCorreo |
| Columnas (en orden) | nombre del fichero, contenido del fichero |
| Ordenación por defecto | nombre del fichero ascendente |
| ¿Permite buscar? | NO |
| Formulario que abre el onclick | — |
| Botones del toolbar | "Añadir adjunto" |
| Botones de las columnas | — |

### Botones

| Botón | Qué hace |
|-------|----------|
| "Añadir adjunto" | Permite añadir un fichero como nuevo AdjuntoCorreo dentro de la TareaCorreo (solo disponible en modo nuevo) |

### Reglas de UI

| ID | Disparador | Efecto | Campo/Panel afectado | Condición |
|----|------------|--------|----------------------|-----------|
| U-todos-004 | continuo | Mostrar/ocultar | botón "Añadir adjunto" del toolbar | Visible solo cuando el formulario está en modo nuevo |