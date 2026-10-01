---
type: implementation-task
template: system
---

# Tarea 04 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality

### Fichero(s) de esta tarea (de «Ficheros a crear o modificar» de `design/design.md`)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/base/util/MensajeSmsUtil.java` | Crear | k-code-quality | `cabeEnUnSms(String)`: decide la codificación y cuenta GSM-7 (límite 160, extendidos 2 unidades) o UCS-2 (límite 70) |

### Paso 2 — `base/util`: `NumeroTelefono`, `MensajeSmsUtil` y `TextUtil.trazaCompleta`

**Ficheros:** `base/util/NumeroTelefono.java` (Crear), `base/util/MensajeSmsUtil.java` (Crear), `base/util/TextUtil.java` (Modificar), `base/util/CLAUDE.md` (Modificar).

#### `com.educaflow.base.util.MensajeSmsUtil`

```java
public static boolean cabeEnUnSms(String mensaje);
private static boolean esCodificableEnGsm7(String mensaje);
private static int contarUnidadesGsm7(String mensaje);
```

Helper stateless (`static`, sin estado) de la familia de `DniUtil`/`EMailUtil`/`IbanUtil`. **Único** dueño de
la decisión «este texto cabe en un solo SMS» (`design-guidelines.md`; `decisiones.md` D2): ni la vista ni el
modelo la replican.

- **`cabeEnUnSms(String)`** — aplica el algoritmo de `design-guidelines.md`, con un máximo de **una** parte (sin
  concatenación): si el texto es codificable en GSM-7, sus unidades GSM-7 han de ser ≤ 160; si no, se cuentan
  caracteres UCS-2 con `mensaje.length() <= 70` (`String.length()` cuenta unidades UTF-16, que es exactamente lo
  que cuenta UCS-2). El cuerpo es **una sola expresión** que compone los dos privados de abajo.
  **MUST** rechazar `null`, `""` y `" "` con
  `TextUtil.requireNonBlank(mensaje, "El mensaje del SMS es obligatorio")` (`com.educaflow.base.util`): el
  llamador (V-Sms-008) solo pregunta cuando el mensaje está indicado, así que un mensaje ausente o en blanco
  aquí es un error de programación. **MUST NOT** usarse `Objects.requireNonNull` sobre un `String`
  (`k-code-quality/java-idioms.md`): dejaría pasar `""` y `" "` y esta clase respondería «cabe» a un mensaje
  vacío. Es la misma llamada que ya usan `base.infrastructure.sms.Sms`, `TwilioCredential` y `SmsSenderFactory`.
- **`esCodificableEnGsm7(String)`** — `true` si **todos** los caracteres están en la tabla básica o en la
  extendida de GSM-7. Un solo carácter fuera de las dos (por ejemplo «á», «ó», «ú», que **no** pertenecen al
  alfabeto GSM-7) obliga a UCS-2.
- **`contarUnidadesGsm7(String)`** — suma las unidades del texto en GSM-7: 1 por carácter de la tabla básica y 2
  por carácter de la extendida (que viaja con un escape delante). Solo se llama cuando `esCodificableEnGsm7` es
  `true`.

Son **dos privados**, no dos pasos comentados dentro del público (`k-code-quality/metodos.md` §«Descomposición
de métodos»): decidir la codificación y contar unidades son dos responsabilidades, y el día que haga falta la
concatenación (153/67) solo cambia el público. Las dos tablas son **constantes privadas** de la clase, tomadas
de 3GPP TS 23.038:

```text
BASICO    = "@£$¥èéùìòÇ\nØø\rÅå" + "Δ_ΦΓΛΩΠΨΣΘΞ" + "ÆæßÉ"
          + " !\"#¤%&'()*+,-./" + "0123456789:;<=>?"
          + "¡ABCDEFGHIJKLMNO"  + "PQRSTUVWXYZÄÖÑÜ§"
          + "¿abcdefghijklmno"  + "pqrstuvwxyzäöñüà"
EXTENDIDO = "\f^{}\\[~]|€"      (cada uno cuenta 2 unidades)
```

### Verificación del Paso 2

**Verificar:** `./gradlew -q test` pasa; `grep -rn "libphonenumber" src/main/java | grep -v NumeroTelefono.java` no devuelve nada (la librería no se ha filtrado a ninguna otra clase).
