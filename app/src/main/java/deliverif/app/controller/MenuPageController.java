/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package deliverif.app.controller;

import deliverif.app.model.graph.Tour;
import deliverif.app.model.map.Intersection;
import deliverif.app.model.map.Map;
import deliverif.app.model.map.Segment;
import deliverif.app.model.request.Observable;
import deliverif.app.model.request.Observer;
import deliverif.app.model.request.Path;
import deliverif.app.model.request.PlanningRequest;
import deliverif.app.model.request.Request;
import deliverif.app.view.App;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import org.graphstream.graph.Edge;
import org.graphstream.graph.EdgeRejectedException;
import org.graphstream.graph.ElementNotFoundException;
import org.graphstream.graph.Graph;
import org.graphstream.graph.IdAlreadyInUseException;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.spriteManager.Sprite;
import org.graphstream.ui.spriteManager.SpriteManager;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.util.InteractiveElement;
import java.util.Random;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import org.graphstream.graph.Node;
import org.graphstream.ui.graphicGraph.stylesheet.Selector;

/**
 *
 * @author fabien
 */
public class MenuPageController implements Observer {

    @FXML
    private Button loadCityMapButton;

    @FXML
    private Button loadRequestButton;

    @FXML
    private Button computeTourButton;

    @FXML
    private Button editTourButton;

    @FXML
    private AnchorPane mapPane;

    @FXML
    private Text loadMapText;

    @FXML
    private Text longitudeText;

    @FXML
    private Text latitudeText;

    @FXML
    private Text infosText;

    @FXML
    private Text segmentNameText;

    @FXML
    private ListView<Text> requestList;
    
    @FXML
    private ListView<Text> pathList;

    private final XmlReader xmlReader = new XmlReader();

    private Map map;

    private PlanningRequest planningRequest = null;

    private Graph graph;

    private FxViewPanel panel;

    private SpriteManager sman;

    private GraphProcessor graphProcessor;

    private Tour tour;
    
    private String[] selectedEdges = null;
    
    private List<String> graphEdges = new ArrayList<>();
    
    private PathThread pathThread = null;
    
    private String selectedNode = null;
    
    private ListOfCommands loc = null;

    public MenuPageController() {
        KeyboardEventManager kem = new KeyboardEventManager(this);
        loc = new ListOfCommands();
    }

    
    public void updateSelection(GraphicElement element) {
        
        System.out.println("UPDATE SELECTION");
        if (element.getSelectorType() == Selector.Type.EDGE) {
            Edge edge = graph.getEdge(element.getId());
            for (String id : this.graphEdges) {
                if (element.getId().equals(id)) {
                    System.out.println(id + " is on the path");
                    for (Text t : this.pathList.getItems()) {
                        if (t.getId().contains(id)) {
                            this.pathList.getSelectionModel().select(t);
                            String[] ids = t.getId().split("#");
                            String numString = t.getText().substring(5);
                            int num = Integer.parseInt(numString);
                            this.setSelectedPath(num);
                            break;
                        }
                    }
                    break;
                }
            }
            String name = (String) edge.getAttribute("segment.name");
            System.out.println(name);
            if (name != null) {
                segmentNameText.setText(name);
                sman.removeSprite("segmentSprite");

                Sprite segmentSprite = sman.addSprite("segmentSprite");
                double x, y, z;

                segmentSprite.setAttribute("ui.class", "segmentSprite");
                segmentSprite.setAttribute("ui.style", "fill-color: green;");
                Node origin = edge.getNode0();
                Node dest = edge.getNode1();
                Float longitude = (((Float) origin.getAttribute("x")) + ((Float) dest.getAttribute("x"))) / 2;
                Float latitude = (((Float) origin.getAttribute("y")) + ((Float) dest.getAttribute("y"))) / 2;
                segmentSprite.setPosition(longitude, latitude, 0);
            }
            return;
        }
        
        if (this.planningRequest == null) {
            return;
        }
        if (element.getSelectorType() != Selector.Type.SPRITE) {
            return;
        }
        
        String idElement = element.getId();
        
        this.setSelectedSprite(element.getId());
        Text spriteText = null;
        for(Text t : this.requestList.getItems()) {
            if(t.getId().equals(idElement)) {
                spriteText = t;
            }
        }
        this.requestList.getSelectionModel().select(spriteText);
               
    }

