package com.educaflow.base.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ReflectionUtil {


    public static boolean hasMethod(Class<?> baseClass, String methodName, Class<?> returnClass, Class<? extends Annotation> annotation, Class<?>[] parameterTypes) {
        return getMethod(baseClass, methodName, returnClass, annotation, parameterTypes).isPresent();
    }

    public static Optional<Method> getMethod(Class<?> baseClass, String methodName, Class<?> returnClass, Class<? extends Annotation> annotation, Class<?>[] parameterTypes) {
        List<Method> matchingMethods = Arrays.stream(baseClass.getDeclaredMethods())
                .filter(method -> methodName == null || method.getName().equals(methodName))
                .filter(method -> returnClass == null || returnClass.isAssignableFrom(method.getReturnType()))
                .filter(method -> annotation == null || method.isAnnotationPresent(annotation))
                .filter(method -> parameterTypes == null || Arrays.equals(method.getParameterTypes(), parameterTypes))
                .toList();

        if (matchingMethods.size() > 1) {
            throw new RuntimeException("Se encontró más de un método: " + methodName + " en la clase: " + baseClass.getName() + " con Nº parámetros: " + (parameterTypes != null ? parameterTypes.length : "N/A") + " y retorno: " + (returnClass != null ? returnClass.getName() : "N/A") + " y la anotación: " + (annotation != null ? annotation.getName() : "N/A"));
        }

        return matchingMethods.stream().findFirst();
    }

    public static Enum getEnumConstant(Class<? extends Enum> enumClass, String constantName) {
        Objects.requireNonNull(enumClass, "enumClass no puede ser null");
        TextUtil.requireNonBlank(constantName, "constantName no puede ser null ni blank");

        try {
            return (Enum)Enum.valueOf(enumClass, constantName);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("No enum constant " + enumClass.getName() + "." + constantName, e);
        }
    }


    public static Object getFieldValue(Object obj, String fieldName) {
        try {
            Field profileField = obj.getClass().getDeclaredField(fieldName);
            profileField.setAccessible(true);
            return profileField.get(obj);
        } catch (Exception ex) {
            throw new RuntimeException("No se pudo acceder al campo '" + fieldName + "' del objeto " + obj, ex);
        }
    }



}
