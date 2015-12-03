var React = require('react');
var Router = require('react-router');
var classSet = React.addons.classSet;
var $ = require('jquery');
var _ = require('lodash');
var ValidationMixin = require('react-validation-mixin');
var Joi = require('joi');
var Select = require('react-select');
var moment = require('moment');

var ClusterActions = require('../actions/ClusterActions');
var TribActions = require('../actions/TribActions');

var ClusterInfoNodesAddNodeModal = require('../components/ClusterInfoNodesAddNodeModal');
var ClusterInfoNodesReshardModal = require('../components/ClusterInfoNodesReshardModal');
var ClusterInfoNodesReshardModalBySlots = require('../components/ClusterInfoNodesReshardModalBySlots');
var ClusterInfoNodesReplicateModal = require('../components/ClusterInfoNodesReplicateModal');
var ClusterInfoNodesFailoverModal = require('../components/ClusterInfoNodesFailoverModal');
var ClusterInfoNodesDeleteNodeModal = require('../components/ClusterInfoNodesDeleteNodeModal');
var ClusterInfoNodesDeleteFailNodeModal = require('../components/ClusterInfoNodesDeleteFailNodeModal');
var ClusterInfoNodesShutdownModal = require('../components/ClusterInfoNodesShutdownModal');

var UserStore = require('../stores/UserStore');

var Utils = require('../utils/Utils');