    @FXML
    private void loadCityMapAction() throws IOException {
        System.out.println("loadCityMapAction");
        this.map = App.choseMapFile(this.xmlReader);
        this.chargerGraph(this.map);
        this.graph.setAttribute("ui.stylesheet", App.styleSheet);
        //this.graph.setAutoCreate(true);
        //this.graph.setStrict(false);
        
        Viewer viewer = new FxViewer(graph, FxViewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
        panel = (FxViewPanel) viewer.addDefaultView(false);
        panel.enableMouseOptions();
        panel.setMouseManager(new MouseOverMouseManager(EnumSet.of(InteractiveElement.EDGE, InteractiveElement.SPRITE), this));
        mapPane.getChildren().remove(loadMapText);
        mapPane.getChildren().add(panel);
        AnchorPane.setTopAnchor(panel, 1.0);
        AnchorPane.setLeftAnchor(panel, 1.0);
        AnchorPane.setRightAnchor(panel, 1.0);
        AnchorPane.setBottomAnchor(panel, 1.0);
        graphProcessor = new GraphProcessor(map);
        sman = new SpriteManager(this.graph);
    }

    @FXML
    private void loadRequestAction() throws IOException {
        System.out.println("loadRequestAction");
        if (this.xmlReader.getMap() == null) {
            System.out.println("Il faut charger une map avant");
            return;
        }
        this.chargerPlanningRequests();
        //System.out.println(this.planningRequest);
    }

    @FXML
    private void computeTourAction() throws IOException {
        System.out.println("computeTourAction");
        tour = graphProcessor.optimalTour(this.planningRequest);
        tour.addObserver(this);

        renderTour();
    }
    
    public void renderTour() {
        Text txt = null;
        int cpt = 1;
        for (Path p : tour.getPaths()) {
            txt = new Text("Step " + cpt);
            String id = "";
            for (Segment s : p.getSegments()) {
                String originId = s.getOrigin().getId().toString();
                String destId = s.getDestination().getId().toString();
                Edge edge = graph.getEdge(originId + "|" + destId);
                if (edge != null) {
                    edge.setAttribute("ui.style", "fill-color: red;");
                    edge.setAttribute("ui.style", "size: 4px;");
                    edge.setAttribute("ui.class","pathEdge");
                    this.graphEdges.add(edge.getId());
                    id = id + edge.getId() + "#";
                } else {
                    edge = graph.getEdge(destId + "|" + originId);
                    if (edge != null) {
                        edge.setAttribute("ui.style", "fill-color: red;");
                        edge.setAttribute("ui.style", "size: 4px;");
                        edge.setAttribute("ui.class","pathEdge");
                        this.graphEdges.add(edge.getId());
                        id = id + edge.getId() + "#";
                    } else {
                        System.out.println("Edge not found");
                    }
                }
            }
            txt.setId(id);
            this.pathList.getItems().add(txt);
            cpt++;
        }
        System.out.println("compute tour done");
    }

    @FXML
    private void editTourAction() throws IOException {
        System.out.println("editTourAction");
    }

    private void chargerGraph(Map map) {
        this.graph = new SingleGraph("Graph test 1");

        map.getIntersections().entrySet().forEach((mapentry) -> {
            String nodeId = mapentry.getKey().toString();
            Intersection intersection = (Intersection) mapentry.getValue();
            graph.addNode(nodeId);
            graph.getNode(nodeId).setAttribute("x", intersection.getLongitude());
            graph.getNode(nodeId).setAttribute("y", intersection.getLatitude());

        });

        map.getSegments().forEach((s) -> {
            String origin = s.getOrigin().getId().toString();
            String destination = s.getDestination().getId().toString();
            try {
                graph.addEdge(origin + "|" + destination, origin, destination);
                graph.getEdge(origin + "|" + destination).setAttribute("segment.name", s.getName());
            } catch (EdgeRejectedException | ElementNotFoundException | IdAlreadyInUseException e) {
                //System.out.println("Error edge " + origin + " -> " + destination);
                Edge ed = graph.getEdge(destination + "|" + origin);
                if (ed != null) {
                    ed.setAttribute("ui.style", "size: 2px;");
                }
            }
        });
    }

    @FXML
    public void requestListClick(MouseEvent arg0) {
        System.out.println("clicked on " + requestList.getSelectionModel().getSelectedItem().getText());
        
        String spriteId = requestList.getSelectionModel().getSelectedItem().getId();
        this.setSelectedSprite(spriteId);
        
    }
    
    @FXML
    public void pathListClick(MouseEvent arg0) {
        System.out.println("clicked on " + this.pathList.getSelectionModel().getSelectedItem().getText());
        int num = Integer.parseInt(this.pathList.getSelectionModel().getSelectedItem().getText().substring(5));
        this.setSelectedPath(num);    
    }
    
    private void setSelectedPath(int num) {
        if (this.pathThread != null) {
            this.pathThread.end();
            while(this.pathThread.isIsFinished() == false) {}
        }
        this.pathThread = new PathThread(this, num);
        this.pathThread.start();
    }
    
    private void setSelectedSprite(String spriteId) {
        selectedNode = spriteId;
        Sprite sprite = sman.getSprite(spriteId);
        sman.removeSprite("bigSprite");
        Sprite bigSprite = sman.addSprite("bigSprite");
        bigSprite.setPosition(sprite.getX(),sprite.getY(),sprite.getZ());
        String spriteType = (String) sprite.getAttribute("ui.class");
        String bigSpriteType = spriteType + "Selected";
        bigSprite.setAttribute("ui.class",bigSpriteType);        
        bigSprite.setAttribute("ui.style",sprite.getAttribute("ui.style"));
        
        String longitude = "Longitude = ";
        String latitude = "Latitude = ";
        if(spriteType.equals("depotSprite")) {
            this.longitudeText.setText(longitude + String.valueOf(sprite.getX()));
            this.latitudeText.setText(latitude + String.valueOf(sprite.getY()));
            SimpleDateFormat hourFormat = new SimpleDateFormat("HH:mm:ss");
            String strDate = hourFormat.format(this.planningRequest.getDepot().getDepartureTime());
            this.infosText.setText("Departure time = " + strDate);
        } else {
            for (Request r : this.planningRequest.getRequests()) {
                String idPickupAddress = r.getPickupAddress().getId().toString();
                String idDeliveryAdress = r.getDeliveryAddress().getId().toString();
                if (idPickupAddress.equals(spriteId)) {
                    this.longitudeText.setText(longitude + String.valueOf(r.getPickupAddress().getLongitude()));
                    this.latitudeText.setText(latitude + String.valueOf(r.getPickupAddress().getLatitude()));
                    this.infosText.setText("Pickup duration = " + String.valueOf(r.getPickupDuration()));
                    return;
                }
                if (idDeliveryAdress.equals(spriteId)) {
                    this.longitudeText.setText(longitude + String.valueOf(r.getDeliveryAddress().getLongitude()));
                    this.latitudeText.setText(latitude + String.valueOf(r.getDeliveryAddress().getLatitude()));
                    this.infosText.setText("Delivery duration = " + String.valueOf(r.getDeliveryDuration()));
                    return;
                }
            }
        }
    }

    public int[] randomColorSprite() {

        Random rand = new Random();

        int min, max;
        min = 55;
        max = 200;

        int r = rand.nextInt((max - min) + 1) + min;
        int g = rand.nextInt((max - min) + 1) + min;
        int b = rand.nextInt((max - min) + 1) + min;

        int[] rgb = {r, g, b};
        return rgb;
    }

    private void chargerPlanningRequests() throws IOException {
        sman = new SpriteManager(this.graph);
        this.planningRequest = App.choseRequestFile(this.xmlReader);

        Text txt;
        Intersection depot = planningRequest.getDepot().getAddress();
        Sprite depotSprite = sman.addSprite(depot.getId().toString());
        depotSprite.setAttribute("ui.class", "depotSprite");
        depotSprite.setPosition(depot.getLongitude(), depot.getLatitude(), 0);
        txt = new Text("Depot");
        txt.setId(depotSprite.getId());
        txt.setFill(Color.RED);
        this.requestList.getItems().add(txt);
        int cpt = 1;
        for (Request r : planningRequest.getRequests()) {

            int[] rgb = randomColorSprite();
            String color = "fill-color: rgb(" + rgb[0] + "," + rgb[1] + "," + rgb[2] + ");";
            Intersection pickupAddress = r.getPickupAddress();
            Sprite pickupAddressSprite = sman.addSprite(pickupAddress.getId().toString());
            pickupAddressSprite.setAttribute("ui.class", "pickupSprite");
            pickupAddressSprite.setAttribute("ui.style", color);
            pickupAddressSprite.setPosition(pickupAddress.getLongitude(), pickupAddress.getLatitude(), 0);
            txt = new Text("Pickup " + cpt);
            txt.setId(pickupAddressSprite.getId());
            txt.setFill(Color.rgb(rgb[0], rgb[1], rgb[2]));
            this.requestList.getItems().add(txt);

            Intersection deliveryAdress = r.getDeliveryAddress();
            Sprite deliveryAdressSprite = sman.addSprite(deliveryAdress.getId().toString());
            deliveryAdressSprite.setAttribute("ui.class", "deliverySprite");
            deliveryAdressSprite.setAttribute("ui.style", color);
            deliveryAdressSprite.setPosition(deliveryAdress.getLongitude(), deliveryAdress.getLatitude(), 0);
            txt = new Text("Delivery " + cpt);
            txt.setId(deliveryAdressSprite.getId());
            txt.setFill(Color.rgb(rgb[0], rgb[1], rgb[2]));
            this.requestList.getItems().add(txt);
            cpt++;
        }
    }

    public Graph getGraph() {
        return graph;
    }

    public Tour getTour() {
        return tour;
    }
    
    public String getSelectedNode() {
        return selectedNode;
    }
    
    public void removeRequest() {
        if (selectedNode == null) return;
        Request selectedRequest = null;
        for (Request r : this.planningRequest.getRequests()) {
            String idPickupAddress = r.getPickupAddress().getId().toString();
            String idDeliveryAdress = r.getDeliveryAddress().getId().toString();
            if (idPickupAddress.equals(selectedNode)) {
                selectedRequest = r;
                break;
            }
            if (idDeliveryAdress.equals(selectedNode)) {
                selectedRequest = r;
                break;
            }
        }
        if (selectedRequest == null) return;
        RemoveRequest rr = new RemoveRequest(graphProcessor, tour, selectedRequest);
        loc.addCommand(rr);
        rr.doCommand();
        
    }

    @Override
    public void update(Observable observed, Object arg) {
        Tour t = (Tour) observed;
        if (t != tour) return;
        renderTour();
    }
}
