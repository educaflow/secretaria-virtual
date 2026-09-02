---
type: implementation-task
---

# Tarea 10 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-datainit
- k-sistemas
- k-secure-coding
- k-code-quality

## Fila de la tabla «Ficheros a crear o modificar» del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/secretariavirtual/datademo/TareaFirmaDemoLoader.java` | Crear | k-datainit | Callback `call=` del data-import de demo: crea los `DocumentoFirma` con el PDF de ejemplo |

**Alcance de esta tarea:** solo la clase `com.educaflow.secretariavirtual.datademo.TareaFirmaDemoLoader`.
El `TareaFirmaDemoNotifier` lo materializa la Tarea 09 (ya está en el árbol); aquí se copia el bloque entero
**verbatim** porque el diseño describe las dos clases juntas, y porque el `Loader` fija
`fqcnFirmaNotifier` con el nombre del `Notifier`.

## Texto del diseño (verbatim)

### Paso 10 — Datos de demo: las ocho tareas de firma precargadas

**Ficheros:**
`src/main/java/com/educaflow/secretariavirtual/datademo/TareaFirmaDemoNotifier.java` (Crear)
`src/main/java/com/educaflow/secretariavirtual/datademo/TareaFirmaDemoLoader.java` (Crear)
`src/main/resources/data-demo/input/firmas-demo.xml` (Crear)
`src/main/resources/data-demo/input-config.xml` (Modificar)

Son **datos de demo**, no datos iniciales: van en `src/main/resources/data-demo/` junto a `usuarios-demo.xml`
(lo pide `design-guidelines.md`) y por tanto solo se cargan con `data.import.demo-data = true`.
**MUST NOT** ponerlos en la `data-init` del subsistema.

#### 10.3 Las dos clases Java

