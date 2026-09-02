---
type: implementation-task
---

# Tarea 01 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-datainit

## Fila de la tabla «Ficheros a crear o modificar» del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/resources/data-demo/input/documento_ejemplo_firma.pdf` | Crear | k-datainit | PDF de ejemplo (1 página) que usan las tareas de firma de demo |

## Texto del diseño (verbatim)

### Paso 1 — Recurso estático: el PDF de ejemplo a firmar

**Fichero:** `src/main/resources/data-demo/input/documento_ejemplo_firma.pdf` (Crear)

Documento PDF de **una sola página**, tamaño A4 (595 × 842 puntos), con un título corto en la parte superior
(p. ej. «Documento de ejemplo para pruebas de firma») y el resto de la página en blanco. **MUST** dejar libre la
banda donde las tareas de demo colocan el recuadro de la firma: `x=75`, `y=200`, `width=400`, `height=60`
(coordenadas PDF, origen abajo-izquierda), página `1`.

**MUST NOT** llevar campos de formulario, ni firmas previas, ni conformancia PDF/A (el firmado se hace en modo
*append*, así que un PDF plano es lo más seguro).

Va **junto a los datos de demo** (`src/main/resources/data-demo/input/`, la misma carpeta que `usuarios-demo.xml`),
porque `design-guidelines.md` lo dice literalmente: «las tareas de firma precargadas **y el PDF de ejemplo** son
datos de demo, así que su sitio natural es `src/main/resources/data-demo/`, no la `data-init` del subsistema».
Es el único consumidor que tiene: lo lee el `TareaFirmaDemoLoader` del Paso 10 y nadie más. **MUST NOT** ponerlo
en `src/main/resources/firma/` (donde vive `mi_certificado.p12`, que sí es un recurso del programa usado en
producción) ni en la `data-init` de `subsystem/firmas`.

Al colgar de `src/main/resources`, su **ruta de classpath** es `data-demo/input/documento_ejemplo_firma.pdf`, que
es la que usa el Paso 10.3.

**Verificación:** `ls src/main/resources/data-demo/input/documento_ejemplo_firma.pdf` y abrirlo: una página, sin firmas.

## Decisión tomada por el descomponedor ante una ambigüedad

El diseño describe el **contenido** del PDF pero no la herramienta con la que producirlo, y el fichero es
**binario**: no hay ningún fichero materializado en `design/` que copiar. Decisión: genéralo con cualquier
herramienta disponible en el entorno (por ejemplo la librería PDF que ya usa el proyecto, o LibreOffice
convirtiendo a PDF), respetando **exactamente** lo que exige el Paso 1: una sola página A4 (595 × 842 puntos),
un título corto arriba, resto en blanco, la banda `x=75`, `y=200`, `width=400`, `height=60` libre, sin campos de
formulario, sin firmas previas y sin conformancia PDF/A. Lo que se verifica es el fichero resultante, no el
procedimiento.
