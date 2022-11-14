package com.controller;

import com.listeners.RedisData;
import com.listeners.StateMachineListener;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class SMController {

    private final StateMachineListener listener;

    public SMController(StateMachineListener listener) {
        this.listener = listener;
    }

    @Data
    @Accessors(chain = true)
    public class Respo {
        private String eventId;
        private Boolean stabilized;
        private String initialState;
        private Map<String, Double> lux;
        private String decisionBy;
        private String decisionBarcode;
        private String flowName;
        private String flowType;
        private String barcodeScanner;

    }
    @RequestMapping(value = "/redis")
    public List<RedisData> getRedis() {
        return listener.getLastEventData();
    }

    @RequestMapping(value = "/all")
    public Map<String, List<RedisData>> getAll() {
        return listener.getAll();
    }

    @RequestMapping(value = "/event/{eventId}")
    public List<RedisData> getByEventId(@PathVariable String eventId) {
        return listener.getByEventId(eventId);
    }

    @RequestMapping(value = "/data")
    public ResponseEntity<Respo> getData() {
        return getDataByEventId(null);

    }
    @SneakyThrows
    @RequestMapping(value = "/data/{eventId}")
    public ResponseEntity<Respo> getDataByEventId(@PathVariable String eventId) {
        List<RedisData> lastEventData = eventId == null ? listener.getLastEventData() : listener.getByEventId(eventId);
        if (lastEventData == null) return ResponseEntity.notFound().build();
        Map<String, List<RedisData>> map = lastEventData.stream().collect(Collectors.groupingBy(RedisData::getEventType));


        Map<String, Object> createEventPayload = map.get("create").get(0).getPayload();
        Map<String, Object> flowNamePayload =  map.get("flowName").get(0).getPayload();
        return ResponseEntity.ok(new Respo()
                .setEventId(lastEventData.get(0).getEventId())
                .setLux((Map<String, Double>) createEventPayload.get("lux"))
                .setStabilized((Boolean) createEventPayload.get("stabilized"))
                .setInitialState((String) createEventPayload.get("initial_state"))
//                .setDecisionBarcode((String) map.get("decision").get(0).getPayload().get("decision"))
//                .setDecisionBy((String) map.get("decision").get(0).getPayload().get("route"))
//                .setFlowName((String) flowNamePayload.get("flowName"))
//                .setFlowType((String) flowNamePayload.get("type"))
//                .setBarcodeScanner((String) Optional.ofNullable(map.get("barcodeScanner")).map(d->d.get(0).getPayload().get("barcode")).orElse(null))
        );
    }
}
