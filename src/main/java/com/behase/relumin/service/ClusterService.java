package com.behase.relumin.service;

import java.io.IOException;
import java.util.Set;

import com.behase.relumin.model.Cluster;

public interface ClusterService {
	Set<String> getClusters();

	Cluster getCluster(String clusterName) throws IOException;

	Cluster getClusterByHostAndPort(String hostAndPort) throws IOException;

	boolean existsClusterName(String clusterName);

	void setCluster(String clusterName, String node) throws IOException;

	void deleteCluster(String clusterName);

	void refreshClusters();
}
