(ns fc-rings.core
  (:require [clojure.math.combinatorics :as combo]
            [overtone.osc :as osc]
            [quil.core :as q]
            [quil.middleware :as m])
  (:gen-class))

(set! *warn-on-reflection* true)

(def width 512)
(def height 288)

(def grid-x 8)
(def grid-y 8)

(defonce ctrl-state (atom {}))

(defn update-hue
  [{:keys [args] :as msg}]
  (let [[hue] args]
    (when hue
      (swap! ctrl-state assoc :hue hue))))

(defonce osc-server (atom nil))

(defn setup []
  (q/frame-rate 30)
  (q/color-mode :hsb 100.0)

  (when (nil? @osc-server)
    (reset! osc-server (osc/osc-server 4242)))
  (osc/osc-handle @osc-server "/rings/hue" update-hue)
  {:now 0
   :dx  0
   :dy  0
   :dz  0})

(defn update-state [state]
  (let [now    (q/millis)
        speed  0.002
        zspeed 0.1
        angle  (q/sin (* now 0.001))]
    {:now now
     :dx  (+ (:dx state) (* (q/cos angle) speed))
     :dy  (+ (:dy state) (* (q/sin angle) speed))
     :dz  (+ (:dz state) (* (-
                             (q/noise (* now 0.000014))
                             0.5)
                            zspeed))}))

(defn fractal-noise
  [sx sy sz]
  (let [iter-fn (fn [params octaves]
                  (loop [[x y z r amp] params
                         n             octaves]
                    (if (zero? n)
                      r
                      (recur [(* 2 x)
                              (* 2 y)
                              (* 2 z)
                              (+ r (* (q/noise x y z) amp))
                              (/ amp 2)]
                             (dec n)))))
        start   [sx sy sz 0 1.0]]
    (iter-fn start 4)))

(defn render-grid
  []
  (let [x-count (/ (q/width) grid-x)
        y-count (/ (q/height) grid-y)]
    (combo/cartesian-product
     (map #(int (* (+ % 0.5) (/ (q/width) x-count))) (range x-count))
     (map #(int (* (+ % 0.5) (/ (q/height) y-count))) (range y-count)))))

(defn fast-aset
  [pixels idx col]
  (let [^ints p pixels
        ^int i  idx
        ^int c  col]
    (aset p i c)))

(defn draw-state
  [{:keys [now dx dy dz] :as state}]
  (q/background 0)
  (let [coords     (render-grid)
        z          (* now 0.00008)
        hue        (* now 0.01)
        saturation (* 100
                      (q/constrain
                       (q/pow
                        (* 1.15 (q/noise (* now 0.000122)))
                        2.5)
                       0 1))
        scale      0.005
        spacing    (* (q/noise (* now 0.000124))
                      0.1)
        centerx    (* (q/noise (* now 0.000125))
                      1.25
                      (q/width))
        centery    (* (q/noise (* now -0.000125))
                      1.25
                      (q/height))]
    (run!
     (fn [[x y]]
       (let [dist  (q/sqrt (+ (q/pow (- x centerx) 2)
                              (q/pow (- y centery) 2)))
             pulse (* (- (q/sin (+ dz (* dist spacing)))
                         0.3)
                      0.3)
             n     (- (fractal-noise (+ dx (* x scale) pulse)
                                     (+ dy (* y scale))
                                     z)
                      0.75)
             m     (- (fractal-noise (+ dx (* x scale))
                                     (+ dy (* y scale))
                                     (+ z 10.0))
                      0.75)
             idx   (+ x (* y (q/width)))
             color (q/color (mod (+ hue
                                    (* 40.0 m)
                                    (:hue @ctrl-state))
                                 100)
                            saturation
                            (* 100.0
                               (q/constrain (q/pow (* 3.0 n) 1.5)
                                            0.0 0.9)))]
         (q/fill color)
         (q/stroke 0 0 0 20)
         (q/rect (- x (/ grid-x 2))
                 (- y (/ grid-y 2))
                 grid-x
                 grid-y)
         #_(q/ellipse x y
                 grid-x
                 grid-y)))
     coords)
    (q/fill 100)
    (q/text (str (q/current-frame-rate)) 20 20)))

(defn -main [& args]
  (q/defsketch fc-rings
    :title "Rings"
    :size [width height]
    :setup setup
    :update update-state
    :draw draw-state
    :features [:keep-on-top]
    :middleware [m/pause-on-error
                 m/fun-mode]))
