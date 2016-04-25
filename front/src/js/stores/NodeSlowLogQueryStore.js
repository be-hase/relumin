var AppDispatcher = require('../dispatcher/AppDispatcher');
var EventEmitter = require('events').EventEmitter;
var assign = require('object-assign');
var _ = require('lodash');
var $ = require('jquery');
var moment = require('moment');

var AppConstants = require('../constants/AppConstants');
var ClusterStore = require('../stores/ClusterStore');

var _data = {};

var PREFIX = "slowlog.query.";
var CHANGE_EVENT = 'change';

var defaultNodeMetricsNames = [
    'used_memory', 'used_memory_rss', 'used_memory_peak', 'mem_fragmentation_ratio',
    'total_connections_received', 'total_commands_processed', 'instantaneous_ops_per_sec',
    'keyspace_hits', 'keyspace_misses',
].join(',');

function getFromData(clusterName) {
    var query = _data[clusterName];

    if (!query) {
        return false;
    }

    return query;
}

function getFromLocalStorage(clusterName) {
    if (!window.localStorage) {
        return false;
    }

    var savedStr = window.localStorage.getItem(PREFIX + clusterName);
    try {
        var saved = $.parseJSON(savedStr);
        saved.start = false;
        saved.end = false;

        return saved;
    } catch (e) {
        return false;
    }
}

function filterNodes(clusterName, query) {
    var cluster = ClusterStore.getCluster(clusterName);
    var nodesArray = [];
    _.each(query.nodes.split(','), function(nodeId) {
        var index = _.findIndex(cluster.nodes, function(node) {
            return node.node_id === nodeId;
        });
        if (index >= 0) {
            nodesArray.push(nodeId);
        }
    });

    if (nodesArray.length === 0) {
        query.nodes = getDefault(clusterName).nodes;
        return;
    }

    query.nodes = nodesArray.join(',');
}

function setLocalStorage(clusterName, query) {
    if (!window.localStorage) {
        return;
    }

    try {
        var savedStr = JSON.stringify(query);
        window.localStorage.setItem(PREFIX + clusterName, savedStr);
    } catch (e) {
    }
}

function getDefault(clusterName) {
    var cluster = ClusterStore.getCluster(clusterName);

    var query = {
        start: false,
        end: false,
        nodes: _.find(cluster.nodes, function(node) { return !node.master_node_id; }).node_id
    };

    return query;
}

var NodeSlowLogQueryStore = assign({}, EventEmitter.prototype, {
    getNodeSlowLogQuery: function(clusterName) {
        var query = getFromData(clusterName);
        if (!query) {
            query = getFromLocalStorage(clusterName);
        }
        if (!query) {
            query = getDefault(clusterName);
        }

        filterNodes(clusterName, query);

        _data[clusterName] = query;
        return query;
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
    var query;
    switch(action.actionType) {
        case AppConstants.SET_NODE_SLOWLOG_QUERY:
            query = NodeSlowLogQueryStore.getNodeSlowLogQuery(action.clusterName);
            query = _.assign(query, action.data);

            _data[action.clusterName] = query;
            setLocalStorage(action.clusterName, _data[action.clusterName]);
            NodeSlowLogQueryStore.emitChange();
            break;
        default:
            // no operation
    }
});

module.exports = NodeSlowLogQueryStore;
