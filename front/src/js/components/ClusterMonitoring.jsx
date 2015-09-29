var React = require('react');
var Router = require('react-router');
var classSet = React.addons.classSet;
var $ = require('jquery');
var Highcharts = require('react-highcharts');
var _ = require('lodash');
var Spinner = require('react-spinkit');
var moment = require('moment');
var numeral = require('numeral');

var ClusterActions = require('../actions/ClusterActions');
var NodeMetricsStore = require('../stores/NodeMetricsStore');
var NodeMetricsQueryStore = require('../stores/NodeMetricsQueryStore');
var Utils = require('../utils/Utils');

var ClusterMonitoring = React.createClass({
    getInitialState: function() {
        return {
            metrics: NodeMetricsStore.getNodeMetrics(this.props.cluster.cluster_name),
            loading: true,
            fullViewConfig: {}
        };
    },
    componentDidMount: function() {
        var _this = this;
        var query = NodeMetricsQueryStore.getNodeMetricsQuery(this.props.cluster.cluster_name);

        NodeMetricsStore.addChangeListener(this.handleChangeNodeMetrics);
        NodeMetricsQueryStore.addChangeListener(this.handleChangeQuery);

        ClusterActions.getNodeMetrics(
            this.props.cluster.cluster_name,
            {
                start: false,
                end: false,
                nodes: query.nodes,
                fields: query.fields,
            },
            {
                complete: function() {
                    _this.setState({loading: false});
            }
        });

        $(React.findDOMNode(this)).find('[data-toggle="tooltip"]').tooltip();
    },
    componentDidUpdate: function() {
        $(React.findDOMNode(this)).find('[data-toggle="tooltip"]').tooltip();
    },
    componentWillUnmount: function() {
        NodeMetricsStore.removeChangeListener(this.handleChangeNodeMetrics);
        NodeMetricsQueryStore.removeChangeListener(this.handleChangeQuery);
    },
    render: function() {
        var _this = this;
        var query = NodeMetricsQueryStore.getNodeMetricsQuery(this.props.cluster.cluster_name);

        if (this.state.loading) {
            return (
                <div className="cluster-monitoring-components">
                    <div className="text-center" style={{'marginTop': '90px'}}>
                        <Spinner spinnerName="three-bounce" ref="spinner" />
                    </div>
                </div>
            );
        }

        if (_.isEmpty(this.state.metrics)) {
            return (
                <div className="cluster-monitoring-components">
                    <div className="alert alert-warning">
                        No metrics.
                    </div>
                </div>
            );
        }

        return (
            <div className="cluster-monitoring-components">
                <div className="row">
                    {
                        _.map(query.fields.split(','), function(metricsName) {
                            return _this.renderMetrics(metricsName);
                        })
                    }
                </div>
                <div className="modal full-view-modal" ref="full-view-modal">
                    <div className="modal-dialog modal-lg">
                        <div className="modal-content">
                            <div className="modal-header">
                                <button type="button" className="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                                <h4 className="modal-title">
                                </h4>
                            </div>
                            <div className="modal-body">
                            </div>
                            <div className="modal-footer">
                                <button className="btn btn-default" data-dismiss="modal">Close</button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
    },
    handleChangeNodeMetrics: function() {
        this.setState({
            metrics: NodeMetricsStore.getNodeMetrics(this.props.cluster.cluster_name)
        });
    },
    handleChangeQuery: function() {
        var _this = this;
        var query = NodeMetricsQueryStore.getNodeMetricsQuery(this.props.cluster.cluster_name);

        var requestData = {
            start: query.start,
            end: query.end,
            nodes: query.nodes,
            fields: query.fields,
        };

        if (query.isAutoRefresh) {
            ClusterActions.getNodeMetrics(
                this.props.cluster.cluster_name,
                requestData
            );
        } else {
            this.setState({
                loading: true,
                query: query
            });
            ClusterActions.getNodeMetrics(
                this.props.cluster.cluster_name,
                requestData,
                {
                    complete: function() {
                        _this.setState({loading: false});
                    }
                }
            );
        }
    },
    renderMetrics: function(metricsName) {
        var _this = this;
        var metricsInfo = Utils.getRedisMetricsByName(metricsName);
        var data = {};

        _.each(this.state.metrics, function(nodeMetrics, nodeId) {
            if (!nodeMetrics[metricsName]) {
                return;
            }

            data[nodeId] = nodeMetrics[metricsName];
            data[nodeId] = _.sortBy(data[nodeId], function(val) {
                return val[0];
            });
        });

        var config = {
            chart: {
                type: metricsInfo.graphType,
                zoomType: 'x',
                height: 280
            },
            title: {
                text: ''
            },
            xAxis: {
                type: 'datetime',
                dateTimeLabelFormats: {
                    millisecond: '%H:%M:%S.%L',
                    second: '%H:%M:%S',
                    minute: '%H:%M',
                    hour: '%H:%M',
                    day: '%m/%d',
                    week: '%m/%d',
                    month: '%Y/%m',
                    year: '%Y',
                }
            },
            yAxis: {
                title: {
                    text: ''
                }
            },
            tooltip: {
                xDateFormat: '%Y/%m/%d %H:%M',
                pointFormatter: function() {
                    return '<span style="color:'+this.color+'">\u25CF</span> ' + this.series.name + ' : <b>' + numeral(this.y).format('0,0.[0000]') + '</b><br/>';
                }
            },
            legend: {
                borderWidth: 0
            }
        };

        config.series = _.map(data, function(val, nodeId) {
            var node = Utils.getNodeByNodeId(_this.props.cluster.nodes, nodeId);
            if (!!node && !_.isEmpty(node)) {
                return {
                    name: node.host_and_port,
                    animation: false,
                    data: val
                };
            }
        });

        var handleClickFullView = function() {
            var $fullViewModal = $(React.findDOMNode(_this.refs['full-view-modal']));
            $fullViewModal.find('h4').html(metricsName + ' <i class="glyphicon glyphicon-question-sign" data-toggle="tooltip" data-placement="bottom" title="' + metricsInfo.desc + '"></i>');
            $fullViewModal.find('[data-toggle="tooltip"]').tooltip();
            $fullViewModal.modal('show');

            config.chart.height = 550;

            React.render(<Highcharts config={config}/>, $fullViewModal.find('.modal-body')[0]);
        };

        return (
            <div key={metricsName} className="col-lg-4 col-md-6">
                <div className="panel panel-default">
                    <div className="panel-heading clearfix">
                        {metricsName} <i className="glyphicon glyphicon-question-sign" data-toggle="tooltip" data-placement="top" title={metricsInfo.desc}></i>
                        <div className="pull-right">
                            <button className="btn btn-default btn-xs" onClick={handleClickFullView}>
                                <i className="glyphicon glyphicon-resize-full"></i>
                            </button>
                        </div>
                    </div>
                    <div className="panel-body">
                        <Highcharts config={config}/>
                    </div>
                </div>
            </div>
        );
    }
});

module.exports = ClusterMonitoring;
