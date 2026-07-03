package com.waiitz.ai_mini_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiChatResponse {
    private String answer;
    private List<String> suggestions;
    private String intent;
    private Boolean needTool;


}
