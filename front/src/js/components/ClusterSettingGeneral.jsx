var React = require('react');
var Router = require('react-router');
var classSet = React.addons.classSet;
var $ = require('jquery');
var _ = require('lodash');
var DateRangePicker = require('react-bootstrap-daterangepicker');
var moment = require('moment');
var ValidationMixin = require('react-validation-mixin');
var Joi = require('joi');

var ValidationRender = require('../mixins/ValidationRender');
var ClusterActions = require('../actions/ClusterActions');
var Utils = require('../utils/Utils');

var UserStore = require('../stores/UserStore');

var ClusterSettingGeneral = React.createClass({
    mixins: [ValidationMixin, ValidationRender, React.addons.LinkedStateMixin],
    validatorTypes: {
        mailTo:  Joi.string().allow('').max(1000).trim().label('To'),
        mailFrom:  Joi.string().allow('').max(1000).trim().label('From'),
        httpUrl: Joi.string().allow('').uri({scheme:['http', 'https']}).max(1000).trim().label('URL')
    },
    getInitialState: function () {
        return this.getUpdatedState(this.props);
    },
    componentWillReceiveProps: function(nextProps) {
        this.setState(this.getUpdatedState(nextProps));
    },
    getUpdatedState: function(props) {
        var notice = props.notice;
        var endDate = false;

        if (notice.invalid_end_time) {
            var endMoment = moment(notice.invalid_end_time, 'x');
            if (endMoment.isValid() && endMoment.isAfter(moment())) {
                endDate = endMoment;
            }
        }

        return {
            mailTo: notice.mail.to ? notice.mail.to : '',
            mailFrom: notice.mail.from ? notice.mail.from : '',
            httpUrl: notice.http.url ? notice.http.url : '',
            endDate: endDate
        };
    },
    render: function() {
        var me = UserStore.getMe();
        var hasPermission = !AUTH_ENABLED || me.role === 'RELUMIN_ADMIN';

        var label;
        if (this.state.endDate) {
            label = this.state.endDate.format('YYYY-MM-DD HH:mm');
        } else {
            label = 'Not Set.';
        }

        var mailToInputView;
        var mailFromInputView;
        var httpUrlInputView;
        var stopNotifyView;
        if (hasPermission) {
            mailToInputView = (
                <input type='text' valueLink={this.linkState('mailTo')} onBlur={this.handleValidation('mailTo')} className='form-control' placeholder='to@example.com' />
            );
            mailFromInputView = (
                <input type='text' valueLink={this.linkState('mailFrom')} onBlur={this.handleValidation('mailFrom')} className='form-control' placeholder='from@example.com' />
            );
            httpUrlInputView = (
                <input type='text' valueLink={this.linkState('httpUrl')} onBlur={this.handleValidation('httpUrl')} className='form-control' placeholder='http://example.com/notify' />
            );
            stopNotifyView = (
                <div style={{width: '0px'}}>
                    <DateRangePicker
                        format="YYYY-MM-DD HH:mm"
                        singleDatePicker={true}
                        startDate={this.state.endDate}
                        endDate={this.state.endDate}
                        minDate={moment()}
                        timePicker={true} timePickerIncrement={10} timePicker12Hour={false}
                        showDropdowns={true}
                        applyClass="btn-primary" cancelClass="btn-default"
                        onApply={this.handleApplyDateRangePicker}
                        >
                        <button className="btn btn-default">
                            <i className="glyphicon glyphicon-calendar"></i> <small>{label}</small>
                        </button>
                    </DateRangePicker>
                </div>
            );
        } else {
            mailToInputView = (
                <pre>{this.state.mailTo || 'Not set.'}</pre>
            );
            mailFromInputView = (
                <pre>{this.state.mailFrom || 'Not set.'}</pre>
            );
            httpUrlInputView = (
                <pre>{this.state.httpUrl || 'Not set.'}</pre>
            );
            stopNotifyView = (
                <pre>{label}</pre>
            );
        }

        var saveBtnView;
        if (hasPermission) {
            saveBtnView = (
                <div className="modal-footer">
                    <button className="btn btn-primary" onClick={this.handleClickSaveBtn}>Save</button>
                </div>
            );
        }

        return (
            <div className="cluster-setting-general-components">
                <div className="panel panel-default">
                    <div className="panel-heading">Setting of notification destination</div>
                    <div className="panel-body">
                        <div className="row">
                            <div className="col-lg-6">
                                <h4>Mail notification</h4>
                                <div className={this.getClasses('mailTo')}>
                                    <label>To (Comma reparated)</label>
                                    {mailToInputView}
                                    {this.getValidationMessages('mailTo').map(this.renderHelpText)}
                                </div>
                                <div className={this.getClasses('mailFrom')}>
                                    <label>From (Optional. Override value registered at config file.)</label>
                                    {mailFromInputView}
                                    {this.getValidationMessages('mailFrom').map(this.renderHelpText)}
                                </div>
                            </div>
                            <div className="col-lg-6">
                                <h4>Http notification</h4>
                                <div className={this.getClasses('httpUrl')}>
                                    <label>URL</label>
                                    {httpUrlInputView}
                                    {this.getValidationMessages('httpUrl').map(this.renderHelpText)}
                                </div>
                            </div>
                        </div>
                        <div className="row">
                            <div className="col-lg-6">
                                <h4>Common</h4>
                                <div className="form-group">
                                    <label>Don't notify until this time. (Stop temporarily)</label>
                                    {stopNotifyView}
                                </div>
                            </div>
                        </div>
                        {saveBtnView}
                    </div>
                </div>
            </div>
        );
    },
    handleApplyDateRangePicker: function(event, picker) {
        this.setState({
            endDate: picker.endDate
        });
    },
    handleClickSaveBtn: function(event) {
        var _this = this;
        var $btn = $(event.currentTarget);

        event.preventDefault();
        var onValidate = function(error, validationErrors) {
            if (error) {
                return;
            }

            $btn.button('loading');
            var data = _.assign({}, _this.props.notice, {
                mail: {
                    to: _.trim(_this.state.mailTo),
                    from: _.trim(_this.state.mailFrom)
                },
                http : {
                    url: _.trim(_this.state.httpUrl)
                },
                invalid_end_time: _this.state.endDate ? _this.state.endDate.format('x') : ''
            });
            ClusterActions.setClusterNotice(
                _this.props.cluster.cluster_name,
                {
                    notice: JSON.stringify(data)
                },
                {
                    success: function() {
                        Utils.showAlert({
                            message: 'Saved successfully',
                            level: 'success'
                        });
                    },
                    complete: function() {
                        if (_this.isMounted()) {
                            $btn.button('reset');
                        }
                    }
                }
            );
        };

        this.validate(onValidate);
    }
});

module.exports = ClusterSettingGeneral;
