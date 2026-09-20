/**
 * Copyright (C) 2025  Cesar y Lorenzo
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>
 **/
package com.educaflow.subsystem.expedientes.tramitacion.eventmanager;

import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.subsystem.expedientes.db.Tramite;

import java.util.Objects;

public record ContextoTramitacion(Tramite tramite, Centro centro, Profile profile,boolean presentadoEnPapel, boolean presentadoEnRepresentacion) {

    public ContextoTramitacion {
        Objects.requireNonNull(tramite, "tramite no puede ser nulo");
        Objects.requireNonNull(centro, "centro no puede ser nulo");
        Objects.requireNonNull(profile, "profile no puede ser nulo");
    }


}
