var React = require('react');
var Router = require('react-router');

var Utils = require('../utils/Utils');
var Users = require('../components/Users');

var UsersHandler = React.createClass({
    mixins: [Router.State],
    componentDidMount: function() {
        Utils.pageChangeInit();
    },
    render: function() {
        return (
            <Users />
        );
    }
});

module.exports = UsersHandler;
