package spbu.meetingAI.util;

import java.util.ArrayList;
import java.util.List;

public class GeneratedTextParser {
    public static List<String> getListValues(String text, boolean lowerCase) {
        if (lowerCase) {
            text = text.toLowerCase();
        }
        List<String> tokens = new ArrayList<>();
        for (var token: text.split("\n")) {
            StringBuilder parsedStr = new StringBuilder();
            for (int i = 0; i < token.length(); i++) {
                token = token.trim();
                char c = token.charAt(i);
                if (c == '\n') {
                    if (!parsedStr.isEmpty()) {
                        tokens.add(parsedStr.toString());
                    }
                    parsedStr.delete(0, parsedStr.length());
                }

                if (c != '\t' && !(parsedStr.isEmpty() &&
                        ((c >= '0' && c <= '9') || c == ' ' || c == '.')) &&
                        !((c == '.' || c == '!') && (i == token.length() - 1 || token.charAt(i + 1) == '\n'))) {
                    parsedStr.append(c);
                }
            }
            if (!parsedStr.isEmpty()) {
                tokens.add(parsedStr.toString());
            }
        }
        return tokens;
    }

    public static String removeExcessChars(String text) {
        if (text.charAt(0) == '«') {
            if (text.charAt(text.length() - 1) == '.') {
                return text.substring(1, text.length() - 2);
            }
            return text.substring(1, text.length() - 1);
        }
        return text;
    }
}
