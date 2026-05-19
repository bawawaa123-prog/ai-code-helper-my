package com.yupi.aicodehelper.ai.rag;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai.rag")
public class RagProperties {

    /**
     * 是否默认关闭RAG，接口请求可以通过 useRag 参数覆盖这个默认值。
     */
    private boolean enabledByDefault = false;

    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    public void setEnabledByDefault(boolean enabledByDefault) {
        this.enabledByDefault = enabledByDefault;
    }
}
