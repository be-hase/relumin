var AppDispatcher = require('../dispatcher/AppDispatcher');
var EventEmitter = require('events').EventEmitter;
var AppConstants = require('../constants/AppConstants');
var assign = require('object-assign');
var _ = require('lodash');

var _data = {};

var CHANGE_EVENT = 'change';

var ClusterNoticeStore = assign({}, EventEmitter.prototype, {
    getClusterNotice: function(clusterName) {
        if (!_data[clusterName]) {
            return {
                mail: {
                    to: '',
                    from: ''
                },
                http: {
                    url: ''
                },
                invalid_end_time: '',
                items: []
            };
        }
        return _data[clusterName];
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
        case AppConstants.GET_CLUSTER_NOTICE:
            _data[action.clusterName] = action.data;
            ClusterNoticeStore.emitChange();
            break;
        default:
            // no operation
    }
});

module.exports = ClusterNoticeStore;
