package com.example.exam.module.paper.service;

import com.example.exam.module.paper.dto.PaperAutoGenRequest;
import com.example.exam.module.paper.support.PaperComposeAsyncExecutor;
import com.example.exam.module.paper.support.PaperComposeTaskRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaperComposeTaskService {

    private final PaperComposeTaskRegistry registry;
    private final PaperComposeAsyncExecutor asyncExecutor;

    public String submit(PaperAutoGenRequest req) {
        String id = UUID.randomUUID().toString();
        registry.putRunning(id);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        asyncExecutor.execute(id, req, auth);
        return id;
    }

    public PaperComposeTaskRegistry.TaskEntry get(String taskId) {
        return registry.get(taskId);
    }
}
