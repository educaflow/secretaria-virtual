package com.educaflow.common.criptografia.impl;

import com.educaflow.common.criptografia.DatosCertificado;
import com.educaflow.common.criptografia.EntornoCriptografico;
import com.educaflow.common.criptografia.TipoCertificado;
import com.educaflow.common.criptografia.TipoEmisorCertificado;
import com.educaflow.common.criptografia.impl.helper.CertificateParser;
import com.educaflow.common.criptografia.impl.helper.CriptografiaUtil;

import java.security.KeyStore;
import java.security.cert.*;
import java.util.*;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;

/**
 *
 * @author logongas
 */
public class DatosCertificadoImpl implements DatosCertificado {

    private static final String OID_NIF_FNMT = "1.3.6.1.4.1.5734.1.4";
    private static final String OID_NOMBRE_FNMT = "1.3.6.1.4.1.5734.1.1";
    private static final String OID_APE1_FNMT = "1.3.6.1.4.1.5734.1.2";
    private static final String OID_APE2_FNMT = "1.3.6.1.4.1.5734.1.3";
    private static final String OID_NIF_ACCV = "0.9.2342.19200300.100.1.1";
    private static final String OID_NOMBRE_APELLIDOS_ACCV = "2.5.4.3";
    private static final String OID_NOMBRE_APELLIDOS_ACCV_LOCATION = "2.5.29.17";
    private static final String OID_TIME_STAMPING = "1.3.6.1.5.5.7.3.8";
    private static final String OID_EXTENDED_KEY_USAGE = "2.5.29.37";

    private final X509Certificate certificate;
    private final TipoEmisorCertificado tipoEmisorCertificado;
    private String dni="";
    private String nombre="";
    private String apellidos="";
    private String cif="";
    private String cnSubject;
    private String cnIssuer;
    private final boolean validoEnListaCertificadosConfiables;
    private TipoCertificado tipoCertificado;
    private boolean selloTiempo;
    private Date validoNoAntesDe;
    private Date validoNoDespuesDe;

