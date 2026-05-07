---
name: k-validaciones/integridad
description: Validaciones de integridad — unicidad (claves, ámbitos), integridad referencial (comportamientos en cascada), cardinalidad mínima/máxima, y registros maestros necesarios antes de operar
---

# Validaciones de Integridad

Las validaciones de integridad verifican la coherencia del registro respecto al sistema en su conjunto. A diferencia de las validaciones de campo, estas requieren consultar la base de datos para verificarse.

---

## 3A. Unicidad

Un campo (o combinación de campos) no puede repetirse dentro de un ámbito.

### Qué documentar

```
Regla UI-[número]
Entidad: [nombre de la entidad]
Clave de unicidad: [campo o combinación de campos]
Ámbito: [global | por centro | por año | por tipo | etc.]
Mensaje: "[texto del mensaje, incluyendo el valor duplicado]"
```

### Ámbitos frecuentes

| Ámbito | Descripción | Ejemplo |
|--------|-------------|---------|
| Global | Único en todo el sistema | NIF de una persona física |
| Por organización | Único dentro de la misma empresa/centro | Número de empleado por empresa |
| Por año | Único dentro del mismo año natural | Número de expediente por año y centro |
| Por combinación | Único para una combinación de valores | Un alumno no puede matricularse dos veces en la misma asignatura y curso |
| Por tipo | Único dentro del mismo tipo de registro | Código de producto único por categoría |

### Ejemplos documentados

```
Regla UI-001
Entidad: Persona
Clave de unicidad: NIF
Ámbito: Global
Mensaje: "Ya existe una persona registrada con el NIF {valor}. 
          Verifique que no está duplicando el registro."
```

```
Regla UI-002
Entidad: Expediente
Clave de unicidad: número_expediente + año + centro
Ámbito: Global (la combinación de los tres es única)
Mensaje: "Ya existe un expediente con el número {número} en el año {año} 
          para el centro {centro}."
```

```
Regla UI-003
Entidad: Matrícula
Clave de unicidad: alumno + asignatura + curso_académico
Ámbito: Global
Mensaje: "El alumno {nombre} ya está matriculado en {asignatura} 
          para el curso {curso}."
```

### Cuándo verificar la unicidad

La unicidad solo puede verificarse al intentar guardar (no en tiempo real mientras el usuario escribe), porque requiere consultar la base de datos. Indicarlo en la especificación para que el diseño de la UI lo tenga en cuenta.

---

## 3B. Integridad referencial

Cuando un campo referencia a otra entidad, esa referencia debe existir.

### Comportamientos al borrar el registro padre

| Comportamiento | Descripción | Cuándo usarlo |
|----------------|-------------|---------------|
| **RESTRICT** | No se puede borrar si tiene hijos. El sistema bloquea la operación. | Cuando los hijos no tienen sentido sin el padre (un expediente sin tipo de expediente) |
| **CASCADE** | Al borrar el padre, se borran todos sus hijos automáticamente. | Cuando los hijos son "propiedad" del padre y no tienen vida propia (las líneas de un pedido) |
| **SET NULL** | Al borrar el padre, la referencia en los hijos se pone a null/vacío. | Cuando el hijo puede existir sin padre (un documento cuyo redactor ha sido dado de baja) |

### Formato de documentación

```
Regla RI-[número]
Campo: [entidad].[campo_referencia]
Referencia a: [entidad_padre]
Filtro de valores válidos: [si solo son válidos algunos registros de la entidad padre]
Comportamiento al borrar el padre: [RESTRICT | CASCADE | SET NULL]
Mensaje al intentar borrar con hijos: "[texto del mensaje]"
```

### Ejemplos

```
Regla RI-001
Campo: Expediente.tipo_expediente
Referencia a: TipoExpediente (solo los activos)
Comportamiento al borrar: RESTRICT — no se puede eliminar un TipoExpediente 
                          que tenga expedientes asociados
Mensaje: "No se puede eliminar el tipo de expediente '{nombre}' porque 
          tiene {n} expedientes asociados. Desactive el tipo de expediente 
          en lugar de eliminarlo."
```

```
Regla RI-002
Campo: Alumno.tutor
Referencia a: Tutor
Comportamiento al borrar: RESTRICT — no se puede eliminar un Tutor con alumnos asignados
Mensaje: "No se puede eliminar el tutor '{nombre}' porque tiene {n} alumnos 
          asignados. Reasigne los alumnos antes de eliminar al tutor."
```

