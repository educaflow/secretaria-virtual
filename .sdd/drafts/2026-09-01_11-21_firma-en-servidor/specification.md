---
type: specification
---

# Objetivo

Que un firmante pueda firmar sus documentos **en el servidor**, con el certificado digital que la secretaría virtual tenga dado de alta para su DNI, en lugar de tener que usar siempre AutoFirma desde su ordenador.

**Modifica:** subsystem/firmas

La pantalla de documentos pendientes de firma decide **por sí sola** qué ofrecerle al firmante según la situación en que esté su certificado digital; el firmante no elige. Depende funcionalmente del subsistema de criptografía, que es quien sabe si una persona tiene certificado dado de alta y si su PIN o su contraseña están guardados o hay que pedírselos.

# Actores

- **Firmante**: cualquier usuario de la secretaría virtual (Profesor con o sin cargo, Administrativo, Alumno, Familiar, Administrador…) al que se le ha asignado una tarea de firma. Solo ve y resuelve las suyas.
- **Administrador**: da de alta, modifica y deshabilita los certificados digitales de las personas. No firma en nombre de nadie; solo determina, al dar de alta un certificado, en qué situación queda esa persona a la hora de firmar.

# Historias de usuario

## HU-001 — Como Firmante quiero que la secretaría virtual firme mis documentos en el servidor cuando tengo un certificado digital dado de alta, para no depender de AutoFirma en mi ordenador

- ESC-001 — Firma en el servidor con la contraseña del certificado ya guardada:
  1. El Administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de certificados digitales y pulsa «Añadir certificado digital».
  3. Rellena «DNI» con «85432016B», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12» y «Contraseña» con «nadanada», y deja marcado «Habilitado».
  4. Pulsa «Guardar».
  5. El Administrador cierra sesión.
  6. El director inicia sesión con «director@mislata.es» y contraseña «demo1234».
  7. Abre «Firmar documentos → Pendientes» y entra en la tarea de firma «Firma de prueba 1».
  8. Pulsa «Firmar todos los documentos».
  9. El sistema muestra el texto «Los documentos se firmarán en el servidor con su certificado digital.», no muestra ningún campo «PIN» ni ningún campo «Contraseña», y muestra el botón «Firmar todos los documentos y finalizar» sin mostrar el botón de AutoFirma.
  10. Pulsa «Firmar todos los documentos y finalizar».
  11. El sistema firma el documento de la tarea en el servidor y devuelve al firmante al listado de «Firmar documentos → Pendientes».
  12. El listado ya no muestra «Firma de prueba 1».
  13. Abre «Firmar documentos → Firmados» y comprueba que «Firma de prueba 1» aparece con estado «Firmado».
  14. Entra en «Firma de prueba 1», abre su documento y comprueba que tiene versión firmada.

- ESC-002 — Firma en el servidor tecleando la contraseña del certificado:
  1. El Administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de certificados digitales y pulsa «Añadir certificado digital».
  3. Rellena «DNI» con «85432016B», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12», **deja «Contraseña» vacía**, y deja marcado «Habilitado».
  4. Pulsa «Guardar».
  5. El Administrador cierra sesión.
  6. El director inicia sesión con «director@mislata.es» y contraseña «demo1234».
  7. Abre «Firmar documentos → Pendientes» y entra en la tarea de firma «Firma de prueba 2».
  8. Pulsa «Firmar todos los documentos».
  9. El sistema muestra el texto «Los documentos se firmarán en el servidor con su certificado digital. Introduzca la contraseña de su certificado.» junto a un campo «Contraseña» vacío, y no muestra el botón de AutoFirma.
  10. Escribe «nadanada» en «Contraseña».
  11. Pulsa «Firmar todos los documentos y finalizar».
  12. El sistema firma el documento de la tarea en el servidor y devuelve al firmante al listado de «Firmar documentos → Pendientes».
  13. El listado ya no muestra «Firma de prueba 2».
  14. Abre «Firmar documentos → Firmados» y comprueba que «Firma de prueba 2» aparece con estado «Firmado».
  15. Entra en «Firma de prueba 2», abre su documento y comprueba que tiene versión firmada.

