(ns chessground.ctrl
  "User actions"
  (:require [chessground.common :as common :refer [pp]]
            [chessground.data :as data]
            [chessground.chess :as chess]
            [cljs.core.async :as a])
  (:require-macros [cljs.core.async.macros :as am]))

(defn- move-start [app orig]
  "A move has been started by clicking on a piece"
  (update-in app [:chess] chess/set-selected orig (-> app :movable :dests)))

(defn- move-piece [app [orig dest]]
  "A move initiated through the UI"
  (or (when (data/can-move? app orig dest)
        (data/move-piece app orig dest))
      (when (data/can-premove? app orig dest)
        (data/set-current-premove app [orig dest]))
      (if (= orig dest)
        app
        (if (or (data/movable? app dest) (data/premovable? app dest))
          (move-start app dest)
          (update-in app [:chess] chess/set-unselected)))))

(defn- select-square [app key]
  (or (if-let [orig (chess/get-selected (:chess app))]
        (when (not (= orig key))
          (move-piece app [orig key]))
        (when (or (data/movable? app key) (data/premovable? app key))
          (move-start app key)))
      (data/cancel-premove app)))

; (defn- drag-start [app orig]
;   "A move has been started, by dragging a piece"
;   (update-in app [:chess] chess/set-selected orig (-> app :movable :dests)))

(defn- drop-off [app]
  (update-in
    (or (when (= "trash" (-> app :movable :drop-off))
          (when-let [key (chess/get-selected (:chess app))]
            (update-in app [:chess] chess/set-pieces {key nil})))
        app)
    [:chess] chess/set-unselected))

(defn- drop-on [app dest]
  (if-let [orig (chess/get-selected (:chess app))]
    (move-piece app [orig dest])
    (drop-off app)))

(defn handler [app-atom]
  (fn [function data]
    (case function
      :select-square (swap! app-atom #(select-square % data))
      ; :drag-start (swap! app-atom #(drag-start % data))
      :drop-off (swap! app-atom drop-off)
      :drop-on (swap! app-atom #(drop-on % data)))))
