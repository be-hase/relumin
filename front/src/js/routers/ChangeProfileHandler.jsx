var React = require('react');
var Router = require('react-router');

var ChangePassword = require('../components/ChangePassword');
var ChangeName = require('../components/ChangeName');
var Utils = require('../utils/Utils');

var ChangeProfileHandler = React.createClass({
    mixins: [Router.State],
    componentDidMount: function() {
        Utils.pageChangeInit();
    },
    render: function() {
        if (AUTH_ENABLED) {
            return (
                <div>
                    <ChangeName />
                    <ChangePassword />
                </div>
            );
        } else {
            return (
                <div>Page does not exists.</div>
            );
        }
    }
});

module.exports = ChangeProfileHandler;
