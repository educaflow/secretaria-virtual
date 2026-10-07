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


Se crean los alias:
CertFirmaDigitalDirector
CertFirmaDigitalSecretario
