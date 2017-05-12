package org.relumin.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;

import lombok.Data;

@Data
public class CreateClusterParam {
    private String startSlotNumber;
    private String endSlotNumber;
    private String master;
    private List<String> replicas = Lists.newArrayList();

    @JsonIgnore
    private String masterNodeId;
}
