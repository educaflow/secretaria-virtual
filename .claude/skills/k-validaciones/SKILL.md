---
name: k-validaciones
description: Referencia completa de validaciones desde el punto de vista del análisis funcional — taxonomía de 5 niveles, tipos de campo, validaciones cruzadas, integridad, ciclo de vida, campos calculados, mensajes de error y plantillas de documentación. Independiente de cualquier framework o lenguaje.
---

# k-validaciones — Validaciones en Análisis Funcional

Referencia para analistas funcionales sobre cómo identificar, clasificar y documentar validaciones en aplicaciones empresariales. No trata de programación: trata del **qué** debe cumplir el dato, no del **cómo** se implementa.

## Ficheros

| Fichero | Contenido |
|---------|-----------|
| `tipos.md` | Taxonomía completa de los 5 niveles de validación + distinción validación vs. regla de negocio |
| `campo.md` | Validaciones de campo individual: obligatoriedad, tipo, longitud, formato, rango, dominio, dígito de control |
| `cruzadas.md` | Validaciones entre campos: consistencia, condicionales, tablas de decisión, efectos laterales |
| `integridad.md` | Unicidad, integridad referencial, cardinalidad, registros maestros |
| `estado.md` | Estados y ciclo de vida: transiciones, campos editables por estado, validaciones propias |
| `calculados.md` | Campos calculados: tipos, fórmulas, dependencias, circularidades |
| `mensajes.md` | Guía de mensajes de error: estructura, ejemplos bien/mal, reglas de escritura |
| `plantillas.md` | Plantillas de documentación: ficha de campo, tabla resumen, Given-When-Then, diccionario de datos |

---

## Los 5 niveles de validación (resumen)

```
Nivel 1 — Campo individual
  1A Obligatoriedad    1B Tipo de dato      1C Longitud
  1D Formato/patrón    1E Rango numérico    1F Rango de fechas
  1G Dominio/lista     1H Caracteres        1I Dígito de control

Nivel 2 — Entre campos (cross-field)
  2A Consistencia      2B Dependencia condicional    2C Totales cruzados

Nivel 3 — Integridad
  3A Unicidad          3B Integridad referencial
  3C Cardinalidad      3D Registros maestros

Nivel 4 — Estado y ciclo de vida
  4A Transiciones válidas    4B Campos editables por estado
  4C Validaciones propias del estado

Nivel 5 — Reglas de negocio
  5A Restricciones     5B Cálculos/derivaciones
  5C Autorizaciones    5D Reglas temporales
```

---

## Las 6 dimensiones de calidad de dato

| Dimensión | Qué garantiza | Validaciones que la protegen |
|-----------|--------------|------------------------------|
| **Completitud** | Todos los datos necesarios están presentes | Obligatoriedad (1A) |
| **Exactitud** | El dato representa correctamente la realidad | Formato (1D), rango (1E/1F), dominio (1G) |
| **Consistencia** | El dato es coherente con los demás | Validaciones cruzadas (Nivel 2) |
| **Validez** | Cumple las reglas del negocio | Reglas de negocio (Nivel 5) |
| **Unicidad** | No hay duplicados | Unicidad (3A) |
| **Integridad** | Las relaciones entre datos son correctas | Integridad referencial (3B), cardinalidad (3C) |

---

## Checklist del analista funcional

Para cada campo de una entidad, responder estas preguntas antes de dar la especificación por completa.

### Obligatoriedad
- [ ] ¿Es siempre obligatorio, siempre opcional, o condicionalmente obligatorio?
- [ ] Si es condicional: ¿qué valor/estado de qué otro campo activa la obligatoriedad?

### Tipo y formato
- [ ] ¿Qué tipo de dato es? (texto, número, fecha, booleano, lista, referencia)
- [ ] ¿Tiene un formato o patrón específico? (email, NIF, IBAN, teléfono...)
- [ ] ¿Tiene restricciones de longitud mínima y máxima?
- [ ] ¿Qué caracteres están permitidos? ¿Se permiten tildes? ¿Ñ? ¿Caracteres especiales?
- [ ] ¿Tiene dígito de control verificable? (NIF, IBAN, código de barras...)

### Dominio y rango
- [ ] ¿Tiene lista de valores permitidos? ¿Es cerrada (solo esos) o abierta (sugerencias)?
- [ ] Si es numérico: ¿mínimo? ¿máximo? ¿los extremos están incluidos? ¿negativos? ¿cuántos decimales?
- [ ] Si es fecha: ¿puede ser pasada? ¿puede ser futura? ¿hay rango absoluto o relativo?
- [ ] ¿Qué pasa con el valor cero? ¿Y con el valor nulo/vacío?

### Dependencias entre campos
- [ ] ¿Su validez depende del valor de otro campo? (validación cruzada)
- [ ] ¿Este campo determina la obligatoriedad o el dominio de otro campo?
- [ ] Si cambia este campo, ¿qué otros campos deberían limpiarse o recalcularse?
- [ ] ¿Es un campo calculado? ¿Cuándo se recalcula? ¿Puede editarse manualmente?

### Integridad
- [ ] ¿Debe ser único en el sistema? ¿En qué ámbito? (global, por centro, por año...)
- [ ] ¿Referencia a otra entidad? ¿Qué pasa si esa referencia se borra?

### Ciclo de vida
- [ ] ¿Es editable en todos los estados del registro?
- [ ] ¿Su obligatoriedad cambia según el estado?
- [ ] ¿En qué estados tiene restricciones adicionales?

### Mensajes
- [ ] ¿Cuál es el mensaje exacto para cada caso de error?
- [ ] ¿Los mensajes incluyen el valor problemático y cómo corregirlo?
- [ ] ¿La severidad es correcta? (error bloqueante, advertencia, informativa)
