var React = require('react');

var ClusterActions = require('../actions/ClusterActions');

var ClusterInfoStatus = require('../components/ClusterInfoStatus');
var ClusterInfoSlots = require('../components/ClusterInfoSlots');
var ClusterInfoNodes = require('../components/ClusterInfoNodes');

var ClusterInfo = React.createClass({
    componentDidMount: function() {
        ClusterActions.getCluster(this.props.cluster.cluster_name);
    },
    render: function() {
        return (
            <div className="cluster-info-components">
                <div className="row">
                    <div className="col-lg-6">
                        <ClusterInfoStatus cluster={this.props.cluster} />
                    </div>
                    <div className="col-lg-6">
                        <ClusterInfoSlots cluster={this.props.cluster} />
                    </div>
                </div>
                <div className="row">
                    <div className="col-lg-12">
                        <ClusterInfoNodes cluster={this.props.cluster} />
                    </div>
                </div>
            </div>
        );
    }
});

module.exports = ClusterInfo;
