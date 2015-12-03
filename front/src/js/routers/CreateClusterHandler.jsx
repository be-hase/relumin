var React = require('react');
var Router = require('react-router');

var CreateClusterForm = require('../components/CreateClusterForm');
var GetCreateClusterParamsForm = require('../components/getCreateClusterParamsForm');
var Utils = require('../utils/Utils');
var UserStore = require('../stores/UserStore');

var CreateClusterHandler = React.createClass({
    componentDidMount: function() {
        Utils.pageChangeInit();
    },
    render: function() {
        var me = UserStore.getMe();
        
        if (!AUTH_ENABLED || me.role === 'RELUMIN_ADMIN') {
            return (
                <div>
                    <h1 className="page-header">Create cluster</h1>
                    <div className="row">
                        <div className="col-lg-4">
                            <GetCreateClusterParamsForm ref="getCreateClusterParamsForm" />
                        </div>
                        <div className="col-lg-8">
                            <CreateClusterForm onReset={this.handleReset} ref="createClusterForm" />
                        </div>
                    </div>
                </div>
            );
        } else {
            return (
                <div>Page does not exists.</div>
            );
        }
    },
    handleReset: function() {
        this.refs.getCreateClusterParamsForm.reset();
        this.refs.createClusterForm.reset();
    }
});

module.exports = CreateClusterHandler;
