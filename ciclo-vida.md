---
name: k-validaciones/ciclo-vida
description: Ciclo de vida y campos calculados — Parte A estados, transiciones, condiciones, campos editables por estado y validaciones propias del estado. Parte B campos calculados (tiempo real, al guardar, derivados) con plantilla, dependencias y circularidad.
---

# Ciclo de Vida y Campos Calculados

## Parte A — Estados y transiciones

La mayoría de las entidades empresariales tienen un ciclo de vida: pasan por diferentes estados con transiciones permitidas y restringidas. Es uno de los aspectos más críticos y más frecuentemente mal especificados en el análisis funcional.

### Los 8 elementos a documentar

Para cualquier entidad con estados, el análisis debe responder:

1. **Lista de estados** con descripción de cada uno.
2. **Estado inicial** al crear el registro.
3. **Estados finales** de los que no se puede salir.
4. **Transiciones permitidas**: de qué estado a qué estado.
5. **Condiciones** para que cada transición sea válida.
6. **Acciones desencadenadas** por cada transición.
7. **Quién puede ejecutar** cada transición (rol).
8. **Qué campos son editables** en cada estado.

### Diagrama de estados (notación ASCII)

Representar visualmente antes de hacer las tablas. Facilita detectar estados y transiciones que faltan.

```
[BORRADOR] ──enviar──────────────► [PENDIENTE DE REVISIÓN]
     ▲                                    │
     │                              ┌─────┴──────┐
     └──devolver──────────────────── │            │
                                  aprobar      rechazar
                                    │            │
                                    ▼            ▼
                             [APROBADO]      [RECHAZADO]
                                    │            │
                              archivar        archivar
                                    └─────┬──────┘
                                          ▼
                                     [ARCHIVADO]
```

Convenciones: `[ESTADO]` entre corchetes, `──nombre──►` para transiciones. Estados finales: aquellos sin flechas salientes hacia otros estados.

### Tabla de estados

| Código | Etiqueta | Descripción | Inicial | Final |
|--------|----------|-------------|---------|-------|
| BOZ | Borrador | En redacción por el solicitante | Sí | No |
| PEN | Pendiente de revisión | Enviado, esperando revisión | No | No |
| APR | Aprobado | Revisado y aprobado | No | No |
| REC | Rechazado | Revisado y rechazado | No | No |
| ARC | Archivado | Proceso finalizado | No | Sí |

### Tabla de transiciones

| De | Transición | A | Condiciones previas | Rol | Acción posterior |
|----|-----------|---|--------------------|-----|-----------------|
| BORRADOR | Enviar | PENDIENTE | Todos los obligatorios + ≥1 documento adjunto | Solicitante | Email al revisor |
| PENDIENTE | Aprobar | APROBADO | — | Revisor, Director | Email al solicitante |
| PENDIENTE | Rechazar | RECHAZADO | "Motivo de rechazo" relleno | Revisor | Email al solicitante |
| PENDIENTE | Devolver | BORRADOR | "Motivo de devolución" relleno | Revisor | Email al solicitante |
| APROBADO | Archivar | ARCHIVADO | — | Admin, Director | — |
| RECHAZADO | Archivar | ARCHIVADO | — | Admin, Director | — |

**Formato de documentación de una transición:**
```
Transición: [nombre]
De: [estado_origen]   A: [estado_destino]
Condiciones previas:
  - [condición 1]
Rol: [lista de roles]
Mensaje si falla: "[texto]"
Acción posterior: [qué pasa automáticamente]
```

### Campos editables por estado

Para cada campo, documentar en qué estados es editable, solo lectura o no visible.

| Campo | BOZ | PEN | APR | REC | ARC |
|-------|-----|-----|-----|-----|-----|
| Título | E | R | R | R | R |
| Descripción | E | R | R | R | R |
| Documentos | E± | E+ | R | R | R |
| Motivo de rechazo | N | E (revisor) | N | R | R |
| Motivo de devolución | N | E (revisor) | N | N | N |
| Fecha de resolución | N | N | Auto | Auto | R |

**Leyenda:** E = Editable, R = Solo lectura, N = No visible, E+ = Solo añadir, E± = Añadir/eliminar, Auto = Calculado.

### Validaciones propias del estado: borrador relajado / envío estricto

Patrón más común: durante la edición (borrador), el sistema permite guardar registros incompletos. Solo al avanzar de estado se verifica completitud.

| Momento | Qué se valida |
|---------|---------------|
| Guardar en borrador | Solo lo introducido (formato, rango...). No se exige completitud. |
| Intentar enviar | Todos los obligatorios + cruzadas + cardinalidad mínima de documentos. |
| Aprobar/rechazar | Condiciones específicas de la transición (ej. motivo de rechazo). |

**Validaciones adicionales por estado** (más allá de la completitud):
```
Estado PENDIENTE:
  - El revisor asignado no puede ser el mismo que el solicitante.
  - La fecha de revisión no puede ser anterior a la fecha de envío.

Estado APROBADO:
  - Debe existir al menos una firma electrónica registrada.

Estado ARCHIVADO:
  - Ningún documento puede estar en "pendiente de firma".
```

### Transiciones inválidas

Documentar explícitamente qué transiciones NO se permiten y el mensaje:

