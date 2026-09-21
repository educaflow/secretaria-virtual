# Modelo: TipoExpediente

**Modelo existente:** sí

Cada versión de un trámite que hay dada de alta en la aplicación.
Los tipos de expediente no los crea ni los mantiene ningún usuario: los registra la propia aplicación al arrancar, a partir de la definición de cada trámite.
No cambia ninguno de sus campos.
Lo que cambia es que desde la aplicación pasa a ser **solo de consulta**: no se puede dar de alta, modificar ni borrar ninguno, y de él solo se enseñan el código, el nombre y el trámite.

## Restricciones

- RES-TipoExpediente-001 — Ningún usuario, tampoco el Administrador, puede crear, modificar ni borrar un tipo de expediente desde la aplicación

## Acción: Crear

**Input AllowProperties:** (ninguna — los tipos de expediente son de solo consulta: los registra la aplicación al arrancar)

## Acción: Modificar

**Input AllowProperties:** (ninguna — los tipos de expediente son de solo consulta: ningún valor se puede cambiar)
