---
name: k-validaciones/calculados
description: Campos calculados — tipos (tiempo real, al guardar, derivados), plantilla de documentación, ejemplos con fórmulas y dependencias, dependencias circulares
---

# Campos Calculados

Un campo calculado es aquel cuyo valor se obtiene automáticamente a partir de otros campos, mediante una fórmula o regla de derivación. El usuario no lo introduce directamente (aunque a veces puede editarlo manualmente).

---

## Tipos de campos calculados

### Calculados en tiempo real (computed fields)

El valor se actualiza automáticamente cada vez que cambian los campos de los que depende. El usuario ve el cambio de forma inmediata, sin necesidad de guardar.

**Características:**
- El usuario no puede editarlos directamente (campo de solo lectura)
- Se recalculan mientras el usuario rellena el formulario
- Típicos para totales, subtotales, duraciones, porcentajes derivados

**Ejemplos:**
- Total de línea = precio unitario × cantidad
- Importe de IVA = base imponible × tipo de IVA / 100
- Edad = años entre fecha de nacimiento y hoy
- Días entre dos fechas
- Suma de calificaciones

### Calculados al guardar (triggered fields)

El valor se calcula cuando se guarda el registro o cuando se ejecuta una acción específica (como cambiar de estado). No se actualiza continuamente.

**Características:**
- Pueden ser editables en algunos casos (el usuario puede sobreescribirlos)
- Se generan una vez o en momentos específicos del ciclo de vida
- Típicos para códigos, numeraciones, fechas de auditoría

**Ejemplos:**
- Número de expediente: se genera al crear (formato AÑO-SECUENCIA-CENTRO)
- Fecha de última modificación: se actualiza al guardar
- Fecha de envío: se registra al cambiar de estado a "Enviado"
- Hash del documento: se calcula al subir el fichero

### Derivados con reglas de negocio

El valor se calcula aplicando lógica de negocio más compleja, que puede incluir consultas a otros registros o aplicar tablas de referencia.

**Características:**
- Dependen del estado del sistema, no solo de los campos del formulario
- Pueden cambiar aunque el usuario no haya modificado el registro
- Típicos para estados derivados, clasificaciones, indicadores

**Ejemplos:**
- Estado de morosidad: derivado de los días de vencimiento de las facturas pendientes
- Categoría de cliente: derivada del volumen de compra en los últimos 12 meses
- Situación académica del alumno: derivada del porcentaje de asignaturas superadas
- Nivel de riesgo: derivado de múltiples indicadores combinados

---

## Plantilla de documentación

Para cada campo calculado, documentar:

```
Campo: [nombre del campo]
Etiqueta en UI: [cómo aparece en pantalla]
Tipo de cálculo: [En tiempo real | Al guardar | Derivado]

Fórmula / Regla:
  [Descripción precisa en lenguaje natural de cómo se calcula el valor]

Campos de entrada (dependencias):
  - [campo_1]: [cómo se usa]
  - [campo_2]: [cómo se usa]

Redondeo: [si aplica: al entero más cercano | 2 decimales | truncar | sin redondeo]

Cuándo se recalcula:
  [Descripción del trigger: al cambiar campo X, al guardar, al cambiar estado, etc.]

Editable por el usuario: [No | Sí | Solo si {condición}]

Valor cuando los campos de entrada están vacíos:
  [0 | null | no mostrar | mostrar "--" | etc.]
```

---

## Ejemplos documentados

### Total de línea de pedido

```
Campo: importe_linea
Etiqueta en UI: Importe
Tipo de cálculo: En tiempo real

Fórmula:
  importe_linea = precio_unitario × cantidad × (1 - descuento_porcentaje / 100)

Campos de entrada:
  - precio_unitario: precio sin descuento
  - cantidad: número de unidades
  - descuento_porcentaje: porcentaje de descuento aplicado (0-100)

Redondeo: 2 decimales, redondeo estándar (≥ 0.005 redondea al alza)

Cuándo se recalcula: Cada vez que cambia precio_unitario, cantidad 
                     o descuento_porcentaje

Editable: No

Valor cuando campos vacíos: 0,00 €
```

### Total del pedido

```
Campo: total_pedido
Etiqueta en UI: Total pedido
Tipo de cálculo: En tiempo real

Fórmula:
  total_pedido = suma de importe_linea de todas las líneas 
                 + gastos_envio 
                 + total_pedido × tipo_iva_general / 100

Campos de entrada:
  - importe_linea (de cada línea del pedido)
  - gastos_envio: coste de envío (puede ser 0)
  - tipo_iva_general: tipo de IVA del pedido

Redondeo: 2 decimales

Cuándo se recalcula: Al modificar cualquier línea, al cambiar gastos de 
                     envío o al cambiar el tipo de IVA

Editable: No

Valor cuando no hay líneas: 0,00 €
```

