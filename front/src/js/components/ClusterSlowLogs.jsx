var React = require('react');

var ClusterActions = require('../actions/ClusterActions');
var NodeMetricsQueryStore = require('../stores/NodeMetricsQueryStore');
var NodeSlowLogsStore = require('../stores/NodeSlowLogsStore');
var Utils = require('../utils/Utils');

var ClusterSlowLogs = React.createClass({
    getInitialState: function() {
        return {
            slowlogs: NodeSlowLogsStore.getSlowLogs(this.props.cluster.cluster_name),
            loading: true,
            fullViewConfig: {}
        };
    },
    componentDidMount: function() {
        var _this = this;
        var query = NodeMetricsQueryStore.getNodeMetricsQuery(this.props.cluster.cluster_name);

        NodeSlowLogsStore.addChangeListener(this.handleChangeNodeSlowLogs);
        // NodeSlowLogsQueryStore.addChangeListener(this.handleChangeQuery);

        ClusterActions.getNodeSlowLogs(
            this.props.cluster.cluster_name,
            {
                nodes: query.nodes
            },
            {
                complete: function() {
                    _this.setState({loading: false});
            }
        });
    },
    componentWillUnmount: function() {
    },
    render: function() {
        console.log(this.state);
        return (
            <div className="panel panel-default cluster-info-nodes-components">
                <div className="panel-heading clearfix">
                    SlowLogs
                    <div className="pull-right">
                        <span>
                            <span>select : </span>
                        </span>
                    </div>
                </div>

                <div className="panel-body">
                    <div className="table-responsive">
                        <table className="table table-striped">
                            <thead>
                                <tr>
                                    <th><span data-toggle="tooltip" data-placement="top" title="">slowlog id</span></th>
                                    <th><span data-toggle="tooltip" data-placement="top" title="">time_stamp</span></th>
                                    <th><span data-toggle="tooltip" data-placement="top" title="">execution_time</span></th>
                                    <th><span data-toggle="tooltip" data-placement="top" title="">args</span></th>
                                    <th></th>
                                </tr>
                            </thead>
                            <tbody>
                            {
                                _.map(this.state.slowlogs, function(slowlogs) {
                                  return (
                                    _.map(slowlogs, function(slowlog) {
                                      return (
                                        <tr>
                                          <td>{slowlog.id}</td>
                                          <td>{slowlog.time_stamp}</td>
                                          <td>{slowlog.execution_time}</td>
                                          <td>{slowlog.args.join(' ')}</td>
                                        </tr>
                                      )
                                    })
                                  )
                                })
                            }
                            </tbody>
                        </table>

                    </div>
                </div>
            </div>
        );
    },
    handleChangeNodeSlowLogs: function() {
      console.log(123123);
        this.setState({
            slowlogs: NodeSlowLogsStore.getSlowLogs(this.props.cluster.cluster_name)
        });
    }
});

module.exports = ClusterSlowLogs;
