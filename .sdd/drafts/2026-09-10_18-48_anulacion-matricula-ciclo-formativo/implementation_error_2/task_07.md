---
type: implementation-task
template: expediente
---

# Tarea 07 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-tipo-expediente
- k-i18n

## Qué hay que hacer

Materializar el `views.xml` de la **raíz de la versión**: el form plantilla `exp-AnulacionMatriculaCicloFormativoV1-Templates`, que es el **almacén de paneles** del que tiran los `views.xml` de las tres fases.

**Origen:** `design/views.xml`
**Destino:** `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/views.xml`

**Cópialo literalmente**, **sobrescribiendo** el esqueleto que dejó `CreateFilesTask`. **MUST NOT** modificarlo, reescribirlo ni regenerarlo.

Va **antes** que los `views.xml` de fase: esos solo pueden incluir paneles que existan aquí.

## Fila de la tabla `## 6. Ficheros a crear o modificar` del diseño (verbatim)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/views.xml` | Crear | `k-tipo-expediente` | Copia de `design/views.xml` |

## Paso del diseño, con su resumen estructural de paneles (verbatim)

### Paso 7 — `views.xml` de la raíz de la versión

El fichero está materializado en `design/views.xml`. **Cópialo literalmente** a `…/v1/views.xml`, **sobrescribiendo** el esqueleto. **MUST NOT** modificarlo, reescribirlo ni regenerarlo.

**Resumen estructural** — un único form plantilla `exp-AnulacionMatriculaCicloFormativoV1-Templates` (`model` = la entidad, `width="large"`, `groups="admins,users"`) con 19 paneles. ASCII Layout de los paneles no triviales (rejilla de 12 columnas, cada fila suma 12):

```
datos-alumno
aaaaabbbbccc   ← apellidos(5) + nombre(4) + NIA(3)
dddeeeeeefff   ← DNI/NIE(3) + dirección(6) + teléfono(3)
gggggHHHHiii   ← población(5) + provincia(4) + código postal(3)

datos-alumno-resumen          (el director solo necesita identificar al alumno)
aaaabbbccddd   ← apellidos(4) + nombre(3) + NIA(2) + DNI/NIE(3)

matricula
aaabbbbbbbbb   ← curso académico(3) + centro(9)
cccccccccddd   ← ciclo(9) + grado o nivel(3)

matricula-director            (añade la fecha de presentación y su indicación)
  · con sentidoRevision == ACEPTAR
aaabbbbbbbbb   ← curso académico(3) + centro(9)
cccccccccddd   ← ciclo(9) + grado o nivel(3)
eeeeffffffff   ← fecha y hora de presentación(4) + help «Si firma…»(8, showIf ACEPTAR)
  · con sentidoRevision != ACEPTAR (en la práctica, RECHAZAR)
aaabbbbbbbbb   ← curso académico(3) + centro(9)
cccccccccddd   ← ciclo(9) + grado o nivel(3)
eeee........   ← fecha y hora de presentación(4); el help desaparece y la fila queda en 4/12

presentacion
aaaa........   ← fecha y hora de presentación(4); el resto de la fila queda libre porque el visor ocupa 12
bbbbbbbbbbbb   ← visor del justificante de presentación (iframe)

presentacion-descarga
aaaabbbbbbbb   ← fecha y hora de presentación(4) + justificante como descarga(8)

presentacion-descarga-aceptada   (gemelo del anterior solo para ACEPTADA: la indicación de
                                  RUI-ACEPTADA-GENERICA-002 acompaña a la fecha, no al aviso del pie)
aaaabbbbbbbb   ← fecha y hora de presentación(4) + help «La anulación… desde esta fecha»(8)
cccccccccccc   ← justificante como descarga(12)

revision
  · con sentidoRevision == RECHAZAR
aaaaaaaa....   ← sentido de la revisión(8, SwitchSelect); nada más cabe en su fila: los dos textos de abajo son de 12
bbbbbbbbbbbb   ← motivo del rechazo(12, showIf RECHAZAR)
  · con sentidoRevision == SUBSANAR
aaaaaaaa....   ← sentido de la revisión(8)
cccccccccccc   ← qué hay que subsanar(12, showIf SUBSANAR)
  · con sentidoRevision == ACEPTAR o sin elegir (estado inicial de la pantalla)
aaaaaaaa....   ← sentido de la revisión(8); el panel queda en una sola fila

devolucion-view
aaaaaaaaaaaa   ← motivo de la devolución(12)
bbbbcccccccc   ← fecha de la devolución(4) + devuelto por(8)

decision-secretaria
  · con sentidoRevision == RECHAZAR
aaaaabbbcccc   ← sentido de la revisión(5) + fecha de la revisión(3) + revisado por(4)
dddddddddddd   ← motivo del rechazo(12, showIf RECHAZAR)
  · con sentidoRevision != RECHAZAR
aaaaabbbcccc   ← sentido de la revisión(5) + fecha de la revisión(3) + revisado por(4)
               (la fila del motivo desaparece entera; la que queda sigue sumando 12)

resolucion-firmada
  · con sentidoRevision == RECHAZAR (estado RECHAZADA)
aaaaaaaaaaaa   ← motivo del rechazo(12, showIf RECHAZAR)
bbbbcccccccc   ← fecha de la resolución(4) + firmado por(8)
dddddddddddd   ← visor de la resolución firmada (iframe)
  · con sentidoRevision != RECHAZAR (estado ACEPTADA)
bbbbcccccccc   ← fecha de la resolución(4) + firmado por(8)
dddddddddddd   ← visor de la resolución firmada (iframe)
               (la fila del motivo desaparece entera; las que quedan siguen sumando 12)
```