var ClusterInfoNodes = React.createClass({
    getInitialState: function() {
        return {
            nodeFilterType: 'all'
        };
    },
    componentDidMount: function() {
        $(React.findDOMNode(this)).find('[data-toggle="tooltip"]').tooltip();
        $(React.findDOMNode(this)).find('[data-toggle="popover"]').popover();
    },
    componentDidUpdate: function() {
        $(React.findDOMNode(this)).find('[data-toggle="tooltip"]').tooltip();
        $(React.findDOMNode(this)).find('[data-toggle="popover"]').popover();
    },
    render: function() {
        var _this = this;
        var nodes = this.props.cluster.nodes;
        var me = UserStore.getMe();
        var hasPermission = !AUTH_ENABLED || me.role === 'RELUMIN_ADMIN';

        var nodesList = _.map(nodes, function(node) {
            var isMaster = !!_.find(node.flags, function(flag) { return flag === 'master'; });
            var isDander = !!_.find(node.flags, function(flag) { return flag === 'fail'; });
            var isWarning = !!_.find(node.flags, function(flag) { return flag === 'fail?'; });
            var nodeClass = classSet({
                danger: isDander,
                warning: isWarning
            });

            if (_this.state.nodeFilterType === 'all') {
                //ok
            } else if (_this.state.nodeFilterType === 'master' && !isMaster) {
                return;
            } else if (_this.state.nodeFilterType === 'slave' && isMaster) {
                return;
            }

            var nodeAction;
            if (hasPermission) {
                var action;
                if (isDander || isWarning) {
                    action = [
                        (<li><a href="#" onClick={function(event){ _this.handleClickDeleteDeadNode(event, node); }}>Delete from cluster</a></li>)
                    ];
                } else {
                    if (isMaster) {
                        action = [
                            (<li><a href="#" onClick={function(event){ _this.handleClickReshard(event, node); }}>Reshard by nodes</a></li>),
                            (<li className="divider"></li>),
                            (<li><a href="#" onClick={function(event){ _this.handleClickReshardBySlots(event, node); }}>Reshard by slots range</a></li>),
                            (<li className="divider"></li>)
                        ];
                    } else {
                        action = [
                            (<li><a href="#" onClick={function(event){ _this.handleClickFailover(event, node); }}>Failover</a></li>),
                            (<li className="divider"></li>)
                        ];
                    }
                    action.push((<li><a href="#" onClick={function(event){ _this.handleClickReplicate(event, node); }}>Replicate</a></li>));
                    action.push((<li className="divider"></li>));
                    action.push((<li><a href="#" onClick={function(event){ _this.handleClickDeleteNode(event, node); }}>Delete from cluster</a></li>));
                    action.push((<li className="divider"></li>));
                    action.push((<li><a href="#" onClick={function(event){ _this.handleClickShutdownNode(event, node); }}>Shutdown</a></li>));
                }
                
                nodeAction = (
                    <div className="btn-group node-action-btn">
                        <button type="button" className="btn btn-default btn-xs dropdown-toggle" data-toggle="dropdown" aria-expanded="false">
                            Node action <span className="caret"></span>
                        </button>
                        <ul className="dropdown-menu dropdown-menu-right" role="menu">
                            {action}
                        </ul>
                    </div>
                );
            }

            var masterNode = Utils.getNodeByNodeId(_this.props.cluster.nodes, node.master_node_id);

            var flags = [];
            _.each(node.flags, function(flag) {
                switch (flag) {
                    case 'myself':
                        break;
                    case 'master':
                        var slaveNodes = Utils.getSlaveNodesOfMasterNode(_this.props.cluster.nodes, node.node_id);
                        var htmlContent = "";
                        _.each(slaveNodes, function(slaveNode) {
                            htmlContent += slaveNode.host_and_port + "<br>";
                        });
                        flags.push((<span key={flag} className="label label-success" data-title="slave nodes" data-toggle="popover" data-placement="top" data-content={htmlContent} data-html="true" data-trigger="hover">{flag}</span>));
                        break;
                    case 'slave':
                        flags.push((<span key={flag} className="label label-default">{flag}</span>));
                        break;
                    case 'fail':
                        flags.push((<span key={flag} className="label label-default">{flag}</span>));
                        break;
                    case 'fail?':
                        flags.push((<span key={flag} className="label label-default">{flag}</span>));
                        break;
                    case 'handshake':
                        flags.push((<span key={flag} className="label label-default">{flag}</span>));
                        break;
                    case 'noaddr':
                        flags.push((<span key={flag} className="label label-default">{flag}</span>));
                        break;
                    default:
                        flags.push((<span key={flag} className="label label-default">{flag}</span>));
                        break;
                }
            });

            return (
                <tr key={node.node_id} className={nodeClass}>
                    <td data-label="node_id">{node.node_id}</td>
                    <td data-label="host_and_port">{node.host_and_port}</td>
                    <td data-label="flags">
                        {flags}
                    </td>
                    <td data-label="master_host_and_port">{!!masterNode ? masterNode.host_and_port : ''}</td>
                    <td data-label="config_epoch">{node.config_epoch}</td>
                    <td data-label="connect">{node.connect ? 'connected' : 'disconnected'}</td>
                    <td data-label="served_slots">
                        <ul className="slots-list">
                        {_.isEmpty(node.served_slots) ? '' : _.map(node.served_slots.split(','), function(slots) {
                            return (<li key={slots}>{slots}</li>);
                        })}
                        </ul>
                    </td>
                    <td data-label="slot_count">{node.slot_count}</td>
                    <td>{nodeAction}</td>
                </tr>
            );
        });

        var addNodeBtnView;
        if (hasPermission) {
            addNodeBtnView = (
                <button className="btn btn-default btn-xs add-node-btn" data-toggle="modal" data-target=".add-node-modal">
                    Add node
                </button>
            );
        }

        var nodeFilterType = {
            all: {
                classes: 'btn btn-default btn-xs ' + (_this.state.nodeFilterType === 'all' ? 'active' : '')
            },
            master: {
                classes: 'btn btn-default btn-xs ' + (_this.state.nodeFilterType === 'master' ? 'active' : '')
            },
            slave: {
                classes: 'btn btn-default btn-xs ' + (_this.state.nodeFilterType === 'slave' ? 'active' : '')
            }
        };

        return (
            <div className="panel panel-default cluster-info-nodes-components">
                <div className="panel-heading clearfix">
                    Nodes
                    <div className="pull-right">
                        <span>
                            <span>select : </span>
                            <div className="btn-group" data-toggle="buttons">
                                <button className={nodeFilterType.all.classes} onClick={function(){_this.handleClickNodeFilterType('all');}}>All</button>
                                <button className={nodeFilterType.master.classes} onClick={function(){_this.handleClickNodeFilterType('master');}}>master</button>
                                <button className={nodeFilterType.slave.classes} onClick={function(){_this.handleClickNodeFilterType('slave');}}>slave</button>
                            </div>
                        </span>
                        {addNodeBtnView}
                    </div>
                </div>

                <div className="panel-body">
                    <div className="table-responsive">
                        <table className="table table-striped">
                            <thead>
                                <tr>
                                    <th><span data-toggle="tooltip" data-placement="top" title="The node ID, a 40 characters random string generated when a node is created and never changed again (unless CLUSTER RESET HARD is used).">node id</span></th>
                                    <th><span data-toggle="tooltip" data-placement="top" title="The node address where clients should contact the node to run queries.">host:port</span></th>
                                    <th><span data-toggle="tooltip" data-placement="top" title="A list of comma separated flags: myself, master, slave, fail?, fail, handshake, noaddr, noflags. Flags are explained in detail in the next section.">flags</span></th>
                                    <th><span data-toggle="tooltip" data-placement="top" title="If the node is a slave, and the master is known, the master node host:port.">master node</span></th>
                                    <th><span data-toggle="tooltip" data-placement="top" title="The configuration epoch (or version) of the current node (or of the current master if the node is a slave). Each time there is a failover, a new, unique, monotonically increasing configuration epoch is created. If multiple nodes claim to serve the same hash slots, the one with higher configuration epoch wins.">config-epoch</span></th>
                                    <th><span data-toggle="tooltip" data-placement="top" title="The state of the link used for the node-to-node cluster bus. We use this link to communicate with the node. Can be connected or disconnected.">link state</span></th>
                                    <th><span data-toggle="tooltip" data-placement="top" title="This is the list of hash slots served by this node. If the entry is just a number, is parsed as such. If it is a range, it is in the form start-end, and means that the node is responsible for all the hash slots from start to end including the start and end values.">slot</span></th>
                                    <th><span data-toggle="tooltip" data-placement="top" title="This is total slot count which this node has.">slot count</span></th>
                                    <th></th>
                                </tr>
                            </thead>
                            <tbody>
                                {nodesList}
                            </tbody>
                        </table>

                        <ClusterInfoNodesReshardModal cluster={this.props.cluster} ref="reshard-node-modal" />
                        <ClusterInfoNodesReshardModalBySlots cluster={this.props.cluster} ref="reshard-node-by-slots-modal" />
                        <ClusterInfoNodesReplicateModal cluster={this.props.cluster} ref="replicate-node-modal" />
                        <ClusterInfoNodesFailoverModal cluster={this.props.cluster} ref="failover-node-modal" />
                        <ClusterInfoNodesDeleteNodeModal cluster={this.props.cluster} ref="delete-node-modal" />
                        <ClusterInfoNodesDeleteFailNodeModal cluster={this.props.cluster} ref="delete-dead-node-modal" />
                        <ClusterInfoNodesShutdownModal cluster={this.props.cluster} ref="shutdown-node-modal" />
                    </div>
                </div>

                <ClusterInfoNodesAddNodeModal cluster={this.props.cluster} />
            </div>
        );
    },
    renderHelpText: function(message) {
        return (
          <p className="help-block">{message}</p>
        );
    },
    getClasses: function(field) {
        return React.addons.classSet({
          'form-group': true,
          'has-error': !this.isValid(field)
        });
    },
    getMasterNodesWithoutByNodeId: function(nodeId) {
        return _.filter(Utils.getMasterNodes(this.props.cluster.nodes), function(node) {
            return node.node_id !== nodeId;
        });
    },
    handleClickReshard: function(event, node) {
        var modal = this.refs['reshard-node-modal'];
        event.preventDefault();

        var masterNodes = this.getMasterNodesWithoutByNodeId(node.node_id);
        var reshardMasterNodeOptions = _.map(masterNodes, function(val) {
            return {value: val.node_id, label: val.host_and_port};
        });

        modal.setState({
            slotCount: '',
            fromNodeIds: '',
            toNodeId: node.node_id,
            masterNodeOptions: reshardMasterNodeOptions
        });
        modal.clearValidations();
        modal.showModal();
    },
    handleClickReshardBySlots: function(event, node) {
        var modal = this.refs['reshard-node-by-slots-modal'];
        event.preventDefault();

        modal.setState({
            slots: '',
            toNodeId: node.node_id
        });
        modal.clearValidations();
        modal.showModal();
    },
    handleClickReplicate: function(event, node) {
        var modal = this.refs['replicate-node-modal'];
        event.preventDefault();

        var masterNodes = this.getMasterNodesWithoutByNodeId(node.node_id);
        if (node.master_node_id) {
            masterNodes = _.filter(masterNodes, function(masterNode) {
                return masterNode.node_id !== node.master_node_id;
            });
        }
        var replicatMasterNodeOptions = _.map(masterNodes, function(val) {
            return {value: val.node_id, label: val.host_and_port};
        });

        modal.setState({
            hostAndPort: node.host_and_port,
            masterNodeId: masterNodes[0].node_id,
            masterNodeOptions: replicatMasterNodeOptions
        });
        modal.showModal();
    },
    handleClickFailover: function(event, node) {
        var modal = this.refs['failover-node-modal'];
        event.preventDefault();

        modal.setState({
            hostAndPort: node.host_and_port
        });
        modal.showModal();
    },
    handleClickDeleteNode: function(event, node) {
        var modal = this.refs['delete-node-modal'];
        event.preventDefault();

        modal.setState({
            nodeId: node.node_id,
            reset: '',
            shutdown: true
        });
        modal.showModal();
    },
    handleClickDeleteDeadNode: function(event, node) {
        var modal = this.refs['delete-dead-node-modal'];
        event.preventDefault();

        modal.setState({
            nodeId: node.node_id
        });
        modal.showModal();
    },
    handleClickShutdownNode: function(event, node) {
        var modal = this.refs['shutdown-node-modal'];
        event.preventDefault();

        modal.setState({
            hostAndPort: node.host_and_port
        });
        modal.showModal();
    },
    handleClickNodeFilterType: function(val) {
        this.setState({
            nodeFilterType: val,
        });
    }
});

module.exports = ClusterInfoNodes;
