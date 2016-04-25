var React = require('react');
var Router = require('react-router');
var classSet = React.addons.classSet;

var ClusterStore = require('../stores/ClusterStore');

var ClusterTab = React.createClass({
    mixins: [Router.State],
    render: function() {
        var _this = this;
        var paths = this.getPathname().split('/');
        var tab = paths[3] || 'info';
        var clusterName = this.props.cluster.cluster_name;
        var list = [];

        _.each(['Info', 'Monitoring', 'Slowlog', 'Setting'], function(val){
            var lowerVal = val.toLowerCase();
            var liClass = classSet({
                'active': lowerVal === tab
            });
            var href = '#/cluster/' + clusterName;
            if (lowerVal !== 'info') {
                href += '/' + lowerVal;
            }

            list.push(
                <li key={val} className={liClass}><a href={href}>{val}</a></li>
            );
        });
        return (
            <div className="cluster-tab-components">
                <h2><span className="glyphicon glyphicon-menu-right"></span> {clusterName}</h2>
                <ul className="nav nav-tabs">
                    {list}
                </ul>
            </div>
        );
    }
});

module.exports = ClusterTab;
