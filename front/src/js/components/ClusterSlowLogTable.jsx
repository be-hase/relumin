var React = require('react');
var _ = require('lodash');
var moment = require('moment');
var numeral = require('numeral');

var ClusterSlowLogTable = React.createClass({
    render: function() {
        var trs = _.map(this.props.slowLog.data, function(val) {
            var key = val.node_id + "-" + val.id;
            var timestamp = moment(val.time_stamp * 1000).format('YYYY/MM/DD HH:mm:ss');
            var command = val.args.join(' ');

            return (
                <tr key={key}>
                    <td>{timestamp}</td>
                    <td>{val.host_and_port}</td>
                    <td>{val.execution_time}</td>
                    <td>{command}</td>
                </tr>
            );
        });

        return (
            <div className="cluster-slowlog-table-components">
                <div className="table-responsive">
                    <table className="table table-striped">
                        <thead>
                            <tr>
                                <th>timestamp</th>
                                <th>host:port</th>
                                <th>exec time<br/>(microseconds)</th>
                                <th>command</th>
                            </tr>
                        </thead>
                        <tbody>
                            {trs}
                        </tbody>
                    </table>
                </div>
            </div>
        );
    }
});

module.exports = ClusterSlowLogTable;
