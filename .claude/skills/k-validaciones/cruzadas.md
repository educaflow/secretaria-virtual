---
name: k-validaciones/cruzadas
description: Validaciones entre campos — consistencia temporal/numérica/dominio, requerimiento mutuo, exclusión, tablas de decisión, validaciones condicionales con SI/ENTONCES, efectos laterales
---

# Validaciones Cruzadas y Condicionales

Una validación cruzada es aquella en la que la validez de un dato depende del valor de otro campo. Son las más expresivas desde el punto de vista funcional: capturan las reglas de coherencia del negocio que no pueden especificarse campo a campo.

---

## 2A. Consistencia temporal

Una fecha debe ser anterior, posterior o igual a otra.

### Patrones habituales

| Patrón | Descripción | Ejemplo |
|--------|-------------|---------|
| `Fecha_A < Fecha_B` | A debe ser anterior a B | Fecha de inicio < Fecha de fin |
| `Fecha_A <= Fecha_B` | A debe ser anterior o igual a B | Fecha de pedido <= Fecha de entrega |
| `Fecha_A > Fecha_B` | A debe ser posterior a B | Fecha de incorporación > Fecha de baja |
| `Fecha_A < Hoy` | A debe ser pasada | Fecha de nacimiento < Hoy |
| `Fecha_A >= Hoy` | A debe ser presente o futura | Fecha de inicio de contrato >= Hoy |
| `Fecha_A <= Fecha_B + N días` | A no puede ser mucho posterior a B | Fecha de fin <= Fecha de inicio + 365 días |

### Cuándo aplica

Especificar siempre si la validación cruzada aplica solo cuando ambos campos tienen valor o también cuando uno está vacío.

```
Regla CR-001
Tipo: Consistencia temporal
Campos: Fecha de inicio, Fecha de fin
Condición: Fecha de fin >= Fecha de inicio
Cuándo aplica: Solo cuando ambos campos tienen valor
Mensaje: "La fecha de fin ({valor_fin}) no puede ser anterior a la fecha 
          de inicio ({valor_inicio})"
```

---

## 2B. Consistencia numérica

Un valor numérico debe guardar una relación matemática con otro.

| Patrón | Ejemplo |
|--------|---------|
| `Valor_A <= Valor_B` | Importe pagado <= Importe total de la factura |
| `Valor_A >= Valor_B` | Stock mínimo <= Stock actual |
| `Suma de partes = Total` | Suma de líneas de pedido = Total pedido |
| `A% de B = C` | % de IVA × Base imponible = Cuota IVA |
| `Valor_A < Límite derivado de B` | Descuento euros <= Precio total del artículo |

---

## 2C. Consistencia de dominio

El valor de un campo filtra o condiciona los valores válidos de otro.

```
El municipio seleccionado debe pertenecer a la provincia seleccionada.
El cargo seleccionado debe ser compatible con el departamento seleccionado.
El tipo de documento debe ser compatible con el tipo de expediente.
```

Documentar también el comportamiento cuando cambia el campo padre:
- ¿Se limpia el campo hijo automáticamente?
- ¿Se mantiene si el valor sigue siendo válido en el nuevo contexto?
- ¿Se muestra advertencia al usuario?

---

## 2D. Requerimiento mutuo y exclusión

### Requerimiento mutuo (AND)

Si un campo tiene valor, otro también debe tenerlo.

```
SI [Fecha de fin] tiene valor → [Fecha de inicio] es obligatorio
SI [Teléfono de empresa] tiene valor → [Nombre de empresa] es obligatorio
SI [Persona de contacto] tiene valor → [Medio de contacto] es obligatorio
```

### Al menos uno de N campos

Debe rellenarse al menos uno de varios campos alternativos.

```
Debe rellenarse al menos uno de: Teléfono fijo, Teléfono móvil, Email
No se permite que todos estén vacíos simultáneamente
```

### Exclusión mutua (OR exclusivo)

No pueden tener valor dos campos a la vez.

```
Persona física y Persona jurídica no pueden estar ambas marcadas simultáneamente
Solo puede seleccionarse un método de pago por pedido
Los campos NIF y CIF son mutuamente excluyentes (una persona tiene uno u otro)
```

---

## 2E. Totales cruzados

La suma de varios campos debe coincidir con otro campo.

```
Total factura = suma de (precio × cantidad × (1 - descuento)) de cada línea + gastos de envío
Porcentaje participación socio A + socio B + socio C = 100%
Horas asignadas a tareas <= Horas totales del proyecto
```

---

## Formato de documentación de validaciones cruzadas

```
Regla CR-[número]
Tipo: [temporal | numérica | dominio | requerimiento | exclusión | total]
Campos involucrados: [campo_1], [campo_2], ...
Condición: [descripción precisa de la regla]
Cuándo aplica: [siempre | solo si campo_X tiene valor | solo en estado Y]
Severidad: [Error bloqueante | Advertencia | Informativa]
Mensaje: "[texto del mensaje al usuario, con {valores} interpolados si ayuda]"
```

### Ejemplo completo para "Periodo de baja"

