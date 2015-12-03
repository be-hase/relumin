var _ = require('lodash');
var path = require('path');
var webpack = require('webpack');
var argv = require('minimist')(process.argv.slice(2));
var DEBUG = !argv.release;

var GLOBALS = {
    'process.env.NODE_ENV': DEBUG ? '"development"' : '"production"',
    '__DEV__': DEBUG
};

var config = {
    output: {
        path: './build/js'
    },

    cache: DEBUG,
    debug: DEBUG,
    devtool: DEBUG ? '#inline-source-map' : false,

    stats: {
        colors: true,
        reasons: DEBUG
    },

    plugins: [
        new webpack.optimize.OccurenceOrderPlugin()
    ],

    resolve: {
        extensions: ['', '.webpack.js', '.js', '.jsx', '.json']
    },

    node: {
      net: 'empty',
      dns: 'empty'
    },

    module: {
        loaders: [
            {
                test: require.resolve("jquery"),
                loader: "expose?jQuery"
            },
            {
                test: /\.jsx?$/,
                loader: 'jsx'
            },
            {
                test: /\.css$/,
                loader: "style!css"
            },
            {
                test: /\.scss$/,
                loader: "style!css"
            }
        ]
    },

    externals: [
        {
            "window": "window"
        }
    ],
};

var clientConfig = _.merge({}, config, {
    entry: './src/js/app.js',
    output: {
        filename: 'app.js'
    },
    plugins: config.plugins.concat(
        [
            new webpack.DefinePlugin(_.merge(GLOBALS, {'__SERVER__': false}))
        ].concat(
            DEBUG ? [] : [
            new webpack.optimize.DedupePlugin(),
            new webpack.optimize.UglifyJsPlugin(),
            new webpack.optimize.AggressiveMergingPlugin()
        ])
    )
});

module.exports = clientConfig;
