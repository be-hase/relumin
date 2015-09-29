var React = require('react/addons');
var ValidationMixin = require('react-validation-mixin');
var Joi = require('joi');
var $ = require('jquery');
var AceEditor  = require('react-ace');
require('brace/mode/json');
require('brace/theme/monokai');

var Utils  = require('../utils/Utils');
var TribActions  = require('../actions/TribActions');
var CreateClusterParamsStore = require('../stores/CreateClusterParamsStore');
var ValidationRender = require('../mixins/ValidationRender');

var CreateCluste = React.createClass({
    mixins: [ValidationMixin, ValidationRender, React.addons.LinkedStateMixin],
    validatorTypes: {
        clusterName: Joi.string().allow('').max(20).regex(/^[a-zA-Z0-9_-]+$/).trim().label('Cluster name')
    },
    componentDidMount: function() {
        CreateClusterParamsStore.addChangeListener(this.onChangeHandle);
    },
    componentWillUnmount: function() {
        CreateClusterParamsStore.removeChangeListener(this.onChangeHandle);
    },
    getInitialState: function() {
        return {
            clusterName: '',
            editorValue: ''
        };
    },
    render: function() {
        return (
            <div className="create-cluster-form-components">
                <h3>Create cluster</h3>
                <div className={this.getClasses('clusterName')}>
                    <label>Cluster name (options)</label>
                    <input type="text" valueLink={this.linkState('clusterName')} onBlur={this.handleValidation('clusterName')} className='form-control' placeholder='Cluster name' />
                    <p className="help-block" style={{'color': '#C8C8C8'}}>If cluster name is empty, we just only create cluster, not regist cluster on Rlumin.</p>
                    {this.getValidationMessages('clusterName').map(this.renderHelpText)}
                </div>
                <div className="form-group">
                    <label>Cluster setting</label>
                    <AceEditor mode="json" theme="monokai" width="100%" fontSize={14} name="editor" value={this.state.editorValue} onChange={this.onEditorChange} />
                </div>
                <div className="modal-footer">
                    <button className="btn btn-default" onClick={this.handleClickClear}>Clear</button>
                    <button className="btn btn-primary" onClick={this.handleClickSend} ref="sendBtn">OK</button>
                </div>
            </div>
        );
    },
    onEditorChange: function(value) {
        this.setState({editorValue: value});
    },
    reset: function(event) {
        this.clearValidations();
        this.setState(this.getInitialState());
    },
    handleClickClear: function(event) {
        event.preventDefault();
        this.reset();
    },
    handleClickSend: function(event) {
        var _this = this;
        var $btn = $(event.currentTarget);

        event.preventDefault();
        var onValidate = function(error, validationErrors) {
            if (error) {
                return;
            }

            Utils.loading(true, true);
            $btn.button('loading');
            TribActions.createCluster(_this.state.clusterName, {params: _this.state.editorValue}, {
                success: function() {
                    if (_this.isMounted()) {
                        _this.props.onReset();
                    }
                    Utils.showAlert({
                        message: 'Created successfully',
                        level: 'success'
                    });
                },
                complete: function() {
                    Utils.loading(false, true);
                    if (_this.isMounted()) {
                        $btn.button('reset');
                    }
                }
            });
        };

        this.validate(onValidate);
    },
    onChangeHandle: function() {
        var data = CreateClusterParamsStore.getCreateClusterParams();
        if (data) {
            this.setState({editorValue: data});
        }
    }
});

module.exports = CreateCluste;
