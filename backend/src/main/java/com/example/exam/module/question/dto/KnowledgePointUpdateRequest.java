package com.example.exam.module.question.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class KnowledgePointUpdateRequest {

    private String name;

    private Integer sortOrder;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @JsonIgnore
    private Long parentId;

    /** JSON 中出现 parentId 键（含 null）时为 true，避免与未传该字段混淆 */
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @JsonIgnore
    private boolean parentIdPresent;

    @JsonProperty("parentId")
    public void bindParentId(Long parentId) {
        this.parentId = parentId;
        this.parentIdPresent = true;
    }

    public Long getParentId() {
        return parentId;
    }

    public boolean isParentIdPresent() {
        return parentIdPresent;
    }
}
