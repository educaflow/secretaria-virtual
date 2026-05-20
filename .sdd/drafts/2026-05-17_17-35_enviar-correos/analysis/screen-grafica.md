# Pantalla: "Gráfica de correos enviados"

## Identidad

- **Quién la usa:** Administrador.
- **Qué muestra:** una gráfica diaria del número de TareaCorreo creadas en un rango de fechas, desglosada por estado (PENDIENTE, ENVIANDO, ENVIADO, FALLADO). El usuario elige fecha inicial y fecha final, ambas obligatorias.

## Menú

| Propiedad        | Valor                                                |
|------------------|------------------------------------------------------|
| Ruta jerárquica  | "Correos" → "Gráfica de correos enviados"            |
| Título visible   | "Gráfica de correos enviados"                        |
| Quién lo ve      | Administrador                                        |

---

## Estructura jerarquica de las pantallas

```
TareaCorreo
```

*(La pantalla no navega a sub-pantallas: solo presenta la gráfica agregada.)*

---

## Formulario 1 — "Gráfica de correos enviados"

### Propiedades

| Propiedad     | Valor                                                                                   |
|---------------|-----------------------------------------------------------------------------------------|
| Entidad       | TareaCorreo                                                                             |
| Solo lectura  | no — el formulario permite editar los criterios (fechas) pero no modifica TareaCorreo.  |

### Paneles

| Panel (título)             | Tipo                | Campos                                                                                                                              |
|----------------------------|---------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| "Rango de fechas"          | normal              | fecha inicial, fecha final, botón "Actualizar gráfica"                                                                              |
| "Correos por día"          | normal              | gráfica diaria del número de TareaCorreo creadas en el rango seleccionado, desglosada por estado (PENDIENTE, ENVIANDO, ENVIADO, FALLADO) |

### Botones

| Botón                  | Qué hace                                                                                                                                       |
|------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| "Actualizar gráfica"   | Recarga la gráfica con el rango de fechas seleccionado. Si el rango no es válido (ver U-grafica-003), muestra el aviso y no recarga.           |

### Reglas de UI (U-grafica-NNN)

| ID              | Disparador            | Efecto             | Campo/Panel afectado     | Condición                                                                                              | Origen EARS |
|-----------------|-----------------------|--------------------|--------------------------|--------------------------------------------------------------------------------------------------------|-------------|
| U-grafica-001   | continuo              | Marcar obligatorio | campo "fecha inicial"    | Siempre.                                                                                               | E-UB-010    |
| U-grafica-002   | continuo              | Marcar obligatorio | campo "fecha final"      | Siempre.                                                                                               | E-UB-010    |
| U-grafica-003   | continuo              | Mostrar aviso      | panel "Rango de fechas"  | Si la fecha inicial es posterior a la fecha final, mostrar el mensaje "El rango de fechas no es válido: la fecha inicial es posterior a la fecha final." | E-UN-009    |
