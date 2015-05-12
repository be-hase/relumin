package com.behase.relumin.service;

import java.io.IOException;
import java.util.List;

import com.behase.relumin.model.param.CreateClusterParam;

public interface RedisTribService {
	List<CreateClusterParam> getCreateClusterParams(int replicas, List<String> hostAndPorts) throws IOException;
}
