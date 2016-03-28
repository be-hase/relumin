package com.behase.relumin.model.param;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import lombok.Data;

import java.util.List;

@Data
public class CreateClusterParam {
    private String startSlotNumber;
    private String endSlotNumber;
    private String master;
    private List<String> replicas = Lists.newArrayList();

    @JsonIgnore
    private String masterNodeId;
}