- ESC-003 — Una contraseña incorrecta no firma ningún documento y se puede reintentar:
  1. El Administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de certificados digitales y pulsa «Añadir certificado digital».
  3. Rellena «DNI» con «85432016B», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12», deja «Contraseña» vacía, y deja marcado «Habilitado».
  4. Pulsa «Guardar».
  5. El Administrador cierra sesión.
  6. El director inicia sesión con «director@mislata.es» y contraseña «demo1234».
  7. Abre «Firmar documentos → Pendientes» y entra en la tarea de firma «Firma de prueba 3», que tiene dos documentos.
  8. Pulsa «Firmar todos los documentos».
  9. El sistema muestra el texto «Los documentos se firmarán en el servidor con su certificado digital. Introduzca la contraseña de su certificado.» junto a un campo «Contraseña» vacío.
  10. Escribe «claveequivocada» en «Contraseña».
  11. Pulsa «Firmar todos los documentos y finalizar».
  12. El sistema muestra un error que empieza por «No se han podido firmar los documentos:», deja la pantalla en el paso de firmar y conserva «claveequivocada» escrita en el campo «Contraseña».
  13. Pulsa «Atrás»; el sistema vuelve al primer paso y la tarea sigue mostrando el estado «Pendiente de firmar».
  14. Entra en el primer documento del listado «Documentos a firmar», comprueba que no tiene versión firmada y vuelve al formulario de la tarea.
  15. Entra en el segundo documento del listado «Documentos a firmar», comprueba que tampoco tiene versión firmada y vuelve al formulario de la tarea.
  16. Pulsa «Firmar todos los documentos»; el sistema vuelve a mostrar el campo «Contraseña» vacío.
  17. Escribe «nadanada» en «Contraseña».
  18. Pulsa «Firmar todos los documentos y finalizar».
  19. El sistema firma los dos documentos en el servidor y devuelve al firmante al listado de «Firmar documentos → Pendientes».
  20. El listado ya no muestra «Firma de prueba 3».
  21. Abre «Firmar documentos → Firmados» y comprueba que «Firma de prueba 3» aparece con estado «Firmado».
  22. Entra en «Firma de prueba 3» y comprueba que sus dos documentos tienen versión firmada.

- ESC-004 — La contraseña se deja en blanco:
  1. El Administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de certificados digitales y pulsa «Añadir certificado digital».
  3. Rellena «DNI» con «85432016B», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12», deja «Contraseña» vacía, y deja marcado «Habilitado».
  4. Pulsa «Guardar».
  5. El Administrador cierra sesión.
  6. El director inicia sesión con «director@mislata.es» y contraseña «demo1234».
  7. Abre «Firmar documentos → Pendientes» y entra en la tarea de firma «Firma de prueba 5».
  8. Pulsa «Firmar todos los documentos».
  9. El sistema muestra el texto «Los documentos se firmarán en el servidor con su certificado digital. Introduzca la contraseña de su certificado.» junto a un campo «Contraseña» vacío.
  10. Sin escribir nada en «Contraseña», pulsa «Firmar todos los documentos y finalizar».
  11. El sistema muestra «La contraseña es obligatoria» y no firma nada.
  12. Pulsa «Atrás»; el sistema vuelve al primer paso y la tarea sigue mostrando el estado «Pendiente de firmar».
  13. Entra en el documento del listado «Documentos a firmar» y comprueba que no tiene versión firmada.

