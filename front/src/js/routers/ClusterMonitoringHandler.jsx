var React = require('react');
var Router = require('react-router');

var ClusterStore = require('../stores/ClusterStore');
var NodeMetricsQueryStore = require('../stores/NodeMetricsQueryStore');
var NoClusterRender = require('../mixins/NoClusterRender');
var ClusterTab = require('../components/ClusterTab');
var ClusterMonitoring = require('../components/ClusterMonitoring');
var ClusterMonitoringQuery = require('../components/ClusterMonitoringQuery');
var Utils = require('../utils/Utils');

var ClusterMonitoringHandler = React.createClass({
    mixins: [Router.State, NoClusterRender],
    componentDidMount: function() {
        Utils.pageChangeInit();
    },
    render: function() {
        var cluster = ClusterStore.getCluster(this.getParams().clusterName);
        var key = 'cluster-monitoring-' + this.getParams().clusterName;

        if (cluster) {
            return (
                <div key={key}>
                    <ClusterTab cluster={cluster} />
                    <ClusterMonitoringQuery cluster={cluster} />
                    <ClusterMonitoring cluster={cluster} />
                </div>
            );
        } else {
            return this.renderNoCluster(this.props.params.clusterName, key);
        }
    }
});

module.exports = ClusterMonitoringHandler;
