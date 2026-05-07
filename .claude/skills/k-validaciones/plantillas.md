---
name: k-validaciones/plantillas
description: Plantillas de documentación de validaciones — ficha de campo, tabla resumen de entidad, formato de regla de negocio, Given-When-Then, diccionario de datos completo, matriz de validaciones cruzadas
---

# Plantillas de Documentación

Plantillas listas para usar en la documentación de análisis funcional. Copiar y rellenar para cada entidad o campo.

---

## Plantilla 1: Ficha de campo

Para cada campo de una entidad, rellenar esta ficha. Es el nivel más detallado de documentación.

```
=== CAMPO: [nombre_campo] ===

Entidad:                [nombre de la entidad a la que pertenece]
Etiqueta en UI:         [cómo aparece en pantalla para el usuario]
Tipo de dato:           [texto | número entero | número decimal | fecha | 
                         fecha y hora | booleano | lista cerrada | lista abierta | 
                         referencia a {entidad}]

OBLIGATORIEDAD
  Obligatorio:          [Siempre | Nunca | Condicional]
  Condición (si aplica): [Descripción de la condición que lo hace obligatorio]

TIPO Y FORMATO
  Longitud mínima:      [número de caracteres mínimo, o "-" si no aplica]
  Longitud máxima:      [número de caracteres máximo, o "-" si no aplica]
  Formato/patrón:       [descripción del patrón, o "-" si no aplica]
  Caracteres permitidos: [descripción, o "todos" si no hay restricción]
  Dígito de control:    [algoritmo o estándar, o "-" si no aplica]

DOMINIO / RANGO
  Tipo de dominio:      [libre | enumerado cerrado | enumerado abierto | rango | referencia a {entidad}]
  Valores / Rango:      [lista de valores si es enumerado; "mín a máx" si es rango]
  Decimales:            [número de decimales permitidos, o "-" si no aplica]
  Negativos:            [Sí | No | No aplica]
  Si es fecha:          [¿puede ser pasada? Sí/No; ¿puede ser futura? Sí/No]

EDICIÓN
  Editable:             [Siempre | Nunca | Condicional: {descripción}]
  Solo lectura cuando:  [descripción de cuándo no es editable]
  Visible:              [Siempre | Nunca | Condicional: {descripción}]

CÁLCULO (rellenar solo si el campo se calcula)
  Tipo de cálculo:      [En tiempo real | Al guardar | Derivado]
  Fórmula:              [descripción de la regla de cálculo]
  Campos de los que depende: [lista de campos]
  Cuándo se recalcula:  [descripción del trigger]

VALIDACIONES
  V-001: [descripción de la regla]
         Severidad: [Error | Advertencia | Info]
         Mensaje:   "[texto exacto del mensaje al usuario]"
  V-002: [descripción de la regla]
         Severidad: [Error | Advertencia | Info]
         Mensaje:   "[texto exacto del mensaje al usuario]"

DEPENDENCIAS CON OTROS CAMPOS
  Este campo hace obligatorio a: [lista de campos que dependen de este]
  Este campo filtra el dominio de: [lista de campos cuyo dominio depende de este]
  Al cambiar este campo, se limpia: [lista de campos que se limpian]
  Al cambiar este campo, se recalcula: [lista de campos que se recalculan]
```

---

## Plantilla 2: Tabla resumen de validaciones de una entidad

Vista de alto nivel de todas las validaciones de una entidad. Útil como checklist durante el análisis y como resumen ejecutivo.

