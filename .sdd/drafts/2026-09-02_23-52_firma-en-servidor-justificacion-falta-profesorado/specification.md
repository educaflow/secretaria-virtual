---
type: specification
template: expediente
---

# Objetivo

Permitir que el profesorado que presenta una justificación de falta firme su solicitud en el servidor, con el certificado digital que la secretaría virtual custodie para su documento de identidad, en lugar de tener que firmarla siempre desde su propio equipo.

# El trámite

- **Nombre visible:** Justificación de falta del profesorado
- **A qué colectivo va dirigido:** profesorado
- **Quién puede iniciarlo:** cualquier Profesor de un centro
- **Para qué sirve:** un profesor comunica al centro que ha faltado uno o varios días, indica el motivo y adjunta el justificante. La solicitud se firma y se presenta oficialmente, y a partir de ahí la resuelve la jefatura de estudios.
- **Texto de ayuda que ve el usuario antes de empezar:** *(sin cambios respecto a la versión actual)*
- **Versión:** una **modificación** de la versión actual (la primera) del trámite «Justificación de falta del profesorado». Cambia **solo** cómo se firma la solicitud en el estado `RECEPCION / PENDIENTE_PRESENTACION`: hasta ahora la solicitud se firmaba siempre en el equipo del propio profesor con la aplicación de firma del ciudadano; a partir de ahora, si la secretaría virtual custodia un certificado digital habilitado para su documento de identidad, la firma la pone el servidor con ese certificado. Todo lo demás del trámite se conserva tal cual.

  El cambio es **compatible con los expedientes ya abiertos**: no se elimina ni se renombra ninguna fase, ningún estado ni ningún dato, y no se reinterpreta ningún dato ya guardado. Un expediente que estuviera esperando a ser presentado sigue en el mismo estado y se presenta igual, solo que quizá firmándose de otra manera.

# Actores y perfiles

| Perfil | Qué papel juega en este trámite | Quién lo ostenta |
|---|---|---|
| CREADOR | Es el profesor que falta: crea el expediente, rellena los datos de la falta, adjunta el justificante y **firma y presenta** la solicitud. Es el único perfil al que afecta esta modificación. | Profesor del centro |

Además del perfil anterior, cualquier otro usuario con acceso al expediente lo ve en solo consulta mientras está en este estado, y solo puede salir.

El resto del trámite —en particular el perfil RESPONSABLE, que resuelve el expediente en la fase de tramitación— **no cambia** y por eso no se declara aquí.

# Historias de usuario

## HU-001 — Como Profesor con un certificado digital custodiado por la secretaría virtual quiero que sea ella quien firme mi solicitud, para no depender de la aplicación de firma de mi equipo

- ESC-001 — Se firma en el servidor con la contraseña del certificado ya guardada:
  1. El Administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de certificados digitales y pulsa el botón de añadir un certificado digital.
  3. Rellena «DNI» con «85432016B», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12» y «Contraseña» con «nadanada», y deja marcado «Habilitado».
  4. Pulsa «Guardar».
  5. El sistema guarda el certificado y lo muestra en el listado de certificados digitales, con el DNI «85432016B» y marcado como habilitado.
  6. El Administrador cierra sesión.
  7. El director inicia sesión con «director@mislata.es» y contraseña «demo1234».
  8. Abre la lista de trámites disponibles, elige «Justificación de falta del profesorado» y crea un expediente nuevo.
  9. Rellena «Días» con «3», elige «Mes» «Octubre» y deja en «Año» el año en curso, que es el que propone el sistema.
  10. Elige «Tipo de jornada» «Toda la jornada» y «Motivo» «Enfermedad común».
  11. Adjunta como justificante el fichero «justificante.pdf», un documento PDF de menos de 5 MB.
  12. Pulsa «Siguiente».
  13. El sistema deja el expediente en el estado «Pendiente de presentación» y muestra la solicitud generada.
  14. El sistema muestra el aviso «La solicitud se firmará en el servidor con su certificado digital.», no muestra ningún campo «Contraseña» ni ningún campo «PIN», muestra el botón «Firmar y Presentar la solicitud» y no muestra el botón «Firmar con AutoFirma y Presentar la solicitud».
  15. Pulsa «Firmar y Presentar la solicitud».
  16. El sistema pide confirmación con el texto «¿Esta seguro que desea presentar la documentación? No podrá deshacer esta acción».
  17. El director confirma.
  18. El sistema firma la solicitud en el servidor, deja constancia de la entrada de la documentación y deja el expediente en el estado «Pendiente de resolución».
  19. Abre la lista de sus expedientes de «Justificación de falta del profesorado».
  20. Abre el expediente que acaba de presentar, el de «3» días de «Octubre».
  21. Comprueba que la solicitud presentada aparece firmada.
  22. Comprueba que el expediente tiene guardado el justificante del registro de entrada.

