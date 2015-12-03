var AppDispatcher = require('../dispatcher/AppDispatcher');
var EventEmitter = require('events').EventEmitter;
var AppConstants = require('../constants/AppConstants');
var assign = require('object-assign');

var status = {};

var ALERT_EVENT = 'change:alert';

function alert(param) {
    status.alert = param;
}

var AppStore = assign({}, EventEmitter.prototype, {
    getAlert: function() {
        return status.alert;
    },
    emitAlert: function() {
        this.emit(ALERT_EVENT);
    },
    addAlertListener: function(callback) {
        this.on(ALERT_EVENT, callback);
    },
    removeAlertListener: function(callback) {
        this.removeListener(ALERT_EVENT, callback);
    }
});

AppDispatcher.register(function(action) {
    switch(action.actionType) {
        case AppConstants.SHOW_ALERT:
            if (action.param) {
                alert(action.param);
                AppStore.emitAlert();
            }
            break;
        default:
            // no operation
    }
});

module.exports = AppStore;
