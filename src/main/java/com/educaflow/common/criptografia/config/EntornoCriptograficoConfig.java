package com.educaflow.common.criptografia.config;

import java.util.List;

public class EntornoCriptograficoConfig {

    private final AlmacenCertificadosConfiablesConfig almacenCertificadosConfiablesConfig;
    private final List<DispositivoCriptograficoConfig> dispositivoCriptograficoConfigs;

    public EntornoCriptograficoConfig(AlmacenCertificadosConfiablesConfig almacenCertificadosConfiablesConfig, List<DispositivoCriptograficoConfig> dispositivoCriptograficoConfigs) {
        this.almacenCertificadosConfiablesConfig = almacenCertificadosConfiablesConfig;
        this.dispositivoCriptograficoConfigs = dispositivoCriptograficoConfigs;
    }

    public AlmacenCertificadosConfiablesConfig getAlmacenCertificadosConfiablesConfig() {
        return almacenCertificadosConfiablesConfig;
    }

    public List<DispositivoCriptograficoConfig> getDispositivoCritograficoConfigs() {
        return dispositivoCriptograficoConfigs;
    }

}
