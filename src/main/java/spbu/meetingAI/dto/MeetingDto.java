package spbu.meetingAI.dto;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import spbu.meetingAI.entity.Meeting;

public class MeetingDto {
    public UUID id;

    public String title;

    public LocalDateTime createdAt;

    public LocalDateTime updatedAt;

    public LocalDateTime startTime;

    public Duration duration;

    public String description;

    public String transcript;

    public String summary;

    public List<String> participants;

    public List<String> keyWords;

    public UUID userId;

    public static MeetingDto convertFromModel(Meeting meeting) {
        var dto = new MeetingDto();
        dto.id = meeting.getId();
        dto.title = meeting.getTitle();
        dto.createdAt = meeting.getCreatedAt();
        dto.updatedAt = meeting.getUpdatedAt();
        dto.startTime = meeting.getStartTime();
        dto.duration = meeting.getDuration();
        dto.description = meeting.getDescription();
        dto.transcript = meeting.getTranscript();
        dto.summary = meeting.getSummary();
        dto.participants = meeting.getParticipants();
        dto.keyWords = meeting.getKeyWords();
        if (meeting.getCreatedBy() == null) {
            dto.userId = null;
        } else {
            dto.userId = meeting.getCreatedBy().getId();
        }
        return dto;
    }
}
