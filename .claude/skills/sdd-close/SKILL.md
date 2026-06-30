---
name: sdd-close
description: Último paso del pipeline SDD. Cierra una iniciativa ya implementada moviendo su carpeta de draft **verbatim** de `.sdd/drafts/<nombre>` a `.sdd/archive/<nombre>`. Como el resto de skills `/sdd-*`, primero identifica qué iniciativa cerrar (ruta explícita o la última por timestamp) y la confirma con el usuario. No genera ni reescribe documentación: el cierre es solo el archivado del draft.
---

# sdd-close

Eres el **paso de cierre** del pipeline SDD: tomas una iniciativa ya implementada y archivas su draft moviéndolo **verbatim** de `.sdd/drafts/` a `.sdd/archive/`. No produces documentación ni tocas el código: el cierre es **solo** el archivado de la carpeta.

Como todos los `/sdd-*`, lo primero es **identificar la iniciativa** sobre la que actúas (ruta explícita o la última por timestamp) y confirmarla. El resto es mover la carpeta.

---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty). Argumentos esperables:

- `ruta-iniciativa` (opcional, posicional 1): ruta a la carpeta del draft a cerrar. Si se omite, auto-detecta la última (§4.1).
- Flags de override `--in=`, `--out=`, `--root=` (Apéndice A).

Si los argumentos están vacíos, asume cierre del último draft.

---

## Outline

1. **Fase 0 — Localizar** el draft a cerrar y **confirmar** con el usuario (§4).
2. **Fase 1 — Archivar**: mover la carpeta del draft **verbatim** a `.sdd/archive/<nombre>` (§5).
3. **Fase 2 — Reportar** dónde quedó archivado el draft (§6).

**STOP conditions**:

- No se encuentra ninguna carpeta de iniciativa en `.sdd/drafts/` y el usuario no da ruta → **STOP** y avisa: no hay iniciativas que cerrar.
- El usuario rechaza el draft auto-detectado y no da ruta alternativa (§4.2) → **STOP**.
- El destino `.sdd/archive/<nombre>` **ya existe** → **STOP** y avisa: la iniciativa parece ya cerrada; **MUST NOT** sobrescribir (§5).

---

## 1. Entrada y salida

### 1.1 Entrada

- La carpeta del draft de la iniciativa en `.sdd/drafts/{YYYY-MM-DD_HH-MM_nombre}/`. No se lee su contenido: se mueve tal cual.

### 1.2 Salida

- La carpeta del draft movida **verbatim** a `.sdd/archive/<nombre>` (mismo nombre, con su timestamp).
- En la conversación: el reporte final (§6).

**MUST NOT** reescribir, corregir ni regenerar ningún artefacto del draft ni ninguna documentación del código: el draft es histórico inmutable y se mueve tal cual. **MUST NOT** tocar `.sdd/drafts/` salvo para mover la carpeta a `.sdd/archive/` en la Fase 1.

### 1.3 Estructura de carpetas

```
.sdd/
├── drafts/
│   └── 2026-06-28_10-00_firmas-bulk/     ← se MUEVE entera en la Fase 1
│       ├── specification.md
│       ├── analysis/ · design/ · implementation/ · test-e2e-desc/   (lo que haya)
└── archive/
    └── 2026-06-28_10-00_firmas-bulk/     ← destino del draft (verbatim, mismo nombre)
```

---

## 2. Principios (aplican a todas las fases)

### 2.1 El cierre es solo archivado

- El cierre **MUST** limitarse a mover la carpeta del draft a `.sdd/archive/`. **MUST NOT** generar, regenerar ni reescribir documentación (`CLAUDE.md`, modelos, imágenes), ni tocar el código de `src/main/...`.
- **MUST NOT** lanzar subagentes: es una operación de un único movimiento de carpeta.

### 2.2 Movimiento verbatim

- La carpeta se mueve **sin alterar su contenido** y con el **mismo nombre** (timestamp incluido). **MUST NOT** renombrar ni renumerar al archivar.

### 2.3 El cierre es semi-autónomo

La **única** pregunta al usuario es en la **Fase 0** (qué iniciativa cerrar, como todos los `/sdd-*`). A partir de ahí **MUST NOT** usar `AskUserQuestion`; solo las **STOP conditions** del Outline detienen la pasada.

---

## 3. Flujo general

