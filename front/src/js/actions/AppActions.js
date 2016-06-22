var AppDispatcher = require('../dispatcher/AppDispatcher');
var AppConstants = require('../constants/AppConstants');

var AppActions = {
    showAlert: function(param) {
        AppDispatcher.dispatch({
            actionType: AppConstants.SHOW_ALERT,
            param: param
        });
    }
};

module.exports = AppActions;
