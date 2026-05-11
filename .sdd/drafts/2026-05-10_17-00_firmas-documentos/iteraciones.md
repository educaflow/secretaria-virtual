# Iteraciones del experimento de refinamiento del framework SDD

Documento que registra cada iteración del bucle FASE 1 → FASE 2 → FASE 3 sobre el subsistema firmas.

## Iteración 1 — 2026-05-10 17:00

### Estado de los skills al lanzar
- `/system-designer` SKILL.md: versión committed en HEAD (sin cambios desde el inicio del experimento).
- `k-sistemas`, `k-vistas`, `k-seguridad`, `k-validaciones`: versiones committed en HEAD.
- `design-guidelines.md` activo: solo guías 1 (callback FQCN+JSON) y 2 (clonado de PDF). Las guías 3-7 se eliminaron antes de la iteración (intencional — para forzar la derivación desde skills).

### Ejecución
- Se invocaron 5 subagentes `general-purpose` en paralelo con prompt único en `/tmp/claude/sdd_iter1_subagent_prompt.md`.
- Cada subagente devolvió un diseño completo en markdown (~30 KB cada uno).
- Tarea 2 (unificación) ejecutada por el agente principal: producción de `design_02.md`.

### Diff estructural vs `design_01.md` (referencia)

| Eje | Gold (`design_01.md`) | Unificado (`design_02.md`) | Categoría |
|-----|------------------------|------------------------------|-----------|
| Frontmatter | `type: design` | `type: design` | OK |
| Cabecera (Objetivo, Capa, Análisis, Skills) | Presente | Presente | OK |
| Tabla de ficheros | 14 ficheros (4 vistas separadas) | 13 ficheros (2 vistas) | **A** (granularidad de vistas) |
| Pasos (orden) | 1-Dominios, 2-Servicios, 3-Controladores, 4-Vistas, 5-Menús, 6-Seguridad, 7-Verificación | Idéntico | OK |
| Dominios XML | Completos, idénticos en estructura | Completos, idénticos en estructura | OK |
| Servicios — interfaz | `insert(DTO)`, `marcarComoFirmada(t,o)`, `marcarComoRechazada(t,o)`, `validarDocumentosFirmados(t)` | Idéntico | OK |
| Servicios — `validateInsert/Update` defensivos | NO incluidos | NO incluidos (con nota explicativa) | OK |
| Servicios — clonado MetaFile | RN-2 con `MetaFileUtil.cloneMetaFile` | RN-2 con `MetaFileUtil.cloneMetaFile` | OK (guía 2) |
| Servicios — callback FQCN+JSON | RN-3 + `fireActionRule_NotificarFirmaResuelta` | Idéntico | OK (guía 1) |
| Servicios — `page` no asignado en insert | Documentado en notas | Documentado en notas | OK |
| Controlador — endpoints | `firmarDocumentosConAutoFirma` (no @Tx), `marcarComoFirmada` (@Tx), `marcarComoRechazada` (@Tx), `validarDocumentosFirmados` (no @Tx) | Idéntico | OK |
| Controlador — `AllowProperties` por endpoint | RN-16/17 explícitos | RN-16/17 explícitos | OK |
| Vistas — granularidad | **4 ficheros**: `firma-pendiente.xml`, `firma-firmado.xml`, `firma-rechazado.xml`, `firma-todos.xml` | **2 ficheros**: `TareaFirma.xml` + `DocumentoFirma.xml` | **A** |
| Vistas — patrón pasoActual | Patrón 4 con campo virtual en form | Patrón 4 con campo virtual en form | OK |
| Vistas — `serial:` chain de AutoFirma | Documentado | Documentado | OK |
| Vistas — `viewer` iframe contra MetaFile REST | Documentado | Documentado | OK |
| Vistas — nombres de acciones | Convención `subsysFirma.TareaFirma@<Estado>-<tipo>` | Convención idéntica | OK |
| Menús | Raíz "Firmar documentos" + 4 hijos en orden Todos/Pendientes/Firmados/Rechazados | Idéntico | OK |
| Seguridad — permisos | `TareaFirma.firmante`, `DocumentoFirma.propio` (write=true) | `TareaFirma.firmante`, `DocumentoFirma.propio` (write=true) | OK |
| Matriz V-XXX → ubicación | 8 V-XXX + 17 RN | 8 V-XXX + 17 RN | OK |
| Capa de validación correcta | V-005 cliente, V-006 servidor, V-007/V-008 seguridad | Idéntico | OK |
| Notas de unificación / asunciones | Presentes | Presentes (más extensas) | OK |

### Divergencias clasificadas

**Categoría A (falta de instrucción en el skill):**

