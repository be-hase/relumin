var React = require('react');
var Router = require('react-router');
var Route = Router.Route;
var DefaultRoute = Router.DefaultRoute;
var NotFoundRoute = Router.NotFoundRoute;

var App = require('../components/App');
var HomeHandler = require('./HomeHandler');
var CreateClusterHandler = require('./CreateClusterHandler');
var NotFoundHandler = require('./NotFoundHandler');
var ClusterInfoHandler = require('./ClusterInfoHandler');
var ClusterMonitoringHandler = require('./ClusterMonitoringHandler');
var ClusterSlowLogHandler = require('./ClusterSlowLogHandler');
var ClusterSettingHandler = require('./ClusterSettingHandler');
var ChangeProfileHandler = require('./ChangeProfileHandler');
var UsersHandler = require('./UsersHandler');

var appRoutes = (
    <Route path="/" handler={App}>
        <DefaultRoute name="home" handler={HomeHandler}/>
        <NotFoundRoute handler={NotFoundHandler} />
        <Route name="create-cluster" path="/create-cluster" handler={CreateClusterHandler} />
        <Route name="cluster-info" path="/cluster/:clusterName" handler={ClusterInfoHandler} />
        <Route name="cluster-monitoring" path="/cluster/:clusterName/monitoring" handler={ClusterMonitoringHandler} />
        <Route name="cluster-slowlog" path="/cluster/:clusterName/slowlog" handler={ClusterSlowLogHandler} />
        <Route name="cluster-setting" path="/cluster/:clusterName/setting" handler={ClusterSettingHandler} />
        <Route name="change-profile" path="/change-profile" handler={ChangeProfileHandler} />
        <Route name="users" path="/users" handler={UsersHandler} />
    </Route>
);

module.exports = appRoutes;
