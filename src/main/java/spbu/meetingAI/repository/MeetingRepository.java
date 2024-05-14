package spbu.meetingAI.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import spbu.meetingAI.entity.Meeting;

import java.util.Optional;
import java.util.UUID;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    Optional<Meeting> findById(UUID id);

    @Transactional
    void deleteById(UUID id);
}