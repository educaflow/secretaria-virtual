Esta carpeta configura un dispositivo criptografico para cargar 2 certificados.
Los certificados son los de demo del director y del secretario de CIPFP Mislata (director@mislata.es y
secretario@mislata.es), emitidos por la CA FALSA de demo: salen de ../demo/certificados/<DNI>.p12
(ver ../demo/README.txt) y su contraseña es demo1234.
install.sh saca el DNI de cada uno de data-demo/input/usuarios-demo.xml buscando por su email.
Pero los scripts se pueden aplicar a certificados reales.

Para configurarlo todo ejecutar
"./install.sh"

inicializar_dispositivo.sh : Borra el discopositivo y lo deja listo para meter los certificados
load_certificate.sh: Carga el certificado en el dispositivo
  Uso: ./load_certificate.sh ID SUFIJO_ALIAS PIN CERTIFICADO [PASSWORD_P12]
    ID            Identificador unico del certificado en el dispositivo (empezar en 01)
    SUFIJO_ALIAS  Nombre de la persona; se concatena a los alias KeyFirmaDigital<sufijo> y CertFirmaDigital<sufijo>
    PIN           PIN del dispositivo que se uso al inicializarlo
    CERTIFICADO   Ruta del archivo .p12
    PASSWORD_P12  Contrasena del .p12 (opcional; si no se indica se usa demo1234, la de los certificados de demo)
  Para un certificado real con otra contrasena hay que pasarla como quinto parametro.


Se crean los alias:
CertFirmaDigitalDirector
CertFirmaDigitalSecretario

Ver lo que hay en el dispositivo: "pkcs15-tool -D" (o --list-keys / --list-certificates).
Si load_certificate.sh falla deja la traza completa de pkcs15-init en <CERTIFICADO>.pkcs15-init.log.

PROBLEMA CONOCIDO con el ePass2003 y OpenSC (hasta la 0.25 al menos) y motivo del parche
----------------------------------------------------------------------------------------
Sintoma: al cargar ALGUNOS certificados load_certificate.sh fallaba con
  "Failed to store private key: Incorrect parameters in APDU"
y en la traza de pkcs15-init (-vvv) salia
  card-epass2003.c internal_write_rsa_key_factor: Write rsa key factor failed: -1205 (Incorrect parameters in APDU)
  card-epass2003.c internal_write_rsa_key: write d failed ...   (el token responde SW 6A80 = datos incorrectos)
Otros certificados entraban sin problema, lo que hacia pensar que fallaba "el segundo"; no era eso: fallaba una
clave concreta, independientemente del orden. Ademas el script antiguo no se paraba en el error y guardaba igualmente
el certificado, con lo que el token quedaba con un certificado sin clave privada (inutil para firmar).

Causa: una clave RSA se compone de varios numeros enormes; uno de ellos es el exponente privado d. El driver
epass2003 de OpenSC escribe la clave en el token en dos envios: primero el modulo n y luego d, y manda d tal cual,
sin rellenar. OpenSSL (que es quien lee el .p12) guarda los numeros sin ceros a la izquierda, igual que escribimos
42 y no 042. Como d se calcula "al azar", en una de cada ~128 claves su primer byte es cero, OpenSSL lo quita y
d queda de 255 bytes en vez de los 256 del modulo. El firmware del ePass2003 exige que d mida exactamente lo que
el modulo y rechaza el envio. Es un fallo del driver, no del token ni del certificado: la clave es perfectamente
valida (en el PC firma sin problema) y el mismo certificado se carga en otros tokens.
Se puede comprobar con: openssl rsa -in clave.pem -noout -text (comparar los bytes de "modulus" y "privateExponent").

Por que no esta arreglado en el OpenSC de Ubuntu (comprobado el 2026-10-12): porque no esta arreglado en NINGUNA version
de OpenSC. El codigo de internal_write_rsa_key en card-epass2003.c es identico en la 0.25.0 (la de Ubuntu 24.04), en la
0.27.1 (la ultima publicada) y en la rama master: manda rsa->d sin rellenar. Tampoco hay ninguna issue en el proyecto
que describa este fallo ("write d failed" no aparece en su buscador de issues); los commits recientes del driver son de
desbordamientos de buffer y del Secure Messaging, no de la importacion de claves. Es decir, no es que Ubuntu vaya
atrasado, es que nadie lo ha reportado ni corregido arriba. El motivo probable de que pase desapercibido: la
documentacion del ePass2003 recomienda GENERAR la clave en el token (--generate-key), con lo que d no viaja nunca por
este camino; importar un .p12 es menos habitual, y encima solo falla en una de cada ~128 claves, asi que quien lo
sufre suele pensar que es su clave o su token, no el driver. (Seria bueno abrir una issue en OpenSC adjuntando
epass2003-pad-d.patch.)

Referencias:
  - Codigo del driver con el fallo (funciones internal_write_rsa_key e internal_write_rsa_key_factor):
      https://github.com/OpenSC/OpenSC/blob/0.25.0/src/libopensc/card-epass2003.c
      https://github.com/OpenSC/OpenSC/blob/0.27.1/src/libopensc/card-epass2003.c   (sigue igual)
  - Wiki de OpenSC sobre el ePass2003 (uso de pkcs15-init, perfil pkcs15+onepin, generacion de claves):
      https://github.com/OpenSC/OpenSC/wiki/Feitian-ePass2003
  - Issues de OpenSC con el mismo mensaje "Incorrect parameters in APDU" en el ePass2003 pero por OTRAS causas
    (es el error generico con el que el driver traduce el SW 6A80/6A86 del token; sirven para no confundirse):
      https://github.com/OpenSC/OpenSC/issues/760   (segundo PIN/ID con --generate-key, OpenSC 0.15)
      https://github.com/OpenSC/OpenSC/issues/1177  (generar clave EC: el ePass2003 no lo soporta)
      https://github.com/OpenSC/OpenSC/issues/2815  (idem, criptografia eliptica)
      https://github.com/OpenSC/OpenSC/issues/2400  (importar .p12 con clave ECDSA: "Not supported")
      https://github.com/OpenSC/OpenSC/issues/1669  (no se puede crear la estructura PKCS#15 en un ePass2003Auto)
  - Issue de otro driver (isoApplet) con el mismo patron, fallo al importar una clave por la longitud de los datos:
      https://github.com/OpenSC/OpenSC/issues/1747

Arreglo: el parche epass2003-pad-d.patch (esta carpeta). En opensc-parcheado/ hay un pkcs15-init de OpenSC 0.25.0
YA COMPILADO con el (bin/pkcs15-init es un wrapper que carga lib/libopensc.so.11 parcheada; compilado en Ubuntu 24.04
x86_64, usa las libpcsclite/libssl/zlib del sistema). load_certificate.sh lo usa automaticamente; con la variable
PKCS15_INIT se puede indicar otro. Solo hay que recompilarlo en otra arquitectura o si OpenSC cambia de version:
  curl -sL https://github.com/OpenSC/OpenSC/releases/download/0.25.0/opensc-0.25.0.tar.gz | tar xz
  cd opensc-0.25.0 && patch -p1 < <esta carpeta>/epass2003-pad-d.patch
  ./configure --prefix=/usr --sysconfdir=/etc --enable-openssl --enable-zlib --disable-doc --disable-man && make -j$(nproc)
  cp src/tools/.libs/pkcs15-init <esta carpeta>/opensc-parcheado/bin/pkcs15-init.bin
  cp src/libopensc/.libs/libopensc.so.11.0.1 <esta carpeta>/opensc-parcheado/lib/
