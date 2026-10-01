---
type: implementation-task
template: system
---

# Tarea 05 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality

### Fichero(s) de esta tarea (de «Ficheros a crear o modificar» de `design/design.md`)

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

### Eliminaciones declaradas — fila relacionada (de `design/design.md`)

| Elemento eliminado | Fichero | Justificación |
|---|---|---|
| Método privado `trazaCompleta(Throwable)` de `CorreoServiceImpl` | `src/main/java/com/educaflow/subsystem/correos/service/impl/CorreoServiceImpl.java` | — (sin ID de spec: `k-code-quality/disenyo.md` §«Una decisión con varios dueños»). **Sale del fichero, no desaparece**: su cuerpo se mueve tal cual a `TextUtil.trazaCompleta` (paso 2), público, y las dos llamadas (correos y SMS) van ahí. Evita que «qué texto se guarda en `descripcionUltimoFallo`» tenga un dueño por subsistema, justo lo que D3 decide para el mecanismo asíncrono. Se aprovecha que el paso 3 reescribe este fichero igualmente |

### Notas y supuestos 8 (de `design/design.md`)

8. **`trazaCompleta` se MUEVE a `TextUtil`, y ningún servicio lo declara ya como privado.** No se copia de correos a SMS: el paso 3 reescribe `CorreoServiceImpl` de todas formas (campo inyectado, fusión de los dos `fireActionRule_Programar*Asincrono`, imports), así que «tocar correos una segunda vez» dejó de ser un coste. Con la copia, «qué texto se guarda en `descripcionUltimoFallo`» tendría un dueño en cada subsistema; con el método en `base/util` tiene uno solo, y `base/util/CLAUDE.md` —que obliga a mirar ahí antes de reimplementar cualquier helper— lo anuncia. **MUST NOT** reutilizarse `AsciiTableUtil.getStackTrace`: es privado y devuelve otra cosa (una `List<String>` indentada para tabla), no la traza estándar. Ver la fila `Modificar` de `base/util/TextUtil.java` y la de `base/util/CLAUDE.md` en «Ficheros a crear o modificar», y la entrada correspondiente de «Eliminaciones declaradas».

### Verificación del Paso 2

**Verificar:** `./gradlew -q test` pasa; `grep -rn "libphonenumber" src/main/java | grep -v NumeroTelefono.java` no devuelve nada (la librería no se ha filtrado a ninguna otra clase).

### Decisión del descomponedor (no es texto del diseño)

Esta tarea solo **añade** `trazaCompleta` a `TextUtil` (fila `Modificar`): el resto de la clase se conserva intacto. El borrado del privado de `CorreoServiceImpl` y el cambio de su llamada son de la tarea de `CorreoServiceImpl`.
