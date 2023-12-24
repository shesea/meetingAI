package spbu.meetingAI.dto;

import java.util.List;

import spbu.meetingAI.entity.Meeting;

public class MeetingDto {
    public long id;

    public String title;

    public List<String> keyWords;

    public static MeetingDto convertFromModel(Meeting meeting) {
        var dto = new MeetingDto();
        dto.id = meeting.getId();
        dto.title = meeting.getTitle();
        dto.keyWords = meeting.getKeyWords();
        return dto;
    }
}
