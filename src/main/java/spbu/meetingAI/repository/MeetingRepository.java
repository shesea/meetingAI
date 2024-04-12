package spbu.meetingAI.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import spbu.meetingAI.entity.Meeting;

import java.util.Optional;
import java.util.UUID;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    Optional<Meeting> findById(UUID id);
}