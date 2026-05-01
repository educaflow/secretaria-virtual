package com.educaflow.subsystem.common.db.repo;

import com.axelor.auth.AuthService;
import com.axelor.auth.db.User;
import com.axelor.inject.Beans;
import com.educaflow.base.util.TextUtil;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UsuarioListener {

    private final Logger logger = LoggerFactory.getLogger(UsuarioListener.class);

    /*@PrePersist
    private void onPrePersist(User usuario) {

        if (TextUtil.isNullOrBlank(usuario.getCode())){
            usuario.setCode(usuario.getDni());
        }


        if (TextUtil.isNullOrBlank(usuario.getName())) {
            usuario.setName(usuario.getNombre() + " " + usuario.getApellidos());
        }


    }

    @PreUpdate
    private void onPreUpdate(User usuario){
        // 2. Concatenar nombre con seguridad
        String nombre = usuario.getNombre() != null ? usuario.getNombre() : "";
        String apellidos = usuario.getApellidos() != null ? usuario.getApellidos() : "";
        String nombreCompleto = (nombre + " " + apellidos).trim();

        // Solo asignamos si el nombre resultante no está vacío y el actual sí lo está
        if (TextUtil.isNullOrBlank(usuario.getName()) && !nombreCompleto.isEmpty()) {
            usuario.setName(nombreCompleto);
        }
    }*/

}
