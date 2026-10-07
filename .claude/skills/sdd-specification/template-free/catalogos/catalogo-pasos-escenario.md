# Catálogo de calidad de los pasos de un escenario

Catálogo de referencia para comprobar, escenario a escenario, que cada `ESC-` cumple las dos propiedades que exige la guía (sección «Historias de usuario»): **granularidad** (cada paso es UNA acción concreta, no un resumen de varias) y **autosuficiencia** (el escenario no depende del estado previo de la base de datos: prepara él mismo todo lo que necesita; el único estado admisible es el de «Recursos y datos iniciales» y los datos de demo).

Es una ayuda **no exhaustiva**: cualquier otro paso que falte para poder reproducir el escenario sin adivinar nada debe proponerse igualmente.

## Pasos que suelen faltar

| Paso | Cuándo hace falta | Ejemplo |
|---|---|---|
| Inicio de sesión con un usuario **concreto de los datos de demo** | Siempre — todo escenario empieza así | «El supervisor «supervisor1@mislata.es» inicia sesión con contraseña «demo1234»» |
| Preparación de los datos que la prueba necesita | Si la acción se ejecuta sobre registros o configuración que deben existir | «El administrador crea el aviso «Mantenimiento» y lo marca activo» |
| Cierre de sesión y cambio de actor | Si en el escenario intervienen varios actores | «El administrador cierra sesión» antes de que entre el supervisor |
| Navegación a la pantalla donde ocurre la acción | Antes de interactuar con ella | «Abre la pantalla de avisos globales» |
| El valor concreto que se introduce en **cada** campo | En todo alta o modificación | «Rellena el texto con «Mantenimiento el viernes»» |
| La pulsación que dispara la acción | En toda acción | «Pulsa «Guardar»» |
| La respuesta **literal** del sistema (mensaje exacto, estado resultante, qué se ve) | Al final de todo escenario | «El sistema muestra el error «El texto del aviso es obligatorio»» |
| La comprobación de lo persistido (reabrir el registro, volver a entrar, verlo en el listado) | Cuando el resultado debe poder verificarse | «Al volver a iniciar sesión, el banner sigue visible» |
| **Deshacer** lo que la feature cambió de forma global | Cuando la feature cambia algo que afecta a todos los usuarios (una configuración global, un aviso) | «El administrador desactiva el aviso al terminar» — para que el siguiente escenario no lo herede |

## Paso genérico → descomposición

Un paso que **agrupa varias acciones consecutivas** debe descomponerse en un paso por acción:

- ❌ INCORRECTO: `2. El administrador configura el aviso y lo publica.` (agrupa navegar, rellenar N campos, guardar y activar)
- ✅ CORRECTO:
  `2. Abre la pantalla de avisos globales y pulsa «Nuevo».`
  `3. Rellena el texto con «Mantenimiento el viernes» y marca «Activo».`
  `4. Pulsa «Guardar».`
- ❌ INCORRECTO: `1. Prepara los datos necesarios para la prueba.` (no dice qué datos ni cómo)
- ❌ INCORRECTO: `3. Comprueba que todo ha ido bien.` (no dice qué respuesta literal del sistema se espera)

## Dependencias del estado de la base de datos

El escenario debe poder ejecutarse contra una aplicación **recién arrancada**:

- ❌ INCORRECTO: `2. Abre el aviso existente «Mantenimiento».` (presupone que ese aviso ya está en la BD; nadie lo ha creado en el escenario)
- ✅ CORRECTO: un paso previo lo crea dentro del propio escenario (con el actor que corresponda, iniciando sesión si hace falta) y después se abre.
- ❌ INCORRECTO: `1. Inicia sesión el usuario «pepe@test.com».` (usuario inventado; **MUST** ser una cuenta de los datos de demo)
- ✅ CORRECTO: los únicos datos que no se crean en el escenario son los de «Recursos y datos iniciales» y los de demo (centros, cuentas, DNI).
