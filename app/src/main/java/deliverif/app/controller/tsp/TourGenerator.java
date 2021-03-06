/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package deliverif.app.controller.tsp;

import deliverif.app.controller.tsp.BranchAndBound.TSP1;
import deliverif.app.controller.GraphProcessor;
import deliverif.app.controller.Observer.Observable;
import deliverif.app.controller.Observer.Observer;
import deliverif.app.model.graph.Tour;
import deliverif.app.model.graph.Vertex;
import deliverif.app.model.map.Intersection;
import deliverif.app.model.map.Map;
import deliverif.app.model.request.Path;
import deliverif.app.model.request.PlanningRequest;
import deliverif.app.model.request.Request;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.Semaphore;
import javafx.application.Platform;

/**
 * Handles TSP updates of the best tour found
 * @author polo
 */
public class TourGenerator extends Observable implements Observer {

    /**
     * Planning request that we are trying to compute the tour
     */
    private final PlanningRequest pr;
    /**
     * Current graph processor controller
     */
    private final GraphProcessor gp;
    /**
     * Current map
     */
    private final Map map;
    /**
     * Current best tour found by the TSP
     */
    private Tour tour;
    /**
     * Time at which the last render occurred
     */
    private long lastRender = 0;

    /**
     * Create a tour generator used to be notified whenever a new best tour is found by the algorithm
     * @param tsp the travelling salesman algorithm running
     * @param pr the planning request of the current computed tour
     * @param gp the graph processor processing the current t
     * @param map the current map
     */
    public TourGenerator(TSP1 tsp, PlanningRequest pr, GraphProcessor gp, Map map) {
        super();
        this.pr = pr;
        this.gp = gp;
        this.tour = null;
        this.map = map;
        tsp.addObserver(this);
    }

    @Override
    public void update(Observable observed, Object arg) {
        if (!hasObservers()) {
            computeTour((TSP1)observed);
            return;
        }
        boolean isFinished = (boolean) arg;
        if (!isFinished && lastRender != 0 && System.currentTimeMillis() - lastRender < 5000) {
            return;
        }
        lastRender = System.currentTimeMillis();
        Platform.runLater(() -> {
            computeTour((TSP1)observed);
            notifiyObservers(arg);
        });

    }

    /**
     * Compute a tour from vertex array returned by algorithm
     * @param tsp the tsp object containing the current best solution
     */
    private void computeTour(TSP1 tsp) {
        tour = new Tour(pr);
        Vertex[] sol = (tsp).getSolution();
        double velocity = 15. * 1000. / 3600.;
        Calendar cal = Calendar.getInstance();
        cal.setTime(pr.getDepot().getDepartureTime());
        if (sol == null || sol[0] == null) {
            return;
        }
        gp.currentVertex.addAll(Arrays.asList(sol));
        HashMap<Long, Integer> pickups = new HashMap<>();
        HashMap<Long, Integer> deliveries = new HashMap<>();
        for (Request r : pr.getRequests()) {
            pickups.put(r.getPickupAddress().getId(), r.getPickupDuration());
            deliveries.put(r.getDeliveryAddress().getId(), r.getDeliveryDuration());
        }
        System.out.println("---------Tour Deets---------");
        // Adding paths excluding warehouse
        for (int i = 0; i < sol.length - 1; i++) {
            Intersection curr = map.getIntersectionParId(sol[i].getId());
            Intersection next = map.getIntersectionParId(sol[i + 1].getId());
            Path path = gp.shortestPathBetweenTwoIntersections(curr, next);
            path.setDepatureTime(cal.getTime());
            int commute = (int) (path.getLength() / velocity);

            cal.add(Calendar.SECOND, commute);
            path.setArrivalTime(cal.getTime());
            if (pickups.containsKey(next.getId())) {
                commute = pickups.get(next.getId());
            } else if (deliveries.containsKey(next.getId())) {
                commute = deliveries.get(next.getId());
            }
            cal.add(Calendar.SECOND, commute);
            //System.out.println("Path" + i + ":" + path + "\n");
            tour.addPath(path);
        }
        // Adding path back to warehouse
        Intersection last = map.getIntersectionParId(sol[sol.length - 1].getId());
        Intersection warehouse = pr.getDepot().getAddress();
        Path back = gp.shortestPathBetweenTwoIntersections(last, warehouse);
        back.setDepatureTime(cal.getTime());
        int commute = (int) (back.getLength() / velocity);
        cal.add(Calendar.SECOND, commute);
        back.setArrivalTime(cal.getTime());
        tour.addPath(back);
        System.out.println("-----------End of Tour---------");
    }

    /**
     * Get the current best tour
     * @return current best tour
     */
    public Tour getTour() {
        return tour;
    }
}