```
De APROBADO no se puede volver a BORRADOR.
  "Este expediente ya ha sido aprobado y no puede volver a borrador.
   Si necesita modificarlo, contacte con el administrador."

De ARCHIVADO no se puede realizar ninguna transición.
  "Este expediente está archivado y no puede modificarse."
```

### Acciones automáticas en las transiciones

| Transición | Acciones automáticas |
|-----------|---------------------|
| Borrador → Pendiente | Registrar fecha y hora de envío; notificar al revisor; asignar número definitivo. |
| Pendiente → Aprobado | Registrar fecha de resolución; notificar al solicitante; generar PDF de resolución. |
| Pendiente → Rechazado | Registrar fecha de resolución; notificar al solicitante con motivo. |
| Pendiente → Borrador | Notificar al solicitante con el motivo de devolución. |
| Cualquier → Archivado | Marcar todos los documentos como "definitivos". |

---

## Parte B — Campos calculados

Un campo calculado obtiene su valor automáticamente a partir de otros campos. El usuario no lo introduce directamente (aunque a veces puede editarlo).

### Tipos de cálculo

| Tipo | Cuándo | Editable | Casos típicos |
|------|--------|----------|---------------|
| **Tiempo real** | Cada vez que cambian sus dependencias | No | Totales, subtotales, edad, días entre fechas |
| **Al guardar (triggered)** | Al guardar o al ejecutar una acción | A veces | Número de expediente, fecha modificación, hash documento |
| **Derivado** | Al consultarse; depende del estado del sistema | No | Estado de morosidad, situación académica, nivel de riesgo |

### Plantilla de documentación

```
Campo: [nombre del campo]
Etiqueta UI: [cómo aparece en pantalla]
Tipo: [Tiempo real | Al guardar | Derivado]

Fórmula:
  [descripción precisa de cómo se calcula]

Dependencias:
  - [campo_1]: [cómo se usa]
  - [campo_2]: [cómo se usa]

Redondeo: [al entero | 2 decimales | truncar | sin redondeo | no aplica]
Cuándo se recalcula: [trigger específico]
Editable por el usuario: [No | Sí | Solo si {condición}]
Valor cuando dependencias vacías: [0 | null | "--" | no mostrar]
```

### Ejemplos representativos

**Ejemplo 1: cálculo en tiempo real (línea de pedido)**
```
Campo: importe_linea
Etiqueta UI: Importe
Tipo: Tiempo real

Fórmula:
  importe_linea = precio_unitario × cantidad × (1 - descuento_porcentaje / 100)

Dependencias:
  - precio_unitario, cantidad, descuento_porcentaje

Redondeo: 2 decimales (≥ 0.005 al alza)
Recalcula: Cada cambio de cualquier dependencia
Editable: No
Valor en vacío: 0,00 €
```

**Ejemplo 2: cálculo derivado (depende del sistema y del tiempo)**
```
Campo: dias_demora
Etiqueta UI: Días de demora
Tipo: Derivado

Fórmula:
  SI factura pendiente:
    dias_demora = días entre fecha_vencimiento y HOY
    (positivo = vencida; negativo = no vencida)
  SI factura cobrada:
    dias_demora = días entre fecha_vencimiento y fecha_cobro

Dependencias:
  - fecha_vencimiento, fecha_cobro (si existe), fecha actual del sistema

Redondeo: Entero (días completos)
Recalcula: Cada vez que se abre el registro (depende de la fecha actual)
Editable: No

Nota: Cambia de valor cada día aunque el usuario no toque el registro.
      Documentarlo para que no genere confusión.
```

**Patrones habituales (sin desarrollar):**
- Total de pedido = Σ importe_linea + gastos_envio + IVA (tiempo real)
- Número de expediente = AÑO/SECUENCIA/CENTRO (al guardar, una sola vez)
- Edad = años entre fecha_nacimiento y hoy (derivado)
- Situación académica = clasificación según % asignaturas superadas (derivado)

### Dependencias entre campos calculados

Los campos calculados pueden depender de otros campos calculados. Documentar la cadena:

```
precio_unitario ──┐
cantidad ─────────┼─► importe_linea ──┐
descuento_pct ────┘                    ├─► total_pedido
                  gastos_envio ────────┤
                  tipo_iva_general ────┘
```

### Dependencias circulares

Una dependencia circular ocurre cuando A depende de B y B depende de A (directa o indirectamente). Es un error de diseño que debe detectarse en el análisis.

**Ejemplo incorrecto:**
```
precio_final = precio_base - descuento_euros
descuento_euros = precio_final × porcentaje_descuento / 100
```
`precio_final` depende de `descuento_euros` y viceversa. No es calculable sin un valor de partida.

**Cómo resolverlo:** identificar el campo origen (lo introduce el usuario) y el resultado, luego redefinir:
```
descuento_euros = precio_base × porcentaje_descuento / 100
precio_final    = precio_base - descuento_euros
```

### Cuándo un campo calculado puede editarse manualmente

Algunos campos tienen valor por defecto calculado pero el usuario puede sobreescribirlos. Documentar exactamente cuándo:

```
Campo: fecha_entrega_prevista
Calculado por defecto: fecha_pedido + plazo_tipo_producto días
Editable: Sí, el usuario puede cambiar la fecha propuesta
Al modificarla: Marcar como "Fecha personalizada"
Si se borra el valor manual: Restaurar el valor calculado
```
