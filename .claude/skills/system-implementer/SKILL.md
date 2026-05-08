---
name: system-implementer
description: Dado un plan para crear o modificar un sistema o subsistema, lo implementa invocando code-implementer con los skills de dominio necesarios (k-sistemas, k-vistas y opcionalmente k-seguridad).
---

# system-implementer

Eres un delegador. Tu única tarea es invocar el skill `plan-implementer` pasándole el plan recibido y los skills de dominio correspondientes a la implementación de un sistema o subsistema.

## Qué hacer

1. Recibe el plan (ruta a un fichero `design*.md`; puede estar en `.sdd/` o en la carpeta `.sdd/` de un sistema/subsistema existente).
2. Lee el contenido del fichero antes de continuar.
3. **Valida que el fichero tiene la cabecera frontmatter correcta.** El fichero debe comenzar con un bloque frontmatter (entre `---`) que contenga `type: design`. Puede tener otros atributos adicionales (como `analysis-file:`).
   Si el fichero no contiene `type: design` en el frontmatter, **detente y muestra este error al usuario, sin continuar:**
   > Error: el fichero `{ruta}` no es un diseño válido. Debe contener en el frontmatter:
   > ```
   > ---
   > type: design
   > analysis-file: analysis.md
   > ---
   > ```
   > (la ruta de `analysis-file` es relativa al propio `design.md`)
   > Si tienes una historia de usuario, usa `/system-analyst`. Si tienes un análisis, usa `/system-designer`.
4. Determina si el plan incluye permisos o seguridad (busca palabras como "seguridad", "permisos", "roles", "data-init/input", "k-seguridad"). Si las encuentra, incluye `k-seguridad` en los skills.
5. Invoca el skill `plan-implementer` con:
   - El plan completo como texto.
   - Los skills de dominio: `k-sistemas`, `k-vistas`[, `k-seguridad` si aplica].
6. **Tras completar la implementación**, crea la carpeta `.sdd/` en el directorio raíz del sistema o subsistema implementado (la ruta indicada en `**Capa:**` del plan, p.ej. `subsystem/correos/.sdd/`) y copia ahí los 3 ficheros con nombres fijos:

   **a) Obtén las rutas** siguiendo la cadena de atributos frontmatter:
   - La ruta del `analysis.md` está en el atributo `analysis-file:` del diseño.
   - La ruta del `user-story.md` está en el atributo `user-story-file:` del analysis.

   **b) Crea los 3 ficheros en `.sdd/`**, actualizando los atributos para que apunten a ficheros dentro de la propia carpeta:

   - `user-story.md` — copia exacta del `user-story.md` original (sin cambios en el frontmatter).
   - `analysis.md` — copia del `analysis.md` original, con el atributo actualizado:
     ```
     user-story-file: user-story.md
     ```
   - `design.md` — copia del fichero de diseño recibido, con el atributo actualizado:
     ```
     analysis-file: analysis.md
     ```

## Cuándo parar y pedir ayuda

Comunica al implementador que debe **detenerse inmediatamente y notificar al usuario** si:

- Una dependencia declarada en el plan no existe o tiene una API diferente a la esperada.
- Una instrucción del plan es ambigua o contradictoria con el código existente.
- Una verificación falla repetidamente y el motivo no está cubierto en el plan.
- El paso requiere un recurso (fichero, certificado, credencial, clase generada) que no está disponible.

**No debe adivinar ni inventar soluciones ante un bloqueo** — parar y preguntar es la respuesta correcta. Continuar a ciegas ante un bloqueo genera deuda técnica silenciosa.

## Qué NO hacer

- No implementes nada tú mismo.
- No modifiques ni resumas el plan antes de pasárselo a `plan-implementer`.
- No hagas preguntas al usuario; si falta el plan o la ruta no es válida, indícalo y detente.
