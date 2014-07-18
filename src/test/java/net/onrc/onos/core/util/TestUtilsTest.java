package net.onrc.onos.core.util;

import static org.junit.Assert.*;
import static net.onrc.onos.core.util.TestUtils.*;

import org.junit.Before;
import org.junit.Test;

/**
 * Test and usage examples for TestUtils.
 */
public class TestUtilsTest {

    /**
     * Test data.
     */
    private static final class TestClass {

        @SuppressWarnings("unused")
        private int privateField = 42;

        @SuppressWarnings("unused")
        protected int protectedField = 2501; // CHECKSTYLE IGNORE THIS LINE

        /**
         * Protected method with multiple argument.
         *
         * @param x simply returns
         * @param y not used
         * @return x
         */
        @SuppressWarnings("unused")
        private int privateMethod(Number x, Long y) {
            return x.intValue();
        }

        /**
         * Protected method with no argument.
         *
         * @return int
         */
        @SuppressWarnings("unused")
        protected int protectedMethod() {
            return 42;
        }

        /**
         * Method returning array.
         *
         * @param ary random array
         * @return ary
         */
        @SuppressWarnings("unused")
        private int[] arrayReturnMethod(int[] ary) {
            return ary;
        }

        /**
         * Method without return value.
         *
         * @param s ignored
         */
        @SuppressWarnings("unused")
        private void voidMethod(String s) {
            System.out.println(s);
        }
    }

    private TestClass test;

    /**
     * setUp.
     */
    @Before
    public void setUp() {
        test = new TestClass();
    }

    /**
     * Example to access private field.
     */
    @Test
    public void testSetGetPrivateField() {

        assertEquals(42, getField(test, "privateField"));
        setField(test, "privateField", 0xDEAD);
        assertEquals(0xDEAD, getField(test, "privateField"));
    }

    /**
     * Example to access protected field.
     */
    @Test
    public void testSetGetProtectedField() {

        assertEquals(2501, getField(test, "protectedField"));
        setField(test, "protectedField", 0xBEEF);
        assertEquals(0xBEEF, getField(test, "protectedField"));
    }

    /**
     * Example to call private method and multiple parameters.
     * <p/>
     * It also illustrates that paramTypes must match declared type,
     * not the runtime types of arguments.
     */
    @Test
    public void testCallPrivateMethod() {

        int result = callMethod(test, "privateMethod",
                        new Class<?>[] {Number.class, Long.class},
                        Long.valueOf(42), Long.valueOf(32));
        assertEquals(42, result);
    }

    /**
     * Example to call protected method and no parameters.
     */
    @Test
    public void testCallProtectedMethod() {

        int result = callMethod(test, "protectedMethod",
                        new Class<?>[] {});
        assertEquals(42, result);
    }

    /**
     * Example to call method returning array.
     * <p/>
     * Note: It is not required to receive as Object.
     * Following is just verifying it is not Boxed arrays.
     */
    @Test
    public void testCallArrayReturnMethod() {

        int[] array = {1, 2, 3};
        Object aryResult = callMethod(test, "arrayReturnMethod",
                new Class<?>[] {int[].class}, array);
        assertEquals(int[].class, aryResult.getClass());
        assertArrayEquals(array, (int[]) aryResult);
    }


    /**
     * Example to call void returning method.
     * <p/>
     * Note: Return value will be null for void methods.
     */
    @Test
    public void testCallVoidReturnMethod() {

        Object voidResult = callMethod(test, "voidMethod",
                String.class, "foobar");
        assertNull(voidResult);
    }
}
