var React = require('react');
var Router = require('react-router');
var classSet = React.addons.classSet;
var $ = require('jquery');
var _ = require('lodash');
var Select = require('react-select');

var TribActions = require('../actions/TribActions');
var Utils = require('../utils/Utils');

var ClusterInfoNodesReplicateModal = React.createClass({
    getInitialState: function() {
        return {
            hostAndPort: '',
            masterNodeId: '',
            masterNodeOptions: []
        };
    },
    render: function() {
        return (
            <div className="modal replicate-node-modal">
                <div className="modal-dialog">
                    <div className="modal-content">
                        <div className="modal-header">
                            <button type="button" className="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                            <h4 className="modal-title">Replicate node</h4>
                        </div>
                        <div className="modal-body">
                            <div className='form-group'>
                                <label>Master node</label>
                                <Select name="replicate-master-node" value={this.state.masterNodeId} options={this.state.masterNodeOptions} clearable={false} onChange={this.handleChangeMasterNode}/>
                            </div>
                        </div>
                        <div className="modal-footer">
                            <button className="btn btn-default" data-dismiss="modal">Cancel</button>
                            <button className="btn btn-primary" onClick={this.handleClickOK}>OK</button>
                        </div>
                    </div>
                </div>
            </div>
        );
    },
    handleChangeMasterNode: function(val) {
        this.setState({
            masterNodeId: val
        });
    },
    handleClickOK: function(event) {
        var _this = this;
        var $btn = $(event.currentTarget);

        Utils.loading(true, true);
        $btn.button('loading');
        TribActions.replicate(
            _this.props.cluster.cluster_name,
            {
                hostAndPort: _.trim(_this.state.hostAndPort),
                masterNodeId: _.trim(_this.state.masterNodeId)
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