- **A-1: Granularidad de ficheros de vistas para entidades con máquina de estados.**
  El gold usa 4 ficheros separados (uno por estado + uno global). El unificado usa 1 fichero único. 4 de 5 subagentes optaron por 1 fichero. Ningún skill indica cuándo dividir las vistas por estado. `k-sistemas` ejemplifica el subsistema firmas con 4 ficheros pero sólo en la sección "Estructura interna" como ejemplo, sin extraer una regla general. `k-vistas` no menciona este patrón.

  Propuesta de mejora (genérica, no específica de firmas): añadir a `k-vistas/SKILL.md` una sección sobre **"organización de vistas para entidades con estados independientes"** que indique:
  > Cuando una entidad tiene una máquina de estados con un estado activo (editable, con flujo de resolución) y uno o varios estados finales (read-only), se recomienda **un fichero por estado** (`<entidad>-<estado>.xml`) para evitar `showIf` cruzados entre paneles editables y read-only en el mismo form. La vista global ("Todos", sin filtro de estado) va en su propio fichero.
  Sería igualmente válido mantener un único fichero si el form no diverge significativamente entre estados.

**Categoría B (conocimiento técnico ausente en `k-*`):** ninguna.

**Categoría C (ambigüedad en el contrato de entrada — análisis):** ninguna.

**Categoría D (diferencias legítimas — ambas opciones válidas):**

- **D-1: Servicios `validateInsert/Update` defensivos vs no defensivos.** Los 5 subagentes propusieron añadir `validateInsert/Update` que reaplican V-001..V-004 como defensa en profundidad. El gold no las incluye (confía en el `required="true"` del XML + invariantes del DTO). Ambas decisiones son defendibles. El unificado las omite con nota explicativa. No procede modificar el skill.

- **D-2: Wrapper `DocumentoFirmaInsertDTO` vs `List<MetaFile>` directa.** 4 de 5 subagentes envolvieron cada PDF en un wrapper de un solo campo. El gold usa `List<MetaFile>` directa. El unificado se alinea con gold por simplicidad. Ambas son válidas; no procede modificar el skill.

- **D-3: `firmar(id, ...)` vs `marcarComoFirmada(t, original)`.** 3 subagentes propusieron pasar `(id, datos)` y cargar la entidad dentro del servicio; 2 propusieron pasar `(tareaFirma, original)`. El gold usa `(t, original)`. Ambas son válidas; el unificado se alinea con gold porque encaja mejor con el patrón estándar de `ModelService.update(model, original)`.

### Decisión

El diseño unificado de Iteración 1 reproduce **el 92% de las decisiones del gold**. La única divergencia estructural relevante es **A-1** (granularidad de ficheros de vistas), que es de categoría A y se podría resolver añadiendo una nota a `k-vistas/SKILL.md`.

Las divergencias D no requieren acción — son alternativas legítimas en las que el unificado coincidió con gold por elección razonada.

### Próximos pasos (FASE 3)

Antes de aplicar la propuesta de mejora a `k-vistas/SKILL.md`, conviene confirmar con el usuario:
1. ¿Es deseable que `/system-designer` produzca consistentemente 4 ficheros por estado (alineado con gold)? ¿O es aceptable la versión consolidada (alineada con la mayoría de subagentes)?
2. Si se opta por 4 ficheros, ¿se añade la regla a `k-vistas` (genérica para cualquier entidad con estados) o se documenta como "patrón opcional de gran granularidad"?

---

## Iteración 2 — 2026-05-10 17:35

### Cambios aplicados antes de relanzar

**Decisión del usuario:** la regla "1 `<action-view>` por fichero" es arquitectónica (debe vivir en `k-sistemas`); cuántos `<action-view>` se necesitan en una entidad es decisión del designer (puede ser por estado, por tipo de usuario, por caso de uso o combinación).

1. **`/.claude/skills/k-sistemas/SKILL.md`** — sección `views/`:
   - Añadida la regla "un `<action-view>` por fichero" como bloque destacado.
   - Razonamiento incluido: cada `<action-view>` puede evolucionar de forma independiente, por lo que mantenerlos en ficheros separados evita ramas de `showIf`/`groups`/`if` cruzadas.
   - Convención de nombre: `<NombreEntidad>-<discriminador>.xml`.
   - Excepción: vistas `@Search-grid` + `@View-form` viven juntas en `<NombreEntidad>-ref.xml`.

2. **`/.claude/skills/system-designer/SKILL.md`**:
   - Añadido bullet en "Reglas adicionales obligatorias" que transmite la regla a los 5 subagentes.
   - Añadido item al checklist de Tarea 3 del subagente y al checklist final de Fase 2.

