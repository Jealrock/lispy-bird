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
  (-> @opts
    (update :f inc)
    (dissoc :u)))

;; game functions
(defn pipe [{:keys [line column]}]
  (if (not= 0 (- column 2))
    (with-meta '(pipe) { :line line, :column (- column 1)})))

(defn bird
  "move birb down every other frame"
  [{:keys [line column]}]
  (let [jump (- (get @opts :u 0))]
    (with-meta '(bird) { :line (+ line jump (mod (:f @opts) 2)) , :column column})))

(defn over [& args]
  (with-meta '(over) { :line (/ (:h @opts) 2) , :column (/ (:w @opts) 2)}))

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
  (let [header (str "(game " (next-opts) \newline)
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

(defn colliding? [x y]
  (let [mx (meta x)
        my (meta y)
        xstart (:column mx)
        ystart (:column my)
        xend (+ xstart (count (str \' x)))
        yend (+ ystart (count (str \' y)))]
    ;;         xstart   ystart   xend   yend
    ;; +----------|--------|-------|------|---------+
    ;;         ystart   xstart   yend   xend
    (and (= (:line mx) (:line my))
         (= (< xstart yend) (> xend ystart)))))

(defn collide [entities]
  (let [bird (first (filter #(= '(bird) %) entities))
        pred (fn [x] (and (not= x '(bird)) (colliding? bird x)))]
    (if (some pred entities)
      (list (over))
      entities)))

(defn over? [entities]
  (some #(= '(over) %) entities))

(defn game [arg-opts & args]
  (reset! opts arg-opts)
  (if (over? args)
    (render args)
    (->> args move generate-pipe collide render)))

(comment
  (load-file "game.clj")
  ,)
