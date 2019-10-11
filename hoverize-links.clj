(require '[clojure.java.shell :as shell])
(require '[clojure.data.xml :as xml])
(require '[clojure.zip :as zip])
(require '[clojure.java.io :as io])

(defn css-style []
  (xml/parse-str
   (str "<style xmlns=\"http://www.w3.org/1999/xhtml\" type=\"text/css\">\n"
        "<![CDATA[\n"
        "@import url('https://fonts.googleapis.com/css?family=Arvo&display=swap');\n"
        "a > ellipse:hover {\n"
        "  stroke-width: 2;\n"
        "  fill:#99ccff;}]]>\n"
        "</style>")))

(defn adjust-attr [loc attr-k value]
  (zip/edit loc #(assoc-in % [:attrs attr-k] value)))

(defn hoverize[svg-zip]
  (loop [loc (zip/insert-child svg-zip (css-style))]
    (if (zip/end? loc)
      (zip/root loc)
      (let [tag (some-> loc zip/node :tag name)
            new-loc (cond (= tag "ellipse")
                          (adjust-attr loc :pointer-events "fill")

                          (= tag "foreignObject")
                          (adjust-attr loc :pointer-events "none")

                          (= tag "a")
                          (adjust-attr loc :target "_blank")

                          :else loc)]
        (recur (zip/next new-loc))))))

;; /Applications/draw.io.app/Contents/MacOS/draw.io --export --format svg --border 10 --output test123.svg lee-clojure-journey.drawio

(defn temp-file []
  (let [f (java.io.File/createTempFile "lee-clojure-journey" "svg")]
    (.deleteOnExit f)
    (str f)))

(defn drawio->svg [in-filename out-filename]
  (shell/sh "/Applications/draw.io.app/Contents/MacOS/draw.io"
            "--export"
            "--format" "svg"
            "--border" "10"
            "--output" out-filename
            in-filename))

(defn svg->hoverized-svg [in-filename out-filename]
  (spit out-filename (-> in-filename
                         io/input-stream
                         xml/parse
                         zip/xml-zip
                         hoverize
                         xml/indent-str)))

(defn convert [in-filename out-filename]
  (println "--[draw.io conversion]--")
  (println "input file:" in-filename)
  (let [svg-filename (temp-file)
        {:keys [exit out err]} (drawio->svg in-filename svg-filename)]
    (if (not= 0 exit)
      (do (println "exit:" exit)
          (when out (println "out:" out))
          (when err (println "err:" err))
          (System/exit exit))
      (do
        (println "--[link hoverization]--")
        (svg->hoverized-svg svg-filename out-filename)
        (println "output file:" out-filename)
        (println ".all done.")))))

(convert "lee-clojure-journey.drawio"
         "lee-clojure-journey.svg")

(shutdown-agents)
