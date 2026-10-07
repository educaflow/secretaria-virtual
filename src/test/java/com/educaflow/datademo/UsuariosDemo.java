package com.educaflow.datademo;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Lee los usuarios de demo de {@code data-demo/input/usuarios-demo.xml}, para que los tests saquen de ahí sus DNI
 * en lugar de copiarlos.
 */
public class UsuariosDemo {

    private static final String RUTA_USUARIOS_DEMO = "data-demo/input/usuarios-demo.xml";

    public record UsuarioDemo(String email, String documento, String nombre, String apellidos) {
    }

    public static List<UsuarioDemo> getUsuariosDemo() {
        List<UsuarioDemo> usuariosDemo = new ArrayList<>();
        NodeList usuarios = getUsuarios();
        for (int i = 0; i < usuarios.getLength(); i++) {
            Element usuario = (Element) usuarios.item(i);
            usuariosDemo.add(new UsuarioDemo(usuario.getAttribute("email"), usuario.getAttribute("documento"), usuario.getAttribute("nombre"), usuario.getAttribute("apellidos")));
        }
        return usuariosDemo;
    }

    public static UsuarioDemo getUsuarioDemo(String email) {
        return getUsuariosDemo().stream()
                .filter(usuario -> usuario.email().equals(email))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No existe ningún usuario de demo con el email " + email));
    }

    private static NodeList getUsuarios() {
        try (InputStream inputStream = UsuariosDemo.class.getClassLoader().getResourceAsStream(RUTA_USUARIOS_DEMO)) {
            if (inputStream == null) {
                throw new IllegalStateException("No se encuentra " + RUTA_USUARIOS_DEMO + " en el classpath");
            }
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
            return document.getElementsByTagName("usuario");
        } catch (Exception ex) {
            throw new RuntimeException("No se puede leer " + RUTA_USUARIOS_DEMO, ex);
        }
    }
}