    public DatosCertificadoImpl(X509Certificate certificate, KeyStore trustedKeyStore) {

        try {
            this.certificate = certificate;
            this.cnSubject = getCnPrincipal(certificate.getSubjectX500Principal());
            this.cnIssuer = getCnPrincipal(certificate.getIssuerX500Principal());
            this.tipoCertificado=getTipoCertificado(this.certificate);
            this.selloTiempo= isSelloTiempo(this.certificate);
            this.validoNoAntesDe= certificate.getNotBefore();
            this.validoNoDespuesDe= certificate.getNotAfter();

            String organizacionIssuer = getOrganizacionPrincipal(certificate.getIssuerX500Principal());
            this.tipoEmisorCertificado = getTipoEmisorCertificado(organizacionIssuer, this.cnIssuer);
            this.validoEnListaCertificadosConfiables = isValidoEnListaCertificadosConfiables(certificate, trustedKeyStore);

            if ((this.selloTiempo==false) && ((this.tipoCertificado==TipoCertificado.USUARIO_FINAL) || (this.tipoCertificado==TipoCertificado.REPRESENTACION))) {
                populateDatosComunesCertificado(this.tipoEmisorCertificado,this.tipoCertificado);
            }

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }



    @Override
    public String getDNI() {
        return dni;
    }

    @Override
    public String getNombre() {
        return nombre;
    }

    @Override
    public String getApellidos() {
        return apellidos;
    }

    @Override
    public String getCif() {
        return cif;
    }    
    
    @Override
    public String getCnSubject() {
        return cnSubject;
    }

    @Override
    public String getCnIssuer() {
        return cnIssuer;
    }

    @Override
    public TipoEmisorCertificado getTipoEmisorCertificado() {
        return tipoEmisorCertificado;
    }

    @Override
    public boolean isValidoEnListaCertificadosConfiables() {
        return validoEnListaCertificadosConfiables;
    }

    @Override
    public TipoCertificado getTipoCertificado() {
        return tipoCertificado;
    }

    @Override
    public boolean isSelloTiempo() {
        return selloTiempo;
    }

    @Override
    public Date getValidoNoAntesDe() {
        return validoNoAntesDe;
    }
    @Override
    public Date getValidoNoDespuesDe() {
        return validoNoDespuesDe;
    }

    @Override
    public X509Certificate getCertificate() {
        return this.certificate;
    }

    
    
    
    private boolean isValidoEnListaCertificadosConfiables(X509Certificate certificate, KeyStore trustStore) {
        try {
            if (trustStore==null) {
                return false;
            }


            CertPathValidator validator = CertPathValidator.getInstance("PKIX");

            PKIXParameters params = new PKIXParameters(trustStore);
            params.setRevocationEnabled(false);
            List<CertStore> certStoresCertificateRevocationList=getCertStoresFromCertificateRevocationLists(EntornoCriptografico.getAlmacenCertificadosConfiables().getCertificateRevocationLists());
            params.setCertStores(certStoresCertificateRevocationList);
            PKIXRevocationChecker rc = (PKIXRevocationChecker) validator.getRevocationChecker();
            rc.setOptions(EnumSet.of(PKIXRevocationChecker.Option.SOFT_FAIL));
            params.addCertPathChecker(rc);


            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            List<Certificate> certs = Collections.singletonList(certificate);
            CertPath certPath = cf.generateCertPath(certs);

            try {
                PKIXCertPathValidatorResult result = (PKIXCertPathValidatorResult) validator.validate(certPath, params);

                TrustAnchor trustAnchor = result.getTrustAnchor();
                X509Certificate ca = trustAnchor.getTrustedCert();

                return true;
            } catch (CertPathValidatorException ex) {
                return false;
            }

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }

    private TipoEmisorCertificado getTipoEmisorCertificado(String organizacionIssuer,String cnIssuer) {
        TipoEmisorCertificado tipoEmisorCertificado;
        if (organizacionIssuer.contains("FNMT") && !cnIssuer.contains("ACCV") && !cnIssuer.contains("DNIE")) {
            tipoEmisorCertificado = TipoEmisorCertificado.FNMT;
        } else if (!organizacionIssuer.contains("FNMT") && cnIssuer.contains("ACCV") && !cnIssuer.contains("DNIE")) {
            tipoEmisorCertificado = TipoEmisorCertificado.ACCV;
        } else if (!organizacionIssuer.contains("FNMT") && !cnIssuer.contains("ACCV") && cnIssuer.contains("DNIE")) {
            tipoEmisorCertificado = TipoEmisorCertificado.DNI;
        } else {
            tipoEmisorCertificado = null;
        }

        return tipoEmisorCertificado;
    }

    private TipoCertificado getTipoCertificado(X509Certificate x509Certificate) {
        try {
            int basicConstraints = x509Certificate.getBasicConstraints();
            X500Principal subject = x509Certificate.getSubjectX500Principal();
            X500Principal issuer = x509Certificate.getIssuerX500Principal();
            String organizacionIssuer=getOrganizacionPrincipal(issuer);
            String cnIssuer=getCnPrincipal(issuer);
            

            TipoCertificado tipoCertificado;

            if (basicConstraints == -1) {
                if (organizacionIssuer.contains("FNMT") && (cnIssuer.contains("Representación"))) {
                    tipoCertificado= TipoCertificado.REPRESENTACION;
                } else {
                    tipoCertificado= TipoCertificado.USUARIO_FINAL;
                }
            } else if (subject.equals(issuer)) {
                tipoCertificado= TipoCertificado.CA_RAIZ;
            } else {
                tipoCertificado= TipoCertificado.CA_INTERMEDIA;
            }

            return tipoCertificado;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private boolean isSelloTiempo(X509Certificate x509Certificate) {
        try {
            List<String> eku = x509Certificate.getExtendedKeyUsage();
            if (eku == null || eku.isEmpty()) {
                return false;
            }

            if (eku.contains(OID_TIME_STAMPING)==false) {
                return false;
            }

            boolean critical = (x509Certificate.getCriticalExtensionOIDs() != null) && (x509Certificate.getCriticalExtensionOIDs().contains(OID_EXTENDED_KEY_USAGE));
            if (critical==false) {
                return false;
            }

            return true;
        } catch (CertificateParsingException e) {
            throw new RuntimeException("Error analizando EKU del certificado", e);
        }
    }

    private List<CertStore> getCertStoresFromCertificateRevocationLists(List<CRL> certStoreFromCertificateRevocationLists) {
        CertStore certStore = CriptografiaUtil.getCertStore(certStoreFromCertificateRevocationLists);

        return Collections.singletonList(certStore);
    }

    /*******************************************************************************************************/
    /********************** Extraer Información del certificado según el emissor (CA) **********************/
    /*******************************************************************************************************/     
    

    private void populateDatosComunesCertificado(TipoEmisorCertificado tipoEmisorCertificado,TipoCertificado tipoCertificado) {

        if (tipoEmisorCertificado != null) {
            switch (tipoEmisorCertificado) {
                case FNMT:
                    if (tipoCertificado==TipoCertificado.USUARIO_FINAL) {
                        populateDatosComunesCertificadoUsuarioFNMT();
                    } else if (tipoCertificado==TipoCertificado.REPRESENTACION) {
                        populateDatosComunesCertificadoRepresentacionFNMT();
                    } else {
                        throw new RuntimeException("El tipo de certificado no es válido:" + this.tipoCertificado);
                    }
                    break;
                case ACCV:
                    populateDatosComunesCertificadoACCV();
                    break;
                case DNI:
                    populateDatosComunesCertificadoDNI();
                    break;
                default:
                    throw new RuntimeException("El tipo de emisor es desconocido:" + this.tipoEmisorCertificado);
            }
        } else {
            populateDatosComunesCertificadoEmpy();
        }


    }

    private void populateDatosComunesCertificadoUsuarioFNMT() {
        try {
            dni = getOnlyValueInMap(CertificateParser.findOidsWithLocation(certificate, OID_NIF_FNMT));
        } catch (Exception ex) {
            //Si falla algo se quedan los datos sin cargar
        }
        try {
            nombre = getOnlyValueInMap(CertificateParser.findOidsWithLocation(certificate, OID_NOMBRE_FNMT));
        } catch (Exception ex) {
            //Si falla algo se quedan los datos sin cargar
        }
        try {
            apellidos = (getOnlyValueInMap(CertificateParser.findOidsWithLocation(certificate, OID_APE1_FNMT)) + " " + getOnlyValueInMap(CertificateParser.findOidsWithLocation(certificate, OID_APE2_FNMT))).trim();
        } catch (Exception ex) {
            //Si falla algo se quedan los datos sin cargar
        }
        cif="";

    }
    
    private void populateDatosComunesCertificadoRepresentacionFNMT() {
        try {
            dni = CertificateParser.getDNIFromIDCES(getOnlyValueInMap(CertificateParser.findOidsWithLocation(certificate, "1.3.6.1.4.1.5734.1.4")));
        } catch (Exception ex) {
            //Si falla algo se quedan los datos sin cargar
        }
        try {
            nombre = getOnlyValueInMap(CertificateParser.findOidsWithLocation(certificate, "1.3.6.1.4.1.5734.1.1"));
        } catch (Exception ex) {
            //Si falla algo se quedan los datos sin cargar
        }
        try {
            apellidos = (getOnlyValueInMap(CertificateParser.findOidsWithLocation(certificate, "1.3.6.1.4.1.5734.1.2")) + " " + getOnlyValueInMap(CertificateParser.findOidsWithLocation(certificate, "1.3.6.1.4.1.5734.1.3"))).trim();
        } catch (Exception ex) {
            //Si falla algo se quedan los datos sin cargar
        }
        try {
            cif = CertificateParser.getCIFFromVATES(getOnlyValueInMap(CertificateParser.findOidsWithLocation(certificate, "1.3.6.1.4.1.5734.1.7")));
        } catch (Exception ex) {
            //Si falla algo se quedan los datos sin cargar
        } 
    }
    
    
    private void populateDatosComunesCertificadoACCV() {
        try {
            dni = getOnlyValueInMap(CertificateParser.findOidsWithLocation(certificate, OID_NIF_ACCV));
        } catch (Exception ex) {
            //Si falla algo se quedan los datos sin cargar
        }


        try {
            String nombreApellidos = CertificateParser.findOidsWithLocation(certificate, OID_NOMBRE_APELLIDOS_ACCV).get(OID_NOMBRE_APELLIDOS_ACCV_LOCATION);
            if ((nombreApellidos!=null) && (nombreApellidos.contains("|"))) {
                String[] arrNombreApellidos = nombreApellidos.split("\\|");
                nombre = arrNombreApellidos[0];
                apellidos = String.join(" ", Arrays.copyOfRange(arrNombreApellidos, 1, arrNombreApellidos.length));
            }
        } catch (Exception ex) {
            //Si falla algo se quedan los datos sin cargar
        }
        cif="";        
    }

    private void populateDatosComunesCertificadoDNI() {
        X500Principal x500Principal = certificate.getSubjectX500Principal();
        Map<String, String> data = getX500PrincipalData(x500Principal);
        try {
            dni = data.get("SERIALNUMBER");
        } catch (Exception ex) {
            //Si falla algo se quedan los datos sin cargar
        }
        try {
            nombre = data.get("GIVENNAME");
        } catch (Exception ex) {
            //Si falla algo se quedan los datos sin cargar
        }
        try {
            apellidos = data.get("CN").split(",")[0].trim();
        } catch (Exception ex) {
            //Si falla algo se quedan los datos sin cargar
        }
        cif="";        
    }



    
    private void populateDatosComunesCertificadoEmpy() {
        dni = "";
        nombre = "";
        apellidos = "";
        cif="";
    }    
    
    
    
    /*************************************************************************************/
    /********************************** Datos Principal **********************************/
    /*************************************************************************************/
    
    private Map<String, String> getX500PrincipalData(X500Principal x500Principal) {

        try {

            LdapName ldapDN = new LdapName(x500Principal.getName());
            Map<String, String> campos = new HashMap<>();

            Map<String, String> oidMap = Map.of(
                    "2.5.4.42", "GIVENNAME",
                    "2.5.4.4", "SURNAME",
                    "2.5.4.5", "SERIALNUMBER"
            );

            for (Rdn rdn : ldapDN.getRdns()) {
                String type = rdn.getType();
                Object value = rdn.getValue();
                String strValue;

                // convertir byte[] a String si es necesario
                if (value instanceof byte[]) {
                    strValue = new String((byte[]) value).trim();
                } else if (value == null) {
                    strValue = "";
                } else {
                    strValue = value.toString().trim();
                }

                // usar nombre legible si existe en el mapa
                if (oidMap.containsKey(type)) {
                    campos.put(oidMap.get(type), strValue);
                } else {
                    campos.put(type, strValue);
                }
            }

            return campos;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private String getCnPrincipal(X500Principal certificate) {
        Map<String, String> datos=getX500PrincipalData(certificate);
        String cn=datos.get("CN");

        return cn;
    }
    
    private String getOrganizacionPrincipal(X500Principal certificate) {
        Map<String, String> datos=getX500PrincipalData(certificate);
        String cn=datos.get("O");

        return cn;
    }    
    
    /**************************************************************************************/
    /************************************* Utilidades *************************************/
    /**************************************************************************************/
    
    private String getOnlyValueInMap(Map<String, String> map) {
        if (map.size() == 0) {
            return null;
        }

        String value=null;

        for(Map.Entry<String, String> entry : map.entrySet()) {
            if (value==null) {
                value=entry.getValue();
            } else if (value.equals(entry.getValue())==true) {
                // ok, es el mismo valor
            } else {
                throw new RuntimeException("Existe más de un elemento en el Map:" + map.size() + " " + this.certificate.toString());
            }
        }

        return value;
    }    
    
    
    @Override
    public String toString() {
        return this.getDNI() + ":" + this.getNombre() + "," + this.getApellidos() + "--Tipo:" + this.getTipoEmisorCertificado() + "(" + this.isValidoEnListaCertificadosConfiables() + ")";
    }
    
  


}