| ID | Campo(s) | Tipo | Condición de aplicación | Severidad | Mensaje (resumido) |
|----|----------|------|------------------------|-----------|-------------------|
| V-001 | email | Formato | Siempre | Error | "El email debe tener formato usuario@dominio.com" |
| V-002 | nif | Formato + dígito control | Siempre | Error | "El NIF no es válido. Compruebe la letra verificadora" |
| V-003 | fecha_nacimiento | Rango fechas | Siempre | Error | "La fecha de nacimiento no puede ser futura" |
| V-004 | fecha_fin | Cruzada con fecha_inicio | Cuando fecha_inicio tiene valor | Error | "La fecha de fin debe ser posterior a la de inicio" |
| V-005 | descuento_porcentaje | Rango numérico | Siempre | Error | "El descuento debe estar entre 0% y 100%" |
| V-006 | descuento_porcentaje | Regla de negocio | descuento > 20% | Advertencia | "Descuento superior al 20% requiere aprobación" |
| V-007 | nif | Unicidad | Al guardar | Error | "Ya existe una persona con el NIF {valor}" |
| V-008 | — (entidad completa) | Cardinalidad | Al cambiar a "Enviado" | Error | "Debe adjuntar al menos un documento" |

---

## Plantilla 3: Formato de regla de negocio

Para reglas de negocio complejas (nivel 5 de la taxonomía) que van más allá de la validación de campo.

```
ID:             RN-[número]
Nombre:         [nombre descriptivo corto]
Tipo:           [Restricción | Acción automática | Inferencia | Cálculo]

Descripción:
  [Descripción en lenguaje natural de la regla]

Condición:
  SI [descripción de la condición que activa la regla]

Consecuencia:
  ENTONCES [descripción de lo que el sistema debe hacer o verificar]

Excepciones:
  [Casos en que la regla NO aplica, o "-" si no hay excepciones]

Severidad:      [Bloqueante | Advertencia | Informativa | Automática (no visible)]

Mensaje al usuario:
  "[Texto exacto del mensaje, con {parámetros} si aplica]"

Opciones que se ofrecen al usuario (si es advertencia):
  [Opción 1] → [qué ocurre]
  [Opción 2] → [qué ocurre]

Origen / Motivo:
  [Norma, política o razón de negocio que origina esta regla]
```

### Ejemplo completo

```
ID:             RN-015
Nombre:         Límite de crédito por cliente
Tipo:           Restricción

Descripción:
  El importe acumulado de pedidos pendientes de cobro de un cliente
  no puede superar su límite de crédito asignado.

Condición:
  SI (suma de importe de pedidos pendientes de cobro del cliente)
     + (importe del nuevo pedido que se está creando)
     > límite_crédito del cliente

Consecuencia:
  ENTONCES mostrar advertencia y requerir autorización para continuar

Excepciones:
  No aplica a clientes con categoría "Platinum" (tienen crédito ilimitado)
  No aplica si el pedido es una devolución (importe negativo)

Severidad:      Advertencia (con posibilidad de continuar con autorización)

Mensaje al usuario:
  "El cliente {nombre_cliente} ha alcanzado su límite de crédito 
   ({limite_credito} €). El importe pendiente actual es {acumulado} €. 
   Si continúa, se superará el límite en {diferencia} €."

Opciones:
  [Solicitar autorización al director] → abre flujo de aprobación
  [Corregir el pedido]                 → vuelve al formulario del pedido
  [Cancelar]                           → descarta el pedido

Origen:
  Política de riesgos financieros v2.3, artículo 15. 
  Revisada en comité de dirección de enero de 2024.
```

---

## Plantilla 4: Given-When-Then (para casos de prueba)

El formato Given-When-Then (del BDD - Behavior Driven Development) es especialmente útil para convertir las validaciones directamente en casos de prueba. Cada validación puede dar lugar a varios Given-When-Then.

```
Escenario: [nombre del escenario]

DADO QUE  [estado inicial / contexto del sistema y del formulario]
CUANDO    [acción que realiza el usuario]
ENTONCES  [resultado esperado del sistema]
```

### Ejemplos

```
Escenario: Campo obligatorio vacío al enviar

DADO QUE  el campo "Nombre" está vacío
          Y el formulario de creación de alumno está abierto
CUANDO    el usuario pulsa el botón "Guardar"
ENTONCES  el sistema no guarda el registro
          Y muestra el mensaje "Introduzca el nombre del alumno" 
            junto al campo "Nombre"
          Y el campo "Nombre" queda resaltado
```

