package org.example.flowable.service;

import org.example.flowable.model.ProcessInstanceRepresentation;
import org.example.flowable.model.TaskRepresentation;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProcessService {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    @Transactional
    public String startProcess(String processKey, String businessKey, Map<String, Object> variables) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processKey, businessKey, variables);
        return processInstance.getId();
    }

    @Transactional(readOnly = true)
    public List<TaskRepresentation> getTasks(String assignee) {
        // Query tasks assigned to user OR where user is a candidate
        List<Task> tasks = taskService.createTaskQuery().taskAssignee(assignee).list();
        
        // For demo purpose, if assignee is "admin", we also return tasks for "managers" group
        if ("admin".equals(assignee)) {
            List<Task> groupTasks = taskService.createTaskQuery().taskCandidateGroup("managers").list();
            tasks.addAll(groupTasks);
        }
        
        // Remove duplicates if any
        return tasks.stream().distinct().map(task -> new TaskRepresentation(
                task.getId(),
                task.getName(),
                task.getAssignee(),
                task.getCreateTime(),
                task.getProcessInstanceId(),
                task.getProcessDefinitionId()
        )).collect(Collectors.toList());
    }

    @Transactional
    public void completeTask(String taskId, String outcome, Map<String, Object> variables) {
        if (variables == null) {
            variables = new HashMap<>();
        }
        if (outcome != null) {
            variables.put("outcome", outcome);
        }
        taskService.complete(taskId, variables);
    }

    @Transactional(readOnly = true)
    public List<ProcessInstanceRepresentation> getHistory() {
        List<HistoricProcessInstance> history = historyService.createHistoricProcessInstanceQuery()
                .finished()
                .orderByProcessInstanceEndTime().desc()
                .list();

        return history.stream().map(h -> new ProcessInstanceRepresentation(
                h.getId(),
                h.getName(),
                h.getProcessDefinitionId(),
                h.getStartTime(),
                h.getEndTime(),
                h.getStartUserId(),
                h.getProcessVariables(),
                true
        )).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProcessInstanceRepresentation> getRunning() {
        List<ProcessInstance> running = runtimeService.createProcessInstanceQuery()
                .orderByStartTime().desc()
                .list();

        return running.stream().map(p -> new ProcessInstanceRepresentation(
                p.getId(),
                p.getName(),
                p.getProcessDefinitionId(),
                p.getStartTime(),
                null,
                p.getStartUserId(),
                p.getProcessVariables(),
                false
        )).collect(Collectors.toList());
    }
}
