package com.behase.relumin.model;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeItem {
    private String metricsType;
    private String metricsName;
    private String operator;
    private String valueType;
    private String value;

    public enum NoticeType {
        CLUSTER_INFO("cluster_info"),
        NODE_INFO("node_info");

        @Getter
        private String value;

        NoticeType(String value) {
            this.value = value;
        }

        public static NoticeType getNoticeType(String value) {
            return Arrays.stream(values())
                         .filter(v -> StringUtils.equalsIgnoreCase(v.value, value))
                         .findFirst()
                         .orElse(null);
        }
    }

    public enum NoticeValueType {
        NUMBER("number"),
        STRING("string");

        @Getter
        private String value;

        NoticeValueType(String value) {
            this.value = value;
        }

        public static NoticeValueType getNoticeValueType(String value) {
            return Arrays.stream(values())
                         .filter(v -> StringUtils.equalsIgnoreCase(v.value, value))
                         .findFirst()
                         .orElse(null);
        }
    }

    public enum NoticeOperator {
        EQ("eq", " == "),
        NE("ne", " != "),
        GT("gt", " > "),
        GE("ge", " >= "),
        LT("lt", " < "),
        LE("le", " <= ");

        @Getter
        private String value;
        @Getter
        private String label;

        NoticeOperator(String value, String label) {
            this.value = value;
            this.label = label;
        }

        public static NoticeOperator getNoticeOperator(String value) {
            return Arrays.stream(values())
                         .filter(v -> StringUtils.equalsIgnoreCase(v.value, value))
                         .findFirst()
                         .orElse(null);
        }
    }
}
