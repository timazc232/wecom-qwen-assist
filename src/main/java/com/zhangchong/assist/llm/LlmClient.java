package com.zhangchong.assist.llm;

import java.util.List;
import java.util.Map;

public interface LlmClient {

    LlmResult chat(List<Map<String, Object>> messages);

    String name();
}
