(def min-pipe-diff 23)
(def opts (atom {}))

;; helpers
(defn spaces [n]
  (apply str (repeat n \space)))

(defn row [entities n]
  (filter #(= n (:line (meta %))) entities))

(defn rows [entities]
  (let [ns (range 2 (inc (:h @opts)))]
    (map #(row entities %) ns)))

(defn eval-entity [x] (eval (reverse (conj x (meta x)))))

(defn next-opts []
  (update @opts :f inc))

;; game functions
(defn pipe [{:keys [line column]}]
  (if (not= 0 (- column 2))
    (with-meta '(pipe) { :line line, :column (- column 1)})))

;; move birb down every other frame
(defn bird [{:keys [line column]}]
  (with-meta '(bird) { :line (+ line (mod (:f @opts) 2)) , :column column}))

(defn new-pipe [line column]
  (with-meta '(pipe) { :line line, :column (- column 5)}))

(defn render-entity [acc entity]
  (let [column (:column (meta entity))
        filled (count acc)
        space (spaces (- column filled 2))]
    (str acc space \' entity)))

(defn render-row [row]
  (let [sorted (sort-by (comp :column meta) row)]
    (->> sorted
        (reduce render-entity "")
        (#(str % (spaces (- (:w @opts) (count %))))))))

(defn render [entities]
  (loop [header (str "(game " (next-opts) \newline)
         rows (map render-row (rows entities))]
    (str header (clojure.string/join ",\n" rows) ")")))

(defn move [entities]
  (->> entities
    (map eval-entity)
    (remove nil?)))

;; TODO: diff hole size
(defn pipe-generator []
  (let [hole-at (+ 2 (rand-int (- (:h @opts) 3)))
        hole-til (+ hole-at 4)
        hole-at? (fn [n] (and (< n hole-til) (>= n hole-at)))]
    (fn [idx row]
      (if (hole-at? (+ idx 2)) row
          (concat row (list (new-pipe (+ idx 2) (:w @opts))))))))

;; TODO: diff pipe-diff
(defn generate-pipe [entities]
  (let [pipes (filter #(= '(pipe) %) entities)
        last-pipe (apply max-key #(:column (meta %)) pipes)
        diff (- (:w @opts) (:column (meta last-pipe)))]
    (if (< diff min-pipe-diff)
      entities
      (apply concat (map-indexed (pipe-generator) (rows entities))))))

(defn game [arg-opts & args]
  (reset! opts arg-opts)
  (->> args move generate-pipe render))