- ESC-002 — Se firma en el servidor tecleando la contraseña del certificado:
  1. El Administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de certificados digitales y pulsa el botón de añadir un certificado digital.
  3. Rellena «DNI» con «85432016B», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12», **deja «Contraseña» vacía**, y deja marcado «Habilitado».
  4. Pulsa «Guardar».
  5. El sistema guarda el certificado y lo muestra en el listado de certificados digitales, con el DNI «85432016B» y marcado como habilitado.
  6. El Administrador cierra sesión.
  7. El director inicia sesión con «director@mislata.es» y contraseña «demo1234».
  8. Abre la lista de trámites disponibles, elige «Justificación de falta del profesorado» y crea un expediente nuevo.
  9. Rellena «Días» con «4», elige «Mes» «Octubre» y deja en «Año» el año en curso, que es el que propone el sistema.
  10. Elige «Tipo de jornada» «Toda la jornada» y «Motivo» «Enfermedad común».
  11. Adjunta como justificante el fichero «justificante.pdf», un documento PDF de menos de 5 MB.
  12. Pulsa «Siguiente».
  13. El sistema deja el expediente en el estado «Pendiente de presentación» y muestra el aviso «La solicitud se firmará en el servidor con su certificado digital. Introduzca la contraseña de su certificado.» junto a un campo «Contraseña» vacío.
  14. El sistema no muestra el botón «Firmar con AutoFirma y Presentar la solicitud».
  15. Escribe «nadanada» en «Contraseña».
  16. Pulsa «Firmar y Presentar la solicitud».
  17. El sistema pide confirmación con el texto «¿Esta seguro que desea presentar la documentación? No podrá deshacer esta acción».
  18. El director confirma.
  19. El sistema firma la solicitud en el servidor, deja constancia de la entrada de la documentación y deja el expediente en el estado «Pendiente de resolución».
  20. Abre la lista de sus expedientes de «Justificación de falta del profesorado».
  21. Abre el expediente que acaba de presentar, el de «4» días de «Octubre».
  22. Comprueba que la solicitud presentada aparece firmada.

- ESC-003 — Una contraseña incorrecta no presenta nada y se puede reintentar:
  1. El Administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de certificados digitales y pulsa el botón de añadir un certificado digital.
  3. Rellena «DNI» con «85432016B», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12», deja «Contraseña» vacía, y deja marcado «Habilitado».
  4. Pulsa «Guardar».
  5. El sistema guarda el certificado y lo muestra en el listado de certificados digitales, con el DNI «85432016B» y marcado como habilitado.
  6. El Administrador cierra sesión.
  7. El director inicia sesión con «director@mislata.es» y contraseña «demo1234».
  8. Abre la lista de trámites disponibles, elige «Justificación de falta del profesorado» y crea un expediente nuevo.
  9. Rellena «Días» con «5», elige «Mes» «Octubre» y deja en «Año» el año en curso, que es el que propone el sistema.
  10. Elige «Tipo de jornada» «Toda la jornada» y «Motivo» «Enfermedad común».
  11. Adjunta como justificante el fichero «justificante.pdf», un documento PDF de menos de 5 MB.
  12. Pulsa «Siguiente».
  13. El sistema deja el expediente en el estado «Pendiente de presentación» y muestra el campo «Contraseña» vacío.
  14. Escribe «claveequivocada» en «Contraseña».
  15. Pulsa «Firmar y Presentar la solicitud».
  16. El sistema pide confirmación con el texto «¿Esta seguro que desea presentar la documentación? No podrá deshacer esta acción».
  17. El director confirma.
  18. El sistema muestra el error «La contraseña indicada no es correcta» y deja el expediente en el estado «Pendiente de presentación», sin firmar la solicitud y sin dejar constancia de ninguna entrada.
  19. El sistema vuelve a mostrar el campo «Contraseña» vacío: la clave del intento fallido no se conserva.
  20. Escribe «nadanada» en «Contraseña».
  21. Pulsa «Firmar y Presentar la solicitud».
  22. El sistema pide confirmación con el texto «¿Esta seguro que desea presentar la documentación? No podrá deshacer esta acción».
  23. El director confirma.
  24. El sistema firma la solicitud en el servidor, deja constancia de la entrada de la documentación y deja el expediente en el estado «Pendiente de resolución».

