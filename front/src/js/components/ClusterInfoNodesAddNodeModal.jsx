var React = require('react');
var Router = require('react-router');
var classSet = React.addons.classSet;
var $ = require('jquery');
var _ = require('lodash');
var ValidationMixin = require('react-validation-mixin');
var Joi = require('joi');
var Select = require('react-select');

var TribActions = require('../actions/TribActions');
var ValidationRender = require('../mixins/ValidationRender');
var Utils = require('../utils/Utils');

var ClusterInfoNodesAddNodeModal = React.createClass({
    mixins: [ValidationMixin, ValidationRender, React.addons.LinkedStateMixin],
    validatorTypes: {
        hostAndPort:  Joi.string().regex(/^.+:[0-9]{1,5}$/).trim().required().label('Host and port')
    },
    getInitialState: function() {
        return {
            hostAndPort: '',
            masterNodeId: ''
        };
    },
    render: function() {
        var masterNodeOptions = _.map(Utils.getMasterNodes(this.props.cluster.nodes), function(node) {
            return {value: node.node_id, label: node.host_and_port};
        });

        return (
            <div className="modal add-node-modal cluster-info-nodes-add-node-components" ref="add-node-modal">
                <div className="modal-dialog">
                    <div className="modal-content">
                        <div className="modal-header">
                            <button type="button" className="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                            <h4 className="modal-title">Add node</h4>
                        </div>
                        <div className="modal-body">
                            <div className={this.getClasses('hostAndPort')}>
                                <label>Host and port (host:port)</label>
                                <input type='text' valueLink={this.linkState('hostAndPort')} onBlur={this.handleValidation('hostAndPort')} className='form-control' placeholder='127.0.0.0:6379' />
                                {this.getValidationMessages('hostAndPort').map(this.renderHelpText)}
                            </div>
                            <div className='form-group'>
                                <label>Master node (option)</label>
                                <Select name="master-node" value={this.state.masterNodeId} options={masterNodeOptions} onChange={this.handleChangeMasterNodeSelect}/>
                                <p className="help-block">If master node id is empty, added as a empty master node.</p>
                            </div>
                        </div>
                        <div className="modal-footer">
                            <button className="btn btn-default" data-dismiss="modal">Cancel</button>
                            <button className="btn btn-primary" onClick={this.handleClickSendAddNode}>OK</button>
                        </div>
                    </div>
                </div>
            </div>
        );
    },
    handleChangeMasterNodeSelect: function(val) {
        this.setState({
            masterNodeId: val
        });
    },
    handleClickSendAddNode: function(event) {
        var _this = this;
        var $btn = $(event.currentTarget);

        event.preventDefault();
        var onValidate = function(error, validationErrors) {
            if (error) {
                return;
            }

            Utils.loading(true, true);
            $btn.button('loading');
            TribActions.addNode(
                {
                    clusterName: _.trim(_this.props.cluster.cluster_name),
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
        };
        this.validate(onValidate);
    },
    showModal: function() {
        $(React.findDOMNode(this)).modal('show');
    },
    hideModal: function() {
        $(React.findDOMNode(this)).modal('hide');
    }
});

module.exports = ClusterInfoNodesAddNodeModal;
