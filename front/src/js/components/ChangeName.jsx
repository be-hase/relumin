var React = require('react/addons');
var ValidationMixin = require('react-validation-mixin');
var Joi = require('joi');
var $ = require('jquery');
var _ = require('lodash');

var Utils = require('../utils/Utils');
var UserActions = require('../actions/UserActions');
var UserStore = require('../stores/UserStore');
var ValidationRender = require('../mixins/ValidationRender');

var ChangeName = React.createClass({
    mixins: [ValidationMixin, ValidationRender, React.addons.LinkedStateMixin],
    validatorTypes: {
        displayName: Joi.string().max(255).trim().required().label('Name')
    },
    getInitialState: function() {
        var me = UserStore.getMe();
        return {
            displayName: me ? me.display_name : ''
        };
    },
    render: function() {
        return (
            <div className="change-name-components">
                <div className="row">
                    <div className="col-lg-8 col-lg-offset-2">
                        <h1 className="page-header">Change profile</h1>
                        <div className="panel panel-default">
                            <div className="panel-heading">
                                Change name
                            </div>
                            <div className="panel-body">
                                <div className={this.getClasses('displayName')}>
                                    <label>Name</label>
                                    <input type='text' valueLink={this.linkState('displayName')} onBlur={this.handleValidation('displayName')} className='form-control' placeholder='Name' />
                                    {this.getValidationMessages('displayName').map(this.renderHelpText)}
                                </div>
                                <div className="modal-footer">
                                    <button className="btn btn-default" onClick={this.handleClickClear}>Clear</button>
                                    <button className="btn btn-primary" onClick={this.handleClickSaveBtn}>Update name</button>
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

            Utils.loading(true);
            $btn.button('loading');
            UserActions.updateMe(
                {
                    displayName: _.trim(_this.state.displayName)
                },
                {
                    success: function(data) {
                        Utils.showAlert({level: 'success', message: "Name changed successfully."});
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

module.exports = ChangeName;
