---
type: implementation-task
template: system
---

# Tarea 03 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/base/util/NumeroTelefono.java` | Crear | k-code-quality | Teléfono: validar, saber si es móvil, si es de España y dar el E.164. Única clase que importa `libphonenumber` |

Los pasos 2 y 3 son prerrequisitos técnicos (`base/util` y `base/infrastructure`) del dominio y del servicio: se intercalan entre «ficheros estáticos y recursos» y «dominios» del orden obligatorio, porque el `domains/Sms.xml` y el `SmsServiceImpl` no se pueden compilar sin ellos.

### Paso 2 — `base/util`: `NumeroTelefono`, `MensajeSmsUtil` y `TextUtil.trazaCompleta`

**Ficheros:** `base/util/NumeroTelefono.java` (Crear), `base/util/MensajeSmsUtil.java` (Crear), `base/util/TextUtil.java` (Modificar), `base/util/CLAUDE.md` (Modificar).

#### `com.educaflow.base.util.NumeroTelefono`

```java
public NumeroTelefono(String telefono);
private boolean esValido();
private boolean esMovil();
private boolean esDeEspana();
public boolean esMovilDeEspana();
public String enFormatoE164();
```

Encapsula un teléfono escrito por una persona. Es la **única** clase del proyecto que importa
`libphonenumber`, y ninguno de sus métodos públicos expone un tipo de esa librería (ni `PhoneNumber`, ni
`PhoneNumberUtil`, ni `PhoneNumberType`, ni `NumberParseException`): el objetivo es poder cambiar la librería
sin tocar el resto del código (`design-guidelines.md`). País por defecto: España (`"ES"`). **Estado:** un
campo privado con el número ya parseado (tipo de `libphonenumber`), `null` si el texto no se pudo parsear; se
parsea **una** vez en el constructor y todas las consultas lo reutilizan.

- **`NumeroTelefono(String)`** — no lanza nunca; si el texto (incluido `null`/blanco) no se puede parsear con el
  país por defecto, deja el número interno a `null`.
- **`esValido()`** (privado) — `true` si hay número parseado y la librería lo da por válido; nunca lanza.
- **`esMovil()`** (privado) — `true` si hay número parseado y su tipo es `MOBILE` o `FIXED_LINE_OR_MOBILE`
  (`963000000` → `false`); nunca lanza.
- **`esDeEspana()`** (privado) — `true` si hay número parseado y su prefijo de país es el 34 (`+33…` → `false`);
  nunca lanza.
- **`esMovilDeEspana()`** — la conjunción de los tres privados (RES-Sms-004); nunca lanza. Ver `decisiones.md` D1.
- **`enFormatoE164()`** — devuelve el E.164 (`+34600111222`); precondición `esMovilDeEspana()`, si no lanza
  `IllegalStateException` (**MUST NOT** devolver `null` ni el texto original). Su único llamador es R-Sms-001,
  después de V-Sms-006. Ver D1.

**Verificar:** `./gradlew -q test` pasa; `grep -rn "libphonenumber" src/main/java | grep -v NumeroTelefono.java` no devuelve nada (la librería no se ha filtrado a ninguna otra clase).

Decisión de diseño citada: `design/decisiones.md` D1.
