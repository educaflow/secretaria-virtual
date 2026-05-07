---
name: k-validaciones/mensajes
description: Guía completa de mensajes de error — los 5 elementos de un buen mensaje, ejemplos malos vs. buenos por tipo, reglas de escritura, niveles de severidad, patrón de confirmación para advertencias, internacionalización
---

# Mensajes de Error y Validación

Los mensajes de error son la interfaz más directa entre el sistema y el usuario cuando algo va mal. Un mensaje bien escrito convierte un momento frustrante en una experiencia de aprendizaje. Un mensaje mal escrito genera confusión, llamadas al soporte y abandono del proceso.

---

## Los 5 elementos de un buen mensaje de error

1. **Qué fue mal**: descripción clara y específica del problema (qué campo, qué valor, qué regla)
2. **Por qué ocurrió**: si aporta valor y no es evidente (opcional pero recomendado para reglas de negocio)
3. **Cómo se corrige**: instrucciones concretas y accionables para el usuario
4. **Información de contexto**: incluir el valor problemático cuando ayuda a localizar el error
5. **Tono**: humano, empático, sin culpar al usuario

---

## Los 3 niveles de severidad

### Error bloqueante

La operación no puede continuar. El sistema impide guardar, enviar o avanzar hasta que se corrija.

Cuándo usar:
- Campos obligatorios vacíos
- Valores fuera de rango
- Formato incorrecto
- Violaciones de integridad referencial
- Reglas de negocio críticas

Visual habitual: mensaje en rojo junto al campo o en un banner superior.

### Advertencia (Soft warning)

El sistema detecta un posible problema pero permite continuar si el usuario confirma. El usuario puede estar en lo correcto.

Cuándo usar:
- Valores inusualmente altos o bajos (pero técnicamente válidos)
- Operaciones en fechas festivas o fuera de horario
- Registros similares existentes (posible duplicado)
- Superación de umbrales opcionales

Visual habitual: mensaje en amarillo o naranja con opciones "Continuar" y "Corregir".

### Informativa

El sistema muestra información o una recomendación sin bloquear ni pedir confirmación.

Cuándo usar:
- Recordatorios ("Esta información debe actualizarse trimestralmente")
- Contexto útil ("Este cliente no ha realizado pedidos en 6 meses")
- Ayuda contextual

Visual habitual: mensaje en azul o gris, o icono de información junto al campo.

---

## Ejemplos: malos vs. buenos mensajes

### Errores de presencia (campo vacío / obligatorio)

| Mal mensaje | Buen mensaje |
|-------------|-------------|
| "Campo obligatorio" | "Introduzca el nombre del solicitante" |
| "Error: campo vacío" | "El NIF es obligatorio para completar el registro" |
| "Requerido" | "Debe seleccionar al menos un método de notificación" |
| "Error en el formulario" | "El campo 'Fecha de inicio' es obligatorio" |
| "Datos incompletos" | "Faltan los siguientes campos obligatorios: Nombre, Apellidos, Email" |

### Errores de formato

| Mal mensaje | Buen mensaje |
|-------------|-------------|
| "Formato inválido" | "El email debe tener el formato usuario@dominio.com (por ejemplo: maria@empresa.es)" |
| "Error en NIF" | "El NIF '12345678' no es válido. Compruebe que la letra verificadora es correcta" |
| "Fecha incorrecta" | "La fecha debe tener el formato DD/MM/AAAA. Por ejemplo: 15/03/2024" |
| "Teléfono no válido" | "El teléfono debe tener 9 dígitos y empezar por 6, 7, 8 o 9" |
| "IBAN incorrecto" | "El IBAN introducido no tiene el formato correcto. Debe empezar por 'ES' seguido de 22 dígitos" |

### Errores de rango

| Mal mensaje | Buen mensaje |
|-------------|-------------|
| "Valor fuera de rango" | "La cantidad debe estar entre 1 y 999" |
| "Descuento no permitido" | "El descuento máximo permitido es del 30%. Ha introducido un 45%" |
| "Fecha no válida" | "La fecha de nacimiento no puede ser una fecha futura" |
| "Número demasiado grande" | "El importe no puede superar 99.999,99 €. Ha introducido {valor} €" |
| "Valor incorrecto" | "La nota debe estar entre 0 y 10. Ha introducido {valor}" |

### Errores cruzados (entre campos)

| Mal mensaje | Buen mensaje |
|-------------|-------------|
| "Error de consistencia" | "La fecha de fin (15/03/2024) no puede ser anterior a la fecha de inicio (20/03/2024)" |
| "Datos inconsistentes" | "El municipio 'Valencia' no pertenece a la provincia 'Madrid'. Seleccione el municipio correcto para la provincia elegida" |
| "Importe incorrecto" | "El importe pagado (1.500 €) no puede superar el importe total de la factura (1.200 €)" |
| "Error en el formulario" | "Si introduce una fecha de fin, también debe indicar una fecha de inicio" |

### Errores de negocio y estado

