package com.be_hase.relumin.support;

import java.util.List;

import org.junit.Test;

import com.be_hase.relumin.model.CreateClusterParam;
import com.google.common.collect.Lists;

public class ValidationUtilsTest {
    @Test(expected = IllegalArgumentException.class)
    public void createClusterParamNeed3master() {
        CreateClusterParam param1 = new CreateClusterParam();
        param1.setStartSlotNumber("0");
        param1.setEndSlotNumber("16383");
        param1.setMaster("1.1.1.1:1");
        List<CreateClusterParam> params = Lists.newArrayList(param1);
        ValidationUtils.createClusterParams(params);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createClusterParamDuplicateSlot() {
        CreateClusterParam param1 = new CreateClusterParam();
        param1.setStartSlotNumber("0");
        param1.setEndSlotNumber("5000");
        param1.setMaster("1.1.1.1:1");
        CreateClusterParam param2 = new CreateClusterParam();
        param2.setStartSlotNumber("5001");
        param2.setEndSlotNumber("10000");
        param2.setMaster("1.1.1.1:2");
        CreateClusterParam param3 = new CreateClusterParam();
        param3.setStartSlotNumber("10000");
        param3.setEndSlotNumber("16383");
        param3.setMaster("1.1.1.1:3");
        List<CreateClusterParam> params = Lists.newArrayList(param1, param2, param3);
        ValidationUtils.createClusterParams(params);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createClusterParamSameMaster() {
        CreateClusterParam param1 = new CreateClusterParam();
        param1.setStartSlotNumber("0");
        param1.setEndSlotNumber("5000");
        param1.setMaster("1.1.1.1:1");
        CreateClusterParam param2 = new CreateClusterParam();
        param2.setStartSlotNumber("5001");
        param2.setEndSlotNumber("10000");
        param2.setMaster("1.1.1.1:1");
        CreateClusterParam param3 = new CreateClusterParam();
        param3.setStartSlotNumber("10001");
        param3.setEndSlotNumber("16383");
        param3.setMaster("1.1.1.1:3");
        List<CreateClusterParam> params = Lists.newArrayList(param1, param2, param3);
        ValidationUtils.createClusterParams(params);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createClusterParamSameReplica() {
        CreateClusterParam param1 = new CreateClusterParam();
        param1.setStartSlotNumber("0");
        param1.setEndSlotNumber("5000");
        param1.setMaster("1.1.1.1:1");
        param1.setReplicas(Lists.newArrayList("1.1.1.1:4"));
        CreateClusterParam param2 = new CreateClusterParam();
        param2.setStartSlotNumber("5001");
        param2.setEndSlotNumber("10000");
        param2.setMaster("1.1.1.1:2");
        param2.setReplicas(Lists.newArrayList("1.1.1.1:4"));
        CreateClusterParam param3 = new CreateClusterParam();
        param3.setStartSlotNumber("10001");
        param3.setEndSlotNumber("16383");
        param3.setMaster("1.1.1.1:3");
        param3.setReplicas(Lists.newArrayList("1.1.1.1:6"));
        List<CreateClusterParam> params = Lists.newArrayList(param1, param2, param3);
        ValidationUtils.createClusterParams(params);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createClusterParamNotEnough() {
        CreateClusterParam param1 = new CreateClusterParam();
        param1.setStartSlotNumber("0");
        param1.setEndSlotNumber("2000");
        param1.setMaster("1.1.1.1:1");
        CreateClusterParam param2 = new CreateClusterParam();
        param2.setStartSlotNumber("5001");
        param2.setEndSlotNumber("10000");
        param2.setMaster("1.1.1.1:2");
        CreateClusterParam param3 = new CreateClusterParam();
        param3.setStartSlotNumber("10001");
        param3.setEndSlotNumber("10002");
        param3.setMaster("1.1.1.1:3");
        List<CreateClusterParam> params = Lists.newArrayList(param1, param2, param3);
        ValidationUtils.createClusterParams(params);
    }

    @Test
    public void createClusterParam() {
        CreateClusterParam param1 = new CreateClusterParam();
        param1.setStartSlotNumber("0");
        param1.setEndSlotNumber("5000");
        param1.setMaster("1.1.1.1:1");
        CreateClusterParam param2 = new CreateClusterParam();
        param2.setStartSlotNumber("5001");
        param2.setEndSlotNumber("10000");
        param2.setMaster("1.1.1.1:2");
        CreateClusterParam param3 = new CreateClusterParam();
        param3.setStartSlotNumber("10001");
        param3.setEndSlotNumber("16383");
        param3.setMaster("1.1.1.1:3");
        List<CreateClusterParam> params = Lists.newArrayList(param1, param2, param3);
        ValidationUtils.createClusterParams(params);
    }
}
