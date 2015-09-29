var React = require('react');
var Router = require('react-router');
var classSet = React.addons.classSet;
var $ = require('jquery');
var Highcharts = require('react-highcharts');
var _ = require('lodash');

var ClusterInfo = React.createClass({
    render: function() {
        return (
            <div className="panel panel-default cluster-info-slots-components">
                <div className="panel-heading">Slots assigned</div>
                <div className="panel-body">
                    {this.renderSlots()}
                </div>
            </div>
        );
    },
    renderSlots: function() {
        var slots = this.props.cluster.slots;

        var config = {
            title: {
                text: ''
            },
            tooltip: {
                headerFormat: '',
                pointFormat: '<b>Slots : {point.start_slot_number}-{point.end_slot_number}</b><br>master : {point.name}<br>replicas : {point.replicas}'
            },
            plotOptions: {
                pie: {
                    allowPointSelect: true,
                    animation: false,
                    point: {
                        events: {
                            legendItemClick: function () {
                                return false;
                            }
                        }
                    },
                    cursor: 'pointer',
                    dataLabels: {
                        enabled: false
                    },
                    innerSize: '50%',
                    showInLegend: true
                }
            },
            legend: {
                itemHoverStyle: {
                    cursor: 'default'
                }
            },
            series: [{
                type: 'pie',
                data: []
            }]
        };

        _.each(slots, function(slot) {
            var data = {
                name: slot.master.host_and_port,
                y: slot.end_slot_number - slot.start_slot_number + 1,
                start_slot_number: slot.start_slot_number,
                end_slot_number: slot.end_slot_number,
                replicas: _.map(slot.replicas, function(replica) { return replica.host_and_port; }).join(', ')
            };
            config.series[0].data.push(data);
        });

        return (
            <Highcharts config={config}/>
        );
    }
});

module.exports = ClusterInfo;
