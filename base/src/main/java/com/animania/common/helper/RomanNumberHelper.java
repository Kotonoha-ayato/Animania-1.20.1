package com.animania.common.helper;

/** Roman numeral formatter used by legacy manual/status presentation. */
public final class RomanNumberHelper {
    private static final int[] VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    private RomanNumberHelper() { }

    public static String toRoman(int number) {
        if (number <= 0) throw new IllegalArgumentException("Roman numerals require a positive integer");
        StringBuilder result = new StringBuilder();
        int remaining = number;
        for (int index = 0; index < VALUES.length; index++) {
            while (remaining >= VALUES[index]) {
                result.append(SYMBOLS[index]);
                remaining -= VALUES[index];
            }
        }
        return result.toString();
    }
}
