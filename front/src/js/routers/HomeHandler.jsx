var React = require('react');
var Router = require('react-router');

var ApiUtils = require('../utils/ApiUtils');
var Utils = require('../utils/Utils');

var ClusterStore = require('../stores/ClusterStore');
var ClusterActions = require('../actions/ClusterActions');

var HomeHandler = React.createClass({
    mixins: [Router.State, Router.Navigation],
    componentDidMount: function() {
        Utils.pageChangeInit();
    },
    componentWillMount: function() {
        var clusters = ClusterStore.getClusters();

        if (clusters && !_.isEmpty(clusters)) {
            this.transitionTo('/cluster/' + clusters[0].cluster_name);
            return;
        }
    },
    render: function() {
        return (
            <div>Cluster is not registered.</div>
        );
    }
});

module.exports = HomeHandler;
