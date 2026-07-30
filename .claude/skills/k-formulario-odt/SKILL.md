---
name: k-formulario-odt
description: Formato XML de definición de formularios ODT de la secretaría virtual (documento/seccion/fila/campo/check/texto con hijos valenciano/castellano, rejilla de 12 columnas, campos inline ${...}, rowSpan, XSD formulario-odt.xsd incluido) y sus dos operaciones - generar el .odt desde el XML con el script scripts/xml2odt.py incluido en este skill, y transcribir un .odt existente (formato antiguo GVA) a este XML. Úsalo siempre que se pida crear, modificar o transcribir uno de estos formularios ODT rellenables.
allowed-tools: Read, Write, Edit, Bash(python3 .claude/skills/k-formulario-odt/scripts/xml2odt.py:*), Bash(python3 .claude/skills/k-formulario-odt/scripts/odt2pdf.py:*), Bash(unzip:*), Bash(xmllint:*)
---

# k-formulario-odt

Los formularios oficiales (GVA) de la secretaría virtual se definen en un XML compacto y un script Python los convierte en `.odt` de LibreOffice Writer con controles de formulario rellenables. Este skill documenta el formato del XML, cómo generar el `.odt` y cómo transcribir un `.odt` antiguo al XML. Los ejemplos reales viven en `disenyo-grafico/documentos/` (`prueba.xml`, `permiso_licencia.xml`, `justificacion_falta_profesorado.xml`).

---

## 1. Conceptos clave

- El `.odt` generado es **una única tabla** sobre una rejilla lógica de **12 columnas** (19 cm: página A4 con márgenes de 1 cm), con la cabecera corporativa (logo GVA + título bilingüe) y secciones con letra automática (A, B, C…) en celda gris.
- Todo texto visible es **bilingüe** (el castellano se renderiza en cursiva) y va **siempre** en los elementos hijos `<valenciano>` y `<castellano>` del elemento (`titulo`, `seccion`, `campo`, `check`, `texto`). Un hijo omitido o vacío omite ese idioma.
- El formato está descrito por el XSD `formulario-odt.xsd` **dentro de este skill**. Todo XML de definición **MUST** referenciarlo en la raíz con `xsi:noNamespaceSchemaLocation` (ruta relativa desde el XML hasta el XSD).
- Cada `<campo>`/`<check>` produce un **control de formulario** (`form:text`/`form:checkbox`) cuyo `form:name` es el `nombreCampo` **literal**. El motor de relleno evalúa ese nombre como expresión, por eso son válidos `self.dni`, `String.valueOf(self.anyo)`, `self.tipo==com.educaflow...Enum.VALOR` (checks) o `true` (casilla siempre marcada).
- El script canónico es `scripts/xml2odt.py` **dentro de este skill** (con sus `scripts/assets/`: logo GVA y plantilla de estilos). No regeneres esos assets.

---

## 2. Formato del XML

### 2.1 Estructura

```xml
<?xml version="1.0" encoding="UTF-8"?>
<documento xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:noNamespaceSchemaLocation="../../.claude/skills/k-formulario-odt/formulario-odt.xsd">
    <titulo>
        <valenciano>...</valenciano>
        <castellano>...</castellano>
    </titulo>
    <seccion>
        <valenciano>...</valenciano>
        <castellano>...</castellano>
        <fila>
            <campo nombreCampo="self.x" colspan="5">
                <valenciano>...</valenciano>
                <castellano>...</castellano>
            </campo>
            <check nombreCampo="self.y" colspan="3">
                <valenciano>...</valenciano>
                <castellano>...</castellano>
            </check>
            <texto colspan="4">
                <valenciano>...</valenciano>
                <castellano>...</castellano>
            </texto>
        </fila>
    </seccion>
</documento>
```

(La ruta del `xsi:noNamespaceSchemaLocation` del ejemplo es la relativa desde `disenyo-grafico/documentos/`; ajústala a la ubicación del XML.)

### 2.2 Elementos

| Elemento | Genera | Atributos | Hijos |
|---|---|---|---|
| `<titulo>` | Fila de cabecera: logo GVA + título bilingüe centrado | — | `<valenciano>`, `<castellano>` (opc.) |
| `<seccion>` | Fila con letra gris automática (A, B, C…) + título | — | `<valenciano>`, `<castellano>` (opc.), `<fila>` |
| `<fila>` | Una o varias líneas de la tabla | — | `campo`/`check`/`texto` |
| `<campo>` | Etiqueta bilingüe en mayúsculas + campo de texto rellenable | `nombreCampo`, `colspan`, `rowSpan` (opc.) | `<valenciano>`, `<castellano>` (opc.) |
| `<check>` | Casilla (ocupa 1 columna) + etiqueta bilingüe al lado | `nombreCampo`, `colspan`, `rowSpan` (opc.) | `<valenciano>`, `<castellano>` (opc.) |
| `<texto>` | Párrafos bilingües sin control | `colspan`, `rowSpan` (opc.) | `<valenciano>`, `<castellano>` (opc.) |

### 2.3 Reglas

