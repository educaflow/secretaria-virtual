---
name: sdd-specification-system-review
description: Revisa una `specification.md` ya existente (creada por `/sdd-specification-system` y probablemente editada a mano) sin regenerarla. Valida estructura de secciones, frontmatter, plantillas EARS de cada requisito, numeración local por patrón (`E-UB`, `E-EV`, `E-ST`, `E-UN`, `E-OP`), ubicación correcta de cada requisito según el árbol de decisión §2.4.2, marcas `*` de inferidos coherentes con "Asunciones a confirmar", **sección "Flujos principales" con IDs `F-NNN` narrativos** (1-3 frases, sin Given/When/Then ni nombres de pantalla/botón/campo), ausencia de tecnicismos prohibidos (V/R/U, FQN, JPQL, atributos XML) y coherencia entre entidades/operaciones/pantallas/flujos. Corrige mecánicamente lo que sea inequívoco; pregunta al usuario lo ambiguo. **No** rehace el spec desde la historia de usuario; **no** lanza subagentes en paralelo; **no** pregunta por alcance — preserva la intención de las ediciones manuales.
---

# sdd-specification-system-review

Eres un revisor de especificaciones funcionales. Tomas un `specification.md` ya existente — típicamente generado por `/sdd-specification-system` y editado a mano después — y lo dejas conforme con el contrato actual del skill `sdd-specification-system` (frontmatter, secciones, plantillas EARS, numeración, prohibiciones). **No regeneras nada**: trabajas sobre el contenido que hay, preservando la intención del autor.

---

## 1. Entrada y salida

### 1.1 Entrada

Un único fichero `specification.md` cuyo frontmatter debe contener `type: specification`. La carpeta que lo contiene es la carpeta de la iniciativa.

### 1.2 Salida

El **mismo** fichero `specification.md`, editado en sitio. No se crean ficheros nuevos, no se mueven, no se renombran. Si el fichero queda intacto tras la revisión (ya estaba conforme), se reporta así al usuario sin tocarlo.

---

## 2. Fase 0 — Localizar el fichero

Idéntica a la **Fase 0 del skill `sdd-specification-system`** (ver `.claude/skills/sdd-specification-system/SKILL.md` §4):

- Caso 1 — ruta explícita: valida frontmatter, detente si no es `type: specification`.
- Caso 2 — sin ruta: auto-detecta la última carpeta `.sdd/drafts/YYYY-MM-DD_HH-MM_*/`, busca `specification.md` dentro, pide confirmación con `AskUserQuestion` antes de proceder.

Apéndice A (overrides `--in=`, `--out=`, `--root=`) del skill original aplica igual.

---

## 3. Fase 1 — Cargar contrato y leer el fichero

1. Cargar mentalmente las reglas del contrato actual leyendo `.claude/skills/sdd-specification-system/SKILL.md` §§ 2.1, 2.3, 2.4 (plantillas EARS, árbol de decisión, numeración, inferidos, prohibiciones) y §7.2.3 (checklist).
2. Leer el `specification.md` completo.
3. Si el frontmatter no es `type: specification`, detente con el mismo mensaje de error que usa el skill original.

---

## 4. Fase 2 — Validaciones y correcciones

Las validaciones se ejecutan **en este orden**. Una vez detectado un problema:

- Si la corrección es **mecánica e inequívoca** (formato, prefijo, espacio, marca `*` faltante en bullet ya listado en "Asunciones a confirmar"), aplícala con `Edit`.
- Si la corrección requiere un juicio (¿este requisito es realmente `E-EV` o `E-UN`?, ¿estos dos bullets son el mismo requisito o dos distintos?), **pregunta al usuario con `AskUserQuestion`** ofreciendo las opciones razonables. Aplica la elección.

### 4.1 Estructura general del fichero

- Frontmatter `type: specification`.
- Secciones canónicas presentes (en cualquier orden razonable, pero todas las obligatorias):
  - **Obligatorias:** Especificación funcional, Entidades, Dependencias de otros subsistemas, Operaciones, **Flujos principales**, Pantallas, Menús, Seguridad, **Requisitos (EARS)**, Asunciones a confirmar.
  - **Opcionales (solo si aplica):** Máquina de estados, Campos calculados. No reportar como faltantes si no procede.
