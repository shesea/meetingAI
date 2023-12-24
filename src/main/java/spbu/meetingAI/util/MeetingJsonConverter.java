package spbu.meetingAI.util;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.persistence.*;

@Converter(autoApply = true)
public class MeetingJsonConverter implements AttributeConverter<List<String>, String> {

    private final static ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<String> list) {
        try {
            ArrayNode root = mapper.createArrayNode();
            for (String value : list) {
                root.add(value);
            }
            return mapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            List<String> list = new ArrayList<>(root.size());
            for (JsonNode element : root) {
                list.add(element.asText());
            }
            return list;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
