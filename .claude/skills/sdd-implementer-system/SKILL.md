---
name: sdd-implementer-system
description: Dado un plan para crear o modificar un sistema o subsistema, lo implementa invocando code-implementer con los skills de dominio necesarios (k-sistemas, k-vistas y opcionalmente k-seguridad).
---

# sdd-implementer-system

Eres un delegador. Tu única tarea es invocar el skill `code-implementer` pasándole el plan recibido y los skills de dominio correspondientes a la implementación de un sistema o subsistema.

## Override de rutas (para testing)

Para poder probar este skill en un sandbox alternativo sin tocar el árbol real (testing unitario del propio skill, iteración de mejoras, etc.), se aceptan en el prompt los siguientes overrides (también se reconocen las formas `entrada: <ruta>` y `raíz: <ruta>`):

- `--in=<ruta>` — fichero `design_NN.md` de entrada explícito. Si se indica, sustituye al fichero por defecto y **desactiva la auto-detección**.
- `--root=<ruta>` — raíz alternativa a `.sdd/drafts/`. Todas las rutas relativas se resuelven contra esta raíz.

No hay `--out` porque este skill no crea ficheros en `.sdd/`: delega en `code-implementer`, que escribe código en el árbol del proyecto. Para probar el implementer en un sandbox, ejecútalo apuntando a una copia del proyecto.

Estos argumentos son **opcionales y para testing**: en uso normal no se especifican.

## Qué hacer

1. **Si el usuario no proporciona ruta**, busca el último diseño disponible antes de continuar:

   > **PROCEDIMIENTO OBLIGATORIO para detectar el diseño más reciente:**
   >
   > a. **Listar** las subcarpetas de `.sdd/drafts/` cuyo nombre empieza por `YYYY-MM-DD_HH-MM_` (regex `^[0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{2}-[0-9]{2}_`):
   >    ```bash
   >    ls -d .sdd/drafts/[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]_[0-9][0-9]-[0-9][0-9]_*/ 2>/dev/null
   >    ```
   >    Ordenarlas **alfabéticamente** (orden coincide con cronológico) y tomar **la última** (la iniciativa más reciente).
   > b. Dentro de esa iniciativa, **listar** las subcarpetas `analysis_NN/`:
   >    ```bash
   >    ls -d .sdd/drafts/{iniciativa}/analysis_*/ 2>/dev/null
   >    ```
   >    Tomar la del **número más alto** (NO por `mtime`).
   > c. Dentro de esa subcarpeta, **listar** los ficheros `design_NN.md`:
   >    ```bash
   >    ls .sdd/drafts/{iniciativa}/analysis_NN/design_*.md 2>/dev/null
   >    ```
   >    Tomar el del **número más alto** (NO por `mtime`).
   > d. Si no existe ninguna iniciativa, ningún `analysis_NN/` o ningún `design_NN.md`, indicar al usuario que no hay diseños previos y pedir una ruta. **Detente.**
   > e. **Mostrar al usuario el nombre del fichero `design_NN.md` junto con su ruta** y preguntar con `AskUserQuestion` si quiere usar ese diseño:
   >    - Sí → continuar con el resto de los pasos usando esa ruta.
   >    - No → pedir al usuario la ruta del diseño que quiere implementar. **Detente.**
   >
   > **PROHIBIDO:**
   > - Elegir una iniciativa, `analysis_NN` o `design_NN` que no sea el de mayor timestamp / mayor número en cada nivel.
   > - Usar `mtime` o cualquier criterio distinto del orden alfabético (para timestamp) o numérico (para NN).
   > - Continuar sin confirmación del usuario tras mostrar la ruta detectada.
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
