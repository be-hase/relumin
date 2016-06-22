var React = require('react');
var Router = require('react-router');

var ClusterSlowLogPager = require('../components/ClusterSlowLogPager');
var ClusterSlowLogGraph = require('../components/ClusterSlowLogGraph');
var ClusterSlowLogTable = require('../components/ClusterSlowLogTable');

var ClusterActions = require('../actions/ClusterActions');
var ClusterSlowLogStore = require('../stores/ClusterSlowLogStore');
var Utils = require('../utils/Utils');

var ClusterSlowLog = React.createClass({
    mixins: [Router.State],
    getInitialState: function() {
        return {
            slowLog: ClusterSlowLogStore.getSlowLog(this.props.cluster.cluster_name, this.props.pageNo)
        };
    },
    componentDidMount: function() {
        ClusterSlowLogStore.addChangeListener(this.handleChangeClusterSlowLog);

        var limit = 1000;
        var offset = (this.props.pageNo - 1) * limit;
        ClusterActions.getSlowLog(this.props.cluster.cluster_name, {
            offset: offset,
            limit: limit
        });
    },
    componentWillUnmount: function() {
        ClusterSlowLogStore.removeChangeListener(this.handleChangeClusterSlowLog);
    },
    render: function() {
        if (!this.state.slowLog) {
            return (
                <div></div>
            );
        }

        if (this.state.slowLog.data.length === 0) {
            return (
                <div className="well">
                    No slow log.
                </div>
            );
        }

        return (
            <div className="cluster-slowlog-components">
                <ClusterSlowLogPager cluster={this.props.cluster} slowLog={this.state.slowLog} pageNo={this.props.pageNo} />
                <ClusterSlowLogGraph cluster={this.props.cluster} slowLog={this.state.slowLog} />
                <ClusterSlowLogTable cluster={this.props.cluster} slowLog={this.state.slowLog} />
            </div>
        );
    },
    handleChangeClusterSlowLog: function() {
        this.setState({
            slowLog: ClusterSlowLogStore.getSlowLog(this.props.cluster.cluster_name, this.props.pageNo)
        });
    }
});

module.exports = ClusterSlowLog;
