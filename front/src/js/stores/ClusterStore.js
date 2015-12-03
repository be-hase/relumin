var AppDispatcher = require('../dispatcher/AppDispatcher');
var EventEmitter = require('events').EventEmitter;
var AppConstants = require('../constants/AppConstants');
var assign = require('object-assign');
var _ = require('lodash');

var _data = [];

var CHANGE_EVENT = 'change';

var ClusterStore = assign({}, EventEmitter.prototype, {
    getClusters: function() {
        return _data;
    },
    getCluster: function(clusterName) {
        return _.find(_data, function(val) {
            return val.cluster_name === clusterName;
        });
    },
    emitChange: function() {
        this.emit(CHANGE_EVENT);
    },
    addChangeListener: function(callback) {
        this.on(CHANGE_EVENT, callback);
    },
    removeChangeListener: function(callback) {
        this.removeListener(CHANGE_EVENT, callback);
    }
});

AppDispatcher.register(function(action) {
    switch(action.actionType) {
        case AppConstants.GET_CLUSTERS:
            _data = action.data;
            ClusterStore.emitChange();
            break;
        case AppConstants.GET_CLUSTER:
            var cluster = action.data;

            var index = _.findIndex(_data, function(val) {
                return val.cluster_name === cluster.cluster_name;
            });

            if (index >= 0) {
                _data[index] = cluster;
                ClusterStore.emitChange();
            }
            break;
        default:
            // no operation
    }
});

module.exports = ClusterStore;