```
Escenario: Fecha de fin anterior a fecha de inicio

DADO QUE  el campo "Fecha de inicio" tiene el valor "15/03/2024"
          Y el campo "Fecha de fin" tiene el valor "10/03/2024"
CUANDO    el usuario pulsa "Guardar"
ENTONCES  el sistema no guarda el registro
          Y muestra el mensaje "La fecha de fin (10/03/2024) no puede 
            ser anterior a la fecha de inicio (15/03/2024)"
```

```
Escenario: Descuento supera el umbral de advertencia

DADO QUE  el campo "Descuento" tiene el valor "25%"
          Y el umbral de advertencia está configurado en "20%"
CUANDO    el usuario pulsa "Guardar"
ENTONCES  el sistema muestra una advertencia:
            "El descuento del 25% supera el límite estándar del 20%.
             Los descuentos superiores al 20% requieren aprobación."
          Y ofrece las opciones: [Solicitar autorización] y [Corregir el descuento]
          Y NO guarda hasta que el usuario elija una opción
```

```
Escenario: Transición no permitida desde estado final

DADO QUE  el expediente tiene el estado "Archivado"
CUANDO    el usuario intenta pulsar el botón "Editar"
ENTONCES  el sistema no muestra el botón "Editar" 
          (o lo muestra deshabilitado con tooltip: 
           "Los expedientes archivados no pueden modificarse")
```

---

## Plantilla 5: Diccionario de datos con validaciones

Una tabla por entidad que documenta todos los campos con sus reglas en formato compacto. Útil para revisiones rápidas y para compartir con el equipo de desarrollo.

| Campo | Tipo | Long | Oblig. | Dominio / Rango | Formato | Editable | Calculado | Notas de validación |
|-------|------|------|--------|-----------------|---------|----------|-----------|---------------------|
| nif | Texto | 9 | Sí | — | 8 dígitos + letra (módulo 23) | Sí | No | Único global; dígito de control |
| nombre | Texto | 1-100 | Sí | — | Letras, espacios, guion, apóstrofe | Sí | No | — |
| apellidos | Texto | 1-200 | Sí | — | Letras, espacios, guion, apóstrofe | Sí | No | — |
| fecha_nacimiento | Fecha | — | Sí | 01/01/1900 a hoy | DD/MM/AAAA | Sí | No | No puede ser futura |
| edad | Entero | — | No | 0-150 | — | No | Sí: hoy - fecha_nacimiento | Se recalcula al abrir |
| email | Texto | 5-254 | Cond. | — | usuario@dominio.ext | Sí | No | Obligatorio si teléfono vacío |
| telefono | Texto | 9 | Cond. | — | 9 dígitos, empieza por 6/7/8/9 | Sí | No | Obligatorio si email vacío |
| estado | Lista | — | Sí | BOZ, PEN, APR, REC, ARC | — | Solo via transiciones | No | Ver diagrama de estados |
| fecha_alta | Fecha | — | Sí | Hoy en adelante | DD/MM/AAAA | No | Sí: al crear | Solo lectura |
| observaciones | Texto | 0-2000 | No | — | Texto libre | Sí | No | — |

---

## Plantilla 6: Matriz de validaciones cruzadas

Para visualizar todas las dependencias entre campos de una entidad. En la intersección se indica el tipo de dependencia.

|  | fecha_inicio | fecha_fin | tipo_contrato | horas_semana | centro |
|--|-------------|-----------|---------------|-------------|--------|
| **fecha_inicio** | — | fecha_fin >= fecha_inicio | — | — | — |
| **fecha_fin** | obliga a fecha_inicio | — | — | — | — |
| **tipo_contrato** | — | — | — | tipo=PARCIAL obliga horas | tipo determina centros válidos |
| **horas_semana** | — | — | — | — | — |
| **centro** | — | — | — | — | — |

Leyenda de tipos de dependencia:
- `A >= B` → consistencia temporal o numérica
- `A obliga a B` → si A tiene valor, B es obligatorio
- `A determina dominio de B` → los valores válidos de B dependen de A
- `A calcula B` → el valor de B se calcula a partir de A