- Si encuentras la sección antigua "Reglas y validaciones" (formato previo a EARS), avisa al usuario: el fichero está en formato antiguo y la revisión no lo migra. Sugiere migración manual o relanzar `/sdd-specification-system` desde la historia de usuario. Detente.

**Política ante sección obligatoria faltante:**

- **Flujos principales** y **Requisitos (EARS)** tienen política propia detallada en §4.2 y §4.3 (no se regeneran; se avisa y se ofrece abortar).
- Para el resto de secciones obligatorias, **no regenerar el contenido** (es trabajo de `/sdd-specification-system`, no del review). Reportar la ausencia al usuario y ofrecer (a) abortar la revisión y relanzar `/sdd-specification-system` desde la historia de usuario, o (b) añadir un placeholder vacío con una nota `*(pendiente de completar)*` para que el usuario lo rellene a mano.
- Para **Asunciones a confirmar**: si falta y no hay ningún bullet inferido (`*`) en el resto del spec, añadir la sección vacía con la nota `*(ningún elemento inferido)*` (corrección mecánica). Si hay inferidos sin entrada, ver §4.5.

**Secciones no previstas:** si encuentras secciones que no están en la lista canónica ni son la legacy "Reglas y validaciones" (p.ej. `## Decisiones`, `## TODO`, notas del autor), **no las borres**: pregunta al usuario si forman parte del spec definitivo o si son notas de trabajo a archivar fuera.

### 4.2 Sección "Flujos principales"

- La sección **debe existir** y listar al menos un flujo. Si falta o está vacía, avisar al usuario: sin flujos principales el análisis no podrá generar `tests.md`. Ofrecer (a) abortar y relanzar `/sdd-specification-system` para añadirlos, o (b) continuar dejando la sección abierta (no recomendado).
- Cada bullet empieza por su ID `F-NNN` (con `*` antes si es inferido), seguido de `—` y la frase narrativa.
- Cada flujo se describe en **1 a 3 frases narrativas**. Si un bullet excede 3 frases, avisar y proponer dividirlo en varios flujos.
- Numeración local desde `F-001` sin huecos. Mismas reglas que para EARS sobre duplicados / huecos / IDs malformados (ver §4.4).
- **Prohibido en flujos**: nombres de pantalla concretos (mayúsculas o entre comillas), botones (`"Guardar"`, `"Rechazar"`), nombres de campo UI, mensajes de error literales, pasos numerados Given/When/Then. Si aparecen, avisar al usuario: el flujo está invadiendo territorio del análisis.
- Cada flujo inferido (`*F-NNN`) debe tener entrada en "Asunciones a confirmar". Mismas reglas que §4.5.

### 4.3 Sección "Requisitos (EARS)"

- Tiene 5 subsecciones posibles: `Ubicuos (E-UB)`, `Dirigidos por evento (E-EV)`, `Dirigidos por estado (E-ST)`, `Comportamiento no deseado (E-UN)`, `Características opcionales (E-OP)`. Una subsección vacía se omite (no se deja como sección vacía).
- Cada bullet empieza por su ID `E-XX-NNN` (con `*` antes si es inferido), seguido de `—` y la frase del requisito.
- Cada bullet sigue **literalmente** la plantilla del patrón:
  - `E-UB`: `El <sistema/entidad> debe <respuesta>.`
  - `E-EV`: `Cuando <trigger>, el <sistema/entidad> debe <respuesta>.`
  - `E-ST`: `Mientras <estado>, el <sistema/entidad> debe <respuesta>.`
  - `E-UN`: `Si <condición indeseada>, entonces el <sistema/entidad> debe <respuesta>.`
  - `E-OP`: `Donde <feature>, el <sistema/entidad> debe <respuesta>.`
- Si un bullet está en la subsección incorrecta según el árbol §2.4.2 (gana `E-UN` ante rechazos/errores, luego `E-OP`, `E-EV`, `E-ST`, `E-UB`), pregunta al usuario antes de moverlo.

### 4.4 Numeración

- Numeración local por patrón empezando en `001`.
- Detectar **duplicados** (dos bullets con el mismo ID): corregir reasignando ID o avisar al usuario si los dos contenidos describen el mismo requisito (fusionar) o requisitos distintos (renumerar el segundo).
- Detectar **huecos** (`E-EV-001`, `E-EV-003` sin `E-EV-002`): preguntar al usuario si el hueco es intencionado (regla borrada cuyo ID se conserva por trazabilidad) o si es un error de edición. Si es error y el spec todavía **no** ha sido consumido por `sdd-analyst-system` (no existe `analysis/` en la misma carpeta o el analyst aún no se ha lanzado), ofrecer renumerar; si ya hay análisis, **nunca** renumerar — los huecos se conservan y se documentan.
- Detectar **IDs malformados** (`EUB-001`, `E-UB-1`, `E-UB-01`): corregir al formato canónico `E-XX-NNN` con tres dígitos.

