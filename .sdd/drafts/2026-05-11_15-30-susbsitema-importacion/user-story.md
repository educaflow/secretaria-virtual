---
type: user-story
---

# Importación de usuarios

En la secretaría virtual hay sistemas que generan documentos PDF que un usuario debe firmar para que el trámite avance (un certificado, un acta, una resolución de un expediente, una solicitud, etc.). Hoy esa firma se hace fuera de la aplicación y luego alguien la sube a mano: queremos que los usuarios puedan firmar dentro de la aplicación, con su certificado digital.

## Quién interviene

- **El sistema solicitante**: cualquier parte de la aplicación que genera un PDF y necesita la firma del usuario para continuar (por ejemplo un módulo de expedientes, de actas, de certificados…). No es un usuario humano: es código que pide la firma y necesita saber el resultado.
- **El firmante**: el usuario al que se le pide firmar uno o varios documentos. Tiene un certificado digital instalado en su equipo y la aplicación de AutoFirma.
- **El administrador**: ocasionalmente necesita ver todas las firmas que están en marcha en el sistema para consultas o auditoría.

## Qué tiene que pasar

1. Un sistema solicitante crea una "solicitud de firma" para un firmante concreto. La solicitud incluye uno o varios PDF, el motivo de la firma, y el área del documento donde tiene que aparecer estampada la firma. Junto con la solicitud, el sistema solicitante deja indicado a quién avisar cuando la firma se resuelva (firmada o rechazada) y con qué datos de contexto.

2. El firmante entra en la aplicación y, en el menú "Firmar documentos > Pendientes", ve la lista de solicitudes que tiene abiertas. Solo ve las suyas — nadie ve las de otros.

3. Al abrir una solicitud, el firmante ve el motivo, los PDF que tiene que firmar, y dos opciones: **firmar** o **rechazar**.

4. Si **rechaza**, debe escribir un motivo. Sin motivo no puede confirmar el rechazo. Una vez confirmado, la solicitud queda como rechazada con la fecha de resolución y el sistema solicitante recibe el aviso del rechazo.

5. Si **firma**, la aplicación lanza AutoFirma con su certificado para que firme cada PDF en la zona indicada por el sistema solicitante. Cuando AutoFirma termina, los PDF firmados llegan al servidor; el servidor comprueba que cada firma es válida y que está hecha con el DNI del firmante. Si la comprobación falla en algún documento, se le muestra al usuario el error con el nombre del fichero y por qué falla, y la solicitud sigue pendiente. Si la comprobación pasa, la solicitud queda como firmada con la fecha de resolución y el sistema solicitante recibe el aviso de la firma.

6. El firmante también puede consultar, en menús aparte, las solicitudes que ya están **firmadas** y las que están **rechazadas** (solo lectura). En "Firmadas" ve los PDF firmados; en "Rechazadas" ve los PDF originales y el motivo que dio.

7. El administrador tiene un menú adicional, "Todos", donde ve todas las solicitudes del sistema, sin filtrar por estado ni por usuario, también de solo lectura.

## Restricciones que no pueden romperse

- Un usuario no puede ver, modificar ni borrar solicitudes de otros usuarios.
- Un usuario no puede crear solicitudes desde la pantalla — las crea siempre el sistema solicitante.
- Una solicitud rechazada o firmada no se puede revertir ni cambiar — son estados finales.
- Los PDF originales que el solicitante envía no deben poderse alterar después por el solicitante (por ejemplo, sustituyendo el contenido entre que se crea la solicitud y se firma): la aplicación debe quedarse con su propia copia.
- Si el firmante rechaza, el motivo es obligatorio.
- Si el firmante firma, la firma criptográfica de cada PDF debe validar contra el original y contra su DNI; si no, no se acepta.

## Lo que aporta valor

- El firmante ya no tiene que firmar fuera y subir nada — todo el ciclo está dentro de la aplicación.
- El sistema solicitante recibe automáticamente el resultado y puede continuar el trámite sin intervención humana adicional.
- Cada usuario solo ve lo suyo, lo que evita ruido y respeta la privacidad de las firmas pendientes de otros.
