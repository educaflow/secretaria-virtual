---
name: k-validaciones/estado
description: Validaciones de estado y ciclo de vida — diagrama de estados, transiciones válidas, condiciones por transición, campos editables por estado, validaciones propias de cada estado, borrador relajado vs. envío estricto
---

# Estados y Ciclo de Vida

La mayoría de las entidades empresariales tienen un ciclo de vida: pasan por diferentes estados con transiciones permitidas y restringidas. Este es uno de los aspectos más críticos y más frecuentemente mal especificados en el análisis funcional.

---

## Los 8 elementos a documentar

Para cualquier entidad con estados, el análisis funcional debe responder:

1. **Lista de estados** con descripción de cada uno
2. **Estado inicial** al crear el registro
3. **Estados finales** de los que no se puede salir
4. **Transiciones permitidas**: de qué estado a qué estado
5. **Condiciones** para que cada transición sea válida
6. **Acciones desencadenadas** por cada transición
7. **Quién puede ejecutar** cada transición (rol de usuario)
8. **Qué campos son editables** en cada estado

---

## Diagrama de estados (notación ASCII)

Representar visualmente el ciclo de vida antes de hacer las tablas. Facilita detectar estados y transiciones que faltan.

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

Convenciones:
- `[ESTADO]` entre corchetes indica estado
- `──nombre──►` indica transición con su nombre
- Los estados finales son aquellos de los que no salen flechas hacia otros estados

---

## Tabla de estados

| Código | Etiqueta | Descripción | Inicial | Final |
|--------|----------|-------------|---------|-------|
| BOZ | Borrador | En redacción por el solicitante | Sí | No |
| PEN | Pendiente de revisión | Enviado, esperando revisión | No | No |
| APR | Aprobado | Revisado y aprobado | No | No |
| REC | Rechazado | Revisado y rechazado | No | No |
| ARC | Archivado | Proceso finalizado | No | Sí |

---

## Tabla de transiciones

| De estado | Transición | A estado | Condiciones previas | Rol que puede ejecutarla | Acción posterior |
|-----------|-----------|---------|-------------------|--------------------------|-----------------|
| BORRADOR | Enviar | PENDIENTE | Todos los campos obligatorios rellenos + al menos 1 documento adjunto | Solicitante | Notificación email al revisor |
| PENDIENTE | Aprobar | APROBADO | Ninguna | Revisor, Director | Notificación email al solicitante |
| PENDIENTE | Rechazar | RECHAZADO | Campo "Motivo de rechazo" debe estar relleno | Revisor | Notificación email al solicitante |
| PENDIENTE | Devolver | BORRADOR | Campo "Motivo de devolución" debe estar relleno | Revisor | Notificación email al solicitante |
| APROBADO | Archivar | ARCHIVADO | Ninguna | Admin, Director | — |
| RECHAZADO | Archivar | ARCHIVADO | Ninguna | Admin, Director | — |

### Formato de documentación de una transición

```
Transición: [nombre_transición]
De estado: [estado_origen]
A estado: [estado_destino]
Condiciones previas:
  - [condición 1]
  - [condición 2]
Rol que puede ejecutarla: [lista de roles]
Mensaje de error si las condiciones no se cumplen: "[texto]"
Acción posterior: [qué pasa automáticamente al ejecutar la transición]
```

---

## Tabla de campos editables por estado

Documentar para cada campo en qué estados es editable, de solo lectura o no visible.

| Campo | BORRADOR | PENDIENTE | APROBADO | RECHAZADO | ARCHIVADO |
|-------|---------|-----------|---------|----------|----------|
| Título | Editable | Solo lectura | Solo lectura | Solo lectura | Solo lectura |
| Descripción | Editable | Solo lectura | Solo lectura | Solo lectura | Solo lectura |
| Documentos | Añadir/eliminar | Solo añadir | Solo lectura | Solo lectura | Solo lectura |
| Motivo de rechazo | No visible | Editable (revisor) | No visible | Solo lectura | Solo lectura |
| Motivo de devolución | No visible | Editable (revisor) | No visible | No visible | No visible |
| Fecha de resolución | No visible | No visible | Calculada automát. | Calculada automát. | Solo lectura |

