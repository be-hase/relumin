var React = require('react');
var Router = require('react-router');
var classSet = React.addons.classSet;

var ClusterStore = require('../stores/ClusterStore');

var tabs = [
    {
        name: 'Info',
        link: 'info'
    },
    {
        name: 'Monitoring',
        link: 'monitoring'
    },
    {
        name: 'Slow log',
        link: 'slowlog'
    },
    {
        name: 'Setting',
        link: 'setting'
    }
];

var ClusterTab = React.createClass({
    mixins: [Router.State],
    render: function() {
        var paths = this.getPathname().split('/');
        var tab = paths[3] || 'info';
        var clusterName = this.props.cluster.cluster_name;
        var list = [];

        _.each(tabs, function(val){
            var liClass = classSet({
                'active': val.link === tab
            });
            var href = '#/cluster/' + clusterName;
            if (val.link !== 'info') {
                href += '/' + val.link;
            }
            if (val.link === 'slowlog') {
                href += '/1';
            }

            list.push(
                <li key={val.link} className={liClass}><a href={href}>{val.name}</a></li>
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
