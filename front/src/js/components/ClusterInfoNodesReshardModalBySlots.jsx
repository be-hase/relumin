var React = require('react');
var Router = require('react-router');
var classSet = React.addons.classSet;
var $ = require('jquery');
var _ = require('lodash');
var ValidationMixin = require('react-validation-mixin');
var Joi = require('joi');

var TribActions = require('../actions/TribActions');
var ValidationRender = require('../mixins/ValidationRender');
var Utils = require('../utils/Utils');

var ClusterInfoNodesReshardModalBySlots = React.createClass({
    mixins: [ValidationMixin, ValidationRender, React.addons.LinkedStateMixin],
    getInitialState: function() {
        return {
            slots: '',
            toNodeId: ''
        };
    },
    validatorTypes: {
        slots:  Joi.string().trim().required().label('Slots range')
    },
    render: function() {
        return (
            <div className="modal reshard-node-modal">
                <div className="modal-dialog">
                    <div className="modal-content">
                        <div className="modal-header">
                            <button type="button" className="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                            <h4 className="modal-title">Reshard node</h4>
                        </div>
                        <div className="modal-body">
                            <div className={this.getClasses('slots')}>
                                <label>Slots range. Comma reparated. And allow using range-format by '-'.</label>
                                <input type='text' valueLink={this.linkState('slots')} onBlur={this.handleValidation('slots')} className='form-control' placeholder='500,1000-2000,3000-3500' />
                                {this.getValidationMessages('slots').map(this.renderHelpText)}
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
            TribActions.reshardBySlots(
                {
                    clusterName: _.trim(_this.props.cluster.cluster_name),
                    slots: _.trim(_this.state.slots),
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

module.exports = ClusterInfoNodesReshardModalBySlots;
