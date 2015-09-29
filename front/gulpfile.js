var gulp = require('gulp');
var $ = require('gulp-load-plugins')();
var _ = require('lodash');
var del = require('del');
var path = require('path');
var runSequence = require('run-sequence');
var webpack = require('webpack');
var argv = require('minimist')(process.argv.slice(2));

var watch = false;
var browserSync;

var DEBUG = !argv.release;

var src = {
    assets: 'src/{img,html,vendor}/**/*',
    css: 'src/scss/**/*.scss',
    server: 'build/**/*'
};

// The default task
gulp.task('default', ['sync']);

// Clean output directory
gulp.task('clean', del.bind(
    null, ['.tmp', 'build/*', '!build/.git'], {dot: true}
));

// Static files
gulp.task('assets', function() {
    gulp.src(src.assets).pipe($.changed('build')).pipe(gulp.dest('build')).pipe($.size({title: "assets"}));
});

// CSS
gulp.task('css', function () {
    gulp.src(src.css)
        .pipe($.sass())
        .pipe($.if(!DEBUG, $.minifyCss()))
        .pipe(gulp.dest('build/css'));
});

// JS
gulp.task('js', function(cb) {
    var started = false;
    var config = require('./webpack.config.js');
    var bundler = webpack(config);
    var verbose = !!argv.verbose;

    function bundle(err, stats) {
        if (err) {
            throw new $.util.PluginError('webpack', err);
        }

        console.log(stats.toString({
            colors: $.util.colors.supportsColor,
            hash: verbose,
            version: verbose,
            timings: verbose,
            chunks: verbose,
            chunkModules: verbose,
            cached: verbose,
            cachedAssets: verbose
        }));

        if (!started) {
            started = true;
            return cb();
        }
    }

    if (watch) {
        bundler.watch(200, bundle);
    } else {
        bundler.run(bundle);
    }
});

gulp.task('copy', function () {
    gulp.src('build/{css,js,vendor}/**/*').pipe(gulp.dest('../src/main/resources/static'));
});

// Build the app from source code
gulp.task('build', ['clean'], function(cb) {
    runSequence(['assets', 'css', 'js'], cb);
});

// Build and start watching for modifications
gulp.task('build:watch', function(cb) {
    watch = true;
    runSequence('build', function() {
        gulp.watch(src.assets, ['assets']);
        gulp.watch(src.css, ['css']);
        cb();
    });
});

gulp.task('serve', ['build:watch'], function() {
    gulp.src('build')
        .pipe($.webserver({
            livereload: false,
            directoryListing: true
        }));
});

gulp.task('serve-only', function() {
    gulp.src('build')
        .pipe($.webserver({
            livereload: false,
            directoryListing: true
        }));
});

gulp.task('sync', ['build:watch'], function(cb) {
    browserSync = require('browser-sync');

    browserSync({
        server: {
            baseDir: "build",
            logPrefix: "relumin-front"
        }
    });

    process.on('exit', function() {
        browserSync.exit();
    });

    gulp.watch(['build/**/*'], function() {
        browserSync.reload();
    });
});
