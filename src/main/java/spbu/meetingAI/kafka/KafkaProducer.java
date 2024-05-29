package spbu.meetingAI.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import spbu.meetingAI.util.OperationType;

@Service
public class KafkaProducer {
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public KafkaProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void uploadFile(String topic, String groupId, String id, long fileSize, OperationType operationType, String operationId) {
        try {
            logger.info("Sending data to kafka = '{}' with topic '{}'", id, topic);
            ObjectMapper mapper = new ObjectMapper();
            CreateMeetingRequest request = new CreateMeetingRequest(id, fileSize, operationType, operationId);
            kafkaTemplate.send(topic, groupId, mapper.writeValueAsString(request));
        } catch (Exception e) {
            logger.error("An error occurred! '{}'", e.getMessage());
        }
    }
}
