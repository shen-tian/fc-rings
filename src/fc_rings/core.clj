(ns fc-rings.core
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [clojure.math.combinatorics :as combo])
  (:gen-class))

(set! *warn-on-reflection* true)

(def width 192)
(def height 192)

(defn setup []
  ; Set frame rate to 30 frames per second.
  (q/frame-rate 30)
  ; Set color mode to HSB (HSV) instead of default RGB.
  (q/color-mode :hsb 100.0)
  ; setup function returns initial state. It contains
  ; circle color and position.
  {:now 0
   :dx         0
   :dy         0
   :dz         0})

(defn update-state [state]
  ; Update sketch state by changing circle color and position.
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
  (let [iter-fn     (fn [[x y z r amp]]
                      [(* 2 x)
                       (* 2 y)
                       (* 2 z)
                       (+ r (* (q/noise x y z)
                               amp))
                       (/ amp 2)])
        start       [sx sy sz 0 1.0]
        [_ _ _ r _] (nth (iterate iter-fn start) 4)
        ]
    r))

(defn draw-state
  [{:keys [now dx dy dz] :as state}]
  (let [^ints pixels     (q/pixels)
        coords     (combo/cartesian-product (range (q/width)
                                             )
                                            (range (q/height)
                                             ))
        z          (* now 0.00008)
        hue        (* now 0.01)
        saturation (* 100
                      (q/constrain
                       (q/pow
                        (* 1.15 (q/noise (* now 0.000122)))
                        2.5)
                       0 1))
        scale      0.005
        spacing    (* (- (q/noise (* now 0.000014))
                         0.5)
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

             ^int idx   (+ x (* y (q/width)))
             ^int color (q/color (mod (+ hue (* 40.0 m))
                                 100)
                            saturation
                            (* 100.0
                               (q/constrain (q/pow (* 3.0 n) 1.5)
                                            0.0 0.9)))]
         (aset pixels idx
               color)))
     coords)
    (q/update-pixels)))

(defn -main [& args]
  (q/defsketch fc-rings
    :title "You spin my circle right round"
    :size [width height]
    ;;:renderer :p2d
    ; setup function called only once, during sketch initialization.
    :setup setup
    ; update-state is called on each iteration before draw-state.
    :update update-state
    :draw draw-state
    :features [:keep-on-top]
    ; This sketch uses functional-mode middleware.
    ; Check quil wiki for more info about middlewares and particularly
    ; fun-mode.
    :middleware [m/pause-on-error
                 m/fun-mode]))
