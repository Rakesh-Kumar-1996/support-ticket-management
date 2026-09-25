package com.supporttickets.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateTicketRequest {

    private boolean titlePresent;
    private String title;
    private boolean descriptionPresent;
    private String description;
    private boolean priorityPresent;
    private String priority;
    private boolean assigneePresent;
    private String assignee;
    private boolean statusPresent;

    @JsonProperty("title")
    public void setTitle(String title) {
        this.titlePresent = true;
        this.title = title;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.descriptionPresent = true;
        this.description = description;
    }

    @JsonProperty("priority")
    public void setPriority(String priority) {
        this.priorityPresent = true;
        this.priority = priority;
    }

    @JsonProperty("assignee")
    public void setAssignee(String assignee) {
        this.assigneePresent = true;
        this.assignee = assignee;
    }

    @JsonProperty("status")
    public void setStatus(JsonNode ignored) {
        this.statusPresent = true;
    }

    @JsonIgnore
    public boolean isTitlePresent() {
        return titlePresent;
    }

    public String getTitle() {
        return title;
    }

    @JsonIgnore
    public boolean isDescriptionPresent() {
        return descriptionPresent;
    }

    public String getDescription() {
        return description;
    }

    @JsonIgnore
    public boolean isPriorityPresent() {
        return priorityPresent;
    }

    public String getPriority() {
        return priority;
    }

    @JsonIgnore
    public boolean isAssigneePresent() {
        return assigneePresent;
    }

    public String getAssignee() {
        return assignee;
    }

    @JsonIgnore
    public boolean isStatusPresent() {
        return statusPresent;
    }

    @JsonIgnore
    public boolean hasFieldUpdate() {
        return titlePresent || descriptionPresent || priorityPresent || assigneePresent;
    }
}