**Las dos filas que quedan por debajo de 12 al no cumplirse su condición, y por qué se quedan así.**

- `matricula-director`, tercera fila con `sentidoRevision != ACEPTAR`: quedan los 4 de `fechaHoraPresentacion` y 8 libres. **No se remaqueta.** Ensanchar el campo a 12 exigiría cambiar su `colSpan` según el sentido —es decir, un `<action-attrs>` más y **una decisión de layout con dos dueños** (el XML y una acción)— para ganar solo que una fecha ocupe el ancho de la pantalla, que además la leería peor: la tabla de proporcionalidad de `k-vistas/forms.md` da 4 a una fecha con hora, no 12. El hueco a la derecha de una fecha es el mismo que ya tienen `presentacion-descarga` (4 + 8) y `devolucion-view` (4 + 8) cuando el segundo campo está vacío.
- `revision`, primera fila: `sentidoRevision` ocupa 8 **en todos los estados**, no solo cuando la condición no se cumple. Es un `SwitchSelect` con tres opciones de texto largo («Aceptar la anulación», «Rechazar la anulación», «Pedir subsanación»), que necesita ese ancho para no partir las etiquetas; y los dos campos que podrían acompañarlo son textos largos de 12 que van en su propia fila. Subirlo a 12 estiraría los botones sin ganar nada. Es el hueco justificado que `vistas.md` §2.7 permite, no un campo corto abandonado a mitad de fila.

Los paneles cuyo `showIf` está en el **propio panel** (`subsanacion` y `devolucion-view`) tienen solo dos estados —el panel entero se ve con el dibujo de arriba, o no se ve—, así que un único dibujo los describe por completo.

Los ocho paneles restantes son triviales: `subsanacion` (un texto a 12), los cinco visores/descargas de documentos (`solicitud-visor`, `solicitud-descarga`, `solicitud-firmada-visor`, `solicitud-firmada-descarga`, `justificante-descarga`), `resolucion-a-firmar` (un `<help>` a 12 + el visor a 12) y `devolucion` (un texto a 12).

`resolucion-firmada` sirve a los **dos** estados cerrados: el motivo del rechazo aparece o no según `sentidoRevision`, así que no hace falta un gemelo por estado.

**Verificación:** `diff` vacío; hay exactamente un `<form name="exp-AnulacionMatriculaCicloFormativoV1-Templates">`; no hay ningún `<form state=…>` en este fichero; todos los paneles tienen `name` y son hijos directos del form plantilla.
