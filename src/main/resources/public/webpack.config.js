const { WebPlugin, AutoWebPlugin } = require('web-webpack-plugin');

module.exports = {
    entry: {
        search: './search.js',
        spacetime: './spacetime.js',
    },

    output: {
        filename: 'spime.[name].js',
        pathinfo: true
    },

    node: {
        fs: 'empty',
        net: 'empty',
        tls: 'empty',
        'crypto': 'empty'
    }
};
