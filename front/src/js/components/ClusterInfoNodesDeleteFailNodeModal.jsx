var React = require('react');
var Router = require('react-router');
var classSet = React.addons.classSet;
var $ = require('jquery');
var _ = require('lodash');
var Select = require('react-select');

var TribActions = require('../actions/TribActions');
var Utils = require('../utils/Utils');

var ClusterInfoNodesDeleteFailNodeModal = React.createClass({
    mixins: [React.addons.LinkedStateMixin],
    getInitialState: function() {
        return {
            nodeId: ''
        };
    },
    render: function() {
        return (
            <div className="modal delete-node-modal" ref="delete-node-modal">
                <div className="modal-dialog">
                    <div className="modal-content">
                        <div className="modal-header">
                            <button type="button" className="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                            <h4 className="modal-title">Delete FAIL node</h4>
                        </div>
                        <div className="modal-body">
                            Are you sure ?
                        </div>
                        <div className="modal-footer">
                            <button className="btn btn-default" data-dismiss="modal">Cancel</button>
                            <button className="btn btn-danger" onClick={this.handleClickOK}>DELETE !!</button>
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
        TribActions.deleteNode(
            {
                clusterName: _.trim(_this.props.cluster.cluster_name),
                nodeId: _.trim(_this.state.nodeId),
                isFail: true
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

module.exports = ClusterInfoNodesDeleteFailNodeModal;
