# lein-collisions

A Leiningen plugin to find conflicting files on the classpath.

Classpath conflicts are bad, because there is no good way to define the classpath order in a dependency tree, so the classpath ordering is essantially random. Having different versions of a library on the classpath can create pretty appalling heisenbugs. This plugin helps find the sources of such deployment issues.

## Usage

Put `[lein-collisions "0.1.2"]` into the `:plugins` vector of the `:user` profile in your `~/.lein/profiles.clj`:

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
      


## License

Copyright © 2015 Herwig Hochleitner

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
