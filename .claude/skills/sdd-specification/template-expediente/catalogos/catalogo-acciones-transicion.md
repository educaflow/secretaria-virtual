# Catálogo de efectos de transición y de entrada a estado

Catálogo de referencia para identificar, al rellenar los **Efectos** (`RN-TR-` de cada transición y `RN-<ESTADO>-` de cada entrada a estado en `estados.md`), qué hace el sistema automáticamente. Recorre las tablas transición a transición y, por cada transición, también la **entrada a su(s) estado(s) destino**. Un efecto que debe ocurrir al entrar en el estado *entre por donde entre* es de entrada (`RN-<ESTADO>-`); uno ligado a esa transición concreta es de transición (`RN-TR-`). La columna "fase típica" orienta el atributo `fase`; la decisión final es del negocio.

Es una ayuda **no exhaustiva**: si el negocio necesita un efecto que no figura aquí, decláralo igualmente.

> Recuerda la frontera: un efecto **siempre actúa, nunca bloquea**. Si debe impedir la transición, es una `VAL-TR-`; si solo cambia lo que se ve, es una `RUI-`; si es un valor que fija el servidor, es un `CC-`.

> Y lo que **no** hay que declarar porque la plataforma lo hace sola: guardar el historial de estados (con su evento, fecha y usuario), mostrar la vista del estado nuevo, y la numeración del expediente.

## Documentos PDF

| Efecto | Cuándo se dispara | fase típica | Ejemplo |
|---|---|---|---|
| Generar un documento con los datos del expediente y guardarlo en su campo | Al confirmar los datos / antes de presentar o emitir | antes_de_commit | "Al pasar a PENDIENTE_PRESENTACION se genera el PDF de la solicitud (documento-solicitud) con los datos introducidos" |
| Regenerar un documento porque sus datos cambiaron (subsanación, corrección) | Al volver a confirmar | antes_de_commit | "Tras subsanar, al volver a presentar se regenera el PDF de la solicitud" |

## Registro de entrada / salida

| Efecto | Cuándo se dispara | fase típica | Ejemplo |
|---|---|---|---|
| Presentar un documento por **registro de entrada** (lo presenta el usuario) y guardar el resguardo sellado | Al presentar | antes_de_commit | "Al presentar, la solicitud firmada entra por registro de entrada y el resguardo sellado se guarda en el expediente" |
| Emitir un documento por **registro de salida** (lo emite el centro) y guardar el documento sellado | Al resolver / emitir | antes_de_commit | "Al resolver, la resolución firmada sale por registro de salida y se guarda sellada" |

## Firmas

| Efecto | Cuándo se dispara | fase típica | Ejemplo |
|---|---|---|---|
| Firmar un documento **en pantalla** (AutoFirma o certificado del servidor) — la firma es parte del disparo del botón, referencia su `FIR-` | Al pulsar el botón que firma | antes_de_commit | "El botón «Firmar y presentar» firma el PDF de la solicitud con AutoFirma (FIR-solicitud-001) antes de presentarla" |
| Poner un documento a firmar **en el portafirmas** a alguien ajeno al expediente — típicamente al **entrar** en el estado de espera; el expediente avanza con la transición automática de vuelta | Al entrar en el estado de espera | después_de_commit | "Al entrar en PENDIENTE_FIRMA_DIRECTOR se pone la resolución a firmar al director (FIR-resolucion-002)" |

## Correos y notificaciones

| Efecto | Cuándo se dispara | fase típica | Ejemplo |
|---|---|---|---|
| Notificar al interesado el resultado, con el documento adjunto | Al resolver | después_de_commit | "Al resolver se envía al alumno un correo con la resolución sellada adjunta" |
| Avisar a quien **recibe el turno** de que tiene un expediente pendiente | Al entrar en el estado | después_de_commit | "Al entrar en PENDIENTE_RESOLUCION se avisa por correo al secretario" |
| Avisar de una subsanación o de un plazo de audiencia (qué falta y cuánto plazo hay) | Al pedir la subsanación / abrir la audiencia | después_de_commit | "Al pedir subsanación se envía al alumno un correo con los datos a corregir" |
| Notificar también al **representante legal** cuando el interesado es menor | Al resolver / notificar | después_de_commit | "La resolución se notifica al alumno y a su tutor legal" |

## Sobre el expediente y otros registros

| Efecto | Cuándo se dispara | fase típica | Ejemplo |
|---|---|---|---|
| Fijar la fecha (o el autor) de un hito de negocio | Al confirmarse la transición | antes_de_commit | "Al presentar se fija la fecha de presentación" |
| Recalcular un valor derivado (totales de las líneas hijas) | Al confirmar los datos | antes_de_commit | "Al presentar los gastos se recalcula el total como suma de las líneas" |
| Propagar el resultado a otra parte del sistema (el expediente académico del alumno, un acceso que se concede) | Al resolver | después_de_commit | "Al admitir la anulación, la matrícula del alumno queda anulada" |
| Encadenar con otro trámite o crear un registro relacionado | Al cerrar | después_de_commit | "Al aceptar la comisión se crea la tarea de justificación de gastos" |
