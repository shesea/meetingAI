package spbu.meetingAI.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import spbu.meetingAI.service.MeetingService;

@Component
@Transactional
public class KafkaConsumer {
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);

    private final MeetingService meetingService;

    @Autowired
    public KafkaConsumer(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @KafkaListener(topics = "test-meeting.kafka.post.meeting", groupId = "meetings")
    public void processUploadMeeting(String fileInfoJson) {
        logger.info("Received file = '{}'", fileInfoJson);
        try {
            ObjectMapper mapper = new ObjectMapper();
            FileInfo info = mapper.readValue(fileInfoJson, FileInfo.class);
            meetingService.createMeeting(info.id(), info.fileSize());
            logger.info("Success process meeting '{}' with topic '{}'", info.id(), "test-meeting.kafka.post.meeting");
        } catch (Exception e){
            logger.error("An error occurred! '{}'", e.getMessage());
        }
    }
}