- ESC-004 — La contraseña se deja en blanco:
  1. El Administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de certificados digitales y pulsa el botón de añadir un certificado digital.
  3. Rellena «DNI» con «85432016B», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12», deja «Contraseña» vacía, y deja marcado «Habilitado».
  4. Pulsa «Guardar».
  5. El sistema guarda el certificado y lo muestra en el listado de certificados digitales, con el DNI «85432016B» y marcado como habilitado.
  6. El Administrador cierra sesión.
  7. El director inicia sesión con «director@mislata.es» y contraseña «demo1234».
  8. Abre la lista de trámites disponibles, elige «Justificación de falta del profesorado» y crea un expediente nuevo.
  9. Rellena «Días» con «6», elige «Mes» «Octubre» y deja en «Año» el año en curso, que es el que propone el sistema.
  10. Elige «Tipo de jornada» «Toda la jornada» y «Motivo» «Enfermedad común».
  11. Adjunta como justificante el fichero «justificante.pdf», un documento PDF de menos de 5 MB.
  12. Pulsa «Siguiente».
  13. El sistema deja el expediente en el estado «Pendiente de presentación» y muestra el campo «Contraseña» vacío.
  14. Deja el campo «Contraseña» vacío.
  15. Pulsa «Firmar y Presentar la solicitud».
  16. El sistema pide confirmación con el texto «¿Esta seguro que desea presentar la documentación? No podrá deshacer esta acción».
  17. El director confirma.
  18. El sistema muestra el error «La contraseña es obligatoria», no firma nada, no deja constancia de ninguna entrada y deja el expediente en el estado «Pendiente de presentación».

- ESC-005 — La contraseña tecleada no se conserva al volver atrás:
  1. El Administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de certificados digitales y pulsa el botón de añadir un certificado digital.
  3. Rellena «DNI» con «85432016B», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12», deja «Contraseña» vacía, y deja marcado «Habilitado».
  4. Pulsa «Guardar».
  5. El sistema guarda el certificado y lo muestra en el listado de certificados digitales, con el DNI «85432016B» y marcado como habilitado.
  6. El Administrador cierra sesión.
  7. El director inicia sesión con «director@mislata.es» y contraseña «demo1234».
  8. Abre la lista de trámites disponibles, elige «Justificación de falta del profesorado» y crea un expediente nuevo.
  9. Rellena «Días» con «7», elige «Mes» «Octubre» y deja en «Año» el año en curso, que es el que propone el sistema.
  10. Elige «Tipo de jornada» «Toda la jornada» y «Motivo» «Enfermedad común».
  11. Adjunta como justificante el fichero «justificante.pdf», un documento PDF de menos de 5 MB.
  12. Pulsa «Siguiente».
  13. El sistema deja el expediente en el estado «Pendiente de presentación» y muestra el campo «Contraseña» vacío.
  14. Escribe «nadanada» en «Contraseña».
  15. Pulsa «Atrás».
  16. El sistema devuelve el expediente al estado «Entrada de datos» y muestra los datos de la falta tal como se rellenaron: «Días» con «7», «Mes» «Octubre», el año en curso en «Año», «Tipo de jornada» «Toda la jornada», «Motivo» «Enfermedad común» y el justificante adjunto.
  17. Pulsa «Siguiente».
  18. El sistema vuelve a dejar el expediente en el estado «Pendiente de presentación» y muestra el campo «Contraseña» vacío.

