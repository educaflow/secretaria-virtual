# Modelo: TipoCertificado

Catálogo de los certificados que el centro puede emitir. Es un dato de referencia: las solicitudes apuntan a uno de estos tipos. Se precarga al arrancar. No tiene ciclo de vida (por eso se omite la sección «Estados y transiciones»).

## Campos

- **nombre** — nombre del tipo de certificado (p. ej. «Certificado de matrícula»)
- **descripción** — explicación de para qué sirve el certificado

## Acción: Crear

**Input AllowProperties:** nombre, descripción

## Acción: Modificar

**Input AllowProperties:** nombre, descripción
