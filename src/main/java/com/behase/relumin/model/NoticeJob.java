package com.behase.relumin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeJob {
    private NoticeItem item;
    private List<ResultValue> resultValues;

    @Data
    @AllArgsConstructor
    public static class ResultValue {
        private String nodeId;
        private String hostAndPort;
        private String value;
    }
}
