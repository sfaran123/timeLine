package com.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Queued {
    private String eventName;
    private Long queued;
    private Long started;
    private Long ended;
    private String eventId;
    private String actionId;
}