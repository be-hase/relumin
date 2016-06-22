var React = require('react');
var $ = require('jquery');
var _ = require('lodash');

var TribActions = require('../actions/TribActions');
var Utils = require('../utils/Utils');

var ClusterInfoNodesReplicateModal = React.createClass({
    getInitialState: function() {
        return {
            hostAndPort: ''
        };
    },
    render: function() {
        return (
            <div className="modal failover-node-modal">
                <div className="modal-dialog">
                    <div className="modal-content">
                        <div className="modal-header">
                            <button type="button" className="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                            <h4 className="modal-title">Shutdown node</h4>
                        </div>
                        <div className="modal-body">
                            Are you sure ?<br/>
                            (This action is convinient for debugging.)
                        </div>
                        <div className="modal-footer">
                            <button className="btn btn-default" data-dismiss="modal">Cancel</button>
                            <button className="btn btn-danger" onClick={this.handleClickOK}>SHUTDOWN !!</button>
                        </div>
                    </div>
                </div>
            </div>
        );
    },
    handleClickOK: function(event) {
        var _this = this;
        var $btn = $(event.currentTarget);

        Utils.loading(true, true);
        $btn.button('loading');
        TribActions.shutdown(
            {
                clusterName: _.trim(_this.props.cluster.cluster_name),
                hostAndPort: _.trim(_this.state.hostAndPort)
            },
            {
                success: function() {
                    if (_this.isMounted()) {
                        _this.setState(_this.getInitialState());
                        _this.hideModal();
                    }
                },
                complete: function() {
                    Utils.loading(false, true);
                    if (_this.isMounted()) {
                        $btn.button('reset');
                    }
                }
            }
        );
    },
    showModal: function() {
        $(React.findDOMNode(this)).modal('show');
    },
    hideModal: function() {
        $(React.findDOMNode(this)).modal('hide');
    }
});

module.exports = ClusterInfoNodesReplicateModal;
