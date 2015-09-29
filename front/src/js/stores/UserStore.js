var AppDispatcher = require('../dispatcher/AppDispatcher');
var EventEmitter = require('events').EventEmitter;
var AppConstants = require('../constants/AppConstants');
var assign = require('object-assign');
var _ = require('lodash');

var _data = [];

var CHANGE_EVENT = 'change';

var UserStore = assign({}, EventEmitter.prototype, {
    getUsers: function() {
        return _data;
    },
    getUser: function(username) {
        return _.find(_data, function(val) {
            return val.username === username;
        });
    },
    getMe: function() {
        return UserStore.getUser(USER.username) || {};
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
        case AppConstants.GET_USERS:
            _data = action.data;
            UserStore.emitChange();
            break;
        default:
            // no operation
    }
});

module.exports = UserStore;
