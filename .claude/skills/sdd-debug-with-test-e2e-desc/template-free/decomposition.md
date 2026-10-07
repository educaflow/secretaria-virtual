# Descomposición: de `test-e2e-desc.md` a la carpeta `test-e2e-desc/` (feature libre)

Lo lee el **descomponedor** (README §2.1). Tarea: trocear el `test-e2e-desc.md` en **un fichero autocontenido por test** + un **índice con checkbox por test**, respetando el campo `Manual` de cada test.

---

## 1. Qué leer y qué escribir

1. Lee el `test-e2e-desc.md` íntegro. Identifica:
   - La **cabecera común**: todo lo anterior al primer `## T-`, en particular `## Estado inicial de la base de datos` con la tabla de credenciales (`| Login | Contraseña | Rol / Tipo | Centro |`). Guárdala **verbatim**.
   - Los **bloques de test**: cada `## T-NNN — <nombre>` hasta el siguiente `## T-`, con sus cinco campos de cabecera (`Origen ESC`, `Verifica`, `Pantalla principal`, `Tipo`, **`Manual`**) y sus secciones. **Verbatim**.
2. Escribe en `{iniciativa}/test-e2e-desc/` un `t-NNN-<slug>.desc.md` por test (§2) y el índice (§3).

**MUST NOT** reescribir, resumir, renumerar ni «mejorar» nada. Eres un troceador, no un autor. Si a un test le falta el campo `Manual`, **no lo inventes**: trátalo como `Manual: no` y anótalo en la respuesta (es un fallo del diseño).

---

## 2. Plantilla de `t-NNN-<slug>.desc.md`

**CRITICAL — autocontenido**: el ejecutor recibe **solo este fichero**: lleva la cabecera común además del bloque del test. Los `### Precondiciones` / `### Pasos` / `### Resultado esperado` del bloque fuente suben un nivel a `##` en este fichero; es el único cambio sobre el verbatim.

**Nombre**: `t-NNN` = `T-NNN` en minúsculas con tres dígitos; `<slug>` = el nombre en kebab-case (sin acentos ni signos, separadores colapsados a `-`); extensión `.desc.md`. Ej.: `T-001 — Publicar un aviso y que lo vea un usuario de otro centro` → `t-001-publicar-un-aviso-y-que-lo-vea-un-usuario-de-otro-centro.desc.md`.

```markdown
---
type: test-e2e
id: T-NNN
---

# T-NNN — <nombre del test, tal cual>

**Origen ESC:** <verbatim>
**Verifica:** <verbatim>
**Pantalla principal:** <verbatim>
**Tipo:** <verbatim>
**Manual:** <verbatim: `no` o `sí — <motivo>`>

## Estado inicial de la base de datos

<la sección COMPLETA y VERBATIM del test-e2e-desc.md, incluida la tabla **Usuarios de acceso**>

## Precondiciones

<verbatim>

## Pasos

<verbatim>

## Resultado esperado

<verbatim>
```

- ✅ CORRECTO: `t-003-un-aviso-desactivado-no-se-muestra.desc.md` con `id: T-003`, los cinco campos, la cabecera común y el bloque verbatim.
- ❌ INCORRECTO: omitir `## Estado inicial de la base de datos` o el campo `Manual`; `t-3-...`; slug con mayúsculas; fusionar dos tests.

---

## 3. Plantilla del índice `tests-e2e-desc.md`

Una línea por test, en orden. El estado inicial depende del campo `Manual`:

| Línea | Cuándo | Significado |
|---|---|---|
| `- [ ]` | `Manual: no` | pendiente; el motor lo ejecuta |
| `- [-] … — manual: <motivo>` | `Manual: sí — <motivo>` | **no automatizable**: el motor lo salta y lo reporta aparte |

**MUST NOT** escribir ningún `- [x]` al crear el índice.

```markdown
---
type: test-e2e-index
---

# Tests E2E — <nombre de la iniciativa>

Índice de los tests E2E de esta iniciativa. Cada test vive en su propio fichero autocontenido. `[x]` = pasó contra la aplicación real; `[-]` = no automatizable (requiere una persona; se salta y se reporta como MANUAL). Lo gestiona `/sdd-debug-with-test-e2e-desc`.

- [ ] [T-001 — <nombre>](t-001-<slug>.desc.md)
- [ ] [T-002 — <nombre>](t-002-<slug>.desc.md)
- [-] [T-003 — <nombre>](t-003-<slug>.desc.md) — manual: <motivo, en una línea>
```

- ✅ CORRECTO: `- [-] [T-005 — El usuario firma el documento](t-005-el-usuario-firma-el-documento.desc.md) — manual: la firma abre AutoFirma__!! en la máquina del usuario`
- ❌ INCORRECTO: dejar `- [ ]` un test `Manual: sí` (atascaría al ejecutor), o `- [-]` uno `Manual: no` (se quedaría sin depurar en silencio), `- [x]` al crear, destino que no coincide con el fichero.

---

## 4. Token de salida

- Primera línea **exactamente** `ESCRITO: test-e2e-desc/`.
- Una línea `=== TESTS ===` y debajo **una línea por test**: `{fichero} | {T-NNN} | {nombre}`.
- Si a algún test le faltaba el campo `Manual` (§1), una línea más por cada uno indicándolo.
- **MUST NOT** pegar el contenido de los ficheros.

---

## 5. Checklist del descomponedor

**LIMIT**: 3 iteraciones de autocorrección.

- [ ] ¿Se leyó el `test-e2e-desc.md` íntegro y se localizaron la cabecera común y todos los `## T-NNN`?
- [ ] ¿Exactamente un `t-NNN-<slug>.desc.md` por test, con `t-NNN` de tres dígitos y slug en kebab-case?
- [ ] ¿Cada fichero tiene frontmatter `type: test-e2e` + `id: T-NNN`, los **cinco** campos de cabecera y la sección «Estado inicial de la base de datos» verbatim?
- [ ] ¿El bloque del test se copió verbatim?
- [ ] ¿El índice tiene `type: test-e2e-index`, una línea por test en orden, y el estado de cada una cuadra con su `Manual` (`- [-]` con motivo para `sí`, `- [ ]` para `no`), sin ningún `[x]`?
- [ ] ¿La respuesta sigue el token de salida de §4?
