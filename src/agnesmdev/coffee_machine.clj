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

(def drinks {"T" {:name "tea" :price 40}
             "C" {:name "coffee" :price 60}
             "H" {:name "chocolate" :price 50}
             "O" {:name "orange juice" :price 60}
             "M" {}})

(defn format-int! [value type]
  (try
    (Integer/parseInt value)
    (catch NumberFormatException _ (throw (NumberFormatException. (str "Unexpected " type ", expected integer, got " value))))))

(defn make-order [drink-and-hot sugar stick rest money]
  (if (nil? (get-in drink-and-hot [:drink :name]))
    {:status 200 :body (str/join ":" (filter not-empty (concat [sugar stick] rest)))}
    (try
      (let [sugar-int (format-int! sugar "sugar quantity")
            money-int (format-int! money "money")
            drink (:drink drink-and-hot)
            remainder (- money-int (:price drink))]
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
        drink (get drinks initial)]
    (cond
      (not-empty (first rest)) nil
      (nil? drink) nil
      (and (not-empty extra-hot) (not= "h" extra-hot)) nil
      (and (= "O" initial) (not-empty extra-hot)) nil
      :else {:drink (get drinks initial) :extra-hot (not (nil? extra-hot))})))

(comment
  (extract-drink "Th")
  (extract-drink "T")
  (get drinks "T")
  (get drinks \T))

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

(def app
  (ring/ring-handler
    (ring/router
      ["/coffee-machine"
       ["/order" {:post {:handler handle-order}}]]
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
