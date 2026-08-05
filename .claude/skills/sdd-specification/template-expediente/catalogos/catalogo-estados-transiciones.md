# Catálogo de completitud de la máquina de estados

Catálogo de referencia para comprobar si a la máquina de `estados.md` le **faltan estados, transiciones o decisiones**. No inventa funcionalidad nueva: cada comprobación se deduce de lo que la spec **ya declara** y detecta huecos, estados muertos y decisiones sin tomar.

Es una ayuda **no exhaustiva**: se pueden proponer candidatas que no respondan a ninguna fila si la spec las sugiere.

## Conformidad con las fases

<!-- La máquina se compone de fases instanciadas (ver fases.md). Las fases son GUÍAS — desviarse es legítimo —, pero la composición debe estar declarada y ser coherente. -->

| Comprobación | Ejemplo de hueco |
|---|---|
| El índice tiene el apartado **Fases** con las fases instanciadas, y `estados.md` tiene exactamente los estados que ese apartado declara | "El índice declara F_SALIDA pero en estados.md no hay ningún estado F_SALIDA_*" |
| Todo estado lleva el **prefijo de su fase** y su columna Fase cuadra con él | "PENDIENTE_RESOLUCION no empieza por el código de ninguna fase instanciada" |
| Los **parámetros obligatorios** de cada fase instanciada tienen valor (fases.md, ficha de la fase) | "F_SALIDA no declara destino_atras: el botón «Atrás» no tiene destino" |
| Toda diferencia respecto a la ficha del catálogo está declarada como **desviación con su motivo** | "F_ENTRADA no tiene estado de revisión y ninguna desviación lo explica" |
| Los nombres de **evento** son únicos en el tipo de expediente (compartirlos solo con acción y destino idénticos) | "SIGUIENTE se usa en tres estados con destinos distintos" |
| Los **pasos** de una vista no son estados encubiertos (si cambia el turno, queda en el historial o debe sobrevivir a cerrar la ventana, es un estado) y todo asistente tiene paso inicial y salida alcanzable desde cada paso | "PASO_SUBSANAR no tiene ningún botón que dispare un evento ni vuelva al paso inicial" |

## Estructura de la máquina

| Comprobación | Ejemplo de hueco |
|---|---|
| Hay exactamente **un** estado inicial | "Hay dos estados marcados como iniciales" |
| Todo estado es **alcanzable** desde el inicial por alguna cadena de transiciones | "A PENDIENTE_PAGO no llega ninguna transición" |
| Todo estado **no cerrado** tiene al menos una transición de salida | "EN_ESTUDIO no tiene salida y no está marcado como cerrado" |
| Todo estado tiene perfil con el turno **o** es deliberadamente de espera/consulta (sin turno y con salida automática, o cerrado) | "PENDIENTE_FIRMA no tiene perfil ni transición automática: nadie puede sacarlo de ahí" |
| El borrado está **decidido en cada estado** (lo habitual: solo el inicial) | "Ningún estado declara si se puede borrar el expediente" |
| Cada rama de una transición tiene su **condición** declarada, y las condiciones cubren todos los casos | "RESOLVER tiene tres destinos pero solo dos condiciones" |

## Caminos que suelen faltar

| Comprobación | De dónde se deduce | Ejemplo de hueco |
|---|---|---|
| Además del camino de aceptación existe el de **rechazo/desestimación** | El tipo de resolución (admitir / no admitir) | "La resolución solo puede ACEPTAR: no hay estado RECHAZADO" |
| **Subsanación**: si el centro puede pedir corregir, hay una rama que devuelve el expediente al creador y un camino de re-presentación | La resolución con opción "subsanar datos" | "SUBSANAR_DATOS no lleva a ningún estado editable por el CREADOR" |
| **Desistimiento**: ¿puede el solicitante retirar su solicitud antes de la resolución? (preguntarlo; si no puede, que quede decidido) | Los estados intermedios entre presentar y resolver | "Nada dice si el alumno puede desistir tras presentar" |
| **Trámite de audiencia**: en un expediente de oficio, el interesado tiene un estado con el turno para alegar, con plazo | El trámite se inicia de oficio | "El centro anula la matrícula sin estado de alegaciones del alumno" |
| **Volver atrás**: los estados de revisión previos a una acción irreversible permiten volver (BACK es una transición normal, no existe gratis) | La vista de "revisar antes de presentar" | "Desde PENDIENTE_PRESENTACION no se puede volver a corregir los datos" |
| **Informes intermedios**: si la normativa exige "oír" a otros antes de resolver, cada informe es un posible estado con su perfil | Las instrucciones del trámite | "Se resuelve sin el estado de informe del equipo docente que exige la norma" |

## Transiciones automáticas

| Comprobación | Ejemplo de hueco |
|---|---|
| Toda transición automática **nombra su suceso** concreto | "TR-007 es automática pero no dice qué la dispara" |
| Las firmas de terceros (portafirmas) tienen su pareja completa: efecto que pone a firmar al entrar + estado de espera + transición automática al completarse | "Se envía al portafirmas pero ningún suceso saca al expediente de PENDIENTE_FIRMA" |
| Los **plazos con silencio administrativo** ("resolverá en un mes, silencio estimatorio") son una transición automática por vencimiento, con su resultado | "La norma da un mes para resolver pero la máquina no contempla el vencimiento" |
| El vencimiento de un plazo de audiencia o subsanación avanza el expediente solo (¿y hacia dónde?) | "Si el alumno no subsana nunca, el expediente queda abierto para siempre" |

## Coherencia con el resto de la spec

| Comprobación | Ejemplo de hueco |
|---|---|
| Cada transición de `botón` tiene su botón en la vista de su perfil en `vistas.md` (y a la inversa: cada botón dispara una TR- existente) | "TR-004 no aparece como botón en ninguna vista de PENDIENTE_RESOLUCION" |
| Cada estado alcanzable tiene sus vistas en `vistas.md` (la del perfil con el turno si lo tiene, y la genérica) | "RECHAZADO no tiene vista genérica" |
| Los «Campos editables» de cada transición existen en los «Campos» de un `entity-*.md`, y cada campo que el usuario aporta aparece en la whitelist de alguna transición | "El campo motivo del rechazo no lo puede enviar ninguna transición" |
| Cada documento se genera en algún efecto y cada firma FIR- está anclada a su transición o estado | "documento-resolucion no se genera en ningún RN-" |
| Los perfiles que aparecen en la tabla de estados existen en la tabla de perfiles del índice | "El estado PENDIENTE_VISTO_BUENO usa el perfil JEFATURA, que el índice no declara" |