- ESC-005 — Al salir del paso de firmar la clave tecleada no se conserva:
  1. El Administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de certificados digitales y pulsa «Añadir certificado digital».
  3. Rellena «DNI» con «85432016B», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12», deja «Contraseña» vacía, y deja marcado «Habilitado».
  4. Pulsa «Guardar».
  5. El Administrador cierra sesión.
  6. El director inicia sesión con «director@mislata.es» y contraseña «demo1234».
  7. Abre «Firmar documentos → Pendientes» y entra en la tarea de firma «Firma de prueba 5».
  8. Pulsa «Firmar todos los documentos».
  9. El sistema muestra el campo «Contraseña» vacío.
  10. Escribe «nadanada» en «Contraseña».
  11. Pulsa «Atrás».
  12. El sistema vuelve al primer paso, donde se ofrecen «Rechazar firmar» y «Firmar todos los documentos».
  13. Pulsa «Firmar todos los documentos».
  14. El sistema muestra el campo «Contraseña» vacío.

- ESC-013 — La clave tecleada no se ve en pantalla:
  1. El Administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de certificados digitales y pulsa «Añadir certificado digital».
  3. Rellena «DNI» con «85432016B», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12», deja «Contraseña» vacía, y deja marcado «Habilitado».
  4. Pulsa «Guardar».
  5. El Administrador cierra sesión.
  6. El director inicia sesión con «director@mislata.es» y contraseña «demo1234».
  7. Abre «Firmar documentos → Pendientes» y entra en la tarea de firma «Firma de prueba 5».
  8. Pulsa «Firmar todos los documentos».
  9. El sistema muestra el texto «Los documentos se firmarán en el servidor con su certificado digital. Introduzca la contraseña de su certificado.» junto a un campo «Contraseña» vacío.
  10. Escribe «nadanada» en el campo «Contraseña».
  11. El sistema muestra el contenido del campo «Contraseña» oculto: en la pantalla no se lee «nadanada», sino caracteres enmascarados.

## HU-002 — Como Firmante sin certificado digital dado de alta quiero seguir firmando con AutoFirma, para poder resolver igualmente mis tareas de firma

- ESC-006 — Sin certificado en el servidor se ofrece AutoFirma:
  1. El secretario inicia sesión con «secretario@mislata.es» y contraseña «demo1234».
  2. Abre «Firmar documentos → Pendientes» y entra en la tarea de firma «Firma de prueba del secretario».
  3. Pulsa «Firmar todos los documentos».
  4. El sistema muestra en el paso de firmar el aviso de que para firmar hace falta tener AutoFirma instalada y un certificado digital válido, junto con el enlace de descarga de AutoFirma.
  5. El sistema muestra el botón «Firmar todos los documentos con AutoFirma y finalizar» y no muestra el botón «Firmar todos los documentos y finalizar».
  6. El sistema no muestra ningún campo «PIN» ni ningún campo «Contraseña».

- ESC-007 — Al darle de alta un certificado deja de ofrecerse AutoFirma:
  1. El secretario inicia sesión con «secretario@mislata.es» y contraseña «demo1234».
  2. Abre «Firmar documentos → Pendientes» y entra en la tarea de firma «Firma de prueba del secretario».
  3. Pulsa «Firmar todos los documentos».
  4. El sistema muestra el botón «Firmar todos los documentos con AutoFirma y finalizar».
  5. Pulsa «Atrás».
  6. El secretario cierra sesión.
  7. El Administrador inicia sesión con usuario «admin» y contraseña «admin».
  8. Abre la pantalla de certificados digitales y pulsa «Añadir certificado digital».
  9. Rellena «DNI» con «29050788V», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12» y «Contraseña» con «nadanada», y deja marcado «Habilitado».
  10. Pulsa «Guardar».
  11. El Administrador cierra sesión.
  12. El secretario vuelve a iniciar sesión con «secretario@mislata.es» y contraseña «demo1234».
  13. Abre «Firmar documentos → Pendientes» y entra en la tarea de firma «Firma de prueba del secretario».
  14. Pulsa «Firmar todos los documentos».
  15. El sistema muestra el texto «Los documentos se firmarán en el servidor con su certificado digital.» y el botón «Firmar todos los documentos y finalizar», y ya no muestra el botón «Firmar todos los documentos con AutoFirma y finalizar».

