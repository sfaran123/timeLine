package com.listeners;

import com.Reader;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class StateMachineListener implements InitializingBean {

    static String decision = ".*\\[Decision] '(?<decision>.*)', route '(?<route>.*)', action id '(?<actionId>.*)'";
    static Pattern decisionPattern = Pattern.compile(decision);

    String redisAiMessages = ".* Received Redis pubSub event from topic 'ai_connector'.* payload: '(.*)'";
    Pattern redisAiMessagesPattern = Pattern.compile(redisAiMessages);

    String flowName = ".* Sending flow logging event .* type='(?<type>.*)', flowName='(?<flowName>.*)'.*actionId='(?<actionId>.*)', eventId='(?<eventId>.*)', barcode='(?<barcode>.*)', sourceCameras='(?<sourceCameras>\\d+)'.*";
    Pattern flowNamePattern = Pattern.compile(flowName);

    String redisAppMessages = ".* Received Redis pubSub event from topic 'app_handler_connector'.* payload: '(.*)'";
    Pattern redisAppMessagesPattern = Pattern.compile(redisAppMessages);

    private final Reader reader;

    private final ObjectMapper mapper = new ObjectMapper();

    public StateMachineListener(Reader reader) {
        this.reader = reader;

    }


    Optional<String> lastEventId = Optional.empty();

    @SneakyThrows
    private Optional<RedisData> parse(String line) {
        Matcher matcher = redisAiMessagesPattern.matcher(line);
        if (matcher.find()) {
            RedisData value = mapper.readValue(matcher.group(1), RedisData.class);
            lastEventId = Optional.of(value.getEventId()); //TODO tmp
            return Optional.of(value);
        } else if (line.contains("[Decision]")) { //TODO - add more accurate details when this happend
            matcher = decisionPattern.matcher(line);
            if (matcher.find()) {
                String decision = matcher.group("decision");
                String actionId = matcher.group("actionId");
                String route = matcher.group("route");
                String eventId = lastEventId.orElseThrow();
                return Optional.of(new RedisData().setEventId(eventId).setActionId(actionId).setEventType("decision")
                        .addProperty("decision", decision)
                        .addProperty("route",route));
            }
        } else if (line.matches(flowName)) {
            matcher = flowNamePattern.matcher(line);
            if (matcher.find()) {

                String eventId = matcher.group("eventId");
                String actionId = matcher.group("actionId");
                String barcode = matcher.group("barcode");
                String flowName = matcher.group("flowName");
                String type = matcher.group("type");
                int sourceCameras = Integer.parseInt(matcher.group("sourceCameras"));
                return Optional.of(new RedisData().setEventId(eventId).setActionId(actionId).setEventType("flowName")
                        .addProperty("flowName", flowName)
                        .addProperty("type", type)
                        .addProperty("barcode", barcode)
                        .addProperty("sourceCameras", sourceCameras));
            }
        } else if (line.matches(redisAppMessages)) {
            matcher = redisAppMessagesPattern.matcher(line);
            if (matcher.find()) {
                RedisData data = mapper.readValue(matcher.group(1), RedisData.class);
                data.setEventType("barcodeScanner");
                return Optional.of(data);
            }
        }
        return Optional.empty();
    }

    Map<String, List<RedisData>> eventData = new HashMap<>();
    List<String> events = new ArrayList<>();

    @Override
    public void afterPropertiesSet() throws Exception {
//        Executors.newFixedThreadPool(1).execute(() -> {
//            this.reader.start(line -> {
//                parse(line).filter(x->x.getEventId() != null).ifPresent(d -> {
//                    System.out.println(d);
//                    List<RedisData> list = eventData.computeIfAbsent(d.getKey(), k -> {
//                        events.add(k);
//                        return new ArrayList<>();
//                    });
//                    list.add(d);
//                });
//            });
//        });
    }


    public List<RedisData> getLastEventData() {
        if (eventData.isEmpty()) return null;
        return eventData.get(events.get(events.size() - 1));
    }

    public Map<String, List<RedisData>> getAll() {
        return eventData;
    }

    public List<RedisData> getByEventId(String eventId) {
        return eventData.get(eventId);
    }
}
