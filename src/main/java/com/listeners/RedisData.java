package com.listeners;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

@Data
@Accessors(chain = true)
public class RedisData {
    @JsonProperty("event_id")
    private String eventId;
    @JsonProperty("action_id")
    private String actionId;
    @JsonProperty("type")
    private String eventType;
    private Map<String, Object> payload;

    public RedisData(){
        payload = new HashMap<>();
    }

    @JsonAnyGetter
    public Map<String, Object> getPayload() {
        return payload;
    }

    @JsonAnySetter
    public RedisData addProperty(String key, Object value) {
        payload.put(key, value);
        return this;
    }

    @JsonIgnore
    public String getKey() {
        return eventId;
    }
}