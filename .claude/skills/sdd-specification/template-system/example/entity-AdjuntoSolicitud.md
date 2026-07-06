# Modelo: AdjuntoSolicitud

Documento que el alumno aporta junto a una solicitud de certificado (por ejemplo, un justificante). Pertenece a una única solicitud y se aporta durante el alta; una vez enviada la solicitud, sus adjuntos no cambian. No tiene ciclo de vida propio (por eso se omite la sección «Estados y transiciones»).

## Campos

- **nombre de fichero** — nombre del documento aportado
- **contenido** — el fichero en sí

## Restricciones

- RES-AdjuntoSolicitud-001 — Dentro de una misma solicitud no puede haber dos adjuntos con el mismo nombre de fichero
- RES-AdjuntoSolicitud-002 — El nombre de fichero es obligatorio
- RES-AdjuntoSolicitud-003 — El contenido es obligatorio

## Acción: Crear

**Input AllowProperties:** nombre de fichero, contenido

## Acción: Modificar

**Input AllowProperties:** (ninguna — los adjuntos son inmutables una vez creados; solo se aportan durante el alta de la solicitud)
