var AppDispatcher = require('../dispatcher/AppDispatcher');
var EventEmitter = require('events').EventEmitter;
var AppConstants = require('../constants/AppConstants');
var assign = require('object-assign');

var _data = '';

var CHANGE_EVENT = 'change';

var CreateClusterParamsStore = assign({}, EventEmitter.prototype, {
    getCreateClusterParams: function() {
        return _data;
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
        case AppConstants.GET_CREATE_CLUSTER_PARAMS:
            _data = action.data;
            CreateClusterParamsStore.emitChange();
            break;
        default:
            // no operation
    }
});

module.exports = CreateClusterParamsStore;
