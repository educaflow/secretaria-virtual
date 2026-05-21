---
name: sdd-create-user-story
description: Crea una nueva iniciativa SDD — genera la carpeta `.sdd/drafts/YYYY-MM-DD_HH-MM_{nombre-kebab-case}/` y dentro un `user-story.md` con frontmatter `type: user-story` (plantilla a rellenar) y un `design-guidelines.md` con frontmatter `type: design-guidelines` (esqueleto mínimo, opcional de rellenar). Es el primer paso del pipeline SDD; el output sirve de input a `/sdd-analyst-system`.
handoffs:
  - label: Generar especificación funcional
    agent: sdd-specification-system
    prompt: Generar la especificación funcional a partir del user-story.md recién creado
---

# sdd-create-user-story

Paso de **arranque** del pipeline SDD. Crea la carpeta de una iniciativa nueva en `.sdd/drafts/` y copia dentro dos ficheros plantillados desde `templates/` de este skill. **MUST NOT** rellenar el contenido — solo deja los esqueletos para que el usuario los complete después. El output sirve de input a `/sdd-specification-system`.

---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty). Argumentos esperables:

- `nombre` (opcional, posicional): nombre corto descriptivo de la iniciativa, en cualquier formato (palabras sueltas con espacios, kebab-case, etc.). Si se omite, se pide al usuario con `AskUserQuestion`.
- `--root=<ruta>` / `--out=<ruta>` (opcionales, **solo para testing**): ver Apéndice A.

---

## Outline

1. **Obtener** el nombre de la iniciativa (argumento o `AskUserQuestion`).
2. **Normalizar** el nombre a kebab-case.
3. **Generar** el timestamp `YYYY-MM-DD_HH-MM`.
4. **Calcular** la ruta destino y verificar que no exista.
5. **Crear** la carpeta y copiar literalmente las dos plantillas.
6. **Mostrar** el mensaje final con los próximos pasos.

**STOP conditions**:

- El nombre normalizado queda vacío tras la limpieza (todos los caracteres eran no alfanuméricos) → **ERROR** y pide otro nombre al usuario.
- La ruta destino ya existe y un reintento de timestamp tampoco resuelve la colisión → **STOP** y avisa al usuario en vez de sobrescribir.
- No se puede escribir en `.sdd/drafts/` por permisos → **ERROR** y detente.
- El usuario pasa un `--out=<ruta>` que ya existe → **STOP** y avisa al usuario, **MUST NOT** sobrescribir.

---

## 1. Entrada y salida

### 1.1 Entrada

Texto libre con el nombre de la iniciativa (o vacío, en cuyo caso se pregunta).

### 1.2 Salida

Una carpeta nueva con dos ficheros plantilla copiados literalmente:

```
.sdd/drafts/{timestamp}_{nombre-kebab}/
├── user-story.md          (obligatorio rellenar)
└── design-guidelines.md   (opcional — borrar si no hay desviaciones)
```

---

## 2. Principios

### 2.1 Plantillas inalteradas

**MUST** copiar el contenido de `templates/user-story.md` y `templates/design-guidelines.md` **literalmente**. La única sustitución permitida es `{TÍTULO DESCRIPTIVO}` en `user-story.md` por la capitalización legible del nombre. **MUST NOT** adaptar el contenido al nombre de la iniciativa, añadir secciones ni rellenar ejemplos.

### 2.2 Sin contaminación cruzada

**MUST NOT** leer otros `user-story.md`, `design-guidelines.md` u otros artefactos SDD como inspiración. Cada iniciativa empieza desde cero con las plantillas literales.

### 2.3 El usuario decide cuándo continuar

**MUST NOT** invocar `/sdd-specification-system` ni ningún otro skill SDD automáticamente al terminar. Limítate a mostrar el mensaje final con los próximos pasos.

---

## 3. Fase 1 — Obtener el nombre

- Si el usuario pasó nombre como argumento posicional, úsalo.
- Si no, pregunta con `AskUserQuestion`: "¿Qué nombre corto le pones a esta iniciativa? (3–6 palabras descriptivas, por ejemplo `firma documentos`, `gestión de cargos`)". Acepta texto libre.

---

## 4. Fase 2 — Normalizar el nombre a kebab-case

Aplica en orden:

1. A minúsculas.
2. Quita acentos (`á`→`a`, `ñ`→`n`, etc.).
3. Espacios y caracteres no alfanuméricos → `-`.
4. Colapsa guiones consecutivos a uno solo y recorta guiones de los extremos.

Si el resultado queda vacío → **ERROR** y vuelve a la Fase 1 pidiendo otro nombre.

**Ejemplos**:

- ✅ CORRECTO: `Firma de documentos` → `firma-de-documentos`
- ✅ CORRECTO: `Gestión de cargos` → `gestion-de-cargos`
- ✅ CORRECTO: `subsistema correos` → `subsistema-correos`
- ❌ INCORRECTO: `Firma de documentos` → `Firma-de-documentos` (no aplicó minúsculas)
- ❌ INCORRECTO: `Gestión de cargos` → `gestión-de-cargos` (no quitó acentos)
- ❌ INCORRECTO: `subsistema  correos` → `subsistema--correos` (no colapsó guiones consecutivos)

---

## 5. Fase 3 — Generar el timestamp

Ejecuta:

```bash
date +%Y-%m-%d_%H-%M
```

Obtienes algo como `2026-05-11_18-45`. Ese es el prefijo de la carpeta.

---

## 6. Fase 4 — Verificar que la carpeta no exista

Calcula la ruta destino: `.sdd/drafts/{timestamp}_{nombre-kebab}/` (o la indicada por `--out` / `--root`; ver Apéndice A).

