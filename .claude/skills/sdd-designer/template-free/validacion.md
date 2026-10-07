# Reglas de verificación del diseño de una feature libre

Define **qué cuenta como fallo en un diseño libre**: la validación mecánica de los XML que el diseño materialice (si los hay) y las comprobaciones de estructura, cobertura, arquitectura, **estrategia de verificación** y seguridad. Lo aplica el **verificador** sobre `design/`; el **corrector** lo usa para saber qué arreglar.

**Contrato de salida del verificador:** si encuentra **cualquier** fallo, lo reporta (qué falla, en qué fichero, por qué) en el formato JSONL que fija el skill. Si **no** encuentra nada, responde **exactamente** `OK-CORRECTO`.

**Solo cumplimiento.** El verificador **MUST NOT** reportar olores de diseño ni preferencias (son de los críticos, que pasan antes): aquí se comprueba que el diseño **cumple** el contrato, la spec y las reglas fijas del proyecto.

---

## 1. Validación mecánica de los XML materializados (xmllint)

Solo si `design/` contiene XML. Cada uno **MUST** validar contra su XSD de AOP; un XML que no valida es un **fallo bloqueante**. **MUST** ejecutarse con `Bash`, no «a ojo»:

```bash
# Desde la raíz del proyecto
XSD=../axelor-open-platform/axelor-core/src/main/resources
for f in .sdd/drafts/{iniciativa}/design/domains/*.xml; do
  [ -e "$f" ] && { xmllint --noout --schema "$XSD/domain-models.xsd" "$f" || echo "FAIL: $f"; }
done
for f in .sdd/drafts/{iniciativa}/design/views/*.xml .sdd/drafts/{iniciativa}/design/menus.xml; do
  [ -e "$f" ] && { xmllint --noout --schema "$XSD/object-views.xsd" "$f" || echo "FAIL: $f"; }
done
```

Los XML de `design/data-init/` se comprueban como XML bien formado (`xmllint --noout`) y contra las reglas de `k-datainit`. Si no hay ningún XML, esta sección no aplica (sin fallo).

---

## 2. Comprobaciones del diseño

Cada punto que no se cumpla es un **fallo** a reportar con su ubicación. Las referencias `design-contract.md §N` apuntan a la regla incumplida.

- **a) Estructura** (`design-contract.md` §2 y §3).
  - Frontmatter `type: design` + `template:` copiado de la spec.
  - Cabecera con `**Spec:**`, `**Ubicación del código:**` (todas las carpetas, resolubles) y `**Carpeta de tests E2E:**` (una sola, bajo `src/test/e2e/`, elegida por la regla de §2).
  - Las **10 secciones** en orden, ninguna ausente (las que no aplican, con `*(no aplica — motivo)*`).
  - `decisiones.md` y `log_*.txt` los pone el motor: no son fallos.
- **b) XML válidos** (§1), si los hay. Y cada XML materializado tiene su fila en la tabla de ficheros y viceversa.
- **c) Cobertura spec → diseño.** Cada `REQ-NNN` de la spec (índice y anexos) aparece en la tabla «Requisitos → piezas» o en «Requisitos descartados» con justificación; cada pieza referenciada existe en la tabla de ficheros y en «Piezas». Reportar: `REQ-` sin cubrir, piezas fantasma (la cobertura de `ESC-` se comprueba en f y g).
- **d) Ubicación justificada** (`design-contract.md` §3 `## 2`). Cada carpeta de `**Ubicación del código:**` tiene su justificación frente a alternativas con una regla citada. Y la ubicación **respeta** `agent_docs/architecture-rules.md` y los `CLAUDE.md` de carpeta: nada nuevo en `subsystem/tramitador` sin justificación de que es del motor; `base` sin dependencias hacia sistemas; `expedientes` sin dependencias hacia `tramitador`. Una pieza mal ubicada o una justificación ausente es un fallo.
- **e) Piezas completas** (`design-contract.md` §3 `## 4`). Cada fichero de producción tiene su subsección con responsabilidad, firmas sin cuerpos, colaboradores y comportamiento con `REQ-` y mensajes **exactos** (iguales a la spec). Nada «a criterio del implementador».
- **f) Tests E2E** (`tests-e2e.md`). Si la spec tiene escenarios, `test-e2e-desc.md` existe, empieza por «Estado inicial de la base de datos» con la tabla de usuarios, cada `ESC-` es `Origen ESC` de ≥1 test, cada test lleva los **cinco** campos de cabecera (incluido `Manual`), cada `Manual: sí` tiene su origen en la spec o en la estrategia de verificación, y la numeración arranca en el primer libre de la carpeta destino.
- **g) Estrategia de verificación justificada** (`design-contract.md` §3 `## 6`). **CRITICAL.**
  - La tabla 6.1 lista **todas** las piezas Java/Kotlin de producción de la tabla de ficheros.
  - Toda pieza con lógica tiene test o una justificación de las **válidas** (la infraestructura concreta que no se puede mockear, o adaptador de una línea).
  - La tabla 6.2 lista **todos** los `ESC-`.
  - Todo manual tiene una justificación válida (intervención humana fuera del navegador).
  - Reportar como fallo:
    - una pieza con lógica sin test y sin justificación;
    - una justificación de las **inválidas** («trivial», «difícil», «se prueba por E2E», «lento», «depende de un cron»);
    - un `ESC-` ausente;
    - un test unitario `src/test/java/...` previsto que no está en la tabla de ficheros.
- **h) Pasos TDD** (`design-contract.md` §3 `## 7`). Cada pieza con test unitario tiene su paso de test **inmediatamente antes** de su paso de producción, y este dice «hasta que `<Clase>Test` pase». Orden coherente con las dependencias.
- **i) Reglas fijas** (`design-contract.md` §5). `SecurityUtil.getUser()` y no `AuthUtils`; multicentro según `CLAUDE.md`; `AllowProperties` declarado para toda acción que reciba datos del cliente y campos `servidor` asignados incondicionalmente (`k-secure-coding` §3) — detector mecánico del anti-patrón:
  ```bash
  grep -nE "if\s*\(.*==\s*null\s*\).*set[A-Z]" .sdd/drafts/{iniciativa}/design/design.md
  ```
  cualquier coincidencia sobre un campo `servidor` es un fallo; propiedades de configuración en `axelor-config.properties`; nada de `i18n_*.csv`.
- **j) Prohibiciones** (`design-contract.md` §1.1). Sin cuerpos Java/Kotlin, sin JPQL real, sin acoplar `expedientes` → `tramitador`.
- **k) Coherencia con el árbol real** (`design-contract.md` §1.2). Cada fila `Modificar` referencia un fichero que **existe** (léelo, nunca lo escribas) y su delta no pierde nada preexistente que no esté en «Eliminaciones declaradas»; cada fila `Crear` referencia uno que **no existe**.
- **l) Coherencia con las guías.** Si existe `design-guidelines.md`, cada guía se respeta (y se cita donde aplica). Reportar cada guía incumplida.
- **m) Coherencia con los tests existentes del proyecto** (`design-contract.md` §3 `## 6.3`). Si el diseño toca vistas, la estructura respeta `agent_docs/view-rules.md`; si toca servicios/controladores, el orden de métodos de `k-sistemas`; si cruza capas, `architecture-rules.md`. Un diseño que las rompería es un fallo (no compilaría en `./gradlew clean build`).
