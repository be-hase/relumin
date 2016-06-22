var AppDispatcher = require('../dispatcher/AppDispatcher');
var EventEmitter = require('events').EventEmitter;
var AppConstants = require('../constants/AppConstants');
var assign = require('object-assign');

var _data = {};

var CHANGE_EVENT = 'change';

var ClusterSlowLogStore = assign({}, EventEmitter.prototype, {
    getSlowLog: function(clusterName, pageNo) {
        return _data[clusterName + '-' + pageNo];
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
        case AppConstants.GET_CLUSTER_SLOWLOG:
            var newData = {};
            newData[action.clusterName + '-' + action.data.current_page] = action.data;
            _data = newData;
            ClusterSlowLogStore.emitChange();
            break;
        default:
        // no operation
    }
});

module.exports = ClusterSlowLogStore;
