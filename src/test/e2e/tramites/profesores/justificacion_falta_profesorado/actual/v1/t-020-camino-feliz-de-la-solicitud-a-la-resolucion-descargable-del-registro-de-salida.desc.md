---
type: test-e2e
id: T-020
---

<!-- Escrito a mano (no viene del pipeline SDD). -->

# T-020 — Camino feliz: de la solicitud del profesor a la resolución descargable del registro de salida

**Origen ESC:** —
**Perfil:** `CREADOR` (login `profesor1@mislata.es`) presenta; `TRAMITADOR` (login `jefeestudios1@mislata.es`) verifica; `DIRECTOR` (login `director@mislata.es`) resuelve
**Desde:** `ENTRADA` / `ENTRADA_DATOS`
**Evento:** `GUARDAR_DATOS` → `PRESENTAR` → `VERIFICAR` → `RESOLVER`
**Hasta:** `RESOLUCION` / `ACEPTADO`, con la resolución en el registro de salida
**Tipo:** happy

## Estado inicial de la base de datos

### Actores

| Login | Contraseña | Tipo / Cargo | Centro | Perfil |
|---|---|---|---|---|
| `profesor1@mislata.es` | `demo1234` | tipo de usuario `PROFESOR`, sin cargo | CIPFP Mislata | `CREADOR` |
| `jefeestudios1@mislata.es` | `demo1234` | tipo de usuario `PROFESOR`, cargo `JEFE_ESTUDIOS` | CIPFP Mislata | `TRAMITADOR` |
| `director@mislata.es` | `demo1234` | tipo de usuario `PROFESOR`, cargo `DIRECTOR` | CIPFP Mislata | `DIRECTOR` |

### Datos de demo

La carga de demo (`data.import.demo-data = true`).
El servidor tiene instalados los certificados de los usuarios, así que la solicitud, la resolución y el registro de salida los firma el servidor, sin AutoFirma.

#### Juego de datos

| campo | valor |
|---|---|
| «Tipo de jornada faltada» | Un día completo |
| «Fecha» | hace 2 días |
| «Motivo falta» | Traslado de domicilio |
| «Foto o PDF del justificante» | `justificante.pdf` (PDF pequeño) |
| «Resultado de la verificación» | La solicitud es correcta |
| «Tipo resolución» | Resolver positivamente |

## Pasos

- **Given** `profesor1@mislata.es` ha iniciado sesión.
- **When** crea un expediente de «Justificación de falta del profesorado» desde «Mis trámites» → «Nuevo trámite», rellena el juego de datos, pulsa «Siguiente» y luego «Firmar y Presentar la solicitud» y confirma.
- **Then** el expediente queda en `VERIFICACION` / `PENDIENTE_VERIFICACION`.
- **When** `jefeestudios1@mislata.es` lo abre por «Tramitación» → «Pendientes de mí», elige «La solicitud es correcta» y pulsa «Siguiente».
- **Then** el expediente queda en `RESOLUCION` / `PENDIENTE_RESOLUCION`.
- **When** `director@mislata.es` lo abre por «Tramitación» → «Pendientes de mí», elige «Resolver positivamente», pulsa «Resolver el expediente» y confirma.
- **Then** el expediente queda en `RESOLUCION` / `ACEPTADO`.
- **And** en «Registro» → «Salida», filtrando la columna «Asunto» por el número del expediente, hay exactamente un registro, y al abrirlo muestra una «URL de descarga» `…/ws/public/registro-salida/download?CSV=<csv>` cuyo `<csv>` es el valor de su campo «Código seguro de verificación» y un único anexo (el justificante).
- **And** sin sesión, esa URL responde 200 con `Cache-Control: no-store` y un ZIP cuyos ficheros son el documento de salida (idéntico byte a byte al que descarga el botón del campo «Documento de salida») y el anexo.
