package org.example.flowable.model;

import lombok.Data;
import java.util.Map;

@Data
public class TaskActionRequest {
    private String taskId;
    private String action; // "approve" or "reject"
    private String assignee; // Optional, for claiming or assigning
    private Map<String, Object> variables;
}
