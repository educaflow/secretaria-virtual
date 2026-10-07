# Contrato del diseño de una feature libre

Lo lee el **diseñador** (README §2.1) para producir el diseño; el **juez** y el **enriquecedor** lo usan como criterio; el **verificador** y el **corrector**, para saber qué debía cumplirse.

**Principio de esta plantilla:** la **forma** del diseño es libre (no hay un esquema fijo de piezas), pero **el rigor no**: todo lo que el diseño decide **MUST** estar escrito, justificado y trazado, y la verificación sigue la regla de §3 `## 6`.

---

## 1. Qué es un diseño libre

Un diseño libre describe, **sin escribir el código**, qué piezas hay que crear o modificar, dónde, con qué firmas públicas y qué regla de la spec cumple cada una, de forma que `/sdd-implementer` pueda materializarlo sin tomar decisiones de diseño.

### 1.1 Prohibiciones en `design.md`

- **MUST NOT** contener cuerpos de método Java/Kotlin (sí firmas, responsabilidades, colaboradores, pasos en prosa).
- **MUST NOT** contener JPQL/SQL real (sí «filtra por los centros del usuario»).
- **MUST NOT** acoplar el dominio de `subsystem/expedientes` al motor `subsystem/tramitador` ni ir contra `agent_docs/architecture-rules.md`.
- **MUST NOT** dejar nada «a criterio del implementador»: si una decisión no está tomada, el diseño está incompleto.

### 1.2 Crear vs. Modificar

Lo normal en una feature libre es **modificar** partes existentes.

- Toda fila de la tabla de ficheros lleva `Acción: Crear` (el fichero no existe en el árbol) o `Acción: Modificar` (existe).
- Para una fila `Modificar`, el diseñador **MUST** leer el fichero real y describir el **delta**: qué se añade y qué cambia.
- Si algo se quita, **MUST** declararse en `## Eliminaciones declaradas`.
- Todo lo no mencionado del fichero real **se conserva**.

---

## 2. Cabecera del `design.md` (datos fijos)

El `design.md` empieza **exactamente** así:

```markdown
---
type: design
template: <valor copiado verbatim del frontmatter de specification.md>
---

# Diseño — <nombre de la iniciativa>

**Spec:** ../specification.md
**Ubicación del código:** <lista separada por comas de las carpetas, relativas a `src/main/java/com/educaflow/`, donde vive el código nuevo o modificado; otras rutas (p. ej. `src/main/resources/...`) se escriben completas>
**Carpeta de tests E2E:** src/test/e2e/<ruta>/
```

**CRITICAL — los dos campos son contrato con los skills de aguas abajo:**

- `**Ubicación del código:**` — el descomponedor de `/sdd-implementer` resuelve contra él las rutas relativas de la tabla de ficheros. **MUST** listar **todas** las carpetas tocadas.
  - ✅ CORRECTO: `**Ubicación del código:** base/web, system/gestioncentro, src/main/resources/axelor-config.properties`
  - ❌ INCORRECTO: `**Ubicación del código:** varios sitios` (no resoluble).
- `**Carpeta de tests E2E:**` — `/sdd-create-tests-e2e` persiste ahí los tests. **MUST** ser **una sola** carpeta bajo `src/test/e2e/` y elegirse con esta regla, en orden:
  1. Si **todo** el código vive en un único `<capa>/<sistema>` (`system/x` o `subsystem/x`) → `src/test/e2e/<capa>/<sistema>/` (la misma carpeta que usaría la plantilla de sistema).
  2. Si todo el código vive en `base/` → `src/test/e2e/base/<slug-de-la-feature>/`.
  3. Si toca varias partes → `src/test/e2e/transversal/<slug-de-la-feature>/`.
  El `<slug-de-la-feature>` es el nombre corto de la iniciativa en kebab-case (sin el timestamp).
  - ✅ CORRECTO: `src/test/e2e/transversal/aviso-global-mantenimiento/`
  - ❌ INCORRECTO: `src/test/e2e/2026-06-12_18-30_aviso-global/` (lleva timestamp), `src/test/e2e/aviso-global/` (sin la carpeta de familia), dos carpetas.

---

## 3. Secciones del `design.md` (en este orden)

Tras la cabecera, **todas** estas secciones, en este orden. Una sección que no aplique se escribe igualmente con `*(no aplica — <motivo>)*`.

