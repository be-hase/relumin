var React = require('react/addons');
var ValidationMixin = require('react-validation-mixin');
var Joi = require('joi');
var Router = require('react-router');
var classSet = React.addons.classSet;
var $ = require('jquery');
var _ = require('lodash');

var ClusterActions = require('../actions/ClusterActions');
var Utils = require('../utils/Utils');
var ValidationRender = require('../mixins/ValidationRender');
var UserStore = require('../stores/UserStore');

var ClusterSettingChangeName = React.createClass({
    mixins: [ValidationMixin, ValidationRender, React.addons.LinkedStateMixin, Router.Navigation],
    validatorTypes: {
        newClusterName: Joi.string().max(50).regex(/^[a-zA-Z0-9_-]+$/).trim().required().label('Cluster name')
    },
    getInitialState: function() {
        return {
            newClusterName: ''
        };
    },
    render: function() {
        var me = UserStore.getMe();
        var hasPermission = !AUTH_ENABLED || me.role === 'RELUMIN_ADMIN';

        if (!hasPermission) {
            return (
                <div />
            );
        }

        return (
            <div className="cluster-setting-change-cluster-name-components">
                <div className="panel panel-default">
                    <div className="panel-heading clearfix">
                        Change cluster name
                    </div>
                    <div className="panel-body">
                        <button className="btn btn-default" data-toggle="modal" data-target=".change-cluster-name-modal">Change cluster name</button>
                    </div>
                </div>
                <div className="modal change-cluster-name-modal" ref="change-cluster-name-modal">
                    <div className="modal-dialog">
                        <div className="modal-content">
                            <div className="modal-header">
                                <button type="button" className="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                                <h4 className="modal-title">Change cluster name</h4>
                            </div>
                            <div className="modal-body">
                                <div className={this.getClasses('newClusterName')}>
                                    <label>New cluster name (alphabet, numeric, -, _)</label>
                                    <input type='text' valueLink={this.linkState('newClusterName')} onBlur={this.handleValidation('newClusterName')} className='form-control' placeholder='New cluster name' />
                                    {this.getValidationMessages('newClusterName').map(this.renderHelpText)}
                                </div>
                            </div>
                            <div className="modal-footer">
                                <button className="btn btn-default" data-dismiss="modal">Close</button>
                                <button className="btn btn-primary" onClick={this.handleClickOk}>OK</button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
    },
    handleClickOk: function(event) {
        var _this = this;
        var $btn = $(event.currentTarget);

        event.preventDefault();
        var onValidate = function(error, validationErrors) {
            if (error) {
                return;
            }

            var newClusterName = _.trim(_this.state.newClusterName);
            Utils.loading(true);
            $btn.button('loading');
            ClusterActions.changeClusterName(
                _this.props.cluster.cluster_name,
                {
                    newClusterName: newClusterName,
                },
                {
                    success: function() {
                        if (_this.isMounted()) {
                            _this.hideModal();
                        }
                        Utils.showAlert({
                            message: 'Change cluster name successfully',
                            level: 'success'
                        });
                        //_this.transitionTo('/cluster/' + newClusterName);
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
    hideModal: function() {
        $(React.findDOMNode(this.refs['change-cluster-name-modal'])).modal('hide');
    }
});

module.exports = ClusterSettingChangeName;
