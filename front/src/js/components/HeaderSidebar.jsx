var React = require('react');
var Router = require('react-router');

var ClusterStore = require('../stores/ClusterStore');
var UserStore = require('../stores/UserStore');
var ClusterActions = require('../actions/ClusterActions');

var HeaderSidebar = React.createClass({
    mixins: [Router.State],
    getInitialState: function() {
        return {
            clusters: ClusterStore.getClusters()
        };
    },
    componentDidMount: function() {
        ClusterStore.addChangeListener(this.onChangeHandle);
    },
    componentWillUnmount: function() {
        ClusterStore.removeChangeListener(this.onChangeHandle);
    },
    render: function() {
        var clusterNames = [];
        var currentClusterName = this.getParams().clusterName;
        var me = UserStore.getMe();

        _.each(this.getClusterLinks(), function(val){
            var href = "#cluster/" + val.clusterName;
            var className = "status label label-" + val.status;
            var liClassName = '';

            if (val.clusterName === currentClusterName) {
                liClassName = 'selected';
            }

            clusterNames.push(
                <li key={val.clusterName} className={liClassName}><a href={href}><span className={className}> </span><span className="cluster-name">{val.clusterName}</span></a></li>
            );
        });
        if (clusterNames.length === 0) {
            clusterNames.push(
                <li key="no-cluster"><span>Cluster is not registered.</span></li>
            );
        }

        var registAndCreateClusterMenuView;
        if (!AUTH_ENABLED || me.role === 'RELUMIN_ADMIN') {
            registAndCreateClusterMenuView = [
                (<li><a href="#" data-toggle="modal" data-target=".regist-cluster-modal-components">Regist cluster</a></li>),
                (<li><a href="#/create-cluster">Create cluster</a></li>)
            ];
        }

        var loginDropDownView;
        if (AUTH_ENABLED) {
            if (USER.login) {
                loginDropDownView = (
                    <li className="dropdown user-dropdown">
                        <a href="#" className="dropdown-toggle" data-toggle="dropdown"><i className="fa fa-user"></i> {USER.username}<b className="caret"></b></a>
                        <ul className="dropdown-menu">
                            <li><a href="#/change-profile"><i className="fa fa-gear"></i> Change profile</a></li>
                            <li className="divider"></li>
                            <li><a href="/logout"><i className="fa fa-power-off"></i> Logout</a></li>
                        </ul>
                    </li>
                );
            } else {
                loginDropDownView = (
                    <li><a href="/login">Login</a></li>
                );
            }
        }

        return (
            <nav className="navbar navbar-inverse navbar-fixed-top header-sidebar-components" role="navigation">
                <div className="navbar-header">
                    <button type="button" className="navbar-toggle" data-toggle="collapse" data-target=".navbar-ex1-collapse">
                        <span className="icon-bar"></span>
                        <span className="icon-bar"></span>
                        <span className="icon-bar"></span>
                    </button>
                    <a className="navbar-brand" href="#">Relumin</a>
                </div>
                <div className="collapse navbar-collapse navbar-ex1-collapse">
                    <ul className="nav navbar-nav side-nav">
                        <li><span><i className="glyphicon glyphicon-list"></i> <strong> Cluster list</strong></span></li>
                        {clusterNames}
                    </ul>
                    <ul className="nav navbar-nav navbar-right dis-iframe">
                        {registAndCreateClusterMenuView}
                        <li><a href="#/users">Users</a></li>
                        {loginDropDownView}
                    </ul>
                </div>
            </nav>
        );
    },
    onChangeHandle: function() {
        this.setState({clusters: ClusterStore.getClusters()});
    },
    getClusterLinks: function() {
        var clusters = this.state.clusters;
        var clusterLinks = [];

        _.each(clusters, function(val){
            var status;
            if (val.status === 'fail') {
                status = 'danger';
            } else if (val.status === 'warn') {
                status = 'warning';
            } else {
                status = 'success';
            }
            clusterLinks.push({
                clusterName: val.cluster_name,
                status: status
            });
        });

        return clusterLinks;
    }
});

module.exports = HeaderSidebar;
