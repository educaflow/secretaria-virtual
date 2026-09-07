# Documentos del trámite

Este fichero es un **delta**: declara solo lo que la modificación cambia de los documentos del trámite. El documento de resolución no se toca y por eso no aparece.

## Resumen

| Documento | Cuándo se genera | Quién lo firma | Se registra |
|---|---|---|---|
| Solicitud | al lanzar PRESENTAR desde RECEPCION / PENDIENTE_PRESENTACION | el propio profesor, en su equipo **o** —y esto es lo que cambia— el servidor con el certificado custodiado que corresponde al documento de identidad de ese mismo profesor | de entrada |
| Resolución | *(sin cambios)* | *(sin cambios)* | *(sin cambios)* |

---

## Documento: Solicitud

- **Qué es:** *(sin cambios)* — es el impreso con los datos de la falta que el profesor presenta al centro y que, firmado, acredita que es él quien la comunica.
- **Cuándo se genera:** *(sin cambios)* — se genera al pasar al estado RECEPCION / PENDIENTE_PRESENTACION y se firma al lanzar la acción PRESENTAR desde ese estado.
- **Quién lo firma y dónde:** **es lo único que cambia.** Lo firma siempre el propio profesor interesado, pero la firma se puede producir en dos sitios, y el sistema decide en cuál sin que el profesor elija:
  - **el propio interesado, en su equipo, con su certificado digital** — cuando la secretaría virtual no custodia ningún certificado digital habilitado para su documento de identidad. Necesita tener un certificado instalado y la aplicación de firma del ciudadano, y el sistema comprueba después que la firma es válida, que es una sola, que el certificado es de confianza, que no ha alterado el documento y que corresponde a su documento de identidad. *(es el comportamiento actual, que se conserva)*
  - **el propio interesado, con el certificado custodiado que corresponde a su documento de identidad** — cuando la secretaría virtual sí custodia un certificado digital habilitado para él. La firma la pone el servidor: el profesor solo aporta la clave del certificado si la secretaría virtual no la custodia también.
- **Dónde se estampa la firma:** *(sin cambios)* — el recuadro de la firma va en el mismo sitio de la misma página que ahora, se firme donde se firme.
- **Se registra:** *(sin cambios)* — de entrada, con la solicitud firmada como documento principal y el justificante de la falta como anexo.
- **Qué datos del expediente aparecen en él:** *(sin cambios)*
- **Textos fijos que lleva impresos:** *(sin cambios)*
- **Idiomas:** *(sin cambios)*
- **A quién se le muestra y dónde:** *(sin cambios)*

---

## Trozos comunes a varios documentos

*(sin cambios)*
