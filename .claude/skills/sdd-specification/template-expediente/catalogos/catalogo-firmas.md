# Catálogo de firmas

Catálogo de referencia para las fichas de **Firmas** (`FIR-`) de cada `documento-*.md`: el algoritmo que decide dónde y cómo se firma, las preguntas que hay que hacerse por cada firma y las comprobaciones de completitud del barrido.

Es una ayuda **no exhaustiva**: si el negocio necesita una firma o una decisión que no figura aquí, decláralo igualmente.

## El algoritmo de decisión

Por cada firma, **la relación del firmante con el expediente decide dónde se firma**; el mecanismo se deriva después:

1. **¿El firmante es (o va a ser) el perfil con el turno en el estado en que se firma?** → Firma **en una vista del expediente**: un botón de firma en su vista, que muestra el PDF a firmar.
2. **¿No tiene el turno pero es parte del expediente** (interviene en la tramitación y le es familiar)? → Se le **da el turno para firmar**: un estado propio cuyo perfil con el turno es el firmante, con su vista de firma. Firmará cuando entre al expediente.
3. **¿Es ajeno al expediente** (firma muchos sin conocerlos: el director que firma todas las resoluciones)? → El documento **se envía al portafirmas** (subsistema de firmas): el expediente queda en un estado de espera sin turno y avanza con una **transición automática** cuando la firma se completa. El mecanismo no se especifica: lo decide el propio subsistema.

Para los casos 1 y 2 (firma en pantalla), el **mecanismo** depende del certificado:

- **Certificado en el servidor** → la vista muestra el PDF y avisa de que se firmará en el servidor; el usuario solo confirma.
- **Sin certificado en el servidor** → **AutoFirma**: la vista muestra el PDF y el botón lanza AutoFirma con el certificado personal del usuario, que debe corresponder al DNI de firma del expediente.

## Las preguntas por cada firma

| Pregunta | Por qué importa |
|---|---|
| ¿Quién firma exactamente (perfil, cargo, persona del expediente)? | Es el firmante de la ficha `FIR-` |
| ¿Qué relación tiene con el expediente (turno / parte sin turno / ajeno)? | Decide dónde se firma (el algoritmo de arriba) |
| ¿Tiene esa persona **certificado digital**? (los alumnos normalmente no) | Sin certificado no hay AutoFirma: hay que decidir la alternativa (certificado en servidor, o replantear quién firma / cómo se presenta) |
| ¿Su certificado estará **en el servidor**? | Decide el mecanismo en pantalla |
| Si firman **varios**, ¿en qué orden, y es secuencial o en paralelo? | Cada firma es una ficha; el orden define estados/transiciones |
| ¿Qué pasa si **no firma** (se niega, o pasa el plazo)? | Puede necesitar una rama o una transición automática por plazo |
| ¿Qué versión del documento se guarda y dónde (original / firmada)? | El campo del expediente por versión (el par original/firmado es obligatorio con AutoFirma) |
| ¿La firma va seguida de un **registro** (entrada si presenta el usuario, salida si emite el centro)? | La firma y el registro suelen ir juntos en la misma transición |

## Comprobaciones del barrido

| Comprobación | Ejemplo de hueco |
|---|---|
| Cada documento generado **o se firma o se decide que no se firma** (*(sin firmas)* explícito) | "documento-resolucion no declara firmas ni dice que no lleva" |
| Cada ficha `FIR-` tiene firmante, relación y —si firma en pantalla— mecanismo | "FIR-solicitud-001 no dice si es AutoFirma o servidor" |
| Cada firma en pantalla tiene su **botón** en la vista del estado correspondiente (`vistas.md`) | "FIR-solicitud-001 no aparece en ningún botón" |
| Cada firma ajena tiene su **tríada completa**: efecto que la pone a firmar al entrar (`RN-<ESTADO>-`) + estado de espera + transición automática al completarse | "La resolución va al portafirmas pero no hay transición automática de vuelta" |
| El **DNI de firma** del expediente corresponde al firmante esperado en cada firma con AutoFirma | "El expediente exige firmar con el DNI del alumno pero la firma es del tutor legal" |
| El firmante en pantalla es un **perfil que existe** y en ese estado tiene el turno | "Firma el director en PENDIENTE_FIRMA pero el turno lo tiene el secretario" |
| Si el que estudia la solicitud y el que firma la resolución son personas distintas, la firma es del caso 3 (portafirmas), no un botón de la vista del que estudia | "El secretario resuelve y el mismo botón 'firma como director'" |