- **MUST**: los `colspan` de una `<fila>` suman **12 o un múltiplo de 12**. Cada grupo que suma 12 es una línea; las líneas siguientes se apilan debajo **dentro del mismo rectángulo** (sin borde entre ellas). Un elemento **MUST NOT** cruzar el límite de 12.
- `colspan` admite decimales (`colspan="3.5"`).
- `rowSpan` (≥1, admite decimales) hace más alta la fila y su campo de texto. Úsalo para campos multilínea y para el recuadro de firma.
- **Campos inline**: dentro de los hijos `<valenciano>`/`<castellano>` de cualquier elemento, `${nombre;n}` se sustituye por un campo rellenable llamado `nombre` de `n` columnas de ancho (admite decimales). Si aparece en ambos idiomas se generan **dos controles con el mismo nombre**; al convertir con `odt2pdf.py` quedan como un único campo PDF con dos widgets (mismo nombre, mismo valor en ambos).
- `nombreCampo` con comillas dobles (expresiones como `"    " + f(x)`) → escribe el atributo XML con comillas simples: `nombreCampo='"    " + com...sha256(self.justificante)'`.
- Un `<campo>` sin hijos `<valenciano>`/`<castellano>` (o vacíos) es un campo **sin etiqueta** (útil para apilar varios campos en un rectángulo).
- **MUST NOT** poner `valenciano`/`castellano` como atributos de ningún elemento (formato antiguo): el script aborta con ERROR y el XSD no valida.

### 2.4 Ejemplos ✅/❌

- ✅ CORRECTO: `<fila>` con `colspan` 5 + 5 + 2 (suma 12).
- ✅ CORRECTO: `<fila>` con cuatro `<texto colspan="12">` (suma 48 = 4 líneas apiladas en un rectángulo).
- ✅ CORRECTO: `<fila>` con 6 + 2.1 + 3.9 (decimales, suma 12).
- ✅ CORRECTO: `<valenciano>Jornada parcial. De ${self.horaInicio;1.1} hores a ${self.horaFin;1.1} hores</valenciano>`.
- ❌ INCORRECTO: `<fila>` con 5 + 3 + 2 (suma 10; el script aborta con ERROR).
- ❌ INCORRECTO: `<fila>` con 8 + 8 (el segundo elemento cruza el límite de 12).
- ❌ INCORRECTO: `${self.hora}` (falta el ancho; la sintaxis es `${nombre;n}`).
- ❌ INCORRECTO: `<campo nombreCampo="self.x" valenciano="Nom" castellano="Nombre" colspan="4"/>` (formato antiguo: los idiomas van siempre como elementos hijos, también en `titulo`/`seccion`).

---

## 3. Generar el .odt desde el XML

1. Valida el XML contra el XSD del skill:
   ```bash
   xmllint --noout --schema .claude/skills/k-formulario-odt/formulario-odt.xsd <definicion>.xml
   ```
2. Ejecuta:
   ```bash
    python3 .claude/skills/k-formulario-odt/scripts/xml2odt.py <definicion>.xml [<salida>.odt] [--field-height=n] [--check-height=m]
   ```
   Si no se indica `<salida>.odt`, se usa el nombre del XML cambiando la extensión a `.odt`.
   Parámetros opcionales (alturas en cm, positivas):
   - `--field-height=n` — alto de los campos de texto rellenables, tanto los de `<campo>` como los inline `${...}` (defecto **0.541**). El alto de una fila con campos se calcula como parte fija de la etiqueta (0.31: padding superior 0.05 + línea del label al 80% ~0.23 + separación inferior 0.03) + este alto (+ el extra de `rowSpan`); el campo queda pegado a la etiqueta y casi pegado al borde inferior de la celda sin llegar a taparlo. Si el `<campo>` no tiene etiqueta (sin hijos `<valenciano>`/`<castellano>` o vacíos) la fila es más baja: el campo se centra verticalmente y el alto es este alto + un pequeño hueco arriba y abajo (0.1 por lado).
   - `--check-height=m` — alto total mínimo de una línea cuyo contenido son **solo** checks, uno o varios (defecto **0.818**). En líneas mixtas (checks con campos/textos) se ignora y se usa el estándar. Se evalúa por línea (cada grupo que suma 12), no por `<fila>`. La casilla dibuja 0.745 cm fijos, así que con el padding la línea no baja de ~0.95 cm aunque `m` sea menor.
3. **MUST NOT** sobrescribir el `.odt` original/antiguo del que partiste al transcribir: si en la carpeta existe un original con el mismo nombre que el XML, indica una `<salida>` explícita con sufijo `-generado.odt` (el nombre por defecto lo machacaría).
4. Verifica el resultado renderizándolo y **mirando la imagen** (usa el skill `libreoffice-writer`):
   ```bash
   python3 .claude/skills/libreoffice-writer/scripts/odt_verify.py <salida>.odt -r 120
   ```