Si esa carpeta ya existe (caso muy raro — colisión de timestamp), regenera el timestamp y reintenta. **LIMIT**: máximo 3 reintentos. Si tras el 3.º reintento sigue colisionando → **STOP** y avisa al usuario.

---

## 7. Fase 5 — Crear la carpeta y los ficheros plantillados

Crea la carpeta destino y dentro **dos ficheros**:

> **Nota sobre las plantillas**: el contenido literal de ambos ficheros vive en `templates/user-story.md` y `templates/design-guidelines.md` (adyacentes a este `SKILL.md`). **MUST NOT** embeber su contenido aquí — se evita duplicar la fuente de verdad y mantenerlas en sincronía. **MUST** leerlas en tiempo real desde disco al ejecutar este skill.

### 7.1 `user-story.md`

**MUST** copiar literalmente el contenido del fichero `templates/user-story.md` y sustituir `{TÍTULO DESCRIPTIVO}` por una capitalización legible del nombre dado por el usuario.

**Ejemplos**:

- ✅ CORRECTO: `firma-de-documentos` → `{TÍTULO DESCRIPTIVO}` se sustituye por `Firma de documentos`.
- ✅ CORRECTO: `gestion-de-cargos` → `{TÍTULO DESCRIPTIVO}` se sustituye por `Gestión de cargos` (re-añade acentos legibles).
- ❌ INCORRECTO: sustituir `{TÍTULO DESCRIPTIVO}` por `firma-de-documentos` (sin capitalizar, con guiones).

### 7.2 `design-guidelines.md`

**MUST** copiar literalmente el contenido del fichero `templates/design-guidelines.md`. No hay sustituciones que hacer en este fichero.

---

## 8. Fase 6 — Mensaje final al usuario

Tras crear los ficheros, muestra al usuario:

```
Iniciativa creada: .sdd/drafts/{timestamp}_{nombre-kebab}/
Ficheros generados:
  - user-story.md          (obligatorio rellenar)
  - design-guidelines.md   (opcional — borrar si esta iniciativa no se desvía del default)

Próximos pasos:
  1. Abre el `user-story.md` y rellena las secciones marcadas con [corchetes].
  2. Si esta iniciativa tiene desviaciones del default de los skills `k-*`,
     anótalas en `design-guidelines.md`. Si no, borra el fichero.
  3. Cuando esté listo, lanza `/sdd-specification-system` para producir la
     especificación funcional.
```

Sustituye `{timestamp}_{nombre-kebab}` por los valores reales.

---

## 9. PROHIBIDO

- **MUST NOT** rellenar la historia de usuario ni el `design-guidelines.md` tú mismo, ni siquiera si el usuario te dio una descripción larga al invocar. Las plantillas se entregan intactas.
- **MUST NOT** inferir desviaciones de los skills `k-*` a partir del nombre o la descripción.
- **MUST NOT** sobrescribir una carpeta o un `--out=<ruta>` que ya exista.

---

## 10. Checklist final

Aplica antes de mostrar el mensaje final al usuario. **LIMIT**: máximo 3 iteraciones de corrección.

- [ ] ¿El nombre se ha normalizado correctamente a kebab-case (minúsculas, sin acentos, sin guiones consecutivos, sin guiones en los extremos)?
- [ ] ¿La carpeta destino existe ahora en el filesystem?
- [ ] ¿Contiene exactamente dos ficheros: `user-story.md` y `design-guidelines.md`?
- [ ] ¿`user-story.md` es copia literal de `templates/user-story.md` con la única sustitución de `{TÍTULO DESCRIPTIVO}`?
- [ ] ¿`design-guidelines.md` es copia literal de `templates/design-guidelines.md` sin modificaciones?
- [ ] ¿No has añadido contenido inferido del nombre o de lo que dijo el usuario al invocar?
- [ ] ¿El mensaje final referencia `/sdd-specification-system` como próximo paso (no `/sdd-analyst-system`)?

Si tras 3 iteraciones algún punto sigue fallando, documenta el problema y avisa al usuario.

---

## Quick Guidelines

- Crea `.sdd/drafts/{timestamp}_{nombre-kebab}/` con dos ficheros copiados literalmente desde `templates/` (única sustitución: `{TÍTULO DESCRIPTIVO}` en `user-story.md`).
- Normaliza el nombre a kebab-case: minúsculas + sin acentos + no-alfanuméricos a `-` + colapso y trim de guiones.
- **LIMIT**: máximo 3 reintentos de timestamp ante colisión; tras el 3.º, **STOP** y avisa.
- **MUST NOT** rellenar el contenido de los ficheros ni leer otros artefactos SDD como inspiración.
- **MUST NOT** invocar `/sdd-specification-system` automáticamente — el usuario decide cuándo lanzarlo.
- Si la ruta destino (por defecto o `--out=`) ya existe, **STOP** sin sobrescribir.

---

## Apéndice A — Override de rutas (para testing)

Para poder probar este skill en un sandbox alternativo sin tocar el árbol real:

- `--root=<ruta>` — raíz alternativa a `.sdd/drafts/`. La iniciativa se creará en `<ruta>/{timestamp}_{nombre-kebab}/`.
- `--out=<ruta>` — carpeta de iniciativa explícita. Si se indica, **MUST** usar esa ruta literal para crear los ficheros e **MUST** ignorar tanto la raíz por defecto como el cálculo `{timestamp}_{nombre-kebab}`.

Reglas:

- Si la ruta indicada con `--out` ya existe → **STOP** y avisa al usuario; **MUST NOT** sobrescribir.
- En uso normal no se especifican: la iniciativa se crea bajo `.sdd/drafts/`.