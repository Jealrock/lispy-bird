(import '[org.jline.terminal TerminalBuilder])
(use '[clojure.string :only (replace-first)])
(load-file "main.clj")

(def terminal-reader
  (let [terminal (.build (TerminalBuilder/builder))]
    (.enterRawMode terminal)
    (.reader terminal)))

(def frame (atom { :s (slurp "game.clj") :t (System/currentTimeMillis)}))
(defn render-frame
  ([] (render-frame {}))
  ([opts]
   (let [old-opts (eval (read-string (re-find #"\{.*\}" (:s @frame))))
         code (replace-first (:s @frame) #"\{.*\}" (str (merge old-opts opts)))]
    (reset! frame { :s (load-string code) :t (System/currentTimeMillis)})
    (print "\033c")
    (println (:s @frame)))))

(defn process-input []
  (load-file "main.clj")
  (while true
    (let [key-char (char (.read terminal-reader))]
      (if (= key-char \space) (render-frame { :u 2})))))

(defn once []
  (print "\033c")
  (println (slurp "game.clj"))
  (println (load-file "game.clj")))

(defn play []
  (render-frame)
  (.start (Thread. process-input))
  (while true
    (if (>= (- (System/currentTimeMillis) (:t @frame)) 500)
      (render-frame))))

(defn -main [& args]
  (if (= (first args) "once") (once) (play)))

(comment
  (render-frame)
  ,)

(apply -main *command-line-args*)