### `## 1. Resumen de la solución`

Cinco a diez líneas: qué se construye y cómo encaja en la aplicación, en términos técnicos pero sin detalle de implementación.

### `## 2. Dónde vive el código y por qué`

**REQUIRED — es la sección que distingue un diseño libre bueno de uno improvisado.**
Para cada carpeta de `**Ubicación del código:**`: qué va ahí y **por qué ahí y no en las alternativas** (otra capa, un sistema nuevo, `base`, `tramites/util`).
**MUST** citar la regla de `agent_docs/architecture.md` / `architecture-rules.md` o el `CLAUDE.md` de carpeta que respalda la decisión.
Si crea un sistema o subsistema nuevo, **MUST** justificar por qué no se usó la plantilla de sistema (normalmente: porque es solo una parte pequeña de una feature más amplia).

- ✅ CORRECTO: «El banner va en `base/web` porque lo pinta el marco común de todas las pantallas y `base` MUST NOT depender de ningún sistema (C-3); la pantalla de administración va en `system/gestioncentro` porque es configuración del administrador, como el resto de esa carpeta.»
- ❌ INCORRECTO: «Se crea un paquete `avisos` en `base`» (sin alternativas ni regla que lo respalde).

### `## 3. Ficheros a crear o modificar`

Tabla con **todos** los ficheros que el implementador tocará:

| Fichero | Acción | Skill | Descripción |
|---|---|---|---|
| `base/web/AvisoGlobalBanner.java` | Crear | k-code-quality, k-secure-coding | Componente que inyecta el banner |
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | Añadir el menú de avisos |

- Rutas relativas a `src/main/java/com/educaflow/`; las que empiezan por `src/` van completas.
- `Skill`: los `k-*` que el implementador debe cargar para ese fichero. **MUST** incluir `k-secure-coding` y `k-code-quality` en todo fichero Java/Kotlin que toque entidades, servicios, controladores o permisos.
- Un fichero de **test unitario** (`src/test/java/...`) **también** va en la tabla, con `Acción: Crear` y una descripción que remita a `test-unit-desc.md`: `/sdd-implementer` lo genera **antes** que su pieza de producción (TDD).
- Los XML materializados (§4) van en la tabla con su ruta destino y la descripción «copiar literalmente desde `design/...`».

### `## 4. Piezas`

Una subsección `### <ruta o nombre de la pieza>` por cada fichero de producción de la tabla (los tests no: están en `test-unit-desc.md`). Para una clase:

- **Responsabilidad** (una frase; si necesita dos, son dos clases).
- **Firmas públicas** (bloque de código con solo las firmas, sin cuerpos).
- **Colaboradores** (qué recibe inyectado o usa, y de dónde sale).
- **Comportamiento** en prosa, paso a paso, citando los `REQ-` que aplica y los mensajes **exactos** de la spec.
- **Seguridad** si aplica: qué campos fija el servidor, qué `AllowProperties` por acción, qué comprobación de centro/rol (`k-secure-coding`).

Para un XML, un fichero de recursos o de configuración: qué contiene y, si se materializa en `design/`, la ruta.

### `## 5. Trazabilidad`

Una tabla, **Requisitos → piezas**: una fila por `REQ-NNN` de la spec: `| REQ | Pieza(s) que lo cumplen | Cómo se verifica (T-NNN y/o test unitario) |`. **Cada `REQ-` de la spec aparece** o está en «Requisitos descartados».

La cobertura `ESC-` → `T-NNN` (y si es manual) está en la tabla 6.2 de `## 6`.

Y debajo, `### Requisitos descartados`: los `REQ-` que el diseño decide no cumplir, con justificación (normalmente ninguno; si hay, es una errata de la spec que el motor devolverá).

### `## 6. Estrategia de verificación`

**CRITICAL — la sección que materializa la regla «TDD y E2E siempre que se pueda; si no, se justifica».**

**6.1 Tests unitarios (TDD).** Tabla con **todas** las piezas Java/Kotlin de producción de la tabla de ficheros:

