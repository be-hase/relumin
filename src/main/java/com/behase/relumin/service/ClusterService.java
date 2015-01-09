package com.behase.relumin.service;

import java.io.IOException;
import java.util.Set;

import com.behase.relumin.exception.ApiException;
import com.behase.relumin.model.Cluster;

public interface ClusterService {
	Set<String> getClusters();

	Cluster getCluster(String clusterName) throws ApiException, IOException;

	void setCluster(String clusterName, String node) throws ApiException, IOException;

	void deleteCluster(String clusterName);

	void refreshClusters();
}
