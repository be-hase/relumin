var AppDispatcher = require('../dispatcher/AppDispatcher');
var AppConstants = require('../constants/AppConstants');
var ApiUtils = require('../utils/ApiUtils');
var Utils = require('../utils/Utils');

var UserActions = {
    getUsers: function(callbacks) {
        callbacks = callbacks || {};

        Utils.wrapSuccess(callbacks, function(data) {
            AppDispatcher.dispatch({
                actionType: AppConstants.GET_USERS,
                data: data
            });
        });

        ApiUtils.User.getUsers(callbacks);
    },
    updateMe: function(data, callbacks) {
        callbacks = callbacks || {};

        Utils.wrapSuccess(callbacks, function(data) {
            UserActions.getUsers();
        });

        ApiUtils.User.updateMe(data, callbacks);
    },
    changePassword: function(data, callbacks) {
        callbacks = callbacks || {};

        ApiUtils.User.changePassword(data, callbacks);
    },
    addUser: function(username, data, callbacks) {
        callbacks = callbacks || {};

        Utils.wrapSuccess(callbacks, function(data) {
            UserActions.getUsers();
        });

        ApiUtils.User.addUser(username, data, callbacks);
    },
    updateUser: function(username, data, callbacks) {
        callbacks = callbacks || {};

        Utils.wrapSuccess(callbacks, function(data) {
            UserActions.getUsers();
        });

        ApiUtils.User.updateUser(username, data, callbacks);
    },
    deleteUser: function(username, callbacks) {
        callbacks = callbacks || {};

        Utils.wrapSuccess(callbacks, function(data) {
            UserActions.getUsers();
        });

        ApiUtils.User.deleteUser(username, callbacks);
    }
};

module.exports = UserActions;
