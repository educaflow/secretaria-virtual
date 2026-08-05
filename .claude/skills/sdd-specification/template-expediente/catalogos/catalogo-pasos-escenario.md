# Catálogo de calidad de los pasos de un escenario

Catálogo de referencia para comprobar, escenario a escenario, que cada `ESC-` cumple las dos propiedades que exige la guía (sección «Historias de usuario»): **granularidad** (cada paso es UNA acción concreta, no un resumen de varias) y **autosuficiencia** (el escenario no depende del estado previo de la base de datos: prepara él mismo todo lo que necesita; el único estado admisible es el de «Recursos y datos iniciales» y los datos de demo).

Es una ayuda **no exhaustiva**: cualquier otro paso que falte para poder reproducir el escenario sin adivinar nada debe proponerse igualmente.

## Pasos que suelen faltar

| Paso | Cuándo hace falta | Ejemplo |
|---|---|---|
| Inicio de sesión con un usuario **concreto de los datos de demo** | Siempre — todo escenario empieza así | "El profesor «profesor1@mislata.es» inicia sesión con contraseña «demo1234»" |
| **Crear el expediente desde el árbol de trámites**, eligiendo el trámite por su nombre | Al principio de la tramitación | "Abre «Crear un nuevo expediente» y elige «Renuncia a la convocatoria»" |
| El valor concreto que se introduce en **cada** campo (incluidas las filas de las tablas hijas y los adjuntos) | En todo estado de entrada de datos | "Añade el módulo «Inglés» marcando la convocatoria ordinaria, y adjunta el justificante «parte-medico.pdf»" |
| La pulsación del **botón del evento**, con su etiqueta visible, y la aceptación de la **confirmación** si la pide | En toda transición de botón | "Pulsa «Resolver el expediente» y acepta la confirmación" |
| El paso de **firma** explícito (AutoFirma o firma en servidor) cuando el botón firma | En las transiciones que firman | "Pulsa «Firmar con AutoFirma y Presentar la solicitud» y firma con su certificado" |
| **Cierre de sesión y cambio de turno**: el siguiente perfil inicia sesión y abre el expediente **desde su bandeja** | Siempre que cambia el actor | "El profesor cierra sesión. El secretario «secretario@mislata.es» inicia sesión y abre el expediente desde su bandeja de pendientes" |
| La espera o provocación del **suceso** de una transición automática | En las transiciones automáticas | "El director firma en su portafirmas el documento pendiente; el expediente pasa a RESUELTO" |
| La respuesta **literal** del sistema: el mensaje exacto, el **estado resultante** (visible en la cabecera) | Al final de todo escenario | "El sistema muestra «Debe aportar el justificante» y el expediente sigue en ENTRADA_DATOS" |
| La comprobación de los **efectos visibles**: el documento generado se ve en su visor, el registro consta en el historial, el correo llegó | Cuando la transición tiene efectos | "Abre el historial de estados y comprueba que la presentación tiene registro de entrada" |

## Paso genérico → descomposición

Un paso que **agrupa varias acciones consecutivas** debe descomponerse en un paso por acción:

- ❌ INCORRECTO: `2. El alumno crea la renuncia con sus datos y la presenta.` (agrupa crear desde el árbol, rellenar N campos, añadir filas, adjuntar, avanzar y firmar)
- ✅ CORRECTO:
  `2. Abre «Crear un nuevo expediente» y elige «Renuncia a la convocatoria».`
  `3. Elige la circunstancia «Desempeño de un puesto de trabajo» y adjunta el justificante «contrato.pdf».`
  `4. Añade el módulo «Inglés» marcando la convocatoria ordinaria.`
  `5. Pulsa «Siguiente»; el expediente pasa a PENDIENTE_PRESENTACION y muestra el PDF de la solicitud.`
  `6. Pulsa «Firmar con AutoFirma y Presentar la solicitud», acepta la confirmación y firma.`
- ❌ INCORRECTO: `3. El secretario tramita el expediente.` (no dice qué botón pulsa, qué elige ni qué responde el sistema)
- ❌ INCORRECTO: `4. Comprueba que todo ha ido bien.` (no dice qué respuesta literal ni qué estado se espera)

## Dependencias del estado de la base de datos

El escenario debe poder ejecutarse contra una aplicación **recién arrancada**:

- ❌ INCORRECTO: `2. El secretario abre el expediente pendiente de resolución.` (presupone que ese expediente existe; nadie lo ha creado ni presentado en el escenario)
- ✅ CORRECTO: los pasos previos del escenario lo crean y presentan (con el actor que corresponda, iniciando sesión cada uno).
- ❌ INCORRECTO: `1. Inicia sesión el alumno «pepe@test.com».` (usuario inventado; **MUST** ser una cuenta de los datos de demo)
- ❌ INCORRECTO: firmar con un DNI inventado (la firma exige el DNI real de la cuenta de demo, el del expediente).
- ✅ CORRECTO: los únicos datos que no se crean en el escenario son los de «Recursos y datos iniciales» y los de demo (centros, cuentas, DNI).
