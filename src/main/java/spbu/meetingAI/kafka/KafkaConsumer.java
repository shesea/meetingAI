package spbu.meetingAI.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import spbu.meetingAI.service.MeetingService;

import java.util.concurrent.CompletableFuture;

@Component
@Transactional
public class KafkaConsumer {
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);

    private final MeetingService meetingService;

    @Autowired
    public KafkaConsumer(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @KafkaListener(topics = "meeting-ai.kafka.post.meeting", groupId = "meetings", concurrency = "4")
    public void processUploadMeeting(String fileInfoJson) {
        logger.info("Received file = '{}'", fileInfoJson);
        try {
            ObjectMapper mapper = new ObjectMapper();
            CreateMeetingRequest request = mapper.readValue(fileInfoJson, CreateMeetingRequest.class);
            CompletableFuture.runAsync(() -> {
                try {
                    meetingService.createMeeting(request.id(), request.fileSize(), request.operationType(), request.operationId());
                } catch (Exception e) {
                    logger.error("Got exception '{}' while processing meeting '{}'", e.getMessage(), request.id());
                }
            }).thenAccept(ignored ->
                    logger.info("Processed meeting '{}' with topic '{}'", request.id(), "meeting-ai.kafka.post.meeting"));
        } catch (Exception e){
            logger.error("An error occurred! '{}'", e.getMessage());
        }
    }
}
