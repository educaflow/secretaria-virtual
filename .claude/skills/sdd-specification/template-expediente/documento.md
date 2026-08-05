# Documento: <Nombre>

<!-- Un fichero por documento PDF. El documento es la fuente de verdad de QUÉ es y QUIÉN lo firma; CUÁNDO se genera, registra o envía lo dicen las transiciones (RN- en estados.md) y aquí se referencia. -->

## Identidad

- **Qué es:** <qué representa el documento y para qué sirve>
- **Procedencia:** impreso oficial — <referencia o URL de donde se obtiene el PDF, que se usa tal cual> | propio
- **Ciclo:** se genera en <RN-TR-NNN-NNN>; se guarda en <el campo del expediente> (versiones <original / firmada / sellada>); registro de <entrada — lo presenta el usuario | salida — lo emite el centro | ninguno> en <RN-…>; se envía por correo a <quién> en <RN-…>

## Contenido

<!-- Si procedencia = impreso oficial: NO se especifican secciones — se especifica el MAPEO (qué dato del expediente rellena cada hueco del impreso); borra el bloque de secciones. Si procedencia = propio: describe las secciones en orden; borra el bloque de mapeo. Textos en castellano (el valenciano se genera automáticamente; solo se anota el término que no deba traducirse). Sin maquetación: eso es del diseño. El título, si no se dice otra cosa, es el nombre del trámite. -->

**Mapeo (impreso oficial):**

| Hueco del impreso | Dato del expediente |
|---|---|
| <Apellidos> | <los apellidos de la persona interesada> |
| <Circunstancia (casillas)> | <la circunstancia alegada> |

**Secciones (documento propio):**

### <Sección>

- <campo o texto: qué dato del expediente vuelca, o qué texto fijo lleva>

## Firmas

<!-- Una ficha FIR- por cada firma que recibe el documento, EN EL ORDEN en que se firman. La relación decide DÓNDE se firma (el algoritmo completo: catalogos/catalogo-firmas.md): tiene el turno → botón de firma en su vista; parte del expediente sin el turno → un estado propio cuyo perfil con el turno es el firmante; ajeno al expediente → portafirmas, con estado de espera y transición automática al completarse. El mecanismo solo se declara cuando se firma en pantalla (en el portafirmas lo decide el propio subsistema). Si el documento no se firma, sustituye la lista por *(sin firmas)*. -->

- FIR-<slug>-NNN — <quién firma>
  - relación: tiene el turno (estado <ESTADO>) | parte del expediente, sin el turno (estado <ESTADO>) | ajeno al expediente
  - mecanismo: AutoFirma | en el servidor
  - dónde: botón «<Etiqueta>» de TR-NNN | portafirmas, al entrar en <ESTADO> (RN-<ESTADO>-NNN); el expediente avanza con TR-NNN (automática)
