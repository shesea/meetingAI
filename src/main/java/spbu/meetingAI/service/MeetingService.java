package spbu.meetingAI.service;

import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import spbu.meetingAI.dto.MeetingDto;
import spbu.meetingAI.entity.Meeting;
import spbu.meetingAI.repository.MeetingRepository;
import spbu.meetingAI.util.GeneratedTextParser;

@Service
public class MeetingService {
    private static final Logger logger
            = LoggerFactory.getLogger(MeetingService.class);

    public static final String BUCKET_NAME = "mediafiles";
    private static final String EXTENSION = ".pcm";
    private static final String S3_ENDPOINT = "storage.yandexcloud.net";
    private static final String TRANSCRIPTION_ENDPOINT = "https://transcribe.api.cloud.yandex.net/speech/stt/v2/longRunningRecognize";
    private static final String OPERATION_RESULT_ENDPOINT = "https://operation.api.cloud.yandex.net/operations/";
    private static final String COMPLETION_ENDPOINT = "https://llm.api.cloud.yandex.net/foundationModels/v1/completionAsync";
    private static final String LANGUAGE_MODEL_URI = "gpt://b1gsskjdo0qgt9m8g39i/yandexgpt/latest";

    final static HttpClient client = HttpClient.newHttpClient();
    final static ObjectMapper mapper = new ObjectMapper();

    MeetingRepository meetingRepository;

    @Autowired
    public MeetingService(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }

    public void createMeeting(String id, long fileSize) throws IOException, URISyntaxException, InterruptedException {
        Meeting meeting = new Meeting(UUID.fromString(id));
        meeting.setCreatedAt(LocalDateTime.now());
        meeting.setDuration(Duration.of(fileSize / 64_000, ChronoUnit.SECONDS));
        logger.info("Created meeting entity with id {} with createdAt {} and duration {}", meeting.getId(), meeting.getCreatedAt(), meeting.getDuration());
        meetingRepository.save(meeting);

        String operationId = sendToRecognition(meeting);
        String transcript = getTranscript(operationId);
        logger.info("Setting transcript in meeting {}", meeting.getId());
        meeting.setTranscript(transcript);

        logger.info("Creating summary for meeting {}", meeting.getId());
        String summary = getGeneratedValue("Напиши краткое содержание текста от первого лица", transcript);
        logger.info("Setting summary in meeting {}", meeting.getId());
        meeting.setSummary(summary);

        logger.info("Creating key words for meeting {}", meeting.getId());
        String keyWords = getGeneratedValue("Выдели до 10 ключевых слов и словосочетаний из текста", transcript);
        logger.info("Setting key words in meeting {}", meeting.getId());
        meeting.setKeyWords(GeneratedTextParser.getListValues(keyWords, true));

        logger.info("Creating description for meeting {}", meeting.getId());
        String description = getGeneratedValue("Опиши полученный текст в одном предложении, ни за что не используй markdown", transcript);
        logger.info("Setting description in meeting {}", meeting.getId());
        meeting.setDescription(description);

        logger.info("Creating title for meeting {}", meeting.getId());
        String title = getGeneratedValue("Придумай короткое название для текста без кавычек", transcript);
        logger.info("Settings title in meeting {}", meeting.getId());
        meeting.setTitle(GeneratedTextParser.removeExcessChars(title));

        logger.info("Creating quotes for meeting {}", meeting.getId());
        String quotes = getGeneratedValue("Выдели из текста от двух до четырех цитат длиной до 30 слов. Между всеми цитатами ставь ровно один перевод строки, нумеруй каждую цитату", transcript);
        logger.info("Setting quotes in meeting {}", meeting.getId());
        meeting.setQuotes(GeneratedTextParser.getListValues(quotes, false));

        logger.info("Saving meeting {}", meeting.getId());
        meetingRepository.save(meeting);
    }

    public CompletableFuture<Meeting> getMeeting(UUID id) {
        Meeting meeting = meetingRepository.findById(id).orElse(null);
        if (meeting == null) {
            throw new RuntimeException("Meeting not found with id: " + id);
        }
        return CompletableFuture.completedFuture(meeting);
        //TODO add handle
    }

    public CompletableFuture<Meeting> updateMeeting(MeetingDto dto) {
        Meeting meeting = meetingRepository.findById(UUID.fromString(dto.id)).orElse(null);
        if (meeting == null) {
            throw new RuntimeException("Meeting not found with id: " + UUID.fromString(dto.id));
        }
        meeting.setCustomSummary(dto.customSummary);
        Meeting updatedMeeting = meetingRepository.save(meeting);
        logger.info("Successfully updated meeting {}", meeting.getId());
        return CompletableFuture.completedFuture(updatedMeeting);
    }

    public void deleteMeeting(UUID id) {
        logger.info("Deleting meeting {}", id);
        meetingRepository.deleteById(id);
        logger.info("Successfully deleted meeting {}", id);
    }

