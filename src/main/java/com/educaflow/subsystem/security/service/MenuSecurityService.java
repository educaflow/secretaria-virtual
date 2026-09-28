package com.educaflow.subsystem.security.service;

import com.axelor.script.ScriptAllowed;

/**
 * Decide qué menús ve el usuario autenticado.
 *
 * <p>Lo evalúa el atributo {@code if} de <b>todos</b> los {@code <menuitem>}, que no se escribe a
 * mano: lo añade el preprocesador de vistas de EducaFlowBuildTools como
 * {@code __config__.menuSecurity.isVisible("<name>")} (la propiedad {@code context.menuSecurity} de
 * {@code axelor-config.properties} apunta a esta interfaz y Axelor la resuelve con Guice). Los
 * {@code groups} de Axelor solo distinguen {@code admins} de {@code users}; lo demás se decide aquí.
 *
 * <p><b>Esto no autoriza nada.</b> Un menú que no se ve no protege la vista que abre: lo que cada
 * usuario puede leer o tramitar lo deciden los permisos de Axelor y el tramitador.
 *
 * <p>{@code @ScriptAllowed} es imprescindible: la política de scripts de Axelor ({@code ScriptPolicy})
 * solo deja que un script Groovy invoque clases de su lista blanca o anotadas así (la anotación se
 * busca también en las interfaces, por eso va aquí y vale para la implementación). Sin ella el
 * {@code if} lanza {@code ScriptPolicyException} y el menú desaparece para todos.
 */
@ScriptAllowed
public interface MenuSecurityService {

    /** Si el usuario autenticado ve el menú con ese {@code name}. */
    boolean isVisible(String menuName);
}