### Número de expediente

```
Campo: numero_expediente
Etiqueta en UI: Número de expediente
Tipo de cálculo: Al guardar (al crear el registro)

Fórmula:
  numero_expediente = AÑO_ACTUAL + "/" + SECUENCIA_ANUAL + "/" + CODIGO_CENTRO
  Donde:
  - AÑO_ACTUAL: año en 4 dígitos (ej: 2024)
  - SECUENCIA_ANUAL: número correlativo dentro del año para ese centro, 
                      con 5 dígitos con relleno de ceros (ej: 00042)
  - CODIGO_CENTRO: código de 3 letras del centro (ej: MIS)
  Ejemplo resultado: 2024/00042/MIS

Campos de entrada:
  - Fecha de creación (para el año)
  - Secuencia del centro para ese año (consultada en el sistema)
  - Centro al que pertenece el expediente

Redondeo: No aplica

Cuándo se genera: Al crear el expediente (una sola vez; no se modifica nunca)

Editable: No

Valor en borrador: Puede mostrarse "PENDIENTE" o vacío hasta la creación definitiva
```

### Días de demora en pago

```
Campo: dias_demora
Etiqueta en UI: Días de demora
Tipo de cálculo: Derivado (se recalcula cada vez que se consulta)

Fórmula:
  SI la factura está pendiente de cobro:
    dias_demora = días entre fecha_vencimiento y HOY
    (positivo = factura vencida; negativo = todavía no ha vencido)
  SI la factura ya está cobrada:
    dias_demora = días entre fecha_vencimiento y fecha_cobro
    (positivo = se cobró tarde; negativo o 0 = se cobró a tiempo)

Campos de entrada:
  - fecha_vencimiento
  - fecha_cobro (si existe)
  - fecha actual (del sistema)

Redondeo: Entero (días completos)

Cuándo se recalcula: Cada vez que se abre el registro (depende de la fecha actual)

Editable: No

Nota: Este campo cambia de valor cada día aunque el usuario no toque el registro.
      Es importante documentarlo para que no genere confusión al usuario.
```

### Situación académica

```
Campo: situacion_academica
Etiqueta en UI: Situación académica
Tipo de cálculo: Derivado

Fórmula:
  Calcular porcentaje_superado = asignaturas_superadas / asignaturas_matriculadas × 100
  SI porcentaje_superado >= 75% → "PROGRESO ADECUADO"
  SI porcentaje_superado >= 50% Y < 75% → "EN SEGUIMIENTO"
  SI porcentaje_superado < 50% → "EN RIESGO"
  SI asignaturas_matriculadas = 0 → "SIN DATOS"

Campos de entrada:
  - Número de asignaturas matriculadas en el curso actual
  - Número de asignaturas con calificación aprobatoria en el curso actual

Cuándo se recalcula: Al actualizar cualquier calificación del alumno

Editable: No (solo informativo)
```

---

## Dependencias entre campos calculados

Los campos calculados pueden depender de otros campos calculados. Documentar la cadena de dependencias:

```
precio_unitario ──────────────────────────────────┐
cantidad ──────────────────────────────────────────┤──► importe_linea
descuento_porcentaje ────────────────────────────────┘        │
                                                               │
gastos_envio ─────────────────────────────────────────────────┤──► total_pedido
tipo_iva_general ─────────────────────────────────────────────┘
```

---

## Dependencias circulares

Una dependencia circular ocurre cuando el campo A depende del campo B y el campo B depende del campo A (directa o indirectamente). Son un error de diseño que debe detectarse en el análisis.

**Ejemplo de dependencia circular incorrecta:**
```
precio_final = precio_base - descuento_euros
descuento_euros = precio_final × porcentaje_descuento / 100
```
Aquí `precio_final` depende de `descuento_euros` y `descuento_euros` depende de `precio_final`. No es calculable sin un valor de partida.

**Cómo resolverlo:**
- Identificar cuál de los campos es el "origen" (el que introduce el usuario) y cuál es el "resultado"
- Redefinir la fórmula para que no haya ciclo:
```
descuento_euros = precio_base × porcentaje_descuento / 100
precio_final = precio_base - descuento_euros
```

---

## Cuándo un campo calculado puede editarse manualmente

Algunos campos calculados tienen un valor por defecto calculado pero el usuario puede sobreescribirlos. Documentar exactamente cuándo:

```
Campo: fecha_entrega_prevista
Calculado por defecto: fecha_pedido + plazo_tipo_producto días
Editable: Sí, el usuario puede cambiar la fecha propuesta
Al modificarla: El campo se marca como "Fecha personalizada" 
                para distinguirla de la calculada automáticamente
Si se borra el valor manual: Se restaura el valor calculado
```