| Mal mensaje | Buen mensaje |
|-------------|-------------|
| "Operación no permitida" | "Este expediente no puede enviarse porque está en estado 'Rechazado'. Primero devuélvalo a borrador, realice las correcciones y vuelva a enviarlo" |
| "Sin permisos" | "Solo los usuarios con el rol 'Director' pueden aprobar solicitudes superiores a 10.000 €" |
| "Error de crédito" | "El cliente {nombre} ha superado su límite de crédito ({limite} €). El importe pendiente actual es {acumulado} €. Para continuar, solicite autorización al director" |
| "No disponible" | "No se puede crear un expediente de este tipo porque no hay ninguna plantilla de tramitación configurada. Contacte con el administrador del sistema" |

### Errores de unicidad

| Mal mensaje | Buen mensaje |
|-------------|-------------|
| "Registro duplicado" | "Ya existe un alumno registrado con el NIF '12345678Z'. ¿Desea ver su ficha?" |
| "Error de validación" | "El código '00042' ya está en uso. Introduzca un código diferente" |
| "Error en base de datos" | "Ya existe una matrícula para {nombre_alumno} en {asignatura} para el curso {curso}" |

---

## Reglas de escritura

### Hacer

- Usar lenguaje simple y directo, en el idioma del usuario
- Incluir el valor problemático: "Ha introducido 150; el máximo es 100"
- Dar instrucciones concretas: "Introduzca 8 dígitos seguidos de una letra"
- Nombrar el campo cuando el mensaje no está junto a él: "El campo 'Email'"
- Usar "debe" en lugar de "tiene que" o "es necesario"
- Incluir ejemplos de valores correctos cuando el formato no es obvio

### No hacer

- No usar palabras como "inválido", "incorrecto", "ilegal", "error", "fallo"
- No culpar al usuario: "Ha introducido mal..." → "El valor introducido..."
- No usar tecnicismos: "Violación de constraint", "null pointer", "FK violation"
- No usar mayúsculas en todo el mensaje (parece que el sistema grita)
- No poner signos de exclamación innecesarios
- No ser vago: "Algo fue mal" → especificar qué y en qué campo
- No incluir códigos de error técnicos visibles al usuario
- No concatenar mensajes con valores: usar plantillas con parámetros

### Tono empático

Estos recursos mejoran el tono sin sonar condescendiente:
- Usar "ha habido un problema" en lugar de "ha cometido un error"
- Empezar con el campo o el valor, no con "Error:" o "Atención:"
- "El teléfono debe tener 9 dígitos" mejor que "El teléfono que ha escrito no tiene 9 dígitos"

---

## Patrón de confirmación para advertencias

Cuando la validación es una advertencia (el usuario puede tener razón), documentar exactamente qué se pregunta y qué opciones se ofrecen:

```
Advertencia: "El descuento del {valor}% supera el límite estándar del 20%. 
              Los descuentos superiores al 20% requieren aprobación 
              del responsable."

Opciones:
  [Corregir el descuento]   → vuelve al formulario con el campo de descuento enfocado
  [Solicitar autorización]  → abre el flujo de aprobación
  [Cancelar]                → descarta los cambios sin guardar
```

```
Advertencia: "Ya existe un cliente con nombre similar: 'Empresa García S.L.'. 
              ¿Está seguro de que no es el mismo cliente?"

Opciones:
  [Ver el cliente existente]  → abre la ficha del cliente similar
  [Continuar creando nuevo]   → guarda el nuevo registro
  [Cancelar]                  → vuelve al formulario
```

```
Advertencia: "La fecha seleccionada (26/12/2024) es festivo nacional. 
              ¿Confirma que es la fecha correcta?"

Opciones:
  [Sí, confirmar]    → continúa con esa fecha
  [Cambiar fecha]    → vuelve al campo de fecha
```

---

## Mensajes para campos calculados

Los campos calculados pueden mostrar indicadores en lugar de mensajes de error:

```
Campo "Días de demora" con valor positivo (factura vencida):
  Mostrar en rojo: "Vencida hace {n} días"

Campo "Días de demora" con valor negativo (factura no vencida):
  Mostrar en verde: "Vence en {n} días"

Campo "Total pedido" cuando alguna línea tiene datos incompletos:
  Mostrar con icono de advertencia: "Pendiente de calcular (hay líneas incompletas)"
```

---

## Internacionalización de mensajes

Si la aplicación soporta múltiples idiomas, aplicar estas reglas:

**Usar parámetros nombrados, nunca concatenación:**
```
Correcto:   "La cantidad máxima es {max_cantidad}"
Incorrecto: "La cantidad máxima es " + max_cantidad
```

**Por qué importa:** en algunos idiomas, el valor puede aparecer al principio de la frase; al concatenar se pierde esa flexibilidad y el mensaje suena raro.

**Tener en cuenta el género gramatical:** en español, "el alumno seleccionado" / "la alumna seleccionada" son ambas correctas y dependen del género de la persona. Documentar si hay mensajes con género gramatical variable.

**Tener en cuenta el plural:** "1 documento" / "2 documentos". Documentar los mensajes con cantidades variables para que puedan pluralizarse correctamente en todos los idiomas.

```
Correcto:   "{n} documentos adjuntos" (con lógica de pluralización)
Incorrecto: "Adjuntados " + n + " documento(s)"
```
