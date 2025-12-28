https://github.com/OpenSC/OpenSC/wiki/Feitian-ePass2003


sudo apt-get install pcscd libccid libpcsclite-dev libssl-dev libreadline-dev autoconf automake build-essential docbook-xsl xsltproc libtool pkg-config
sudo apt-get install zlib1g-dev
sudo apt-get install openpace libaec-dev

pkcs15-init -E
pkcs15-init --create-pkcs15  -p pkcs15+onepin --pin 123456 --puk 12345678 --label "Prueba1"


openssl pkcs12 -in certificado.p12 -nocerts -nodes -out key.pem
openssl pkcs12 -in certificado.p12 -clcerts -nokeys -out cert.pem
pkcs15-init --store-private-key key.pem -a 01 --key-usage sign,decrypt --pin 123456
pkcs15-init --store-certificate cert.pem -a 01 --auth-id 01


https://manpages.ubuntu.com/manpages/focal/man5/opensc.conf.5.html
Es necesario configurar el fichero:

/etc/opensc/opensc.conf


con


app default {
	disable_popups = true
}


los dipositivos se ven con:

opensc-tool -l
o con
pkcs11-tool --list-slots
El numero es el slot.



Y los algoritmos se ven con:

//Muestra la lista de algoritmos
Provider p = Security.getProviders("Signature.NONEwithRSA")[0];
for (Provider.Service s : p.getServices()) {
    if (s.getType().equals("Signature")) {
        System.out.println("Algoritmo soportado: " + s.getAlgorithm());
    }
}

Se necesita el Path a la libreria del dispositivo:
"/usr/lib/x86_64-linux-gnu/opensc-pkcs11.so"
O la que haya