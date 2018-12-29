package com.richslide.atesearch.business.helper;

import java.util.Objects;

public class CommonsUtil {

    public static <T> T parsePrimitiveWrapper(final Class<T> clazz, final String value) {
        final String val = Objects.requireNonNull(value);
        Object result = val;
        if (Boolean.class == clazz) result = Boolean.parseBoolean(val);
        if (Byte.class == clazz)    result = Byte.parseByte(val);
        if (Short.class == clazz)   result = Short.parseShort(val);
        if (Integer.class == clazz) result = Integer.parseInt(val);
        if (Long.class == clazz)    result = Long.parseLong(val);
        if (Float.class == clazz)   result = Float.parseFloat(val);
        if (Double.class == clazz)  result = Double.parseDouble(val);
        return clazz.cast(result);
    }

    public static String convertCamelCase(final String string, final String prefix, final String suffix) {
        if (Objects.nonNull(prefix) && prefix.trim().length() > 0)
            return convertCamelCase(string, true, prefix, suffix);
        else
            return convertCamelCase(string, false, prefix, suffix);
    }

    public static String convertCamelCase(final String string, final boolean startsUpper
            , final String prefix, final String suffix) {

        if (Objects.isNull(string)) return null;

        StringBuilder builder = new StringBuilder();

        // Append prefix
        if (Objects.nonNull(prefix) && prefix.trim().length() > 0)
            builder.append(prefix.trim());

        // Add converted
        final String[] trimmed = string.trim().split(" ");
        if (startsUpper)
            builder.append(trimmed[0].substring(0, 1).toUpperCase());
        else
            builder.append(trimmed[0].substring(0, 1).toLowerCase());
        builder.append(trimmed[0].substring(1));

        if (trimmed.length > 1) {
            for (int i = 1; i < trimmed.length; i++) {
                builder.append(trimmed[i].substring(0, 1).toUpperCase());
                builder.append(trimmed[i].substring(1));
            }
        }

        // Append suffix
        if (Objects.nonNull(suffix) && suffix.trim().length() > 0)
            builder.append(suffix.trim());

        return builder.toString();
    }
}