| Regla | Campos | Condición | Cuándo aplica | Mensaje |
|-------|--------|-----------|--------------|---------|
| CR-001 | Fecha inicio, Fecha fin | Fecha fin >= Fecha inicio | Cuando ambas tienen valor | "La fecha de fin ({fin}) debe ser posterior a la de inicio ({inicio})" |
| CR-002 | Tipo de baja, Motivo | Si Tipo = IT, Motivo es obligatorio | Siempre que Tipo = IT | "Debe indicar el motivo para bajas por incapacidad temporal" |
| CR-003 | Fecha baja, Fecha incorporación | Fecha incorporación > Fecha baja | Cuando ambas tienen valor | "La fecha de incorporación debe ser posterior a la de baja" |
| CR-004 | Días, Fecha inicio, Fecha fin | Días = diferencia(fin, inicio) | Cuando ambas fechas tienen valor | Campo calculado automáticamente, no editable |

---

## Validaciones condicionales

Una validación condicional es aquella que solo se aplica bajo determinadas circunstancias. Usa la estructura SI/ENTONCES.

### Estructura básica

```
SI [condición previa]
ENTONCES [campo X] debe cumplir [regla]
```

### Ejemplos

```
SI tipo_persona = "JURIDICA"
ENTONCES campo CIF es obligatorio y campo NIF no aplica

SI forma_pago = "TRANSFERENCIA"
ENTONCES campo IBAN es obligatorio

SI nivel_descuento > 20%
ENTONCES el campo "Aprobado por" es obligatorio

SI alumno es menor de edad
ENTONCES el campo "Representante legal" es obligatorio

SI estado = "ENVIADO"
ENTONCES ningún campo es editable (formulario de solo lectura)

SI país = "EXTRANJERO"
ENTONCES el campo "Pasaporte" sustituye al campo "NIF" (NIF deja de ser obligatorio)
```

### Condiciones compuestas

```
SI tipo_contrato = "Tiempo Parcial" Y horas_mes > horas_contratadas
ENTONCES mostrar advertencia de superación de horas contratadas

SI estado IN ("APROBADO", "ARCHIVADO") Y rol_usuario != "ADMIN"
ENTONCES ningún campo es editable

SI importe_total > 10000 O descuento_porcentaje > 30%
ENTONCES campo "Aprobado por director" es obligatorio
```

---

## Tablas de decisión

Cuando hay múltiples condiciones que se combinan, las tablas de decisión son la herramienta más clara para documentar todas las combinaciones posibles.

### Cómo construirlas

1. Listar todas las condiciones (filas superiores)
2. Listar todas las acciones posibles (filas inferiores)
3. Cada columna es una regla con una combinación de condiciones
4. Rellenar cada celda: Sí/No para condiciones; marcado/en blanco para acciones

### Ejemplo: Validaciones según método de pago

| | R1 | R2 | R3 | R4 |
|----------------------------------|----|----|----|----|
| **Condiciones** | | | | |
| Forma de pago = Efectivo | Sí | No | No | No |
| Forma de pago = Tarjeta | No | Sí | No | No |
| Forma de pago = Transferencia | No | No | Sí | No |
| Forma de pago = Domiciliación | No | No | No | Sí |
| **Acciones** | | | | |
| IBAN es obligatorio | | | ✓ | ✓ |
| Número de tarjeta es obligatorio | | ✓ | | |
| Titular de tarjeta es obligatorio | | ✓ | | |
| Importe máximo: 500€ | ✓ | | | |
| Sin límite de importe | | ✓ | ✓ | ✓ |

### Ejemplo: Tipo de persona y documentación requerida

| | R1 | R2 | R3 |
|--------------------------------------|----|----|-----|
| **Condiciones** | | | |
| Tipo persona = Física española | Sí | No | No |
| Tipo persona = Física extranjera | No | Sí | No |
| Tipo persona = Jurídica | No | No | Sí |
| **Acciones** | | | |
| NIF obligatorio | ✓ | | |
| NIE o Pasaporte obligatorio | | ✓ | |
| CIF obligatorio | | | ✓ |
| Nombre y apellidos obligatorios | ✓ | ✓ | |
| Razón social obligatoria | | | ✓ |
| Representante legal obligatorio | | | ✓ |

---

## Efectos laterales de las validaciones condicionales

Al especificar una validación condicional, documentar también qué ocurre en el formulario:

| Efecto | Descripción | Ejemplo |
|--------|-------------|---------|
| **Mostrar/ocultar campo** | El campo solo es visible si se cumple la condición | El campo "CIF" solo se muestra si Tipo persona = Jurídica |
| **Habilitar/deshabilitar** | El campo existe pero no es editable si no se cumple la condición | El campo "Motivo rechazo" solo es editable para el rol Revisor |
| **Limpiar campo** | Al cambiar la condición, el campo dependiente se vacía | Al cambiar la Provincia, el campo Municipio se limpia |
| **Recalcular** | Al cambiar un campo, otro se recalcula automáticamente | Al cambiar la Fecha de fin, los Días se recalculan |
| **Cambiar dominio** | Al cambiar la condición, cambia la lista de valores del campo dependiente | Al cambiar el País, la lista de Provincias cambia |

### Cuándo limpiar automáticamente

Documentar explícitamente si el campo hijo debe limpiarse al cambiar el padre:
- Si el valor actual del hijo ya no es válido en el nuevo contexto → limpiar automáticamente
- Si el valor actual del hijo sigue siendo válido → mantener (no limpiar)
- Si hay ambigüedad → preguntar al usuario o mostrar advertencia
