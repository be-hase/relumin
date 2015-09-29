var React = require('react');

var ValidationRender = {
    renderHelpText: function(message) {
        return (
          <p className="help-block">{message}</p>
        );
    },
    getClasses: function(field) {
        return React.addons.classSet({
          'form-group': true,
          'has-error': !this.isValid(field)
        });
    }
};

module.exports = ValidationRender;