- ESC-006 — La contraseña tecleada no se ve en pantalla:
  1. El Administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de certificados digitales y pulsa el botón de añadir un certificado digital.
  3. Rellena «DNI» con «85432016B», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12», deja «Contraseña» vacía, y deja marcado «Habilitado».
  4. Pulsa «Guardar».
  5. El sistema guarda el certificado y lo muestra en el listado de certificados digitales, con el DNI «85432016B» y marcado como habilitado.
  6. El Administrador cierra sesión.
  7. El director inicia sesión con «director@mislata.es» y contraseña «demo1234».
  8. Abre la lista de trámites disponibles, elige «Justificación de falta del profesorado» y crea un expediente nuevo.
  9. Rellena «Días» con «8», elige «Mes» «Octubre» y deja en «Año» el año en curso, que es el que propone el sistema.
  10. Elige «Tipo de jornada» «Toda la jornada» y «Motivo» «Enfermedad común».
  11. Adjunta como justificante el fichero «justificante.pdf», un documento PDF de menos de 5 MB.
  12. Pulsa «Siguiente».
  13. El sistema deja el expediente en el estado «Pendiente de presentación» y muestra el aviso «La solicitud se firmará en el servidor con su certificado digital. Introduzca la contraseña de su certificado.» junto a un campo «Contraseña» vacío.
  14. Escribe «nadanada» en el campo «Contraseña».
  15. El sistema muestra el contenido del campo «Contraseña» oculto: en la pantalla no se lee «nadanada», sino caracteres enmascarados.

- ESC-010 — La clave que custodia la secretaría virtual no abre el certificado:
  1. El Administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de certificados digitales y pulsa el botón de añadir un certificado digital.
  3. Rellena «DNI» con «85432016B», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12» y «Contraseña» con «claveequivocada», y deja marcado «Habilitado».
  4. Pulsa «Guardar».
  5. El sistema guarda el certificado y lo muestra en el listado de certificados digitales, con el DNI «85432016B» y marcado como habilitado.
  6. El Administrador cierra sesión.
  7. El director inicia sesión con «director@mislata.es» y contraseña «demo1234».
  8. Abre la lista de trámites disponibles, elige «Justificación de falta del profesorado» y crea un expediente nuevo.
  9. Rellena «Días» con «12», elige «Mes» «Octubre» y deja en «Año» el año en curso, que es el que propone el sistema.
  10. Elige «Tipo de jornada» «Toda la jornada» y «Motivo» «Enfermedad común».
  11. Adjunta como justificante el fichero «justificante.pdf», un documento PDF de menos de 5 MB.
  12. Pulsa «Siguiente».
  13. El sistema deja el expediente en el estado «Pendiente de presentación», muestra el aviso «La solicitud se firmará en el servidor con su certificado digital.» y no muestra ningún campo «Contraseña».
  14. Pulsa «Firmar y Presentar la solicitud».
  15. El sistema pide confirmación con el texto «¿Esta seguro que desea presentar la documentación? No podrá deshacer esta acción».
  16. El director confirma.
  17. El sistema muestra el error «La clave guardada de su certificado digital no es correcta. Póngase en contacto con el administrador», no firma la solicitud, no deja constancia de ninguna entrada y deja el expediente en el estado «Pendiente de presentación».

## HU-002 — Como Profesor sin certificado digital custodiado quiero seguir firmando la solicitud en mi propio equipo, para poder presentarla igualmente

- ESC-007 — Sin certificado custodiado se sigue ofreciendo la firma en el equipo del profesor:
  1. El secretario inicia sesión con «secretario@mislata.es» y contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Justificación de falta del profesorado» y crea un expediente nuevo.
  3. Rellena «Días» con «9», elige «Mes» «Octubre» y deja en «Año» el año en curso, que es el que propone el sistema.
  4. Elige «Tipo de jornada» «Toda la jornada» y «Motivo» «Enfermedad común».
  5. Adjunta como justificante el fichero «justificante.pdf», un documento PDF de menos de 5 MB.
  6. Pulsa «Siguiente».
  7. El sistema deja el expediente en el estado «Pendiente de presentación» y muestra la solicitud generada.
  8. El sistema muestra el aviso «Para presentar la solicitud debe tener la aplicación de AutoFirma instalada y un certificado digital válido» y el botón «Firmar con AutoFirma y Presentar la solicitud».
  9. El sistema no muestra el botón «Firmar y Presentar la solicitud», ni ningún campo «Contraseña», ni ningún campo «PIN».

