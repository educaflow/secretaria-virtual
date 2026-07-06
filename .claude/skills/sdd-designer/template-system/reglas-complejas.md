# Parte del diseño: reglas de negocio complejas

Mientras produce el diseño, **el diseñador** detecta las reglas de negocio `R-<Entidad>-NNN` que cumplen unos criterios de complejidad (§1) y, **por cada una**, crea un fichero de diseño detallado `rules/R-<Entidad>-NNN.md` dentro de su carpeta `design_<n>/`, además de referenciarlo desde el comentario del método `fireActionRule_*` en `design.md`.

**Cuándo se incluye:** solo si **alguna** regla `R-<Entidad>-NNN` cumple al menos uno de los criterios de §1. Si ninguna los cumple, **no se crea** la carpeta `design_<n>/rules/` ni referencias a ella (esperable en subsistemas CRUD sencillos).

**Quién más lo usa** (`README.md` §2): el **verificador** comprueba que cada `R-` compleja tiene su `rules/R-*.md` con este formato y sin cuerpos Java (`validacion.md` §2.f); el **corrector** solo consulta este fichero si un fallo reportado afecta a un `rules/R-*.md`.

---

## 1. Criterios para considerar una regla "compleja"

Una regla `R-<Entidad>-NNN` se considera compleja — y por tanto necesita su propio fichero `rules/R-<Entidad>-NNN.md` — si su implementación cumple **al menos uno** de estos criterios:

- Necesita **clases auxiliares** propias (helpers, builders, calculadoras, parsers, generadores) que no encajan en el `*ServiceImpl` y que no son utilidades genéricas de `base/infrastructure/`.
- Necesita **tipos propios** del dominio de la regla (DTOs, value objects, records, sealed types) que no son entidades JPA y no existen ya.
- Necesita **interfaces nuevas** (contratos para estrategias, adaptadores de integración, ports de hexagonal).
- Implementa una **máquina de estados** con transiciones, guardas y acciones por transición.
- Coordina **varios subsistemas** o servicios (más de dos colaboradores externos al servicio donde vive `fireActionRule_*`).
- Integra con un **sistema externo** (correo SMTP, HSM, firma, OCR, registro telemático, pasarela de pagos, etc.) más allá de un wrapper trivial.
- Aplica un **algoritmo no trivial** (planificación, optimización, conciliación, paginación específica, retry/backoff con políticas) que merece quedar documentado.
- Tiene **efectos colaterales transaccionales** complejos (commit/rollback parcial, idempotencia, deduplicación, locks).
- Genera artefactos (PDF, CSV, XML firmado) con su propio diseño de plantilla, contenido y composición.

Una regla que se reduce a 2-3 llamadas directas a un servicio existente **no** es compleja: se documenta inline en el comentario del `fireActionRule_*` del `design.md` y no necesita fichero aparte.

---

## 2. Cómo se diseña cada regla compleja (en orden)

1. **Análisis de la regla** (sección `## Análisis de la regla`): qué hace en términos funcionales, paso a paso — qué se dispara y cuándo; qué información lee y de dónde; qué acciones realiza y en qué orden; qué efectos colaterales y garantías de transaccionalidad/idempotencia; qué errores puede encontrar y cómo tratarlos; entradas/salidas de cada colaborador. **Solo después** se pasa al diseño detallado.
2. **Diseño detallado** (sin escribir cuerpo Java): clases nuevas (FQN, responsabilidad, firmas + comentario), interfaces (métodos + justificación), tipos propios (DTOs/value objects/records/enums con campos y semántica), diagrama de secuencia (ASCII o lista numerada), tabla de errores, y el contenido del método `fireActionRule_*` (firma + comentario con la secuencia de llamadas, referenciando este fichero). **Sin código Java real.**

---

## 3. Formato del fichero `rules/R-<Entidad>-NNN.md`

El diseñador escribe en `design_<n>/rules/R-<Entidad>-NNN.md` un fichero con esta estructura:

````markdown
# R-<Entidad>-NNN — <título corto de la regla>

**Entidad:** <Entidad>
**Origen spec:** <RN-<Entidad>-NNN, …>
**Operación:** insert | update | remove | <operación custom>
**Momento:** Antes | Después de repository.save/remove
**Servicio host:** com.educaflow.subsystem.<x>.service.impl.<Entidad>ServiceImpl
**Método host:** fireActionRule_<nombreLegible>(<firma>)

## Análisis de la regla
<descripción funcional paso a paso del qué/cuándo/cómo/errores>

## Diseño detallado

### Clases nuevas
- <FQN> — <responsabilidad en una frase>
  - <firma de método> — <comentario>
  - …

### Interfaces
- <FQN> — <responsabilidad y justificación>
  - <firma de método> — <comentario>

### Tipos propios
- <FQN> (record/value object/enum) — <campos> — <semántica>

### Diagrama de secuencia
fireActionRule_<x>
  ├─ <Colaborador1>.metodo(...) → <qué devuelve>
  ├─ <Colaborador2>.metodo(...) → <qué devuelve>
  └─ …

### Errores
| Condición | Origen | Tratamiento |
|-----------|--------|-------------|
| <cuándo>  | <clase.método> | <BusinessMessages | excepción | log + retry | …> |

### Contenido del método `fireActionRule_*`
```java
// Firma:
<firma completa>
//   Implementa R-<Entidad>-NNN (Origen spec: RN-<Entidad>-NNN). Diseño detallado en design/rules/R-<Entidad>-NNN.md.
//   Secuencia:
//     1. <llamada 1>
//     2. <llamada 2>
//     …
```
````

En el `design.md`, el comentario del método `fireActionRule_*` correspondiente referencia este fichero (`Diseño detallado en design/rules/R-<Entidad>-NNN.md`), y la matriz de trazabilidad marca la regla con un puntero al fichero detallado, p.ej.:

```
| R-Bar-003 | RN-Bar-008 | BarServiceImpl.fireActionRule_publicar (Después de repository.save) | Detalle: design/rules/R-Bar-003.md |
```
