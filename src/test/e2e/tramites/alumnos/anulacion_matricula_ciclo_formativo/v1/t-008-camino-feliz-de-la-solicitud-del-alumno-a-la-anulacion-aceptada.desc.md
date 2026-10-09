---
type: test-e2e
id: T-008
---

<!-- Escrito a mano (no viene del pipeline SDD). -->

# T-008 — Camino feliz: de la solicitud del alumno a la anulación aceptada

**Origen ESC:** —
**Perfil:** `CREADOR` (login `alumno1@mislata.es`) presenta; `TRAMITADOR` (login `administrativo1@mislata.es`) verifica y resuelve; el secretario (`secretario@mislata.es`) y el director (`director@mislata.es`) firman la resolución en su bandeja de firmas
**Desde:** `ENTRADA` / `ENTRADA_DATOS`
**Evento:** `GUARDAR_DATOS` → `PRESENTAR` → `VERIFICAR` → `RESOLVER` → `FIRMAR` (secretario) → `FIRMAR` (director)
**Hasta:** `CIERRE` / `ACEPTADA`
**Tipo:** happy
**Manual:** no

## Estado inicial de la base de datos

### Actores

| Login | Contraseña | Tipo / Cargo | Centro | Papel en el test |
|---|---|---|---|---|
| `alumno1@mislata.es` | `demo1234` | tipo de usuario `ALUMNO` | CIPFP Mislata | `CREADOR`: rellena y presenta la solicitud |
| `administrativo1@mislata.es` | `demo1234` | tipo de usuario `ADMINISTRATIVO` | CIPFP Mislata | `TRAMITADOR`: verifica y resuelve |
| `secretario@mislata.es` | `demo1234` | cargo `SECRETARIO` | CIPFP Mislata | firma la resolución en primer lugar |
| `director@mislata.es` | `demo1234` | cargo `DIRECTOR` | CIPFP Mislata | firma la resolución en segundo lugar |

### Datos de demo

La carga de demo (`data.import.demo-data = true`).
El servidor tiene instalados, con su contraseña, los certificados del alumno, del secretario y del director, así que la solicitud y las dos firmas de la resolución las hace el servidor, sin AutoFirma.

#### Juego de datos

| campo | valor |
|---|---|
| «NIA» | 12345678 |
| «Teléfono» | 612345678 |
| «Dirección» | Calle Mayor, 1 |
| «Municipio» | Mislata |
| «Código Postal» | 46920 |
| «Ciclo formativo» | Desarrollo de Aplicaciones Web |
| «Resultado de la verificación» | La solicitud es correcta |
| «Tipo resolución» | Aceptar la anulación |

## Pasos

- **Given** `alumno1@mislata.es` ha iniciado sesión.
- **When** crea un expediente de «Anulación de matrícula en ciclo formativo» desde «Mis trámites» → «Nuevo trámite», rellena el juego de datos, pulsa «Siguiente» y luego «Firmar y presentar la solicitud» y confirma.
- **Then** el expediente queda en `VERIFICACION` / `PENDIENTE_VERIFICACION`.
- **When** `administrativo1@mislata.es` lo abre por «Tramitación» → «Pendientes de mí», elige «La solicitud es correcta» y pulsa «Siguiente».
- **Then** el expediente queda en `RESOLUCION` / `PENDIENTE_RESOLUCION`.
- **When** elige «Aceptar la anulación», pulsa «Enviar a la firma» y confirma el aviso.
- **Then** el expediente queda en `FIRMA` / `PENDIENTE_FIRMA_SECRETARIO`.
- **When** `secretario@mislata.es` abre en «Firmas» → «Pendientes» la tarea de firma de la resolución del expediente, pulsa «Firmar todos los documentos» y luego «Firmar todos los documentos y finalizar».
- **Then** la tarea sale de sus pendientes y el expediente pasa a `FIRMA` / `PENDIENTE_FIRMA_DIRECTOR`.
- **When** `director@mislata.es` hace lo mismo en su bandeja de firmas.
- **Then** el expediente pasa a `CIERRE` / `ACEPTADA`.
- **And** en «Registro» → «Salida», filtrando la columna «Asunto» por el número del expediente, hay exactamente un registro, sin anexos, y al abrirlo muestra una «URL de descarga» `…/ws/public/registro-salida/download?CSV=<csv>` cuyo `<csv>` es el valor de su campo «Código seguro de verificación».
- **And** sin sesión, esa URL responde 200 con `Cache-Control: no-store` y el PDF de la resolución, idéntico byte a byte al que descarga el botón del campo «Documento de salida».
- **And** `alumno1@mislata.es` lo encuentra en «Mis trámites» → «Finalizados» con la fase «Cierre», el estado «Anulación aceptada» y la resolución firmada visible.