| Pieza | ¿Tiene lógica? | Test unitario | Justificación si no |
|---|---|---|---|
| `base/web/AvisoGlobalBanner.java` | sí | `AvisoGlobalBannerTest` (TDD) | — |
| `domains/AvisoGlobal.xml` → entidad generada | no | — | POJO generado por Axelor, sin lógica |
| `system/gestioncentro/views/AvisoGlobal-form.xml` | no | — | XML de vista; se verifica por E2E (T-001, T-002) |

Reglas:
- «Tiene lógica» = cualquier rama, cálculo, validación, consulta o efecto. Un getter, un POJO generado, un XML, un fichero de propiedades no la tienen.
- Toda pieza con lógica **MUST** tener test unitario, salvo justificación.
  - Justificaciones válidas:
    - la lógica es inseparable de la infraestructura y no existe forma de mockearla en el proyecto (**MUST** decir cuál es esa infraestructura y por qué `mockStatic`/Mockito no sirven);
    - la pieza es un adaptador fino de una línea que solo delega.
  - Justificaciones inválidas:
    - «es trivial»;
    - «es difícil»;
    - «se prueba por E2E» (el E2E complementa, no sustituye);
    - «no da tiempo».
- Si la lógica está en un sitio no testeable (una vista, un controlador gordo), la solución es **moverla** a un servicio o helper testeable, no renunciar al test.

**6.2 Tests E2E.** Tabla con **todos** los `ESC-NNN` de la spec:

| ESC | T-NNN | Automatizable | Justificación si manual |
|---|---|---|---|
| ESC-001 | T-001 | sí | — |
| ESC-005 | T-005 | **no** (manual) | la firma abre AutoFirma__!! en la máquina del usuario |

Reglas:
- Cada `ESC-` tiene al menos un `T-NNN` (`tests-e2e.md`), también los manuales: se describen enteros y se persisten con la marca que la plantilla de `/sdd-create-tests-e2e` decida.
- Un escenario es manual **solo** si exige la intervención de una persona fuera del navegador (firma con certificado en cliente, leer un correo real, un dispositivo físico). El marcador `*(manual: …)*` de la spec se respeta; el diseño puede **añadir** uno si descubre la imposibilidad (y lo anota en `decisiones.md`), pero **MUST NOT** quitar uno de la spec sin corregir la spec (vía errata).
- **Justificaciones inválidas**: «es lento», «es complicado de pilotar», «depende de un cron» (un cron se sondea con timeout, ver la plantilla de ejecución de `/sdd-debug-with-test-e2e-desc`).

**6.3 Otras verificaciones.** Si la feature toca algo que los tests existentes del proyecto vigilan (reglas ArchUnit de `architecture-rules.md`, tests de vistas de `view-rules.md`, orden de métodos, CPD, CRAP), dilo aquí y cómo el diseño las respeta. `./gradlew clean build` las ejecuta todas: un diseño que las rompa no compila.

### `## 7. Pasos`

Lista numerada con el orden de implementación. **Regla TDD**: para cada pieza con test unitario, el paso del test va **inmediatamente antes** del paso de la pieza de producción, y el paso de producción dice «hasta que `<Clase>Test` pase».

Orden general:

1. XML materializados (dominios, vistas, menús, data-init).
2. (test → pieza) por cada pieza, respetando dependencias.
3. Configuración y recursos.

### `## 8. Eliminaciones declaradas`

Elementos preexistentes (métodos, campos, paneles, menús, propiedades) que el diseño **quita** a propósito de un fichero `Modificar`. Ausente o `*(no aplica)*` = nada se elimina. `/sdd-implementer` comprueba la conservación contra esta lista.

### `## 9. Tests E2E supersedidos`

Rutas de `.spec.ts` de iniciativas anteriores que esta feature invalida **a propósito**, con el `REQ-`/`ESC-` que lo justifica. `/sdd-create-tests-e2e` solo retira un test rojo ajeno si está aquí. Normalmente `*(no aplica)*`.

### `## 10. Tests`

Dos líneas que remiten a `test-e2e-desc.md` (tests E2E) y a `test-unit-desc.md` (tests unitarios, lo escribe el rol `test-unitarios` después).

---

## 4. XML materializados (solo si la solución los necesita)

Si la feature crea o modifica **entidades**, **vistas**, **menús** o **datos iniciales**, el diseñador **MUST** materializar esos XML en `design/` (no describirlos en prosa), igual que la plantilla de sistema, porque `/sdd-implementer` los copia literalmente:

