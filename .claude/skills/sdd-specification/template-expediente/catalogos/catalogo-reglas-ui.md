# Catálogo de tipos de regla de UI

Catálogo de referencia para identificar, al rellenar las **Reglas de UI** (`RUI-`) de cada vista de `vistas.md`, qué comportamientos de pantalla necesita cada una. Recorre las tablas vista a vista (y, dentro de cada una, sus paneles, campos y botones) considerando el **perfil** que la ve y los **valores** del expediente. La columna "disparador típico" orienta el atributo `disparador`.

Es una ayuda **no exhaustiva**: si el negocio necesita una regla de UI que no figura aquí, decláralo igualmente. Todo en **lenguaje de negocio**: qué ve el usuario, nunca cómo se implementa.

> Recuerda la frontera: una RUI **solo cambia lo que el usuario ve o puede editar en pantalla** — no bloquea la transición (eso es una `VAL-TR-`) ni escribe en el sistema (eso es un `RN-`).

> Lo que **no** hay que declarar porque la vista ya lo fija estructuralmente: qué paneles se ven y en qué modo (edición/lectura) lo declara la propia vista; la confirmación de un botón la declara el `Disparador` de su transición; la cabecera, el historial y el botón «Salir» son de la plataforma.

## Visibilidad condicionada

| Descripción de la regla | disparador típico | Ejemplo |
|---|---|---|
| Un campo solo se muestra cuando otro campo tiene cierto valor | continuo | "Las horas de inicio y fin solo se muestran si la jornada es parcial" |
| El campo "especificar" solo se muestra con la opción OTRAS | continuo | "El texto de otras circunstancias solo se muestra si se marca OTRAS" |
| Un panel solo se muestra si tiene contenido | continuo | "El panel de disconformidad solo se muestra si hubo subsanación" |
| Un aviso de ayuda cambia según el valor elegido | continuo | "Si el motivo es enfermedad común: «Es necesario aportar justificante médico a partir del segundo día»" |
| Un campo o botón solo lo ve cierto rol | al cargar (+ actor) | "El botón de resolución de oficio solo lo ve el director" |

## Edición y marcas

| Descripción de la regla | disparador típico | Ejemplo |
|---|---|---|
| Un campo se marca visualmente como obligatorio, espejo de una `VAL-TR-` de la transición que se va a disparar *(la marca no es la defensa: la defensa es la VAL-)* | continuo | "El motivo del rechazo se marca obligatorio al elegir RECHAZAR" |
| Un campo de una **hija** se marca obligatorio o con formato en su formulario de alta, espejo de la validación que saltará al confirmar | continuo | "Al añadir una línea de gasto, el importe se marca obligatorio" |
| Un campo puntual queda en lectura dentro de un panel editable | continuo | "Dentro de los datos de la falta, el año no es editable" |

## Valores por defecto

| Descripción de la regla | disparador típico | Ejemplo |
|---|---|---|
| Al crear (o al entrar por primera vez), un campo se propone con un dato del contexto | al crear | "El curso académico se propone con el actual" |
| Al cambiar un campo, otro se rellena o se limpia en consecuencia | al cambiar <campo> | "Al cambiar el tipo de jornada a completa se limpian las horas" |
| Al añadir una fila hija, sus campos se proponen con valores derivados | al crear (hija) | "Al añadir un módulo se propone la convocatoria ordinaria" |

## Opciones disponibles

| Descripción de la regla | disparador típico | Ejemplo |
|---|---|---|
| Las opciones de un selector se limitan a lo que aplica al interesado | al cargar | "Solo se pueden elegir módulos en los que el alumno está matriculado" |
| Las opciones de un campo se limitan según el valor de otro | al cambiar <campo> | "Al elegir el ciclo, solo se muestran sus módulos" |
| Una opción de un enum no se ofrece a cierto perfil o en cierta variante | al cargar | "La circunstancia «propuesta del equipo educativo» no se ofrece al alumno" |

## Avisos

| Descripción de la regla | disparador típico | Ejemplo |
|---|---|---|
| La vista muestra un aviso permanente con lo que se necesita para continuar | al cargar | "«Para presentar debe tener AutoFirma instalada y un certificado digital válido»" |
| Al abrir el expediente en cierto estado se muestra un aviso informativo | al cargar | "En subsanación se muestra el motivo de la disconformidad indicado por el centro" |
| Un aviso informa de un plazo (cuánto queda, cuándo vence) | al cargar | "«El plazo de alegaciones termina el 12/09»" |
