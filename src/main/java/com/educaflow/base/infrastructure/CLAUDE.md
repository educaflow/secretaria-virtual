# CLAUDE.md — `base.infrastructure`

Clases completas y reutilizables en cualquier proyecto (no dependen del dominio de negocio de la secretaría virtual). Cada subpaquete resuelve una capacidad técnica concreta.

## Paquetes

- `autofirma` — Integración con el cliente de escritorio AutoFirma para firma con certificado del usuario.
- `axelorhelper` — Helpers sobre las acciones de Axelor: `ActionRequestHelper` / `ActionResponseHelper`.
- `criptografia` — Certificados X.509, almacenes de clave (fichero/HSM/dispositivo), emisores (FNMT/ACCV/DNI) y datos de certificado.
- `db` — Acceso de bajo nivel al esquema de base de datos y operaciones masivas (`DatabaseSchema`, `Table`, `BulkTables`).
- `evaluator` — Evaluación de expresiones (implementación Groovy).
- `mail` — Envío de correo (`Mail`, `Attach`, `MailSender` y su impl SMTP).
- `mapper` — Mapeo entre DTOs y entidades Axelor (`BeanMapperModel`) y comparación de colecciones de modelos.
- `metafile` — Helper de alto nivel sobre `MetaFile` de Axelor (`MetaFileHelper`).
- `numeradores` — Generación de números/secuencias persistentes (entidad `Numerador` y su repositorio).
- `pdf` — Operaciones sobre PDF con iText: generación, campos de firma y firma digital.
- `validation` — DSL de validación de negocio: `BusinessMessages`/`BusinessException`, motor de reglas y mensajes.

> Las utilidades de **bajo nivel** (sin estado, `static`) viven en `base.util`, no aquí.