| Qué | Dónde | Contrato |
|---|---|---|
| Entidad nueva o modificada | `design/domains/<Entidad>.xml` (fichero **completo** resultante si es `Modificar`) | `k-sistemas/modelos.md` |
| Vista nueva o modificada | `design/views/<Fichero>.xml` | `k-vistas` (un `<action-view>` por fichero, `buttons-panel`, ASCII Layout en el `design.md`) |
| Menús | `design/menus.xml` (solo los `<menuitem>` nuevos) | `k-vistas/menus.md` |
| Datos iniciales | `design/data-init/…` | `k-datainit` |

Cada XML **MUST** validar contra su XSD de AOP (`validacion.md` §1). Si la feature no toca nada de esto, no se crea ninguna de estas carpetas (sin error).

---

## 5. Reglas fijas del proyecto que la libertad NO relaja

- **MUST** respetar las convenciones de `CLAUDE.md` del proyecto y de los `CLAUDE.md` de carpeta, más `k-secure-coding`.
- Los tests de `src/test/java/com/educaflow/{architecture,views,ordenmetodos,tiposexpedientes}` **MUST** seguir pasando: el diseño no propone excepciones a esas reglas.

---

## 6. Las guías de diseño (`design-guidelines.md`)

Si existe, es **input obligatorio**: cada guía se respeta y se cita en «Dónde vive el código y por qué» o en la pieza afectada. En una feature libre suele contener justo las decisiones de ubicación («esto va en `base`», «reutilizad X»), así que **MUST** leerse antes de decidir §2.

---

## 7. Erratas de la spec

Si al diseñar aparece una errata pequeña en la spec (un ID mal, un nombre inconsistente, un mensaje duplicado con distinto texto), el diseñador la anota en `decisiones.md` con `**Cambio en la especificación:** PENDIENTE` como manda el motor, y sigue diseñando con la lectura corregida. **MUST NOT** editar la spec él mismo.

---

## 8. Decisiones mínimas que `decisiones.md` MUST recoger en una feature libre

El formato lo fija el motor; esta plantilla fija el **contenido mínimo**:

- al menos una decisión sobre la **ubicación del código** (§3 `## 2`);
- una sobre la **estrategia de verificación** si alguna pieza o escenario queda sin test;
- una por cada **patrón nuevo** que el proyecto no tuviera.

---

## 9. Checklist del diseñador

**MUST NOT** devolver `ESCRITO:` si queda algún punto sin cumplir. **LIMIT**: 3 iteraciones de autocorrección.

- [ ] ¿La cabecera tiene `type: design`, `template:` copiado verbatim, `**Ubicación del código:**` con todas las carpetas y `**Carpeta de tests E2E:**` elegida por la regla de §2?
- [ ] ¿Están las 10 secciones de §3, en orden, ninguna vacía sin su `*(no aplica — motivo)*`?
- [ ] ¿«Dónde vive el código y por qué» justifica cada carpeta frente a sus alternativas citando la regla que lo respalda?
- [ ] ¿Cada fichero de la tabla tiene `Acción` correcta contra el árbol real (`Crear` no existe / `Modificar` existe) y sus skills?
- [ ] ¿Cada pieza tiene responsabilidad, firmas sin cuerpos, colaboradores y comportamiento con los `REQ-` y mensajes exactos?
- [ ] ¿Cada `REQ-` de la spec está en la trazabilidad (o en descartados con justificación)? ¿Cada `ESC-` tiene su `T-NNN` en la tabla 6.2?
- [ ] ¿La «Estrategia de verificación» cubre **todas** las piezas y **todos** los escenarios, y cada «no» lleva una justificación **válida** según §3 `## 6`?
- [ ] ¿Los pasos siguen la regla TDD (test inmediatamente antes de su pieza)?
- [ ] ¿Se respetan las reglas fijas de §5 y las guías de §6?
- [ ] Si hay XML materializados: ¿validan con `xmllint` (`validacion.md` §1)?
- [ ] ¿`test-e2e-desc.md` cumple `tests-e2e.md` (incluido el campo `Manual` y la numeración desde la carpeta destino)?
- [ ] ¿`decisiones.md` recoge al menos las decisiones de §8?
- [ ] ¿No hay cuerpos Java, JPQL real ni nada «a criterio del implementador»?
