var React = require('react');
var Router = require('react-router');
var $ = require('jquery');

var ClusterSlowLog = require('../components/ClusterSlowLog');
var ClusterStore = require('../stores/ClusterStore');
var NoClusterRender = require('../mixins/NoClusterRender');
var ClusterTab = require('../components/ClusterTab');
var Utils = require('../utils/Utils');

var ClusterSlowLogHandler = React.createClass({
    mixins: [Router.State, NoClusterRender],
    componentDidMount: function() {
        Utils.pageChangeInit();
    },
    render: function() {
        var cluster = ClusterStore.getCluster(this.getParams().clusterName);
        var pageNo = this.getParams().pageNo;
        if (!pageNo || !$.isNumeric(pageNo) || pageNo <= 0) {
            pageNo = 1;
        }
        var key = 'cluster-slowlog-' + this.getParams().clusterName + '-' + pageNo;

        if (cluster) {
            return (
                <div key={key}>
                    <ClusterTab cluster={cluster} />
                    <ClusterSlowLog cluster={cluster} pageNo={pageNo} />
                </div>
            );
        } else {
            return this.renderNoCluster(this.props.params.clusterName, key);
        }
    }
});

module.exports = ClusterSlowLogHandler;
