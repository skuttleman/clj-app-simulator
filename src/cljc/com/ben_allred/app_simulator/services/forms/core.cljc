(ns com.ben-allred.app-simulator.services.forms.core)

(defprotocol ISync
  (ready! [this status result])
  (sync! [this])
  (syncing? [this]))

(defprotocol IChange
  (changed? [this] [this path])
  (touched? [this] [this path]))

(defprotocol IValidate
  (errors [this])
  (valid? [this]))

(defprotocol ITry
  (try! [this])
  (tried? [this]))