```
Fase 0  Localizar el draft (ruta explícita | última por timestamp) ── confirmar con el usuario
   │
Fase 1  Mover .sdd/drafts/<nombre> ──► .sdd/archive/<nombre> (verbatim)
   │
Fase 2  Reportar al usuario
```

---

## 4. Fase 0 — Localizar y confirmar el draft

### 4.1 Localizar la iniciativa

Si el usuario no da `ruta-iniciativa`:

1. Lista las carpetas con formato de iniciativa:
   ```bash
   ls -d .sdd/drafts/[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]_[0-9][0-9]-[0-9][0-9]_*/ 2>/dev/null
   ```
2. Ordena alfabéticamente (el prefijo timestamp = orden cronológico) y toma la **última**.
3. Si ninguna existe → **STOP** y avisa: no hay iniciativas que cerrar.

Ejemplos de nombre de iniciativa:

- ✅ CORRECTO: `2026-05-21_14-30_firmas-bulk`
- ❌ INCORRECTO: `firmas-bulk_2026-05-21` (timestamp al final, no ordenable)
- ❌ INCORRECTO: `2026-5-21_14-30_firmas-bulk` (mes sin pad de cero; no cumple el patrón)

### 4.2 Confirmar con el usuario

Pregunta con `AskUserQuestion`:

> Voy a cerrar la iniciativa: `{nombre-iniciativa}`
> Se archivará el draft (verbatim) en `.sdd/archive/{nombre-iniciativa}`.
> ¿Continuamos?

Opciones: "Sí, cerrar esta iniciativa" / "No, quiero indicar otra ruta". Si "No", pide la ruta y vuelve a §4.1 con ella.

**MUST NOT** usar `mtime` ni elegir una carpeta que no sea la última por orden alfabético del timestamp.

---

## 5. Fase 1 — Archivar el draft (verbatim)

1. Determina el destino: `.sdd/archive/{nombre-iniciativa}` (mismo nombre que la carpeta del draft, con su timestamp). Crea `.sdd/archive/` si no existe.
2. Si el destino **ya existe** → **STOP** y avisa: la iniciativa parece ya cerrada; **MUST NOT** sobrescribir.
3. **Mueve** la carpeta entera del draft al destino (verbatim, sin alterar su contenido):
   ```bash
   git mv .sdd/drafts/{nombre-iniciativa} .sdd/archive/{nombre-iniciativa} 2>/dev/null \
     || mv .sdd/drafts/{nombre-iniciativa} .sdd/archive/{nombre-iniciativa}
   ```
   (`git mv` si está versionado; si no, `mv` normal.)

- ✅ CORRECTO: `.sdd/archive/2026-05-21_14-30_firmas-bulk/` (mismo nombre, timestamp incluido)
- ❌ INCORRECTO: renumerar o renombrar la carpeta al archivar (p.ej. `.sdd/archive/0007_firmas-bulk/`); el archivado es un **movimiento verbatim**, no una transformación.

---

## 6. Fase 2 — Reportar al usuario

Plantilla literal del mensaje final:

```text
Iniciativa cerrada: {nombre-iniciativa}

Draft archivado (verbatim) en: .sdd/archive/{nombre-iniciativa}/
```

---

## Quick Guidelines

- **El cierre es solo archivado** (§2.1): mover el draft a `.sdd/archive/`. **MUST NOT** generar ni regenerar documentación, ni tocar el código, ni lanzar subagentes.
- **Localizar** (§4): ruta explícita o la **última** iniciativa por timestamp, y **confirmar**. **MUST NOT** usar `mtime`.
- **Archivar verbatim** (§5): mover `.sdd/drafts/<nombre>` → `.sdd/archive/<nombre>` con el **mismo nombre**; si el destino existe → **STOP**. **MUST NOT** reescribir el draft.
- **Semi-autónomo** (§2.3): la única pregunta es la Fase 0 (qué iniciativa). Después, **MUST NOT** `AskUserQuestion`.

---

## Apéndice A — Override de rutas (para testing y versatilidad)

- `--in=<ruta>` — carpeta de la iniciativa (draft) de entrada explícita. Desactiva la auto-detección de §4.1.
- `--out=<ruta>` — carpeta de archivo alternativa a `.sdd/archive/` para el movimiento de la Fase 1.
- `--root=<ruta>` — raíz alternativa a `.sdd/` para resolver `drafts/` y `archive/`.

En uso normal no se especifican: se usa la última iniciativa, `.sdd/drafts/` y `.sdd/archive/`.