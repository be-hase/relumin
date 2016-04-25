var $ = require('jquery');
var _ = require('lodash');
var moment = require('moment');

var Utils = require('./Utils');

var BASE_URL;
if (__DEV__) {
    BASE_URL = 'http://localhost:8080';
} else {
    BASE_URL = '';
}

var ajaxPool = [];

function commonErrorHandle(jqXHR, textStatus) {
    var data;

    // abortのときはなにもエラー表示しない
    if (textStatus === 'abort') {
        return;
    }

    data = parseError(jqXHR);

    if (data && _.has(data, 'error') && _.has(data.error, 'message')) {
        Utils.showAlert({
            message: data.error.message,
            level: 'error'
        });
        return;
    }

    Utils.showAlert({
        message: 'Server error.',
        level: 'error'
    });
}

function parseError(jqXHR) {
    try {
        return $.parseJSON(jqXHR.responseText);
    } catch(e) {
        return false;
    }
}

var ApiUtils = {
    clearAllAjax: function() {
        ajaxPool = [];
    },
    abortAllAjax: function() {
        _.each(ajaxPool, function(value){
                value.abort();
        });
        ajaxPool = [];
    },
    addAjax: function(jqXHR) {
        ajaxPool.push(jqXHR);
    },
    completeAjax: function(jqXHR) {
        if (jqXHR) {
            ajaxPool = _.without(ajaxPool, jqXHR);
        }
    },
    Cluster: {
        getClusters: function(options, callbacks) {
            var apiUrl = BASE_URL + '/api/clusters';
            if (options.full) {
                apiUrl += '?full=true';
            }

            var ajaxOptions = {
                type: 'GET',
                url: apiUrl
            };
            _.assign(ajaxOptions, callbacks);
            return $.ajax(ajaxOptions);
        },
        getCluster: function(clusterName, callbacks) {
            var apiUrl = BASE_URL + '/api/cluster/' + clusterName;

            var ajaxOptions = {
                type: 'GET',
                url: apiUrl
            };
            _.assign(ajaxOptions, callbacks);
            return $.ajax(ajaxOptions);
        },
        setClusters: function(data, callbacks) {
            var apiUrl = BASE_URL + '/api/cluster/' + data.clusterName;

            var ajaxOptions = {
                type: 'POST',
                url: apiUrl,
                data: data
            };
            _.assign(ajaxOptions, callbacks);
            return $.ajax(ajaxOptions);
        },
        getNodeMetrics: function(clusterName, data, callbacks) {
            var apiUrl = BASE_URL + '/api/cluster/' + clusterName + '/metrics';

            if (!data.start) {
                data.start = moment().subtract(24, 'h').format('x');
            }
            if (!data.end) {
                data.end = moment().format('x');
            }

            var ajaxOptions = {
                type: 'POST',
                url: apiUrl,
                data: data
            };
            _.assign(ajaxOptions, callbacks);
            return $.ajax(ajaxOptions);
        },
        getNodeSlowlogs: function(clusterName, data, callbacks) {
            var apiUrl = BASE_URL + '/api/cluster/' + clusterName + '/slowlogs';

            if (!data.start) {
                data.start = moment().subtract(24, 'h').format('x');
            }
            if (!data.end) {
                data.end = moment().format('x');
            }

            var ajaxOptions = {
                type: 'POST',
                url: apiUrl,
                data: data
            };
            _.assign(ajaxOptions, callbacks);
            return $.ajax(ajaxOptions);
        },
        getClusterNotice: function(clusterName, callbacks) {
            var apiUrl = BASE_URL + '/api/cluster/' + clusterName + '/notice';

            var ajaxOptions = {
                type: 'GET',
                url: apiUrl
            };
            _.assign(ajaxOptions, callbacks);
            return $.ajax(ajaxOptions);
        },
        setClusterNotice: function(clusterName, data, callbacks) {
            var apiUrl = BASE_URL + '/api/cluster/' + clusterName + '/notice';

            var ajaxOptions = {
                type: 'POST',
                url: apiUrl,
                data: data
            };
            _.assign(ajaxOptions, callbacks);
            return $.ajax(ajaxOptions);
        },
        deleteCluster: function(clusterName, callbacks) {
            var apiUrl = BASE_URL + '/api/cluster/' + clusterName + '/delete';

            var ajaxOptions = {
                type: 'POST',
                url: apiUrl
            };
            _.assign(ajaxOptions, callbacks);
            return $.ajax(ajaxOptions);
        },
        changeClusterName: function(clusterName, data, callbacks) {
            var apiUrl = BASE_URL + '/api/cluster/' + clusterName + '/change-cluster-name';

            var ajaxOptions = {
                type: 'POST',
                url: apiUrl,
                data: data
            };
            _.assign(ajaxOptions, callbacks);
            return $.ajax(ajaxOptions);
        }
    },
    Trib: {
        getCreateClusterParams: function(query, callbacks) {
            var apiUrl = BASE_URL + '/api/trib/create/params';

            var ajaxOptions = {
                type: 'GET',
                url: apiUrl,
                data: query
            };
            _.assign(ajaxOptions, callbacks);
            return $.ajax(ajaxOptions);
        },
        createCluster: function(clusterName, data, callbacks) {
            var apiUrl = BASE_URL + '/api/trib/create';
            if (clusterName) {
                apiUrl += '/' + clusterName;
            }

            var ajaxOptions = {
                type: 'POST',
                url: apiUrl,
                data : data
            };
            _.assign(ajaxOptions, callbacks);
            return $.ajax(ajaxOptions);
        },
        addNode: function(data, callbacks) {
            var apiUrl = BASE_URL + '/api/trib/add-node';

            var ajaxOptions = {
                type: 'POST',
                url: apiUrl,
                data : data
            };
            _.assign(ajaxOptions, callbacks);
            return $.ajax(ajaxOptions);
        },
        reshard: function(data, callbacks) {
            var apiUrl = BASE_URL + '/api/trib/reshard';

            var ajaxOptions = {
                type: 'POST',
                url: apiUrl,
                data : data
            };
            _.assign(ajaxOptions, callbacks);
            return $.ajax(ajaxOptions);
        },
        reshardBySlots: function(data, callbacks) {
            var apiUrl = BASE_URL + '/api/trib/reshard-by-slots';

            var ajaxOptions = {
                type: 'POST',
                url: apiUrl,
                data : data
            };
            _.assign(ajaxOptions, callbacks);
            return $.ajax(ajaxOptions);
        },
        replicate: function(data, callbacks) {
            var apiUrl = BASE_URL + '/api/trib/replicate';

            var ajaxOptions = {
                type: 'POST',
                url: apiUrl,
                data : data
            };
            _.assign(ajaxOptions, callbacks);
            return $.ajax(ajaxOptions);
        },
        failover: function(data, callbacks) {
            var apiUrl = BASE_URL + '/api/trib/failover';

            var ajaxOptions = {
                type: 'POST',
                url: apiUrl,
                data : data
            };
            _.assign(ajaxOptions, callbacks);
            return $.ajax(ajaxOptions);
        },
        deleteNode: function(data, callbacks) {
            var apiUrl = BASE_URL + '/api/trib/delete-node';

            var ajaxOptions = {
                type: 'POST',
                url: apiUrl,
                data : data
            };
            _.assign(ajaxOptions, callbacks);
            return $.ajax(ajaxOptions);
        },
        shutdown: function(data, callbacks) {
            var apiUrl = BASE_URL + '/api/trib/shutdown';

            var ajaxOptions = {
                type: 'POST',
                url: apiUrl,
                data : data
            };
            _.assign(ajaxOptions, callbacks);
            return $.ajax(ajaxOptions);
        }
    },
    User: {
        changePassword: function(data, callbacks) {
            var apiUrl = BASE_URL + '/api/me/change-password';

            var ajaxOptions = {
                type: 'POST',
                url: apiUrl,
                data : data
            };
            _.assign(ajaxOptions, callbacks);
            return $.ajax(ajaxOptions);
        },
        updateMe: function(data, callbacks) {
            var apiUrl = BASE_URL + '/api/me/update';

            var ajaxOptions = {
                type: 'POST',
                url: apiUrl,
                data : data
            };
            _.assign(ajaxOptions, callbacks);
            return $.ajax(ajaxOptions);
        },
        getUsers: function(callbacks) {
            var apiUrl = BASE_URL + '/api/users';

            var ajaxOptions = {
                type: 'GET',
                url: apiUrl
            };
            _.assign(ajaxOptions, callbacks);
            return $.ajax(ajaxOptions);
        },
        addUser: function(username, data, callbacks) {
            var apiUrl = BASE_URL + '/api/user/' + username;

            var ajaxOptions = {
                type: 'POST',
                url: apiUrl,
                data : data
            };
            _.assign(ajaxOptions, callbacks);
            return $.ajax(ajaxOptions);
        },
        updateUser: function(username, data, callbacks) {
            var apiUrl = BASE_URL + '/api/user/' + username + '/update';

            var ajaxOptions = {
                type: 'POST',
                url: apiUrl,
                data : data
            };
            _.assign(ajaxOptions, callbacks);
            return $.ajax(ajaxOptions);
        },
        deleteUser: function(username, callbacks) {
            var apiUrl = BASE_URL + '/api/user/' + username + '/delete';

            var ajaxOptions = {
                type: 'POST',
                url: apiUrl
            };
            _.assign(ajaxOptions, callbacks);
            return $.ajax(ajaxOptions);
        }
    }
};

$.ajaxSetup({
    dataType: 'json',
    timeout: 10 * 60 * 1000,
    cache: false,
    error: function(jqXHR, textStatus) {
        if (jqXHR.status === 401) {
            location.href = "/login";
            return;
        }
        commonErrorHandle(jqXHR, textStatus);
    }
});

module.exports = ApiUtils;
