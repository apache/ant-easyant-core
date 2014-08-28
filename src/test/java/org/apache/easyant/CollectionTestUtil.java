package org.apache.easyant;

public class CollectionTestUtil {
    private CollectionTestUtil() {
    }

    public static boolean containsClass(Iterable<?> iterable, Class<?> classToSearch) {
        for (Object o : iterable) {
            if (o.getClass() == classToSearch) {
                return true;
            }
        }
        return false;
    }
}
