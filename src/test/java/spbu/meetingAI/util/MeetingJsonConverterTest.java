package spbu.meetingAI.util;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MeetingJsonConverterTest {

    MeetingJsonConverter converter;

    @BeforeEach
    public void setUp() {
        converter = new MeetingJsonConverter();
    }

    @Test
    void convertToDatabaseColumn() {
        var list = List.of("key1", "key2", "key3");
        var column = converter.convertToDatabaseColumn(list);
        assertEquals(column, "[\"key1\",\"key2\",\"key3\"]");
    }

    @Test
    void convertToEntityAttribute() {
        var json = "[\"key1\", \"key2\", \"key3\"]";
        var list = converter.convertToEntityAttribute(json);
        assertEquals(list, List.of("key1", "key2", "key3"));
    }
}