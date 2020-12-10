/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package deliverif.app.model.graph;

import deliverif.app.controller.Observer.Observable;
import deliverif.app.model.request.Path;
import deliverif.app.model.request.PlanningRequest;
import deliverif.app.model.request.Request;
import java.util.ArrayList;
import java.util.Date;

/**
 *
 * @author zakaria
 */
public class Tour extends Observable {

    private ArrayList<Path> paths;
    private PlanningRequest pr;

    /**
     * Create a new tour
     *
     * @param pr the planning request of the tour
     */
    public Tour(PlanningRequest pr) {
        this.pr = pr;
        this.paths = new ArrayList<>();
    }

    /**
     * Create a copy of tour
     *
     * @param t the tour to copy
     */
    public Tour(Tour t) {
        this.pr = t.pr;
        paths = new ArrayList<>();
        for (Path p : t.getPaths()) {
            paths.add(p);
        }
    }

    /**
     * Copy the content of a given tour in the current tour
     *
     * @param t the tour to copy
     */
    public void copyTour(Tour t) {
        this.pr = new PlanningRequest();
        pr.setDepot(t.getPr().getDepot());
        for (Request r : t.getPr().getRequests()) {
            pr.addRequest(r);
        }
        paths.clear();
        for (Path p : t.getPaths()) {
            paths.add(p);
        }
    }

    /**
     * Get total tour distance
     *
     * @return total tour distance
     */
    public float getTotalDistance() {
        float total = 0;
        for (Path p : paths) {
            total += p.getLength();
        }
        return total;
    }

    /**
     * Get total tour duration
     *
     * @return Total duration of the tour (seconds)
     */
    public int getTotalDuration() {
        return (int) ((paths.get(paths.size() - 1).getArrivalTime().getTime()
                - pr.getDepot().getDepartureTime().getTime()) / 1000);
    }

    /**
     * Get tour departure time
     *
     * @return tour departure time
     */
    public Date getDepartureTime() {
        return paths.get(0).getDepatureTime();
    }

    /**
     * Get tour arrival time
     *
     * @return tour arrival time
     */
    public Date getArrivalTime() {
        return paths.get(paths.size() - 1).getArrivalTime();
    }

    /**
     * Add a list of paths to the tour
     *
     * @param pl the list of paths to add
     */
    public void addPaths(ArrayList<Path> pl) {
        for (Path p : pl) {
            paths.add(p);
        }
    }

    /**
     * Add a path to the tour
     *
     * @param p the path to add
     */
    public void addPath(Path p) {
        paths.add(p);
    }

    /**
     * Remove a path from the tour
     *
     * @param p the path to remove
     */
    public void removePath(Path p) {
        paths.remove(p);
    }

    /**
     * Get the tour list of paths
     *
     * @return the list of paths
     */
    public ArrayList<Path> getPaths() {
        return paths;
    }

    /**
     * Get the tour planning request
     *
     * @return the tour planning request
     */
    public PlanningRequest getPr() {
        return pr;
    }

    /**
     * Set the tour list of paths
     *
     * @param paths list of paths
     */
    public void setPaths(ArrayList<Path> paths) {
        this.paths = paths;
    }

    /**
     * Set the planning request linked to the tour
     *
     * @param pr the new planning request
     */
    public void setPr(PlanningRequest pr) {
        this.pr = pr;
    }

    @Override
    public String toString() {
        String pathDetails = "";
        for (Path p : paths) {
            pathDetails += p.toString();
        }
        return "Tour{" + "paths=" + pathDetails + ", pr=" + pr + '}';
    }

}
