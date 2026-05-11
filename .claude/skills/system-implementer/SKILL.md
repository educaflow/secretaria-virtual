---
name: system-implementer
description: Dado un plan para crear o modificar un sistema o subsistema, lo implementa invocando code-implementer con los skills de dominio necesarios (k-sistemas, k-vistas y opcionalmente k-seguridad).
---

# system-implementer

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
   > Si tienes una historia de usuario, usa `/system-analyst`. Si tienes un análisis, usa `/system-designer`.
5. Determina si el plan incluye permisos o seguridad (busca palabras como "seguridad", "permisos", "roles", "data-init/input", "k-seguridad"). Si las encuentra, incluye `k-seguridad` en los skills.
6. Invoca el skill `code-implementer` con:
   - El plan completo como texto.
   - Los skills de dominio: `k-sistemas`, `k-vistas`[, `k-seguridad` si aplica].
7. **Tras completar la implementación**, copia los 3 ficheros a una nueva carpeta en `.sdd/specs/`.

   **a) Localiza los ficheros por estructura de carpetas** (sin leer frontmatter):
   - El diseño recibido está en `.sdd/drafts/{iniciativa}/analysis_NN/design_NN.md`
   - El `analysis.md` está en la misma carpeta: `.sdd/drafts/{iniciativa}/analysis_NN/analysis.md`
   - El `user-story.md` está dos niveles arriba: `.sdd/drafts/{iniciativa}/user-story.md`
   - El `design-guidelines.md` (opcional) está dos niveles arriba, junto al `user-story.md`: `.sdd/drafts/{iniciativa}/design-guidelines.md`. Si no existe, no se copia nada — no es un error.

   **b) Determina el destino en `.sdd/specs/`:**
   - El número es el siguiente disponible: lista las entradas de `.sdd/specs/`, **considera solo las carpetas cuyo nombre empieza por 4 dígitos seguidos de `_`** (regex `^[0-9]{4}_`), toma el máximo de esos números y suma 1. Formato de 4 dígitos: 0001, 0002… (Las carpetas auxiliares como `_archived/` o `README/` se ignoran y no rompen la numeración.)
   - La descripción es el nombre de la carpeta de iniciativa sin el prefijo de timestamp (todo lo que va después de `YYYY-MM-DD_HH-MM_`).
   - Ejemplo: si la iniciativa es `2026-05-07_22-09_subsistema-correos-registro-envio` y ya hay 0 carpetas en `specs/` que empiecen por 4 dígitos, el destino es `.sdd/specs/0001_subsistema-correos-registro-envio/`.

   **c) Copia los ficheros al destino** con nombres fijos (sin modificar el frontmatter):
   - `user-story.md`
   - `analysis.md`
   - `design.md`
   - `design-guidelines.md` (solo si existía en la iniciativa; si no existe, no se copia y no se considera error)

7. **Mensaje final al usuario.** Tras copiar los ficheros, indica al usuario:

   ```
   Implementación completada.
   Spec final guardada en .sdd/specs/{NNNN}_{descr}/ ({lista de ficheros copiados}).
   Los drafts originales se conservan en .sdd/drafts/{iniciativa}/ por si necesitas iterar.
   ```

   Sustituye `{NNNN}_{descr}` y `{iniciativa}` por los nombres reales. La `{lista de ficheros copiados}` será `user-story.md, analysis.md, design.md` o `user-story.md, analysis.md, design.md, design-guidelines.md` según haya existido o no el fichero de guías.

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
