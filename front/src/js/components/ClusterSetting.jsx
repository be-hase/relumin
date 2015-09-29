var React = require('react');
var Router = require('react-router');
var classSet = React.addons.classSet;
var $ = require('jquery');
var _ = require('lodash');
var moment = require('moment');

var ClusterActions = require('../actions/ClusterActions');
var ClusterNoticeStore = require('../stores/ClusterNoticeStore');
var ClusterSettingGeneral = require('../components/ClusterSettingGeneral');
var ClusterSettingThreshold = require('../components/ClusterSettingThreshold');
var ClusterSettingDelete = require('../components/ClusterSettingDelete');
var Utils = require('../utils/Utils');

var ClusterSetting = React.createClass({
    getInitialState: function() {
        return {
            notice: ClusterNoticeStore.getClusterNotice(this.props.cluster.cluster_name)
        };
    },
    componentDidMount: function() {
        var _this = this;

        ClusterNoticeStore.addChangeListener(this.handleChangeClusterNotice);

        ClusterActions.getClusterNotice(this.props.cluster.cluster_name);
    },
    componentWillUnmount: function() {
        ClusterNoticeStore.removeChangeListener(this.handleChangeClusterNotice);
    },
    render: function() {
        return (
            <div className="cluster-setting-components">
                <ClusterSettingGeneral cluster={this.props.cluster} notice={this.state.notice} />
                <ClusterSettingThreshold cluster={this.props.cluster} notice={this.state.notice} />
                <ClusterSettingDelete cluster={this.props.cluster} />
            </div>
        );
    },
    handleChangeClusterNotice: function() {
        this.setState({
            notice: ClusterNoticeStore.getClusterNotice(this.props.cluster.cluster_name)
        });
    }
});

module.exports = ClusterSetting;
