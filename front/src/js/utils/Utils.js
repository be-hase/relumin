var _ = require('lodash');
var $ = require('jquery');
var moment = require('moment');

var AppActions = require('../actions/AppActions');
var RedisConstants = require('../constants/RedisConstants');

var Utils = {
    showAlert: function(param) {
        AppActions.showAlert(param);
    },
    loading: function(loading, block) {
        if (loading) {
            $('.global-loading').show();
        } else {
            $('.global-loading').hide();
        }
        if (block) {
            if (loading) {
                Utils.blockDisplay();
            } else {
                Utils.unblockDisplay();
            }
        }
    },
    wrapSuccess: function(callbacks, success) {
        if (callbacks.success) {
            callbacks.success = _.wrap(callbacks.success, function(func, data){
                success(data);
                func(data);
            });
        } else {
            callbacks.success = success;
        }
    },
    blockDisplay: function() {
        var $body = $('body');
        var exists = $body.find('.global-modal-backdrop').length > 0;
        if (exists) {
            return;
        }
        $body.append('<div class="modal-backdrop in global-modal-backdrop"></div>');
    },
    unblockDisplay: function() {
        $('.global-modal-backdrop').remove();
    },
    pageChangeInit: function() {
        $('.modal-backdrop').remove();
        Utils.loading(false);
    },
    getRedisMetricsByName: function(name) {
        return _.find(RedisConstants.metrics, function(val) {
            return val.name === name;
        });
    },
    getRedisInfoByName: function(name) {
        return _.find(RedisConstants.info, function(val) {
            return val.name === name;
        });
    },
    getNodeByNodeId: function(nodes, nodeId) {
        return _.find(nodes, function(val) {
            return val.node_id === nodeId;
        });
    },
    getAnyAliveNode: function(nodes) {
        return _.find(nodes, function(node) {
            var hasPfailOrFail = _.findIndex(node.flags, function(flag) { return flag === 'fail?' || flag === 'fail'; }) >= 0;
            return !hasPfailOrFail;
        });
    },
    getMasterNodes: function(nodes) {
        return _.filter(nodes, function(node) {
            return _.findIndex(node.flags, function(flag) { return flag === 'master'; }) >= 0;
        });
    },
    getSlaveNodesOfMasterNode: function(nodes, masterNodeId) {
        return _.filter(nodes, function(node) {
            return node.master_node_id === masterNodeId;
        });
    },
    getPickerRanges: function() {
        return {
            'Last 1h': [moment().subtract(1, 'h'), moment()],
            'Last 6h': [moment().subtract(6, 'h'), moment()],
            'Last 12h': [moment().subtract(12, 'h'), moment()],
            'Last 24h': [moment().subtract(24, 'h'), moment()],
            'Last 2d': [moment().subtract(2, 'd'), moment()],
            'Last 7d': [moment().subtract(6, 'd'), moment()],
            'Last 30d': [moment().subtract(30, 'd'), moment()]
        };
    }
};

window.Utils = Utils;

module.exports = Utils;
