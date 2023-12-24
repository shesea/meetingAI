package spbu.meetingAI.entity;

import java.util.List;

import jakarta.persistence.*;
import spbu.meetingAI.util.MeetingJsonConverter;

@Entity
@Table(name = "meeting")
public class Meeting {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(name = "title")
    private String title;

    @Column(name = "keyWords", columnDefinition = "json")
    @Convert(converter = MeetingJsonConverter.class)
    private List<String> keyWords;

    public Meeting() {
    }

    public Meeting(long id, String title, List<String> keyWords) {
        this.id = id;
        this.title = title;
        this.keyWords = keyWords;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getKeyWords() {
        return keyWords;
    }

    public void setKeyWords(List<String> keyWords) {
        this.keyWords = keyWords;
    }
}
