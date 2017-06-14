module.exports = {
    entry: {
        search: './search.js',
        spacetime: './spacetime.js',
    },
    output: {
        filename: 'spime.[name].js'
    },
    node: {
        fs: 'empty',
        net: 'empty',
        tls: 'empty',
        'crypto': 'empty'
    }
};
