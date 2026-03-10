package org.example.flowable.model;

import lombok.Data;
import java.util.Map;

@Data
public class StartProcessRequest {
    private String processKey;
    private String businessKey;
    private Map<String, Object> variables;
}