- ESC-012 — Al deshabilitar su certificado se vuelve a ofrecer AutoFirma:
  1. El Administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de certificados digitales y pulsa «Añadir certificado digital».
  3. Rellena «DNI» con «85432016B», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12» y «Contraseña» con «nadanada», y deja marcado «Habilitado».
  4. Pulsa «Guardar» y cierra sesión.
  5. El director inicia sesión con «director@mislata.es» y contraseña «demo1234».
  6. Abre «Firmar documentos → Pendientes» y entra en la tarea de firma «Firma de prueba 5».
  7. Pulsa «Firmar todos los documentos».
  8. El sistema muestra el texto «Los documentos se firmarán en el servidor con su certificado digital.» y el botón «Firmar todos los documentos y finalizar».
  9. Pulsa «Atrás».
  10. El director cierra sesión.
  11. El Administrador inicia sesión con usuario «admin» y contraseña «admin».
  12. Abre la pantalla de certificados digitales y entra en el certificado del DNI «85432016B».
  13. Desmarca «Habilitado» y pulsa «Guardar».
  14. El Administrador cierra sesión.
  15. El director vuelve a iniciar sesión con «director@mislata.es» y contraseña «demo1234».
  16. Abre «Firmar documentos → Pendientes» y entra en la tarea de firma «Firma de prueba 5».
  17. Pulsa «Firmar todos los documentos».
  18. El sistema muestra el aviso de que para firmar hace falta tener AutoFirma instalada y un certificado digital válido, junto con el enlace de descarga de AutoFirma.
  19. El sistema muestra el botón «Firmar todos los documentos con AutoFirma y finalizar» y ya no muestra el botón «Firmar todos los documentos y finalizar» ni el texto «Los documentos se firmarán en el servidor con su certificado digital.».

## HU-003 — Como Firmante sin DNI en mi ficha quiero que la secretaría virtual me explique por qué no puedo firmar, para saber que tengo que dirigirme al administrador

- ESC-008 — Un firmante sin DNI no puede firmar:
  1. El Administrador inicia sesión con usuario «admin» y contraseña «admin», cuyo usuario no tiene DNI.
  2. Abre el menú «Firmar documentos → Pendientes».
  3. El listado muestra la tarea de firma «Firma de prueba del administrador 1».
  4. Pulsa la fila «Firma de prueba del administrador 1» y se abre el formulario de firma.
  5. Pulsa «Firmar todos los documentos».
  6. El sistema muestra «No es posible firmar los documentos porque su usuario no tiene un DNI. Póngase en contacto con el administrador.» y no muestra ningún botón de firmar: ni «Firmar todos los documentos con AutoFirma y finalizar» ni «Firmar todos los documentos y finalizar».
  7. Pulsa «Atrás»; el sistema vuelve al primer paso de la pantalla, donde se elige entre rechazar y firmar.
  8. Vuelve a abrir «Firmar documentos → Pendientes» y «Firma de prueba del administrador 1» sigue apareciendo en el listado, sin firmar.

- ESC-009 — Un firmante sin DNI sí puede rechazar la firma:
  1. El Administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «Firmar documentos → Pendientes».
  3. El listado muestra la tarea de firma «Firma de prueba del administrador 2».
  4. Pulsa la fila «Firma de prueba del administrador 2» y se abre el formulario de firma.
  5. Pulsa «Rechazar firmar».
  6. Escribe «No me corresponde firmar estos documentos» en el motivo del rechazo.
  7. Pulsa «Finalizar».
  8. El sistema deja la tarea en estado «Rechazada la firma».
  9. Vuelve a abrir «Firmar documentos → Pendientes» y el listado ya no muestra «Firma de prueba del administrador 2».

