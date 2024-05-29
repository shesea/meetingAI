package spbu.meetingAI.kafka;

import spbu.meetingAI.util.OperationType;

public record CreateMeetingRequest(String id, long fileSize, OperationType operationType, String operationId) {
}
