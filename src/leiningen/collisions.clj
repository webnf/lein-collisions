(ns leiningen.collisions
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [leiningen.core.classpath :refer [get-classpath]]))

(set! *warn-on-reflection* true)

;; Inlined from webnf.base.platform
;; slim down

(defmacro forcat
  "Concat the return value of a for expression"
  [bindings body]
  `(apply concat (for ~bindings ~body)))

(defn ze-name ^String [^java.util.zip.ZipEntry ze]
  (.getName ze))

(defn split-path [path]
  (loop [acc () ^java.io.File path (io/file path)]
    (if path
      (recur (cons (.getName path) acc)
             (.getParentFile path))
      (vec acc))))

(defn render-path [p]
  (str/join "/" p))

(defn dir-entries
  ([dir] (dir-entries dir (constantly true)))
  ([dir want-file?]
   (forcat [e (.listFiles (io/file dir))]
           (cond (.isDirectory ^java.io.File e) (dir-entries e want-file?)
                 (want-file? e)   [e]))))

(defn zip-entries
  ([zip] (zip-entries zip (constantly true)))
  ([zip want-entry?]
   (with-open [zf (java.util.zip.ZipFile. (io/file zip))]
     (into [] (remove #_(do (println (ze-name %) "=>" (.endsWith (ze-name %) "/")))
                      #(.endsWith (ze-name %) "/")
                      (enumeration-seq
                       (.entries zf)))))))

(defn relativize [base path]
  (let [base (when base (.getCanonicalFile (io/file base)))
        path (io/file path)]
    (loop [acc ()
           ^java.io.File path' path]
      (when-not path'
        (throw (ex-info (str "Resource dir not contained in project root"
                             {:base base :path path})
                        {:base base :path path})))
      (if (= base path')
        (vec acc)
        (recur (cons (.getName path') acc)
               (.getParentFile path'))))))

(defn system-classpath-roots []
  (str/split (System/getProperty "java.class.path") #":"))

(defn classpath-resources
  ([] (classpath-resources (system-classpath-roots)))
  ([roots] (if (string? roots)
             (recur (str/split roots #":"))
             (forcat [r roots
                      :let [f (.getCanonicalFile (io/file r))
                            ze-meta {:classpath-entry f}]]
                     (cond
                       (.isDirectory f) (for [de (dir-entries f)]
                                          (with-meta (relativize f de)
                                            ze-meta))
                       (.isFile f)      (for [ze (zip-entries f)]
                                          (with-meta (split-path (str ze))
                                            ze-meta)))))))

(defn find-ambigous-resources
  ([] (find-ambigous-resources (classpath-resources)))
  ([pathes]
   (loop [[p0 & [p1 :as pn]] (sort pathes)
          duplicate-files {}]
     (let [cpe0 (:classpath-entry (meta p0))
           cpe1 (:classpath-entry (meta p1))]
       (cond
         (empty? pn) duplicate-files
         (and (= p0 p1)
              (not= cpe0 cpe1))
         (recur pn (update-in duplicate-files [p0] (fnil into #{}) [cpe0 cpe1]))
         :else (recur pn duplicate-files))))))

;; plugin

(defn collisions
  "Check for file collisions on the project's class path
     :exclusions a set of paths to exclude
         default '#{[\"META-INF\"] [\"project.clj\"] [\"README\"] [\"README.md\"] [\"AUTHORS\"] [\"LICENSE\"] [\"deps.cljs\"]}'"
  [project & {:as opts}]
  ;; (println "Checking classpath" (mapv pr-str (get-classpath project)))
  (let [exclusions* (reduce #(assoc-in %1 %2 ::exclude)
                            {} (read-string (get opts ":exclusions"
                                                 "#{[\"META-INF\"] [\"project.clj\"] [\"README\"] [\"README.md\"] [\"AUTHORS\"] [\"LICENSE\"] [\"deps.cljs\"]}")))
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
