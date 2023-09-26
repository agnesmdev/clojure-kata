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

(def drinks {"T" {:name "tea" :price 0.4}
             "C" {:name "coffee" :price 0.6}
             "H" {:name "chocolate" :price 0.5}
             "M" {}})

(defn make-order [drink sugar rest]
  (if (nil? (:name drink))
    {:status 200 :body (str sugar ":" (str/join ":" rest))}
    (try
      (let [sugar-int (Integer/parseInt sugar)]
        {:status 201 :body {:drink (:name drink)
                            :sugar sugar-int
                            :stick (> 0 sugar-int)}})
      (catch NumberFormatException _ {:status 400 :body (str "Unexpected sugar quantity, expected number, got " sugar)}))))

(defn order-drink [drink-name sugar rest]
  (let [drink (get drinks drink-name)]
    (if (nil? drink)
      {:status 400 :body (str "Unexpected drink, expected T, H, C or M, got " drink-name)}
      (make-order drink sugar rest)
      )))

(defn handle-order [request]
  (let [[drink-name sugar & rest] (str/split (:query-string request) #":" 3)]
    (order-drink drink-name sugar rest)))

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
