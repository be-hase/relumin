var React = require('react');
var _ = require('lodash');
var Select = require('react-select');
var DateRangePicker = require('react-bootstrap-daterangepicker');
var moment = require('moment');

var RedisConstants = require('../constants/RedisConstants');
var ClusterActions = require('../actions/ClusterActions');
var Utils = require('../utils/Utils');
var NodeMetricsQueryStore = require('../stores/NodeMetricsQueryStore');

var ClusterMonitoringQuery = React.createClass({
    getInitialState: function () {
        var query = NodeMetricsQueryStore.getNodeMetricsQuery(this.props.cluster.cluster_name);

        var ranges = Utils.getPickerRanges();
        var chosenLabel = 'Last 24h';

        return {
            ranges: ranges,
            chosenLabel: chosenLabel,
            startDate: ranges[chosenLabel][0],
            endDate: ranges[chosenLabel][1],
            nodesValues: query.nodes,
            metricsNameValues: query.fields,
            autoRefresh: query.autoRefresh
        };
    },
    componentDidMount: function() {
        var _this = this;

        this.authRefreshTimer = setInterval(function(){
            _this.autoRefresh();
        }, COLLECT_METRICS_INTERVAL_MILLS ? COLLECT_METRICS_INTERVAL_MILLS : 2 * 60 * 1000);
    },
    componentWillUnmount: function() {
        clearInterval(this.authRefreshTimer);
    },
    render: function() {
        var cluster = this.props.cluster;

        var nodesOptions = _.map(cluster.nodes, function(val) {
            var label = val.host_and_port;
            if (!val.master_node_id) {
                label += ' (master)';
            }
            return {value: val.node_id, label: label};
        });
        var nodesValues = _.filter(this.state.nodesValues.split(','), function(val) {
            return !!val;
        });

        var metricsNameOptions = _.map(_.filter(RedisConstants.metrics, function(val) {
            return val.valueType === 'number';
        }), function(val) {
            return {value: val.name, label: val.name};
        });
        var metricsNameValues = _.filter(this.state.metricsNameValues.split(','), function(val) {
            return !!val;
        });

        var momentFormat = 'MM-DD HH:mm';
        if (this.state.startDate.year() !== this.state.endDate.year()) {
            momentFormat = 'YYYY-MM-DD HH:mm';
        }
        var start = this.state.startDate.format(momentFormat);
        var end = this.state.endDate.format(momentFormat);
        var label = start + ' - ' + end;
        if (start === end) {
            label = start;
        }

        var autoRefreshCheckbox = (
            <div className="checkbox">
                <label>
                    <input type="checkbox" onChange={this.handleChangeAutoRefreshCheckbox} checked={this.state.autoRefresh} /> <small>Auto refresh</small>
                </label>
            </div>
        );
        if (!this.state.ranges[this.state.chosenLabel]) {
            autoRefreshCheckbox = (
                <div></div>
            );
        }

        return (
            <div className="cluster-monitoring-query-components">
                <div className="form-inline text-right">
                    {autoRefreshCheckbox}
                    <div className="form-group" style={{'marginLeft': '10px'}}>
                        <button data-toggle="modal" data-target=".nodes-modal" className="btn btn-default"><small>Node</small></button>
                    </div>
                    <div className="form-group" style={{'marginLeft': '10px'}}>
                        <button data-toggle="modal" data-target=".metrics-name-modal" className="btn btn-default"><small>Metrics name</small></button>
                    </div>
                    <div className="form-group" style={{'marginLeft': '10px'}}>
                        <DateRangePicker
                            format="YYYY-MM-DD HH:mm"
                            opens="left"
                            startDate={this.state.startDate} endDate={this.state.endDate}
                            timePicker={true} timePickerIncrement={10} timePicker12Hour={false}
                            ranges={this.state.ranges}
                            applyClass="btn-primary" cancelClass="btn-default"
                            onApply={this.handleApplyDateRangePicker}
                            >
                            <button className="btn btn-default">
                                <i className="glyphicon glyphicon-calendar"></i> <small>{label}</small>
                            </button>
                        </DateRangePicker>
                    </div>
                    <button className="btn btn-primary" onClick={this.handleGetClick} style={{'marginLeft': '10px'}}>Get</button>
                </div>
                <div className="modal nodes-modal" ref="nodes-modal">
                    <div className="modal-dialog">
                        <div className="modal-content">
                            <div className="modal-header">
                                <button type="button" className="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                                <h4 className="modal-title">Select node</h4>
                            </div>
                            <div className="modal-body">
                                <div className="form-group">
                                    <label>Nodes</label>
                                    <Select name="nodes" multi={true} value={nodesValues} delimiter="," options={nodesOptions} onChange={this.handleChangeNodesSelect}/>
                                </div>
                            </div>
                            <div className="modal-footer">
                                <button className="btn btn-default" data-dismiss="modal">OK</button>
                            </div>
                        </div>
                    </div>
                </div>
                <div className="modal metrics-name-modal" ref="metrics-name-modal">
                    <div className="modal-dialog">
                        <div className="modal-content">
                            <div className="modal-header">
                                <button type="button" className="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                                <h4 className="modal-title">Select metrics name</h4>
                            </div>
                            <div className="modal-body">
                                <div className="form-group">
                                    <label>Metrics name</label>
                                    <Select name="metricsName" multi={true} value={metricsNameValues} delimiter="," options={metricsNameOptions} onChange={this.handleChangeMetricsNameSelect}/>
                                </div>
                                <p className="text-right">ref : <a href="http://redis.io/commands/info" target="_blank">http://redis.io/commands/info</a></p>
                            </div>
                            <div className="modal-footer">
                                <button className="btn btn-default" onClick={this.handleClickDefault}>Default</button>
                                <button className="btn btn-default" data-dismiss="modal">OK</button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
    },
    handleChangeAutoRefreshCheckbox: function(event) {
        this.setState({
            autoRefresh: event.target.checked
        });
        ClusterActions.setNodeMetricsQueryOnlyAutoRefresh(this.props.cluster.cluster_name, event.target.checked);
    },
    handleApplyDateRangePicker: function(event, picker) {
        var ranges = Utils.getPickerRanges();

        var startDate = picker.startDate;
        var endDate = picker.endDate;
        var chosen = ranges[picker.chosenLabel];
        if (chosen) {
            startDate = chosen[0];
            endDate = chosen[1];
        }

        this.setState({
            ranges: ranges,
            chosenLabel: picker.chosenLabel,
            startDate: startDate,
            endDate: endDate,
            autoRefresh: chosen ? this.state.autoRefresh : false
        });
    },
    autoRefresh: function() {
        if (!this.state.autoRefresh) {
            return;
        }

        var ranges = Utils.getPickerRanges();

        var chosen = ranges[this.state.chosenLabel];
        if (!chosen) {
            return;
        }
        var startDate = chosen[0];
        var endDate = chosen[1];

        this.setState({
            ranges: ranges,
            startDate: startDate,
            endDate: endDate
        });

        var query = {
            start: startDate.format('x'),
            end: endDate.format('x'),
            isAutoRefresh: true
        };

        ClusterActions.setNodeMetricsQuery(this.props.cluster.cluster_name, query);
    },
    handleChangeNodesSelect: function(val) {
        this.setState({
            nodesValues: val
        });
    },
    handleChangeMetricsNameSelect: function(val) {
        this.setState({
            metricsNameValues: val
        });
    },
    handleGetClick: function(event) {
        event.preventDefault();
        var query = {
            start: this.state.startDate.format('x'),
            end: this.state.endDate.format('x'),
            nodes: this.state.nodesValues,
            fields: this.state.metricsNameValues,
            isAutoRefresh: false
        };
        if (!query.nodes) {
            Utils.showAlert({
                message: 'Nodes is empty.',
                level: 'error'
            });
            return;
        }
        if (!query.fields) {
            Utils.showAlert({
                message: 'Metrics name is empty.',
                level: 'error'
            });
            return;
        }

        ClusterActions.setNodeMetricsQuery(this.props.cluster.cluster_name, query);
    },
    handleClickDefault: function(event) {
        event.preventDefault();
        this.setState({
            metricsNameValues: NodeMetricsQueryStore.getDefaultNodeMetricsNames()
        });
    }
});

module.exports = ClusterMonitoringQuery;
