// webpack.config.js
const webpack = require('webpack');
const path = require('path');



const config = {
    context: path.resolve(__dirname, '.'),
    entry: [
        './spacegraph.js'
    ],
    output: {
        path: path.resolve(__dirname, 'dist'),
        filename: 'bundle.js'
    },
    plugins: [
        new webpack.ProvidePlugin({
            "$": "jquery",
            "jQuery": "jquery",
            "window.jQuery": "jquery",
            "window.$": "jquery"
        })
    ]


//    debug   : true,
    //   devtool : 'source-map',
    // optimize : {
    //   minimize : true,
    // },
    // module: {
    //     rules: [{
    //         test: /\.js$/,
    //         include: path.resolve(__dirname, 'src')
    //         ,use: [{
    //       loader: 'babel-loader',
    //       options: {
    //         presets: [
    //           ['es2015', { modules: false }]
    //         ]
    //       }
    //     }]
    //     }]
    // }
};

module.exports = config;
