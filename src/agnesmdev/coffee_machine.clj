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
             "M" {}})

(defn format-int! [value type]
  (try
    (Integer/parseInt value)
    (catch NumberFormatException _ (throw (NumberFormatException. (str "Unexpected " type ", expected integer, got " value))))))

(defn make-order [drink sugar stick rest money]
  (if (nil? (:name drink))
    {:status 200 :body (str sugar ":" stick ":" (str/join ":" rest))}
    (try
      (let [sugar-int (format-int! sugar "sugar quantity")
            money-int (format-int! money "money")
            remainder (- money-int (:price drink))]
        (if (< remainder 0)
          {:status 400 :body (str "Missing " (- remainder) " cents for " (:name drink))}
          {:status 201 :body {:drink (:name drink)
                              :sugar sugar-int
                              :stick (= "0" stick)
                              :remainder remainder}}))
      (catch NumberFormatException ex {:status 400 :body (ex-message ex)}))))

(defn order-drink [drink-name sugar stick rest money]
  (let [drink (get drinks drink-name)]
    (if (nil? drink)
      {:status 400 :body (str "Unexpected drink, expected T, H, C or M, got " drink-name)}
      (make-order drink sugar stick rest money)
      )))

(defn handle-order [request]
  (let [params (:query-params request) money (get params "money")]
    (let [[drink-name sugar stick & rest] (str/split (get params "order") #":" 4)]
      (order-drink drink-name sugar stick rest money))))

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
