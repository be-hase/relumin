var React = require('react');

var ClusterSlowLogPager = React.createClass({
    render: function() {
        var slowLog = this.props.slowLog;
        var pathBase = "#/cluster/" + this.props.cluster.cluster_name + "/slowlog/";
        var currentHref = pathBase + this.props.pageNo;
        var prev;
        var next;

        if (slowLog.prev_page) {
            prev = (
                <li><a href={pathBase + slowLog.prev_page}>Prev</a></li>
            );
        } else {
            prev = (
                <li className="disabled"><a href={currentHref}>Prev</a></li>
            );
        }
        if (slowLog.next_page) {
            next = (
                <li><a href={pathBase + slowLog.next_page}>Next</a></li>
            );
        } else {
            next = (
                <li className="disabled"><a href={currentHref}>Next</a></li>
            );
        }

        return (
            <div className="cluster-slowlog-pager-components clearfix">
                <div className="pull-right">
                    <nav>
                        <ul className="pagination">
                            {prev}
                            <li className="active"><a href={currentHref}>{this.props.pageNo}</a></li>
                            {next}
                        </ul>
                    </nav>
                </div>
            </div>
        );
    }
});

module.exports = ClusterSlowLogPager;
