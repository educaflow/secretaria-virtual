# Ejemplo completo de especificación

Esta carpeta es un ejemplo **instanciado** de una especificación terminada (la de "solicitud de certificados académicos"): el índice `specification.md`, un `entity-*.md` por modelo y un `screen-*.md` por pantalla, tal y como quedarían en la carpeta de una iniciativa real. Sirve como referencia del aspecto final.

- `specification.md` — el índice (único con frontmatter `type: specification`).
- `entity-SolicitudCertificado.md` — modelo con ciclo de vida: estados, restricciones, campo calculado y varias acciones con sus propiedades editables (`AllowProperties`), validaciones y reglas de negocio.
- `entity-TipoCertificado.md` — modelo de catálogo, sin ciclo de vida (muestra cómo se omiten las secciones no aplicables).
- `screen-mis-solicitudes.md`, `screen-solicitudes-centro.md` — listados sin reglas de UI propias.
- `screen-formulario-solicitud.md`, `screen-formulario-resolucion.md` — formularios con reglas de UI (`RUI`).

**MUST NOT** copiarse el contenido de estos ficheros al output: es solo referencia de forma.
