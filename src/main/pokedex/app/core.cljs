(ns pokedex.app.core
  (:require [reagent.dom :as rdom]
            [reagent.core :as r])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<!]])
  (:require [goog.string :as gstring]))

(def input-value (r/atom nil))

(def data (r/atom nil))

(defn title-case [name]
  (gstring/capitalize name))

(defn get-pokemon [name]
  (go (<! (http/get (str "https://pokeapi.co/api/v2/pokemon/" name) {:with-credentials? false
                                                                     :query-params {}}))))
(defn read-response [response-chan]
  (go (let [resp (<! response-chan)]
        (reset! data (:body resp)))))  ; <- the only place where you can "touch" resp!

;; ---- View ----
(defn search-input [value]
  [:input
   {:type "text"
    :placeholder "Type a pokemon name eg: pikachu"
    :value @value
    :on-change #(reset! value (-> % .-target .-value))}])

(defn search-form []
  (let [val (r/atom "")]
    (defn on-submit-value [e] (.preventDefault e)
      (reset! input-value @val)
      (read-response (get-pokemon @val)))
    (fn []
      [:form {:on-submit on-submit-value}
       [search-input val]
       [:button "Search"]])))

(defn card []
  [:a
   [:div.details-wrapper
    [:div.card

     [:div
      [:h3 (title-case (:name @data))]
      [:select
       [:option "Default"]
       [:option "3D"]]]

     [:img {:src (get (get (get (get @data :sprites) :other) :home) :front_default) :alt (:name @data)}]

     [:ul.type-list
      (for [type (get @data :types)]
        ^{:key (get type :slot)}  [:li (title-case (get (get type :type) :name))])]]]])

(defn pokedex []
  [:div.container
   [:header
    [:h1 "Pokedex"]]
   [search-form]
   (when-not (nil? (:name @data)) [card])])

(defn render []
  (rdom/render [pokedex] (.getElementById js/document "root")))

(defn ^:export main []
  (render))

(defn ^:dev/after-load reload! []
  (render))
