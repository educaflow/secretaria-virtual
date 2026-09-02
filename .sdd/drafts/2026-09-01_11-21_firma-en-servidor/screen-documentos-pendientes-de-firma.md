# Pantalla: Documentos pendientes de firma

**Pantalla existente:** sí

## Identidad

- **Quién la usa:** cualquier usuario de la secretaría virtual que tenga tareas de firma pendientes, sobre las suyas y solo las suyas.
- **Qué muestra:** lo que cambia es el **paso de firmar** del formulario. Hasta ahora ese paso era único y solo ofrecía AutoFirma; ahora hay **seis paneles distintos y excluyentes**, y el sistema muestra el que corresponde a la situación de firma del firmante. El resto de la pantalla —el listado de tareas pendientes, los datos de la tarea, el listado de documentos y el paso de rechazo— no cambia.

## Menú

- Firmar documentos → Pendientes — lo ven todos los usuarios de la secretaría virtual; lleva a esta pantalla. No cambia.

## Estructura jerárquica de las vistas

```
Listado de tareas de firma pendientes
└── Formulario de firma   (se abre al pulsar una fila)
    └── Listado de documentos a firmar   (panel maestro-detalle «Documentos a firmar» del formulario de firma)
        └── Formulario de documento   (se abre al pulsar una fila del listado de documentos)
```

De estas cuatro vistas, esta iniciativa **solo cambia el formulario de firma**; las otras tres se quedan como están y por eso no se detallan aquí.

## Vista: Formulario de firma

- **Slug:** formulario
- **Tipo:** formulario
- **Qué muestra:** la tarea de firma pendiente del propio usuario y sus documentos, en edición.
- **Se abre desde:** el listado de tareas de firma pendientes, al pulsar una fila.

### Propiedades

- **Modo:** editable solo en lo que el firmante rellena (el motivo del rechazo y la clave de firma); los datos de la tarea y sus documentos, en lectura. No cambia respecto a lo que ya hay.

### Paneles

Los seis paneles siguientes **sustituyen** al único panel de firma que hay hoy. Son excluyentes: en el paso de firmar se ve **exactamente uno**, el que corresponde a la situación de firma del firmante. Todos llevan el mismo título visible, «Firmar el documento».

- **Firmar el documento — sin certificado** (normal) — el aviso de que para firmar hace falta tener AutoFirma instalada y un certificado digital válido, con el enlace de descarga de AutoFirma. Es el panel que ya existe hoy.
- **Firmar el documento — dispositivo con PIN guardado** (normal) — el texto «Los documentos se firmarán en el servidor con su certificado digital.». No pide nada al firmante.
- **Firmar el documento — dispositivo sin PIN guardado** (normal) — el texto «Los documentos se firmarán en el servidor con su certificado digital. Introduzca el PIN de su dispositivo criptográfico.» y el campo de la clave de firma, titulado «PIN».
- **Firmar el documento — fichero con contraseña guardada** (normal) — el texto «Los documentos se firmarán en el servidor con su certificado digital.». No pide nada al firmante.
- **Firmar el documento — fichero sin contraseña guardada** (normal) — el texto «Los documentos se firmarán en el servidor con su certificado digital. Introduzca la contraseña de su certificado.» y el campo de la clave de firma, titulado «Contraseña».
- **Firmar el documento — firmante sin DNI** (normal) — el texto «No es posible firmar los documentos porque su usuario no tiene un DNI. Póngase en contacto con el administrador.». No pide nada y no lleva ningún botón de firmar.

### Botones

- **Firmar todos los documentos con AutoFirma y finalizar** — lanza la firma con AutoFirma en el ordenador del firmante y deja la tarea firmada. Es el botón que ya existe, y a partir de ahora solo se ve en el panel de «sin certificado».
- **Firmar todos los documentos y finalizar** — firma en el servidor todos los documentos de la tarea y la deja firmada. Se ve en los cuatro paneles de firma en el servidor (dispositivo con y sin PIN guardado, fichero con y sin contraseña guardada).
- **Atrás** — vuelve al primer paso de la pantalla, donde el firmante elige entre rechazar y firmar. Se ve en los seis paneles, incluido el del firmante sin DNI, y cancela la firma sin cambiar nada.
- Desviación del estándar: en el panel de **firmante sin DNI** no hay ningún botón de firmar; ese firmante solo puede volver atrás y, si quiere, rechazar la firma.

### Reglas de UI

- RUI-documentos-pendientes-de-firma-formulario-001 — En el paso de firmar se muestra el panel de AutoFirma cuando el firmante tiene DNI pero no tiene ningún certificado digital habilitado dado de alta
  - disparador: continuo
  - condición: la situación de firma del firmante es «sin certificado»
- RUI-documentos-pendientes-de-firma-formulario-002 — En el paso de firmar se muestra el panel de firma en el servidor sin pedir nada cuando el firmante tiene un dispositivo criptográfico con el PIN guardado
  - disparador: continuo
  - condición: la situación de firma del firmante es «dispositivo con PIN guardado»