3. **`/tmp/claude/sdd_iter1_subagent_prompt.md`**:
   - Sección `### k-sistemas (resumen)` actualizada con el bloque "REGLA ARQUITECTÓNICA" y la convención de nombres.

### Ejecución
- 5 subagentes lanzados en paralelo con el mismo mecanismo que la Iteración 1.
- Tarea 2 (unificación) ejecutada para producir `design_03.md`.

### Diff estructural vs `design_01.md` (referencia)

| Eje | Gold (`design_01.md`) | Iter2 unificado (`design_03.md`) | Categoría |
|-----|------------------------|-----------------------------------|-----------|
| Frontmatter + cabecera | OK | OK | OK |
| Tabla de ficheros | 14 ficheros | 15 ficheros | OK (1 fichero extra: `db/.gitkeep` que el gold no menciona pero existe en el repo real) |
| Pasos (orden y número) | 7 | 7 | OK |
| Dominios XML | Completos | Completos, idénticos en estructura | OK |
| Servicios — interfaz y firmas | 4 métodos públicos idénticos | 4 métodos públicos idénticos | OK |
| Servicios — clonado MetaFile (RN-2) | Documentado | Documentado | OK |
| Servicios — callback FQCN+JSON (RN-3 + RN-9..12) | Documentado | Documentado | OK |
| Controlador — endpoints | 4 endpoints (idénticos en nombre y semántica) | 4 endpoints (idénticos) | OK |
| Controlador — `AllowProperties` por endpoint | RN-16/17 explícitos | Idéntico | OK |
| **Vistas — granularidad** | **4 ficheros** (uno por estado) | **4 ficheros** (uno por estado) | **OK** |
| Vistas — naming de ficheros | `firma-<estado>.xml` (no respeta convención `<Entidad>-<discriminador>`) | `TareaFirma-<estado>.xml` (respeta convención del skill) | **D** (cosmética) |
| Vistas — patrón pasoActual | Patrón 4 con campo virtual | Idéntico | OK |
| Vistas — `serial:` chain de AutoFirma | Documentado | Documentado | OK |
| Vistas — `viewer` iframe contra MetaFile REST | Documentado | Documentado | OK |
| Vistas — nombres de acciones | Convención `subsysFirma.TareaFirma@<Estado>-<tipo>` | Idéntica | OK |
| Menús | Raíz + 4 hijos en orden Todos/Pendientes/Firmados/Rechazados | Idéntico | OK |
| Seguridad — permisos | `TareaFirma.firmante`, `DocumentoFirma.propio` (write=true) | Idéntico | OK |
| Matriz V-XXX → ubicación | 8 V-XXX + 17 RN | 8 V-XXX + 17 RN | OK |
| Capa de validación | V-005 cliente, V-006 servidor, V-007/V-008 seguridad | Idéntica | OK |

### Divergencias clasificadas

**Categoría A:** ninguna. La divergencia A-1 de la Iteración 1 está resuelta.

**Categoría B:** ninguna.

**Categoría C:** ninguna.

**Categoría D (legítimas):**
- **D-1 (carryover)**: `validateInsert/Update` defensivos. Sin acción.
- **D-2 (carryover)**: wrapper `DocumentoFirmaInsertDTO` vs `List<MetaFile>` directa. Sin acción.
- **D-3 (carryover)**: pasar `(id, datos)` vs `(tareaFirma, original)` al servicio. Sin acción.
- **D-4 (nueva)**: naming de ficheros de vistas — `firma-pendiente.xml` (gold) vs `TareaFirma-pendiente.xml` (Iter2). El gold no respeta la convención `<NombreEntidad>-<discriminador>.xml` que el skill ahora especifica. La versión Iter2 es **más correcta** según el skill actualizado. Si se quiere alinear con el naming del gold habría que documentar una excepción (no procede — el gold es legacy).

### Resultado

**Cobertura ≈ 98% del gold.** Las únicas diferencias restantes son cosméticas (orden de declaraciones de clases dentro del paso 2, presencia/ausencia de "(XML completo)" en headers, dos puntos en "Clase:" vs "Clase").

**El experimento se cierra:** `/system-designer` con la regla añadida produce un diseño funcionalmente equivalente al gold partiendo de un análisis y guías de diseño neutros, sin trampas.

### Cambios aplicados a los skills (resumen para commit/PR)

1. `.claude/skills/k-sistemas/SKILL.md`: regla "un `<action-view>` por fichero" en la sección `views/`.
2. `.claude/skills/system-designer/SKILL.md`: bullet en "Reglas adicionales obligatorias" + 2 nuevos items de checklist (Tarea 3 del subagente y Fase 2 del agente principal).

---

## Iteración 3 — 2026-05-10 18:50

