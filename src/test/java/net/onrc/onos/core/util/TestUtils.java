package net.onrc.onos.core.util;

import static org.junit.Assert.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * Utils for testing.
 */
public final class TestUtils {

    /**
     * Sets the field, bypassing scope restriction.
     *
     * @param subject Object where the field belongs
     * @param fieldName name of the field to set
     * @param value value to set to the field.
     * @param <T> subject type
     * @param <U> value type
     */
    public static <T, U> void setField(T subject, String fieldName, U value) {
        @SuppressWarnings("unchecked")
        Class<T> clazz = (Class<T>) subject.getClass();
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(subject, value);
        } catch (NoSuchFieldException | SecurityException |
                 IllegalArgumentException | IllegalAccessException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            fail(sw.toString());
        }
    }

    /**
     * Gets the field, bypassing scope restriction.
     *
     * @param subject Object where the field belongs
     * @param fieldName name of the field to get
     * @return value of the field.
     * @param <T> subject type
     * @param <U> field value type
     */
    public static <T, U> U getField(T subject, String fieldName) {
        try {
            @SuppressWarnings("unchecked")
            Class<T> clazz = (Class<T>) subject.getClass();
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);

            @SuppressWarnings("unchecked")
            U result = (U) field.get(subject);
            return result;
        } catch (NoSuchFieldException | SecurityException |
                 IllegalArgumentException | IllegalAccessException e) {

            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            fail(sw.toString());
            return null;
        }
    }

    /**
     * Calls the method, bypassing scope restriction.
     *
     * @param subject Object where the method belongs
     * @param methodName name of the method to call
     * @param paramTypes formal parameter type array
     * @param args arguments
     * @return return value or null if void
     * @param <T> subject type
     * @param <U> return value type
     */
    public static <T, U> U callMethod(T subject, String methodName, Class<?>[] paramTypes, Object...args) {

        try {
            @SuppressWarnings("unchecked")
            Class<T> clazz = (Class<T>) subject.getClass();
            Method method = clazz.getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);

            @SuppressWarnings("unchecked")
            U result = (U) method.invoke(subject, args);
            return result;
        } catch (NoSuchMethodException | SecurityException |
                IllegalAccessException | IllegalArgumentException |
                InvocationTargetException e) {

            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            fail(sw.toString());
            return null;
        }
    }

    /**
     * Calls the method, bypassing scope restriction.
     *
     * @param subject Object where the method belongs
     * @param methodName name of the method to call
     * @param paramType formal parameter type
     * @param arg argument
     * @return return value or null if void
     * @param <T> subject type
     * @param <U> return value type
     */
    public static <T, U> U callMethod(T subject, String methodName, Class<?> paramType, Object arg) {
        return callMethod(subject, methodName, new Class<?>[]{paramType}, arg);
    }

    /**
     * Avoid instantiation.
     */
    private TestUtils() {}
}
