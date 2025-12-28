#!/bin/bash
#Este script crea una truststore con los CA oficiales con los que se permiten firmas "cosas"


TRUSTSTORE=./truststore.jks
TRUSTSTORE_PASSWORD=s3cr3T
CERTS_DIR=./certs
CRL_DIR=./crl
CRL_FILE_LIST=crls.xml
#Lista de confianza de prestadores cualificados de servicios electrónicos de confianza
#Pagina desde donde descargarla: https://sedediatid.digital.gob.es/Prestadores/Paginas/Inicio.aspx
URL_TSL=https://sedediatid.digital.gob.es/Prestadores/TSL/TSL.xml

rm -rf ${CRL_DIR}
rm -rf ${CERTS_DIR}
rm *.zip
rm TSL.xml
rm ${TRUSTSTORE}
mkdir -p ${CERTS_DIR}
mkdir -p ${CRL_DIR}

download_crt() {
    local url="$1"
    local file="$(basename "$url")"  # Extrae el nombre del archivo de la URL
    wget -O ${CERTS_DIR}/"$file" "$url"
}


download_crt_and_unzip() {
    local url="$1"
    local file="$(basename "$url")"  # Extrae el nombre del archivo de la URL
    wget -O "$file" "$url"
    unzip -o "$file" -d ${CERTS_DIR}   # -o sobrescribe sin preguntar
    rm "$file"
}

download_crl() {
    local url="$1"
    local file="$(basename "$url")"  # Extrae el nombre del archivo de la URL
    wget -O ${CRL_DIR}/"$file" "$url"
}


import_certs() {


    # Recorrer todos los archivos .cer en el directorio de certificados
    find "$CERTS_DIR" -name "*.cer" | while read cert_file; do
        # Extraer el nombre del archivo sin la extensión y sin el directorio
        # Esto se usará como alias para el certificado en el truststore
        alias_name=$(basename "$cert_file" .cer)

        echo "Importando $cert_file con alias: $alias_name"

        # Comando para importar el certificado
        # -noprompt para que no pida confirmación "yes/no"
        keytool -import -trustcacerts -file "$cert_file" -alias "$alias_name" -keystore "$TRUSTSTORE" -storepass "$TRUSTSTORE_PASSWORD" -noprompt

        if [ $? -eq 0 ]; then
            echo "Importado exitosamente: $cert_file"
        else
            echo "Error al importar: $cert_file"
            exit 1
        fi
    done

    echo "Proceso de importación de certificados completado."

}


# Lista de URLs de certificados del DNI Electrónico
urls_cert_download_unzip=(
    "https://www.dnielectronico.es/ZIP/ACRAIZ-DNIE2.zip"
    "https://www.dnielectronico.es/ZIP/ACRAIZ-SHA1.zip"
    "https://www.dnielectronico.es/ZIP/ACRAIZ-SHA2.zip"
    "https://www.dnielectronico.es/ZIP/ACDNIE001.crt.zip"
    "https://www.dnielectronico.es/ZIP/ACDNIE002.crt.zip"
    "https://www.dnielectronico.es/ZIP/ACDNIE003.crt.zip"
    "https://www.dnielectronico.es/ZIP/ACDNIE004.crt.zip"
    "https://www.dnielectronico.es/ZIP/ACDNIE005.crt.zip"
    "https://www.dnielectronico.es/ZIP/ACDNIE006.crt.zip"
)

# Descargar y descomprimir todos los zips
for url in "${urls_cert_download_unzip[@]}"; do
    download_crt_and_unzip "$url"
done

urls_cert_download=(
"https://www.accv.es/fileadmin/Archivos/certificados/accv_rsa1_tls_cross.crt"
"https://www.accv.es/fileadmin/Archivos/certificados/accv_root_rsa_eidas_2023.crt"
"https://www.accv.es/fileadmin/Archivos/certificados/accv_rsa1_cliente.crt"
"https://www.accv.es/fileadmin/Archivos/certificados/accv_rsa1_profesionales.crt"
"https://www.accv.es/fileadmin/Archivos/certificados/accv_root_rsa_tls_2024.crt"
"https://www.accv.es/fileadmin/Archivos/certificados/accv_root_ecc_tls_2024.crt"
"https://www.accv.es/fileadmin/Archivos/certificados/accv_root_rsa_eidas_tsa_2024.crt"
"https://www.accv.es/fileadmin/Archivos/certificados/accv_root_ecc_eidas_tsa_2024.crt"
"https://www.accv.es/fileadmin/Archivos/certificados/accv_rsa1_tls.crt"
"https://www.accv.es/fileadmin/Archivos/certificados/accv_ecc1_tls.crt"
"https://www.accv.es/fileadmin/Archivos/certificados/accv_rsa1_tsa.crt"
"https://www.accv.es/fileadmin/Archivos/certificados/accv_ecc1_tsa.crt"
"https://www.accv.es/fileadmin/Archivos/certificados/accv_rsa1_componentes.crt"
"https://www.accv.es/fileadmin/Archivos/certificados/accv_root_ecc_eidas_2023.crt"
"https://www.accv.es/fileadmin/Archivos/certificados/accv_ecc1_cliente.crt"
"https://www.accv.es/fileadmin/Archivos/certificados/accv_ecc1_profesionales.crt"
"https://www.accv.es/fileadmin/Archivos/certificados/accv_ecc1_componentes.crt"
"https://www.accv.es/fileadmin/Archivos/certificados/ACCVRAIZ1.crt"
"https://www.accv.es/fileadmin/Archivos/certificados/ACCVCA120.crt"
"https://www.accv.es/fileadmin/Archivos/certificados/ACCVCA110SHA2.cacert.crt"
"https://www.accv.es/fileadmin/Archivos/certificados/ACCVCA130SHA2.cacert.crt"
"https://www.accv.es/fileadmin/Archivos/certificados/rootca.crt"
"https://www.accv.es/fileadmin/Archivos/certificados/accv-ca2.crt"
"https://www.accv.es/fileadmin/Archivos/certificados/accv-ca1.crt"
"https://www.accv.es/fileadmin/Archivos/certificados/accv-ca3.crt"
"https://www.accv.es/fileadmin/Archivos/certificados/ca.crt"
)

