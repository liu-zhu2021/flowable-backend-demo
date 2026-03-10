package org.example.flowable.model;

import lombok.Data;
import java.util.Date;

@Data
public class TaskRepresentation {
    private String id;
    private String name;
    private String assignee;
    private Date createTime;
    private String processInstanceId;
    private String processDefinitionId;
    
    public TaskRepresentation(String id, String name, String assignee, Date createTime, String processInstanceId, String processDefinitionId) {
        this.id = id;
        this.name = name;
        this.assignee = assignee;
        this.createTime = createTime;
        this.processInstanceId = processInstanceId;
        this.processDefinitionId = processDefinitionId;
    }
}
