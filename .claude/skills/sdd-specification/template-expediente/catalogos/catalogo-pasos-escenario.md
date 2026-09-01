# Catálogo de calidad de los pasos de un escenario

Catálogo de referencia para comprobar, escenario a escenario, que cada `ESC-` cumple las dos propiedades que exige la guía: **granularidad** (cada paso es UNA acción concreta, no un resumen de varias) y **autosuficiencia** (el escenario no depende del estado previo de la base de datos: él mismo crea el expediente y lo lleva hasta el estado que quiere probar).

El único estado previo admisible es el del apartado «Datos iniciales» del índice y los usuarios y centros de los datos de demo.

Es una ayuda **no exhaustiva**: cualquier otro paso que falte para poder reproducir el escenario sin adivinar nada debe proponerse igualmente.

## Pasos que suelen faltar

| Comprobación | De dónde se deduce | Ejemplo de hueco |
|---|---|---|
| Inicio de sesión con una cuenta **concreta de los datos de demo** | Todo escenario empieza así | «El interesado inicia sesión con «alumno1@mislata.es» y contraseña «demo1234»» |
| Elegir el trámite en la lista y crear el expediente | El expediente no existe hasta que alguien lo crea | «Abre la lista de trámites disponibles, elige «<nombre del trámite>» y pulsa crear un expediente nuevo» |
| Recorrer, uno a uno, los estados intermedios hasta el que se quiere probar | La tabla de transiciones | «El escenario empieza con el expediente ya presentado, sin que nadie lo haya presentado dentro del escenario» |
| Cierre de sesión y cambio de actor entre fases | Cada estado tiene su perfil | «Falta que el interesado cierre sesión antes de que entre quien tramita» |
| El valor concreto que se introduce en **cada** dato de la acción | «Datos que el usuario envía al lanzarla» | «Rellena el motivo con «Avería del equipo anterior»» en vez de «rellena los datos» |
| La pulsación literal del botón que dispara la acción | «Botones» de la pantalla | «Pulsa «Presentar»» |
| El paso de firma en el equipo del interesado, cuando lo hay | «Quién lo firma y dónde» de `documentos.md` | «Falta el paso en que el interesado firma el documento con su certificado» |
| La respuesta **literal** del sistema: el mensaje de error o el estado resultante | «Comprobaciones» y «A qué estado lleva» | «El sistema muestra «El motivo es obligatorio»» / «El expediente queda en el estado de pendiente de valoración» |
| La comprobación de lo producido: el documento generado, el registro, el aviso | «Qué produce la acción» | «Falta comprobar que tras presentar aparece el justificante de presentación» |

## Paso genérico → descomposición

Un paso que **agrupa varias acciones consecutivas** debe descomponerse en un paso por acción:

- ❌ INCORRECTO: `2. El interesado rellena la solicitud y la presenta.` (agrupa crear el expediente, rellenar N datos, avanzar de estado, firmar y presentar)
- ✅ CORRECTO:
  `2. Abre la lista de trámites, elige «<nombre del trámite>» y crea un expediente nuevo.`
  `3. Rellena <dato> con «<valor>» y <dato> con «<valor>», y adjunta el fichero «<nombre>.pdf».`
  `4. Pulsa «<texto del botón>».`
  `5. El sistema muestra el documento generado y el expediente queda en el estado <ESTADO>.`
- ❌ INCORRECTO: `1. Prepara un expediente en el estado de valoración.` (no dice quién ni cómo)
- ❌ INCORRECTO: `4. Comprueba que todo ha ido bien.` (no dice qué respuesta literal se espera)

## Dependencias del estado de la base de datos

El escenario debe poder ejecutarse contra una aplicación **recién arrancada**:

- ❌ INCORRECTO: `2. Abre el expediente pendiente de valoración.` (nadie lo ha creado ni presentado dentro del escenario)
- ✅ CORRECTO: pasos previos crean el expediente con el actor que corresponda, lo rellenan y lo hacen avanzar hasta ese estado, cerrando e iniciando sesión cada vez que cambia el actor.
- ❌ INCORRECTO: `1. Inicia sesión el usuario «pepe@test.com».` (cuenta inventada; **MUST** ser una de los datos de demo)
- ✅ CORRECTO: los únicos datos que no se crean dentro del escenario son los de «Datos iniciales» y los de demo (centros, cuentas, documentos de identidad).
