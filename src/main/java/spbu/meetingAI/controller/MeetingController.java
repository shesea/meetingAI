package spbu.meetingAI.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeetingController {

    @GetMapping("/")
    public String index() {
        return "Hello!";
    }
}
