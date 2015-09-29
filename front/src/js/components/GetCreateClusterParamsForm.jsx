var React = require('react/addons');
var ValidationMixin = require('react-validation-mixin');
var $ = require('jquery');
var Joi = require('joi');

var TribActions  = require('../actions/TribActions');
var ValidationRender = require('../mixins/ValidationRender');
var Utils = require('../utils/Utils');

var GetCreateClusterParamsForm = React.createClass({
    mixins: [ValidationMixin, ValidationRender, React.addons.LinkedStateMixin],
    validatorTypes: {
        replicas: Joi.number().integer().min(0).label('Replica number.'),
        hostAndPorts:  Joi.string().trim().required().label('Host and ports')
    },
    getInitialState: function() {
        return {
            replicas: '',
            hostAndPorts: ''
        };
    },
    render: function() {
        return (
            <div className="get-create-cluster-params-form-components">
                <h3>Get recommend cluster setting</h3>
                <div className={this.getClasses('replicas')}>
                    <label>Replica number</label>
                    <input type='text' valueLink={this.linkState('replicas')} onBlur={this.handleValidation('replicas')} className='form-control' placeholder='Replica number' />
                    {this.getValidationMessages('replicas').map(this.renderHelpText)}
                </div>
                <div className={this.getClasses('hostAndPorts')}>
                    <label>Host and ports(host:port). Comma reparated. And allow using range-format by '-'.</label>
                    <input type='text' valueLink={this.linkState('hostAndPorts')} onBlur={this.handleValidation('hostAndPorts')} className='form-control' placeholder='127.0.0.0:1000,127.0.0.0:2000-2004' />
                    {this.getValidationMessages('hostAndPorts').map(this.renderHelpText)}
                </div>
                <div className="modal-footer">
                    <button className="btn btn-default" onClick={this.handleClickClear}>Clear</button>
                    <button className="btn btn-primary" onClick={this.handleClickGet}>Get</button>
                </div>
            </div>
        );
    },
    reset: function() {
        this.clearValidations();
        this.setState(this.getInitialState());
    },
    handleClickClear: function(event) {
        event.preventDefault();
        this.reset();
    },
    handleClickGet: function(event) {
        var _this = this;
        var $btn = $(event.currentTarget);

        event.preventDefault();
        var onValidate = function(error, validationErrors) {
            if (error) {
                return;
            }

            Utils.loading(true);
            $btn.button('loading');
            TribActions.getCreateClusterParams(
                {
                    replicas: _this.state.replicas,
                    hostAndPorts: _this.state.hostAndPorts
                },
                {
                    complete: function() {
                        Utils.loading(false);
                        if (_this.isMounted()) {
                            $btn.button('reset');
                        }
                    }
                }
            );
        };

        this.validate(onValidate);
    }
});

module.exports = GetCreateClusterParamsForm;