```java
// Clase: com.educaflow.secretariavirtual.datademo.TareaFirmaDemoNotifier
// implements com.educaflow.subsystem.firmas.service.TareaFirmaNotifier
public void notify(TareaFirma tareaFirma, Object callBackData);
//   Notificador sin efectos para las tareas de firma de demo: no hay ningún proceso que avisar.
//   Cuerpo: no hace nada (a lo sumo una traza a nivel debug con el id de la tarea; NUNCA con datos sensibles).
//   Existe porque fireActionRule_NotificarFirmaResuelta hace Class.forName(fqcnFirmaNotifier): una tarea de
//   demo sin notificador rompería al firmarla o al rechazarla.

// Clase: com.educaflow.secretariavirtual.datademo.TareaFirmaDemoLoader
public Object crearDocumentos(Object bean, Map values);
//   Callback `call=` del data-import de demo (firma obligatoria de dos parámetros).
//   Secuencia:
//     1. Castea el bean a TareaFirma. Si ya tiene documentos (recarga sobre una tarea existente), lo devuelve
//        tal cual sin tocar nada: la carga es idempotente.
//     2. Fija fqcnFirmaNotifier con el nombre de TareaFirmaDemoNotifier.
//     3. Lee UNA sola vez del classpath los bytes de `data-demo/input/documento_ejemplo_firma.pdf` (Paso 1)
//        con TareaFirmaDemoLoader.class.getClassLoader().getResourceAsStream("data-demo/input/documento_ejemplo_firma.pdf")
//        (sin barra inicial, porque se pide al ClassLoader), en try-with-resources y envolviendo la IOException
//        en RuntimeException (es una guarda de código, no una validación del usuario). Si el recurso no existe,
//        getResourceAsStream devuelve null: MUST fallar con un RuntimeException explícito y no con un NPE opaco.
//        Después, tantas veces como diga el alias `numeroDocumentos` (índice i = 1..numeroDocumentos), crea la
//        copia de ese documento así:
//          a) String fileName = "documento_ejemplo_firma_" + i + ".pdf";
//             MUST ser distinguible por documento dentro de la misma tarea, porque el grid
//             subsysFirmas.Pendiente@TareaFirma.DocumentoFirma-grid muestra y ordena por
//             `documentoOriginal.fileName`, y los pasos 14-15 de ESC-003 («entra en el primer/segundo documento
//             del listado») necesitan dos filas distinguibles y con orden estable en «Firma de prueba 3»
//             (numeroDocumentos = 2).
//          b) DocumentoPdf documentoPdf = DocumentoPdfFactory.getDocumentoPdf(bytes, fileName);
//             (com.educaflow.base.infrastructure.pdf.DocumentoPdfFactory)
//          c) MetaFile metaFile = MetaFileHelper.createMetaFile(documentoPdf);
//             (com.educaflow.base.infrastructure.metafile.MetaFileHelper) — es el helper que usa el resto del
//             proyecto y el ÚNICO que deja el MetaFile con `fileName` y `fileType = "application/pdf"` puestos
//             (por dentro ya hace createMetaFileInstance + setFileName + setFileType + uploadContent).
//             CRITICAL — MUST NOT construir el MetaFile con MetaFileUtil.createMetaFileInstance() +
//             MetaFileUtil.uploadContent(...) «a secas»: `com.educaflow.base.util.MetaFileUtil` NO tiene ningún
//             método que rellene fileName/fileType (createMetaFileInstance() devuelve un MetaFile vacío y
//             uploadContent(metaFile, bytes) solo sube el contenido), así que el MetaFile quedaría con
//             fileType == null y fileName == null. Consecuencia real: la FASE 1 de
//             fireActionRule_FirmarDocumentosEnServidor (rules/R-TareaFirma-001.md) llama a
//             MetaFileHelper.getDocumentoPdf(documentoFirma.getDocumentoOriginal()), que en isPdf lanza
//             RuntimeException("El MetaFile no tiene fileType definido") → la firma en servidor de TODAS las
//             tareas de demo fallaría siempre con «No se han podido firmar los documentos: …» y ESC-001/002/003
//             (T-001/T-002/T-003) no podrían pasar; y con fileName == null el grid citado en (a) pintaría filas
//             sin texto. Si por lo que sea se usara MetaFileUtil directamente, MUST hacerse antes
//             metaFile.setFileName(fileName) y metaFile.setFileType(MetaFileHelper.PDF_MIME_TYPE), y quedarse
//             con el MetaFile que DEVUELVE uploadContent (no con la instancia pasada).
//          d) Un DocumentoFirma con ese MetaFile como documentoOriginal, documentoFirmado a null y la tarea como
//             padre. Cada DocumentoFirma tiene su PROPIO MetaFile (su propia copia física del PDF), para que
//             firmar uno no afecte al otro.
//     4. Asigna la lista a la tarea y devuelve el bean.
//   MUST NOT llamar a TareaFirmaService.insert(...): el data-import ya persiste el bean que devuelve este
//   método, y hacerlo crearía la tarea dos veces.
```

**Por qué en `secretariavirtual.datademo`.** El conjunto de datos de demo es global (`src/main/resources/data-demo`,
con los centros y los usuarios de todos los subsistemas), así que su código de apoyo pertenece al ensamblaje,
que es la capa que puede depender de cualquier subsistema y de la que no depende nadie (reglas C3/C4/C5 de
`architecture-rules.md`). Meterlo en `subsystem/firmas` obligaría a inventar una carpeta que la estructura
canónica de un subsistema no contempla.

**Verificación:** con `data.import.demo-data = true` y la BD recreada, arrancar y comprobar en `psql`:
```sql
SELECT t.motivo_firma, u.code, count(d.id)
FROM firmas_tarea_firma t
JOIN auth_user u ON u.id = t.firmante
LEFT JOIN firmas_documento_firma d ON d.tarea_firma = t.id
GROUP BY t.motivo_firma, u.code ORDER BY 1;
```
Ocho filas; «Firma de prueba 3» con 2 documentos y el resto con 1.
