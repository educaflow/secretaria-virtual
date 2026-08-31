package com.educaflow.subsystem.expedientes.services.eventmanager;


import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.subsystem.expedientes.services.internal.ExpedienteUtil;
import com.educaflow.subsystem.registroentradasalida.db.RegistroEntrada;
import com.educaflow.subsystem.registroentradasalida.db.RegistroSalida;
import com.educaflow.subsystem.registroentradasalida.service.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EventContext {

    final private Expediente expediente;
    final private Profile profile;
    final private Centro centro;
    private RegistroEntrada registroEntrada=null;
    private RegistroSalida registroSalida=null;
    final private ModelServiceFactory modelServiceFactory;

    /**
     * El {@code ModelServiceFactory} llega por parámetro y no de {@code Beans.get} porque un
     * EventContext se construye a mano por cada evento (no lo crea Guice): así la dependencia es
     * explícita y la clase se puede instanciar sin contenedor. Lo pasa quien sí es un bean, el
     * {@code ExpedienteController}, que lo tiene inyectado.
     */
    public EventContext(Expediente expediente,Profile profile, Centro centro, ModelServiceFactory modelServiceFactory) {
        this.expediente = expediente;
        this.profile = profile;
        this.centro = centro;
        this.modelServiceFactory=Objects.requireNonNull(modelServiceFactory, "modelServiceFactory no puede ser null");
    }

    public Profile getProfile() {
        return profile;
    }
    public Centro getCentro() { return centro; }

    public String toString() {
        return "EventContext [profile=" + profile + ", centro=" + centro + "]";
    }

    public void updateState(State state) {
        ExpedienteUtil.updateState(expediente,state);
    }


    public RegistroEntrada createRegistroEntrada(MetaFile documentoPdf, List<MetaFile> anexos) {
        Objects.requireNonNull(expediente, "No es posible añadir un registro de salida ya que aun no existe el expediente");
        Objects.requireNonNull(documentoPdf, "documentoPdf no puede ser null");
        if (this.registroEntrada!=null) {
            throw new RuntimeException("Ya existe un registro de entrada definido");
        }

        anexos=cloneAnexos(anexos);

        RegistroEntradaInsertDTO registroEntradaInsertDTO =new RegistroEntradaInsertDTO(
                this.getCentro(),
                new PersonaRegistro(
                        this.expediente.getPersonaSolicitante().getNombre()+ " "+this.expediente.getPersonaSolicitante().getApellidos(),
                        this.expediente.getPersonaSolicitante().getDni()
                ),
                new PersonaRegistro(
                        this.expediente.getPersonaInteresada().getNombre()+ " "+this.expediente.getPersonaInteresada().getApellidos(),
                        this.expediente.getPersonaInteresada().getDni()
                ),
                this.expediente.getNumeroExpediente(),
                getAsunto()
        );

        RegistroEntradaService registroEntradaService=(RegistroEntradaService)modelServiceFactory.resolve(RegistroEntrada.class);
        RegistroEntrada registroEntrada= registroEntradaService.createRegistroEntrada(registroEntradaInsertDTO,documentoPdf,anexos);

        this.registroEntrada=registroEntrada;

        return registroEntrada;
    }
    public RegistroSalida createRegistroSalida(MetaFile documentoPdf,List<MetaFile> anexos) {
        Objects.requireNonNull(expediente, "No es posible añadir un registro de salida ya que aun no existe el expediente");
        Objects.requireNonNull(documentoPdf, "documentoPdf no puede ser null");
        if (this.registroSalida!=null) {
            throw new RuntimeException("Ya existe un registro de salida definido");
        }

        anexos=cloneAnexos(anexos);

        RegistroSalidaInsertDTO registroSalidaInsertDTO =new RegistroSalidaInsertDTO(
                this.expediente.getCentro(),
                getAsunto()
        );

        RegistroSalidaService registroSalidaService=(RegistroSalidaService)modelServiceFactory.resolve(RegistroSalida.class);
        RegistroSalida registroSalida=registroSalidaService.createRegistroSalida(registroSalidaInsertDTO,documentoPdf,anexos);

        this.registroSalida=registroSalida;

        return registroSalida;
    }

    private List<MetaFile> cloneAnexos(List<MetaFile> anexos) {
        if (anexos == null) {
            return List.of();
        }
        List<MetaFile> clon = new ArrayList<>(anexos.size());
        for (MetaFile metaFile : anexos) {
            Objects.requireNonNull(metaFile.getFileName());

            clon.add(MetaFileUtil.cloneMetaFile(metaFile));
        }
        return clon;
    }

    private String getAsunto() {
        return "Expediente: "+this.expediente.getNumeroExpediente()+" - "+this.expediente.getName();
    }


    public RegistroEntrada getRegistroEntrada() {
        return registroEntrada;
    }

    public RegistroSalida getRegistroSalida() {
        return registroSalida;
    }
}
