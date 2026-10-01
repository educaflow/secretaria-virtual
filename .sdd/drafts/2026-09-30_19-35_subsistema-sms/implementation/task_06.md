---
type: implementation-task
template: system
---

# Tarea 06 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/base/util/CLAUDE.md` | Modificar | k-code-quality | Documentar `NumeroTelefono`, `MensajeSmsUtil` y el nuevo `TextUtil.trazaCompleta` en «Clases disponibles» **y acotar en «Convenciones» la excepción de `NumeroTelefono`** (clase de instancia) |

### Paso 2 — `base/util`: `NumeroTelefono`, `MensajeSmsUtil` y `TextUtil.trazaCompleta`

**Ficheros:** `base/util/NumeroTelefono.java` (Crear), `base/util/MensajeSmsUtil.java` (Crear), `base/util/TextUtil.java` (Modificar), `base/util/CLAUDE.md` (Modificar).

- En `base/util/CLAUDE.md`, **tres cambios**:
  - añadir las dos entradas a «Clases disponibles», por orden alfabético, con el mismo formato que las
    demás (una línea por método público: de `NumeroTelefono`, solo `esMovilDeEspana` y `enFormatoE164`);
  - añadir a la entrada ya existente de `TextUtil` la línea de `trazaCompleta` («devuelve el stack trace
    completo de un `Throwable` como `String`; úsalo siempre que haya que guardar o mostrar el detalle
    de un error, en vez de reimplementar el `StringWriter`»);
  - **acotar la frase de «Convenciones»** que hoy dice «Son helpers **stateless**: métodos `static` y sin
    estado mutable», porque `NumeroTelefono` es la primera clase **de instancia** del paquete. Reescribirla
    como: «Son helpers **stateless**: métodos `static` y sin estado mutable. **Excepción**: una clase de
    instancia solo cuando encapsula un parseo caro que todas sus consultas reutilizan y cuyo estado es
    **inmutable** tras el constructor (`NumeroTelefono`).» MUST NOT dejar la convención diciendo una cosa
    y la lista de clases otra.
