# Reglas de verificación del diseño de un sistema

Define **qué cuenta como fallo en un diseño**: la validación de los artefactos (XML contra su XSD) y las comprobaciones de cobertura, coherencia y seguridad; las comprobaciones específicas de las vistas (convenciones y **auditoría de layout**) están delegadas en `vistas.md` §3 (invocado desde §2.f). Lo aplica el **subagente verificador** sobre la carpeta `design/`; el **corrector** lo usa para saber qué arreglar.

**Contrato de salida del verificador:** si encuentra **cualquier** fallo, lo reporta de la forma más clara posible (qué falla, en qué fichero, por qué). Si **no** encuentra nada que corregir, responde **exactamente** `OK-CORRECTO`.

---

## 1. Validación mecánica de los artefactos XML (xmllint)

Cada fichero XML del diseño **MUST** validar contra su XSD; un XML que no valida es un **fallo bloqueante**.

Esta validación es **mecánica y determinista**: **la ejecuta el verificador** corriendo (con `Bash`) el script `validate.sh` de esta plantilla, en lugar de validar los XML "a ojo". **MUST** ejecutarlo; **MUST NOT** sustituirlo por una inspección manual. `validate.sh` es la **fuente de verdad**; está junto a este fichero y valida estos artefactos contra los XSD de AOP:

- **Dominios** (`design/domains/*.xml`) → `../axelor-open-platform/axelor-core/src/main/resources/domain-models.xsd`
- **Vistas y menús** (`design/views/*.xml`, `design/menus.xml`) → `../axelor-open-platform/axelor-core/src/main/resources/object-views.xsd`

```bash
# Desde la raíz del proyecto, con la carpeta del diseño a validar:
bash {ruta-de-esta-plantilla}/validate.sh .sdd/drafts/{iniciativa}/design
# Imprime "FAIL: <fichero>" (con el error de xmllint) por cada XML inválido,
# y como última línea "VALIDACION-XML: OK" (código 0) o "VALIDACION-XML: FAIL" (código ≠0).
```

Cada línea `FAIL: <fichero>` (o un código de salida `≠0`) es un **fallo bloqueante** que el verificador **MUST** reportar con el error de `xmllint` y el fichero afectado.

---

## 2. Comprobaciones del diseño

Cada punto que no se cumpla es un **fallo** a reportar (con su ubicación). Las referencias `design-contract.md §N` apuntan a las reglas que el diseño debía cumplir.

- **a) Estructura.** `design.md` con frontmatter `type: design` y las secciones canónicas (cabecera + metadatos, `## Ficheros a crear o modificar`, `## Pasos` en el orden obligatorio, la matriz de trazabilidad — ver `design-contract.md` §7, §8, §10). Un `domains/<Entidad>.xml` por entidad del spec; un `views/<Fichero>.xml` por `<action-view>`; `menus.xml`. Cada `rules/R-*.md` referenciado desde `design.md` y viceversa.
- **b) XML válido** (§1). El verificador **MUST** ejecutar `validate.sh` (no validar a ojo); cualquier `FAIL: <fichero>` o código de salida `≠0` es un fallo bloqueante.
- **c) Cobertura spec → V/R/U → ubicación.** Cada `RES-`/`VAL-`/`RN-`/`RUI-`/`CC-` del spec aparece como `Origen spec` de al menos una V/R/U en la matriz (o, para `CC-` de lectura, en un campo del modelo), **o** está en "Reglas del spec descartadas" con justificación. Cada ubicación referenciada en la matriz existe en un fichero real del diseño. Reportar: reglas del spec sin cubrir y entradas con referencia rota.
- **d) Frontera de confianza — AllowProperties y campos `servidor`** (`design-contract.md` §8.3 + `[[k-secure-coding]]` §3). Si hay acciones invocadas desde `@CallMethod`, la sección **MUST** existir. Las columnas `Origen` coherentes con las líneas `Input AllowProperties` y los `CC-` del spec (`design-contract.md` §3). Cualquier incumplimiento de `[[k-secure-coding]]` §3 es vulnerabilidad de mass-assignment. Detector mecánico del anti-patrón para campos `servidor`:
  ```bash
  grep -nE "if\s*\(.*==\s*null\s*\).*set[A-Z]" .sdd/drafts/{iniciativa}/design/design.md
  ```
  Cualquier coincidencia sobre un campo `servidor` es un fallo (la corrección es eliminar el `if`).
- **e) Reglas arquitectónicas** (`design-contract.md` §6). FQN coherentes (`com.educaflow.subsystem.X.…` / `com.educaflow.system.X.…`); ningún cuerpo Java de implementación en los comentarios de `design.md`; cada V/R/U en su capa correcta (`design-contract.md` §5); ningún módulo Guice para `ModelService`; ningún listener JPA para lógica de negocio; parámetros del controlador llamados `actionRequest`/`actionResponse`.
- **f) Vistas** (`vistas.md` §3). Aplica **todas** las comprobaciones y detectores de `vistas.md` §3 sobre `views/*.xml`, `menus.xml` y los resúmenes estructurales del `design.md`: estructura de ficheros y PI `sv-*`, patrón `buttons-panel`, validación remota global, coherencia acción ↔ método, cierre `save` → `back`, forms modales de detalle y la **auditoría de layout (ASCII Layout)** — reconstruir el ASCII Layout de cada `<form>` desde los `colSpan`/`colOffset` reales, pasarle el «Checklist de maquetación» de `k-vistas/forms.md` y comprobar que coincide con los ASCII Layout declarados en el `design.md`. Cada incumplimiento es un fallo a reportar (los de `buttons-panel` son **bloqueantes**).
- **g) Reglas R complejas** (`reglas-complejas.md`). Cada `R-` que cumple los criterios tiene su `rules/R-<Entidad>-NNN.md` (y viceversa); ningún `rules/R-*.md` con cuerpos Java.
- **h) Prohibiciones en `design.md`** (`design-contract.md` §1.1). Sin cuerpos de método Java, sin JPQL real, sin acoplamiento a `expedientes`/`tramites`.
- **i) Tests E2E** (`design/test-e2e-desc.md`, ver `tests-e2e.md`). Si el spec tiene escenarios, `test-e2e-desc.md` **MUST** existir y cada escenario del spec aparece como `Origen ESC` en al menos un test; cada `Verifica` y `Pantalla principal` referencia algo que existe. Si el spec no tiene escenarios, no se exige `test-e2e-desc.md`.
- **j) Coherencia diseño ↔ spec.** Los campos de cada `domains/<Entidad>.xml` coinciden con su `entity-*.md` (mismos nombres, mismos enums); las columnas/paneles de `views/*.xml` coinciden con su `screen-*.md`; los `<menuitem>` coinciden con los menús del spec.
- **k) Coherencia con las guías de diseño.** Si existe `design-guidelines.md`, el diseño **MUST** respetar lo que pide (encapsulaciones, nombres de clase/paquete/método prescritos, mecanismos obligatorios, patrones a evitar). Reportar cada guía incumplida con la ubicación de la fuga.
