package org.example.flowable.model;

import lombok.Data;
import java.util.Date;
import java.util.Map;

@Data
public class ProcessInstanceRepresentation {
    private String id;
    private String name;
    private String processDefinitionId;
    private Date startTime;
    private Date endTime;
    private String startUserId;
    private Map<String, Object> variables;
    private boolean completed;
    
    public ProcessInstanceRepresentation(String id, String name, String processDefinitionId, Date startTime, Date endTime, String startUserId, Map<String, Object> variables, boolean completed) {
        this.id = id;
        this.name = name;
        this.processDefinitionId = processDefinitionId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.startUserId = startUserId;
        this.variables = variables;
        this.completed = completed;
    }
}
