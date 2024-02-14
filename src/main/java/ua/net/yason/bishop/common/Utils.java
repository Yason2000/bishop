package ua.net.yason.bishop.common;

import lombok.experimental.UtilityClass;

import java.text.DecimalFormat;
import java.util.Formatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class Utils {
    private final DecimalFormat decimalFormat = new DecimalFormat("0.00");

    public static String toString(double value) {
        return decimalFormat.format(value);
    }

    public static String bytesToHexPrintString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);
        for (int i = 0; i < bytes.length; i++) {
            formatter.format("%02X", bytes[i]);
            if ((i + 1) % 16 == 0) {
                sb.append("\n");
            } else {
                sb.append(" ");
            }
        }
        formatter.close();
        return sb.toString();
    }

    public static Optional<String> findSubstring(String input, Pattern pattern) {
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

    public static Optional<Double> findDouble(String input, Pattern pattern) {
        return findSubstring(input, pattern)
                .map(Double::parseDouble);
    }
}