- ESC-008 — Al darle de alta un certificado deja de ofrecerse la firma en el equipo:
  1. El secretario inicia sesión con «secretario@mislata.es» y contraseña «demo1234».
  2. Abre la lista de trámites disponibles, elige «Justificación de falta del profesorado» y crea un expediente nuevo.
  3. Rellena «Días» con «10», elige «Mes» «Octubre» y deja en «Año» el año en curso, que es el que propone el sistema.
  4. Elige «Tipo de jornada» «Toda la jornada» y «Motivo» «Enfermedad común».
  5. Adjunta como justificante el fichero «justificante.pdf», un documento PDF de menos de 5 MB.
  6. Pulsa «Siguiente».
  7. El sistema deja el expediente en el estado «Pendiente de presentación» y muestra el botón «Firmar con AutoFirma y Presentar la solicitud».
  8. El secretario cierra sesión.
  9. El Administrador inicia sesión con usuario «admin» y contraseña «admin».
  10. Abre la pantalla de certificados digitales y pulsa el botón de añadir un certificado digital.
  11. Rellena «DNI» con «29050788V», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12» y «Contraseña» con «nadanada», y deja marcado «Habilitado».
  12. Pulsa «Guardar».
  13. El sistema guarda el certificado y lo muestra en el listado de certificados digitales, con el DNI «29050788V» y marcado como habilitado.
  14. El Administrador cierra sesión.
  15. El secretario vuelve a iniciar sesión con «secretario@mislata.es» y contraseña «demo1234».
  16. Abre la lista de sus expedientes de «Justificación de falta del profesorado».
  17. Abre el expediente que dejó en «Pendiente de presentación», el de «10» días de «Octubre».
  18. El sistema muestra el aviso «La solicitud se firmará en el servidor con su certificado digital.» y el botón «Firmar y Presentar la solicitud».
  19. El sistema ya no muestra el botón «Firmar con AutoFirma y Presentar la solicitud» ni el aviso «Para presentar la solicitud debe tener la aplicación de AutoFirma instalada y un certificado digital válido».

- ESC-009 — Al deshabilitar su certificado se vuelve a ofrecer la firma en el equipo:
  1. El Administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de certificados digitales y pulsa el botón de añadir un certificado digital.
  3. Rellena «DNI» con «85432016B», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12» y «Contraseña» con «nadanada», y deja marcado «Habilitado».
  4. Pulsa «Guardar».
  5. El sistema guarda el certificado y lo muestra en el listado de certificados digitales, con el DNI «85432016B» y marcado como habilitado.
  6. El Administrador cierra sesión.
  7. El director inicia sesión con «director@mislata.es» y contraseña «demo1234».
  8. Abre la lista de trámites disponibles, elige «Justificación de falta del profesorado» y crea un expediente nuevo.
  9. Rellena «Días» con «11», elige «Mes» «Octubre» y deja en «Año» el año en curso, que es el que propone el sistema.
  10. Elige «Tipo de jornada» «Toda la jornada» y «Motivo» «Enfermedad común».
  11. Adjunta como justificante el fichero «justificante.pdf», un documento PDF de menos de 5 MB.
  12. Pulsa «Siguiente».
  13. El sistema deja el expediente en el estado «Pendiente de presentación», muestra el aviso «La solicitud se firmará en el servidor con su certificado digital.» y el botón «Firmar y Presentar la solicitud».
  14. El director cierra sesión.
  15. El Administrador inicia sesión con usuario «admin» y contraseña «admin».
  16. Abre la pantalla de certificados digitales y entra en el certificado del DNI «85432016B».
  17. Desmarca «Habilitado».
  18. Pulsa «Guardar».
  19. El sistema guarda el certificado y lo muestra en el listado de certificados digitales con el DNI «85432016B» y ya no marcado como habilitado.
  20. El Administrador cierra sesión.
  21. El director vuelve a iniciar sesión con «director@mislata.es» y contraseña «demo1234».
  22. Abre la lista de sus expedientes de «Justificación de falta del profesorado».
  23. Abre el expediente que dejó en «Pendiente de presentación», el de «11» días de «Octubre».
  24. El sistema muestra el aviso «Para presentar la solicitud debe tener la aplicación de AutoFirma instalada y un certificado digital válido» y el botón «Firmar con AutoFirma y Presentar la solicitud».
  25. El sistema ya no muestra el botón «Firmar y Presentar la solicitud» ni el aviso «La solicitud se firmará en el servidor con su certificado digital.».

