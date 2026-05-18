---
name: sdd-create-user-story
description: Crea una nueva iniciativa SDD — genera la carpeta `.sdd/drafts/YYYY-MM-DD_HH-MM_{nombre-kebab-case}/` y dentro un `user-story.md` con frontmatter `type: user-story` (plantilla a rellenar) y un `design-guidelines.md` con frontmatter `type: design-guidelines` (esqueleto mínimo, opcional de rellenar). Es el primer paso del pipeline SDD; el output sirve de input a `/sdd-analyst-system`.
---

# sdd-create-user-story

Eres el paso de **arranque** del pipeline SDD. Tu única tarea es crear la carpeta de una iniciativa nueva en `.sdd/drafts/` con dos ficheros plantillados:

- `user-story.md` con frontmatter `type: user-story` — esqueleto detallado a rellenar.
- `design-guidelines.md` con frontmatter `type: design-guidelines` — esqueleto mínimo, **opcional**: solo se rellena si esta iniciativa concreta tiene desviaciones del default de los skills `k-*` (k-sistemas, k-vistas, k-seguridad…). Si no hay ninguna excepción, el usuario puede borrar el fichero.

**NO rellenas tú ninguno de los dos ficheros** — solo dejas los esqueletos para que el usuario los complete después.

## Argumentos aceptados

```
/sdd-create-user-story [nombre] [--root=<ruta>] [--out=<ruta>]
```

- `nombre` (opcional): nombre corto descriptivo de la iniciativa, en cualquier formato (palabras sueltas con espacios, kebab-case, etc.). Si se omite, se pide al usuario con `AskUserQuestion`.

### Override de rutas (para testing)

Para poder probar este skill en un sandbox alternativo sin tocar el árbol real (testing unitario del propio skill, iteración de mejoras, etc.), se aceptan en el prompt los siguientes overrides (también se reconocen las formas `raíz: <ruta>` y `salida: <ruta>`):

- `--root=<ruta>` — raíz alternativa a `.sdd/drafts/`. La iniciativa se creará en `<ruta>/{timestamp}_{nombre-kebab}/`.
- `--out=<ruta>` — carpeta de iniciativa explícita. Si se indica, **se usa esa ruta literal** para crear los ficheros y se ignoran tanto la raíz por defecto como el cálculo `{timestamp}_{nombre-kebab}`.

Reglas:
- Si la ruta indicada con `--out` ya existe, detente y avisa al usuario en vez de sobrescribir.
- Estos argumentos son **opcionales y para testing**: en uso normal no se especifican y la iniciativa se crea bajo `.sdd/drafts/`.

## Qué hacer

### 1. Obtener el nombre

- Si el usuario pasó nombre como argumento, úsalo.
- Si no, pregunta con `AskUserQuestion`: "¿Qué nombre corto le pones a esta iniciativa? (3–6 palabras descriptivas, por ejemplo `firma documentos`, `gestión de cargos`)". Acepta texto libre.

### 2. Normalizar el nombre a kebab-case

- A minúsculas.
- Quita acentos (`á`→`a`, `ñ`→`n`, etc.).
- Espacios y caracteres no alfanuméricos → `-`.
- Colapsa guiones consecutivos a uno solo y recorta guiones de los extremos.
- Si el resultado queda vacío, pide otro nombre.

Ejemplos:
- `Firma de documentos` → `firma-de-documentos`
- `Gestión de cargos` → `gestion-de-cargos`
- `subsistema correos` → `subsistema-correos`

### 3. Generar el timestamp

Ejecuta:
```bash
date +%Y-%m-%d_%H-%M
```

Obtienes algo como `2026-05-11_18-45`. Ese es el prefijo de la carpeta.

### 4. Verificar que la carpeta no exista

Calcula la ruta destino: `.sdd/drafts/{timestamp}_{nombre-kebab}/`.

Si esa carpeta ya existe (caso muy raro — colisión de timestamp), espera 1 minuto y vuelve a generar el timestamp, o pide al usuario que reintente.

### 5. Crear la carpeta y los ficheros plantillados

Crea la carpeta `.sdd/drafts/{timestamp}_{nombre-kebab}/` y dentro **dos ficheros**:

#### 5.1. `user-story.md`

Copia **literalmente** el contenido del fichero `templates/user-story.md` (relativo a la carpeta de este skill) y sustituye `{TÍTULO DESCRIPTIVO}` por una capitalización legible del nombre dado por el usuario — por ejemplo de `firma-de-documentos` → `Firma de documentos`.

#### 5.2. `design-guidelines.md`

Copia **literalmente** el contenido del fichero `templates/design-guidelines.md` (relativo a la carpeta de este skill). No adaptes el contenido al nombre de la iniciativa, no añadas secciones, no rellenes ejemplos concretos.

### 6. Mensaje final al usuario

Tras crear el fichero, indica al usuario:

```
Iniciativa creada: .sdd/drafts/{timestamp}_{nombre-kebab}/
Ficheros generados:
  - user-story.md          (obligatorio rellenar)
  - design-guidelines.md   (opcional — borrar si esta iniciativa no se desvía del default)

Próximos pasos:
  1. Abre el `user-story.md` y rellena las secciones marcadas con [corchetes].
  2. Si esta iniciativa tiene desviaciones del default de los skills `k-*`,
     anótalas en `design-guidelines.md`. Si no, borra el fichero.
  3. Cuando esté listo, lanza `/sdd-analyst-system` para producir el análisis funcional.
```

Sustituye `{timestamp}_{nombre-kebab}` por los valores reales.

## Cuándo parar y pedir ayuda

- Si la carpeta destino ya existe y un reintento de timestamp tampoco soluciona la colisión.
- Si el nombre normalizado queda vacío tras la limpieza (todos los caracteres eran no alfanuméricos).
- Si no se puede escribir en `.sdd/drafts/` (problema de permisos).

## Qué NO hacer

- **No rellenes la historia de usuario tú mismo.** La plantilla es para el usuario, no para ti. Si el usuario te dio una descripción más larga al invocar, igualmente deja la plantilla vacía y dile al usuario que la rellene él.
- **No rellenes el `design-guidelines.md` tú mismo.** Tampoco infieras desviaciones de los skills `k-*` a partir del nombre o la descripción. Deja el comentario plantillado tal cual; el usuario decidirá si lo rellena o lo borra.
- **No leas otros `user-story.md` ni otros `design-guidelines.md`** ni otros artefactos SDD como inspiración. Cada iniciativa empieza desde cero con las plantillas literales.
- **No invoques `/sdd-analyst-system`** automáticamente al terminar. El usuario decide cuándo lanzarlo.
- **No modifiques las plantillas** según el nombre dado. Las plantillas son siempre las mismas — las secciones son genéricas y se aplican a cualquier iniciativa.