## HU-004 — Como Firmante quiero ver y resolver únicamente mis propias tareas de firma pendientes, para que nadie firme en mi nombre ni yo en el de otra persona

- ESC-010 — Cada firmante solo ve sus propias tareas pendientes:
  1. El director inicia sesión con «director@mislata.es» y contraseña «demo1234».
  2. Abre «Firmar documentos → Pendientes».
  3. El listado muestra «Firma de prueba 5» y no muestra ni «Firma de prueba del secretario» ni ninguna «Firma de prueba del administrador».
  4. El director cierra sesión.
  5. El secretario inicia sesión con «secretario@mislata.es» y contraseña «demo1234».
  6. Abre «Firmar documentos → Pendientes».
  7. El listado muestra «Firma de prueba del secretario» y no muestra ninguna «Firma de prueba» numerada del director ni ninguna «Firma de prueba del administrador».
  8. El secretario cierra sesión.
  9. El Administrador inicia sesión con usuario «admin» y contraseña «admin».
  10. Abre «Firmar documentos → Pendientes».
  11. El listado muestra «Firma de prueba del administrador 1» y, pese a ser administrador, no muestra ninguna «Firma de prueba» numerada del director ni «Firma de prueba del secretario».

- ESC-011 — El rechazo sigue funcionando para un firmante con certificado en el servidor:
  1. El Administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de certificados digitales y pulsa «Añadir certificado digital».
  3. Rellena «DNI» con «85432016B», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12» y «Contraseña» con «nadanada», y deja marcado «Habilitado».
  4. Pulsa «Guardar».
  5. El Administrador cierra sesión.
  6. El director inicia sesión con «director@mislata.es» y contraseña «demo1234».
  7. Abre «Firmar documentos → Pendientes» y entra en la tarea de firma «Firma de prueba 4».
  8. Pulsa «Rechazar firmar».
  9. Escribe «Estos documentos no me corresponden» en el motivo del rechazo.
  10. Pulsa «Finalizar».
  11. El sistema deja la tarea «Firma de prueba 4» en estado «Rechazada la firma».
  12. Abre «Firmar documentos → Pendientes» y comprueba que «Firma de prueba 4» ya no aparece en el listado.
  13. Abre «Firmar documentos → Rechazados», entra en «Firma de prueba 4», abre su documento y comprueba que no tiene versión firmada.

# Modelos

| Fichero | Modelo | Qué representa |
|---|---|---|
| [entity-TareaFirma.md](./entity-TareaFirma.md) | TareaFirma | El encargo de firmar uno o varios documentos que se le hace a una persona |

Una tarea de firma agrupa los documentos que hay que firmar (modelo existente `DocumentoFirma`, hijo suyo con borrado en cascada), cada uno con su versión original y, cuando se firma, su versión firmada. Esta iniciativa **no cambia** ese modelo hijo: solo cambia quién produce la versión firmada — hasta ahora siempre AutoFirma desde el ordenador del firmante, ahora también el propio servidor.

La tarea de firma no pertenece a ningún centro: se dirige a una persona concreta, su firmante.

El certificado digital de una persona es un dato del subsistema de criptografía que esta iniciativa solo **consulta** por el DNI del firmante; no lo modifica ni lo da de alta.

# Pantallas

| Fichero | Pantalla | Para qué sirve |
|---|---|---|
| [screen-documentos-pendientes-de-firma.md](./screen-documentos-pendientes-de-firma.md) | Documentos pendientes de firma | Donde cada firmante ve sus tareas de firma pendientes y las firma o las rechaza |

# Seguridad

