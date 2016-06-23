var React = require('react');
var _ = require('lodash');
var moment = require('moment');
var Highcharts = require('react-highcharts');
var numeral = require('numeral');

var ClusterSlowLogGraph = React.createClass({
    render: function() {
        return (
            <div className="cluster-slowlog-graph-components">
                {this.renderGraph()}
            </div>
        );
    },
    renderGraph: function() {
        var data = _.sortBy(this.props.slowLog.data, function(val) {
            return val.time_stamp;
        });

        var series_node = {};
        _.each(data, function(val) {
            if (!series_node[val.host_and_port]) {
                series_node[val.host_and_port] = {
                    name: val.host_and_port,
                    data: []
                };
            }

            // series_node[val.host_and_port].data.push([val.time_stamp * 1000, val.execution_time]);
            series_node[val.host_and_port].data.push({
                x: val.time_stamp * 1000,
                y: val.execution_time,
                command: val.args.join(' ')
            });

        });

        var series = [];
        _.each(series_node, function(val) {
            series.push(val);
        });
        
        var config = {
            chart: {
                type: 'scatter',
                zoomType: 'x',
                height: 350
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
                    text: 'exec time (microseconds)'
                }
            },
            legend: {
                borderWidth: 0
            },
            plotOptions: {
                scatter: {
                    marker: {
                        radius: 5,
                        states: {
                            hover: {
                                enabled: true,
                                lineColor: 'rgb(100,100,100)'
                            }
                        }
                    },
                    states: {
                        hover: {
                            marker: {
                                enabled: false
                            }
                        }
                    },
                    tooltip: {
                        headerFormat: '',
                        pointFormatter: function() {
                            var text = '';
                            text += 'node : ' + this.series.name + '<br/>';
                            text += 'timestamp : ' + moment(this.x).format('YYYY/MM/DD HH:mm:ss') + '<br/>';
                            text += 'exec time : ' + numeral(this.y).format('0,0.[0000]') + '<br/>';
                            text += 'command : <br/>' + this.command;
                            return text;
                        }
                    }
                }
            },
            series: series
        };

        return (
            <Highcharts config={config} />
        );
    }
});

module.exports = ClusterSlowLogGraph;