- RUI-documentos-pendientes-de-firma-formulario-003 — En el paso de firmar se muestra el panel que pide el PIN cuando el firmante tiene un dispositivo criptográfico sin el PIN guardado
  - disparador: continuo
  - condición: la situación de firma del firmante es «dispositivo sin PIN guardado»
- RUI-documentos-pendientes-de-firma-formulario-004 — En el paso de firmar se muestra el panel de firma en el servidor sin pedir nada cuando el firmante tiene un certificado en fichero con la contraseña guardada
  - disparador: continuo
  - condición: la situación de firma del firmante es «fichero con contraseña guardada»
- RUI-documentos-pendientes-de-firma-formulario-005 — En el paso de firmar se muestra el panel que pide la contraseña cuando el firmante tiene un certificado en fichero sin la contraseña guardada
  - disparador: continuo
  - condición: la situación de firma del firmante es «fichero sin contraseña guardada»
- RUI-documentos-pendientes-de-firma-formulario-006 — En el paso de firmar se muestra el panel que avisa de que no se puede firmar cuando el firmante no tiene DNI en su ficha
  - disparador: continuo
  - condición: la situación de firma del firmante es «sin DNI»
- RUI-documentos-pendientes-de-firma-formulario-007 — El campo de la clave de firma se titula «PIN» en el panel del dispositivo sin PIN guardado
  - disparador: continuo
  - condición: la situación de firma del firmante es «dispositivo sin PIN guardado»
- RUI-documentos-pendientes-de-firma-formulario-008 — El campo de la clave de firma se titula «Contraseña» en el panel del fichero sin contraseña guardada
  - disparador: continuo
  - condición: la situación de firma del firmante es «fichero sin contraseña guardada»
- RUI-documentos-pendientes-de-firma-formulario-009 — Lo que se teclea en el campo de la clave de firma no se ve en pantalla: se muestra oculto
  - disparador: continuo
  - condición: Siempre
- RUI-documentos-pendientes-de-firma-formulario-010 — El campo de la clave de firma se marca como obligatorio en los dos paneles que lo piden
  - disparador: continuo
  - condición: la situación de firma del firmante es «dispositivo sin PIN guardado» o «fichero sin contraseña guardada»
- RUI-documentos-pendientes-de-firma-formulario-011 — El campo de la clave de firma aparece vacío al abrir la tarea y se vacía cada vez que se entra en el paso de firmar, de modo que lo tecleado antes no se conserva
  - disparador: al cargar
  - condición: Siempre
- RUI-documentos-pendientes-de-firma-formulario-012 — El botón «Firmar todos los documentos con AutoFirma y finalizar» solo se muestra en el panel de «sin certificado»
  - disparador: continuo
  - condición: la situación de firma del firmante es «sin certificado»
- RUI-documentos-pendientes-de-firma-formulario-013 — El botón «Firmar todos los documentos y finalizar» solo se muestra en los cuatro paneles de firma en el servidor
  - disparador: continuo
  - condición: la situación de firma del firmante es «dispositivo con PIN guardado», «dispositivo sin PIN guardado», «fichero con contraseña guardada» o «fichero sin contraseña guardada»
- RUI-documentos-pendientes-de-firma-formulario-014 — En el panel del firmante sin DNI no se muestra ningún botón de firmar; solo queda «Atrás»
  - disparador: continuo
  - condición: la situación de firma del firmante es «sin DNI»
- RUI-documentos-pendientes-de-firma-formulario-015 — El botón «Atrás» se muestra siempre en el paso de firmar, cualquiera que sea la situación de firma del firmante, incluida la de «sin DNI», para que nadie se quede en un paso sin salida
  - disparador: continuo
  - condición: Siempre
- RUI-documentos-pendientes-de-firma-formulario-016 — El campo de la clave de firma solo se muestra en los dos paneles que la piden; en los otros cuatro (sin certificado, dispositivo con PIN guardado, fichero con contraseña guardada y firmante sin DNI) no se muestra
  - disparador: continuo
  - condición: la situación de firma del firmante es «dispositivo sin PIN guardado» o «fichero sin contraseña guardada»
- RUI-documentos-pendientes-de-firma-formulario-017 — Cuando la firma en el servidor no se completa, el firmante se queda en el paso de firmar y con el mismo panel, de modo que pueda volver a intentarlo sin salir de la pantalla
  - disparador: al terminar la acción de firmar
  - condición: la firma no se ha podido completar
- RUI-documentos-pendientes-de-firma-formulario-018 — Cuando la firma en el servidor no se completa, lo tecleado en el campo de la clave de firma **se conserva**, para que el firmante pueda corregirlo sin volver a escribirlo entero. Solo se vacía al volver a entrar en el paso de firmar (RUI-documentos-pendientes-de-firma-formulario-011)
  - disparador: al terminar la acción de firmar
  - condición: la firma no se ha podido completar
