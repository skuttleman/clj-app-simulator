(ns com.ben-allred.clj-app-simulator.templates.core)

(defn render [[node & args :as tree]]
  (when tree
    (let [[node & args] (if (fn? node)
                          (loop [node (apply node args)]
                            (if (fn? node)
                              (recur (apply node args))
                              node))
                          tree)]
      (->> args
           (map (fn [arg]
                  (cond
                    (vector? arg) (render arg)
                    (list? arg) (map render arg)
                    :else arg)))
           (into [node])))))
