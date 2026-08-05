# Vistas

## Paneles

- **datos-solicitante** (normal) — nombre, apellidos y DNI de la persona solicitante (precargados; siempre en lectura)
- **disconformidad** (normal) — los datos a subsanar indicados por el centro
- **solicitud** (normal) — la exposición y la solicitud
- **adjuntos** (maestro-detalle) — lista los adjuntos de la instancia y abre el formulario de alta/edición de cada uno, que pide: el nombre descriptivo y el fichero
- **visor-instancia** (visor de documento) — muestra embebido el PDF de la instancia generada (documento-instancia)
- **visor-instancia-presentada** (visor de documento) — muestra embebido el resguardo de la instancia sellado por el registro de entrada
- **resolucion** (normal) — el tipo de resolución, la respuesta y los datos a subsanar
- **visor-respuesta** (visor de documento) — muestra embebido el PDF de la respuesta sellada por el registro de salida (documento-respuesta)
- **ayuda-autofirma** (ayuda) — "Para presentar la solicitud debe tener la aplicación AutoFirma instalada y un certificado digital válido"

## Estado: F_ENTRADA_DATOS

### Vista del perfil CREADOR

- **Paneles (en orden):** datos-solicitante (lectura), disconformidad (lectura), solicitud (edición), adjuntos (edición)
- **Botones:**
  - **Siguiente** — dispara TR-001

#### Reglas de UI

- RUI-F_ENTRADA_DATOS-CREADOR-001 — El panel de disconformidad solo se muestra si el centro ha pedido subsanación
  - disparador: continuo
  - condición: los datos a subsanar tienen valor
- RUI-F_ENTRADA_DATOS-CREADOR-002 — Al añadir un adjunto, el nombre descriptivo y el fichero se marcan como obligatorios (espejo de RES-AdjuntoInstancia-001 y 002)
  - disparador: continuo

### Vista genérica

- **Paneles (en orden):** datos-solicitante (lectura), disconformidad (lectura), solicitud (lectura), adjuntos (lectura)

#### Reglas de UI

- RUI-F_ENTRADA_DATOS-GENERAL-001 — El panel de disconformidad solo se muestra si el centro ha pedido subsanación
  - disparador: continuo
  - condición: los datos a subsanar tienen valor

## Estado: F_ENTRADA_PENDIENTE_PRESENTACION_AUTOFIRMA

### Vista del perfil CREADOR

- **Paneles (en orden):** visor-instancia (lectura), ayuda-autofirma
- **Botones:**
  - **Atrás** — dispara TR-002
  - **Firmar con AutoFirma y Presentar la solicitud** — firma la instancia (FIR-instancia-001) y dispara TR-003

### Vista genérica

- **Paneles (en orden):** visor-instancia (lectura)

## Estado: F_SALIDA_PENDIENTE_RESOLUCION

### Vista del perfil RESPONSABLE

- **Paneles (en orden):** datos-solicitante (lectura), solicitud (lectura), adjuntos (lectura), visor-instancia-presentada (lectura), resolucion (edición)
- **Botones:**
  - **Resolver el expediente** — dispara TR-004

#### Reglas de UI

- RUI-F_SALIDA_PENDIENTE_RESOLUCION-RESPONSABLE-001 — La respuesta solo se muestra si el tipo de resolución es RESPONDER
  - disparador: continuo
  - condición: tipo de resolución == RESPONDER
- RUI-F_SALIDA_PENDIENTE_RESOLUCION-RESPONSABLE-002 — Los datos a subsanar solo se muestran si el tipo de resolución es SUBSANAR_DATOS
  - disparador: continuo
  - condición: tipo de resolución == SUBSANAR_DATOS
- RUI-F_SALIDA_PENDIENTE_RESOLUCION-RESPONSABLE-003 — La respuesta se marca como obligatoria al elegir RESPONDER (espejo de VAL-TR-004-002)
  - disparador: al cambiar tipo de resolución
  - condición: tipo de resolución == RESPONDER

### Vista genérica

- **Paneles (en orden):** datos-solicitante (lectura), solicitud (lectura), adjuntos (lectura), visor-instancia-presentada (lectura)

## Estado: F_SALIDA_PENDIENTE_FIRMA_DIRECTOR

### Vista genérica

- **Paneles (en orden):** datos-solicitante (lectura), solicitud (lectura), resolucion (lectura), visor-instancia-presentada (lectura)
- **Mensaje de ayuda (opcional):** "El expediente está pendiente de la firma del director"

## Estado: F_TERMINADO_RESPONDIDO

### Vista genérica

- **Paneles (en orden):** datos-solicitante (lectura), solicitud (lectura), adjuntos (lectura), visor-respuesta (lectura)
