var React = require('react');
var Router = require('react-router');

var ClusterStore = require('../stores/ClusterStore');
var NoClusterRender = require('../mixins/NoClusterRender');
var ClusterTab = require('../components/ClusterTab');
var ClusterSetting = require('../components/ClusterSetting');
var Utils = require('../utils/Utils');

var ClusterSettingHandler = React.createClass({
    mixins: [Router.State, NoClusterRender],
    componentDidMount: function() {
        Utils.pageChangeInit();
        ClusterStore.addChangeListener(this.onChangeHandle);
    },
    componentWillUnmount: function() {
        ClusterStore.removeChangeListener(this.onChangeHandle);
    },
    render: function() {
        var cluster = ClusterStore.getCluster(this.getParams().clusterName);
        var key = 'cluster-setting-' + this.getParams().clusterName;

        if (cluster) {
            return (
                <div key={key}>
                    <ClusterTab cluster={cluster} />
                    <ClusterSetting cluster={cluster} />
                </div>
            );
        } else {
            return this.renderNoCluster(this.props.params.clusterName, key);
        }
    },
    onChangeHandle: function() {
        this.forceUpdate();
    }
});

module.exports = ClusterSettingHandler;
