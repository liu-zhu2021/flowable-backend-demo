package org.example.flowable.controller;

import org.example.flowable.common.Result;
import org.example.flowable.model.ProcessInstanceRepresentation;
import org.example.flowable.model.StartProcessRequest;
import org.example.flowable.model.TaskActionRequest;
import org.example.flowable.model.TaskRepresentation;
import org.example.flowable.service.ProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/process")
@CrossOrigin(origins = "*")
public class ProcessController {

    @Autowired
    private ProcessService processService;

    @PostMapping("/start")
    public Result<String> startProcess(@RequestBody StartProcessRequest request) {
        String processInstanceId = processService.startProcess(request.getProcessKey(), request.getBusinessKey(), request.getVariables());
        return Result.success(processInstanceId);
    }

    @GetMapping("/tasks")
    public Result<List<TaskRepresentation>> getTasks(@RequestParam(required = false, defaultValue = "admin") String assignee) {
        return Result.success(processService.getTasks(assignee));
    }

    @PostMapping("/complete")
    public Result<String> completeTask(@RequestBody TaskActionRequest request) {
        // "approve" or "reject" is passed in action field, mapped to "outcome" variable in service
        // We can pass it directly as outcome
        processService.completeTask(request.getTaskId(), request.getAction(), request.getVariables());
        return Result.success("Task completed");
    }

    @GetMapping("/history")
    public Result<List<ProcessInstanceRepresentation>> getHistory() {
        return Result.success(processService.getHistory());
    }

    @GetMapping("/running")
    public Result<List<ProcessInstanceRepresentation>> getRunning() {
        return Result.success(processService.getRunning());
    }
}