    private String sendToRecognition(Meeting meeting) throws URISyntaxException, IOException, InterruptedException {
        String json = new JSONObject()
                .put("config", new JSONObject()
                        .put("specification", new JSONObject()
                                .put("languageCode", "ru-RU")
                                .put("model", "general:rc")
                                .put("audioEncoding", "LINEAR16_PCM")
                                .put("sampleRateHertz", 16000)
                                .put("audioChannelCount", 2)
                                .put("literature_text", true)))
                .put("audio", new JSONObject()
                        .put("uri", "https://" + S3_ENDPOINT + "/" + BUCKET_NAME + "/" + meeting.getId() + EXTENSION))
                .toString();

        var request = HttpRequest.newBuilder()
                .uri(new URI(TRANSCRIPTION_ENDPOINT))
                .headers("Authorization", "Api-Key " + System.getenv("API_KEY"), "Content-Type", "application/json")
                .version(HttpClient.Version.HTTP_1_1)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        var operationId = "";

        logger.info("Sending meeting {} to recognition", meeting.getId());
        var response = client.send(request, HttpResponse.BodyHandlers.ofLines());
        logger.info("Got response code: {}", response.statusCode());
        var body = response.body();
        //TODO add status code handling
        var idString = body.filter(s -> s.contains("id")).findFirst();
        if (idString.isPresent()) {
            var temp = idString.get();
            operationId = temp.substring(8, temp.length() - 2);
            System.out.println(operationId);
        }
        logger.info("Got operation id: {}", operationId);
        return operationId;
    }

    private String getTranscript(String operationId) throws URISyntaxException, InterruptedException, IOException {
        var request = HttpRequest.newBuilder()
                .uri(new URI(OPERATION_RESULT_ENDPOINT + operationId))
                .headers("Authorization", "Api-Key " + System.getenv("API_KEY"))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        boolean found = false;
        var str = "";
        while (!found) {
            Thread.sleep(10000);
            logger.info("Sending request to get operation: {}", operationId);
            str = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
            found = str.contains("\"done\": true,");
        }
        logger.info("Received 'done' status in operation {}", operationId);

        StringBuilder transcript = new StringBuilder();
        ObjectMapper mapper = new ObjectMapper();

        JsonNode root = mapper.readTree(str);
        var chunks = root.get("response").get("chunks");
        if (chunks == null) {
            logger.info("Got no chunks in operation {}, returning empty transcript", operationId);
            return "";
        }
        for (JsonNode chunk: chunks) {
            var channel = chunk.get("channelTag").asInt();
            if (channel != 1) {
                continue;
            }
            var alternatives = chunk.get("alternatives");
            if (alternatives.isEmpty()) {
                logger.info("Got no alternatives in chunk: {}", chunk);
            } else {
                if (alternatives.size() > 1) {
                    logger.info("Got more than one alternative in chunk: {}", chunk);
                }
                var text = alternatives.get(0).get("text").textValue();
                if (!transcript.isEmpty()) {
                    transcript.append(" ");
                }
                transcript.append(text);
            }
        }
        logger.info("Successfully received transcript: {}", transcript);
        return transcript.toString();
    }

    private String getGeneratedValue(String command, String transcript) throws URISyntaxException, IOException, InterruptedException {
        var response = sendGptRequest(command, transcript);
        System.out.println(response.statusCode());
        var body = response.body();
        //TODO add status code handling
        var operationId = getOperationId(body);

        if (operationId.isEmpty()) {
            return "";
        }

        var responseMessage = getCompleteOperation(operationId);
        JsonNode root = mapper.readTree(responseMessage);
        String summary = root.get("response").get("alternatives").get(0).get("message").get("text").textValue();

        System.out.println(summary);
        return summary;
    }

    private HttpResponse<String> sendGptRequest(String command, String transcript) throws URISyntaxException, IOException, InterruptedException {
        String json = new JSONObject()
                .put("modelUri", LANGUAGE_MODEL_URI)
                .put("completionOptions", new JSONObject()
                        .put("stream", true)
                        .put("temperature", 0)
                        .put("maxTokens", 2000))
                .put("messages", new JSONArray()
                        .put(new JSONObject()
                                .put("role", "system")
                                .put("text", command))
                        .put(new JSONObject()
                                .put("role", "user")
                                .put("text", transcript)))
                .toString();

        var request = HttpRequest.newBuilder()
                .uri(new URI(COMPLETION_ENDPOINT))
                .headers("Authorization", "Api-Key " + System.getenv("API_KEY"), "Content-Type", "application/json")
                .version(HttpClient.Version.HTTP_1_1)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();


        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String getOperationId(String body) {
        var operationId = "";
        var idString = Arrays.stream(body.split(",")).filter(s -> s.contains("id")).findFirst();
        if (idString.isPresent()) {
            var temp = idString.get();
            operationId = temp.substring(7, temp.length() - 1);
            System.out.println(operationId);
        }
        return operationId;
    }

    private String getCompleteOperation(String operationId) throws URISyntaxException, InterruptedException, IOException {
        var request = HttpRequest.newBuilder()
                .uri(new URI(OPERATION_RESULT_ENDPOINT + operationId))
                .headers("Authorization", "Api-Key " + System.getenv("API_KEY"))
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        boolean found = false;
        var str = "";
        while (!found) {
            Thread.sleep(10000);
            str = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
            found = str.contains("\"done\": true,");
        }
        return str;
    }
}
