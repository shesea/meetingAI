package spbu.meetingAI.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import spbu.meetingAI.entity.Meeting;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {
}