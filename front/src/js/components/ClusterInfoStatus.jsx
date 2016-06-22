var React = require('react');
var classSet = React.addons.classSet;
var $ = require('jquery');
var _ = require('lodash');

var Utils = require('../utils/Utils');

var ClusterInfoStatus = React.createClass({
    componentDidMount: function() {
        $(React.findDOMNode(this)).find('[data-toggle="tooltip"]').tooltip();
    },
    componentDidUpdate: function() {
        $(React.findDOMNode(this)).find('[data-toggle="tooltip"]').tooltip();
    },
    render: function() {
        var cluster = this.props.cluster;

        var statusClass = classSet({
            'status': true,
            'label': true,
            'label-success': cluster.status === 'ok',
            'label-warning': cluster.status === 'warn',
            'label-danger': cluster.status === 'fail'
        });

        return (
            <div className="panel panel-default cluster-info-status-components">
                <div className="panel-heading">Info</div>
                <div className="panel-body">
                    <div className="table-wrap">
                        <table className="table table-striped">
                            <tr key="tr-Status">
                                <th><span title={Utils.getRedisInfoByName('cluster_state').desc} data-toggle="tooltip" data-placement="right">cluster_state</span></th>
                                <td><span className={statusClass} > </span> <span className="status-text">{cluster.status}</span></td>
                            </tr>
                            <tr key="tr-Slots-assigned">
                                <th><span title={Utils.getRedisInfoByName('cluster_slots_assigned').desc} data-toggle="tooltip" data-placement="right">cluster_slots_assigned</span></th>
                                <td>{cluster.info.cluster_slots_assigned}</td>
                            </tr>
                            <tr key="tr-Slots-OK">
                                <th><span title={Utils.getRedisInfoByName('cluster_slots_ok').desc} data-toggle="tooltip" data-placement="right">cluster_slots_ok</span></th>
                                <td>{cluster.info.cluster_slots_ok}</td>
                            </tr>
                            <tr key="tr-Slots-FAIL">
                                <th><span title={Utils.getRedisInfoByName('cluster_slots_fail').desc} data-toggle="tooltip" data-placement="right">cluster_slots_fail</span></th>
                                <td>{cluster.info.cluster_slots_fail}</td>
                            </tr>
                            <tr key="tr-Slots-PFAIL">
                                <th><span title={Utils.getRedisInfoByName('cluster_slots_pfail').desc} data-toggle="tooltip" data-placement="right">cluster_slots_pfail</span></th>
                                <td>{cluster.info.cluster_slots_pfail}</td>
                            </tr>
                            <tr key="tr-Known-nodes">
                                <th><span title={Utils.getRedisInfoByName('cluster_known_nodes').desc} data-toggle="tooltip" data-placement="right">cluster_known_nodes</span></th>
                                <td>{cluster.info.cluster_known_nodes}</td>
                            </tr>
                            <tr key="tr-Size">
                                <th><span title={Utils.getRedisInfoByName('cluster_size').desc} data-toggle="tooltip" data-placement="right">cluster_size</span></th>
                                <td>{cluster.info.cluster_size}</td>
                            </tr>
                            <tr key="tr-Current-epoch">
                                <th><span title={Utils.getRedisInfoByName('cluster_current_epoch').desc} data-toggle="tooltip" data-placement="right">cluster_current_epoch</span></th>
                                <td>{cluster.info.cluster_current_epoch}</td>
                            </tr>
                            <tr key="tr-Stats-messages-sent">
                                <th><span title={Utils.getRedisInfoByName('cluster_stats_messages_sent').desc} data-toggle="tooltip" data-placement="right">cluster_stats_messages_sent</span></th>
                                <td>{cluster.info.cluster_stats_messages_sent}</td>
                            </tr>
                            <tr key="tr-Stats-messages-received">
                                <th><span title={Utils.getRedisInfoByName('cluster_stats_messages_received').desc} data-toggle="tooltip" data-placement="right">cluster_stats_messages_received</span></th>
                                <td>{cluster.info.cluster_stats_messages_received}</td>
                            </tr>
                        </table>
                    </div>
                </div>
            </div>
        );
    }
});

module.exports = ClusterInfoStatus;
