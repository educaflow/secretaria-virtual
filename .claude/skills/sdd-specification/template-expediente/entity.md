# Modelo: <Nombre>

<Descripción en lenguaje de negocio: qué representa. El expediente no repite aquí su ciclo de vida (vive en estados.md); una entidad hija dice de quién cuelga y si se borra con su padre.>

## Campos

<!-- Campos funcionalmente relevantes, SIN tipo de dato y SIN campos técnicos. MUST NOT redeclarar los comunes de todo expediente (número de expediente, nombre, centro, estado, historial, persona solicitante, persona interesada): se usan por su nombre cuando hagan falta. Cada campo indica CUÁNDO SE RELLENA: en qué transición lo aporta el usuario (debe cuadrar con los «Campos editables» de esa TR-), si lo precarga la creación, o si lo fija el sistema (→ entonces suele ser un CC- o el resultado de un efecto RN-). Un campo NO declara "obligatorio" ni "editable": lo obligatorio es RES-/VAL- y la editabilidad la gobiernan los «Campos editables» de cada transición. Los valores de un enum van en la descripción del campo.
     Campos típicos de un expediente, vistos en los impresos reales: los datos de identificación que NO están en la ficha del usuario (los que sí están se precargan, no se redeclaran); la circunstancia alegada (enum: enfermedad prolongada o accidente, obligaciones de tipo personal o familiar, desempeño de un puesto de trabajo, maternidad/paternidad/adopción/acogimiento, otras — con su texto "especificar" condicionado); el justificante adjunto; la tabla de elementos solicitados (entidad hija: módulos con su convocatoria ordinaria/extraordinaria, líneas de gasto…); el tipo de resolución (enum: admitir, no admitir, subsanar) con su motivo; y un campo por cada documento PDF que el expediente guarda. -->

- **<campo>** — <qué representa; valores si es un enum> *(se rellena: en TR-NNN | precargado al crear | lo fija el sistema)*
- **<documento X>** — el PDF de <…> (ver [documento-<slug>.md](./documento-<slug>.md)), en sus versiones <original / firmada / sellada por el registro> *(las fija el sistema)*

## Restricciones

<!-- Invariantes de la entidad: deben cumplirse SIEMPRE, se dispare la transición que se dispare. Si la condición solo aplica al disparar una transición concreta, es una VAL-TR- de esa transición (en estados.md), no una RES-. Si no hay, elimina la sección. -->

- RES-<Entidad>-NNN — <condición que siempre debe cumplirse>

## Campos calculados

<!-- Valores que calcula el servidor, nunca el usuario. Un CC- no puede aparecer en los «Campos editables» de ninguna transición. Si no hay, elimina la sección. -->

- CC-<Entidad>-NNN — <nombre_campo>
  - momento: lectura | escritura
  - sobreescribible: nunca | [ROL1, ROL2]
  - cálculo: <descripción en lenguaje de negocio>
