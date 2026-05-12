---
name: sdd-implementer-system
description: Dado un plan para crear o modificar un sistema o subsistema, lo implementa invocando code-implementer con los skills de dominio necesarios (k-sistemas, k-vistas y opcionalmente k-seguridad).
---

# sdd-implementer-system

Eres un delegador. Tu única tarea es invocar el skill `code-implementer` pasándole el plan recibido y los skills de dominio correspondientes a la implementación de un sistema o subsistema.

## Qué hacer

1. **Si el usuario no proporciona ruta**, busca el último diseño disponible antes de continuar:
   a. Lista las subcarpetas de `.sdd/drafts/` cuyo nombre empieza por `YYYY-MM-DD_HH-MM_` (regex `^[0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{2}-[0-9]{2}_`) y ordénalas alfabéticamente — el prefijo de timestamp hace que el orden alfabético coincida con el cronológico — y toma la última (la iniciativa más reciente).
   b. Dentro de esa iniciativa, lista las subcarpetas `analysis_NN/` y toma la del número más alto (el análisis más reciente).
   c. Dentro de esa subcarpeta, lista los ficheros `design_NN.md` y toma el del número más alto (el diseño más reciente).
   d. Si no existe ninguna iniciativa, ningún `analysis_NN/` o ningún `design_NN.md`, indica al usuario que no hay diseños previos y pídele que indique una ruta. Detente.
   e. **Muestra al usuario el nombre del fichero `design_NN.md` junto con su ruta** y pregunta con `AskUserQuestion` si quiere usar ese diseño:
      - Sí → continúa con el resto de los pasos usando esa ruta.
      - No → pide al usuario la ruta del diseño que quiere implementar. Detente.
2. Recibe el plan (ruta a un fichero `design*.md`; debe estar en `.sdd/drafts/{iniciativa}/analysis_NN/`).
3. Lee el contenido del fichero antes de continuar.
4. **Valida que el fichero tiene la cabecera frontmatter correcta.** El fichero debe comenzar con un bloque frontmatter (entre `---`) que contenga `type: design`.
   Si el fichero no contiene `type: design` en el frontmatter, **detente y muestra este error al usuario, sin continuar:**
   > Error: el fichero `{ruta}` no es un diseño válido. Debe contener en el frontmatter:
   > ```
   > ---
   > type: design
   > ---
   > ```
   > Si tienes una historia de usuario, usa `/sdd-analyst-system`. Si tienes un análisis, usa `/sdd-designer-system`.
5. Determina si el plan incluye permisos o seguridad (busca palabras como "seguridad", "permisos", "roles", "data-init/input", "k-seguridad"). Si las encuentra, incluye `k-seguridad` en los skills.
6. Invoca el skill `code-implementer` con:
   - El plan completo como texto.
   - Los skills de dominio: `k-sistemas`, `k-vistas`[, `k-seguridad` si aplica].

7. **Mensaje final al usuario.** Tras completar la implementación, indica:

   ```
   Implementación completada.
   Los artefactos del draft se mantienen en .sdd/drafts/{iniciativa}/ — no se ha archivado nada en .sdd/specs/.
   Cuando estés conforme con la implementación, lanza `/sdd-close-spec` para cerrar la iniciativa: actualizará los CLAUDE.md afectados y archivará la spec en .sdd/specs/.
   ```

   Sustituye `{iniciativa}` por el nombre real de la carpeta del draft.

## Cuándo parar y pedir ayuda

Comunica al implementador que debe **detenerse inmediatamente y notificar al usuario** si:

- Una dependencia declarada en el plan no existe o tiene una API diferente a la esperada.
- Una instrucción del plan es ambigua o contradictoria con el código existente.
- Una verificación falla repetidamente y el motivo no está cubierto en el plan.
- El paso requiere un recurso (fichero, certificado, credencial, clase generada) que no está disponible.

**No debe adivinar ni inventar soluciones ante un bloqueo** — parar y preguntar es la respuesta correcta. Continuar a ciegas ante un bloqueo genera deuda técnica silenciosa.

## Qué NO hacer

- No implementes nada tú mismo.
- No modifiques ni resumas el plan antes de pasárselo a `code-implementer`.
- No hagas preguntas al usuario; si falta el plan o la ruta no es válida, indícalo y detente.
- **No leas otros ficheros `design*.md` ni `analysis.md` de `.sdd/` como referencia.** Implementa únicamente el plan recibido; consultar diseños anteriores llevaría a mezclar decisiones de distintas iteraciones.