Leyenda:
- **Editable**: el usuario puede ver y modificar el campo
- **Solo lectura**: el campo se muestra pero no se puede modificar
- **No visible**: el campo no aparece en el formulario
- **Solo añadir**: se pueden añadir nuevos registros relacionados pero no eliminar los existentes
- **Calculada automát.**: el sistema calcula el valor y el usuario no puede editarlo

---

## Validaciones propias de cada estado

### El patrón "borrador relajado / envío estricto"

Este es el patrón más común: durante la edición (estado borrador), el sistema permite guardar registros incompletos. Solo cuando el usuario intenta avanzar al siguiente estado se verifican todas las reglas de completitud.

| Momento | Qué se valida |
|---------|--------------|
| **Al guardar en borrador** | Solo los campos con información ya introducida (no hay errores de formato, rango, etc.) pero no se exige que todos los obligatorios estén rellenos |
| **Al intentar enviar (transición)** | Todos los campos obligatorios deben estar rellenos + todas las validaciones cruzadas + cardinalidad mínima de documentos |
| **Al aprobar/rechazar** | Las condiciones específicas de esa transición (ej: motivo de rechazo obligatorio) |

### Documentar validaciones adicionales por estado

Algunos estados tienen validaciones que no aplican en otros:

```
Estado: PENDIENTE
Validaciones adicionales:
  - El revisor asignado no puede ser el mismo que el solicitante
  - La fecha de revisión no puede ser anterior a la fecha de envío

Estado: APROBADO  
Validaciones adicionales:
  - Debe existir al menos una firma electrónica registrada

Estado: ARCHIVADO
Validaciones adicionales:
  - Ningún documento adjunto puede estar en estado "pendiente de firma"
```

---

## Transiciones inválidas

Documentar explícitamente qué transiciones NO están permitidas y el mensaje que se muestra:

```
De APROBADO no se puede volver a BORRADOR
Mensaje: "Este expediente ya ha sido aprobado y no puede volver 
          a estado borrador. Si necesita modificarlo, contacte 
          con el administrador."

De ARCHIVADO no se puede realizar ninguna transición
Mensaje: "Este expediente está archivado y no puede modificarse."
```

---

## Acciones automáticas en las transiciones

Documentar qué ocurre automáticamente cuando se ejecuta una transición (sin intervención del usuario):

| Transición | Acciones automáticas |
|-----------|---------------------|
| Borrador → Pendiente | Registrar fecha y hora de envío; notificar al revisor por email; asignar número de expediente definitivo |
| Pendiente → Aprobado | Registrar fecha de resolución; notificar al solicitante; generar documento PDF de resolución |
| Pendiente → Rechazado | Registrar fecha de resolución; notificar al solicitante con el motivo |
| Pendiente → Borrador | Notificar al solicitante con el motivo de devolución |
| Cualquier → Archivado | Marcar todos los documentos como "definitivos" (no modificables) |

---

## Ejemplo completo: Solicitud de permiso

### Diagrama

```
[BORRADOR] ──enviar──► [PENDIENTE] ──aprobar──► [APROBADO] ──archivar──► [ARCHIVADO]
    ▲                       │                                                  ▲
    └────devolver───────────┤                                                  │
                            └──rechazar──► [RECHAZADO] ──archivar─────────────┘
```

### Estados

| Estado | Descripción |
|--------|-------------|
| BORRADOR | El solicitante está preparando la solicitud |
| PENDIENTE | La solicitud está siendo revisada por RRHH |
| APROBADO | La solicitud ha sido aprobada |
| RECHAZADO | La solicitud ha sido denegada |
| ARCHIVADO | El proceso ha concluido (estado final) |

### Campos editables

| Campo | BOZ | PEN | APR | REC | ARC |
|-------|-----|-----|-----|-----|-----|
| Tipo de permiso | E | R | R | R | R |
| Fecha de inicio | E | R | R | R | R |
| Fecha de fin | E | R | R | R | R |
| Justificación | E | R | R | R | R |
| Documentación | E+/- | E+ | R | R | R |
| Motivo denegación | N | E(RRHH) | N | R | R |
| Fecha resolución | N | N | Auto | Auto | R |

**Leyenda:** E=Editable, R=Solo lectura, N=No visible, E+=Solo añadir, Auto=Calculado, (RRHH)=Solo rol RRHH
