(ns leiningen.collisions
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [leiningen.core.classpath :refer
             [get-classpath]]
            [webnf.base.platform :refer
             [find-ambigous-resources classpath-resources]]))

(defn render-path [p]
  (str/join "/" p))

(defn collisions
  "Check for file collisions on the project's class path
     :exclusions a set of paths to exclude
         default '#{[\"META-INF\"] [\"project.clj\"] [\"README\"] [\"README.md\"] [\"AUTHORS\"] [\"LICENSE\"]}'"
  [project & {:as opts}]
  ;; (println "Checking classpath" (mapv pr-str (get-classpath project)))
  (let [exclusions* (reduce #(assoc-in %1 %2 ::exclude)
                            {} (read-string (get opts ":exclusions"
                                                 "#{[\"META-INF\"] [\"project.clj\"] [\"README\"] [\"README.md\"] [\"AUTHORS\"] [\"LICENSE\"]}")))
        in-exclusions? (fn ie?
                         ([path] (ie? exclusions* path))
                         ([exc [p & path]]
                          (when-let [exc' (get exc p)]
                            (or (= ::exclude exc')
                                (when path (recur exc' path))))))]
    (if-let [files (seq
                    (find-ambigous-resources
                     (remove in-exclusions?
                             (classpath-resources (get-classpath project)))))]
      (do
        (println "File collisions:")
        (doseq [[fname fs] files
                :let[_ (println (render-path fname) "--" (count fs) " collisions:")]
                path fs]
          (println "  --" (str path))))
      (println "No file collisions :-)"))))
