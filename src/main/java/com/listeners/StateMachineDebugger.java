package com.listeners;

import com.Reader;
import com.dto.Queued;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class StateMachineDebugger implements InitializingBean {

    static String testString = "Sep 07 11:04:48 master-cart5 java[23999]: 2022-09-07 11:04:48.439 23999 [pool-1-thread-1     " +
            "     ] gers.events.ai.strategy.SynchronizedAiEventStrategy INFO  : Handling event of type [BARCODE_LINE] with action_id [1662548684909_50] and event_id [1662548686035_37]: queued at [1662548687237] for [1075] ms. Started at [1662548688312], ended at [1662548688438], and processed in [126]";
    static String regex = "Handling event of type \\[(.*)] with action_id \\[(.*)] and event_id \\[(.*)]: queued at \\[(.*)] for \\[.*] ms. Started at \\[(.*)], ended at \\[(.*)], and processed in \\[.*]";

    static String decision = "(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}.\\d{3}).*\\[Decision](.*)";
    static Pattern decisionPattern = Pattern.compile(decision);


    static SimpleDateFormat sm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    static Pattern pattern = Pattern.compile(regex);

    private final Reader reader;

    public StateMachineDebugger(Reader reader) {
        this.reader = reader;

    }
//
//    @SneakyThrows
//    public static void main(String[] args) {
//
//        List<Queued> decision1 = Files.readAllLines(Paths.get("/tmp/logs")).stream()
//                .map(StateMachineDebugger::parse)
//                .filter(Objects::nonNull)
//                .sorted(Comparator.comparing(Queued::getQueued))
//                .collect(Collectors.toList());
//
//        long creationTime = -1, decisionTime = -1, decisionBarcodeTime = -1, finalBarcodeTime = -1, finalDirectionTime = -1;
//        for (Queued queued : decision1) {
//            if (queued.getEventName().equalsIgnoreCase("CREATE"))
//                creationTime = queued.getQueued();
//            if (queued.getEventName().equalsIgnoreCase("Decision"))
//                decisionTime = queued.getQueued();
//            if (queued.getEventName().equalsIgnoreCase("BARCODE_LINE") && decisionTime != -1)
//                decisionBarcodeTime = queued.getQueued();
//            if (queued.getEventName().equalsIgnoreCase("FINAL_BARCODE_LINE") && decisionTime != -1)
//                finalBarcodeTime = queued.getQueued();
//            if (queued.getEventName().equalsIgnoreCase("FINAL_DIRECTION") && decisionTime != -1)
//                finalDirectionTime = queued.getQueued();
//
//            System.out.println(queued.getQueued() + "," + queued.getStarted() + "," + queued.getEnded() + "," + queued.getEventName());
//        }
//
//        System.out.printf("Creation: [%s]%n", creationTime);
//        System.out.printf("Decision: [%s], it was [%s] after create%n", creationTime, (decisionTime - creationTime));
//        System.out.printf("BR of decision: [%s], it was [%s] after create%n", decisionBarcodeTime, (decisionBarcodeTime - creationTime));
//        System.out.printf("FBR after decision: [%s], it was [%s] after create%n", finalBarcodeTime, (finalBarcodeTime - creationTime));
//        System.out.printf("FD after decision: [%s], it was [%s] after create%n", finalDirectionTime, (finalDirectionTime - creationTime));
//
////
//    }

    private Queued parse(String line) {
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {

            String eventName = matcher.group(1);
            String actionId = matcher.group(2);
            String eventId = matcher.group(3);
            long queuedAt = Long.parseLong(matcher.group(4));
            long startedAt = Long.parseLong(matcher.group(5));
            long endedAt = Long.parseLong(matcher.group(6));


            Queued queued = new Queued();
            queued.setEventName(eventName);
            queued.setQueued(queuedAt);
            queued.setStarted(startedAt);
            queued.setEnded(endedAt);
            queued.setActionId(actionId);
            queued.setEventId(eventId);
            return queued;
        } else if (line.contains("[Decision]")) { //TODO - add more accurate details when this happend
            matcher = decisionPattern.matcher(line);
            if (matcher.find()) {
                String dateTime = matcher.group(1);
                long timestamp = -1;
                try {
                    timestamp = sm.parse(dateTime).getTime();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                timestamp += 3 * 1000 * 60 * 60;

                String[] key = order.get(order.size() -1).split(":");
                String actionId = key[0];
                String eventId = key[1];
                return new Queued().setEventName("Decision")
                        .setActionId(actionId)
                        .setEventId(eventId)
                        .setQueued(timestamp).setStarted(timestamp).setEnded(timestamp + 100);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private Map<String, List<Queued>> map = new HashMap<>();
    private List<String> order = new ArrayList<>();


    @Override
    public void afterPropertiesSet() throws Exception {
        Executors.newFixedThreadPool(1).execute(() -> {
            this.reader.start(line -> {
                Queued parse = parse(line);
                if (parse == null) return;
                List<Queued> list = map.computeIfAbsent(buildKey(parse), k -> {
                    order.add(k);
                    return new ArrayList<>();
                });
                list.add(parse);
                System.out.println(parse);
            });
        });
    }

    private String buildKey(Queued parse) {
        return parse.getActionId() + ":" + parse.getEventId();
    }

    public List<Queued> getLast() {
        return map.get(order.get(order.size() - 1));
    }
}
