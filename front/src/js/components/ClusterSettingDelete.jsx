var React = require('react');
var $ = require('jquery');

var ClusterActions = require('../actions/ClusterActions');
var Utils = require('../utils/Utils');

var UserStore = require('../stores/UserStore');

var ClusterSettingDelete = React.createClass({
    render: function() {
        var me = UserStore.getMe();
        var hasPermission = !AUTH_ENABLED || me.role === 'RELUMIN_ADMIN';

        if (!hasPermission) {
            return (
                <div />
            );
        }

        return (
            <div className="cluster-setting-delete-components">
                <div className="panel panel-default">
                    <div className="panel-heading clearfix">
                        Unregist cluster
                    </div>
                    <div className="panel-body">
                        <button className="btn btn-danger" data-toggle="modal" data-target=".unregist-cluster-modal">Unregist cluster.</button>
                    </div>
                </div>
                <div className="modal unregist-cluster-modal" ref="unregist-cluster-modal">
                    <div className="modal-dialog">
                        <div className="modal-content">
                            <div className="modal-header">
                                <button type="button" className="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                                <h4 className="modal-title">Unregist this cluster</h4>
                            </div>
                            <div className="modal-body">
                                Are you sure ?
                            </div>
                            <div className="modal-footer">
                                <button className="btn btn-default" data-dismiss="modal">Close</button>
                                <button className="btn btn-danger" onClick={this.handleClickDelete}>Delete !!</button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
    },
    handleClickDelete: function(event) {
        var _this = this;
        var $btn = $(event.currentTarget);

        Utils.loading(true);
        $btn.button('loading');
        ClusterActions.deleteCluster(
            this.props.cluster.cluster_name,
            {
                success: function() {
                    if (_this.isMounted()) {
                        _this.hideModal();
                    }
                    Utils.showAlert({
                        message: 'Unregisted successfully',
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
    },
    hideModal: function() {
        $(React.findDOMNode(this.refs['unregist-cluster-modal'])).modal('hide');
    }
});

module.exports = ClusterSettingDelete;
