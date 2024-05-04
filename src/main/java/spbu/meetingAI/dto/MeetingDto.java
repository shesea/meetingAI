package spbu.meetingAI.dto;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import spbu.meetingAI.entity.Meeting;

public class MeetingDto {
    public boolean done;

    public String id;

    public String title;

    public LocalDateTime createdAt;

    public LocalDateTime updatedAt;

    public LocalDateTime startTime;

    public Duration duration;

    public String description;

    public String transcript;

    public String summary;

    public String customSummary;

    public List<String> participants;

    public List<String> keyWords;

    public List<String> quotes;

    public URL videoLink;

    public String userId;

    public static MeetingDto fromModel(Meeting meeting) {
        var dto = new MeetingDto();
        dto.id = meeting.getId().toString();
        dto.title = meeting.getTitle();
        dto.createdAt = meeting.getCreatedAt();
        dto.updatedAt = meeting.getUpdatedAt();
        dto.startTime = meeting.getStartTime();
        dto.duration = meeting.getDuration();
        dto.description = meeting.getDescription();
        dto.transcript = meeting.getTranscript();
        dto.summary = meeting.getSummary();
        dto.customSummary = meeting.getCustomSummary();
        dto.participants = meeting.getParticipants();
        dto.keyWords = meeting.getKeyWords();
        dto.quotes = meeting.getQuotes();
        dto.videoLink = meeting.getVideoLink();
        if (meeting.getCreatedBy() == null) {
            dto.userId = null;
        } else {
            dto.userId = meeting.getCreatedBy().getId().toString();
        }
        dto.done = dto.summary != null && dto.keyWords != null && dto.quotes != null && dto.description != null && dto.title != null;
        return dto;
    }
}
