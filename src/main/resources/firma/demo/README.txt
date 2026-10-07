CA DE DEMO (FALSA)
==================

Esta carpeta contiene una autoridad de certificación FALSA, "ACCV DEMO EducaFlow - NO VALIDA", y los
certificados que emite para los usuarios de demo. Sirve para que los tests (unitarios y E2E) y las pruebas a
mano puedan firmar en el servidor sin abrir AutoFirma.

La CA NO es la ACCV ni tiene nada que ver con ella: su CN contiene "ACCV" solo para que la aplicación
clasifique sus certificados como ACCV y lea de ellos el DNI, el nombre y los apellidos.
Solo es de confianza en los entornos que usan el almacén de demo almacen/truststore-demo.jks
(desarrollo y preproducción); producción usa truststore.jks, que no la contiene. El almacén de demo solo
se admite en un entorno que carga los datos de demo (data.import.demo-data = true): lo comprueba el test
ConfiguracionEntornosTest sobre la configuración de cada entorno.

Todo está versionado a propósito, INCLUIDAS LAS CLAVES PRIVADAS Y LAS CONTRASEÑAS: es material de desarrollo
sin ningún valor fuera de los entornos de demo.

Contenido
---------
ca/ca.key.pem, ca/ca.crt.pem        La CA (clave privada y certificado raíz).
certificados/<DNI>.p12              Un certificado por usuario de usuarios-demo.xml (contraseña demo1234),
                                    con el DNI del usuario como nombre del fichero. Están dados de alta en
                                    CertificadoDigital con data-demo/input/certificados-demo.xml.
                                    Los únicos usuarios de demo sin certificado son sincertificado1@mislata.es,
                                    sincertificado2@mislata.es y autofirma@mislata.es.
almacen/                            El almacén de certificados confiables de demo, que usan desarrollo y
                                    preproducción (no toca nada de ../AlmacenCertificadosConfiables):
                                    truststore-demo.jks (truststore.jks más esta CA), educaflow-demo-ca.crl
                                    (la CRL de esta CA) y crls-demo.xml (la lista de CRLs: las oficiales,
                                    para poder probar con certificados reales, más la de esta CA).
comun_demo.sh                       Funciones comunes de los scripts (no se ejecuta directamente).

Los certificados de test (los que usan los tests que dan de alta filas en CertificadoDigital) también los
emite esta CA, pero están en src/test/resources/firma/test/ (ver su README.txt): no son de ningún usuario y
no van en el WAR.

Scripts (se pueden lanzar desde cualquier directorio; todos admiten -h)
-----------------------------------------------------------------------
crear_ca_demo.sh
    Crea la CA y, al acabar, llama a crear_truststore_demo.sh. Se lanza UNA vez: si ca/ ya existe aborta,
    porque regenerar la CA invalidaría todos los .p12 (los de demo y los de test) y el truststore de demo.

crear_truststore_demo.sh
    Genera almacen/truststore-demo.jks (contraseña s3cr3T) copiando ../AlmacenCertificadosConfiables/
    truststore.jks, que no se toca, y añadiéndole la raíz de esta CA (alias educaflow-demo-ca).
    Genera también la CRL vacía de esta CA (almacen/educaflow-demo-ca.crl) y almacen/crls-demo.xml, la lista
    de CRLs: las de ../AlmacenCertificadosConfiables/crl/crls.xml (referenciadas con ruta relativa, sin
    copiarlas, para poder probar con certificados reales) más la de esta CA, sin la que la aplicación rechaza
    los certificados de demo, que no tienen OCSP ni CRLDP. Si ya existen los regenera.
    Hay que volver a lanzarlo a mano cada vez que se regenere truststore.jks o crls.xml.

crear_certificado_usuario_demo.sh <email>
    Emite certificados/<DNI>.p12 con el DNI, el nombre y los apellidos del usuario de usuarios-demo.xml.
    Solo crea el .p12: el alta en CertificadoDigital hay que añadirla a mano en certificados-demo.xml.
    No sobrescribe un .p12 que ya exista.

crear_certificado_test.sh <nombre>
    Emite src/test/resources/firma/test/<nombre>.p12 con un DNI de demo nuevo que no es de ningún usuario.

Los certificados emitidos son de usuario final, firmados directamente por la CA (sin CA intermedia), válidos
20 años y sin CRL ni OCSP. El DNI va en el UID del subject y en el SubjectAltName, junto al CN
"<nombre>|<apellidos>", codificados como UTF8String (si no, la aplicación no los lee: el script lo comprueba).