# Descargar los certificados
for url in "${urls_cert_download[@]}"; do
    download_crt "$url"
done

urls_crl_download=(
"https://www.accv.es/fileadmin/Archivos/certificados/accv_root_rsa_eidas_2023.crl"
"https://www.accv.es/fileadmin/Archivos/certificados/accv_rsa1_cliente.crl"
"https://www.accv.es/fileadmin/Archivos/certificados/accv_rsa1_profesionales.crl"
"https://www.accv.es/fileadmin/Archivos/certificados/accv_rsa1_componentes.crl"
"https://www.accv.es/fileadmin/Archivos/certificados/accv_root_ecc_eidas_2023.crl"
"https://www.accv.es/fileadmin/Archivos/certificados/accv_ecc1_cliente.crl"
"https://www.accv.es/fileadmin/Archivos/certificados/accv_ecc1_profesionales.crl"
"https://www.accv.es/fileadmin/Archivos/certificados/accv_ecc1_componentes.crl"
"https://www.accv.es/fileadmin/Archivos/certificados/raizaccv1_der.crl"
"https://www.accv.es/fileadmin/Archivos/certificados/accvca120_der.crl"
"https://www.accv.es/fileadmin/Archivos/certificados/accvca110_der.crl"
"https://www.accv.es/fileadmin/Archivos/certificados/accvca130_der.crl"
"https://www.accv.es/fileadmin/Archivos/certificados/rootgva_der.crl"
"https://www.accv.es/fileadmin/Archivos/certificados/ciudadanos.crl"
"https://www.accv.es/fileadmin/Archivos/certificados/cagva-entidad_der.crl"
"https://www.accv.es/fileadmin/Archivos/certificados/cagva-entidad_der.crl"
"https://www.accv.es/fileadmin/Archivos/certificados/cagva_der.crl"
)

for url in "${urls_crl_download[@]}"; do
    download_crl "$url"
done

#Generar el fichero XML con la lista de CRL
{
    echo '<?xml version="1.0" encoding="UTF-8"?>'
    echo '<crls>'
    for fichero in "${CRL_DIR}"/*.crl; do
        nombre=$(basename "$fichero")
        echo "  <crl>${nombre}</crl>"
    done
    echo '</crls>'
} > "${CRL_DIR}/${CRL_FILE_LIST}"



#Renombrar los .crt a .cer para que el keytool los acepte
for f in ${CERTS_DIR}/*.crt; do
    [ -e "$f" ] || continue  # Saltar si no hay archivos .crt
    mv "$f" "${f%.crt}.cer"
done


#Generar el KeyStore
$JAVA_HOME/bin/keytool -genkeypair -alias educaflow -keystore $TRUSTSTORE -storepass $TRUSTSTORE_PASSWORD -keyalg RSA -keysize 2048  -validity 9999 -dname "CN=EducaFlow, OU=IT, O=Educaflow, L=Valencia, ST=Valencia, C=ES"
#Bajar la lista oficial de Lista de confianza de prestadores cualificados de servicios electrónicos de confianza
#https://sedediatid.digital.gob.es/Prestadores/Paginas/Inicio.aspx
wget -O TSL.xml "${URL_TSL}"
#Genera los certificados a partir del XML
python3 extract_certs.py
#Importar los certificados en el KeyStore
import_certs
rm -rf ${CERTS_DIR}
rm TSL.xml

lineasTSL=$(keytool -list -keystore "$TRUSTSTORE" -storepass "$TRUSTSTORE_PASSWORD" -v | grep Propietario | wc -l)

if [ "$lineasTSL" -lt 20 ]; then
    echo "Error: Hay muy poco proveedores en el TSL.xml hay debe haber ido mal ($lineas)"
    exit 1
fi

keytool -list -keystore "$TRUSTSTORE" -storepass "$TRUSTSTORE_PASSWORD" -v | grep Propietario

