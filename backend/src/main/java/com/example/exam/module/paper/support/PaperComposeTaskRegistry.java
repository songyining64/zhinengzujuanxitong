package com.example.exam.module.paper.support;

import com.example.exam.module.paper.dto.PaperAutoGenResult;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class PaperComposeTaskRegistry {

    private final ConcurrentHashMap<String, TaskEntry> tasks = new ConcurrentHashMap<>();

    public void putRunning(String id) {
        TaskEntry e = new TaskEntry();
        e.setStatus("RUNNING");
        tasks.put(id, e);
    }

    public void complete(String id, PaperAutoGenResult result, String errorMessage) {
        TaskEntry e = tasks.get(id);
        if (e == null) {
            return;
        }
        if (errorMessage != null) {
            e.setStatus("FAILED");
            e.setErrorMessage(errorMessage);
        } else {
            e.setStatus("DONE");
            e.setResult(result);
        }
    }

    public TaskEntry get(String id) {
        return tasks.get(id);
    }

    @Data
    public static class TaskEntry {
        private String status = "RUNNING";
        private PaperAutoGenResult result;
        private String errorMessage;
    }
}