- **Todos los tipos de usuario y todos los cargos** (Administrador, Supervisor, Profesor, Exprofesor, Alumno, Exalumno, Externo, Familiar; Director, Jefe de estudios, Secretario, Vicesecretario, Administrativo, Conserje): cada uno ve y resuelve **solo sus propias** tareas de firma —aquellas en las que él es el firmante—, y puede firmarlas o rechazarlas. No hay alcance por centro: la tarea de firma no pertenece a un centro, sino a una persona. Nadie puede firmar la tarea de otro, ni siquiera el Administrador.
- **Administrador**: además de lo anterior, es quien da de alta, modifica y deshabilita los certificados digitales de cualquier persona, y por tanto quien determina en qué situación de firma queda cada una.
- La firma en el servidor **no está restringida por tipo de usuario ni por cargo**: la puede usar cualquiera que tenga un certificado digital habilitado dado de alta para su DNI. El control real está en a quién se le da de alta un certificado.

# Recursos y datos iniciales

- **Certificado digital de ejemplo incluido en la aplicación**: el fichero `firma/mi_certificado.p12` que ya viaja dentro de la aplicación, con contraseña `nadanada`. No es un dato de base de datos: es un recurso del propio programa, y es el que se usa para dar de alta certificados de prueba.
- **Documento PDF de ejemplo incluido en la aplicación**: un PDF de una página, con espacio libre para colocar el recuadro de la firma, que sirve de documento a firmar de las tareas de firma precargadas.
- **Tareas de firma pendientes precargadas** (datos de demo), todas en estado «Pendiente de firmar», con su recuadro y su página de firma ya fijados y con el documento PDF de ejemplo como documento a firmar. Cada escenario que **resuelve** una tarea (firmándola o rechazándola) tiene la suya propia, porque una tarea resuelta no se puede devolver a pendiente:
  - Para el firmante `director@mislata.es` (DNI `85432016B`), cinco tareas: «Firma de prueba 1», «Firma de prueba 2», «Firma de prueba 4» y «Firma de prueba 5» con **un** documento cada una, y «Firma de prueba 3» con **dos** documentos.
  - Para el firmante `secretario@mislata.es` (DNI `29050788V`), una tarea: «Firma de prueba del secretario», con un documento.
  - Para el firmante `admin` (sin DNI), dos tareas: «Firma de prueba del administrador 1» y «Firma de prueba del administrador 2», con un documento cada una.

# Fuera de alcance

- **Crear tareas de firma desde la interfaz.** Sigue sin haber ninguna pantalla para encargar una firma a alguien; las tareas de firma con las que se trabaja son las precargadas.
- **La «firma rápida»**, el campo que ya existe en la tarea de firma y que nadie usa: se queda igual, sin uso.
- **Hacer opcional el PIN del dispositivo criptográfico.** El PIN sigue siendo obligatorio al dar de alta un dispositivo, así que la situación «dispositivo sin PIN guardado» **no se puede dar hoy**: su panel se construye igualmente, preparado para cuando el PIN deje de ser obligatorio, pero de momento queda inalcanzable.
- **Los escenarios de las dos situaciones de dispositivo criptográfico** (con PIN guardado y sin PIN guardado): exigen un dispositivo físico conectado y configurado en el servidor, que no existe en el entorno de pruebas. Su comportamiento queda especificado, pero sin escenario que lo compruebe.
- **El escenario de que la situación de firma cambie con la pantalla ya abierta** (que a alguien le deshabiliten el certificado justo mientras está en el paso de firmar). La regla está especificada —el servidor comprueba siempre la situación real en el momento de firmar y, si ya no puede, cancela con su mensaje de error—, pero no se escribe escenario que la compruebe porque exigiría dos sesiones simultáneas en el navegador.
- **Que la secretaría virtual recuerde el PIN o la contraseña** que el firmante teclea. Se usan para esa firma y se descartan siempre.
- **Avisar al firmante con una confirmación al terminar de firmar.** No se añade: la firma en el servidor se comporta como AutoFirma, que tampoco avisa; el firmante ve que la tarea desaparece de sus pendientes.
- **Ofrecer AutoFirma como alternativa cuando hay certificado en el servidor.** Si se puede firmar en el servidor, se firma en el servidor; el firmante no elige.
- **Firmar en el servidor la tarea de otra persona**, aunque sea el Administrador.
