package spbu.meetingAI.controller;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import spbu.meetingAI.dto.MeetingDto;
import spbu.meetingAI.kafka.KafkaConsumer;
import spbu.meetingAI.kafka.KafkaProducer;
import spbu.meetingAI.service.MeetingService;
import spbu.meetingAI.service.RecordingService;
import spbu.meetingAI.util.OperationType;

@RestController
@CrossOrigin(origins = "https://84.201.175.36")
@RequestMapping("/api/meeting")
public class MeetingController {
    private static final Logger logger
            = LoggerFactory.getLogger(MeetingController.class);

    private final RecordingService recordingService;

    private final MeetingService meetingService;

    private final KafkaProducer producer;

    @Value("${spring.kafka.consumer.group-id}")
    String groupId;

    @Value("${meetings.kafka.post.meeting}")
    String postMeetingTopic;

    @Autowired
    public MeetingController(RecordingService recordingService, MeetingService meetingService, KafkaProducer producer, KafkaConsumer consumer) {
        this.recordingService = recordingService;
        this.meetingService = meetingService;
        this.producer = producer;
    }

    @PostMapping("/upload")
    public String createMeeting(
            @RequestParam("file") MultipartFile file) {
        logger.info("Received upload file request with {} size", file.getSize());
        try {
            String id = recordingService.uploadRecording(file).toString();
            logger.info("Generated id: {}", id);
            producer.uploadFile(postMeetingTopic, groupId, id, file.getSize(), OperationType.NONE, "");
            return id;
        } catch (Exception e){
            System.out.println("Got error: " + e.getMessage());
        }

        return "";
    }

    @GetMapping("/{meetingId}")
    public CompletableFuture<MeetingDto> getMeetingById(
            @PathVariable("meetingId") UUID meetingId
    ) {
        return meetingService.getMeeting(meetingId)
                .thenApply(recordingService::getRecordingLink)
                .thenApply(MeetingDto::fromModel);
    }

    @PutMapping("/{meetingId}")
    public CompletableFuture<MeetingDto> updateMeetingById(
            @RequestBody MeetingDto meetingDto
    ) {
        return meetingService.updateMeeting(meetingDto)
                .thenApply(recordingService::getRecordingLink)
                .thenApply(MeetingDto::fromModel);
    }

    @DeleteMapping("/{meetingId}")
    public void deleteMeetingById(
            @PathVariable("meetingId") UUID meetingId
    ) {
        meetingService.deleteMeeting(meetingId);
        recordingService.deleteRecording(meetingId);
    }
}
