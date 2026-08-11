package com.animania.common.helper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Restricted reflection cache retained for addon diagnostics, not gameplay. */
public final class ReflectionUtil {
    private static final Map<String, Method> METHODS = new ConcurrentHashMap<>();
    private static final Map<String, Field> FIELDS = new ConcurrentHashMap<>();

    private ReflectionUtil() { }

    public static Method findMethod(Class<?> type, String name, Class<?>... parameters) {
        String key = type.getName() + '#' + name + java.util.Arrays.toString(parameters);
        return METHODS.computeIfAbsent(key, ignored -> {
            try {
                Method method = type.getDeclaredMethod(name, parameters);
                method.setAccessible(true);
                return method;
            } catch (ReflectiveOperationException error) {
                throw new IllegalArgumentException("Missing method " + key, error);
            }
        });
    }

    public static Field findField(Class<?> type, String name) {
        String key = type.getName() + '#' + name;
        return FIELDS.computeIfAbsent(key, ignored -> {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (ReflectiveOperationException error) {
                throw new IllegalArgumentException("Missing field " + key, error);
            }
        });
    }
}
