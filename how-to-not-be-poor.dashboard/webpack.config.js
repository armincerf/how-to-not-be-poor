module.exports = function(env, argv) {
    return {
        entry: "./src/js/index.js",
        output: {
            filename:
            (argv.mode || "production") === "production"
                ? "index_bundle.min.js"
                : "index_bundle.js"
        }
    };
};
