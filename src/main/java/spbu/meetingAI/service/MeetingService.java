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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import spbu.meetingAI.dto.MeetingDto;
import spbu.meetingAI.entity.Meeting;
import spbu.meetingAI.kafka.KafkaProducer;
import spbu.meetingAI.repository.MeetingRepository;
import spbu.meetingAI.util.GeneratedTextParser;

@Service
public class MeetingService {
    private static final Logger logger
            = LoggerFactory.getLogger(MeetingService.class);

    private final KafkaProducer producer;

    @Value("${spring.kafka.consumer.group-id}")
    String groupId;

    @Value("${meetings.kafka.post.meeting}")
    String postMeetingTopic;

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
    public MeetingService(KafkaProducer producer, MeetingRepository meetingRepository) {
        this.producer = producer;
        this.meetingRepository = meetingRepository;
    }

    public void createMeeting(String id, long fileSize) throws IOException, URISyntaxException, InterruptedException {
        Meeting meeting = meetingRepository.findById(UUID.fromString(id)).orElse(null);
        if (meeting == null) {
            meeting = new Meeting(UUID.fromString(id));
            meeting.setCreatedAt(LocalDateTime.now());
            meeting.setDuration(Duration.of(fileSize / 64_000, ChronoUnit.SECONDS));
            logger.info("Created meeting entity with id {} with createdAt {} and duration {}", meeting.getId(), meeting.getCreatedAt(), meeting.getDuration());
            meetingRepository.save(meeting);

            String operationId = sendToRecognition(meeting);
            String transcript = getTranscript(operationId);
            logger.info("Setting transcript in meeting {}", meeting.getId());
            meeting.setTranscript(transcript);
            meetingRepository.save(meeting);
        }

        logger.info("Creating summary for meeting {}", meeting.getId());
        String summary = getGeneratedValue("Напиши краткое содержание текста от первого лица", meeting.getTranscript(), meeting);
        if (summary.isEmpty()) {
            logger.warn("Creating summary failed in meeting {}", meeting.getId());
            return;
        }
        logger.info("Setting summary: '{}' in meeting {}", summary, meeting.getId());
        meeting.setSummary(summary);

        logger.info("Creating key words for meeting {}", meeting.getId());
        String keyWords = getGeneratedValue("Выдели до 10 ключевых слов и словосочетаний из текста", meeting.getTranscript(), meeting);
        if (keyWords.isEmpty()) {
            logger.warn("Creating key words failed in meeting {}", meeting.getId());
            return;
        }
        List<String> parsedKeyWords = GeneratedTextParser.getListValues(keyWords, true);
        logger.info("Setting key words: '{}' in meeting {}", parsedKeyWords, meeting.getId());
        meeting.setKeyWords(parsedKeyWords);

        logger.info("Creating description for meeting {}", meeting.getId());
        String description = getGeneratedValue("Опиши полученный текст в одном предложении, ни за что не используй markdown", meeting.getTranscript(), meeting);
        if (description.isEmpty()) {
            logger.warn("Creating description failed in meeting {}", meeting.getId());
            return;
        }
        String parsedDescription = GeneratedTextParser.removeExcessChars(description);
        logger.info("Setting description: '{}' in meeting {}", parsedDescription, meeting.getId());
        meeting.setDescription(parsedDescription);

        logger.info("Creating title for meeting {}", meeting.getId());
        String title = getGeneratedValue("Придумай короткое название для текста без кавычек", meeting.getTranscript(), meeting);
        if (title.isEmpty()) {
            logger.warn("Creating title failed in meeting {}", meeting.getId());
            return;
        }
        String parsedTitle = GeneratedTextParser.removeExcessChars(title);
        logger.info("Settings title: '{}' in meeting {}", parsedTitle, meeting.getId());
        meeting.setTitle(parsedTitle);

        logger.info("Creating quotes for meeting {}", meeting.getId());
        String quotes = getGeneratedValue("Выдели из текста от двух до четырех цитат длиной до 30 слов. Между всеми цитатами ставь ровно один перевод строки, нумеруй каждую цитату", meeting.getTranscript(), meeting);
        if (quotes.isEmpty()) {
            logger.warn("Creating quotes failed in meeting {}", meeting.getId());
            return;
        }
        List<String> parsedQuotes = GeneratedTextParser.getListValues(quotes, false);
        logger.info("Setting quotes: '{}' in meeting {}", GeneratedTextParser.getListValues(quotes, false), meeting.getId());
        meeting.setQuotes(parsedQuotes);

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

    private String getGeneratedValue(String command, String transcript, Meeting meeting) throws URISyntaxException, IOException, InterruptedException {
        var response = sendGptRequest(command, transcript);
        var body = response.body();
        //TODO add status code handling
        var operationId = getOperationId(body);
        logger.info("Generation request has been sent with operation id: {}", operationId);

        if (operationId.isEmpty()) {
            return "";
        }

        var responseMessage = getCompleteOperation(operationId, meeting);
        if (responseMessage.isEmpty()) {
            return "";
        }
        JsonNode root = mapper.readTree(responseMessage);
        return root.get("response").get("alternatives").get(0).get("message").get("text").textValue();
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
        }
        return operationId;
    }

    private String getCompleteOperation(String operationId, Meeting meeting) throws URISyntaxException, InterruptedException, IOException {
        var request = HttpRequest.newBuilder()
                .uri(new URI(OPERATION_RESULT_ENDPOINT + operationId))
                .headers("Authorization", "Api-Key " + System.getenv("API_KEY"))
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        boolean found = false;
        var str = "";
        long startTime = System.currentTimeMillis();
        while (!found && System.currentTimeMillis() - startTime < 20000) {
            Thread.sleep(2000);
            str = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
            found = str.contains("\"done\": true,");
        }
        if (!found) {
            //producer.uploadFile(postMeetingTopic, groupId, meeting.getId().toString(), meeting.getDuration().getSeconds() * 64000);
            return "";
        }
        return str;
    }
}
