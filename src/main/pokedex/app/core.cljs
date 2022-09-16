(ns pokedex.app.core
  (:require [reagent.dom :as rdom]
            [reagent.core :as r])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<!]])
  (:require [goog.string :as gstring]))

;; ---- States ----
(def input-value (r/atom nil))

(def data (r/atom nil))

(def view (r/atom nil))

(def all-image (r/atom nil))

(def image (r/atom nil))

;; ---- Utils ----
(defn title-case [name]
  (gstring/capitalize name))


(defn get-pokemon [name]
  (go (<! (http/get (str "https://pokeapi.co/api/v2/pokemon/" name) {:with-credentials? false
                                                                     :query-params {}}))))
(defn read-response [response-chan]
  (go (let [resp (<! response-chan)]
        (reset! data (:body resp)))))

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
      (reset! all-image [{:name "default"
                          :image (get (get @data :sprites) :front_default)}
                         {:name "3d"
                          :image (get (get (get (get @data :sprites) :other) :home) :front_default)}])
      (read-response (get-pokemon @val)))
    (fn []
      [:form {:on-submit on-submit-value}
       [search-input val]
       [:button "Search"]])))

(defn change-image [value]
  (for [item @all-image]
    (when (= (get item :name) value) (reset! image item))))


(defn view-selector []
  (fn []
    [:select {:on-change #(reset! view (-> % .-target .-value))}
     [:option {:value "default"} "Default"]
     [:option {:value "3d"} "3D"]
     [:option {:value "original"} "Original"]]))

(defn card []
  [:a
   [:div.details-wrapper
    [:div.card
     [:div
      [:h3 (title-case (:name @data))]
      [view-selector]]
     [:img {:src (cond
                   (= @view "original") (get (get @data :sprites) :front_default)
                   :else (get (get (get (get @data :sprites) :other) :home) :front_default)) :alt (get @image :name)}]

     [:ul.type-list
      (for [type (get @data :types)]
        ^{:key (get type :slot)}  [:li {:class (get (get type :type) :name)} (get (get type :type) :name)
                                   [:div.tooltip  (title-case (get (get type :type) :name))]])]]]])

;; ---- Main ----
(defn pokedex []
  [:div.container
   [:header
    [:h1 "Pokedex"]]
   [search-form]
   (cond
     (nil? (:name @data)) [:h4 "Feel free to input your favourite pokemon. eg: groudon, pikachu, arcanine..."] :else [card])])

(defn render []
  (rdom/render [pokedex] (.getElementById js/document "root")))

(defn ^:export main []
  (render))

(defn ^:dev/after-load reload! []
  (render))
