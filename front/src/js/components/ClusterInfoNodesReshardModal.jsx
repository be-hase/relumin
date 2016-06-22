var React = require('react');
var $ = require('jquery');
var _ = require('lodash');
var ValidationMixin = require('react-validation-mixin');
var Joi = require('joi');
var Select = require('react-select');

var TribActions = require('../actions/TribActions');
var ValidationRender = require('../mixins/ValidationRender');
var Utils = require('../utils/Utils');

var ClusterInfoNodesReshardModal = React.createClass({
    mixins: [ValidationMixin, ValidationRender, React.addons.LinkedStateMixin],
    getInitialState: function() {
        return {
            slotCount: '',
            fromNodeIds: '',
            toNodeId: '',
            masterNodeOptions: []
        };
    },
    validatorTypes: {
        slotCount:  Joi.number().integer().min(0).max(16384).label('Slot count'),
        fromNodeIds:  Joi.string().trim().required().label('From nodes')
    },
    render: function() {
        var fromNodeIds = _.filter(this.state.fromNodeIds.split(','), function(val) {
            return !!val;
        });

        return (
            <div className="modal reshard-node-modal">
                <div className="modal-dialog">
                    <div className="modal-content">
                        <div className="modal-header">
                            <button type="button" className="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                            <h4 className="modal-title">Reshard node</h4>
                        </div>
                        <div className="modal-body">
                            <div className={this.getClasses('slotCount')}>
                                <label>Receive slot count</label>
                                <input type='text' valueLink={this.linkState('slotCount')} onBlur={this.handleValidation('slotCount')} className='form-control' placeholder='1000' />
                                {this.getValidationMessages('slotCount').map(this.renderHelpText)}
                            </div>
                            <div className={this.getClasses('fromNodeIds')}>
                                <label>From nodes</label>
                                <Select name="reshard-to-master-node" multi={true} value={fromNodeIds} delimiter="," options={this.state.masterNodeOptions} clearable={false} onChange={this.handleChangeFromMasterNodes}/>
                                {this.getValidationMessages('fromNodeIds').map(this.renderHelpText)}
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
    handleChangeFromMasterNodes: function(val) {
        this.setState({
            fromNodeIds: val
        });
    },
    handleClickOK: function(event) {
        var _this = this;
        var $btn = $(event.currentTarget);

        event.preventDefault();
        var onValidate = function(error, validationErrors) {
            if (error) {
                return;
            }

            Utils.loading(true, true);
            $btn.button('loading');
            TribActions.reshard(
                {
                    clusterName: _.trim(_this.props.cluster.cluster_name),
                    slotCount: _.trim(_this.state.slotCount),
                    fromNodeIds: _.trim(_this.state.fromNodeIds),
                    toNodeId: _.trim(_this.state.toNodeId)
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

module.exports = ClusterInfoNodesReshardModal;
