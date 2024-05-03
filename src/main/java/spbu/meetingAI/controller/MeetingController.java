package spbu.meetingAI.controller;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import spbu.meetingAI.dto.MeetingDto;
import spbu.meetingAI.service.MeetingService;

@RestController
@RequestMapping("/api/meeting")
public class MeetingController {
    private final MeetingService service;

    @Autowired
    public MeetingController(MeetingService service) {
        this.service = service;
    }

    @GetMapping("/{meetingId}")
    public CompletableFuture<MeetingDto> getMeetingById(
            @PathVariable("meetingId") UUID meetingId
    ) {
        return service.getMeeting(meetingId)
                .thenApply(MeetingDto::fromModel);
    }

    @PostMapping("/upload")
    public void handleFileUploadUsingCurl(
            @RequestParam("file") MultipartFile file) throws IOException, InterruptedException, URISyntaxException {

        System.out.println("Got file " + file.getSize());
        System.out.println("Content type is " + file.getContentType());

        service.uploadRecording(file);

    }

    @PutMapping("/{meetingId}")
    public CompletableFuture<MeetingDto> updateMeetingById(
            @RequestBody MeetingDto meetingDto
    ) {
        return service.updateMeeting(meetingDto).thenApply(MeetingDto::fromModel);
    }
}
