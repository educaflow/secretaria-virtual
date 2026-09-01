# Catálogo de comprobaciones por pareja (estado, acción)

Catálogo de referencia para identificar las **comprobaciones** (`VAL-`) que faltan en `estados.md`. En un expediente **toda** comprobación se ancla a una pareja (estado, acción): no existen invariantes que valgan para todo el ciclo de vida, porque el expediente vive en la base de datos desde que nace, con casi todos sus datos vacíos, y se van exigiendo a medida que avanza. Por eso este catálogo se recorre **acción a acción** y, dentro de cada una, **dato a dato**.

Es una ayuda **no exhaustiva**: si el negocio necesita una comprobación que no figura aquí, decláralo igualmente.

> **REQUIRED — empieza por la obligatoriedad.** Antes de bajar a los tipos concretos, recorre **uno a uno** los datos de la lista «Datos que el usuario envía al lanzarla» de cada acción y pregúntate: *«¿puede quedar vacío?»*. Es la comprobación más trivial y por eso la más olvidada. Solo después pasa al resto.
>
> Redacta el `mensaje` en lenguaje de negocio, diciendo **cómo debe ser** el dato, no cómo no debe ser, y sin jerga técnica.

## Obligatoriedad

| Comprobación | De dónde se deduce | Ejemplo de hueco |
|---|---|---|
| El dato es obligatorio en esta acción | Cada dato de «Datos que el usuario envía al lanzarla» | «Se envía el motivo al denegar y nadie exige que esté relleno» |
| El dato es obligatorio **solo si** otro dato tiene cierto valor | Datos que solo aplican en algunos casos | «Se pide el número de cuenta solo cuando se elige cobrar por transferencia, y no se exige» |
| El dato es obligatorio **solo para** cierto perfil | Acciones que lanza más de un perfil | «Quien tramita debe justificar la denegación; el interesado no» |
| El documento adjunto es obligatorio | «Datos que el usuario envía» que sean ficheros | «Se pide adjuntar el certificado de empadronamiento y no se comprueba que se haya adjuntado» |

## Sobre el propio dato

| Comprobación | De dónde se deduce | Ejemplo de hueco |
|---|---|---|
| El dato debe cumplir un formato concreto | Datos con estructura (identificadores, listas, códigos) | «Los códigos de las asignaturas se escriben separados por comas y no se comprueba el formato» |
| El dato debe tener una longitud mínima o máxima | Textos libres | «El texto de la explicación puede quedarse en una letra» |
| El dato debe respetar la convención de escritura del impreso (mayúsculas y minúsculas, sin abreviaturas) | Textos libres que se imprimen en un documento | «El domicilio se escribe entero en mayúsculas y el impreso sale ilegible» |
| El dato numérico debe estar dentro de un rango | Números, años, cantidades | «El número de ejemplares que se piden en préstamo admite un valor negativo» |
| La fecha debe estar dentro de un intervalo, o ser pasada o futura | Fechas | «La fecha del hecho puede ser posterior a hoy» |
| El dato debe ser uno de una lista cerrada de opciones | Datos con opciones predefinidas | «El sentido de la decisión sobre la reclamación admite cualquier texto» |
| El dato debe referirse a un registro que exista | Datos que apuntan a otra ficha | «Se elige un material que ya no está dado de alta» |

## Entre varios datos de la misma acción

| Comprobación | De dónde se deduce | Ejemplo de hueco |
|---|---|---|
| Un dato debe ser mayor (posterior) que otro | Parejas inicio/fin, desde/hasta | «La fecha de devolución puede ser anterior a la de recogida» |
| Dos datos no pueden rellenarse a la vez, o deben rellenarse juntos | Datos alternativos o complementarios | «Se pide a la vez el cobro por transferencia y el cobro en efectivo» |
| La suma o el total de varios datos debe cumplir un límite | Datos que se acumulan | «Las horas de las asignaturas elegidas superan el máximo del curso» |

## Ficheros que aporta el usuario

| Comprobación | De dónde se deduce | Ejemplo de hueco |
|---|---|---|
| El fichero debe ser de uno de los tipos admitidos | Datos que son documentos aportados | «Se admite cualquier fichero, también un ejecutable» |
| El fichero no puede superar un tamaño máximo | Datos que son documentos aportados | «No hay límite de tamaño para el documento aportado» |
| El fichero debe llevar nombre | Datos que son documentos aportados | «Se admite un adjunto sin nombre» |

## Firma

| Comprobación | De dónde se deduce | Ejemplo de hueco |
|---|---|---|
| El documento firmado está presente | «Quién lo firma y dónde» de `documentos.md` | «Se puede presentar sin haber firmado» |
| La firma es válida, es una sola, es de un certificado de confianza y no altera el texto del documento | Documentos firmados en el equipo del interesado | «Se admite un documento firmado con un certificado que no corresponde» |
| La firma corresponde al documento de identidad de la persona esperada | «Quién queda registrado como interesado» | «Cualquiera puede firmar el documento de otro» |

## Situación del expediente y de quien actúa

| Comprobación | De dónde se deduce | Ejemplo de hueco |
|---|---|---|
| Quien lanza la acción es de un centro que puede verla | Apartado «Seguridad» (alcance por centro) | «No se comprueba que quien resuelve es del mismo centro que el expediente» |
| Un dato que se aporta apunta a algo del mismo centro | Datos que apuntan a otra ficha | «Se elige un material de otro centro» |
| Una condición del negocio ajena al expediente impide la acción | Reglas del apartado «El trámite» | «Se concede el préstamo aunque ya haya uno vivo de la misma persona» |
