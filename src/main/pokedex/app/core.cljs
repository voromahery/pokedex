(ns pokedex.app.core
  (:require [reagent.dom :as rdom]
            [reagent.core :as r]
            [react :as react])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<!]])
  (:require [goog.string :as gstring])
  (:require [clojure.string :as str]))

;; ---- States ----
(def input-value (r/atom nil))

(def loading (r/atom false))

(def data (r/atom nil))

(defonce all-pokemon (r/atom nil))

(def view (r/atom nil))

(def all-image (r/atom nil))

(def image (r/atom nil))

;; ---- Utils ----
(defn title-case [name]
  (gstring/capitalize name))


(defn get-all-pokemon []
  (go
    (let [response (<! (http/get "https://pokeapi.co/api/v2/pokemon?limit=100000" {:with-credentials? false}))]
      (reset! all-pokemon (get (:body response) :results)))))

(get-all-pokemon)

(defn name-checker [name]
  (cond (nil? name) "pikachu"
        :else name))

(defn search-form []
  (defn get-pokemon [name]
    (go (<! (http/get (str "https://pokeapi.co/api/v2/pokemon/" (name-checker name)) {:with-credentials? false
                                                                                      :query-params {}}))))
  (defn read-response [response-chan]

    (go
      (reset! loading true)
      (let [resp (<! response-chan)]
        (reset! data (:body resp))
        (reset! loading false))))

  (defn update-pokemon [value]
    (reset! all-image [{:name "default"
                        :image (get (get @data :sprites) :front_default)}
                       {:name "3d"
                        :image (get (get (get (get @data :sprites) :other) :home) :front_default)}])
    (read-response (get-pokemon (str/lower-case value))))

  ;; ---- Generate a random pokemon ----

  (defn get-random-pokemon []
    (read-response (get-pokemon (get (rand-nth @all-pokemon) :name))))

  (when (nil? (:name @data)) (get-random-pokemon))

;; ---- View ----
  (defn search-input [value]
    [:input
     {:type "text"
      :placeholder "Type a pokemon name eg: pikachu"
      :value @value
      :on-change #(reset! value (-> % .-target .-value))}])

  (let [val (r/atom "")]

    (defn on-submit-value [e] (.preventDefault e)
      (when (> (count @val) 0) (reset! input-value @val)
            (update-pokemon @input-value)))
    (fn []
      [:form {:on-submit on-submit-value}
       [search-input val]
       [:button {:class "search-btn" :style {:color (if (> (count @val) 0) "#2B3151" "#fff")
                                             :background-color (if (> (count @val) 0) "#f4b916" "#2b3151")}} "Search"]])))

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
  (fn []
    [:div.details-wrapper
     [:div.card
      [:div
       [:h2 (title-case (:name @data))]
       [view-selector]]
      [:div.image-wrapper {:on-click get-random-pokemon}
       (if @loading [:img {:src "/running-pikachu.gif" :alt "loading" :class "loading"}]
           [:img {:src (if
                        (= @view "original") (get (get @data :sprites) :front_default)
                        (get (get (get (get @data :sprites) :other) :home) :front_default)) :alt (:name @data)}])]

      [:ul.type-list
       (for [type (get @data :types)]
         ^{:key (get type :slot)}  [:li {:class (get (get type :type) :name)}
                                    [:div.tooltip  (title-case (get (get type :type) :name))]])]]]))

;; ---- Main ----
(defn pokedex []
  [:div.container
   [:header
    [:h1 "Pokedex"]]
   [search-form]
   (cond
     (nil? (:name @data)) [:div.error-wrapper
                           [:p "Feel free to input a name of a pokemon correctly. eg: groudon, pikachu, arcanine... Or generate a pokemon by pressing the button below"]
                           [:button {:on-click get-random-pokemon} "Generate a pokemon"]] :else [card])])

(defn render []
  (rdom/render [pokedex] (.getElementById js/document "root")))

(defn ^:export main []
  (render))

(defn ^:dev/after-load reload! []
  (render))