- ESC-011 — El certificado de otra persona no habilita la firma en el servidor:
  1. El Administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de certificados digitales y comprueba en el listado que no hay ningún certificado para el DNI «29050788V».
  3. Pulsa el botón de añadir un certificado digital.
  4. Rellena «DNI» con «85432016B», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12» y «Contraseña» con «nadanada», y deja marcado «Habilitado».
  5. Pulsa «Guardar».
  6. El sistema guarda el certificado y lo muestra en el listado de certificados digitales, con el DNI «85432016B» y marcado como habilitado.
  7. El Administrador cierra sesión.
  8. El secretario inicia sesión con «secretario@mislata.es» y contraseña «demo1234», cuyo documento de identidad es «29050788V».
  9. Abre la lista de trámites disponibles, elige «Justificación de falta del profesorado» y crea un expediente nuevo.
  10. Rellena «Días» con «13», elige «Mes» «Octubre» y deja en «Año» el año en curso, que es el que propone el sistema.
  11. Elige «Tipo de jornada» «Toda la jornada» y «Motivo» «Enfermedad común».
  12. Adjunta como justificante el fichero «justificante.pdf», un documento PDF de menos de 5 MB.
  13. Pulsa «Siguiente».
  14. El sistema deja el expediente en el estado «Pendiente de presentación», muestra el aviso «Para presentar la solicitud debe tener la aplicación de AutoFirma instalada y un certificado digital válido» y el botón «Firmar con AutoFirma y Presentar la solicitud».
  15. El sistema no muestra el aviso «La solicitud se firmará en el servidor con su certificado digital.», ni el botón «Firmar y Presentar la solicitud», ni ningún campo «Contraseña».

## HU-003 — Como Jefe de estudios quiero consultar un expediente que está pendiente de presentación sin que se me ofrezca firmarlo, para que la solicitud solo la firme quien la presenta

- ESC-012 — Quien no tiene el turno no ve el bloque de firma ni sus botones:
  1. El Administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla de certificados digitales y pulsa el botón de añadir un certificado digital.
  3. Rellena «DNI» con «85432016B», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12» y «Contraseña» con «nadanada», y deja marcado «Habilitado».
  4. Pulsa «Guardar».
  5. El sistema guarda el certificado y lo muestra en el listado de certificados digitales, con el DNI «85432016B» y marcado como habilitado.
  6. El Administrador cierra sesión.
  7. El director inicia sesión con «director@mislata.es» y contraseña «demo1234».
  8. Abre la lista de trámites disponibles, elige «Justificación de falta del profesorado» y crea un expediente nuevo.
  9. Rellena «Días» con «14», elige «Mes» «Octubre» y deja en «Año» el año en curso, que es el que propone el sistema.
  10. Elige «Tipo de jornada» «Toda la jornada» y «Motivo» «Enfermedad común».
  11. Adjunta como justificante el fichero «justificante.pdf», un documento PDF de menos de 5 MB.
  12. Pulsa «Siguiente».
  13. El sistema deja el expediente en el estado «Pendiente de presentación» y muestra el aviso «La solicitud se firmará en el servidor con su certificado digital.».
  14. El director cierra sesión.
  15. El jefe de estudios inicia sesión con «jefeestudios1@mislata.es» y contraseña «demo1234».
  16. Abre la lista de expedientes de «Justificación de falta del profesorado» de su centro.
  17. Abre el expediente que el director dejó en «Pendiente de presentación», el de «14» días de «Octubre».
  18. El sistema muestra la solicitud generada en solo consulta y no muestra el aviso «La solicitud se firmará en el servidor con su certificado digital.», ni ningún campo «Contraseña», ni ningún campo «PIN», ni el botón «Firmar y Presentar la solicitud», ni el botón «Firmar con AutoFirma y Presentar la solicitud».
  19. El sistema muestra un único botón «Salir».
  20. Pulsa «Salir».
  21. El sistema le devuelve al listado y el expediente sigue en el estado «Pendiente de presentación», sin ningún cambio.

# Fases y estados

- **Estado en el que nace el expediente:** ENTRADA_DATOS (fase RECEPCION) — *(sin cambios)*
- **Estados que cierran el expediente:** ACEPTADO, RECHAZADO (fase TRAMITACION) — *(sin cambios)*
- **Desde qué estado se puede borrar el expediente:** ENTRADA_DATOS (fase RECEPCION) — *(sin cambios)*

Lo que esta modificación cambia del ciclo de vida está en [estados.md](./estados.md). Todo lo que ese fichero no menciona se conserva tal cual.

| Fase | Título que ve el usuario | Estados | Pantallas |
|---|---|---|---|
| RECEPCION | Recepción | ENTRADA_DATOS, PENDIENTE_PRESENTACION | [pantallas-recepcion.md](./pantallas-recepcion.md) |
| TRAMITACION | Tramitación | PENDIENTE_RESOLUCION, ACEPTADO, RECHAZADO | *(sin cambios)* |

