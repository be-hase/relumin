var React = require('react/addons');
var ValidationMixin = require('react-validation-mixin');
var Joi = require('joi');
var $ = require('jquery');
var _ = require('lodash');

var Utils = require('../utils/Utils');
var UserActions = require('../actions/UserActions');
var ValidationRender = require('../mixins/ValidationRender');

var ChangePassword = React.createClass({
    mixins: [ValidationMixin, ValidationRender, React.addons.LinkedStateMixin],
    validatorTypes: {
        oldPassword: Joi.string().min(8).max(255).regex(/^[a-zA-Z0-9!-/:-@¥[-`{-~]*$/).trim().label('Old password'),
        password: Joi.string().min(8).max(255).regex(/^[a-zA-Z0-9!-/:-@¥[-`{-~]*$/).trim().label('Password'),
        confirmPassword: Joi.string().min(8).max(255).regex(/^[a-zA-Z0-9!-/:-@¥[-`{-~]*$/).trim().label('Confirm password')
    },
    getInitialState: function() {
        return {
            oldPassword: '',
            password: '',
            confirmPassword: ''
        };
    },
    render: function() {
        return (
            <div className="change-password-components">
                <div className="row">
                    <div className="col-lg-8 col-lg-offset-2">
                        <div className="panel panel-default">
                            <div className="panel-heading">
                                Change password
                            </div>
                            <div className="panel-body">
                                <div className={this.getClasses('oldPassword')}>
                                    <label>Old password</label>
                                    <input type='password' valueLink={this.linkState('oldPassword')} onBlur={this.handleValidation('oldPassword')} className='form-control' placeholder='Old password' />
                                    {this.getValidationMessages('oldPassword').map(this.renderHelpText)}
                                </div>
                                <div className={this.getClasses('password')}>
                                    <label>Password</label>
                                    <input type='password' valueLink={this.linkState('password')} onBlur={this.handleValidation('password')} className='form-control' placeholder='Password' />
                                    {this.getValidationMessages('password').map(this.renderHelpText)}
                                </div>
                                <div className={this.getClasses('confirmPassword')}>
                                    <label>Confirm password</label>
                                    <input type='password' valueLink={this.linkState('confirmPassword')} onBlur={this.handleValidation('confirmPassword')} className='form-control' placeholder='Confirm password' />
                                    {this.getValidationMessages('confirmPassword').map(this.renderHelpText)}
                                </div>
                                <div className="modal-footer">
                                    <button className="btn btn-default" onClick={this.handleClickClear}>Clear</button>
                                    <button className="btn btn-primary" onClick={this.handleClickSaveBtn}>Update password</button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
    },
    handleClickClear: function(event) {
        this.clearValidations();
        this.setState(this.getInitialState());
    },
    handleClickSaveBtn: function(event) {
        var _this = this;
        var $btn = $(event.currentTarget);

        event.preventDefault();
        var onValidate = function(error, validationErrors) {
            if (error) {
                return;
            }
            if (_this.state.password !== _this.state.confirmPassword) {
                Utils.showAlert({level: 'error', message: "Password doesn't match the confirmation"});
                return;
            }

            Utils.loading(true);
            $btn.button('loading');
            UserActions.changePassword(
                {
                    oldPassword: _.trim(_this.state.oldPassword),
                    password: _.trim(_this.state.password)
                },
                {
                    success: function(data) {
                        Utils.showAlert({level: 'success', message: "Password changed successfully."});
                        if (_this.isMounted()) {
                            _this.setState(_this.getInitialState());
                        }
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
    }
});

module.exports = ChangePassword;
