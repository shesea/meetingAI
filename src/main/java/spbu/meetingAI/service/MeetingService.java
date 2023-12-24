package spbu.meetingAI.service;

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import spbu.meetingAI.entity.Meeting;
import spbu.meetingAI.repository.MeetingRepository;

@Service
public class MeetingService {
    MeetingRepository meetingRepository;

    @Autowired
    public MeetingService(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }

    public CompletableFuture<Meeting> getMeeting(long id) {
        return CompletableFuture.completedFuture(meetingRepository.findById(id).orElseThrow());
                //TODO add handle
    }
}
