(ns com.ben-allred.app-simulator.services.forms.core)

(defprotocol ISync
  (ready! [this] [this status result])
  (sync! [this])
  (syncing? [this]))

(defprotocol IChange
  (touch! [this path])
  (changed? [this] [this path])
  (touched? [this] [this path]))

(defprotocol IValidate
  (errors [this])
  (valid? [this]))

(defprotocol ITry
  (try! [this])
  (tried? [this]))