```
Regla RI-003
Campo: Documento.autor
Referencia a: Usuario
Comportamiento al borrar: SET NULL — si se elimina el usuario, el documento 
                          queda sin autor pero no se borra
Mensaje: (no aplica; la acción se permite automáticamente)
```

### Filtros en las referencias

A veces no todos los registros de la entidad padre son valores válidos. Documentar el filtro:
- "Solo alumnos con estado 'Activo'"
- "Solo centros del mismo grupo educativo"
- "Solo asignaturas del curso académico en vigor"
- "Solo usuarios con rol 'Profesor'"

---

## 3C. Cardinalidad

La cardinalidad define cuántos registros relacionados puede o debe tener un registro.

### Tipos de cardinalidad

| Notación | Significado | Ejemplo |
|----------|-------------|---------|
| `0..1` | Cero o uno: la relación es opcional y única | Un empleado puede tener o no un coche de empresa (máximo 1) |
| `1` | Exactamente uno: siempre debe existir exactamente un registro relacionado | Un expediente debe tener exactamente un solicitante |
| `0..*` | Cero o más: relación opcional sin límite | Un cliente puede tener cualquier número de pedidos (incluido 0) |
| `1..*` | Uno o más: debe existir al menos uno | Un pedido debe tener al menos una línea |
| `N..M` | Entre N y M: rango específico | Un tribunal debe estar formado por entre 3 y 5 miembros |

### Cuándo verificar la cardinalidad mínima

La cardinalidad mínima > 0 no suele verificarse al guardar (porque en ese momento el registro puede estar incompleto), sino en un momento concreto, como:
- Al intentar enviar o tramitar
- Al intentar cambiar a un estado específico
- Al intentar imprimir o exportar

Documentar en qué momento se verifica:

```
Regla CAR-001
Relación: Expediente → Documentos (1..*)
Cardinalidad mínima: 1 documento
Cuándo se verifica: Al intentar cambiar estado a "Enviado"
Mensaje: "No se puede enviar el expediente. Debe adjuntar al menos 
          un documento antes de enviarlo."
```

```
Regla CAR-002
Relación: Pedido → Líneas de pedido (1..*)
Cardinalidad mínima: 1 línea
Cuándo se verifica: Al intentar guardar el pedido
Mensaje: "El pedido debe tener al menos una línea. 
          Añada un artículo para continuar."
```

```
Regla CAR-003
Relación: Tribunal → Miembros (3..5)
Cardinalidad mínima: 3 miembros
Cardinalidad máxima: 5 miembros
Cuándo se verifica: Al intentar activar el tribunal
Mensaje mínimo: "El tribunal debe estar formado por al menos 3 miembros. 
                 Actualmente tiene {n}."
Mensaje máximo: "El tribunal no puede tener más de 5 miembros. 
                 Actualmente tiene {n}."
```

---

## 3D. Registros maestros necesarios

Antes de permitir ciertas operaciones, el sistema debe verificar que existen los datos de configuración necesarios.

### Ejemplos habituales

```
Regla RM-001
Operación: Crear un expediente de tipo X
Registro maestro requerido: Debe existir al menos una plantilla activa 
                            para el tipo de expediente X
Mensaje: "No se puede crear un expediente de tipo '{tipo}' porque 
          no hay ninguna plantilla de tramitación configurada. 
          Contacte con el administrador."
```

```
Regla RM-002
Operación: Generar factura con IVA al 10%
Registro maestro requerido: Debe existir la cuenta contable configurada 
                            para el tipo de IVA 10%
Mensaje: "No se puede generar la factura. Falta configurar la cuenta 
          contable para el IVA al 10%. Contacte con el departamento 
          de administración."
```

```
Regla RM-003
Operación: Matricular alumno en el curso 2024-2025
Registro maestro requerido: Debe existir el curso académico 2024-2025 
                            creado en el sistema
Mensaje: "No se puede realizar la matrícula. El curso académico 2024-2025 
          no está disponible. Contacte con secretaría."
```

### Por qué documentar esto

Los errores por ausencia de registros maestros son de los más confusos para el usuario, porque el formulario parece correcto pero la operación falla. Anticiparlos en el análisis funcional permite:
1. Que el sistema muestre un mensaje claro en lugar de un error genérico
2. Que la documentación de instalación/configuración incluya la creación de esos registros
3. Que los tests de aceptación incluyan el escenario de configuración incompleta
