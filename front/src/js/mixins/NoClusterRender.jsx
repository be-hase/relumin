var NoClusterRender = {
    renderNoCluster: function(clusterName, key) {
        return (
            <div key={key} className="alert alert-warning">
                This Cluster name (<strong>{clusterName}</strong>) does not exist.<br />
                Please confirm your URL.
            </div>
        );
    }
};

module.exports = NoClusterRender;
