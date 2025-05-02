(load-file "main.clj")

(def mode (last *command-line-args*))

(defn once []
  (print "\033c")
  (println (slurp "game.clj"))
  (println (load-file "game.clj")))

(defn play []
  (loop [frame (load-file "game.clj")]
    (print "\033c")
    (println frame)
    (Thread/sleep 500)
    (recur (load-string frame))))

(if (= mode "once") (once) (play))
