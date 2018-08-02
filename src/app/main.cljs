
(ns app.main
  (:require [respo.core :refer [render! clear-cache! realize-ssr!]]
            [app.comp.container :refer [comp-container]]
            [app.updater :refer [updater]]
            [app.schema :as schema]
            [reel.util :refer [listen-devtools!]]
            [reel.core :refer [reel-updater refresh-reel]]
            [reel.schema :as reel-schema]
            [cljs.reader :refer [read-string]]
            [app.config :as config]
            ["highlight.js/lib/index" :as hljs]
            ["highlight.js/lib/languages/clojure" :as clojure-lang]
            ["highlight.js/lib/languages/bash" :as bash-lang]
            ["highlight.js/lib/languages/javascript" :as javascript-lang]
            ["highlight.js/lib/languages/typescript" :as typescript-lang]))

(defonce *reel
  (atom (-> reel-schema/reel (assoc :base schema/store) (assoc :store schema/store))))

(defn dispatch! [op op-data]
  (comment println "Dispatch:" op)
  (reset! *reel (reel-updater updater @*reel op op-data)))

(def mount-target (.querySelector js/document ".app"))

(defn render-app! [renderer]
  (renderer mount-target (comp-container @*reel) #(dispatch! %1 %2)))

(def ssr? (some? (js/document.querySelector "meta.respo-ssr")))

(defn main! []
  (if ssr? (render-app! realize-ssr!))
  (.registerLanguage hljs "clojure" clojure-lang)
  (.registerLanguage hljs "bash" bash-lang)
  (.registerLanguage hljs "javascript" javascript-lang)
  (.registerLanguage hljs "typescript" typescript-lang)
  (render-app! render!)
  (add-watch *reel :changes (fn [] (render-app! render!)))
  (listen-devtools! "a" dispatch!)
  (.addEventListener
   js/window
   "beforeunload"
   (fn [] (.setItem js/localStorage (:storage config/site) (pr-str (:store @*reel)))))
  (let [raw (.getItem js/localStorage (:storage config/site))]
    (if (some? raw) (do (dispatch! :hydrate-storage (read-string raw)))))
  (println "App started."))

(defn reload! []
  (clear-cache!)
  (reset! *reel (refresh-reel @*reel schema/store updater))
  (println "Code updated."))

(set! (.-onload js/window) main!)
