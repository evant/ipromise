package me.tatarka.ipromise.compat;

public class Objects {
    public static boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));

    }

    public static int hashCode(Object o) {
        return o != null ? o.hashCode() : 0;
    }
}
