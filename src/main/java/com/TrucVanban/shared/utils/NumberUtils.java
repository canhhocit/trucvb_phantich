package com.TrucVanban.shared.utils;

public class NumberUtils {
    public static boolean isNullOrNegative(Long number) {
        return number == null || number < 0;
    }

    public static boolean isNullOrNegative(Integer number) {
        return number == null || number < 0;
    }
}
