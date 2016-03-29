package com.behase.relumin.util;

import com.behase.relumin.exception.InvalidParameterException;
import com.behase.relumin.model.param.CreateClusterParam;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.List;

public class ValidationUtilsTest {

    @Test(expected = InvalidParameterException.class)
    public void createClusterParam_need_3master() {
        CreateClusterParam param1 = new CreateClusterParam();
        param1.setStartSlotNumber("0");
        param1.setEndSlotNumber("16383");
        param1.setMaster("1.1.1.1:1");
        List<CreateClusterParam> params = Lists.newArrayList(param1);
        ValidationUtils.createClusterParams(params);
    }

    @Test(expected = InvalidParameterException.class)
    public void createClusterParam_duplicate_slot() {
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

    @Test(expected = InvalidParameterException.class)
    public void createClusterParam_same_master() {
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

    @Test(expected = InvalidParameterException.class)
    public void createClusterParam_same_replica() {
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

    @Test(expected = InvalidParameterException.class)
    public void createClusterParam_not_enough() {
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
