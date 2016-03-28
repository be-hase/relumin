package com.behase.relumin.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotInfo {
    private int startSlotNumber;
    private int endSlotNumber;
    private Map<String, String> master = Maps.newLinkedHashMap();
    private List<Map<String, String>> replicas = Lists.newArrayList();
}
