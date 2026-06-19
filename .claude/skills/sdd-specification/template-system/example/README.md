# Ejemplo completo de especificación

Esta carpeta es un ejemplo **instanciado** de una especificación terminada (la de "solicitud de certificados académicos"): el índice `specification.md`, un `entity-*.md` por modelo y un `screen-*.md` por pantalla, tal y como quedarían en la carpeta de una iniciativa real. Sirve como referencia del aspecto final.

- `specification.md` — el índice (único con frontmatter `type: specification`).
- `entity-SolicitudCertificado.md` — modelo con ciclo de vida: estados, restricciones, campo calculado y varias acciones con sus propiedades editables (`AllowProperties`), validaciones y reglas de negocio.
- `entity-AdjuntoSolicitud.md` — modelo hijo (composición): los documentos que el alumno aporta con una solicitud; inmutables una vez creados.
- `entity-TipoCertificado.md` — modelo de catálogo, sin ciclo de vida (muestra cómo se omiten las secciones no aplicables).
- `screen-mis-solicitudes.md` — pantalla de **varias vistas anidadas** que alternan listado y formulario: listado de solicitudes → formulario de solicitud → listado de documentos adjuntos (panel maestro-detalle) → formulario del adjunto. Es el ejemplo de árbol «Estructura jerárquica de las vistas» con `## Vista` por vista, y muestra cómo un **listado** (columnas, orden, búsqueda, qué formulario abre) y un **formulario** (modo, paneles tipados) se describen con subsecciones distintas.
- `screen-solicitudes-centro.md` — pantalla de **varias vistas**: un listado y su formulario de detalle (con la acción de resolución y su regla de UI `RUI`).

**MUST NOT** copiarse el contenido de estos ficheros al output: es solo referencia de forma.
