var React = require('react/addons');
var $ = require('jquery');
var _ = require('lodash');

var Utils = require('../utils/Utils');
var UserActions = require('../actions/UserActions');
var UserStore = require('../stores/UserStore');

var Users = React.createClass({
    mixins: [React.addons.LinkedStateMixin],
    getInitialState: function() {
        return {
            role: this.props.user.role,
            initialRole: this.props.user.role
        };
    },
    render: function() {
        var user = this.props.user;
        var me = UserStore.getMe();

        var haPermission = !AUTH_ENABLED || me.role === 'RELUMIN_ADMIN' && me.username !== this.props.user.username;

        var roleView;
        if (haPermission) {
            roleView = (
                <select className="form-control" valueLink={this.linkState('role')} style={{width: '200px'}}>
                    <option value="VIEWER">VIEWER</option>
                    <option value="RELUMIN_ADMIN">RELUMIN_ADMIN</option>
                </select>
            );
        } else {
            roleView = (<span>{this.state.role}</span>);
        }

        var updateBtnView;
        if (haPermission) {
            if (this.state.role === this.state.initialRole) {
                updateBtnView = (
                    <button className="btn btn-primary btn-xs" style={{marginRight: '10px', display: 'none'}}>Update</button>
                );
            } else {
                updateBtnView = (
                    <button className="btn btn-primary btn-xs" style={{marginRight: '10px', display: 'inline-block'}} onClick={this.handleClickUpdate}>Update</button>
                );
            }
        }

        var deleteBtnView;
        if (haPermission) {
            deleteBtnView = (
                <button className="btn btn-danger btn-xs" style={{marginRight: '10px'}} onClick={this.showModal}>DELETE</button>
            );
        }

        return (
            <tr key={user.username} className="user-item-components">
                <td>{user.username}</td>
                <td>{user.display_name}</td>
                <td>
                    {roleView}
                </td>
                <td style={{width: '220px'}} className="text-right">
                    {updateBtnView}
                    {deleteBtnView}
                </td>
            </tr>
        );
    },
    showModal: function(event) {
        if (event) {
            event.preventDefault();
        }
        $('.delete-user-modal').modal('show');
        $('.delete-user-modal .btn-danger').attr('data-username', this.props.user.username);
    },
    closeModal: function() {
        $(React.findDOMNode(this.refs.modal)).modal('hide');
    },
    handleClickUpdate: function(event) {
        var _this = this;
        var $btn = $(event.currentTarget);

        event.preventDefault();

        Utils.loading(true);
        $btn.button('loading');
        UserActions.updateUser(
            _.trim(_this.props.user.username),
            {
                role: _.trim(_this.state.role),
            },
            {
                success: function(data) {
                    Utils.showAlert({level: 'success', message: "Role changed successfully."});
                    if (_this.isMounted()) {
                        _this.setState({
                            initialRole: _this.state.role,
                        });
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
    }
});

module.exports = Users;
