package spbu.meetingAI.entity;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import spbu.meetingAI.util.MeetingJsonConverter;

@Entity
@Table(name = "meetings")
public class Meeting {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "title")
    private String title;

    @CreationTimestamp
    @Column(name = "createdAt")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;

    @Column(name = "startTime")
    private LocalDateTime startTime;

    @Column(name = "duration")
    private Duration duration;

    @Column(name = "description", columnDefinition = "varchar")
    private String description;

    @Column(name = "transcript", columnDefinition = "varchar")
    private String transcript;

    @Column(name = "summary", columnDefinition = "varchar")
    private String summary;

    @Column(name = "customSummary", columnDefinition = "varchar")
    private String customSummary;

    @Column(name = "participants", columnDefinition = "json")
    @Convert(converter = MeetingJsonConverter.class)
    @ColumnTransformer(write = "?::jsonb")
    private List<String> participants;

    @Column(name = "keyWords", columnDefinition = "json")
    @Convert(converter = MeetingJsonConverter.class)
    @ColumnTransformer(write = "?::jsonb")
    private List<String> keyWords;

    @Column(name = "quotes", columnDefinition = "json")
    @Convert(converter = MeetingJsonConverter.class)
    @ColumnTransformer(write = "?::jsonb")
    private List<String> quotes;

    @Transient
    private URL videoLink;

    @ManyToOne
    @JoinColumn(name = "createdBy")
    private User createdBy;

    public Meeting() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getCustomSummary() {
        return customSummary;
    }

    public void setCustomSummary(String customSummary) {
        this.customSummary = customSummary;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public List<String> getQuotes() {
        return quotes;
    }

    public void setQuotes(List<String> quotes) {
        this.quotes = quotes;
    }

    public URL getVideoLink() {
        return videoLink;
    }

    public void setVideoLink(URL videoLink) {
        this.videoLink = videoLink;
    }
}
