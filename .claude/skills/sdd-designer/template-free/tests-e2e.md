# Parte del diseño: tests E2E

Como parte del diseño, **el diseñador** escribe `design_<n>/test-e2e-desc.md` a partir de los escenarios `ESC-NNN` de la spec.

**Cuándo se incluye:** siempre que `specification.md` contenga al menos un `ESC-NNN`. Si no tiene ninguno (muy raro en una feature libre: significaría que no tiene comportamiento observable), no se crea y la «Estrategia de verificación» del `design.md` lo justifica.

**Quién más lo usa** (`README.md` §2): el **verificador** comprueba que existe y cubre cada `ESC-NNN` (`validacion.md` §2.f); el **corrector** solo lo toca si un fallo lo afecta.

Cada `ESC-NNN` se convierte en uno o más tests `T-NNN` Given/When/Then en lenguaje de negocio, usando los nombres reales de pantallas, botones, campos y mensajes que el propio diseño fija. **MUST NOT** incluir comandos `playwright-cli` ni selectores CSS.

---

## 1. Reglas de materialización

- Numeración `T-NNN` de tres dígitos, global al fichero y sin huecos. **Arranca en el primer número libre de la carpeta destino** `**Carpeta de tests E2E:**` del `design.md`:
  ```bash
  ls <Carpeta de tests E2E>/t-*.desc.md 2>/dev/null
  ```
  Carpeta vacía o inexistente → `T-001`; carpeta con tests persistidos (otra iniciativa sobre la misma parte) → el siguiente al mayor `NNN`. **MUST NOT** rellenar huecos.
  **CRITICAL** — el `T-NNN` viaja intacto hasta el nombre del fichero persistido en una carpeta que **comparten** iniciativas; este es el único punto donde se evita el choque.
- Cada test declara en su cabecera: `Origen ESC` (mínimo 1), `Verifica` (lista de `REQ-NNN` que ejerce, o `—`), `Pantalla principal` (nombre de la pantalla o parte de la aplicación donde ocurre), `Tipo` (`happy` | `error` | `UI` | `no-regresion`) y **`Manual`** (`no`, o `sí — <motivo>`).
- **`Manual: sí — <motivo>`** se pone **solo** si el escenario de la spec lleva ` *(manual: <motivo>)*` o si la «Estrategia de verificación» del `design.md` lo declara manual con justificación. El motivo se copia. **MUST NOT** marcar manual un test por laborioso. Un test manual se describe **entero**, con todos sus pasos y aserciones: es el único registro ejecutable de ese escenario y una persona lo correrá.
- **Cobertura mínima obligatoria**: cada `ESC-NNN` de la spec aparece como `Origen ESC` en al menos un test. Un escenario con ramas puede dar más de un test.
- Pasos en lenguaje de negocio con `Dado`/`Cuando`/`Y`/`Entonces`, con nombres reales entrecomillados.
- Cada test es **autosuficiente e independiente**: login del actor, preparación de sus datos, acción, verificación, y **si cambió algo global** (una configuración, un aviso para todos) lo **deshace al final** para no contaminar a los demás.
- **Estado inicial de la base de datos**: `test-e2e-desc.md` **MUST** empezar con la sección `## Estado inicial de la base de datos` con los datos de «Recursos y datos iniciales» de la spec y la **tabla de usuarios** (`| Login | Contraseña | Rol / Tipo | Centro |`) de **cada** actor que inicia sesión en algún test, sacados de los datos de demo que la spec usó (`admin`/`admin` para el administrador).

- ✅ CORRECTO `Manual`: `no` · `sí — la firma abre AutoFirma__!! en la máquina del usuario`
- ❌ INCORRECTO: `Manual: sí — el flujo es muy largo` (laborioso ≠ manual), omitir el campo `Manual`.

---

## 2. Plantilla de `test-e2e-desc.md`

```markdown
# Tests E2E

Tests concretos end-to-end materializados a partir de los escenarios (`ESC-NNN`) de las historias de usuario del `specification.md` y de los requisitos (`REQ-NNN`) de la especificación.

Cada test es **independiente** (no depende del estado dejado por otro) y **trazable** (declara qué `ESC-NNN` materializa y qué `REQ-NNN` verifica). `/sdd-debug-with-test-e2e-desc` lo ejecuta contra la aplicación real tras la implementación; los `Manual: sí` los salta y los entrega a una persona.

---

## Estado inicial de la base de datos

Estado previo del que parten **todos** los tests. Ningún test puede presuponer más estado que este; cada test lo referencia en sus `Precondiciones`.

- <Dato de «Recursos y datos iniciales» de la spec, o «Solo los datos de demo»>

**Usuarios de acceso** (login y contraseña que `/sdd-debug-with-test-e2e-desc` usará para iniciar sesión):

| Login | Contraseña | Rol / Tipo | Centro |
|---|---|---|---|
| admin | admin | Administrador | — |
| <login de demo> | demo1234 | <rol> | <centro> |

---

## T-001 — <Nombre corto descriptivo del escenario>

**Origen ESC:** ESC-001
**Verifica:** REQ-002, REQ-005
**Pantalla principal:** <nombre de la pantalla o parte de la aplicación>
**Tipo:** happy | error | UI | no-regresion
**Manual:** no

### Precondiciones
- El usuario `<login>` ha iniciado sesión.
- (Si aplica) Existe <lo que el propio test crea antes, o nada más allá del estado inicial>.

### Pasos
1. **Dado** que el usuario está en la pantalla "<Pantalla>".
2. **Cuando** pulsa "<Botón>".
3. **Y** rellena "<Campo>" con "<valor>".
4. **Entonces** …

### Resultado esperado
- El sistema muestra el mensaje "<Mensaje exacto del REQ- de la spec>".
- <Estado resultante concreto>.
- (Si cambió algo global) El test lo deja como estaba: <qué deshace>.

---

## T-002 — <Otro escenario>

**Origen ESC:** ESC-005
**Verifica:** —
**Pantalla principal:** <…>
**Tipo:** happy
**Manual:** sí — <motivo verbatim del escenario de la spec>

### Precondiciones
- …

### Pasos
1. …
2. **Y** (paso manual) <lo que hace la persona, en una frase>.
3. …

### Resultado esperado
- …
```

---

## 3. Checklist de los tests

- [ ] ¿`test-e2e-desc.md` empieza con «Estado inicial de la base de datos» y su tabla de usuarios cubre a **cada** actor que inicia sesión?
- [ ] ¿Cada `ESC-NNN` de la spec aparece como `Origen ESC` en al menos un test?
- [ ] ¿Cada test tiene `Origen ESC`, `Verifica`, `Pantalla principal`, `Tipo` y **`Manual`**?
- [ ] ¿Cada `Manual: sí` corresponde a un ` *(manual: …)*` de la spec o a una justificación de la «Estrategia de verificación», y ninguno es «manual» por laborioso?
- [ ] ¿Cada `REQ-` de `Verifica` existe en la spec?
- [ ] ¿Cada pantalla, botón, campo o mensaje de los pasos existe en el diseño o en la spec (no inventado)?
- [ ] ¿Los pasos están en `Dado`/`Cuando`/`Y`/`Entonces`, sin selectores ni comandos?
- [ ] ¿Cada test es independiente, prepara lo suyo desde el login y deshace lo global que cambió?
- [ ] ¿La numeración `T-NNN` arranca en el primer libre de la `**Carpeta de tests E2E:**` del `design.md` (§1)?
