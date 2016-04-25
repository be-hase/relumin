var AppDispatcher = require('../dispatcher/AppDispatcher');
var AppConstants = require('../constants/AppConstants');
var ApiUtils = require('../utils/ApiUtils');
var Utils = require('../utils/Utils');

var ClusterActions = {
    getClusters: function(callbacks) {
        callbacks = callbacks || {};

        Utils.wrapSuccess(callbacks, function(data) {
            AppDispatcher.dispatch({
                actionType: AppConstants.GET_CLUSTERS,
                data: data
            });
        });

        ApiUtils.Cluster.getClusters({full: true}, callbacks);
    },
    getCluster: function(clusterName, callbacks) {
        callbacks = callbacks || {};

        Utils.wrapSuccess(callbacks, function(data) {
            AppDispatcher.dispatch({
                actionType: AppConstants.GET_CLUSTER,
                data: data
            });
        });

        ApiUtils.Cluster.getCluster(clusterName, callbacks);
    },
    setClusters: function(data, callbacks) {
        callbacks = callbacks || {};

        Utils.wrapSuccess(callbacks, function(data) {
            ClusterActions.getClusters();
        });

        ApiUtils.Cluster.setClusters(data, callbacks);
    },
    getNodeMetrics: function(clusterName, data, callbacks) {
        callbacks = callbacks || {};

        Utils.wrapSuccess(callbacks, function(data) {
            AppDispatcher.dispatch({
                actionType: AppConstants.GET_NODE_METRICS,
                clusterName: clusterName,
                data: data
            });
        });

        ApiUtils.Cluster.getNodeMetrics(clusterName, data, callbacks);
    },
    setNodeMetricsQuery: function(clusterName, query) {
        AppDispatcher.dispatch({
            actionType: AppConstants.SET_NODE_METRICS_QUERY,
            clusterName: clusterName,
            data: query
        });
    },
    setNodeMetricsQueryOnlyAutoRefresh: function(clusterName, autoRefresh) {
        AppDispatcher.dispatch({
            actionType: AppConstants.SET_NODE_METRICS_QUERY_ONLY_AUTO_REFRESH,
            clusterName: clusterName,
            autoRefresh: autoRefresh
        });
    },
    getNodeSlowLog: function(clusterName, data, callbacks) {
        callbacks = callbacks || {};

        Utils.wrapSuccess(callbacks, function(data) {
            AppDispatcher.dispatch({
                actionType: AppConstants.GET_NODE_SLOWLOG,
                clusterName: clusterName,
                data: data
            });
        });

        ApiUtils.Cluster.getNodeSlowlogs(clusterName, data, callbacks);
    },
    setNodeSlowLogQuery: function(clusterName, query) {
        AppDispatcher.dispatch({
            actionType: AppConstants.SET_NODE_SLOWLOG_QUERY,
            clusterName: clusterName,
            data: query
        });
    },
    getClusterNotice: function(clusterName, callbacks) {
        callbacks = callbacks || {};

        Utils.wrapSuccess(callbacks, function(data) {
            AppDispatcher.dispatch({
                actionType: AppConstants.GET_CLUSTER_NOTICE,
                clusterName: clusterName,
                data: data
            });
        });

        ApiUtils.Cluster.getClusterNotice(clusterName, callbacks);
    },
    setClusterNotice: function(clusterName, data, callbacks) {
        callbacks = callbacks || {};

        Utils.wrapSuccess(callbacks, function(data) {
            AppDispatcher.dispatch({
                actionType: AppConstants.GET_CLUSTER_NOTICE,
                clusterName: clusterName,
                data: data
            });
        });

        ApiUtils.Cluster.setClusterNotice(clusterName, data, callbacks);
    },
    deleteCluster: function(clusterName, callbacks) {
        callbacks = callbacks || {};

        Utils.wrapSuccess(callbacks, function(data) {
            ClusterActions.getClusters();
        });

        ApiUtils.Cluster.deleteCluster(clusterName, callbacks);
    },
    changeClusterName: function(clusterName, data, callbacks) {
        callbacks = callbacks || {};

        Utils.wrapSuccess(callbacks, function(data) {
            ClusterActions.getClusters();
        });

        ApiUtils.Cluster.changeClusterName(clusterName, data, callbacks);
    }
};

module.exports = ClusterActions;
