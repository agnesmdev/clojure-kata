(ns agnesmdev.coffee-machine
  (:require [clojure.string :as str]
            [clojure.walk]
            [mount.core :refer [defstate]]
            [muuntaja.core :as m]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [ring.adapter.jetty :refer [run-jetty]]
            ))

(def drinks (atom {"T" {:initial "T" :name "tea" :price 40 :sold 0}
             "C" {:initial "C" :name "coffee" :price 60 :sold 0}
             "H" {:initial "H" :name "chocolate" :price 50 :sold 0}
             "O" {:initial "O" :name "orange juice" :price 60 :sold 0}}))

(defn format-int! [value type]
  (try
    (Integer/parseInt value)
    (catch NumberFormatException _ (throw (NumberFormatException. (str "Unexpected " type ", expected integer, got " value))))))

(defn sold-drink [drink]
  (swap! drinks conj {(:initial drink) (conj drink {:sold (inc (:sold drink))})}))

(defn make-order [drink-and-hot sugar stick rest money]
  (if (nil? (:drink drink-and-hot))
    {:status 200 :body (str/join ":" (filter not-empty (concat [sugar stick] rest)))}
    (try
      (let [sugar-int (format-int! sugar "sugar quantity")
            money-int (format-int! money "money")
            drink (:drink drink-and-hot)
            remainder (- money-int (:price drink))]
        (when (>= remainder 0) (sold-drink drink))
        (if (< remainder 0)
          {:status 400 :body (str "Missing " (- remainder) " cents for " (:name drink))}
          {:status 201 :body {:drink     (:name drink)
                              :sugar     sugar-int
                              :stick     (= "0" stick)
                              :remainder remainder
                              :extra-hot (:extra-hot drink-and-hot)}}))
      (catch NumberFormatException ex {:status 400 :body (ex-message ex)}))))

(defn extract-drink [drink-name]
  (let [[initial extra-hot & rest] (str/split drink-name #"" 3)
        drink (get @drinks initial)]
    (cond
      (= "M" initial) {:drink nil}
      (not-empty (first rest)) nil
      (nil? drink) nil
      (and (not-empty extra-hot) (not= "h" extra-hot)) nil
      (and (= "O" initial) (not-empty extra-hot)) nil
      :else {:drink (get @drinks initial) :extra-hot (not (nil? extra-hot))})))

(defn order-drink [drink-name sugar stick rest money]
  (let [drink (extract-drink drink-name)]
    (if (nil? drink)
      {:status 400 :body (str "Unexpected drink, expected T, H, C or M, got " drink-name)}
      (make-order drink sugar stick rest money)
      )))

(defn handle-order [request]
  (let [params (:query-params request)
        money (get params "money")
        [drink-name sugar stick & rest] (str/split (get params "order") #":" 4)]
    (order-drink drink-name sugar stick rest money)))

(defn extract-total-drinks-sold []
  (->>@drinks
      (map (fn [[_ drink]] (* (:sold drink) (:price drink))))
      (reduce +)))

(defn extract-drinks-sold []
  (flatten (map (fn [[_ drink]] {(:name drink) (:sold drink)}) @drinks)))

(defn handle-report [_]
  {:status 200 :body {:sold (extract-drinks-sold) :total (extract-total-drinks-sold)}})

(def app
  (ring/ring-handler
    (ring/router
      ["/coffee-machine"
       ["/order" {:post {:handler handle-order}}]
       ["/report" {:get {:handler handle-report}}]]
      {:data {:muuntaja   m/instance
              :middleware [muuntaja/format-middleware
                           coercion/coerce-exceptions-middleware
                           coercion/coerce-request-middleware
                           coercion/coerce-response-middleware
                           parameters/parameters-middleware]}})))

(defstate server
          :start (run-jetty #'app {:port 8080 :join? false})
          :stop (.stop server))
(defn -main
  [& _args]
  (mount.core/start))
