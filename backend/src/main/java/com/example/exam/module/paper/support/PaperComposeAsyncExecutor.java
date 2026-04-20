package com.example.exam.module.paper.support;

import com.example.exam.module.paper.dto.PaperAutoGenRequest;
import com.example.exam.module.paper.dto.PaperAutoGenResult;
import com.example.exam.module.paper.service.PaperService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaperComposeAsyncExecutor {

    private final PaperService paperService;
    private final PaperComposeTaskRegistry registry;

    @Async
    public void execute(String taskId, PaperAutoGenRequest req, Authentication authentication) {
        Authentication prev = SecurityContextHolder.getContext().getAuthentication();
        try {
            if (authentication != null) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            PaperAutoGenResult r = paperService.generateAuto(req);
            registry.complete(taskId, r, null);
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            registry.complete(taskId, null, msg);
        } finally {
            SecurityContextHolder.getContext().setAuthentication(prev);
        }
    }
}