5. Comprueba en el render: anchos de celda coherentes con los `colspan`, casillas pegadas a su etiqueta, campos inline presentes (se ven como hueco en blanco: los controles de texto no tienen borde), y paginación (si un bloque cae a la página siguiente, ajusta `rowSpan`).
6. Si además hace falta el PDF, convierte el `.odt` con el script del skill (usa LibreOffice headless con perfil temporal propio, así funciona aunque haya otro LibreOffice abierto):
   ```bash
   python3 .claude/skills/k-formulario-odt/scripts/odt2pdf.py <documento>.odt [<salida>.pdf]
   ```
   Si no se indica `<salida>.pdf`, se usa el nombre del `.odt` cambiando la extensión a `.pdf`. El PDF conserva los controles como campos AcroForm y, si el `.odt` tiene controles con el mismo nombre, deshace el renombrado `_2`, `_3`... de LibreOffice fusionándolos en un único campo con varios widgets (mismo nombre original).

---

## 4. Transcribir un .odt antiguo al XML

Receta para convertir un formulario `.odt` de formato antiguo (hecho a mano en la GVA) en el XML:

1. Renderiza el `.odt` a imagen (`odt_verify.py`, ver §3) y estudia su estructura visual.
2. Extrae y formatea su contenido:
   ```bash
   unzip -p <doc>.odt content.xml > /tmp/content.xml   # formatea con xml.dom.minidom para leerlo
   ```
3. Lista los controles (`grep -o 'form:name="[^"]*"' `): el orden de declaración da `control1..N`; anota tipo (`form:text`/`form:checkbox`) y nombre. **MUST** copiar los nombres **literales** al `nombreCampo`, aunque sean expresiones (`String.valueOf(...)`, `==Enum.X`, `true`).
4. Convierte los anchos: con las `style:rel-column-width` de las columnas, cada celda ocupa `suma_rel_celda * 12 / suma_rel_total` columnas → ese es su `colspan`. Redondea a enteros o a un decimal limpio (2.1, 3.9) según lo que claramente quiso el autor.
5. Mapea cada fila de la tabla:
   - Fila de título con logo → `<titulo>`.
   - Fila-cabecera de bloque (fondo/negrita, texto "Val / Cast") → `<seccion>` separando los dos idiomas en sus hijos `<valenciano>`/`<castellano>`.
   - Celda con etiqueta + control → `<campo>`; celda con casilla + texto → `<check>`; celda solo texto → `<texto>`; los idiomas van siempre en los hijos `<valenciano>`/`<castellano>`.
   - Varios bloques apilados en una misma celda o celdas sin borde entre sí → misma `<fila>` con varias líneas de 12 (campos extra sin etiqueta si no la tienen).
   - Control incrustado en medio de una frase → `${nombre;n}` con `n = ancho_cm * 12 / 19` (12 / ancho de tabla; en los `.odt` antiguos, de márgenes más anchos, la tabla solía medir 17).
   - Celda notablemente alta (campo multilínea, recuadro de firma) → `rowSpan` (≈ alto_cm / 0.6 unidades extra).
6. Detalles de equivalencia: los enlaces van como texto plano (no hay hipervínculos en el XML); si una URL aparece una sola vez compartida, ponla solo en `castellano`; texto solo en un idioma → omite el otro hijo.
7. Valida: genera un `.odt` de prueba con el script (§3) — si aborta con ERROR revisa las sumas de `colspan` — y compara el render con el original.
8. Avisa al usuario de lo que **no** se conserva al regenerar: el estándar nuevo añade letras de sección y cabeceras bilingües en dos líneas, y unifica tipografías; el documento puede quedar más alto y repaginar.

---

## 5. Anti-patrones

- **MUST NOT** editar a mano el `content.xml` de un `.odt` generado: cambia el XML de definición y regenera.
- **MUST NOT** usar el `.odt` antiguo como salida del script ni "mejorar" sus textos al transcribir: la transcripción es literal (incluidos errores del original; señálalos al usuario aparte).
- **MUST NOT** duplicar el script fuera de este skill; si hay que cambiarlo, se cambia `scripts/xml2odt.py` aquí.
- **MUST NOT** inventar `nombreCampo`: si el control del documento original se llama `true` o es una expresión, se copia tal cual.

---

## Quick Guidelines

- Rejilla de 12 columnas; cada `<fila>` suma 12 o un múltiplo de 12 (múltiplo = líneas apiladas en el mismo rectángulo).
- Idiomas: siempre elementos hijos `<valenciano>`/`<castellano>`, en todos los elementos (`titulo`, `seccion`, `campo`, `check`, `texto`); nunca atributos.
- Todo XML referencia el XSD del skill (`formulario-odt.xsd`) con `xsi:noNamespaceSchemaLocation` y se valida con `xmllint --schema`.
- `colspan` y `rowSpan` admiten decimales; la casilla de un `<check>` ocupa siempre 1 columna.
- `${nombre;n}` = campo inline de `n` columnas dentro de cualquier texto bilingüe.
- `nombreCampo` es literal y puede ser una expresión; con comillas dobles dentro, atributo con comillas simples.
- Generar: `python3 .claude/skills/k-formulario-odt/scripts/xml2odt.py in.xml out-generado.odt`; verificar SIEMPRE renderizando con `odt_verify.py` y mirando la imagen.
- Transcribir ODT→XML: render + `content.xml` + `form:name` literales + anchos relativos → colspans; validar generando.