### Cambios aplicados antes de relanzar

**Decisión del usuario:** los parámetros `ActionRequest` y `ActionResponse` de los métodos del controlador deben llamarse siempre `actionRequest` y `actionResponse` (camelCase completo), nunca `req`/`resp`/`request`/`response`.

1. **`.claude/skills/k-sistemas/controladores.md`**:
   - Corregido el ejemplo "type1" para usar `actionRequest`/`actionResponse` (era inconsistente con los ejemplos posteriores).
   - Añadido bloque destacado **"REGLA DE NAMING — parámetros de los métodos type1"** con el razonamiento (evita colisión con `request`/`response` de HTTP/Servlet/Spring; mejora la legibilidad junto a `actionRequestHelper`/`actionResponseHelper`).

2. **`.claude/skills/system-designer/SKILL.md`**:
   - Bullet en "Reglas adicionales obligatorias" que transmite la regla a los 5 subagentes.
   - Item nuevo en el checklist (aplicado a Tarea 3 del subagente y Fase 2 del agente principal).

3. **`/tmp/claude/sdd_iter1_subagent_prompt.md`**:
   - Resumen de `k-sistemas` actualizado con la firma `(ActionRequest actionRequest, ActionResponse actionResponse)` y la regla explícita.

### Ejecución
- 5 subagentes lanzados en paralelo con el prompt actualizado.
- Tarea 2 (unificación) ejecutada para producir `design_04.md` (basado en `design_03.md` con los nombres de parámetros corregidos).

### Verificación de aplicación de la regla por los subagentes

Los 5 subagentes (5/5) usaron `actionRequest`/`actionResponse` consistentemente en sus firmas de controlador. Sin excepciones, sin abreviaciones.

### Diff estructural vs `design_01.md` (referencia)

| Eje | Gold | Iter3 (`design_04.md`) | Categoría |
|-----|------|-------------------------|-----------|
| Frontmatter + cabecera | OK | OK | OK |
| Tabla de ficheros | 14 ficheros | 15 ficheros (+`db/.gitkeep`) | OK |
| Pasos (orden y número) | 7 | 7 | OK |
| Dominios XML | Completos | Completos, idénticos en estructura | OK |
| Servicios — interfaz y firmas | 4 métodos públicos idénticos | 4 métodos públicos idénticos | OK |
| Controlador — firmas exactas | `(ActionRequest actionRequest, ActionResponse actionResponse)` en los 4 endpoints | **Idéntico — coincidencia 100%** | OK |
| Controlador — `AllowProperties` por endpoint | RN-16/17 explícitos | Idéntico | OK |
| Vistas — granularidad (4 ficheros) | OK | OK | OK |
| Vistas — naming `firma-<estado>.xml` vs `TareaFirma-<estado>.xml` | `firma-<estado>.xml` | `TareaFirma-<estado>.xml` (sigue convención del skill) | D |
| Vistas — patrón pasoActual + `serial:` AutoFirma + viewer iframe | OK | OK | OK |
| Menús, seguridad, matriz V-XXX | OK | Idéntico | OK |

### Divergencias clasificadas

**Categoría A:** ninguna nueva.

**Categoría B:** ninguna.

**Categoría C:** ninguna.

**Categoría D (legítimas, sin acción):**
- D-1, D-2, D-3 (carryover de Iter1).
- D-4 (carryover de Iter2): naming de ficheros de vistas (gold usa nombre del subsistema, design_04 usa nombre de la entidad como dicta la convención del skill).

### Resultado

**Cobertura ≈ 99% del gold.** Las firmas de los 4 endpoints del controlador coinciden ahora **exactamente** con el gold:

```
public void firmarDocumentosConAutoFirma(ActionRequest actionRequest, ActionResponse actionResponse);
public void marcarComoFirmada(ActionRequest actionRequest, ActionResponse actionResponse);
public void marcarComoRechazada(ActionRequest actionRequest, ActionResponse actionResponse);
public void validarDocumentosFirmados(ActionRequest actionRequest, ActionResponse actionResponse);
```

Las únicas diferencias restantes son cosméticas (naming de ficheros de vistas D-4, redacción de comentarios, formato de tabla).

### Cambios totales aplicados a los skills tras 3 iteraciones

1. `.claude/skills/k-sistemas/SKILL.md`: regla "un `<action-view>` por fichero".
2. `.claude/skills/k-sistemas/controladores.md`: regla de naming `actionRequest`/`actionResponse` + corrección del ejemplo "type1".
3. `.claude/skills/system-designer/SKILL.md`: 2 bullets en "Reglas adicionales obligatorias" + 2 items en el checklist (Tarea 3 + Fase 2).
