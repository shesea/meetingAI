package spbu.meetingAI.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class GeneratedTextParserTest {

    @Test
    void keyWordsTest() {
        String generatedText = "1. Слово слово 1.\n2. Слово слово 2.";
        List<String> parsedText = GeneratedTextParser.getListValues(generatedText, true);
        Assertions.assertEquals(parsedText, List.of("слово слово 1", "слово слово 2"));

        generatedText = "1.\t\tСлово.\n2.\t\tслово.";
        parsedText = GeneratedTextParser.getListValues(generatedText, true);
        Assertions.assertEquals(parsedText, List.of("слово", "слово"));

        generatedText = "Слово";
        parsedText = GeneratedTextParser.getListValues(generatedText, true);
        Assertions.assertEquals(parsedText, List.of("слово"));

        generatedText = "Слово.  ";
        parsedText = GeneratedTextParser.getListValues(generatedText, true);
        Assertions.assertEquals(parsedText, List.of("слово"));

        generatedText = "слово\n\n\nслово 2\n   слово 3.";
        parsedText = GeneratedTextParser.getListValues(generatedText, true);
        Assertions.assertEquals(parsedText, List.of("слово", "слово 2", "слово 3"));
    }

    @Test
    void quotesTest() {
        String generatedText = "1. «Цитата 1».\n2. «Цитата 2»";
        List<String> parsedText = GeneratedTextParser.getListValues(generatedText, false);
        Assertions.assertEquals(parsedText, List.of("«Цитата 1»", "«Цитата 2»"));
    }

    @Test
    void titleTest() {
        String generatedText = "«Название придумал».";
        String parsedText = GeneratedTextParser.removeExcessChars(generatedText);
        Assertions.assertEquals(parsedText, "Название придумал");

        generatedText = "Название придумал";
        parsedText = GeneratedTextParser.removeExcessChars(generatedText);
        Assertions.assertEquals(parsedText, "Название придумал");

        generatedText = "«Название придумал»";
        parsedText = GeneratedTextParser.removeExcessChars(generatedText);
        Assertions.assertEquals(parsedText, "Название придумал");
    }
}
