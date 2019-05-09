var React = require('react/addons');
var ValidationMixin = require('react-validation-mixin');
var Joi = require('joi');
var $ = require('jquery');

var ClusterActions = require('../actions/ClusterActions');
var Utils = require('../utils/Utils');

var RegisterClusterModal = React.createClass({
    mixins: [ValidationMixin, React.addons.LinkedStateMixin],
    validatorTypes: {
        clusterName: Joi.string().max(50).regex(/^[a-zA-Z0-9_-]+$/).trim().required().label('Cluster name'),
        hostAndPort:  Joi.string().regex(/^.+:[0-9]{1,5}$/).trim().required().label('Host and port')
    },
    getInitialState: function() {
        return {
            clusterName: '',
            hostAndPort: ''
        };
    },
    render: function() {
        return (
            <div className="modal register-cluster-modal-components" ref="modal">
                <div className="modal-dialog">
                    <div className="modal-content">
                        <div className="modal-header">
                            <button type="button" className="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                            <h4 className="modal-title">Register cluster</h4>
                        </div>
                        <div className="modal-body">
                            <div className={this.getClasses('clusterName')}>
                                <label>Cluster name (alphabet, numeric, -, _)</label>
                                <input type='text' valueLink={this.linkState('clusterName')} onBlur={this.handleValidation('clusterName')} className='form-control' placeholder='Cluster name' />
                                {this.getValidationMessages('clusterName').map(this.renderHelpText)}
                            </div>
                            <div className={this.getClasses('hostAndPort')}>
                                <label>Host and port (host:port)</label>
                                <input type='text' valueLink={this.linkState('hostAndPort')} onBlur={this.handleValidation('hostAndPort')} className='form-control' placeholder='127.0.0.0:6379' />
                                {this.getValidationMessages('hostAndPort').map(this.renderHelpText)}
                            </div>
                        </div>
                        <div className="modal-footer">
                            <button className="btn btn-default" data-dismiss="modal">Close</button>
                            <button className="btn btn-primary" onClick={this.handleSubmit}>OK</button>
                        </div>
                    </div>
                </div>
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
    handleSubmit: function(event) {
        var _this = this;
        var $btn = $(event.currentTarget);

        event.preventDefault();
        var onValidate = function(error, validationErrors) {
            if (error) {
                return;
            }

            Utils.loading(true);
            $btn.button('loading');
            ClusterActions.setClusters(
                {
                    clusterName: _.trim(_this.state.clusterName),
                    hostAndPort: _.trim(_this.state.hostAndPort)
                },
                {
                    success: function() {
                        if (_this.isMounted()) {
                            _this.setState(_this.getInitialState());
                            _this.closeModal();
                        }
                        Utils.showAlert({
                            message: 'Registered successfully',
                            level: 'success'
                        });
                    },
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
    },
    closeModal: function() {
        $(React.findDOMNode(this.refs.modal)).modal('hide');
    }
});

module.exports = RegisterClusterModal;
