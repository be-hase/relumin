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

        _.each(['Info', 'Monitoring', 'Setting'], function(val){
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
            <ul className="nav nav-tabs cluster-tab-components">
                {list}
            </ul>
        );
    }
});

module.exports = ClusterTab;
