# Pantalla: "Gráfica de correos enviados"

## Identidad
- **Quién la usa:** Administrador.
- **Qué muestra:** una gráfica de barras diaria con el número de TareaCorreo creadas entre dos fechas elegidas, desglosado por estado (PENDIENTE, ENVIANDO, ENVIADO, FALLADO).

## Menú
| Propiedad | Valor |
|-----------|-------|
| Ruta jerárquica | "Correos" → "Gráfica de correos enviados" |
| Título visible | "Gráfica de correos enviados" |
| Quién lo ve | Administrador |

---

## Estructura jerarquica de las pantallas

```
TareaCorreo (agregada)
```

---

## Gráfica 1 — "Correos enviados por día"

### Propiedades
| Propiedad | Valor |
|-----------|-------|
| Entidad | TareaCorreo (agregada para la serie) |
| Tipo de gráfica | barras apiladas |
| Eje X | día (fecha de creación, agrupada por día) |
| Eje Y | número de TareaCorreo |
| Series | una por estado (PENDIENTE, ENVIANDO, ENVIADO, FALLADO) |
| Parámetros de entrada | fecha inicial (obligatoria), fecha final (obligatoria) |
| Botones | "Refrescar" |

### Botones
| Botón | Qué hace |
|-------|----------|
| "Refrescar" | Vuelve a calcular y dibujar la gráfica con el rango de fechas indicado |

### Reglas de UI (U-grafica-NNN)

| ID | Disparador | Efecto | Campo/Panel afectado | Condición |
|----|------------|--------|----------------------|-----------|
| U-grafica-001 | onLoad | Valor por defecto | campo "fecha inicial" | Primer día del mes en curso |
| U-grafica-002 | onLoad | Valor por defecto | campo "fecha final" | Día de hoy |
| U-grafica-003 | continuo | Marcar obligatorio | campo "fecha inicial" | Siempre |
| U-grafica-004 | continuo | Marcar obligatorio | campo "fecha final" | Siempre |
| U-grafica-005 | onChange:fecha inicial | Mostrar/ocultar | mensaje de error "El rango de fechas no es válido: la fecha inicial '{valor}' es posterior a la fecha final" | Visible cuando fecha inicial > fecha final |
| U-grafica-006 | onChange:fecha final | Mostrar/ocultar | mensaje de error "El rango de fechas no es válido: la fecha inicial es posterior a la fecha final '{valor}'" | Visible cuando fecha inicial > fecha final |