---
type: implementation-task
template: expediente
---

# Tarea 13 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-tipo-expediente

## Qué hay que hacer

1. **Mueve** con `git mv` el PDF escaneado del impreso oficial:
   `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/documentospdf/modelo.pdf`
   → `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/documentospdf/originales/modelo.pdf`
   Motivo: todo `.pdf` que cuelgue directamente de `documentospdf/` genera una constante en el enum `TipoDocumentoPdf`, y un modelo escaneado no es un documento del trámite. La subcarpeta `originales/` no se escanea.
2. **Copia literalmente** los dos XML ya materializados por el diseño, **sobrescribiendo** lo que haya:
   - `design/documentospdf/solicitud.xml` → `…/v1/documentospdf/solicitud.xml`
   - `design/documentospdf/resolucion.xml` → `…/v1/documentospdf/resolucion.xml`
   Son **contrato fijo**: **MUST NOT** modificarse, reescribirse ni regenerarse.
3. **MUST NOT** crearse ningún fragmento `_*.xml`: los dos documentos no comparten ningún trozo.

## Filas de la tabla «## 6. Ficheros a crear o modificar» del diseño (verbatim)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/documentospdf/solicitud.xml` | Crear | `k-tipo-expediente` | Copia de `design/documentospdf/solicitud.xml` |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/documentospdf/resolucion.xml` | Crear | `k-tipo-expediente` | Copia de `design/documentospdf/resolucion.xml` |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/documentospdf/modelo.pdf` | Modificar | `k-tipo-expediente` | **Moverlo** (`git mv`) a `…/v1/documentospdf/originales/modelo.pdf`. Motivo: todo `.pdf` directo en `documentospdf/` genera una constante en `TipoDocumentoPdf`, y el modelo escaneado del ANEXO VII no es un documento del trámite |

## `### Paso 14 — documentospdf/` del diseño (verbatim)

### Paso 14 — `documentospdf/`

1. **Mueve** `…/v1/documentospdf/modelo.pdf` a `…/v1/documentospdf/originales/modelo.pdf` (`git mv`), para que el escaneo del enum `TipoDocumentoPdf` no lo tome por un documento del trámite.
2. Los dos XML están materializados en `design/documentospdf/`. **Cópialos literalmente** a `…/v1/documentospdf/` (`solicitud.xml`, `resolucion.xml`). **MUST NOT** modificarlos, reescribirlos ni regenerarlos. **MUST NOT** crearse ningún fragmento `_*.xml`: los dos documentos no comparten ningún trozo (§5).

**Verificación:** en `documentospdf/` solo hay dos `.xml` (`solicitud.xml` y `resolucion.xml`, ningún `_*.xml`) y la subcarpeta `originales/`; el build genera `solicitud.pdf` y `resolucion.pdf`; el `<extra-code-model>` que reescribe el build sigue teniendo exactamente las dos constantes `SOLICITUD` y `RESOLUCION`.


## `## 5. Documentos PDF` del diseño, completa (verbatim)

## 5. Documentos PDF

| fichero | constante | en qué transición se genera | en qué campo se guarda | quién lo firma | se registra |
|---|---|---|---|---|---|
| `documentospdf/solicitud.xml` | `SOLICITUD` | `SOLICITUD.DATOS_SOLICITUD` + `CONTINUAR` | `pdfSolicitud` (`servidor`) | `cliente (AutoFirma__!!)` o, si el alumno tiene certificado custodiado, `servidor: DNI SecurityUtil.getUser().getDni()` | `entrada` (al presentar, sobre `pdfSolicitudFirmada`) |
| `documentospdf/resolucion.xml` | `RESOLUCION` | `REVISION.PENDIENTE_REVISION` + `ENVIAR_A_FIRMA`, y de nuevo en `RESOLUCION.PENDIENTE_FIRMA_DIRECTOR` + `FIRMAR` | `pdfResolucion` (`servidor`); la firmada, en `pdfResolucionFirmada` (`servidor`) | `servidor: DIRECTOR` | `salida` (al firmar) |

Fragmentos: **ninguno**, y los tres trozos que `documentos.md` declara comunes se resuelven así:

- **Cabecera institucional** — no es contenido del documento: la pinta el generador (logo + `<titulo>`), no hay nada que compartir (§14 nota 17).
- **Identificación del alumno** — los dos documentos abren con una sección de identificación, pero **no imprimen lo mismo**: la solicitud imprime el **bloque A completo del impreso oficial** (apellidos, nombre, NIA, DNI/NIE, dirección, teléfono, población, provincia y CP) y la resolución solo los cuatro datos del destinatario (apellidos, nombre, DNI/NIE y NIA) que su ficha declara. Compartir obligaría a imprimir de más en la resolución o a partir el bloque A en dos (§14 nota 4).
- **Referencia de la matrícula** — aquí sí hay **una línea duplicada literalmente**, pero **no es factorizable con el mecanismo disponible**: un `<include>` solo va como hijo directo de `<documento>` y esa línea es un `<texto>` **dentro** de una sección distinta en cada documento («Expone» / «Vistos»). Se asume la duplicación y **MUST** tocarse en los dos ficheros a la vez (§14 nota 4).

Cada documento declara por tanto su propia `<seccion>` y **MUST NOT** existir ningún fichero con prefijo `_` en `documentospdf/`.

Notas:

- El PDF escaneado del impreso oficial que hoy está en `documentospdf/modelo.pdf` **MUST moverse** a `documentospdf/originales/modelo.pdf`: todo `.pdf` que cuelgue directamente de `documentospdf/` genera una constante en el enum `TipoDocumentoPdf`, y un modelo visual no es un documento del trámite. La subcarpeta `originales/` no se escanea (precedente: `justificacion_falta_profesorado`).
- Recuadros de firma (las constantes viven en el `PhaseEventManagerImpl` de su fase, §9):
  - solicitud: `Rectangulo(320, 60, 250, 70)`, página `1` — **los mismos ocho argumentos** que la `<action-method>` de la vista de la fase `SOLICITUD`.
  - resolución: `Rectangulo(150, 220, 300, 60)`, página `1`.
- Las expresiones Groovy de los documentos **fallan en silencio** (log + campo vacío): el paso final de verificación exige abrir los dos PDF generados en runtime y comprobar que ningún hueco queda en blanco.

