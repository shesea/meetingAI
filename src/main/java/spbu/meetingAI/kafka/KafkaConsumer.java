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
            FileInfo info = mapper.readValue(fileInfoJson, FileInfo.class);
            CompletableFuture.runAsync(() -> {
                try {
                    meetingService.createMeeting(info.id(), info.fileSize());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).thenAccept(ignored ->
                    logger.info("Success process meeting '{}' with topic '{}'", info.id(), "meeting-ai.kafka.post.meeting"));
        } catch (Exception e){
            logger.error("An error occurred! '{}'", e.getMessage());
        }
    }
}
