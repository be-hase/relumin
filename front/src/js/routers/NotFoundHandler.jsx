var React = require('react');
var Router = require('react-router');

var NotFoundHandler = React.createClass({
    mixins: [Router.State],
    componentDidMount: function() {
        Utils.pageChangeInit();
    },
    render: function() {
        return (
            <div>Page does not exists.</div>
        );
    },
});

module.exports = NotFoundHandler;