# Pantallas

| Fichero | Fase | Qué pantallas contiene |
|---|---|---|
| [pantallas-recepcion.md](./pantallas-recepcion.md) | RECEPCION | La pantalla del perfil CREADOR en el estado PENDIENTE_PRESENTACION, que es la única que cambia |
| *(sin cambios)* | TRAMITACION | Ninguna pantalla de esta fase cambia |

# Documentos

El trámite genera dos documentos —la solicitud y la resolución— y esta modificación solo afecta a **quién firma la solicitud y dónde**. El delta está en [documentos.md](./documentos.md); la resolución no cambia.

# Registros de entrada y salida, y notificaciones

- **Registros de entrada:** *(sin cambios)* — se sigue dejando constancia de la entrada al presentar la solicitud, con la solicitud firmada como documento principal y el justificante como anexo. Lo único que cambia es de dónde sale la firma de ese documento principal.
- **Registros de salida:** *(sin cambios)*
- **Avisos que se envían:** *(sin cambios)*

# Seguridad

- **Profesor:** crea, firma y presenta sus propias justificaciones de falta — solo los expedientes que él mismo ha creado. *(sin cambios)*
- **Jefe de estudios:** resuelve las justificaciones de falta — las de su centro. *(sin cambios)*
- **Administrador:** da de alta, modifica y deshabilita los certificados digitales de cualquier persona y, por tanto, es quien determina si la solicitud de un profesor se firmará en su equipo o en el servidor — todos los centros. No firma en nombre de nadie ni interviene en el expediente.

La firma en el servidor **no se restringe por tipo de usuario ni por cargo**: la usa cualquier profesor para cuyo documento de identidad exista un certificado digital habilitado. El control real está en a quién se le da de alta un certificado.

# Datos iniciales

- **Asignación de perfiles:** *(sin cambios)*
- **Categoría del trámite:** *(sin cambios)*
- **Otros datos maestros que el trámite necesita:** el trámite **no** precarga ningún certificado digital. Los escenarios usan el certificado de ejemplo que ya viaja dentro de la aplicación, que el Administrador da de alta indicando como ubicación «firma/mi_certificado.p12» y como contraseña «nadanada».

# Fuera de alcance

- **Todo el resto del trámite.** No cambian la entrada de datos, el borrado del expediente, la vuelta atrás, la fase de tramitación, la resolución, la subsanación ni ninguna de sus pantallas.
- **El contenido de la solicitud y el lugar donde se estampa su firma.** Solo cambia quién produce la firma.
- **Que el profesor elija cómo firmar.** Si la secretaría virtual custodia un certificado suyo habilitado, se firma en el servidor; si no, se firma en su equipo. El profesor no elige.
- **Dar de alta certificados digitales desde este trámite.** Los certificados se siguen gestionando solo desde la pantalla del Administrador.
- **Que la secretaría virtual recuerde la contraseña o el PIN que el profesor teclea.** Se usan para esa firma y se descartan siempre.
- **Avisar al profesor con una confirmación adicional al terminar de firmar.** Sigue bastando con que el expediente pase a estar pendiente de resolución.
- **Los escenarios de las dos situaciones de dispositivo criptográfico** (con PIN guardado y sin PIN guardado): exigen un dispositivo físico conectado y configurado en el servidor, que no existe en el entorno de pruebas. Su comportamiento queda especificado, pero sin escenario que lo compruebe.
- **El escenario de presentar la solicitud firmándola de principio a fin en el equipo del profesor.** Ese camino es exactamente el comportamiento actual, que esta modificación no toca, y además exige tener instalada la aplicación de firma del ciudadano en la máquina desde la que se prueba. Los escenarios ESC-007, ESC-008, ESC-009 y ESC-011 sí comprueban lo que esta modificación decide sobre ese camino: cuándo se ofrece y cuándo deja de ofrecerse.
- **El escenario del profesor sin documento de identidad.** La regla queda especificada —no se le ofrece firmar y se le remite al administrador—, pero no hay ninguna cuenta de profesor de demo sin documento de identidad con la que reproducirlo.
- **El escenario de que la situación de firma cambie con la pantalla ya abierta** (que a alguien le deshabiliten el certificado justo mientras tiene el expediente abierto). La regla está especificada —el servidor comprueba siempre la situación real en el momento de firmar—, pero no se escribe escenario porque exigiría dos sesiones simultáneas en el navegador.
