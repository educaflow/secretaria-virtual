---
type: implementation-task
template: system
---

# Tarea 12 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-skill
- k-secure-coding

Amplía la «Excepción — expedientes» de la regla multicentro para que incluya también el sistema que **crea**
expedientes (`system/ventanilla`). Son dos ficheros `Modificar` en una sola tarea porque el diseño exige el
**mismo texto en los dos sitios** para que sigan siendo coherentes:

- `.claude/skills/k-secure-coding/SKILL.md`, § 4 «Multi-centro / IDOR».
- `CLAUDE.md`, § «La aplicación».

Las dos son **ediciones quirúrgicas sobre ficheros que ya existen**: se añade `system/ventanilla` a la
enumeración de la excepción y **MUST NOT** tocarse nada más ni reescribirse el fichero. No hay tests que
reproyectar.

Al modificar un `SKILL.md`, **MUST** aplicarse el skill `k-skill` (reglas, frontmatter y convenciones de
redacción de los skills del proyecto).

## Filas de la tabla «Ficheros a crear o modificar»


| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `.claude/skills/k-secure-coding/SKILL.md` | Modificar | k-secure-coding | § 4: ampliar la «Excepción — expedientes» al sistema que crea expedientes (ver «Cambios necesarios fuera del sistema») |
| `CLAUDE.md` | Modificar | — | § «La aplicación»: la misma ampliación de la excepción multicentro, con el mismo texto |

## Texto del diseño — Paso 10, regla de acceso en lenguaje natural

Regla de acceso en lenguaje natural: **cualquier usuario con sesión iniciada** ve el menú y puede usar el asistente; el permiso solo le deja operar sobre la ficha de pantalla, que no se guarda. El alcance real —qué centros se le ofrecen, qué trámites, cómo puede presentar y para quién— lo decide el servidor en cada paso a partir de sus `CentroUsuario` y de `PerfilesUsuarioService`, **nunca** de `User.centroActivo`: el centro es el que el usuario elige en el paso 1, exactamente como en los expedientes que este asistente crea. Eso es la «Excepción — expedientes» de `[[k-secure-coding]]` § 4, que hoy enumera tres paquetes y **no** incluye ninguno de `system/**`; la ampliación al sistema que crea expedientes está declarada en el **punto 6** de «Cambios necesarios fuera del sistema» y **MUST NOT** darse por supuesta aquí. El Administrador no tiene aquí ningún alcance especial: se le tratan sus centros como a cualquier otro.

## Texto del diseño — «Cambios necesarios fuera del sistema», punto 6

Los dos primeros son ficheros de datos del proyecto; los **cuatro últimos** son **normativos** y se declaran aquí porque el diseño usa piezas y alcances reales del proyecto que su documentación todavía no recoge: una capacidad del fork de AOP (D4 de `decisiones.md`), cómo cancela un formulario maestro que no persiste (D8) y el alcance de la excepción multicentro de los expedientes. **MUST NOT** aplicarlos el diseñador: los aplica `/sdd-implementer`.

6. **`.claude/skills/k-secure-coding/SKILL.md` § 4 «Multi-centro / IDOR» + `CLAUDE.md` § «La aplicación»** — ampliar la **«Excepción — expedientes»** para que incluya también el sistema que **crea** expedientes. Los dos textos enumeran hoy exactamente tres paquetes (`subsystem/expedientes`, `subsystem/tramitador` y `tramites`) y para todo lo demás la regla vigente es la contraria: filtrar por `User.centroActivo`. Este diseño se comporta como la excepción —el centro es el que el usuario elige en el paso 1, entre los suyos, y el alcance son sus `CentroUsuario`— pero desde `system/ventanilla`, que no está en la lista, así que cualquier revisión posterior que aplique § 4 al paquete `system/**` marcaría como violación de una regla **CRITICAL** los centros candidatos del paso 1, los trámites del paso 2 y las dos validaciones del paso 3. Añadir `system/ventanilla` a la enumeración de la excepción, con el **mismo texto en los dos sitios** para que sigan siendo coherentes. No hay tests que reproyectar.
