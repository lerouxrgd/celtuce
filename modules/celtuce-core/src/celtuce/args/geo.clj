(ns celtuce.args.geo
  (:import 
   (io.lettuce.core
    GeoArgs GeoArgs$Unit GeoArgs$Sort GeoRadiusStoreArgs)))

(defn ^GeoArgs$Unit ->unit [u]
  (case u
    :m  GeoArgs$Unit/m
    :km GeoArgs$Unit/km
    :ft GeoArgs$Unit/ft
    :mi GeoArgs$Unit/mi
    (throw (ex-info "Invalid Unit" {:unit u :valids #{:m :km :ft :mi}}))))

(defn ^GeoArgs$Sort ->sort [s]
  (case s
    :asc   GeoArgs$Sort/asc
    :desc  GeoArgs$Sort/desc
    :none  GeoArgs$Sort/none
    (throw (ex-info "Invalid Sort" {:sort s :valids #{:asc :desc :none}}))))

(defn geo-args [& {with-dist :with-dist with-coord :with-coord with-hash :with-hash
                   count :count sort :sort}]
  (cond-> (GeoArgs.)
    with-dist  (.withDistance)
    with-coord (.withCoordinates)
    with-hash  (.withHash)
    count      (.withCount count)
    sort       (.sort (->sort sort))))

(defn georadius-store-args [& {store-key :store-key store-dist-key :store-dist-key
                               count :count sort :sort}]
  (cond-> (GeoRadiusStoreArgs.)
    store-key      (.withStore store-key)
    store-dist-key (.withStoreDist store-dist-key)
    count          (.withCount count)
    sort           (.sort (->sort sort))))

