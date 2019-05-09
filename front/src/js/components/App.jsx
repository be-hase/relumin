var React = require('react');
var Router = require('react-router');
var RouteHandler = Router.RouteHandler;
var NotificationSystem = require('react-notification-system');
var Spinner = require('react-spinkit');

var HeaderSidebar = require('./HeaderSidebar');
var RegisterClusterModal = require('./RegisterClusterModal');

var AppStore = require('../stores/AppStore');
var UserStore = require('../stores/UserStore');

var App = React.createClass({
    componentDidMount: function() {
        AppStore.addAlertListener(this.onAlertHandle);
    },
    componentWillUnmount: function() {
        AppStore.removeAlertListener(this.onAlertHandle);
    },
    render: function() {
        var registerClusterModalView;
        var me = UserStore.getMe();
        
        if (!AUTH_ENABLED || me.role === 'RELUMIN_ADMIN') {
            registerClusterModalView = (<RegisterClusterModal />);
        }
        return (
            <div>
                <div id="wrapper">
                    <HeaderSidebar />
                    <div id="page-wrapper">
                        <RouteHandler/>
                    </div>
                </div>
                {registerClusterModalView}
                <NotificationSystem ref="notificationSystem" />
                <div className="global-loading">
                    <Spinner spinnerName='three-bounce' ref="spinner" />
                </div>
            </div>
        );
    },
    onAlertHandle: function() {
        this.refs.notificationSystem.addNotification(AppStore.getAlert());
    }
});

module.exports = App;
