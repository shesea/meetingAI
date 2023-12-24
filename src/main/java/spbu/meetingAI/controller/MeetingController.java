package spbu.meetingAI.controller;

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
            @PathVariable("meetingId") long meetingId
    ) {
        return service.getMeeting(meetingId)
                .thenApply(MeetingDto::convertFromModel);
    }
}
