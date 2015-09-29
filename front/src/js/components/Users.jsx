var React = require('react/addons');
var ValidationMixin = require('react-validation-mixin');
var Joi = require('joi');
var $ = require('jquery');
var _ = require('lodash');

var Utils = require('../utils/Utils');
var UserActions = require('../actions/UserActions');
var UserStore = require('../stores/UserStore');
var ValidationRender = require('../mixins/ValidationRender');
var UserItem = require('../components/UserItem');

var Users = React.createClass({
    mixins: [ValidationMixin, ValidationRender, React.addons.LinkedStateMixin],
    validatorTypes: {
        username: Joi.string().min(4).max(20).regex(/^[a-zA-Z0-9_-]+$/).trim().label('ID'),
        displayName: Joi.string().max(255).trim().required().label('Name'),
        password: Joi.string().min(8).max(255).regex(/^[a-zA-Z0-9!-/:-@¥[-`{-~]*$/).trim().label('Password'),
        confirmPassword: Joi.string().min(8).max(255).regex(/^[a-zA-Z0-9!-/:-@¥[-`{-~]*$/).trim().label('Confirm password')
    },
    getInitialState: function() {
        return {
            users: UserStore.getUsers(),
            username: '',
            displayName: '',
            role: 'VIEWER',
            password: '',
            confirmPassword: ''
        };
    },
    componentDidMount: function() {
        var _this = this;

        UserStore.addChangeListener(this.handleChangeUsers);
        UserActions.getUsers();
    },
    componentWillUnmount: function() {
        UserStore.removeChangeListener(this.handleChangeUsers);
    },
    render: function() {
        var me = UserStore.getMe();
        var hasPermission = !AUTH_ENABLED || me.role === 'RELUMIN_ADMIN';

        var addUserBtnView;
        var addUserModal;
        if (hasPermission) {
            addUserBtnView = (
                <button className="btn btn-default" data-toggle="modal" data-target=".add-user-modal">
                    Add user
                </button>
            );
            addUserModal = (
                <div className="modal add-user-modal" ref="add-user-modal">
                    <div className="modal-dialog">
                        <div className="modal-content">
                            <div className="modal-header">
                                <button type="button" className="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                                <h4 className="modal-title">Add user</h4>
                            </div>
                            <div className="modal-body">
                                <div className={this.getClasses('username')}>
                                    <label>ID (alphabet, numeric, -, _)</label>
                                    <input type='text' valueLink={this.linkState('username')} onBlur={this.handleValidation('username')} className='form-control' placeholder='ID' />
                                    {this.getValidationMessages('username').map(this.renderHelpText)}
                                </div>
                                <div className={this.getClasses('displayName')}>
                                    <label>Name</label>
                                    <input type='text' valueLink={this.linkState('displayName')} onBlur={this.handleValidation('displayName')} className='form-control' placeholder='Name' />
                                    {this.getValidationMessages('displayName').map(this.renderHelpText)}
                                </div>
                                <div className={this.getClasses('role')}>
                                    <label>Role</label>
                                    <select className="form-control" valueLink={this.linkState('role')}>
                                        <option value="VIEWER">VIEWER</option>
                                        <option value="RELUMIN_ADMIN">RELUMIN_ADMIN</option>
                                    </select>
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

                            </div>
                            <div className="modal-footer">
                                <button className="btn btn-default" data-dismiss="modal">Close</button>
                                <button className="btn btn-primary" onClick={this.handleClickOk}>OK</button>
                            </div>
                        </div>
                    </div>
                </div>
            );
        }

        var authSettingView;
        if (!AUTH_ENABLED) {
            authSettingView = (
                <div className="alert alert-warning">
                    Currently, 'auth.enabled' is false.
                </div>
            );
        } else if (AUTH_ENABLED && AUTH_ALLOW_ANONYMOUS) {
            authSettingView = (
                <div className="alert alert-warning">
                    Currently,'auth.enabled' is true. 'auth.allowAnonymous' is also true. <br/>
                    So anonymous user can access relumin as VIEWER.
                </div>
            );
        }

        var userItemsView;
        if (this.state.users.length === 0) {
            userItemsView = (
                <div>
                    {authSettingView}
                    <div>
                        <strong>No user.</strong>
                    </div>
                </div>
            );
        } else {
            userItemsView = (
                <div>
                    {authSettingView}
                    <table className="table table-striped">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Name</th>
                                <th>Role</th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody>
                            {
                                _.map(this.state.users, function(user) {
                                    return (
                                        <UserItem user={user} />
                                    );
                                })
                            }
                        </tbody>
                    </table>
                </div>
            );
        }

        return (
            <div className="users-components">
                <div className="row">
                    <div className="col-lg-8 col-lg-offset-2">
                        <h1 className="page-header clearfix">
                            Users
                            <div className="pull-right">
                                {addUserBtnView}
                            </div>
                        </h1>
                        <div>
                            {userItemsView}
                        </div>
                    </div>
                </div>

                {addUserModal}

                <div className="modal delete-user-modal" ref="delete-user-modal">
                    <div className="modal-dialog">
                        <div className="modal-content">
                            <div className="modal-header">
                                <button type="button" className="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                                <h4 className="modal-title">Delete user</h4>
                            </div>
                            <div className="modal-body">
                                Are you sure ?
                            </div>
                            <div className="modal-footer">
                                <button className="btn btn-default" data-dismiss="modal">Cancel</button>
                                <button className="btn btn-danger" onClick={this.handleClickDelete}>DELETE !!</button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
    },
    handleChangeUsers: function() {
        this.setState({
            users: UserStore.getUsers()
        });
    },
    handleClickOk: function(event) {
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
            UserActions.addUser(
                _.trim(_this.state.username),
                {
                    displayName: _.trim(_this.state.displayName),
                    role: _.trim(_this.state.role),
                    password: _.trim(_this.state.password)
                },
                {
                    success: function(data) {
                        Utils.showAlert({level: 'success', message: "Added user successfully."});
                        if (_this.isMounted()) {
                            _this.setState({
                                username: '',
                                displayName: '',
                                password: '',
                                confirmPassword: ''
                            });
                            _this.closeAddUserModal();
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
    },
    closeAddUserModal: function() {
        $(React.findDOMNode(this.refs['add-user-modal'])).modal('hide');
    },
    handleClickDelete: function(event) {
        var _this = this;
        var $btn = $(event.currentTarget);

        event.preventDefault();

        Utils.loading(true);
        $btn.button('loading');
        UserActions.deleteUser(
            _.trim($btn.attr('data-username')),
            {
                success: function(data) {
                    Utils.showAlert({level: 'success', message: "Deleted user successfully."});
                    if (_this.isMounted()) {
                        _this.closeDeleteUserModal();
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
    },
    closeDeleteUserModal: function() {
        $(React.findDOMNode(this.refs['delete-user-modal'])).modal('hide');
    }
});

module.exports = Users;
