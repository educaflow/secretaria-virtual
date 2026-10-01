---
type: implementation-task
template: system
---

# Tarea 05 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/base/util/TextUtil.java` | Modificar | k-code-quality | Un método más: `trazaCompleta(Throwable)` público, **movido** desde el privado de `CorreoServiceImpl` (único dueño de `descripcionUltimoFallo` para correos y SMS) |

### Paso 2 — `base/util`: `NumeroTelefono`, `MensajeSmsUtil` y `TextUtil.trazaCompleta`

**Ficheros:** `base/util/NumeroTelefono.java` (Crear), `base/util/MensajeSmsUtil.java` (Crear), `base/util/TextUtil.java` (Modificar), `base/util/CLAUDE.md` (Modificar).

#### `com.educaflow.base.util.TextUtil` (Modificar — delta)

```java
public static String trazaCompleta(Throwable excepcion);
```

`StringWriter` + `PrintWriter`: devuelve el stack trace completo (con causas) como `String`, tal cual lo escribe
`printStackTrace`. Es el **mismo** cuerpo que hoy es el privado `CorreoServiceImpl.trazaCompleta`
(`CorreoServiceImpl.java:386`), que el paso 3 **borra**: el método se **mueve** aquí, no se copia. A partir de
este delta, «qué texto se guarda en `descripcionUltimoFallo`» tiene **un** solo dueño para los dos subsistemas
(`k-code-quality/disenyo.md` §«Una decisión con varios dueños»), y `base/util/CLAUDE.md` obliga a mirar aquí
antes de reimplementar cualquier helper. **MUST NOT** reutilizarse `AsciiTableUtil.getStackTrace`: es privado y
devuelve una `List<String>` con otro formato (indentado para tabla), no la traza estándar. No cambia ninguna
otra línea de `TextUtil`.



## Eliminaciones declaradas

| Elemento eliminado | Fichero | Justificación |
|---|---|---|
| Método privado `trazaCompleta(Throwable)` de `CorreoServiceImpl` | `src/main/java/com/educaflow/subsystem/correos/service/impl/CorreoServiceImpl.java` | — (sin ID de spec). Se mueve a `TextUtil.trazaCompleta` (paso 2) |



6. **`trazaCompleta` se mueve de `CorreoServiceImpl` a `TextUtil`**; ningún servicio lo declara como privado. Ver paso 2.
