(deffacts startup
(tile F)
(tile B)
(tile L)
(tile R)
(tile C)
)

(defrule cleanOptimization
(declare (salience 5))
(clean ?X)
(not (and (CStation ?X) (isReturning)) )
=>
(assert(wall ?X))
)

(defrule lowBatteryReturn
(declare (salience 5))
(or (lowBattery) (isCharging))
(tile ?X)
(not (BCrumb ?X))
(not (and (CStation ?X) (isReturning)) )
=>
(assert(wall ?X))
)

(defrule charge
(declare (salience 5))
(CStation C)
(or (isReturning) (isCharging))
=>
(charge)
(assert(standby)))

(defrule clean1
(declare (salience 4))
(not(clean C))
(not(wall C))
(or (Floor C) (CStation C))
(isCleaning)
=>
(cleanFloor))

(defrule clean2
(declare (salience 4))
(not(clean C))
(not(wall C))
(Carpet C)
(isCleaning)
=>
(cleanCarpet))

(defrule trap
(wall F)
(wall B)
(wall L)
(wall R)
(not(standby))
=>
(handleTrap)
)

(defrule free
(not (wall F))
(not (wall B))
(not (wall L))
(not (wall R))
=>
(fwd)
)

(defrule fwd
(wall R)
(not (wall F))
=>
(fwd))
 
(defrule left2
(not(wall R))
(wall F)
(not (wall B))
=>
(lt90))

(defrule right1
(not(wall R))
(wall B)
=>
(rt90)
(assert(wall L)))

(defrule left1
(not (wall L))
(wall F)
=>
(lt90)
(assert(wall R)))

(defrule flip1
(wall R)
(wall F)
(wall L)
(not (wall B))
=>
(rt90)
(rt90))

(defrule flip2
(not (wall R))
(not (wall F))
(wall L)
(not (wall B))
=>
(rt90)
(rt90))