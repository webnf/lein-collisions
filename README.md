# lein-collisions

A Leiningen plugin to find conflicting files on the classpath.

Classpath conflicts are bad, because there is no good way to define the classpath order in a dependency tree, so the classpath ordering is essantially random. Having different versions of a library on the classpath can create pretty appalling heisenbugs. This plugin helps find the sources of such deployment issues.

## Usage

Put the dependency into the `:plugins` vector of the `:user` profile in your `~/.lein/profiles.clj`: [![Clojars Project](http://clojars.org/lein-collisions/latest-version.svg)](http://clojars.org/lein-collisions)

Example output for a project with collisions in `rhino` and `jsr305`

    $ lein collisions
    org/mozilla/javascript/xmlimpl/XmlProcessor.class -- 3  collisions:
      -- /home/user/.m2/repository/org/mozilla/rhino/1.7R4/rhino-1.7R4.jar
      -- /home/user/.m2/repository/com/yahoo/platform/yui/yuicompressor/2.4.7/yuicompressor-2.4.7.jar
      -- /home/user/.m2/repository/rhino/js/1.6R7/js-1.6R7.jar
    ...
    javax/annotation/Nonnull.class -- 2  collisions:
      -- /home/user/.m2/repository/com/google/code/findbugs/annotations/2.0.1/annotations-2.0.1.jar
      -- /home/user/.m2/repository/com/google/code/findbugs/jsr305/1.3.9/jsr305-1.3.9.jar
    ...
    ...
      

## Comparison / Examples

This is not to be confused with the `:pedantic` flag in leiningen. `:pedantic` catches cases, where transitive dependencies would be pulled in with different versions, e.g. (ring/ring-core "1.1.0" vs "1.2.0")

`lein-collisions` is complementary: It catches cases, where _different_ artefacts provide the same classes or resources. An example is `ibdknox/tools.reader` vs `org.clojure/tools.reader`.
Another increasingly common case is the resource `data_readers.clj` being defined by multiple libraries.

### Examples

Library repackages like the `tools.reader` example are especially problematic, because of the following, typical scenario:

Program fails because of an outdated `tools.reader` version. You think "strange, why is the library pulling an old version", but insert the most recent version anyway. It still fails. You randomly tweak dependencies, suddenly it works (because the classpath order changed), until it doesn't (most often in production). The actual problem has been the whole time that `ibdknox/tools.reader` provides classes resources in `clojure/tools/reader*`, but it's very hard to discovery this.

Another example: `garden` (the css library) depends on `yuicompressor`, which repackages (not depends on) an old version of `rhino`. You eventually will run into bugs relating to old rhino versions, only it will take you a long time to figure it out, because you'll be pretty sure to depend on the most recent version.

`lein-collisions` helps you to find out about this and take measures before problems show up.

## License

Copyright © 2015 Herwig Hochleitner

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