### 4.5 Inferidos (`*`)

- Cada bullet con `*` antes del ID debe tener una entrada en la sección "Asunciones a confirmar" que referencie ese ID. Si falta, añadirla con una justificación corta a partir de la propia frase del requisito (preguntar al usuario si la justificación no es obvia).
- Cada referencia en "Asunciones a confirmar" debe corresponder a un ID que existe (con o sin `*`). Si la entrada referencia un ID inexistente, preguntar si eliminarla o corregir el ID.

### 4.6 Prohibiciones (ver §2.3 del skill original)

Buscar y reportar (corregir cuando sea inequívoco, preguntar cuando no):

- Tipos Java (`String`, `LocalDateTime`, `Integer`, `boolean`, `Long`…) en cualquier sección.
- FQN `com.educaflow.*`, nombres de clase Java (`*Service`, `*Controller`, `*Impl`).
- Tipos del framework Axelor (`ActionRequest`, `ActionResponse`, `ModelService`, `@Inject`, `@CallMethod`).
- Nombres técnicos de acciones / vistas (`@Main-action`, `@All-action`, `@Search-grid`, `@View-form`).
- JPQL, SQL, Groovy, expresiones de dominio Axelor (`self.X = :user`, `eval:`).
- Atributos XML (`showIf`, `requiredIf`, `<action-attrs>`, `<action-record>`).
- Identificadores `V-XXX`, `R-XXX`, `U-XXX` o cualquier clasificación V/R/U dentro de la sección de requisitos. Si aparecen, avisar: la clasificación pertenece al análisis, no al spec.
- Pasos Given/When/Then numerados, nombres de pantalla concretos, botones o mensajes literales dentro de "Flujos principales". Si aparecen, avisar: el flujo está invadiendo el territorio del análisis (`tests.md`).
- Detalles de capa ("en el servicio", "en `validateInsert`", "en el controlador").
- Campos técnicos en Entidades (IDs, FKs internas, auditoría, versiones, flags de control).

### 4.7 Coherencia interna

- Cada entidad mencionada en Operaciones / Pantallas / Requisitos existe en la sección Entidades.
- Cada pantalla mencionada en Menús existe en la sección Pantallas.
- Cada estado mencionado en Requisitos (`Mientras una TareaCorreo está en ENVIADO …`) aparece en la Máquina de estados (si la entidad tiene una).
- Cada rol mencionado en Seguridad / Requisitos coincide con los tipos de usuario del proyecto (Administrador, Supervisor, Administrativa, Profesor, Exprofesor, Alumno, Exalumno, Familiar, Externo).
- Multicentro declarado en Seguridad coincide con lo que digan los requisitos `E-UB-*` sobre visibilidad por centro.

### 4.8 Checklist completo

Aplicar el **checklist §7.2.3 del skill original** entero, en este punto, sobre el resultado de las correcciones. Si algún punto falla y no se pudo resolver, listarlo en el informe final.

---

## 5. Fase 3 — Informe al usuario

Al terminar, mostrar un resumen estructurado:

```
Revisión de specification.md completada.

Correcciones aplicadas mecánicamente (N):
  - <lista corta>

Decisiones tomadas tras pregunta al usuario (N):
  - <lista corta con la decisión elegida>

Puntos del checklist que siguen abiertos (N):
  - <lista corta con el motivo>

Cambios escritos en: <ruta absoluta del specification.md>
```

Si el fichero no necesitaba ninguna corrección:

```
specification.md ya está conforme con el contrato actual. No se ha modificado nada.
```

---

## Apéndice A — Override de rutas (para testing)

Idéntico al Apéndice A del skill `sdd-specification-system`:

- `--in=<ruta>` — fichero `specification.md` de entrada explícito.
- `--out=<ruta>` — fichero de salida si se quiere escribir en otro sitio en vez de editar en sitio.
- `--root=<ruta>` — raíz alternativa a `.sdd/drafts/`.

En uso normal no se especifican.
