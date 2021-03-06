
(ns app.comp.container
  (:require [hsl.core :refer [hsl]]
            [respo-ui.core :as ui]
            [respo.macros
             :refer
             [defcomp cursor-> list-> <> div button textarea input span pre]]
            [verbosely.core :refer [verbosely!]]
            [respo.comp.space :refer [=<]]
            [reel.comp.reel :refer [comp-reel]]
            [respo-md.comp.md :refer [comp-md]]
            [app.config :refer [dev?]]
            [app.snippets :refer [files]]
            ["escape-html" :as escape-html]
            ["highlight.js/lib/index" :as hljs]
            ["copy-to-clipboard" :as copy]
            ["shortid" :as shortid]
            [clojure.string :as string]
            [respo-message.action :as message-action]
            [respo-message.comp.messages :refer [comp-messages]]
            [app.controller :refer [copy-text!]]))

(def style-code-preview
  {:font-family ui/font-code,
   :font-size 12,
   :line-height "18px",
   :margin 0,
   :padding 8,
   :padding-bottom 40,
   :width "100%",
   :height "100%",
   :overflow :auto,
   :padding-top 32,
   :color (hsl 0 0 60),
   :cursor :pointer})

(def supported-langs
  {"clojure" "clojure",
   "bash" "bash",
   "clj" "clojure",
   "javascript" "javascript",
   "js" "javascript",
   "typescript" "typescript",
   "ts" "typescript"})

(defcomp
 comp-snippet
 (snippet)
 (div
  {:style {:border (str "1px solid " (hsl 0 0 92)),
           :display :inline-block,
           :margin 8,
           :position :relative,
           :height 280,
           :width 400}}
  (div
   {:style {:padding "4px 8px",
            :border-bottom (str "1px solid " (hsl 0 0 90)),
            :position :absolute,
            :background-color (hsl 0 0 90 0.8),
            :width "100%"}}
   (<> (:name snippet) {:font-weight 300, :font-size 16}))
  (pre
   {:innerHTML (let [code (:content snippet), lang (:lang snippet)]
      (if (contains? supported-langs lang)
        (.-value (.highlight hljs (get supported-langs lang) code))
        (escape-html code))),
    :style style-code-preview,
    :on-click (fn [e d! m!] (copy-text! (:content snippet) d!))})
  (span
   {:inner-text "Copy",
    :style {:position :absolute,
            :bottom 8,
            :right 8,
            :border "1px solid #ddd",
            :padding "0 8px",
            :cursor :pointer,
            :border-radius "8px"},
    :class-name "clickable",
    :on-click (fn [e d! m!] (copy-text! (:content snippet) d!))})))

(defcomp
 comp-container
 (reel)
 (let [store (:store reel), states (:states store)]
   (div
    {:style (merge ui/global ui/fullscreen ui/column)}
    (div
     {:style (merge ui/center {:height 48, :border-bottom (str "1px solid " (hsl 0 0 92))})}
     (input
      {:style (merge ui/input {:border :none, :width 240}),
       :placeholder "Search...",
       :value (:content store),
       :on-input (fn [e d! m!] (d! :content (:value e)))}))
    (list->
     {:style (merge
              ui/flex
              ui/row
              {:padding 16,
               :overflow :auto,
               :padding-bottom 120,
               :justify-content :center,
               :flex-wrap :wrap})}
     (->> files
          (filter
           (fn [snippet]
             (string/includes? (string/lower-case (:name snippet)) (:content store))))
          (map (fn [snippet] [(:key snippet) (comp-snippet snippet)]))))
    (comp-messages
     (:messages store)
     {}
     (fn [info d! m!] (d! message-action/remove-one info)))
    (when dev? (cursor-> :reel comp-reel states reel {})))))
