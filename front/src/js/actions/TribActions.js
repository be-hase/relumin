var AppDispatcher = require('../dispatcher/AppDispatcher');
var AppConstants = require('../constants/AppConstants');
var ApiUtils = require('../utils/ApiUtils');
var Utils = require('../utils/Utils');
var ClusterActions = require('../actions/ClusterActions');

var TribActions = {
    getCreateClusterParams: function(query, callbacks) {
        callbacks = callbacks || {};

        Utils.wrapSuccess(callbacks, function(data) {
            AppDispatcher.dispatch({
                actionType: AppConstants.GET_CREATE_CLUSTER_PARAMS,
                data: JSON.stringify(data, null, '    ')
            });
        });

        ApiUtils.Trib.getCreateClusterParams(query, callbacks);
    },
    createCluster: function(clusterName, data, callbacks) {
        callbacks = callbacks || {};

        Utils.wrapSuccess(callbacks, function(data) {
            if (clusterName) {
                ClusterActions.getClusters();
            }
        });

        ApiUtils.Trib.createCluster(clusterName, data, callbacks);
    },
    addNode: function(data, callbacks) {
        callbacks = callbacks || {};

        Utils.wrapSuccess(callbacks, function(successData) {
            successData.cluster_name = data.clusterName;
            AppDispatcher.dispatch({
                actionType: AppConstants.GET_CLUSTER,
                data: successData
            });
        });

        ApiUtils.Trib.addNode(data, callbacks);
    },
    reshard: function(data, callbacks) {
        callbacks = callbacks || {};

        Utils.wrapSuccess(callbacks, function(successData) {
            successData.cluster_name = data.clusterName;
            AppDispatcher.dispatch({
                actionType: AppConstants.GET_CLUSTER,
                data: successData
            });
        });

        ApiUtils.Trib.reshard(data, callbacks);
    },
    reshardBySlots: function(data, callbacks) {
        callbacks = callbacks || {};

        Utils.wrapSuccess(callbacks, function(successData) {
            successData.cluster_name = data.clusterName;
            AppDispatcher.dispatch({
                actionType: AppConstants.GET_CLUSTER,
                data: successData
            });
        });

        ApiUtils.Trib.reshardBySlots(data, callbacks);
    },
    replicate: function(clusterName, data, callbacks) {
        callbacks = callbacks || {};

        Utils.wrapSuccess(callbacks, function(successData) {
            successData.cluster_name = clusterName;
            AppDispatcher.dispatch({
                actionType: AppConstants.GET_CLUSTER,
                data: successData
            });
        });

        ApiUtils.Trib.replicate(data, callbacks);
    },
    failover: function(clusterName, data, callbacks) {
        callbacks = callbacks || {};

        Utils.wrapSuccess(callbacks, function(successData) {
            successData.cluster_name = clusterName;
            AppDispatcher.dispatch({
                actionType: AppConstants.GET_CLUSTER,
                data: successData
            });
        });

        ApiUtils.Trib.failover(data, callbacks);
    },
    deleteNode: function(data, callbacks) {
        callbacks = callbacks || {};

        Utils.wrapSuccess(callbacks, function(successData) {
            successData.cluster_name = data.clusterName;
            AppDispatcher.dispatch({
                actionType: AppConstants.GET_CLUSTER,
                data: successData
            });
        });

        ApiUtils.Trib.deleteNode(data, callbacks);
    },
    shutdown: function(data, callbacks) {
        callbacks = callbacks || {};

        Utils.wrapSuccess(callbacks, function(successData) {
            successData.cluster_name = data.clusterName;
            AppDispatcher.dispatch({
                actionType: AppConstants.GET_CLUSTER,
                data: successData
            });
        });

        ApiUtils.Trib.shutdown(data, callbacks);
    }
};

module.exports = TribActions;
