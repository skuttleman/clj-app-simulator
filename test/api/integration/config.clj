(ns integration.config)

(def port 4000)

(def host-port (str "localhost:" port))

(defn ->url
    ([path] (->url :http path))
    ([protocol path]
        (str (name protocol) "://" host-port path)))
